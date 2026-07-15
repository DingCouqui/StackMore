# Design: `/stackmore setlanguage` 命令

## 概述

为管理员提供游戏内切换 StackMore 插件语言的命令，无需手动编辑 `config.yml`。

## 需求

- 管理员通过命令切换语言，切换后立即生效
- 语言设置持久化到 `config.yml`，服务器重启后保持
- 权限要求：`stackmore.admin`

## 命令格式

```
/stackmore setlanguage <语言代码>  ← 仅写入 config.yml，不自动重载
/stackmore reload                  ← 手动重载配置（需另行执行）
/stackmore                         ← 无参数，返回用法提示
```

**注意**：`setlanguage` 和 `reload` 是独立的两个操作。管理员需先 `setlanguage` 切换语言，再手动执行 `reload` 使新语言生效。

## 实现方案

### 1. `ConfigManager.java` — 新增 `setLanguage()` 方法

```java
public void setLanguage(String lang) {
    this.language = lang;
    config.set("language", lang);
    saveConfig();
}
```

### 2. `ReloadCommand.java` — 扩展子命令路由

在 `onCommand()` 中根据 `args[0]` 分派：

| `args[0]` | 行为 |
|---|---|
| `"reload"` | 现有逻辑（重载全部配置） |
| `"setlanguage"` | 校验参数 → 校验文件存在 → `configManager.setLanguage(lang)` → 返回成功提示（不自动重载） |
| 其他/空 | 显示用法提示 |

### 3. 消息键

在 7 个语言文件中新增：

| 键 | 用途 | 示例值 |
|---|---|---|
| `language_changed` | 成功切换 | `&a语言已切换为 %language%。` |
| `language_not_found` | 文件不存在 | `&c语言文件 %language%.yml 不存在。` |
| `setlanguage_usage` | 参数缺失 | `&e用法: /stackmore setlanguage <语言代码>` |

### 4. 错误处理

| 场景 | 返回消息 |
|---|---|
| 无 `stackmore.admin` 权限 | `no_permission` |
| 缺少语言代码参数 | `setlanguage_usage` |
| 语言文件不存在 | `language_not_found` |
| 切换成功 | `language_changed` |

## 文件变更

| 文件 | 改动 |
|---|---|
| `ConfigManager.java` | 新增 `setLanguage(String)` |
| `ReloadCommand.java` | 扩展 `onCommand()` 添加 `setlanguage` 分支 |
| 7 个 `lang/*.yml` | 新增 3 个消息键 |

## 不涉及

- 不修改 `plugin.yml`（`/stackmore` 命令已注册，子命令路由由 Java 处理）
- 不新建文件
- 不修改 `StackMorePlugin.java`
