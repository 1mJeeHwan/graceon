package org.streamhub.api.v1.chat.admin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.chat.entity.ChatSession;

/**
 * Admin-side JPA repository for {@link ChatSession} (C5). Kept separate from the public
 * {@code ChatSessionRepository} so admin queries can evolve without touching the widget path.
 */
public interface ChatSessionAdminRepository extends JpaRepository<ChatSession, Long> {

    /** All sessions, newest first by id. */
    List<ChatSession> findAllByOrderByIdDesc();
}
