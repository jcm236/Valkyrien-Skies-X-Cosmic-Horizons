package net.jcm.vsch.ship;

import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.function.BiConsumer;

public class MagnetData {
	public static final BiConsumer<Vector3d, Vector3d> EMPTY_FORCE = (a, b) -> {};
	public volatile Vector3f facing;
	public volatile BiConsumer<Vector3d, Vector3d> forceCalculator = EMPTY_FORCE;

	public MagnetData(Vector3f facing) {
		this.facing = facing;
	}
}
