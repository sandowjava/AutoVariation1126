package com.csurf.templatecore.storage.serialization;

import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Custom serializer for OstHueColumnStep objects.
 * Serializes all OstHueColumnStep information for completeness and verification,
 * ensuring that deserialization can reconstruct the object correctly.
 * 
 * The serializer outputs the following fields:
 * - stdHueNum: The standard (code-facing) hue number (0-35)
 * - hueNum: The hue number (same as stdHueNum, for backward compatibility)
 * - column: The column number (1-12)
 * - step: The step within the column (1 ≤ step ≤ column)
 * - fillOrderNum: The fill order number (1-78)
 * - seriesW: The white series number
 * - seriesB: The black series number
 */
public class OstHueColumnStepSerializer extends JsonSerializer<OstHueColumnStep> {
    
    @Override
    public void serialize(OstHueColumnStep ohcs, JsonGenerator gen, SerializerProvider provider) 
            throws IOException {
        gen.writeStartObject();
        
        // Primary field: stdHueNum (required by deserializer)
        gen.writeNumberField("stdHueNum", ohcs.getStdCodeHueNum());
        
        // Backward compatibility fields (root-level)
        gen.writeNumberField("hueNum", ohcs.getStdCodeHueNum());
        gen.writeNumberField("column", ohcs.getColumn());
        gen.writeNumberField("step", ohcs.getStep());
        gen.writeNumberField("fillOrderNum", ohcs.getFillOrderNum());
        gen.writeNumberField("seriesW", ohcs.getSeriesW());
        gen.writeNumberField("seriesB", ohcs.getSeriesB());
        gen.writeNumberField("drawOrderNum", ohcs.getDrawOrderNum());
        
        gen.writeEndObject();
    }
}
