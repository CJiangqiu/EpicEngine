package com.epic_engine.API.custom;

import net.minecraft.network.chat.Component;
import javax.annotation.Nullable;

/**
 * External text interface for main menu integration.
 * Represents custom text that can be displayed on the main menu by external mods.
 */
public interface IExternalText {

    /**
     * Get the unique identifier for this text component.
     * @return Unique text identifier
     */
    String getId();

    /**
     * Get the text content to display.
     * @return Text component to display
     */
    Component getDisplayText();

    /**
     * Get the color of the text in hexadecimal format.
     * @return Color value (e.g., 0xFFFFFF for white)
     */
    default int getColor() {
        return 0xFFFFFF;
    }

    /**
     * Get the font scale multiplier for this text.
     * @return Scale factor (1.0f = normal size, 2.0f = double size, etc.)
     */
    default float getFontScale() {
        return 1.0f;
    }

    /**
     * Check if this text should render with a shadow.
     * @return true to render with shadow, false for no shadow
     */
    default boolean hasShadow() {
        return true;
    }

    /**
     * Get preferred placement for this text (optional).
     * If null is returned, Epic Engine will automatically place the text.
     * @return Preferred placement configuration, or null for automatic placement
     */
    @Nullable
    default ComponentPlacement getPreferredPlacement() {
        return null;
    }
}