package org.streamhub.api.v1.chat.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.chat.entity.ChatSession;

/** JPA repository for {@link ChatSession} (C5). */
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionKey(String sessionKey);
}
