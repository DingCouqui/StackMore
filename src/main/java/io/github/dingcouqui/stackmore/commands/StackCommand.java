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

/**
 * {@code /stack [amount|all]} 命令执行器。
 *
 * <p>将玩家背包中同种方块吸收到手持物品中，形成或扩大"特殊堆叠"。
 * 行为根据权限分化：</p>
 * <ul>
 *   <li><b>普通玩家</b> ({@code stackmore.use})：
 *       首次使用时将手持物品转为特殊堆叠，随后从背包各格吸收同种方块增加数量。</li>
 *   <li><b>管理员</b> ({@code stackmore.admin})：
 *       直接设置特殊堆叠数量，无需消耗背包中的实际物品。</li>
 * </ul>
 *
 * <p>吸收顺序：背包 0-35 号槽位（跳过手持槽）→ 副手。每次最多吸收到配置上限
 * ({@code 64 × max_stack_multiplier})。</p>
 */
public class StackCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("stackmore.use")) {
            player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("no_permission")));
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir() || !hand.getType().isBlock()) {
            player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("not_holding_block")));
            return true;
        }
        Material material = hand.getType();
        if (StackMorePlugin.getConfigManager().isMaterialDisabled(material)) {
            player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("material_disabled")));
            return true;
        }

        int maxSize = StackMorePlugin.getConfigManager().getMaxStackSize();
        boolean isAdmin = player.hasPermission("stackmore.admin");

        int currentAmount;
        boolean isSpecial = StackItemManager.isSpecialStack(hand);
        if (isSpecial) {
            currentAmount = StackItemManager.getAmount(hand);
        } else {
            currentAmount = hand.getAmount();
        }

        int targetAmount = maxSize;
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("all")) {
                targetAmount = maxSize;
            } else {
                try {
                    targetAmount = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        if (isAdmin) {
            int setTo = Math.min(targetAmount, maxSize);
            if (!isSpecial) {
                ItemStack special = StackItemManager.createSpecialStack(hand, setTo, player.getName(), player.getUniqueId());
                player.getInventory().setItemInMainHand(special);
                player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("stack_all", "%total%", String.valueOf(setTo))));
            } else {
                StackItemManager.setAmount(hand, setTo);
                // PDC 修改后需调用 setItemInMainHand 将变更同步到客户端，否则玩家看不到数量更新
                player.getInventory().setItemInMainHand(hand);
                player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("stack_success", "%amount%", String.valueOf(setTo - currentAmount), "%total%", String.valueOf(setTo))));
            }
            return true;
        }

        if (!isSpecial) {
            hand = StackItemManager.createSpecialStack(hand, currentAmount, player.getName(), player.getUniqueId());
            player.getInventory().setItemInMainHand(hand);
        }

        int toAdd = targetAmount - currentAmount;
        if (toAdd <= 0) {
            player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("stack_limit", "%amount%", "0", "%total%", String.valueOf(currentAmount))));
            return true;
        }

        int absorbed = absorbFromPlayer(player, material, toAdd);
        int newTotal = currentAmount + absorbed;
        StackItemManager.setAmount(hand, newTotal);
        // PDC 修改后需调用 setItemInMainHand 将变更同步到客户端，否则玩家看不到数量更新
        player.getInventory().setItemInMainHand(hand);

        String msgPath = newTotal >= maxSize ? "stack_limit" : "stack_success";
        player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get(msgPath, "%amount%", String.valueOf(absorbed), "%total%", String.valueOf(newTotal))));
        return true;
    }

    /**
     * 从玩家背包中扫描并吸收指定数量的同种普通物品。
     *
     * <p>扫描顺序：背包 0-35 号槽位（跳过手持槽）→ 副手。
     * 跳过已为特殊堆叠的物品格。</p>
     *
     * @param player   目标玩家
     * @param material 要吸收的物品材料
     * @param needed   需要的数量
     * @return 实际吸收的数量（≤ needed）
     */
    private int absorbFromPlayer(Player player, Material material, int needed) {
        PlayerInventory inv = player.getInventory();
        int absorbed = 0;
        for (int i = 0; i < 36; i++) {
            if (i == inv.getHeldItemSlot()) continue;
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == material && !StackItemManager.isSpecialStack(item)) {
                int available = item.getAmount();
                int take = Math.min(needed - absorbed, available);
                item.setAmount(available - take);
                if (item.getAmount() <= 0) inv.clear(i);
                absorbed += take;
                if (absorbed >= needed) break;
            }
        }
        if (absorbed < needed) {
            ItemStack off = inv.getItemInOffHand();
            if (off.getType() == material && !StackItemManager.isSpecialStack(off)) {
                int take = Math.min(needed - absorbed, off.getAmount());
                off.setAmount(off.getAmount() - take);
                if (off.getAmount() <= 0) inv.setItemInOffHand(null);
                absorbed += take;
            }
        }
        return absorbed;
    }
}
