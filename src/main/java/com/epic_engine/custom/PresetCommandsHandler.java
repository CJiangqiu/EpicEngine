package com.epic_engine.custom;

import com.epic_engine.config.EpicEngineCustomConfig;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber
public class PresetCommandsHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static MinecraftServer serverInstance;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        serverInstance = event.getServer();
        LOGGER.info("[EPIC ENGINE]: PresetCommandsHandler initialized");
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        // 使用智能配置获取方法 - 支持服务端配置覆盖
        if (!EpicEngineCustomConfig.getEffectivePresetCommandsEnabled()
                || !EpicEngineCustomConfig.OTHERS_MODULE_ENABLED.get()) {
            LOGGER.debug("[EPIC ENGINE]: Preset commands disabled (config source: {})",
                    EpicEngineCustomConfig.getConfigSource());
            return;
        }

        // 使用主世界存储全局状态（不受玩家当前维度影响）
        ServerLevel overworld = serverInstance.overworld();
        PresetData data = getGlobalPresetData(overworld);

        // 如果已经执行过，直接返回
        if (data.hasExecuted()) {
            LOGGER.debug("[EPIC ENGINE]: Preset commands already executed for this server");
            return;
        }

        LOGGER.info("[EPIC ENGINE]: First player joined, triggering preset commands execution (config source: {})",
                EpicEngineCustomConfig.getConfigSource());
        scheduleCommandExecution(overworld);
    }

    /**
     * 获取全局预设数据（存储在主世界，但代表整个服务器的状态）
     */
    private static PresetData getGlobalPresetData(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                PresetData::load,
                PresetData::new,
                "epic_engine_global_preset_commands"  // 明确标明这是全局状态
        );
    }

    private static void scheduleCommandExecution(ServerLevel overworld) {
        // 使用智能配置获取方法
        int delayTicks = EpicEngineCustomConfig.getEffectivePresetCommandsDelay();
        long delayMillis = delayTicks * 50L;

        scheduler.schedule(() -> {
            try {
                executeGlobalPresetCommands(overworld);
            } catch (Exception e) {
                LOGGER.error("[EPIC ENGINE]: Error executing global preset commands", e);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    private static void executeGlobalPresetCommands(ServerLevel overworld) {
        if (serverInstance == null) {
            return;
        }

        // 再次使用智能配置检查 - 支持运行时配置变更
        if (!EpicEngineCustomConfig.getEffectivePresetCommandsEnabled()
                || !EpicEngineCustomConfig.OTHERS_MODULE_ENABLED.get()) {
            LOGGER.warn("[EPIC ENGINE]: Preset commands disabled during execution, aborting (config source: {})",
                    EpicEngineCustomConfig.getConfigSource());
            return;
        }

        PresetData data = getGlobalPresetData(overworld);
        if (data.hasExecuted()) {
            LOGGER.debug("[EPIC ENGINE]: Global preset commands already executed");
            return;
        }

        // 使用智能配置获取命令列表
        String raw = EpicEngineCustomConfig.getEffectivePresetCommandsList();
        String[] commands = raw == null || raw.isBlank()
                ? new String[0]
                : raw.split(";");

        if (commands.length == 0) {
            data.markAsExecuted();
            LOGGER.info("[EPIC ENGINE]: No preset commands configured, marking as executed (config source: {})",
                    EpicEngineCustomConfig.getConfigSource());
            return;
        }

        LOGGER.info("[EPIC ENGINE]: Executing {} global preset commands for server initialization (config source: {})",
                commands.length, EpicEngineCustomConfig.getConfigSource());

        Commands commandManager = serverInstance.getCommands();
        var source = serverInstance.createCommandSourceStack()
                .withLevel(overworld)  // 在主世界上下文中执行命令
                .withPermission(4)
                .withSuppressedOutput();

        AtomicInteger successCount = new AtomicInteger(0);

        // 异步执行每个命令，避免阻塞调度器线程
        for (int i = 0; i < commands.length; i++) {
            String cmd = commands[i].trim();
            if (cmd.isEmpty()) continue;

            final int commandIndex = i;
            final boolean isLastCommand = (i == commands.length - 1);
            final String finalCmd = cmd;

            // 每个命令延迟执行，避免阻塞
            scheduler.schedule(() -> {
                try {
                    LOGGER.info("[EPIC ENGINE]: Running {} command {}/{}: {}",
                            EpicEngineCustomConfig.getConfigSource(),
                            commandIndex + 1, commands.length, finalCmd);
                    int result = commandManager.performPrefixedCommand(source, finalCmd);
                    if (result > 0) {
                        successCount.incrementAndGet();
                    }

                    // 如果是最后一个命令，标记完成
                    if (isLastCommand) {
                        data.markAsExecuted();
                        LOGGER.info("[EPIC ENGINE]: Global preset commands completed: {}/{} succeeded (config source: {})",
                                successCount.get(), commands.length, EpicEngineCustomConfig.getConfigSource());
                        LOGGER.info("[EPIC ENGINE]: Server initialization commands will not run again for this world");
                    }
                } catch (Exception e) {
                    LOGGER.error("[EPIC ENGINE]: Failed to run {} preset command: {}",
                            EpicEngineCustomConfig.getConfigSource(), finalCmd, e);

                    // 即使失败，最后一个命令也要标记完成
                    if (isLastCommand) {
                        data.markAsExecuted();
                        LOGGER.info("[EPIC ENGINE]: Global preset commands completed with errors: {}/{} succeeded (config source: {})",
                                successCount.get(), commands.length, EpicEngineCustomConfig.getConfigSource());
                    }
                }
            }, 50L * commandIndex, TimeUnit.MILLISECONDS);
        }
    }

    /** 重置全局执行状态（测试用） */
    public static void resetGlobalExecution() {
        if (serverInstance != null) {
            PresetData data = getGlobalPresetData(serverInstance.overworld());
            data.reset();
            LOGGER.info("[EPIC ENGINE]: Global preset commands execution status reset - commands will run again");
        }
    }

    /** 查询是否已全局执行过（测试用） */
    public static boolean hasGloballyExecuted() {
        if (serverInstance != null) {
            PresetData data = getGlobalPresetData(serverInstance.overworld());
            return data.hasExecuted();
        }
        return false;
    }

    /** 手动触发预设命令执行（管理员工具） */
    public static boolean forceExecuteCommands(ServerPlayer admin) {
        if (serverInstance == null) {
            return false;
        }

        // 检查权限
        if (!admin.hasPermissions(4)) {
            LOGGER.warn("[EPIC ENGINE]: Player {} attempted to force execute preset commands without permission",
                    admin.getName().getString());
            return false;
        }

        ServerLevel overworld = serverInstance.overworld();
        PresetData data = getGlobalPresetData(overworld);

        // 临时重置状态以允许重新执行
        data.reset();

        LOGGER.info("[EPIC ENGINE]: Admin {} is forcing preset commands execution", admin.getName().getString());
        scheduleCommandExecution(overworld);

        return true;
    }

    /** 获取预设命令执行状态信息 */
    public static String getExecutionStatusInfo() {
        if (serverInstance == null) {
            return "Server not available";
        }

        PresetData data = getGlobalPresetData(serverInstance.overworld());
        StringBuilder info = new StringBuilder();

        info.append("Preset Commands Status:\n");
        info.append("- Config Source: ").append(EpicEngineCustomConfig.getConfigSource()).append("\n");
        info.append("- Enabled: ").append(EpicEngineCustomConfig.getEffectivePresetCommandsEnabled()).append("\n");
        info.append("- Commands: ").append(getCommandCount()).append("\n");
        info.append("- Executed: ").append(data.hasExecuted()).append("\n");

        if (data.hasExecuted()) {
            info.append("- Execution Time: ").append(new java.util.Date(data.getExecutionTime())).append("\n");
            info.append("- Server Version: ").append(data.getServerVersion()).append("\n");
        }

        return info.toString();
    }

    /** 获取配置的命令数量 */
    private static int getCommandCount() {
        String raw = EpicEngineCustomConfig.getEffectivePresetCommandsList();
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        return raw.split(";").length;
    }

    /** 关闭调度器（服务端停服时调用） */
    public static void shutdown() {
        LOGGER.info("[EPIC ENGINE]: Shutting down PresetCommandsHandler scheduler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("[EPIC ENGINE]: Scheduler did not terminate gracefully, forcing shutdown");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.warn("[EPIC ENGINE]: Interrupted while waiting for scheduler shutdown");
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** 全局预设命令执行状态 */
    public static class PresetData extends SavedData {
        private boolean executed = false;
        private long executionTime = 0L;
        private String serverVersion = "";
        private String configSource = "";

        public PresetData() {}

        public PresetData(CompoundTag tag) {
            this.executed = tag.getBoolean("executed");
            this.executionTime = tag.getLong("execution_time");
            this.serverVersion = tag.getString("server_version");
            this.configSource = tag.getString("config_source");
        }

        public static PresetData load(CompoundTag tag) {
            return new PresetData(tag);
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("executed", this.executed);
            tag.putLong("execution_time", this.executionTime);
            tag.putString("server_version", this.serverVersion);
            tag.putString("config_source", this.configSource);
            return tag;
        }

        public boolean hasExecuted() {
            return this.executed;
        }

        public void markAsExecuted() {
            this.executed = true;
            this.executionTime = System.currentTimeMillis();
            this.serverVersion = serverInstance != null ? serverInstance.getServerVersion() : "unknown";
            this.configSource = EpicEngineCustomConfig.getConfigSource();
            this.setDirty();
        }

        public void reset() {
            this.executed = false;
            this.executionTime = 0L;
            this.serverVersion = "";
            this.configSource = "";
            this.setDirty();
        }

        public long getExecutionTime() {
            return this.executionTime;
        }

        public String getServerVersion() {
            return this.serverVersion;
        }

        public String getConfigSource() {
            return this.configSource;
        }
    }
}