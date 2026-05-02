package io.github.dingcouqui.stackmore.commands;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.item.StackItemManager;
import io.github.dingcouqui.stackmore.util.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class StackInfoCommand implements CommandExecutor {

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

        List<String> format = StackMorePlugin.getMessageManager().getStringList("info_format");
        if (format.isEmpty()) {
            format = List.of("&6Info: &e%amount%");
        }

        // 获取可翻译物品名并转为传统颜色格式
        Component itemNameComp = Component.translatable(hand.getType().translationKey());
        String itemName = LegacyComponentSerializer.legacySection().serialize(itemNameComp);

        String uuid = StackItemManager.getUUID(hand);
        String amount = String.valueOf(StackItemManager.getAmount(hand));
        String max = String.valueOf(StackMorePlugin.getConfigManager().getMaxStackSize());
        String owner = StackItemManager.getOwnerName(hand);
        String ownerUUID = StackItemManager.getOwnerUUID(hand);

        for (String line : format) {
            line = line
                    .replace("%item%", itemName)
                    .replace("%uuid%", uuid)
                    .replace("%amount%", amount)
                    .replace("%max%", max)
                    .replace("%owner%", owner)
                    .replace("%owner_uuid%", ownerUUID);
            player.sendMessage(TextUtils.toComponent(line));
        }
        return true;
    }
}