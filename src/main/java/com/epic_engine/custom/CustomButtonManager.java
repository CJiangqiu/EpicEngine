package com.epic_engine.custom;

import com.epic_engine.API.custom.*;
import com.epic_engine.config.EpicEngineCustomConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomButtonManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<VanillaButtonInfo> vanillaButtons = new ArrayList<>();
    private static final List<CustomMainMenuButton> customButtons = new ArrayList<>();
    private static final List<ExternalMainMenuButton> externalButtons = new ArrayList<>();
    private static final Map<String, VanillaButtonInfo> buttonMapping = new HashMap<>();

    private static boolean initialized = false;
    private static TitleScreen currentTitleScreen = null;

    public static class VanillaButtonInfo {
        public final String id;
        public final String displayText;
        public final AbstractWidget originalWidget;
        public final Button.OnPress onPress;
        public final int originalX, originalY, originalWidth, originalHeight;

        public VanillaButtonInfo(String id, String displayText, AbstractWidget widget,
                                 Button.OnPress onPress, int x, int y, int width, int height) {
            this.id = id;
            this.displayText = displayText;
            this.originalWidget = widget;
            this.onPress = onPress;
            this.originalX = x;
            this.originalY = y;
            this.originalWidth = width;
            this.originalHeight = height;
        }

        public void triggerAction() {
            if (onPress != null && originalWidget instanceof Button) {
                try {
                    onPress.onPress((Button) originalWidget);
                    LOGGER.debug("[EPIC ENGINE]: Triggered vanilla action for button: {}", id);
                } catch (Exception e) {
                    LOGGER.error("[EPIC ENGINE]: Failed to trigger vanilla action for button: {}", id, e);
                }
            }
        }
    }

    /**
     * External button implementation for API integration
     */
    public static class ExternalMainMenuButton extends CustomMainMenuButton {
        private final IExternalButton externalButton;
        private final String sourceModId;

        public ExternalMainMenuButton(String buttonId, int x, int y, int width, int height,
                                      Component message, ButtonAction action,
                                      IExternalButton externalButton, String sourceModId,
                                      MainMenuLayoutData.ButtonComponent buttonData) {
            super(buttonId, x, y, width, height, message, action, buttonData);
            this.externalButton = externalButton;
            this.sourceModId = sourceModId;
        }

        @Override
        protected void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            // Update enabled state from external button
            this.active = externalButton.isEnabled();

            // Use custom texture if provided
            net.minecraft.resources.ResourceLocation customTexture = externalButton.getCustomTexture();
            if (customTexture != null) {
                renderCustomExternalTexture(guiGraphics, customTexture, this.isHovered());
            } else {
                // Use default rendering
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
            }
        }

        private void renderCustomExternalTexture(net.minecraft.client.gui.GuiGraphics guiGraphics,
                                                 net.minecraft.resources.ResourceLocation texture, boolean isHovered) {
            com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

            float alpha = this.active ? 1.0F : 0.5F;
            float brightness = (isHovered) ? 1.1F : 1.0F;
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);

            guiGraphics.blit(texture,
                    this.getX(), this.getY(),
                    this.getWidth(), this.getHeight(),
                    0.0F, 0.0F,
                    this.getWidth(), this.getHeight(),
                    this.getWidth(), this.getHeight());

            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        public Component getExternalTooltip() {
            return externalButton.getTooltip();
        }

        @Override
        public net.minecraft.client.gui.components.Tooltip getTooltip() {
            Component tooltipComponent = externalButton.getTooltip();
            if (tooltipComponent != null) {
                return net.minecraft.client.gui.components.Tooltip.create(tooltipComponent);
            }
            return super.getTooltip();
        }

        public String getSourceModId() {
            return sourceModId;
        }

        public IExternalButton getExternalButton() {
            return externalButton;
        }
    }

    public static void initialize(TitleScreen titleScreen, MainMenuLayoutData layoutData) {
        if (initialized && currentTitleScreen == titleScreen) {
            LOGGER.debug("[EPIC ENGINE]: Button manager already initialized for this screen");
            return;
        }

        reset();
        currentTitleScreen = titleScreen;

        LOGGER.info("[EPIC ENGINE]: Initializing custom button system");

        EpicEngineI18n.initialize();

        scanVanillaButtons(titleScreen);
        completelyRemoveVanillaButtons(titleScreen);
        createCustomButtons(titleScreen, layoutData);

        // Create external mod components if enabled
        if (EpicEngineCustomConfig.isExternalModComponentsEnabled()) {
            createExternalModComponents(titleScreen, layoutData);
        }

        initialized = true;
        LOGGER.info("[EPIC ENGINE]: Custom button system initialized with {} vanilla buttons, {} external buttons",
                customButtons.size(), externalButtons.size());
    }

    private static void scanVanillaButtons(TitleScreen titleScreen) {
        vanillaButtons.clear();
        buttonMapping.clear();

        List<AbstractWidget> widgets = new ArrayList<>();
        titleScreen.children().forEach(child -> {
            if (child instanceof AbstractWidget) {
                widgets.add((AbstractWidget) child);
            }
        });

        int buttonIndex = 1;
        for (AbstractWidget widget : widgets) {
            if (widget instanceof Button) {
                Button button = (Button) widget;
                String buttonId = identifyButtonType(button);

                if (!buttonId.equals("unknown")) {
                    VanillaButtonInfo info = new VanillaButtonInfo(
                            buttonId,
                            button.getMessage().getString(),
                            widget,
                            extractButtonAction(button),
                            widget.getX(), widget.getY(),
                            widget.getWidth(), widget.getHeight()
                    );

                    vanillaButtons.add(info);
                    buttonMapping.put(buttonId, info);

                    LOGGER.info("[EPIC ENGINE]: Found vanilla button: {} at ({}, {}) - '{}'",
                            buttonId, widget.getX(), widget.getY(), button.getMessage().getString());
                    buttonIndex++;
                }
            }
        }
    }

    private static String identifyButtonType(Button button) {
        String message = button.getMessage().getString().toLowerCase();

        if (message.contains("singleplayer") || message.contains("单人")) {
            return "singleplayer";
        } else if (message.contains("multiplayer") || message.contains("多人")) {
            return "multiplayer";
        } else if (message.contains("realms") || message.contains("online")) {
            return "realms";
        } else if (message.contains("options") || message.contains("选项")) {
            return "options";
        } else if (message.contains("quit") || message.contains("退出")) {
            return "quit";
        } else if (message.contains("mods") || message.contains("mod")) {
            return "mods";
        } else if (message.contains("language") || message.contains("语言")) {
            return "language";
        } else if (message.contains("accessibility") || message.contains("辅助")) {
            return "accessibility";
        }

        LOGGER.debug("[EPIC ENGINE]: Unknown button type: '{}'", message);
        return "unknown";
    }

    private static Button.OnPress extractButtonAction(Button button) {
        try {
            java.lang.reflect.Field onPressField = Button.class.getDeclaredField("onPress");
            onPressField.setAccessible(true);
            return (Button.OnPress) onPressField.get(button);
        } catch (Exception e) {
            LOGGER.warn("[EPIC ENGINE]: Failed to extract button action for: {}",
                    button.getMessage().getString(), e);
            return null;
        }
    }

    private static void completelyRemoveVanillaButtons(TitleScreen titleScreen) {
        List<AbstractWidget> widgetsToRemove = new ArrayList<>();

        for (VanillaButtonInfo info : vanillaButtons) {
            widgetsToRemove.add(info.originalWidget);
        }

        for (AbstractWidget widget : widgetsToRemove) {
            titleScreen.children().remove(widget);
            titleScreen.renderables.remove(widget);
            LOGGER.info("[EPIC ENGINE]: Completely removed vanilla button: '{}'",
                    ((Button)widget).getMessage().getString());
        }

        LOGGER.info("[EPIC ENGINE]: Removed {} vanilla buttons from screen", widgetsToRemove.size());
    }

    private static void createCustomButtons(TitleScreen titleScreen, MainMenuLayoutData layoutData) {
        customButtons.clear();

        if (layoutData.buttons.isEmpty()) {
            createDefaultButtonLayout(layoutData, titleScreen.width, titleScreen.height);
        }

        for (MainMenuLayoutData.ButtonComponent buttonComponent : layoutData.buttons) {
            // Skip external buttons - they will be handled separately
            if (buttonComponent.id.startsWith("external_")) {
                continue;
            }

            // Skip disabled buttons
            if (!buttonComponent.enabled) {
                LOGGER.debug("[EPIC ENGINE]: Skipping disabled button: {}", buttonComponent.id);
                continue;
            }

            String baseButtonId = extractBaseButtonId(buttonComponent.id);
            VanillaButtonInfo vanillaInfo = buttonMapping.get(baseButtonId);

            if (vanillaInfo == null) {
                LOGGER.warn("[EPIC ENGINE]: No vanilla button found for: {}", baseButtonId);
                continue;
            }

            String displayText = getButtonDisplayText(buttonComponent, vanillaInfo);

            CustomMainMenuButton customButton = new CustomMainMenuButton(
                    buttonComponent.id,
                    buttonComponent.position.x,
                    buttonComponent.position.y,
                    buttonComponent.position.width,
                    buttonComponent.position.height,
                    Component.literal(displayText),
                    () -> vanillaInfo.triggerAction(),
                    buttonComponent
            );

            titleScreen.addRenderableWidget(customButton);
            customButtons.add(customButton);

            LOGGER.info("[EPIC ENGINE]: Created custom button: {} at ({}, {})",
                    baseButtonId, buttonComponent.position.x, buttonComponent.position.y);
        }
    }

    private static void createExternalModComponents(TitleScreen titleScreen, MainMenuLayoutData layoutData) {
        externalButtons.clear();

        List<IMainMenuComponentProvider> providers = MainMenuAPIRegistry.getEnabledProviders();
        if (providers.isEmpty()) {
            LOGGER.debug("[EPIC ENGINE]: No external component providers found");
            return;
        }

        LOGGER.info("[EPIC ENGINE]: Creating external mod components from {} providers", providers.size());

        int externalButtonIndex = 1000; // Start from 1000 to avoid conflicts with vanilla buttons

        for (IMainMenuComponentProvider provider : providers) {
            try {
                String modId = provider.getModId();
                LOGGER.debug("[EPIC ENGINE]: Processing components from mod: {}", modId);

                // Create external button
                IExternalButton externalButton = provider.createButton(layoutData);
                if (externalButton != null) {
                    createExternalButton(titleScreen, layoutData, externalButton, modId, externalButtonIndex++);
                }

                // Create external texts
                List<IExternalText> texts = provider.createTexts(layoutData);
                for (IExternalText text : texts) {
                    createExternalText(layoutData, text, modId);
                }

            } catch (Exception e) {
                LOGGER.error("[EPIC ENGINE]: Error creating components for mod: {}",
                        provider.getModId(), e);
            }
        }

        LOGGER.info("[EPIC ENGINE]: Created {} external buttons", externalButtons.size());
    }

    private static void createExternalButton(TitleScreen titleScreen, MainMenuLayoutData layoutData,
                                             IExternalButton externalButton, String modId, int index) {

        String buttonId = "external_" + externalButton.getId();
        MainMenuLayoutData.ButtonComponent existingLayout = layoutData.findButtonById(buttonId);

        int x, y, width, height;
        if (existingLayout != null) {
            // Use saved layout
            x = existingLayout.position.x;
            y = existingLayout.position.y;
            width = existingLayout.position.width;
            height = existingLayout.position.height;
            LOGGER.debug("[EPIC ENGINE]: Using saved layout for external button: {} at ({}, {})",
                    buttonId, x, y);
        } else {
            // Calculate position based on preferred placement or use automatic positioning
            ComponentPlacement placement = externalButton.getPreferredPlacement();
            if (placement != null) {
                CalculatedPosition pos = calculatePlacement(placement, layoutData);
                x = pos.x;
                y = pos.y;
                width = pos.width;
                height = pos.height;
            } else {
                // Automatic layout: place below vanilla buttons
                x = titleScreen.width / 2 - 100;
                y = titleScreen.height / 4 + 48 + 24 * vanillaButtons.size() + (index - 1000) * 24;
                width = 200;
                height = 20;
            }

            // Save to layout data for future use
            MainMenuLayoutData.ButtonComponent newButton = layoutData.addButton(
                    buttonId, index, externalButton.getDisplayText().getString(),
                    x, y, width, height);
            newButton.properties.custom_text = externalButton.getDisplayText().getString();

            LOGGER.debug("[EPIC ENGINE]: Created new layout for external button: {} at ({}, {})",
                    buttonId, x, y);
        }

        // Create external button component
        ExternalMainMenuButton customButton = new ExternalMainMenuButton(
                buttonId, x, y, width, height,
                externalButton.getDisplayText(),
                externalButton::onClick,
                externalButton,
                modId,
                existingLayout
        );

        titleScreen.addRenderableWidget(customButton);
        externalButtons.add(customButton);

        LOGGER.info("[EPIC ENGINE]: Created external button '{}' from mod '{}' at ({}, {})",
                externalButton.getId(), modId, x, y);
    }

    private static void createExternalText(MainMenuLayoutData layoutData, IExternalText externalText, String modId) {
        String textId = "external_text_" + externalText.getId();
        MainMenuLayoutData.CustomTextComponent existingText = layoutData.findTextById(textId);

        if (existingText != null) {
            // Update existing text properties
            existingText.properties.text = externalText.getDisplayText().getString();
            existingText.properties.color = String.format("#%06X", externalText.getColor() & 0xFFFFFF);
            existingText.properties.font_scale = externalText.getFontScale();
            existingText.properties.shadow = externalText.hasShadow();
        } else {
            // Create new text component
            ComponentPlacement placement = externalText.getPreferredPlacement();
            int x, y;
            if (placement != null) {
                CalculatedPosition pos = calculatePlacement(placement, layoutData);
                x = pos.x;
                y = pos.y;
            } else {
                // Default position: top-right corner
                x = layoutData.screen_resolution.width - 200;
                y = 50;
            }

            layoutData.addCustomText(textId, externalText.getDisplayText().getString(),
                    x, y, String.format("#%06X", externalText.getColor() & 0xFFFFFF), externalText.getFontScale());
        }

        LOGGER.info("[EPIC ENGINE]: Registered external text '{}' from mod '{}'",
                externalText.getId(), modId);
    }

    private static class CalculatedPosition {
        final int x, y, width, height;

        CalculatedPosition(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static CalculatedPosition calculatePlacement(ComponentPlacement placement, MainMenuLayoutData layoutData) {
        int screenWidth = layoutData.screen_resolution.width;
        int screenHeight = layoutData.screen_resolution.height;

        int x, y;
        switch (placement.type) {
            case ABSOLUTE:
                x = placement.x;
                y = placement.y;
                break;

            case RELATIVE_TO_CENTER:
                x = screenWidth / 2 + placement.x;
                y = screenHeight / 2 + placement.y;
                break;

            case BELOW_BUTTONS:
                x = screenWidth / 2 - placement.width / 2;
                y = screenHeight / 4 + 48 + 24 * vanillaButtons.size() + placement.y;
                break;

            case ABOVE_BUTTONS:
                x = screenWidth / 2 - placement.width / 2;
                y = screenHeight / 4 + 48 - placement.height - 10 + placement.y;
                break;

            case LEFT_SIDE:
                x = placement.x;
                y = placement.y;
                break;

            case RIGHT_SIDE:
                x = screenWidth - placement.width + placement.x;
                y = placement.y;
                break;

            default:
                x = placement.x;
                y = placement.y;
                break;
        }

        // Ensure the component stays within screen bounds
        x = Math.max(0, Math.min(x, screenWidth - placement.width));
        y = Math.max(0, Math.min(y, screenHeight - placement.height));

        return new CalculatedPosition(x, y, placement.width, placement.height);
    }

    private static String getButtonDisplayText(MainMenuLayoutData.ButtonComponent buttonComponent,
                                               VanillaButtonInfo vanillaInfo) {
        if (buttonComponent.properties.custom_text != null && !buttonComponent.properties.custom_text.isEmpty()) {
            return buttonComponent.properties.custom_text;
        }

        String translationKey = "epic_engine.button." + extractBaseButtonId(buttonComponent.id);
        return EpicEngineI18n.translate(translationKey, vanillaInfo.displayText);
    }

    private static String extractBaseButtonId(String fullId) {
        if (fullId.endsWith("_button")) {
            return fullId.substring(0, fullId.length() - "_button".length());
        }
        return fullId;
    }

    public static void createDefaultButtonLayout(MainMenuLayoutData layoutData, int screenWidth, int screenHeight) {
        LOGGER.info("[EPIC ENGINE]: Creating default button layout for {}x{}", screenWidth, screenHeight);

        layoutData.buttons.clear();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = screenWidth / 2 - buttonWidth / 2;
        int startY = screenHeight / 4 + 48;
        int buttonSpacing = 24;

        int index = 1;
        int currentY = startY;

        for (VanillaButtonInfo vanillaInfo : vanillaButtons) {
            String translationKey = "epic_engine.button." + vanillaInfo.id;

            MainMenuLayoutData.ButtonComponent button = layoutData.addButton(
                    vanillaInfo.id + "_button", index,
                    translationKey,
                    centerX, currentY, buttonWidth, buttonHeight
            );

            button.properties.texture_scale = 1.0f;
            button.properties.show_text_over_texture = true;

            currentY += buttonSpacing;
            index++;

            LOGGER.debug("[EPIC ENGINE]: Added default button: {} at ({}, {}) with translation key: {}",
                    vanillaInfo.id, centerX, currentY - buttonSpacing, translationKey);
        }

        LOGGER.info("[EPIC ENGINE]: Created default layout with {} buttons", layoutData.buttons.size());
    }

    public static void clearButtons() {
        customButtons.clear();
        externalButtons.clear();
        LOGGER.debug("[EPIC ENGINE]: Cleared all custom buttons");
    }

    public static List<CustomMainMenuButton> getCustomButtons() {
        return new ArrayList<>(customButtons);
    }

    public static List<ExternalMainMenuButton> getExternalButtons() {
        return new ArrayList<>(externalButtons);
    }

    public static List<CustomMainMenuButton> getAllButtons() {
        List<CustomMainMenuButton> allButtons = new ArrayList<>(customButtons);
        allButtons.addAll(externalButtons);
        return allButtons;
    }

    public static CustomMainMenuButton findButtonById(String buttonId) {
        // Search in vanilla buttons first
        CustomMainMenuButton button = customButtons.stream()
                .filter(b -> b.getButtonId().equals(buttonId))
                .findFirst()
                .orElse(null);

        // If not found, search in external buttons
        if (button == null) {
            button = externalButtons.stream()
                    .filter(b -> b.getButtonId().equals(buttonId))
                    .findFirst()
                    .orElse(null);
        }

        return button;
    }

    public static void updateButtonPosition(String buttonId, int x, int y) {
        CustomMainMenuButton button = findButtonById(buttonId);
        if (button != null) {
            button.updatePosition(x, y);
            LOGGER.debug("[EPIC ENGINE]: Updated button {} position to ({}, {})", buttonId, x, y);
        }
    }

    // 修改这个方法以使用UnifiedLayoutEditor
    public static boolean handleButtonClick(int mouseX, int mouseY, int button) {
        if (!UnifiedLayoutEditor.isEditMode()) {
            return false;
        }

        // Check all buttons (vanilla and external)
        for (CustomMainMenuButton customButton : getAllButtons()) {
            if (customButton.isClickedInEditMode(mouseX, mouseY)) {
                LOGGER.info("[EPIC ENGINE]: Button clicked in edit mode: {}", customButton.getButtonId());
                // 不再调用MainMenuLayoutEditor.startDragIfPossible，改为返回false让UnifiedLayoutEditor处理
                return false; // 让UnifiedLayoutEditor处理拖拽
            }
        }

        return false;
    }

    public static void reset() {
        clearButtons();
        vanillaButtons.clear();
        buttonMapping.clear();
        initialized = false;
        currentTitleScreen = null;
        LOGGER.info("[EPIC ENGINE]: Custom button manager reset");
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static List<VanillaButtonInfo> getVanillaButtons() {
        return new ArrayList<>(vanillaButtons);
    }

    public static VanillaButtonInfo getVanillaButtonInfo(String buttonId) {
        return buttonMapping.get(buttonId);
    }

    /**
     * Get external buttons from a specific mod
     * @param modId The mod ID to filter by
     * @return List of external buttons from the specified mod
     */
    public static List<ExternalMainMenuButton> getExternalButtonsFromMod(String modId) {
        return externalButtons.stream()
                .filter(button -> modId.equals(button.getSourceModId()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get count of external components
     * @return Number of external buttons registered
     */
    public static int getExternalComponentCount() {
        return externalButtons.size();
    }

    /**
     * Check if external components are currently loaded
     * @return true if any external components are present
     */
    public static boolean hasExternalComponents() {
        return !externalButtons.isEmpty();
    }
}