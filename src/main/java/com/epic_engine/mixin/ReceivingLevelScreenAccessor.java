package com.epic_engine.mixin;

import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ReceivingLevelScreen.class)
public interface ReceivingLevelScreenAccessor {

    @Accessor("loadingPacketsReceived")
    boolean epic_engine$isLoadingPacketsReceived();

    @Accessor("createdAt")
    long epic_engine$getCreatedAt();
}