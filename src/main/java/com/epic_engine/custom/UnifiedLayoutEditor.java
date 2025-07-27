package com.epic_engine.custom;

import com.epic_engine.config.EpicEngineCustomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class UnifiedLayoutEditor {
    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean isEditMode = false;
    private static EditPage currentPage = EditPage.MAIN_MENU;

    private static MainMenuLayoutData mainMenuLayout = null;
    private static LoadingScreenLayoutData loadingScreenLayout = null;

    private static boolean isDragging = false;
    private static ComponentType draggingType = ComponentType.NONE;
    private static String draggingComponentId = "";
    private static int dragStartX, dragStartY;
    private static int componentStartX, componentStartY;
    
    // 跟踪进度条是否被手动拖动过
    private static boolean progressBarManuallyPositioned = false;

    private static Button editToggleButton = null;
    private static Button prevPageButton = null;
    private static Button nextPageButton = null;
    private static Button pageIndicatorButton = null;
    private static Button saveLayoutButton = null;

    private static final int COLOR_HOVER = 0x8000FF00;
    private static final int COLOR_SELECTED = 0x80FF0000;
    private static final int COLOR_BORDER = 0x80FFFFFF;

    public enum EditPage {
        MAIN_MENU("epic_engine.editor.page_main_menu"),
        LOADING_SCREEN("epic_engine.editor.page_loading_screen");

        private final String translationKey;

        EditPage(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    public enum ComponentType {
        NONE,
        TITLE_IMAGE,
        MENU_BUTTON,
        CUSTOM_TEXT,
        PROGRESS_BAR,
        TIP_TEXT,
        PERCENTAGE_TEXT,
        LOADING_CUSTOM_TEXT
    }

    public static void init(int screenWidth, int screenHeight) {
        LOGGER.info("[EPIC ENGINE]: Initializing unified layout editor - Screen: {}x{}", screenWidth, screenHeight);

        if (!EpicEngineCustomConfig.MAIN_MENU_MODULE_ENABLED.get()) {
            LOGGER.info("[EPIC ENGINE]: Main menu module disabled, skipping editor init");
            return;
        }

        loadLayoutData();
        updateScreenResolution(screenWidth, screenHeight);
        createEditorButtons(screenWidth, screenHeight);

        if (isEditMode) {
            initializeCurrentPageSystems();
        }

        LOGGER.info("[EPIC ENGINE]: Unified layout editor initialized - EditMode: {}, Page: {}", isEditMode, currentPage);
    }

    private static void loadLayoutData() {
        if (mainMenuLayout == null) {
            mainMenuLayout = MainMenuLayoutData.load(EpicEngineCustomConfig.getLayoutFile());
            LOGGER.info("[EPIC ENGINE]: Main menu layout loaded - {} buttons, {} texts",
                    mainMenuLayout.getButtonCount(), mainMenuLayout.getCustomTextCount());
        }

        if (loadingScreenLayout == null) {
            loadingScreenLayout = LoadingScreenLayoutData.load(getLoadingLayoutFile());
            LOGGER.info("[EPIC ENGINE]: Loading screen layout loaded - {} custom texts",
                    loadingScreenLayout.getCustomTextCount());
        }
    }

    private static void updateScreenResolution(int screenWidth, int screenHeight) {
        if (mainMenuLayout != null) {
            mainMenuLayout.updateScreenResolution(screenWidth, screenHeight);
        }
        if (loadingScreenLayout != null) {
            loadingScreenLayout.updateScreenResolution(screenWidth, screenHeight);
        }
    }

    private static void createEditorButtons(int screenWidth, int screenHeight) {
        editToggleButton = Button.builder(
                Component.translatable(isEditMode ?
                        "epic_engine.editor.exit_edit" :
                        "epic_engine.editor.edit_layout"),
                button -> toggleEditMode()
        ).bounds(screenWidth - 120, screenHeight - 30, 110, 20).build();

        if (isEditMode) {
            createEditModeButtons(screenWidth, screenHeight);
        } else {
            clearEditModeButtons();
        }
    }

    private static void createEditModeButtons(int screenWidth, int screenHeight) {
        int topButtonY = 10;
        int buttonHeight = 20;
        int centerX = screenWidth / 2;

        prevPageButton = Button.builder(
                Component.translatable("epic_engine.editor.prev_page"),
                button -> switchToPreviousPage()
        ).bounds(centerX - 120, topButtonY, 80, buttonHeight).build();

        updatePageIndicatorButton(centerX, topButtonY);

        nextPageButton = Button.builder(
                Component.translatable("epic_engine.editor.next_page"),
                button -> switchToNextPage()
        ).bounds(centerX + 40, topButtonY, 80, buttonHeight).build();

        saveLayoutButton = Button.builder(
                Component.translatable("epic_engine.editor.save_layout"),
                button -> saveCurrentLayout()
        ).bounds(screenWidth - 120, screenHeight - 55, 110, 20).build();

        updateButtonStates();

        LOGGER.info("[EPIC ENGINE]: Edit mode buttons created for page: {}", currentPage);
    }

    private static void updatePageIndicatorButton(int centerX, int topButtonY) {
        String pageText = Component.translatable(currentPage.getTranslationKey()).getString();

        pageIndicatorButton = Button.builder(
                Component.literal(pageText),
                button -> {}
        ).bounds(centerX - 40, topButtonY, 80, 20).build();
        pageIndicatorButton.active = false;
    }

    private static void updateButtonStates() {
        if (prevPageButton != null) {
            prevPageButton.active = (currentPage != EditPage.MAIN_MENU);
        }
        if (nextPageButton != null) {
            nextPageButton.active = (currentPage != EditPage.LOADING_SCREEN);
        }
    }

    private static void clearEditModeButtons() {
        prevPageButton = null;
        nextPageButton = null;
        pageIndicatorButton = null;
        saveLayoutButton = null;
    }

    private static void toggleEditMode() {
        boolean oldMode = isEditMode;
        isEditMode = !isEditMode;

        if (!isEditMode) {
            stopDragging();
            currentPage = EditPage.MAIN_MENU;
        }

        LOGGER.info("[EPIC ENGINE]: Layout editor mode changed: {} -> {}", oldMode, isEditMode);

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            init(mc.screen.width, mc.screen.height);
        }
    }

    private static void switchToPreviousPage() {
        if (currentPage == EditPage.LOADING_SCREEN) {
            currentPage = EditPage.MAIN_MENU;
            stopDragging();
            // 切换页面时重置进度条手动定位标志
            progressBarManuallyPositioned = false;

            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) {
                updatePageIndicatorButton(mc.screen.width / 2, 10);
                updateButtonStates();
                initializeCurrentPageSystems();
            }

            LOGGER.info("[EPIC ENGINE]: Switched to Main Menu page");
        }
    }

    private static void switchToNextPage() {
        if (currentPage == EditPage.MAIN_MENU) {
            currentPage = EditPage.LOADING_SCREEN;
            stopDragging();
            // 切换到加载页面时不重置手动定位标志，保持用户的自定义位置

            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) {
                updatePageIndicatorButton(mc.screen.width / 2, 10);
                updateButtonStates();
                initializeCurrentPageSystems();
            }

            LOGGER.info("[EPIC ENGINE]: Switched to Loading Screen page");
        }
    }

    private static void initializeCurrentPageSystems() {
        if (currentPage == EditPage.MAIN_MENU) {
            if (mainMenuLayout != null) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof TitleScreen titleScreen) {
                    CustomButtonManager.initialize(titleScreen, mainMenuLayout);
                }
            }
        } else if (currentPage == EditPage.LOADING_SCREEN) {
            if (loadingScreenLayout != null && loadingScreenLayout.tip_text != null &&
                    loadingScreenLayout.tip_text.properties != null) {
                TipTextManager.initialize(loadingScreenLayout.tip_text.properties);
            }
        }
    }

    private static void saveCurrentLayout() {
        boolean success = false;

        if (currentPage == EditPage.MAIN_MENU && mainMenuLayout != null) {
            mainMenuLayout.save(EpicEngineCustomConfig.getLayoutFile());
            success = true;
            LOGGER.info("[EPIC ENGINE]: Main menu layout saved successfully");
        } else if (currentPage == EditPage.LOADING_SCREEN && loadingScreenLayout != null) {
            loadingScreenLayout.save(getLoadingLayoutFile());
            success = true;
            LOGGER.info("[EPIC ENGINE]: Loading screen layout saved successfully");
        }

        if (success) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui != null) {
                mc.gui.setOverlayMessage(
                        Component.translatable("epic_engine.editor.layout_saved"),
                        false
                );
            }
        }
    }

    public static void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!shouldShowEditor()) {
            return;
        }

        if (isEditMode) {
            renderCurrentPageContent(graphics, mouseX, mouseY, partialTicks);

            resetRenderState();
            renderEditorControlBar(graphics, mouseX, mouseY, partialTicks);

            resetRenderState();
            if (saveLayoutButton != null) {
                saveLayoutButton.render(graphics, mouseX, mouseY, partialTicks);
            }
        }

        resetRenderState();
        if (editToggleButton != null) {
            editToggleButton.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    private static void resetRenderState() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
    }

    private static void renderEditorControlBar(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int screenWidth = graphics.guiWidth();

        graphics.fill(0, 0, screenWidth, 40, 0x80000000);

        if (prevPageButton != null) {
            prevPageButton.render(graphics, mouseX, mouseY, partialTicks);
        }
        if (pageIndicatorButton != null) {
            pageIndicatorButton.render(graphics, mouseX, mouseY, partialTicks);
        }
        if (nextPageButton != null) {
            nextPageButton.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    private static void renderCurrentPageContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (currentPage == EditPage.MAIN_MENU) {
            renderMainMenuEditContent(graphics, mouseX, mouseY, partialTicks);
        } else if (currentPage == EditPage.LOADING_SCREEN) {
            renderLoadingScreenEditContent(graphics, mouseX, mouseY, partialTicks);
        }
    }

    private static void renderMainMenuEditContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderEditModeOverlay(graphics, mouseX, mouseY, "gui.epic_engine.main_menu.edit_mode_title");
        renderMainMenuComponentEditAreas(graphics, mouseX, mouseY);
        handleDragLogic(mouseX, mouseY);
    }

    private static void renderLoadingScreenEditContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.pose().pushPose();

        try {
            renderLoadingScreenBackground(graphics);
            renderLoadingScreenPreviewComponents(graphics);
        } finally {
            graphics.pose().popPose();
        }

        renderEditModeOverlay(graphics, mouseX, mouseY, "gui.epic_engine.loading_screen.edit_mode_title");
        renderLoadingScreenComponentEditAreas(graphics, mouseX, mouseY);
        handleDragLogic(mouseX, mouseY);
    }

    private static void renderEditModeOverlay(GuiGraphics graphics, int mouseX, int mouseY, String titleKey) {
        int screenWidth = graphics.guiWidth();

        graphics.fill(0, 45, screenWidth, 70, 0x80000000);

        graphics.drawCenteredString(
                Minecraft.getInstance().font,
                Component.translatable(titleKey).getString(),
                screenWidth / 2, 53, 0xFFFFFF
        );

        if (isDragging) {
            String dragInfo = Component.translatable("epic_engine.editor.dragging_info",
                    draggingType.toString()).getString();
            graphics.drawString(Minecraft.getInstance().font, dragInfo, 10, 75, 0xFFFF00);
        }

        String mouseInfo = Component.translatable("epic_engine.editor.mouse_info",
                mouseX, mouseY).getString();
        graphics.drawString(Minecraft.getInstance().font, mouseInfo, 10, 90, 0xCCCCCC);

        // Add button toggle hint for main menu page
        if (currentPage == EditPage.MAIN_MENU) {
            String toggleHint = Component.translatable("epic_engine.editor.button_toggle_hint").getString();
            graphics.drawString(Minecraft.getInstance().font, toggleHint, 10, 105, 0xAAFFAA);
        }
    }

    private static void renderMainMenuComponentEditAreas(GuiGraphics graphics, int mouseX, int mouseY) {
        if (mainMenuLayout == null) return;

        if (EpicEngineCustomConfig.isMainMenuTitleEnabled()) {
            renderTitleEditArea(graphics, mouseX, mouseY);
        }

        if (EpicEngineCustomConfig.isMainMenuButtonsEnabled()) {
            renderButtonEditAreas(graphics, mouseX, mouseY);
        }

        renderMainMenuCustomTextEditAreas(graphics, mouseX, mouseY);
    }

    private static void renderTitleEditArea(GuiGraphics graphics, int mouseX, int mouseY) {
        MainMenuLayoutData.Position titlePos = mainMenuLayout.title_image.position;

        boolean isHovered = isMouseOverPosition(mouseX, mouseY, titlePos);
        boolean isSelected = isDragging && draggingType == ComponentType.TITLE_IMAGE;

        int borderColor = isSelected ? COLOR_SELECTED : (isHovered ? COLOR_HOVER : COLOR_BORDER);
        renderComponentBorder(graphics, titlePos, borderColor);

        if (isHovered || isSelected) {
            String posInfo = Component.translatable("gui.epic_engine.main_menu.component_title",
                    titlePos.x, titlePos.y).getString();
            graphics.drawString(Minecraft.getInstance().font, posInfo,
                    titlePos.x, Math.max(titlePos.y - 15, 5), 0xFFFFFF);
        }
    }

    private static void renderButtonEditAreas(GuiGraphics graphics, int mouseX, int mouseY) {
        if (currentPage != EditPage.MAIN_MENU || mainMenuLayout == null) {
            return;
        }

        // Render enabled buttons normally
        for (CustomMainMenuButton customButton : CustomButtonManager.getCustomButtons()) {
            boolean isHovered = isMouseOverWidget(mouseX, mouseY, customButton);
            boolean isSelected = isDragging && draggingType == ComponentType.MENU_BUTTON &&
                    customButton.getButtonId().equals(draggingComponentId);

            int borderColor = isSelected ? COLOR_SELECTED : (isHovered ? COLOR_HOVER : COLOR_BORDER);
            renderWidgetBorder(graphics, customButton, borderColor);

            if (isHovered || isSelected) {
                String buttonText = customButton.getMessage().getString();
                String buttonInfo = Component.translatable("gui.epic_engine.main_menu.component_button",
                        buttonText, customButton.getX(), customButton.getY()).getString();
                graphics.drawString(Minecraft.getInstance().font, buttonInfo,
                        customButton.getX(), Math.max(customButton.getY() - 15, 5), 0xFFFFFF);
            }
        }

        // Render disabled buttons with special styling
        for (MainMenuLayoutData.ButtonComponent buttonComponent : mainMenuLayout.buttons) {
            if (!buttonComponent.enabled && !buttonComponent.id.startsWith("external_")) {
                boolean isHovered = isMouseOverPosition(mouseX, mouseY, buttonComponent.position);
                boolean isSelected = isDragging && draggingType == ComponentType.MENU_BUTTON &&
                        buttonComponent.id.equals(draggingComponentId);

                // Use red border for disabled buttons
                int borderColor = isSelected ? 0xFFFF0000 : (isHovered ? 0xFFFF6666 : 0xFF800000);
                renderComponentBorder(graphics, buttonComponent.position, borderColor);

                // Add disabled overlay
                graphics.fill(buttonComponent.position.x, buttonComponent.position.y,
                        buttonComponent.position.x + buttonComponent.position.width,
                        buttonComponent.position.y + buttonComponent.position.height, 0x80FF0000);

                if (isHovered || isSelected) {
                    String buttonInfo = Component.translatable("gui.epic_engine.main_menu.component_button_disabled",
                            buttonComponent.id, buttonComponent.position.x, buttonComponent.position.y).getString();
                    graphics.drawString(Minecraft.getInstance().font, buttonInfo,
                            buttonComponent.position.x, Math.max(buttonComponent.position.y - 15, 5), 0xFFFFFF);
                }
            }
        }
    }

    private static void renderMainMenuCustomTextEditAreas(GuiGraphics graphics, int mouseX, int mouseY) {
        for (MainMenuLayoutData.CustomTextComponent text : mainMenuLayout.custom_texts) {
            boolean isHovered = isMouseOverPosition(mouseX, mouseY, text.position);
            boolean isSelected = isDragging && draggingType == ComponentType.CUSTOM_TEXT &&
                    text.id.equals(draggingComponentId);

            int borderColor = isSelected ? COLOR_SELECTED : (isHovered ? COLOR_HOVER : COLOR_BORDER);
            renderComponentBorder(graphics, text.position, borderColor);

            if (isHovered || isSelected) {
                String textContent = text.properties.text.isEmpty() ? text.id : text.properties.text;
                String textInfo = Component.translatable("gui.epic_engine.main_menu.component_text",
                        textContent, text.position.x, text.position.y).getString();
                graphics.drawString(Minecraft.getInstance().font, textInfo,
                        text.position.x, Math.max(text.position.y - 15, 5), 0xFFFFFF);
            }
        }
    }

    private static void renderLoadingScreenBackground(GuiGraphics graphics) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        ResourceLocation bgTexture = UnifiedLoadingRenderer.getBackgroundTexture();
        if (bgTexture != null) {
            try {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                graphics.blit(bgTexture, 0, 0, screenWidth, screenHeight,
                        0.0F, 0.0F, 1024, 1024, 1024, 1024);
                return;
            } catch (Exception e) {
                LOGGER.warn("[EPIC ENGINE]: Failed to render loading background texture", e);
            }
        }

        renderStylizedBackground(graphics, screenWidth, screenHeight);
    }

    private static void renderStylizedBackground(GuiGraphics graphics, int screenWidth, int screenHeight) {
        graphics.fillGradient(0, 0, screenWidth, screenHeight, 0xFF1a1a2e, 0xFF16213e);

        int gridSize = 50;
        int gridColor = 0x30FFFFFF;

        for (int x = gridSize; x < screenWidth; x += gridSize) {
            graphics.fill(x, 0, x + 1, screenHeight, gridColor);
        }
        for (int y = gridSize; y < screenHeight; y += gridSize) {
            graphics.fill(0, y, screenWidth, y + 1, gridColor);
        }

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        graphics.fill(centerX, 0, centerX + 1, screenHeight, 0x60FFFFFF);
        graphics.fill(0, centerY, screenWidth, centerY + 1, 0x60FFFFFF);
    }

    private static void renderLoadingScreenPreviewComponents(GuiGraphics graphics) {
        if (loadingScreenLayout == null) return;

        try {
            if (loadingScreenLayout.progress_bar.enabled) {
                renderPreviewProgressBar(graphics);
            }

            if (loadingScreenLayout.tip_text.enabled) {
                renderPreviewTipText(graphics);
            }

            if (loadingScreenLayout.percentage_text.enabled) {
                renderPreviewPercentageText(graphics);
            }

            for (LoadingScreenLayoutData.CustomTextComponent textComponent : loadingScreenLayout.custom_texts) {
                if (!textComponent.properties.text.isEmpty()) {
                    renderPreviewCustomText(graphics, textComponent);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[EPIC ENGINE]: Error rendering loading screen preview components", e);
        }
    }

    private static void renderPreviewProgressBar(GuiGraphics graphics) {
        LoadingScreenLayoutData.ProgressBarComponent progressBar = loadingScreenLayout.progress_bar;

        int barX, barY, barDisplayWidth, barDisplayHeight;

        // 使用统一的当前位置获取方法
        LoadingScreenLayoutData.Position currentPos = getCurrentProgressBarPosition();
        barX = currentPos.x;
        barY = currentPos.y;
        barDisplayWidth = currentPos.width;
        barDisplayHeight = currentPos.height;
        
        // 更新布局数据以反映当前渲染位置
        progressBar.position.x = barX;
        progressBar.position.y = barY;
        progressBar.position.width = barDisplayWidth;
        progressBar.position.height = barDisplayHeight;

        float progress = 0.75f;
        int progressWidth = (int)(barDisplayWidth * progress);

        // 渲染背景
        if (progressBar.show_background) {
            int bgColor = parseColor(progressBar.background_color) | (progressBar.background_alpha << 24);
            graphics.fill(barX, barY, barX + barDisplayWidth, barY + barDisplayHeight, bgColor);
        } else {
            graphics.fill(barX, barY, barX + barDisplayWidth, barY + barDisplayHeight, 0xFF2a2a2a);
        }

        // 渲染进度条
        graphics.fillGradient(barX, barY, barX + progressWidth, barY + barDisplayHeight,
                0xFF00aa00, 0xFF00ff00);

        // 渲染边框
        graphics.fill(barX - 1, barY - 1, barX + barDisplayWidth + 1, barY, 0xFFFFFFFF);
        graphics.fill(barX - 1, barY + barDisplayHeight, barX + barDisplayWidth + 1, barY + barDisplayHeight + 1, 0xFFFFFFFF);
        graphics.fill(barX - 1, barY, barX, barY + barDisplayHeight, 0xFFFFFFFF);
        graphics.fill(barX + barDisplayWidth, barY, barX + barDisplayWidth + 1, barY + barDisplayHeight, 0xFFFFFFFF);

        // 注意：不在进度条内部渲染百分比文本，因为有独立的百分比文本组件
    }

    private static void renderPreviewTipText(GuiGraphics graphics) {
        LoadingScreenLayoutData.TipTextComponent tipText = loadingScreenLayout.tip_text;
        LoadingScreenLayoutData.Position pos = tipText.position;
        LoadingScreenLayoutData.TipTextComponent.TipProperties props = tipText.properties;

        String text = TipTextManager.getCurrentTipText();
        if (text == null || text.isEmpty()) {
            text = "Sample tip text for preview...";
        }

        int color = parseColor(props.color);

        graphics.pose().pushPose();
        graphics.pose().translate(pos.x, pos.y, 0);
        graphics.pose().scale(props.font_scale, props.font_scale, 1.0f);

        int textWidth = Minecraft.getInstance().font.width(text);
        int centeredX = (int)((pos.width - textWidth * props.font_scale) / (2 * props.font_scale));

        if (props.shadow) {
            graphics.drawString(Minecraft.getInstance().font, text, centeredX + 1, 1, 0x000000);
            graphics.drawString(Minecraft.getInstance().font, text, centeredX, 0, color);
        } else {
            graphics.drawString(Minecraft.getInstance().font, text, centeredX, 0, color, false);
        }

        graphics.pose().popPose();
    }

    private static void renderPreviewPercentageText(GuiGraphics graphics) {
        LoadingScreenLayoutData.PercentageTextComponent percentageText = loadingScreenLayout.percentage_text;
        LoadingScreenLayoutData.Position pos = percentageText.position;
        LoadingScreenLayoutData.PercentageTextComponent.PercentageProperties props = percentageText.properties;

        String text = String.format(props.format, 75.0f);
        int color = parseColor(props.color);

        graphics.pose().pushPose();
        graphics.pose().translate(pos.x, pos.y, 0);
        graphics.pose().scale(props.font_scale, props.font_scale, 1.0f);

        if (props.shadow) {
            graphics.drawString(Minecraft.getInstance().font, text, 1, 1, 0x000000);
            graphics.drawString(Minecraft.getInstance().font, text, 0, 0, color);
        } else {
            graphics.drawString(Minecraft.getInstance().font, text, 0, 0, color, false);
        }

        graphics.pose().popPose();
    }

    private static void renderPreviewCustomText(GuiGraphics graphics, LoadingScreenLayoutData.CustomTextComponent textComponent) {
        LoadingScreenLayoutData.Position pos = textComponent.position;
        LoadingScreenLayoutData.CustomTextComponent.TextProperties props = textComponent.properties;

        String displayText = EpicEngineI18n.getDisplayText(props.text);
        int color = parseColor(props.color);

        graphics.pose().pushPose();
        graphics.pose().translate(pos.x, pos.y, 0);
        graphics.pose().scale(props.font_scale, props.font_scale, 1.0f);

        if (props.shadow) {
            graphics.drawString(Minecraft.getInstance().font, displayText, 1, 1, 0x000000);
            graphics.drawString(Minecraft.getInstance().font, displayText, 0, 0, color);
        } else {
            graphics.drawString(Minecraft.getInstance().font, displayText, 0, 0, color, false);
        }

        graphics.pose().popPose();
    }

    private static void renderLoadingScreenComponentEditAreas(GuiGraphics graphics, int mouseX, int mouseY) {
        if (loadingScreenLayout == null) return;

        if (loadingScreenLayout.progress_bar.enabled) {
            renderProgressBarEditArea(graphics, mouseX, mouseY);
        }

        if (loadingScreenLayout.tip_text.enabled) {
            renderTipTextEditArea(graphics, mouseX, mouseY);
        }

        if (loadingScreenLayout.percentage_text.enabled) {
            renderPercentageTextEditArea(graphics, mouseX, mouseY);
        }

        renderLoadingScreenCustomTextEditAreas(graphics, mouseX, mouseY);
    }

    private static void renderProgressBarEditArea(GuiGraphics graphics, int mouseX, int mouseY) {
        LoadingScreenLayoutData.Position pos = loadingScreenLayout.progress_bar.position;

        // 使用实际渲染位置进行边框绘制（位置已在 renderPreviewProgressBar 中更新）
        boolean isHovered = isMouseOverPosition(mouseX, mouseY, pos);
        boolean isSelected = isDragging && draggingType == ComponentType.PROGRESS_BAR;

        int borderColor = isSelected ? COLOR_SELECTED : (isHovered ? COLOR_HOVER : COLOR_BORDER);
        renderComponentBorder(graphics, pos, borderColor);

        if (isHovered || isSelected) {
            String posInfo = Component.translatable("gui.epic_engine.loading_screen.component_progress_bar",
                    pos.x, pos.y, pos.width, pos.height).getString();
            graphics.drawString(Minecraft.getInstance().font, posInfo,
                    pos.x, Math.max(pos.y - 15, 5), 0xFFFFFF);
        }
    }

    private static void renderTipTextEditArea(GuiGraphics graphics, int mouseX, int mouseY) {
        LoadingScreenLayoutData.Position pos = loadingScreenLayout.tip_text.position;

        boolean isHovered = isMouseOverPosition(mouseX, mouseY, pos);
        boolean isSelected = isDragging && draggingType == ComponentType.TIP_TEXT;

        int borderColor = isSelected ? COLOR_SELECTED : (isHovered ? COLOR_HOVER : COLOR_BORDER);
        renderComponentBorder(graphics, pos, borderColor);

        if (isHovered || isSelected) {
            String currentTip = TipTextManager.getCurrentTipText();
            if (currentTip == null || currentTip.isEmpty()) {
                currentTip = "Tip Text";
            }
            String posInfo = Component.translatable("gui.epic_engine.loading_screen.component_tip_text",
                    currentTip, pos.x, pos.y).getString();
            graphics.drawString(Minecraft.getInstance().font, posInfo,
                    pos.x, Math.max(pos.y - 15, 5), 0xFFFFFF);
        }
    }

    private static void renderPercentageTextEditArea(GuiGraphics graphics, int mouseX, int mouseY) {
        LoadingScreenLayoutData.Position pos = loadingScreenLayout.percentage_text.position;

        boolean isHovered = isMouseOverPosition(mouseX, mouseY, pos);
        boolean isSelected = isDragging && draggingType == ComponentType.PERCENTAGE_TEXT;

        int borderColor = isSelected ? COLOR_SELECTED : (isHovered ? COLOR_HOVER : COLOR_BORDER);
        renderComponentBorder(graphics, pos, borderColor);

        if (isHovered || isSelected) {
            String posInfo = Component.translatable("gui.epic_engine.loading_screen.component_percentage",
                    pos.x, pos.y).getString();
            graphics.drawString(Minecraft.getInstance().font, posInfo,
                    pos.x, Math.max(pos.y - 15, 5), 0xFFFFFF);
        }
    }

    private static void renderLoadingScreenCustomTextEditAreas(GuiGraphics graphics, int mouseX, int mouseY) {
        for (LoadingScreenLayoutData.CustomTextComponent text : loadingScreenLayout.custom_texts) {
            boolean isHovered = isMouseOverPosition(mouseX, mouseY, text.position);
            boolean isSelected = isDragging && draggingType == ComponentType.LOADING_CUSTOM_TEXT &&
                    text.id.equals(draggingComponentId);

            int borderColor = isSelected ? COLOR_SELECTED : (isHovered ? COLOR_HOVER : COLOR_BORDER);
            renderComponentBorder(graphics, text.position, borderColor);

            if (isHovered || isSelected) {
                String textContent = text.properties.text.isEmpty() ? text.id : text.properties.text;
                String textInfo = Component.translatable("gui.epic_engine.loading_screen.component_custom_text",
                        textContent, text.position.x, text.position.y).getString();
                graphics.drawString(Minecraft.getInstance().font, textInfo,
                        text.position.x, Math.max(text.position.y - 15, 5), 0xFFFFFF);
            }
        }
    }

    public static boolean handleMouseClick(int mouseX, int mouseY, int button) {
        LOGGER.info("[EPIC ENGINE]: Mouse click at ({}, {}) button: {}, editMode: {}, page: {}",
                mouseX, mouseY, button, isEditMode, currentPage);

        if (editToggleButton != null && editToggleButton.mouseClicked(mouseX, mouseY, button)) {
            LOGGER.info("[EPIC ENGINE]: Edit toggle button clicked");
            return true;
        }

        if (!isEditMode) {
            return false;
        }

        if (prevPageButton != null && prevPageButton.mouseClicked(mouseX, mouseY, button)) {
            LOGGER.info("[EPIC ENGINE]: Previous page button clicked");
            return true;
        }
        if (nextPageButton != null && nextPageButton.mouseClicked(mouseX, mouseY, button)) {
            LOGGER.info("[EPIC ENGINE]: Next page button clicked");
            return true;
        }
        if (saveLayoutButton != null && saveLayoutButton.mouseClicked(mouseX, mouseY, button)) {
            LOGGER.info("[EPIC ENGINE]: Save button clicked");
            return true;
        }

        if (currentPage == EditPage.MAIN_MENU) {
            return handleMainMenuClick(mouseX, mouseY, button);
        } else if (currentPage == EditPage.LOADING_SCREEN) {
            return handleLoadingScreenClick(mouseX, mouseY, button);
        }

        return false;
    }

    private static boolean handleMainMenuClick(int mouseX, int mouseY, int button) {
        if (button == 1) {
            LOGGER.info("[EPIC ENGINE]: Right click - stopping drag");
            stopDragging();
            return true;
        }

        if (button == 2) {
            LOGGER.info("[EPIC ENGINE]: Middle click - attempting to toggle button enabled state");
            return toggleButtonEnabled(mouseX, mouseY);
        }

        if (button == 0) {
            LOGGER.info("[EPIC ENGINE]: Left click - attempting to start drag");
            return startMainMenuDrag(mouseX, mouseY);
        }

        return false;
    }

    private static boolean handleLoadingScreenClick(int mouseX, int mouseY, int button) {
        if (button == 1) {
            LOGGER.info("[EPIC ENGINE]: Loading screen right click - stopping drag");
            stopDragging();
            return true;
        }

        if (button == 0) {
            LOGGER.info("[EPIC ENGINE]: Loading screen left click - attempting to start drag");
            return startLoadingScreenDrag(mouseX, mouseY);
        }

        return false;
    }

    private static boolean startMainMenuDrag(int mouseX, int mouseY) {
        LOGGER.info("[EPIC ENGINE]: Checking for draggable main menu components at ({}, {})", mouseX, mouseY);

        for (CustomMainMenuButton customButton : CustomButtonManager.getCustomButtons()) {
            if (isMouseOverWidget(mouseX, mouseY, customButton)) {
                LOGGER.info("[EPIC ENGINE]: Starting custom button drag: {}", customButton.getButtonId());
                MainMenuLayoutData.Position pos = new MainMenuLayoutData.Position(
                        customButton.getX(), customButton.getY(),
                        customButton.getWidth(), customButton.getHeight());
                startDragging(ComponentType.MENU_BUTTON, customButton.getButtonId(), mouseX, mouseY, pos);
                return true;
            }
        }

        if (EpicEngineCustomConfig.isMainMenuTitleEnabled() && mainMenuLayout != null &&
                isMouseOverPosition(mouseX, mouseY, mainMenuLayout.title_image.position)) {
            LOGGER.info("[EPIC ENGINE]: Starting title drag");
            startDragging(ComponentType.TITLE_IMAGE, "", mouseX, mouseY, mainMenuLayout.title_image.position);
            return true;
        }

        if (mainMenuLayout != null) {
            for (MainMenuLayoutData.CustomTextComponent text : mainMenuLayout.custom_texts) {
                if (isMouseOverPosition(mouseX, mouseY, text.position)) {
                    LOGGER.info("[EPIC ENGINE]: Starting custom text drag: {}", text.id);
                    startDragging(ComponentType.CUSTOM_TEXT, text.id, mouseX, mouseY, text.position);
                    return true;
                }
            }
        }

        LOGGER.info("[EPIC ENGINE]: No draggable main menu component found at ({}, {})", mouseX, mouseY);
        return false;
    }

    /**
     * Toggle button enabled state when middle-clicked
     */
    private static boolean toggleButtonEnabled(int mouseX, int mouseY) {
        LOGGER.info("[EPIC ENGINE]: Checking for buttons to toggle at ({}, {})", mouseX, mouseY);

        if (mainMenuLayout == null) {
            return false;
        }

        // Check all buttons in layout
        for (MainMenuLayoutData.ButtonComponent buttonComponent : mainMenuLayout.buttons) {
            // Skip external buttons
            if (buttonComponent.id.startsWith("external_")) {
                continue;
            }

            // Check if mouse is over this button's position
            if (isMouseOverPosition(mouseX, mouseY, buttonComponent.position)) {
                // Toggle enabled state
                buttonComponent.enabled = !buttonComponent.enabled;
                
                String status = buttonComponent.enabled ? "enabled" : "disabled";
                LOGGER.info("[EPIC ENGINE]: Toggled button '{}' to {}", buttonComponent.id, status);
                
                // Save layout immediately to persist changes
                saveCurrentLayout();
                
                return true;
            }
        }

        LOGGER.info("[EPIC ENGINE]: No button found at ({}, {}) to toggle", mouseX, mouseY);
        return false;
    }

    private static boolean startLoadingScreenDrag(int mouseX, int mouseY) {
        LOGGER.info("[EPIC ENGINE]: Checking for draggable loading components at ({}, {})", mouseX, mouseY);

        if (loadingScreenLayout == null) return false;

        // 检查进度条拖动 - 使用当前实际渲染位置
        if (loadingScreenLayout.progress_bar.enabled) {
            LoadingScreenLayoutData.Position currentProgressBarPos = getCurrentProgressBarPosition();
            if (isMouseOverPosition(mouseX, mouseY, currentProgressBarPos)) {
                LOGGER.info("[EPIC ENGINE]: Starting progress bar drag at current position ({}, {}) size {}x{}", 
                        currentProgressBarPos.x, currentProgressBarPos.y, currentProgressBarPos.width, currentProgressBarPos.height);
                startDragging(ComponentType.PROGRESS_BAR, "", mouseX, mouseY, currentProgressBarPos);
                return true;
            }
        }

        if (loadingScreenLayout.tip_text.enabled &&
                isMouseOverPosition(mouseX, mouseY, loadingScreenLayout.tip_text.position)) {
            LOGGER.info("[EPIC ENGINE]: Starting tip text drag");
            startDragging(ComponentType.TIP_TEXT, "", mouseX, mouseY, loadingScreenLayout.tip_text.position);
            return true;
        }

        if (loadingScreenLayout.percentage_text.enabled &&
                isMouseOverPosition(mouseX, mouseY, loadingScreenLayout.percentage_text.position)) {
            LOGGER.info("[EPIC ENGINE]: Starting percentage text drag");
            startDragging(ComponentType.PERCENTAGE_TEXT, "", mouseX, mouseY, loadingScreenLayout.percentage_text.position);
            return true;
        }

        for (LoadingScreenLayoutData.CustomTextComponent text : loadingScreenLayout.custom_texts) {
            if (isMouseOverPosition(mouseX, mouseY, text.position)) {
                LOGGER.info("[EPIC ENGINE]: Starting loading custom text drag: {}", text.id);
                startDragging(ComponentType.LOADING_CUSTOM_TEXT, text.id, mouseX, mouseY, text.position);
                return true;
            }
        }

        LOGGER.info("[EPIC ENGINE]: No draggable loading component found at ({}, {})", mouseX, mouseY);
        return false;
    }

    private static void startDragging(ComponentType type, String componentId, int mouseX, int mouseY,
                                      MainMenuLayoutData.Position position) {
        isDragging = true;
        draggingType = type;
        draggingComponentId = componentId;
        dragStartX = mouseX;
        dragStartY = mouseY;
        componentStartX = position.x;
        componentStartY = position.y;

        LOGGER.info("[EPIC ENGINE]: Started dragging {} '{}' from ({}, {})",
                type, componentId, position.x, position.y);
    }

    private static void startDragging(ComponentType type, String componentId, int mouseX, int mouseY,
                                      LoadingScreenLayoutData.Position position) {
        isDragging = true;
        draggingType = type;
        draggingComponentId = componentId;
        dragStartX = mouseX;
        dragStartY = mouseY;
        componentStartX = position.x;
        componentStartY = position.y;

        LOGGER.info("[EPIC ENGINE]: Started dragging {} '{}' from ({}, {})",
                type, componentId, position.x, position.y);
    }

    private static void stopDragging() {
        if (isDragging) {
            LOGGER.info("[EPIC ENGINE]: Stopped dragging {} '{}'", draggingType, draggingComponentId);
            
            // 如果是进度条拖动停止，记录最终位置
            if (draggingType == ComponentType.PROGRESS_BAR && loadingScreenLayout != null) {
                LOGGER.info("[EPIC ENGINE]: Progress bar final position: ({}, {}) manually positioned: {}", 
                        loadingScreenLayout.progress_bar.position.x, 
                        loadingScreenLayout.progress_bar.position.y,
                        progressBarManuallyPositioned);
            }
        }

        isDragging = false;
        draggingType = ComponentType.NONE;
        draggingComponentId = "";
    }

    private static void handleDragLogic(int mouseX, int mouseY) {
        if (!isDragging) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        long windowHandle = mc.getWindow().getWindow();
        int leftButtonState = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT);

        if (leftButtonState == GLFW.GLFW_RELEASE) {
            LOGGER.info("[EPIC ENGINE]: Mouse released, stopping drag");
            handleMouseRelease(mouseX, mouseY, 0);
            return;
        }

        handleMouseDrag(mouseX, mouseY);
    }

    public static boolean handleMouseDrag(int mouseX, int mouseY) {
        if (!isEditMode || !isDragging) {
            return false;
        }

        int deltaX = mouseX - dragStartX;
        int deltaY = mouseY - dragStartY;
        int newX = componentStartX + deltaX;
        int newY = componentStartY + deltaY;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            newX = Math.max(0, Math.min(newX, mc.screen.width - 50));
            newY = Math.max(0, Math.min(newY, mc.screen.height - 20));
        }

        updateComponentPosition(newX, newY);

        LOGGER.debug("[EPIC ENGINE]: Dragging {} to ({}, {})", draggingType, newX, newY);
        return true;
    }

    private static void updateComponentPosition(int newX, int newY) {
        switch (draggingType) {
            case TITLE_IMAGE:
                if (mainMenuLayout != null) {
                    mainMenuLayout.title_image.position.x = newX;
                    mainMenuLayout.title_image.position.y = newY;
                }
                break;

            case MENU_BUTTON:
                if (mainMenuLayout != null) {
                    MainMenuLayoutData.ButtonComponent button = mainMenuLayout.findButtonById(draggingComponentId);
                    if (button != null) {
                        button.position.x = newX;
                        button.position.y = newY;

                        CustomMainMenuButton customButton = CustomButtonManager.findButtonById(draggingComponentId);
                        if (customButton != null) {
                            customButton.setX(newX);
                            customButton.setY(newY);
                        }
                    }
                }
                break;

            case CUSTOM_TEXT:
                if (mainMenuLayout != null) {
                    MainMenuLayoutData.CustomTextComponent text = mainMenuLayout.findTextById(draggingComponentId);
                    if (text != null) {
                        text.position.x = newX;
                        text.position.y = newY;
                    }
                }
                break;

            case PROGRESS_BAR:
                if (loadingScreenLayout != null) {
                    loadingScreenLayout.progress_bar.position.x = newX;
                    loadingScreenLayout.progress_bar.position.y = newY;
                    // 标记进度条为手动定位
                    progressBarManuallyPositioned = true;
                    LOGGER.debug("[EPIC ENGINE]: Progress bar manually positioned to ({}, {})", newX, newY);
                }
                break;

            case TIP_TEXT:
                if (loadingScreenLayout != null) {
                    loadingScreenLayout.tip_text.position.x = newX;
                    loadingScreenLayout.tip_text.position.y = newY;
                }
                break;

            case PERCENTAGE_TEXT:
                if (loadingScreenLayout != null) {
                    loadingScreenLayout.percentage_text.position.x = newX;
                    loadingScreenLayout.percentage_text.position.y = newY;
                }
                break;

            case LOADING_CUSTOM_TEXT:
                if (loadingScreenLayout != null) {
                    LoadingScreenLayoutData.CustomTextComponent text = loadingScreenLayout.findTextById(draggingComponentId);
                    if (text != null) {
                        text.position.x = newX;
                        text.position.y = newY;
                    }
                }
                break;
        }
    }

    public static boolean handleMouseRelease(int mouseX, int mouseY, int button) {
        if (!isEditMode) {
            return false;
        }

        if (isDragging && button == 0) {
            LOGGER.info("[EPIC ENGINE]: Drag completed - {} '{}' moved to ({}, {})",
                    draggingType, draggingComponentId, mouseX, mouseY);
            stopDragging();
            return true;
        }

        return false;
    }

    public static boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEditMode) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            LOGGER.info("[EPIC ENGINE]: ESC pressed, exiting edit mode");
            toggleEditMode();
            return true;
        }

        return false;
    }

    private static boolean isMouseOverPosition(int mouseX, int mouseY, MainMenuLayoutData.Position pos) {
        int width = Math.max(pos.width, 50);
        int height = Math.max(pos.height, 20);
        return mouseX >= pos.x && mouseX <= pos.x + width &&
                mouseY >= pos.y && mouseY <= pos.y + height;
    }

    private static boolean isMouseOverPosition(int mouseX, int mouseY, LoadingScreenLayoutData.Position pos) {
        int width = Math.max(pos.width, 50);
        int height = Math.max(pos.height, 20);
        return mouseX >= pos.x && mouseX <= pos.x + width &&
                mouseY >= pos.y && mouseY <= pos.y + height;
    }

    private static boolean isMouseOverWidget(int mouseX, int mouseY, CustomMainMenuButton widget) {
        return mouseX >= widget.getX() && mouseX <= widget.getX() + widget.getWidth() &&
                mouseY >= widget.getY() && mouseY <= widget.getY() + widget.getHeight();
    }

    private static void renderComponentBorder(GuiGraphics graphics, MainMenuLayoutData.Position pos, int color) {
        int x1 = pos.x - 2;
        int y1 = pos.y - 2;
        int x2 = pos.x + Math.max(pos.width, 50) + 2;
        int y2 = pos.y + Math.max(pos.height, 20) + 2;

        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }

    private static void renderComponentBorder(GuiGraphics graphics, LoadingScreenLayoutData.Position pos, int color) {
        int x1 = pos.x - 2;
        int y1 = pos.y - 2;
        int x2 = pos.x + Math.max(pos.width, 50) + 2;
        int y2 = pos.y + Math.max(pos.height, 20) + 2;

        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }

    private static void renderWidgetBorder(GuiGraphics graphics, CustomMainMenuButton widget, int color) {
        int x1 = widget.getX() - 2;
        int y1 = widget.getY() - 2;
        int x2 = widget.getX() + widget.getWidth() + 2;
        int y2 = widget.getY() + widget.getHeight() + 2;

        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }

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
     * 获取进度条纹理宽度，与 UnifiedLoadingRenderer 保持一致
     */
    private static int getProgressBarTextureWidth() {
        try {
            // 从 UnifiedLoadingRenderer 获取实际纹理尺寸
            return UnifiedLoadingRenderer.getProgressBarTextureWidth();
        } catch (Exception e) {
            LOGGER.warn("[EPIC ENGINE]: Failed to get progress bar texture width, using default", e);
            return 400;
        }
    }

    /**
     * 获取进度条纹理高度，与 UnifiedLoadingRenderer 保持一致
     */
    private static int getProgressBarTextureHeight() {
        try {
            // 从 UnifiedLoadingRenderer 获取实际纹理尺寸
            return UnifiedLoadingRenderer.getProgressBarTextureHeight();
        } catch (Exception e) {
            LOGGER.warn("[EPIC ENGINE]: Failed to get progress bar texture height, using default", e);
            return 32;
        }
    }

    /**
     * 计算进度条的默认渲染位置和大小（未手动拖动时的位置）
     */
    private static LoadingScreenLayoutData.Position calculateDefaultProgressBarPosition() {
        if (loadingScreenLayout == null) {
            return new LoadingScreenLayoutData.Position(0, 0, 400, 32);
        }

        // 使用与渲染相同的逻辑计算位置和大小
        Minecraft mc = Minecraft.getInstance();
        com.mojang.blaze3d.platform.Window window = mc.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        // 获取实际纹理尺寸
        int progressBarImageWidth = getProgressBarTextureWidth();
        int progressBarImageHeight = getProgressBarTextureHeight();

        // 使用原始图片尺寸
        int barDisplayWidth = progressBarImageWidth;
        int barDisplayHeight = progressBarImageHeight;

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

        return new LoadingScreenLayoutData.Position(barX, barY, barDisplayWidth, barDisplayHeight);
    }

    /**
     * 获取进度条当前实际的渲染位置和大小
     * 考虑手动拖动状态，与 renderPreviewProgressBar 完全一致
     */
    private static LoadingScreenLayoutData.Position getCurrentProgressBarPosition() {
        if (loadingScreenLayout == null) {
            return new LoadingScreenLayoutData.Position(0, 0, 400, 32);
        }

        if (progressBarManuallyPositioned) {
            // 如果进度条被手动拖动过，使用手动位置，但确保尺寸正确
            LoadingScreenLayoutData.Position defaultSize = calculateDefaultProgressBarPosition();
            return new LoadingScreenLayoutData.Position(
                loadingScreenLayout.progress_bar.position.x,
                loadingScreenLayout.progress_bar.position.y,
                defaultSize.width,  // 使用计算出的实际尺寸
                defaultSize.height  // 使用计算出的实际尺寸
            );
        } else {
            // 如果进度条还没有被手动拖动，使用计算出的默认位置
            return calculateDefaultProgressBarPosition();
        }
    }

    private static boolean shouldShowEditor() {
        return EpicEngineCustomConfig.MAIN_MENU_MODULE_ENABLED.get();
    }

    private static java.io.File getLoadingLayoutFile() {
        return EpicEngineCustomConfig.getCustomDir().resolve("loading_screen_layout.json").toFile();
    }

    public static EditPage getCurrentPage() {
        return currentPage;
    }

    public static boolean isEditMode() {
        return isEditMode && shouldShowEditor();
    }

    public static boolean isMainMenuPage() {
        return currentPage == EditPage.MAIN_MENU;
    }

    public static boolean isLoadingScreenPage() {
        return currentPage == EditPage.LOADING_SCREEN;
    }

    public static boolean isDragging() {
        return isDragging;
    }

    public static MainMenuLayoutData getCurrentMainMenuLayout() {
        return mainMenuLayout;
    }

    public static LoadingScreenLayoutData getCurrentLoadingScreenLayout() {
        return loadingScreenLayout;
    }

    public static void switchToPage(EditPage page) {
        if (page == currentPage) {
            return;
        }

        currentPage = page;
        stopDragging();

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            updatePageIndicatorButton(mc.screen.width / 2, 10);
            updateButtonStates();
            initializeCurrentPageSystems();
        }

        LOGGER.info("[EPIC ENGINE]: Forced switch to page: {}", page);
    }

    public static void reset() {
        isEditMode = false;
        currentPage = EditPage.MAIN_MENU;
        mainMenuLayout = null;
        loadingScreenLayout = null;
        stopDragging();
        clearEditModeButtons();
        editToggleButton = null;
        
        // 重置进度条手动定位标志
        progressBarManuallyPositioned = false;

        LOGGER.info("[EPIC ENGINE]: Unified layout editor reset");
    }

    public static void refreshLayoutData() {
        mainMenuLayout = null;
        loadingScreenLayout = null;
        loadLayoutData();
        LOGGER.info("[EPIC ENGINE]: Layout data refreshed");
    }

    public static boolean hasUnsavedChanges() {
        return isEditMode;
    }

    public static String getEditorStatusInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Unified Layout Editor Status:\n");
        info.append("- Edit Mode: ").append(isEditMode).append("\n");
        info.append("- Current Page: ").append(currentPage).append("\n");
        info.append("- Dragging: ").append(isDragging).append("\n");
        if (isDragging) {
            info.append("- Dragging Type: ").append(draggingType).append("\n");
            info.append("- Dragging Component: ").append(draggingComponentId).append("\n");
        }
        info.append("- Main Menu Layout Loaded: ").append(mainMenuLayout != null).append("\n");
        info.append("- Loading Screen Layout Loaded: ").append(loadingScreenLayout != null).append("\n");
        return info.toString();
    }
}