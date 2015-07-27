package com.dracade.ember.core;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class Spawnpoint {

    private Vector3d position;
    private Vector3d rotation; /** Source: {@link org.spongepowered.api.entity.Entity} line 208 */
    private World world;

    /**
     * Creates a spawnpoint at the given position and world where a player can spawn it with the given rotation.
     * @param position The position the player spawns at.
     * @param rotation The rotation the player spawns in.
     * @param world The world the players spawns in.
     */
    public Spawnpoint(Vector3d position, Vector3d rotation, World world){
        this.position = position;
        this.rotation = rotation;
        this.world = world;
    }

    /**
     * @return The Spawnpoint as a Location.
     */
    public Location getLocation() {
        return new Location(world, position);
    }


    public void setPosition(Vector3d position) { this.position = position; }
    public Vector3d getPosition() { return this.position; }

    public void setRotation(Vector3d rotation) { this.rotation = rotation; }
    public Vector3d getRotation() { return this.rotation; }

    public void setWorld(World world) { this.world = world; }
    public World getWorld() { return this.world; }

}
