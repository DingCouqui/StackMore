package io.github.dingcouqui.stackmore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

/**
 * 文本工具类。
 *
 * <p>提供传统 Minecraft 颜色代码（{@code &} 前缀）与 Adventure
 * {@link Component} 之间的转换。插件内部消息均以传统颜色字符串
 * 存储（兼容 YAML 配置），发送时通过 {@link #toComponent(String)}
 * 转为 Adventure 格式。</p>
 */
public class TextUtils {

    /** 使用 {@code &} 作为颜色代码前缀的 Legacy 序列化器 */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    /**
     * 将 {@code &} 前缀的颜色代码转为 Minecraft 原生颜色字符（§）。
     *
     * @param text 含 {@code &} 颜色代码的原始文本
     * @return 转换后的彩色字符串
     */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * 将传统颜色字符串（含 {@code &} 或 § 代码）转为 Adventure Component。
     *
     * @param text 传统格式颜色字符串
     * @return Adventure Component，可直接通过 {@code player.sendMessage()} 等发送
     */
    public static Component toComponent(String text) {
        return LEGACY.deserialize(text);
    }

    /**
     * 去除字符串中的所有 Minecraft 颜色代码。
     *
     * @param text 含颜色代码的字符串
     * @return 纯文本
     */
    public static String stripColor(String text) {
        return ChatColor.stripColor(text);
    }
}
