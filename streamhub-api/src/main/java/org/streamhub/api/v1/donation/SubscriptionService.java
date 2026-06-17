package org.streamhub.api.v1.donation;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResInfinityList;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.donation.dto.SubscriptionDetail;
import org.streamhub.api.v1.donation.dto.SubscriptionListItem;
import org.streamhub.api.v1.donation.dto.SubscriptionSearchRequest;
import org.streamhub.api.v1.donation.dto.SubscriptionStatusRequest;
import org.streamhub.api.v1.donation.entity.Subscription;
import org.streamhub.api.v1.donation.entity.SubscriptionPlan;
import org.streamhub.api.v1.donation.mapper.SubscriptionMapper;
import org.streamhub.api.v1.donation.repository.SubscriptionPlanRepository;
import org.streamhub.api.v1.donation.repository.SubscriptionRepository;

/**
 * Subscription listing/detail (MyBatis joins) and lifecycle transitions. The state machine itself
 * lives on the {@link Subscription} entity; this service translates illegal transitions into
 * {@link ApiException}.
 */
@Service
public class SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final ActionLogPublisher actionLogPublisher;

    public SubscriptionService(
            SubscriptionMapper subscriptionMapper,
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            ActionLogPublisher actionLogPublisher) {
        this.subscriptionMapper = subscriptionMapper;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.actionLogPublisher = actionLogPublisher;
    }

    @Transactional(readOnly = true)
    public ResInfinityList<SubscriptionListItem> list(SubscriptionSearchRequest request) {
        String status = request.status() == null ? null : request.status().name();
        String keyword = blankToNull(request.keyword());
        int size = request.pageSizeOrDefault();

        var contents = subscriptionMapper.selectList(keyword, status, request.planId(), request.offset(), size);
        long total = subscriptionMapper.countList(keyword, status, request.planId());
        return ResInfinityList.of(contents, total, size);
    }

    @Transactional(readOnly = true)
    public SubscriptionDetail getDetail(Long id) {
        SubscriptionDetail detail = subscriptionMapper.selectDetail(id);
        if (detail == null) {
            throw new ApiException(ResultCode.NOT_FOUND);
        }
        return detail;
    }

    /** Applies a lifecycle transition (pause/resume/cancel) enforced by the entity state machine. */
    @Transactional
    public SubscriptionDetail changeStatus(Long id, SubscriptionStatusRequest request) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        try {
            switch (request.status()) {
                case PAUSED -> subscription.pause();
                case CANCELED -> subscription.cancel();
                case ACTIVE -> subscription.resume(nextBillingFrom(subscription));
            }
        } catch (IllegalStateException e) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "허용되지 않는 상태 전이입니다");
        }
        subscriptionRepository.saveAndFlush(subscription);
        actionLogPublisher.publish("SUBSCRIPTION_STATUS", "SUBSCRIPTION",
                String.valueOf(id), request.status().name());
        return getDetail(id);
    }

    /** Next billing date on resume: today + the plan's billing period. */
    private LocalDateTime nextBillingFrom(Subscription subscription) {
        SubscriptionPlan plan = planRepository.findById(subscription.getPlanId())
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        return LocalDateTime.now().plusMonths(plan.getPeriodMonths());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
