package net.jcm.vsch.blocks.entity;

import com.mojang.logging.LogUtils;

import dan200.computercraft.shared.Capabilities;

import net.jcm.vsch.blocks.custom.template.BlockEntityWithEntity;
import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.compat.cc.MagnetPeripheral;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.entity.MagnetEntity;
import net.jcm.vsch.entity.VSCHEntities;
import net.jcm.vsch.ship.ThrusterData;
import net.jcm.vsch.ship.VSCHForceInducedShips;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Collections;
import java.util.List;

public class MagnetBlockEntity extends BlockEntityWithEntity<MagnetEntity> {
	private static final Logger LOGGER = LogUtils.getLogger();

	private Vec3 facing; // TODO: update facing as block updated
	private final ThrusterData thrusterData;
	private volatile float power = -1.0f; // Range: [-1.0, 1.0]
	private volatile boolean isPeripheralMode = false;
	private boolean wasPeripheralMode = true;
	private LazyOptional<Object> lazyPeripheral = LazyOptional.empty();

	public MagnetBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.MAGNET_BLOCK_ENTITY.get(), pos, state);
		this.facing = Vec3.atLowerCornerOf(state.getValue(DirectionalBlock.FACING).getNormal());
		this.thrusterData = new ThrusterData(
			new Vector3d(),
			ThrusterData.ThrusterMode.POSITION
		);
	}

	public float getAttractDistance() {
		return VSCHConfig.MAGNET_BLOCK_DISTANCE.get().floatValue();
	}

	@Override
	public MagnetEntity createLinkedEntity(ServerLevel level, BlockPos pos) {
		MagnetEntity entity = new MagnetEntity(VSCHEntities.MAGNET_ENTITY.get(), level);
		entity.setAttachedBlockPos(this.getBlockPos());
		return entity;
	}

	public Vector3f getFacing() {
		Vector3f facing = this.facing.toVector3f();
		Ship ship = VSGameUtilsKt.getShipObjectManagingPos(this.getLevel(), this.getBlockPos());
		if (ship != null) {
			facing = ship.getShipToWorld().transformDirection(facing);
		}
		return facing;
	}

	/**
	 * @return magnet power between -1.0~1.0
	 */
	public float getPower() {
		return this.power;
	}

	public void setPower(float power) {
		this.setPower(power, true);
	}

	protected void setPower(float power, boolean update) {
		float newPower = Math.min(Math.max(power, -1), 1);
		if (this.power == newPower) {
			return;
		}
		this.power = newPower;
		if (update) {
			this.markPowerChanged();
		}
	}

	public boolean getPeripheralMode() {
		return this.isPeripheralMode;
	}

	public void setPeripheralMode(boolean on) {
		if (this.isPeripheralMode != on) {
			this.isPeripheralMode = on;
			this.setChanged();
		}
	}

	protected void markPowerChanged() {
		this.setChanged();
		this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 11);
	}

	private Vector3f getStandardForceTo(Vec3 pos, Vector3f direction) {
		final double maxForce = VSCHConfig.MAGNET_BLOCK_MAX_FORCE.get().doubleValue();
		Vec3 selfPos = this.getLinkedEntity().position();
		double distance = selfPos.distanceToSqr(pos);
		Vector3f facing = this.getFacing();
		float angle = facing.angle(direction.mul(-1));
		float force = (float)(maxForce / distance * Math.cos(angle / 180 * Math.PI));
		return pos.toVector3f().sub((float)(selfPos.x), (float)(selfPos.y), (float)(selfPos.z)).normalize(force, new Vector3f());
	}

	/**
	 * @return the list of magnets that the current magnet is moving towards to.
	 *   or {@code null} if the current magnet is not on a {@link ServerShip}
	 */
	private List<MagnetEntity> scanMagnets() {
		Level level = this.getLevel();
		if (!(level instanceof ServerLevel serverLevel)) {
			return null;
		}
		BlockPos blockPos = this.getBlockPos();
		ServerShip currentShip = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, blockPos);
		if (currentShip == null || currentShip.isStatic()) {
			return Collections.emptyList();
		}
		final float maxDistance = this.getAttractDistance();
		final float maxDistanceSqr = maxDistance * maxDistance;
		Vector3f facing = this.getFacing();
		Matrix4dc transform = currentShip.getShipToWorld();
		Vector3d center = transform.transformPosition(new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5).add(this.facing.toVector3f().mul(0.49f)));

		Vec3 axisX = new Vec3(transform.transformDirection(new Vector3f(maxDistance, 0, 0)));
		Vec3 axisY = new Vec3(transform.transformDirection(new Vector3f(0, maxDistance, 0)));
		Vec3 axisZ = new Vec3(transform.transformDirection(new Vector3f(0, 0, maxDistance)));
		// TODO: find a easier calculation without ton of min/maxes
		double minX = Math.min(Math.min(Math.min(Math.min(Math.min(axisX.x, axisY.x), axisZ.x), -axisX.x), -axisY.x), -axisZ.x);
		double minY = Math.min(Math.min(Math.min(Math.min(Math.min(axisX.y, axisY.y), axisZ.y), -axisX.y), -axisY.y), -axisZ.y);
		double minZ = Math.min(Math.min(Math.min(Math.min(Math.min(axisX.z, axisY.z), axisZ.z), -axisX.z), -axisY.z), -axisZ.z);
		double maxX = Math.max(Math.max(Math.max(Math.max(Math.max(axisX.x, axisY.x), axisZ.x), -axisX.x), -axisY.x), -axisZ.x);
		double maxY = Math.max(Math.max(Math.max(Math.max(Math.max(axisX.y, axisY.y), axisZ.y), -axisX.y), -axisY.y), -axisZ.y);
		double maxZ = Math.max(Math.max(Math.max(Math.max(Math.max(axisX.z, axisY.z), axisZ.z), -axisX.z), -axisY.z), -axisZ.z);
		AABB box = new AABB(minX, minY, minZ, maxX, maxY, maxZ).move(center.x, center.y, center.z);

		LOGGER.info("magnet box: " + this + ": " + box);
		LOGGER.info("this: " + this.getLinkedEntity());
		return serverLevel.<MagnetEntity>getEntities(MagnetEntity.TESTER, box, (magnet) -> {
			Vec3 position = magnet.position();
			LOGGER.info("magnet force: " + this + " -> " + magnet);
			if (center.distanceSquared(position.x, position.y, position.z) > maxDistanceSqr) {
				return false;
			}
			ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, magnet.getAttachedBlockPos());
			if (ship == currentShip) {
				return false;
			}
			return true;
		});
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction direction) {
		if (CompatMods.COMPUTERCRAFT.isLoaded() && cap == Capabilities.CAPABILITY_PERIPHERAL) {
			if (!lazyPeripheral.isPresent()) {
				lazyPeripheral = LazyOptional.of(() -> new MagnetPeripheral(this));
			}
			return lazyPeripheral.cast();
		}
		return super.getCapability(cap, direction);
	}

	public void neighborChanged(Block block, BlockPos pos, boolean moving) {
		if (!this.isPeripheralMode) {
			this.updatePowerByRedstone();
		}
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		super.tickForce(level, pos, state);

		if (this.wasPeripheralMode != this.isPeripheralMode && !this.isPeripheralMode) {
			this.updatePowerByRedstone();
		}
		this.wasPeripheralMode = this.isPeripheralMode;

		boolean facingChanged = true; // TODO: find a proper way to check if the facing is changed
		if (facingChanged) {
			this.facing = Vec3.atLowerCornerOf(state.getValue(DirectionalBlock.FACING).getNormal());
		}

		List<MagnetEntity> magnets = this.scanMagnets();
		this.thrusterData.setForce((totalForce) -> {
			totalForce.set(0f);
			for (MagnetEntity magnet : magnets) {
				Vector3f force = this.getStandardForceTo(magnet.position(), magnet.getFacing()).mul(this.power * magnet.getAttachedBlock().power);
				LOGGER.info("force " + force + " power:" + -this.power * magnet.getAttachedBlock().power);
				totalForce.add(force);
			}
			LOGGER.info("total force " + totalForce);
		});

		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
		if (ships != null && ships.getThrusterAtPos(pos) == null) {
			ships.addThruster(pos, this.thrusterData);
		}
	}

	private void updatePowerByRedstone() {
		float newPower = getPowerByRedstone(this.getLevel(), this.getBlockPos());
		this.setPower(newPower);
	}

	private static float getPowerByRedstone(Level level, BlockPos pos) {
		return (float)(level.getBestNeighborSignal(pos)) / 15 * 2 - 1;
	}
}
