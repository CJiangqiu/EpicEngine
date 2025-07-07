package com.epic_engine.init;

import com.epic_engine.EpicEngineMod;
import com.epic_engine.item.StaffOfInfinityItem;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.Item;

public class EpicEngineModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, EpicEngineMod.MODID);
	public static final RegistryObject<Item> STAFF_OF_INFINITY = REGISTRY.register("staff_of_infinity", () -> new StaffOfInfinityItem());

}
