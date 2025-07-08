package com.epic_engine.init;

import com.epic_engine.EpicEngineMod;
import com.epic_engine.config.EpicEngineWorldConfig;
import com.epic_engine.config.EpicEngineCustomConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

@Mod.EventBusSubscriber(modid = EpicEngineMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EpicEngineModConfigs {

    @SubscribeEvent
    public static void register(FMLConstructModEvent event) {
        event.enqueueWork(() -> {
            // 注册世界配置
            ModLoadingContext.get().registerConfig(
                    ModConfig.Type.COMMON,
                    EpicEngineWorldConfig.CONFIG,
                    "Epic_Engine_World.toml"
            );

            // 注册自定义界面配置
            ModLoadingContext.get().registerConfig(
                    ModConfig.Type.CLIENT,
                    EpicEngineCustomConfig.CONFIG,
                    "Epic_Engine_Custom.toml"
            );
        });
    }
}