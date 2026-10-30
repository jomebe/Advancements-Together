package dev.sharedadvancements.core;

import java.util.*;

/** One tile per advancement; shared recipients are never credited as achievers. */
public final class HistoryView {
    public record Entry(String id, long time, List<Ledger.Completion> credits) {
        public boolean completed() { return time >= 0; }
    }
    public static List<Entry> entries(Ledger.Snapshot snapshot, Set<String> catalog, UUID player, boolean timeline) {
        Map<String, List<Ledger.Completion>> groups = new LinkedHashMap<>();
        for (String id : catalog) groups.put(id, new ArrayList<>());
        for (var c : snapshot.completions()) groups.computeIfAbsent(c.advancement(), ignored -> new ArrayList<>()).add(c);
        List<Entry> result = new ArrayList<>();
        for (var group : groups.entrySet()) {
            var credits = group.getValue().stream().filter(Ledger.Completion::verified).toList();
            if (player != null && credits.stream().noneMatch(c -> c.player().equals(player))) continue;
            long time = group.getValue().stream().mapToLong(Ledger.Completion::time).min().orElse(-1);
            if (timeline && time < 0) continue;
            result.add(new Entry(group.getKey(), time, credits));
        }
        result.sort(Comparator.comparingLong((Entry e) -> e.completed() ? e.time() : Long.MAX_VALUE).thenComparing(Entry::id));
        return List.copyOf(result);
    }
}
