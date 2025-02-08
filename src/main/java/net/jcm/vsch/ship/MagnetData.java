package net.jcm.vsch.ship;

import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.function.BiConsumer;

public class MagnetData {
	public volatile Vector3f facing;
	public volatile BiConsumer<Vector3d, Vector3d> forceCalculator;

	public MagnetData(Vector3f facing) {
		this.facing = facing;
		this.forceCalculator = (a, b) -> {};
	}
}
