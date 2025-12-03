# SharedCure

English | [简体中文](README_zh.md)

SharedCure is a Paper plugin that implements server-wide villager discount sharing using a small tag (PDC) and Just-In-Time reputation injection approach. When a villager has been cured by any player, this plugin ensures other players who interact with that villager receive the highest trade discount (MAJOR_POSITIVE) without requiring bulk updates across all players.

## Features

- **PDC three-state management**: Each villager receives a `discount_status` tag (PDC) indicating `UNKNOWN`, `NOT_CURED`, or `CURED` to minimize server-wide operations.
- **Instant Reputation Injection**: When a player interacts with a villager in the `CURED` state, the plugin will, if necessary, instantly inject `MAJOR_POSITIVE` reputation for the player's UUID, ensuring that the discount is immediately reflected in the trading interface. This only affects the cured reputation (`MAJOR_POSITIVE`) and does not impact other types of reputation.
- **Legacy villager backfill**: On plugin load and chunk load, villagers that lack the plugin tag are deep-checked for existing reputations (gossip) and tagged accordingly to support older worlds.
- **Cure capture**: The plugin listens for `EntityTransformEvent` and marks zombie villager conversions as `CURED` instantly.

## Architecture Overview

| Module | Event | Purpose |
| --- | --- | --- |
| Existing villager initialization | `ChunkLoadEvent` + startup scan | Tag existing villagers and deep-check gossip/inventory for unknown states. |
| New villager tagging | `CreatureSpawnEvent` | Tag newly spawned villagers `NOT_CURED`. |
| Cure capture | `EntityTransformEvent` | Mark newly cured villagers as `CURED`. |
| JIT injection | `PlayerInteractEntityEvent` | Inject `MAJOR_POSITIVE` reputation for the interacting player when villager is `CURED`.

## Build & Usage

Requirements:
- JDK 21
- Maven 3.9+

Build:

```bash
mvn clean package
```

Install:

Copy the generated shaded jar (e.g., `target/sharedcure-1.0.0-SNAPSHOT-shaded.jar`) to the `plugins/` folder of your Paper/Spigot 1.21.x server and restart it.

