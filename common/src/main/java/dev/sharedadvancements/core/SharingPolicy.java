package dev.sharedadvancements.core;

import java.util.Set;
import java.util.stream.Collectors;

public final class SharingPolicy {
    /** Historical IDs absent from the current server remain stored but cannot be granted. */
    public static Set<String> catchUp(Ledger.Snapshot history, Set<String> currentIds) {
        return history.completions().stream().map(Ledger.Completion::advancement)
                .filter(currentIds::contains).collect(Collectors.toSet());
    }
}
