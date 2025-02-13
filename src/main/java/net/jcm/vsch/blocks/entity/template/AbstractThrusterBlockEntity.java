package net.jcm.vsch.blocks.entity.template;

import dan200.computercraft.shared.Capabilities;

import net.jcm.vsch.blocks.custom.template.AbstractThrusterBlock;
import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.compat.cc.ThrusterPeripheral;
import net.jcm.vsch.ship.ThrusterData;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.lointain.cosmos.init.CosmosModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public abstract class AbstractThrusterBlockEntity extends BlockEntity implements ParticleBlockEntity {
	private final String typeString;
	private Vector3f facing;
	private final ThrusterData thrusterData;
	private volatile float power = 0;
	private volatile boolean powerChanged = false;
	// Peripheral mode determines if the throttle is controlled by redstone, or by CC computers
	private volatile boolean isPeripheralMode = false;
	private boolean wasPeripheralMode = true;
	private LazyOptional<Object> lazyPeripheral = LazyOptional.empty();

	protected AbstractThrusterBlockEntity(String typeStr, BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);

		this.typeString = typeStr;
		this.facing = state.getValue(DirectionalBlock.FACING).step();
		this.thrusterData = new ThrusterData(
			new Vector3d(),
			state.getValue(AbstractThrusterBlock.MODE));
	}

	public String getTypeString() {
		return this.typeString;
	}

	public abstract float getMaxThrottle();

	public float getThrottle() {
		return getPower() * getMaxThrottle();
	}

	/**
	 * @return thruster power between 0.0~1.0
	 */
	public float getPower() {
		return this.power;
	}

	public void setPower(float power) {
		setPower(power, true);
	}

	protected void setPower(float power, boolean update) {
		float newPower = Math.min(Math.max(power, 0), 1);
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
		this.powerChanged = true;
		this.setChanged();
		this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 11);
	}

	public ThrusterData.ThrusterMode getThrusterMode() {
		return this.getBlockState().getValue(AbstractThrusterBlock.MODE);
	}

	public void setThrusterMode(ThrusterData.ThrusterMode mode) {
		this.getLevel().setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(AbstractThrusterBlock.MODE, mode));
		this.thrusterData.mode = mode;
	}

	public ThrusterData getThrusterData() {
		return this.thrusterData;
	}

	private void updateForce() {
		this.thrusterData.setForce((force) -> force.set(this.facing).mul(this.getThrottle()));
	}

	@Override
	public void load(CompoundTag data) {
		this.setPower(data.getFloat("Power"), false);
		this.isPeripheralMode = CompatMods.COMPUTERCRAFT.isLoaded() && data.getBoolean("PeripheralMode");
		this.updateForce();
		super.load(data);
	}

	@Override
	public void saveAdditional(CompoundTag data) {
		super.saveAdditional(data);
		data.putFloat("Power", this.getPower());
		data.putBoolean("PeripheralMode", this.getPeripheralMode());
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag data = super.getUpdateTag();
		data.putFloat("Power", this.getPower());
		return data;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction direction) {
		if (CompatMods.COMPUTERCRAFT.isLoaded() && cap == Capabilities.CAPABILITY_PERIPHERAL) {
			if (!lazyPeripheral.isPresent()) {
				lazyPeripheral = LazyOptional.of(() -> new ThrusterPeripheral(this));
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
		// If we have changed peripheral mode, and we aren't peripheral mode
		if (this.wasPeripheralMode != this.isPeripheralMode && !this.isPeripheralMode) {
			this.updatePowerByRedstone();
		}
		this.wasPeripheralMode = this.isPeripheralMode;

		if (this.powerChanged) {
			this.powerChanged = false;
			this.updateForce();
		}

		boolean isLit = state.getValue(AbstractThrusterBlock.LIT);
		boolean powered = getPower() > 0;
		if (powered != isLit) {
			level.setBlockAndUpdate(pos, state.setValue(AbstractThrusterBlock.LIT, powered));
		}

		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
		if (ships == null) {
			return;
		}

		if (ships.getThrusterAtPos(pos) == null) {
			ships.addThruster(pos, this.thrusterData);
		}
	}

	private void updatePowerByRedstone() {
		float newPower = getPowerByRedstone(this.getLevel(), this.getBlockPos());
		this.setPower(newPower);
	}

	protected ParticleOptions getThrusterParticleType() {
		return CosmosModParticleTypes.THRUSTED.get();
	}

	protected ParticleOptions getThrusterSmokeParticleType() {
		return CosmosModParticleTypes.THRUST_SMOKE.get();
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {
		Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
		// If we aren't on a ship, then we skip
		if (ship == null) {
			return;
		}

		// If we are unpowered, do no particles
		if (getPower() == 0.0) {
			return;
		}

		// BlockPos is always at the corner, getCenter gives us a Vec3 thats centered YAY
		Vec3 center = pos.getCenter();
		// Transform that shipyard pos into a world pos
		Vector3d worldPos = ship.getTransform().getShipToWorld().transformPosition(new Vector3d(center.x, center.y, center.z));

		// Get blockstate direction, NORTH, SOUTH, UP, DOWN, etc
		Direction dir = state.getValue(DirectionalBlock.FACING);
		Vector3d direction = ship.getTransform().getShipToWorldRotation().transform(new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ()));

		spawnParticles(worldPos, direction);
	}

	protected void spawnParticles(Vector3d pos, Vector3d direction) {
		// Offset the XYZ by a little bit so its at the end of the thruster block
		double x = pos.x - direction.x;
		double y = pos.y - direction.y;
		double z = pos.z - direction.z;

		Vector3d speed = new Vector3d(direction).mul(-getPower());

		speed.mul(0.6);

		// All that for one particle per tick...
		level.addParticle(
				getThrusterParticleType(),
				x, y, z,
				speed.x, speed.y, speed.z
				);

		speed.mul(1.06);

		// Ok ok, two particles per tick
		level.addParticle(
				getThrusterSmokeParticleType(),
				x, y, z,
				speed.x, speed.y, speed.z
				);
	}

	private static float getPowerByRedstone(Level level, BlockPos pos) {
		return (float)(level.getBestNeighborSignal(pos)) / 15;
	}
}
