package io.github.dingcouqui.stackmore.commands;

import io.github.dingcouqui.stackmore.item.StackItemManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 堆叠命令的共用工具方法。
 *
 * <p>从 {@link StackCommand} 中抽取的可复用逻辑，供 {@link StackCommand}
 * 和 {@link StackToCommand} 共用。提供从玩家背包中扫描并吸收同种普通物品的能力。</p>
 */
public class StackCommandHelper {

    /**
     * 从玩家背包中扫描并吸收指定数量的同种普通物品。
     *
     * <p>扫描规则：</p>
     * <ol>
     *   <li>遍历背包 0-35 号槽位（跳过手持槽位），找到非特殊堆叠的同种物品</li>
     *   <li>从每个格子取走所需数量，该格归零时清空</li>
     *   <li>背包吸完后如仍不足，从副手补充</li>
     * </ol>
     *
     * @param player   目标玩家
     * @param material 要吸收的材料类型
     * @param needed   需要的数量
     * @return 实际吸收的数量，可能小于 {@code needed}
     */
    public static int absorbFromPlayer(Player player, Material material, int needed) {
        PlayerInventory inv = player.getInventory();
        int absorbed = 0;

        for (int i = 0; i < 36; i++) {
            if (i == inv.getHeldItemSlot()) continue;
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == material
                    && !StackItemManager.isSpecialStack(item)
                    && !StackItemManager.hasExternalPDCTags(item)) {
                int take = Math.min(needed - absorbed, item.getAmount());
                item.setAmount(item.getAmount() - take);
                if (item.getAmount() <= 0) inv.clear(i);
                absorbed += take;
                if (absorbed >= needed) break;
            }
        }

        if (absorbed < needed) {
            ItemStack off = inv.getItemInOffHand();
            if (off != null && off.getType() == material
                    && !StackItemManager.isSpecialStack(off)
                    && !StackItemManager.hasExternalPDCTags(off)) {
                int take = Math.min(needed - absorbed, off.getAmount());
                off.setAmount(off.getAmount() - take);
                if (off.getAmount() <= 0) inv.setItemInOffHand(null);
                absorbed += take;
            }
        }
        return absorbed;
    }
}
