package io.github.dingcouqui.stackmore.commands;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.util.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * {@code /stackmore reload} 命令执行器。
 *
 * <p>重载插件的 {@code config.yml} 和语言文件，无需重启服务器。
 * 需要 {@code stackmore.admin} 权限。</p>
 */
public class ReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("stackmore.admin")) {
            sender.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("no_permission")));
            return true;
        }
        StackMorePlugin.getInstance().reload();
        sender.sendMessage(TextUtils.toComponent(StackMorePlugin.getMessageManager().get("reload_success")));
        return true;
    }
}
