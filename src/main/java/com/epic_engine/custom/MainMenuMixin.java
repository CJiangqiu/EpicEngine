package com.epic_engine.custom;

import com.epic_engine.config.EpicEngineCustomConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 只负责背景与标题的替换
 */
@Mixin(TitleScreen.class)
public class MainMenuMixin {

    /* -------------------- 日志与状态 -------------------- */
    @Unique private static final Logger epicEngine$LOGGER = LogManager.getLogger();

    @Unique private ResourceLocation epicEngine$customBackground = null;
    @Unique private ResourceLocation epicEngine$customTitle      = null;

    @Unique private boolean epicEngine$customBackgroundLoaded = false;
    @Unique private boolean epicEngine$customTitleLoaded      = false;
    @Unique private boolean epicEngine$initializeAttempted    = false;

    /* 标题尺寸 */
    @Unique private int epicEngine$titleWidth  = 0;
    @Unique private int epicEngine$titleHeight = 0;

    /* -------------------- 初始化：加载资源 -------------------- */
    @Inject(method = "init", at = @At("TAIL"))
    private void epicEngine$onInit(CallbackInfo ci) {
        if (epicEngine$initializeAttempted) return;
        epicEngine$initializeAttempted = true;

        if (!epicEngine$shouldUseCustomMainMenu()) return;

        EpicEngineCustomConfig.initializeResources();
        epicEngine$loadCustomResources();
    }

    /* -------------------- 接管 render -------------------- */
    @Inject(method = "render",
            at     = @At("HEAD"),
            cancellable = true)
    private void epicEngine$onRenderStart(GuiGraphics guiGraphics,
                                          int mouseX, int mouseY, float partialTicks,
                                          CallbackInfo ci) {

        /* 若未启用或资源未加载成功 → 走原版逻辑 */
        if (!epicEngine$shouldUseCustomMainMenu() ||
                (!epicEngine$customBackgroundLoaded && !epicEngine$customTitleLoaded)) {
            return;
        }

        /* 1. 背景 */
        if (epicEngine$customBackgroundLoaded) {
            epicEngine$renderCustomBackground(guiGraphics);
        } else {                                   // 后备渐变
            epicEngine$renderOriginalBackground(guiGraphics);
        }

        /* 2. 原版 UI & 文字（全部渲染，包含 Mojang/版权行） */
        TitleScreen self = (TitleScreen)(Object)this;
        self.renderables.forEach(r -> r.render(guiGraphics, mouseX, mouseY, partialTicks));

        /* 3. 自定义标题 */
        if (epicEngine$customTitleLoaded) {
            epicEngine$renderCustomTitle(guiGraphics);
        }

        /* ——全部完成，阻止原版 render—— */
        ci.cancel();
    }

    /* -------------------- 自定义背景 -------------------- */
    @Unique
    private void epicEngine$renderCustomBackground(GuiGraphics guiGraphics) {
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        guiGraphics.blit(epicEngine$customBackground,
                0, 0, sw, sh,      // 目标矩形
                0F, 0F, 1024, 1024,// 纹理采样
                1024, 1024);       // 纹理尺寸
    }

    /* 后备渐变背景（若自定义失败） */
    @Unique
    private void epicEngine$renderOriginalBackground(GuiGraphics guiGraphics) {
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        guiGraphics.fillGradient(0, 0, sw, sh, 0xFF0F0F23, 0xFF0F0F23);
    }

    /* -------------------- 自定义标题 -------------------- */
    @Unique
    private void epicEngine$renderCustomTitle(GuiGraphics guiGraphics) {
        try {
            int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();

            int ow = Math.max(1, epicEngine$titleWidth);
            int oh = Math.max(1, epicEngine$titleHeight);

            float scale = Math.min((sw * 0.8F) / ow, (sh * 0.15F) / oh);
            scale = Math.max(scale, 0.5F);

            int drawW = (int)(ow * scale);
            int drawH = (int)(oh * scale);
            int x = (sw - drawW) / 2;
            int y = 30;

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(scale, scale, 1F);
            guiGraphics.blit(epicEngine$customTitle,
                    0, 0,
                    0, 0,
                    ow, oh,
                    ow, oh);
            guiGraphics.pose().popPose();

        } catch (Exception e) {
            epicEngine$LOGGER.error("Error rendering custom title", e);
        }
    }

    /* -------------------- 资源加载 -------------------- */
    @Unique
    private void epicEngine$loadCustomResources() {
        try {
            File cfgDir = new File(FMLPaths.CONFIGDIR.get().toFile(), "epic_engine");
            if (!cfgDir.exists()) cfgDir.mkdirs();

            /* 背景 */
            File bgFile = EpicEngineCustomConfig.getTextureFile(
                    EpicEngineCustomConfig.MAIN_MENU_BACKGROUND.get());
            if (EpicEngineCustomConfig.fileExists(bgFile)) {
                NativeImage img = epicEngine$readImage(bgFile);
                if (img != null) {
                    epicEngine$customBackground =
                            new ResourceLocation("epic_engine", "custom_background");
                    Minecraft.getInstance().getTextureManager()
                            .register(epicEngine$customBackground, new DynamicTexture(img));
                    epicEngine$customBackgroundLoaded = true;
                }
            }

            /* 标题 */
            File titleFile = EpicEngineCustomConfig.getTextureFile(
                    EpicEngineCustomConfig.MAIN_MENU_TITLE_IMAGE.get());
            if (EpicEngineCustomConfig.fileExists(titleFile)) {
                NativeImage img = epicEngine$readImage(titleFile);
                if (img != null) {
                    epicEngine$titleWidth  = img.getWidth();
                    epicEngine$titleHeight = img.getHeight();
                    epicEngine$customTitle =
                            new ResourceLocation("epic_engine", "custom_title");
                    Minecraft.getInstance().getTextureManager()
                            .register(epicEngine$customTitle, new DynamicTexture(img));
                    epicEngine$customTitleLoaded = true;
                }
            }

        } catch (Exception e) {
            epicEngine$LOGGER.error("Error loading custom resources", e);
        }
    }

    @Unique
    private NativeImage epicEngine$readImage(File file) {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return NativeImage.read(in);
        } catch (IOException e) {
            epicEngine$LOGGER.error("Failed to read image: {}", file.getPath(), e);
            return null;
        }
    }

    /* -------------------- 配置开关 -------------------- */
    @Unique
    private boolean epicEngine$shouldUseCustomMainMenu() {
        return EpicEngineCustomConfig.ENABLE_CUSTOMIZATION.get()
                && EpicEngineCustomConfig.ENABLE_CUSTOM_MAIN_MENU.get();
    }
}
