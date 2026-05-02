package io.github.dingcouqui.stackmore.listeners;

import io.github.dingcouqui.stackmore.item.StackItemManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.Set;  // 新增

public class InventoryListener implements Listener {

    private static final Set<InventoryAction> ALLOWED_ACTIONS = Set.of(
            InventoryAction.PICKUP_ALL,
            InventoryAction.PLACE_ALL,
            InventoryAction.SWAP_WITH_CURSOR,
            InventoryAction.HOTBAR_SWAP,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
    );

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();
        InventoryType topType = top.getType();

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        boolean cursorSpecial = StackItemManager.isSpecialStack(cursor);
        boolean currentSpecial = StackItemManager.isSpecialStack(current);

        if (cursorSpecial || currentSpecial) {
            if (isRestrictedContainer(topType)) {
                if (event.getClickedInventory() == top && cursorSpecial) {
                    event.setCancelled(true);
                    return;
                }
                if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && currentSpecial) {
                    if (event.getClickedInventory() != top) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            InventoryAction action = event.getAction();
            if (!ALLOWED_ACTIONS.contains(action)) {
                event.setCancelled(true);
            }
        }

        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR && cursorSpecial) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack cursor = event.getOldCursor();
        if (StackItemManager.isSpecialStack(cursor)) {
            event.setCancelled(true);
        }
    }

    private boolean isRestrictedContainer(InventoryType type) {
        return type == InventoryType.WORKBENCH ||
               type == InventoryType.ANVIL ||
               type == InventoryType.GRINDSTONE ||
               type == InventoryType.SMITHING ||
               type == InventoryType.ENCHANTING ||
               type == InventoryType.CRAFTING;
    }
}