package org.streamhub.api.v1.sms.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.streamhub.api.v1.sms.entity.SmsMessage;

/** JPA repository for {@link SmsMessage} (CRUD). Listing/search uses MyBatis. */
public interface SmsMessageRepository extends JpaRepository<SmsMessage, Long> {

    List<SmsMessage> findByMemberId(Long memberId);

    List<SmsMessage> findByRefTypeAndRefId(String refType, String refId);
}
