package com.dracade.ember.core.events.minigame;

import com.dracade.ember.core.Minigame;
import com.dracade.ember.core.events.MinigameEvent;

public class MinigameStoppedEvent extends MinigameEvent {

    /**
     * Minigame constructor.
     *
     * @param minigame the running minigame.
     */
    public MinigameStoppedEvent(Minigame minigame) {
        super(minigame);
    }

}
