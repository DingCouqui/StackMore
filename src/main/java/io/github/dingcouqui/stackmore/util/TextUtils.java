package io.github.dingcouqui.stackmore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public class TextUtils {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static Component toComponent(String text) {
        return LEGACY.deserialize(text);
    }

    public static String stripColor(String text) {
        return ChatColor.stripColor(text);
    }
}