package com.dracade.ember.core;

public interface Minigame extends Runnable {

    /**
     * Gets the delay that the task was scheduled to run after. A delay of 0
     * represents that the task started immediately.
     *
     * @return The delay (offset) in either milliseconds or ticks (ticks are
     *         exclusive to synchronous tasks)
     */
    long delay();

    /**
     * Gets the interval for repeating tasks. An interval of 0 represents that
     * the task does not repeat.
     *
     * @return The interval (period) in either milliseconds or ticks (ticks are
     *         exclusive to synchronous tasks)
     */
    long interval();

    /**
     * Whether or not this object is registered to the event bus.
     * This should be set to FALSE if the class specified is the
     * plugin class annotated with {@code @Plugin}.
     *
     * @return True if you wish to register events.
     */
    boolean events();

}
