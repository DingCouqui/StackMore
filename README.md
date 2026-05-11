# StackMore - Super Stackable Blocks Plugin

[中文版本](#chinese) | [English](#english)

---

<a id="english"></a>
## English

### Overview
StackMore is a Paper plugin that allows players to compress more than 64 blocks into a **special oversized stackable item**, breaking the vanilla stack limit while preserving visual identity and placement logic.  
It supports quantity control, anti-split protection, and compatibility with land protection plugins — ideal for survival and building servers.

### Features
- **Super Stacking**: Convert normal blocks into special stacks storing up to **64 × max_stack_multiplier** blocks.
- **Smart Placement**: Placing blocks decrements the internal count; when it reaches 64 or less, the item automatically reverts to a normal stack.
- **Flexible Unstacking**: Partially or fully unstack items; excess items that don't fit in the inventory are dropped on the ground.
- **Admin Mode**: Players with `stackmore.admin` can set any amount directly via `/stack` or `/stackto` without consuming inventory items.
- **Secure**: All shulker boxes are forcibly disabled; container blocks (chests, furnaces, etc.) have NBT cleared when stacked to prevent item duplication.
- **HUD Display**: ActionBar shows item name and current amount (can be toggled).
- **Multi-language**: Built-in Chinese and English messages, fully customisable.

### Commands

| Command | Arguments | Description | Permission |
|---------|-----------|-------------|------------|
| `/stack` | `[amount \| all]` | Convert held normal block to a special stack and absorb the same type from inventory; `all` stacks to the maximum limit. Admins set directly to the limit without consuming items. | `stackmore.use` |
| `/stackto` | `<amount>` | Fill up to the specified amount (only if greater than current). Admins set directly. | `stackmore.use` |
| `/unstack` | `[amount \| all]` | Unstack as much as possible (keep remainder); `all` fully unstacks and drops excess; with an amount less than current, unstacks that many. | `stackmore.use` |
| `/unstackto` | `<amount>` | Unstack until the held stack contains exactly the specified amount (excess dropped if inventory full). | `stackmore.use` |
| `/stackinfo` | none | Show details of the held special stack (item, UUID, amount, owner). | `stackmore.use` |
| `/stackmore reload` | none | Reload configuration and language files. | `stackmore.admin` |

- `stackmore.use` is default for all players.
- `stackmore.admin` is default for OPs only.

### Configuration
On first start, the plugin generates `config.yml` and `lang/` inside `plugins/StackMore/`.

**Sample `config.yml`**:
```yaml
language: zh_cn              # Language file (inside lang/)
max_stack_multiplier: 54     # Max stack = 64 × 54 = 3456
hud_enabled: true            # Toggle ActionBar display
disabled_materials: []       # Extra block materials to disable (uppercase), e.g., ["BEDROCK"]
```
Language files (`lang/zh_cn.yml`, `lang/en_us.yml`) contain all messages, supporting `&` colour codes.

### Usage
1. Place the plugin Jar in `plugins/`, restart/reload the server.
2. **Create a special stack**: hold a block (no shulker boxes) and run `/stack all` to merge that block type from your inventory.
3. **Place blocks**: place as normal; count decreases. When it falls to ≤ 64, it becomes a normal stack.
4. **Unstack**: hold a special stack and run `/unstack 64` to extract 64 blocks (remains if more); `/unstack all` unstacks everything.
5. **Admin**: with `stackmore.admin`, `/stack 1000` directly sets the held block to 1000 without consuming any items.

### Important Notes
- **Shulker Boxes**: All shulker boxes are hardcoded as disabled — cannot be stacked to prevent content duplication.
- **Container Blocks**: Chests, furnaces, brewing stands, etc., lose all NBT when turned into a special stack; placed as empty containers.
- **Protection Plugins**: If placement is blocked by another plugin (e.g., WorldGuard), one item is consumed as normal.
- **Compatibility**: Built for **Paper 1.21.3+**, works on 1.21.x. Adjust API for older versions.

### FAQ
**Q: Why can't I stack shulker boxes?**  
A: For security, all shulker boxes are permanently disabled and cannot be overridden in config.

**Q: The amount didn't change after `/stack`?**  
A: As a non-admin, your inventory must contain enough normal blocks of that type. Otherwise a "reached limit" message appears.

**Q: Blocks disappear after placement?**  
A: The plugin handles block placement carefully; if you still see issues, check for conflicting plugins (e.g. anti‑cheat) and server logs.

**Q: How do I change the language?**  
A: Set `language` in `config.yml` to `en_us` (or any custom filename) and place the corresponding file in `lang/`.

### Build & Development
Build with Gradle: `gradle build` → Jar in `build/libs/`.  
Source: [GitHub Repository](https://github.com/DingCouqui/StackMore)

> **Note:** This plugin was developed with AI assistance, including code generation,
> architecture design, and documentation review.

### License
This project is licensed under the **GNU General Public License v3.0**.  
See [LICENSE](LICENSE) for the full text.

---

<a id="chinese"></a>
## 中文

### 插件概述
StackMore 是一个 Paper 插件，允许玩家将超过 64 个的方块压缩为**特殊超大堆叠物品**，突破原版堆叠限制，同时保留物品视觉与放置逻辑。  
支持数量控制、防拆分保护、与领地/保护插件兼容，适合生存与建筑服务器。

### 功能特点
- **超大堆叠**：将普通方块转换为特殊堆叠，存储高达 **64 × max_stack_multiplier** 个方块。
- **智能放置**：放置方块时数量递减，当数量降至 64 或以下时自动变回普通堆叠。
- **解压灵活**：可部分解压或全部解压；背包满时多余物品掉落地面。
- **管理员模式**：拥有 `stackmore.admin` 权限的玩家使用 `/stack` 或 `/stackto` 时，可直接设定任意数量，不消耗背包物品。
- **安全防护**：所有潜影盒强制禁用；容器类方块（箱子、熔炉等）堆叠时清除 NBT，杜绝物品复制。
- **HUD 显示**：手持特殊堆叠时 ActionBar 显示物品名称与当前数量（可开关）。
- **多语言支持**：内置中文/英文语言文件，消息颜色可自定义。

### 指令列表

| 指令 | 参数 | 说明 | 权限 |
|------|------|------|------|
| `/stack` | `[数量 \| all]` | 将手持普通方块变为特殊堆叠并吸收背包同类物品；`all` 堆叠至上限；管理员直接设为上限 | `stackmore.use` |
| `/stackto` | `<数量>` | 仅当数量 > 当前时，向上填充到指定数量；管理员直接设置 | `stackmore.use` |
| `/unstack` | `[数量 \| all]` | 无参数尽量解压（保留剩余）；`all` 强制全解压并掉落多余物品；带数量且 < 当前则解压该数量 | `stackmore.use` |
| `/unstackto` | `<数量>` | 解压到手中只剩指定数量（背包满则掉落差额） | `stackmore.use` |
| `/stackinfo` | 无 | 查看手持特殊堆叠详细信息 | `stackmore.use` |
| `/stackmore reload` | 无 | 重载配置文件与语言文件 | `stackmore.admin` |

- `stackmore.use` 默认赋予所有玩家。
- `stackmore.admin` 默认仅 OP 拥有。

### 配置文件
首次启动后，插件在 `plugins/StackMore/` 下生成 `config.yml` 和 `lang/` 文件夹。

**`config.yml` 示例**：
```yaml
language: zh_cn              # 语言文件名（位于 lang/）
max_stack_multiplier: 54     # 最大堆叠 = 64 × 54 = 3456
hud_enabled: true            # 是否启用 ActionBar 显示
disabled_materials: []       # 额外禁止堆叠的方块材质（大写），如 ["BEDROCK"]
```
语言文件（`lang/zh_cn.yml`）内可自定义所有提示消息，支持 `&` 颜色代码。

### 使用方法
1. 将插件 Jar 放入 `plugins` 文件夹，重启服务器或执行 `/reload`。
2. **创建特殊堆叠**：手持方块（非潜影盒），输入 `/stack all` 即可将该方块及背包同类普通方块合并为一个特殊堆叠。
3. **放置方块**：像普通方块一样放置，内部计数自动减少；低于 64 时变回原版堆叠。
4. **解压**：手持特殊堆叠，`/unstack 64` 解压出 64 个方块（若剩余多于 64 则保留手中）；`/unstack all` 全部解压。
5. **管理员模式**：拥有 `stackmore.admin` 权限后，`/stack 1000` 直接将手中方块设为 1000 个，无需消耗背包物品。

### 注意事项
- **潜影盒**：所有颜色的潜影盒均禁止堆叠，且无法通过配置文件更改。
- **容器方块**：箱子、熔炉、酿造台等会在创建特殊堆叠时清除内部 NBT，放置后为空容器。
- **保护插件**：放置被第三方插件（如 WorldGuard）阻止时，特殊堆叠正常消耗一个物品。
- **兼容性**：基于 **Paper 1.21.3** 开发，适用于 1.21.x 版本。旧版需自行调整 API。

### 常见问题
**Q：为什么无法堆叠潜影盒？**  
A：出于防止物品复制的安全考量，所有潜影盒被硬编码禁用。

**Q：使用 `/stack` 后数量没变？**  
A：普通玩家需要背包或副手有足够同类方块可供吸收，否则会提示已达上限。

**Q：放置方块后消失？**  
A：插件已妥善处理方块同步；若仍有问题，请检查是否有冲突插件（如反作弊）并查看服务器日志。

**Q：如何更改语言？**  
A：修改 `config.yml` 的 `language` 为 `en_us` 或自定义文件名，并在 `lang/` 下放置对应语言文件即可。

### 构建与开发
使用 Gradle 构建：`gradle build`，Jar 文件生成在 `build/libs/` 下。  
项目地址：[GitHub](https://github.com/DingCouqui/StackMore)

### 许可证
本项目使用 **GNU General Public License v3.0** 开源。  
完整许可证文本请见 [LICENSE](LICENSE) 文件。

---

Feel free to contribute by opening issues or pull requests on the GitHub repository.  
欢迎在 GitHub 仓库提交 Issue 或 Pull Request 参与贡献。
