package com.epic_engine.mixin;

import com.epic_engine.config.EpicEngineCustomConfig;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

@Mixin(Minecraft.class)
public class TitleMixin {

    @Unique private static final Logger epicEngine$LOGGER = LogManager.getLogger();

    @Unique
    private void epicEngine$applyCustomTitle() {
        Minecraft mc = (Minecraft)(Object)this;
        if (mc.getWindow() != null) {
            String title = EpicEngineCustomConfig.getWindowTitle();
            if (title != null && !title.isEmpty()) {
                mc.getWindow().setTitle(title);
            }
        }
    }

    @Unique
    private void epicEngine$applyCustomIcon() {
        epicEngine$LOGGER.info("[EPIC ENGINE]: Attempting to apply custom window icon");

        if (!EpicEngineCustomConfig.isWindowIconEnabled()) {
            epicEngine$LOGGER.info("[EPIC ENGINE]: Custom window icon is disabled");
            return;
        }

        Minecraft mc = (Minecraft)(Object)this;
        if (mc.getWindow() == null) {
            epicEngine$LOGGER.warn("[EPIC ENGINE]: Window is null, cannot set icon");
            return;
        }

        try {
            File icon16 = EpicEngineCustomConfig.getTextureFile(
                    EpicEngineCustomConfig.WINDOW_ICON_16_FILENAME.get());
            File icon32 = EpicEngineCustomConfig.getTextureFile(
                    EpicEngineCustomConfig.WINDOW_ICON_32_FILENAME.get());

            boolean has16 = icon16.exists() && icon16.isFile();
            boolean has32 = icon32.exists() && icon32.isFile();

            epicEngine$LOGGER.info("[EPIC ENGINE]: Icon file check - 16x16: {}, 32x32: {}", has16, has32);

            if (!has16 && !has32) {
                epicEngine$LOGGER.warn("[EPIC ENGINE]: No custom window icon files found");
                return;
            }

            long windowHandle = mc.getWindow().getWindow();
            epicEngine$LOGGER.info("[EPIC ENGINE]: Window handle: {}", windowHandle);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                GLFWImage.Buffer buffer = GLFWImage.malloc(2, stack);
                int count = 0;

                if (has16) {
                    epicEngine$LOGGER.info("[EPIC ENGINE]: Loading 16x16 icon: {}", icon16.getAbsolutePath());
                    ByteBuffer data16 = epicEngine$loadIcon(icon16, stack);
                    if (data16 != null) {
                        buffer.position(count).width(16).height(16).pixels(data16);
                        count++;
                        epicEngine$LOGGER.info("[EPIC ENGINE]: 16x16 icon loaded successfully");
                    } else {
                        epicEngine$LOGGER.error("[EPIC ENGINE]: Failed to load 16x16 icon");
                    }
                }

                if (has32) {
                    epicEngine$LOGGER.info("[EPIC ENGINE]: Loading 32x32 icon: {}", icon32.getAbsolutePath());
                    ByteBuffer data32 = epicEngine$loadIcon(icon32, stack);
                    if (data32 != null) {
                        buffer.position(count).width(32).height(32).pixels(data32);
                        count++;
                        epicEngine$LOGGER.info("[EPIC ENGINE]: 32x32 icon loaded successfully");
                    } else {
                        epicEngine$LOGGER.error("[EPIC ENGINE]: Failed to load 32x32 icon");
                    }
                }

                if (count > 0) {
                    buffer.position(0).limit(count);
                    GLFW.glfwSetWindowIcon(windowHandle, buffer);
                    epicEngine$LOGGER.info("[EPIC ENGINE]: Custom window icon set successfully with {} images", count);
                } else {
                    epicEngine$LOGGER.error("[EPIC ENGINE]: No valid icon images to set");
                }
            }
        } catch (Exception e) {
            epicEngine$LOGGER.error("[EPIC ENGINE]: Failed to set custom window icon", e);
        }
    }

    @Unique
    private ByteBuffer epicEngine$loadIcon(File file, MemoryStack stack) {
        try (FileInputStream fis = new FileInputStream(file);
             FileChannel fc = fis.getChannel()) {

            epicEngine$LOGGER.info("[EPIC ENGINE]: Reading icon file: {} (size: {} bytes)",
                    file.getName(), fc.size());

            ByteBuffer buf = ByteBuffer.allocateDirect((int) fc.size());
            fc.read(buf);
            buf.flip();

            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1), c = stack.mallocInt(1);
            ByteBuffer img = STBImage.stbi_load_from_memory(buf, w, h, c, 4);

            if (img == null) {
                String reason = STBImage.stbi_failure_reason();
                epicEngine$LOGGER.error("[EPIC ENGINE]: STB failed to load icon {}: {}", file.getName(), reason);
                return null;
            }

            int wi = w.get(0), hi = h.get(0);
            epicEngine$LOGGER.info("[EPIC ENGINE]: Icon {} loaded: {}x{}, channels: {}",
                    file.getName(), wi, hi, c.get(0));

            // 如果不是标准尺寸，进行缩放
            if (wi != hi || (wi != 16 && wi != 32)) {
                int targetSize = (wi <= 24) ? 16 : 32;
                epicEngine$LOGGER.info("[EPIC ENGINE]: Scaling icon from {}x{} to {}x{}",
                        wi, hi, targetSize, targetSize);
                ByteBuffer scaled = epicEngine$scaleIcon(img, wi, hi, targetSize, targetSize);
                STBImage.stbi_image_free(img);
                return scaled;
            }
            return img;
        } catch (IOException e) {
            epicEngine$LOGGER.error("[EPIC ENGINE]: Failed to read icon file: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    @Unique
    private ByteBuffer epicEngine$scaleIcon(ByteBuffer orig, int sw, int sh, int dw, int dh) {
        ByteBuffer dst = MemoryUtil.memAlloc(dw * dh * 4);
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                int sx = x * sw / dw, sy = y * sh / dh;
                int si = (sy * sw + sx) * 4, di = (y * dw + x) * 4;
                for (int i = 0; i < 4; i++) {
                    dst.put(di + i, orig.get(si + i));
                }
            }
        }
        return dst;
    }

    @Inject(method = "updateTitle", at = @At("HEAD"), cancellable = true)
    private void epicEngine$onUpdateTitle(CallbackInfo ci) {
        if (EpicEngineCustomConfig.isWindowTitleEnabled()) {
            epicEngine$applyCustomTitle();
            ci.cancel();
        }
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void epicEngine$onConstructed(CallbackInfo ci) {
        Minecraft mc = (Minecraft)(Object)this;
        mc.tell(() -> {
            // 设置自定义标题
            if (EpicEngineCustomConfig.isWindowTitleEnabled()) {
                epicEngine$applyCustomTitle();
            }
            // 设置自定义图标 - 只尝试一次，延迟执行确保窗口完全初始化
            if (EpicEngineCustomConfig.isWindowIconEnabled()) {
                try {
                    Thread.sleep(500); // 延迟500ms确保窗口完全初始化
                    epicEngine$applyCustomIcon();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    epicEngine$LOGGER.warn("[EPIC ENGINE]: Icon setup interrupted");
                }
            }
        });
    }
}