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

/**
 * 插件配置管理器。
 *
 * <p>负责加载、缓存并提供对 {@code config.yml} 的访问。主要配置项：</p>
 * <ul>
 *   <li>{@code language} — 语言文件名前缀（如 {@code zh_cn}、{@code en_us}）</li>
 *   <li>{@code max_stack_multiplier} — 最大堆叠倍数，实际上限 = 64 × 倍数（默认 54 → 3456）</li>
 *   <li>{@code hud_enabled} — 是否启用动作栏 HUD 显示</li>
 *   <li>{@code disabled_materials} — 禁止堆叠的材料列表（Material 枚举名）</li>
 * </ul>
 *
 * <p><b>安全硬编码：</b>所有潜影盒（名称以 {@code SHULKER_BOX} 结尾的材料）
 * 无条件禁止堆叠，防止利用潜影盒的内部存储实现物品复制。</p>
 */
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

    /**
     * 从磁盘加载配置文件。如文件不存在则从 jar 资源中复制默认配置。
     * 解析 {@code disabled_materials} 列表时对无效材料名输出警告日志。
     */
    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        language = config.getString("language", "en_us");
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

    /** 将当前内存中的配置写回磁盘文件。 */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml");
        }
    }

    /** @return 配置的最大堆叠倍数 */
    public int getMaxStackMultiplier() {
        return maxStackMultiplier;
    }

    /**
     * @return 实际最大堆叠数量 = {@code 64 × max_stack_multiplier}
     */
    public int getMaxStackSize() {
        return 64 * maxStackMultiplier;
    }

    /**
     * 检查指定材料是否被禁止堆叠。
     *
     * <p>两层检查：</p>
     * <ol>
     *   <li>硬编码：所有潜影盒（{@code *_SHULKER_BOX}）无条件禁止</li>
     *   <li>配置列表：{@code disabled_materials} 中列出的材料</li>
     * </ol>
     *
     * @param material 要检查的材料
     * @return {@code true} 如果该材料不允许堆叠
     */
    public boolean isMaterialDisabled(Material material) {
        if (material == null) return true;
        // 硬编码禁止所有潜影盒，防止利用其内部存储刷物品
        if (material.name().endsWith("SHULKER_BOX")) {
            return true;
        }
        return disabledMaterials.contains(material);
    }

    /**
     * 切换语言并持久化到 config.yml。
     *
     * <p>更新内存中的语言设置，写入配置文件。
     * 调用者需自行调用 {@code plugin.reload()} 使新语言即时生效。</p>
     *
     * @param lang 语言文件名前缀（如 {@code "zh_cn"}）
     */
    public void setLanguage(String lang) {
        this.language = lang;
        config.set("language", lang);
        saveConfig();
    }

    /** @return 当前语言文件名前缀（如 {@code zh_cn}） */
    public String getLanguage() {
        return language;
    }

    /** @return 是否启用动作栏 HUD */
    public boolean isHudEnabled() {
        return hudEnabled;
    }

    /** @return 原始 FileConfiguration 对象，供高级用途 */
    public FileConfiguration getConfig() {
        return config;
    }
}
