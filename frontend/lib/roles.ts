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

/**
 * Pantallas del socio que llevan la barra inferior en móvil: todo `/cuenta/*` más `/horario` y
 * `/entrenador-ia`, que viven fuera de `/cuenta` (son también públicas) pero son parte de la
 * misma zona para quien ha iniciado sesión como socio. Única fuente de verdad para esa lista:
 * la usan tanto las páginas que pintan la barra como `TopNav` para ocultar el menú de móvil ahí.
 */
const MEMBER_AREA_EXTRA_PATHS = ["/horario", "/entrenador-ia"];

export function isMemberAreaPath(pathname: string): boolean {
  return pathname.startsWith("/cuenta") || MEMBER_AREA_EXTRA_PATHS.includes(pathname);
}
