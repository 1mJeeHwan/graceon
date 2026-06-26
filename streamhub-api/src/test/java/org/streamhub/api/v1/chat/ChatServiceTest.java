package org.streamhub.api.v1.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.streamhub.api.v1.chat.adapter.ChatProvider;
import org.streamhub.api.v1.chat.adapter.ChatProviderRouter;
import org.streamhub.api.v1.chat.adapter.ChatReply;
import org.streamhub.api.v1.chat.dto.ChatReplyDto;
import org.streamhub.api.v1.chat.dto.ChatSendRequest;
import org.streamhub.api.v1.chat.entity.ChatIntent;
import org.streamhub.api.v1.chat.entity.ChatMessage;
import org.streamhub.api.v1.chat.entity.ChatRole;
import org.streamhub.api.v1.chat.entity.ChatSession;
import org.streamhub.api.v1.chat.repository.ChatMessageRepository;
import org.streamhub.api.v1.chat.repository.ChatSessionRepository;
import org.streamhub.api.v1.chat.repository.ChatUnansweredRepository;

/**
 * Session ownership binding (C5 security): a session is owned by its server-issued secret token. A
 * caller can only continue a session or read its history by presenting the matching token; without
 * it the server issues a fresh session (never hijacks) and history returns nothing.
 */
class ChatServiceTest {

    private final ChatSessionRepository sessionRepo = mock(ChatSessionRepository.class);
    private final ChatMessageRepository messageRepo = mock(ChatMessageRepository.class);
    private final ChatUnansweredRepository unansweredRepo = mock(ChatUnansweredRepository.class);
    private final ChatProviderRouter router = mock(ChatProviderRouter.class);
    private final ChatProvider provider = mock(ChatProvider.class);

    private final ChatService service =
            new ChatService(sessionRepo, messageRepo, unansweredRepo, router);

    @BeforeEach
    void setUp() {
        when(router.resolve()).thenReturn(provider);
        when(provider.code()).thenReturn("RULE");
        when(provider.reply(any(), any())).thenReturn(ChatReply.of("안녕하세요", ChatIntent.FAQ));
        // save() returns the entity it was given (id stays null — fine for these assertions).
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepo.findBySessionIdOrderByCreatedAtAscIdAsc(any())).thenReturn(List.of());
    }

    @Test
    void send_newSession_issuesKeyAndSecretToken() {
        when(sessionRepo.findBySessionKey(any())).thenReturn(Optional.empty());

        ChatReplyDto reply = service.send(new ChatSendRequest(null, null, "안녕"));

        assertThat(reply.sessionKey()).isNotBlank();
        assertThat(reply.sessionToken()).isNotBlank();
    }

    @Test
    void send_correctToken_continuesSameSession() {
        ChatSession owned = ChatSession.builder()
                .sessionKey("sess-1").sessionToken("secret-1").provider("RULE").build();
        when(sessionRepo.findBySessionKey("sess-1")).thenReturn(Optional.of(owned));

        ChatReplyDto reply = service.send(new ChatSendRequest("sess-1", "secret-1", "이어서"));

        assertThat(reply.sessionKey()).isEqualTo("sess-1");
        assertThat(reply.sessionToken()).isEqualTo("secret-1");
    }

    @Test
    void send_wrongToken_doesNotHijack_issuesFreshSession() {
        ChatSession victim = ChatSession.builder()
                .sessionKey("victim").sessionToken("victim-secret").provider("RULE").build();
        when(sessionRepo.findBySessionKey("victim")).thenReturn(Optional.of(victim));

        ChatReplyDto reply = service.send(new ChatSendRequest("victim", "guessed-wrong", "침입"));

        // The attacker gets a brand-new session, never the victim's.
        assertThat(reply.sessionKey()).isNotEqualTo("victim");
        assertThat(reply.sessionToken()).isNotEqualTo("victim-secret");
    }

    @Test
    void history_returnsMessagesOnlyWithMatchingToken() {
        ChatSession owned = ChatSession.builder()
                .sessionKey("sess-1").sessionToken("secret-1").provider("RULE").build();
        when(sessionRepo.findBySessionKey("sess-1")).thenReturn(Optional.of(owned));
        when(messageRepo.findBySessionIdOrderByCreatedAtAscIdAsc(any())).thenReturn(List.of(
                ChatMessage.builder().sessionId(1L).role(ChatRole.USER).content("배송비?").build()));

        assertThat(service.history("sess-1", "secret-1")).hasSize(1);   // owner
        assertThat(service.history("sess-1", "wrong")).isEmpty();        // wrong token
        assertThat(service.history("sess-1", null)).isEmpty();           // no token
        assertThat(service.history("unknown", "secret-1")).isEmpty();    // unknown session
    }
}
