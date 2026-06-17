package org.streamhub.api.v1.donation;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.donation.dto.BillingCalendarItem;
import org.streamhub.api.v1.donation.dto.BillingCalendarRequest;
import org.streamhub.api.v1.donation.dto.DonationListItem;
import org.streamhub.api.v1.donation.dto.DonationSearchRequest;
import org.streamhub.api.v1.donation.dto.OnceDonationRequest;
import org.streamhub.api.v1.donation.entity.Donation;
import org.streamhub.api.v1.donation.entity.DonationStatus;
import org.streamhub.api.v1.donation.entity.DonationType;
import org.streamhub.api.v1.donation.mapper.DonationMapper;
import org.streamhub.api.v1.donation.repository.DonationRepository;
import org.streamhub.api.v1.member.repository.MemberRepository;
import org.streamhub.api.v1.sms.SmsService;

/**
 * Donation history listing (MyBatis joins/filters), manual one-off donation entry, and the
 * billing-schedule calendar aggregation. All donations are test mode.
 *
 * <p>A one-off donation triggers a best-effort receipt SMS via {@link SmsService} (mock — no
 * real dispatch); a notification failure never breaks the donation entry.
 */
@Slf4j
@Service
public class DonationService {

    /** One-off donations accrue 1% of the amount as grace points (policy constant). */
    private static final long ONCE_POINT_RATE_DIVISOR = 100L;

    private final DonationMapper donationMapper;
    private final DonationRepository donationRepository;
    private final MemberRepository memberRepository;
    private final PointLedgerWriter pointLedgerWriter;
    private final ActionLogPublisher actionLogPublisher;
    private final SmsService smsService;

    public DonationService(
            DonationMapper donationMapper,
            DonationRepository donationRepository,
            MemberRepository memberRepository,
            PointLedgerWriter pointLedgerWriter,
            ActionLogPublisher actionLogPublisher,
            SmsService smsService) {
        this.donationMapper = donationMapper;
        this.donationRepository = donationRepository;
        this.memberRepository = memberRepository;
        this.pointLedgerWriter = pointLedgerWriter;
        this.actionLogPublisher = actionLogPublisher;
        this.smsService = smsService;
    }

    @Transactional(readOnly = true)
    public ResInfinityList<DonationListItem> list(DonationSearchRequest request) {
        String type = request.type() == null ? null : request.type().name();
        String status = request.status() == null ? null : request.status().name();
        String keyword = blankToNull(request.keyword());
        int size = request.pageSizeOrDefault();

        List<DonationListItem> contents = donationMapper.selectList(
                keyword, type, status, request.from(), request.to(), request.offset(), size);
        long total = donationMapper.countList(keyword, type, status, request.from(), request.to());
        return ResInfinityList.of(contents, total, size);
    }

    /** Records a manual one-off donation, accrues points, and returns the joined list row. */
    @Transactional
    public DonationListItem createOnce(OnceDonationRequest request) {
        if (!memberRepository.existsById(request.memberId())) {
            throw new ApiException(ResultCode.NOT_FOUND);
        }
        long point = request.amount() / ONCE_POINT_RATE_DIVISOR;
        Donation saved = donationRepository.save(Donation.builder()
                .memberId(request.memberId())
                .subscriptionId(null)
                .type(DonationType.ONCE)
                .amount(request.amount())
                .cycleNo(null)
                .status(DonationStatus.PAID)
                .pointAwarded(point)
                .testMode("Y")
                .paidAt(LocalDateTime.now())
                .build());

        pointLedgerWriter.append(request.memberId(), point, "단건 후원 적립", saved.getId());

        actionLogPublisher.publish("DONATION_ONCE", "DONATION",
                String.valueOf(saved.getId()), "₩" + request.amount());
        try {
            smsService.sendForDonation(request.memberId(), saved.getId(), request.amount());
        } catch (RuntimeException e) {
            log.warn("Failed to send donation SMS for {}: {}", saved.getId(), e.getMessage());
        }
        return donationMapper.selectDetail(saved.getId());
    }

    @Transactional(readOnly = true)
    public List<BillingCalendarItem> billingCalendar(BillingCalendarRequest request) {
        return donationMapper.billingCalendar(request.from(), request.to());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
