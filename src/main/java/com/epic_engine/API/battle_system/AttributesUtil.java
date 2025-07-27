package com.epic_engine.API.battle_system;

import com.epic_engine.init.EpicEngineModAttributes;
import net.minecraft.world.entity.LivingEntity;

/**
 * Epic Engine Attributes System Utility Class
 * Provides convenient methods for mana and stamina operations
 */
public class AttributesUtil {

    // ========== Mana System API ==========

    /**
     * Gets the current mana value of an entity
     * @param entity The target entity
     * @return Current mana value
     */
    public static double getMana(LivingEntity entity) {
        return entity.getAttributeValue(EpicEngineModAttributes.MANA.get());
    }

    /**
     * Gets the maximum mana value of an entity
     * @param entity The target entity
     * @return Maximum mana value
     */
    public static double getMaxMana(LivingEntity entity) {
        return entity.getAttributeValue(EpicEngineModAttributes.MAX_MANA.get());
    }

    /**
     * Sets the current mana value of an entity
     * @param entity The target entity
     * @param value The new mana value (will be clamped between 0 and max mana)
     */
    public static void setMana(LivingEntity entity, double value) {
        var attribute = entity.getAttribute(EpicEngineModAttributes.MANA.get());
        if (attribute != null) {
            double maxMana = getMaxMana(entity);
            attribute.setBaseValue(Math.max(0.0, Math.min(value, maxMana)));
        }
    }

    /**
     * Gets the mana regeneration rate of an entity
     * @param entity The target entity
     * @return Mana regeneration per second
     */
    public static double getManaRegeneration(LivingEntity entity) {
        return entity.getAttributeValue(EpicEngineModAttributes.MANA_REGENERATION.get());
    }

    /**
     * Consumes mana from an entity
     * @param entity The target entity
     * @param amount Amount of mana to consume
     * @return true if successful (sufficient mana), false otherwise
     */
    public static boolean consumeMana(LivingEntity entity, double amount) {
        double current = getMana(entity);
        if (current >= amount) {
            setMana(entity, current - amount);
            return true;
        }
        return false;
    }

    /**
     * Restores mana to an entity
     * @param entity The target entity
     * @param amount Amount of mana to restore
     */
    public static void restoreMana(LivingEntity entity, double amount) {
        double current = getMana(entity);
        setMana(entity, current + amount);
    }

    /**
     * Checks if an entity has sufficient mana
     * @param entity The target entity
     * @param amount Required mana amount
     * @return true if entity has enough mana, false otherwise
     */
    public static boolean hasSufficientMana(LivingEntity entity, double amount) {
        return getMana(entity) >= amount;
    }

    /**
     * Gets the mana percentage of an entity
     * @param entity The target entity
     * @return Mana percentage as a value between 0.0 and 1.0
     */
    public static double getManaPercentage(LivingEntity entity) {
        double max = getMaxMana(entity);
        if (max <= 0) return 0.0;
        return getMana(entity) / max;
    }

    // ========== Stamina System API ==========

    /**
     * Gets the current stamina value of an entity
     * @param entity The target entity
     * @return Current stamina value
     */
    public static double getStamina(LivingEntity entity) {
        return entity.getAttributeValue(EpicEngineModAttributes.STAMINA.get());
    }

    /**
     * Gets the maximum stamina value of an entity
     * @param entity The target entity
     * @return Maximum stamina value
     */
    public static double getMaxStamina(LivingEntity entity) {
        return entity.getAttributeValue(EpicEngineModAttributes.MAX_STAMINA.get());
    }

    /**
     * Sets the current stamina value of an entity
     * @param entity The target entity
     * @param value The new stamina value (will be clamped between 0 and max stamina)
     */
    public static void setStamina(LivingEntity entity, double value) {
        var attribute = entity.getAttribute(EpicEngineModAttributes.STAMINA.get());
        if (attribute != null) {
            double maxStamina = getMaxStamina(entity);
            attribute.setBaseValue(Math.max(0.0, Math.min(value, maxStamina)));
        }
    }

    /**
     * Gets the stamina regeneration rate of an entity
     * @param entity The target entity
     * @return Stamina regeneration per second
     */
    public static double getStaminaRegeneration(LivingEntity entity) {
        return entity.getAttributeValue(EpicEngineModAttributes.STAMINA_REGENERATION.get());
    }

    /**
     * Consumes stamina from an entity
     * @param entity The target entity
     * @param amount Amount of stamina to consume
     * @return true if successful (sufficient stamina), false otherwise
     */
    public static boolean consumeStamina(LivingEntity entity, double amount) {
        double current = getStamina(entity);
        if (current >= amount) {
            setStamina(entity, current - amount);
            return true;
        }
        return false;
    }

    /**
     * Restores stamina to an entity
     * @param entity The target entity
     * @param amount Amount of stamina to restore
     */
    public static void restoreStamina(LivingEntity entity, double amount) {
        double current = getStamina(entity);
        setStamina(entity, current + amount);
    }

    /**
     * Checks if an entity has sufficient stamina
     * @param entity The target entity
     * @param amount Required stamina amount
     * @return true if entity has enough stamina, false otherwise
     */
    public static boolean hasSufficientStamina(LivingEntity entity, double amount) {
        return getStamina(entity) >= amount;
    }

    /**
     * Gets the stamina percentage of an entity
     * @param entity The target entity
     * @return Stamina percentage as a value between 0.0 and 1.0
     */
    public static double getStaminaPercentage(LivingEntity entity) {
        double max = getMaxStamina(entity);
        if (max <= 0) return 0.0;
        return getStamina(entity) / max;
    }

    // ========== Combined Operations API ==========

    /**
     * Consumes both mana and stamina from an entity
     * @param entity The target entity
     * @param manaAmount Amount of mana to consume
     * @param staminaAmount Amount of stamina to consume
     * @return true if successful (both resources sufficient), false otherwise
     */
    public static boolean consumeBoth(LivingEntity entity, double manaAmount, double staminaAmount) {
        if (hasSufficientMana(entity, manaAmount) && hasSufficientStamina(entity, staminaAmount)) {
            consumeMana(entity, manaAmount);
            consumeStamina(entity, staminaAmount);
            return true;
        }
        return false;
    }

    /**
     * Restores both mana and stamina to an entity
     * @param entity The target entity
     * @param manaAmount Amount of mana to restore
     * @param staminaAmount Amount of stamina to restore
     */
    public static void restoreBoth(LivingEntity entity, double manaAmount, double staminaAmount) {
        restoreMana(entity, manaAmount);
        restoreStamina(entity, staminaAmount);
    }

    /**
     * Restores both mana and stamina to their maximum values
     * @param entity The target entity
     */
    public static void restoreToFull(LivingEntity entity) {
        setMana(entity, getMaxMana(entity));
        setStamina(entity, getMaxStamina(entity));
    }

    /**
     * Checks if an entity is in low mana state (below 25%)
     * @param entity The target entity
     * @return true if mana is below 25% of maximum, false otherwise
     */
    public static boolean isLowMana(LivingEntity entity) {
        return getManaPercentage(entity) < 0.25;
    }

    /**
     * Checks if an entity is in low stamina state (below 25%)
     * @param entity The target entity
     * @return true if stamina is below 25% of maximum, false otherwise
     */
    public static boolean isLowStamina(LivingEntity entity) {
        return getStaminaPercentage(entity) < 0.25;
    }
}