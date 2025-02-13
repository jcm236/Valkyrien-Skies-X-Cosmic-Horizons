package net.jcm.vsch.blocks.custom.template;

import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.entity.IAttachableEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A block entity for spawning and removing an entity with the block.
 * Make sure to overwrite createLinkedEntity.
 * @see BlockWithEntity
 * @param <E> The entity class to be using
 */
public abstract class BlockEntityWithEntity<E extends Entity & IAttachableEntity> extends BlockEntity implements ParticleBlockEntity {
	private UUID entityUUID = null;
	private E entity = null;

	public BlockEntityWithEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * The function used by this BE to create the entity object to spawn.
	 * Overwrite with creating a new object of your entity class.
	 * @param level Level, for help creating the entity object
	 * @return
	 */
	public abstract E createLinkedEntity(ServerLevel level, BlockPos pos);

	public E getLinkedEntity() {
		if (this.entity == null) {
			if (this.entityUUID != null && this.getLevel() instanceof ServerLevel serverLevel) {
				this.entity = (E) serverLevel.getEntity(this.entityUUID);
			}
		}
		return this.entity;
	}

	public E getOrSpawnLinkedEntity() {
		if (this.entity == null && this.getLevel() instanceof ServerLevel serverLevel) {
			if (this.entityUUID != null) {
				this.entity = (E) serverLevel.getEntity(this.entityUUID);
			}
			if (this.entity == null) {
				this.spawnLinkedEntityImpl(serverLevel, this.getBlockPos());
			}
		}
		return this.entity;
	}

	public void respawnLinkedEntity() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (this.entityUUID != null) {
			this.removeLinkedEntity();
		}
		this.spawnLinkedEntityImpl(serverLevel, this.getBlockPos());
	}

	private void spawnLinkedEntityImpl(ServerLevel level, BlockPos blockPos) {
		E entity = this.createLinkedEntity(level, blockPos);
		entity.setPos(Vec3.atCenterOf(blockPos));
		level.addFreshEntity(entity);
		this.entityUUID = entity.getUUID();
		this.entity = entity;
	}

	public void removeLinkedEntity() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (this.entityUUID == null) {
			return;
		}
		Entity entity = this.getLinkedEntity();
		this.entityUUID = null;
		this.entity = null;
		// Don't remove the entity if it doesn't exist, duh
		if (entity != null) {
			entity.discard();
		}
	}

	// Saving and loading the attached entity UUID on world reload:
	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (this.entityUUID != null) {
			tag.putUUID("EntityUUID", this.entityUUID);
		}
	}

	@Override
	public void load(CompoundTag tag) {
		if (tag.hasUUID("EntityUUID")) {
			this.entityUUID = tag.getUUID("EntityUUID");
		}
		super.load(tag);
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		this.getOrSpawnLinkedEntity().setAttachedBlockPos(pos);
	}
}
