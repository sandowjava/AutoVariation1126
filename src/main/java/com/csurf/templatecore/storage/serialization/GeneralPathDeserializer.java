package com.csurf.templatecore.storage.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.awt.geom.GeneralPath;
import java.io.IOException;

/**
 * Custom deserializer for GeneralPath objects.
 * Reconstructs the path from a sequence of path commands and coordinates.
 */
public class GeneralPathDeserializer extends JsonDeserializer<GeneralPath> {
    
    @Override
    public GeneralPath deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        GeneralPath path = new GeneralPath();
        
        JsonNode segments = node.get("segments");
        if (segments.isArray()) {
            for (JsonNode segment : segments) {
                String type = segment.get("type").asText();
                
                switch (type) {
                    case "MOVETO":
                        path.moveTo(
                            segment.get("x").floatValue(),
                            segment.get("y").floatValue()
                        );
                        break;
                        
                    case "LINETO":
                        path.lineTo(
                            segment.get("x").floatValue(),
                            segment.get("y").floatValue()
                        );
                        break;
                        
                    case "QUADTO":
                        path.quadTo(
                            segment.get("x1").floatValue(),
                            segment.get("y1").floatValue(),
                            segment.get("x2").floatValue(),
                            segment.get("y2").floatValue()
                        );
                        break;
                        
                    case "CUBICTO":
                        path.curveTo(
                            segment.get("x1").floatValue(),
                            segment.get("y1").floatValue(),
                            segment.get("x2").floatValue(),
                            segment.get("y2").floatValue(),
                            segment.get("x3").floatValue(),
                            segment.get("y3").floatValue()
                        );
                        break;
                        
                    case "CLOSE":
                        path.closePath();
                        break;
                }
            }
        }
        
        return path;
    }
}
