package net.jcm.vsch.ship;

import net.minecraft.util.StringRepresentable;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.function.Consumer;

public class ThrusterData {

	public enum ThrusterMode implements StringRepresentable  {
		POSITION("position"),
		GLOBAL("global");

		private final String name;

		// Constructor that takes a string parameter
		ThrusterMode(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}


		public ThrusterMode toggle() {
			return this == POSITION ? GLOBAL : POSITION;
		}
	}

	public volatile Vector3d force;
	private Vector3d forceSwap = new Vector3d();
	public volatile ThrusterMode mode;

	public ThrusterData(Vector3d force, ThrusterMode mode) {
		this.force = force;
		this.mode = mode;
	}

	public void setForce(Vector3dc force) {
		synchronized (this.forceSwap) {
			Vector3d f = this.force;
			this.force = this.forceSwap.set(force);
			this.forceSwap = f;
		}
	}

	public void setForce(Consumer<Vector3d> setter) {
		synchronized (this.forceSwap) {
			Vector3d f = this.force;
			setter.accept(this.forceSwap);
			this.force = this.forceSwap;
			this.forceSwap = f;
		}
	}

	public String toString() {
		Vector3d force = this.force;
		return "Direction: " + force.normalize() + " Throttle: " + force.length() + " Mode: " + this.mode;
	}
}
