package com.epic_engine.mixin;

import com.epic_engine.custom.UnifiedLoadingRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ProgressScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Progress Screen Mixin
 * Replaces various progress display screens
 */
@Mixin(ProgressScreen.class)
public class ProgressScreenMixin {

    @Unique
    private static final Logger epicEngine$LOGGER = LogManager.getLogger();

    /**
     * Initialize loading phase when screen is created
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void epicEngine$onInit(CallbackInfo ci) {
        UnifiedLoadingRenderer.updateLoadingPhase(UnifiedLoadingRenderer.LoadingPhase.LOADING);
        epicEngine$LOGGER.debug("[EPIC ENGINE]: ProgressScreen initialized - Phase: LOADING");
    }

    /**
     * Replace original rendering with unified loading screen
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void epicEngine$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        UnifiedLoadingRenderer.renderUnifiedLoadingScreen(guiGraphics,this);
        ci.cancel();
    }
}