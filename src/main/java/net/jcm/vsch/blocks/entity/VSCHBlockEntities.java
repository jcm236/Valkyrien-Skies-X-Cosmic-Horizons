package net.jcm.vsch.blocks.entity;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.blocks.VSCHBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class VSCHBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, VSCHMod.MODID);

	public static final RegistryObject<BlockEntityType<ThrusterBlockEntity>> THRUSTER_BLOCK_ENTITY =
			BLOCK_ENTITIES.register("thruster_block",
					() -> BlockEntityType.Builder.of(ThrusterBlockEntity::new, VSCHBlocks.THRUSTER_BLOCK.get())
					.build(null));

	public static final RegistryObject<BlockEntityType<AirThrusterBlockEntity>> AIR_THRUSTER_BLOCK_ENTITY =
			BLOCK_ENTITIES.register("air_thruster_block",
					() -> BlockEntityType.Builder.of(AirThrusterBlockEntity::new, VSCHBlocks.AIR_THRUSTER_BLOCK.get())
					.build(null));

	public static final RegistryObject<BlockEntityType<PowerfulThrusterBlockEntity>> POWERFUL_THRUSTER_BLOCK_ENTITY =
			BLOCK_ENTITIES.register("powerful_thruster_block",
					() -> BlockEntityType.Builder.of(PowerfulThrusterBlockEntity::new, VSCHBlocks.POWERFUL_THRUSTER_BLOCK.get())
					.build(null));

	public static final RegistryObject<BlockEntityType<DragInducerBlockEntity>> DRAG_INDUCER_BLOCK_ENTITY =
			BLOCK_ENTITIES.register("drag_inducer_block",
					() -> BlockEntityType.Builder.of(DragInducerBlockEntity::new, VSCHBlocks.DRAG_INDUCER_BLOCK.get())
					.build(null));

	public static final RegistryObject<BlockEntityType<GravityInducerBlockEntity>> GRAVITY_INDUCER_BLOCK_ENTITY =
			BLOCK_ENTITIES.register("gravity_inducer_block",
					() -> BlockEntityType.Builder.of(GravityInducerBlockEntity::new, VSCHBlocks.GRAVITY_INDUCER_BLOCK.get())
							.build(null));

	public static final RegistryObject<BlockEntityType<DockerBlockEntity>> DOCKER_BLOCK_ENTITY =
			BLOCK_ENTITIES.register("dock",
					() -> BlockEntityType.Builder.of(DockerBlockEntity::new, VSCHBlocks.DOCKER_BLOCK.get())
							.build(null));

	public static final RegistryObject<BlockEntityType<MagnetBlockEntity>> MAGNET_BLOCK_ENTITY =
			BLOCK_ENTITIES.register("magnet_block",
					() -> BlockEntityType.Builder.of(MagnetBlockEntity::new, VSCHBlocks.MAGNET_BLOCK.get())
							.build(null));



	public static void register(IEventBus eventBus) {
		BLOCK_ENTITIES.register(eventBus);
	}
}
