package com.epic_engine.init;


import com.epic_engine.EpicEngineMod;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

public class EpicEngineModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EpicEngineMod.MODID);
	public static final RegistryObject<SoundEvent> WIND_SOUND = REGISTRY.register("wind_sound", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("epic_engine", "wind_sound")));
	public static final RegistryObject<SoundEvent> WORLDS_THROAT = REGISTRY.register("worlds_throat", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("epic_engine", "worlds_throat")));
}
