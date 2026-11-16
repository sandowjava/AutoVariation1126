package com.csurf.templatecore.storage.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.IOException;

/**
 * Custom serializer for GeneralPath objects.
 * Converts the path into a sequence of path commands and coordinates.
 */
public class GeneralPathSerializer extends JsonSerializer<GeneralPath> {
    
    @Override
    public void serialize(GeneralPath path, JsonGenerator gen, SerializerProvider provider) 
            throws IOException {
        gen.writeStartObject();
        gen.writeArrayFieldStart("segments");
        
        float[] coords = new float[6];
        PathIterator pi = path.getPathIterator(null);
        
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            gen.writeStartObject();
            
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    gen.writeStringField("type", "MOVETO");
                    gen.writeNumberField("x", coords[0]);
                    gen.writeNumberField("y", coords[1]);
                    break;
                    
                case PathIterator.SEG_LINETO:
                    gen.writeStringField("type", "LINETO");
                    gen.writeNumberField("x", coords[0]);
                    gen.writeNumberField("y", coords[1]);
                    break;
                    
                case PathIterator.SEG_QUADTO:
                    gen.writeStringField("type", "QUADTO");
                    gen.writeNumberField("x1", coords[0]);
                    gen.writeNumberField("y1", coords[1]);
                    gen.writeNumberField("x2", coords[2]);
                    gen.writeNumberField("y2", coords[3]);
                    break;
                    
                case PathIterator.SEG_CUBICTO:
                    gen.writeStringField("type", "CUBICTO");
                    gen.writeNumberField("x1", coords[0]);
                    gen.writeNumberField("y1", coords[1]);
                    gen.writeNumberField("x2", coords[2]);
                    gen.writeNumberField("y2", coords[3]);
                    gen.writeNumberField("x3", coords[4]);
                    gen.writeNumberField("y3", coords[5]);
                    break;
                    
                case PathIterator.SEG_CLOSE:
                    gen.writeStringField("type", "CLOSE");
                    break;
            }
            
            gen.writeEndObject();
            pi.next();
        }
        
        gen.writeEndArray();
        gen.writeEndObject();
    }
}
