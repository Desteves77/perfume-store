import type { Problem } from "./types";

const API_URL = (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080").replace(/\/$/, "");

export class ApiError extends Error {
  constructor(public status: number, public problem: Problem) {
    super(problem.message ?? problem.title ?? "Não foi possível concluir a operação.");
    this.name = "ApiError";
  }
}

async function csrf(): Promise<string> {
  const response = await fetch(API_URL + "/api/v1/security/csrf", { credentials: "include", cache: "no-store" });
  if (!response.ok) throw new ApiError(response.status, await response.json().catch(() => ({})));
  const body = (await response.json()) as { token: string };
  return body.token;
}

export function apiUrl(path: string): string {
  return path.startsWith("http") ? path : API_URL + path;
}

export function operationKey(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID() : String(Date.now()) + "-" + Math.random().toString(16).slice(2);
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  if (init.body && !(init.body instanceof FormData)) headers.set("Content-Type", "application/json");
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) headers.set("X-XSRF-TOKEN", await csrf());
  const response = await fetch(apiUrl(path), { ...init, method, headers, credentials: "include" });
  if (response.status === 204) return undefined as T;
  const contentType = response.headers.get("content-type") ?? "";
  const body = contentType.includes("json") ? await response.json() : await response.text();
  if (!response.ok) throw new ApiError(response.status, typeof body === "object" ? body : { message: body });
  return body as T;
}

export async function upload<T>(path: string, form: FormData): Promise<T> {
  const response = await fetch(apiUrl(path), {
    method: "POST", body: form, credentials: "include", headers: { "X-XSRF-TOKEN": await csrf() }
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new ApiError(response.status, body);
  return body as T;
}

export const apiClient = {
  get: <T>(path: string) => api<T>(path),
  post: <T>(path: string, body?: unknown, key?: string) =>
    api<T>(path, { method: "POST", body: body === undefined ? undefined : JSON.stringify(body),
      headers: key ? { "Idempotency-Key": key } : undefined }),
  patch: <T>(path: string, body: unknown) => api<T>(path, { method: "PATCH", body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) => api<T>(path, { method: "PUT", body: JSON.stringify(body) }),
  delete: <T>(path: string) => api<T>(path, { method: "DELETE" })
};
