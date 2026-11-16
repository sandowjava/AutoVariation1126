package com.csurf.templatecore.storage.serialization;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.templatecore.model.DrawObj;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.util.UUID;

public class DrawObjDeserializer extends JsonDeserializer<DrawObj> {
    
    @Override
    public DrawObj deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        String name = node.get("name").asText();
        GeneralPath path = p.getCodec().treeToValue(node.get("path"), GeneralPath.class);
        
        // Get the hue and fill order numbers
        int hueNum = node.get("hueNum").asInt();
        int fillOrderNum = node.get("fillOrderNum").asInt();
        
        // Get the color from MonotriBook using hue and fill order
        ColorPtOstDot color = MonotriBook.getInstance()
            .getMonoTriPage(hueNum)
            .getColorByFillOrder(fillOrderNum);
        
        // Check if uniqueId is present in the JSON
        UUID uniqueId = null;
        if (node.has("uniqueId")) {
            try {
                uniqueId = UUID.fromString(node.get("uniqueId").asText());
                System.out.println("Deserializing DrawObj with preserved UUID: " + uniqueId);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid UUID format in JSON: " + node.get("uniqueId").asText());
            }
        }
        
        // Create DrawObj with the correct color and preserved UUID if available
        DrawObj drawObj;
        if (uniqueId != null) {
            drawObj = new DrawObj(name, path, color.toAWTColor(), uniqueId);
        } else {
            drawObj = new DrawObj(name, path, color.toAWTColor());
            System.out.println("Created DrawObj with new UUID: " + drawObj.getUniqueId() + " (no UUID in JSON)");
        }
        
        // Set additional properties
        if (node.has("selected")) {
            drawObj.setSelected(node.get("selected").asBoolean());
        }
        
        return drawObj;
    }
}
