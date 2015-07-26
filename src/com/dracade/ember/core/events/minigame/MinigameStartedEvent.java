package com.dracade.ember.core.events.minigame;

import com.dracade.ember.core.Minigame;
import com.dracade.ember.core.events.MinigameEvent;

public class MinigameStartedEvent extends MinigameEvent {

    /**
     * Minigame constructor.
     *
     * @param minigame the running minigame.
     */
    public MinigameStartedEvent(Minigame minigame) {
        super(minigame);
    }

}
