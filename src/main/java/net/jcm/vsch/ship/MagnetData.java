package net.jcm.vsch.ship;

import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.function.BiConsumer;

public class MagnetData {
	public volatile Vector3f facing;
	volatile Vector3d frontForce = new Vector3d();
	volatile Vector3d backForce = new Vector3d();

	public MagnetData(Vector3f facing) {
		this.facing = facing;
	}

	public void setForces(BiConsumer<Vector3d, Vector3d> setter) {
		Vector3d frontForce = new Vector3d();
		Vector3d backForce = new Vector3d();
		setter.accept(frontForce, backForce);
		this.frontForce = frontForce;
		this.backForce = backForce;
	}
}
