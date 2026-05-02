package io.github.dingcouqui.stackmore.config;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final StackMorePlugin plugin;
    private File configFile;
    private FileConfiguration config;

    private String language;
    private int maxStackMultiplier;
    private boolean hudEnabled;
    private Set<Material> disabledMaterials;

    public ConfigManager(StackMorePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        language = config.getString("language", "zh_cn");
        maxStackMultiplier = config.getInt("max_stack_multiplier", 54);
        hudEnabled = config.getBoolean("hud_enabled", true);

        disabledMaterials = new HashSet<>();
        List<String> list = config.getStringList("disabled_materials");
        for (String s : list) {
            try {
                Material mat = Material.valueOf(s.toUpperCase());
                disabledMaterials.add(mat);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid material in disabled_materials: " + s);
            }
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml");
        }
    }

    public int getMaxStackMultiplier() {
        return maxStackMultiplier;
    }

    public int getMaxStackSize() {
        return 64 * maxStackMultiplier;
    }

    /**
     * 检查材料是否被禁用。所有潜影盒默认禁止。
     */
    public boolean isMaterialDisabled(Material material) {
        if (material == null) return true;
        // 硬编码禁止所有潜影盒
        if (material.name().endsWith("SHULKER_BOX")) {
            return true;
        }
        return disabledMaterials.contains(material);
    }

    public String getLanguage() {
        return language;
    }

    public boolean isHudEnabled() {
        return hudEnabled;
    }

    public FileConfiguration getConfig() {
        return config;
    }
}