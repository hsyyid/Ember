package com.dracade.ember.system;

import com.dracade.ember.core.adapters.ClassAdapter;
import com.dracade.ember.core.adapters.WorldAdapter;
import com.google.gson.*;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * A class to handle object serialization.
 */
public final class Serializer {

    // Backup singleton
    private static Serializer instance;

    /**
     * @return Serializer singleton instance
     */
    public static Serializer instance() {
        return (Serializer.instance == null) ? (Serializer.instance = new Serializer()) : instance;
    }

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
        return this.getType(json).cast(this.gson().fromJson(json, this.getType(json)));
    }

}
