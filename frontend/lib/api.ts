export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public campos?: Record<string, string>,
  ) {
    super(message);
  }
}

/** O Spring entrega o token CSRF num cookie legivel; o front devolve o valor no cabecalho. */
function xsrf(): string {
  const m = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
  return m ? decodeURIComponent(m[1]) : "";
}

async function req<T>(method: string, url: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  const init: RequestInit = { method, headers };
  if (method !== "GET") headers["X-XSRF-TOKEN"] = xsrf();
  if (body instanceof FormData) {
    init.body = body; // o navegador define o Content-Type com o boundary
  } else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    init.body = JSON.stringify(body);
  }
  const res = await fetch(url, init);
  if (res.status === 204) return undefined as T;
  const dados = await res.json().catch(() => null);
  if (!res.ok) {
    // ProblemDetail (RFC 9457): "detail" e, na validacao, "campos"
    throw new ApiError(res.status, dados?.detail ?? "Erro inesperado. Tente de novo.", dados?.campos);
  }
  return dados as T;
}

export const api = {
  get: <T>(url: string) => req<T>("GET", url),
  post: <T>(url: string, body?: unknown) => req<T>("POST", url, body),
  put: <T>(url: string, body: unknown) => req<T>("PUT", url, body),
};
