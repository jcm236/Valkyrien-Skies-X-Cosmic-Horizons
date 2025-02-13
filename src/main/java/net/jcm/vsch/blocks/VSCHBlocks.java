package net.jcm.vsch.blocks;

import java.util.function.Supplier;

import net.jcm.vsch.VSCHMod;

import net.jcm.vsch.blocks.custom.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.jcm.vsch.items.VSCHItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class VSCHBlocks {
	public static final DeferredRegister<Block> BLOCKS =
			DeferredRegister.create(ForgeRegistries.BLOCKS, VSCHMod.MODID); //this when registering a block doesnt make an item!


	public static final RegistryObject<Block> THRUSTER_BLOCK = registerBlock("thruster_block",
			() -> new ThrusterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
					.strength(5f)
					.noOcclusion()));

	public static final RegistryObject<Block> AIR_THRUSTER_BLOCK = registerBlock("air_thruster_block",
			() -> new AirThrusterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
					.strength(5f)
					.noOcclusion()));

	public static final RegistryObject<Block> POWERFUL_THRUSTER_BLOCK = registerBlock("powerful_thruster_block",
			() -> new PowerfulThrusterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
					.strength(5f)
					.noOcclusion()));

	public static final RegistryObject<Block> DRAG_INDUCER_BLOCK = registerBlock("drag_inducer_block",
			() -> new DragInducerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
					.strength(5f)
					.noOcclusion()));

	public static final RegistryObject<Block> GRAVITY_INDUCER_BLOCK = registerBlock("gravity_inducer_block",
			() -> new GravityInducerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
					.strength(5f)
					.noOcclusion()));

	public static final RegistryObject<Block> DOCKER_BLOCK = registerBlock("dock",
			() -> new DockerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
					.strength(5f)
					.noOcclusion()));

	public static final RegistryObject<Block> MAGNET_BLOCK = registerBlock("magnet_block",
			() -> new MagnetBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.COPPER)
					.strength(5f)
					.noOcclusion()));


	// below is just tools used in adding the block
	private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
		RegistryObject<T> toReturn = BLOCKS.register(name, block); //registers the block
		registerBlockItem(name, toReturn); //registers the item for the block
		return toReturn; //returns something or other idk
	}

	// since when registering it doesnt have an item, this function manually adds an item
	private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
		return VSCHItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
	}

	// register
	public static void register(IEventBus eventBus) {
		BLOCKS.register(eventBus);
	}
}

