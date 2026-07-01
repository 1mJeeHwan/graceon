"use client";

import type { ApexOptions } from "apexcharts";

import type { AnalyticsBreakdownDtoByDevice } from "@/apis/query/graceOnAdminAPI.schemas";
import ApexChart from "@/components/dashboard/ApexChart";
import ChartCard from "@/components/dashboard/ChartCard";

// 기기 표시 순서/라벨/색상 고정 (PC / MOBILE / TABLET).
const DEVICE_ORDER = ["PC", "MOBILE", "TABLET"] as const;
const DEVICE_LABELS: Record<(typeof DEVICE_ORDER)[number], string> = {
  PC: "PC",
  MOBILE: "모바일",
  TABLET: "태블릿",
};
const DEVICE_COLORS = ["#2563eb", "#60a5fa", "#bfdbfe"];

interface AnalyticsDeviceDonutChartProps {
  byDevice?: AnalyticsBreakdownDtoByDevice;
  isLoading?: boolean;
  isError?: boolean;
  className?: string;
}

/**
 * AnalyticsDeviceDonutChart renders the PC/MOBILE/TABLET event split as a donut
 * chart from the breakdown endpoint's `byDevice` object map. Keys are read
 * defensively — missing buckets are treated as 0 so the three categories are
 * always present even if the backend omits one.
 */
export default function AnalyticsDeviceDonutChart({
  byDevice,
  isLoading = false,
  isError = false,
  className,
}: AnalyticsDeviceDonutChartProps) {
  const counts = DEVICE_ORDER.map((device) => byDevice?.[device] ?? 0);
  const labels = DEVICE_ORDER.map((device) => DEVICE_LABELS[device]);
  const isEmpty = counts.every((count) => count === 0);

  const options: ApexOptions = {
    chart: { type: "donut", fontFamily: "inherit" },
    colors: DEVICE_COLORS,
    labels,
    legend: {
      position: "bottom",
      fontSize: "12px",
      labels: { colors: "#64748b" },
    },
    dataLabels: {
      enabled: true,
      formatter: (value: number) => `${Math.round(value)}%`,
    },
    plotOptions: {
      pie: { donut: { size: "62%" } },
    },
    tooltip: {
      y: { formatter: (value: number) => `${value.toLocaleString("ko-KR")}건` },
    },
    stroke: { width: 0 },
  };

  return (
    <ChartCard
      title="기기별 분포"
      className={className}
      isLoading={isLoading}
      isError={isError}
      isEmpty={isEmpty}
    >
      <ApexChart options={options} series={counts} type="donut" height="100%" />
    </ChartCard>
  );
}
