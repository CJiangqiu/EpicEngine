package com.epic_engine.mixin;

import com.epic_engine.custom.UnifiedLoadingRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericDirtMessageScreen.class)
public class GenericDirtMessageScreenMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void epic_engine$onInit(Component message, CallbackInfo ci) {
        UnifiedLoadingRenderer.updateLoadingPhase(UnifiedLoadingRenderer.LoadingPhase.PREPARE);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void epic_engine$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        UnifiedLoadingRenderer.renderUnifiedLoadingScreen(guiGraphics, this);
        ci.cancel();
    }
}
