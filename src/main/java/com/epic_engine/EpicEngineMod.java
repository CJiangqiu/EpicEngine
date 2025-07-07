package com.epic_engine;

import com.epic_engine.init.EpicEngineModItems;
import com.epic_engine.init.EpicEngineModSounds;
import com.epic_engine.init.EpicEngineModTabs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod(EpicEngineMod.MODID)
public class EpicEngineMod {
    public static final String MODID = "epic_engine";
    public static final Logger LOGGER = LogManager.getLogger();

    /** Network protocol version */
    private static final String PROTOCOL_VERSION = "1";
    /** SimpleChannel for custom packets */
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, MODID),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int messageID = 0;

    /** Queue for delayed server tasks */
    private static final Queue<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

    public EpicEngineMod() {

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        EpicEngineModSounds.REGISTRY.register(modBus);
        EpicEngineModItems.REGISTRY.register(modBus);
        EpicEngineModTabs.REGISTRY.register(modBus);

        modBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("EJRA core initialized");
    }

    /**
     * Called after FMLConstructModEvent and config registration,
     * safe to perform setup that depends on config values.
     */
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("EJRA common setup complete — config should be loaded now");
        // Here you can put initialization logic that requires config,
        // e.g. schedule world-gen hooks, register custom serializers, etc.
    }

    /** Utility: register a network message */
    public static <T> void addNetworkMessage(Class<T> type,
                                             BiConsumer<T, FriendlyByteBuf> encoder,
                                             Function<FriendlyByteBuf, T> decoder,
                                             BiConsumer<T, Supplier<NetworkEvent.Context>> handler) {
        PACKET_HANDLER.registerMessage(messageID++, type, encoder, decoder, handler);
    }

    /** Utility: queue a task to run on the server thread after `delayTicks` */
    public static void queueServerWork(int delayTicks, Runnable task) {
        if (Thread.currentThread().getThreadGroup().getName().equals("SERVER")) {
            workQueue.add(new AbstractMap.SimpleEntry<>(task, delayTicks));
        }
    }

    /** Server tick handler for queued tasks */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        List<AbstractMap.SimpleEntry<Runnable, Integer>> ready = new ArrayList<>();
        workQueue.forEach(entry -> {
            entry.setValue(entry.getValue() - 1);
            if (entry.getValue() <= 0) ready.add(entry);
        });
        ready.forEach(entry -> entry.getKey().run());
        workQueue.removeAll(ready);
    }
}
