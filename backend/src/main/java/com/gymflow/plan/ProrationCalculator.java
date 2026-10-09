package com.gymflow.plan;

import java.time.Instant;

/**
 * Importe a pagar hoy al subir de tarifa (Trozo 9b, LOGIC.md §12): la diferencia de precio
 * prorrateada por los días que quedan del periodo actual. Todo en milisegundos y céntimos
 * enteros, nunca con coma flotante: una suscripción es dinero, no algo donde valga "casi".
 */
public final class ProrationCalculator {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private ProrationCalculator() {
    }

    /**
     * @param currentPriceCents precio de la tarifa actual, en céntimos
     * @param newPriceCents     precio de la tarifa nueva (tiene que ser mayor; lo valida quien llama)
     * @param now                instante del cambio
     * @param periodStart        inicio del periodo actual
     * @param periodEnd          fin del periodo actual (la próxima renovación)
     * @return importe a pagar hoy, en céntimos, redondeado al céntimo más cercano (mitad hacia arriba)
     */
    public static int upgradeAmountCents(
            int currentPriceCents, int newPriceCents, Instant now, Instant periodStart, Instant periodEnd) {
        long totalDays = exactDays(periodStart, periodEnd);
        if (totalDays <= 0) {
            return Math.max(0, newPriceCents - currentPriceCents);
        }

        long remainingDays = ceilDays(now, periodEnd);
        if (remainingDays < 0) {
            remainingDays = 0;
        } else if (remainingDays > totalDays) {
            remainingDays = totalDays;
        }

        long priceDiffCents = (long) newPriceCents - currentPriceCents;
        return Math.toIntExact(roundHalfUp(priceDiffCents * remainingDays, totalDays));
    }

    /** Días completos entre dos instantes (el periodo siempre es un mes natural en UTC: exacto). */
    private static long exactDays(Instant from, Instant to) {
        return (to.toEpochMilli() - from.toEpochMilli()) / DAY_MILLIS;
    }

    /**
     * Días que quedan, contando como un día entero cualquier fracción de día que falte: cambiar
     * de tarifa el último día del periodo, por poco que quede, sigue contando como 1 día, no 0
     * (es el mismo criterio que ya usa, p. ej., Stripe para prorratear).
     */
    private static long ceilDays(Instant from, Instant to) {
        long remainingMillis = to.toEpochMilli() - from.toEpochMilli();
        if (remainingMillis <= 0) {
            return 0;
        }
        return (remainingMillis + DAY_MILLIS - 1) / DAY_MILLIS;
    }

    /** Redondeo al entero más cercano, mitad hacia arriba, con división entera (sin coma flotante). */
    private static long roundHalfUp(long numerator, long denominator) {
        if (numerator >= 0) {
            return (numerator + denominator / 2) / denominator;
        }
        return -((-numerator + denominator / 2) / denominator);
    }
}
