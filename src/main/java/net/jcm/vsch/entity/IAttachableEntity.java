package net.jcm.vsch.entity;

import net.minecraft.core.BlockPos;

public interface IAttachableEntity {
	BlockPos getAttachedBlockPos();

	void setAttachedBlockPos(BlockPos pos);
}
