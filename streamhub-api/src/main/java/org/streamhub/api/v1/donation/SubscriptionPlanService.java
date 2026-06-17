package org.streamhub.api.v1.donation;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.donation.dto.PlanCreateRequest;
import org.streamhub.api.v1.donation.dto.PlanResponse;
import org.streamhub.api.v1.donation.entity.SubscriptionPlan;
import org.streamhub.api.v1.donation.entity.SubscriptionStatus;
import org.streamhub.api.v1.donation.repository.SubscriptionPlanRepository;
import org.streamhub.api.v1.donation.repository.SubscriptionRepository;

/**
 * Membership-plan CRUD. Each response carries the live count of active subscriptions on the plan.
 */
@Service
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ActionLogPublisher actionLogPublisher;

    public SubscriptionPlanService(
            SubscriptionPlanRepository planRepository,
            SubscriptionRepository subscriptionRepository,
            ActionLogPublisher actionLogPublisher) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.actionLogPublisher = actionLogPublisher;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> list() {
        return planRepository.findAllByOrderByPriceAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse getDetail(Long id) {
        return toResponse(findPlan(id));
    }

    @Transactional
    public PlanResponse create(PlanCreateRequest request) {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.name())
                .grade(request.grade())
                .price(request.price())
                .periodMonths(request.periodMonths())
                .pointRate(request.pointRate())
                .benefit(request.benefit())
                .active(request.active())
                .build();
        SubscriptionPlan saved = planRepository.save(plan);
        actionLogPublisher.publish("PLAN_CREATE", "SUBSCRIPTION_PLAN",
                String.valueOf(saved.getId()), request.name());
        return toResponse(saved);
    }

    @Transactional
    public PlanResponse update(Long id, PlanCreateRequest request) {
        SubscriptionPlan plan = findPlan(id);
        plan.update(request.name(), request.grade(), request.price(), request.periodMonths(),
                request.pointRate(), request.benefit(), request.active());
        planRepository.saveAndFlush(plan);
        actionLogPublisher.publish("PLAN_UPDATE", "SUBSCRIPTION_PLAN",
                String.valueOf(id), request.name());
        return toResponse(plan);
    }

    @Transactional
    public void delete(Long id) {
        SubscriptionPlan plan = findPlan(id);
        planRepository.delete(plan);
        actionLogPublisher.publish("PLAN_DELETE", "SUBSCRIPTION_PLAN",
                String.valueOf(id), plan.getName());
    }

    private SubscriptionPlan findPlan(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
    }

    private PlanResponse toResponse(SubscriptionPlan plan) {
        long activeCount =
                subscriptionRepository.countByPlanIdAndStatus(plan.getId(), SubscriptionStatus.ACTIVE);
        return PlanResponse.of(plan, activeCount);
    }
}
