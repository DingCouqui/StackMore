# AGENTS.md

## Tech stack
- Java 21, Gradle (Groovy DSL), PaperMC 1.21.3 plugin
- Single module at `src/main/java/io/github/dingcouqui/stackmore/`

## Build & run

```bash
gradle build        # produces build/libs/StackMore-{version}.jar
gradle clean        # deletes build/
gradle wrapper      # generate gradlew (no wrapper committed)
```

- **No Gradle wrapper is committed.** If `gradlew` is missing, install Gradle locally and run `gradle wrapper` first.
- Java 21 toolchain is enforced in `build.gradle`.
- `processResources` expands `${version}` from `build.gradle` into `plugin.yml`. Both must be in sync — `plugin.yml` already uses `version: "${version}"`.

## Commands (for verification)

| Task | Command |
|---|---|
| Build | `gradle build` |
| Clean | `gradle clean` |
| Output JAR | `build/libs/StackMore-{version}.jar` (currently `1.0.2`) |

## Testing

**There are no tests.** No `src/test/`, no JUnit, no test dependencies in `build.gradle`. The only verification is that `gradle build` compiles successfully.

## Lint & format

**None configured.** No Checkstyle, SpotBugs, formatter, or `.editorconfig`. Do not introduce linter config unless asked.

## Architecture notes

### Plugin entrypoint
`StackMorePlugin.java` — standard `JavaPlugin`. In `onEnable()`:
- Registers commands: `stack` (+ aliases `sm`, `stk`), `stackto`, `unstack`, `unstackto`, `stackinfo`, `stackmore` via `getCommand().setExecutor()`
- Registers **2 event listeners**: `BlockListener`, `InventoryListener`
- Starts **1 repeating HUD task**: `HudTask` (BukkitRunnable, 20 tick period)

`HudTask` shows item name + amount on the ActionBar for players holding a special stack; gated by `hud_enabled` config.

### PDC-based item stacking
The core trick: items use `PersistentDataContainer` (NBT) to store real stack counts while keeping the vanilla `amount` field at 1. This bypasses the client's 64-stack limit. Key class: `StackItemManager.java`.

PDC keys stored per-item (`StackItemManager.java:42-48`):
- `stack_uuid` → `PersistentDataType.STRING` — unique stack identifier
- `stack_amount` → `PersistentDataType.INTEGER` — current count
- `stack_owner_name` → `PersistentDataType.STRING` — creator name
- `stack_owner_uuid` → `PersistentDataType.STRING` — creator UUID (serialized as String, NOT native UUID type)

### Critical safety patterns (do NOT break these)

1. **NBT clearing on creation** (`StackItemManager.java:122`): `new ItemStack(type, 1)` creates an entirely new item, stripping ALL NBT from the original (including container inventories for chests, furnaces, shulker boxes). This prevents item duplication. Only custom display names (anvil-renamed) are preserved (lines 127-130).

2. **Shulker box hard-ban** (`ConfigManager.java:106`): `material.name().endsWith("SHULKER_BOX")` blocks all shulker box variants unconditionally, enforced at `StackCommand.java:48` via `isMaterialDisabled()`. This is **NOT configurable** — do not make it so without careful thought about NBT-based dupes.

3. **Block placement dual-priority** (`BlockListener.java:47-95`): Uses `LOWEST` (`ignoreCancelled=true`) → `MONITOR` (`ignoreCancelled=false`) priority pair. Saves a clone of the special stack before placement (line 54), restores it (amount-1) after placement unless the event was cancelled (line 73). If a protection plugin cancels the event at a higher priority, the item is consumed normally. Do NOT collapse this into a single handler.

4. **Crafting GUI restrictions** (`InventoryListener.java`): Six container types block special stacks — `WORKBENCH`, `ANVIL`, `GRINDSTONE`, `SMITHING`, `ENCHANTING`, `CRAFTING` (player 2x2 grid). Cursor-clicks, drags, shift-clicks, and double-click collect involving special stacks in these GUIs are all cancelled; only `PICKUP_ALL`, `PLACE_ALL`, `SWAP_WITH_CURSOR`, `HOTBAR_SWAP`, and `MOVE_TO_OTHER_INVENTORY` are allowed.

5. **Third-party PDC protection** (`StackItemManager.java:hasExternalPDCTags()`): Before absorbing items during `/stack` or `/stackto`, and before converting a held item into a special stack via `/stack`, items are checked for PDC keys from external plugins (namespaces other than `minecraft` or `stackmore`). This prevents accidental destruction of specially-tagged items from plugins like Infinite-Blocks. Applied in three locations:
   - `StackCommand.onCommand()` — rejects held items with external PDC tags
   - `StackCommand.absorbFromPlayer()` — skips external-tagged items during absorption
   - `StackCommandHelper.absorbFromPlayer()` — same (shared by `/stackto` path)

### Config & messages

- `ConfigManager.java` loads `plugins/StackMore/config.yml`; `MessageManager.java` loads `plugins/StackMore/lang/{language}.yml`.
- `config.yml` in `src/main/resources/` is the default — it gets copied on first load.
- Default language is **`en_us`** (`config.yml` default + `ConfigManager.java` fallback). If the configured language file doesn't exist, `MessageManager.java` falls back to **`en_us.yml`**.
- 7 built-in language files under `src/main/resources/lang/`: `zh_cn.yml`, `en_us.yml`, `de_de.yml`, `ru_ru.yml`, `es_es.yml`, `fr_fr.yml`, `ja_jp.yml`.
- `config.yml` settings: `language`, `max_stack_multiplier` (max stack = 64 × multiplier), `hud_enabled`, `disabled_materials` (additional block materials to reject).

## Conventions

- Javadoc is bilingual: **class-level docs in Chinese**, **method-level docs in English**. (Private methods may deviate.)
- Inline source comments are in Chinese; strings visible to players are in `lang/*.yml`.
- Plugin uses Adventure Component API for text; `TextUtils.java` provides `colorize()` (&→§), `toComponent()` (&→Component), and `stripColor()`.
- `StackMorePlugin` exposes a static singleton (`getInstance()`, `getConfigManager()`, `getMessageManager()`) — used throughout the codebase.
- `folia-supported: false` in `plugin.yml` — do not claim Folia compatibility.

## CI/CD

**None.** No `.github/workflows/`, no pre-commit hooks, no Docker.

## Git

- Repo: `github.com/DingCouqui/StackMore`, branches: `main` (upstream), `BilicraftVersion` (customised).
- `.deepseek/`, `.omo/`, `Reference/`, and `session_*.json` are gitignored (AI assistant traces).
- `gradle/wrapper/gradle-wrapper.jar` is whitelisted in `.gitignore` (`!gradle/wrapper/gradle-wrapper.jar`), but the wrapper directory doesn't exist yet.

## Local build notes

- Gradle is **not** in PATH. Available extracted distributions in `~/.gradle/wrapper/dists/`: `gradle-8.8`, `gradle-9.5.0`, `gradle-9.6.1`.
- Java 21 is at `C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot`. Set `JAVA_HOME` before running Gradle:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
  & "C:\Users\Ding_\.gradle\wrapper\dists\gradle-8.8-bin\4u0rgm4geyrm56fyhoco0c9in\gradle-8.8\bin\gradle.bat" build
  ```
