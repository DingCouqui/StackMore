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
        Player player = CommandUtils.validatePlayerWithPermission(sender, "stackmore.use");
        if (player == null) return true;

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

        // 拒绝被第三方插件（如 Infinite-Blocks）PDC 标记的物品
        if (StackItemManager.hasExternalPDCTags(hand)) {
            player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("external_item_rejected")));
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

        int targetAmount = parseTargetAmount(args, maxSize);
        if (targetAmount < 0) return false;

        if (isAdmin) {
            return handleAdminStack(player, hand, targetAmount, maxSize, isSpecial, currentAmount);
        }
        return handlePlayerStack(player, hand, material, targetAmount, currentAmount, maxSize, isSpecial);
    }

    /**
     * Parse the target stack amount from command arguments.
     *
     * @param args    command arguments
     * @param maxSize the configured maximum stack size
     * @return the parsed target amount, or -1 on parse error, or maxSize if no argument given
     */
    private static int parseTargetAmount(String[] args, int maxSize) {
        if (args.length == 1 && !args[0].equalsIgnoreCase("all")) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return maxSize;
    }

    /**
     * Handle /stack for admin players — directly set the stack amount without
     * consuming inventory items.
     */
    private static boolean handleAdminStack(Player player, ItemStack hand, int targetAmount,
                                            int maxSize, boolean isSpecial, int currentAmount) {
        int setTo = Math.min(targetAmount, maxSize);
        if (!isSpecial) {
            ItemStack special = StackItemManager.createSpecialStack(hand, setTo,
                    player.getName(), player.getUniqueId());
            player.getInventory().setItemInMainHand(special);
            player.sendMessage(TextUtils.toComponent(
                    StackMorePlugin.getMessageManager().get("stack_all", "%total%", String.valueOf(setTo))));
        } else {
            StackItemManager.setAmount(hand, setTo);
            // PDC 修改后需调用 setItemInMainHand 将变更同步到客户端，否则玩家看不到数量更新
            player.getInventory().setItemInMainHand(hand);
            player.sendMessage(TextUtils.toComponent(
                    StackMorePlugin.getMessageManager().get("stack_success",
                            "%amount%", String.valueOf(setTo - currentAmount),
                            "%total%", String.valueOf(setTo))));
        }
        return true;
    }

    /**
     * Handle /stack for normal players — convert held item to special stack
     * (if not already), then absorb matching items from inventory.
     */
    private static boolean handlePlayerStack(Player player, ItemStack hand, Material material,
                                             int targetAmount, int currentAmount,
                                             int maxSize, boolean isSpecial) {
        if (!isSpecial) {
            hand = StackItemManager.createSpecialStack(hand, currentAmount,
                    player.getName(), player.getUniqueId());
            player.getInventory().setItemInMainHand(hand);
        }

        int toAdd = targetAmount - currentAmount;
        if (toAdd <= 0) {
            player.sendMessage(TextUtils.toComponent(
                    StackMorePlugin.getMessageManager().get("stack_limit",
                            "%amount%", "0", "%total%", String.valueOf(currentAmount))));
            return true;
        }

        int absorbed = StackCommandHelper.absorbFromPlayer(player, material, toAdd);
        int newTotal = currentAmount + absorbed;
        StackItemManager.setAmount(hand, newTotal);
        // PDC 修改后需调用 setItemInMainHand 将变更同步到客户端，否则玩家看不到数量更新
        player.getInventory().setItemInMainHand(hand);

        String msgPath = newTotal >= maxSize ? "stack_limit" : "stack_success";
        player.sendMessage(TextUtils.toComponent(
                StackMorePlugin.getMessageManager().get(msgPath,
                        "%amount%", String.valueOf(absorbed),
                        "%total%", String.valueOf(newTotal))));
        return true;
    }
}
