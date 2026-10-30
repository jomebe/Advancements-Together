package dev.sharedadvancements.fabric;

import dev.sharedadvancements.core.Ledger;
public record Message(String kind, Ledger.Player player, Ledger.Completion completion, String id, String title) {
    public static Message reset() { return new Message("reset", null, null, null, null); }
}
