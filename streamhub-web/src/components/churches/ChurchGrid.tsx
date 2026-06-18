"use client";

import { useMemo } from "react";
import { useRouter } from "next/navigation";
import {
  AllCommunityModule,
  ModuleRegistry,
  themeQuartz,
  type ColDef,
  type GridReadyEvent,
  type ICellRendererParams,
} from "ag-grid-community";
import { AgGridReact } from "ag-grid-react";

import type { ChurchListItem } from "@/apis/query/streamHubAdminAPI.schemas";
import {
  DenominationBadge,
  OpenBadge,
} from "@/components/churches/ChurchBadges";

// AG Grid v33 requires explicit module registration. Registering the full
// community bundle once at module scope covers every feature used here.
ModuleRegistry.registerModules([AllCommunityModule]);

// v33 Theming API: a Tailwind-friendly Quartz theme tuned to the slate palette.
const gridTheme = themeQuartz.withParams({
  accentColor: "#2563eb",
  borderColor: "#e2e8f0",
  headerBackgroundColor: "#f8fafc",
  headerTextColor: "#334155",
  fontFamily: "inherit",
  fontSize: 13,
  rowHeight: 52,
  headerHeight: 44,
});

interface ChurchGridProps {
  rows: ChurchListItem[];
}

function ThumbnailCell({ url }: { url?: string }) {
  if (!url) {
    return (
      <div className="flex h-9 w-9 items-center justify-center rounded bg-slate-100 text-[10px] text-slate-400">
        없음
      </div>
    );
  }
  return (
    // Plain <img>: thumbnails come from arbitrary storage origins (thumbnailUrl
    // is an absolute URL) and next/image would require per-host config.
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={url}
      alt="썸네일"
      className="h-9 w-9 rounded object-cover"
      loading="lazy"
    />
  );
}

/**
 * ChurchGrid renders the church result set with AG Grid (community, v33).
 * Rows are read-only; edits happen on the detail page reached via the 상세 button
 * (or by clicking the row).
 */
export default function ChurchGrid({ rows }: ChurchGridProps) {
  const router = useRouter();

  const columnDefs = useMemo<ColDef<ChurchListItem>[]>(
    () => [
      {
        headerName: "",
        field: "thumbnailUrl",
        minWidth: 60,
        maxWidth: 64,
        sortable: false,
        filter: false,
        cellRenderer: (params: ICellRendererParams<ChurchListItem>) => (
          <ThumbnailCell url={params.value ?? undefined} />
        ),
      },
      {
        field: "name",
        headerName: "교회명",
        minWidth: 180,
        flex: 1.4,
      },
      {
        field: "denomination",
        headerName: "교단",
        minWidth: 110,
        cellRenderer: (params: ICellRendererParams<ChurchListItem>) => (
          <DenominationBadge value={params.value ?? undefined} />
        ),
      },
      {
        field: "regionName",
        headerName: "지역",
        minWidth: 120,
        valueFormatter: (params) => params.value ?? "-",
      },
      {
        field: "address",
        headerName: "주소",
        minWidth: 200,
        flex: 1.2,
        valueFormatter: (params) => params.value ?? "-",
      },
      {
        field: "pastorName",
        headerName: "담임목사",
        minWidth: 110,
        valueFormatter: (params) => params.value ?? "-",
      },
      {
        field: "phone",
        headerName: "전화",
        minWidth: 130,
        valueFormatter: (params) => params.value ?? "-",
      },
      {
        field: "openYn",
        headerName: "공개",
        minWidth: 90,
        cellRenderer: (params: ICellRendererParams<ChurchListItem>) => (
          <OpenBadge value={params.value ?? undefined} />
        ),
      },
      {
        headerName: "상세",
        minWidth: 70,
        maxWidth: 80,
        sortable: false,
        filter: false,
        cellRenderer: (params: ICellRendererParams<ChurchListItem>) => {
          const id = params.data?.id;
          if (id == null) {
            return null;
          }
          return (
            <button
              type="button"
              onClick={(event) => {
                event.stopPropagation();
                router.push(`/churches/${id}`);
              }}
              className="text-sm font-medium text-brand hover:underline"
            >
              상세
            </button>
          );
        },
      },
    ],
    [router],
  );

  const defaultColDef = useMemo<ColDef<ChurchListItem>>(
    () => ({ sortable: true, resizable: true, suppressMovable: true }),
    [],
  );

  const handleGridReady = (event: GridReadyEvent) => {
    event.api.sizeColumnsToFit();
  };

  return (
    <div className="h-[560px] w-full">
      <AgGridReact<ChurchListItem>
        theme={gridTheme}
        rowData={rows}
        columnDefs={columnDefs}
        defaultColDef={defaultColDef}
        onGridReady={handleGridReady}
        onRowClicked={(event) => {
          const id = event.data?.id;
          if (id != null) {
            router.push(`/churches/${id}`);
          }
        }}
        getRowId={(params) => String(params.data.id)}
        overlayNoRowsTemplate="조회된 교회가 없습니다."
      />
    </div>
  );
}
