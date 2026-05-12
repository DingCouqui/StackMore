# AGENTS.md

## Tech stack
- Java 21, Gradle (Groovy DSL), PaperMC 1.21.3 plugin
- Single module at `src/main/java/io/github/dingcouqui/stackmore/`

## Build & run

```bash
gradle build        # produces build/libs/StackMore-1.0.0.jar
gradle clean        # deletes build/
gradle wrapper      # generate gradlew (no wrapper committed)
```

- **No Gradle wrapper is committed.** If `gradlew` is missing, install Gradle locally and run `gradle wrapper` first.
- Java 21 toolchain is enforced in `build.gradle`.
- `processResources` expands `${version}` in `plugin.yml` at build time — don't hardcode the version in `plugin.yml`.

## Commands (for verification)

| Task | Command |
|---|---|
| Build | `gradle build` |
| Clean | `gradle clean` |
| Output JAR | `build/libs/StackMore-1.0.0.jar` |

## Testing

**There are no tests.** No `src/test/`, no JUnit, no test dependencies in `build.gradle`. The only verification is that `gradle build` compiles successfully.

## Lint & format

**None configured.** No Checkstyle, SpotBugs, formatter, or `.editorconfig`. Do not introduce linter config unless asked.

## Architecture notes

### Plugin entrypoint
`StackMorePlugin.java:27` — standard `JavaPlugin`. Registers 6 commands, 2 event listeners, and a repeating HUD task (20 ticks).

### PDC-based item stacking
The core trick: items use `PersistentDataContainer` (NBT) to store real stack counts while keeping the vanilla `amount` field at 1. This bypasses the client's 64-stack limit. Key class: `StackItemManager.java:33`.

PDC keys stored per-item: `stack_uuid` (String), `stack_amount` (Integer), `stack_owner_name` (String), `stack_owner_uuid` (String).

### Critical safety patterns (do NOT break these)

1. **NBT clearing on creation** (`StackItemManager.java:120`): `new ItemStack(type, 1)` strips ALL existing NBT including container inventories (chests, furnaces, shulker boxes). This prevents item duplication. Only custom display names are preserved.

2. **Shulker box hard-ban**: Materials ending in `SHULKER_BOX` are rejected for stacking (checked in `StackCommandHelper.java`). This is NOT configurable — do not make it so without careful thought about NBT-based dupes.

3. **Block placement dual-priority** (`BlockListener.java:47-64`): Uses `LOWEST` → `MONITOR` priority pair. Saves a clone of the special stack before placement, restores it (amount-1) after. If a protection plugin cancels the event at a higher priority, the item is consumed normally. Do NOT collapse this into a single handler.

### Config & messages

- `ConfigManager.java` loads `config.yml`; `MessageManager.java` loads `lang/{language}.yml`.
- Language fallback: if the configured language file doesn't exist, falls back to `zh_cn.yml` (Chinese).
- `config.yml` in `src/main/resources/` is the default — it gets copied to the server's `plugins/StackMore/` on first load.

### Inventory restrictions
`InventoryListener.java` restricts special stacks in crafting GUIs (workbench, anvil, enchantment table, grindstone, smithing table) to prevent exploits.

## Conventions

- Javadoc is bilingual: class-level docs in Chinese, method-level docs in English.
- Comments in source are in Chinese; strings visible to players are in `lang/*.yml` (Chinese + English).
- Plugin uses Adventure Component API for text; `TextUtils.java` converts legacy `&` color codes.
- `StackMorePlugin` exposes a static singleton (`getInstance()`, `getConfigManager()`, `getMessageManager()`) — used throughout the codebase.
- `folia-supported: false` in `plugin.yml` — do not claim Folia compatibility.

## CI/CD

**None.** No `.github/workflows/`, no pre-commit hooks, no Docker.

## Git

- Repo: `github.com/DingCouqui/StackMore`, single branch `main`.
- `.deepseek/` and `session_*.json` are gitignored (AI assistant traces).
- `gradle/wrapper/gradle-wrapper.jar` is NOT gitignored (unusual: `.gitignore` has `!gradle/wrapper/gradle-wrapper.jar` whitelist), but the wrapper directory doesn't exist yet.
