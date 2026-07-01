"use client";

import type { ApexOptions } from "apexcharts";

import type { TimeseriesPointDto } from "@/apis/query/graceOnAdminAPI.schemas";
import ApexChart from "@/components/dashboard/ApexChart";
import ChartCard from "@/components/dashboard/ChartCard";
import { formatNumber } from "@/lib/format";

interface AnalyticsDailyTrendChartProps {
  points: TimeseriesPointDto[];
  isLoading?: boolean;
  isError?: boolean;
  className?: string;
}

/**
 * AnalyticsDailyTrendChart renders the last-30-day event and session trend as a
 * two-series area chart from `/v1/analytics/timeseries`. The backend fills empty
 * days with 0 so the x-axis stays continuous. Uses the shared SSR-safe ApexChart
 * wrapper (same dynamic import pattern as the operations/visits dashboards).
 */
export default function AnalyticsDailyTrendChart({
  points,
  isLoading = false,
  isError = false,
  className,
}: AnalyticsDailyTrendChartProps) {
  const categories = points.map((point) => point.date ?? "");
  const events = points.map((point) => point.events ?? 0);
  const sessions = points.map((point) => point.sessions ?? 0);
  const isEmpty = points.length === 0;

  const options: ApexOptions = {
    chart: {
      type: "area",
      toolbar: { show: false },
      fontFamily: "inherit",
      zoom: { enabled: false },
    },
    colors: ["#2563eb", "#60a5fa"],
    dataLabels: { enabled: false },
    stroke: { curve: "smooth", width: 2 },
    fill: {
      type: "gradient",
      gradient: { shadeIntensity: 0.3, opacityFrom: 0.4, opacityTo: 0.05 },
    },
    grid: { borderColor: "#e2e8f0", strokeDashArray: 4 },
    legend: {
      position: "top",
      horizontalAlign: "right",
      fontSize: "12px",
      labels: { colors: "#64748b" },
    },
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
      y: { formatter: (value: number) => `${formatNumber(value)}건` },
    },
  };

  const series = [
    { name: "이벤트", data: events },
    { name: "세션", data: sessions },
  ];

  return (
    <ChartCard
      title="일별 활동 추이 (최근 30일)"
      className={className}
      isLoading={isLoading}
      isError={isError}
      isEmpty={isEmpty}
    >
      <ApexChart options={options} series={series} type="area" height="100%" />
    </ChartCard>
  );
}
