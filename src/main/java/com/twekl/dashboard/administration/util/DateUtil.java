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
        String upper = filter.trim().toUpperCase();
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        return switch (upper) {
            case "TODAY" -> nowUtc.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "TOMORROW" -> nowUtc.plusDays(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "YESTERDAY" -> nowUtc.minusDays(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "24_HOURS", "LAST_24_HOURS" -> nowUtc.minusHours(24).toInstant();
            case "3_DAYS" -> nowUtc.minusDays(3).toInstant();
            case "LAST_7_DAYS", "7_DAYS", "WEEKLY" -> nowUtc.minusDays(7).toInstant();
            case "LAST_30_DAYS", "30_DAYS", "MONTHLY" -> nowUtc.minusDays(30).toInstant();
            case "2_MONTHS", "60_DAYS" -> nowUtc.minusDays(60).toInstant();
            case "THIS_MONTH" -> nowUtc.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "LAST_MONTH" -> nowUtc.minusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "THIS_YEAR", "YEARLY" -> nowUtc.withDayOfYear(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            default -> {
                if (upper.endsWith("_HOURS")) {
                    try {
                        long h = Long.parseLong(upper.replace("_HOURS", "").replace("LAST_", ""));
                        yield nowUtc.minusHours(h).toInstant();
                    } catch (Exception ignored) {}
                } else if (upper.endsWith("_DAYS")) {
                    try {
                        long d = Long.parseLong(upper.replace("_DAYS", "").replace("LAST_", ""));
                        yield nowUtc.minusDays(d).toInstant();
                    } catch (Exception ignored) {}
                } else if (upper.endsWith("_MONTHS")) {
                    try {
                        long m = Long.parseLong(upper.replace("_MONTHS", "").replace("LAST_", ""));
                        yield nowUtc.minusMonths(m).toInstant();
                    } catch (Exception ignored) {}
                }
                yield null;
            }
        };
    }
}

