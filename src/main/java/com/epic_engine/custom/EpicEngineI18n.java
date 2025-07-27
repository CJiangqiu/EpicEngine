package com.epic_engine.custom;

import com.epic_engine.config.EpicEngineCustomConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Epic Engine Internationalization System
 * Handles dynamic language file generation and translation management
 * Now includes automatic tip text generation based on layout configuration
 */
public class EpicEngineI18n {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Path LANG_DIR = EpicEngineCustomConfig.getCustomDir().resolve("lang");
    private static final Map<String, String> translations = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static String currentLanguage = "en_us";
    private static String lastDetectedLanguage = "en_us";
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) {
            return;
        }

        createLangDirectory();

        boolean hasExistingFiles = checkExistingLangFiles();
        if (!hasExistingFiles) {
            LOGGER.info("[EPIC ENGINE]: No existing language files found, creating default templates");
            createDefaultLangFiles();
            LOGGER.info("[EPIC ENGINE]: Default language templates created. You can customize them in: {}", LANG_DIR);
        } else {
            LOGGER.info("[EPIC ENGINE]: Existing language files detected, using custom translations");
        }

        loadCurrentLanguage();

        initialized = true;
        LOGGER.info("[EPIC ENGINE]: I18n system initialized with language: {}", currentLanguage);
    }

    private static void createLangDirectory() {
        try {
            Files.createDirectories(LANG_DIR);
            LOGGER.debug("[EPIC ENGINE]: Lang directory created: {}", LANG_DIR);
        } catch (IOException e) {
            LOGGER.error("[EPIC ENGINE]: Failed to create lang directory", e);
        }
    }

    private static boolean checkExistingLangFiles() {
        File enFile = LANG_DIR.resolve("en_us.json").toFile();
        File zhFile = LANG_DIR.resolve("zh_cn.json").toFile();
        return enFile.exists() || zhFile.exists();
    }

    private static void createDefaultLangFiles() {
        createDefaultEnglishFile();
        createDefaultChineseFile();
    }

    private static void createDefaultEnglishFile() {
        File enFile = LANG_DIR.resolve("en_us.json").toFile();
        if (!enFile.exists()) {
            try {
                JsonObject langJson = new JsonObject();

                // Main menu buttons
                langJson.addProperty("epic_engine.button.singleplayer", "Singleplayer");
                langJson.addProperty("epic_engine.button.multiplayer", "Multiplayer");
                langJson.addProperty("epic_engine.button.realms", "Minecraft Realms");
                langJson.addProperty("epic_engine.button.options", "Options");
                langJson.addProperty("epic_engine.button.quit", "Quit Game");
                langJson.addProperty("epic_engine.button.mods", "Mods");
                langJson.addProperty("epic_engine.button.language", "Language");
                langJson.addProperty("epic_engine.button.accessibility", "Accessibility");

                // Main menu texts
                langJson.addProperty("epic_engine.text.welcome", "Welcome to Epic Engine!");
                langJson.addProperty("epic_engine.text.version", "Version 1.0");
                langJson.addProperty("epic_engine.text.modpack_name", "My Awesome Modpack");

                // Editor interface
                langJson.addProperty("epic_engine.editor.edit_layout", "Edit Layout");
                langJson.addProperty("epic_engine.editor.exit_edit", "Exit Edit");
                langJson.addProperty("epic_engine.editor.save_layout", "Save Layout");
                langJson.addProperty("epic_engine.editor.edit_mode_title", "Layout Editor - Drag to move components");
                langJson.addProperty("epic_engine.editor.layout_saved", "Layout saved successfully!");
                langJson.addProperty("epic_engine.editor.prev_page", "◀ Prev");
                langJson.addProperty("epic_engine.editor.next_page", "Next ▶");
                langJson.addProperty("epic_engine.editor.page_main_menu", "Main Menu");
                langJson.addProperty("epic_engine.editor.page_loading_screen", "Loading Screen");

                // Loading screen tips - auto-generated from layout defaults
                addDefaultLoadingTips(langJson);

                // Loading screen editor
                langJson.addProperty("gui.epic_engine.loading_screen.edit_mode_title", "Loading Screen Editor - Drag to move components");
                langJson.addProperty("gui.epic_engine.loading_screen.dragging_info", "Dragging: %s");
                langJson.addProperty("gui.epic_engine.loading_screen.mouse_info", "Mouse: (%d, %d)");
                langJson.addProperty("gui.epic_engine.loading_screen.current_tip", "Tip %d/%d: %s");
                langJson.addProperty("gui.epic_engine.loading_screen.component_progress_bar", "Progress Bar (%d, %d) %dx%d");
                langJson.addProperty("gui.epic_engine.loading_screen.component_tip_text", "Tip Text: %s (%d, %d)");
                langJson.addProperty("gui.epic_engine.loading_screen.component_percentage", "Percentage (%d, %d)");
                langJson.addProperty("gui.epic_engine.loading_screen.component_custom_text", "Custom Text: %s (%d, %d)");

                // Main menu editor
                langJson.addProperty("gui.epic_engine.main_menu.edit_mode_title", "Main Menu Editor - Drag to move components");
                langJson.addProperty("gui.epic_engine.main_menu.dragging_info", "Dragging: %s - %s");
                langJson.addProperty("gui.epic_engine.main_menu.mouse_info", "Mouse: (%d, %d)");
                langJson.addProperty("gui.epic_engine.main_menu.layout_info", "Title: (%d, %d), Buttons: %d");
                langJson.addProperty("gui.epic_engine.main_menu.component_title", "Title Image (%d, %d)");
                langJson.addProperty("gui.epic_engine.main_menu.component_button", "Button: %s (%d, %d)");
                langJson.addProperty("gui.epic_engine.main_menu.component_text", "Text: %s (%d, %d)");

                saveJsonFile(langJson, enFile);
                LOGGER.info("[EPIC ENGINE]: Created default English lang file - you can customize it at: {}", enFile.getAbsolutePath());

            } catch (Exception e) {
                LOGGER.error("[EPIC ENGINE]: Failed to create default English lang file", e);
            }
        } else {
            LOGGER.debug("[EPIC ENGINE]: English lang file already exists, skipping auto-generation");
        }
    }

    /**
     * Add default loading tips to language file based on layout configuration
     */
    private static void addDefaultLoadingTips(JsonObject langJson) {
        // These correspond to the default tip_keys in LoadingScreenLayoutData.TipProperties
        langJson.addProperty("epic_engine.tip.welcome", "Welcome to Epic Engine!");
        langJson.addProperty("epic_engine.tip.customization", "Epic Engine makes modpack customization easy!");
        langJson.addProperty("epic_engine.tip.layout_editor", "You can customize this loading screen layout!");
        langJson.addProperty("epic_engine.tip.drag_components", "Drag components in edit mode to rearrange them");
        langJson.addProperty("epic_engine.tip.custom_images", "Loading screens can be customized with your own images");
        langJson.addProperty("epic_engine.tip.multilingual", "Epic Engine supports multiple languages");
        langJson.addProperty("epic_engine.tip.auto_rotation", "Tip texts rotate automatically every few seconds");

        LOGGER.info("[EPIC ENGINE]: Added {} default loading tips to language file", 7);
    }

    private static void createDefaultChineseFile() {
        File zhFile = LANG_DIR.resolve("zh_cn.json").toFile();
        if (!zhFile.exists()) {
            try {
                JsonObject langJson = new JsonObject();

                // Main menu buttons
                langJson.addProperty("epic_engine.button.singleplayer", "单人游戏");
                langJson.addProperty("epic_engine.button.multiplayer", "多人游戏");
                langJson.addProperty("epic_engine.button.realms", "Minecraft Realms");
                langJson.addProperty("epic_engine.button.options", "选项");
                langJson.addProperty("epic_engine.button.quit", "退出游戏");
                langJson.addProperty("epic_engine.button.mods", "模组");
                langJson.addProperty("epic_engine.button.language", "语言");
                langJson.addProperty("epic_engine.button.accessibility", "辅助功能");

                // Main menu texts
                langJson.addProperty("epic_engine.text.welcome", "欢迎使用 Epic Engine！");
                langJson.addProperty("epic_engine.text.version", "版本 1.0");
                langJson.addProperty("epic_engine.text.modpack_name", "我的整合包");

                // Editor interface
                langJson.addProperty("epic_engine.editor.edit_layout", "布局编辑");
                langJson.addProperty("epic_engine.editor.exit_edit", "退出编辑");
                langJson.addProperty("epic_engine.editor.save_layout", "保存布局");
                langJson.addProperty("epic_engine.editor.edit_mode_title", "布局编辑器 - 拖拽移动组件");
                langJson.addProperty("epic_engine.editor.layout_saved", "布局保存成功！");
                langJson.addProperty("epic_engine.editor.prev_page", "◀ 上一页");
                langJson.addProperty("epic_engine.editor.next_page", "下一页 ▶");
                langJson.addProperty("epic_engine.editor.page_main_menu", "主界面");
                langJson.addProperty("epic_engine.editor.page_loading_screen", "加载界面");

                // Loading screen tips - auto-generated from layout defaults
                addDefaultLoadingTipsChinese(langJson);

                // Loading screen editor
                langJson.addProperty("gui.epic_engine.loading_screen.edit_mode_title", "加载界面编辑器 - 拖拽移动组件");
                langJson.addProperty("gui.epic_engine.loading_screen.dragging_info", "正在拖拽：%s");
                langJson.addProperty("gui.epic_engine.loading_screen.mouse_info", "鼠标位置：(%d, %d)");
                langJson.addProperty("gui.epic_engine.loading_screen.current_tip", "提示 %d/%d：%s");
                langJson.addProperty("gui.epic_engine.loading_screen.component_progress_bar", "进度条 (%d, %d) %dx%d");
                langJson.addProperty("gui.epic_engine.loading_screen.component_tip_text", "提示文本：%s (%d, %d)");
                langJson.addProperty("gui.epic_engine.loading_screen.component_percentage", "百分比 (%d, %d)");
                langJson.addProperty("gui.epic_engine.loading_screen.component_custom_text", "自定义文本：%s (%d, %d)");

                // Main menu editor
                langJson.addProperty("gui.epic_engine.main_menu.edit_mode_title", "主界面编辑器 - 拖拽移动组件");
                langJson.addProperty("gui.epic_engine.main_menu.dragging_info", "正在拖拽：%s - %s");
                langJson.addProperty("gui.epic_engine.main_menu.mouse_info", "鼠标位置：(%d, %d)");
                langJson.addProperty("gui.epic_engine.main_menu.layout_info", "标题：(%d, %d)，按钮：%d");
                langJson.addProperty("gui.epic_engine.main_menu.component_title", "标题图片 (%d, %d)");
                langJson.addProperty("gui.epic_engine.main_menu.component_button", "按钮：%s (%d, %d)");
                langJson.addProperty("gui.epic_engine.main_menu.component_text", "文本：%s (%d, %d)");

                saveJsonFile(langJson, zhFile);
                LOGGER.info("[EPIC ENGINE]: Created default Chinese lang file - you can customize it at: {}", zhFile.getAbsolutePath());

            } catch (Exception e) {
                LOGGER.error("[EPIC ENGINE]: Failed to create default Chinese lang file", e);
            }
        } else {
            LOGGER.debug("[EPIC ENGINE]: Chinese lang file already exists, skipping auto-generation");
        }
    }

    /**
     * Add default loading tips in Chinese to language file
     */
    private static void addDefaultLoadingTipsChinese(JsonObject langJson) {
        // These correspond to the default tip_keys in LoadingScreenLayoutData.TipProperties
        langJson.addProperty("epic_engine.tip.welcome", "欢迎使用 Epic Engine！");
        langJson.addProperty("epic_engine.tip.customization", "Epic Engine 让整合包定制变得简单！");
        langJson.addProperty("epic_engine.tip.layout_editor", "你可以自定义这个加载界面的布局！");
        langJson.addProperty("epic_engine.tip.drag_components", "在编辑模式下拖拽组件来重新排列它们");
        langJson.addProperty("epic_engine.tip.custom_images", "加载界面可以使用你自己的图片进行定制");
        langJson.addProperty("epic_engine.tip.multilingual", "Epic Engine 支持多种语言");
        langJson.addProperty("epic_engine.tip.auto_rotation", "提示文本每隔几秒会自动切换");

        LOGGER.info("[EPIC ENGINE]: Added {} default loading tips to Chinese language file", 7);
    }

    private static void saveJsonFile(JsonObject jsonObject, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(jsonObject, writer);
        }
    }

    private static void loadCurrentLanguage() {
        currentLanguage = detectCurrentLanguage();
        lastDetectedLanguage = currentLanguage;

        LOGGER.info("[EPIC ENGINE]: Using language: {}", currentLanguage);

        loadTranslations(currentLanguage);

        if (translations.isEmpty() && !currentLanguage.equals("en_us")) {
            LOGGER.warn("[EPIC ENGINE]: No translations found for {}, falling back to en_us", currentLanguage);
            loadTranslations("en_us");
        }
    }

    private static void loadTranslations(String languageCode) {
        translations.clear();

        File langFile = LANG_DIR.resolve(languageCode + ".json").toFile();
        if (!langFile.exists()) {
            LOGGER.debug("[EPIC ENGINE]: Lang file not found: {}", langFile.getName());
            return;
        }

        try (FileReader reader = new FileReader(langFile, StandardCharsets.UTF_8)) {
            JsonObject langJson = GSON.fromJson(reader, JsonObject.class);

            if (langJson != null) {
                for (String key : langJson.keySet()) {
                    translations.put(key, langJson.get(key).getAsString());
                }

                LOGGER.info("[EPIC ENGINE]: Loaded {} translations for language: {}",
                        translations.size(), languageCode);
            }

        } catch (IOException e) {
            LOGGER.error("[EPIC ENGINE]: Failed to load translations for: {}", languageCode, e);
        }
    }

    public static String translate(String key) {
        if (!initialized) {
            initialize();
        }

        String translation = translations.get(key);
        if (translation != null) {
            return translation;
        }

        LOGGER.debug("[EPIC ENGINE]: Missing translation for key: {}", key);
        return key;
    }

    public static String translate(String key, String fallback) {
        String translation = translate(key);
        return translation.equals(key) ? fallback : translation;
    }

    public static boolean isTranslationKey(String text) {
        return text != null && text.startsWith("epic_engine.");
    }

    public static String getDisplayText(String text) {
        if (!initialized) {
            initialize();
        }

        checkAndReloadLanguage();

        if (isTranslationKey(text)) {
            return translate(text);
        }
        return text;
    }

    private static void checkAndReloadLanguage() {
        try {
            String detectedLanguage = detectCurrentLanguage();
            if (!detectedLanguage.equals(lastDetectedLanguage)) {
                LOGGER.info("[EPIC ENGINE]: Language changed from {} to {}, reloading translations",
                        lastDetectedLanguage, detectedLanguage);
                lastDetectedLanguage = detectedLanguage;
                currentLanguage = detectedLanguage;
                loadTranslations(currentLanguage);

                if (translations.isEmpty() && !currentLanguage.equals("en_us")) {
                    LOGGER.warn("[EPIC ENGINE]: No translations found for {}, falling back to en_us", currentLanguage);
                    loadTranslations("en_us");
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[EPIC ENGINE]: Error checking language change", e);
        }
    }

    private static String detectCurrentLanguage() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getLanguageManager() != null) {
                Object selected = mc.getLanguageManager().getSelected();
                if (selected != null) {
                    String langCode = selected.toString();
                    if (langCode.matches("[a-z]{2}_[a-z]{2}")) {
                        return langCode;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[EPIC ENGINE]: Could not detect language", e);
        }
        return "en_us";
    }

    public static void reloadLanguage() {
        loadCurrentLanguage();
        LOGGER.info("[EPIC ENGINE]: Reloaded language: {}", currentLanguage);
    }

    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    public static Path getLangDirectory() {
        return LANG_DIR;
    }

    /**
     * Add or update a translation key in the current language file
     */
    public static boolean addTranslation(String key, String value) {
        return addTranslation(key, value, currentLanguage);
    }

    /**
     * Add or update a translation key in a specific language file
     */
    public static boolean addTranslation(String key, String value, String languageCode) {
        if (key == null || value == null || languageCode == null) {
            return false;
        }

        try {
            File langFile = LANG_DIR.resolve(languageCode + ".json").toFile();
            JsonObject langJson;

            // Load existing file or create new
            if (langFile.exists()) {
                try (FileReader reader = new FileReader(langFile, StandardCharsets.UTF_8)) {
                    langJson = GSON.fromJson(reader, JsonObject.class);
                    if (langJson == null) {
                        langJson = new JsonObject();
                    }
                }
            } else {
                langJson = new JsonObject();
            }

            // Add or update the translation
            langJson.addProperty(key, value);

            // Save back to file
            saveJsonFile(langJson, langFile);

            // Update in-memory translations if it's the current language
            if (languageCode.equals(currentLanguage)) {
                translations.put(key, value);
            }

            LOGGER.info("[EPIC ENGINE]: Added translation '{}' = '{}' to {}", key, value, languageCode);
            return true;

        } catch (Exception e) {
            LOGGER.error("[EPIC ENGINE]: Failed to add translation", e);
            return false;
        }
    }

    /**
     * Remove a translation key from the current language file
     */
    public static boolean removeTranslation(String key) {
        return removeTranslation(key, currentLanguage);
    }

    /**
     * Remove a translation key from a specific language file
     */
    public static boolean removeTranslation(String key, String languageCode) {
        if (key == null || languageCode == null) {
            return false;
        }

        try {
            File langFile = LANG_DIR.resolve(languageCode + ".json").toFile();
            if (!langFile.exists()) {
                return false;
            }

            JsonObject langJson;
            try (FileReader reader = new FileReader(langFile, StandardCharsets.UTF_8)) {
                langJson = GSON.fromJson(reader, JsonObject.class);
                if (langJson == null) {
                    return false;
                }
            }

            // Remove the translation
            boolean removed = langJson.remove(key) != null;
            if (removed) {
                // Save back to file
                saveJsonFile(langJson, langFile);

                // Update in-memory translations if it's the current language
                if (languageCode.equals(currentLanguage)) {
                    translations.remove(key);
                }

                LOGGER.info("[EPIC ENGINE]: Removed translation '{}' from {}", key, languageCode);
            }

            return removed;

        } catch (Exception e) {
            LOGGER.error("[EPIC ENGINE]: Failed to remove translation", e);
            return false;
        }
    }

    /**
     * Get all translation keys for the current language
     */
    public static java.util.Set<String> getAllTranslationKeys() {
        if (!initialized) {
            initialize();
        }
        return new java.util.HashSet<>(translations.keySet());
    }

    /**
     * Get all translations for the current language
     */
    public static Map<String, String> getAllTranslations() {
        if (!initialized) {
            initialize();
        }
        return new HashMap<>(translations);
    }

    /**
     * Check if a translation exists for the given key
     */
    public static boolean hasTranslation(String key) {
        if (!initialized) {
            initialize();
        }
        return translations.containsKey(key);
    }

    /**
     * Get available language codes
     */
    public static java.util.List<String> getAvailableLanguages() {
        java.util.List<String> languages = new java.util.ArrayList<>();

        try {
            if (Files.exists(LANG_DIR)) {
                Files.list(LANG_DIR)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> {
                            String filename = path.getFileName().toString();
                            String langCode = filename.substring(0, filename.length() - 5); // Remove .json
                            languages.add(langCode);
                        });
            }
        } catch (IOException e) {
            LOGGER.error("[EPIC ENGINE]: Failed to list available languages", e);
        }

        return languages;
    }

    /**
     * Create a new language file based on the English template
     */
    public static boolean createLanguageFile(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return false;
        }

        try {
            File sourceFile = LANG_DIR.resolve("en_us.json").toFile();
            File targetFile = LANG_DIR.resolve(languageCode + ".json").toFile();

            if (targetFile.exists()) {
                LOGGER.warn("[EPIC ENGINE]: Language file already exists: {}", languageCode);
                return false;
            }

            if (!sourceFile.exists()) {
                LOGGER.error("[EPIC ENGINE]: English template file not found");
                return false;
            }

            // Copy English file as template
            Files.copy(sourceFile.toPath(), targetFile.toPath());

            LOGGER.info("[EPIC ENGINE]: Created new language file: {}", languageCode);
            return true;

        } catch (Exception e) {
            LOGGER.error("[EPIC ENGINE]: Failed to create language file for: {}", languageCode, e);
            return false;
        }
    }

    /**
     * Get statistics about the current language file
     */
    public static String getLanguageStatistics() {
        if (!initialized) {
            initialize();
        }

        StringBuilder stats = new StringBuilder();
        stats.append("Language Statistics:\n");
        stats.append("- Current Language: ").append(currentLanguage).append("\n");
        stats.append("- Total Translations: ").append(translations.size()).append("\n");
        stats.append("- Available Languages: ").append(getAvailableLanguages()).append("\n");

        // Count translations by category
        int buttonCount = 0, tipCount = 0, editorCount = 0, guiCount = 0, textCount = 0;
        for (String key : translations.keySet()) {
            if (key.startsWith("epic_engine.button.")) buttonCount++;
            else if (key.startsWith("epic_engine.tip.")) tipCount++;
            else if (key.startsWith("epic_engine.editor.")) editorCount++;
            else if (key.startsWith("gui.epic_engine.")) guiCount++;
            else if (key.startsWith("epic_engine.text.")) textCount++;
        }

        stats.append("- Button Translations: ").append(buttonCount).append("\n");
        stats.append("- Tip Translations: ").append(tipCount).append("\n");
        stats.append("- Editor Translations: ").append(editorCount).append("\n");
        stats.append("- GUI Translations: ").append(guiCount).append("\n");
        stats.append("- Text Translations: ").append(textCount).append("\n");

        return stats.toString();
    }
}