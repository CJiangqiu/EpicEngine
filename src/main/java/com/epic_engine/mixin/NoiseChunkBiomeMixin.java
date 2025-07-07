package com.epic_engine.mixin;

import com.epic_engine.world.BiomeContext;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 NoiseChunk.forChunk(...) 方法开始处捕获当前 Chunk 的 biome Holder，
 * 并存入 ThreadLocal，供 EnhancedMountainDensityFunction 使用。
 */
@Mixin(NoiseChunk.class)
public abstract class NoiseChunkBiomeMixin {

    @Inject(
        method = "forChunk",
        at     = @At("HEAD")
    )
    private static void epic_engine$captureBiomeBeforeCreate(
            ChunkAccess chunk,
            RandomState state,
            DensityFunctions.BeardifierOrMarker beardifier,
            NoiseGeneratorSettings settings,
            Aquifer.FluidPicker fluidPicker,
            Blender blender,
            CallbackInfoReturnable<NoiseChunk> cir
    ) {
        // 取 Chunk 中心坐标
        int bx = chunk.getPos().getMiddleBlockX();
        int bz = chunk.getPos().getMiddleBlockZ();

        // 安全获取 biome，避免尚未注册时崩溃
        Holder<Biome> holder = null;
        try {
            holder = chunk.getNoiseBiome(bx, 0, bz);
        } catch (IllegalStateException ignored) {
            // Biome 系统尚未就绪，保持 holder 为 null
        }

        BiomeContext.CURRENT.set(holder);
    }
}