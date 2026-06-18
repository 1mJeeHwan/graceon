package org.streamhub.api.v1.community.dto;

/**
 * Optional filters for the community post listing. {@code keyword} matches the title
 * (case-insensitive, contains); a null field means "no filter on that dimension".
 */
public record CommunityPostSearchRequest(Long boardId, String category, String keyword) {
}
