package io.github.dingcouqui.stackmore.commands;

import io.github.dingcouqui.stackmore.StackMorePlugin;
import io.github.dingcouqui.stackmore.util.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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