package io.github.dingcouqui.stackmore.commands;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.item.StackItemManager;
import io.github.dingcouqui.stackmore.util.CommandUtils;
import io.github.dingcouqui.stackmore.util.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * {@code /stackinfo} 命令执行器。
 *
 * <p>查看手持特殊堆叠物品的详细信息，包括物品翻译名、唯一 UUID、
 * 当前数量 / 最大容量、创建者名称和 UUID。输出格式由语言文件中的
 * {@code info_format} 列表定义，支持占位符和颜色代码。</p>
 */
public class StackInfoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = CommandUtils.validatePlayerWithPermission(sender, "stackmore.use");
        if (player == null) return true;

        ItemStack hand = CommandUtils.getHeldSpecialStack(player);
        if (hand == null) return true;

        List<String> format = StackMorePlugin.getMessageManager().getStringList("info_format");
        if (format.isEmpty()) {
            format = List.of("&6Info: &e%amount%");
        }

        // 通过 Adventure translatable 获取客户端可翻译的物品名，再转换回传统颜色格式输出
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
