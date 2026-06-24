package org.hyzionstudios.hyextras.util;

public final class ArithmeticExpression {

    private ArithmeticExpression() {}

    public static double evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("expression is empty");
        }
        Parser parser = new Parser(expression);
        double value = parser.parseExpression();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw new IllegalArgumentException("unexpected token at position " + parser.position());
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("expression result is not finite");
        }
        return value;
    }

    public static String format(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value is not finite");
        }
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value += parseTerm();
                } else if (match('-')) {
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parseFactor();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value *= parseFactor();
                } else if (match('/')) {
                    double divisor = parseFactor();
                    if (divisor == 0.0D) {
                        throw new IllegalArgumentException("division by zero");
                    }
                    value /= divisor;
                } else if (match('%')) {
                    double divisor = parseFactor();
                    if (divisor == 0.0D) {
                        throw new IllegalArgumentException("modulo by zero");
                    }
                    value %= divisor;
                } else {
                    return value;
                }
            }
        }

        private double parseFactor() {
            skipWhitespace();
            if (match('+')) {
                return parseFactor();
            }
            if (match('-')) {
                return -parseFactor();
            }
            if (match('(')) {
                double value = parseExpression();
                skipWhitespace();
                if (!match(')')) {
                    throw new IllegalArgumentException("missing ')' at position " + index);
                }
                return value;
            }
            return parseLiteral();
        }

        private double parseLiteral() {
            skipWhitespace();
            int start = index;
            while (!isAtEnd()) {
                char c = input.charAt(index);
                if ((c >= '0' && c <= '9') || c == '.') {
                    index++;
                } else {
                    break;
                }
            }
            if (start == index) {
                if (matchWord("true")) {
                    return 1.0D;
                }
                if (matchWord("false")) {
                    return 0.0D;
                }
                throw new IllegalArgumentException("expected number at position " + index);
            }
            try {
                return Double.parseDouble(input.substring(start, index));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid number at position " + start, e);
            }
        }

        private boolean match(char expected) {
            if (!isAtEnd() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private boolean matchWord(String expected) {
            if (input.regionMatches(true, index, expected, 0, expected.length())) {
                int end = index + expected.length();
                if (end == input.length() || !Character.isLetterOrDigit(input.charAt(end))) {
                    index = end;
                    return true;
                }
            }
            return false;
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean isAtEnd() {
            return index >= input.length();
        }

        private int position() {
            return index;
        }
    }
}
