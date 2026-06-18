package org.hyzionstudios.hyextras.codec;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.simple.*;

import java.util.Map;

/**
 * Factory helpers for {@link KeyedCodec} instances used in HyExtras effect/condition CODECs.
 *
 * <p>The full BuilderCodec pattern:</p>
 * <pre>{@code
 * public static final BuilderCodec<MyAction> CODEC = BuilderCodec.builder(
 *         MyAction.class, MyAction::new, TriggerEffect.BASE_CODEC)
 *     .append(CodecHelper.string("myField"), MyAction::setMyField, MyAction::getMyField).add()
 *     .append(CodecHelper.optString("optField"), MyAction::setOptField, MyAction::getOptField).add()
 *     .build();
 * }</pre>
 *
 * <p>Key points:</p>
 * <ul>
 *   <li>{@code .append(...).add()} — the terminal on FieldBuilder is {@code .add()}, returns the Builder</li>
 *   <li>Required field: {@code new KeyedCodec<>(key, codec)} — third arg defaults to required=true</li>
 *   <li>Optional field: {@code new KeyedCodec<>(key, codec, false)} — false = not required</li>
 *   <li>Primitive codecs: {@code com.hypixel.hytale.codec.codecs.simple.*}</li>
 * </ul>
 */
public final class CodecHelper {

    private CodecHelper() {}

    /** Required String field. */
    public static KeyedCodec<String> string(String key) {
        return new KeyedCodec<>(key, new StringCodec());
    }

    /** Optional String field (omitted from JSON is allowed). */
    public static KeyedCodec<String> optString(String key) {
        return new KeyedCodec<>(key, new StringCodec(), false);
    }

    /** Required int field. */
    public static KeyedCodec<Integer> integer(String key) {
        return new KeyedCodec<>(key, new IntegerCodec());
    }

    /** Optional int field. */
    public static KeyedCodec<Integer> optInteger(String key) {
        return new KeyedCodec<>(key, new IntegerCodec(), false);
    }

    /** Required long field. */
    public static KeyedCodec<Long> longField(String key) {
        return new KeyedCodec<>(key, new LongCodec());
    }

    /** Optional long field. */
    public static KeyedCodec<Long> optLong(String key) {
        return new KeyedCodec<>(key, new LongCodec(), false);
    }

    /** Required float field. */
    public static KeyedCodec<Float> floatField(String key) {
        return new KeyedCodec<>(key, new FloatCodec());
    }

    /** Optional float field. */
    public static KeyedCodec<Float> optFloat(String key) {
        return new KeyedCodec<>(key, new FloatCodec(), false);
    }

    /** Required boolean field. */
    public static KeyedCodec<Boolean> bool(String key) {
        return new KeyedCodec<>(key, Codec.BOOLEAN);
    }

    /** Optional boolean field. */
    public static KeyedCodec<Boolean> optBool(String key) {
        return new KeyedCodec<>(key, Codec.BOOLEAN, false);
    }

    /** Required enum field. Use aliases to keep old JSON values compatible. */
    public static <T extends Enum<T>> KeyedCodec<T> enumField(
            String key, Class<T> enumClass, Map<T, String> aliases) {
        return new KeyedCodec<>(key, enumCodec(enumClass, aliases));
    }

    /** Optional enum field. Use aliases to keep old JSON values compatible. */
    public static <T extends Enum<T>> KeyedCodec<T> optEnum(
            String key, Class<T> enumClass, Map<T, String> aliases) {
        return new KeyedCodec<>(key, enumCodec(enumClass, aliases), false);
    }

    private static <T extends Enum<T>> Codec<T> enumCodec(Class<T> enumClass, Map<T, String> aliases) {
        EnumCodec<T> codec = new EnumCodec<>(enumClass);
        for (Map.Entry<T, String> entry : aliases.entrySet()) {
            codec.documentKey(entry.getKey(), entry.getValue());
        }
        return codec;
    }
}
