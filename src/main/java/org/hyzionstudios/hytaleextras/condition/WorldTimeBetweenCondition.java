package org.hyzionstudios.hytaleextras.condition;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import org.hyzionstudios.hytaleextras.HytaleextrasPlugin;
import org.hyzionstudios.hytaleextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;

import java.util.logging.Level;

/**
 * Passes if the current world hour (0–23) is within [{@code fromHour}, {@code toHour}].
 *
 * <p>If {@code fromHour} &gt; {@code toHour} the range wraps midnight — useful for night-time
 * checks, e.g. {@code fromHour: 22, toHour: 5} spans 22:00 → 05:00.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "world_time_between", "fromHour": 6,  "toHour": 18 }   // daytime
 * { "type": "world_time_between", "fromHour": 22, "toHour": 5  }   // night (wraps midnight)
 * }</pre>
 *
 * <p>World hour is read from {@link WorldTimeResource#getCurrentHour()} via
 * {@link TriggerVolumeApiAdapter#getWorldHour}.
 */
public class WorldTimeBetweenCondition extends TriggerCondition {

    public static final BuilderCodec<WorldTimeBetweenCondition> CODEC = BuilderCodec.builder(
                    WorldTimeBetweenCondition.class, WorldTimeBetweenCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.integer("FromHour"),
                    WorldTimeBetweenCondition::setFromHour,
                    WorldTimeBetweenCondition::getFromHour).add()
            .append(CodecHelper.integer("ToHour"),
                    WorldTimeBetweenCondition::setToHour,
                    WorldTimeBetweenCondition::getToHour).add()
            .build();

    private int fromHour;
    private int toHour;

    public WorldTimeBetweenCondition() {}

    @Override
    public boolean test(TriggerContext ctx) {
        try {
            int hour = TriggerVolumeApiAdapter.getWorldHour(ctx.getStore());
            if (hour < 0) {
                HytaleextrasPlugin.get().getLogger()
                        .at(Level.WARNING)
                        .log("[world_time_between] world time unavailable — passing by default");
                return true;
            }
            if (fromHour <= toHour) {
                return hour >= fromHour && hour <= toHour;
            }
            // Wrap-around: e.g. fromHour=22, toHour=5 covers 22:00 → 05:00
            return hour >= fromHour || hour <= toHour;
        } catch (Exception e) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[world_time_between] condition test failed");
            return false;
        }
    }

    public int getFromHour() { return fromHour; }
    public void setFromHour(int fromHour) { this.fromHour = fromHour; }

    public int getToHour() { return toHour; }
    public void setToHour(int toHour) { this.toHour = toHour; }
}
