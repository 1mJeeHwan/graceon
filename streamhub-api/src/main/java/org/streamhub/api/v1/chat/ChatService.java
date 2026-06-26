package org.streamhub.api.v1.chat;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.v1.chat.adapter.ChatProvider;
import org.streamhub.api.v1.chat.adapter.ChatProviderRouter;
import org.streamhub.api.v1.chat.adapter.ChatReply;
import org.streamhub.api.v1.chat.adapter.ChatTurn;
import org.streamhub.api.v1.chat.dto.ChatHistoryItem;
import org.streamhub.api.v1.chat.dto.ChatReplyDto;
import org.streamhub.api.v1.chat.dto.ChatSendRequest;
import org.streamhub.api.v1.chat.entity.ChatIntent;
import org.streamhub.api.v1.chat.entity.ChatMessage;
import org.streamhub.api.v1.chat.entity.ChatRole;
import org.streamhub.api.v1.chat.entity.ChatSession;
import org.streamhub.api.v1.chat.entity.ChatUnanswered;
import org.streamhub.api.v1.chat.repository.ChatMessageRepository;
import org.streamhub.api.v1.chat.repository.ChatSessionRepository;
import org.streamhub.api.v1.chat.repository.ChatUnansweredRepository;

/**
 * Chatbot orchestration (C5): resolves/creates the session, loads the recent conversation, persists
 * the USER turn, delegates to the configured {@link ChatProvider} (rule by default; Gemini when
 * {@code app.chat.provider=llm}), persists the BOT turn with its intent, and returns the reply.
 *
 * <p>The last {@value #MAX_HISTORY_TURNS} stored turns are passed to the provider as context, so the
 * LLM provider supports multi-turn follow-ups; the rule provider ignores them. {@code testMode} is
 * true only for the rule provider, so the widget can label a real-LLM reply honestly.
 */
@Service
public class ChatService {

    /** Max prior turns fed back to the provider as context (bounds LLM token use). */
    private static final int MAX_HISTORY_TURNS = 10;

    /** Source of session secrets — cryptographically strong, shared (thread-safe). */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatUnansweredRepository chatUnansweredRepository;
    private final ChatProviderRouter chatProviderRouter;

    public ChatService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            ChatUnansweredRepository chatUnansweredRepository,
            ChatProviderRouter chatProviderRouter) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatUnansweredRepository = chatUnansweredRepository;
        this.chatProviderRouter = chatProviderRouter;
    }

    /** Handles a user message: persists USER + BOT turns (with prior context) and returns the reply. */
    @Transactional
    public ChatReplyDto send(ChatSendRequest request) {
        ChatProvider provider = chatProviderRouter.resolve();
        ChatSession session = resolveOwnedSession(request, provider.code());

        // Prior turns (before this message) become the provider's conversation context.
        List<ChatTurn> history = recentHistory(session.getId());

        chatMessageRepository.save(ChatMessage.builder()
                .sessionId(session.getId())
                .role(ChatRole.USER)
                .content(request.message())
                .build());

        ChatReply reply = provider.reply(request.message(), history);

        chatMessageRepository.save(ChatMessage.builder()
                .sessionId(session.getId())
                .role(ChatRole.BOT)
                .intent(reply.intent())
                .content(reply.text())
                .build());

        // Learning queue (A): a FALLBACK means the bot had no answer — collect the question so an
        // operator can review and turn it into knowledge.
        if (reply.intent() == ChatIntent.FALLBACK) {
            chatUnansweredRepository.save(ChatUnanswered.builder()
                    .question(request.message())
                    .sessionKey(session.getSessionKey())
                    .build());
        }

        boolean testMode = !"LLM".equals(provider.code());
        return ChatReplyDto.of(reply, testMode, session.getSessionKey(), session.getSessionToken());
    }

    /**
     * Resolves the session the caller owns, or issues a fresh one. The caller continues an existing
     * session only by presenting its secret {@code sessionToken}; an unknown key, a missing token, or
     * a token mismatch all yield a brand-new server-issued session (self-healing — never a hard error)
     * so no caller can hijack or write into another session by knowing only its {@code sessionKey}.
     */
    private ChatSession resolveOwnedSession(ChatSendRequest request, String providerCode) {
        if (request.sessionKey() != null && !request.sessionKey().isBlank()) {
            ChatSession existing = chatSessionRepository.findBySessionKey(request.sessionKey()).orElse(null);
            if (existing != null) {
                if (existing.getSessionToken() != null
                        && existing.getSessionToken().equals(request.sessionToken())) {
                    return existing; // proven owner → continue
                }
                // Key exists but caller can't prove ownership → fall through to a fresh session.
            } else {
                // Unknown key the caller chose: safe to create it (no one owns it yet).
                return createSession(request.sessionKey(), providerCode);
            }
        }
        return createSession(randomKey(), providerCode);
    }

    private ChatSession createSession(String sessionKey, String providerCode) {
        return chatSessionRepository.save(ChatSession.builder()
                .sessionKey(sessionKey)
                .sessionToken(randomToken())
                .provider(providerCode)
                .build());
    }

    /** 32-byte URL-safe random — the session's secret capability. */
    private String randomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 18-byte URL-safe random session id (≤40 chars) when the client supplied none. */
    private String randomKey() {
        byte[] bytes = new byte[18];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Loads the last {@value #MAX_HISTORY_TURNS} stored turns for a session (oldest first). */
    private List<ChatTurn> recentHistory(Long sessionId) {
        List<ChatMessage> messages =
                chatMessageRepository.findBySessionIdOrderByCreatedAtAscIdAsc(sessionId);
        int from = Math.max(0, messages.size() - MAX_HISTORY_TURNS);
        return messages.subList(from, messages.size()).stream()
                .map(m -> new ChatTurn(m.getRole(), m.getContent()))
                .toList();
    }

    /**
     * Returns a session's full message history (oldest first), but ONLY to the owner: the caller must
     * present the session's secret {@code sessionToken}. A missing/mismatched token (or unknown key)
     * yields an empty list — never the conversation — so the public history endpoint can't be used to
     * read another user's chat by its {@code sessionKey} alone.
     */
    @Transactional(readOnly = true)
    public List<ChatHistoryItem> history(String sessionKey, String sessionToken) {
        ChatSession session = chatSessionRepository.findBySessionKey(sessionKey).orElse(null);
        if (session == null || session.getSessionToken() == null
                || sessionToken == null || !session.getSessionToken().equals(sessionToken)) {
            return List.of();
        }
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId()).stream()
                .map(ChatHistoryItem::from)
                .toList();
    }
}
