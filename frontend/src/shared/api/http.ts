import { API_BASE_URL } from "@/shared/config/env";
import { ApiClientError } from "@/shared/api/errors";
import type { ApiEnvelope } from "@/shared/api/types";

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

export async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(buildUrl(path), {
    method: "GET",
    headers: {
      Accept: "application/json"
    }
  });

  return unwrapResponse<T>(response);
}

export async function postJson<TResponse, TRequest>(path: string, body: TRequest): Promise<TResponse> {
  const response = await fetch(buildUrl(path), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json"
    },
    body: JSON.stringify(body)
  });

  return unwrapResponse<TResponse>(response);
}

export async function putJson<TResponse, TRequest>(path: string, body: TRequest): Promise<TResponse> {
  const response = await fetch(buildUrl(path), {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json"
    },
    body: JSON.stringify(body)
  });

  return unwrapResponse<TResponse>(response);
}

export async function deleteJson<TResponse>(path: string): Promise<TResponse> {
  const response = await fetch(buildUrl(path), {
    method: "DELETE",
    headers: {
      Accept: "application/json"
    }
  });

  return unwrapResponse<TResponse>(response);
}

export async function postFormData<TResponse>(path: string, formData: FormData): Promise<TResponse> {
  const response = await fetch(buildUrl(path), {
    method: "POST",
    body: formData
  });

  return unwrapResponse<TResponse>(response);
}

export async function downloadFile(path: string): Promise<Blob> {
  const response = await fetch(buildUrl(path), {
    method: "GET"
  });

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
