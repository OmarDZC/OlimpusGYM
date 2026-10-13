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

/** {@code website}: campo trampa oculto contra bots, debe llegar siempre vacío (ver /contacto). */
export async function submitContact(input: { name: string; email: string; message: string; website: string }): Promise<void> {
  const response = await fetch(`${API_URL}/api/v1/public/contact`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
  if (!response.ok) await throwProblem(response);
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

export type PublicLiveStatus = {
  classesRemainingToday: number;
  nextClass: ScheduleSession | null;
  classesWithFreeSlotsNow: number;
};

export async function fetchPublicLive(): Promise<PublicLiveStatus> {
  const response = await fetch(`${API_URL}/api/v1/public/live`);
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export type PublicConfig = {
  demoMode: boolean;
};

export async function fetchPublicConfig(): Promise<PublicConfig> {
  const response = await fetch(`${API_URL}/api/v1/public/config`);
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function demoLogin(role: "MEMBER" | "STAFF"): Promise<AuthResponse> {
  const response = await fetch(`${API_URL}/api/v1/auth/demo-login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ role }),
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

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

/** Solo clases futuras, la más próxima primero (el backend ya filtra y ordena). */
export async function fetchMyBookings(token: string): Promise<MyBooking[]> {
  const response = await fetch(`${API_URL}/api/v1/me/bookings`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

/** Clases ya pasadas con una reserva activa, la más reciente primero: "Historial" en Mis reservas. */
export async function fetchMyBookingsHistory(token: string): Promise<MyBooking[]> {
  const response = await fetch(`${API_URL}/api/v1/me/bookings/history`, {
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

export type Membership = {
  planCode: PlanCode;
  planName: string;
  priceCents: number;
  classesPerMonth: number | null;
  status: "ACTIVE" | "PAUSED" | "CANCELLED";
  startedAt: string;
  periodStart: string;
  periodEnd: string;
  aiTrainer: boolean;
  aiAssistant: boolean;
  pendingPlanCode: PlanCode | null;
  pendingPlanName: string | null;
};

export async function fetchMembership(token: string): Promise<Membership> {
  const response = await fetch(`${API_URL}/api/v1/me/membership`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

/** {@code direction}: CURRENT (la que ya tiene), UPGRADE (se puede subir ya) o DOWNGRADE (se programaría). */
export type PlanChangeOption = {
  planCode: PlanCode;
  planName: string;
  priceCents: number;
  direction: "CURRENT" | "UPGRADE" | "DOWNGRADE";
  amountDueTodayCents: number | null;
  effectiveAt: string | null;
};

export async function fetchPlanOptions(token: string): Promise<PlanChangeOption[]> {
  const response = await fetch(`${API_URL}/api/v1/me/membership/plan-options`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function upgradeMembershipPlan(token: string, planCode: PlanCode): Promise<Membership> {
  const response = await fetch(`${API_URL}/api/v1/me/membership/upgrade`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({ planCode }),
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function scheduleMembershipDowngrade(token: string, planCode: PlanCode): Promise<Membership> {
  const response = await fetch(`${API_URL}/api/v1/me/membership/schedule-downgrade`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({ planCode }),
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function cancelScheduledDowngrade(token: string): Promise<Membership> {
  const response = await fetch(`${API_URL}/api/v1/me/membership/cancel-downgrade`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export type SetProgress = { setIndex: number; done: boolean };

export type ExerciseProgress = {
  exerciseIndex: number;
  name: string;
  reps: string;
  weightKg: number | null;
  restSeconds: number | null;
  note: string | null;
  sets: SetProgress[];
};

export type TodayWorkout = {
  hasPlan: boolean;
  restDay: boolean;
  dayIndex: number;
  dayLabel: string | null;
  focus: string | null;
  exercises: ExerciseProgress[];
  /** Solo con restDay=true: qué toca el próximo día de entrenamiento. */
  nextLabel: string | null;
  nextFocus: string | null;
};

export async function fetchTodayWorkout(token: string): Promise<TodayWorkout> {
  const response = await fetch(`${API_URL}/api/v1/me/workout/today`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export type DayProgress = {
  dayIndex: number;
  label: string;
  focus: string | null;
  restDay: boolean;
  exercises: ExerciseProgress[];
};

export type WeekWorkout = {
  hasPlan: boolean;
  todayIndex: number;
  days: DayProgress[];
};

export async function fetchWeekWorkout(token: string): Promise<WeekWorkout> {
  const response = await fetch(`${API_URL}/api/v1/me/workout/week`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function logWorkoutSet(
  token: string,
  input: { dayIndex: number; exerciseIndex: number; setIndex: number; done: boolean },
): Promise<void> {
  const response = await fetch(`${API_URL}/api/v1/me/workout/logs`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify(input),
  });
  if (!response.ok) await throwProblem(response);
}

export type StreakDay = { dayIndex: number; label: string; trained: boolean; restDay: boolean; isToday: boolean };

export type Streak = {
  hasPlan: boolean;
  targetDays: number;
  completedDays: number;
  days: StreakDay[];
};

export async function fetchStreak(token: string): Promise<Streak> {
  const response = await fetch(`${API_URL}/api/v1/me/streak`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

// ---- Panel de gestión (Trozo 4) ----

export type MemberToContact = {
  userId: string;
  fullName: string;
  email: string;
  lastBookingAt: string | null;
};

export type AdminSummary = {
  activeMembers: number;
  todaySessions: number;
  todayBookedSeats: number;
  todayCapacity: number;
  monthlyRevenueCents: number | null;
  membersToContact: MemberToContact[];
};

export async function fetchAdminSummary(token: string): Promise<AdminSummary> {
  const response = await fetch(`${API_URL}/api/v1/admin/summary`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function fetchAdminSessions(
  token: string,
  params: { from?: string; to?: string } = {},
): Promise<ScheduleSession[]> {
  const query = new URLSearchParams();
  if (params.from) query.set("from", params.from);
  if (params.to) query.set("to", params.to);
  const response = await fetch(`${API_URL}/api/v1/admin/sessions?${query.toString()}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function createAdminSession(
  token: string,
  input: {
    name: string;
    discipline: string;
    description?: string;
    coachUserId?: string;
    room: string;
    startsAt: string;
    endsAt: string;
    capacity: number;
    repeatWeeks?: number;
  },
): Promise<ScheduleSession[]> {
  const response = await fetch(`${API_URL}/api/v1/admin/sessions`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify(input),
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function cancelAdminSession(token: string, sessionId: string): Promise<{ cancelledBookings: number }> {
  const response = await fetch(`${API_URL}/api/v1/admin/sessions/${sessionId}/cancel`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export type AdminMember = {
  userId: string;
  fullName: string;
  email: string;
  planCode: PlanCode | null;
  planName: string | null;
  membershipStatus: string | null;
  priceCents: number | null;
};

export async function fetchAdminMembers(token: string): Promise<AdminMember[]> {
  const response = await fetch(`${API_URL}/api/v1/admin/members`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function changeMemberPlan(token: string, memberId: string, planCode: PlanCode): Promise<AdminMember> {
  const response = await fetch(`${API_URL}/api/v1/admin/members/${memberId}/plan`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({ planCode }),
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export type AdminBooking = {
  bookingId: string;
  userId: string;
  memberName: string;
  sessionName: string;
  sessionStartsAt: string;
  status: BookingStatus;
  waitlistPosition: number | null;
};

export async function fetchAdminBookings(
  token: string,
  params: { from?: string; to?: string } = {},
): Promise<AdminBooking[]> {
  const query = new URLSearchParams();
  if (params.from) query.set("from", params.from);
  if (params.to) query.set("to", params.to);
  const response = await fetch(`${API_URL}/api/v1/admin/bookings?${query.toString()}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function cancelAdminBooking(token: string, bookingId: string): Promise<void> {
  const response = await fetch(`${API_URL}/api/v1/admin/bookings/${bookingId}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
}

export type AdminContactMessage = {
  id: string;
  name: string;
  email: string;
  message: string;
  createdAt: string;
  read: boolean;
};

export async function fetchAdminMessages(token: string): Promise<AdminContactMessage[]> {
  const response = await fetch(`${API_URL}/api/v1/admin/messages`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

export async function markAdminMessageRead(token: string, messageId: string): Promise<AdminContactMessage> {
  const response = await fetch(`${API_URL}/api/v1/admin/messages/${messageId}/read`, {
    method: "PATCH",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

// ---- Entrenador con IA (Trozo 5) ----

export type TrainerGoal = "hypertrophy" | "strength" | "fat_loss" | "general";
export type TrainerLevel = "beginner" | "intermediate" | "advanced";

export type GeneratedPlanExercise = {
  exerciseIndex: number;
  name: string;
  sets: number;
  reps: string;
  weightKg: number | null;
  restSeconds: number | null;
  note: string | null;
};

export type GeneratedPlanDay = {
  dayIndex: number;
  label: string;
  focus: string;
  restDay: boolean;
  exercises: GeneratedPlanExercise[];
};

export type GeneratedWeeklyPlan = { days: GeneratedPlanDay[] };

export async function generateWorkoutPlan(
  token: string,
  input: { goal: TrainerGoal; level: TrainerLevel; daysPerWeek: number; equipment: string[]; limitations: string[] },
): Promise<GeneratedWeeklyPlan> {
  const response = await fetch(`${API_URL}/api/v1/me/workout/generate`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify(input),
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}

// ---- Asistente de reservas con IA (Trozo 6) ----

export type AssistantChatRole = "user" | "assistant";

export type AssistantChatMessage = { role: AssistantChatRole; content: string };

/** Una clase o reserva real, construida por el servidor: lo que pinta la tarjeta del chat. */
export type AssistantScheduleOption = {
  id: string;
  name: string | null;
  discipline: string | null;
  startsAt: string | null;
  room: string | null;
  coachName: string | null;
  freeSlots: number | null;
};

export type AssistantPendingAction = {
  type: "book" | "cancel";
  sessionId: string | null;
  bookingId: string | null;
  summary: string;
  option: AssistantScheduleOption | null;
};

export type AssistantChatResult = {
  reply: string;
  pendingAction: AssistantPendingAction | null;
  options: AssistantScheduleOption[] | null;
  /** Ids de las tarjetas que acaba de mostrar: se reenvían tal cual en el siguiente mensaje
   * (contextOptionIds) para que "sí"/"vale"/"ok" con una sola pueda elegirla sin volver a
   * hablar con el modelo. */
  contextOptionIds: string[];
};

export async function assistantChat(
  token: string,
  input: {
    messages: AssistantChatMessage[];
    confirmAction?: AssistantPendingAction | null;
    contextOptionIds?: string[];
  },
): Promise<AssistantChatResult> {
  const response = await fetch(`${API_URL}/api/v1/me/assistant/chat`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({
      messages: input.messages,
      confirmAction: input.confirmAction ?? null,
      contextOptionIds: input.contextOptionIds ?? [],
    }),
  });
  if (!response.ok) await throwProblem(response);
  return response.json();
}
