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
 * 主界面自定义Mixin
 * 用于替换主界面背景和标题图像
 */
@Mixin(TitleScreen.class)
public class MainMenuMixin {
    @Unique
    private static final Logger eJRA$LOGGER = LogManager.getLogger();

    // 自定义资源
    @Unique
    private ResourceLocation eJRA$customBackground = null;
    @Unique
    private ResourceLocation eJRA$customTitle = null;
    @Unique
    private boolean eJRA$customBackgroundLoaded = false;
    @Unique
    private boolean eJRA$customTitleLoaded = false;
    @Unique
    private boolean eJRA$initializeAttempted = false;

    // 图像尺寸信息
    @Unique
    private int eJRA$titleWidth = 0;
    @Unique
    private int eJRA$titleHeight = 0;

    /**
     * 在初始化主界面时加载自定义资源
     */
    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        if (!eJRA$initializeAttempted) {
            eJRA$initializeAttempted = true;
            if (eJRA$shouldUseCustomMainMenu()) {
                // 确保配置目录和默认资源已初始化
                EpicEngineCustomConfig.initializeResources();
                // 加载自定义资源
                eJRA$loadCustomResources();
            }
        }
    }

    /**
     * 在渲染前检查是否使用自定义背景
     * 注意: Minecraft 1.20.1 使用GuiGraphics而不是PoseStack
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (eJRA$shouldUseCustomMainMenu() && eJRA$customBackgroundLoaded) {
            eJRA$renderCustomBackground(guiGraphics);
        }
    }

    /**
     * 在正常渲染完成后渲染自定义标题
     */
    @Inject(method = "render", at = @At("RETURN"))
    private void afterRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (eJRA$shouldUseCustomMainMenu() && eJRA$customTitleLoaded) {
            eJRA$renderCustomTitle(guiGraphics);
        }
    }

    /**
     * 加载自定义资源
     */
    @Unique
    private void eJRA$loadCustomResources() {
        try {
            // 检查配置目录是否存在
            File configDir = new File(FMLPaths.CONFIGDIR.get().toFile(), "epic_engine");
            if (!configDir.exists()) {
                eJRA$LOGGER.info("EJRA config directory not found, attempting to create it");
                configDir.mkdirs();
                // 初始化默认资源
                EpicEngineCustomConfig.initializeResources();
            }

            // 加载背景
            File bgFile = EpicEngineCustomConfig.getTextureFile(
                    EpicEngineCustomConfig.MAIN_MENU_BACKGROUND.get());

            if (EpicEngineCustomConfig.fileExists(bgFile)) {
                try {
                    // 加载为NativeImage
                    NativeImage nativeImage = eJRA$loadImage(bgFile);
                    if (nativeImage != null) {
                        DynamicTexture texture = new DynamicTexture(nativeImage);
                        eJRA$customBackground = Minecraft.getInstance().getTextureManager()
                                .register("epic_engine:custom_background", texture);
                        eJRA$customBackgroundLoaded = true;
                        eJRA$LOGGER.info("Custom main menu background loaded from: {}", bgFile.getPath());
                    }
                } catch (Exception e) {
                    eJRA$LOGGER.error("Failed to load custom background: {}", bgFile.getPath(), e);
                }
            } else {
                eJRA$LOGGER.info("Custom background file not found: {}", bgFile.getPath());
            }

            // 加载标题
            File titleFile = EpicEngineCustomConfig.getTextureFile(
                    EpicEngineCustomConfig.MAIN_MENU_TITLE_IMAGE.get());

            if (EpicEngineCustomConfig.fileExists(titleFile)) {
                try {
                    // 加载为NativeImage
                    NativeImage nativeImage = eJRA$loadImage(titleFile);
                    if (nativeImage != null) {
                        // 保存标题尺寸
                        eJRA$titleWidth = nativeImage.getWidth();
                        eJRA$titleHeight = nativeImage.getHeight();

                        DynamicTexture texture = new DynamicTexture(nativeImage);
                        eJRA$customTitle = Minecraft.getInstance().getTextureManager()
                                .register("epic_engine:custom_title", texture);
                        eJRA$customTitleLoaded = true;
                        eJRA$LOGGER.info("Custom main menu title loaded: {}x{} from: {}",
                               eJRA$titleWidth, eJRA$titleHeight, titleFile.getPath());
                    }
                } catch (Exception e) {
                    eJRA$LOGGER.error("Failed to load custom title: {}", titleFile.getPath(), e);
                }
            } else {
                eJRA$LOGGER.info("Custom title file not found: {}", titleFile.getPath());
            }
        } catch (Exception e) {
            eJRA$LOGGER.error("Error during custom resource loading", e);
        }
    }

    /**
     * 将文件加载为NativeImage
     * 返回null而不是抛出异常，以增强容错性
     */
    @Unique
    private NativeImage eJRA$loadImage(File file) {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return NativeImage.read(inputStream);
        } catch (IOException e) {
            eJRA$LOGGER.error("Error loading image: {}", file.getPath(), e);
            return null;
        }
    }

    /**
     * 渲染自定义背景
     */
    @Unique
    private void eJRA$renderCustomBackground(GuiGraphics guiGraphics) {
        try {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // 绘制全屏图像，适应当前窗口尺寸
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();

            // 1.20.1使用GuiGraphics.blit
            guiGraphics.blit(eJRA$customBackground, 0, 0, 0, 0, width, height);
        } catch (Exception e) {
            eJRA$LOGGER.error("Error rendering custom background", e);
        }
    }

    /**
     * 渲染自定义标题
     */
    @Unique
    private void eJRA$renderCustomTitle(GuiGraphics guiGraphics) {
        try {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // 使用已保存的标题尺寸
            int titleWidth = eJRA$titleWidth;
            int titleHeight = eJRA$titleHeight;

            // 如果未获取到尺寸，使用默认值
            if (titleWidth <= 0 || titleHeight <= 0) {
                titleWidth = 1024;  // 原版标题默认尺寸
                titleHeight = 256;
            }

            // 计算居中位置
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int x = (width - titleWidth) / 2;
            int y = 30; // 标题Y位置

            // 绘制标题
            guiGraphics.blit(eJRA$customTitle, x, y, 0, 0, titleWidth, titleHeight);
        } catch (Exception e) {
            eJRA$LOGGER.error("Error rendering custom title", e);
        }
    }

    /**
     * 检查是否应该使用自定义主界面
     */
    @Unique
    private boolean eJRA$shouldUseCustomMainMenu() {
        try {
            return EpicEngineCustomConfig.ENABLE_CUSTOMIZATION.get() &&
                    EpicEngineCustomConfig.ENABLE_CUSTOM_MAIN_MENU.get();
        } catch (Exception e) {
            eJRA$LOGGER.error("Error checking main menu config", e);
            return false;
        }
    }
}