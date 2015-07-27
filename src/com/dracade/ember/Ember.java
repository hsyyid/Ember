package com.dracade.ember;

import com.dracade.ember.core.Arena;
import com.dracade.ember.core.Minigame;
import com.dracade.ember.core.adapters.ClassAdapter;
import com.dracade.ember.core.events.minigame.MinigameStartedEvent;
import com.dracade.ember.core.events.minigame.MinigameStoppedEvent;
import com.dracade.ember.core.events.minigame.MinigameStoppingEvent;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.inject.Inject;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.state.InitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.scheduler.Task;

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
            MinigameStoppingEvent stoppingEvent = new MinigameStoppingEvent((Minigame) task.getRunnable());
            Ember.instance.game.getEventManager().post(stoppingEvent);

            if (stoppingEvent.isCancelled())
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
    public ImmutableList<Arena> getArenas() {
        return ImmutableList.copyOf(Ember.arenas.keySet());
    }

    /**
     * Get all of the currently running minigames.
     *
     * @return An ImmutableList of Minigame objects.
     */
    public ImmutableList<Minigame> getMinigames() {
        List<Minigame> games = new ArrayList<Minigame>();
        for (Task t : Ember.arenas.values()) {
            games.add((Minigame) t.getRunnable());
        }
        return ImmutableList.copyOf(games);
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
                b.registerTypeAdapter(entry.getKey(), entry.getValue().newInstance());
            }
            return b.create();
        }

    }

}
