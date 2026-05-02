package io.github.dingcouqui.stackmore.commands;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.item.StackItemManager;
import io.github.dingcouqui.stackmore.util.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UnstackToCommand implements CommandExecutor {

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
        if (args.length < 1) return false;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!StackItemManager.isSpecialStack(hand)) {
            player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("not_holding_special")));
            return true;
        }

        int current = StackItemManager.getAmount(hand);
        int target;
        try {
            target = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return false;
        }

        if (target >= current) {
            // 等效于 /unstack all
            UnstackCommand.unstack(player, hand, current, current, true);
        } else {
            // ★ 解压到只剩 target 个，放不下的强制掉落
            UnstackCommand.unstack(player, hand, current, current - target, true);
        }
        return true;
    }
}