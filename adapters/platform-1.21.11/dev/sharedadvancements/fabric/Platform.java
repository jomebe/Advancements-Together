package dev.sharedadvancements.fabric;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Platform {
    public static Identifier id() { return Identifier.fromNamespaceAndPath("shared_advancements", "history_v1"); }
    public static void registerPayload() { PayloadTypeRegistry.playS2C().register(HistoryPayload.TYPE, HistoryPayload.CODEC); }
    public static String name(ServerPlayer player) { return player.getName().getString(); }
    public static boolean hidden(DisplayInfo display) { return display.isHidden(); }
    public static Component title(DisplayInfo display) { return display.getTitle(); }
}
