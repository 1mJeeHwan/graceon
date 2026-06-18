package org.streamhub.api.v1.community.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.community.entity.Board;

/**
 * A community board row. Used as both the admin create/update input and the list output. All
 * values are demo/fictional (PII guard). Mutable to match the project DTO style.
 */
@Getter
@Setter
@NoArgsConstructor
public class BoardDto {
    private Long id;
    private String code;
    private String name;
    private int readLevel;
    private int writeLevel;
    private String useYn;
    private int sortOrder;
    private LocalDateTime createdAt;

    /** Builds a DTO from a persisted board. */
    public static BoardDto from(Board board) {
        BoardDto dto = new BoardDto();
        dto.id = board.getId();
        dto.code = board.getCode();
        dto.name = board.getName();
        dto.readLevel = board.getReadLevel();
        dto.writeLevel = board.getWriteLevel();
        dto.useYn = board.getUseYn();
        dto.sortOrder = board.getSortOrder();
        dto.createdAt = board.getCreatedAt();
        return dto;
    }
}
