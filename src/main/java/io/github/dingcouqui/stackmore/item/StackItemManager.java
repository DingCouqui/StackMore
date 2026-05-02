package io.github.dingcouqui.stackmore.item;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StackItemManager {

    private static final NamespacedKey KEY_UUID;
    private static final NamespacedKey KEY_AMOUNT;
    private static final NamespacedKey KEY_OWNER_NAME;
    private static final NamespacedKey KEY_OWNER_UUID;

    static {
        StackMorePlugin plugin = StackMorePlugin.getInstance();
        KEY_UUID = new NamespacedKey(plugin, "stack_uuid");
        KEY_AMOUNT = new NamespacedKey(plugin, "stack_amount");
        KEY_OWNER_NAME = new NamespacedKey(plugin, "stack_owner_name");
        KEY_OWNER_UUID = new NamespacedKey(plugin, "stack_owner_uuid");
    }

    public static boolean isSpecialStack(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(KEY_UUID, PersistentDataType.STRING);
    }

    public static int getAmount(ItemStack item) {
        if (!isSpecialStack(item)) return item.getAmount();
        ItemMeta meta = item.getItemMeta();
        Integer amount = meta.getPersistentDataContainer().get(KEY_AMOUNT, PersistentDataType.INTEGER);
        return amount != null ? amount : 0;
    }

    public static void setAmount(ItemStack item, int amount) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(KEY_AMOUNT, PersistentDataType.INTEGER, amount);
        updateLore(meta, amount);
        item.setItemMeta(meta);
    }

    public static String getUUID(ItemStack item) {
        if (!isSpecialStack(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_UUID, PersistentDataType.STRING);
    }

    public static String getOwnerName(ItemStack item) {
        if (!isSpecialStack(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_OWNER_NAME, PersistentDataType.STRING);
    }

    public static String getOwnerUUID(ItemStack item) {
        if (!isSpecialStack(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_OWNER_UUID, PersistentDataType.STRING);
    }

    /**
     * 创建特殊堆叠物品，清除容器类方块的 NBT 防止刷物品。
     */
    public static ItemStack createSpecialStack(ItemStack normalItem, int amount, String ownerName, UUID ownerUUID) {
        // 使用无参构造全新物品，清除所有原有 NBT
        ItemStack item = new ItemStack(normalItem.getType(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // 保留原物品的自定义显示名称（如铁砧重命名）
        ItemMeta originalMeta = normalItem.getItemMeta();
        if (originalMeta != null && originalMeta.hasDisplayName()) {
            meta.setDisplayName(originalMeta.getDisplayName());
        }

        // 设置 PDC
        meta.getPersistentDataContainer().set(KEY_UUID, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.getPersistentDataContainer().set(KEY_AMOUNT, PersistentDataType.INTEGER, amount);
        meta.getPersistentDataContainer().set(KEY_OWNER_NAME, PersistentDataType.STRING, ownerName);
        meta.getPersistentDataContainer().set(KEY_OWNER_UUID, PersistentDataType.STRING, ownerUUID.toString());

        // 附魔光效
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        updateLore(meta, amount);
        item.setItemMeta(meta);
        return item;
    }

    private static void updateLore(ItemMeta meta, int amount) {
        List<String> lore = new ArrayList<>();
        lore.add("§e数量: §b" + amount);
        meta.setLore(lore);
    }

    /**
     * 放置方块后调整物品：数量 ≤64 则转为普通堆叠，否则更新数量。
     */
    public static ItemStack adjustAfterPlacement(ItemStack specialItem, int newAmount) {
        if (newAmount <= 64) {
            ItemStack normal = new ItemStack(specialItem.getType(), newAmount);
            ItemMeta specialMeta = specialItem.getItemMeta();
            if (specialMeta != null && specialMeta.hasDisplayName()) {
                ItemMeta normalMeta = normal.getItemMeta();
                normalMeta.setDisplayName(specialMeta.getDisplayName());
                normal.setItemMeta(normalMeta);
            }
            return normal;
        } else {
            setAmount(specialItem, newAmount);
            return specialItem;
        }
    }
}