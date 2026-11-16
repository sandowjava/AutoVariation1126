package com.csurf.templatecore.storage.serialization;

import com.csurf.templatecore.model.PaletteWShapeNames;
import com.csurf.templatecore.model.ShapeColorPair;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Jackson serializer for PaletteWShapeNames objects.
 * 
 * Serializes a PaletteWShapeNames by writing:
 * - paletteName: The name of the palette
 * - pairs: List of ShapeColorPair objects (delegated to ShapeColorPairSerializer)
 * 
 * The serializer delegates to ShapeColorPairSerializer for each pair,
 * ensuring consistent serialization of color data using Ostwald coordinates.
 */
public class PaletteWShapeNamesSerializer extends StdSerializer<PaletteWShapeNames> {
    
    public PaletteWShapeNamesSerializer() {
        super(PaletteWShapeNames.class);
    }
    
    @Override
    public void serialize(PaletteWShapeNames palette, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        
        gen.writeStartObject();
        
        // Write palette name
        if (palette.getPaletteName() != null) {
            gen.writeStringField("paletteName", palette.getPaletteName());
        } else {
            gen.writeNullField("paletteName");
        }
        
        // Write pairs array
        gen.writeArrayFieldStart("pairs");
        for (ShapeColorPair pair : palette.getPairs()) {
            provider.defaultSerializeValue(pair, gen);
        }
        gen.writeEndArray();
        
        gen.writeEndObject();
    }
}
