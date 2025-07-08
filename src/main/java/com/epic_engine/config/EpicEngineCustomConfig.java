package com.epic_engine.config;

import com.epic_engine.EpicEngineMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;


public class EpicEngineCustomConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final ForgeConfigSpec CONFIG;

    // 资源目录路径
    private static final File Epic_Engine_DIR = new File(FMLPaths.CONFIGDIR.get().toFile(), "epic_engine");
    private static final File TEXTURES_DIR = new File(Epic_Engine_DIR, "textures");
    private static final File VIDEOS_DIR = new File(Epic_Engine_DIR, "videos");
    private static final File SOUNDS_DIR = new File(Epic_Engine_DIR, "sounds");

    // ======== 常规设置 ========
    /** 主开关。设为false时，所有自定义功能都将被禁用 */
    public static final ForgeConfigSpec.BooleanValue ENABLE_CUSTOMIZATION;

    // ======== 游戏窗口设置 ========
    /** 是否启用自定义游戏窗口标题 */
    public static final ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_WINDOW_TITLE;

    /** 自定义游戏窗口标题文本 */
    public static final ForgeConfigSpec.ConfigValue<String> CUSTOM_WINDOW_TITLE;

    // ======== 加载界面设置 ========
    /** 是否启用自定义加载界面 */
    public static final ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_LOADING_SCREEN;

    /** 自定义加载界面背景图片文件名 */
    public static final ForgeConfigSpec.ConfigValue<String> LOADING_SCREEN_BACKGROUND;

    /** 自定义加载进度条颜色 */
    public static final ForgeConfigSpec.ConfigValue<String> LOADING_BAR_COLOR;

    /** 自定义加载文本颜色 */
    public static final ForgeConfigSpec.ConfigValue<String> LOADING_TEXT_COLOR;

    /** 是否显示加载百分比 */
    public static final ForgeConfigSpec.BooleanValue SHOW_LOADING_PERCENTAGE;

    /** 自定义加载提示文本列表 */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LOADING_TIPS;

    // ======== 主界面设置 ========
    /** 是否启用自定义主界面 */
    public static final ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_MAIN_MENU;

    /** 自定义主界面背景图片文件名 */
    public static final ForgeConfigSpec.ConfigValue<String> MAIN_MENU_BACKGROUND;

    /** 自定义主界面音乐文件名 */
    public static final ForgeConfigSpec.ConfigValue<String> MAIN_MENU_MUSIC;

    /** 自定义主界面标题图片文件名 */
    public static final ForgeConfigSpec.ConfigValue<String> MAIN_MENU_TITLE_IMAGE;

    /** 是否启用自定义按钮文本 */
    public static final ForgeConfigSpec.BooleanValue CUSTOM_BUTTON_TEXT;

    /** 单人游戏按钮文本 */
    public static final ForgeConfigSpec.ConfigValue<String> SINGLEPLAYER_BUTTON_TEXT;

    /** 多人游戏按钮文本 */
    public static final ForgeConfigSpec.ConfigValue<String> MULTIPLAYER_BUTTON_TEXT;

    /** 模组按钮文本 */
    public static final ForgeConfigSpec.ConfigValue<String> MODS_BUTTON_TEXT;

    /** 退出按钮文本 */
    public static final ForgeConfigSpec.ConfigValue<String> QUIT_BUTTON_TEXT;

    // ======== 视频播放设置 ========
    /** 是否启用进入世界时的视频播放 */
    public static final ForgeConfigSpec.BooleanValue ENABLE_INTRO_VIDEOS;

    /** 默认视频文件名 */
    public static final ForgeConfigSpec.ConfigValue<String> DEFAULT_INTRO_VIDEO;

    /** 是否允许跳过视频 */
    public static final ForgeConfigSpec.BooleanValue ALLOW_VIDEO_SKIP;

    /** 长按ESC跳过视频所需的时间（毫秒） */
    public static final ForgeConfigSpec.IntValue VIDEO_SKIP_HOLD_TIME;

    /** 是否只在首次进入世界时播放视频 */
    public static final ForgeConfigSpec.BooleanValue PLAY_VIDEO_ONLY_FIRST_JOIN;

    /** 是否为每个维度设置不同的视频 */
    public static final ForgeConfigSpec.BooleanValue DIMENSION_SPECIFIC_VIDEOS;

    /** 主世界视频文件名 */
    public static final ForgeConfigSpec.ConfigValue<String> OVERWORLD_VIDEO;

    /** 下界视频文件名 */
    public static final ForgeConfigSpec.ConfigValue<String> NETHER_VIDEO;

    /** 末地视频文件名 */
    public static final ForgeConfigSpec.ConfigValue<String> END_VIDEO;

    static {
        // 创建必要的目录
        createDirectories();

        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // 常规设置
        builder.push("general");
        ENABLE_CUSTOMIZATION = builder
                .comment("Master switch. When false, all customization features will be disabled.")
                .define("enableCustomization", true);
        builder.pop();

        // 游戏窗口设置
        builder.push("window");
        ENABLE_CUSTOM_WINDOW_TITLE = builder
                .comment("Enable custom game window title.")
                .define("enableCustomWindowTitle", true);
        CUSTOM_WINDOW_TITLE = builder
                .comment("Custom game window title text.")
                .define("customWindowTitle", "Epic Engine:A good tool to make your modpack!");
        builder.pop();

        // 加载界面设置
        builder.push("loading_screen");
        ENABLE_CUSTOM_LOADING_SCREEN = builder
                .comment("Enable custom loading screen.")
                .define("enableCustomLoadingScreen", false);
        LOADING_SCREEN_BACKGROUND = builder
                .comment("Custom loading screen background image filename in config/epic_engine/textures/. Recommended size: 1920x1080.")
                .define("loadingScreenBackground", "loading.png");
        LOADING_BAR_COLOR = builder
                .comment("Custom loading bar color (hexadecimal RGB or RGBA format).")
                .define("loadingBarColor", "#55AAFF");
        LOADING_TEXT_COLOR = builder
                .comment("Custom loading text color (hexadecimal RGB or RGBA format).")
                .define("loadingTextColor", "#FFFFFF");
        SHOW_LOADING_PERCENTAGE = builder
                .comment("Show loading percentage.")
                .define("showLoadingPercentage", true);
        LOADING_TIPS = builder
                .comment("Custom loading tips list, randomly displayed during loading.")
                .defineList("loadingTips",
                        Arrays.asList(
                                "Tip: Epic Engine brings you more realistic mountains and terrain!",
                                "Tip: You can see farther from mountaintops!",
                                "Tip: Remember to prepare for your adventure!",
                                "Tip: Explore newly generated valleys and canyons!",
                                "Tip: Don't forget to backup your world saves!"
                        ), s -> s instanceof String);
        builder.pop();

        // 主界面设置
        builder.push("main_menu");
        ENABLE_CUSTOM_MAIN_MENU = builder
                .comment("Enable custom main menu.")
                .define("enableCustomMainMenu", true);
        MAIN_MENU_BACKGROUND = builder
                .comment("Custom main menu background image filename in config/epic_engine/textures/. Recommended size: 1920x1080 or match your game resolution.")
                .define("mainMenuBackground", "background.png");
        MAIN_MENU_MUSIC = builder
                .comment("Custom main menu music filename in config/epic_engine/sounds/.")
                .define("mainMenuMusic", "menu.ogg");
        MAIN_MENU_TITLE_IMAGE = builder
                .comment("Custom main menu title image filename in config/epic_engine/textures/. Recommended size: 1024x256 with transparent background.")
                .define("mainMenuTitleImage", "title.png");

        // 按钮文本设置
        builder.push("buttons");
        CUSTOM_BUTTON_TEXT = builder
                .comment("Enable custom button text.")
                .define("customButtonText", false);
        SINGLEPLAYER_BUTTON_TEXT = builder
                .comment("Singleplayer button text.")
                .define("singleplayerButtonText", "Start Adventure");
        MULTIPLAYER_BUTTON_TEXT = builder
                .comment("Multiplayer button text.")
                .define("multiplayerButtonText", "Multiplayer World");
        MODS_BUTTON_TEXT = builder
                .comment("Mods button text.")
                .define("modsButtonText", "Mod Options");
        QUIT_BUTTON_TEXT = builder
                .comment("Quit button text.")
                .define("quitButtonText", "End Adventure");
        builder.pop(); // 按钮文本

        builder.pop(); // 主界面

        // 视频播放设置
        builder.push("intro_videos");
        ENABLE_INTRO_VIDEOS = builder
                .comment("Enable intro videos when entering worlds.")
                .define("enableIntroVideos", false);
        DEFAULT_INTRO_VIDEO = builder
                .comment("Default intro video filename in config/epic_engine/videos/. Recommended format: MP4 with H.264 encoding.")
                .define("defaultIntroVideo", "default.mp4");
        ALLOW_VIDEO_SKIP = builder
                .comment("Allow players to skip videos.")
                .define("allowVideoSkip", true);
        VIDEO_SKIP_HOLD_TIME = builder
                .comment("Time in milliseconds to hold ESC to skip video.")
                .defineInRange("videoSkipHoldTime", 750, 100, 5000);
        PLAY_VIDEO_ONLY_FIRST_JOIN = builder
                .comment("Only play videos on first world join.")
                .define("playVideoOnlyFirstJoin", true);

        // 维度特定视频
        builder.push("dimension_videos");
        DIMENSION_SPECIFIC_VIDEOS = builder
                .comment("Use different videos for each dimension.")
                .define("dimensionSpecificVideos", false);
        OVERWORLD_VIDEO = builder
                .comment("Overworld video filename in config/epic_engine/videos/.")
                .define("overworldVideo", "overworld.mp4");
        NETHER_VIDEO = builder
                .comment("Nether video filename in config/epic_engine/videos/.")
                .define("netherVideo", "nether.mp4");
        END_VIDEO = builder
                .comment("End video filename in config/epic_engine/videos/.")
                .define("endVideo", "end.mp4");
        builder.pop(); // 维度特定视频

        builder.pop(); // 视频播放

        CONFIG = builder.build();
    }

    /**
     * 创建必要的目录结构
     */
    private static void createDirectories() {
        try {
            // 创建主目录和子目录
            Epic_Engine_DIR.mkdirs();
            TEXTURES_DIR.mkdirs();
            VIDEOS_DIR.mkdirs();
            SOUNDS_DIR.mkdirs();

            LOGGER.info("Epic Engine resource directories created or verified");
        } catch (Exception e) {
            LOGGER.error("Failed to create Epic Engine resource directories", e);
        }
    }

    /**
     * 初始化配置目录和默认资源
     * 在模组初始化时调用
     */
    public static void initializeResources() {
        try {
            // 确保目录存在
            createDirectories();

            // 提取默认资源
            extractDefaultResourceIfNeeded("textures/custom/background.png", getTextureFile("background.png"));
            extractDefaultResourceIfNeeded("textures/custom/title.png", getTextureFile("title.png"));
            extractDefaultResourceIfNeeded("textures/custom/loading.png", getTextureFile("loading.png"));

            LOGGER.info("Epic Engine default resources initialized");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Epic Engine default resources", e);
        }
    }

    /**
     * 如果目标文件不存在，则从模组资源中提取默认文件
     */
    private static void extractDefaultResourceIfNeeded(String resourcePath, File targetFile) {
        // 如果目标文件已存在，不需要提取
        if (targetFile.exists()) {
            return;
        }

        try {
            // 从模组资源中加载默认文件
            InputStream inputStream = EpicEngineCustomConfig.class.getClassLoader()
                .getResourceAsStream("assets/" + EpicEngineMod.MODID + "/" + resourcePath);

            if (inputStream != null) {
                // 确保目标目录存在
                targetFile.getParentFile().mkdirs();

                // 复制到目标位置
                FileUtils.copyInputStreamToFile(inputStream, targetFile);
                LOGGER.info("Extracted default resource to: {}", targetFile.getPath());

                inputStream.close();
            } else {
                LOGGER.warn("Default resource not found: assets/{}/{}", EpicEngineMod.MODID, resourcePath);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to extract default resource: {}", resourcePath, e);
        }
    }

    /**
     * 获取完整的纹理文件路径
     *
     * @param filename 纹理文件名
     * @return 完整的文件对象
     */
    public static File getTextureFile(String filename) {
        return new File(TEXTURES_DIR, filename);
    }

    /**
     * 获取完整的视频文件路径
     *
     * @param filename 视频文件名
     * @return 完整的文件对象
     */
    public static File getVideoFile(String filename) {
        return new File(VIDEOS_DIR, filename);
    }

    /**
     * 获取完整的音频文件路径
     *
     * @param filename 音频文件名
     * @return 完整的文件对象
     */
    public static File getSoundFile(String filename) {
        return new File(SOUNDS_DIR, filename);
    }

    /**
     * 获取特定维度的视频路径
     *
     * @param dimensionId 维度ID，例如"minecraft:overworld"
     * @return 视频文件名
     */
    public static String getVideoForDimension(String dimensionId) {
        if (!DIMENSION_SPECIFIC_VIDEOS.get()) {
            return DEFAULT_INTRO_VIDEO.get();
        }

        switch (dimensionId) {
            case "minecraft:overworld":
                return OVERWORLD_VIDEO.get();
            case "minecraft:the_nether":
                return NETHER_VIDEO.get();
            case "minecraft:the_end":
                return END_VIDEO.get();
            default:
                return DEFAULT_INTRO_VIDEO.get();
        }
    }

    /**
     * 获取指定维度的视频文件
     *
     * @param dimensionId 维度ID
     * @return 视频文件对象
     */
    public static File getVideoFileForDimension(String dimensionId) {
        return getVideoFile(getVideoForDimension(dimensionId));
    }

    /**
     * 检查是否应该播放视频
     *
     * @return 如果启用了视频播放功能，返回true
     */
    public static boolean shouldPlayVideos() {
        return ENABLE_CUSTOMIZATION.get() && ENABLE_INTRO_VIDEOS.get();
    }

    /**
     * 获取游戏窗口标题
     *
     * @return 自定义窗口标题，如果未启用则返回null
     */
    public static String getWindowTitle() {
        if (!ENABLE_CUSTOMIZATION.get() || !ENABLE_CUSTOM_WINDOW_TITLE.get()) {
            return null; // 返回null表示使用默认标题
        }

        return CUSTOM_WINDOW_TITLE.get();
    }

    /**
     * 检查文件是否存在
     *
     * @param file 文件对象
     * @return 如果文件存在且可读返回true
     */
    public static boolean fileExists(File file) {
        return file != null && file.exists() && file.isFile() && file.canRead();
    }
}