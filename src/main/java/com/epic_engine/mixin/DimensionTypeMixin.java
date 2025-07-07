package com.epic_engine.mixin;

import com.epic_engine.config.EpicEngineWorldConfig;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修改维度类型参数 - 控制世界高度和建筑限制
 */
@Mixin(DimensionType.class)
public class DimensionTypeMixin {

    /**
     * 修改世界总高度 - 这会影响地形生成和建筑上限
     */
    @Inject(
        method = "height",
        at = @At("RETURN"),
        cancellable = true
    )
    private void modifyWorldHeight(CallbackInfoReturnable<Integer> cir) {
        if (!EpicEngineWorldConfig.ENABLE_WORLD_MODIFICATIONS.get()) {
            return;
        }

        // 计算所需世界高度，确保足够容纳高山
        int minHeight = EpicEngineWorldConfig.MIN_BUILD_HEIGHT.get();
        int maxMountainHeight = EpicEngineWorldConfig.MAX_MOUNTAIN_HEIGHT.get();

        // 计算所需总高度 = 最大山脉高度 - 最小高度 + 额外空间
        int requiredHeight = maxMountainHeight - minHeight + 64; // 额外空间

        // 设置世界总高度
        cir.setReturnValue(requiredHeight);
    }

    /**
     * 修改世界最小高度 - 这会影响建筑下限和地形生成的起点
     */
    @Inject(
        method = "minY",
        at = @At("RETURN"),
        cancellable = true
    )
    private void modifyMinWorldHeight(CallbackInfoReturnable<Integer> cir) {
        if (!EpicEngineWorldConfig.ENABLE_WORLD_MODIFICATIONS.get()) {
            return;
        }

        // 设置世界最小高度
        cir.setReturnValue(EpicEngineWorldConfig.MIN_BUILD_HEIGHT.get());
    }

    /**
     * 修改逻辑高度 - 这会影响一些游戏机制，如重力、云层高度等
     */
    @Inject(
        method = "logicalHeight",
        at = @At("RETURN"),
        cancellable = true
    )
    private void modifyLogicalHeight(CallbackInfoReturnable<Integer> cir) {
        if (!EpicEngineWorldConfig.ENABLE_WORLD_MODIFICATIONS.get()) {
            return;
        }

        // 设置逻辑高度 - 通常是世界高度的一部分，但不要太高以避免问题
        int minHeight = EpicEngineWorldConfig.MIN_BUILD_HEIGHT.get();
        int maxMountainHeight = EpicEngineWorldConfig.MAX_MOUNTAIN_HEIGHT.get();

        // 逻辑高度 = 山脉高度 - 最小高度 + 一些额外空间
        int logicalHeight = Math.min(
            maxMountainHeight - minHeight + 32,
            384 // 一个安全的上限值，对应原版的逻辑高度
        );

        cir.setReturnValue(logicalHeight);
    }
}