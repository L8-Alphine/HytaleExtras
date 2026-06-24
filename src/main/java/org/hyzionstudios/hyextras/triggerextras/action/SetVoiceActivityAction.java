package org.hyzionstudios.hyextras.triggerextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.modules.voice.VoiceModule;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.codec.CodecHelper;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SetVoiceActivityAction extends TriggerEffect {

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    public static final BuilderCodec<SetVoiceActivityAction> CODEC = BuilderCodec.builder(
                    SetVoiceActivityAction.class, SetVoiceActivityAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.enumField("Mode", Mode.class, Mode.ALIASES),
                    SetVoiceActivityAction::setMode,
                    SetVoiceActivityAction::getMode).add()
            .append(CodecHelper.optString("TargetPlayer"),
                    SetVoiceActivityAction::setTargetPlayer,
                    SetVoiceActivityAction::getTargetPlayer).add()
            .build();

    private Mode mode;
    @Nullable private String targetPlayer;

    public SetVoiceActivityAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return;
        }
        try {
            if (mode == null) {
                warn("mode is empty");
                return;
            }
            VoiceModule voiceModule = VoiceModule.get();
            if (voiceModule == null) {
                warnOnce("missing-module", "VoiceModule is unavailable");
                return;
            }
            if (!voiceModule.isVoiceEnabled()) {
                warnOnce("disabled", "voice is globally disabled");
                return;
            }
            UUID target = resolveTarget(ctx);
            if (target == null) {
                warn("target player is unavailable");
                return;
            }
            switch (mode) {
                case MUTE -> voiceModule.mutePlayer(target);
                case UNMUTE -> voiceModule.unmutePlayer(target);
                case TOGGLE -> {
                    if (voiceModule.isPlayerMuted(target)) {
                        voiceModule.unmutePlayer(target);
                    } else {
                        voiceModule.mutePlayer(target);
                    }
                }
            }
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger().at(Level.WARNING).withCause(e)
                    .log("[set_voice_activity] failed");
        }
    }

    @Nullable
    private UUID resolveTarget(TriggerContext ctx) {
        if (targetPlayer != null && !targetPlayer.isBlank()) {
            return TriggerVolumeApiAdapter.getPlayerUuidByName(ctx.getStore(), targetPlayer);
        }
        return TriggerVolumeApiAdapter.getEntityUuid(ctx);
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger().at(Level.WARNING)
                .log("[set_voice_activity] skipped: " + reason);
    }

    private void warnOnce(String key, String reason) {
        if (WARNED.add(key)) {
            warn(reason);
        }
    }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    @Nullable public String getTargetPlayer() { return targetPlayer; }
    public void setTargetPlayer(@Nullable String targetPlayer) { this.targetPlayer = targetPlayer; }

    public enum Mode {
        MUTE,
        UNMUTE,
        TOGGLE;

        public static final Map<Mode, String> ALIASES = Map.of(
                MUTE, "mute",
                UNMUTE, "unmute",
                TOGGLE, "toggle"
        );
    }
}
