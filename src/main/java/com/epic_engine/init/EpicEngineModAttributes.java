package com.epic_engine.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;

import com.epic_engine.EpicEngineMod;
import com.epic_engine.config.EpicEngineBattleSystemConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EpicEngineModAttributes {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<Attribute> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, EpicEngineMod.MODID);

    // ========== Mana System ==========
    public static final RegistryObject<Attribute> MANA = REGISTRY.register("mana",
            () -> new RangedAttribute("attribute.epic_engine.mana", 0.0, 0, 1024).setSyncable(true));

    public static final RegistryObject<Attribute> MAX_MANA = REGISTRY.register("max_mana",
            () -> new RangedAttribute("attribute.epic_engine.max_mana", 100.0, 1, 1024).setSyncable(true));

    public static final RegistryObject<Attribute> MANA_REGENERATION = REGISTRY.register("mana_regeneration",
            () -> new RangedAttribute("attribute.epic_engine.mana_regeneration", 2.0, 0.0, 20.0).setSyncable(true));

    // ========== Stamina System ==========
    public static final RegistryObject<Attribute> STAMINA = REGISTRY.register("stamina",
            () -> new RangedAttribute("attribute.epic_engine.stamina", 0.0, 0, 1024).setSyncable(true));

    public static final RegistryObject<Attribute> MAX_STAMINA = REGISTRY.register("max_stamina",
            () -> new RangedAttribute("attribute.epic_engine.max_stamina", 100.0, 1, 1024).setSyncable(true));

    public static final RegistryObject<Attribute> STAMINA_REGENERATION = REGISTRY.register("stamina_regeneration",
            () -> new RangedAttribute("attribute.epic_engine.stamina_regeneration", 4.0, 0.0, 30.0).setSyncable(true));

    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        // 属性注册时不检查配置，因为配置还没加载
        // 直接注册所有属性，后续通过事件来控制是否使用
        event.getTypes().forEach(entityType -> {
            event.add(entityType, MANA.get());
            event.add(entityType, MAX_MANA.get());
            event.add(entityType, MANA_REGENERATION.get());
            event.add(entityType, STAMINA.get());
            event.add(entityType, MAX_STAMINA.get());
            event.add(entityType, STAMINA_REGENERATION.get());
        });

        LOGGER.info("[EPIC ENGINE]: Registered combat attributes to all living entities");
    }

    // ========== Player Attributes Sync ==========
    @Mod.EventBusSubscriber
    public static class PlayerAttributesSync {

        @SubscribeEvent
        public static void onPlayerJoin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            // 当玩家加入时，检查战斗系统是否启用，并应用配置
            try {
                if (EpicEngineBattleSystemConfig.isBattleSystemEnabled()) {
                    Player player = (Player) event.getEntity();
                    applyConfigToPlayer(player);
                    LOGGER.debug("[EPIC ENGINE]: Applied config values to player {} (Battle system enabled)",
                            player.getName().getString());
                } else {
                    // 如果战斗系统被禁用，将属性设置为0或最小值
                    Player player = (Player) event.getEntity();
                    disableBattleAttributes(player);
                    LOGGER.debug("[EPIC ENGINE]: Battle system disabled, attributes set to minimal values for player {}",
                            player.getName().getString());
                }
            } catch (Exception e) {
                LOGGER.warn("[EPIC ENGINE]: Failed to handle player join event", e);
            }
        }

        @SubscribeEvent
        public static void playerClone(PlayerEvent.Clone event) {
            Player oldPlayer = event.getOriginal();
            Player newPlayer = event.getEntity();

            try {
                if (!EpicEngineBattleSystemConfig.isBattleSystemEnabled()) {
                    // 战斗系统未启用时禁用属性
                    disableBattleAttributes(newPlayer);
                    LOGGER.debug("[EPIC ENGINE]: Battle system disabled, attributes disabled for cloned player");
                    return;
                }

                if (event.isWasDeath()) {
                    // 死亡后重置为配置的默认值
                    applyConfigToPlayer(newPlayer);
                    LOGGER.debug("[EPIC ENGINE]: Player died - attributes reset to config values");
                } else {
                    // 维度切换时根据配置决定是否保留
                    if (EpicEngineBattleSystemConfig.shouldPreserveOnDimensionChange()) {
                        copyAttributeValue(oldPlayer, newPlayer, MANA.get());
                        copyAttributeValue(oldPlayer, newPlayer, MAX_MANA.get());
                        copyAttributeValue(oldPlayer, newPlayer, MANA_REGENERATION.get());
                        copyAttributeValue(oldPlayer, newPlayer, STAMINA.get());
                        copyAttributeValue(oldPlayer, newPlayer, MAX_STAMINA.get());
                        copyAttributeValue(oldPlayer, newPlayer, STAMINA_REGENERATION.get());

                        LOGGER.debug("[EPIC ENGINE]: Player attributes preserved during dimension change");
                    } else {
                        applyConfigToPlayer(newPlayer);
                        LOGGER.debug("[EPIC ENGINE]: Player attributes reset to config values during dimension change");
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[EPIC ENGINE]: Failed to handle player clone event", e);
            }
        }

        /**
         * Apply config values to a player's attributes
         */
        private static void applyConfigToPlayer(Player player) {
            setAttributeValue(player, MANA.get(), EpicEngineBattleSystemConfig.getMana());
            setAttributeValue(player, MAX_MANA.get(), EpicEngineBattleSystemConfig.getMaxMana());
            setAttributeValue(player, MANA_REGENERATION.get(), EpicEngineBattleSystemConfig.getManaRegeneration());
            setAttributeValue(player, STAMINA.get(), EpicEngineBattleSystemConfig.getStamina());
            setAttributeValue(player, MAX_STAMINA.get(), EpicEngineBattleSystemConfig.getMaxStamina());
            setAttributeValue(player, STAMINA_REGENERATION.get(), EpicEngineBattleSystemConfig.getStaminaRegeneration());
        }

        /**
         * Disable battle attributes by setting them to minimal/zero values
         */
        private static void disableBattleAttributes(Player player) {
            setAttributeValue(player, MANA.get(), 0.0);
            setAttributeValue(player, MAX_MANA.get(), 1.0);  // 最小值1，避免除零
            setAttributeValue(player, MANA_REGENERATION.get(), 0.0);
            setAttributeValue(player, STAMINA.get(), 0.0);
            setAttributeValue(player, MAX_STAMINA.get(), 1.0);  // 最小值1，避免除零
            setAttributeValue(player, STAMINA_REGENERATION.get(), 0.0);
        }

        /**
         * Set attribute value safely
         */
        private static void setAttributeValue(Player player, Attribute attribute, double value) {
            var attr = player.getAttribute(attribute);
            if (attr != null) {
                attr.setBaseValue(value);
            }
        }

        /**
         * Copy a single attribute value
         */
        private static void copyAttributeValue(Player from, Player to, Attribute attribute) {
            var fromAttr = from.getAttribute(attribute);
            var toAttr = to.getAttribute(attribute);
            if (fromAttr != null && toAttr != null) {
                toAttr.setBaseValue(fromAttr.getBaseValue());
            }
        }
    }
}