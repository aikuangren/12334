package com.fiftytalk.carddraw.service;

import com.fiftytalk.carddraw.config.CardDrawProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class BusinessClock {
    private static final int DEFAULT_RESET_HOUR = 5;
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private final CardDrawProperties properties;

    public BusinessClock(CardDrawProperties properties) {
        this.properties = properties;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(BEIJING);
    }

    public LocalDate businessDate() {
        LocalDateTime now = now();
        if (now.getHour() < businessResetHour()) {
            return now.toLocalDate().minusDays(1);
        }
        return now.toLocalDate();
    }

    public Instant nextResetAt(LocalDate quotaDate) {
        return quotaDate.plusDays(1)
                .atTime(businessResetHour(), 0)
                .atZone(BEIJING)
                .toInstant();
    }

    public LocalDateTime businessDayStart(LocalDate quotaDate) {
        return quotaDate.atTime(businessResetHour(), 0);
    }

    public LocalDateTime businessDayEnd(LocalDate quotaDate) {
        return quotaDate.plusDays(1).atTime(businessResetHour(), 0);
    }

    private int businessResetHour() {
        int configuredHour = properties.getBusinessResetHour();
        return configuredHour >= 0 && configuredHour <= 23
                ? configuredHour
                : DEFAULT_RESET_HOUR;
    }
}
