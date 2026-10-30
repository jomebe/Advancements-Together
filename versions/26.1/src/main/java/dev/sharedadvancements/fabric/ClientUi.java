package dev.sharedadvancements.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import java.util.UUID;

public final class ClientUi {
    public static void open(Minecraft client, Screen screen) { client.setScreen(screen); }
    public static void face(GuiGraphicsExtractor graphics, Minecraft client, UUID id, int x, int y) {
        var player = client.getConnection() == null ? null : client.getConnection().getPlayerInfo(id);
        var skin = player == null ? DefaultPlayerSkin.get(id) : player.getSkin();
        PlayerFaceExtractor.extractRenderState(graphics, skin, x, y, 16);
    }
}
