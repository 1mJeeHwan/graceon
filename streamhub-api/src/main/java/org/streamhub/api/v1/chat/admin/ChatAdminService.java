package org.streamhub.api.v1.chat.admin;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.chat.admin.dto.ChatMessageRow;
import org.streamhub.api.v1.chat.admin.dto.ChatReplyRequest;
import org.streamhub.api.v1.chat.admin.dto.ChatSessionRow;
import org.streamhub.api.v1.chat.entity.ChatMessage;
import org.streamhub.api.v1.chat.entity.ChatRole;
import org.streamhub.api.v1.chat.entity.ChatSession;
import org.streamhub.api.v1.chat.repository.ChatMessageRepository;

/**
 * Admin chat-console orchestration (C5): lists conversation sessions for the operator, exposes
 * the full message thread, and appends a manual operator reply. Read-only over the existing chat
 * tables except for the operator reply, which is persisted as a {@code BOT} turn (the
 * assistant/operator side — {@link ChatRole} has no dedicated admin value). The public widget
 * flow in {@code ChatService} is left untouched.
 */
@Service
public class ChatAdminService {

    private final ChatSessionAdminRepository chatSessionAdminRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatAdminService(
            ChatSessionAdminRepository chatSessionAdminRepository,
            ChatMessageRepository chatMessageRepository) {
        this.chatSessionAdminRepository = chatSessionAdminRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    /** Lists all sessions ordered by last activity (newest first); unanswered sessions surface their flag. */
    @Transactional(readOnly = true)
    public List<ChatSessionRow> listSessions() {
        return chatSessionAdminRepository.findAllByOrderByIdDesc().stream()
                .map(this::toRow)
                .sorted(Comparator.comparing(
                        ChatSessionRow::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Full message thread for a session, oldest first. */
    @Transactional(readOnly = true)
    public List<ChatMessageRow> messages(String sessionKey) {
        ChatSession session = chatSessionAdminRepository.findAllByOrderByIdDesc().stream()
                .filter(s -> s.getSessionKey().equals(sessionKey))
                .findFirst()
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId()).stream()
                .map(ChatMessageRow::from)
                .toList();
    }

    /** Appends a manual operator reply (stored as a {@code BOT} turn) and returns the created message. */
    @Transactional
    public ChatMessageRow reply(String sessionKey, ChatReplyRequest request) {
        ChatSession session = chatSessionAdminRepository.findAllByOrderByIdDesc().stream()
                .filter(s -> s.getSessionKey().equals(sessionKey))
                .findFirst()
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .sessionId(session.getId())
                .role(ChatRole.BOT)
                .content(request.getContent())
                .build());
        return ChatMessageRow.from(saved);
    }

    // --- helpers -----------------------------------------------------------

    private ChatSessionRow toRow(ChatSession session) {
        List<ChatMessage> messages =
                chatMessageRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        ChatMessage last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        return ChatSessionRow.from(session, last, messages.size());
    }
}
