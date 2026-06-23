package org.streamhub.api.v1.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * A managed end-user (member of a church). Distinct from {@code AdminAccount}
 * (the operators who manage these members).
 */
@Entity
@Table(name = "MEMBER", indexes = {
        @Index(name = "idx_member_church", columnList = "church_id"),
        @Index(name = "idx_member_status", columnList = "user_status"),
        @Index(name = "idx_member_created", columnList = "created_at"),
        @Index(name = "idx_member_grade", columnList = "grade")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "church_id", nullable = false)
    private Long churchId;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 20)
    private UserStatus userStatus;

    /** Whether the member is permitted to watch live broadcasts. */
    @Column(name = "live_yn", nullable = false, length = 1)
    private String liveYn;

    /** Membership grade (benefit tier). Defaults to {@link MemberGrade#BRONZE}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 20)
    private MemberGrade grade;

    /** Cached cumulative grace-point balance; the point ledger is the source of truth. */
    @Column(name = "point_balance", nullable = false)
    private long pointBalance;

    /**
     * Marketing-communication consent (선택 약관). Nullable so the column can be added to an
     * existing live table without a NOT NULL backfill; null on legacy rows means "not asked".
     */
    @Column(name = "marketing_agreed")
    private Boolean marketingAgreed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Member(Long churchId, String email, String password, String name,
                   String phone, UserStatus userStatus, String liveYn,
                   MemberGrade grade, Long pointBalance, Boolean marketingAgreed,
                   LocalDateTime createdAt) {
        this.churchId = churchId;
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.userStatus = userStatus;
        this.liveYn = liveYn;
        this.grade = grade != null ? grade : MemberGrade.BRONZE;
        this.pointBalance = pointBalance != null ? pointBalance : 0L;
        this.marketingAgreed = marketingAgreed;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** Updates editable profile fields. */
    public void updateProfile(String name, String phone, String liveYn) {
        this.name = name;
        this.phone = phone;
        this.liveYn = liveYn;
        this.updatedAt = LocalDateTime.now();
    }

    /** Transitions the member's lifecycle status. */
    public void changeStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Adjusts the cached point balance by {@code delta} (negative deducts).
     * The balance may not go below zero.
     *
     * @throws IllegalStateException if the resulting balance would be negative
     */
    public void addPoint(long delta) {
        long next = this.pointBalance + delta;
        if (next < 0) {
            throw new IllegalStateException("point balance cannot go negative");
        }
        this.pointBalance = next;
        this.updatedAt = LocalDateTime.now();
    }

    /** Changes the membership grade. */
    public void changeGrade(MemberGrade grade) {
        this.grade = grade;
        this.updatedAt = LocalDateTime.now();
    }
}
