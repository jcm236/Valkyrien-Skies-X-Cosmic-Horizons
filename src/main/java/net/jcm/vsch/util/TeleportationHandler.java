package net.jcm.vsch.util;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.ducks.IEntityDuck;
import net.jcm.vsch.mixin.accessor.ServerShipObjectWorldAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.*;
import org.valkyrienskies.core.apigame.ShipTeleportData;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.apigame.physics.PhysicsEntityServer;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.DimensionIdProvider;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.*;

import static net.jcm.vsch.util.ShipUtils.transformFromId;

public class TeleportationHandler {

    private static final Logger logger = LogManager.getLogger(VSCHMod.MODID);

    private static final Map<Long, Set<Integer>> shipIdToConstraints = ((ServerShipObjectWorldAccessor) VSGameUtilsKt.getShipObjectWorld(ValkyrienSkiesMod.getCurrentServer())).getShipIdToConstraints();
    private static final Map<Integer, VSConstraint> constraintIdToConstraint = ((ServerShipObjectWorldAccessor) VSGameUtilsKt.getShipObjectWorld(ValkyrienSkiesMod.getCurrentServer())).getConstraints();

    private final Map<Long, Vector3d> shipToPos = new HashMap<>();
    private final Map<Entity, Vector3d> entityToPos = new HashMap<>();
    private final Map<Entity,Entity> passengerVehicleMap = new HashMap<>();
    private final ServerShipWorldCore shipWorld;
    private double greatestOffset;
    private final ServerLevel newDim;
    private final ServerLevel originalDim;
    private final boolean isReturning;

    public TeleportationHandler(ServerLevel newDim, ServerLevel originalDim, boolean isReturning) {
        shipWorld = VSGameUtilsKt.getShipObjectWorld(newDim);
        this.newDim = newDim;
        this.originalDim = originalDim;
        //Look for the lowest ship when escaping, in order to not collide with the planet.
        //Look for the highest ship when reentering, in order to not collide with the atmosphere.
        this.isReturning = isReturning;
    }

    public void handleTeleport(Ship ship, Vector3d newPos) {
        collectShips(ship, newPos);
        handleTeleport();
    }


    private void collectConnected(Long currentPhysObject, Vector3dc origin, Vector3d newPos) {
        if (currentPhysObject == null) return;
        if (shipToPos.containsKey(currentPhysObject)) return;
        Set<Integer> constraints = shipIdToConstraints.get(currentPhysObject);
        Vector3dc pos = transformFromId(currentPhysObject, shipWorld).getPositionInWorld();


        //TODO if planet collision position matters for reentry angle THIS SHOULD BE FIXED!! Currently a fix is not needed.
        double offset = pos.get(1) - origin.get(1);

        offset *= isReturning ? 1 : -1;
        if (offset > greatestOffset) greatestOffset = offset;

        shipToPos.put(currentPhysObject, pos.sub(origin, new Vector3d()).add(newPos, new Vector3d()));
        if (constraints != null) {
            constraints.iterator().forEachRemaining(id -> {
                VSConstraint constraint = constraintIdToConstraint.get(id);
                collectConnected(constraint.getShipId0(), origin, newPos);
                collectConnected(constraint.getShipId1(), origin, newPos);
            });
        }
    }

    private void collectShips(Ship ship, Vector3d newPos) {
        Vector3dc origin = ship.getTransform().getPositionInWorld();
        collectConnected(ship.getId(), origin, newPos);
        collectNearby(origin, newPos);
    }

    private void collectNearby(Vector3dc origin, Vector3d newPos) {
        Map<Long, Vector3d> newShipToPos = new HashMap<>();
        shipToPos.keySet().forEach(id -> {
            if (shipToPos.containsKey(id)) return;
            QueryableShipData<LoadedServerShip> loadedShips = shipWorld.getLoadedShips();
            Ship ship = loadedShips.getById(id);
            if (ship == null) return;
            loadedShips.getIntersecting(VectorConversionsMCKt.toJOML(VectorConversionsMCKt.toMinecraft(ship.getWorldAABB()).inflate(10)))
                    .forEach(intersecting -> newShipToPos.put(intersecting.getId(), intersecting.getTransform().getPositionInWorld().sub(origin, new Vector3d()).add(newPos, new Vector3d())));
        });
        shipToPos.putAll(newShipToPos);
    }

    private void handleTeleport() {
        greatestOffset *= isReturning ? -1 : 1;
        shipToPos.forEach((id, newPos) -> {
            collectEntities(id, newPos);
            handleShipTeleport(id, newPos);
            teleportEntities();
        });

    }

    private void collectEntities(Long id, Vector3d shipNewPos) {
        ServerShip ship = shipWorld.getLoadedShips().getById(id);
        if (ship == null) return;
        originalDim.getAllEntities().forEach(entity -> {
            if (collectEntity(entity, id,ship, shipNewPos) && entity.isVehicle()) {
                entity.getPassengers().forEach(e -> {
                    passengerVehicleMap.put(e, entity);
                    // Dismout the passengers to prevent problems
                    e.dismountTo(e.getX(), e.getY(), e.getZ());
                    collectEntity(entity, id,ship, shipNewPos);
                });
            }
        });
    }

    private boolean collectEntity(Entity entity,Long id , Ship ship, Vector3d shipNewPos) {
        if (entityToPos.containsKey(entity)) return false;
        //Entities mounted to shipyard entities
        if (VSGameUtilsKt.getShipMountedTo(entity) == ship) {
            collectWorldEntity(entity, ship, shipNewPos);
            return true;
        }
        //Shipyard entities
        if (VSGameUtilsKt.getShipObjectManagingPos(originalDim, VectorConversionsMCKt.toJOML(entity.position())) == ship) {
            //Shipyard coordinates are maintained between dimensions
            entityToPos.put(entity, VectorConversionsMCKt.toJOML(entity.position()));
            return true;
        }
        //Entities dragged by ships
        if (ShipUtils.isEntityDraggedByShip(id, entity)) {
            collectWorldEntity(entity, ship, shipNewPos);
            return true;
        }
        //Entities in range
        AABB inflatedAABB = VectorConversionsMCKt.toMinecraft(VSCHUtils.transformToAABBd(ship.getPrevTickTransform(), ship.getShipAABB())).inflate(10);
        if(entity.getBoundingBox().intersects(inflatedAABB)) {
            collectWorldEntity(entity, ship, shipNewPos);
            return true;
        }
        return false;
    }

    private void collectWorldEntity(Entity entity, Ship ship, Vector3d shipNewPos) {
        Vector3d newPos = shipNewPos.add(VectorConversionsMCKt.toJOML(entity.position()).sub(ship.getTransform().getPositionInWorld()), new Vector3d());
        entityToPos.put(entity, newPos.add(0,greatestOffset,0));

    }


    private void teleportEntities() {
        entityToPos.forEach((entity, newPos) -> {
            if (entity instanceof ServerPlayer player)
                player.teleportTo(newDim, newPos.x, newPos.y, newPos.z, entity.getYRot(), entity.getXRot());
            else {
                entity.teleportTo(newDim, newPos.x, newPos.y, newPos.z, null, entity.getYRot(), entity.getXRot());
            }
        });
        passengerVehicleMap.forEach((e1,e2) -> {
            ifPlayerThenNormal(e1).startRiding(ifPlayerThenNormal(e2));
        });
    }

    private Entity ifPlayerThenNormal(Entity entity) {
        if (entity instanceof ServerPlayer) return entity;
        else return ((IEntityDuck) entity).vsch$getNewEntity();
    }

    private void handleShipTeleport(Long id, Vector3d newPos) {
        String vsDimName = ((DimensionIdProvider) newDim).getDimensionId();

        ServerShip ship = shipWorld.getLoadedShips().getById(id);
        if (ship == null) {
            PhysicsEntityServer physEntity = ((ShipObjectServerWorld) shipWorld).getLoadedPhysicsEntities().get(id);
            if (physEntity == null) {
                logger.warn("[VSCH]: Failed to teleport physics object with id " + id + "! It's neither a Ship nor a Physics Entity!");
                return;
            }
            ShipTeleportData teleportData = new ShipTeleportDataImpl(newPos.add(0,greatestOffset,0), physEntity.getShipTransform().getShipToWorldRotation(), new Vector3d(), new Vector3d(), vsDimName, null);
            shipWorld.teleportPhysicsEntity(physEntity, teleportData);
        }
        ShipTeleportData teleportData = new ShipTeleportDataImpl(newPos.add(0,greatestOffset,0), ship.getTransform().getShipToWorldRotation(), new Vector3d(), new Vector3d(), vsDimName, null);
        shipWorld.teleportShip(ship, teleportData);

    }

}
