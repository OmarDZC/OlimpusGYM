const STORAGE_KEY = "ogym-auth";

/** Guarda solo el token: el resto de datos del usuario se recarga con GET /auth/me. */
export function readStoredToken(): string | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as { token?: string };
    return parsed.token ?? null;
  } catch {
    return null;
  }
}

export function writeStoredToken(token: string): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token }));
  } catch {
    // localStorage no disponible (p. ej. ventana privada): la sesión no persiste al recargar.
  }
}

export function clearStoredToken(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Nada que limpiar si no hay localStorage.
  }
}
