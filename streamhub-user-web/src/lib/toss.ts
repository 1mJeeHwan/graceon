"use client";

// Toss Payments v2 "standard" SDK loader (CDN, no npm dependency — same approach as the Leaflet
// map). The script exposes a global `TossPayments(clientKey)` factory. We load it once and cache
// the promise so repeated checkouts reuse the same script tag.
// @docs https://docs.tosspayments.com/sdk/v2/js

const SDK_URL = "https://js.tosspayments.com/v2/standard";

/** Amount payload for requestPayment (v2). */
interface TossAmount {
  currency: "KRW";
  value: number;
}

/** Subset of payment.requestPayment params we use (card flow). */
interface TossRequestPaymentParams {
  method: "CARD";
  amount: TossAmount;
  orderId: string;
  orderName: string;
  successUrl: string;
  failUrl: string;
  customerName?: string;
  customerEmail?: string;
}

interface TossPaymentInstance {
  requestPayment(params: TossRequestPaymentParams): Promise<void>;
}

interface TossPaymentsInstance {
  payment(opts: { customerKey: string }): TossPaymentInstance;
}

type TossPaymentsFactory = (clientKey: string) => TossPaymentsInstance;

declare global {
  interface Window {
    TossPayments?: TossPaymentsFactory;
  }
}

let loadingPromise: Promise<TossPaymentsFactory> | null = null;

/** Loads the Toss v2 SDK and resolves with the global `TossPayments` factory. */
export function loadTossPayments(): Promise<TossPaymentsFactory> {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("Toss SDK는 브라우저에서만 로드할 수 있습니다."));
  }
  if (window.TossPayments) {
    return Promise.resolve(window.TossPayments);
  }
  if (loadingPromise) {
    return loadingPromise;
  }

  loadingPromise = new Promise<TossPaymentsFactory>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = SDK_URL;
    script.async = true;
    script.onload = () => {
      if (window.TossPayments) {
        resolve(window.TossPayments);
      } else {
        loadingPromise = null;
        reject(new Error("Toss SDK 로드에 실패했습니다."));
      }
    };
    script.onerror = () => {
      loadingPromise = null;
      reject(new Error("Toss SDK를 불러올 수 없습니다(네트워크)."));
    };
    document.head.appendChild(script);
  });
  return loadingPromise;
}
