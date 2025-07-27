package com.epic_engine.API.custom;

import com.epic_engine.custom.MainMenuLayoutData;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Main menu component provider interface.
 * Other mods can implement this interface to add custom components to Epic Engine's main menu.
 */
public interface IMainMenuComponentProvider {

    /**
     * Get the mod ID that owns this component provider.
     * @return The unique identifier of the mod
     */
    String getModId();

    /**
     * Create a custom button for the main menu.
     * @param layoutData Current layout data containing screen dimensions and other layout information
     * @return Button component, or null if no button is needed
     */
    @Nullable
    IExternalButton createButton(MainMenuLayoutData layoutData);

    /**
     * Create custom text components for the main menu.
     * @param layoutData Current layout data
     * @return List of text components, or empty list if no text is needed
     */
    default List<IExternalText> createTexts(MainMenuLayoutData layoutData) {
        return java.util.Collections.emptyList();
    }

    /**
     * Check whether this component should be displayed.
     * Can be used for conditional display, such as only showing in singleplayer or multiplayer.
     * @return true if the component should be displayed, false otherwise
     */
    default boolean shouldDisplay() {
        return true;
    }
}