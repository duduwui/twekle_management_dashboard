package com.twekl.dashboard.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Custom Jackson Deserializer for Instant that accepts:
 * 1. Numeric epoch timestamps (milliseconds e.g. 1730000000000 or seconds)
 * 2. ISO-8601 strings (e.g. 2026-10-26T10:00:00Z)
 * 3. Standard date strings (e.g. 2026-10-26)
 */
public class FlexibleInstantDeserializer extends JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken currentToken = p.currentToken();

        // 1. Numeric timestamp (epoch seconds or millis)
        if (currentToken == JsonToken.VALUE_NUMBER_INT) {
            long val = p.getLongValue();
            if (val < 100_000_000_000L) {
                return Instant.ofEpochSecond(val);
            } else {
                return Instant.ofEpochMilli(val);
            }
        }

        // 2. String representation
        if (currentToken == JsonToken.VALUE_STRING) {
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            String clean = text.trim();

            // Try ISO-8601 Instant: e.g. 2026-10-26T10:30:00Z
            try {
                return Instant.parse(clean);
            } catch (Exception ignored) {}

            // Try date only: yyyy-MM-dd
            try {
                return LocalDate.parse(clean, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        .atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (Exception ignored) {}

            // Try slash date: yyyy/MM/dd
            try {
                return LocalDate.parse(clean, DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                        .atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (Exception ignored) {}

            // Try dd/MM/yyyy
            try {
                return LocalDate.parse(clean, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        .atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (Exception ignored) {}

            // Try numeric in string
            try {
                long epoch = Long.parseLong(clean);
                if (epoch < 100_000_000_000L) {
                    return Instant.ofEpochSecond(epoch);
                } else {
                    return Instant.ofEpochMilli(epoch);
                }
            } catch (Exception ignored) {}
        }

        return null;
    }
}
