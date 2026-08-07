package io.github.dingcouqui.stackmore.listeners;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.item.StackItemManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
    // 跨事件传递：右键交互前保存特殊堆叠牌子副本，用于检测第三方插件（如 BlockLocker）的自动放牌消耗
    private final Map<UUID, ItemStack> pendingSignStacks = new HashMap<>();

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

    /**
     * 监听特殊方块交互，在下一tick恢复物品
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void restoreSpecialStackInteraction(PlayerInteractEvent event) {
        if (!isRightClickBlockWithHand(event)) {
            return;
        }

        if (event.useItemInHand() == Event.Result.DENY || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack item = event.getItem();
        if (!StackItemManager.isSpecialStack(item)) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !canVanillaConsumeInteraction(block.getType(), item.getType())) {
            return;
        }

        // 保存物品副本
        EquipmentSlot hand = event.getHand();
        ItemStack original = item.clone();
        // 下一tick恢复
        plugin.getServer().getScheduler().runTask(plugin, () -> restoreConsumedInteraction(player, hand, original));
    }

    /**
     * 在第三方插件（如 BlockLocker）处理右键交互前保存特殊堆叠牌子的副本。
     *
     * <p>LOWEST 优先级确保在其他任何监听器（包括 BlockLocker）之前执行。
     * 仅保存特殊堆叠的牌子：BlockLocker 的自动放牌只会消耗牌子类物品。</p>
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void saveSignStackOnInteract(PlayerInteractEvent event) {
        if (!isRightClickBlockWithHand(event)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack item = getItemInHand(player.getInventory(), event.getHand());
        if (StackItemManager.isSpecialStack(item) && Tag.SIGNS.isTagged(item.getType())) {
            pendingSignStacks.put(player.getUniqueId(), item.clone());
        }
    }

    /**
     * 检测第三方插件（如 BlockLocker）自动放置牌子时的物品消耗并恢复。
     *
     * <p>BlockLocker 在右键可保护方块且手持牌子时，会手动放置 [Private] 牌子并
     * 直接从手槽扣减一个物品。由于特殊堆叠的原版 amount 恒为 1，这种扣减会把
     * 整个特殊堆叠当作单个物品删除。这里通过交互事件的"签名"识别该消耗：</p>
     * <ol>
     *   <li>事件被取消（BlockLocker 自动放牌成功后会取消原交互事件）</li>
     *   <li>对应手槽已被清空（物品被消耗）</li>
     *   <li>点击面相对位置出现了新牌子（消耗的副作用证据，排除其他原因清空手槽）</li>
     * </ol>
     *
     * <p>恢复发生在同一事件派发内（同步），不存在跨 tick 窗口，因此不会与
     * 玩家的丢弃/移动等操作产生竞态。无副作用的静默消耗（不满足条件 3）
     * 不做恢复，物品丢失风险由设计接受。</p>
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void restoreConsumedSignStack(PlayerInteractEvent event) {
        if (!isRightClickBlockWithHand(event)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        UUID playerId = player.getUniqueId();
        ItemStack original = pendingSignStacks.remove(playerId);
        if (original == null) {
            return;
        }

        // 条件 1：BlockLocker 自动放牌成功后会取消原交互事件
        if (!event.isCancelled()) {
            return;
        }

        // 条件 2：对应手槽已被清空（特殊堆叠被当作单个物品删除）
        if (!isEmpty(getItemInHand(player.getInventory(), event.getHand()))) {
            return;
        }

        // 条件 3：点击面相对位置出现了新牌子（消耗的副作用证据）
        Block clickedBlock = event.getClickedBlock();
        BlockFace clickedFace = event.getBlockFace();
        if (clickedBlock == null || clickedFace == null) {
            return;
        }
        Material placed = clickedBlock.getRelative(clickedFace).getType();
        if (!Tag.STANDING_SIGNS.isTagged(placed) && !Tag.WALL_SIGNS.isTagged(placed)) {
            return;
        }

        // 数量减 1 后放回原手槽（与正常放置的消耗一致）
        int newAmount = StackItemManager.getAmount(original) - 1;
        if (newAmount <= 0) {
            return;
        }
        setItemInHand(player.getInventory(), event.getHand(),
                StackItemManager.adjustAfterPlacement(original, newAmount));
    }

    private boolean isRightClickBlockWithHand(PlayerInteractEvent event) {
        return event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() != null;
    }

    /**
     * 当前堆叠物品是否会被特殊方块交互消耗
     *
     * @param blockType 交互方块
     * @param itemType  玩家的堆叠物品
     */
    private boolean canVanillaConsumeInteraction(Material blockType, Material itemType) {
        if (Tag.FLOWER_POTS.isTagged(blockType)) {
            return true;
        }
        if (blockType == Material.COMPOSTER && itemType.isCompostable()) {
            return true;
        }
        if (blockType == Material.CAKE && Tag.CANDLES.isTagged(itemType)) {
            return true;
        }
        return blockType == Material.RESPAWN_ANCHOR && itemType == Material.GLOWSTONE;
    }

    /**
     * 特殊方块交互的物品消耗恢复任务
     */
    private void restoreConsumedInteraction(Player player, EquipmentSlot hand, ItemStack original) {
        if (!player.isOnline()) {
            return;
        }

        PlayerInventory inv = player.getInventory();
        ItemStack current = getItemInHand(inv, hand);
        if (!isEmpty(current)) {
            return;
        }

        int newAmount = StackItemManager.getAmount(original) - 1;
        if (newAmount <= 0) {
            return;
        }

        setItemInHand(inv, hand, StackItemManager.adjustAfterPlacement(original, newAmount));
    }

    private ItemStack getItemInHand(PlayerInventory inv, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            return inv.getItemInOffHand();
        }
        return inv.getItemInMainHand();
    }

    private void setItemInHand(PlayerInventory inv, EquipmentSlot hand, ItemStack item) {
        if (hand == EquipmentSlot.OFF_HAND) {
            inv.setItemInOffHand(item);
        } else {
            inv.setItemInMainHand(item);
        }
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }
}
