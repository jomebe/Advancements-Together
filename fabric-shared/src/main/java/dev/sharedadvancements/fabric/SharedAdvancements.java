package dev.sharedadvancements.fabric;

import dev.sharedadvancements.core.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
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
import java.util.Locale;
import java.util.HashSet;
import java.util.ArrayList;

public final class SharedAdvancements implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("shared_advancements");
    private static MinecraftServer activeServer;
    private static Ledger ledger;
    private static Store store;
    private static SharingPolicy.Mode mode = SharingPolicy.Mode.INDIVIDUAL;
    private static Path modeFile;
    private static boolean sharing;
    private static long sharedTime;
    private static final java.util.Set<java.util.UUID> readyPlayers = new HashSet<>();
    private static Config config = new Config();
    public static final class Config {
        public boolean notifications = true;
        public String format = "[Shared Advancements] {player} — {advancement} ({achieved}/{players})";
    }
    @Override public void onInitialize() {
        Platform.registerPayload();
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> dispatcher.register(
                Commands.literal("sharedadvancements")
                        .executes(context -> { context.getSource().sendSuccess(() -> Component.literal("Shared Advancements: " + mode.name().toLowerCase(Locale.ROOT)), false); return 1; })
                        .then(Commands.literal("mode").requires(Platform::canManage)
                                .then(Commands.literal("individual").executes(context -> changeMode(SharingPolicy.Mode.INDIVIDUAL)))
                                .then(Commands.literal("cooperative").executes(context -> changeMode(SharingPolicy.Mode.COOPERATIVE))))));
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
            modeFile = server.getWorldPath(LevelResource.ROOT).resolve("shared-advancements/mode.txt");
            try {
                ledger = store.load();
                mode = Files.exists(modeFile) ? SharingPolicy.Mode.valueOf(Files.readString(modeFile).trim()) : SharingPolicy.Mode.INDIVIDUAL;
            }
            catch (IOException e) { throw new IllegalStateException("Shared Advancements history failed to load; restore .bak manually", e); }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { ledger = null; store = null; activeServer = null; modeFile = null; mode = SharingPolicy.Mode.INDIVIDUAL; sharing = false; readyPlayers.clear(); });
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
                record(player, holder, time, false);
            }
            catchUp(player);
            readyPlayers.add(player.getUUID());
            send(player, Message.reset());
            send(player, modeMessage());
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
                send(player, new Message("catalog", null, null, h.id().toString(), Platform.title(display).getString()));
        });
    }
    public static void completed(ServerPlayer player, AdvancementHolder holder) {
        if (ledger == null || holder.value().display().isEmpty()) return;
        if (player.getAdvancements().getOrStartProgress(holder).isDone()) {
            record(player, holder, sharing ? sharedTime : System.currentTimeMillis(), !sharing);
            if (!sharing && mode == SharingPolicy.Mode.COOPERATIVE) share(holder, null);
        }
    }
    private static Message modeMessage() { return new Message("mode", null, null, mode.name(), null); }

    private static int changeMode(SharingPolicy.Mode next) {
        try {
            Files.createDirectories(modeFile.getParent());
            Path temporary = modeFile.resolveSibling("mode.txt.tmp");
            Files.writeString(temporary, next.name());
            try { Files.move(temporary, modeFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, modeFile, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { throw new IllegalStateException("Could not save sharing mode", e); }
        mode = next;
        broadcast(modeMessage());
        catchUp(null);
        activeServer.getPlayerList().broadcastSystemMessage(Component.literal("[Shared Advancements] Mode: " + mode.name().toLowerCase(Locale.ROOT)
                + (mode == SharingPolicy.Mode.INDIVIDUAL ? " (existing unlocks are kept)" : " (everyone shares completed advancements)")), false);
        return 1;
    }

    private static void catchUp(ServerPlayer joining) {
        if (mode != SharingPolicy.Mode.COOPERATIVE || sharing) return;
        var current = new HashSet<String>();
        for (var holder : activeServer.getAdvancements().getAllAdvancements()) if (holder.value().display().isPresent()) current.add(holder.id().toString());
        var ids = SharingPolicy.catchUp(mode, ledger.snapshot(), current);
        for (var holder : activeServer.getAdvancements().getAllAdvancements()) if (ids.contains(holder.id().toString())) share(holder, joining);
    }

    private static void share(AdvancementHolder holder, ServerPlayer joining) {
        sharing = true;
        // Shared recipients must not displace the player who actually completed it first.
        long first = ledger.snapshot().completions().stream().filter(c -> c.advancement().equals(holder.id().toString())).mapToLong(Ledger.Completion::time).min().orElse(0);
        sharedTime = Math.max(System.currentTimeMillis(), first + 1);
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
        if (!ledger.award(player.getUUID(), id, time)) return;
        persist();
        for (var p : activeServer.getPlayerList().getPlayers()) sendCatalog(p, holder);
        broadcast(new Message("completion", null, new Ledger.Completion(player.getUUID(), id, time), null, null));
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
