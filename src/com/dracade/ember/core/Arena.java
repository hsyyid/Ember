package com.dracade.ember.core;

import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.world.Location;

import java.util.UUID;

public abstract class Arena implements Identifiable {

    // Arena properties.
    private String name;
    private UUID uniqueId;
    private Location spawn;

    // Serialization purposes only.
    private Class<?> type;

    /**
     * Creates a new Arena instance.
     *
     * @param name the name of the arena.
     * @param spawn the spawn location of the arena.
     */
    protected Arena(String name, Location spawn) {
        this.name = name;
        this.spawn = spawn;
        this.type = this.getClass();
        this.uniqueId = UUID.randomUUID();
    }

    /**
     * @return the name of the arena.
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Set the spawn of the arena.
     *
     * @param spawn location to be used as the spawn point.
     */
    public final void setSpawn(Location spawn) {
        this.spawn = spawn;
    }

    /**
     * @return the spawn location of the arena.
     */
    public final Location getSpawn() {
        return this.spawn;
    }

    @Override
    public final UUID getUniqueId() {
        return this.uniqueId;
    }

}
