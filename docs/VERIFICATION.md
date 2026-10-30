# Verification — 2026-10-30

[GitHub Actions run 41024888865](https://github.com/jomebe/Shared-Advancements/actions/runs/41024888865) passed all 12 build/test/packaged-server jobs for implementation commit `c85667b`. Subsequent commits update documentation and local artifact collection only.

## Local evidence

| Scope | Result |
| --- | --- |
| All 12 target dependency manifests | Release entries, Java version and Fabric API artifacts verified |
| All 12 target JAR builds | Passed; exact-version Fabric metadata |
| All 12 packaged JAR dedicated servers | Started, loaded `shared_advancements`, saved, stopped successfully on Java 21 / 25 as appropriate |
| Common JUnit tests | 7 passed |
| 1.21.1 two protocol clients | Each received both players' actual completion packets |
| 1.21.1 duplicate + revoke/re-award | One record per UUID/ID; original timestamp unchanged |
| 1.21.1 offline + clean restart + reconnect | Both persisted player histories synchronized again |
| 1.21.1 graphical client | Vanilla screen, Shared button, player faces/colors/progress, timeline, first achiever, per-player filter and return navigation verified |
| 1.21.1 integrated singleplayer | New world loaded, completion detected, notification shown, world-local ledger saved with only its own player |
| 26.3 graphical multiplayer client | Loaded, received completions, rendered vanilla hover panel and Shared player face/progress screen |
| Other versions' graphical clients | Not yet verified; server smoke is not client compatibility certification |

Synthetic players `SharedTestA`, `SharedTestB`, `SharedUiTest` were used only on a loopback test server. The protocol probe had a third-party recipe-packet parsing warning; its custom history messages were received and compared with the saved server ledger. It is not treated as evidence of a full graphical client.

The Minecraft client run revealed and fixed a button overlap with vanilla's Done footer. Concurrent development execution revealed that reusing the Gradle project path `:fabric` across targets lets output cleanup remove another target's development classes. Each target now uses a unique project path (`:fabric-1.21.1`, etc.). Packaged mod classes are included in the JAR.

![1.21.1 timeline](evidence/1.21.1-timeline.png)

![1.21.1 singleplayer](evidence/1.21.1-singleplayer.png)

![26.3 multiplayer progress](evidence/26.3-progress.png)

Raw local build logs: `build/matrix/<version>.log`.
Raw local dedicated server logs: `.tools/smoke/<version>/smoke.log`.
These ignored logs contain local environment paths and are not published verbatim. CI retains its own logs and test reports.

## Remaining release qualification

The full [checklist](TESTING.md) remains required before calling an exact version fully supported. In particular, intermediate-version client rendering, all GUI scales, authenticated custom skins, large servers and arbitrary third-party datapacks have not been certified. Offline/unavailable skins may use vanilla default faces. No Modrinth/CurseForge version compatibility is claimed or published by this repository.

Modrinth publication was explicitly deferred by the owner. Build artifacts are development alpha candidates, not a stable release.
