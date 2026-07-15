package io.github.dingcouqui.stackmore.util;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.item.StackItemManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 命令通用校验工具。
 *
 * <p>提供所有命令共享的前置校验逻辑，包括玩家类型检查、权限检查和
 * 手持特殊堆叠物品验证，消除各 Command 类中的重复样板代码。</p>
 */
public class CommandUtils {

    /**
     * Validate that the sender is a player.
     *
     * <p>Sends a localized "player_only" message to non-player senders.</p>
     *
     * @param sender the command sender
     * @return the player, or {@code null} if sender is not a player
     */
    public static Player validatePlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(
                    StackMorePlugin.getMessageManager().get("player_only")));
            return null;
        }
        return player;
    }

    /**
     * Validate that the sender is a player with the given permission.
     *
     * <p>Sends localized "player_only" or "no_permission" messages on failure.</p>
     *
     * @param sender     the command sender
     * @param permission the permission node to check
     * @return the player, or {@code null} if validation fails
     */
    public static Player validatePlayerWithPermission(CommandSender sender, String permission) {
        Player player = validatePlayer(sender);
        if (player == null) return null;
        if (!player.hasPermission(permission)) {
            player.sendMessage(TextUtils.toComponent(
                    StackMorePlugin.getMessageManager().get("no_permission")));
            return null;
        }
        return player;
    }

    /**
     * Get the player's main-hand item if it is a valid special stack.
     *
     * <p>Sends localized "not_holding_special" message if the hand item is not
     * a special stack.</p>
     *
     * @param player the player
     * @return the special stack, or {@code null} if not holding one
     */
    public static ItemStack getHeldSpecialStack(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!StackItemManager.isSpecialStack(hand)) {
            player.sendMessage(TextUtils.toComponent(
                    StackMorePlugin.getMessageManager().get("not_holding_special")));
            return null;
        }
        return hand;
    }
}
