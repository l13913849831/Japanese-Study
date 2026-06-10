import type { ApiFieldError } from "@/shared/api/types";

export class ApiClientError extends Error {
  statusCode: number;
  code?: string;
  details: ApiFieldError[];

  constructor(message: string, statusCode: number, code?: string, details: ApiFieldError[] = []) {
    super(message);
    this.name = "ApiClientError";
    this.statusCode = statusCode;
    this.code = code;
    this.details = details;
  }
}
