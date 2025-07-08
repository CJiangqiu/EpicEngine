package com.epic_engine.custom;

import com.epic_engine.config.EpicEngineCustomConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;    // ← 正确类
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * 进/出维度加载界面：替换背景 + 底部提示
 */
@Mixin(LevelLoadingScreen.class)
public class LevelLoadingScreenMixin {

    /* ---------- 日志 & 状态 ---------- */
    @Unique private static final Logger LOGGER = LogManager.getLogger();
    @Unique private ResourceLocation bgId = null;
    @Unique private boolean bgReady = false;

    /* ---------- 提示文字 ---------- */
    @Unique private String tip = "";
    @Unique private long   lastSwap = 0;
    @Unique private static final long INTERVAL = 3_000L;

    /* =====================================================
     * 1. 构造函数尾：加载自定义背景 & 选第一条 Tip
     * ===================================================== */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (!enabled()) return;

        EpicEngineCustomConfig.initializeResources();
        loadBackground();
        pickTip();
    }

    /* =====================================================
     * 2. 原版 render 结束后，把背景和 Tip 画到最上层
     * ===================================================== */
    @Inject(method = "render", at = @At("TAIL"))
    private void afterRender(GuiGraphics gg, int mx, int my, float pt, CallbackInfo ci) {
        if (!enabled()) return;

        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        /* 2-1 背景 */
        if (bgReady) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

            gg.blit(bgId, 0, 0, sw, sh,
                    0F, 0F, 1024, 1024,
                    1024, 1024);
        }

        /* 2-2 底部 Tip */
        maybeSwapTip();
        if (!tip.isEmpty()) {
            int color = parseColor(EpicEngineCustomConfig.LOADING_TEXT_COLOR.get());
            int w = Minecraft.getInstance().font.width(tip);
            int x = (sw - w) / 2;
            int y = sh - 28;

            gg.drawString(Minecraft.getInstance().font, tip, x + 1, y + 1, 0xFF000000); // 阴影
            gg.drawString(Minecraft.getInstance().font, tip, x,     y,     color);
        }
    }

    /* =====================================================
     * 3. 工具方法
     * ===================================================== */
    @Unique
    private boolean enabled() {
        return EpicEngineCustomConfig.ENABLE_CUSTOMIZATION.get() &&
                EpicEngineCustomConfig.ENABLE_CUSTOM_LOADING_SCREEN.get();
    }

    /* ---- 背景 ---- */
    @Unique
    private void loadBackground() {
        try {
            File file = EpicEngineCustomConfig.getTextureFile(
                    EpicEngineCustomConfig.LOADING_SCREEN_BACKGROUND.get());

            if (!EpicEngineCustomConfig.fileExists(file)) return;

            try (InputStream in = Files.newInputStream(file.toPath())) {
                NativeImage img = NativeImage.read(in);
                bgId = new ResourceLocation("epic_engine", "level_loading_bg");
                Minecraft.getInstance().getTextureManager()
                        .register(bgId, new DynamicTexture(img));
                bgReady = true;
                LOGGER.info("Custom LevelLoading background loaded");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load LevelLoading background", e);
        }
    }

    /* ---- 提示 ---- */
    @Unique
    private void maybeSwapTip() {
        long now = System.currentTimeMillis();
        if (now - lastSwap < INTERVAL) return;
        pickTip();
        lastSwap = now;
    }

    @Unique
    private void pickTip() {
        List<? extends String> list = EpicEngineCustomConfig.LOADING_TIPS.get();
        tip = list.isEmpty() ? "" :
                list.get(RandomSource.create().nextInt(list.size()));
    }

    /* ---- #RRGGBB / RRGGBBAA 转 int ---- */
    @Unique
    private int parseColor(String c) {
        if (c == null) return 0xFFFFFFFF;
        if (c.startsWith("#")) c = c.substring(1);
        try {
            if (c.length() == 6) return (int)Long.parseLong("FF" + c, 16);
            if (c.length() == 8) return (int)Long.parseLong(c, 16);
        } catch (NumberFormatException ignored) {}
        return 0xFFFFFFFF;
    }
}
