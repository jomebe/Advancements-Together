package dev.sharedadvancements.fabric;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Platform {
    public static ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath("shared_advancements", "history_v2"); }
    public static void registerPayload() { PayloadTypeRegistry.playS2C().register(HistoryPayload.TYPE, HistoryPayload.CODEC); }
    public static boolean canManage(net.minecraft.commands.CommandSourceStack source) { return source.hasPermission(2); }
    public static String name(ServerPlayer player) { return player.getName().getString(); }
    public static boolean hidden(DisplayInfo display) { return display.isHidden(); }
    public static Component title(DisplayInfo display) { return display.getTitle(); }
}
