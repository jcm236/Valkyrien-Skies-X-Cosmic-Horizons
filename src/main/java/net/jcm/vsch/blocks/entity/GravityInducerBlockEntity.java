package net.jcm.vsch.blocks.entity;

import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.config.VSCHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;

public class GravityInducerBlockEntity extends BlockEntity implements ParticleBlockEntity {
	private static final double MIN_FORCE = 0.01;
	public GravityInducerBlockEntity(BlockPos pos, BlockState blockState) {
		super(VSCHBlockEntities.GRAVITY_INDUCER_BLOCK_ENTITY.get(), pos, blockState);
	}


	public double getAttractDistance() {
		return VSCHConfig.GRAVITY_DISTANCE.get().doubleValue();
	}
	public double getMaxForce() {
		return VSCHConfig.GRAVITY_MAX_FORCE.get().doubleValue();
	}


	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, pos);
		if (ship == null) {
			return;
		}
		List<Entity> entities = level.getEntities(null, VectorConversionsMCKt.toMinecraft(ship.getWorldAABB()));
		for (Entity entity : entities) {
			if (entity.noPhysics) {
				continue;
			}
			if (entity instanceof ServerPlayer player) {
				if (player.getAbilities().flying) {
					continue;
				}
			}
			double maxDistance = getAttractDistance();

			Vec3 direction = new Vec3(0, -1, 0); // TODO: maybe we can change the direction to match the ship that player stands on?
			Vec3 startPos = entity.position(); // Starting position (player's position)
			Vec3 endPos = startPos.add(direction.scale(maxDistance));
			HitResult hitResult = level.clip(new ClipContext(
					startPos,
					endPos,
					ClipContext.Block.COLLIDER, // Raycast considers block collision shapes, maybe we don't want this?
					ClipContext.Fluid.NONE,     // Ignore fluids
					entity
			));
			double distance = startPos.distanceToSqr(hitResult.getLocation());
			double scaledForce = Math.min(maxDistance * maxDistance / distance * MIN_FORCE, getMaxForce());

			Vec3 force = direction.scale(scaledForce);
			System.out.println(entity);
			System.out.println(force);
			entity.push(force.x, force.y, force.z);
		}
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {
	}
}
