package com.dracade.ember.core.adapters;

import com.dracade.ember.Ember;
import com.dracade.ember.core.Spawnpoint;
import com.google.common.base.Optional;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;
import org.spongepowered.api.Game;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.util.UUID;

public class WorldAdapter extends TypeAdapter<World> {

    @Inject private Game game;

    @Override
    public void write(JsonWriter out, World value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.beginObject();
            out.name("name");
            out.value(value.getName());

            out.name("uniqueId");
            out.value(value.getUniqueId().toString());
        out.endObject();

    }

    @Override
    public World read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            return null;
        }

        in.beginObject();
            in.nextName();
            in.nextName();

        Optional<World> optional = this.game.getServer().getWorld(UUID.fromString(in.nextString()));

        return optional.isPresent()? optional.get() : null;
    }

}
