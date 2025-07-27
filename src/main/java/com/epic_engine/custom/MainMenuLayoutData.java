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

public class MainMenuLayoutData {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public String version = "1.0";
    public String created_time = "";
    public String last_modified = "";
    public ScreenResolution screen_resolution = new ScreenResolution();

    public BackgroundComponent background = new BackgroundComponent();
    public TitleComponent title_image = new TitleComponent();
    public List<ButtonComponent> buttons = new ArrayList<>();
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
        public String texture_name = "background.png";

        public BackgroundComponent() {}
    }

    public static class TitleComponent {
        public String texture_name = "title.png";
        public float scale = 1.0f;
        public Position position = new Position();

        public TitleComponent() {}
    }

    public static class ButtonComponent {
        public String id = "";
        public int button_index = 1;
        public boolean enabled = true;
        public ButtonProperties properties = new ButtonProperties();
        public Position position = new Position();

        public ButtonComponent() {}

        public ButtonComponent(String id, int index) {
            this.id = id;
            this.button_index = index;
        }

        public static class ButtonProperties {
            public String custom_text = "";
            public String texture_name = "";
            public String text_color = "#FFFFFF";
            public float texture_scale = 1.0f;
            public boolean show_text_over_texture = true;

            public ButtonProperties() {}
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

    public void save(File file) {
        try {
            this.last_modified = LocalDateTime.now().format(TIME_FORMATTER);

            file.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(this, writer);
                LOGGER.info("[EPIC ENGINE]: Main menu layout saved to: {}", file.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("[EPIC ENGINE]: Failed to save layout to: {}", file.getAbsolutePath(), e);
        }
    }

    public static MainMenuLayoutData load(File file) {
        if (!file.exists()) {
            LOGGER.info("[EPIC ENGINE]: Layout file not found, creating default: {}", file.getAbsolutePath());
            MainMenuLayoutData defaultLayout = createDefault();
            defaultLayout.save(file);
            return defaultLayout;
        }

        try (FileReader reader = new FileReader(file)) {
            MainMenuLayoutData data = GSON.fromJson(reader, MainMenuLayoutData.class);
            if (data == null) {
                LOGGER.warn("[EPIC ENGINE]: Failed to parse layout file, using default");
                return createDefault();
            }

            LOGGER.info("[EPIC ENGINE]: Main menu layout loaded from: {}", file.getAbsolutePath());
            return data;
        } catch (Exception e) {
            LOGGER.error("[EPIC ENGINE]: Failed to load layout from: {}, using default", file.getAbsolutePath(), e);
            return createDefault();
        }
    }

    public static MainMenuLayoutData createDefault() {
        MainMenuLayoutData data = new MainMenuLayoutData();

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

        data.background.texture_name = "background.png";

        data.title_image.texture_name = "title.png";
        data.title_image.scale = 1.0f;

        int screenWidth = data.screen_resolution.width;
        int screenHeight = data.screen_resolution.height;

        int titleWidth = 400;
        int titleHeight = 100;
        int centerX = (screenWidth - titleWidth) / 2;
        int topY = screenHeight / 4 - 50;

        data.title_image.position = new Position(
                centerX,
                topY,
                titleWidth,
                titleHeight
        );

        LOGGER.info("[EPIC ENGINE]: Created default layout - Screen: {}x{}, Title at ({}, {})",
                screenWidth, screenHeight, centerX, topY);

        return data;
    }

    public ButtonComponent addButton(String buttonId, int index, String customText, int x, int y, int width, int height) {
        ButtonComponent button = new ButtonComponent(buttonId, index);
        button.properties.custom_text = customText != null ? customText : "";
        button.properties.texture_name = getButtonTextureName(index);
        button.properties.texture_scale = 1.0f;
        button.properties.show_text_over_texture = true;
        button.position = new Position(x, y, width, height);

        buttons.add(button);
        return button;
    }

    public CustomTextComponent addCustomText(String textId, String text, int x, int y, String color, float scale) {
        CustomTextComponent textComponent = new CustomTextComponent(textId);
        textComponent.properties.text = text != null ? text : "";
        textComponent.properties.color = color != null ? color : "#FFFFFF";
        textComponent.properties.font_scale = scale;
        textComponent.position = new Position(x, y, -1, -1);

        custom_texts.add(textComponent);
        return textComponent;
    }

    private String getButtonTextureName(int index) {
        return "button_" + index + ".png";
    }

    public ButtonComponent findButtonById(String buttonId) {
        return buttons.stream()
                .filter(button -> buttonId.equals(button.id))
                .findFirst()
                .orElse(null);
    }

    public CustomTextComponent findTextById(String textId) {
        return custom_texts.stream()
                .filter(text -> textId.equals(text.id))
                .findFirst()
                .orElse(null);
    }

    public void updateScreenResolution(int width, int height) {
        this.screen_resolution.width = width;
        this.screen_resolution.height = height;
        this.last_modified = LocalDateTime.now().format(TIME_FORMATTER);
    }

    public int getButtonCount() {
        return buttons.size();
    }

    public int getCustomTextCount() {
        return custom_texts.size();
    }

    public boolean validate() {
        if (background == null || background.texture_name == null || background.texture_name.isEmpty()) {
            LOGGER.warn("[EPIC ENGINE]: Invalid background configuration");
            return false;
        }

        if (title_image == null || title_image.texture_name == null || title_image.texture_name.isEmpty()) {
            LOGGER.warn("[EPIC ENGINE]: Invalid title image configuration");
            return false;
        }

        for (ButtonComponent button : buttons) {
            if (button.id == null || button.id.isEmpty()) {
                LOGGER.warn("[EPIC ENGINE]: Found button with empty ID");
                return false;
            }
            if (button.properties == null) {
                LOGGER.warn("[EPIC ENGINE]: Found button with null properties: {}", button.id);
                return false;
            }
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