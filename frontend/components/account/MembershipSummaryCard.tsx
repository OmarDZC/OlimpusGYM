"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import * as api from "@/lib/api";
import type { Membership } from "@/lib/api";

function formatPrice(cents: number): string {
  return (cents / 100).toLocaleString("es-ES", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/** Resumen de la cuota en Inicio (referencia v2/mi-cuenta), con enlace al detalle en /cuenta/cuota. */
export function MembershipSummaryCard() {
  const { token } = useAuth();
  const [membership, setMembership] = useState<Membership | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) return;
    api
      .fetchMembership(token)
      .then(setMembership)
      .catch(() => setMembership(null))
      .finally(() => setLoading(false));
  }, [token]);

  if (loading || !membership) return null;

  return (
    <Card className="flex flex-wrap items-center justify-between gap-4">
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">Mi cuota de socio</p>
        <p className="mt-1 font-display text-xl font-bold uppercase tracking-tight">{membership.planName}</p>
      </div>
      <div className="flex items-center gap-4">
        <p className="font-display text-2xl font-bold tabular-nums text-accent-text">
          {formatPrice(membership.priceCents)} € <span className="text-sm font-sans text-text-muted">/mes</span>
        </p>
        <Link href="/cuenta/cuota" className="text-sm text-accent-text hover:underline">
          Ver detalle →
        </Link>
      </div>
    </Card>
  );
}
