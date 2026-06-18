package org.streamhub.api.v1.visit.dto;

/**
 * A path and its visit count, used for the top-paths breakdown.
 *
 * @param path  the requested front-site path
 * @param count number of visits to that path
 */
public record PathCountDto(String path, long count) {
}
