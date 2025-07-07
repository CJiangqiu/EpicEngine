package com.epic_engine.mixin;

import com.epic_engine.config.EpicEngineWorldConfig;
import com.epic_engine.world.EnhancedMountainDensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 增强山脉生成的Mixin：
 *  - 包裹 initialDensityWithoutJaggedness，使地表高度能被拉升到配置上限
 *  - 包裹 finalDensity，保留锯齿状细节
 *  - 包裹 depth 和 ridges，增强山脉的形态
 */
@Mixin(NoiseRouter.class)
public class NoiseRouterMixin {

    /**
     * 包裹初始密度函数（无锯齿版本），以便让地表高度应用我们的增强逻辑
     */
    @Inject(
            method = "initialDensityWithoutJaggedness",
            at = @At("RETURN"),
            cancellable = true
    )
    private void wrapInitialDensity(CallbackInfoReturnable<DensityFunction> cir) {
        if (!EpicEngineWorldConfig.ENABLE_WORLD_MODIFICATIONS.get()) {
            return;
        }
        DensityFunction original = cir.getReturnValue();
        cir.setReturnValue(new EnhancedMountainDensityFunction(original));
    }

    /**
     * 包裹最终密度函数（带锯齿版本），继续在其上施加增强
     */
    @Inject(
            method = "finalDensity",
            at = @At("RETURN"),
            cancellable = true
    )
    private void wrapFinalDensity(CallbackInfoReturnable<DensityFunction> cir) {
        if (!EpicEngineWorldConfig.ENABLE_WORLD_MODIFICATIONS.get()) {
            return;
        }
        DensityFunction original = cir.getReturnValue();
        cir.setReturnValue(new EnhancedMountainDensityFunction(original));
    }

    /**
     * 增强深度函数，影响地形的整体高度变化
     */
    @Inject(method = "depth", at = @At("RETURN"), cancellable = true)
    private void wrapDepth(CallbackInfoReturnable<DensityFunction> cir) {
        if (!EpicEngineWorldConfig.ENABLE_WORLD_MODIFICATIONS.get()) return;
        cir.setReturnValue(new EnhancedMountainDensityFunction(cir.getReturnValue()));
    }

    /**
     * 增强山脊函数，使山脉更加陡峭和戏剧性
     */
    @Inject(method = "ridges", at = @At("RETURN"), cancellable = true)
    private void wrapRidges(CallbackInfoReturnable<DensityFunction> cir) {
        if (!EpicEngineWorldConfig.ENABLE_WORLD_MODIFICATIONS.get()) return;
        cir.setReturnValue(new EnhancedMountainDensityFunction(cir.getReturnValue()));
    }
}