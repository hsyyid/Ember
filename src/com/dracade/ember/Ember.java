package com.dracade.ember;

import com.dracade.ember.core.Arena;
import com.dracade.ember.core.Minigame;
import com.dracade.ember.core.events.minigame.MinigameStartedEvent;
import com.dracade.ember.core.events.minigame.MinigameStoppedEvent;
import com.dracade.ember.core.events.minigame.MinigameStoppingEvent;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
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

    // A list to store the arenas.
    private static HashMap<Arena, Task> arenas;

    /**
     * This method is called on server initialization.
     */
    @Subscribe
    private void onInitialization(InitializationEvent event) {
        Ember.instance = this;
        Ember.arenas = new HashMap<Arena, Task>();
    }

    /**
     * Add an arena.
     *
     * @param arena the arena object.
     */
    public static void addArena(Arena arena) {
        if (Ember.getArena(arena.getUniqueId()).isPresent()) return;
        Ember.arenas.put(arena, null);
    }

    /**
     * Get an arena by it's identifier.
     *
     * @param uniqueId the arena's unique identifer.
     * @return the arena wrapped in an Optional.
     */
    public static Optional<Arena> getArena(UUID uniqueId) {
        for (Arena a : Ember.arenas.keySet()) {
            if (a.getUniqueId().equals(uniqueId)) return Optional.of(a);
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
     * Remove an arena.
     *
     * @param arena The arena object.
     * @return true if the arena was removed successfully.
     */
    public static boolean removeArena(Arena arena) {
        return Ember.removeArena(arena.getUniqueId());
    }

    /**
     * Remove an arena.
     *
     * @param uniqueId the unique identifer of the arena.
     * @return true if the arena was removed successfully.
     */
    public static boolean removeArena(UUID uniqueId) {
        for (Iterator<Arena> iter = Ember.arenas.keySet().iterator(); iter.hasNext();) {
            Arena a = iter.next();
            if (a.getUniqueId().equals(uniqueId)) {
                Ember.arenas.remove(a);
                return true;
            }
        }
        return false;
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
     * Set the minigame to be played on an arena.
     *
     * @param arena the arena for the game to be played on.
     * @param minigame the game to be played.
     * @return true if the minigame was set successfully.
     * @throws Exception if the minigame was unable to override the currently running minigame.
     */
    public static boolean setMinigame(Arena arena, Minigame minigame) throws Exception {
        // If the arena specified isn't registered, return false.
        if (!Ember.getArena(arena.getUniqueId()).isPresent()) return false;

        // If the minigame is already registered, return false.
        if (Ember.getArena(minigame).isPresent()) return false;

        // Get the currently occupying Task.
        Task task = Ember.arenas.get(arena);

        // If the task exists...
        if (task != null) {
            // Call an event so that the plugins know a minigame is being stopped.
            MinigameStoppingEvent stoppingEvent = new MinigameStoppingEvent((Minigame) task.getRunnable());
            Ember.instance.game.getEventManager().post(stoppingEvent);

            if (stoppingEvent.isCancelled()) throw new Exception("Unable to override the currently running minigame.");

            // If the event isn't cancelled, we continue cancelling the currently
            // running minigame.
            task.cancel();

            // Call an event so that the plugins know a minigame has stopped.
            Ember.instance.game.getEventManager().post(new MinigameStoppedEvent((Minigame) task.getRunnable()));

            // Unregister the object from the EventManager.
            Ember.instance.game.getEventManager().unregister(task.getRunnable());
        }

        // If the minigame isn't null, then...
        if (minigame != null) {
            // We then register our new minigame to the EventHandler.
            Ember.instance.game.getEventManager().register(Ember.instance, minigame);

            // Call an event so that the plugins know a minigame has started.
            Ember.instance.game.getEventManager().post(new MinigameStartedEvent(minigame));

            // We then create a new Task.
            task = Ember.instance.game.getScheduler().getTaskBuilder()
                    .name(arena.getName())
                    .delay(minigame.getDelay())
                    .interval(minigame.getInterval())
                    .execute(minigame)
                    .submit(Ember.instance);

            // We then register the task to be executed on the specified arena.
            Ember.arenas.put(arena, task);
        }

        // We return true to acknowlegde the task has been registered successfully.
        return true;
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

}
