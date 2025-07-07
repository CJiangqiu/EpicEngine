package com.epic_engine.world;

import com.epic_engine.config.EpicEngineWorldConfig;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 增强山脉密度函数：创造连绵不绝的壮丽雪山
 * 平原地形平坦化：强制切割地形创造超级平坦的平原
 */
public class EnhancedMountainDensityFunction implements DensityFunction {
    private final DensityFunction original;

    // 山地生物群系标签
    private static final TagKey<Biome> MOUNTAIN_TAG = TagKey.create(Registries.BIOME,
            new ResourceLocation("minecraft", "is_mountain"));

    // 尖峭山峰生物群系ID
    private static final String JAGGED_PEAKS_ID = "minecraft:jagged_peaks";

    // 平原类型生物群系ID集合
    private static final Set<String> PLAINS_BIOMES = Set.of(
        "minecraft:plains",
        "minecraft:sunflower_plains"
    );

    // 草甸生物群系ID
    private static final String MEADOW_ID = "minecraft:meadow";

    // 缓存群系基准高度
    private static final Map<Long, Integer> BIOME_BASE_HEIGHTS = new HashMap<>();

    public EnhancedMountainDensityFunction(DensityFunction original) {
        this.original = original;
    }

    @Override
    public double compute(@NotNull FunctionContext ctx) {
        if (!EpicEngineWorldConfig.ENABLE_WORLD_MODIFICATIONS.get()) {
            return original.compute(ctx);
        }
        double val = original.compute(ctx);
        int x = ctx.blockX();
        int y = ctx.blockY();
        int z = ctx.blockZ();
        return enhanceTerrain(val, x, y, z);
    }

    @Override
    public void fillArray(double @NotNull [] array, @NotNull ContextProvider prov) {
        if (!EpicEngineWorldConfig.ENABLE_WORLD_MODIFICATIONS.get()) {
            original.fillArray(array, prov);
            return;
        }
        original.fillArray(array, prov);
        for (int i = 0; i < array.length; i++) {
            FunctionContext ctx = prov.forIndex(i);
            int x = ctx.blockX();
            int y = ctx.blockY();
            int z = ctx.blockZ();
            array[i] = enhanceTerrain(array[i], x, y, z);
        }
    }

    /**
     * 生成大尺度山脉控制值 - 决定山脉的连续性和位置
     */
    private static double getMountainRangeControl(int x, int z) {
        // 使用超低频噪声定义山脉的总体布局
        double frequency = EpicEngineWorldConfig.MOUNTAIN_RANGE_SCALE.get();
        double noise = PerlinNoise.fractalNoise(x * frequency, 0, z * frequency, 3, 0.6);

        // 将噪声映射到[0, 1]范围
        return (noise + 1.0) * 0.5;
    }

    /**
     * 生成山脊线控制 - 创造连绵的山脊
     */
    private static double getRidgeControl(int x, int z) {
        // 使用不同方向的一维噪声创造交叉的山脊线
        double freq1 = 0.002;
        double freq2 = 0.0015;

        // 创建主要山脊
        double ridge1 = 1.0 - Math.abs(PerlinNoise.noise(x * freq1, 0, z * freq1));
        // 创建次要山脊，与主要山脊成一定角度
        double ridge2 = 1.0 - Math.abs(PerlinNoise.noise(x * freq2, 0, z * freq2 + 100));

        // 合并山脊线，形成网络
        return Math.max(ridge1, ridge2);
    }

    /**
     * 获取生物群系区域的唯一键
     */
    private static long getBiomeRegionKey(int x, int z, int regionSize) {
        long regionX = Math.floorDiv(x, regionSize);
        long regionZ = Math.floorDiv(z, regionSize);
        return (regionX << 32) | (regionZ & 0xFFFFFFFFL);
    }

    /**
     * 获取或计算平原区域的基准高度
     * 这个方法会确定平原区域的固定高度
     */
    private static int getPlainBaseHeight(int x, int z, boolean isMeadow) {
        // 定义生物群系区域大小
        int regionSize = isMeadow ? 128 : 256;

        // 获取区域唯一键
        long regionKey = getBiomeRegionKey(x, z, regionSize);

        // 检查缓存
        if (BIOME_BASE_HEIGHTS.containsKey(regionKey)) {
            return BIOME_BASE_HEIGHTS.get(regionKey);
        }

        // 基础高度 - 通常为海平面
        int baseHeight = 63;

        // 获取高度变化配置
        int maxVariation = EpicEngineWorldConfig.PLAINS_MAX_HEIGHT_VARIATION.get();

        // 检测是否在山地附近
        boolean nearMountains = getMountainProximityFactor(x, z) > 0.3;

        // 确定基准高度
        int targetHeight;

        if (isMeadow || nearMountains) {
            // 草甸和山地附近的平原凹陷
            int variation = maxVariation / 2;
            targetHeight = baseHeight - variation;
        } else {
            // 普通平原凸起
            int variation = maxVariation / 2;
            targetHeight = baseHeight + variation;
        }

        // 添加轻微变化，避免所有区域完全相同
        // 使用区域键的哈希值增加一点随机性
        int hashCode = Long.hashCode(regionKey);
        int smallVariation = Math.abs(hashCode % (maxVariation + 1));

        if (isMeadow || nearMountains) {
            // 草甸/山地附近区域向下随机变化
            targetHeight -= smallVariation / 3;
        } else {
            // 普通平原向上随机变化
            targetHeight += smallVariation / 3;
        }

        // 缓存计算结果
        BIOME_BASE_HEIGHTS.put(regionKey, targetHeight);

        return targetHeight;
    }

    /**
     * 强制平坦化平原地形 - 切割方法
     */
    private static double cutFlattenPlainsTerrain(double v, int x, int y, int z, String biomeId) {
        boolean isPlainsType = isPlainsBiome(biomeId);
        boolean isMeadow = isMeadowBiome(biomeId);

        if (!isPlainsType && !isMeadow) {
            return v; // 不是目标群系，不处理
        }

        // 获取此平原区域的基准高度
        int baseHeight = getPlainBaseHeight(x, z, isMeadow);

        // 极端切割法: 在基准高度之上的部分，全部为空气
        if (y > baseHeight) {
            return -1.0; // 强制设为空气
        }

        // 地表以下几个方块的地表填充
        int surfaceDepth = 4;

        // 如果启用了填充地表洞穴选项，且在表层范围内
        if (EpicEngineWorldConfig.PLAINS_FILL_SURFACE_CAVES.get() &&
            y <= baseHeight &&
            y > baseHeight - surfaceDepth) {

            // 将任何空洞(负值)转换为实体方块(正值)
            if (v < 0) {
                // 使用正值，确保生成固体方块
                return 0.2; // 使用小正值，让其他生成系统有空间工作
            }
        }

        // 地下部分保持原样
        return v;
    }

    /**
     * 获取山地接近度因子 (0-1)
     */
    private static double getMountainProximityFactor(int x, int z) {
        double mountainValue = getMountainRangeControl(x, z);
        double proximity = Math.max(0, 1.0 - Math.abs(mountainValue - 0.4) * 2.5);
        return Math.pow(proximity, 1.5);
    }

    /**
     * 检查是否为平原生物群系
     */
    private static boolean isPlainsBiome(String biomeId) {
        return PLAINS_BIOMES.contains(biomeId);
    }

    /**
     * 检查是否为草甸生物群系
     */
    private static boolean isMeadowBiome(String biomeId) {
        return MEADOW_ID.equals(biomeId);
    }

    /**
     * 山脉地形增强的核心逻辑
     */
    private static double enhanceTerrain(double v, int x, int y, int z) {
        // 获取当前生物群系
        Holder<Biome> currentBiome = BiomeContext.CURRENT.get();
        boolean isJaggedPeaks = false;
        String biomeId = "";

        // 处理当前生物群系
        if (currentBiome != null && currentBiome.unwrapKey().isPresent()) {
            biomeId = currentBiome.unwrapKey().get().location().toString();

            // 检查是否为平原或草甸，如果是则使用切割平坦化逻辑
            if (isPlainsBiome(biomeId) || isMeadowBiome(biomeId)) {
                return cutFlattenPlainsTerrain(v, x, y, z, biomeId);
            }

            // 检查是否为山地生物群系
            if (currentBiome.is(MOUNTAIN_TAG)) {
                // 检查是否为尖峭山峰
                isJaggedPeaks = JAGGED_PEAKS_ID.equals(biomeId);

                // 最终应用的因子
                double factor = 1.0;

                // 1. 基础地形小幅提升 - 仅限低地
                if (y < EpicEngineWorldConfig.BASE_TERRAIN_HEIGHT_LIMIT.get()) {
                    factor = EpicEngineWorldConfig.BASE_TERRAIN_FACTOR.get();
                }

                // 获取大尺度山脉控制值 - 决定连绵山脉的位置和形态
                double rangeControl = getMountainRangeControl(x, z);

                // 计算山脊控制 - 创造连续的山脊线
                double ridgeControl = getRidgeControl(x, z);

                // 调整山脉连续性阈值
                double continuityThreshold = 0.4 * (1.0 - EpicEngineWorldConfig.MOUNTAIN_CONTINUITY.get());

                // 山脉主体区域 - 完全增强
                if (rangeControl > continuityThreshold) {
                    // 计算主山脉区域的增强强度
                    double rangeStrength = Math.min(1.0, (rangeControl - continuityThreshold) / (1.0 - continuityThreshold));

                    // 2. 山地分层增强 - 不使用柏林噪声，直接应用固定因子
                    double mountainFactor;
                    if (y < EpicEngineWorldConfig.MOUNTAIN_LOW_THRESHOLD.get()) {
                        // 低山区域
                        mountainFactor = EpicEngineWorldConfig.MOUNTAIN_LOW_FACTOR.get();
                    } else if (y < EpicEngineWorldConfig.MOUNTAIN_MID_THRESHOLD.get()) {
                        // 中山区域
                        mountainFactor = EpicEngineWorldConfig.MOUNTAIN_MID_FACTOR.get();
                    } else {
                        // 高山区域
                        mountainFactor = EpicEngineWorldConfig.MOUNTAIN_HIGH_FACTOR.get();

                        // 随高度线性增强而不是使用噪声
                        double heightScale = (y - EpicEngineWorldConfig.MOUNTAIN_MID_THRESHOLD.get()) / 128.0;
                        mountainFactor *= (1.0 + Math.min(0.5, heightScale * 0.3));
                    }

                    // 应用山脊增强，创造连绵山脊
                    double ridgeThreshold = 0.65;
                    if (ridgeControl > ridgeThreshold && y > EpicEngineWorldConfig.MOUNTAIN_LOW_THRESHOLD.get()) {
                        double ridgeStrength = (ridgeControl - ridgeThreshold) / (1.0 - ridgeThreshold);
                        double ridgeBoost = 1.0 + ridgeStrength * EpicEngineWorldConfig.RIDGE_STRENGTH.get();
                        mountainFactor *= ridgeBoost;
                    }

                    // 3. 尖峭山峰特殊处理 - 直接使用配置的固定增强因子
                    if (isJaggedPeaks) {
                        mountainFactor *= EpicEngineWorldConfig.JAGGED_PEAKS_EXTRA_FACTOR.get();

                        // 高海拔额外增强 - 使用平滑的高度过渡而不是噪声
                        if (y > EpicEngineWorldConfig.MOUNTAIN_MID_THRESHOLD.get()) {
                            double heightAboveThreshold = y - EpicEngineWorldConfig.MOUNTAIN_MID_THRESHOLD.get();
                            double maxMountainHeight = EpicEngineWorldConfig.MAX_MOUNTAIN_HEIGHT.get();
                            double progress = Math.min(1.0, heightAboveThreshold / (maxMountainHeight * 0.7));
                            double boostFactor = 1.0 + (progress * 0.8);
                            mountainFactor *= boostFactor;
                        }

                        // 在山脊附近创造尖锐的峰顶
                        if (ridgeControl > 0.8 && y > EpicEngineWorldConfig.MOUNTAIN_MID_THRESHOLD.get()) {
                            mountainFactor *= 1.2; // 额外增强20%
                        }
                    }

                    // 应用山脉主体强度
                    mountainFactor = factor + (mountainFactor - factor) * rangeStrength;

                    // 使用山地因子替换基础因子
                    factor = mountainFactor;
                } else {
                    // 山脉过渡区域 - 创造山脉边缘的平滑过渡
                    double transitionFactor = 1.0 + (EpicEngineWorldConfig.MOUNTAIN_LOW_FACTOR.get() - 1.0)
                                                * (rangeControl / continuityThreshold);
                    factor = Math.max(factor, transitionFactor);
                }

                // 最大可达比率计算
                double maxRatio = (double) EpicEngineWorldConfig.MAX_MOUNTAIN_HEIGHT.get()
                                / EpicEngineWorldConfig.calculateMaxBuildHeight();

                // 应用增强因子并限制在合理范围内
                return Mth.clamp(v * factor, -2.0 * maxRatio, 2.0 * maxRatio);
            }
        }

        // 如果不是特殊处理的生物群系，返回原值
        return v;
    }

    // ——— 以下为必须的样板代码 ———
    @Override
    public double minValue() {
        return original.minValue();
    }

    @Override
    public double maxValue() {
        return original.maxValue();
    }

    @Override
    public @NotNull DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new EnhancedMountainDensityFunction(original.mapAll(visitor)));
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KeyDispatchDataCodec.of(MapCodec.unit(this));
    }
}