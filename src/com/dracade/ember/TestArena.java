package com.dracade.ember;

import com.dracade.ember.core.Arena;
import com.dracade.ember.core.Spawnpoint;
import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import org.spongepowered.api.Game;
import org.spongepowered.api.world.World;

public class TestArena extends Arena {
    /**
     * Creates a new Arena instance.
     */
    private String apples = "bannanas";

    protected TestArena(World world) {
        super("TestArena", new Spawnpoint(Vector3d.ZERO, Vector3d.ZERO, world));
    }
}
