package com.epic_engine.mixin;

import com.epic_engine.custom.UnifiedLoadingRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReceivingLevelScreen.class)
public class ReceivingLevelScreenMixin{

    @Inject(method = "<init>", at = @At("TAIL"))
    private void epic_engine$onInit(CallbackInfo ci) {
        UnifiedLoadingRenderer.updateLoadingPhase(UnifiedLoadingRenderer.LoadingPhase.TERRAIN);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void epic_engine$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        UnifiedLoadingRenderer.renderUnifiedLoadingScreen(guiGraphics, this);
        ci.cancel();
    }
}