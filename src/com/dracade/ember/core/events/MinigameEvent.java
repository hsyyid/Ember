package com.dracade.ember.core.events;

import com.dracade.ember.core.Minigame;
import org.spongepowered.api.event.AbstractEvent;

public abstract class MinigameEvent extends AbstractEvent {

    private Minigame minigame;

    /**
     * Minigame constructor.
     *
     * @param minigame the running minigame.
     */
    protected MinigameEvent(Minigame minigame) {
        this.minigame = minigame;
    }

    /**
     * Get the minigame.
     *
     * @return the running minigame.
     */
    public Minigame getMinigame() {
        return this.minigame;
    }

}
