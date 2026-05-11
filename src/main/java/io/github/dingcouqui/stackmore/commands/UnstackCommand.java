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
     * 从特殊堆叠中解压物品到玩家背包。
     *
     * <p>放置策略：</p>
     * <ol>
     *   <li>优先补满已有同种非特殊堆叠的格子（≤64）</li>
     *   <li>再放入空格子（每次生成至多 64 个）</li>
     *   <li>若仍有剩余且允许掉落，则掉落在地上</li>
     * </ol>
     *
     * <p>解压完成后更新手持物品：</p>
     * <ul>
     *   <li>剩余数量 ≤ 0 → 清空手持</li>
     *   <li>剩余数量 ≤ 64 → 退化为普通堆叠</li>
     *   <li>剩余数量 &gt; 64 → 保留特殊堆叠并更新数量</li>
     * </ul>
     *
     * @param player         玩家
     * @param hand           当前特殊堆叠物品
     * @param currentAmount  特殊堆叠内的总数
     * @param toExtract      本次需要解压的数量
     * @param dropExcess     如果背包满，多出的物品是否掉落在地
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
            // 转为普通堆叠，保留原始自定义显示名称
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
