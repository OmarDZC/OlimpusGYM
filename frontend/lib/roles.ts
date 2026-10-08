import type { PlanCode, Role } from "@/lib/api";

export const roleLabels: Record<Role, string> = {
  OWNER: "Dueño",
  STAFF: "Staff",
  MEMBER: "Socio",
};

/** LOGIC.md §3. Hasta que exista GET /public/plans, los precios se mantienen aquí a mano. */
export const planOptions: { code: PlanCode; label: string }[] = [
  { code: "BASICA", label: "Básica · 39 €/mes" },
  { code: "COMPLETA", label: "Completa · 59 €/mes" },
  { code: "ATLETA_PRO", label: "Atleta Pro · 79 €/mes" },
];

export function homeForRole(role: Role): string {
  return role === "MEMBER" ? "/cuenta" : "/admin";
}
