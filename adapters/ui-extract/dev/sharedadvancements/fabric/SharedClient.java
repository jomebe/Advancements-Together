package dev.sharedadvancements.fabric;

import dev.sharedadvancements.core.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class SharedClient implements ClientModInitializer {
    public static Ledger ledger = new Ledger();
    public static final Map<String, String> catalog = new LinkedHashMap<>();
    public static boolean connected;
    public static String mode = "Individual";
    public static String hovered;
    public static void clear() { ledger = new Ledger(); catalog.clear(); connected = false; mode = "Individual"; }
    @Override public void onInitializeClient() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientPlayNetworking.registerGlobalReceiver(HistoryPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            Message m = Store.JSON.fromJson(payload.json(), Message.class);
            switch (m.kind()) {
                case "reset" -> { clear(); connected = true; }
                case "mode" -> mode = SharingPolicy.Mode.valueOf(m.id()) == SharingPolicy.Mode.COOPERATIVE ? "Co-op" : "Individual";
                case "player" -> ledger.join(m.player().id(), m.player().name());
                case "completion" -> ledger.award(m.completion().player(), m.completion().advancement(), m.completion().time());
                case "catalog" -> catalog.put(m.id(), m.title());
                case "catalog-reset" -> catalog.clear();
                default -> throw new IllegalArgumentException("Unsupported Shared Advancements protocol");
            }
        }));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof AdvancementsScreen && connected) {
                ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> hovered = null);
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
                    if (hovered == null) return;
                    var snapshot = ledger.snapshot();
                    int panelWidth = Math.min(220, s.width - 16);
                    int x = s.width >= 740 ? 8 : s.width - panelWidth - 8;
                    int y = 30;
                    int rows = Math.min(snapshot.players().size(), Math.max(1, (s.height - 70) / 22));
                    graphics.fill(x - 3, y - 3, x + panelWidth, y + 20 + rows * 22, 0xEE202020);
                    graphics.text(client.font, "Shared: " + ledger.achievers(hovered) + "/" + snapshot.players().size(), x, y, 0xFFFFFFFF);
                    for (int i = 0; i < rows; i++) {
                        var p = snapshot.players().get(i);
                        var c = snapshot.completions().stream().filter(a -> a.player().equals(p.id()) && a.advancement().equals(hovered)).findFirst();
                        int rowY = y + 16 + i * 22;
                        ClientUi.face(graphics, client, p.id(), x, rowY);
                        graphics.text(client.font, p.name() + (c.isPresent() ? " ✓" : " —"), x + 20, rowY, HistoryScreen.color(p.id()));
                        String date = c.map(a -> java.time.Instant.ofEpochMilli(a.time()).toString()).orElse("Not achieved");
                        graphics.text(client.font, date, x + 20, rowY + 10, 0xFFBBBBBB);
                    }
                });
                Screens.getWidgets(screen).add(Button.builder(Component.literal("Shared"), button ->
                        ClientUi.open(client, new HistoryScreen(screen))).bounds(8, 4, 100, 20).build());
            }
        });
    }
}
