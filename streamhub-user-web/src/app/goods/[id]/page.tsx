"use client";

import { useEffect, useState } from "react";
import { ShoppingBag } from "lucide-react";
import clsx from "clsx";
import { useGood } from "@/lib/goods";
import { ApiError } from "@/lib/api";
import { BackLink } from "@/components/BackLink";
import { EmptyState, ErrorState } from "@/components/States";

/** Image gallery: a hero image + selectable thumbnails when more than one image exists. */
function Gallery({ images, name }: { images: string[]; name: string }) {
  const [active, setActive] = useState(0);
  const [failed, setFailed] = useState<Record<number, boolean>>({});

  // Clamp the active index if the image list changes (e.g. navigating between items).
  useEffect(() => {
    setActive(0);
    setFailed({});
  }, [images]);

  const heroSrc = images[active];
  const heroFailed = failed[active];

  return (
    <div>
      <div className="mx-auto aspect-square w-full max-w-[280px] overflow-hidden rounded-card bg-gradient-to-br from-card to-surface">
        {heroSrc && !heroFailed ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={heroSrc}
            alt={name}
            onError={() => setFailed((f) => ({ ...f, [active]: true }))}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="grid h-full w-full place-items-center">
            <ShoppingBag className="h-14 w-14 text-inactive" />
          </div>
        )}
      </div>

      {images.length > 1 && (
        <div className="hrow mx-auto mt-3 max-w-[280px] justify-center" role="group" aria-label="이미지 선택">
          {images.map((src, i) => (
            <button
              key={i}
              onClick={() => setActive(i)}
              aria-label={`이미지 ${i + 1}`}
              aria-pressed={i === active}
              className={clsx(
                "h-14 w-14 shrink-0 overflow-hidden rounded-lg border-2 transition-colors",
                i === active ? "border-primary" : "border-border",
              )}
            >
              {!failed[i] ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={src}
                  alt={`${name} 썸네일 ${i + 1}`}
                  loading="lazy"
                  onError={() => setFailed((f) => ({ ...f, [i]: true }))}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="grid h-full w-full place-items-center bg-card">
                  <ShoppingBag className="h-5 w-5 text-inactive" />
                </div>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default function GoodsDetailPage({ params }: { params: { id: string } }) {
  const id = Number(params.id);
  const { data, isLoading, isError, error, refetch } = useGood(id);

  return (
    <div className="animate-fade-up">
      <div className="px-5 pt-4">
        <BackLink href="/goods" label="굿즈샵" />
      </div>

      {isLoading ? (
        <div className="px-5 pt-4">
          <div className="skeleton mx-auto aspect-square w-full max-w-[280px] rounded-card" />
          <div className="skeleton mx-auto mt-5 h-6 w-2/5 rounded" />
          <div className="skeleton mx-auto mt-3 h-4 w-1/4 rounded" />
        </div>
      ) : isError ? (
        <div className="pt-3">
          {(error as ApiError)?.status === 404 ? (
            <EmptyState message="굿즈를 찾을 수 없습니다." />
          ) : (
            <ErrorState message={(error as Error)?.message} onRetry={() => refetch()} />
          )}
        </div>
      ) : data ? (
        <article className="pt-3">
          <div className="px-5">
            <Gallery images={data.imageUrls} name={data.name} />

            <div className="mt-5 text-center">
              <h1 className="text-xl font-bold text-active">{data.name}</h1>
              <p className="mt-2 text-2xl font-extrabold tracking-tight text-primary">
                {data.price.toLocaleString()}
                <span className="ml-0.5 text-base font-bold">원</span>
              </p>
              {data.soldOut ? (
                <span className="mt-2 inline-block rounded-full bg-card px-2.5 py-0.5 text-[11px] font-bold text-inactive">
                  품절
                </span>
              ) : data.stock != null ? (
                <p className="mt-1.5 text-xs text-inactive">재고 {data.stock.toLocaleString()}개</p>
              ) : null}
            </div>

            {data.description && (
              <p className="mt-4 whitespace-pre-line text-sm leading-relaxed text-inactive">
                {data.description}
              </p>
            )}
          </div>

          {/* Goods checkout flow is out of scope for now — purchase is disabled. */}
          <div className="sticky bottom-0 z-40 mt-7 border-t border-border bg-bg/95 px-5 py-3 backdrop-blur">
            <button
              type="button"
              disabled
              aria-disabled="true"
              className="grid w-full cursor-not-allowed place-items-center rounded-xl border border-border bg-card py-3.5 text-[15px] font-bold text-inactive"
            >
              구매 준비중
            </button>
          </div>
        </article>
      ) : null}
    </div>
  );
}
