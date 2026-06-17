package org.streamhub.api.v1.sms.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.streamhub.api.v1.sms.entity.SmsChannel;
import org.streamhub.api.v1.sms.entity.SmsKind;
import org.streamhub.api.v1.sms.entity.SmsMessage;
import org.streamhub.api.v1.sms.entity.SmsStatus;

/** One row of the SMS history list (C6). Built from MyBatis rows or directly from an entity. */
@Getter
@Setter
@NoArgsConstructor
public class SmsListItem {
    private Long id;
    private String toNumber; // masked
    private String content;
    private SmsKind kind;
    private SmsChannel channel;
    private String sender;
    private SmsStatus status;
    private String testMode;
    private Long memberId;
    private String memberName;
    private String refType;
    private String refId;
    private LocalDateTime sentAt;

    private SmsListItem(SmsMessage entity) {
        this.id = entity.getId();
        this.toNumber = entity.getToNumber();
        this.content = entity.getContent();
        this.kind = entity.getKind();
        this.channel = entity.getChannel();
        this.sender = entity.getSender();
        this.status = entity.getStatus();
        this.testMode = entity.getTestMode();
        this.memberId = entity.getMemberId();
        this.refType = entity.getRefType();
        this.refId = entity.getRefId();
        this.sentAt = entity.getSentAt();
    }

    /** Maps a freshly persisted {@link SmsMessage} to the list DTO (member name omitted). */
    public static SmsListItem from(SmsMessage entity) {
        return new SmsListItem(entity);
    }
}
