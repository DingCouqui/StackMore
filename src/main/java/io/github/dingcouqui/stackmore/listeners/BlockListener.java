package io.github.dingcouqui.stackmore.listeners;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.item.StackItemManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 方块放置事件监听器。
 *
 * <p>处理特殊堆叠物品放置方块时的数量递减。由于特殊堆叠物品的
 * 原版 {@code amount} 始终为 1，原版放置消耗后物品会消失。
 * 此监听器通过双事件优先级模式解决该问题：</p>
 *
 * <ol>
 *   <li><b>LOWEST（保存阶段）</b> — 在原版和其他插件处理前保存特殊堆叠副本</li>
 *   <li><b>MONITOR（恢复阶段）</b> — 在所有监听器（包括保护插件）之后，
 *       仅在放置未被取消时将数量减一的物品放回手中</li>
 * </ol>
 *
 * <p>此设计确保与 GriefPrevention、WorldGuard 等土地保护插件兼容：
 * 如果放置被取消，特殊堆叠物品被正常消耗（不再恢复），
 * 等同于原版行为。</p>
 */
public class BlockListener implements Listener {

    private final StackMorePlugin plugin = StackMorePlugin.getInstance();
    // 跨事件传递：放置前保存特殊堆叠副本，放置后读取并恢复
    private final Map<UUID, ItemStack> pendingSpecialStacks = new HashMap<>();

    /**
     * 在原版消耗前保存特殊堆叠物品的副本。
     * LOWEST 优先级确保在其他任何监听器之前执行。
     * 创造模式玩家无需处理（原版不会消耗物品）。
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void saveSpecialStack(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack item = event.getItemInHand();
        if (StackItemManager.isSpecialStack(item)) {
            pendingSpecialStacks.put(player.getUniqueId(), item.clone());
        }
    }

    /**
     * 在其他所有监听器之后恢复特殊堆叠物品（数量减 1）。
     * MONITOR 优先级确保保护插件（如领地/地皮）有机会取消放置事件。
     * 若放置被取消，不恢复特殊堆叠，物品被正常消耗。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void restoreSpecialStack(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        UUID playerId = player.getUniqueId();
        ItemStack original = pendingSpecialStacks.remove(playerId);
        if (original == null) return;

        // 放置被取消（如被保护插件阻止），不恢复，物品被正常消耗
        if (event.isCancelled()) return;

        // 计算新数量：当前 - 1
        int currentAmount = StackItemManager.getAmount(original);
        int newAmount = currentAmount - 1;

        // 生成新物品：≤0 返回 null（清除手持），>0 根据阈值调整
        ItemStack newItem;
        if (newAmount <= 0) {
            newItem = null;
        } else {
            newItem = StackItemManager.adjustAfterPlacement(original, newAmount);
        }

        // 设置到正确的装备槽（主手或副手）
        PlayerInventory inv = player.getInventory();
        EquipmentSlot hand = event.getHand();
        if (hand == EquipmentSlot.HAND) {
            inv.setItemInMainHand(newItem);
        } else if (hand == EquipmentSlot.OFF_HAND) {
            inv.setItemInOffHand(newItem);
        }
    }
}
