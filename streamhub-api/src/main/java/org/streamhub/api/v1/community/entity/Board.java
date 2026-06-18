package org.streamhub.api.v1.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A community board (게시판) such as 공지사항 or 자유게시판. {@code readLevel}/{@code writeLevel}
 * gate visibility and posting by member level (1..10). All values are demo/fictional (PII guard).
 */
@Entity
@Table(name = "BOARD", indexes = {
        @Index(name = "idx_board_code", columnList = "code"),
        @Index(name = "idx_board_use", columnList = "use_yn")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** Minimum member level (1..10) required to read. */
    @Column(name = "read_level", nullable = false)
    private int readLevel;

    /** Minimum member level (1..10) required to write. */
    @Column(name = "write_level", nullable = false)
    private int writeLevel;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Board(String code, String name, int readLevel, int writeLevel, String useYn,
                  int sortOrder, LocalDateTime createdAt) {
        this.code = code;
        this.name = name;
        this.readLevel = readLevel;
        this.writeLevel = writeLevel;
        this.useYn = useYn != null ? useYn : "Y";
        this.sortOrder = sortOrder;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /** Updates editable fields. */
    public void update(String code, String name, int readLevel, int writeLevel, String useYn,
                       int sortOrder) {
        this.code = code;
        this.name = name;
        this.readLevel = readLevel;
        this.writeLevel = writeLevel;
        this.useYn = useYn;
        this.sortOrder = sortOrder;
    }
}
