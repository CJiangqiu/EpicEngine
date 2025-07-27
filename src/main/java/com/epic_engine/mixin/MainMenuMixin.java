package com.epic_engine.mixin;

import com.epic_engine.custom.UnifiedLayoutEditor;
import com.epic_engine.custom.CustomButtonManager;
import com.epic_engine.custom.EpicEngineI18n;
import com.epic_engine.custom.MainMenuLayoutData;
import com.epic_engine.config.EpicEngineCustomConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Mixin(TitleScreen.class)
public class MainMenuMixin {

    @Unique private static final Logger epicEngine$LOGGER = LogManager.getLogger();

    @Unique private static ResourceLocation epicEngine$customBackground = null;
    @Unique private static ResourceLocation epicEngine$customTitle = null;
    @Unique private static boolean epicEngine$customBackgroundLoaded = false;
    @Unique private static boolean epicEngine$customTitleLoaded = false;
    @Unique private static boolean epicEngine$resourcesProcessed = false;
    @Unique private static int epicEngine$titleWidth = 0;
    @Unique private static int epicEngine$titleHeight = 0;

    @Unique private static boolean epicEngine$configCached = false;
    @Unique private static boolean epicEngine$shouldCustomize = false;
    @Unique private static boolean epicEngine$backgroundEnabled = false;
    @Unique private static boolean epicEngine$titleEnabled = false;
    @Unique private static boolean epicEngine$buttonsEnabled = false;

    @Inject(method = "init", at = @At("HEAD"))
    private void epicEngine$onInitStart(CallbackInfo ci) {
        CustomButtonManager.reset();
        epicEngine$LOGGER.info("[EPIC ENGINE]: MainMenu init started, reset custom button system");
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void epicEngine$onInit(CallbackInfo ci) {
        epicEngine$LOGGER.info("[EPIC ENGINE]: MainMenu init completed, starting customization");

        TitleScreen titleScreen = (TitleScreen)(Object)this;

        epicEngine$cacheConfig();

        if (!epicEngine$resourcesProcessed) {
            EpicEngineCustomConfig.initializeResources();
            epicEngine$loadCustomResources();
            epicEngine$resourcesProcessed = true;
            epicEngine$LOGGER.info("[EPIC ENGINE]: Resources processed and cached");
        }

        MainMenuLayoutData layoutData = UnifiedLayoutEditor.getCurrentMainMenuLayout();
        if (layoutData == null) {
            layoutData = MainMenuLayoutData.load(EpicEngineCustomConfig.getLayoutFile());
        }

        layoutData.updateScreenResolution(titleScreen.width, titleScreen.height);

        if (epicEngine$buttonsEnabled) {
            CustomButtonManager.initialize(titleScreen, layoutData);
            epicEngine$LOGGER.info("[EPIC ENGINE]: Custom button system activated - scanned and replaced vanilla buttons");
        }

        UnifiedLayoutEditor.init(titleScreen.width, titleScreen.height);

        epicEngine$LOGGER.info("[EPIC ENGINE]: MainMenu customization complete - Background: {}, Title: {}, Buttons: {}",
                epicEngine$backgroundEnabled, epicEngine$titleEnabled, epicEngine$buttonsEnabled);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void epicEngine$onRenderStart(GuiGraphics guiGraphics,
                                          int mouseX, int mouseY, float partialTicks,
                                          CallbackInfo ci) {

        TitleScreen titleScreen = (TitleScreen)(Object)this;
        MainMenuLayoutData layoutData = UnifiedLayoutEditor.getCurrentMainMenuLayout();

        // 如果是加载界面编辑模式，渲染加载界面编辑器并取消主界面渲染
        if (UnifiedLayoutEditor.isEditMode() && UnifiedLayoutEditor.isLoadingScreenPage()) {
            // 渲染一个简单的背景，避免完全空白
            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();
            guiGraphics.fillGradient(0, 0, screenWidth, screenHeight, 0xFF1a1a2e, 0xFF16213e);

            // 渲染加载界面编辑器
            UnifiedLayoutEditor.render(guiGraphics, mouseX, mouseY, partialTicks);
            ci.cancel();
            return;
        }

        if ((epicEngine$shouldCustomize || UnifiedLayoutEditor.isEditMode()) &&
                UnifiedLayoutEditor.isMainMenuPage()) {
            epicEngine$renderCustomizedInterface(guiGraphics, mouseX, mouseY, partialTicks, layoutData, titleScreen);
            ci.cancel();
            return;
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void epicEngine$onRenderEnd(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // 只有在非编辑模式下才渲染
        if (!UnifiedLayoutEditor.isEditMode()) {
            UnifiedLayoutEditor.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        // 编辑模式下的渲染已经在 onRenderStart 中处理了
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void epicEngine$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (CustomButtonManager.handleButtonClick((int)mouseX, (int)mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }

        if (UnifiedLayoutEditor.handleMouseClick((int)mouseX, (int)mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }

        if (UnifiedLayoutEditor.isEditMode()) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void epicEngine$renderCustomizedInterface(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks,
                                                      MainMenuLayoutData layoutData, TitleScreen titleScreen) {

        if (epicEngine$backgroundEnabled && epicEngine$customBackgroundLoaded) {
            epicEngine$renderCustomBackground(guiGraphics);
        } else {
            epicEngine$renderOriginalBackground(guiGraphics, partialTicks);
        }

        if (epicEngine$titleEnabled && epicEngine$customTitleLoaded && layoutData != null) {
            epicEngine$renderCustomTitleWithLayout(guiGraphics, layoutData.title_image);
        }

        if (UnifiedLayoutEditor.isMainMenuPage() || !UnifiedLayoutEditor.isEditMode()) {
            titleScreen.renderables.forEach(
                    r -> r.render(guiGraphics, mouseX, mouseY, partialTicks)
            );
        }

        if (layoutData != null) {
            epicEngine$renderCustomTexts(guiGraphics, layoutData);
        }

        UnifiedLayoutEditor.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Unique
    private void epicEngine$cacheConfig() {
        if (!epicEngine$configCached) {
            epicEngine$shouldCustomize = EpicEngineCustomConfig.ENABLE_CUSTOMIZATION.get()
                    && EpicEngineCustomConfig.MAIN_MENU_MODULE_ENABLED.get();

            if (epicEngine$shouldCustomize) {
                epicEngine$backgroundEnabled = EpicEngineCustomConfig.MAIN_MENU_BACKGROUND_ENABLED.get();
                epicEngine$titleEnabled = EpicEngineCustomConfig.MAIN_MENU_TITLE_ENABLED.get();
                epicEngine$buttonsEnabled = EpicEngineCustomConfig.MAIN_MENU_BUTTONS_ENABLED.get();

                epicEngine$shouldCustomize = epicEngine$backgroundEnabled || epicEngine$titleEnabled || epicEngine$buttonsEnabled;
            }

            epicEngine$configCached = true;
            epicEngine$LOGGER.info("[EPIC ENGINE]: Configuration cached - customize: {}, bg: {}, title: {}, buttons: {}",
                    epicEngine$shouldCustomize, epicEngine$backgroundEnabled, epicEngine$titleEnabled, epicEngine$buttonsEnabled);
        }
    }

    @Unique
    private void epicEngine$renderCustomBackground(GuiGraphics guiGraphics) {
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        guiGraphics.blit(epicEngine$customBackground,
                0, 0, sw, sh, 0F, 0F, 1024, 1024, 1024, 1024);
    }

    @Unique
    private void epicEngine$renderOriginalBackground(GuiGraphics guiGraphics, float partialTicks) {
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        guiGraphics.fillGradient(0, 0, sw, sh, 0xFF0F0F23, 0xFF0F0F23);
    }

    @Unique
    private void epicEngine$renderCustomTitleWithLayout(GuiGraphics guiGraphics, MainMenuLayoutData.TitleComponent titleLayout) {
        try {
            int originalWidth = Math.max(1, epicEngine$titleWidth);
            int originalHeight = Math.max(1, epicEngine$titleHeight);

            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

            float defaultAutoScale = Math.min((screenWidth * 0.8F) / originalWidth, (screenHeight * 0.15F) / originalHeight);
            defaultAutoScale = Math.max(defaultAutoScale, 0.5F);

            float layoutScaleMultiplier = titleLayout.scale;
            float finalScale = defaultAutoScale * layoutScaleMultiplier;

            finalScale = Math.max(finalScale, 0.1F);
            finalScale = Math.min(finalScale, 5.0F);

            int x = titleLayout.position.x;
            int y = titleLayout.position.y;

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(finalScale, finalScale, 1F);

            guiGraphics.blit(epicEngine$customTitle,
                    0, 0, 0, 0,
                    originalWidth, originalHeight,
                    originalWidth, originalHeight);

            guiGraphics.pose().popPose();

        } catch (Exception e) {
            epicEngine$LOGGER.error("[EPIC ENGINE]: Error rendering custom title with layout", e);
        }
    }

    @Unique
    private void epicEngine$renderCustomTexts(GuiGraphics guiGraphics, MainMenuLayoutData layoutData) {
        for (MainMenuLayoutData.CustomTextComponent textComponent : layoutData.custom_texts) {
            if (textComponent.properties.text.isEmpty()) continue;

            int textColor = epicEngine$parseColor(textComponent.properties.color);
            float scale = textComponent.properties.font_scale;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(textComponent.position.x, textComponent.position.y, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);

            String displayText = EpicEngineI18n.getDisplayText(textComponent.properties.text);

            if (textComponent.properties.shadow) {
                guiGraphics.drawString(Minecraft.getInstance().font, displayText, 0, 0, textColor);
            } else {
                guiGraphics.drawString(Minecraft.getInstance().font, displayText, 0, 0, textColor, false);
            }

            guiGraphics.pose().popPose();
        }
    }

    @Unique
    private void epicEngine$loadCustomResources() {
        try {
            if (epicEngine$backgroundEnabled) {
                File bgFile = EpicEngineCustomConfig.getTextureFile(
                        EpicEngineCustomConfig.MAIN_MENU_BACKGROUND_FILENAME.get());
                if (bgFile.exists() && bgFile.isFile()) {
                    NativeImage img = epicEngine$readImage(bgFile);
                    if (img != null) {
                        epicEngine$customBackground = new ResourceLocation("epic_engine", "custom_background");
                        Minecraft.getInstance().getTextureManager()
                                .register(epicEngine$customBackground, new DynamicTexture(img));
                        epicEngine$customBackgroundLoaded = true;
                        epicEngine$LOGGER.info("[EPIC ENGINE]: Custom background loaded successfully");
                    }
                }
            }

            if (epicEngine$titleEnabled) {
                File titleFile = EpicEngineCustomConfig.getTextureFile(
                        EpicEngineCustomConfig.MAIN_MENU_TITLE_FILENAME.get());
                if (titleFile.exists() && titleFile.isFile()) {
                    NativeImage img = epicEngine$readImage(titleFile);
                    if (img != null) {
                        epicEngine$titleWidth = img.getWidth();
                        epicEngine$titleHeight = img.getHeight();
                        epicEngine$customTitle = new ResourceLocation("epic_engine", "custom_title");
                        Minecraft.getInstance().getTextureManager()
                                .register(epicEngine$customTitle, new DynamicTexture(img));
                        epicEngine$customTitleLoaded = true;
                        epicEngine$LOGGER.info("[EPIC ENGINE]: Custom title loaded successfully ({}x{})",
                                epicEngine$titleWidth, epicEngine$titleHeight);
                    }
                }
            }

            if (epicEngine$buttonsEnabled) {
                epicEngine$loadButtonTextures();
            }
        } catch (Exception e) {
            epicEngine$LOGGER.error("[EPIC ENGINE]: Error loading custom resources", e);
        }
    }

    @Unique
    private void epicEngine$loadButtonTextures() {
        String prefix = EpicEngineCustomConfig.getMainMenuButtonPrefix();

        for (int i = 1; i <= 10; i++) {
            String fileName = prefix + i + ".png";
            File buttonFile = EpicEngineCustomConfig.getTextureFile(fileName);

            if (buttonFile.exists() && buttonFile.isFile()) {
                try {
                    NativeImage img = epicEngine$readImage(buttonFile);
                    if (img != null) {
                        ResourceLocation textureLocation = new ResourceLocation("epic_engine", "custom_button_" + i);
                        Minecraft.getInstance().getTextureManager()
                                .register(textureLocation, new DynamicTexture(img));
                        epicEngine$LOGGER.info("[EPIC ENGINE]: Loaded button texture: {}", fileName);
                    }
                } catch (Exception e) {
                    epicEngine$LOGGER.warn("[EPIC ENGINE]: Failed to load button texture: {}", fileName, e);
                }
            }
        }
    }

    @Unique
    private int epicEngine$parseColor(String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                return Integer.parseInt(colorStr.substring(1), 16);
            }
            return Integer.parseInt(colorStr, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    @Unique
    private NativeImage epicEngine$readImage(File file) {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return NativeImage.read(in);
        } catch (IOException e) {
            epicEngine$LOGGER.error("[EPIC ENGINE]: Failed to read image: {}", file.getPath(), e);
            return null;
        }
    }
}