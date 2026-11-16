package com.csurf.templatecore.storage.serialization;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Custom serializer for ColorPtOstDot objects.
 * Stores all OstHueColumnStep information for completeness and verification,
 * even though only hueNum and fillOrderNum are needed for reconstruction.
 * This provides implementation independence and helps with testing/debugging.
 */
public class ColorPtOstDotSerializer extends JsonSerializer<ColorPtOstDot> {
    
    @Override
    public void serialize(ColorPtOstDot color, JsonGenerator gen, SerializerProvider provider) 
            throws IOException {
        gen.writeStartObject();

        // Store complete OstHueColumnStep information for robustness
        gen.writeNumberField("hueNum", color.getOstHCS().getStdCodeHueNum());
        gen.writeNumberField("column", color.getOstHCS().getColumn());
        gen.writeNumberField("step", color.getOstHCS().getStep());
        gen.writeNumberField("fillOrderNum", color.getOstHCS().getFillOrderNum());
        gen.writeNumberField("seriesW", color.getOstHCS().getSeriesW());
        gen.writeNumberField("seriesB", color.getOstHCS().getSeriesB());
        
        gen.writeEndObject();
    }
}
