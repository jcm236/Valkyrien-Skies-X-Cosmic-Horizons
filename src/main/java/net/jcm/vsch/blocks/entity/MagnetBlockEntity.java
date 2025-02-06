package net.jcm.vsch.blocks.entity;

import dan200.computercraft.shared.Capabilities;

import net.jcm.vsch.blocks.custom.template.BlockEntityWithEntity;
import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.compat.cc.MagnetPeripheral;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.entity.MagnetEntity;
import net.jcm.vsch.entity.VSCHEntities;
import net.jcm.vsch.ship.MagnetData;
import net.jcm.vsch.ship.VSCHForceInducedShips;

import net.jcm.vsch.util.VSCHUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;

public class MagnetBlockEntity extends BlockEntityWithEntity<MagnetEntity> {
	private Vec3 facing; // TODO: update facing as block updated
	private final MagnetData magnetData;
	private boolean needInit = true;
	private volatile float power = -1.0f; // Range: [-1.0, 1.0]
	private volatile float tickPower = 0f;
	private volatile boolean isPeripheralMode = false;
	private boolean wasPeripheralMode = true;
	private volatile boolean isGenerator = false;
	private LazyOptional<Object> lazyPeripheral = LazyOptional.empty();
	private final MagnetEnergyStorage energyStorage = new MagnetEnergyStorage(VSCHConfig.MAGNET_BLOCK_CONSUME_ENERGY.get().intValue());

	public MagnetBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.MAGNET_BLOCK_ENTITY.get(), pos, state);
		this.facing = Vec3.atLowerCornerOf(state.getValue(DirectionalBlock.FACING).getNormal());
		this.magnetData = new MagnetData(this.facing.toVector3f());
	}

	public float getAttractDistance() {
		return VSCHConfig.MAGNET_BLOCK_DISTANCE.get().floatValue() + 1;
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
	 * @return magnet target working power between -1.0~1.0
	 */
	public float getPower() {
		return this.power;
	}

	/**
	 * @return the max power the magnet can activate based on the energy input
	 */
	public float getMaxAvaliablePower() {
		return this.energyStorage.maxEnergyRate == 0 ? 1.0f : (float)(this.energyStorage.stored) / this.energyStorage.maxEnergyRate;
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

	private static Vector3f getStandardForceTo(Vector3f selfPos, float angle, Vector3f pos, Vector3f dest) {
		final double maxForce = VSCHConfig.MAGNET_BLOCK_MAX_FORCE.get().doubleValue();
		final double maxDistance = VSCHConfig.MAGNET_BLOCK_DISTANCE.get().doubleValue();

		pos.sub(selfPos, dest);
		float distanceSquared = dest.lengthSquared();

		// Distance factor in range [0, 1], where 1 is close and 0 is at max range
		// this way max force is more evenly distributed along the magnets range
		float distanceFactor = (float) (1.0 - Math.min(1.0, Math.sqrt(distanceSquared) / maxDistance));

		// ChatGPT falloff curve, feel free to change
		float force = (float) (maxForce * Math.pow(distanceFactor, 2) * Math.cos(angle));

		return dest.normalize(force);
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
		Vector3d center = transform.transformPosition(new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5));

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

		return serverLevel.<MagnetEntity>getEntities(MagnetEntity.TESTER, box, (magnet) -> {
			Vec3 position = magnet.position();
			if (center.distanceSquared(position.x, position.y, position.z) > maxDistanceSqr) {
				return false;
			}
			MagnetBlockEntity block = magnet.getAttachedBlock();
			if (block == null) {
				return false;
			}
			if (block.tickPower == 0) {
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
		if (cap == ForgeCapabilities.ENERGY) {
			return LazyOptional.of(() -> this.energyStorage).cast();
		} else if (CompatMods.COMPUTERCRAFT.isLoaded() && cap == Capabilities.CAPABILITY_PERIPHERAL) {
			if (!lazyPeripheral.isPresent()) {
				lazyPeripheral = LazyOptional.of(() -> new MagnetPeripheral(this));
			}
			return lazyPeripheral.cast();
		}
		return super.getCapability(cap, direction);
	}

	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("StoredEnergy", this.energyStorage.stored);
	}

	@Override
	public void load(CompoundTag tag) {
		this.energyStorage.stored = tag.getInt("StoredEnergy");
		super.load(tag);
	}

	public void neighborChanged(Block block, BlockPos pos, boolean moving) {
		if (!this.isPeripheralMode) {
			this.updatePowerByRedstone();
		}
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos blockPos, BlockState state) {
		super.tickForce(level, blockPos, state);

		if (this.wasPeripheralMode != this.isPeripheralMode && !this.isPeripheralMode) {
			this.updatePowerByRedstone();
		}
		this.wasPeripheralMode = this.isPeripheralMode;
		// ---------- //


		// TODO: find a proper way to check if the facing is changed
		boolean facingChanged = false;
		if (facingChanged) {
			this.facing = Vec3.atLowerCornerOf(state.getValue(DirectionalBlock.FACING).getNormal());
			this.magnetData.facing = this.facing.toVector3f();
		}

		if (!this.isGenerator) {
			// Determine the energy required for this tick
			float requiredEnergy = this.power * this.energyStorage.maxEnergyRate;

			if (requiredEnergy == 0) {
				// No energy needed (aka we store 0 energy), directly set tickPower
				this.tickPower = this.power;
			} else {
				// Clamp required energy within the available stored energy range
				if (requiredEnergy < 0) {
					requiredEnergy = Math.max(requiredEnergy, -this.energyStorage.stored);
				} else {
					requiredEnergy = Math.min(requiredEnergy, this.energyStorage.stored);
				}

				// Consume the energy (should we use the extract function for this?)
				this.energyStorage.stored -= (int) Math.abs(requiredEnergy);
				this.tickPower = requiredEnergy / this.energyStorage.maxEnergyRate;
			}
		}


		if (!VSGameUtilsKt.isBlockInShipyard(this.getLevel(), this.getBlockPos())) {
			return;
		}

		if (this.needInit) {
			VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, blockPos);
			if (ships != null && ships.getThrusterAtPos(blockPos) == null) {
				ships.addMagnet(blockPos, this.magnetData);
				this.needInit = false;
			}
		}

		if (tickPower == 0) {
			return;
		}

		Vec3 selfPos = this.getLinkedEntity().position();
		Vector3f selfFacing = this.getFacing().mul(0.4f);
		Vector3f selfPosFront = new Vector3f((float)(selfPos.x) + selfFacing.x, (float)(selfPos.y) + selfFacing.y, (float)(selfPos.z) + selfFacing.z);
		Vector3f selfPosBack = new Vector3f((float)(selfPos.x) - selfFacing.x, (float)(selfPos.y) - selfFacing.y, (float)(selfPos.z) - selfFacing.z);
		List<MagnetEntity> magnets = this.scanMagnets();
		this.magnetData.setForces((frontForce, backForce) -> {
			Vector3f forceDest1 = new Vector3f();
			Vector3f forceDest2 = new Vector3f();
			Vector3f forceDest3 = new Vector3f();
			Vector3f forceDest4 = new Vector3f();
			for (MagnetEntity magnet : magnets) {
				float power = tickPower * magnet.getAttachedBlock().tickPower;
				Vec3 pos = magnet.position();
				Vector3f facing = magnet.getFacing().mul(0.4f);
				float angle = selfFacing.angle(facing);

				forceDest1.set((float)(pos.x), (float)(pos.y), (float)(pos.z)).add(facing);
				getStandardForceTo(selfPosFront, angle, forceDest1, forceDest1);
				forceDest1.mul(power);

				forceDest2.set((float)(pos.x), (float)(pos.y), (float)(pos.z)).sub(facing);
				getStandardForceTo(selfPosFront, angle, forceDest2, forceDest2);
				forceDest2.mul(power);

				forceDest3.set((float)(pos.x), (float)(pos.y), (float)(pos.z)).add(facing);
				getStandardForceTo(selfPosBack, angle, forceDest3, forceDest3);
				forceDest3.mul(power);

				forceDest4.set((float)(pos.x), (float)(pos.y), (float)(pos.z)).sub(facing);
				getStandardForceTo(selfPosBack, angle, forceDest4, forceDest4);
				forceDest4.mul(power);

				float maxForce = forceDest1.lengthSquared();
				Vector3f maxForceVec = forceDest1;
				for (Vector3f v : new Vector3f[]{forceDest2, forceDest3, forceDest4}) {
					float l = v.lengthSquared();
					if (l > maxForce) {
						maxForce = l;
						maxForceVec = v;
					}
				}
				if (maxForceVec == forceDest1 || maxForceVec == forceDest2) {
					frontForce.add(maxForceVec);
				} else {
					backForce.add(maxForceVec);
				}
			}
		});
	}

	private void updatePowerByRedstone() {
		float newPower = getPowerByRedstone(this.getLevel(), this.getBlockPos());
		this.setPower(newPower);
	}

	private static float getPowerByRedstone(Level level, BlockPos pos) {
		int signal = level.getBestNeighborSignal(pos);
		return signal == 0 ? -1 : (float)(signal - 1) / 14 * 2 - 1;
	}

	private class MagnetEnergyStorage implements IEnergyStorage {
		final int maxEnergyRate;
		int stored = 0;

		MagnetEnergyStorage(int maxEnergyRate) {
			this.maxEnergyRate = maxEnergyRate;
		}

		@Override
		public boolean canReceive() {
			return !MagnetBlockEntity.this.isGenerator;
		}

		@Override
		public int receiveEnergy(int avaliable, boolean simulate) {
			int received = this.maxEnergyRate - this.stored;
			if (received > avaliable) {
				received = avaliable;
			}
			if (!simulate) {
				this.stored += received;
			}
			return received;
		}

		@Override
		public int getEnergyStored() {
			return this.stored;
		}

		@Override
		public int getMaxEnergyStored() {
			return this.maxEnergyRate;
		}

		@Override
		public boolean canExtract() {
			return MagnetBlockEntity.this.isGenerator;
		}

		@Override
		public int extractEnergy(int require, boolean simulate) {
			if (require > this.stored) {
				require = this.stored;
			}
			if (!simulate) {
				this.stored -= require;
			}
			return require;
		}
	}
}
