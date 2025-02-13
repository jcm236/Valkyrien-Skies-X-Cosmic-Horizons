package net.jcm.vsch.entity;

import net.jcm.vsch.blocks.VSCHBlocks;
import net.jcm.vsch.blocks.entity.MagnetBlockEntity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class MagnetEntity extends Entity implements IAttachableEntity {
	public static final EntityTypeTest TESTER = EntityTypeTest.forClass(MagnetEntity.class);

	private BlockPos pos;
	private int keepAlive = 0;

	public MagnetEntity(EntityType<? extends MagnetEntity> entityType, Level level) {
		super(entityType, level);
		this.noPhysics = true; // Prevents collision with blocks
		this.setInvisible(true);
	}

	public BlockPos getAttachedBlockPos() {
		return this.pos;
	}

	public void setAttachedBlockPos(BlockPos pos) {
		this.pos = pos;
		this.keepAlive = 1;
	}

	public MagnetBlockEntity getAttachedBlock() {
		if (this.pos == null) {
			return null;
		}
		return (MagnetBlockEntity)(this.level().getBlockEntity(this.pos));
	}

	public Vector3f getFacing() {
		return this.getAttachedBlock().getFacing();
	}

	@Override
	public void tick() {
		Level level = this.level();
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		this.keepAlive--;
		if (this.pos == null || this.keepAlive < 0) {
			this.discard();
			return;
		}

		MagnetBlockEntity block = this.getAttachedBlock();
		if (block == null) {
			this.discard();
			return;
		}
		Vector3d pos = block.getWorldPos();
		this.setPosRaw(pos.x, pos.y, pos.z);
	}

	@Override
	protected void defineSynchedData() {}

	@Override
	protected void readAdditionalSaveData(CompoundTag compoundTag) {
		int x = compoundTag.getInt("attachPosX");
		int y = compoundTag.getInt("attachPosY");
		int z = compoundTag.getInt("attachPosZ");
		this.pos = new BlockPos(x, y, z);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag compoundTag) {
		compoundTag.putInt("attachPosX", pos.getX());
		compoundTag.putInt("attachPosY", pos.getY());
		compoundTag.putInt("attachPosZ", pos.getZ());
	}
	
	@Override	
	public boolean broadcastToPlayer(ServerPlayer player) {
		return false;
	}

	@Override
	public boolean shouldRender(double pX, double pY, double pZ) {
		return false;
	}

	public static class Renderer extends EntityRenderer<MagnetEntity> {
		public Renderer(EntityRendererProvider.Context ctx) {
			super(ctx);
		}

		@Override
		public boolean shouldRender(MagnetEntity a0, Frustum a1, double a2, double a3, double a4) {
			return false;
		}

		@Override
		public ResourceLocation getTextureLocation(MagnetEntity a0) {
			return null;
		}
	}
}
