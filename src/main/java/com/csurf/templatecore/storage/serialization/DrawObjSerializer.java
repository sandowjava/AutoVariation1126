package com.csurf.templatecore.storage.serialization;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.DrawObj;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class DrawObjSerializer extends StdSerializer<DrawObj> {
    
    public DrawObjSerializer() {
        super(DrawObj.class);
    }
    
    @Override
    public void serialize(DrawObj value, JsonGenerator gen, SerializerProvider provider) 
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField("name", value.getName());
        gen.writeStringField("id", value.getPipeUnityId());
        
        // Serialize the uniqueId field
        if (value.getUniqueId() != null) {
            gen.writeStringField("uniqueId", value.getUniqueId().toString());
            System.out.println("Serializing DrawObj with UUID: " + value.getUniqueId());
        }
        
        gen.writeObjectField("path", value.getPath());
        
        // Serialize the color's hue and fill order numbers directly
        ColorPtOstDot color = value.getCurrentColor();
        gen.writeNumberField("hueNum", color.getStdCodeHueNum());
        gen.writeNumberField("fillOrderNum", color.getFillOrderNum());
        
        gen.writeEndObject();
    }
}
