// Album order domain — authenticated member surface for the user site.
// Mirrors the backend public order DTOs (POST/GET /pub/v1/orders). Reuses the shared
// request helper; all calls require a member Bearer token (401 when missing/anonymous).

"use client";

import { useQuery } from "@tanstack/react-query";
import { query, request } from "./api";
import type { InfinityList } from "./types";

/** Payment providers accepted by the order endpoint. Matches the checkout method buttons. */
export type PayProvider = "TOSS" | "KAKAO" | "PAYPAL" | "CARD";

/** Lifecycle status of an order — mirrors org.streamhub.api.v1.order.entity.OrderStatus. */
export type OrderStatus =
  | "PLACED"
  | "PAID"
  | "READY"
  | "SHIPPING"
  | "DONE"
  | "CANCEL"
  | "RETURN";

/** Body of POST /pub/v1/orders. */
export interface CreateOrderInput {
  albumId: number;
  payProvider: PayProvider;
  /** Optional discount-coupon code redeemed server-side against this order. */
  couponCode?: string;
}

/** Result of POST /pub/v1/orders — a freshly created order. */
export interface OrderResult {
  orderNo: string;
  status: OrderStatus;
  total: number;
  paidAt: string | null;
}

/** Body of POST /pub/v1/orders/prepare (phase 1 of a real-PG purchase). */
export interface PreparePaymentInput {
  albumId: number;
  provider: PayProvider;
  /** Optional discount-coupon code redeemed server-side; the returned amount is post-discount. */
  couponCode?: string;
}

/** Result of /prepare — everything the browser needs to open the PG payment window. */
export interface PreparePaymentResult {
  orderNo: string;
  orderName: string;
  amount: number;
  provider: PayProvider;
  /** PG client (publishable) key for the browser SDK (Toss); empty for redirect PGs. */
  clientKey: string;
  /** Member-scoped customer key for the window. */
  customerKey: string;
  /** Redirect URL for server-initiated PGs (Kakao/PayPal); absent for client-SDK PGs (Toss). */
  redirectUrl?: string;
}

/** Body of POST /pub/v1/orders/confirm — the values the PG window redirects back with. */
export interface ConfirmPaymentInput {
  orderNo: string;
  paymentKey: string;
  amount: number;
}

/** One scan event in a shipment's progress (GET /pub/v1/orders/{orderNo}/tracking). */
export interface TrackingEvent {
  time: string | null;
  location: string | null;
  description: string | null;
}

/** Live shipment status for an order, from the courier API via the backend delivery seam. */
export interface Tracking {
  carrierCode: string | null;
  carrierName: string | null;
  invoiceNo: string | null;
  level: number;
  completed: boolean;
  senderName: string | null;
  receiverName: string | null;
  events: TrackingEvent[];
}

/** One row of GET /pub/v1/orders (my order history). Mirrors MemberOrderListItem. */
export interface OrderListItem {
  orderNo: string;
  /** Name of the first product in the order, e.g. "찬양 1집". */
  productName: string;
  total: number;
  status: OrderStatus;
  orderedAt: string;
}

/** One ordered line on a receipt (MemberOrderReceipt.Line). */
export interface ReceiptLine {
  goodsName: string;
  optionName: string | null;
  unitPrice: number;
  qty: number;
  lineTotal: number;
}

/** Full receipt detail for one of the member's orders (GET /pub/v1/orders/{orderNo}). */
export interface OrderReceipt {
  orderNo: string;
  status: OrderStatus;
  orderedName: string | null;
  orderedAt: string;
  paidAt: string | null;
  items: ReceiptLine[];
  goodsTotal: number;
  shipFee: number;
  couponDiscount: number;
  pointUsed: number;
  total: number;
  payMethod: string | null;
  payProvider: string | null;
  payStatus: string | null;
  txnId: string | null;
  receiverName: string | null;
  receiverPhone: string | null;
  receiverAddr: string | null;
  trackingNo: string | null;
  shipCompany: string | null;
}

/** Korean labels for each order status, used in the history list and receipt. */
export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PLACED: "주문접수",
  PAID: "결제완료",
  READY: "배송준비",
  SHIPPING: "배송중",
  DONE: "배송완료",
  CANCEL: "취소됨",
  RETURN: "반품",
};

export const orderApi = {
  /** One-shot mock purchase: creates an order and approves it server-side (no PG window). */
  create: (input: CreateOrderInput, token: string) =>
    request<OrderResult>("/pub/v1/orders", { method: "POST", body: input, token }),
  /** Real-PG phase 1: create the order and get the payment-window parameters (clientKey, amount…). */
  prepare: (input: PreparePaymentInput, token: string) =>
    request<PreparePaymentResult>("/pub/v1/orders/prepare", { method: "POST", body: input, token }),
  /** Real-PG phase 2: confirm with the key the window redirected back with (calls the live PG). */
  confirm: (input: ConfirmPaymentInput, token: string) =>
    request<OrderResult>("/pub/v1/orders/confirm", { method: "POST", body: input, token }),
  /** A page of the signed-in member's orders, newest first. */
  list: (token: string, pageNumber: number, pageSize: number) =>
    request<InfinityList<OrderListItem>>(
      `/pub/v1/orders${query({ pageNumber, pageSize })}`,
      { token },
    ),
  /** Full receipt detail for one of the member's own orders (member-scoped server-side). */
  receipt: (orderNo: string, token: string) =>
    request<OrderReceipt>(`/pub/v1/orders/${orderNo}`, { token }),
  /** Live delivery tracking for one of the member's orders (calls the courier API). */
  tracking: (orderNo: string, token: string) =>
    request<Tracking>(`/pub/v1/orders/${orderNo}/tracking`, { token }),
};

export const orderKeys = {
  list: (pageNumber: number) => ["orders", pageNumber] as const,
  receipt: (orderNo: string) => ["order-receipt", orderNo] as const,
};

/** A page of my order history — enabled only when a member token is present. */
export function useMyOrders(token: string | null, pageNumber: number, pageSize: number) {
  return useQuery({
    queryKey: orderKeys.list(pageNumber),
    queryFn: () => orderApi.list(token as string, pageNumber, pageSize),
    enabled: token != null,
    placeholderData: (prev) => prev, // no flicker between pages
  });
}

/** Receipt detail for one order — fetched lazily when the receipt modal opens. */
export function useOrderReceipt(orderNo: string | null, token: string | null) {
  return useQuery({
    queryKey: orderKeys.receipt(orderNo ?? ""),
    queryFn: () => orderApi.receipt(orderNo as string, token as string),
    enabled: orderNo != null && token != null,
  });
}
