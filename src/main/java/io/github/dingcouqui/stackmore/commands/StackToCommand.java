package io.github.dingcouqui.stackmore.commands;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.item.StackItemManager;
import io.github.dingcouqui.stackmore.util.CommandUtils;
import io.github.dingcouqui.stackmore.util.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /stackto <amount>} 命令执行器。
 *
 * <p>将手持特殊堆叠物品填充到指定数量。与 {@code /stack} 不同，
 * 此命令<strong>必须</strong>手持已有的特殊堆叠才能使用。</p>
 *
 * <p>权限分化：</p>
 * <ul>
 *   <li><b>普通玩家</b> — 从背包中吸收同种物品补足差额</li>
 *   <li><b>管理员</b> — 直接设置数量，无需消耗物品</li>
 * </ul>
 */
public class StackToCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = CommandUtils.validatePlayerWithPermission(sender, "stackmore.use");
        if (player == null) return true;
        if (args.length < 1) return false;

        ItemStack hand = CommandUtils.getHeldSpecialStack(player);
        if (hand == null) return true;

        int target;
        try {
            target = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return false;
        }

        int current = StackItemManager.getAmount(hand);
        if (target <= current) {
            player.sendMessage(TextUtils.toComponent(
                    StackMorePlugin.getMessageManager().get("stackto_target_too_low")));
            return true;
        }

        boolean isAdmin = player.hasPermission("stackmore.admin");
        int maxSize = StackMorePlugin.getConfigManager().getMaxStackSize();
        int setTo = Math.min(target, maxSize);

        if (isAdmin) {
            StackItemManager.setAmount(hand, setTo);
            player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("stack_success", "%amount%", String.valueOf(setTo - current), "%total%", String.valueOf(setTo))));
            return true;
        }

        int needed = setTo - current;
        int absorbed = StackCommandHelper.absorbFromPlayer(player, hand.getType(), needed);
        int newTotal = current + absorbed;
        StackItemManager.setAmount(hand, newTotal);
        String msgPath = newTotal >= maxSize ? "stack_limit" : "stack_success";
        player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get(msgPath, "%amount%", String.valueOf(absorbed), "%total%", String.valueOf(newTotal))));
        return true;
    }
}
