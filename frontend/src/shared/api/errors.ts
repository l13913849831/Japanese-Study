import type { ApiFieldError } from "@/shared/api/types";

export class ApiClientError extends Error {
  status: number;
  code?: string;
  details: ApiFieldError[];

  constructor(message: string, status: number, code?: string, details: ApiFieldError[] = []) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.code = code;
    this.details = details;
  }
}
