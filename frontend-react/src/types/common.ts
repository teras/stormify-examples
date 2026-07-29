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
  pageSize: number;
  totalItems: number;
  totalPages: number;
}

export interface ErrorResponse {
  message: string;
  errorCode: string;
  details?: Record<string, unknown>;
}

/** A minimal `{id, label}` pointer to a related row, as embedded in details responses. */
export interface Ref {
  id: number;
  label: string;
}

export interface SearchPayload {
  filters?: Record<string, string>;
  sorts?: Record<string, SortDir>;
  page?: number;
  pageSize?: number;
  caseSensitive?: Record<string, boolean>;
}
