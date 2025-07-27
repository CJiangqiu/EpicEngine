package com.epic_engine.custom;

import com.epic_engine.config.EpicEngineCustomConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class CustomMainMenuButton extends AbstractWidget {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");

    private final String buttonId;
    private final ButtonAction action;
    private ResourceLocation customTexture;
    private ResourceLocation hoverTexture;
    private final MainMenuLayoutData.ButtonComponent buttonData;
    private boolean hasCustomTexture = false;
    private int originalTextureWidth = 0;
    private int originalTextureHeight = 0;

    private boolean isPressed = false;
    private int textColor = 0xFFFFFF;
    private int hoverTextColor = 0xFFFF00;
    private int disabledTextColor = 0x808080;

    public interface ButtonAction {
        void execute();
    }

    public CustomMainMenuButton(String buttonId, int x, int y, int width, int height,
                                Component message, ButtonAction action,
                                MainMenuLayoutData.ButtonComponent buttonData) {
        super(x, y, width, height, message);
        this.buttonId = buttonId;
        this.action = action;
        this.buttonData = buttonData;

        if (buttonData != null && buttonData.properties.text_color != null) {
            this.textColor = parseColor(buttonData.properties.text_color);
        }

        loadCustomTexture();
    }

    private void loadCustomTexture() {
        hasCustomTexture = false;
        originalTextureWidth = 0;
        originalTextureHeight = 0;

        try {
            if (buttonData != null && buttonData.button_index > 0) {
                String prefix = EpicEngineCustomConfig.getMainMenuButtonPrefix();
                String fileName = prefix + buttonData.button_index + ".png";
                File textureFile = EpicEngineCustomConfig.getTextureFile(fileName);

                if (textureFile.exists() && textureFile.isFile()) {
                    try (InputStream stream = Files.newInputStream(textureFile.toPath())) {
                        NativeImage image = NativeImage.read(stream);
                        this.originalTextureWidth = image.getWidth();
                        this.originalTextureHeight = image.getHeight();

                        this.customTexture = new ResourceLocation("epic_engine", "custom_button_" + buttonData.button_index);
                        this.hasCustomTexture = true;

                        LOGGER.info("[EPIC ENGINE]: Loaded custom texture: {} ({}x{}, scale: {})",
                                fileName, originalTextureWidth, originalTextureHeight, getTextureScale());
                        return;
                    } catch (IOException e) {
                        LOGGER.error("[EPIC ENGINE]: Failed to read texture: {}", fileName, e);
                    }
                }

                LOGGER.debug("[EPIC ENGINE]: No custom texture found for button: {} ({}), using vanilla style",
                        buttonId, fileName);
            }
        } catch (Exception e) {
            LOGGER.warn("[EPIC ENGINE]: Failed to load custom texture for button: {}, using vanilla style", buttonId, e);
        }

        this.hasCustomTexture = false;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        boolean isHovered = this.isHovered();

        if (hasCustomTexture && customTexture != null) {
            renderCustomTexture(guiGraphics, isHovered);
        } else {
            renderVanillaStyle(guiGraphics, isHovered);
        }

        renderButtonText(guiGraphics, isHovered);

        if (UnifiedLayoutEditor.isEditMode()) {
            renderEditBorder(guiGraphics, isHovered);
        }
    }

    private void renderCustomTexture(GuiGraphics guiGraphics, boolean isHovered) {
        ResourceLocation texture = (isHovered && hoverTexture != null) ? hoverTexture : customTexture;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float alpha = this.active ? 1.0F : 0.5F;
        float brightness = (isHovered || isPressed) ? 1.1F : 1.0F;
        RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);

        float scale = getTextureScale();

        int scaledWidth = (int)(originalTextureWidth * scale);
        int scaledHeight = (int)(originalTextureHeight * scale);

        int centerX = this.getX() + (this.getWidth() - scaledWidth) / 2;
        int centerY = this.getY() + (this.getHeight() - scaledHeight) / 2;

        guiGraphics.blit(texture,
                centerX, centerY,
                scaledWidth, scaledHeight,
                0.0F, 0.0F,
                originalTextureWidth, originalTextureHeight,
                originalTextureWidth, originalTextureHeight);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private float getTextureScale() {
        if (buttonData != null) {
            return buttonData.properties.texture_scale;
        }
        return 1.0f;
    }

    private void renderVanillaStyle(GuiGraphics guiGraphics, boolean isHovered) {
        int u = 0;
        int v = 46;

        if (!this.active) {
            v = 46;
        } else if (isHovered || isPressed) {
            v = 86;
        } else {
            v = 66;
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();

        guiGraphics.blitNineSliced(WIDGETS_LOCATION, x, y, width, height, 20, 4, 200, 20, u, v);
    }

    private void renderButtonText(GuiGraphics guiGraphics, boolean isHovered) {
        String text = getDisplayText();
        if (text.isEmpty()) return;

        int color;
        if (!this.active) {
            color = 0xA0A0A0;
        } else if (isHovered) {
            color = 0xFFFFA0;
        } else {
            color = 0xFFFFFF;
        }

        int textWidth = Minecraft.getInstance().font.width(text);
        int textX = this.getX() + (this.getWidth() - textWidth) / 2;
        int textY = this.getY() + (this.getHeight() - 8) / 2;

        if (!hasCustomTexture || buttonData.properties.show_text_over_texture) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, text,
                    this.getX() + this.getWidth() / 2, textY, color);
        }
    }

    private String getDisplayText() {
        String customText = "";
        if (buttonData != null && buttonData.properties.custom_text != null && !buttonData.properties.custom_text.isEmpty()) {
            customText = buttonData.properties.custom_text;
        } else {
            customText = this.getMessage().getString();
        }

        return EpicEngineI18n.getDisplayText(customText);
    }

    private void renderEditBorder(GuiGraphics guiGraphics, boolean isHovered) {
        int borderColor = isHovered ? 0xFF00FF00 : 0x8000FF00;

        int x = this.getX() - 2;
        int y = this.getY() - 2;
        int w = this.getWidth() + 4;
        int h = this.getHeight() + 4;

        guiGraphics.fill(x, y, x + w, y + 1, borderColor);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, borderColor);
        guiGraphics.fill(x, y, x + 1, y + h, borderColor);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, borderColor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (UnifiedLayoutEditor.isEditMode()) {
            return;
        }

        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        if (action != null && this.active) {
            try {
                action.execute();
                LOGGER.info("[EPIC ENGINE]: Executed action for button: {}", buttonId);
            } catch (Exception e) {
                LOGGER.error("[EPIC ENGINE]: Error executing action for button: {}", buttonId, e);
            }
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        this.isPressed = false;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    public void updatePosition(int x, int y) {
        this.setX(x);
        this.setY(y);

        if (buttonData != null) {
            buttonData.position.x = x;
            buttonData.position.y = y;
        }
    }

    public void updateSize(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);

        if (buttonData != null) {
            buttonData.position.width = width;
            buttonData.position.height = height;
        }
    }

    public String getButtonId() {
        return buttonId;
    }

    public MainMenuLayoutData.ButtonComponent getButtonData() {
        return buttonData;
    }

    private int parseColor(String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                return Integer.parseInt(colorStr.substring(1), 16);
            }
            return Integer.parseInt(colorStr, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    public boolean isClickedInEditMode(int mouseX, int mouseY) {
        return UnifiedLayoutEditor.isEditMode() &&
                mouseX >= this.getX() && mouseX < this.getX() + this.getWidth() &&
                mouseY >= this.getY() && mouseY < this.getY() + this.getHeight();
    }

    public boolean hasCustomTexture() {
        return hasCustomTexture;
    }

    public void refreshTexture() {
        loadCustomTexture();
    }
}