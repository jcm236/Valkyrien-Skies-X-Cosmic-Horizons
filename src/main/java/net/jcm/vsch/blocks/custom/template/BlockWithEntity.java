package net.jcm.vsch.blocks.custom.template;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * For making a block that has an entity always attached (an actual entity)
 * @param <T> The block entity class that is responsible for spawning the entity, removing it, etc
 * @see BlockEntityWithEntity
 */
public abstract class BlockWithEntity<T extends BlockEntityWithEntity<?>> extends DirectionalBlock implements EntityBlock {
	public BlockWithEntity(Properties properties) {
		super(properties);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		super.onRemove(state, level, pos, newState, isMoving);
		if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof BlockEntityWithEntity<?> blockEntityWithEntity) {
				blockEntityWithEntity.removeLinkedEntity();
			}
		}
	}

	@Override
	public abstract T newBlockEntity(BlockPos pos, BlockState state);
}
