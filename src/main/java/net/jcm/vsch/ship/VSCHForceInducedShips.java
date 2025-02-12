package net.jcm.vsch.ship;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.core.impl.program.VSCoreImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.util.VSCHUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public class VSCHForceInducedShips implements ShipForcesInducer {

	/**
	 * Don't mess with this unless you know what your doing. I'm making it public for all the people that do know what their doing.
	 * Instead, look at {@link #addThruster(BlockPos, ThrusterData)} or {@link #removeThruster(BlockPos)} or {@link #getThrusterAtPos(BlockPos)}
	 */
	public Map<BlockPos, ThrusterData> thrusters = new ConcurrentHashMap<>();
	private Map<BlockPos, MagnetData> magnets = new ConcurrentHashMap<>();

	/**
	 * Don't mess with this unless you know what your doing. I'm making it public for all the people that do know what their doing.
	 * Instead, look at {@link #addDragger(BlockPos, DraggerData)} or {@link #removeDragger(BlockPos)} or {@link #getDraggerAtPos(BlockPos)}
	 */
	public Map<BlockPos, DraggerData> draggers = new ConcurrentHashMap<>();

	private String dimensionId = null;

	public VSCHForceInducedShips() {}

	public VSCHForceInducedShips(String dimensionId) {
		this.dimensionId = dimensionId;
	}

	@Override
	public void applyForces(@NotNull PhysShip physicShip) {
		PhysShipImpl physShip = (PhysShipImpl) physicShip;
		// Apply thrusters force
		thrusters.forEach((pos, data) -> {
			if (data.force.lengthSquared() == 0.0f) {
				return;
			}

			// Transform force direction from ship relative to world relative
			Vector3d tForce = physicShip.getTransform().getShipToWorld().transformDirection(data.force, new Vector3d());

			if (VSCHConfig.LIMIT_SPEED.get()) {
				int maxSpeed = VSCHConfig.MAX_SPEED.get().intValue();
				Vector3dc linearVelocity = physShip.getPoseVel().getVel();
				if (linearVelocity.lengthSquared() >= maxSpeed * maxSpeed) {
					double dotProduct = tForce.dot(linearVelocity);
					if (dotProduct > 0) {
						switch (data.mode) {
							case GLOBAL -> applyScaledForce(physShip, linearVelocity, tForce, maxSpeed);
							case POSITION -> {
								// POSITION should be the only other value
								Vector3d tPos = VectorConversionsMCKt.toJOMLD(pos)
									.add(0.5, 0.5, 0.5)
									.sub(physicShip.getTransform().getPositionInShip());

								Vector3d parallel = new Vector3d(tPos).mul(tForce.dot(tPos) / tForce.dot(tForce));
								Vector3d perpendicular = new Vector3d(tForce).sub(parallel);

								// rotate the ship
								physicShip.applyInvariantForceToPos(perpendicular, tPos);

								// apply global force, since the force is perfectly lined up with the centre of gravity
								applyScaledForce(physShip, linearVelocity, parallel, maxSpeed);
							}
						}
						return;
					}
				}
			}

			// Switch between applying force at position and just applying the force
			switch (data.mode) {
				case GLOBAL -> physicShip.applyInvariantForce(tForce);
				case POSITION -> {
					Vector3d tPos = VectorConversionsMCKt.toJOMLD(pos)
						.add(0.5, 0.5, 0.5)
						.sub(physicShip.getTransform().getPositionInShip());
					physicShip.applyInvariantForceToPos(tForce, tPos);
				}
			}
		});

		Vector3d frontForce = new Vector3d();
		Vector3d backForce = new Vector3d();
		magnets.forEach((blockPos, data) -> {
			Vector3f facing = data.facing;
			boolean isGenerator = data.isGenerator;
			MagnetData.ForceCalculator forceCalculator = data.forceCalculator;
			frontForce.set(0, 0, 0);
			backForce.set(0, 0, 0);
			forceCalculator.calc(physShip, frontForce, backForce);
			if (isGenerator) {
				physShip.applyInvariantForce(frontForce);
				physShip.applyInvariantTorque(backForce);
				return;
			}

			boolean hasFrontForce = frontForce.lengthSquared() != 0;
			boolean hasBackForce = backForce.lengthSquared() != 0;
			if (!hasFrontForce && !hasBackForce) {
				return;
			}
			ShipTransform transform = physicShip.getTransform();
			Vector3d frontPos = new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5)
				.sub(transform.getPositionInShip())
				.add(facing.div(2, new Vector3f()));
			Vector3d backPos = frontPos.sub(facing, new Vector3d());

			if (VSCHConfig.LIMIT_SPEED.get()) {
				int maxSpeed = VSCHConfig.MAX_SPEED.get().intValue();
				Vector3dc linearVelocity = physShip.getPoseVel().getVel();
				if (linearVelocity.lengthSquared() >= maxSpeed * maxSpeed) {
					if (hasFrontForce) {
						if (frontForce.dot(linearVelocity) > 0) {
							Vector3d parallel = new Vector3d(frontPos).mul(frontForce.dot(frontPos) / frontForce.dot(frontForce));
							Vector3d perpendicular = new Vector3d(frontForce).sub(parallel);

							// rotate the ship
							physicShip.applyInvariantForceToPos(perpendicular, frontPos);

							// apply global force, since the force is perfectly lined up with the centre of gravity
							applyScaledForce(physShip, linearVelocity, parallel, maxSpeed);
						} else {
							physicShip.applyInvariantForceToPos(frontForce, frontPos);
						}
					}
					if (hasBackForce) {
						if (backForce.dot(linearVelocity) > 0) {
							Vector3d parallel = new Vector3d(backPos).mul(backForce.dot(backPos) / backForce.dot(backForce));
							Vector3d perpendicular = new Vector3d(backForce).sub(parallel);

							// rotate the ship
							physicShip.applyInvariantForceToPos(perpendicular, backPos);

							// apply global force, since the force is perfectly lined up with the centre of gravity
							applyScaledForce(physShip, linearVelocity, parallel, maxSpeed);
						} else {
							physicShip.applyInvariantForceToPos(backForce, backPos);
						}
					}
					return;
				}
			}

			if (hasFrontForce) {
				physicShip.applyInvariantForceToPos(frontForce, frontPos);
			}
			if (hasBackForce) {
				physicShip.applyInvariantForceToPos(backForce, backPos);
			}
		});

		// Prep for draggers
		// Apply draggers force
		draggers.forEach((pos, data) -> {
			Vector3dc linearVelocity = physShip.getPoseVel().getVel();
			Vector3dc angularVelocity = physShip.getPoseVel().getOmega();

			if (!data.on) {
				return;
			}

			// Get position relative to center of mass

			// ChatGPT math, I suck at this stuff lol:
			// Get rotational velocity as the cross product of angular velocity and relative position
			//angularVelocity.cross(relativePosition, rotationalVelocity);

			// Add linear and rotational velocities
			Vector3d acceleration = linearVelocity.negate(new Vector3d());
			Vector3d force = acceleration.mul(physShip.getInertia().getShipMass());

			force = VSCHUtils.clampVector(force, VSCHConfig.MAX_DRAG.get().intValue());

			Vector3d rotAcceleration = angularVelocity.negate(new Vector3d());
			Vector3d rotForce = rotAcceleration.mul(physShip.getInertia().getShipMass());

			rotForce = VSCHUtils.clampVector(rotForce, VSCHConfig.MAX_DRAG.get().intValue());

			physShip.applyInvariantForce(force);
			physShip.applyInvariantTorque(rotForce);
		});
	}

	private static void applyScaledForce(PhysShipImpl physShip, Vector3dc linearVelocity, Vector3d tForce, int maxSpeed) {
		assert ValkyrienSkiesMod.getCurrentServer() != null;
		double deltaTime = 1.0 / (VSGameUtilsKt.getVsPipeline(ValkyrienSkiesMod.getCurrentServer()).computePhysTps());
		double mass = physShip.getInertia().getShipMass();

		// Invert the parallel projection of tForce onto linearVelocity and scales it so that the resulting speed is exactly
		// equal to length of linearVelocity, but still in the direction the ship would have been going without the speed limit
		Vector3d targetVelocity = new Vector3d(linearVelocity).add(new Vector3d(tForce).mul(deltaTime / mass)).normalize(maxSpeed).sub(linearVelocity);

		// Apply the force at no specific position
		physShip.applyInvariantForce(targetVelocity.mul(mass / deltaTime));
	}

	// ----- Thrusters ----- //

	public void addThruster(BlockPos pos, ThrusterData data) {
		thrusters.put(pos, data);
	}

	public void removeThruster(BlockPos pos) {
		thrusters.remove(pos);
	}

	@Nullable
	public ThrusterData getThrusterAtPos(BlockPos pos) {
		return thrusters.get(pos);
	}

	// ----- Magnets ----- //

	public void addMagnet(BlockPos pos, MagnetData data) {
		magnets.put(pos, data);
	}

	public void removeMagnet(BlockPos pos) {
		magnets.remove(pos);
	}

	@Nullable
	public MagnetData getMagnetAtPos(BlockPos pos) {
		return magnets.get(pos);
	}

	// ----- Draggers ----- //

	public void addDragger(BlockPos pos, DraggerData data) {
		draggers.put(pos, data);
	}

	public void removeDragger(BlockPos pos) {
		draggers.remove(pos);
	}

	@Nullable
	public DraggerData getDraggerAtPos(BlockPos pos) {
		return draggers.get(pos);
	}

	// ----- Force induced ships ----- //

	public static VSCHForceInducedShips getOrCreate(ServerShip ship, String dimensionId) {
		VSCHForceInducedShips attachment = ship.getAttachment(VSCHForceInducedShips.class);
		if (attachment == null) {
			attachment = new VSCHForceInducedShips(dimensionId);
			ship.saveAttachment(VSCHForceInducedShips.class, attachment);
		}
		return attachment;
	}

	public static VSCHForceInducedShips getOrCreate(ServerShip ship) {
		return getOrCreate(ship, ship.getChunkClaimDimension());
	}

	public static VSCHForceInducedShips get(Level level, BlockPos pos) {
		ServerLevel serverLevel = (ServerLevel) level;
		ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, pos);
		// Seems counter-intutive at first. But basically, it returns null if it wasn't a ship. Otherwise, it gets the attachment OR creates and then gets it
		return ship != null ? getOrCreate(ship) : null;
	}
}
