package dev.sharedadvancements.fabric;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class TooltipUi {
    public static void draw(GuiGraphicsExtractor graphics, Font font, List<Component> lines, int x, int y) {
        graphics.setComponentTooltipForNextFrame(font, lines, x, y);
    }
}
