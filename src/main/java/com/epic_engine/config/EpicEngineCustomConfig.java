package com.epic_engine.config;

import com.epic_engine.EpicEngineMod;
import com.epic_engine.custom.EpicEngineI18n;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Map;

public class EpicEngineCustomConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final ForgeConfigSpec CONFIG;

    private static final Path EPIC_ENGINE_DIR = FMLPaths.CONFIGDIR.get().resolve("epic_engine");
    private static final Path CUSTOM_DIR      = EPIC_ENGINE_DIR.resolve("custom");
    private static final Path TEXTURES_DIR    = CUSTOM_DIR.resolve("textures");
    private static final Path LAYOUT_FILE     = CUSTOM_DIR.resolve("main_menu_layout.json");

    private static boolean isOverriddenByServer = false;
    private static boolean serverPresetCommandsEnabled = false;
    private static String serverPresetCommandsList = "";
    private static int serverPresetCommandsDelay = 20;

    public static final ForgeConfigSpec.BooleanValue ENABLE_CUSTOMIZATION;

    public static final ForgeConfigSpec.BooleanValue WINDOW_MODULE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue WINDOW_TITLE_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> WINDOW_TITLE_TEXT;
    public static final ForgeConfigSpec.BooleanValue WINDOW_ICON_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> WINDOW_ICON_16_FILENAME;
    public static final ForgeConfigSpec.ConfigValue<String> WINDOW_ICON_32_FILENAME;

    public static final ForgeConfigSpec.BooleanValue MAIN_MENU_MODULE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue MAIN_MENU_BACKGROUND_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> MAIN_MENU_BACKGROUND_FILENAME;
    public static final ForgeConfigSpec.BooleanValue MAIN_MENU_TITLE_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> MAIN_MENU_TITLE_FILENAME;
    public static final ForgeConfigSpec.BooleanValue MAIN_MENU_BUTTONS_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> MAIN_MENU_BUTTON_PREFIX;
    public static final ForgeConfigSpec.BooleanValue EXTERNAL_MOD_COMPONENTS_ENABLED;

    public static final ForgeConfigSpec.BooleanValue LOADING_SCREEN_MODULE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue LOADING_SCREEN_BACKGROUND_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> LOADING_SCREEN_BACKGROUND_FILENAME;
    public static final ForgeConfigSpec.BooleanValue LOADING_SCREEN_PROGRESS_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> LOADING_SCREEN_PROGRESS_BAR_FILENAME;
    public static final ForgeConfigSpec.BooleanValue LOADING_SCREEN_PROGRESS_SHOW_PERCENTAGE;
    public static final ForgeConfigSpec.BooleanValue LOADING_SCREEN_TIP_TEXT_ENABLED;
    public static final ForgeConfigSpec.IntValue LOADING_SCREEN_TIP_SWITCH_INTERVAL;

    public static final ForgeConfigSpec.BooleanValue OTHERS_MODULE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue PRESET_COMMANDS_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> PRESET_COMMANDS_LIST;
    public static final ForgeConfigSpec.IntValue PRESET_COMMANDS_DELAY;

    static {
        createDirectories();

        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Epic Engine Configuration", "Control all customization features of Epic Engine")
                .push("epic_engine");

        ENABLE_CUSTOMIZATION = builder.comment("Master switch for all Epic Engine features")
                .define("enableCustomization", true);

        builder.comment("Window Customization Module", "Customize game window appearance")
                .push("window_customization");

        WINDOW_MODULE_ENABLED = builder.comment("Enable window customization module")
                .define("moduleEnabled", true);

        builder.comment("Window Title Settings").push("title");
        WINDOW_TITLE_ENABLED = builder.comment("Enable custom window title").define("enabled", true);
        WINDOW_TITLE_TEXT    = builder.comment("Custom window title text")
                .define("text", "Epic Engine: A good tool to make your modpack!");
        builder.pop();

        builder.comment("Window Icon Settings").push("icon");
        WINDOW_ICON_ENABLED      = builder.comment("Enable custom window icon").define("enabled", true);
        WINDOW_ICON_16_FILENAME  = builder.comment("Small icon filename (16×16) in config/epic_engine/custom/textures/")
                .define("icon16", "icon16.png");
        WINDOW_ICON_32_FILENAME  = builder.comment("Large icon filename (32×32) in config/epic_engine/custom/textures/")
                .define("icon32", "icon32.png");
        builder.pop();
        builder.pop();

        builder.comment("Main Menu Customization Module", "Customize main menu appearance")
                .push("main_menu_customization");

        MAIN_MENU_MODULE_ENABLED = builder.comment("Enable main menu customization module")
                .define("moduleEnabled", true);

        builder.comment("Background Settings").push("background");
        MAIN_MENU_BACKGROUND_ENABLED  = builder.comment("Enable custom main menu background").define("enabled", true);
        MAIN_MENU_BACKGROUND_FILENAME = builder.comment("Background image filename in config/epic_engine/custom/textures/")
                .define("filename", "background.png");
        builder.pop();

        builder.comment("Title Image Settings").push("title_image");
        MAIN_MENU_TITLE_ENABLED  = builder.comment("Enable custom main menu title image").define("enabled", true);
        MAIN_MENU_TITLE_FILENAME = builder.comment("Title image filename in config/epic_engine/custom/textures/")
                .define("filename", "title.png");
        builder.pop();

        builder.comment("Button Customization Settings").push("buttons");
        MAIN_MENU_BUTTONS_ENABLED = builder.comment("Enable custom main menu buttons",
                        "Allows customizing button textures and text using layout editor")
                .define("enabled", true);
        MAIN_MENU_BUTTON_PREFIX = builder.comment("Button image filename prefix in config/epic_engine/custom/textures/",
                        "Button images will be searched as: [prefix]1.png, [prefix]2.png, etc.")
                .define("prefix", "button_");
        builder.pop();

        builder.comment("External Mod Integration Settings").push("external_mods");
        EXTERNAL_MOD_COMPONENTS_ENABLED = builder
                .comment("Allow external mods to add custom buttons and text to the main menu",
                        "When enabled, other mods can use Epic Engine's API to register their own main menu components",
                        "External components will be integrated into the layout editor and can be positioned like vanilla buttons")
                .define("allowExternalComponents", true);
        builder.pop();

        builder.pop();

        builder.comment("Loading Screen Customization Module",
                        "Unified loading screen with custom background and progress bar")
                .push("loading_screen_customization");

        LOADING_SCREEN_MODULE_ENABLED = builder.comment("Enable loading screen customization module")
                .define("moduleEnabled", true);

        builder.comment("Background Settings").push("background");
        LOADING_SCREEN_BACKGROUND_ENABLED = builder.comment("Enable custom loading screen background")
                .define("enabled", true);
        LOADING_SCREEN_BACKGROUND_FILENAME = builder.comment("Background image filename in config/epic_engine/custom/textures/")
                .define("filename", "loading.png");
        builder.pop();


        builder.comment("Custom Progress Bar Settings").push("loading_progress_bar");
        LOADING_SCREEN_PROGRESS_ENABLED = builder.comment("Enable custom progress bar")
                .define("enabled", true);
        LOADING_SCREEN_PROGRESS_BAR_FILENAME = builder.comment("Progress bar image filename in config/epic_engine/custom/textures/")
                .define("filename", "loading_progress_bar.png");
        LOADING_SCREEN_PROGRESS_SHOW_PERCENTAGE = builder.comment("Show percentage number above progress bar")
                .define("showPercentage", true);
        builder.pop();

        builder.comment("Loading Tip Text Settings").push("loading_tip_text");
        LOADING_SCREEN_TIP_TEXT_ENABLED = builder.comment("Enable rotating tip text during loading")
                .define("enabled", true);
        LOADING_SCREEN_TIP_SWITCH_INTERVAL = builder.comment("Tip text switch interval in seconds")
                .defineInRange("switchIntervalSeconds", 3, 1, 30);
        builder.pop();
        builder.pop();

        builder.comment("Others Module", "Customize miscellaneous behaviors")
                .push("others");

        OTHERS_MODULE_ENABLED = builder.comment("Enable others module").define("moduleEnabled", true);

        builder.comment("Preset Commands Settings",
                        "Execute commands when players join (SECURITY NOTE: In multiplayer, server config overrides client config)")
                .push("preset_commands");
        PRESET_COMMANDS_ENABLED = builder.comment("Enable preset commands execution").define("enabled", true);
        PRESET_COMMANDS_LIST    = builder.comment("Commands to execute, separated by semicolons (;)",
                        "WARNING: In multiplayer servers, only server admin can control these commands")
                .define("commands",
                        "say [EPIC ENGINE]:Welcome to Epic Engine!;xp add @a 1");
        PRESET_COMMANDS_DELAY   = builder.comment("Delay in ticks before executing commands (20 ticks = 1 second)")
                .defineInRange("delayTicks", 20, 0, 1200);
        builder.pop();
        builder.pop();

        builder.pop();

        CONFIG = builder.build();
    }

    public static void overrideFromServer(boolean enabled, String commands, int delay) {
        isOverriddenByServer = true;
        serverPresetCommandsEnabled = enabled;
        serverPresetCommandsList = commands != null ? commands : "";
        serverPresetCommandsDelay = delay;

        LOGGER.info("[EPIC ENGINE]: Config overridden by server - enabled: {}, delay: {}ms, {} commands",
                enabled, delay * 50,
                commands != null && !commands.isBlank() ? commands.split(";").length : 0);
    }

    public static void restoreLocalConfig() {
        if (isOverriddenByServer) {
            isOverriddenByServer = false;
            serverPresetCommandsEnabled = false;
            serverPresetCommandsList = "";
            serverPresetCommandsDelay = 20;

            LOGGER.info("[EPIC ENGINE]: Restored local config");
        }
    }

    public static boolean getEffectivePresetCommandsEnabled() {
        return isOverriddenByServer ? serverPresetCommandsEnabled : PRESET_COMMANDS_ENABLED.get();
    }

    public static String getEffectivePresetCommandsList() {
        return isOverriddenByServer ? serverPresetCommandsList : PRESET_COMMANDS_LIST.get();
    }

    public static int getEffectivePresetCommandsDelay() {
        return isOverriddenByServer ? serverPresetCommandsDelay : PRESET_COMMANDS_DELAY.get();
    }

    public static boolean isOverriddenByServer() {
        return isOverriddenByServer;
    }

    public static String getConfigSource() {
        return isOverriddenByServer ? "SERVER" : "LOCAL";
    }

    public static void initializeResources() {
        createDirectories();
        try {
            copyResourceDirectory("assets/" + EpicEngineMod.MODID + "/custom", CUSTOM_DIR);
            EpicEngineI18n.initialize();
            LOGGER.info("[EPIC ENGINE]: Default resources extracted to {}", CUSTOM_DIR);
        } catch (Exception e) {
            LOGGER.error("[EPIC ENGINE]: Failed to extract default resources", e);
        }
    }

    public static java.io.File getTextureFile(String filename) {
        return TEXTURES_DIR.resolve(filename).toFile();
    }

    public static java.io.File getLayoutFile() {
        return LAYOUT_FILE.toFile();
    }

    public static Path getCustomDir() {
        return CUSTOM_DIR;
    }

    // Window Configuration Methods
    public static boolean isWindowTitleEnabled() {
        try {
            return ENABLE_CUSTOMIZATION.get() && WINDOW_MODULE_ENABLED.get() && WINDOW_TITLE_ENABLED.get();
        } catch (IllegalStateException e) { return false; }
    }

    public static boolean isWindowIconEnabled() {
        try {
            return ENABLE_CUSTOMIZATION.get() && WINDOW_MODULE_ENABLED.get() && WINDOW_ICON_ENABLED.get();
        } catch (IllegalStateException e) { return false; }
    }

    public static String getWindowTitle() {
        return isWindowTitleEnabled() ? WINDOW_TITLE_TEXT.get() : null;
    }

    // Main Menu Configuration Methods
    public static boolean isMainMenuBackgroundEnabled() {
        try {
            return ENABLE_CUSTOMIZATION.get() && MAIN_MENU_MODULE_ENABLED.get() && MAIN_MENU_BACKGROUND_ENABLED.get();
        } catch (IllegalStateException e) { return false; }
    }

    public static boolean isMainMenuTitleEnabled() {
        try {
            return ENABLE_CUSTOMIZATION.get() && MAIN_MENU_MODULE_ENABLED.get() && MAIN_MENU_TITLE_ENABLED.get();
        } catch (IllegalStateException e) { return false; }
    }

    public static boolean isMainMenuButtonsEnabled() {
        try {
            return ENABLE_CUSTOMIZATION.get() && MAIN_MENU_MODULE_ENABLED.get() && MAIN_MENU_BUTTONS_ENABLED.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static String getMainMenuButtonPrefix() {
        return isMainMenuButtonsEnabled() ? MAIN_MENU_BUTTON_PREFIX.get() : "button_";
    }

    /**
     * Check if external mod components are enabled for main menu.
     * This combines the master switch, main menu module, and external mod component settings.
     * @return true if external mods can add components to the main menu
     */
    public static boolean isExternalModComponentsEnabled() {
        try {
            return ENABLE_CUSTOMIZATION.get()
                    && MAIN_MENU_MODULE_ENABLED.get()
                    && EXTERNAL_MOD_COMPONENTS_ENABLED.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    // Loading Screen Configuration Methods
    public static boolean isLoadingScreenCustomizationEnabled() {
        try {
            return ENABLE_CUSTOMIZATION.get() && LOADING_SCREEN_MODULE_ENABLED.get();
        } catch (IllegalStateException e) { return false; }
    }

    public static boolean isLoadingScreenBackgroundEnabled() {
        try {
            return isLoadingScreenCustomizationEnabled() && LOADING_SCREEN_BACKGROUND_ENABLED.get();
        } catch (IllegalStateException e) { return false; }
    }


    public static boolean isLoadingScreenProgressEnabled() {
        try {
            return isLoadingScreenCustomizationEnabled() && LOADING_SCREEN_PROGRESS_ENABLED.get();
        } catch (IllegalStateException e) { return false; }
    }

    /**
     * Check if loading screen tip text is enabled
     * @return true if tip text should be displayed during loading
     */
    public static boolean isLoadingScreenTipTextEnabled() {
        try {
            return isLoadingScreenCustomizationEnabled() && LOADING_SCREEN_TIP_TEXT_ENABLED.get();
        } catch (IllegalStateException e) { return false; }
    }

    public static String getLoadingScreenBackgroundFilename() {
        return isLoadingScreenBackgroundEnabled() ? LOADING_SCREEN_BACKGROUND_FILENAME.get() : null;
    }


    public static String getLoadingScreenProgressBarFilename() {
        return isLoadingScreenProgressEnabled() ? LOADING_SCREEN_PROGRESS_BAR_FILENAME.get() : null;
    }

    public static boolean shouldShowProgressPercentage() {
        return isLoadingScreenProgressEnabled() && LOADING_SCREEN_PROGRESS_SHOW_PERCENTAGE.get();
    }

    /**
     * Get loading screen tip switch interval in seconds
     * @return tip switch interval, default 3 seconds
     */
    public static int getLoadingScreenTipSwitchInterval() {
        return isLoadingScreenTipTextEnabled() ? LOADING_SCREEN_TIP_SWITCH_INTERVAL.get() : 3;
    }

    // Others Module Configuration Methods
    public static boolean isOthersModuleEnabled() {
        try {
            return ENABLE_CUSTOMIZATION.get() && OTHERS_MODULE_ENABLED.get();
        } catch (IllegalStateException e) { return false; }
    }

    public static boolean isPresetCommandsEnabled() {
        try {
            return isOthersModuleEnabled() && getEffectivePresetCommandsEnabled();
        } catch (IllegalStateException e) { return false; }
    }

    // Utility Methods
    private static void createDirectories() {
        try {
            Files.createDirectories(TEXTURES_DIR);
            LOGGER.debug("[EPIC ENGINE]: Resource directories created or already exist");
        } catch (IOException e) {
            LOGGER.error("[EPIC ENGINE]: Failed to create resource directories", e);
        }
    }

    private static void copyResourceDirectory(String resourcePath, Path targetDir)
            throws IOException, URISyntaxException {

        URL url = EpicEngineCustomConfig.class.getClassLoader().getResource(resourcePath);
        if (url == null) {
            LOGGER.warn("[EPIC ENGINE]: Resource folder not found: {}", resourcePath);
            return;
        }

        URI uri = url.toURI();
        switch (uri.getScheme()) {
            case "file" -> {
                Path base = Paths.get(uri);
                walkAndCopy(base, base, targetDir);
            }

            case "jar" -> {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                    Path jarPath = fs.getPath(resourcePath);
                    walkAndCopy(jarPath, jarPath, targetDir);
                }
            }

            case "union" -> {
                Path base = Paths.get(uri);
                walkAndCopy(base, base, targetDir);
            }

            default -> LOGGER.warn("[EPIC ENGINE]: Unsupported URI scheme: {}", uri.getScheme());
        }
    }

    private static void walkAndCopy(Path start, Path base, Path targetBase) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.walk(start)) {
            stream.forEach(source -> copyPath(source, base, targetBase));
        }
    }

    private static void copyPath(Path source, Path base, Path targetBase) {
        try {
            Path relative = base.relativize(source);
            Path target   = targetBase.resolve(relative.toString());

            if (Files.isDirectory(source)) {
                Files.createDirectories(target);
            } else if (Files.notExists(target)) {
                Files.createDirectories(target.getParent());
                try (InputStream in = EpicEngineCustomConfig.class
                        .getClassLoader()
                        .getResourceAsStream(base.resolve(relative).toString())) {

                    if (in != null) Files.copy(in, target);
                }
            }
        } catch (IOException e) {
            LOGGER.error("[EPIC ENGINE]: Error copying resource {} -> {}", source, targetBase, e);
        }
    }

    // Configuration Validation Methods

    /**
     * Validate all configuration values
     * @return true if configuration is valid
     */
    public static boolean validateConfiguration() {
        try {
            // Test all configuration access
            ENABLE_CUSTOMIZATION.get();
            WINDOW_MODULE_ENABLED.get();
            MAIN_MENU_MODULE_ENABLED.get();
            LOADING_SCREEN_MODULE_ENABLED.get();
            OTHERS_MODULE_ENABLED.get();

            // Test all string configurations
            WINDOW_TITLE_TEXT.get();
            MAIN_MENU_BACKGROUND_FILENAME.get();
            LOADING_SCREEN_BACKGROUND_FILENAME.get();

            // Test all integer configurations
            LOADING_SCREEN_TIP_SWITCH_INTERVAL.get();
            PRESET_COMMANDS_DELAY.get();

            return true;
        } catch (Exception e) {
            LOGGER.error("[EPIC ENGINE]: Configuration validation failed", e);
            return false;
        }
    }

    /**
     * Get configuration summary for debugging
     * @return formatted configuration summary
     */
    public static String getConfigurationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Epic Engine Configuration Summary:\n");
        summary.append("- Master Enable: ").append(ENABLE_CUSTOMIZATION.get()).append("\n");
        summary.append("- Window Module: ").append(WINDOW_MODULE_ENABLED.get()).append("\n");
        summary.append("- Main Menu Module: ").append(MAIN_MENU_MODULE_ENABLED.get()).append("\n");
        summary.append("- Loading Screen Module: ").append(LOADING_SCREEN_MODULE_ENABLED.get()).append("\n");
        summary.append("- Loading Screen Tip Text: ").append(isLoadingScreenTipTextEnabled()).append("\n");
        summary.append("- Tip Switch Interval: ").append(getLoadingScreenTipSwitchInterval()).append("s\n");
        summary.append("- Others Module: ").append(OTHERS_MODULE_ENABLED.get()).append("\n");
        summary.append("- External Components: ").append(isExternalModComponentsEnabled()).append("\n");
        return summary.toString();
    }

    /**
     * Reset configuration to defaults (for testing)
     */
    public static void resetToDefaults() {
        // This would require recreating the config spec, which is complex
        // For now, just log the action
        LOGGER.info("[EPIC ENGINE]: Configuration reset requested - restart required for full reset");
    }

    /**
     * Check if configuration has been initialized
     * @return true if configuration is ready for use
     */
    public static boolean isConfigurationReady() {
        try {
            return CONFIG != null && ENABLE_CUSTOMIZATION != null;
        } catch (Exception e) {
            return false;
        }
    }
}