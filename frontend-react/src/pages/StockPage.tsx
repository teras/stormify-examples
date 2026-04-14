import { Stack } from "@mui/material";
import type { ColDef, ICellRendererParams } from "ag-grid-community";
import { useCallback } from "react";
import { resources } from "../api/resources";
import { PageCard } from "../components/PageCard";
import { RefLink } from "../components/RefLink";
import { SearchDataGrid, numberFilterParams } from "../components/SearchDataGrid";
import { useApiBaseUrl } from "../contexts/ApiBaseUrlContext";
import type { PageSpec } from "../types/common";
import type { StockListItem } from "../types/domain";

const columns: ColDef<StockListItem>[] = [
  {
    field: "warehouseName",
    headerName: "Warehouse",
    flex: 1.2,
    cellRenderer: (p: ICellRendererParams<StockListItem>) =>
      p.data ? <RefLink type="warehouse" id={p.data.warehouseId} label={p.data.warehouseName} /> : null,
  },
  {
    field: "productSku",
    headerName: "SKU",
    flex: 0.8,
    cellRenderer: (p: ICellRendererParams<StockListItem>) =>
      p.data ? <RefLink type="product" id={p.data.productId} label={p.data.productSku} /> : null,
  },
  {
    field: "productName",
    headerName: "Product",
    flex: 1.4,
    cellRenderer: (p: ICellRendererParams<StockListItem>) =>
      p.data ? <RefLink type="product" id={p.data.productId} label={p.data.productName} /> : null,
  },
  { field: "quantityOnHand", headerName: "On Hand", flex: 0.8, filter: "agNumberColumnFilter", filterParams: numberFilterParams },
  { field: "quantityReserved", headerName: "Reserved", flex: 0.8, filter: "agNumberColumnFilter", filterParams: numberFilterParams },
  { field: "availableQuantity", headerName: "Available", flex: 0.8, filter: "agNumberColumnFilter", filterParams: numberFilterParams },
  { field: "reorderLevel", headerName: "Reorder", flex: 0.8, filter: "agNumberColumnFilter", filterParams: numberFilterParams },
  { field: "lastUpdatedAt", headerName: "Last Updated", flex: 1.1 },
];

export function StockPage() {
  const { baseUrl } = useApiBaseUrl();

  const fetchPage = useCallback(
    (spec: PageSpec) => resources.stock.search(baseUrl, spec),
    [baseUrl],
  );

  return (
    <PageCard title="Stock" subtitle="Read-only inventory view backed by stateless PagedQuery facets.">
      <SearchDataGrid<StockListItem>
        columns={columns}
        fetchPage={fetchPage}
        exportFileName="stock"
        exportUrl={`${baseUrl}/api/stock/export`}
      />
    </PageCard>
  );
}
