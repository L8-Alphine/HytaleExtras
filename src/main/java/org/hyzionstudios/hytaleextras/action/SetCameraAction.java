package org.hyzionstudios.hytaleextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hytaleextras.HyextrasPlugin;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.logging.Level;

/**
 * Sends a {@link SetServerCamera} packet to switch the triggering player's camera mode.
 *
 * <p>Modes:
 * <ul>
 *   <li>{@code "first_person"} — standard first-person view (default)</li>
 *   <li>{@code "third_person"} — third-person / over-the-shoulder view</li>
 *   <li>{@code "reset"} — return to unlocked first-person (cancels any locked camera)</li>
 * </ul>
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "set_camera", "Mode": "third_person" }
 * { "type": "set_camera", "Mode": "third_person", "Locked": true }
 * { "type": "set_camera", "Mode": "reset" }
 * }</pre>
 */
public class SetCameraAction extends TriggerEffect {

    public static final BuilderCodec<SetCameraAction> CODEC = BuilderCodec.builder(
                    SetCameraAction.class, SetCameraAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.enumField("Mode", CameraMode.class, CameraMode.ALIASES),
                    SetCameraAction::setMode, SetCameraAction::getMode).add()
            .append(CodecHelper.optBool("Locked"), SetCameraAction::setLocked, SetCameraAction::getLocked).add()
            .build();

    private CameraMode mode;
    @Nullable private Boolean locked; // default false

    public SetCameraAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            PlayerRef pr = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
            if (pr == null) return;

            CameraMode cameraMode = mode != null ? mode : CameraMode.FIRST_PERSON;
            ClientCameraView view = switch (cameraMode) {
                case THIRD_PERSON -> ClientCameraView.ThirdPerson;
                case FIRST_PERSON, RESET -> ClientCameraView.FirstPerson;
            };

            SetServerCamera packet = new SetServerCamera();
            packet.clientCameraView = view;
            packet.isLocked = (locked != null && locked) && cameraMode != CameraMode.RESET;
            pr.getPacketHandler().write(packet);
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[set_camera] failed for mode=" + mode);
        }
    }

    public CameraMode getMode() { return mode; }
    public void setMode(CameraMode mode) { this.mode = mode; }
    @Nullable public Boolean getLocked() { return locked; }
    public void setLocked(@Nullable Boolean locked) { this.locked = locked; }

    public enum CameraMode {
        FIRST_PERSON,
        THIRD_PERSON,
        RESET;

        public static final Map<CameraMode, String> ALIASES = Map.of(
                FIRST_PERSON, "first_person",
                THIRD_PERSON, "third_person",
                RESET, "reset"
        );
    }
}
