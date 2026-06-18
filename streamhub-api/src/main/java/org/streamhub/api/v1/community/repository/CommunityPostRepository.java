package org.streamhub.api.v1.community.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.community.entity.CommunityPost;

/** JPA repository for {@link CommunityPost} (community posts). */
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    List<CommunityPost> findByBoardId(Long boardId);
}
