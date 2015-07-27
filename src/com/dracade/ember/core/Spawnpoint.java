/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Dracade
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.dracade.ember.core;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class Spawnpoint {

    // Spawnpoint properties
    private Vector3d position;
    private Vector3d rotation;
    private World world;

    /**
     * Creates a spawnpoint at the given position and world where a player can spawn it with the given rotation.
     *
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
     * Set's the position of the Spawnpoint.
     *
     * @param position as a Vector3d object.
     */
    public void setPosition(Vector3d position) {
        this.position = position;
    }

    /**
     * Get's the position of the Spawnpoint.
     *
     * @return the position as a Vector3d object.
     */
    public Vector3d getPosition() {
        return this.position;
    }

    /**
     * Set's the rotation of the Spawnpoint.
     *
     * @param rotation as a Vector3d object.
     */
    public void setRotation(Vector3d rotation) {
        this.rotation = rotation;
    }

    /**
     * Get's the rotation of the Spawnpoint.
     *
     * @return the rotation as a Vector3d object.
     */
    public Vector3d getRotation() {
        return this.rotation;
    }

    /**
     * Set the world that will hold the Spawnpoint.
     *
     * @param world the World object.
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * Get the world that holds the Spawnpoint.
     *
     * @return the World object.
     */
    public World getWorld() {
        return this.world;
    }

    /**
     * Get the Spawnpoint in the form of a Location object.
     * This ignores rotation, and only returns the
     * {@link org.spongepowered.api.world.extent.Extent}
     * and Position.
     *
     * @return The Spawnpoint as a Location object.
     */
    public Location getLocation() {
        return new Location(world, position);
    }

}
