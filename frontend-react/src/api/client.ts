import type { ErrorResponse, PageSpec, PagedResponse } from "../types/common";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, payload: ErrorResponse) {
    super(payload.message);
    this.status = status;
    this.code = payload.errorCode;
  }
}

async function request<T>(baseUrl: string, path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    const payload = (await response.json().catch(() => ({
      message: response.statusText,
      errorCode: "HTTP_ERROR",
    }))) as ErrorResponse;
    throw new ApiError(response.status, payload);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const apiClient = {
  search<T>(baseUrl: string, path: string, spec: PageSpec) {
    return request<PagedResponse<T>>(baseUrl, `${path}/search`, {
      method: "POST",
      body: JSON.stringify(spec),
    });
  },
  get<T>(baseUrl: string, path: string) {
    return request<T>(baseUrl, path);
  },
  create<TRequest, TResponse>(baseUrl: string, path: string, body: TRequest) {
    return request<TResponse>(baseUrl, path, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },
  update<TRequest, TResponse>(baseUrl: string, path: string, body: TRequest) {
    return request<TResponse>(baseUrl, path, {
      method: "PUT",
      body: JSON.stringify(body),
    });
  },
  remove(baseUrl: string, path: string) {
    return request<void>(baseUrl, path, { method: "DELETE" });
  },
  action<TResponse>(baseUrl: string, path: string) {
    return request<TResponse>(baseUrl, path, { method: "POST" });
  },
};
