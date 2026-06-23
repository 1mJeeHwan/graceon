"use client";

/** Iamport(포트원) JS SDK 로딩 + 본인인증 Promise 래퍼. ng-front utils/iamport.ts와 동일. */

declare global {
  interface Window {
    IMP?: {
      init: (impCode: string) => void;
      certification: (
        params: IamportCertificationParams,
        callback: (res: IamportCertificationResult) => void,
      ) => void;
    };
  }
}

export type IamportCertificationParams = {
  merchant_uid: string;
  pg?: string;
  name?: string;
  phone?: string;
  popup?: boolean;
  m_redirect_url?: string;
};

export type IamportCertificationResult = {
  success: boolean;
  imp_uid: string;
  merchant_uid: string;
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
      reject(new Error("본인인증 모듈을 불러오지 못했습니다."));
    };
    document.head.appendChild(script);
  });
  return sdkLoadingPromise;
}

/** Loads the SDK, runs IMP.certification, and resolves with the popup result. */
export async function runCertification(
  impCode: string,
  params: IamportCertificationParams,
): Promise<IamportCertificationResult> {
  await loadIamportSdk();
  if (!window.IMP) throw new Error("본인인증 모듈을 사용할 수 없습니다.");
  window.IMP.init(impCode);
  return new Promise<IamportCertificationResult>((resolve) => {
    window.IMP!.certification(params, (res) => resolve(res));
  });
}
