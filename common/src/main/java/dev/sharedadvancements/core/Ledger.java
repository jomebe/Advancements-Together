package dev.sharedadvancements.core;

import java.util.*;

/** Append-only first-completion history. Missing advancement IDs remain in history. */
public final class Ledger {
    public record Player(UUID id, String name) {}
    public record Completion(UUID player, String advancement, long time, boolean verified) {
        public Completion(UUID player, String advancement, long time) { this(player, advancement, time, true); }
    }
    public record Snapshot(int schema, List<Player> players, List<Completion> completions) {}
    private final Map<UUID, Player> players = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Completion>> completions = new LinkedHashMap<>();

    public boolean join(UUID id, String name) {
        Objects.requireNonNull(id);
        if (name == null || name.isBlank() || name.length() > 64) throw new IllegalArgumentException("Invalid player name");
        return !new Player(id, name).equals(players.put(id, new Player(id, name)));
    }

    public boolean award(UUID player, String advancement, long time) {
        return award(player, advancement, time, true);
    }

    public boolean award(UUID player, String advancement, long time, boolean verified) {
        if (!players.containsKey(player)) throw new IllegalArgumentException("Unknown player");
        if (advancement == null || !advancement.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || time < 0)
            throw new IllegalArgumentException("Invalid completion");
        return completions.computeIfAbsent(player, ignored -> new LinkedHashMap<>())
                .putIfAbsent(advancement, new Completion(player, advancement, time, verified)) == null;
    }

    public Snapshot snapshot() {
        return new Snapshot(2, List.copyOf(players.values()), completions.values().stream()
                .flatMap(m -> m.values().stream()).sorted(Comparator.comparingLong(Completion::time)
                        .thenComparing(c -> c.player().toString()).thenComparing(Completion::advancement)).toList());
    }

    public static Ledger restore(Snapshot snapshot) {
        if (snapshot == null || (snapshot.schema() != 1 && snapshot.schema() != 2) || snapshot.players() == null || snapshot.completions() == null)
            throw new IllegalArgumentException("Unsupported or corrupt ledger schema");
        Ledger result = new Ledger();
        for (Player p : snapshot.players()) result.join(p.id(), p.name());
        for (Completion c : snapshot.completions()) {
            if (!result.award(c.player(), c.advancement(), c.time(), snapshot.schema() == 2 && c.verified())) throw new IllegalArgumentException("Duplicate completion");
        }
        return result;
    }

    public long count(UUID player, Set<String> currentIds) {
        return completions.getOrDefault(player, Map.of()).values().stream().filter(c -> c.verified() && currentIds.contains(c.advancement())).count();
    }

    public long achievers(String advancement) {
        return completions.values().stream().filter(m -> m.containsKey(advancement)).count();
    }
}
