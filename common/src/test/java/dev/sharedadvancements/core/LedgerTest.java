package dev.sharedadvancements.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LedgerTest {
    @Test void transientWindowsFileLockIsRetried() throws Exception {
        int[] attempts={0};
        Store.retryAccessDenied(()->{if(++attempts[0]<3)throw new AccessDeniedException("history.json");});
        assertEquals(3,attempts[0]);
    }
    @Test void permanentDenialIsBoundedAndOtherIoErrorsAreNotRetried() {
        int[] attempts={0};
        assertThrows(AccessDeniedException.class,()->Store.retryAccessDenied(()->{attempts[0]++;throw new AccessDeniedException("history.json");}));
        assertEquals(5,attempts[0]); attempts[0]=0;
        assertThrows(java.io.IOException.class,()->Store.retryAccessDenied(()->{attempts[0]++;throw new java.io.IOException("disk error");}));
        assertEquals(1,attempts[0]);
    }
    @TempDir Path directory;
    final UUID a = UUID.randomUUID();
    Ledger ledger() { Ledger l = new Ledger(); l.join(a, "Alex"); return l; }
    @Test void duplicateDoesNotReplaceOriginalTime() {
        Ledger l = ledger(); assertTrue(l.award(a, "minecraft:story/root", 20));
        assertFalse(l.award(a, "minecraft:story/root", 30));
        assertEquals(20, l.snapshot().completions().getFirst().time());
    }
    @Test void renameKeepsIdentityAndHistory() {
        Ledger l = ledger(); l.award(a, "minecraft:story/root", 20); l.join(a, "Renamed");
        assertEquals(1, l.snapshot().players().size()); assertEquals(1, l.achievers("minecraft:story/root"));
    }
    @Test void missingIdsRetainedButExcludedFromCurrentProgress() {
        Ledger l = ledger(); l.award(a, "old:removed", 20);
        assertEquals(0, l.count(a, Set.of("minecraft:story/root")));
        assertEquals(1, Ledger.restore(l.snapshot()).snapshot().completions().size());
    }
    @Test void rejectsUntrustedAndInvalidRecords() {
        Ledger l = ledger(); assertThrows(IllegalArgumentException.class, () -> l.award(UUID.randomUUID(), "a:b", 1));
        assertThrows(IllegalArgumentException.class, () -> l.award(a, "../invalid", 1));
        assertThrows(IllegalArgumentException.class, () -> l.award(a, "a:b", -1));
        assertThrows(IllegalArgumentException.class, () -> Ledger.restore(new Ledger.Snapshot(9, List.of(), List.of())));
    }
    @Test void restartRetainsOfflinePlayersAndBackup() throws Exception {
        Store s = new Store(directory.resolve("history.json")); Ledger l = ledger();
        s.save(l); l.award(a, "minecraft:story/root", 20); s.save(l);
        assertEquals(l.snapshot(), s.load().snapshot());
        assertEquals(0, new Store(directory.resolve("history.json.bak")).load().snapshot().completions().size());
    }
    @Test void corruptFileIsNeverSilentlyOverwritten() throws Exception {
        Path p = directory.resolve("history.json"); Files.writeString(p, "broken");
        assertThrows(java.io.IOException.class, () -> new Store(p).load()); assertEquals("broken", Files.readString(p));
    }
    @Test void legacyMigrationKeepsPermanentOriginalAndNeverCreditsSharedRecipients() throws Exception {
        Path file=directory.resolve("history.json");
        String original=Store.JSON.toJson(new Ledger.Snapshot(1,List.of(new Ledger.Player(a,"Alex")),List.of(new Ledger.Completion(a,"a:old",1))));
        Files.writeString(file,original);
        Store store=new Store(file); Ledger migrated=store.load();
        assertFalse(migrated.snapshot().completions().getFirst().verified());
        store.save(migrated); migrated.award(a,"a:new",2); store.save(migrated);
        assertEquals(original,Files.readString(directory.resolve("history.schema-1.json")));
        assertEquals(1,store.load().count(a,Set.of("a:old","a:new")));
    }
    @Test void timelineIsChronologicalAndCountsArePerPlayer() {
        Ledger l = ledger(); UUID b = UUID.randomUUID(); l.join(b, "Steve");
        l.award(a, "a:b", 20); l.award(b, "a:b", 10);
        assertEquals(b, l.snapshot().completions().getFirst().player()); assertEquals(2, l.achievers("a:b"));
    }
}
