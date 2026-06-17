package org.streamhub.api.v1.sms.entity;

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
 * An SMS/LMS message log (C6). Modeled after {@code ActionLog} — the mock sender only
 * persists the row, never performs an external send ({@code testMode} always {@code Y}).
 */
@Entity
@Table(name = "SMS_MESSAGE", indexes = {
        @Index(name = "idx_sms_sent_at", columnList = "sent_at"),
        @Index(name = "idx_sms_kind", columnList = "kind"),
        @Index(name = "idx_sms_member", columnList = "member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmsMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Recipient number — masked on seed/input ({@code 010-1234-****}). */
    @Column(name = "to_number", nullable = false, length = 20)
    private String toNumber;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private SmsKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 8)
    private SmsChannel channel;

    /** Adapter used ({@code MOCK}/{@code ALIGO}/{@code SOLAPI}). */
    @Column(name = "sender", nullable = false, length = 20)
    private String sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private SmsStatus status;

    /** Always {@code Y} (demo). */
    @Column(name = "test_mode", nullable = false, length = 1)
    private String testMode;

    /** Associated member (if any). */
    @Column(name = "member_id")
    private Long memberId;

    /** {@code ORDER}/{@code DONATION}. */
    @Column(name = "ref_type", length = 30)
    private String refType;

    /** Associated domain PK. */
    @Column(name = "ref_id", length = 60)
    private String refId;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Builder
    private SmsMessage(String toNumber, String content, SmsKind kind, SmsChannel channel,
                       String sender, SmsStatus status, String testMode, Long memberId,
                       String refType, String refId, LocalDateTime sentAt) {
        this.toNumber = toNumber;
        this.content = content;
        this.kind = kind;
        this.channel = channel;
        this.sender = sender;
        this.status = status != null ? status : SmsStatus.SENT;
        this.testMode = testMode != null ? testMode : "Y";
        this.memberId = memberId;
        this.refType = refType;
        this.refId = refId;
        this.sentAt = sentAt != null ? sentAt : LocalDateTime.now();
    }
}
