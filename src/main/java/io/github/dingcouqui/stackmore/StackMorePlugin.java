package io.github.dingcouqui.stackmore;

import io.github.dingcouqui.stackmore.commands.*;
import io.github.dingcouqui.stackmore.config.ConfigManager;
import io.github.dingcouqui.stackmore.config.MessageManager;
import io.github.dingcouqui.stackmore.listeners.BlockListener;
import io.github.dingcouqui.stackmore.listeners.HudTask;
import io.github.dingcouqui.stackmore.listeners.InventoryListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * StackMore 插件主类。
 *
 * <p>核心功能：突破 Minecraft 原版 64 物品堆叠上限，将大量同种方块压缩到
 * 单个"特殊堆叠"物品格中，通过 PersistentDataContainer (PDC) 存储真实数量。
 * 最大容量为 {@code 64 × max_stack_multiplier}（默认 3456 个/格）。</p>
 *
 * <h3>模块划分</h3>
 * <ul>
 *   <li>{@code commands} — 6 条玩家和管理员命令（堆叠/解压/查询/重载）</li>
 *   <li>{@code config} — 配置管理器与多语言消息管理器</li>
 *   <li>{@code item} — 特殊堆叠物品的 PDC 读写与生命周期管理</li>
 *   <li>{@code listeners} — 方块放置恢复、背包操作限制、动作栏 HUD 刷新</li>
 *   <li>{@code util} — Adventure 文本工具</li>
 * </ul>
 */
public class StackMorePlugin extends JavaPlugin {

    private static StackMorePlugin instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private HudTask hudTask;

    /**
     * 插件启用入口。初始化配置与消息管理器，注册全部命令、事件监听器，
     * 并启动每秒一次的动作栏 HUD 任务（20 tick 周期）。
     */
    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);

        reload();

        // Register commands
        getCommand("stack").setExecutor(new StackCommand());
        getCommand("sm").setExecutor(new StackCommand());
        getCommand("stk").setExecutor(new StackCommand());
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

    /**
     * 插件禁用时取消 HUD 定时任务，避免残留在调度器中。
     */
    @Override
    public void onDisable() {
        if (hudTask != null) hudTask.cancel();
    }

    /**
     * 重载配置与语言文件。由 {@code /stackmore reload} 命令和 {@link #onEnable()} 调用。
     */
    public void reload() {
        configManager.loadConfig();
        messageManager.loadMessages(configManager.getLanguage());
    }

    /** @return 插件单例实例 */
    public static StackMorePlugin getInstance() {
        return instance;
    }

    /** @return 当前配置管理器 */
    public static ConfigManager getConfigManager() {
        return instance.configManager;
    }

    /** @return 当前消息管理器 */
    public static MessageManager getMessageManager() {
        return instance.messageManager;
    }
}
