package io.github.dingcouqui.stackmore.listeners;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.item.StackItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class HudTask extends BukkitRunnable {

    private final StackMorePlugin plugin;

    public HudTask(StackMorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfigManager().isHudEnabled()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (StackItemManager.isSpecialStack(hand)) {
                int amount = StackItemManager.getAmount(hand);
                // 使用 translatable 使客户端自动翻译物品名
                Component message = Component.translatable(hand.getType().translationKey())
                        .append(Component.text(" " + amount, NamedTextColor.GOLD));
                player.sendActionBar(message);
            }
        }
    }
}