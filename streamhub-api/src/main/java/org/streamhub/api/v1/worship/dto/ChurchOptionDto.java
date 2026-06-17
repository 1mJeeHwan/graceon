package org.streamhub.api.v1.worship.dto;

import org.streamhub.api.v1.member.entity.Church;

/**
 * Church option for the public registration form's church selector (open churches only).
 *
 * @param id   church id
 * @param name church name
 */
public record ChurchOptionDto(Long id, String name) {

    public static ChurchOptionDto from(Church church) {
        return new ChurchOptionDto(church.getId(), church.getName());
    }
}
