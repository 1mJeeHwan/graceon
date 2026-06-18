package org.streamhub.api.v1.pub.order;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.album.entity.Album;
import org.streamhub.api.v1.album.entity.AlbumStatus;
import org.streamhub.api.v1.album.repository.AlbumRepository;
import org.streamhub.api.v1.goods.entity.GoodsItem;
import org.streamhub.api.v1.goods.repository.GoodsItemRepository;
import org.streamhub.api.v1.member.entity.Member;
import org.streamhub.api.v1.member.repository.MemberRepository;
import org.streamhub.api.v1.order.entity.Order;
import org.streamhub.api.v1.order.entity.OrderItem;
import org.streamhub.api.v1.order.entity.OrderReceipt;
import org.streamhub.api.v1.order.entity.OrderStatus;
import org.streamhub.api.v1.order.entity.ReceiptKind;
import org.streamhub.api.v1.order.repository.OrderItemRepository;
import org.streamhub.api.v1.order.repository.OrderReceiptRepository;
import org.streamhub.api.v1.order.repository.OrderRepository;
import org.streamhub.api.v1.payment.PaymentService;
import org.streamhub.api.v1.payment.dto.PayApproveCommand;
import org.streamhub.api.v1.payment.dto.PayRequestCommand;
import org.streamhub.api.v1.payment.dto.PaymentResultDto;
import org.streamhub.api.v1.pub.order.dto.MemberOrderCreateRequest;
import org.streamhub.api.v1.pub.order.dto.MemberOrderListItem;
import org.streamhub.api.v1.pub.order.dto.MemberOrderResult;

/**
 * Public (member-authenticated) album purchase. Creates a real {@code ORDERS} row + line item from
 * an on-sale album's bridge {@code GOODS_ITEM}, then drives it to {@code PAID} through the existing
 * mock {@link PaymentService} (request → approve) — reusing the order state machine for stock
 * deduction and the PAY receipt. No new state machine is introduced; the resulting order is visible
 * in the admin order list. All approvals are demo/test (실 PG 미연동).
 */
@Service
public class MemberOrderService {

    private static final DateTimeFormatter ORDER_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int ORDER_NO_RETRIES = 8;
    private static final String DEFAULT_PROVIDER = "TOSS";
    private static final String PAY_METHOD = "CARD";

    private final AlbumRepository albumRepository;
    private final GoodsItemRepository goodsItemRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderReceiptRepository orderReceiptRepository;
    private final PaymentService paymentService;
    private final SecureRandom random = new SecureRandom();

    public MemberOrderService(
            AlbumRepository albumRepository,
            GoodsItemRepository goodsItemRepository,
            MemberRepository memberRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderReceiptRepository orderReceiptRepository,
            PaymentService paymentService) {
        this.albumRepository = albumRepository;
        this.goodsItemRepository = goodsItemRepository;
        this.memberRepository = memberRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderReceiptRepository = orderReceiptRepository;
        this.paymentService = paymentService;
    }

    /**
     * Purchases an on-sale album as the given member: creates the order + line item, then drives the
     * mock payment (request → approve) so the order ends {@code PAID} with stock deducted and a PAY
     * receipt written — all reusing the existing order/payment domain.
     *
     * @throws ApiException {@code NOT_FOUND} if the album is missing or not on sale,
     *                      {@code INVALID_PARAMETER} if the album is not purchasable (no bridge goods)
     */
    @Transactional
    public MemberOrderResult purchase(Long memberId, MemberOrderCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ResultCode.UNAUTHORIZED));

        Album album = albumRepository.findById(request.albumId())
                .orElseThrow(() -> new ApiException(ResultCode.NOT_FOUND));
        if (album.getStatus() != AlbumStatus.ON_SALE) {
            throw new ApiException(ResultCode.NOT_FOUND);
        }
        if (album.getGoodsItemId() == null) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "구매할 수 없는 앨범입니다");
        }

        GoodsItem goods = goodsItemRepository.findById(album.getGoodsItemId())
                .orElseThrow(() -> new ApiException(ResultCode.INVALID_PARAMETER, "구매할 수 없는 앨범입니다"));
        long price = goods.getPrice();

        Order order = createOrder(member, price);
        orderItemRepository.save(OrderItem.builder()
                .orderId(order.getId())
                .goodsId(goods.getId())
                .goodsName(album.getTitle())
                .optionName(null)
                .unitPrice(price)
                .qty(1)
                .lineTotal(price)
                .build());

        approvePayment(order.getId(), resolveProvider(request.payProvider()));

        Order paid = orderRepository.findById(order.getId()).orElseThrow();
        return new MemberOrderResult(
                paid.getOrderNo(), paid.getStatus(), paid.getTotal(),
                paidAt(order.getId()), true);
    }

    /** A member's own purchase history, newest first. */
    @Transactional(readOnly = true)
    public List<MemberOrderListItem> myOrders(Long memberId) {
        return orderRepository.findByMemberIdOrderByOrderedAtDescIdDesc(memberId).stream()
                .map(order -> new MemberOrderListItem(
                        order.getOrderNo(),
                        firstProductName(order.getId()),
                        order.getTotal(),
                        order.getStatus(),
                        order.getOrderedAt()))
                .toList();
    }

    // --- helpers -----------------------------------------------------------

    private Order createOrder(Member member, long price) {
        Order order = Order.builder()
                .orderNo(nextOrderNo())
                .memberId(member.getId())
                .status(OrderStatus.PLACED)
                .orderedName(member.getName())
                .orderedPhone(member.getPhone())
                .receiverName(member.getName())
                .receiverPhone(member.getPhone())
                .goodsTotal(price)
                .shipFee(0L)
                .couponDiscount(0L)
                .pointUsed(0L)
                .total(price)
                .payMethod(PAY_METHOD)
                .orderedAt(LocalDateTime.now())
                .build();
        return orderRepository.save(order);
    }

    /** Drives the existing mock payment: request → approve (PLACED → PAID, stock + PAY receipt). */
    private void approvePayment(Long orderId, String provider) {
        PaymentResultDto requested = paymentService.request(new PayRequestCommand(orderId, provider));
        paymentService.approve(new PayApproveCommand(orderId, requested.txnId(), null));
    }

    /** Generates a unique {@code YYYYMMDD-XXXXXX} order number, retrying on collision. */
    private String nextOrderNo() {
        String day = LocalDateTime.now().format(ORDER_DAY);
        for (int attempt = 0; attempt < ORDER_NO_RETRIES; attempt++) {
            String candidate = day + "-" + String.format("%06d", random.nextInt(1_000_000));
            if (!orderRepository.existsByOrderNo(candidate)) {
                return candidate;
            }
        }
        throw new ApiException(ResultCode.INTERNAL_ERROR, "주문번호 발급에 실패했습니다");
    }

    private String resolveProvider(String requested) {
        return requested == null || requested.isBlank() ? DEFAULT_PROVIDER : requested.toUpperCase();
    }

    private LocalDateTime paidAt(Long orderId) {
        List<OrderReceipt> receipts = orderReceiptRepository.findByOrderIdOrderByCreatedAtAscIdAsc(orderId);
        LocalDateTime latest = null;
        for (OrderReceipt receipt : receipts) {
            if (receipt.getKind() == ReceiptKind.PAY) {
                latest = receipt.getCreatedAt();
            }
        }
        return latest;
    }

    private String firstProductName(Long orderId) {
        return orderItemRepository.findByOrderId(orderId).stream()
                .findFirst()
                .map(OrderItem::getGoodsName)
                .orElse(null);
    }
}
