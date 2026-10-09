package com.gymflow.plan;

public enum PlanChangeType {
    /** Subida de tarifa, al instante, con el cobro prorrateado. */
    UPGRADE,
    /** Bajada de tarifa, programada para la siguiente renovación. */
    DOWNGRADE_SCHEDULED,
    /** Una bajada programada se deshace antes de que llegue la renovación. */
    DOWNGRADE_CANCELLED,
    /** La renovación llega y aplica una bajada que estaba programada. */
    DOWNGRADE_APPLIED,
    /** Renovación periódica normal: mismo plan, se cobra el precio completo. */
    RENEWAL
}
