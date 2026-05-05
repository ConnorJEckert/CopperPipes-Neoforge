# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Simple Copper Pipes** — a Vanilla+ item and fluid transport NeoForge mod. Adds copper pipes, pumps, and valves for moving items, water, lava, and smoke.

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.219
- **Java**: 21
- **Mod ID**: `copperpipes`
- **Main package**: `com.example.copperpipes`

## Common Commands

All commands use the Gradle wrapper (`./gradlew` on Unix, `gradlew` on Windows).

```bash
# Launch game client for testing
./gradlew runClient

# Launch dedicated server for testing
./gradlew runServer

# Run GameTests and exit
./gradlew runGameTestServer

# Run data generators (outputs to src/generated/resources/)
./gradlew runData

# Build the mod jar (output in build/libs/)
./gradlew build
```

There is no unit test runner separate from `runGameTestServer` — NeoForge GameTests run inside the game engine.

## Architecture

### Entry Points

- **`CopperPipesMod.java`** — Main mod class, annotated `@Mod("copperpipes")`. FML injects `IEventBus` and `ModContainer` into its constructor. All `DeferredRegister` instances are created and registered here.
- **`CopperPipesModClient.java`** — Client-only class, annotated `@Mod(dist = Dist.CLIENT)`. Only loaded on the physical client. Use for client-side rendering, keybinds, and screen registration.

### Registration Pattern

All game objects (blocks, items, creative tabs, entities, etc.) use **NeoForge's DeferredRegister**:

```java
// Declare at class level (static fields)
public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
public static final DeferredBlock<Block> MY_BLOCK = BLOCKS.registerSimpleBlock("my_block", BlockBehaviour.Properties.of());

// In the mod constructor, register the DeferredRegister with the event bus
BLOCKS.register(modEventBus);
```

Every new registry type (entities, block entities, sounds, etc.) needs its own `DeferredRegister` declared and registered in the constructor.

### Event System

Two separate event buses exist:
- **`modEventBus`** (injected into constructor) — mod lifecycle events: setup, registry, creative tabs. Use `modEventBus.addListener(this::method)` or `@EventBusSubscriber(modid = MODID, bus = Bus.MOD)`.
- **`NeoForge.EVENT_BUS`** (global) — game events: server start, player join, block break. Use `NeoForge.EVENT_BUS.register(this)` + `@SubscribeEvent`.

### Resource Pipeline

- `src/main/resources/` — Hand-authored assets and data (lang files, textures, recipes, etc.)
- `src/generated/resources/` — Output of `runData`; committed to source control. Both are included in the final jar via `sourceSets.main.resources`.
- `src/main/resources/META-INF/neoforge.mods.toml` — Mod metadata (display name, dependencies). Properties are expanded from `gradle.properties` at build time using `${mod_id}` syntax.

