package com.csurf.templatecore.storage.serialization;

import com.csurf.templatecore.model.PaletteWShapeNames;
import com.csurf.templatecore.model.ShapeColorPair;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson deserializer for PaletteWShapeNames objects.
 * 
 * Implements fail-fast validation strategy:
 * - Validates paletteName field (can be null)
 * - Validates pairs array is present and not null
 * - Delegates to ShapeColorPairDeserializer for each pair
 * - Validates paletteIndex sequence (1, 2, 3, ...)
 * - Validates no duplicate shape names
 * 
 * All validation is performed during deserialization. Invalid data results in
 * an exception with a clear error message, making data corruption obvious.
 * 
 * This deserializer has no dependencies on DrawObjRoster or DrawObj, enabling
 * use in different modules (e.g., mosaic module).
 */
public class PaletteWShapeNamesDeserializer extends StdDeserializer<PaletteWShapeNames> {
    
    public PaletteWShapeNamesDeserializer() {
        super(PaletteWShapeNames.class);
    }
    
    @Override
    public PaletteWShapeNames deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        
        JsonNode node = p.getCodec().readTree(p);
        
        // Read palette name (can be null)
        String paletteName = null;
        if (node.has("paletteName") && !node.get("paletteName").isNull()) {
            paletteName = node.get("paletteName").asText();
        }
        
        // Validate and read pairs array
        if (!node.has("pairs")) {
            throw new IOException("Missing required field 'pairs' in PaletteWShapeNames JSON");
        }
        
        JsonNode pairsNode = node.get("pairs");
        if (!pairsNode.isArray()) {
            throw new IOException("Field 'pairs' must be an array, got: " + pairsNode.getNodeType());
        }
        
        // Deserialize each pair
        List<ShapeColorPair> pairs = new ArrayList<>();
        ShapeColorPairDeserializer pairDeserializer = new ShapeColorPairDeserializer();
        
        for (int i = 0; i < pairsNode.size(); i++) {
            JsonNode pairNode = pairsNode.get(i);
            try {
                JsonParser pairParser = pairNode.traverse(p.getCodec());
                pairParser.nextToken();
                ShapeColorPair pair = pairDeserializer.deserialize(pairParser, ctxt);
                pairs.add(pair);
            } catch (IOException e) {
                throw new IOException("Error deserializing pair at index " + i + ": " + e.getMessage(), e);
            }
        }
        
        // Create and return the palette
        // The PaletteWShapeNames constructor will validate:
        // - paletteIndex sequence (1, 2, 3, ...)
        // - no duplicate shape names
        // - no null pairs
        return new PaletteWShapeNames(pairs, paletteName);
    }
}
