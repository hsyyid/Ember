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
package com.dracade.ember;

import com.dracade.ember.core.Arena;
import com.dracade.ember.core.Minigame;
import com.dracade.ember.core.events.minigame.MinigameStartedEvent;
import com.dracade.ember.core.events.minigame.MinigameStoppedEvent;
import com.dracade.ember.core.events.minigame.MinigameStoppingEvent;
import com.dracade.ember.system.Backup;
import com.dracade.ember.system.Serializer;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.scheduler.Task;
import org.spongepowered.api.world.World;

import java.util.*;

@Plugin(name = "Ember", id = "EMBER", version = "1.0.0")
public class Ember {

    // Injects;
    private Game game;

    // Singletons
    private static Ember instance;
    private static Serializer serializer;
    private static Backup backup;

    // A map to store arenas and their games.
    private static HashMap<Arena, Task> arenas;

    /**
     * Ember constructor.
     */
    @Inject
    private Ember(Game game) {
        Ember.instance = this;
        this.game = game;
    }

    /**
     * This method is called on server initialization.
     */
    @Listener
    private void onInitialization(GameInitializationEvent event) {
        Ember.serializer = Serializer.instance();
        Ember.backup = Backup.instance("backups");

        Ember.arenas = new HashMap<Arena, Task>();
    }

    /**
     * Get the Game.
     *
     * @return Sponge's game instance.
     */
    public static Game game() {
        return Ember.instance.game;
    }

    /**
     * Get the Serializer.
     *
     * @return Ember's serialization manager.
     */
    public static Serializer serializer() {
        return Ember.serializer;
    }

    /**
     * Get the backup manager.
     *
     * @return Ember's backup manager.
     */
    public static Backup backup() {
        return Ember.backup;
    }

    /**
     * Set the minigame to be played on an arena.
     *
     * @param arena the arena for the game to be played on.
     * @param minigame the game to be played.
     * @return true if the minigame was set successfully.
     * @throws Exception if the minigame was unable to override the currently running minigame.
     */
    public static boolean register(Arena arena, Minigame minigame) throws Exception {
        if (Ember.getArena(minigame).isPresent()) return false;

        // If the arena is already registered then..
        if (Ember.getArena(arena.getUniqueId()).isPresent()) {
            Ember.unregister(arena);
        }

        // If the minigame isn't null, then...
        if (minigame != null) {
            // We then register our new minigame to the EventHandler.
            if (minigame.events()) {
                Ember.instance.game.getEventManager().registerListeners(Ember.instance, minigame);
            }

            // Call an event so that the plugins know a minigame has started.
            Ember.instance.game.getEventManager().post(new MinigameStartedEvent(minigame));

            // We then create a new Task.
            Task task = Ember.instance.game.getScheduler().createTaskBuilder()
                    .name(arena.getName())
                    .delay(minigame.delay())
                    .interval(minigame.interval())
                    .execute(minigame)
                    .submit(Ember.instance);

            // We then register the task to be executed on the specified arena.
            Ember.arenas.put(arena, task);
        }

        // We return true to acknowledge the task has been registered successfully.
        return true;
    }

    /**
     * Unregister an arena.
     *
     * @param arena The arena object.
     * @return true if the arena was removed successfully.
     * @throws Exception if the minigame was unable to override the currently running minigame.
     */
    public static boolean unregister(Arena arena) throws Exception {
        // Get the currently occupying Task.
        Task task = Ember.arenas.get(arena);

        // If the task exists...
        if (task != null) {
            // Call an event so that the plugins know a minigame is being stopped.
            boolean cancelled = Ember.instance.game.getEventManager().post(new MinigameStoppingEvent((Minigame) task.getRunnable()));

            if (cancelled)
                throw new Exception("Unable to override the currently running minigame.");

            // If the event isn't cancelled, we continue cancelling the currently
            // running minigame.
            task.cancel();

            // Call an event so that the plugins know a minigame has stopped.
            Ember.instance.game.getEventManager().post(new MinigameStoppedEvent((Minigame) task.getRunnable()));

            // Unregister the object from the EventManager.
            Ember.instance.game.getEventManager().unregisterListeners(task.getRunnable());

            // Remove the arena.
            Ember.arenas.remove(arena);

            return true;
        }
        return false;
    }

    /**
     * Get an arena by it's identifier.
     *
     * @param id the arena's unique identifer.
     * @return the arena wrapped in an Optional.
     */
    public static Optional<Arena> getArena(UUID id) {
        for (Arena a : Ember.arenas.keySet()) {
            if (a.getUniqueId().equals(id)) return Optional.of(a);
        }
        return Optional.absent();
    }

    /**
     * Get an arena by it's running minigame.
     *
     * @param minigame the currently running minigame.
     * @return the arena wrapped in an Optional.
     */
    public static Optional<Arena> getArena(Minigame minigame) {
        for (Arena a : Ember.arenas.keySet()) {
            if (Ember.arenas.get(a).getRunnable().equals(minigame)) return Optional.of(a);
        }
        return Optional.absent();
    }

    /**
     * Get the minigame of a specific arena.
     *
     * @param arena the arena you wish to get the minigame.
     * @return the minigame wrapped in an Optional.
     */
    public static Optional<Minigame> getMinigame(Arena arena) {
        return (Ember.arenas.get(arena) != null) ? Optional.of((Minigame) Ember.arenas.get(arena).getRunnable()) : Optional.<Minigame>absent();
    }

    /**
     * Get all of the currently registered arenas.
     *
     * @return An ImmutableList of Arena objects.
     */
    public static ImmutableList<Arena> getArenas() {
        return ImmutableList.copyOf(Ember.arenas.keySet());
    }

    /**
     * Gets the Arena from the passed world.
     *
     * @param world The world a Arena is on.
     * @return Arena in that world.
     */
    public static ImmutableList<Arena> getArenas(World world){
        // A collection to store the arena temporarily.
        Collection<Arena> arenas = new ArrayList<Arena>();

        // Loop through all the arenas.
        for (Arena a : Ember.arenas.keySet()) {

            //If the arena's world is equal to the specified world.
            if (a.getSpawn().getWorld().equals(world)) {
                // Add it to the collection.
                arenas.add(a);
            }
        }
        return ImmutableList.copyOf(arenas);
    }

    /**
     * Get all of the currently running minigames.
     *
     * @return An ImmutableList of Minigame objects.
     */
    public static ImmutableList<Minigame> getMinigames() {
        List<Minigame> games = new ArrayList<Minigame>();
        for (Task t : Ember.arenas.values()) {
            games.add((Minigame) t.getRunnable());
        }
        return ImmutableList.copyOf(games);
    }

    /**
     * Gets the minigames that are being played on the provided world.
     *
     * @param world The world that one or more minigames are being played on.
     * @return An ImmutableList containing the minigames for that world.
     */
    public static ImmutableList<Minigame> getMinigames(World world){
        // A collection to store the minigames temporarily.
        Collection<Minigame> minigames = new ArrayList<Minigame>();

        // Loop through the active minigames.
        for (Arena a : Ember.getArenas(world)) {
            // Get the minigame for the arena.
            Optional<Minigame> minigame = Ember.getMinigame(a);

            // If the minigame exists...
            if(minigame.isPresent()) {
                // Then return the minigame.
                minigames.add(minigame.get());
            }
        }
        return ImmutableList.copyOf(minigames);
    }

}
