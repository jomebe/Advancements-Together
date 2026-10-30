package dev.sharedadvancements.core;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SharingPolicyTest {
    @Test void individualModeNeverGrantsOtherPlayersHistory() {
        Ledger ledger = history();
        assertTrue(SharingPolicy.catchUp(SharingPolicy.Mode.INDIVIDUAL, ledger.snapshot(), Set.of("a:first")).isEmpty());
        assertEquals(2, ledger.snapshot().completions().size());
    }
    @Test void cooperativeCatchUpIncludesOfflineHistoryButSkipsRemovedIds() {
        Ledger restored = Ledger.restore(history().snapshot());
        assertEquals(Set.of("a:first"), SharingPolicy.catchUp(SharingPolicy.Mode.COOPERATIVE, restored.snapshot(), Set.of("a:first", "a:not_done")));
    }
    @Test void switchingBackDoesNotErasePreviouslyGrantedRecords() {
        Ledger ledger = history();
        var before = ledger.snapshot();
        SharingPolicy.catchUp(SharingPolicy.Mode.COOPERATIVE, before, Set.of("a:first"));
        SharingPolicy.catchUp(SharingPolicy.Mode.INDIVIDUAL, before, Set.of("a:first"));
        assertEquals(before, ledger.snapshot());
    }
    private Ledger history() {
        Ledger ledger = new Ledger(); UUID player = UUID.randomUUID(); ledger.join(player, "Alex");
        ledger.award(player, "a:first", 10); ledger.award(player, "old:removed", 20); return ledger;
    }
}
