package io.github.dingcouqui.stackmore.config;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class MessageManager {

    private final StackMorePlugin plugin;
    private YamlConfiguration messages;

    public MessageManager(StackMorePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadMessages(String language) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        // Save default language files from resources if not exist
        saveDefaultLang("zh_cn.yml");
        saveDefaultLang("en_us.yml");

        File langFile = new File(langFolder, language + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file " + language + ".yml not found, falling back to zh_cn.yml");
            langFile = new File(langFolder, "zh_cn.yml");
        }
        messages = YamlConfiguration.loadConfiguration(langFile);
    }

    private void saveDefaultLang(String fileName) {
        File file = new File(plugin.getDataFolder(), "lang/" + fileName);
        if (!file.exists()) {
            plugin.saveResource("lang/" + fileName, false);
        }
    }

    public String get(String path) {
        return messages.getString(path, "");
    }

    public String get(String path, String... replacements) {
        String msg = get(path);
        if (msg == null) return "";
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public List<String> getStringList(String path) {
        return messages.getStringList(path);
    }
}