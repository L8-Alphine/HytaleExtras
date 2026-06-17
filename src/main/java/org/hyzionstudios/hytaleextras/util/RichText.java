package org.hyzionstudios.hytaleextras.util;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Converts simple trigger-volume text into Hytale formatted messages.
 *
 * <p>Supported color/style codes:
 * {@code &0-&9}, {@code &a-&f}, {@code &#RRGGBB}, {@code &l}, {@code &o},
 * {@code &n}, {@code &r}, and {@code &&} for a literal ampersand.
 */
public final class RichText {

    private static final String[] LEGACY_COLORS = {
            "#000000", "#0000AA", "#00AA00", "#00AAAA",
            "#AA0000", "#AA00AA", "#FFAA00", "#AAAAAA",
            "#555555", "#5555FF", "#55FF55", "#55FFFF",
            "#FF5555", "#FF55FF", "#FFFF55", "#FFFFFF"
    };

    private RichText() {}

    public static Message toMessage(String text) {
        return new Message(toFormattedMessage(text));
    }

    public static FormattedMessage toFormattedMessage(String text) {
        if (text == null || text.isEmpty()) {
            return raw("");
        }

        List<FormattedMessage> parts = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        Style currentStyle = new Style();
        boolean sawFormatting = false;

        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c != '&' || i + 1 >= text.length()) {
                currentText.append(c);
                i++;
                continue;
            }

            char code = Character.toLowerCase(text.charAt(i + 1));
            if (code == '&') {
                currentText.append('&');
                i += 2;
                continue;
            }

            String color = legacyColor(code);
            if (color != null) {
                flush(parts, currentText, currentStyle);
                currentStyle = currentStyle.withColor(color);
                sawFormatting = true;
                i += 2;
                continue;
            }

            if (code == '#' && i + 7 < text.length()) {
                String hex = text.substring(i + 2, i + 8);
                if (isHexColor(hex)) {
                    flush(parts, currentText, currentStyle);
                    currentStyle = currentStyle.withColor("#" + hex.toUpperCase(Locale.ROOT));
                    sawFormatting = true;
                    i += 8;
                    continue;
                }
            }

            if (code == 'l' || code == 'o' || code == 'n' || code == 'r') {
                flush(parts, currentText, currentStyle);
                currentStyle = switch (code) {
                    case 'l' -> currentStyle.withBold();
                    case 'o' -> currentStyle.withItalic();
                    case 'n' -> currentStyle.withUnderlined();
                    default -> new Style();
                };
                sawFormatting = true;
                i += 2;
                continue;
            }

            currentText.append(c);
            i++;
        }

        flush(parts, currentText, currentStyle);

        if (parts.isEmpty()) {
            return raw("");
        }

        if (!sawFormatting && parts.size() == 1) {
            return parts.getFirst();
        }

        FormattedMessage root = new FormattedMessage();
        root.children = parts.toArray(FormattedMessage[]::new);
        return root;
    }

    private static void flush(List<FormattedMessage> parts, StringBuilder currentText, Style style) {
        if (currentText.length() == 0) {
            return;
        }
        FormattedMessage part = raw(currentText.toString());
        part.color = style.color;
        part.bold = style.bold ? Boolean.TRUE : null;
        part.italic = style.italic ? Boolean.TRUE : null;
        part.underlined = style.underlined ? Boolean.TRUE : null;
        parts.add(part);
        currentText.setLength(0);
    }

    private static FormattedMessage raw(String text) {
        FormattedMessage message = new FormattedMessage();
        message.rawText = text;
        return message;
    }

    private static String legacyColor(char code) {
        int index;
        if (code >= '0' && code <= '9') {
            index = code - '0';
        } else if (code >= 'a' && code <= 'f') {
            index = 10 + code - 'a';
        } else {
            return null;
        }
        return LEGACY_COLORS[index];
    }

    private static boolean isHexColor(String value) {
        if (value.length() != 6) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) == -1) {
                return false;
            }
        }
        return true;
    }

    private record Style(String color, boolean bold, boolean italic, boolean underlined) {
        private Style() {
            this(null, false, false, false);
        }

        private Style withColor(String color) {
            return new Style(color, bold, italic, underlined);
        }

        private Style withBold() {
            return new Style(color, true, italic, underlined);
        }

        private Style withItalic() {
            return new Style(color, bold, true, underlined);
        }

        private Style withUnderlined() {
            return new Style(color, bold, italic, true);
        }
    }
}
