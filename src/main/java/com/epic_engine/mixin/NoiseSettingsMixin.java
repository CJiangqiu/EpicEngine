package com.epic_engine.mixin;

import com.epic_engine.config.EpicEngineWorldConfig;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.NoiseSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 确保 NoiseSettings.clampToHeightAccessor 返回值使用我们修改后的 minY 和 height，
 * 真正解锁噪声生成高度范围。
 */
@Mixin(NoiseSettings.class)
public class NoiseSettingsMixin {
    @Inject(
        method = "clampToHeightAccessor",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onClampToHeightAccessor(LevelHeightAccessor heightAccessor, CallbackInfoReturnable<NoiseSettings> cir) {
        // 如果总开关关闭，就不做任何改动
        if (!EpicEngineWorldConfig.ENABLE_WORLD_MODIFICATIONS.get()) {
            return;
        }

        // 原始返回的 NoiseSettings
        NoiseSettings orig = cir.getReturnValue();

        // 计算我们想要的 minY 和 height
        int newMinY = EpicEngineWorldConfig.MIN_BUILD_HEIGHT.get();
        int newHeight = EpicEngineWorldConfig.MAX_MOUNTAIN_HEIGHT.get()
                      - EpicEngineWorldConfig.MIN_BUILD_HEIGHT.get()
                      + 64; // 保留额外空间

        // 复用原来的 noiseSizeHorizontal 和 noiseSizeVertical
        NoiseSettings tweaked = new NoiseSettings(
            newMinY,
            newHeight,
            orig.noiseSizeHorizontal(),
            orig.noiseSizeVertical()
        );

        // 替换返回值
        cir.setReturnValue(tweaked);
    }
}
