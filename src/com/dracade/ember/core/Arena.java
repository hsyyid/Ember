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

import org.spongepowered.api.util.Identifiable;

import java.util.UUID;

public abstract class Arena implements Identifiable {

    // Arena properties.
    private String name;
    private UUID uniqueId;
    private Spawnpoint spawn;

    // Serialization purposes only.
    private Class type;

    /**
     * Constructs a new Arena instance.
     *
     * @param name the name of the arena.
     * @param spawn the spawn location of the arena.
     */
    public Arena(String name, Spawnpoint spawn) {
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
