"use client";

/** Iamport(포트원) JS SDK 로딩 + 결제 Promise 래퍼. ng-api와 동일한 IMP.request_pay 흐름. */

declare global {
  interface Window {
    IMP?: {
      init: (impCode: string) => void;
      request_pay: (
        params: IamportPayParams,
        callback: (res: IamportPayResult) => void,
      ) => void;
    };
  }
}

export type IamportPayParams = {
  /** PG 채널(예: "html5_inicis"). 미지정 시 가맹점 기본 채널 사용. */
  pg?: string;
  pay_method: string;
  merchant_uid: string;
  name: string;
  amount: number;
  buyer_name?: string;
  /** 모바일에서 결제 후 돌아올 URL. */
  m_redirect_url?: string;
};

export type IamportPayResult = {
  success: boolean;
  imp_uid: string;
  merchant_uid: string;
  paid_amount?: number;
  error_code?: string;
  error_msg?: string;
};

const SDK_URL = "https://cdn.iamport.kr/v1/iamport.js";
let sdkLoadingPromise: Promise<void> | null = null;

function loadIamportSdk(): Promise<void> {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("Iamport SDK can only be loaded in browser"));
  }
  if (window.IMP) return Promise.resolve();
  if (sdkLoadingPromise) return sdkLoadingPromise;

  sdkLoadingPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = SDK_URL;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => {
      sdkLoadingPromise = null;
      reject(new Error("결제 모듈을 불러오지 못했습니다."));
    };
    document.head.appendChild(script);
  });
  return sdkLoadingPromise;
}

/** Loads the SDK, runs IMP.request_pay, and resolves with the payment popup result. */
export async function runPayment(
  impCode: string,
  params: IamportPayParams,
): Promise<IamportPayResult> {
  await loadIamportSdk();
  if (!window.IMP) throw new Error("결제 모듈을 사용할 수 없습니다.");
  window.IMP.init(impCode);
  return new Promise<IamportPayResult>((resolve) => {
    window.IMP!.request_pay(params, (res) => resolve(res));
  });
}
