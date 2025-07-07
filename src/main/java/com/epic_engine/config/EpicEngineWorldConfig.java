package com.epic_engine.config;

import net.minecraftforge.common.ForgeConfigSpec;


public class EpicEngineWorldConfig {
    public static final ForgeConfigSpec CONFIG;

    // ======== 常规设置 ========
    /** 主开关。设为false时，所有世界修改功能都将被禁用 */
    public static final ForgeConfigSpec.BooleanValue ENABLE_WORLD_MODIFICATIONS;

    /** 世界的最小建筑高度（包括基岩层） */
    public static final ForgeConfigSpec.IntValue MIN_BUILD_HEIGHT;

    /** 世界最高山峰的最大高度 */
    public static final ForgeConfigSpec.IntValue MAX_MOUNTAIN_HEIGHT;

    // ======== 地形修改 - 山地 ========
    /** 山地分层 - 低海拔边界 (低于此高度为低山区) */
    public static final ForgeConfigSpec.IntValue MOUNTAIN_LOW_THRESHOLD;

    /** 山地分层 - 中海拔边界 (此高度到高边界为中山区) */
    public static final ForgeConfigSpec.IntValue MOUNTAIN_MID_THRESHOLD;

    /** 山地增强因子 - 低山区 */
    public static final ForgeConfigSpec.DoubleValue MOUNTAIN_LOW_FACTOR;

    /** 山地增强因子 - 中山区 */
    public static final ForgeConfigSpec.DoubleValue MOUNTAIN_MID_FACTOR;

    /** 山地增强因子 - 高山区 */
    public static final ForgeConfigSpec.DoubleValue MOUNTAIN_HIGH_FACTOR;

    /** 尖峭山峰额外增强因子 */
    public static final ForgeConfigSpec.DoubleValue JAGGED_PEAKS_EXTRA_FACTOR;

    /** 山脉系统水平大小控制 - 值越小，山脉范围越大 */
    public static final ForgeConfigSpec.DoubleValue MOUNTAIN_RANGE_SCALE;

    /** 山脉连续性控制 - 值越大，山脉越连续 */
    public static final ForgeConfigSpec.DoubleValue MOUNTAIN_CONTINUITY;

    /** 山脊线强度 - 控制山脊的突出程度 */
    public static final ForgeConfigSpec.DoubleValue RIDGE_STRENGTH;

    /** 柏林噪声基础频率 */
    public static final ForgeConfigSpec.DoubleValue PERLIN_NOISE_FREQUENCY;

    /** 柏林噪声倍频数 */
    public static final ForgeConfigSpec.IntValue PERLIN_NOISE_OCTAVES;

    /** 柏林噪声持续度 */
    public static final ForgeConfigSpec.DoubleValue PERLIN_NOISE_PERSISTENCE;

    /** 柏林噪声影响强度 (0-1) */
    public static final ForgeConfigSpec.DoubleValue PERLIN_NOISE_INFLUENCE;

    // ======== 地形修改 - 平原 ========
    /** 基础地形增强高度限制 - 只对低于此高度的地形应用基础增强 */
    public static final ForgeConfigSpec.IntValue BASE_TERRAIN_HEIGHT_LIMIT;

    /** 基础地形增强因子 - 应用于所有低于高度限制的地形 */
    public static final ForgeConfigSpec.DoubleValue BASE_TERRAIN_FACTOR;

    /** 平原地形的最大高度变化（上下波动范围） */
    public static final ForgeConfigSpec.IntValue PLAINS_MAX_HEIGHT_VARIATION;

    /** 是否启用无瑕疵平原（填充地表洞穴） */
    public static final ForgeConfigSpec.BooleanValue PLAINS_FILL_SURFACE_CAVES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // 常规设置
        builder.push("general");
        ENABLE_WORLD_MODIFICATIONS = builder
                .comment("Master switch. When false, no world modifications will occur.")
                .define("enableWorldModifications", true);
        MIN_BUILD_HEIGHT = builder
                .comment("Min build height (including bedrock).")
                .defineInRange("minBuildHeight", -64, -1024, 0);
        MAX_MOUNTAIN_HEIGHT = builder
                .comment("Max mountain height.")
                .defineInRange("maxMountainHeight", 512, 256, 4096);
        builder.pop();

        // 地形修改
        builder.push("terrain_modifications");

        // 山地设置
        builder.push("mountains");
        BASE_TERRAIN_HEIGHT_LIMIT = builder
                .comment("Height limit for base terrain enhancement. Only terrain below this height gets the enhancement.")
                .defineInRange("baseTerrainHeightLimit", 80, 0, 256);
        BASE_TERRAIN_FACTOR = builder
                .comment("Enhancement factor for base terrain below the height limit.")
                .defineInRange("baseTerrainFactor", 1.1, 1.0, 1.5);
        MOUNTAIN_LOW_THRESHOLD = builder
                .comment("Low mountain threshold - below this is considered low mountain area.")
                .defineInRange("lowMountainThreshold", 96, 0, 256);
        MOUNTAIN_MID_THRESHOLD = builder
                .comment("Mid mountain threshold - below this and above low threshold is mid mountain area.")
                .defineInRange("midMountainThreshold", 128, 96, 512);
        MOUNTAIN_LOW_FACTOR = builder
                .comment("Enhancement factor for low mountain areas.")
                .defineInRange("lowMountainFactor", 1.3, 1.0, 2.0);
        MOUNTAIN_MID_FACTOR = builder
                .comment("Enhancement factor for mid mountain areas.")
                .defineInRange("midMountainFactor", 1.8, 1.0, 3.0);
        MOUNTAIN_HIGH_FACTOR = builder
                .comment("Enhancement factor for high mountain areas.")
                .defineInRange("highMountainFactor", 2.5, 1.0, 4.0);
        JAGGED_PEAKS_EXTRA_FACTOR = builder
                .comment("Extra enhancement factor for jagged peaks biome.")
                .defineInRange("jaggedPeaksExtraFactor", 1.4, 1.0, 2.0);

        // 山脉设置（放在山地子分类下）
        builder.push("mountain_range");
        MOUNTAIN_RANGE_SCALE = builder
                .comment("Controls the horizontal scale of mountain ranges (smaller values = bigger mountains).")
                .defineInRange("rangeScale", 0.0004, 0.0001, 0.01);
        MOUNTAIN_CONTINUITY = builder
                .comment("Controls how continuous mountain ranges are (higher = more continuous).")
                .defineInRange("continuity", 0.8, 0.0, 1.0);
        RIDGE_STRENGTH = builder
                .comment("Controls how prominent mountain ridges are (higher = more prominent).")
                .defineInRange("ridgeStrength", 0.5, 0.1, 1.0);

        // 噪声设置（放在山地子分类下）
        builder.push("noise");
        PERLIN_NOISE_FREQUENCY = builder
                .comment("Base frequency for Perlin noise.")
                .defineInRange("frequency", 0.006, 0.001, 0.1);
        PERLIN_NOISE_OCTAVES = builder
                .comment("Octaves for Perlin noise.")
                .defineInRange("octaves", 4, 1, 8);
        PERLIN_NOISE_PERSISTENCE = builder
                .comment("Persistence for Perlin noise.")
                .defineInRange("persistence", 0.5, 0.1, 0.9);
        PERLIN_NOISE_INFLUENCE = builder
                .comment("How much the noise affects the terrain (0-1).")
                .defineInRange("influence", 0.25, 0.0, 0.5);
        builder.pop(); // 噪声
        builder.pop(); // 山脉
        builder.pop(); // 山地

        // 平原设置
        builder.push("plains");
        PLAINS_MAX_HEIGHT_VARIATION = builder
                .comment("Maximum height variation for plains biomes (total blocks from lowest to highest).")
                .defineInRange("maxHeightVariation", 4, 1, 16);
        PLAINS_FILL_SURFACE_CAVES = builder
                .comment("Enable flawless plains by filling surface caves and holes.")
                .define("fillSurfaceCaves", true);
        builder.pop(); // 平原

        builder.pop(); // 地形修改

        CONFIG = builder.build();
    }

    /** 计算 worldHeight = maxMountainHeight - minBuildHeight + 64 */
    public static int calculateMaxBuildHeight() {
        int minY = MIN_BUILD_HEIGHT.get();
        int maxH = MAX_MOUNTAIN_HEIGHT.get();
        return (maxH - minY + 64) + minY;
    }
}