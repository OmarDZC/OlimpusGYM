"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import type { Role } from "@/lib/api";
import { homeForRole } from "@/lib/roles";

/**
 * Protege una sección por rol en el cliente. La autorización real se comprueba
 * siempre en el servidor (CLAUDE.md): esto solo evita que alguien sin el rol
 * adecuado vea la pantalla, no reemplaza los 403 del backend.
 */
export function RequireRole({ roles, children }: { roles: Role[]; children: React.ReactNode }) {
  const { status, user } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "loading") return;
    if (status === "signed-out") {
      router.replace("/acceso");
      return;
    }
    if (user && !roles.includes(user.role)) {
      router.replace(homeForRole(user.role));
    }
    // roles se construye de nuevo en cada render si se pasa inline: solo comparamos por contenido vía status/user.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, user, router]);

  if (status === "loading") {
    return (
      <div className="flex min-h-[50vh] items-center justify-center text-sm text-text-muted">
        Comprobando tu sesión...
      </div>
    );
  }

  if (status === "signed-out" || !user || !roles.includes(user.role)) {
    return null;
  }

  return <>{children}</>;
}
