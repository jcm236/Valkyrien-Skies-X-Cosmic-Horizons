package net.jcm.vsch.blocks.custom.template;

import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import net.jcm.vsch.blocks.entity.template.AbstractThrusterBlockEntity;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.items.VSCHItems;
import net.jcm.vsch.ship.ThrusterData.ThrusterMode;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.jcm.vsch.util.rot.DirectionalShape;
import net.jcm.vsch.util.rot.RotShape;
import net.jcm.vsch.util.rot.RotShapes;

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
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractThrusterBlock<T extends AbstractThrusterBlockEntity> extends DirectionalBlock implements EntityBlock {

	public static final int MULT = 1000;
	// TODO: fix this bounding box
	private static final RotShape SHAPE = RotShapes.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
	public static final EnumProperty<ThrusterMode> MODE = EnumProperty.create("mode", ThrusterMode.class);
	public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

	private final DirectionalShape shape;

	protected AbstractThrusterBlock(Properties properties, DirectionalShape shape) {
		super(properties);
		this.shape = shape;
		registerDefaultState(defaultBlockState()
				.setValue(FACING, Direction.NORTH)
				.setValue(MODE, ThrusterMode.POSITION) // handy string to Enum :D
				.setValue(LIT, Boolean.valueOf(false))
				);
	}

	public AbstractThrusterBlock(Properties properties) {
		this(properties, DirectionalShape.south(SHAPE));
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		builder.add(MODE);
		builder.add(LIT);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	// Should run whenever the block state is changed, but not when the block itself is replaced
	//TODO: See if we can move setting the force inducer to here instead of all over the place (would require some extra stuff for redstone change tho)
	// Disabling this didn't fix Jade :(
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape.get(state.getValue(BlockStateProperties.FACING));
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		super.onRemove(state, level, pos, newState, isMoving);
		if (!(level instanceof ServerLevel)) {
			return;
		}

		// ----- Remove the thruster from the force appliers for the current level ----- //
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
		if (ships != null) {
			ships.removeThruster(pos);
		}
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		// If client side, ignore
		if (!(level instanceof ServerLevel)) {
			return InteractionResult.PASS;
		}

		// If its the right item and mainhand
		if (player.getMainHandItem().getItem() != VSCHItems.WRENCH.get() || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		// If thrusters can be toggled
		if (!VSCHConfig.THRUSTER_TOGGLE.get()) {
			player.displayClientMessage(Component.translatable("vsch.error.thruster_modes_disabled").withStyle(
					ChatFormatting.RED
			), true);
			return InteractionResult.PASS;
		}

		// Get the force handler
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);

		// If a force handler exists (might not if we aren't on a VS ship)
		if (ships == null) {
			// Not on a ship
			player.displayClientMessage(Component.translatable("vsch.error.thruster_not_on_ship").withStyle(
					ChatFormatting.RED
			), true);
			return InteractionResult.FAIL;
		}

		// Get thruster
		AbstractThrusterBlockEntity thruster = (AbstractThrusterBlockEntity) level.getBlockEntity(pos);
		if (thruster == null) {
			return InteractionResult.PASS;
		}

		// Get current state (block property)
		ThrusterMode blockMode = thruster.getThrusterMode();

		// Toggle it between POSITION and GLOBAL
		blockMode = blockMode.toggle();

		// Save the block property into the block
		// And set the thruster data to the new toggled property
		thruster.setThrusterMode(blockMode);

		//Send a chat message to them. The wrench class will handle the actionbar
		player.sendSystemMessage(Component.translatable("vsch.message.toggle").append(Component.translatable("vsch."+blockMode.toString().toLowerCase())));

		return InteractionResult.CONSUME;
	}

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
		super.neighborChanged(state, world, pos, neighbor, neighborPos, moving);
		AbstractThrusterBlockEntity be = (AbstractThrusterBlockEntity) world.getBlockEntity(pos);
		be.neighborChanged(neighbor, neighborPos, moving);
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
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction dir = ctx.getNearestLookingDirection();
		if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) {
			dir = dir.getOpposite();
		}
		return defaultBlockState()
				.setValue(BlockStateProperties.FACING, dir)
				.setValue(MODE, VSCHConfig.THRUSTER_MODE.get());
	}

	// Attach block entity
	@Override
	public abstract T newBlockEntity(BlockPos pos, BlockState state);

	@Override
	public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
		return level.isClientSide() ? (ParticleBlockEntity::clientTick) : ParticleBlockEntity::serverTick;
	}
}
