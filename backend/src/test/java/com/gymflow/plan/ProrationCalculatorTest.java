package com.gymflow.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** No necesita Docker ni Spring: corre siempre, también en local. */
class ProrationCalculatorTest {

    private static final Instant PERIOD_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-01-31T00:00:00Z");

    @Test
    void chargesTheFullDifferenceAtTheStartOfThePeriod() {
        int amount = ProrationCalculator.upgradeAmountCents(5900, 7900, PERIOD_START, PERIOD_START, PERIOD_END);

        assertEquals(2000, amount);
    }

    @Test
    void chargesHalfTheDifferenceAtTheMidpoint() {
        Instant midpoint = PERIOD_START.plusSeconds(15L * 24 * 60 * 60);

        int amount = ProrationCalculator.upgradeAmountCents(5900, 7900, midpoint, PERIOD_START, PERIOD_END);

        assertEquals(1000, amount);
    }

    @Test
    void chargesAtLeastOneDayOnTheLastDayOfThePeriod() {
        Instant lastHour = PERIOD_END.minusSeconds(60 * 60);

        int amount = ProrationCalculator.upgradeAmountCents(5900, 7900, lastHour, PERIOD_START, PERIOD_END);

        assertEquals(67, amount);
    }

    @Test
    void proratesATwoTierUpgradeTheSameWayAsAnAdjacentOne() {
        int amount = ProrationCalculator.upgradeAmountCents(4900, 7900, PERIOD_START, PERIOD_START, PERIOD_END);

        assertEquals(3000, amount);
    }
}
