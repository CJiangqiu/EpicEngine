package com.epic_engine.API.custom;

/**
 * Component placement configuration for positioning custom UI elements.
 * Provides various placement strategies for buttons and text components.
 */
public class ComponentPlacement {
    public final int x, y;
    public final int width, height;
    public final PlacementType type;

    /**
     * Create a new component placement configuration.
     * @param x X coordinate or offset (depending on placement type)
     * @param y Y coordinate or offset (depending on placement type)
     * @param width Component width
     * @param height Component height
     * @param type Placement strategy type
     */
    public ComponentPlacement(int x, int y, int width, int height, PlacementType type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
    }

    /**
     * Create an absolute position placement.
     * The component will be placed at the exact coordinates specified.
     * @param x Absolute X coordinate
     * @param y Absolute Y coordinate
     * @param width Component width
     * @param height Component height
     * @return ComponentPlacement with absolute positioning
     */
    public static ComponentPlacement absolute(int x, int y, int width, int height) {
        return new ComponentPlacement(x, y, width, height, PlacementType.ABSOLUTE);
    }

    /**
     * Create a placement relative to the screen center.
     * The component will be positioned relative to the center of the screen.
     * @param offsetX X offset from center (positive = right, negative = left)
     * @param offsetY Y offset from center (positive = down, negative = up)
     * @param width Component width
     * @param height Component height
     * @return ComponentPlacement relative to screen center
     */
    public static ComponentPlacement relativeToCenter(int offsetX, int offsetY, int width, int height) {
        return new ComponentPlacement(offsetX, offsetY, width, height, PlacementType.RELATIVE_TO_CENTER);
    }

    /**
     * Create a placement below the vanilla menu buttons.
     * The component will be automatically positioned below the standard menu buttons.
     * @param offsetY Additional Y offset from the default position
     * @param width Component width
     * @param height Component height
     * @return ComponentPlacement below vanilla buttons
     */
    public static ComponentPlacement belowButtons(int offsetY, int width, int height) {
        return new ComponentPlacement(0, offsetY, width, height, PlacementType.BELOW_BUTTONS);
    }

    /**
     * Create a placement above the vanilla menu buttons.
     * The component will be automatically positioned above the standard menu buttons.
     * @param offsetY Additional Y offset from the default position
     * @param width Component width
     * @param height Component height
     * @return ComponentPlacement above vanilla buttons
     */
    public static ComponentPlacement aboveButtons(int offsetY, int width, int height) {
        return new ComponentPlacement(0, offsetY, width, height, PlacementType.ABOVE_BUTTONS);
    }

    /**
     * Create a placement in the left side area.
     * @param offsetX X offset from the left edge
     * @param offsetY Y offset from the top
     * @param width Component width
     * @param height Component height
     * @return ComponentPlacement in left side area
     */
    public static ComponentPlacement leftSide(int offsetX, int offsetY, int width, int height) {
        return new ComponentPlacement(offsetX, offsetY, width, height, PlacementType.LEFT_SIDE);
    }

    /**
     * Create a placement in the right side area.
     * @param offsetX X offset from the right edge (usually negative)
     * @param offsetY Y offset from the top
     * @param width Component width
     * @param height Component height
     * @return ComponentPlacement in right side area
     */
    public static ComponentPlacement rightSide(int offsetX, int offsetY, int width, int height) {
        return new ComponentPlacement(offsetX, offsetY, width, height, PlacementType.RIGHT_SIDE);
    }

    /**
     * Placement type enumeration.
     * Defines different strategies for positioning components on the main menu.
     */
    public enum PlacementType {
        /** Absolute positioning using exact screen coordinates */
        ABSOLUTE,

        /** Positioning relative to the screen center */
        RELATIVE_TO_CENTER,

        /** Automatic positioning below vanilla menu buttons */
        BELOW_BUTTONS,

        /** Automatic positioning above vanilla menu buttons */
        ABOVE_BUTTONS,

        /** Positioning in the left side area of the screen */
        LEFT_SIDE,

        /** Positioning in the right side area of the screen */
        RIGHT_SIDE
    }
}