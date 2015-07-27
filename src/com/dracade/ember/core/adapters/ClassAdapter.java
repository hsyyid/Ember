package com.dracade.ember.core.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class ClassAdapter extends TypeAdapter<Class> {

    @Override
    public void write(JsonWriter out, Class value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.beginObject();
            out.name("name");
            out.value(value.getSimpleName());

            out.name("class");
            out.value(value.getName());
        out.endObject();
    }

    @Override
    public Class read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            return null;
        }
        in.beginObject();
            in.nextName();
            in.nextName();

        Class c = null;
        try {
            c = Class.forName(in.nextString());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return c;
    }

}
