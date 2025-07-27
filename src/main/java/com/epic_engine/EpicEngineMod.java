package com.epic_engine;

import com.epic_engine.config.EpicEngineCustomConfig;
import com.epic_engine.init.EpicEngineModAttributes;
import com.epic_engine.init.EpicEngineModItems;
import com.epic_engine.init.EpicEngineModSounds;
import com.epic_engine.init.EpicEngineModTabs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod(EpicEngineMod.MODID)
public class EpicEngineMod {
    public static final String MODID = "epic_engine";
    public static final Logger LOGGER = LogManager.getLogger();

    // 网络相关
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, MODID),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int messageID = 0;

    public EpicEngineMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册模组内容
        EpicEngineModTabs.REGISTRY.register(modBus);
        EpicEngineModAttributes.REGISTRY.register(modBus);
        EpicEngineModSounds.REGISTRY.register(modBus);
        EpicEngineModItems.REGISTRY.register(modBus);


        // 添加 MOD 总线事件监听
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onConfigReload);

        // 客户端专用设置
        if (FMLEnvironment.dist.isClient()) {
            modBus.addListener(this::onClientSetup);
        }

        // 注册 Forge 事件总线
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[EPIC ENGINE]:Initialized");
    }

    /** 通用设置 — 服务端和客户端都会执行 */
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            initNetworking();
            LOGGER.info("[EPIC ENGINE]: Common setup complete");
        });
    }

    /** 客户端专用设置 */
    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                EpicEngineCustomConfig.initializeResources();
                LOGGER.info("[EPIC ENGINE]: Client resources initialized");
            } catch (Exception e) {
                LOGGER.error("[EPIC ENGINE]: Failed to initialize client resources", e);
            }
        });
    }

    /** 配置文件重载事件处理 */
    private void onConfigReload(final ModConfigEvent event) {
        if (!event.getConfig().getModId().equals(MODID)) {
            return;
        }

        LOGGER.info("[EPIC ENGINE]: Config reloading - {}", event.getConfig().getFileName());

        if (event.getConfig().getSpec() == EpicEngineCustomConfig.CONFIG) {
            // 客户端重新初始化资源
            if (FMLEnvironment.dist.isClient()) {
                try {
                    EpicEngineCustomConfig.initializeResources();
                    LOGGER.info("[EPIC ENGINE]: Client resources reloaded");
                } catch (Exception e) {
                    LOGGER.error("[EPIC ENGINE]: Failed to reinitialize resources", e);
                }
            }
        }
    }

    /** 初始化网络消息 */
    private void initNetworking() {
        addNetworkMessage(ConfigSyncPacket.class,
                ConfigSyncPacket::encode,
                ConfigSyncPacket::decode,
                ConfigSyncPacket::handle);
    }

    /** 玩家加入服务器时发送服务端配置 */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 只在多人游戏中同步配置
            if (player.getServer() != null && !player.getServer().isSingleplayer()) {
                syncConfigToPlayer(player);
            }
        }
    }

    /** 玩家离开服务器时恢复本地配置 */
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (FMLEnvironment.dist.isClient()) {
            // 恢复本地配置并重新初始化资源
            EpicEngineCustomConfig.restoreLocalConfig();
            EpicEngineCustomConfig.initializeResources();
            LOGGER.info("[EPIC ENGINE]: Restored local config after leaving server");
        }
    }

    /** 同步服务端配置到指定玩家 */
    public static void syncConfigToPlayer(ServerPlayer player) {
        try {
            // 读取服务端的真实配置
            boolean enabled = EpicEngineCustomConfig.PRESET_COMMANDS_ENABLED.get();
            String commands = EpicEngineCustomConfig.PRESET_COMMANDS_LIST.get();
            int delay = EpicEngineCustomConfig.PRESET_COMMANDS_DELAY.get();

            // 创建包含真实配置的数据包
            ConfigSyncPacket packet = new ConfigSyncPacket(enabled, commands, delay);

            // 发送到客户端
            PACKET_HANDLER.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);

            LOGGER.info("[EPIC ENGINE]: Server config sent to {}: enabled={}, {} commands",
                    player.getName().getString(), enabled,
                    commands != null && !commands.isBlank() ? commands.split(";").length : 0);
        } catch (Exception e) {
            LOGGER.error("[EPIC ENGINE]: Failed to sync config to player: {}",
                    player.getName().getString(), e);
        }
    }

    /** 注册网络消息 */
    public static <T> void addNetworkMessage(Class<T> type,
                                             BiConsumer<T, FriendlyByteBuf> encoder,
                                             Function<FriendlyByteBuf, T> decoder,
                                             BiConsumer<T, Supplier<NetworkEvent.Context>> handler) {
        PACKET_HANDLER.registerMessage(messageID++, type, encoder, decoder, handler);
    }

    /** 配置同步数据包 - 传输服务端配置到客户端 */
    public static class ConfigSyncPacket {
        private boolean presetCommandsEnabled;
        private String presetCommandsList;
        private int presetCommandsDelay;

        public ConfigSyncPacket() {} // 默认构造函数（客户端接收用）

        // 服务端构造函数 - 从服务端配置创建数据包
        public ConfigSyncPacket(boolean enabled, String commands, int delay) {
            this.presetCommandsEnabled = enabled;
            this.presetCommandsList = commands != null ? commands : "";
            this.presetCommandsDelay = delay;
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeBoolean(presetCommandsEnabled);
            buffer.writeUtf(presetCommandsList);
            buffer.writeInt(presetCommandsDelay);
        }

        public static ConfigSyncPacket decode(FriendlyByteBuf buffer) {
            return new ConfigSyncPacket(
                    buffer.readBoolean(),
                    buffer.readUtf(),
                    buffer.readInt()
            );
        }

        public void handle(Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    // 直接覆盖客户端的配置值
                    EpicEngineCustomConfig.overrideFromServer(
                            presetCommandsEnabled,
                            presetCommandsList,
                            presetCommandsDelay
                    );
                    LOGGER.info("[EPIC ENGINE]: Client config overridden by server - commands enabled: {}, {} commands",
                            presetCommandsEnabled,
                            presetCommandsList != null && !presetCommandsList.isBlank() ? presetCommandsList.split(";").length : 0);
                }
            });
            context.get().setPacketHandled(true);
        }
    }
}