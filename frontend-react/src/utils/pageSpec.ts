import type { GridPaginationModel, GridSortModel } from "@mui/x-data-grid";
import type { PageSpec, SearchPayload, SortDir } from "../types/common";

export const emptyPageSpec = (): PageSpec => ({
  page: 0,
  pageSize: 20,
  filters: {},
  sorts: {},
  caseSensitive: {},
});

export const mergeSearchPayload = (
  paginationModel: GridPaginationModel,
  sortModel: GridSortModel,
  payload: SearchPayload,
): PageSpec => {
  const filters = Object.fromEntries(
    Object.entries(payload.filters ?? {}).filter(([, value]) => value.trim().length > 0),
  );

  const sorts: Record<string, SortDir> = sortModel.length
    ? sortModel.reduce<Record<string, SortDir>>((acc, item) => {
        acc[item.field] = item.sort === "desc" ? "DESC" : "ASC";
        return acc;
      }, {})
    : payload.sorts ?? {};

  return {
    page: paginationModel.page,
    pageSize: paginationModel.pageSize,
    filters,
    sorts,
    caseSensitive: payload.caseSensitive ?? {},
  };
};
