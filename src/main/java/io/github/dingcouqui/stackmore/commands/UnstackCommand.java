package io.github.dingcouqui.stackmore.commands;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.item.StackItemManager;
import io.github.dingcouqui.stackmore.util.TextUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class UnstackCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players.");
            return true;
        }
        if (!player.hasPermission("stackmore.use")) {
            player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("no_permission")));
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!StackItemManager.isSpecialStack(hand)) {
            player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("not_holding_special")));
            return true;
        }

        int currentAmount = StackItemManager.getAmount(hand);
        Material material = hand.getType();

        // 默认：无参数 → 尽量解压全部（多余留手）
        int toExtract = currentAmount;
        boolean dropExcess = false;

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("all")) {
                // /unstack all → 强制全部解压，多余掉落
                toExtract = currentAmount;
                dropExcess = true;
            } else {
                try {
                    int argNumber = Integer.parseInt(args[0]);
                    if (argNumber >= currentAmount) {
                        // 数量≥当前 → 等同于 all
                        toExtract = currentAmount;
                        dropExcess = true;
                    } else {
                        // 数量<当前 → 解压指定数量，保留剩余
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
     * 从特殊堆叠中解压物品。
     *
     * @param player         玩家
     * @param hand           当前特殊堆叠物品
     * @param currentAmount  特殊堆叠内的总数
     * @param toExtract      本次需要解压的数量
     * @param dropExcess     如果背包满，多出的物品是否掉落
     */
    public static void unstack(Player player, ItemStack hand, int currentAmount, int toExtract, boolean dropExcess) {
        Material material = hand.getType();
        PlayerInventory inv = player.getInventory();
        int remaining = toExtract;

        // 优先补满已有同种非特殊格子
        for (int i = 0; i < 36; i++) {
            if (i == inv.getHeldItemSlot()) continue;
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == material && !StackItemManager.isSpecialStack(item) && item.getAmount() < 64) {
                int canAdd = 64 - item.getAmount();
                int add = Math.min(canAdd, remaining);
                item.setAmount(item.getAmount() + add);
                remaining -= add;
                if (remaining <= 0) break;
            }
        }

        // 再放入空格子
        if (remaining > 0) {
            for (int i = 0; i < 36; i++) {
                if (i == inv.getHeldItemSlot()) continue;
                ItemStack item = inv.getItem(i);
                if (item == null || item.getType().isAir()) {
                    int add = Math.min(64, remaining);
                    inv.setItem(i, new ItemStack(material, add));
                    remaining -= add;
                    if (remaining <= 0) break;
                }
            }
        }

        // 如果仍然有剩余且允许掉落，则掉在地上
        if (remaining > 0 && dropExcess) {
            while (remaining > 0) {
                int dropAmount = Math.min(64, remaining);
                player.getWorld().dropItem(player.getLocation(), new ItemStack(material, dropAmount));
                remaining -= dropAmount;
            }
            player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("unstack_drop")));
        }

        int actualRemoved = toExtract - remaining;  // 实际成功解压的数量
        int newAmount = currentAmount - actualRemoved;  // 手中剩余的总数

        // 设置手中物品
        if (newAmount <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else if (newAmount <= 64) {
            // 转为普通堆叠
            ItemStack normal = new ItemStack(material, newAmount);
            ItemMeta specialMeta = hand.getItemMeta();
            if (specialMeta != null && specialMeta.hasDisplayName()) {
                ItemMeta normalMeta = normal.getItemMeta();
                normalMeta.setDisplayName(specialMeta.getDisplayName());
                normal.setItemMeta(normalMeta);
            }
            player.getInventory().setItemInMainHand(normal);
        } else {
            // 仍为特殊堆叠，更新数量
            StackItemManager.setAmount(hand, newAmount);
            player.getInventory().setItemInMainHand(hand);
        }

        // 发送消息
        if (dropExcess && toExtract == currentAmount) {
            // /unstack all 或等效
            player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("unstack_all")));
        } else {
            player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("unstack_success",
                    "%amount%", String.valueOf(actualRemoved),
                    "%total%", String.valueOf(newAmount))));
        }
    }
}