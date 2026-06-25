package org.hyzionstudios.hyextras.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ValueCodecTest {

    @Test
    void longRoundTrips() {
        String encoded = ValueCodec.encode(42L);
        assertEquals("L:42", encoded);
        Object decoded = ValueCodec.decode(encoded);
        assertInstanceOf(Long.class, decoded);
        assertEquals(42L, decoded);
    }

    @Test
    void integerEncodesAsLong() {
        assertEquals("L:7", ValueCodec.encode(7));
        assertEquals(7L, ValueCodec.decode("L:7"));
    }

    @Test
    void doubleRoundTrips() {
        Object decoded = ValueCodec.decode(ValueCodec.encode(1.5d));
        assertInstanceOf(Double.class, decoded);
        assertEquals(1.5d, decoded);
    }

    @Test
    void booleanRoundTrips() {
        assertEquals("B:true", ValueCodec.encode(true));
        assertEquals(Boolean.TRUE, ValueCodec.decode("B:true"));
    }

    @Test
    void stringRoundTrips() {
        assertEquals("S:spawn", ValueCodec.encode("spawn"));
        assertEquals("spawn", ValueCodec.decode("S:spawn"));
    }

    @Test
    void nullEncodesToEmptyString() {
        assertEquals("S:", ValueCodec.encode(null));
        assertEquals("", ValueCodec.decode("S:"));
    }

    @Test
    void untaggedInputDecodesAsRawString() {
        assertEquals("legacyValue", ValueCodec.decode("legacyValue"));
    }

    @Test
    void malformedNumberFallsBackToRawString() {
        assertEquals("notANumber", ValueCodec.decode("L:notANumber"));
    }
}
