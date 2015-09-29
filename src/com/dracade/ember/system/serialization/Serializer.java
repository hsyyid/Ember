package com.dracade.ember.system.serialization;

import com.dracade.ember.system.serialization.adapters.ClassAdapter;
import com.dracade.ember.system.serialization.adapters.WorldAdapter;
import com.google.gson.*;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO:
 *
 *  This class should be refactored.
 *  There is no need for the register functions which makes me
 *  wonder what this class is even doing...
 *
 */

/**
 * A class to handle object serialization.
 */
public final class Serializer {

    // Serializer singleton
    private static Serializer instance;

    /**
     * @return Serializer singleton instance
     */
    public static Serializer instance() {
        return (Serializer.instance == null) ? (Serializer.instance = new Serializer()) : instance;
    }

    /**
     * Serializer constructor.
     */
    private Serializer() {

    }

}
