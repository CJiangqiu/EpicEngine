package com.epic_engine.custom;

import com.epic_engine.config.EpicEngineCustomConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;

/**
 * Unified Loading Screen Renderer
 * Provides unified loading experience with real progress integration
 */
public class UnifiedLoadingRenderer {

    private static final Logger LOGGER = LogManager.getLogger();

    // Resource management
    private static ResourceLocation backgroundTexture = null;
    private static ResourceLocation progressBarTexture = null;
    private static boolean resourcesInitialized = false;
    private static int progressBarImageWidth = 0;
    private static int progressBarImageHeight = 0;

    // Progress tracking
    private static float currentProgress = 0.0F;
    private static long lastProgressUpdate = 0;
    private static LoadingPhase currentPhase = LoadingPhase.PREPARE;

    // Progress lock mechanism to prevent regression
    private static boolean progressLocked = false;
    private static float lockedProgress = 0.0F;

    /**
     * Main render method - call this from all Mixins
     */
    public static void renderUnifiedLoadingScreen(GuiGraphics guiGraphics, Object screenInstance) {
        if (!EpicEngineCustomConfig.isLoadingScreenCustomizationEnabled()) {
            return;
        }

        ensureResourcesLoaded();

        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        // Load layout data from saved configuration
        LoadingScreenLayoutData layoutData = loadLayoutData();

        // Update progress with real data
        updateProgress(screenInstance);

        // Initialize tip text manager with layout data
        initializeTipTextManager(layoutData);

        // Render all components using layout data
        if (EpicEngineCustomConfig.isLoadingScreenBackgroundEnabled()) {
            renderBackground(guiGraphics, screenWidth, screenHeight);
        }

        if (EpicEngineCustomConfig.isLoadingScreenProgressEnabled()) {
            renderProgressBarWithLayout(guiGraphics, screenWidth, screenHeight, layoutData);
        }

        if (EpicEngineCustomConfig.shouldShowProgressPercentage()) {
            renderPercentageWithLayout(guiGraphics, screenWidth, screenHeight, layoutData);
        }

        // Render tip text if enabled
        if (EpicEngineCustomConfig.isLoadingScreenTipTextEnabled()) {
            renderTipTextWithLayout(guiGraphics, screenWidth, screenHeight, layoutData);
        }

        // Render custom texts from layout
        if (layoutData != null) {
            renderCustomTextsWithLayout(guiGraphics, layoutData);
        }
    }

    /**
     * Render for edit mode preview
     */
    public static void renderForEditMode(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        ensureResourcesLoaded();

        // Initialize tip text manager
        initializeTipTextManager(null);

        // Render background
        if (EpicEngineCustomConfig.isLoadingScreenBackgroundEnabled() && backgroundTexture != null) {
            renderBackground(guiGraphics, screenWidth, screenHeight);
        } else {
            // Render fallback background for edit mode
            renderFallbackBackground(guiGraphics, screenWidth, screenHeight);
        }

        // Render preview components with sample data
        renderEditModeComponents(guiGraphics, screenWidth, screenHeight);
    }

    /**
     * Render components for edit mode with sample data
     */
    private static void renderEditModeComponents(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        // 修复：使用 UnifiedLayoutEditor 获取布局数据
        LoadingScreenLayoutData layoutData = UnifiedLayoutEditor.getCurrentLoadingScreenLayout();
        if (layoutData == null) {
            return;
        }

        // Render progress bar with 60% progress
        if (layoutData.progress_bar.enabled) {
            renderEditModeProgressBar(guiGraphics, layoutData.progress_bar);
        }

        // Render tip text
        if (layoutData.tip_text.enabled) {
            renderEditModeTipText(guiGraphics, layoutData.tip_text);
        }

        // Render percentage text
        if (layoutData.percentage_text.enabled) {
            renderEditModePercentageText(guiGraphics, layoutData.percentage_text);
        }

        // Render custom texts
        for (LoadingScreenLayoutData.CustomTextComponent textComponent : layoutData.custom_texts) {
            if (!textComponent.properties.text.isEmpty()) {
                renderEditModeCustomText(guiGraphics, textComponent);
            }
        }
    }

    /**
     * Render progress bar for edit mode
     */
    private static void renderEditModeProgressBar(GuiGraphics guiGraphics, LoadingScreenLayoutData.ProgressBarComponent progressBar) {
        // 使用与正式渲染相同的位置和大小计算逻辑
        Minecraft mc = Minecraft.getInstance();
        com.mojang.blaze3d.platform.Window window = mc.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        // 使用原始图片尺寸
        int barDisplayWidth = progressBarImageWidth > 0 ? progressBarImageWidth : 400;
        int barDisplayHeight = progressBarImageHeight > 0 ? progressBarImageHeight : 32;

        // 小屏幕缩放逻辑
        if (screenWidth < 800 || screenHeight < 600) {
            int maxProgressBarWidth = (int)(screenWidth * 0.4f);
            int maxProgressBarHeight = (int)(screenHeight * 0.2f);

            if (barDisplayWidth > maxProgressBarWidth || barDisplayHeight > maxProgressBarHeight) {
                float scaleX = (float) maxProgressBarWidth / barDisplayWidth;
                float scaleY = (float) maxProgressBarHeight / barDisplayHeight;
                float scale = Math.min(scaleX, scaleY);

                barDisplayWidth = (int)(barDisplayWidth * scale);
                barDisplayHeight = (int)(barDisplayHeight * scale);
            }
        }

        // 计算右侧中心位置
        int barX = (screenWidth * 3 / 4) - (barDisplayWidth / 2);
        int barY = (screenHeight / 2) - (barDisplayHeight / 2);

        // 更新位置数据
        progressBar.position.x = barX;
        progressBar.position.y = barY;
        progressBar.position.width = barDisplayWidth;
        progressBar.position.height = barDisplayHeight;

        // Simulate 60% progress for preview
        float progress = 0.6f;
        int progressWidth = (int)(barDisplayWidth * progress);

        // Render background if enabled
        if (progressBar.show_background) {
            int bgColor = parseColor(progressBar.background_color) | (progressBar.background_alpha << 24);
            guiGraphics.fill(barX, barY, barX + barDisplayWidth, barY + barDisplayHeight, bgColor);
        } else {
            // Default dark background
            guiGraphics.fill(barX, barY, barX + barDisplayWidth, barY + barDisplayHeight, 0xFF404040);
        }

        // Render progress (green)
        guiGraphics.fill(barX, barY, barX + progressWidth, barY + barDisplayHeight, 0xFF00AA00);

        // Render border
        guiGraphics.fill(barX - 1, barY - 1, barX + barDisplayWidth + 1, barY, 0xFFFFFFFF);
        guiGraphics.fill(barX - 1, barY + barDisplayHeight, barX + barDisplayWidth + 1, barY + barDisplayHeight + 1, 0xFFFFFFFF);
        guiGraphics.fill(barX - 1, barY, barX, barY + barDisplayHeight, 0xFFFFFFFF);
        guiGraphics.fill(barX + barDisplayWidth, barY, barX + barDisplayWidth + 1, barY + barDisplayHeight, 0xFFFFFFFF);
    }

    /**
     * Render tip text for edit mode
     */
    private static void renderEditModeTipText(GuiGraphics guiGraphics, LoadingScreenLayoutData.TipTextComponent tipText) {
        LoadingScreenLayoutData.Position pos = tipText.position;
        LoadingScreenLayoutData.TipTextComponent.TipProperties props = tipText.properties;

        String text = TipTextManager.getCurrentTipText();
        if (text == null || text.isEmpty()) {
            text = "Sample Tip Text";
        }

        int color = parseColor(props.color);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(pos.x, pos.y, 0);
        guiGraphics.pose().scale(props.font_scale, props.font_scale, 1.0f);

        if (props.shadow) {
            guiGraphics.drawString(Minecraft.getInstance().font, text, 0, 0, color);
        } else {
            guiGraphics.drawString(Minecraft.getInstance().font, text, 0, 0, color, false);
        }

        guiGraphics.pose().popPose();
    }

    /**
     * Render percentage text for edit mode
     */
    private static void renderEditModePercentageText(GuiGraphics guiGraphics, LoadingScreenLayoutData.PercentageTextComponent percentageText) {
        LoadingScreenLayoutData.Position pos = percentageText.position;
        LoadingScreenLayoutData.PercentageTextComponent.PercentageProperties props = percentageText.properties;

        String text = String.format(props.format, 60.0f); // 60% for preview
        int color = parseColor(props.color);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(pos.x, pos.y, 0);
        guiGraphics.pose().scale(props.font_scale, props.font_scale, 1.0f);

        if (props.shadow) {
            guiGraphics.drawString(Minecraft.getInstance().font, text, 0, 0, color);
        } else {
            guiGraphics.drawString(Minecraft.getInstance().font, text, 0, 0, color, false);
        }

        guiGraphics.pose().popPose();
    }

    /**
     * Render custom text for edit mode
     */
    private static void renderEditModeCustomText(GuiGraphics guiGraphics, LoadingScreenLayoutData.CustomTextComponent textComponent) {
        LoadingScreenLayoutData.Position pos = textComponent.position;
        LoadingScreenLayoutData.CustomTextComponent.TextProperties props = textComponent.properties;

        String displayText = EpicEngineI18n.getDisplayText(props.text);
        int color = parseColor(props.color);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(pos.x, pos.y, 0);
        guiGraphics.pose().scale(props.font_scale, props.font_scale, 1.0f);

        if (props.shadow) {
            guiGraphics.drawString(Minecraft.getInstance().font, displayText, 0, 0, color);
        } else {
            guiGraphics.drawString(Minecraft.getInstance().font, displayText, 0, 0, color, false);
        }

        guiGraphics.pose().popPose();
    }

    /**
     * Load layout data from saved configuration file
     */
    private static LoadingScreenLayoutData loadLayoutData() {
        try {
            // First try to get from UnifiedLayoutEditor if available
            LoadingScreenLayoutData editorData = UnifiedLayoutEditor.getCurrentLoadingScreenLayout();
            if (editorData != null) {
                return editorData;
            }

            // Otherwise load from file
            java.io.File layoutFile = EpicEngineCustomConfig.getCustomDir().resolve("loading_screen_layout.json").toFile();
            return LoadingScreenLayoutData.load(layoutFile);
        } catch (Exception e) {
            LOGGER.warn("[EPIC ENGINE]: Failed to load loading screen layout, using defaults", e);
            return LoadingScreenLayoutData.createDefault();
        }
    }

    /**
     * Initialize tip text manager with layout data
     */
    private static void initializeTipTextManager(LoadingScreenLayoutData layoutData) {
        if (layoutData != null && layoutData.tip_text != null && layoutData.tip_text.properties != null) {
            TipTextManager.initialize(layoutData.tip_text.properties);
        } else {
            // Use default configuration
            TipTextManager.initialize(null);
        }
    }

    /**
     * Render progress bar with layout positioning
     */
    private static void renderProgressBarWithLayout(GuiGraphics guiGraphics, int screenWidth, int screenHeight, LoadingScreenLayoutData layoutData) {
        if (layoutData == null || !layoutData.progress_bar.enabled) {
            // Fallback to default rendering
            renderProgressBar(guiGraphics, screenWidth, screenHeight);
            return;
        }

        LoadingScreenLayoutData.ProgressBarComponent progressBar = layoutData.progress_bar;
        LoadingScreenLayoutData.Position pos = progressBar.position;

        // Calculate size (use layout position size if available, otherwise calculate)
        int barWidth = pos.width > 0 ? pos.width : getProgressBarTextureWidth();
        int barHeight = pos.height > 0 ? pos.height : getProgressBarTextureHeight();

        // Apply small screen scaling if needed
        if (screenWidth < 800 || screenHeight < 600) {
            int maxProgressBarWidth = (int)(screenWidth * 0.4f);
            int maxProgressBarHeight = (int)(screenHeight * 0.2f);

            if (barWidth > maxProgressBarWidth || barHeight > maxProgressBarHeight) {
                float scaleX = (float) maxProgressBarWidth / barWidth;
                float scaleY = (float) maxProgressBarHeight / barHeight;
                float scale = Math.min(scaleX, scaleY);

                barWidth = (int)(barWidth * scale);
                barHeight = (int)(barHeight * scale);
            }
        }

        // Use layout position
        int barX = pos.x;
        int barY = pos.y;

        // Only render if there is actual progress
        if (currentProgress > 0.0F && progressBarTexture != null) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // Calculate progress width
            int progressWidth = (int)(barWidth * currentProgress);
            float textureProgressWidth = (progressBarImageWidth * currentProgress);

            // Render background if enabled
            if (progressBar.show_background) {
                int bgColor = parseColor(progressBar.background_color) | (progressBar.background_alpha << 24);
                guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, bgColor);
            }

            // Only render the progress portion
            if (progressWidth > 0) {
                guiGraphics.blit(progressBarTexture,
                        barX, barY,                                      // Screen position
                        progressWidth, barHeight,                        // Display size
                        0.0F, 0.0F,                                     // Texture start
                        (int) textureProgressWidth, progressBarImageHeight, // Texture size
                        progressBarImageWidth, progressBarImageHeight); // Full texture size
            }
        }
    }

    /**
     * Render percentage text with layout positioning
     */
    private static void renderPercentageWithLayout(GuiGraphics guiGraphics, int screenWidth, int screenHeight, LoadingScreenLayoutData layoutData) {
        if (layoutData == null || !layoutData.percentage_text.enabled) {
            // Fallback to default rendering
            renderPercentage(guiGraphics, screenWidth, screenHeight);
            return;
        }

        LoadingScreenLayoutData.PercentageTextComponent percentageText = layoutData.percentage_text;
        LoadingScreenLayoutData.Position pos = percentageText.position;
        LoadingScreenLayoutData.PercentageTextComponent.PercentageProperties props = percentageText.properties;

        String text = String.format(props.format, currentProgress * 100);
        int color = parseColor(props.color);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(pos.x, pos.y, 0);
        guiGraphics.pose().scale(props.font_scale, props.font_scale, 1.0f);

        if (props.shadow) {
            guiGraphics.drawString(Minecraft.getInstance().font, text, 0, 0, color);
        } else {
            guiGraphics.drawString(Minecraft.getInstance().font, text, 0, 0, color, false);
        }

        guiGraphics.pose().popPose();
    }

    /**
     * Render tip text with layout positioning
     */
    private static void renderTipTextWithLayout(GuiGraphics guiGraphics, int screenWidth, int screenHeight, LoadingScreenLayoutData layoutData) {
        if (layoutData == null || !layoutData.tip_text.enabled) {
            // Fallback to default rendering
            renderTipText(guiGraphics, screenWidth, screenHeight);
            return;
        }

        LoadingScreenLayoutData.TipTextComponent tipText = layoutData.tip_text;
        LoadingScreenLayoutData.Position pos = tipText.position;
        LoadingScreenLayoutData.TipTextComponent.TipProperties props = tipText.properties;

        String text = TipTextManager.getCurrentTipText();
        if (text == null || text.isEmpty()) {
            return;
        }

        int color = parseColor(props.color);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(pos.x, pos.y, 0);
        guiGraphics.pose().scale(props.font_scale, props.font_scale, 1.0f);

        if (props.shadow) {
            guiGraphics.drawString(Minecraft.getInstance().font, text, 0, 0, color);
        } else {
            guiGraphics.drawString(Minecraft.getInstance().font, text, 0, 0, color, false);
        }

        guiGraphics.pose().popPose();
    }

    /**
     * Render custom texts with layout positioning
     */
    private static void renderCustomTextsWithLayout(GuiGraphics guiGraphics, LoadingScreenLayoutData layoutData) {
        for (LoadingScreenLayoutData.CustomTextComponent textComponent : layoutData.custom_texts) {
            if (textComponent.properties.text.isEmpty()) continue;

            LoadingScreenLayoutData.Position pos = textComponent.position;
            LoadingScreenLayoutData.CustomTextComponent.TextProperties props = textComponent.properties;

            String displayText = EpicEngineI18n.getDisplayText(props.text);
            int color = parseColor(props.color);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(pos.x, pos.y, 0);
            guiGraphics.pose().scale(props.font_scale, props.font_scale, 1.0f);

            if (props.shadow) {
                guiGraphics.drawString(Minecraft.getInstance().font, displayText, 0, 0, color);
            } else {
                guiGraphics.drawString(Minecraft.getInstance().font, displayText, 0, 0, color, false);
            }

            guiGraphics.pose().popPose();
        }
    }

    /**
     * Update progress with real data where available
     */
    private static void updateProgress(Object screenInstance) {
        // If progress is locked at high value, don't update
        if (progressLocked) {
            currentProgress = lockedProgress;
            LOGGER.debug("[EPIC ENGINE]: Progress locked at {}%, ignoring updates", (int)(lockedProgress * 100));
            return;
        }

        // Try to get real progress
        Optional<Float> realProgress = RealProgressExtractor.extractRealProgress(screenInstance);

        if (realProgress.isPresent()) {
            float progress = realProgress.get();
            // Ensure progress never goes backward
            if (progress >= currentProgress) {
                currentProgress = progress;
                lastProgressUpdate = System.currentTimeMillis();

                // Lock progress if we reach high completion (90%+)
                if (progress >= 0.9f) {
                    progressLocked = true;
                    lockedProgress = progress;
                    LOGGER.debug("[EPIC ENGINE]: Progress locked at {}% to prevent regression", (int)(progress * 100));
                }
                return;
            }
        }

        // Fallback to simulated progress only if not locked
        updateSimulatedProgress();
    }

    /**
     * Update simulated progress based on phase and time
     */
    private static void updateSimulatedProgress() {
        // Don't simulate if locked
        if (progressLocked) {
            currentProgress = lockedProgress;
            return;
        }

        long timeSinceUpdate = System.currentTimeMillis() - lastProgressUpdate;

        // Small incremental progress to prevent stalling
        if (timeSinceUpdate > 100) { // Update every 100ms
            float increment = 0.002f; // 0.2% per 100ms
            float maxProgress = getMaxProgressForPhase();

            currentProgress = Math.min(maxProgress, currentProgress + increment);
            lastProgressUpdate = System.currentTimeMillis();

            // Lock progress if we reach high simulation values
            if (currentProgress >= 0.85f) {
                progressLocked = true;
                lockedProgress = currentProgress;
                LOGGER.debug("[EPIC ENGINE]: Simulated progress locked at {}%", (int)(currentProgress * 100));
            }
        }
    }

    /**
     * Get maximum progress for current phase
     */
    private static float getMaxProgressForPhase() {
        return switch (currentPhase) {
            case PREPARE -> 0.25f;
            case LOADING -> 0.70f;
            case TERRAIN -> 0.95f;
            case FINALIZE -> 1.0f;
        };
    }

    /**
     * Update loading phase
     */
    public static void updateLoadingPhase(LoadingPhase phase) {
        if (phase.ordinal() >= currentPhase.ordinal()) {
            currentPhase = phase;

            // Don't unlock progress when switching phases during late loading
            if (!progressLocked) {
                LOGGER.debug("[EPIC ENGINE]: Phase updated to: {} (progress not locked)", phase);
            } else {
                LOGGER.debug("[EPIC ENGINE]: Phase updated to: {} (progress locked at {}%)",
                        phase, (int)(lockedProgress * 100));
            }
        }
    }

    /**
     * Render background
     */
    private static void renderBackground(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (backgroundTexture != null) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            guiGraphics.blit(backgroundTexture,
                    0, 0, screenWidth, screenHeight,
                    0.0F, 0.0F, 1024, 1024, 1024, 1024);
        } else {
            renderFallbackBackground(guiGraphics, screenWidth, screenHeight);
        }
    }

    /**
     * Render fallback background when no texture is available
     */
    private static void renderFallbackBackground(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        // Dark gradient background
        guiGraphics.fill(0, 0, screenWidth, screenHeight, 0xFF2a2a2a);

        // Add some subtle pattern
        for (int i = 0; i < 40; i++) {
            int x = (screenWidth / 40) * i;
            guiGraphics.fill(x, 0, x + 1, screenHeight, 0xFF353535);
        }
        for (int i = 0; i < 30; i++) {
            int y = (screenHeight / 30) * i;
            guiGraphics.fill(0, y, screenWidth, y + 1, 0xFF353535);
        }
    }


    /**
     * Render tip text
     */
    private static void renderTipText(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        // Get real current window size
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        screenWidth = window.getGuiScaledWidth();
        screenHeight = window.getGuiScaledHeight();

        // Get current tip text
        String tipText = TipTextManager.getCurrentTipText();
        if (tipText == null || tipText.isEmpty()) {
            return;
        }

        int textWidth = mc.font.width(tipText);

        // Position below progress bar (right side center)
        int textX = (screenWidth * 3 / 4) - (textWidth / 2);
        int textY = (screenHeight / 2) + 40;

        // Render with shadow
        guiGraphics.drawString(mc.font, tipText, textX + 1, textY + 1, 0x000000);
        guiGraphics.drawString(mc.font, tipText, textX, textY, 0xFFFFFF);
    }

    /**
     * Render progress bar (positioned at right side center) - DYNAMIC SCREEN SIZE
     * Only renders the actual progress, no background
     */
    private static void renderProgressBar(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (progressBarTexture != null && progressBarImageWidth > 0 && progressBarImageHeight > 0) {

            // Get REAL current window size, not cached values
            Minecraft mc = Minecraft.getInstance();
            Window window = mc.getWindow();
            int realScreenWidth = window.getGuiScaledWidth();
            int realScreenHeight = window.getGuiScaledHeight();

            // Log comparison to debug
            if (screenWidth != realScreenWidth || screenHeight != realScreenHeight) {
                LOGGER.info("[EPIC ENGINE]: Screen size mismatch! GuiGraphics: {}x{}, Window: {}x{}",
                        screenWidth, screenHeight, realScreenWidth, realScreenHeight);
            }

            // Use the REAL current window size
            screenWidth = realScreenWidth;
            screenHeight = realScreenHeight;

            LOGGER.debug("[EPIC ENGINE]: Using real screen size: {}x{}", screenWidth, screenHeight);

            // Use original image dimensions - NO scaling for normal screens
            int barDisplayWidth = progressBarImageWidth;   // 400
            int barDisplayHeight = progressBarImageHeight; // 32

            // Only scale if screen is genuinely small (not just startup artifact)
            if (screenWidth < 800 || screenHeight < 600) {
                int maxProgressBarWidth = (int)(screenWidth * 0.4f);  // 40% for small screens
                int maxProgressBarHeight = (int)(screenHeight * 0.2f); // 20% for small screens

                if (barDisplayWidth > maxProgressBarWidth || barDisplayHeight > maxProgressBarHeight) {
                    float scaleX = (float) maxProgressBarWidth / barDisplayWidth;
                    float scaleY = (float) maxProgressBarHeight / barDisplayHeight;
                    float scale = Math.min(scaleX, scaleY);

                    barDisplayWidth = (int)(barDisplayWidth * scale);
                    barDisplayHeight = (int)(barDisplayHeight * scale);

                    LOGGER.info("[EPIC ENGINE]: Small screen detected, scaling progress bar to {}x{}",
                            barDisplayWidth, barDisplayHeight);
                }
            }

            // Right side center position using REAL screen size
            int barX = (screenWidth * 3 / 4) - (barDisplayWidth / 2);
            int barY = (screenHeight / 2) - (barDisplayHeight / 2);

            LOGGER.debug("[EPIC ENGINE]: Progress bar position: ({}, {}), size: {}x{}",
                    barX, barY, barDisplayWidth, barDisplayHeight);

            // Only render if there is actual progress
            if (currentProgress > 0.0F) {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                // Calculate progress width
                int progressWidth = (int)(barDisplayWidth * currentProgress);
                float textureProgressWidth = (progressBarImageWidth * currentProgress);

                // Only render the progress portion (no background)
                if (progressWidth > 0) {
                    guiGraphics.blit(progressBarTexture,
                            barX, barY,                                      // Screen position
                            progressWidth, barDisplayHeight,                 // Display size
                            0.0F, 0.0F,                                     // Texture start
                            (int) textureProgressWidth, progressBarImageHeight, // Texture size
                            progressBarImageWidth, progressBarImageHeight); // Full texture size
                }
            }
        }
    }

    /**
     * Render percentage text (using real screen size)
     */
    private static void renderPercentage(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        // Get real current window size
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        screenWidth = window.getGuiScaledWidth();
        screenHeight = window.getGuiScaledHeight();

        String percentageText = String.format("%.0f%%", currentProgress * 100);
        int textWidth = mc.font.width(percentageText);

        // Right side center position using real screen size
        int textX = (screenWidth * 3 / 4) - (textWidth / 2);
        int textY = screenHeight / 2 - 4;

        // Render percentage text with shadow
        guiGraphics.drawString(mc.font, percentageText, textX + 1, textY + 1, 0x000000);
        guiGraphics.drawString(mc.font, percentageText, textX, textY, 0xFFFFFF);
    }

    /**
     * Load all resources
     */
    public static void ensureResourcesLoaded() {
        if (!resourcesInitialized) {
            loadBackgroundTexture();
            loadProgressBarTexture();
            resourcesInitialized = true;
        }
    }

    /**
     * Load background texture
     */
    private static void loadBackgroundTexture() {
        try {
            String filename = EpicEngineCustomConfig.getLoadingScreenBackgroundFilename();
            if (filename != null && !filename.trim().isEmpty()) {
                File backgroundFile = EpicEngineCustomConfig.getTextureFile(filename);
                if (backgroundFile.exists() && backgroundFile.isFile()) {
                    backgroundTexture = loadTextureFromFile(backgroundFile, "unified_loading_background");
                    LOGGER.info("[EPIC ENGINE]: Loading background loaded: {}", filename);
                } else {
                    LOGGER.warn("[EPIC ENGINE]: Loading background file not found: {}", filename);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[EPIC ENGINE]: Failed to load background texture", e);
        }
    }

    /**
     * Load progress bar texture
     */
    private static void loadProgressBarTexture() {
        try {
            String filename = EpicEngineCustomConfig.getLoadingScreenProgressBarFilename();
            if (filename != null && !filename.trim().isEmpty()) {
                File progressFile = EpicEngineCustomConfig.getTextureFile(filename);
                if (progressFile.exists() && progressFile.isFile()) {
                    try (InputStream stream = Files.newInputStream(progressFile.toPath())) {
                        NativeImage image = NativeImage.read(stream);
                        progressBarImageWidth = image.getWidth();
                        progressBarImageHeight = image.getHeight();

                        progressBarTexture = new ResourceLocation("epic_engine", "unified_progress_bar");
                        Minecraft.getInstance().getTextureManager()
                                .register(progressBarTexture, new DynamicTexture(image));

                        LOGGER.info("[EPIC ENGINE]: Progress bar loaded: {} ({}x{})",
                                filename, progressBarImageWidth, progressBarImageHeight);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("[EPIC ENGINE]: Failed to load progress bar texture", e);
        }
    }

    /**
     * Load texture from file
     */
    private static ResourceLocation loadTextureFromFile(File file, String resourceName) throws IOException {
        try (InputStream stream = Files.newInputStream(file.toPath())) {
            NativeImage image = NativeImage.read(stream);
            ResourceLocation location = new ResourceLocation("epic_engine", resourceName);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            return location;
        }
    }

    /**
     * Get background texture for external access
     */
    public static ResourceLocation getBackgroundTexture() {
        ensureResourcesLoaded();
        return backgroundTexture;
    }

    /**
     * Check if background texture is loaded
     */
    public static boolean hasBackgroundTexture() {
        return backgroundTexture != null;
    }

    /**
     * Get progress bar texture width for external access
     */
    public static int getProgressBarTextureWidth() {
        ensureResourcesLoaded();
        return progressBarImageWidth > 0 ? progressBarImageWidth : 400; // 默认宽度
    }

    /**
     * Get progress bar texture height for external access
     */
    public static int getProgressBarTextureHeight() {
        ensureResourcesLoaded();
        return progressBarImageHeight > 0 ? progressBarImageHeight : 32; // 默认高度
    }

    /**
     * Parse color string to integer
     */
    private static int parseColor(String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                return Integer.parseInt(colorStr.substring(1), 16);
            }
            return Integer.parseInt(colorStr, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    /**
     * Reset all resources and state
     */
    public static void resetResources() {
        resourcesInitialized = false;
        backgroundTexture = null;
        progressBarTexture = null;
        progressBarImageWidth = 0;
        progressBarImageHeight = 0;
        currentProgress = 0.0F;
        lastProgressUpdate = 0;
        currentPhase = LoadingPhase.PREPARE;

        // Reset progress lock
        progressLocked = false;
        lockedProgress = 0.0F;

        LOGGER.debug("[EPIC ENGINE]: Unified loading renderer reset (progress unlocked)");
    }

    /**
     * Get current progress
     */
    public static float getCurrentProgress() {
        return currentProgress;
    }

    /**
     * Manually unlock progress (for debugging or special cases)
     */
    public static void unlockProgress() {
        progressLocked = false;
        lockedProgress = 0.0F;
        LOGGER.debug("[EPIC ENGINE]: Progress manually unlocked");
    }

    /**
     * Loading phase enumeration
     */
    public enum LoadingPhase {
        PREPARE,    // Preparing world data
        LOADING,    // Loading world
        TERRAIN,    // Downloading terrain/connecting to server
        FINALIZE    // Finalizing loading
    }
}