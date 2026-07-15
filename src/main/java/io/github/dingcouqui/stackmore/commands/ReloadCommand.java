package io.github.dingcouqui.stackmore.commands;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.config.ConfigManager;
import io.github.dingcouqui.stackmore.config.MessageManager;
import io.github.dingcouqui.stackmore.util.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * {@code /stackmore reload} 和 {@code /stackmore setlanguage <lang>} 命令执行器。
 *
 * <p>重载插件的 {@code config.yml} 和语言文件，无需重启服务器。
 * 也可用于切换语言设置。
 * 需要 {@code stackmore.admin} 权限。</p>
 */
public class ReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("stackmore.admin")) {
            sender.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("no_permission")));
            return true;
        }

        if (args.length == 0) {
            showUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "setlanguage" -> handleSetLanguage(sender, args);
            default -> showUsage(sender);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        StackMorePlugin.getInstance().reload();
        sender.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("reload_success")));
    }

    private void handleSetLanguage(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("setlanguage_usage")));
            return;
        }

        String lang = args[1].toLowerCase();
        ConfigManager configManager = StackMorePlugin.getConfigManager();
        MessageManager messageManager = StackMorePlugin.getMessageManager();

        // 检查语言文件是否存在
        File langFile = new File(StackMorePlugin.getInstance().getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            sender.sendMessage(TextUtils.toComponent(messageManager.get("language_not_found", "%language%", lang)));
            return;
        }

        // 切换语言并持久化
        configManager.setLanguage(lang);
        sender.sendMessage(TextUtils.toComponent(messageManager.get("language_changed", "%language%", lang)));
    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage(TextUtils.toComponent("&e用法: /stackmore reload | setlanguage <语言代码>"));
    }
}
