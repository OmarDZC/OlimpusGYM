"use client";

import { Moon, Sun } from "lucide-react";
import { useEffect, useState } from "react";

const STORAGE_KEY = "ogym-theme";

type Theme = "dark" | "light";

function readCurrentTheme(): Theme {
  return document.documentElement.getAttribute("data-theme") === "light" ? "light" : "dark";
}

/** Interruptor sol/luna. Guarda la preferencia en localStorage (con try/catch). */
export function ThemeToggle() {
  const [theme, setTheme] = useState<Theme | null>(null);

  useEffect(() => {
    setTheme(readCurrentTheme());
  }, []);

  function toggle() {
    const next: Theme = theme === "light" ? "dark" : "light";
    setTheme(next);
    if (next === "light") {
      document.documentElement.setAttribute("data-theme", "light");
    } else {
      document.documentElement.removeAttribute("data-theme");
    }
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // localStorage no disponible (p. ej. ventana privada): el tema no persiste, pero no rompe nada.
    }
  }

  // Antes de montar no sabemos el tema real del documento: no renderizamos el icono para evitar parpadeo.
  if (theme === null) {
    return <span className="inline-block h-9 w-9" aria-hidden="true" />;
  }

  const isLight = theme === "light";

  return (
    <button
      type="button"
      onClick={toggle}
      aria-pressed={isLight}
      aria-label={isLight ? "Cambiar a tema oscuro" : "Cambiar a tema claro"}
      className="inline-flex h-9 w-9 items-center justify-center rounded-control border border-border bg-surface text-text-muted transition-colors hover:text-text"
    >
      {isLight ? <Moon size={18} /> : <Sun size={18} />}
    </button>
  );
}
