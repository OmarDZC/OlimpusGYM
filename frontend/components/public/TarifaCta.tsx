"use client";

import { useAuth } from "@/components/auth/AuthProvider";
import { Button } from "@/components/ui/Button";

/**
 * Botón de alta (Trozo 7a): "Hazte socio" lleva al registro si no hay sesión; con sesión de
 * socio, lleva a cambiar de tarifa en vez de registrar una cuenta nueva (Trozo 9b).
 */
export function TarifaCta({ className }: { className?: string }) {
  const { status, user } = useAuth();

  if (status === "signed-in" && user?.role === "MEMBER") {
    return (
      <Button href="/cuenta/cuota/cambiar-tarifa" variant="secondary" className={className}>
        Cambiar de tarifa
      </Button>
    );
  }

  return (
    <Button href="/acceso?modo=registro" variant="primary" className={className}>
      Hazte socio
    </Button>
  );
}
