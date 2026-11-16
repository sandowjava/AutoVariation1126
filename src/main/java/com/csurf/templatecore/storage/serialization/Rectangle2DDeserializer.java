package com.csurf.templatecore.storage.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.awt.geom.Rectangle2D;
import java.io.IOException;

public class Rectangle2DDeserializer extends JsonDeserializer<Rectangle2D> {
    
    @Override
    public Rectangle2D deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        double x = node.get("x").asDouble();
        double y = node.get("y").asDouble();
        double width = node.get("width").asDouble();
        double height = node.get("height").asDouble();
        
        return new Rectangle2D.Double(x, y, width, height);
    }
}
