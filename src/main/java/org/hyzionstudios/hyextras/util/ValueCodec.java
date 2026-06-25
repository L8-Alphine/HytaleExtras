package org.hyzionstudios.hyextras.util;

import javax.annotation.Nullable;

/**
 * Serializes loosely-typed variable values to a compact, type-tagged string form so they
 * round-trip through {@link java.util.Properties} files without a JSON dependency.
 *
 * <p>Encoding is a one-letter type tag, a colon, then the value:
 * <ul>
 *   <li>{@code L:42} — integral, stored as {@code long}</li>
 *   <li>{@code D:1.5} — floating point, stored as {@code double}</li>
 *   <li>{@code B:true} — boolean</li>
 *   <li>{@code S:text} — string / fallback</li>
 * </ul>
 * Unknown or untagged input decodes to the raw string for forward/backward tolerance.
 */
public final class ValueCodec {

    private ValueCodec() {}

    /** Encodes a variable value to its type-tagged string form. {@code null} encodes as an empty string. */
    public static String encode(@Nullable Object value) {
        if (value == null) {
            return "S:";
        }
        if (value instanceof Long || value instanceof Integer
                || value instanceof Short || value instanceof Byte) {
            return "L:" + ((Number) value).longValue();
        }
        if (value instanceof Double || value instanceof Float) {
            return "D:" + ((Number) value).doubleValue();
        }
        if (value instanceof Boolean b) {
            return "B:" + b;
        }
        return "S:" + value;
    }

    /** Decodes a type-tagged string back to a typed value. Untagged/invalid input returns the raw string. */
    public static Object decode(@Nullable String encoded) {
        if (encoded == null) {
            return "";
        }
        if (encoded.length() >= 2 && encoded.charAt(1) == ':') {
            String body = encoded.substring(2);
            switch (encoded.charAt(0)) {
                case 'L':
                    try {
                        return Long.parseLong(body);
                    } catch (NumberFormatException ignored) {
                        return body;
                    }
                case 'D':
                    try {
                        return Double.parseDouble(body);
                    } catch (NumberFormatException ignored) {
                        return body;
                    }
                case 'B':
                    return Boolean.parseBoolean(body);
                case 'S':
                    return body;
                default:
                    break;
            }
        }
        return encoded;
    }
}
