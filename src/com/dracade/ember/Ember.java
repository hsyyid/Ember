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
import com.dracade.ember.core.adapters.ClassAdapter;
import com.dracade.ember.core.adapters.WorldAdapter;
import com.dracade.ember.core.events.minigame.MinigameStartedEvent;
import com.dracade.ember.core.events.minigame.MinigameStoppedEvent;
import com.dracade.ember.core.events.minigame.MinigameStoppingEvent;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerJoinEvent;
import org.spongepowered.api.event.entity.player.PlayerQuitEvent;
import org.spongepowered.api.event.state.InitializationEvent;
import org.spongepowered.api.event.state.ServerStartedEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.scheduler.Task;
import org.spongepowered.api.world.World;

import java.util.*;

@Plugin(name = "Ember", id = "EMBER", version = "1.0.0")
public class Ember {

    // Injects;
    @Inject Game game;

    // Singleton
    private static Ember instance;
    private static Serializer serializer = new Serializer();

    // A map to store arenas and their games.
    private static HashMap<Arena, Task> arenas;

    /**
     * This method is called on server initialization.
     */
    @Subscribe
    private void onInitialization(InitializationEvent event) {
        Ember.instance = this;
        Ember.serializer = new Serializer();
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
                Ember.instance.game.getEventManager().register(Ember.instance, minigame);
            }

            // Call an event so that the plugins know a minigame has started.
            Ember.instance.game.getEventManager().post(new MinigameStartedEvent(minigame));

            // We then create a new Task.
            Task task = Ember.instance.game.getScheduler().getTaskBuilder()
                    .name(arena.getName())
                    .delay(minigame.delay())
                    .interval(minigame.interval())
                    .execute(minigame)
                    .submit(Ember.instance);

            // We then register the task to be executed on the specified arena.
            Ember.arenas.put(arena, task);
        }

        // We return true to acknowlegde the task has been registered successfully.
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
            Ember.instance.game.getEventManager().unregister(task.getRunnable());

            // Remove the arena
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

        //A collection to store the arena temporarily.
        Collection<Arena> arenas = new ArrayList<Arena>();

        //Loop through all the arenas.
        for (Arena a : Ember.arenas.keySet()) {

            //If the arena's world equals the passed world.
            if (a.getSpawn().getWorld().equals(world)) {
                //add it to the collection.
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
        //A collection to store the minigames temporarily.
        Collection<Minigame> minigames = new ArrayList<Minigame>();

        //Loop through the active minigames.
        for (Arena a : Ember.getArenas(world)) {
            //Get the minigame for the arena
            Optional<Minigame> minigame = Ember.getMinigame(a);
                
            //If the minigame is present
            if(minigame.isPresent()) {
                //If so then return the minigame.
                minigames.add(minigame.get());
            }

        }
        return ImmutableList.copyOf(minigames);
    }


    /**
     * A class to handle object serialization.
     */
    public static final class Serializer {

        // A set to store all of our type adapters.
        private HashMap<Class<?>, Class<? extends TypeAdapter>> adapters;

        /**
         * Serializer constructor.
         */
        protected Serializer() {
            this.adapters = new HashMap<Class<?>, Class<? extends TypeAdapter>>();

            this.register(Class.class, ClassAdapter.class);
            this.register(World.class, WorldAdapter.class);
        }

        /**
         * Register a TypeAdapter.
         *
         * @param object the object to adapt toward.
         * @param adapter the adapters class.
         * @param <T>
         * @return true if the adapters was registered successfully.
         */
        public <T extends TypeAdapter> boolean register(Class<?> object, Class<T> adapter) {
            if (!this.adapters.containsKey(object)) {
                this.adapters.put(object, adapter);
            }
            return this.adapters.containsKey(object);
        }

        /**
         * Unregister a TypeAdapter.
         *
         * @param <T>
         * @return true if the adapters was unregistered successfully.
         */
        public <T extends TypeAdapter> boolean unregister(Class<?> object) {
            if (!this.adapters.containsKey(object)) {
                this.adapters.remove(object);
            }
            return this.adapters.containsKey(object);
        }

        /**
         * Get the GsonBuilder.
         *
         * @return GsonBuilder instance.
         * @throws IllegalAccessException if a registered adapter is not accessible.
         * @throws InstantiationException if a registered adapter cannot be instantiated.
         */
        public Gson gson() throws InstantiationException, IllegalAccessException {
            return this.gson(null);
        }

        /**
         * Get the GsonBuilder.
         *
         * @param builder your custom GsonBuilder instance.
         * @return GsonBuilder instance.
         * @throws IllegalAccessException if a registered adapter is not accessible.
         * @throws InstantiationException if a registered adapter cannot be instantiated.
         */
        public Gson gson(GsonBuilder builder) throws IllegalAccessException, InstantiationException {
            GsonBuilder b = (builder != null) ? builder : new GsonBuilder();

            for (Map.Entry<Class<?>, Class<? extends TypeAdapter>> entry : this.adapters.entrySet()) {
                b.registerTypeHierarchyAdapter(entry.getKey(), entry.getValue().newInstance());
            }
            return b.create();
        }

        /**
         * Get the object type from the JSON.
         *
         * @param json the json data.
         * @return the relevant class.
         * @throws ClassNotFoundException if the class wasn't found.
         */
        public Class getType(String json) throws ClassNotFoundException {
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(json);
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("type")) {
                    element = obj.get("type");
                    if (element.isJsonObject()) {
                        obj = element.getAsJsonObject();
                        if (obj.has("class")) {
                            JsonPrimitive value = obj.get("class").getAsJsonPrimitive();
                            if (value.isString()) {
                                return Class.forName(value.getAsString());
                            }
                        }
                    }
                }
            }
            throw new ClassNotFoundException("The JSON data provided doesn't contain a valid \"type\" object.");
        }

        /**
         * Attempts to get and load the correct object from the JSON data.
         *
         * @param json the json data.
         * @return The object.
         * @throws ClassNotFoundException if the object type wasn't found.
         * @throws IllegalAccessException if a registered adapter is not accessible.
         * @throws InstantiationException if a registered adapter cannot be instantiated.
         */
        public Object getAndLoad(String json) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
            return this.gson().fromJson(json, this.getType(json));
        }

    }

}
