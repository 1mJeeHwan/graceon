"use client";

// Kakao Maps implementation of the MapProvider contract (MapViewProps). Drop-in
// replacement for LeafletMap: same props plus an info bubble on marker hover/click
// and a "search this area" button that appears when the user pans the map. The
// Kakao Maps JS SDK is loaded once from dapi.kakao.com with autoload=false; it
// needs a JavaScript app key in NEXT_PUBLIC_KAKAO_MAP_KEY and the serving domain
// registered in the Kakao Developers console. Missing key / blocked domain
// degrades gracefully to an empty container (no crash).

import { useEffect, useRef, useState } from "react";
import { RotateCw } from "lucide-react";
import type { MapMarker, MapViewProps } from "./MapProvider";

// Minimal shape of the Kakao Maps runtime we touch (global `window.kakao`).
interface KakaoLatLng {
  getLat: () => number;
  getLng: () => number;
}
interface KakaoMapInstance {
  setBounds: (bounds: unknown, ...padding: number[]) => void;
  setCenter: (latlng: KakaoLatLng) => void;
  setLevel: (level: number) => void;
  getCenter: () => KakaoLatLng;
  relayout: () => void;
}
interface KakaoBounds {
  extend: (latlng: KakaoLatLng) => void;
}
interface KakaoOverlay {
  setMap: (map: KakaoMapInstance | null) => void;
  setPosition: (latlng: KakaoLatLng) => void;
  setContent: (content: HTMLElement | string) => void;
}
interface KakaoNamespace {
  maps: {
    load: (cb: () => void) => void;
    Map: new (el: HTMLElement, opts: { center: KakaoLatLng; level: number }) => KakaoMapInstance;
    LatLng: new (lat: number, lng: number) => KakaoLatLng;
    LatLngBounds: new () => KakaoBounds;
    CustomOverlay: new (opts: {
      position?: KakaoLatLng;
      content?: HTMLElement | string;
      yAnchor?: number;
      xAnchor?: number;
      zIndex?: number;
      clickable?: boolean;
    }) => KakaoOverlay;
    event: { addListener: (target: unknown, type: string, handler: () => void) => void };
  };
}

declare global {
  interface Window {
    kakao?: KakaoNamespace;
  }
}

const SDK_ID = "kakao-maps-sdk";
let loaderPromise: Promise<KakaoNamespace> | null = null;

/** Inject the Kakao Maps SDK once (autoload=false); resolve with `window.kakao`. */
function loadKakao(appKey: string): Promise<KakaoNamespace> {
  if (typeof window === "undefined") return Promise.reject(new Error("no window"));
  if (window.kakao?.maps) return Promise.resolve(window.kakao);
  if (loaderPromise) return loaderPromise;

  const src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&autoload=false`;
  loaderPromise = new Promise<KakaoNamespace>((resolve, reject) => {
    const done = () => {
      if (window.kakao) resolve(window.kakao);
      else reject(new Error("kakao sdk missing after load"));
    };
    const existing = document.getElementById(SDK_ID) as HTMLScriptElement | null;
    if (existing) {
      existing.addEventListener("load", done);
      existing.addEventListener("error", () => reject(new Error("kakao sdk load failed")));
      return;
    }
    const script = document.createElement("script");
    script.id = SDK_ID;
    script.src = src;
    script.async = true;
    script.onload = done;
    script.onerror = () => reject(new Error("kakao sdk load failed"));
    document.head.appendChild(script);
  });
  return loaderPromise;
}

/** Teal default / pink selected SVG pin, bottom-anchored by the CustomOverlay. */
function pinElement(selected: boolean): HTMLElement {
  const fill = selected ? "#FF1B58" : "#40C1DF";
  const size = selected ? 32 : 26;
  const el = document.createElement("div");
  el.style.cursor = "pointer";
  el.innerHTML = `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="${fill}" stroke="#15171D" stroke-width="1.2" style="filter:drop-shadow(0 2px 3px rgba(0,0,0,.55))">
    <path d="M12 2C8.1 2 5 5.1 5 9c0 5.2 7 13 7 13s7-7.8 7-13c0-3.9-3.1-7-7-7z"/>
    <circle cx="12" cy="9" r="2.6" fill="#15171D"/>
  </svg>`;
  return el;
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c] as string,
  );
}

/** Dark info-bubble shown above a marker on hover/click. */
function bubbleElement(m: MapMarker): HTMLElement {
  const wrap = document.createElement("div");
  const dist = m.distanceText
    ? `<span style="flex:none;border-radius:999px;background:rgba(64,193,223,.18);color:#40C1DF;font-size:11px;font-weight:700;padding:1px 7px;">${escapeHtml(m.distanceText)}</span>`
    : "";
  const subtitle = m.subtitle
    ? `<div style="font-size:11px;color:#9AA0AA;margin-top:3px;">${escapeHtml(m.subtitle)}</div>`
    : "";
  const address = m.address
    ? `<div style="font-size:11px;color:#9AA0AA;margin-top:2px;line-height:1.35;">${escapeHtml(m.address)}</div>`
    : "";
  wrap.innerHTML = `<div style="margin-bottom:34px;">
    <div style="position:relative;min-width:150px;max-width:240px;background:#1B1E26;border:1px solid #2A2E38;border-radius:12px;padding:9px 12px;box-shadow:0 8px 24px rgba(0,0,0,.5);">
      <div style="display:flex;align-items:center;gap:6px;">
        <strong style="font-size:13px;color:#E6E8EC;line-height:1.3;">${escapeHtml(m.label)}</strong>
        ${dist}
      </div>
      ${subtitle}
      ${address}
      <div style="position:absolute;left:50%;bottom:-6px;width:11px;height:11px;background:#1B1E26;border-right:1px solid #2A2E38;border-bottom:1px solid #2A2E38;transform:translateX(-50%) rotate(45deg);"></div>
    </div>
  </div>`;
  return wrap;
}

export default function KakaoMap({
  center,
  markers,
  selectedId,
  onSelect,
  onSearchHere,
  heightClass = "h-full",
}: MapViewProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const onSelectRef = useRef(onSelect);
  onSelectRef.current = onSelect;
  // The map center where the user panned to; drives the "search this area" button.
  const [movedCenter, setMovedCenter] = useState<{ lat: number; lng: number } | null>(null);

  useEffect(() => {
    const appKey = process.env.NEXT_PUBLIC_KAKAO_MAP_KEY;
    if (!appKey) {
      // eslint-disable-next-line no-console
      console.error("Missing NEXT_PUBLIC_KAKAO_MAP_KEY — map disabled");
      return;
    }

    setMovedCenter(null);
    let cancelled = false;
    const overlays: KakaoOverlay[] = [];

    loadKakao(appKey)
      .then((kakao) => {
        kakao.maps.load(() => {
          if (cancelled || !containerRef.current) return;
          const map = new kakao.maps.Map(containerRef.current, {
            center: new kakao.maps.LatLng(center.lat, center.lng),
            level: 3,
          });

          // One shared info bubble, repositioned per hovered/clicked marker.
          const bubble = new kakao.maps.CustomOverlay({ yAnchor: 1, xAnchor: 0.5, zIndex: 10 });
          let pinnedId: number | undefined;
          const byId = new Map<number, { pos: KakaoLatLng; data: MapMarker }>();
          const showBubble = (id: number) => {
            const hit = byId.get(id);
            if (!hit) return;
            bubble.setContent(bubbleElement(hit.data));
            bubble.setPosition(hit.pos);
            bubble.setMap(map);
          };
          const settle = () => (pinnedId != null ? showBubble(pinnedId) : bubble.setMap(null));

          const bounds = new kakao.maps.LatLngBounds();
          for (const m of markers) {
            const position = new kakao.maps.LatLng(m.lat, m.lng);
            byId.set(m.id, { pos: position, data: m });
            const content = pinElement(m.id === selectedId);
            content.addEventListener("mouseover", () => showBubble(m.id));
            content.addEventListener("mouseout", settle);
            content.addEventListener("click", () => {
              pinnedId = m.id;
              onSelectRef.current?.(m.id);
              showBubble(m.id);
            });
            const overlay = new kakao.maps.CustomOverlay({
              position,
              content,
              yAnchor: 1,
              xAnchor: 0.5,
              clickable: true,
            });
            overlay.setMap(map);
            overlays.push(overlay);
            bounds.extend(position);
          }

          if (markers.length > 1) {
            map.setBounds(bounds, 40, 40, 40, 40);
          } else if (markers.length === 1) {
            map.setCenter(new kakao.maps.LatLng(markers[0].lat, markers[0].lng));
            map.setLevel(4);
          }

          // Clicking empty map space unpins the bubble.
          kakao.maps.event.addListener(map, "click", () => {
            pinnedId = undefined;
            bubble.setMap(null);
          });

          // Show the "search this area" button once the user pans away from where
          // the current results are centred. The first idle (post setBounds) is the
          // baseline, so a programmatic re-centre doesn't trigger the button.
          let baseline: { lat: number; lng: number } | null = null;
          kakao.maps.event.addListener(map, "idle", () => {
            if (cancelled) return;
            const c = map.getCenter();
            const here = { lat: c.getLat(), lng: c.getLng() };
            if (!baseline) {
              baseline = here;
              return;
            }
            const moved = Math.abs(here.lat - baseline.lat) > 8e-4 || Math.abs(here.lng - baseline.lng) > 8e-4;
            setMovedCenter(moved ? here : null);
          });

          setTimeout(() => map.relayout(), 0);
        });
      })
      .catch(() => {
        /* missing key / blocked domain / SDK unreachable — degrade to empty container */
      });

    return () => {
      cancelled = true;
      for (const o of overlays) o.setMap(null);
    };
    // Rebuild when the marker set or center changes; selectedId-only changes don't rebuild.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [center.lat, center.lng, markers.map((m) => `${m.id}:${m.lat}:${m.lng}`).join("|")]);

  return (
    <div className={`relative ${heightClass} w-full`}>
      <div
        ref={containerRef}
        role="application"
        aria-label="지도"
        className="h-full w-full overflow-hidden rounded-card"
      />
      {movedCenter && onSearchHere && (
        <button
          type="button"
          onClick={() => {
            onSearchHere(movedCenter);
            setMovedCenter(null);
          }}
          className="absolute left-1/2 top-3 z-10 flex -translate-x-1/2 items-center gap-1.5 rounded-full border border-border/60 bg-bg/90 px-3.5 py-2 text-xs font-bold text-active shadow-lg backdrop-blur-md transition-colors hover:bg-surface"
        >
          <RotateCw className="h-3.5 w-3.5 text-primary" />이 지역에서 재검색
        </button>
      )}
    </div>
  );
}
