package com.csurf.templatecore.storage.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.awt.geom.Rectangle2D;
import java.io.IOException;

public class Rectangle2DSerializer extends StdSerializer<Rectangle2D> {
    
    public Rectangle2DSerializer() {
        super(Rectangle2D.class);
    }
    
    @Override
    public void serialize(Rectangle2D value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("x", value.getX());
        gen.writeNumberField("y", value.getY());
        gen.writeNumberField("width", value.getWidth());
        gen.writeNumberField("height", value.getHeight());
        gen.writeEndObject();
    }
}
