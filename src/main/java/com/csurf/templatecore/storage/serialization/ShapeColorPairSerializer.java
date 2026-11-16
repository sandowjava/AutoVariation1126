package com.csurf.templatecore.storage.serialization;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.csurf.templatecore.model.ShapeColorPair;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Jackson serializer for ShapeColorPair objects.
 * 
 * Serializes a ShapeColorPair by writing:
 * - shapeName: The name of the shape
 * - codeHueNum: The hue number (0-35)
 * - seriesW: The white series coordinate
 * - seriesB: The black series coordinate
 * - paletteIndex: The 1-based position in the palette
 * 
 * Uses domain-native Ostwald coordinates (seriesW, seriesB) instead of fillOrderNum
 * for robust, fail-fast deserialization. These coordinates are immutable and fundamental
 * to the Ostwald color space.
 */
public class ShapeColorPairSerializer extends StdSerializer<ShapeColorPair> {
    
    public ShapeColorPairSerializer() {
        super(ShapeColorPair.class);
    }
    
    @Override
    public void serialize(ShapeColorPair pair, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        
        gen.writeStartObject();
        
        // Write shape name
        gen.writeStringField("shapeName", pair.getShapeName());
        
        // Write color information using Ostwald coordinates
        ColorPtOstDot color = pair.getColor();
        if (color != null) {
            OstHueColumnStep ostHCS = color.getOstHCS();
            if (ostHCS != null) {
                // Write domain-native coordinates
                gen.writeNumberField("codeHueNum", ostHCS.getStdCodeHueNum());
                gen.writeNumberField("seriesW", ostHCS.getSeriesW());
                gen.writeNumberField("seriesB", ostHCS.getSeriesB());
            }
        }
        
        // Write palette index
        gen.writeNumberField("paletteIndex", pair.getPaletteIndex());
        
        gen.writeEndObject();
    }
}
