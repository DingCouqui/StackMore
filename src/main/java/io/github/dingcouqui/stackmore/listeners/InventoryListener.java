package io.github.dingcouqui.stackmore.listeners;

import io.github.dingcouqui.stackmore.item.StackItemManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * 背包操作事件监听器。
 *
 * <p>限制特殊堆叠物品在特定容器中的行为，防止玩家通过工作台、
 * 铁砧等方块拆解、合成或复制特殊堆叠物品。</p>
 *
 * <h3>限制规则</h3>
 * <ul>
 *   <li><b>受限容器</b>（工作台、铁砧、砂轮、锻造台、附魔台、合成格）：
 *       禁止将特殊堆叠物品移入这些容器的界面格子，也禁止通过 Shift+点击
 *       将玩家背包中的特殊堆叠快速移入。</li>
 *   <li><b>操作白名单</b>：涉及特殊堆叠时仅允许 {@code PICKUP_ALL}、
 *       {@code PLACE_ALL}、{@code SWAP_WITH_CURSOR}、{@code HOTBAR_SWAP}、
 *       {@code MOVE_TO_OTHER_INVENTORY} 五种操作，屏蔽双击收集、
 *       合成、分解等可能破坏物品完整性的操作。</li>
 *   <li><b>拖拽操作</b>：完全禁止涉及特殊堆叠物品的拖拽。</li>
 * </ul>
 */
public class InventoryListener implements Listener {

    /**
     * 涉及特殊堆叠物品时允许的背包操作白名单。
     * 只开放最基本的移动、交换和快速转移操作，屏蔽可能拆解或复制物品的操作。
     */
    private static final Set<InventoryAction> ALLOWED_ACTIONS = Set.of(
            InventoryAction.PICKUP_ALL,
            InventoryAction.PLACE_ALL,
            InventoryAction.SWAP_WITH_CURSOR,
            InventoryAction.HOTBAR_SWAP,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
    );

    /**
     * 拦截特殊堆叠物品在受限容器中的点击操作。
     */
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

        // 双击收集到特殊堆叠会导致物品异常合并，必须禁止
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR && cursorSpecial) {
            event.setCancelled(true);
        }
    }

    /**
     * 完全禁止涉及特殊堆叠物品的拖拽操作。
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack cursor = event.getOldCursor();
        if (StackItemManager.isSpecialStack(cursor)) {
            event.setCancelled(true);
        }
    }

    /**
     * 判断容器类型是否受限。
     * 受限容器包含所有可修改物品 NBT 或拆解物品的功能性方块界面。
     */
    private boolean isRestrictedContainer(InventoryType type) {
        return type == InventoryType.WORKBENCH ||
               type == InventoryType.ANVIL ||
               type == InventoryType.GRINDSTONE ||
               type == InventoryType.SMITHING ||
               type == InventoryType.ENCHANTING ||
               type == InventoryType.CRAFTING;
    }
}
