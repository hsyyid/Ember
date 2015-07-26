package com.dracade.ember.core.events.minigame;

import com.dracade.ember.core.Minigame;
import com.dracade.ember.core.events.MinigameEvent;
import org.spongepowered.api.event.AbstractEvent;
import org.spongepowered.api.event.Cancellable;

public class MinigameStoppingEvent extends MinigameEvent implements Cancellable {

    private boolean cancelled;

    /**
     * Minigame constructor.
     *
     * @param minigame the running minigame.
     */
    public MinigameStoppingEvent(Minigame minigame) {
        super(minigame);
        this.cancelled = false;
    }


    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

}
