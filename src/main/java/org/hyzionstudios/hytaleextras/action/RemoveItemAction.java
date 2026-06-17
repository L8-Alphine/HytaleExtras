package org.hyzionstudios.hytaleextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.ActiveSlotInventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import org.hyzionstudios.hytaleextras.HytaleextrasPlugin;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.logging.Level;

/**
 * Removes item(s) from the triggering player's inventory by item ID.
 *
 * <p>Iterates all inventory sections (hotbar, storage, armor, backpack, etc.) via
 * {@link InventoryComponent#getCombined}. Removes up to {@code quantity} items of the
 * given {@code itemId}; if fewer are present, removes all available.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "remove_item", "ItemId": "hytale:gold_coin", "Quantity": 5, "Location": "inventory" }
 * }</pre>
 */
public class RemoveItemAction extends TriggerEffect {

    public static final BuilderCodec<RemoveItemAction> CODEC = BuilderCodec.builder(
                    RemoveItemAction.class, RemoveItemAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("ItemId"),       RemoveItemAction::setItemId,   RemoveItemAction::getItemId).add()
            .append(CodecHelper.optInteger("Quantity"), RemoveItemAction::setQuantity, RemoveItemAction::getQuantity).add()
            .append(CodecHelper.optEnum("Location", Location.class, Location.ALIASES),
                    RemoveItemAction::setLocation, RemoveItemAction::getLocation).add()
            .build();

    private String itemId;
    @Nullable private Integer quantity;
    @Nullable private Location location;

    public RemoveItemAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            if (itemId == null || itemId.isBlank()) {
                HytaleextrasPlugin.get().getLogger()
                        .at(Level.WARNING).log("[remove_item] skipped: itemId is empty");
                return;
            }
            int qty = (quantity != null && quantity > 0) ? quantity : 1;

            ItemStack stack = new ItemStack(itemId, qty);
            switch (location != null ? location : Location.INVENTORY) {
                case INVENTORY -> removeFromInventory(ctx, stack);
                case HOTBAR -> removeFromHotbar(ctx, stack);
                case IN_HAND -> removeFromActiveSlot(ctx, stack);
            }
        } catch (Exception e) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[remove_item] failed for itemId=" + itemId);
        }
    }

    private void removeFromInventory(TriggerContext ctx, ItemStack stack) {
        CombinedItemContainer combined = InventoryComponent.getCombined(
                ctx.getStore(), ctx.getEntityRef(), InventoryComponent.EVERYTHING);
        if (combined == null) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).log("[remove_item] player has no inventory");
            return;
        }
        combined.removeItemStack(stack);
    }

    private void removeFromHotbar(TriggerContext ctx, ItemStack stack) {
        InventoryComponent.Hotbar hotbar = ctx.getStore().getComponent(
                ctx.getEntityRef(), InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null || hotbar.getInventory() == null) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).log("[remove_item] player has no hotbar inventory");
            return;
        }
        hotbar.getInventory().removeItemStack(stack);
    }

    private void removeFromActiveSlot(TriggerContext ctx, ItemStack stack) {
        InventoryComponent.Tool tool = ctx.getStore().getComponent(
                ctx.getEntityRef(), InventoryComponent.Tool.getComponentType());
        if (tool != null && tool.isUsingToolsItem() && removeFromActiveSlot(tool, stack)) {
            return;
        }

        InventoryComponent.Hotbar hotbar = ctx.getStore().getComponent(
                ctx.getEntityRef(), InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null || !removeFromActiveSlot(hotbar, stack)) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).log("[remove_item] no matching active item for itemId=" + itemId);
        }
    }

    private boolean removeFromActiveSlot(ActiveSlotInventoryComponent component, ItemStack requested) {
        byte activeSlot = component.getActiveSlot();
        if (activeSlot == InventoryComponent.INACTIVE_SLOT_INDEX) return false;

        ItemStack activeItem = component.getActiveItem();
        if (ItemStack.isEmpty(activeItem) || !itemId.equals(activeItem.getItemId())) return false;

        ItemContainer inventory = component.getInventory();
        if (inventory == null) return false;
        inventory.removeItemStackFromSlot(activeSlot, requested, requested.getQuantity());
        return true;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    @Nullable public Integer getQuantity() { return quantity; }
    public void setQuantity(@Nullable Integer quantity) { this.quantity = quantity; }

    @Nullable public Location getLocation() { return location; }
    public void setLocation(@Nullable Location location) { this.location = location; }

    public enum Location {
        INVENTORY,
        HOTBAR,
        IN_HAND;

        public static final Map<Location, String> ALIASES = Map.of(
                INVENTORY, "inventory",
                HOTBAR, "hotbar",
                IN_HAND, "in_hand"
        );
    }
}
