package com.epic_engine.custom;

import com.epic_engine.mixin.LevelLoadingScreenAccessor;
import com.epic_engine.mixin.ReceivingLevelScreenAccessor;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Real Progress Extractor - Corrected Version
 */
public class RealProgressExtractor {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Extract real progress from any screen instance
     */
    public static Optional<Float> extractRealProgress(Object screenInstance) {

        // Check if screen instance can be cast to accessor
        if (screenInstance.getClass().getName().contains("LevelLoadingScreen")) {
            return extractLevelLoadingProgress(screenInstance);
        }

        if (screenInstance.getClass().getName().contains("ReceivingLevelScreen")) {
            return extractTerrainProgress(screenInstance);
        }

        return Optional.empty();
    }

    /**
     * Extract real chunk loading progress using cast
     */
    private static Optional<Float> extractLevelLoadingProgress(Object screenInstance) {
        try {
            // Cast to accessor interface
            LevelLoadingScreenAccessor accessor = (LevelLoadingScreenAccessor) screenInstance;
            StoringChunkProgressListener progressListener = accessor.epic_engine$getProgressListener();

            if (progressListener != null) {
                // Get real progress using public method
                int progress = progressListener.getProgress();
                float progressFloat = Math.max(0.0f, Math.min(100.0f, progress)) / 100.0f;

                LOGGER.debug("[EPIC ENGINE]: Real chunk progress: {}%", progress);
                return Optional.of(progressFloat);
            }
        } catch (Exception e) {
            LOGGER.debug("[EPIC ENGINE]: Failed to extract level loading progress", e);
        }

        return Optional.empty();
    }

    /**
     * Extract terrain download progress using cast
     */
    private static Optional<Float> extractTerrainProgress(Object screenInstance) {
        try {
            // Cast to accessor interface
            ReceivingLevelScreenAccessor accessor = (ReceivingLevelScreenAccessor) screenInstance;

            long createdAt = accessor.epic_engine$getCreatedAt();
            boolean packetsReceived = accessor.epic_engine$isLoadingPacketsReceived();

            long elapsed = System.currentTimeMillis() - createdAt;
            long maxWaitTime = 30000L; // 30 seconds

            float timeProgress = Math.min(1.0f, elapsed / (float) maxWaitTime);

            // Boost progress if packets received
            if (packetsReceived) {
                timeProgress = Math.max(timeProgress, 0.5f);
            }

            LOGGER.debug("[EPIC ENGINE]: Terrain progress: {}% (packets: {})",
                    timeProgress * 100, packetsReceived);

            return Optional.of(timeProgress);

        } catch (Exception e) {
            LOGGER.debug("[EPIC ENGINE]: Failed to extract terrain progress", e);
        }

        return Optional.empty();
    }
}