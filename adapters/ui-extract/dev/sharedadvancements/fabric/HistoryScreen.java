package dev.sharedadvancements.fabric;

import dev.sharedadvancements.core.Ledger;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** A paginated detail view linked from the vanilla advancement screen. */
public final class HistoryScreen extends Screen {
    private final Screen parent;
    private int page;
    private boolean timeline;
    private UUID selected;
    private final DateTimeFormatter dates = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    public HistoryScreen(Screen parent) { super(Component.literal("Shared Advancements")); this.parent = parent; }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { page = Math.max(0, page - 1); }).bounds(10, height - 25, 35, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> page++).bounds(50, height - 25, 35, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Players / Timeline"), b -> { timeline = !timeline; selected = null; page = 0; }).bounds(90, height - 25, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Player >"), b -> { var players = SharedClient.ledger.snapshot().players(); if (!players.isEmpty()) { int index = -1; for (int i = 0; i < players.size(); i++) if (players.get(i).id().equals(selected)) index = i; selected = players.get((index + 1) % players.size()).id(); page = 0; } }).bounds(245, height - 25, 75, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(width - 65, height - 25, 55, 20).build());
    }
    @Override public void onClose() { ClientUi.open(minecraft, parent); }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xF0181818);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, Component.literal("Shared Advancements - " + SharedClient.mode), width / 2, 10, 0xFFFFFFFF);
        var snapshot = SharedClient.ledger.snapshot();
        int total = SharedClient.catalog.size();
        long completed = snapshot.players().stream().mapToLong(p -> SharedClient.ledger.count(p.id(), SharedClient.catalog.keySet())).sum();
        long possible = (long) total * snapshot.players().size();
        graphics.text(font, "All players: " + completed + "/" + possible + " (" + (possible == 0 ? 0 : completed * 100 / possible) + "%)", 12, 28, 0xFFDDDDDD);
        int rows = Math.max(1, (height - 90) / 24);
        if (timeline || selected != null) {
            var records = snapshot.completions().stream().filter(c -> selected == null || c.player().equals(selected)).toList();
            page = Math.min(page, Math.max(0, (records.size() - 1) / rows));
            for (int i = page * rows; i < Math.min(records.size(), (page + 1) * rows); i++) {
                var c = records.get(i); int y = 48 + (i % rows) * 24;
                String name = snapshot.players().stream().filter(p -> p.id().equals(c.player())).findFirst().map(Ledger.Player::name).orElse("?");
                String label = SharedClient.catalog.getOrDefault(c.advancement(), "[archived] " + c.advancement());
                boolean first = snapshot.completions().stream().filter(x -> x.advancement().equals(c.advancement())).findFirst().orElseThrow().equals(c);
                graphics.text(font, font.plainSubstrByWidth((first ? "★ " : "") + name + " — " + label, width - 24), 12, y, color(c.player()));
                graphics.text(font, dates.format(Instant.ofEpochMilli(c.time())), 12, y + 11, 0xFFAAAAAA);
            }
        } else {
            var players = snapshot.players().stream().sorted(Comparator.<Ledger.Player>comparingLong(p -> SharedClient.ledger.count(p.id(), SharedClient.catalog.keySet())).reversed().thenComparing(Ledger.Player::name)).toList();
            page = Math.min(page, Math.max(0, (players.size() - 1) / rows));
            for (int i = page * rows; i < Math.min(players.size(), (page + 1) * rows); i++) {
                var p = players.get(i); int y = 48 + (i % rows) * 24;
                long n = SharedClient.ledger.count(p.id(), SharedClient.catalog.keySet());
                ClientUi.face(graphics, minecraft, p.id(), 12, y);
                graphics.text(font, (i + 1) + ". " + p.name() + "  " + n + "/" + total + " (" + (total == 0 ? 0 : n * 100 / total) + "%)", 34, y, color(p.id()));
                graphics.fill(34, y + 13, width - 15, y + 16, 0xFF444444);
                graphics.fill(34, y + 13, 34 + (int) ((width - 49) * n / Math.max(1, total)), y + 16, color(p.id()));
            }
        }
    }
    public static int color(UUID id) { return 0xFF808080 | (id.hashCode() & 0x007F7F7F); }
}
