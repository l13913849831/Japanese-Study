import { API_BASE_URL } from "@/shared/config/env";
import { ApiClientError } from "@/shared/api/errors";
import type { ApiEnvelope } from "@/shared/api/types";

interface CsrfTokenPayload {
  headerName: string;
  parameterName: string;
  token: string;
}

function buildUrl(path: string) {
  return `${API_BASE_URL}${path}`;
}

async function unwrapResponse<T>(response: Response): Promise<T> {
  const payload = (await response.json()) as ApiEnvelope<T>;

  if (!response.ok || !payload.success || payload.data === null) {
    throw new ApiClientError(
      payload.error?.message ?? `Request failed with status ${response.status}`,
      response.status,
      payload.error?.code,
      payload.error?.details ?? []
    );
  }

  return payload.data;
}

async function unwrapFileResponse(response: Response): Promise<Blob> {
  if (!response.ok) {
    const payload = (await response.json()) as ApiEnvelope<null>;
    throw new ApiClientError(
      payload.error?.message ?? `Request failed with status ${response.status}`,
      response.status,
      payload.error?.code,
      payload.error?.details ?? []
    );
  }

  return response.blob();
}

async function getCsrfToken(): Promise<CsrfTokenPayload> {
  const response = await fetch(buildUrl("/auth/csrf"), {
    method: "GET",
    credentials: "include",
    headers: {
      Accept: "application/json"
    }
  });

  return unwrapResponse<CsrfTokenPayload>(response);
}

async function buildMutationHeaders(baseHeaders: Record<string, string> = {}): Promise<Record<string, string>> {
  const csrfToken = await getCsrfToken();
  return {
    ...baseHeaders,
    [csrfToken.headerName]: csrfToken.token
  };
}

export async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(buildUrl(path), {
    method: "GET",
    credentials: "include",
    headers: {
      Accept: "application/json"
    }
  });

  return unwrapResponse<T>(response);
}

export async function postJson<TResponse, TRequest>(path: string, body: TRequest): Promise<TResponse> {
  const headers = await buildMutationHeaders({
    "Content-Type": "application/json",
    Accept: "application/json"
  });
  const response = await fetch(buildUrl(path), {
    method: "POST",
    credentials: "include",
    headers,
    body: JSON.stringify(body)
  });

  return unwrapResponse<TResponse>(response);
}

export async function putJson<TResponse, TRequest>(path: string, body: TRequest): Promise<TResponse> {
  const headers = await buildMutationHeaders({
    "Content-Type": "application/json",
    Accept: "application/json"
  });
  const response = await fetch(buildUrl(path), {
    method: "PUT",
    credentials: "include",
    headers,
    body: JSON.stringify(body)
  });

  return unwrapResponse<TResponse>(response);
}

export async function deleteJson<TResponse>(path: string): Promise<TResponse> {
  const headers = await buildMutationHeaders({
    Accept: "application/json"
  });
  const response = await fetch(buildUrl(path), {
    method: "DELETE",
    credentials: "include",
    headers
  });

  return unwrapResponse<TResponse>(response);
}

export async function postFormData<TResponse>(path: string, formData: FormData): Promise<TResponse> {
  const headers = await buildMutationHeaders();
  const response = await fetch(buildUrl(path), {
    method: "POST",
    credentials: "include",
    headers,
    body: formData
  });

  return unwrapResponse<TResponse>(response);
}

export async function downloadFile(path: string): Promise<Blob> {
  const response = await fetch(buildUrl(path), {
    method: "GET",
    credentials: "include"
  });

  return unwrapFileResponse(response);
}

export async function postDownloadFile(path: string): Promise<Blob> {
  const headers = await buildMutationHeaders();
  const response = await fetch(buildUrl(path), {
    method: "POST",
    credentials: "include",
    headers
  });

  return unwrapFileResponse(response);
}
