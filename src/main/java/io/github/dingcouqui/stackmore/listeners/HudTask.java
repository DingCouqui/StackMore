package io.github.dingcouqui.stackmore.listeners;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.item.StackItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 动作栏 HUD 定时任务。
 *
 * <p>每秒（20 tick）遍历所有在线玩家，若其主手持有特殊堆叠物品，
 * 则在动作栏（ActionBar）显示物品翻译名 + 金色数量。
 * 可通过配置文件中的 {@code hud_enabled} 选项关闭。</p>
 *
 * <p>使用 Adventure {@link Component#translatable(String)} 获取
 * 客户端可翻译的物品名，支持多语言客户端自动适配。</p>
 */
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
                // translatable 使客户端根据自身语言设置自动翻译物品名
                Component message = Component.translatable(hand.getType().translationKey())
                        .append(Component.text(" " + amount, NamedTextColor.GOLD));
                player.sendActionBar(message);
            }
        }
    }
}
