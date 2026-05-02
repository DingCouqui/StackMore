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

public class BlockListener implements Listener {

    private final StackMorePlugin plugin = StackMorePlugin.getInstance();
    // 临时存储即将被原版消耗的特殊堆叠物品副本
    private final Map<UUID, ItemStack> pendingSpecialStacks = new HashMap<>();

    /**
     * 在原版放置之前保存特殊堆叠的副本。
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
     * 原版放置完成后恢复特殊堆叠物品（数量减1）。
     * MONITOR 优先级确保在所有其他监听器（包括保护插件）之后执行。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void restoreSpecialStack(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        UUID playerId = player.getUniqueId();
        ItemStack original = pendingSpecialStacks.remove(playerId);
        if (original == null) return;

        // 如果放置被取消（例如被土地保护插件阻止），不恢复特殊堆叠，物品被正常消耗
        if (event.isCancelled()) return;

        // 计算新数量
        int currentAmount = StackItemManager.getAmount(original);
        int newAmount = currentAmount - 1;

        // 生成新物品（若新数量 ≤0 则返回 null）
        ItemStack newItem;
        if (newAmount <= 0) {
            newItem = null;
        } else {
            newItem = StackItemManager.adjustAfterPlacement(original, newAmount);
        }

        // 设置到正确的装备槽（主手/副手）
        PlayerInventory inv = player.getInventory();
        EquipmentSlot hand = event.getHand();
        if (hand == EquipmentSlot.HAND) {
            inv.setItemInMainHand(newItem);
        } else if (hand == EquipmentSlot.OFF_HAND) {
            inv.setItemInOffHand(newItem);
        }
    }
}