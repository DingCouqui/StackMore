package io.github.dingcouqui.stackmore.config;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

/**
 * 多语言消息管理器。
 *
 * <p>根据配置中的 {@code language} 设置加载对应的 YAML 语言文件。
 * 首次启动时自动将 jar 内的默认语言文件（{@code zh_cn.yml}、{@code en_us.yml}、
 * {@code fr_fr.yml}、{@code es_es.yml}、{@code ja_jp.yml}、{@code ru_ru.yml}）
 * 复制到插件数据文件夹的 {@code lang/} 子目录。</p>
 *
 * <p>消息支持 {@code %placeholder%} 格式的占位符替换，
 * 通过 {@link #get(String, String...)} 方法传入键值对即可。</p>
 */
public class MessageManager {

    private final StackMorePlugin plugin;
    private YamlConfiguration messages;

    public MessageManager(StackMorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载指定语言文件。
     *
     * @param language 语言文件名前缀，如 {@code "zh_cn"} 对应 {@code lang/zh_cn.yml}
     */
    public void loadMessages(String language) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        // 从 jar 资源中提取默认语言文件（仅在目标不存在时）
        saveDefaultLang("zh_cn.yml");
        saveDefaultLang("en_us.yml");
        saveDefaultLang("fr_fr.yml");
        saveDefaultLang("es_es.yml");
        saveDefaultLang("ja_jp.yml");
        saveDefaultLang("ru_ru.yml");

        File langFile = new File(langFolder, language + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file " + language + ".yml not found, falling back to en_us.yml");
            langFile = new File(langFolder, "en_us.yml");
        }
        messages = YamlConfiguration.loadConfiguration(langFile);
    }

    /**
     * 将 jar 内默认语言文件复制到数据文件夹（仅在目标不存在时）。
     */
    private void saveDefaultLang(String fileName) {
        File file = new File(plugin.getDataFolder(), "lang/" + fileName);
        if (!file.exists()) {
            plugin.saveResource("lang/" + fileName, false);
        }
    }

    /**
     * 获取原始消息文本（不含占位符替换）。
     *
     * @param path YAML 键路径
     * @return 对应的消息字符串，不存在时返回空串
     */
    public String get(String path) {
        return messages.getString(path, "");
    }

    /**
     * 获取消息文本并替换占位符。
     *
     * <p>替换参数以键值对形式传入：{@code get("key", "%a%", "1", "%b%", "2")}
     * 会将消息中的 {@code %a%} 替换为 {@code "1"}，{@code %b%} 替换为 {@code "2"}。</p>
     *
     * @param path         YAML 键路径
     * @param replacements 占位符-值对（偶数个参数）
     * @return 替换后的消息字符串
     */
    public String get(String path, String... replacements) {
        String msg = get(path);
        if (msg == null) return "";
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    /**
     * 获取 YAML 中的字符串列表。
     *
     * @param path YAML 键路径
     * @return 字符串列表，用于多行消息（如 {@code info_format}）
     */
    public List<String> getStringList(String path) {
        return messages.getStringList(path);
    }
}
