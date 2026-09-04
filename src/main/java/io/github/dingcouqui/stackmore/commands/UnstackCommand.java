package io.github.dingcouqui.stackmore.commands;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.item.StackItemManager;
import io.github.dingcouqui.stackmore.util.CommandUtils;
import io.github.dingcouqui.stackmore.util.TextUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * {@code /unstack [amount|all]} 命令执行器。
 *
 * <p>从手持特殊堆叠中解压物品到背包。支持三种模式：</p>
 * <ul>
 *   <li><b>无参数</b> — 尽量解压到背包，放不下的留在手中</li>
 *   <li><b>指定数量</b>（小于当前总量） — 解压指定数量，剩余留在手中</li>
 *   <li><b>指定数量 ≥ 当前 / "all"</b> — 全部解压，放不下的掉落在地上</li>
 * </ul>
 *
 * <p>解压后若总量 ≤ 64，特殊堆叠会自动退化为普通物品堆叠。</p>
 */
public class UnstackCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = CommandUtils.validatePlayerWithPermission(sender, "stackmore.use");
        if (player == null) return true;

        ItemStack hand = CommandUtils.getHeldSpecialStack(player);
        if (hand == null) return true;

        int currentAmount = StackItemManager.getAmount(hand);

        // 默认：无参数 → 尽量解压全部（多余留手）
        int toExtract = currentAmount;
        boolean dropExcess = false;

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("all")) {
                toExtract = currentAmount;
                dropExcess = true;
            } else {
                try {
                    int argNumber = Integer.parseInt(args[0]);
                    if (argNumber >= currentAmount) {
                        toExtract = currentAmount;
                        dropExcess = true;
                    } else {
                        toExtract = argNumber;
                        dropExcess = false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        unstack(player, hand, currentAmount, toExtract, dropExcess);
        return true;
    }

    /**
     * 从特殊堆叠中解压物品到玩家背包。
     *
     * <p>放置策略：优先补满已有同种格子 → 填入空格 → 溢出掉落。</p>
     *
     * @param player         玩家
     * @param hand           当前特殊堆叠物品
     * @param currentAmount  特殊堆叠内的总数
     * @param toExtract      本次需要解压的数量
     * @param dropExcess     如果背包满，多出的物品是否掉落在地
     */
    public static void unstack(Player player, ItemStack hand, int currentAmount,
                               int toExtract, boolean dropExcess) {
        Material material = hand.getType();
        PlayerInventory inv = player.getInventory();
        int heldSlot = inv.getHeldItemSlot();

        int remaining = fillExistingStacks(inv, material, toExtract, heldSlot);
        if (remaining > 0) {
            remaining = fillEmptySlots(inv, material, remaining, heldSlot);
        }
        if (remaining > 0 && dropExcess) {
            remaining = dropExcessItems(player, material, remaining);
        }

        int actualRemoved = toExtract - remaining;
        int newAmount = currentAmount - actualRemoved;

        updateHandAfterUnstack(player, hand, newAmount);
        sendUnstackMessage(player, dropExcess, toExtract, currentAmount,
                actualRemoved, newAmount);
    }

    /**
     * Fill existing partial stacks of the same material in inventory.
     *
     * @return remaining items that could not be placed
     */
    private static int fillExistingStacks(PlayerInventory inv, Material material,
                                          int remaining, int heldSlot) {
        for (int i = 0; i < 36; i++) {
            if (i == heldSlot) continue;
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == material
                    && !StackItemManager.isSpecialStack(item)
                    && item.getAmount() < 64) {
                int canAdd = 64 - item.getAmount();
                int add = Math.min(canAdd, remaining);
                item.setAmount(item.getAmount() + add);
                remaining -= add;
                if (remaining <= 0) break;
            }
        }
        return remaining;
    }

    /**
     * Place items into empty inventory slots.
     *
     * @return remaining items that could not be placed
     */
    private static int fillEmptySlots(PlayerInventory inv, Material material,
                                      int remaining, int heldSlot) {
        for (int i = 0; i < 36; i++) {
            if (i == heldSlot) continue;
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) {
                int add = Math.min(64, remaining);
                inv.setItem(i, new ItemStack(material, add));
                remaining -= add;
                if (remaining <= 0) break;
            }
        }
        return remaining;
    }

    /**
     * Drop excess items on the ground at the player's location.
     *
     * @return 0 (all remaining items dropped)
     */
    private static int dropExcessItems(Player player, Material material, int remaining) {
        while (remaining > 0) {
            int dropAmount = Math.min(64, remaining);
            player.getWorld().dropItem(player.getLocation(),
                    new ItemStack(material, dropAmount));
            remaining -= dropAmount;
        }
        player.sendMessage(TextUtils.toComponent(
                StackMorePlugin.getMessageManager().get("unstack_drop")));
        return 0;
    }

    /**
     * Update the player's hand item after unstacking:
     * ≤ 0 → clear, ≤ 64 → normal stack, > 64 → keep as special stack.
     */
    private static void updateHandAfterUnstack(Player player, ItemStack hand,
                                               int newAmount) {
        if (newAmount <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else if (newAmount <= 64) {
            player.getInventory().setItemInMainHand(
                    StackItemManager.toNormalStack(hand, newAmount));
        } else {
            StackCommandHelper.updateHandAmount(player, hand, newAmount);
        }
    }

    /**
     * Send the appropriate success message after unstacking.
     */
    private static void sendUnstackMessage(Player player, boolean dropExcess,
                                           int toExtract, int currentAmount,
                                           int actualRemoved, int newAmount) {
        if (dropExcess && toExtract == currentAmount) {
            player.sendMessage(TextUtils.toComponent(
                    StackMorePlugin.getMessageManager().get("unstack_all")));
        } else {
            player.sendMessage(TextUtils.toComponent(
                    StackMorePlugin.getMessageManager().get("unstack_success",
                            "%amount%", String.valueOf(actualRemoved),
                            "%total%", String.valueOf(newAmount))));
        }
    }
}
