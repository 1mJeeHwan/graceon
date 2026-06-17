package org.streamhub.api.v1.chat.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.chat.entity.ChatMessage;

/** JPA repository for {@link ChatMessage} (C5). */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);
}
