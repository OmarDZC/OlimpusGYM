const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export type Role = "OWNER" | "STAFF" | "MEMBER";
export type PlanCode = "BASICA" | "COMPLETA" | "ATLETA_PRO";

export type AuthUser = {
  userId: string;
  tenantId: string;
  email: string;
  fullName: string;
  role: Role;
};

export type AuthResponse = AuthUser & { token: string };

/** Error de la API con el detalle de un ProblemDetail (RFC 9457) del backend. */
export class ApiError extends Error {
  status: number;
  fieldErrors?: Record<string, string>;

  constructor(message: string, status: number, fieldErrors?: Record<string, string>) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function throwProblem(response: Response): Promise<never> {
  let detail = "No se pudo completar la solicitud. Inténtalo de nuevo.";
  let fieldErrors: Record<string, string> | undefined;
  try {
    const body = await response.json();
    if (typeof body.detail === "string") detail = body.detail;
    if (body.errors && typeof body.errors === "object") fieldErrors = body.errors;
  } catch {
    // La respuesta no traía un cuerpo JSON legible: nos quedamos con el mensaje genérico.
  }
  throw new ApiError(detail, response.status, fieldErrors);
}

export async function register(input: {
  email: string;
  password: string;
  fullName: string;
  planCode: PlanCode;
}): Promise<AuthResponse> {
  const response = await fetch(`${API_URL}/api/v1/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function login(input: { email: string; password: string }): Promise<AuthResponse> {
  const response = await fetch(`${API_URL}/api/v1/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function fetchMe(token: string): Promise<AuthUser> {
  const response = await fetch(`${API_URL}/api/v1/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}
