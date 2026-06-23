# PORTING.MD — Explosive Filter Multi-Version Porting Guide

> ## ⚠️ THIS IS A BARE TEMPLATE
>
> No repositories, branches, or source files exist yet. This document is a step-by-step
> workflow and checklist. Follow it top-to-bottom, version by version.

---

## Overview

The goal is one Git repository per supported Minecraft version, each on its own branch.
The 1.21.1 codebase (defined in `CLAUDE.MD` + `CHANGES.MD`) is the **canonical source**.
All other versions are backports from it.

### Target Versions

| Branch | Minecraft | Java | Mappings | Loom plugin ID | Status |
|---|---|---|---|---|---|
| `main` | 1.21.1 | 21 | Mojang | `fabric-loom` | ✅ Done |
| `1.20.1` | 1.20.1 | 17+ | Mojang | `fabric-loom` | ✅ Done |
| `1.19.4` | 1.19.4 | 17+ | Mojang | `fabric-loom` | ✅ Done |
| `1.19.2` | 1.19.2 | 17+ | Mojang | `fabric-loom` | ✅ Done |
| `1.18.2` | 1.18.2 | 17+ | Mojang | `fabric-loom` | 🔄 In progress |
| `1.16.5` | 1.16.5 | 8+ | Yarn | `fabric-loom` | ⬜ Not started |
| `26.1` | 26.1 | 25 | Mojang official | `net.fabricmc.fabric-loom` | ⬜ Not started |

> Update the **Status** column as you work. Replace ⬜ with 🔄 (in progress) or ✅ (done).

---

## Phase 0 — Create the Main Repository (do this first)

This must be completed before any porting branches can be created.

- [ ] Create a new repository on GitHub (or your preferred host) named `explosive-filter`
- [ ] Clone it locally: `git clone <url> explosive-filter && cd explosive-filter`
- [ ] Create the full 1.21.1 project from scratch following `CLAUDE.MD` and `CHANGES.MD`
- [ ] Verify the build passes: `./gradlew build`
- [ ] Commit all files: `git add -A && git commit -m "Initial 1.21.1 implementation"`
- [ ] Rename the default branch to `main` if it isn't already: `git branch -M main`
- [ ] Push: `git push -u origin main`
- [ ] Tag the initial release: `git tag v1.1.0-1.21.1 && git push origin v1.1.0-1.21.1`

---

## Porting Workflow (repeat for each version below)

For every port, follow these steps in order. Each section below is a version-specific
instantiation of this workflow with the exact values filled in.

### Step 1 — Create the port branch

```bash
# Always branch from main (the 1.21.1 source of truth)
git checkout main
git pull
git checkout -b <branch-name>
```

### Step 2 — Update `gradle.properties`

Replace the four version values with those listed in the version-specific section below.
Always fetch the latest exact values from **https://fabricmc.net/develop/** — the values
in this document are correct as of the time of writing but patch versions change frequently.

```properties
minecraft_version=<value>
yarn_mappings=<value>      # omit for 26.1 (uses Mojang mappings)
loader_version=<value>
fabric_version=<value>
```

### Step 3 — Update `gradle/wrapper/gradle-wrapper.properties`

Set the Gradle distribution URL to the version required by the target Loom. Minimum
versions per Loom:

| Loom version | Min Gradle |
|---|---|
| 0.12.x (1.16.5) | 7.4 |
| 1.2–1.3 (1.19–1.20) | 8.1 |
| 1.6–1.7 (1.21.1) | 8.8 |
| 1.15 (26.1) | 9.4 |

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-<version>-bin.zip
```

### Step 4 — Update `build.gradle`

- Change the Loom plugin version in the `plugins {}` block to the one listed for the target.
- For 26.1 **only**: change the plugin ID from `fabric-loom` to `net.fabricmc.fabric-loom`
  and make the additional build script changes listed in the 26.1 section.

### Step 5 — Update `fabric.mod.json`

- Change `"minecraft": "~1.21.1"` in `depends` to match the target version.
- For 1.19.1 and earlier: change `"fabric-api": "*"` to `"fabric": "*"` (the mod ID
  changed to `fabric-api` in 1.19.2).

### Step 6 — Refresh Gradle and regenerate sources

```bash
./gradlew --refresh-dependencies
./gradlew genSources    # regenerates Minecraft sources with new mappings
```

In IntelliJ: press the **Reload All Gradle Projects** button (elephant icon, top-right).

### Step 7 — Fix all IDE / compiler errors

Run `./gradlew build` and work through every error. The known porting issues for each
version are listed in the version-specific sections below. Common patterns:

- **Renamed Yarn symbol** — look up the new name in the generated sources or the Fabric
  release notes for that version.
- **Removed API** — find the equivalent in Fabric API for that version; older Fabric APIs
  have fewer modules so some features may need manual reimplementation.
- **Missing method signature** — MC's own API changed; check the generated sources for
  the correct parameter list.
- **`CustomPayload` API not present** — versions before 1.20.5 do not have the typed
  payload system; see the networking notes in each version section.

### Step 8 — Verify logical correctness

Beyond compiling, confirm the mod still behaves correctly:

- [ ] `/explosivefilter add`, `remove`, `list`, `reload` work in-game
- [ ] Saying a trigger phrase in chat causes an explosion on that player
- [ ] `worldDamage`, `fire`, `dropItems`, `dealDamage`, `instakill` toggles behave as expected
- [ ] Config file is written and loaded correctly
- [ ] Multiplayer: explosion is server-authoritative; clients receive FX
- [ ] Creative player is instakilled when `instakill` is enabled

### Step 9 — Update version metadata

In `gradle.properties`, set `mod_version` to reflect the port:

```properties
mod_version=1.1.0+mc<minecraft_version>
# e.g. mod_version=1.1.0+mc1.20.1
```

### Step 10 — Commit, push, tag

```bash
git add -A
git commit -m "Port to Minecraft <version>"
git push -u origin <branch-name>
git tag v1.1.0-<minecraft_version>
git push origin v1.1.0-<minecraft_version>
```

---

## Version-Specific Sections

---

### `main` — Minecraft 1.21.1 ✅ (defined in CLAUDE.MD + CHANGES.MD)

This is the source. No porting required. See Phase 0 above.

**`gradle.properties` values:**
```properties
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
loader_version=0.16.5
fabric_version=0.104.0+1.21.1
```

---

### Branch `1.20.1` — Minecraft 1.20.1

**Checklist:**
- [ ] Step 1: `git checkout -b 1.20.1`
- [ ] Step 2: Update `gradle.properties`
- [ ] Step 3: Update Gradle wrapper
- [ ] Step 4: Update Loom plugin version to `1.3-SNAPSHOT`
- [ ] Step 5: Update `fabric.mod.json` minecraft version constraint
- [ ] Step 6: Refresh Gradle + `genSources`
- [ ] Step 7: Fix compiler errors (see notes below)
- [ ] Step 8: Logical verification pass
- [ ] Step 9: Set `mod_version=1.1.0+mc1.20.1`
- [ ] Step 10: Commit, push, tag `v1.1.0-1.20.1`

**`gradle.properties` values** *(verify at fabricmc.net/develop)*:
```properties
minecraft_version=1.20.1
yarn_mappings=1.20.1+build.10
loader_version=0.14.21
fabric_version=0.83.0+1.20.1
```

**Minimum Gradle:** 8.1 | **Java:** 17 | **Loom:** 1.3-SNAPSHOT

**Known porting issues:**

- **`CustomPayload` API does not exist** — 1.20.1 predates the typed payload system
  introduced in 1.20.5. Replace `ExplodeTriggerPayload` and `BoomFeedbackPayload` with the
  legacy `PacketByteBuf`-based API:
  - Server send: `ServerPlayNetworking.send(player, id, buf)`
  - Client receive: `ClientPlayNetworking.registerGlobalReceiver(id, (client, handler, buf, sender) -> ...)`
  - Use `new Identifier("explosive_filter", "explode_trigger")` instead of `CustomPayload.Id`.
  - `PayloadTypeRegistry` does not exist; remove those registration calls from `ExplosiveFilterMod`.
- **`ServerMessageEvents.CHAT_MESSAGE`** — present in Fabric API for 1.20.1, no changes needed.
- **`ServerWorld.ExplosionSourceType`** — present; no changes needed.
- **`Text.translatable()`** — present; no changes needed.
- **`GameMode`** — present as `GameMode.SURVIVAL`; no changes needed.
- **`world.getDamageSources().explosion()`** — present in 1.20.1; no changes needed.

---

### Branch `1.19.4` — Minecraft 1.19.4

**Checklist:**
- [ ] Step 1: `git checkout -b 1.19.4`
- [ ] Step 2: Update `gradle.properties`
- [ ] Step 3: Update Gradle wrapper
- [ ] Step 4: Update Loom plugin version to `1.1-SNAPSHOT`
- [ ] Step 5: Update `fabric.mod.json` minecraft constraint; keep `"fabric-api": "*"` (1.19.4 still uses this ID)
- [ ] Step 6: Refresh Gradle + `genSources`
- [ ] Step 7: Fix compiler errors (see notes below)
- [ ] Step 8: Logical verification pass
- [ ] Step 9: Set `mod_version=1.1.0+mc1.19.4`
- [ ] Step 10: Commit, push, tag `v1.1.0-1.19.4`

**`gradle.properties` values** *(verify at fabricmc.net/develop)*:
```properties
minecraft_version=1.19.4
yarn_mappings=1.19.4+build.2
loader_version=0.14.21
fabric_version=0.76.0+1.19.4
```

**Minimum Gradle:** 8.1 | **Java:** 17 | **Loom:** 1.1-SNAPSHOT

**Known porting issues:**

- **`CustomPayload` API** — does not exist; apply the same `PacketByteBuf` fallback as 1.20.1.
- **`ServerMessageEvents.CHAT_MESSAGE`** — introduced in 1.19.x; verify event signature matches.
  In 1.19.4 the signed message system changed; the event may fire with a different
  `SignedMessage` structure. Check Fabric API changelog for 1.19.4 and adjust parameter handling.
- **`world.getDamageSources()`** — introduced in 1.19.4. If it is missing on your exact
  build, fall back to `DamageSource.explosion(null)` (static, older API).
- **`ServerWorld.ExplosionSourceType`** — introduced in 1.19.4; if absent, use the older
  `world.createExplosion(entity, x, y, z, power, fire, DestructionType)` overload.
  `DestructionType.DESTROY` = blocks + drops; `DestructionType.NONE` = no block change.

---

### Branch `1.19.2` — Minecraft 1.19.2 🔄

**Checklist:**
- [x] Step 1: `git checkout -b 1.19.2`
- [x] Step 2: Update `gradle.properties`
- [x] Step 3: Update Gradle wrapper
- [x] Step 4: Update Loom plugin version (`fabric-loom` at `1.17-SNAPSHOT`; `0.12-SNAPSHOT` also works)
- [x] Step 5: Update `fabric.mod.json` minecraft constraint to `~1.19.2`
- [x] Step 6: Refresh Gradle + `genSources`
- [x] Step 7: Fix compiler errors (see notes below)
- [x] Step 8: Logical verification pass (game runs; creative kill and chat broadcast confirmed)
- [x] Step 9: Set `mod_version=1.1.0+mc1.19.2`
- [x] Step 10: Commit, push, tag `v1.1.0-1.19.2`

**`gradle.properties` values** *(actual working values)*:
```properties
minecraft_version=1.19.2
loader_version=0.19.3
loom_version=1.17-SNAPSHOT
fabric_api_version=0.77.0+1.19.2
```
> Note: uses Mojang official mappings (`loom.officialMojangMappings()`), not Yarn.
> `0.58.0+1.19.2` does not exist on the Fabric maven; earliest available is `0.59.0+1.19.2`.

**Minimum Gradle:** 7.4 | **Java:** 17 | **Loom:** 0.12-SNAPSHOT

**Known porting issues:**

- **`CustomPayload` API** — does not exist; use `PacketByteBuf` API (same as 1.20.1 notes).
- **`ServerMessageEvents`** — introduced in 1.19; verify it exists in this Fabric API version.
  If unavailable, use `ServerPlayConnectionEvents.CHAT` or a mixin on `ServerGameMessageHandler`
  as a fallback. Check the Fabric API release for 1.19.2.
- **`world.getDamageSources()`** — may not exist in 1.19.2; use `DamageSource.GENERIC` or
  a static `DamageSource` factory call (`DamageSource.explosion(entity)`).
- **`ServerWorld.ExplosionSourceType`** — does not exist; use the older `createExplosion`
  overload with `Explosion.DestructionType` (see 1.19.4 notes above).
- **`GameMode.SURVIVAL`** — may be `GameMode.SURVIVAL` or `GameMode.ADVENTURE` depending on
  Yarn naming; verify in generated sources.
- **`fabric.mod.json` mod ID** — in Fabric API ≥ 0.44.0+1.19 the mod ID switched to
  `fabric-api`. For 1.19.2 specifically, the wiki notes it changed at 1.19.2; verify with
  the actual Fabric API jar you're targeting.

---

### Branch `1.18.2` — Minecraft 1.18.2 🔄

**Checklist:**
- [x] Step 1: `git checkout -b 1.18.2`
- [x] Step 2: Update `gradle.properties`
- [x] Step 3: Gradle wrapper unchanged (9.5.1 carries over from 1.19.2, works fine)
- [x] Step 4: Loom plugin unchanged (`fabric-loom` 1.17-SNAPSHOT)
- [x] Step 5: Update `fabric.mod.json`; `fabric-api` → `fabric` (old mod ID); MC `~1.18.2`
- [ ] Step 6: Refresh Gradle + `genSources`
- [ ] Step 7: Fix compiler errors (see notes below)
- [ ] Step 8: Logical verification pass
- [x] Step 9: Set `mod_version=1.1.0+mc1.18.2`
- [ ] Step 10: Commit, push, tag `v1.1.0-1.18.2`

**`gradle.properties` values** *(actual working values)*:
```properties
minecraft_version=1.18.2
loader_version=0.19.3
loom_version=1.17-SNAPSHOT
fabric_api_version=0.77.0+1.18.2
```
> Note: `0.46.4+1.18.2` does not exist; earliest is `0.50.0+1.18.2`. Uses Mojang mappings.

**Minimum Gradle:** 7.4 | **Java:** 17 | **Loom:** 1.17-SNAPSHOT

**Key differences from 1.19.2 already applied:**
- `ServerMessageEvents` → `ChatMixin` on `ServerGamePacketListenerImpl.handleChat()`
- `CommandRegistrationCallback` v2 → v1 (v2 requires 1.19+ `CommandBuildContext`)
- `sendSystemMessage` → `sendMessage(component, Util.NIL_UUID)`
- `fabric-api` mod ID → `fabric`
- Damage type data files removed (data-driven damage types require 1.19.4+)

**Known porting issues:**

- **`CustomPayload` API** — does not exist; use `PacketByteBuf` API.
- **`ServerMessageEvents`** — does not exist in 1.18.2 (introduced in 1.19). This is the
  most significant porting challenge for this version. Replace with a mixin:
  - Mixin target: `net.minecraft.server.network.ServerPlayNetworkHandler`
  - Method: `onChatMessage(ChatMessageC2SPacket)` (Yarn name may vary — check generated sources)
  - Inject `@Inject` at `HEAD`, capture the message string, run the filter logic.
  - Add the mixin class to `explosive_filter.mixins.json` and fill in that file.
- **`world.getDamageSources()`** — does not exist; use `DamageSource.GENERIC` or
  `DamageSource.explosion(entity)` (static fields/methods).
- **`ServerWorld.ExplosionSourceType`** — does not exist; use the older `createExplosion`
  overload. To control block drops vs. destruction, use `Explosion.DestructionType.DESTROY`
  vs. `Explosion.DestructionType.NONE` vs. `Explosion.DestructionType.BREAK`.
- **`GameMode`** — verify Yarn name in generated sources; likely `GameMode.SURVIVAL`.
- **`Text.translatable()`** — in 1.18.2 this may be `new TranslatableText(key)`. Check
  Yarn; the static factory was added later. Use whichever compiles.
- **`fabric.mod.json`** — use `"fabric": "*"` (old mod ID, pre-1.19.2).

---

### Branch `1.16.5` — Minecraft 1.16.5

**Checklist:**
- [ ] Step 1: `git checkout -b 1.16.5`
- [ ] Step 2: Update `gradle.properties`
- [ ] Step 3: Update Gradle wrapper (7.4+)
- [ ] Step 4: Update Loom plugin version to `0.9-SNAPSHOT`
- [ ] Step 5: Update `build.gradle` Java compatibility to `JavaVersion.VERSION_1_8`
- [ ] Step 6: Update `fabric.mod.json`; use `"fabric": "*"` in `depends`
- [ ] Step 7: Refresh Gradle + `genSources`
- [ ] Step 8: Fix compiler errors (see notes below)
- [ ] Step 9: Logical verification pass
- [ ] Step 10: Set `mod_version=1.1.0+mc1.16.5`
- [ ] Step 11: Commit, push, tag `v1.1.0-1.16.5`

**`gradle.properties` values** *(verify at fabricmc.net/develop)*:
```properties
minecraft_version=1.16.5
yarn_mappings=1.16.5+build.10
loader_version=0.14.21
fabric_version=0.36.1+1.16
```

**Minimum Gradle:** 7.4 | **Java:** 8 (compile target) / 16 (toolchain) | **Loom:** 0.9-SNAPSHOT

**Known porting issues:**

- **Java source/target version** — 1.16.5 shipped before Java 16 was required by Minecraft.
  Compile the mod with Java 8 source compatibility. In `build.gradle`:
  ```groovy
  java {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
  }
  ```
  This means **no records** (Java 16+). Replace all `record` types used in the mod:
  - `PhraseEntry` → a regular class with a constructor, `phrase()` and `power()` getter methods.
  - `ExplodeTriggerPayload` → a plain class with fields and a constructor.
  - `BoomFeedbackPayload` → a plain class with a field and a constructor.
- **`ServerMessageEvents`** — does not exist (1.16.5 predates 1.19). Use the same
  `ServerPlayNetworkHandler` mixin approach as 1.18.2, but the method name will differ.
  In 1.16.5 the relevant method is `onGameMessage`. Check generated sources for the exact
  Yarn name and `ChatMessageC2SPacket` equivalent.
- **`CustomPayload` API** — does not exist; use `PacketByteBuf`.
- **`world.getDamageSources()`** — does not exist; use `DamageSource.GENERIC` or
  `DamageSource.explosion(entity)` static members.
- **`ServerWorld.ExplosionSourceType`** — does not exist. In 1.16.5, `createExplosion` has
  a different signature that takes a `boolean destroyBlocks` parameter directly:
  `world.createExplosion(entity, x, y, z, power, destroyBlocks)` or similar — check
  generated sources for the exact overload.
- **`Text.translatable()`** — use `new TranslatableText(key, args...)` in 1.16.5.
- **`FilterExplosionBehavior`** — `ExplosionBehavior` exists in 1.16.5 but with a different
  API surface. Verify `getBlockDropChance` and `getEntityDamage` method signatures in
  generated sources; parameter types may differ.
- **`CommandRegistrationCallback`** — present in Fabric API for 1.16.5; signature is the
  same but verify import paths.
- **`fabric.mod.json`** — use `"fabric": "*"` (old mod ID).

---

### Branch `26.1` — Minecraft 26.1

This version requires the **most significant build script changes** of any port due to the
removal of obfuscation and Yarn. Read the
[official Fabric 26.1 release notes](https://fabricmc.net/2026/03/14/261.html) and
[porting guide](https://docs.fabricmc.net/develop/porting/) in full before starting.

**Checklist:**
- [ ] Step 1: `git checkout -b 26.1`
- [ ] Step 2: Update `gradle.properties` (see below)
- [ ] Step 3: Update Gradle to 9.4.0 (`./gradlew wrapper --gradle-version 9.4.0`)
- [ ] Step 4: Extensive `build.gradle` changes (see below)
- [ ] Step 5: Update `fabric.mod.json` (see below)
- [ ] Step 6: Update IDE — IntelliJ IDEA **2025.3+** required for mixin support with Java 25
- [ ] Step 7: Refresh Gradle + `genSources` (no `yarn_mappings` line needed)
- [ ] Step 8: Rename all Fabric API symbols (see below)
- [ ] Step 9: Fix remaining compiler errors
- [ ] Step 10: Logical verification pass
- [ ] Step 11: Set `mod_version=1.1.0+mc26.1`
- [ ] Step 12: Commit, push, tag `v1.1.0-26.1`

**`gradle.properties` values** *(verify at fabricmc.net/develop)*:
```properties
minecraft_version=26.1
# No yarn_mappings line — Mojang official mappings are used (no separate artifact)
loader_version=0.18.4
fabric_version=<latest for 26.1>  # check fabricmc.net/develop
```

**Minimum Gradle:** 9.4 | **Java:** 25 | **Loom:** 1.15

**`build.gradle` changes required:**

```groovy
plugins {
    // CHANGE: plugin ID changes from 'fabric-loom' to 'net.fabricmc.fabric-loom'
    id 'net.fabricmc.fabric-loom' version '1.15-SNAPSHOT'
    id 'maven-publish'
}

// ...

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"

    // REMOVE the mappings line entirely — no Yarn artifact for 26.1
    // mappings "net.fabricmc:yarn:..." ← DELETE THIS LINE
    // Instead, mappings are the unobfuscated game itself; Loom handles it automatically.

    // CHANGE: modImplementation → implementation (no remapping needed)
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}

java {
    // CHANGE: Java 25
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
```

Also in `build.gradle`, replace any `remapJar` task references with `jar`.

If `settings.gradle` also declares the Loom plugin, update it there too:
```groovy
id 'net.fabricmc.fabric-loom' version '1.15-SNAPSHOT'
```

**`fabric.mod.json` changes:**

- Update `"minecraft": "~26.1"` in `depends`.
- `"fabric-api": "*"` remains the correct mod ID.

**Fabric API name changes (apply to all source files):**

These are renames that Fabric made when switching from Yarn to Mojang official names.
Use IntelliJ's **Migrate → Find Usages** or the migration map provided at
https://docs.fabricmc.net/develop/porting/ to batch-apply these.

| Old name (Yarn / pre-26.1) | New name (Mojang official) |
|---|---|
| `ItemGroupEvents` | `CreativeModeTabEvents` |
| `ServerMessageEvents.CHAT_MESSAGE` | verify current name in Fabric API 26.1 source |
| `ServerPlayNetworking` | likely unchanged — verify |
| `PayloadTypeRegistry` | verify in Fabric API 26.1 source |
| `ServerWorld` | likely `ServerLevel` | 
| `ServerPlayerEntity` | likely `ServerPlayer` |
| `PlayerEntity` | likely `Player` |
| `GameMode` | likely `GameType` |
| `DamageSource` | verify in 26.1 source |
| `Text.translatable()` | likely `Component.translatable()` |
| `Identifier.of()` | likely `ResourceLocation.fromNamespaceAndPath()` or `ResourceLocation.withDefaultNamespace()` |

> **Important:** The table above lists *likely* Mojang names based on known MC source. Always
> verify against the actual generated sources for 26.1. When in doubt, open the generated
> Minecraft source in IntelliJ and search for the vanilla class/method.

**`CustomPayload` in 26.1:**

The typed `CustomPayload` API introduced in 1.20.5 is **still present** in 26.1 (Fabric API
carries it forward). The registration pattern changes slightly because `modImplementation` is
replaced by `implementation`, but the Java API surface is the same. Verify
`PayloadTypeRegistry.playS2C().register(...)` still compiles.

---

## Cross-Version Compatibility Notes

### Networking API evolution

| MC version | Networking approach |
|---|---|
| 1.16.5 – 1.20.4 | `PacketByteBuf` with `Identifier` channel |
| 1.20.5 – present | Typed `CustomPayload` records with `PacketCodec` |

For the `PacketByteBuf` approach (1.16.5–1.20.1):

**Server send:**
```java
PacketByteBuf buf = PacketByteBufs.create();
buf.writeDouble(x); buf.writeDouble(y); buf.writeDouble(z);
buf.writeFloat(power); buf.writeFloat(shakeIntensity); buf.writeInt(shakeDuration);
ServerPlayNetworking.send(player, new Identifier("explosive_filter", "explode_trigger"), buf);
```

**Client receive:**
```java
ClientPlayNetworking.registerGlobalReceiver(
    new Identifier("explosive_filter", "explode_trigger"),
    (client, handler, buf, responseSender) -> {
        double x = buf.readDouble(); // read in same order as write
        // ...
        client.execute(() -> { /* apply shake */ });
    });
```

### `ServerMessageEvents` availability

| MC version | Chat event |
|---|---|
| 1.19+ | `ServerMessageEvents.CHAT_MESSAGE` (Fabric API) |
| 1.18.2 and below | Mixin on `ServerPlayNetworkHandler` — method name varies by version |

For the mixin approach, your `explosive_filter.mixins.json` must be filled in and the mixin
class added. A minimal example:

```java
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ChatMixin {
    @Shadow public ServerPlayerEntity player; // field name may differ per version

    @Inject(method = "onGameMessage", at = @At("HEAD")) // verify method name in sources
    private void onChat(/* packet param — verify type */, CallbackInfo ci) {
        String message = /* extract from packet */;
        float power = ExplosiveFilterConfig.getPowerFor(message);
        if (power < 0) return;
        // trigger explosion logic
    }
}
```

### `ExplosionSourceType` vs `DestructionType`

| MC version | Explosion control |
|---|---|
| 1.20+ | `ServerWorld.ExplosionSourceType.TNT` / `.MOB` |
| 1.19.x | `Explosion.DestructionType.DESTROY` / `.NONE` / `.BREAK` |
| 1.18.x and below | Boolean `destroyBlocks` parameter or `DestructionType` — check per version |

### `DamageSource` API

| MC version | API style |
|---|---|
| 1.20+ | `world.getDamageSources().explosion(...)` (instance-based) |
| 1.19.x | Mix of static and instance; verify per version |
| 1.18.2 and below | `DamageSource.GENERIC` / `DamageSource.explosion(entity)` (static) |

### Java records

Records (`record Foo(Type field) {}`) require Java 16+. MC 1.16.5 targets Java 8.
Replace all records with regular classes for the 1.16.5 branch only.

### `Text.translatable()` vs `new TranslatableText()`

| MC version | API |
|---|---|
| 1.19+ | `Text.translatable(key, args...)` (static factory) |
| 1.18.x and below | `new TranslatableText(key, args...)` |

---

## Git Branch Overview (final state)

```
explosive-filter (GitHub)
├── main          ← 1.21.1 (canonical source)
├── 1.20.1
├── 1.19.4
├── 1.19.2
├── 1.18.2
├── 1.16.5
└── 26.1
```

**Tags:**
```
v1.1.0-1.21.1
v1.1.0-1.20.1
v1.1.0-1.19.4
v1.1.0-1.19.2
v1.1.0-1.18.2
v1.1.0-1.16.5
v1.1.0-26.1
```

## Backporting future changes

When you update the mod on `main`, backport relevant changes to other branches manually:

```bash
# Cherry-pick a specific commit onto a port branch
git checkout 1.20.1
git cherry-pick <commit-hash>
# Resolve any conflicts caused by version-specific code differences, then commit
```

Do **not** use `git merge main` into port branches — it will overwrite version-specific
networking and API shims with 1.21.1 code that won't compile on older versions.
