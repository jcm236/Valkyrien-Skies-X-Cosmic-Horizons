package net.jcm.vsch.blocks.custom;


import net.jcm.vsch.blocks.custom.template.AbstractThrusterBlock;
import net.jcm.vsch.config.VSCHConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.jcm.vsch.blocks.entity.AirThrusterBlockEntity;
import net.jcm.vsch.util.rot.DirectionalShape;
import net.jcm.vsch.util.rot.RotShape;
import net.jcm.vsch.util.rot.RotShapes;


public class AirThrusterBlock extends AbstractThrusterBlock<AirThrusterBlockEntity> {
	// TODO: fix this bounding box
	private static final RotShape SHAPE = RotShapes.box(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);

	public AirThrusterBlock(Properties properties) {
		super(properties, DirectionalShape.down(SHAPE));
	}

	@Override
	public AirThrusterBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AirThrusterBlockEntity(pos, state);
	}
}
