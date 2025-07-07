package com.epic_engine.init;

import com.epic_engine.EpicEngineMod;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

public class EpicEngineModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EpicEngineMod.MODID);
	public static final RegistryObject<CreativeModeTab> EJRA_BUILDING = REGISTRY.register("epic_engine_building",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.epic_engine.epic_engine_building")).icon(() -> new ItemStack(EpicEngineModItems.STAFF_OF_INFINITY.get())).displayItems((parameters, tabData) -> {
				tabData.accept(EpicEngineModItems.STAFF_OF_INFINITY.get());
			}).withSearchBar().build());
}
