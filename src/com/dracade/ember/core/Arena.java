package com.dracade.ember.core;

import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.world.Location;

import java.util.UUID;

public abstract class Arena implements Identifiable {

    // Arena properties.
    private String name;
    private UUID uniqueId;
    private Spawnpoint spawn;

    // Serialization purposes only.
    private Class type;

    /**
     * Creates a new Arena instance.
     *
     * @param name the name of the arena.
     * @param spawn the spawn location of the arena.
     */
    protected Arena(String name, Spawnpoint spawn) {
        this.name = name;
        this.uniqueId = UUID.randomUUID();
        this.spawn = spawn;

        this.type = this.getClass();
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
    public final void setSpawn(Spawnpoint spawn) {
        this.spawn = spawn;
    }

    /**
     * @return the spawn location of the arena.
     */
    public final Spawnpoint getSpawn() {
        return this.spawn;
    }

    @Override
    public final UUID getUniqueId() {
        return this.uniqueId;
    }

}
