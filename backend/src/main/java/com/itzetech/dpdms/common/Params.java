package com.itzetech.dpdms.common;

import java.time.LocalDate;

/** Reads optional query parameters. */
public final class Params {

    private Params() {
    }

    /** The enum value for a query string, {@code null} when absent; an unknown value is a 400 {@code BAD_REQUEST}. */
    public static <E extends Enum<E>> E enumValue(Class<E> type, String raw, String name) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("BAD_REQUEST", "The value for \"" + name + "\" is not valid.");
        }
    }

    public static void checkRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("INVALID_RANGE", "The start date must not be after the end date.");
        }
    }
}
