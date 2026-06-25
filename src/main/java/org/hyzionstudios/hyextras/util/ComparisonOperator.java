package org.hyzionstudios.hyextras.util;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Operators shared by the player and TagNPC variable conditions, with their string aliases and a
 * single evaluation implementation so both conditions behave identically.
 *
 * <p>{@code actual} is the current variable value as a string ({@code null} when the variable is
 * absent); {@code expected} is the configured comparison value. Numeric operators coerce both sides
 * to {@code long} (decimal strings are truncated). {@code regex} is gated by a caller-supplied flag.
 */
public enum ComparisonOperator {
    EXISTS,
    NOT_EXISTS,
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_OR_EQUAL,
    LESS_OR_EQUAL,
    DIVISIBLE_BY,
    CONTAINS,
    REGEX;

    public static final Map<ComparisonOperator, String> ALIASES = Map.ofEntries(
            Map.entry(EXISTS, "exists"),
            Map.entry(NOT_EXISTS, "not_exists"),
            Map.entry(EQUALS, "equals"),
            Map.entry(NOT_EQUALS, "not_equals"),
            Map.entry(GREATER_THAN, "greater_than"),
            Map.entry(LESS_THAN, "less_than"),
            Map.entry(GREATER_OR_EQUAL, "greater_or_equal"),
            Map.entry(LESS_OR_EQUAL, "less_or_equal"),
            Map.entry(DIVISIBLE_BY, "divisible_by"),
            Map.entry(CONTAINS, "contains"),
            Map.entry(REGEX, "regex")
    );

    private static final Map<String, Pattern> REGEX_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_REGEX_LENGTH = 256;

    public boolean evaluate(@Nullable String actual, @Nullable String expected, boolean regexEnabled) {
        return switch (this) {
            case EXISTS -> actual != null;
            case NOT_EXISTS -> actual == null;
            case EQUALS -> Objects.equals(actual, expected);
            case NOT_EQUALS -> !Objects.equals(actual, expected);
            case GREATER_THAN -> toLong(actual) > toLong(expected);
            case LESS_THAN -> toLong(actual) < toLong(expected);
            case GREATER_OR_EQUAL -> toLong(actual) >= toLong(expected);
            case LESS_OR_EQUAL -> toLong(actual) <= toLong(expected);
            case DIVISIBLE_BY -> {
                long divisor = toLong(expected);
                yield divisor != 0 && toLong(actual) % divisor == 0;
            }
            case CONTAINS -> actual != null && expected != null && actual.contains(expected);
            case REGEX -> regexEnabled && matchesRegex(actual, expected);
        };
    }

    private static long toLong(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        String trimmed = value.trim();
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException primary) {
            try {
                return (long) Double.parseDouble(trimmed);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
    }

    private static boolean matchesRegex(@Nullable String input, @Nullable String regex) {
        if (input == null || regex == null || regex.isBlank() || regex.length() > MAX_REGEX_LENGTH) {
            return false;
        }
        try {
            return REGEX_CACHE.computeIfAbsent(regex, Pattern::compile).matcher(input).matches();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }
}
