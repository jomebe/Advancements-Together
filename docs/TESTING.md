# Verification checklist

For **each exact Minecraft target**, retain logs/screenshots and record these independently:

- [ ] Dedicated server starts on its required Java version; mixins apply.
- [ ] Client starts; vanilla advancement tabs render without errors.
- [ ] Integrated singleplayer world loads and saves.
- [ ] Two modded clients join; initial snapshots match.
- [ ] One player's full completion is shown to the other without changing vanilla progress.
- [ ] Revoke/re-award does not duplicate history or change its first time.
- [ ] Notification enabled/disabled/custom format works.
- [ ] Hover faces, colors, incomplete status and dates render at small and large GUI scales.
- [ ] Player rank, progress, timeline and first achiever are correct.
- [ ] Disconnect retains offline history; reconnect restores it.
- [ ] Clean server restart preserves records.
- [ ] Individual mode grants only the actual achiever; cooperative mode grants other online players.
- [ ] Switching to cooperative and reconnecting catch up historical unlocks, preserving the first achiever.
- [ ] Switching back to individual stops sharing without revoking prior unlocks; mode survives restart.
- [ ] Only authorized operators can switch mode; Shared screen shows the synchronized mode.
- [ ] Datapack reload/removal preserves historical IDs and updates current totals.
- [ ] Wrong-version JAR is rejected by Fabric metadata.

Unit tests: `./gradlew -PcoreOnly :common:test` (Java 25 build JVM).
Matrix: `pwsh scripts/build-matrix.ps1` after setting `JAVA_HOME` to Java 25.
Per version: `./gradlew -Pmc=1.21.1 build`.

The local protocol probe uses `minecraft-protocol` and Fabric's common version/channel negotiation to receive the actual custom payload. It is a network probe, **not** a graphical client test. Its dependency is installed under ignored `.tools/bots`. Never infer skin/UI correctness from this probe.

Mode regression: start `scripts/server-console.cjs <java> <disposable-server-directory>` with a fresh 1.21.1 test world on loopback port 25575, then run `node scripts/test-sharing.cjs <disposable-server-directory>`. Install `minecraft-protocol@1.62.0` under `.tools/bots` first. The test changes modes and awards advancements, so never run it against a real player world. For persistence testing, restart that server after a passing run and verify Co-op mode and retained unlocks on reconnect.
