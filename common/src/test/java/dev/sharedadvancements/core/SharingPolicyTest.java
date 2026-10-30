package dev.sharedadvancements.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SharingPolicyTest {
 @Test void catchUpAlwaysSharesExistingIdsIncludingOfflineHistory(){
  Ledger l=new Ledger();UUID p=UUID.randomUUID();l.join(p,"Alex");l.award(p,"a:first",10);l.award(p,"old:removed",20);
  assertEquals(Set.of("a:first"),SharingPolicy.catchUp(Ledger.restore(l.snapshot()).snapshot(),Set.of("a:first","a:locked")));
 }
 @Test void legacyRecipientsArePreservedWithoutInventingAchievers(){
  UUID a=UUID.randomUUID(),b=UUID.randomUUID();
  Ledger l=Ledger.restore(new Ledger.Snapshot(1,List.of(new Ledger.Player(a,"A"),new Ledger.Player(b,"B")),List.of(new Ledger.Completion(a,"a:first",10),new Ledger.Completion(b,"a:first",11))));
  assertEquals(2,l.snapshot().completions().size());assertEquals(0,l.count(a,Set.of("a:first")));
  var entries=HistoryView.entries(l.snapshot(),Set.of("a:first"),null,true);
  assertEquals(1,entries.size());assertTrue(entries.getFirst().completed());assertTrue(entries.getFirst().credits().isEmpty());assertEquals(l.snapshot(),Ledger.restore(l.snapshot()).snapshot());
 }
 @Test void filtersOnlyPersonalCreditsAndOrdersTimeline(){
  Ledger l=new Ledger();UUID a=UUID.randomUUID(),b=UUID.randomUUID();l.join(a,"A");l.join(b,"B");l.award(a,"a:later",20);l.award(b,"a:first",10);l.award(a,"a:legacy",5,false);
  var catalog=Set.of("a:later","a:first","a:legacy","a:locked");
  assertEquals(List.of("a:later"),HistoryView.entries(l.snapshot(),catalog,a,false).stream().map(HistoryView.Entry::id).toList());
  assertEquals(List.of("a:legacy","a:first","a:later"),HistoryView.entries(l.snapshot(),catalog,null,true).stream().map(HistoryView.Entry::id).toList());
  assertEquals(4,HistoryView.entries(l.snapshot(),catalog,null,false).size());
 }
}
