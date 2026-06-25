package org.hyzionstudios.hyextras.util;

import org.junit.jupiter.api.Test;

import static org.hyzionstudios.hyextras.util.ComparisonOperator.CONTAINS;
import static org.hyzionstudios.hyextras.util.ComparisonOperator.DIVISIBLE_BY;
import static org.hyzionstudios.hyextras.util.ComparisonOperator.EQUALS;
import static org.hyzionstudios.hyextras.util.ComparisonOperator.EXISTS;
import static org.hyzionstudios.hyextras.util.ComparisonOperator.GREATER_OR_EQUAL;
import static org.hyzionstudios.hyextras.util.ComparisonOperator.GREATER_THAN;
import static org.hyzionstudios.hyextras.util.ComparisonOperator.LESS_OR_EQUAL;
import static org.hyzionstudios.hyextras.util.ComparisonOperator.NOT_EXISTS;
import static org.hyzionstudios.hyextras.util.ComparisonOperator.REGEX;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComparisonOperatorTest {

    @Test
    void existence() {
        assertTrue(EXISTS.evaluate("x", null, true));
        assertFalse(EXISTS.evaluate(null, null, true));
        assertTrue(NOT_EXISTS.evaluate(null, null, true));
    }

    @Test
    void equality() {
        assertTrue(EQUALS.evaluate("spawn", "spawn", true));
        assertFalse(EQUALS.evaluate("spawn", "lobby", true));
    }

    @Test
    void numericComparisons() {
        assertTrue(GREATER_THAN.evaluate("5", "3", true));
        assertFalse(GREATER_THAN.evaluate("3", "3", true));
        assertTrue(GREATER_OR_EQUAL.evaluate("3", "3", true));
        assertTrue(LESS_OR_EQUAL.evaluate("3", "3", true));
    }

    @Test
    void decimalStringsCoerceToLong() {
        // "5.9" truncates to 5, matching Number.longValue() semantics
        assertTrue(GREATER_OR_EQUAL.evaluate("5.9", "5", true));
        assertFalse(GREATER_THAN.evaluate("5.9", "5", true));
    }

    @Test
    void divisibleBy() {
        assertTrue(DIVISIBLE_BY.evaluate("10", "5", true));
        assertFalse(DIVISIBLE_BY.evaluate("10", "3", true));
        // divisor of zero never matches (no ArithmeticException)
        assertFalse(DIVISIBLE_BY.evaluate("10", "0", true));
    }

    @Test
    void contains() {
        assertTrue(CONTAINS.evaluate("hello world", "world", true));
        assertFalse(CONTAINS.evaluate("hello", "world", true));
        assertFalse(CONTAINS.evaluate(null, "world", true));
    }

    @Test
    void regexRespectsEnableFlag() {
        assertTrue(REGEX.evaluate("abc123", "[a-z]+\\d+", true));
        assertFalse(REGEX.evaluate("abc123", "[a-z]+\\d+", false));
        assertFalse(REGEX.evaluate("abc123", "(", true)); // invalid pattern, no crash
    }
}
