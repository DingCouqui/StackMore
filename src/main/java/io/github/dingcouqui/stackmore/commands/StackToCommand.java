package io.github.dingcouqui.stackmore.commands;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.item.StackItemManager;
import io.github.dingcouqui.stackmore.util.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StackToCommand implements CommandExecutor {

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

        int target;
        try {
            target = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return false;
        }

        int current = StackItemManager.getAmount(hand);
        if (target <= current) {
            player.sendMessage(TextUtils.toComponent("§c目标数量必须大于当前数量."));
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