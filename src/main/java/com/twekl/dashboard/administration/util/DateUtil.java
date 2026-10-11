package com.twekl.dashboard.administration.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Enterprise date and temporal utility.
 * Enforces UTC Instant and calendar-aligned boundary standards.
 */
public final class DateUtil {

    private DateUtil() {}

    /**
     * Resolves the start cutoff Instant for a named time period filter.
     * All boundaries are aligned to UTC calendar starts (midnight 00:00:00).
     */
    public static Instant getFilterCutoff(String filter) {
        if (filter == null || filter.trim().isEmpty() || "ALL".equalsIgnoreCase(filter)) {
            return null;
        }
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        return switch (filter.toUpperCase()) {
            case "TODAY" -> nowUtc.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "TOMORROW" -> nowUtc.plusDays(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "YESTERDAY" -> nowUtc.minusDays(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "LAST_7_DAYS", "7_DAYS", "WEEKLY" -> nowUtc.minusDays(7).toInstant();
            case "LAST_30_DAYS", "30_DAYS", "MONTHLY" -> nowUtc.minusDays(30).toInstant();
            case "THIS_MONTH" -> nowUtc.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "LAST_MONTH" -> nowUtc.minusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "THIS_YEAR", "YEARLY" -> nowUtc.withDayOfYear(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            default -> null;
        };
    }
}

