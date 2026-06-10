import Taro from "@tarojs/taro";
import { API_BASE_URL } from "@/shared/config/env";
import { ApiClientError } from "@/shared/api/errors";
import type { ApiEnvelope } from "@/shared/api/types";
import { getMobileToken } from "@/shared/auth/token-store";

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

interface RequestOptions<TRequest> {
  path: string;
  method: HttpMethod;
  data?: TRequest;
  auth?: boolean;
}

function buildUrl(path: string) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}

function buildHeaders(hasBody: boolean, auth: boolean) {
  const headers: Record<string, string> = {
    Accept: "application/json"
  };

  if (hasBody) {
    headers["Content-Type"] = "application/json";
  }

  const token = getMobileToken();
  if (auth && token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

async function requestJson<TResponse, TRequest = undefined>(options: RequestOptions<TRequest>): Promise<TResponse> {
  const hasBody = options.data !== undefined;

  try {
    const response = await Taro.request<ApiEnvelope<TResponse>, TRequest>({
      url: buildUrl(options.path),
      method: options.method,
      data: options.data,
      header: buildHeaders(hasBody, options.auth ?? true)
    });

    const payload = response.data;

    if (response.statusCode < 200 || response.statusCode >= 300 || !payload?.success || payload.data === null) {
      throw new ApiClientError(
        payload?.error?.message ?? `Request failed with status ${response.statusCode}`,
        response.statusCode,
        payload?.error?.code,
        payload?.error?.details ?? []
      );
    }

    return payload.data;
  } catch (error) {
    if (error instanceof ApiClientError) {
      throw error;
    }

    throw new ApiClientError(error instanceof Error ? error.message : "Network request failed", 0);
  }
}

export function getJson<TResponse>(path: string) {
  return requestJson<TResponse>({ path, method: "GET" });
}

export function postJson<TResponse, TRequest>(path: string, data: TRequest, auth = true) {
  return requestJson<TResponse, TRequest>({ path, method: "POST", data, auth });
}

export function putJson<TResponse, TRequest>(path: string, data: TRequest) {
  return requestJson<TResponse, TRequest>({ path, method: "PUT", data });
}

export function deleteJson<TResponse>(path: string) {
  return requestJson<TResponse>({ path, method: "DELETE" });
}
