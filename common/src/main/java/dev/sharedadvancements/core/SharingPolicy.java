package dev.sharedadvancements.core;

import java.util.Set;
import java.util.stream.Collectors;

public final class SharingPolicy {
    public enum Mode { INDIVIDUAL, COOPERATIVE }

    /** Historical IDs absent from the current server remain stored but cannot be granted. */
    public static Set<String> catchUp(Mode mode, Ledger.Snapshot history, Set<String> currentIds) {
        if (mode == Mode.INDIVIDUAL) return Set.of();
        return history.completions().stream().map(Ledger.Completion::advancement)
                .filter(currentIds::contains).collect(Collectors.toSet());
    }
}
