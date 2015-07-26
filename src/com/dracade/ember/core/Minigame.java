package com.dracade.ember.core;

import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.scheduler.Task;

import java.util.UUID;

public interface Minigame extends Runnable {

    /**
     * Gets the delay that the task was scheduled to run after. A delay of 0
     * represents that the task started immediately.
     *
     * @return The delay (offset) in either milliseconds or ticks (ticks are
     *         exclusive to synchronous tasks)
     */
    long getDelay();

    /**
     * Gets the interval for repeating tasks. An interval of 0 represents that
     * the task does not repeat.
     *
     * @return The interval (period) in either milliseconds or ticks (ticks are
     *         exclusive to synchronous tasks)
     */
    long getInterval();

}
