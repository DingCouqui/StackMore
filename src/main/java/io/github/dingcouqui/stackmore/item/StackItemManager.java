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

/**
 * 特殊堆叠物品管理器。
 *
 * <p>通过 Bukkit PersistentDataContainer (PDC) 在物品的 NBT 中持久化存储
 * 真实堆叠数量及元数据。物品本身的原版 {@code amount} 字段始终为 1，
 * 防止 Minecraft 客户端拒绝超量堆叠。</p>
 *
 * <h3>PDC 键</h3>
 * <ul>
 *   <li>{@code stack_uuid} — 唯一标识符（{@link String}）</li>
 *   <li>{@code stack_amount} — 真实数量（{@link Integer}）</li>
 *   <li>{@code stack_owner_name} — 创建者名称（{@link String}）</li>
 *   <li>{@code stack_owner_uuid} — 创建者 UUID（{@link String}）</li>
 * </ul>
 *
 * <p>特殊堆叠物品视觉特征：耐久附魔光效（隐藏附魔标签）+ 物品 lore 显示当前数量。</p>
 */
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

    /**
     * 判断物品是否为特殊堆叠。
     *
     * @param item 要检查的物品
     * @return {@code true} 如果物品的 PDC 中包含 {@code stack_uuid} 键
     */
    public static boolean isSpecialStack(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(KEY_UUID, PersistentDataType.STRING);
    }

    /**
     * 获取特殊堆叠的真实数量。
     *
     * @param item 物品
     * @return 真实数量；如果是普通物品则返回其 {@code amount}
     */
    public static int getAmount(ItemStack item) {
        if (!isSpecialStack(item)) return item.getAmount();
        ItemMeta meta = item.getItemMeta();
        Integer amount = meta.getPersistentDataContainer().get(KEY_AMOUNT, PersistentDataType.INTEGER);
        return amount != null ? amount : 0;
    }

    /**
     * 设置特殊堆叠的真实数量，并同步更新物品 lore。
     *
     * @param item   特殊堆叠物品
     * @param amount 新的真实数量
     */
    public static void setAmount(ItemStack item, int amount) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(KEY_AMOUNT, PersistentDataType.INTEGER, amount);
        updateLore(meta, amount);
        item.setItemMeta(meta);
    }

    /** @return 特殊堆叠的 UUID 字符串，普通物品返回 {@code null} */
    public static String getUUID(ItemStack item) {
        if (!isSpecialStack(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_UUID, PersistentDataType.STRING);
    }

    /** @return 创建者名称，普通物品返回 {@code null} */
    public static String getOwnerName(ItemStack item) {
        if (!isSpecialStack(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_OWNER_NAME, PersistentDataType.STRING);
    }

    /** @return 创建者 UUID 字符串，普通物品返回 {@code null} */
    public static String getOwnerUUID(ItemStack item) {
        if (!isSpecialStack(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_OWNER_UUID, PersistentDataType.STRING);
    }

    /**
     * 从普通物品创建一个新的特殊堆叠物品。
     *
     * <p><b>安全措施：</b>使用 {@code new ItemStack(type, 1)} 创建全新物品，
     * 彻底清除原始物品的所有 NBT 数据（包括容器方块如箱子、熔炉等的内部物品清单），
     * 防止通过特殊堆叠复制物品。仅保留原始物品的自定义显示名称（如铁砧重命名）。</p>
     *
     * @param normalItem 原始普通物品
     * @param amount     初始真实数量
     * @param ownerName  创建者名称
     * @param ownerUUID  创建者 UUID
     * @return 新创建的特殊堆叠物品
     */
    public static ItemStack createSpecialStack(ItemStack normalItem, int amount, String ownerName, UUID ownerUUID) {
        // 使用无参构造全新物品，清除所有原有 NBT（防止容器方块物品复制）
        ItemStack item = new ItemStack(normalItem.getType(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // 保留原物品的自定义显示名称（如铁砧重命名）
        ItemMeta originalMeta = normalItem.getItemMeta();
        if (originalMeta != null && originalMeta.hasDisplayName()) {
            meta.setDisplayName(originalMeta.getDisplayName());
        }

        // 设置 PDC 元数据
        meta.getPersistentDataContainer().set(KEY_UUID, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.getPersistentDataContainer().set(KEY_AMOUNT, PersistentDataType.INTEGER, amount);
        meta.getPersistentDataContainer().set(KEY_OWNER_NAME, PersistentDataType.STRING, ownerName);
        meta.getPersistentDataContainer().set(KEY_OWNER_UUID, PersistentDataType.STRING, ownerUUID.toString());

        // 附魔光效（隐藏附魔标签）
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        updateLore(meta, amount);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 更新物品的 lore 显示当前数量。
     */
    private static void updateLore(ItemMeta meta, int amount) {
        List<String> lore = new ArrayList<>();
        lore.add("§e数量: §b" + amount);
        meta.setLore(lore);
    }

    /**
     * 放置方块后调整物品状态。
     *
     * <p>放置一个方块后数量减 1。根据剩余数量决定返回类型：</p>
     * <ul>
     *   <li>≤ 64 — 退化为普通堆叠物品</li>
     *   <li>&gt; 64 — 保留特殊堆叠，更新数量</li>
     * </ul>
     *
     * @param specialItem 原始特殊堆叠物品
     * @param newAmount   放置后的新数量
     * @return 调整后的物品（可能是普通 ItemStack 或更新后的特殊堆叠）
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
