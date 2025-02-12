package net.jcm.vsch.ship;

import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.function.BiConsumer;

public class MagnetData {
	public static final ForceCalculator EMPTY_FORCE = (s, a, b) -> {};
	public volatile Vector3f facing;
	public volatile boolean isGenerator = false;
	public volatile ForceCalculator forceCalculator = EMPTY_FORCE;

	public MagnetData(Vector3f facing) {
		this.facing = facing;
	}

	@FunctionalInterface
	public interface ForceCalculator {
		void calc(PhysShipImpl physShip, Vector3d force1, Vector3d force2);
	}
}
