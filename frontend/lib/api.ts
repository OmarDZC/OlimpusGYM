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

export type SessionStatus = "SCHEDULED" | "CANCELLED";

export type ScheduleSession = {
  id: string;
  name: string;
  discipline: string;
  room: string;
  coachName: string | null;
  startsAt: string;
  endsAt: string;
  capacity: number;
  bookedCount: number;
  freeSlots: number;
  waitlistCount: number;
  status: SessionStatus;
};

export type BookingStatus = "BOOKED" | "WAITLIST" | "CANCELLED" | "ATTENDED" | "NO_SHOW";

export type MyBooking = {
  id: string;
  sessionId: string;
  sessionName: string;
  discipline: string;
  room: string;
  coachName: string | null;
  startsAt: string;
  endsAt: string;
  status: BookingStatus;
  waitlistPosition: number | null;
};

export async function fetchSchedule(params: {
  from?: string;
  to?: string;
  discipline?: string;
}): Promise<ScheduleSession[]> {
  const query = new URLSearchParams();
  if (params.from) query.set("from", params.from);
  if (params.to) query.set("to", params.to);
  if (params.discipline) query.set("discipline", params.discipline);
  const response = await fetch(`${API_URL}/api/v1/public/schedule?${query.toString()}`);
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function fetchMyBookings(token: string): Promise<MyBooking[]> {
  const response = await fetch(`${API_URL}/api/v1/me/bookings`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function bookSession(token: string, sessionId: string): Promise<void> {
  const response = await fetch(`${API_URL}/api/v1/me/bookings`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({ sessionId }),
  });
  if (!response.ok) await throwProblem(response);
}

export async function cancelBooking(token: string, bookingId: string): Promise<void> {
  const response = await fetch(`${API_URL}/api/v1/me/bookings/${bookingId}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
}
