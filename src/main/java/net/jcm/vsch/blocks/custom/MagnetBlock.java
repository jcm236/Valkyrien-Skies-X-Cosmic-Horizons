package net.jcm.vsch.blocks.custom;


import net.jcm.vsch.blocks.custom.template.BlockWithEntity;
import net.jcm.vsch.blocks.entity.MagnetBlockEntity;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.ship.DraggerData;
import net.jcm.vsch.ship.ThrusterData;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;


public class MagnetBlock extends BlockWithEntity<MagnetBlockEntity> {

	public MagnetBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
			.setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		pBuilder.add(FACING);
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

		// ----- Remove the thruster from the force appliers for the current level ----- //
		// I guess VS does this automatically when switching a shipyards dimension?
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
		if (ships != null) {
			//ships.removeDragger(pos);
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction dir = ctx.getNearestLookingDirection();
		if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) {
			dir = dir.getOpposite();
		}
		return defaultBlockState()
			.setValue(BlockStateProperties.FACING, dir);
	}

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
		super.neighborChanged(state, world, pos, neighbor, neighborPos, moving);
		MagnetBlockEntity be = (MagnetBlockEntity) world.getBlockEntity(pos);
		be.neighborChanged(neighbor, neighborPos, moving);
	}

	// Attach block entity
	@Override
	public MagnetBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MagnetBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? (ParticleBlockEntity::clientTick) : ParticleBlockEntity::serverTick;
	}
}
