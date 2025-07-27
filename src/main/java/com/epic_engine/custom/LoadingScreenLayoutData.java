package com.epic_engine.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Loading Screen Layout Data
 * Manages layout configuration for loading screens
 */
public class LoadingScreenLayoutData {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public String version = "1.0";
    public String created_time = "";
    public String last_modified = "";
    public ScreenResolution screen_resolution = new ScreenResolution();

    public BackgroundComponent background = new BackgroundComponent();
    public ProgressBarComponent progress_bar = new ProgressBarComponent();
    public TipTextComponent tip_text = new TipTextComponent();
    public PercentageTextComponent percentage_text = new PercentageTextComponent();
    public List<CustomTextComponent> custom_texts = new ArrayList<>();

    public static class ScreenResolution {
        public int width = 1920;
        public int height = 1080;

        public ScreenResolution() {}

        public ScreenResolution(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public static class Position {
        public int x = 0;
        public int y = 0;
        public int width = -1;
        public int height = -1;

        public Position() {}

        public Position(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public static class BackgroundComponent {
        public String texture_name = "loading.png";
        public boolean enabled = true;

        public BackgroundComponent() {}
    }

    public static class ProgressBarComponent {
        public String texture_name = "loading_progress_bar.png";
        public Position position = new Position();
        public boolean enabled = true;
        public boolean show_background = false;
        public String background_color = "#000000";
        public int background_alpha = 128;

        public ProgressBarComponent() {
            // Default position: right side center
            position.x = 1440; // 3/4 of 1920
            position.y = 540;  // center of 1080
            position.width = 400;
            position.height = 32;
        }
    }

    public static class TipTextComponent {
        public Position position = new Position();
        public TipProperties properties = new TipProperties();
        public boolean enabled = true;

        public TipTextComponent() {
            // Default position: below progress bar
            position.x = 1440; // 3/4 of 1920
            position.y = 600;  // below progress bar
            position.width = 400;
            position.height = 30;
        }

        public static class TipProperties {
            public String color = "#FFFFFF";
            public float font_scale = 1.0f;
            public boolean shadow = true;
            public String alignment = "CENTER";
            public int switch_interval_seconds = 3;
            public List<String> tip_keys = new ArrayList<>();

            public TipProperties() {
                // Default tip keys - will be automatically generated in language files
                tip_keys.add("epic_engine.tip.welcome");
                tip_keys.add("epic_engine.tip.customization");
                tip_keys.add("epic_engine.tip.layout_editor");
                tip_keys.add("epic_engine.tip.drag_components");
                tip_keys.add("epic_engine.tip.custom_images");
                tip_keys.add("epic_engine.tip.multilingual");
                tip_keys.add("epic_engine.tip.auto_rotation");
            }
        }
    }

    public static class PercentageTextComponent {
        public Position position = new Position();
        public PercentageProperties properties = new PercentageProperties();
        public boolean enabled = true;

        public PercentageTextComponent() {
            // Default position: above progress bar
            position.x = 1440; // 3/4 of 1920
            position.y = 500;  // above progress bar
            position.width = 100;
            position.height = 20;
        }

        public static class PercentageProperties {
            public String color = "#FFFFFF";
            public float font_scale = 1.0f;
            public boolean shadow = true;
            public String alignment = "CENTER";
            public String format = "%.0f%%";

            public PercentageProperties() {}
        }
    }

    public static class CustomTextComponent {
        public String id = "";
        public TextProperties properties = new TextProperties();
        public Position position = new Position();

        public CustomTextComponent() {}

        public CustomTextComponent(String id) {
            this.id = id;
        }

        public static class TextProperties {
            public String text = "";
            public float font_scale = 1.0f;
            public String color = "#FFFFFF";
            public boolean shadow = true;
            public String alignment = "LEFT";

            public TextProperties() {}
        }
    }

    /**
     * Save layout data to file
     */
    public void save(File file) {
        try {
            this.last_modified = LocalDateTime.now().format(TIME_FORMATTER);

            file.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(this, writer);
                LOGGER.info("[EPIC ENGINE]: Loading screen layout saved to: {}", file.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("[EPIC ENGINE]: Failed to save loading layout to: {}", file.getAbsolutePath(), e);
        }
    }

    /**
     * Load layout data from file
     */
    public static LoadingScreenLayoutData load(File file) {
        if (!file.exists()) {
            LOGGER.info("[EPIC ENGINE]: Loading layout file not found, creating default: {}", file.getAbsolutePath());
            LoadingScreenLayoutData defaultLayout = createDefault();
            defaultLayout.save(file);
            return defaultLayout;
        }

        try (FileReader reader = new FileReader(file)) {
            LoadingScreenLayoutData data = GSON.fromJson(reader, LoadingScreenLayoutData.class);
            if (data == null) {
                LOGGER.warn("[EPIC ENGINE]: Failed to parse loading layout file, using default");
                return createDefault();
            }

            LOGGER.info("[EPIC ENGINE]: Loading screen layout loaded from: {}", file.getAbsolutePath());
            return data;
        } catch (Exception e) {
            LOGGER.error("[EPIC ENGINE]: Failed to load loading layout from: {}, using default", file.getAbsolutePath(), e);
            return createDefault();
        }
    }

    /**
     * Create default loading screen layout
     */
    public static LoadingScreenLayoutData createDefault() {
        LoadingScreenLayoutData data = new LoadingScreenLayoutData();

        String currentTime = LocalDateTime.now().format(TIME_FORMATTER);
        data.created_time = currentTime;
        data.last_modified = currentTime;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() != null) {
            data.screen_resolution = new ScreenResolution(
                    mc.getWindow().getGuiScaledWidth(),
                    mc.getWindow().getGuiScaledHeight()
            );
        }

        // Set default positions based on screen resolution
        int screenWidth = data.screen_resolution.width;
        int screenHeight = data.screen_resolution.height;

        // Progress bar: right side center
        data.progress_bar.position.x = (screenWidth * 3 / 4) - 200;
        data.progress_bar.position.y = (screenHeight / 2) - 16;
        data.progress_bar.position.width = 400;
        data.progress_bar.position.height = 32;

        // Tip text: below progress bar
        data.tip_text.position.x = (screenWidth * 3 / 4) - 200;
        data.tip_text.position.y = (screenHeight / 2) + 30;
        data.tip_text.position.width = 400;
        data.tip_text.position.height = 30;

        // Percentage text: above progress bar
        data.percentage_text.position.x = (screenWidth * 3 / 4) - 50;
        data.percentage_text.position.y = (screenHeight / 2) - 50;
        data.percentage_text.position.width = 100;
        data.percentage_text.position.height = 20;

        LOGGER.info("[EPIC ENGINE]: Created default loading layout - Screen: {}x{}", screenWidth, screenHeight);

        return data;
    }

    /**
     * Add custom text component
     */
    public CustomTextComponent addCustomText(String textId, String text, int x, int y, String color, float scale) {
        CustomTextComponent textComponent = new CustomTextComponent(textId);
        textComponent.properties.text = text != null ? text : "";
        textComponent.properties.color = color != null ? color : "#FFFFFF";
        textComponent.properties.font_scale = scale;
        textComponent.position = new Position(x, y, -1, -1);

        custom_texts.add(textComponent);
        return textComponent;
    }

    /**
     * Find custom text by ID
     */
    public CustomTextComponent findTextById(String textId) {
        return custom_texts.stream()
                .filter(text -> textId.equals(text.id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Update screen resolution
     */
    public void updateScreenResolution(int width, int height) {
        this.screen_resolution.width = width;
        this.screen_resolution.height = height;
        this.last_modified = LocalDateTime.now().format(TIME_FORMATTER);
    }

    /**
     * Get custom text count
     */
    public int getCustomTextCount() {
        return custom_texts.size();
    }

    /**
     * Validate layout data
     */
    public boolean validate() {
        if (background == null || background.texture_name == null || background.texture_name.isEmpty()) {
            LOGGER.warn("[EPIC ENGINE]: Invalid loading background configuration");
            return false;
        }

        if (progress_bar == null) {
            LOGGER.warn("[EPIC ENGINE]: Invalid progress bar configuration");
            return false;
        }

        if (tip_text == null || tip_text.properties == null) {
            LOGGER.warn("[EPIC ENGINE]: Invalid tip text configuration");
            return false;
        }

        if (percentage_text == null || percentage_text.properties == null) {
            LOGGER.warn("[EPIC ENGINE]: Invalid percentage text configuration");
            return false;
        }

        for (CustomTextComponent text : custom_texts) {
            if (text.id == null || text.id.isEmpty()) {
                LOGGER.warn("[EPIC ENGINE]: Found custom text with empty ID");
                return false;
            }
            if (text.properties == null) {
                LOGGER.warn("[EPIC ENGINE]: Found custom text with null properties: {}", text.id);
                return false;
            }
        }

        return true;
    }
}