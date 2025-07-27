package com.epic_engine.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EpicEngineBattleSystemConfig {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final ForgeConfigSpec CONFIG;

    // ========== Main Settings ==========
    public static final ForgeConfigSpec.BooleanValue ENABLE_BATTLE_SYSTEM;
    public static final ForgeConfigSpec.BooleanValue PRESERVE_ON_DIMENSION_CHANGE;

    // ========== Player Attributes ==========
    public static final ForgeConfigSpec.DoubleValue MANA;
    public static final ForgeConfigSpec.DoubleValue MAX_MANA;
    public static final ForgeConfigSpec.DoubleValue MANA_REGENERATION;
    public static final ForgeConfigSpec.DoubleValue STAMINA;
    public static final ForgeConfigSpec.DoubleValue MAX_STAMINA;
    public static final ForgeConfigSpec.DoubleValue STAMINA_REGENERATION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Epic Engine Battle System Configuration")
                .push("battle_system");

        // ========== Main Settings ==========
        ENABLE_BATTLE_SYSTEM = builder
                .comment("Enable the battle system entirely")
                .define("enableBattleSystem", true);

        PRESERVE_ON_DIMENSION_CHANGE = builder
                .comment("Preserve attribute values when changing dimensions")
                .define("preserveOnDimensionChange", true);

        // ========== Player Attributes ==========
        builder.comment("Player Attributes Configuration")
                .push("attributes");

        MANA = builder
                .comment("Default mana value")
                .defineInRange("mana", 0.0, 0.0, Double.MAX_VALUE);

        MAX_MANA = builder
                .comment("Default maximum mana value")
                .defineInRange("maxMana", 100.0, 1.0, Double.MAX_VALUE);

        MANA_REGENERATION = builder
                .comment("Default mana regeneration rate (per second)")
                .defineInRange("manaRegeneration", 2.0, 0.0, Double.MAX_VALUE);

        STAMINA = builder
                .comment("Default stamina value")
                .defineInRange("stamina", 0.0, 0.0, Double.MAX_VALUE);

        MAX_STAMINA = builder
                .comment("Default maximum stamina value")
                .defineInRange("maxStamina", 100.0, 1.0, Double.MAX_VALUE);

        STAMINA_REGENERATION = builder
                .comment("Default stamina regeneration rate (per second)")
                .defineInRange("staminaRegeneration", 4.0, 0.0, Double.MAX_VALUE);

        builder.pop(); // attributes
        builder.pop(); // battle_system

        CONFIG = builder.build();
    }

    // ========== Convenience Methods ==========

    /**
     * Check if battle system is enabled
     */
    public static boolean isBattleSystemEnabled() {
        return ENABLE_BATTLE_SYSTEM.get();
    }

    /**
     * Check if attributes should be preserved on dimension change
     */
    public static boolean shouldPreserveOnDimensionChange() {
        return PRESERVE_ON_DIMENSION_CHANGE.get();
    }

    /**
     * Get configured mana default value
     */
    public static double getMana() {
        return MANA.get();
    }

    /**
     * Get configured max mana default value
     */
    public static double getMaxMana() {
        return MAX_MANA.get();
    }

    /**
     * Get configured mana regeneration default value
     */
    public static double getManaRegeneration() {
        return MANA_REGENERATION.get();
    }

    /**
     * Get configured stamina default value
     */
    public static double getStamina() {
        return STAMINA.get();
    }

    /**
     * Get configured max stamina default value
     */
    public static double getMaxStamina() {
        return MAX_STAMINA.get();
    }

    /**
     * Get configured stamina regeneration default value
     */
    public static double getStaminaRegeneration() {
        return STAMINA_REGENERATION.get();
    }
}