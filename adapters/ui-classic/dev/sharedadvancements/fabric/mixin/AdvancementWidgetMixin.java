package dev.sharedadvancements.fabric.mixin;

import dev.sharedadvancements.fabric.SharedClient;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementWidget.class)
abstract class AdvancementWidgetMixin {
    @Shadow @Final private AdvancementNode advancementNode;
    @Inject(method = "drawHover", at = @At("TAIL"))
    private void sharedAdvancements$hover(CallbackInfo ci) { SharedClient.hovered = advancementNode.holder().id().toString(); }
}
