package dev.sharedadvancements.fabric;

import dev.sharedadvancements.core.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.ArrayList;

public final class SharedAdvancements implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("shared_advancements");
    private static MinecraftServer activeServer;
    private static Ledger ledger;
    private static Store store;
    private static boolean sharing;
    private static final java.util.Set<java.util.UUID> readyPlayers = new HashSet<>();
    private static Config config = new Config();
    public static final class Config {
        public boolean notifications = true;
        public String format = "[Shared Advancements] {player} — {advancement} · 모두에게 공유되었습니다";
    }
    @Override public void onInitialize() {
        Platform.registerPayload();
        Path cfg = FabricLoader.getInstance().getConfigDir().resolve("shared-advancements.json");
        try {
            if (Files.exists(cfg)) {
                config = Store.JSON.fromJson(Files.readString(cfg), Config.class);
                if (config == null || config.format == null || config.format.length() > 2000) throw new IOException("Invalid notification configuration");
            } else Files.writeString(cfg, Store.JSON.toJson(config));
        } catch (IOException | RuntimeException e) { throw new IllegalStateException("Cannot load " + cfg, e); }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            activeServer = server;
            store = new Store(server.getWorldPath(LevelResource.ROOT).resolve("shared-advancements/history.json"));
            try {
                ledger = store.load();
            }
            catch (IOException e) { throw new IllegalStateException("Shared Advancements history failed to load; restore .bak manually", e); }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { ledger = null; store = null; activeServer = null; sharing = false; readyPlayers.clear(); });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> readyPlayers.remove(handler.player.getUUID()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (ledger == null) return;
            if (ledger.join(player.getUUID(), Platform.name(player))) {
                persist(); broadcast(new Message("player", new Ledger.Player(player.getUUID(), Platform.name(player)), null, null, null));
            }
            // Import pre-installation completions using vanilla criterion timestamps.
            for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
                if (holder.value().display().isEmpty()) continue;
                var progress = player.getAdvancements().getOrStartProgress(holder);
                if (!progress.isDone()) continue;
                long time = 0;
                for (String criterion : progress.getCompletedCriteria()) {
                    var detail = progress.getCriterion(criterion);
                    if (detail != null && detail.getObtained() != null) time = Math.max(time, detail.getObtained().toEpochMilli());
                }
                if (ledger.achievers(holder.id().toString()) == 0) record(player, holder, time, false);
            }
            catchUp(player);
            readyPlayers.add(player.getUUID());
            send(player, Message.reset());
            for (var p : ledger.snapshot().players()) send(player, new Message("player", p, null, null, null));
            for (var h : server.getAdvancements().getAllAdvancements()) sendCatalog(player, h);
            for (var c : ledger.snapshot().completions()) send(player, new Message("completion", null, c, null, null));
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success && ledger != null) for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                send(p, new Message("catalog-reset", null, null, null, null));
                for (var h : server.getAdvancements().getAllAdvancements()) sendCatalog(p, h);
            }
        });
    }
    private static void sendCatalog(ServerPlayer player, AdvancementHolder h) {
        h.value().display().ifPresent(display -> {
            if (!Platform.hidden(display) || ledger.achievers(h.id().toString()) > 0)
                send(player, Message.catalog(h.id().toString(), Platform.title(display), Platform.description(display),
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Platform.icon(display).getItem()).toString()));
        });
    }
    public static void completed(ServerPlayer player, AdvancementHolder holder) {
        if (ledger == null || sharing || holder.value().display().isEmpty()) return;
        if (player.getAdvancements().getOrStartProgress(holder).isDone()) {
            record(player, holder, System.currentTimeMillis(), true);
            share(holder, null);
        }
    }
    private static void catchUp(ServerPlayer joining) {
        if (sharing) return;
        var current = new HashSet<String>();
        for (var holder : activeServer.getAdvancements().getAllAdvancements()) if (holder.value().display().isPresent()) current.add(holder.id().toString());
        var ids = SharingPolicy.catchUp(ledger.snapshot(), current);
        for (var holder : activeServer.getAdvancements().getAllAdvancements()) if (ids.contains(holder.id().toString())) share(holder, joining);
    }

    private static void share(AdvancementHolder holder, ServerPlayer joining) {
        sharing = true;
        try {
            var players = new ArrayList<>(activeServer.getPlayerList().getPlayers());
            // Fabric JOIN can fire before PlayerList contains the joining player.
            if (joining != null && !players.contains(joining)) players.add(joining);
            for (var player : players) {
                var progress = player.getAdvancements().getOrStartProgress(holder);
                if (progress.isDone()) continue;
                var remaining = new ArrayList<String>();
                progress.getRemainingCriteria().forEach(remaining::add);
                for (String criterion : remaining) player.getAdvancements().award(holder, criterion);
            }
        } finally { sharing = false; }
    }
    private static void record(ServerPlayer player, AdvancementHolder holder, long time, boolean notify) {
        String id = holder.id().toString();
        String name = Platform.name(player);
        if (ledger.join(player.getUUID(), name)) broadcast(new Message("player", new Ledger.Player(player.getUUID(), name), null, null, null));
        if (!ledger.award(player.getUUID(), id, time, notify)) return;
        persist();
        for (var p : activeServer.getPlayerList().getPlayers()) sendCatalog(p, holder);
        broadcast(new Message("completion", null, new Ledger.Completion(player.getUUID(), id, time, notify), null, null));
        if (notify && config.notifications) {
            String text = config.format.replace("{player}", name).replace("{advancement}", Platform.title(holder.value().display().orElseThrow()).getString())
                    .replace("{achieved}", Long.toString(ledger.achievers(id))).replace("{players}", Integer.toString(ledger.snapshot().players().size()));
            activeServer.getPlayerList().broadcastSystemMessage(Component.literal(text), false);
        }
    }
    private static void persist() {
        try { store.save(ledger); }
        catch (IOException e) { LOG.error("Could not persist advancement history", e); throw new IllegalStateException("History save failed", e); }
    }
    private static void send(ServerPlayer player, Message message) {
        if (readyPlayers.contains(player.getUUID()) && ServerPlayNetworking.canSend(player, HistoryPayload.TYPE)) ServerPlayNetworking.send(player, new HistoryPayload(Store.JSON.toJson(message)));
    }
    private static void broadcast(Message message) { for (var p : activeServer.getPlayerList().getPlayers()) send(p, message); }
}
