package io.github.dingcouqui.stackmore;

import io.github.dingcouqui.stackmore.commands.*;
import io.github.dingcouqui.stackmore.config.ConfigManager;
import io.github.dingcouqui.stackmore.config.MessageManager;
import io.github.dingcouqui.stackmore.listeners.BlockListener;
import io.github.dingcouqui.stackmore.listeners.HudTask;
import io.github.dingcouqui.stackmore.listeners.InventoryListener;
import org.bukkit.plugin.java.JavaPlugin;

public class StackMorePlugin extends JavaPlugin {

    private static StackMorePlugin instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private HudTask hudTask;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);

        reload();

        // Register commands
        getCommand("stack").setExecutor(new StackCommand());
        getCommand("stackto").setExecutor(new StackToCommand());
        getCommand("unstack").setExecutor(new UnstackCommand());
        getCommand("unstackto").setExecutor(new UnstackToCommand());
        getCommand("stackinfo").setExecutor(new StackInfoCommand());
        getCommand("stackmore").setExecutor(new ReloadCommand());

        // Register listeners
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);

        // HUD task
        hudTask = new HudTask(this);
        hudTask.runTaskTimer(this, 20L, 20L);
    }

    @Override
    public void onDisable() {
        if (hudTask != null) hudTask.cancel();
    }

    public void reload() {
        configManager.loadConfig();
        messageManager.loadMessages(configManager.getLanguage());
    }

    public static StackMorePlugin getInstance() {
        return instance;
    }

    public static ConfigManager getConfigManager() {
        return instance.configManager;
    }

    public static MessageManager getMessageManager() {
        return instance.messageManager;
    }
}