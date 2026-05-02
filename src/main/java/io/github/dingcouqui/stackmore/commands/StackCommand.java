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
                player.getInventory().setItemInMainHand(hand); // ★ 更新客户端显示
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
        player.getInventory().setItemInMainHand(hand); // ★ 更新客户端显示

        String msgPath = newTotal >= maxSize ? "stack_limit" : "stack_success";
        player.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get(msgPath, "%amount%", String.valueOf(absorbed), "%total%", String.valueOf(newTotal))));
        return true;
    }

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