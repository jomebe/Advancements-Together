package dev.sharedadvancements.fabric;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Platform {
    public static Identifier id() { return Identifier.fromNamespaceAndPath("shared_advancements", "history_v3"); }
    public static void registerPayload() { PayloadTypeRegistry.clientboundPlay().register(HistoryPayload.TYPE, HistoryPayload.CODEC); }
    public static Identifier parse(String value) { return Identifier.parse(value); }
    public static net.minecraft.world.item.ItemStack icon(DisplayInfo display) { return display.getIcon().create(); }
    public static Component description(DisplayInfo display) { return display.getDescription(); }
    public static String name(ServerPlayer player) { return player.getName().getString(); }
    public static boolean hidden(DisplayInfo display) { return display.isHidden(); }
    public static Component title(DisplayInfo display) { return display.getTitle(); }
}
