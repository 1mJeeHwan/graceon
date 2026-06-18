package org.streamhub.api.v1.community;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.community.dto.CommunityPostDto;
import org.streamhub.api.v1.community.dto.CommunityPostSearchRequest;
import org.streamhub.api.v1.community.entity.CommunityPost;
import org.streamhub.api.v1.community.repository.CommunityPostRepository;

/**
 * Community post management: filtered listing plus detail/delete. The demo dataset is small, so
 * the listing loads all posts and filters/sorts in memory (newest first) — no pagination needed.
 */
@Service
public class CommunityPostService {

    private final CommunityPostRepository postRepository;

    public CommunityPostService(CommunityPostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * Listing: optionally filtered by board, category, and title keyword; newest first. All
     * matching rows are returned (small demo dataset).
     */
    @Transactional(readOnly = true)
    public List<CommunityPostDto> list(CommunityPostSearchRequest request) {
        Long boardId = request != null ? request.boardId() : null;
        String category = request != null ? request.category() : null;
        String keyword = request != null && request.keyword() != null
                ? request.keyword().trim().toLowerCase() : null;
        return postRepository.findAll().stream()
                .filter(post -> boardId == null || boardId.equals(post.getBoardId()))
                .filter(post -> category == null || category.isBlank()
                        || category.equals(post.getCategory()))
                .filter(post -> keyword == null || keyword.isBlank()
                        || (post.getTitle() != null && post.getTitle().toLowerCase().contains(keyword)))
                .sorted(Comparator.comparing(CommunityPost::getCreatedAt).reversed()
                        .thenComparing(Comparator.comparing(CommunityPost::getId).reversed()))
                .map(CommunityPostDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CommunityPostDto detail(Long id) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        return CommunityPostDto.from(post);
    }

    @Transactional
    public void delete(Long id) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        postRepository.delete(post);
    }
}
