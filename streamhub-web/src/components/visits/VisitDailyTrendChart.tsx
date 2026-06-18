"use client";

import type { ApexOptions } from "apexcharts";

import type { DailyCountDto } from "@/apis/query/streamHubAdminAPI.schemas";
import ApexChart from "@/components/dashboard/ApexChart";
import ChartCard from "@/components/dashboard/ChartCard";
import { formatNumber } from "@/lib/format";

interface VisitDailyTrendChartProps {
  daily: DailyCountDto[];
  isLoading?: boolean;
  isError?: boolean;
  className?: string;
}

/**
 * VisitDailyTrendChart renders the daily visit-count trend as an area chart from
 * `/v1/visit/daily`. The backend fills empty days with 0 so the x-axis stays
 * continuous. Uses the shared SSR-safe ApexChart wrapper (same dynamic import
 * pattern as the operations dashboard).
 */
export default function VisitDailyTrendChart({
  daily,
  isLoading = false,
  isError = false,
  className,
}: VisitDailyTrendChartProps) {
  const categories = daily.map((point) => point.date ?? "");
  const counts = daily.map((point) => point.count ?? 0);
  const isEmpty = daily.length === 0;

  const options: ApexOptions = {
    chart: {
      type: "area",
      toolbar: { show: false },
      fontFamily: "inherit",
      zoom: { enabled: false },
    },
    colors: ["#2563eb"],
    dataLabels: { enabled: false },
    stroke: { curve: "smooth", width: 2 },
    fill: {
      type: "gradient",
      gradient: { shadeIntensity: 0.3, opacityFrom: 0.4, opacityTo: 0.05 },
    },
    grid: { borderColor: "#e2e8f0", strokeDashArray: 4 },
    xaxis: {
      categories,
      type: "category",
      tickAmount: 8,
      labels: {
        rotate: 0,
        hideOverlappingLabels: true,
        style: { colors: "#64748b", fontSize: "11px" },
        formatter: (value: string) => (value ? value.slice(5) : value),
      },
      axisTicks: { show: false },
    },
    yaxis: {
      labels: {
        style: { colors: "#64748b", fontSize: "12px" },
        formatter: (value: number) => formatNumber(Math.round(value)),
      },
    },
    tooltip: {
      y: { formatter: (value: number) => `${formatNumber(value)}회` },
    },
  };

  const series = [{ name: "접속 수", data: counts }];

  return (
    <ChartCard
      title="일자별 접속 추이"
      className={className}
      isLoading={isLoading}
      isError={isError}
      isEmpty={isEmpty}
    >
      <ApexChart options={options} series={series} type="area" height="100%" />
    </ChartCard>
  );
}
