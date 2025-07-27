package com.epic_engine.mixin;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelLoadingScreen.class)
public interface LevelLoadingScreenAccessor {

    @Accessor("progressListener")
    StoringChunkProgressListener epic_engine$getProgressListener();

    @Accessor("done")
    boolean epic_engine$isDone();
}