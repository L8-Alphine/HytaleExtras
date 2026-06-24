package org.hyzionstudios.hyextras.triggerextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.util.RichText;
import org.hyzionstudios.hyextras.util.StringTemplate;

import javax.annotation.Nullable;
import java.util.logging.Level;

public class GiveItemRewardAction extends TriggerEffect {

    public static final BuilderCodec<GiveItemRewardAction> CODEC = BuilderCodec.builder(
                    GiveItemRewardAction.class, GiveItemRewardAction::new, TriggerEffect.BASE_CODEC)
            .append(new KeyedCodec<>("Item", ItemStack.CODEC),
                    GiveItemRewardAction::setItem,
                    GiveItemRewardAction::getItem).add()
            .append(CodecHelper.optString("Message"),
                    GiveItemRewardAction::setMessage,
                    GiveItemRewardAction::getMessage).add()
            .build();

    private ItemStack item;
    @Nullable private String message;

    public GiveItemRewardAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return;
        }
        try {
            if (item == null || ItemStack.isEmpty(item)) {
                warn("item is empty");
                return;
            }
            PlayerRef pr = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
            if (pr == null) {
                warn("triggering entity is not a player");
                return;
            }
            CombinedItemContainer inventory = InventoryComponent.getCombined(
                    ctx.getStore(),
                    ctx.getEntityRef(),
                    InventoryComponent.HOTBAR_STORAGE_BACKPACK);
            if (inventory == null) {
                warn("player has no inventory");
                return;
            }
            ItemStack reward = item.cleanCopy();
            if (!inventory.canAddItemStack(reward)) {
                warn("inventory cannot accept reward item " + reward.getItemId());
                return;
            }
            inventory.addItemStack(reward);
            if (message != null && !message.isBlank()) {
                String resolved = StringTemplate.resolve(message, ctx, HyExtrasPlugin.get().getVariableService());
                pr.sendMessage(RichText.toMessage(resolved));
            }
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger().at(Level.WARNING).withCause(e)
                    .log("[give_item_reward] failed");
        }
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger().at(Level.WARNING)
                .log("[give_item_reward] skipped: " + reason);
    }

    public ItemStack getItem() { return item; }
    public void setItem(ItemStack item) { this.item = item; }
    @Nullable public String getMessage() { return message; }
    public void setMessage(@Nullable String message) { this.message = message; }
}
