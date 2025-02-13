package net.jcm.vsch.entity;

import net.jcm.vsch.VSCHMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = VSCHMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class VSCHEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, VSCHMod.MODID);

	public static final RegistryObject<EntityType<MagnetEntity>> MAGNET_ENTITY = registerEntity("magnet_entity",
		() -> EntityType.Builder.<MagnetEntity>of(MagnetEntity::new, MobCategory.MISC).sized(0F, 0F).clientTrackingRange(0).updateInterval(Integer.MAX_VALUE));

	// Makes it easier to register entities registry name
	private static <T extends Entity> RegistryObject<EntityType<T>> registerEntity(String name, Supplier<EntityType.Builder<T>> builder) {
		return ENTITIES.register(name, () -> builder.get().build(VSCHMod.MODID + ":" + name));
	}

	public static void register(IEventBus eventBus) {
		ENTITIES.register(eventBus);
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(MAGNET_ENTITY.get(), MagnetEntity.Renderer::new);
	}
}
