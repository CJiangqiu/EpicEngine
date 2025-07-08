package com.epic_engine.custom;

import com.epic_engine.config.EpicEngineCustomConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 标题设置Mixin
 * 拦截设置标题的方法，应用自定义标题
 */
@Mixin(Minecraft.class)
public class TitleMixin {

    /**
     * 拦截updateTitle方法
     */
    @Inject(
        method = "updateTitle",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUpdateTitle(CallbackInfo ci) {
        if (epicEngine$shouldUseCustomTitle()) {
            epicEngine$applyOurCustomTitle();
            ci.cancel();
        }
    }

    /**
     * 拦截Minecraft构造方法
     */
    @Inject(
        method = "<init>*",
        at = @At("RETURN")
    )
    private void onConstructed(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft)(Object)this;
        minecraft.tell(this::epicEngine$applyOurCustomTitle);
    }

    /**
     * 检查是否应该使用自定义标题
     */
    @Unique
    private boolean epicEngine$shouldUseCustomTitle() {
        try {
            return EpicEngineCustomConfig.ENABLE_CUSTOMIZATION.get() &&
                   EpicEngineCustomConfig.ENABLE_CUSTOM_WINDOW_TITLE.get();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 应用自定义标题
     */
    @Unique
    private void epicEngine$applyOurCustomTitle() {
        try {
            Minecraft minecraft = (Minecraft)(Object)this;

            if (minecraft.getWindow() != null && epicEngine$shouldUseCustomTitle()) {
                String customTitle = epicEngine$getCustomTitle();

                if (customTitle != null && !customTitle.isEmpty()) {
                    minecraft.getWindow().setTitle(customTitle);
                }
            }
        } catch (Exception e) {
            // 静默处理异常
        }
    }

    /**
     * 获取自定义标题
     */
    @Unique
    private String epicEngine$getCustomTitle() {
        return EpicEngineCustomConfig.CUSTOM_WINDOW_TITLE.get();
    }
}