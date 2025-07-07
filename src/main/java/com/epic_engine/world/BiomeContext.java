package com.epic_engine.world;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * 用于在 NoiseChunk 构造时，将当前 chunk 的 biome Holder 写入 ThreadLocal，
 * 然后在 EnhancedMountainDensityFunction.compute() 里读回来做判断。
 */
public class BiomeContext {
    public static final ThreadLocal<Holder<Biome>> CURRENT = new ThreadLocal<>();
}
