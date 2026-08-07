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
| Output JAR | `build/libs/StackMore-{version}.jar` (currently `1.0.3`) |

## Testing

**There are no tests.** No `src/test/`, no JUnit, no test dependencies in `build.gradle`. The only verification is that `gradle build` compiles successfully.

## Lint & format

**None configured.** No Checkstyle, SpotBugs, formatter, or `.editorconfig`. Do not introduce linter config unless asked.

## Architecture notes

### Plugin entrypoint
`StackMorePlugin.java` — standard `JavaPlugin`. In `onEnable()`:
- Registers commands: `stack` (+ aliases `sm`, `stk`), `stackto` (+ alias `stkto`), `unstack` (+ alias `unstk`), `unstackto` (+ alias `unstkto`), `stackinfo`, `stackmore` via `getCommand().setExecutor()`
- Registers **2 event listeners**: `BlockListener`, `InventoryListener`
- Starts **1 repeating HUD task**: `HudTask` (BukkitRunnable, 20 tick period)

Command logic lives in `commands/` — one `CommandExecutor` class per command (`StackCommand`, `StackToCommand`, `UnstackCommand`, `UnstackToCommand`, `StackInfoCommand`, `ReloadCommand`) plus shared helpers `StackCommandHelper` (absorption logic) and `CommandUtils` (player/permission/hand validation). `ReloadCommand` also implements `TabCompleter` for `stackmore` (reload/setlanguage subcommands).

`HudTask` shows item name + amount on the ActionBar for players holding a special stack; gated by `hud_enabled` config.

### PDC-based item stacking
The core trick: items use `PersistentDataContainer` (NBT) to store real stack counts while keeping the vanilla `amount` field at 1. This bypasses the client's 64-stack limit. Key class: `StackItemManager.java`.

PDC keys stored per-item (`StackItemManager.java:47-50`):
- `stack_uuid` → `PersistentDataType.STRING` — unique stack identifier
- `stack_amount` → `PersistentDataType.INTEGER` — current count
- `stack_owner_name` → `PersistentDataType.STRING` — creator name
- `stack_owner_uuid` → `PersistentDataType.STRING` — creator UUID (serialized as String, NOT native UUID type)

### Critical safety patterns (do NOT break these)

1. **NBT clearing on creation** (`StackItemManager.java:151`): `new ItemStack(type, 1)` creates an entirely new item, stripping ALL NBT from the original (including container inventories for chests, furnaces, shulker boxes). This prevents item duplication. Only custom display names (anvil-renamed) are preserved (lines 156-159).

2. **Shulker box hard-ban** (`ConfigManager.java:106`): `material.name().endsWith("SHULKER_BOX")` blocks all shulker box variants unconditionally, enforced at `StackCommand.java:42` via `isMaterialDisabled()`. This is **NOT configurable** — do not make it so without careful thought about NBT-based dupes.

3. **Block placement dual-priority** (`BlockListener.java:53-101`): Uses `LOWEST` (`ignoreCancelled=true`) → `MONITOR` (`ignoreCancelled=false`) priority pair. Saves a clone of the special stack before placement (line 60), restores it (amount-1) after placement unless the event was cancelled (line 79). If a protection plugin cancels the event at a higher priority, the item is consumed normally. Do NOT collapse this into a single handler.

4. **Crafting GUI restrictions** (`InventoryListener.java`): Sixteen container types block special stacks — `WORKBENCH`, `ANVIL`, `GRINDSTONE`, `SMITHING`, `ENCHANTING`, `CRAFTING` (player 2x2 grid), `FURNACE`, `BLAST_FURNACE`, `SMOKER`, `BREWING`, `LOOM`, `CARTOGRAPHY`, `STONECUTTER`, `BEACON`, `MERCHANT`, `CRAFTER`. Cursor-clicks, drags, shift-clicks, hotbar swaps, and double-click collect involving special stacks in these restricted GUIs are all cancelled; only five actions are allowed: `PICKUP_ALL`, `PLACE_ALL`, `SWAP_WITH_CURSOR`, `HOTBAR_SWAP`, `MOVE_TO_OTHER_INVENTORY` (whitelist at lines 39-45). `HOTBAR_SWAP` is additionally checked via `getHotbarSwapItem()` — if the hotbar slot or offhand contains a special stack, the swap into a restricted container is blocked.

5. **Third-party PDC protection** (`StackItemManager.java:hasExternalPDCTags()`): Before absorbing items during `/stack` or `/stackto`, and before converting a held item into a special stack via `/stack`, items are checked for PDC keys from external plugins (namespaces other than `minecraft` or `stackmore`). This prevents accidental destruction of specially-tagged items from plugins like Infinite-Blocks. Applied in two files (three checks):
   - `StackCommand.onCommand()` (`StackCommand.java:48`) — rejects held items with external PDC tags
   - `StackCommandHelper.absorbFromPlayer()` (`StackCommandHelper.java:41` for inventory slots, `:54` for offhand) — skips external-tagged items during absorption (shared by `/stack` and `/stackto`)

6. **Special block interaction restoration** (`BlockListener.java:106-136`): Listens to `PlayerInteractEvent` at `MONITOR` priority (`ignoreCancelled=true`) for four interaction types where vanilla Minecraft consumes the held item on right-click — flower pots, composters (compostable items), cake (candles), and respawn anchors (glowstone). These are enumerated in `canVanillaConsumeInteraction()` (lines 148-159). Because vanilla will consume one item from the special stack, the handler saves a clone of the item **before** vanilla processing and restores it on the **next tick** with `amount - 1` (only if the hand slot is now empty and the new amount is still ≥ 1). Adding new interaction types requires extending `canVanillaConsumeInteraction()`. Do NOT collapse this into a same-tick restore — the next-tick delay is necessary because vanilla's item consumption happens after the event fires.

7. **Auto-placed sign consumption restoration** (`BlockListener.java:saveSignStackOnInteract` / `restoreConsumedSignStack`): Sign-locking plugins like BlockLocker auto-place a `[Private]` sign when a player right-clicks a protectable block while holding a sign — they bypass vanilla consumption entirely: they set the sign block manually, fire a **synthetic** `BlockPlaceEvent`, then manually delete one item from the hand (`removeSingleSignFromHand`). Since a special stack always has vanilla `amount == 1`, that deletion destroys the **entire** special stack. StackMore detects this within the **same synchronous `PlayerInteractEvent` dispatch** (`ignoreCancelled=false`): LOWEST saves the sign-stack clone, MONITOR verifies a 3-part signature — (1) event cancelled, (2) hand slot emptied, (3) a new sign block appeared at the clicked face (`getRelative(blockFace)`) — then restores `amount - 1` to the original hand slot. Condition (3) is the side-effect evidence that distinguishes "consumed 1 for a placement" from any other reason the hand went empty. **Do NOT generalize this into a blanket next-tick "restore if hand empty" check** — that reintroduces duplication races (player drops/inventory moves within the window, third-party post-event consumption). Silent consumption **without** side effects is intentionally NOT restored (accepted item loss by design). The both-hands corner case (special sign stacks in main + offhand, interacted with offhand) is also not healed — BlockLocker removes from main first.

### Config & messages

- `ConfigManager.java` loads `plugins/StackMore/config.yml`; `MessageManager.java` loads `plugins/StackMore/lang/{language}.yml`.
- `config.yml` in `src/main/resources/` is the default — it gets copied on first load.
- Default language is **`en_us`** (`config.yml` default + `ConfigManager.java` fallback). If the configured language file doesn't exist, `MessageManager.java` falls back to **`en_us.yml`**.
- 7 built-in language files under `src/main/resources/lang/`: `zh_cn.yml`, `en_us.yml`, `de_de.yml`, `ru_ru.yml`, `es_es.yml`, `fr_fr.yml`, `ja_jp.yml`.
- `config.yml` settings: `language`, `max_stack_multiplier` (max stack = 64 × multiplier), `hud_enabled`, `disabled_materials` (additional block materials to reject).
- **Lang file upgrade trap**: `saveDefaultLang()` only copies files from the JAR when the target does **not** exist. If a server already has old `lang/*.yml` files, a new JAR with updated keys will NOT overwrite them → missing keys return `""` → empty messages in-game. When adding new message keys, users must manually delete old `plugins/StackMore/lang/*.yml` files (or rename them) and restart for the plugin to re-extract updated copies.

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
- Only `.deepseek/` and `session_*.json` are gitignored as AI-assistant traces; `.omo/` and `Reference/` are **NOT** ignored (currently untracked). `test-server/` and `run/` are ignored (plugin test server dirs).
- `gradle/wrapper/gradle-wrapper.jar` is whitelisted in `.gitignore` (`!gradle/wrapper/gradle-wrapper.jar`), but the wrapper directory doesn't exist yet.

## Local build notes

- Gradle is **not** in PATH. Available extracted distributions in `~/.gradle/wrapper/dists/`: `gradle-8.8`, `gradle-9.5.0`, `gradle-9.6.1`.
- Java 21 is at `C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot`. Set `JAVA_HOME` before running Gradle:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
  & "C:\Users\Ding_\.gradle\wrapper\dists\gradle-8.8-bin\4u0rgm4geyrm56fyhoco0c9in\gradle-8.8\bin\gradle.bat" build
  ```
