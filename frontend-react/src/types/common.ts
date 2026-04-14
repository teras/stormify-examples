export type SortDir = "ASC" | "DESC";

export interface PageSpec {
  page: number;
  pageSize: number;
  filters: Record<string, string>;
  sorts: Record<string, SortDir>;
  caseSensitive: Record<string, boolean>;
}

export interface PagedResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface ErrorResponse {
  message: string;
  errorCode: string;
  details?: Record<string, unknown>;
}

export interface ReferenceSummary {
  id: number;
  name?: string;
  label?: string;
}

export interface SearchPayload {
  filters?: Record<string, string>;
  sorts?: Record<string, SortDir>;
  page?: number;
  pageSize?: number;
  caseSensitive?: Record<string, boolean>;
}
