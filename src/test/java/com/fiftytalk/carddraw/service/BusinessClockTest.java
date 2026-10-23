package com.fiftytalk.carddraw.service;

import com.fiftytalk.carddraw.config.CardDrawProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessClockTest {

    @Test
    void calculatesNextResetFromQuotaDate() {
        CardDrawProperties properties = new CardDrawProperties();
        properties.setBusinessResetHour(5);
        BusinessClock clock = new BusinessClock(properties);

        Instant nextResetAt = clock.nextResetAt(LocalDate.of(2026, 6, 17));

        assertThat(nextResetAt).isEqualTo(Instant.parse("2026-06-17T21:00:00Z"));
    }

    @Test
    void fallsBackToDefaultHourForInvalidConfiguration() {
        CardDrawProperties properties = new CardDrawProperties();
        BusinessClock clock = new BusinessClock(properties);

        properties.setBusinessResetHour(-1);
        assertThat(clock.nextResetAt(LocalDate.of(2026, 6, 17)))
                .isEqualTo(Instant.parse("2026-06-17T21:00:00Z"));

        properties.setBusinessResetHour(24);
        assertThat(clock.nextResetAt(LocalDate.of(2026, 6, 17)))
                .isEqualTo(Instant.parse("2026-06-17T21:00:00Z"));
    }
}
