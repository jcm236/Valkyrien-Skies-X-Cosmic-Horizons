package net.jcm.vsch.blocks.custom;

import net.jcm.vsch.blocks.entity.DragInducerBlockEntity;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.ship.DraggerData;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.jcm.vsch.util.rot.DirectionalShape;
import net.jcm.vsch.util.rot.RotShape;
import net.jcm.vsch.util.rot.RotShapes;
import net.lointain.cosmos.init.CosmosModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class DragInducerBlock extends Block implements EntityBlock {

	// TODO: fix this bounding box
	private static final RotShape SHAPE = RotShapes.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
	private final DirectionalShape dragger_shape = DirectionalShape.south(SHAPE);

	public DragInducerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		super.onRemove(state, level, pos, newState, isMoving);
		if (!(level instanceof ServerLevel)) {
			return;
		}

		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
		if (ships != null) {
			ships.removeDragger(pos);
		}
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		// // If client side, ignore
		// if (!(level instanceof ServerLevel)) {
		// 	return InteractionResult.PASS;
		// }

		// // If its the right item and mainhand
		// if (player.getMainHandItem().getItem() != VSCHItems.WRENCH.get() || hand != InteractionHand.MAIN_HAND) {
		// 	return InteractionResult.PASS;
		// }

		// // If thrusters can be toggled
		// if (!VSCHConfig.THRUSTER_TOGGLE.get()) {
		// 	player.displayClientMessage(Component.translatable("vsch.error.thruster_modes_disabled").withStyle(ChatFormatting.RED), true);
		// 	return InteractionResult.PASS;
		// }

		// // Get the force handler
		// VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);

		// // If a force handler exists (might not if we aren't on a VS ship)
		// if (ships == null) {
		// 	// Not on a ship
		// 	player.displayClientMessage(Component.translatable("vsch.error.thruster_not_on_ship").withStyle(ChatFormatting.RED), true);
		// 	return InteractionResult.FAIL;
		// }

		// DraggerData data = ships.getDraggerAtPos(pos);

		// // Get thruster
		// DragInducerBlockEntity thruster = (DragInducerBlockEntity) level.getBlockEntity(pos);
		// if (thruster == null) {
		// 	return InteractionResult.PASS;
		// }

		// // Get current state (block property)
		// DraggerMode blockMode = thruster.getDraggerMode();

		// // Toggle it between POSITION and GLOBAL
		// blockMode = blockMode.toggle();

		// // Save the block property into the block
		// thruster.setThrusterMode(blockMode);

		// //Send a chat message to them. The wrench class will handle the actionbar
		// player.sendSystemMessage(Component.translatable("vsch.message.toggle").append(Component.translatable("vsch."+blockMode.toString().toLowerCase())));

		return InteractionResult.PASS;
	}


	/*@Override
	public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
		int tier = state.getValue(TournamentProperties.TIER);
		if (tier > 1) {
			drops.add(new ItemStack(TournamentItems.UPGRADE_THRUSTER.get(), tier - 1));
		}
		return drops;
	}*/

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
		super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
		DragInducerBlockEntity be = (DragInducerBlockEntity) level.getBlockEntity(pos);
		be.neighborChanged(neighbor, neighborPos, moving);
	}

	// Attach block entity
	@Override
	public DragInducerBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DragInducerBlockEntity(pos, state);
	}

	/*public static <T extends BlockEntity> BlockEntityTicker<T> getTickerHelper(Level level) {
		return level.isClientSide() && !allowClient ? null : (level0, pos0, state0, blockEntity) -> ((TickableBlockEntity)blockEntity).tick();
	}*/
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? (ParticleBlockEntity::clientTick) : ParticleBlockEntity::serverTick;
	}
}
