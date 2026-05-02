package io.github.dingcouqui.stackmore.commands;

import io.github.dingcouqui.stackmore.item.StackItemManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class StackCommandHelper {

    public static int absorbFromPlayer(Player player, Material material, int needed) {
        PlayerInventory inv = player.getInventory();
        int absorbed = 0;

        for (int i = 0; i < 36; i++) {
            if (i == inv.getHeldItemSlot()) continue;
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == material && !StackItemManager.isSpecialStack(item)) {
                int take = Math.min(needed - absorbed, item.getAmount());
                item.setAmount(item.getAmount() - take);
                if (item.getAmount() <= 0) inv.clear(i);
                absorbed += take;
                if (absorbed >= needed) break;
            }
        }

        if (absorbed < needed) {
            ItemStack off = inv.getItemInOffHand();
            if (off != null && off.getType() == material && !StackItemManager.isSpecialStack(off)) {
                int take = Math.min(needed - absorbed, off.getAmount());
                off.setAmount(off.getAmount() - take);
                if (off.getAmount() <= 0) inv.setItemInOffHand(null);
                absorbed += take;
            }
        }
        return absorbed;
    }
}