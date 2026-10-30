# Shared Advancements architecture

## Verified target dependencies (2026-10-30)

All twelve requested versions are `release` entries in Mojang's manifest, stable game entries in Fabric Meta, and have version-specific Fabric API artifacts. Exact artifact versions and Mojang detail-manifest URLs are pinned in [versions.json](versions.json). Availability is **not** a claim of mod compatibility.

| Targets | Runtime Java | Build pipeline |
| --- | --- | --- |
| 1.21.1, 1.21.4–1.21.11 | 21 | Loom remap + official Mojang mappings |
| 26.1, 26.2, 26.3 | 25 | Unobfuscated Loom; no mappings/remapping |

Build JVM: Java 25, Gradle 9.6.0, Loom 1.17.21. The actual Gradle resolution rejected Loom 1.19.3 because it requires Gradle 9.8; the compatible combination is pinned. Each artifact declares one exact Minecraft version; no `1.21.x` wildcard.

Primary sources:
- https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
- https://meta.fabricmc.net/v2/versions/game
- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml
- https://fabricmc.net/2024/05/31/121.html
- https://docs.fabricmc.net/develop/rendering/basic-concepts
- https://fabricmc.net/2025/09/23/1219.html
- https://fabricmc.net/2025/12/05/12111.html
- https://fabricmc.net/2026/03/14/261.html
- https://fabricmc.net/2026/06/15/262.html
- https://fabricmc.net/2026/09/15/263.html

## Module boundaries

`common` owns UUID identity, first-completion history, validation, deterministic timeline, current-ID progress and atomic file replacement. It has no Minecraft or Fabric imports. JSON uses a versioned schema and preserves unknown advancement IDs.

`fabric-shared` owns server lifecycle, authoritative completion capture, initial import, notification configuration and bounded server-to-client messages. Only the server writes history; there is no client-to-server award packet. The mixin runs after vanilla's successful criterion award and checks full completion. Individual mode records only. Cooperative mode awards remaining vanilla criteria to other players under a recursion guard, including vanilla rewards. Switching back never revokes unlocks.

`adapters` contains real API boundaries, not reflective guessing. The 26.3 source inspection confirmed `Identifier`, record accessors on `DisplayInfo`, `clientboundPlay`, `GuiGraphicsExtractor`, `extractRenderState`, `PlayerFaceExtractor`, and `Minecraft.gui.setScreen`. Classic and extraction UI families have separate implementations. The classic UI family shares `ClientUi`; 26.x adapters isolate screen/skin calls. Changes in intermediate versions are checked by compilation, then separately by runtime tests.

`versions/<minecraft>` contains an independent target build. `-Pmc=<version>` includes only that uniquely named target (`:fabric-<version>`) plus common, preventing mixed Loom namespaces and dependency graphs in a single configuration. Unique project paths also preserve each target's development class output when another target is built. CI uses a job per target. Common classes are embedded in each JAR, so users install one version-specific mod JAR plus Fabric API.

## Data and semantics

- One world = one ledger: `<world>/shared-advancements/history.json`.
- The operator command selects individual/cooperative mode, atomically persisted in `mode.txt`; absent files default to individual. Switching to cooperative or reconnecting catches up current display advancements from retained history. Fabric JOIN can precede PlayerList insertion, so catch-up explicitly includes the joining player.
- Shared recipients receive a timestamp strictly after the original first completion. The `history_v2` channel sends mode with the initial snapshot. Joining clients receive no deltas until their snapshot is ready, preventing completion-before-player ordering.
- Identity is UUID, name is current display metadata. Offline players remain in the denominator.
- One completion per UUID/advancement ID; revoke/re-award does not erase history or change the first timestamp.
- Existing vanilla completions are imported on player join. Offline players who have never joined since installation are not imported automatically.
- Recipes/non-display advancements are excluded. Hidden advancements enter the shared visible catalog after someone completes them. Progress compares against that current catalog. Removed IDs remain in the timeline but do not inflate current progress.
- Datapack reload refreshes the catalog. IDs are not guessed or automatically renamed between Minecraft versions.
- Time is UTC epoch milliseconds. Imported completion time uses the latest completed vanilla criterion time; historical revocations may make an original first-completion time unrecoverable.
- A player record, catalog record or completion is sent in each bounded UTF-8 packet. Reconnect starts with a reset, then players/catalog/history. Disconnect clears client data to prevent leaking another server's history.
- Saves force the temporary file to disk, preserve the preceding file as `.bak`, and use atomic replacement when the filesystem supports it. Corrupt/unknown-schema input fails closed and keeps the original for manual recovery.

## Verification policy

`build=passed` means compilation, packaging and common unit tests passed. `runtime` tracks server evidence separately. Neither means singleplayer/client rendering/two-client gameplay has passed. No public stable release should be published until the manual checklist is completed for its exact version. Build artifacts are development alpha candidates.
