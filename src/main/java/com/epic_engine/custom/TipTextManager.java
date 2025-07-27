package com.epic_engine.custom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Tip Text Manager
 * Manages tip text rotation with global timing to ensure continuity across loading screens
 * Now fully integrated with LoadingScreenLayoutData for dynamic tip configuration
 */
public class TipTextManager {
    private static final Logger LOGGER = LogManager.getLogger();

    // Global state that persists across loading screens
    private static long globalStartTime = 0;
    private static int currentTipIndex = 0;
    private static int switchIntervalSeconds = 3;
    private static List<String> tipKeys = null;
    private static boolean initialized = false;

    /**
     * Initialize tip text manager with configuration from layout data
     */
    public static void initialize(LoadingScreenLayoutData.TipTextComponent.TipProperties tipProperties) {
        if (tipProperties == null || tipProperties.tip_keys == null || tipProperties.tip_keys.isEmpty()) {
            LOGGER.warn("[EPIC ENGINE]: Invalid tip properties, using defaults");
            initializeDefaults();
            return;
        }

        // Only reset timing if configuration changed
        boolean configChanged = !initialized ||
                switchIntervalSeconds != tipProperties.switch_interval_seconds ||
                !tipProperties.tip_keys.equals(tipKeys);

        if (configChanged) {
            LOGGER.info("[EPIC ENGINE]: Tip configuration changed, resetting timing");
            globalStartTime = System.currentTimeMillis();
            currentTipIndex = 0;
        }

        switchIntervalSeconds = tipProperties.switch_interval_seconds;
        tipKeys = new java.util.ArrayList<>(tipProperties.tip_keys); // Make a copy to avoid external modification
        initialized = true;

        LOGGER.debug("[EPIC ENGINE]: Tip manager initialized - {} tips, {}s interval",
                tipKeys.size(), switchIntervalSeconds);
    }

    /**
     * Initialize with default values by reading from layout data
     */
    private static void initializeDefaults() {
        if (!initialized) {
            globalStartTime = System.currentTimeMillis();
            currentTipIndex = 0;
        }

        switchIntervalSeconds = 3;

        // Try to get tip keys from loading screen layout
        try {
            // 修复：使用 UnifiedLayoutEditor 获取加载界面布局数据
            LoadingScreenLayoutData layoutData = UnifiedLayoutEditor.getCurrentLoadingScreenLayout();

            if (layoutData != null && layoutData.tip_text != null &&
                    layoutData.tip_text.properties != null &&
                    layoutData.tip_text.properties.tip_keys != null &&
                    !layoutData.tip_text.properties.tip_keys.isEmpty()) {

                tipKeys = new java.util.ArrayList<>(layoutData.tip_text.properties.tip_keys);
                switchIntervalSeconds = layoutData.tip_text.properties.switch_interval_seconds;
                LOGGER.info("[EPIC ENGINE]: Tip manager initialized with layout defaults - {} tips, {}s interval",
                        tipKeys.size(), switchIntervalSeconds);
                initialized = true;
                return;
            }
        } catch (Exception e) {
            LOGGER.debug("[EPIC ENGINE]: Could not load tips from layout (normal on first startup): {}", e.getMessage());
        }

        // Emergency fallback to hardcoded defaults if no layout is available
        tipKeys = java.util.Arrays.asList(
                "epic_engine.tip.welcome",
                "epic_engine.tip.customization",
                "epic_engine.tip.layout_editor"
        );
        LOGGER.info("[EPIC ENGINE]: Tip manager initialized with emergency fallback defaults - {} tips", tipKeys.size());

        initialized = true;
    }

    /**
     * Get current tip text key with automatic rotation
     */
    public static String getCurrentTipKey() {
        if (!initialized) {
            initializeDefaults();
        }

        if (tipKeys == null || tipKeys.isEmpty()) {
            return "epic_engine.tip.welcome";
        }

        // Calculate which tip should be shown based on global time
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - globalStartTime;
        long elapsedSeconds = elapsedTime / 1000;

        // Calculate tip index based on elapsed time
        int calculatedIndex = (int) (elapsedSeconds / switchIntervalSeconds) % tipKeys.size();

        // Update current index if it changed
        if (calculatedIndex != currentTipIndex) {
            currentTipIndex = calculatedIndex;
            LOGGER.debug("[EPIC ENGINE]: Tip rotated to index {} ({}s elapsed)",
                    currentTipIndex, elapsedSeconds);
        }

        return tipKeys.get(currentTipIndex);
    }

    /**
     * Get current tip display text
     */
    public static String getCurrentTipText() {
        String tipKey = getCurrentTipKey();
        return EpicEngineI18n.getDisplayText(tipKey);
    }

    /**
     * Get progress within current tip cycle (0.0 to 1.0)
     */
    public static float getTipProgress() {
        if (!initialized) {
            return 0.0f;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - globalStartTime;
        long elapsedSeconds = elapsedTime / 1000;

        long cyclePosition = elapsedSeconds % switchIntervalSeconds;
        return (float) cyclePosition / switchIntervalSeconds;
    }

    /**
     * Get remaining time for current tip in seconds
     */
    public static int getRemainingSeconds() {
        if (!initialized) {
            return switchIntervalSeconds;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - globalStartTime;
        long elapsedSeconds = elapsedTime / 1000;

        long cyclePosition = elapsedSeconds % switchIntervalSeconds;
        return (int) (switchIntervalSeconds - cyclePosition);
    }

    /**
     * Reset tip timing (for testing or manual control)
     */
    public static void resetTiming() {
        globalStartTime = System.currentTimeMillis();
        currentTipIndex = 0;
        LOGGER.info("[EPIC ENGINE]: Tip timing reset");
    }

    /**
     * Force switch to next tip
     */
    public static void nextTip() {
        if (!initialized || tipKeys == null || tipKeys.isEmpty()) {
            return;
        }

        currentTipIndex = (currentTipIndex + 1) % tipKeys.size();

        // Adjust global start time to make the new tip start fresh
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - globalStartTime) / 1000;
        long completedCycles = elapsedSeconds / switchIntervalSeconds;
        globalStartTime = currentTime - (completedCycles * switchIntervalSeconds * 1000);

        LOGGER.debug("[EPIC ENGINE]: Manually switched to tip index {}", currentTipIndex);
    }

    /**
     * Get current tip index
     */
    public static int getCurrentTipIndex() {
        getCurrentTipKey(); // Ensure index is updated
        return currentTipIndex;
    }

    /**
     * Get total number of tips
     */
    public static int getTipCount() {
        return tipKeys != null ? tipKeys.size() : 0;
    }

    /**
     * Get switch interval in seconds
     */
    public static int getSwitchInterval() {
        return switchIntervalSeconds;
    }

    /**
     * Check if tip manager is initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Get all tip keys
     */
    public static List<String> getAllTipKeys() {
        return tipKeys != null ? java.util.Collections.unmodifiableList(tipKeys) : java.util.Collections.emptyList();
    }

    /**
     * Update configuration without resetting timing
     */
    public static void updateConfiguration(int newSwitchInterval, List<String> newTipKeys) {
        if (newTipKeys != null && !newTipKeys.isEmpty()) {
            // If we're currently past the new tip count, wrap around
            if (tipKeys != null && currentTipIndex >= newTipKeys.size()) {
                currentTipIndex = currentTipIndex % newTipKeys.size();
            }

            tipKeys = new java.util.ArrayList<>(newTipKeys);
        }

        if (newSwitchInterval > 0) {
            switchIntervalSeconds = newSwitchInterval;
        }

        LOGGER.info("[EPIC ENGINE]: Tip configuration updated - {} tips, {}s interval",
                getTipCount(), switchIntervalSeconds);
    }

    /**
     * Add a new tip key to the current configuration
     */
    public static boolean addTipKey(String tipKey) {
        if (!initialized) {
            initializeDefaults();
        }

        if (tipKey == null || tipKey.trim().isEmpty()) {
            return false;
        }

        if (tipKeys == null) {
            tipKeys = new java.util.ArrayList<>();
        }

        if (!tipKeys.contains(tipKey)) {
            tipKeys.add(tipKey);
            LOGGER.info("[EPIC ENGINE]: Added new tip key: {}", tipKey);
            return true;
        }

        return false; // Already exists
    }

    /**
     * Remove a tip key from the current configuration
     */
    public static boolean removeTipKey(String tipKey) {
        if (!initialized || tipKeys == null || tipKeys.isEmpty()) {
            return false;
        }

        boolean removed = tipKeys.remove(tipKey);
        if (removed) {
            // Adjust current index if necessary
            if (currentTipIndex >= tipKeys.size() && !tipKeys.isEmpty()) {
                currentTipIndex = currentTipIndex % tipKeys.size();
            }
            LOGGER.info("[EPIC ENGINE]: Removed tip key: {}", tipKey);
        }

        return removed;
    }

    /**
     * Clear all tip keys and reset to defaults
     */
    public static void clearAndReset() {
        tipKeys = null;
        initialized = false;
        globalStartTime = 0;
        currentTipIndex = 0;
        switchIntervalSeconds = 3;

        // Reinitialize with defaults
        initializeDefaults();

        LOGGER.info("[EPIC ENGINE]: Tip manager cleared and reset to defaults");
    }

    /**
     * Get configuration summary for debugging
     */
    public static String getConfigurationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("TipTextManager Configuration:\n");
        summary.append("- Initialized: ").append(initialized).append("\n");
        summary.append("- Current Tip Index: ").append(currentTipIndex).append("/").append(getTipCount()).append("\n");
        summary.append("- Switch Interval: ").append(switchIntervalSeconds).append("s\n");
        summary.append("- Current Tip Key: ").append(getCurrentTipKey()).append("\n");
        summary.append("- Remaining Time: ").append(getRemainingSeconds()).append("s\n");
        summary.append("- All Tip Keys: ").append(getAllTipKeys()).append("\n");
        return summary.toString();
    }

    /**
     * Validate current configuration
     */
    public static boolean validateConfiguration() {
        if (!initialized) {
            return false;
        }

        if (tipKeys == null || tipKeys.isEmpty()) {
            return false;
        }

        if (switchIntervalSeconds <= 0) {
            return false;
        }

        // Check if all tip keys are valid (not null or empty)
        for (String tipKey : tipKeys) {
            if (tipKey == null || tipKey.trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get validation errors
     */
    public static java.util.List<String> getValidationErrors() {
        java.util.List<String> errors = new java.util.ArrayList<>();

        if (!initialized) {
            errors.add("TipTextManager not initialized");
        }

        if (tipKeys == null || tipKeys.isEmpty()) {
            errors.add("No tip keys configured");
        } else {
            for (int i = 0; i < tipKeys.size(); i++) {
                String tipKey = tipKeys.get(i);
                if (tipKey == null || tipKey.trim().isEmpty()) {
                    errors.add("Invalid tip key at index " + i);
                }
            }
        }

        if (switchIntervalSeconds <= 0) {
            errors.add("Invalid switch interval: " + switchIntervalSeconds);
        }

        return errors;
    }

    /**
     * Force reinitialize from layout data (for when layout is updated)
     */
    public static void reinitializeFromLayout() {
        initialized = false;
        tipKeys = null;
        initializeDefaults();
        LOGGER.info("[EPIC ENGINE]: TipTextManager reinitialized from layout data");
    }
}