package org.streamhub.api.v1.community.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.community.entity.CommunityPost;

/**
 * A community post row. Used for both list and detail output. All values are demo/fictional
 * (no real PII — PII guard). Mutable to match the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class CommunityPostDto {
    private Long id;
    private Long boardId;
    private String category;
    private String title;
    private String content;
    private String writerName;
    private String secretYn;
    private int recommendCount;
    private int viewCount;
    private LocalDateTime createdAt;

    /** Builds a DTO from a persisted post. */
    public static CommunityPostDto from(CommunityPost post) {
        CommunityPostDto dto = new CommunityPostDto();
        dto.id = post.getId();
        dto.boardId = post.getBoardId();
        dto.category = post.getCategory();
        dto.title = post.getTitle();
        dto.content = post.getContent();
        dto.writerName = post.getWriterName();
        dto.secretYn = post.getSecretYn();
        dto.recommendCount = post.getRecommendCount();
        dto.viewCount = post.getViewCount();
        dto.createdAt = post.getCreatedAt();
        return dto;
    }
}
