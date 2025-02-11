package net.jcm.vsch.blocks.entity;

import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DockerBlockEntity extends BlockEntity {

	public DockerBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(VSCHBlockEntities.DOCKER_BLOCK_ENTITY.get(), pPos, pBlockState);
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, BlockEntity be) {
		DockerBlockEntity docker = (DockerBlockEntity) be;
		docker.clientTick(level, pos, state, be);
	}

	public void clientTick(Level level, BlockPos pos, BlockState state, DockerBlockEntity be) {
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BlockEntity be) {
		DockerBlockEntity docker = (DockerBlockEntity) be;
		docker.serverTick(level, pos, state, be);
	}

	public void serverTick(Level level, BlockPos pos, BlockState state, DockerBlockEntity be) {
//		HitResult hitResult = level.clip(new ClipContext(
//			pos.getCenter(),
//			pos.getCenter().add(new Vec3(0, 10, 0)),
//			ClipContext.Block.COLLIDER,
//			ClipContext.Fluid.NONE,
//			null));
//		System.out.println(hitResult);
	}
}
