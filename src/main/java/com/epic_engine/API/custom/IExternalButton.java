package com.epic_engine.API.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;

/**
 * External button interface for main menu integration.
 * Represents a custom button that can be added to the main menu by external mods.
 */
public interface IExternalButton {

    /**
     * Get the unique identifier for this button.
     * Recommended format: "modid_buttonname" to avoid conflicts.
     * @return Unique button identifier
     */
    String getId();

    /**
     * Get the display text for this button.
     * @return Text component to display on the button
     */
    Component getDisplayText();

    /**
     * Handle button click events.
     * This method will be called when the player clicks the button.
     */
    void onClick();

    /**
     * Get custom texture for the button (optional).
     * If null is returned, the default button style will be used.
     * @return Resource location of the custom texture, or null for default style
     */
    @Nullable
    default ResourceLocation getCustomTexture() {
        return null;
    }

    /**
     * Get preferred placement for this button (optional).
     * If null is returned, Epic Engine will automatically place the button.
     * @return Preferred placement configuration, or null for automatic placement
     */
    @Nullable
    default ComponentPlacement getPreferredPlacement() {
        return null;
    }

    /**
     * Check if this button should be enabled.
     * Disabled buttons will appear grayed out and won't respond to clicks.
     * @return true if the button should be enabled, false if disabled
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Get tooltip text for this button (optional).
     * @return Tooltip component, or null if no tooltip is needed
     */
    @Nullable
    default Component getTooltip() {
        return null;
    }
}