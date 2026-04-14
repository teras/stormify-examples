import { Alert, Box, Button, Stack, TextField, Typography } from "@mui/material";
import ClearAllRounded from "@mui/icons-material/ClearAllRounded";
import DownloadRounded from "@mui/icons-material/DownloadRounded";
import {
  AllCommunityModule,
  ModuleRegistry,
  themeQuartz,
  type ColDef,
  type GridApi,
  type GridReadyEvent,
  type IDatasource,
  type IGetRowsParams,
} from "ag-grid-community";
import { AgGridReact } from "ag-grid-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { PageSpec, PagedResponse, SortDir } from "../types/common";

ModuleRegistry.registerModules([AllCommunityModule]);

export const numberFilterParams = {
  filterOptions: ["equals", "greaterThan", "greaterThanOrEqual", "lessThan", "lessThanOrEqual"],
  maxNumConditions: 1,
  closeOnApply: true,
  buttons: ["apply", "reset"],
};

export const dateFilterParams = {
  filterOptions: ["equals", "notEqual", "lessThan", "greaterThan", "inRange"],
  maxNumConditions: 1,
};

interface SearchDataGridProps<TRow extends { id: number }> {
  columns: ColDef<TRow>[];
  fetchPage: (spec: PageSpec) => Promise<PagedResponse<TRow>>;
  /** Invalidation token: change this value to force a refresh (after create/update/delete). */
  refreshToken?: number;
  baseFilters?: Record<string, string>;
  baseSorts?: Record<string, SortDir>;
  exportFileName?: string;
  exportUrl?: string;
  onRowClick?: (row: TRow) => void;
}

const theme = themeQuartz.withParams({
  accentColor: "#1976d2",
  browserColorScheme: "light",
  fontFamily: "inherit",
  headerFontWeight: 600,
});

function buildSorts(sortModel: { colId: string; sort: "asc" | "desc" | null | undefined }[]): Record<string, SortDir> {
  const sorts: Record<string, SortDir> = {};
  for (const { colId, sort } of sortModel) {
    if (sort === "asc") sorts[colId] = "ASC";
    else if (sort === "desc") sorts[colId] = "DESC";
  }
  return sorts;
}

function buildFilters(filterModel: Record<string, unknown>): Record<string, string> {
  const filters: Record<string, string> = {};
  for (const [field, raw] of Object.entries(filterModel ?? {})) {
    const val = raw as { filterType?: string; type?: string; filter?: unknown; dateFrom?: string; dateTo?: string; filterTo?: unknown; value?: unknown };
    if (!val) continue;
    const filter = val.filter;
    const filterTo = val.filterTo;
    const op = val.type;
    // Translate common AG Grid operators → stormify facet string syntax
    if (val.filterType === "number" || val.filterType === "text") {
      if (op === "equals") filters[field] = `"${filter}"`;
      else if (op === "notEqual") filters[field] = `!= ${filter}`;
      else if (op === "greaterThan") filters[field] = `> ${filter}`;
      else if (op === "greaterThanOrEqual") filters[field] = `>= ${filter}`;
      else if (op === "lessThan") filters[field] = `< ${filter}`;
      else if (op === "lessThanOrEqual") filters[field] = `<= ${filter}`;
      else if (op === "inRange" && filter != null && filterTo != null) filters[field] = `${filter} ... ${filterTo}`;
      else if (op === "contains") filters[field] = String(filter);
      else if (op === "notContains") filters[field] = `-${filter}`;
      else if (op === "startsWith") filters[field] = `${filter}*`;
      else if (op === "endsWith") filters[field] = `*${filter}`;
      else if (op === "blank") filters[field] = "";
      else if (filter != null && String(filter).length > 0) filters[field] = String(filter);
    } else if (val.value != null && typeof val.value === "string" && val.value.length > 0 && !val.filterType) {
      filters[field] = val.value;
    } else if (val.filterType === "date") {
      if (op === "equals" && val.dateFrom) filters[field] = val.dateFrom;
      else if (op === "inRange" && val.dateFrom && val.dateTo) filters[field] = `${val.dateFrom} ... ${val.dateTo}`;
      else if (op === "greaterThan" && val.dateFrom) filters[field] = `> ${val.dateFrom}`;
      else if (op === "lessThan" && val.dateFrom) filters[field] = `< ${val.dateFrom}`;
    }
  }
  return filters;
}

export function SearchDataGrid<TRow extends { id: number }>({
  columns,
  fetchPage,
  refreshToken,
  baseFilters = {},
  baseSorts = {},
  exportFileName,
  exportUrl,
  onRowClick,
}: SearchDataGridProps<TRow>) {
  const [quickFilter, setQuickFilter] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);
  const gridApiRef = useRef<GridApi<TRow> | null>(null);
  const pageSizeRef = useRef(20);
  const currentSpecRef = useRef<PageSpec | null>(null);

  const defaultColDef = useMemo<ColDef>(() => ({
    sortable: true,
    filter: "agTextColumnFilter",
    filterParams: {
      filterOptions: ["contains", "notContains", "equals", "startsWith", "endsWith"],
      maxNumConditions: 1,
      closeOnApply: true,
      buttons: ["apply", "reset"],
    },
    resizable: true,
    flex: 1,
    minWidth: 100,
  }), []);

  const datasource = useMemo<IDatasource>(() => ({
    rowCount: undefined,
    getRows: async (params: IGetRowsParams) => {
      try {
        setError(null);
        const pageSize = pageSizeRef.current;
        const page = Math.floor(params.startRow / pageSize);
        const sorts = { ...baseSorts, ...buildSorts(params.sortModel as Array<{ colId: string; sort: "asc" | "desc" | null | undefined }>) };
        const filters = { ...baseFilters, ...buildFilters(params.filterModel as Record<string, unknown>) };
        if (quickFilter.trim()) filters.search = quickFilter.trim();
        const spec: PageSpec = {
          page,
          pageSize,
          filters,
          sorts,
          caseSensitive: {},
        };
        currentSpecRef.current = spec;
        const result = await fetchPage(spec);
        params.successCallback(result.items, result.totalItems);
      } catch (e) {
        setError(e instanceof Error ? e.message : "Search failed");
        params.failCallback();
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }), [fetchPage, refreshToken, quickFilter, JSON.stringify(baseFilters), JSON.stringify(baseSorts)]);

  const onGridReady = useCallback((e: GridReadyEvent<TRow>) => {
    gridApiRef.current = e.api;
    e.api.setGridOption("datasource", datasource);
  }, [datasource]);

  useEffect(() => {
    gridApiRef.current?.setGridOption("datasource", datasource);
  }, [datasource]);

  const handleClearAll = () => {
    setQuickFilter("");
    const api = gridApiRef.current;
    if (!api) return;
    api.setFilterModel(null);
    api.applyColumnState({ defaultState: { sort: null }, applyOrder: false });
  };

  const handleExport = async () => {
    if (exportUrl && currentSpecRef.current) {
      setExporting(true);
      try {
        const res = await fetch(exportUrl, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(currentSpecRef.current),
        });
        if (!res.ok) throw new Error(`Export failed: ${res.status}`);
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = exportFileName ? `${exportFileName}.csv` : "export.csv";
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
      } catch (e) {
        setError(e instanceof Error ? e.message : "Export failed");
      } finally {
        setExporting(false);
      }
    } else {
      gridApiRef.current?.exportDataAsCsv({
        fileName: exportFileName ? `${exportFileName}.csv` : "export.csv",
      });
    }
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={2} sx={{ alignItems: "center", flexWrap: "wrap" }}>
        <Box sx={{ maxWidth: 320, flex: "1 1 240px" }}>
          <TextField
            fullWidth
            size="small"
            label="Quick filter"
            placeholder="Type to search across columns"
            value={quickFilter}
            onChange={(event) => setQuickFilter(event.target.value)}
          />
        </Box>
        <Button
          variant="text"
          size="small"
          startIcon={<ClearAllRounded />}
          onClick={handleClearAll}
        >
          Clear all
        </Button>
        <Button
          variant="outlined"
          size="small"
          startIcon={<DownloadRounded />}
          onClick={handleExport}
          disabled={exporting}
        >
          {exporting ? "Exporting…" : "Export CSV"}
        </Button>
      </Stack>
      <Typography variant="body2" color="text.secondary">
        Tip: Shift+click a column header to add it as a secondary sort.
      </Typography>
      {error && <Alert severity="error">{error}</Alert>}
      <Box sx={{ height: 600, width: "100%" }}>
        <AgGridReact<TRow>
          theme={theme}
          columnDefs={columns}
          defaultColDef={defaultColDef}
          rowModelType="infinite"
          cacheBlockSize={pageSizeRef.current}
          maxBlocksInCache={10}
          pagination
          paginationPageSize={20}
          paginationPageSizeSelector={[10, 20, 50, 100]}
          onPaginationChanged={(e) => {
            const size = e.api.paginationGetPageSize();
            if (size !== pageSizeRef.current) {
              pageSizeRef.current = size;
              e.api.setGridOption("cacheBlockSize", size);
              e.api.refreshInfiniteCache();
            }
          }}
          onGridReady={onGridReady}
          onRowClicked={onRowClick ? (e) => e.data && onRowClick(e.data) : undefined}
          suppressRowClickSelection
        />
      </Box>
    </Stack>
  );
}
