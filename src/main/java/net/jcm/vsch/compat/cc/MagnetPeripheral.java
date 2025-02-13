package net.jcm.vsch.compat.cc;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

import net.jcm.vsch.blocks.entity.MagnetBlockEntity;
import net.jcm.vsch.ship.ThrusterData;

public class MagnetPeripheral implements IPeripheral {
	private final MagnetBlockEntity entity;

	public MagnetPeripheral(MagnetBlockEntity entity) {
		this.entity = entity;
	}

	@Override
	public Object getTarget() {
		return this.entity;
	}

	@Override
	public String getType() {
		return "starlance_magnet";
	}

	@LuaFunction
	public final boolean isGenerator() {
		return this.entity.getIsGenerator();
	}

	@LuaFunction
	public final void setGenerator(boolean isGenerator) {
		this.entity.setIsGenerator(isGenerator);
	}

	@LuaFunction
	public final boolean getPeripheralMode() {
		return this.entity.getPeripheralMode();
	}

	@LuaFunction
	public final void setPeripheralMode(boolean mode) {
		this.entity.setPeripheralMode(mode);
	}

	@LuaFunction
	public final float getPower() {
		return this.entity.getPower();
	}

	@LuaFunction
	public final void setPower(double power) throws LuaException {
		if (!this.entity.getPeripheralMode()) {
			throw new LuaException("Peripheral mode is off, redstone control only");
		}
		this.entity.setPower((float) power);
	}

	@Override
	public boolean equals(IPeripheral other) {
		if (this == other) {
			return true;
		}
		if (other instanceof MagnetPeripheral otherThruster) {
			return this.entity == otherThruster.entity;
		}
		return false;
	}
}
