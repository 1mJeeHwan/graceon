"use client";

import type { ApexOptions } from "apexcharts";

import type { VisitSummaryDtoDeviceBreakdown } from "@/apis/query/streamHubAdminAPI.schemas";
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

interface VisitDeviceBreakdownChartProps {
  breakdown?: VisitSummaryDtoDeviceBreakdown;
  isLoading?: boolean;
  isError?: boolean;
  className?: string;
}

/**
 * VisitDeviceBreakdownChart renders the PC/MOBILE/TABLET visit split as a donut
 * chart from the summary endpoint's `deviceBreakdown` map. Missing buckets are
 * treated as 0 so the three categories are always present.
 */
export default function VisitDeviceBreakdownChart({
  breakdown,
  isLoading = false,
  isError = false,
  className,
}: VisitDeviceBreakdownChartProps) {
  const counts = DEVICE_ORDER.map((device) => breakdown?.[device] ?? 0);
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
      y: { formatter: (value: number) => `${value.toLocaleString("ko-KR")}회` },
    },
    stroke: { width: 0 },
  };

  return (
    <ChartCard
      title="기기별 접속 분포"
      className={className}
      isLoading={isLoading}
      isError={isError}
      isEmpty={isEmpty}
    >
      <ApexChart options={options} series={counts} type="donut" height="100%" />
    </ChartCard>
  );
}
