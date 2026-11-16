package com.csurf.templatecore.storage.serialization;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.ostwald.domainmonotri.model.MonotriPage;
import com.csurf.templatecore.model.ShapeColorPair;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for ShapeColorPair objects.
 * 
 * Implements fail-fast validation strategy:
 * - Validates all required fields are present
 * - Validates field values are in valid ranges
 * - Looks up color using Ostwald coordinates (codeHueNum, seriesW, seriesB)
 * - Fails immediately with clear error message on any validation failure
 * 
 * Does NOT use fallback strategies or default values. If data is invalid or missing,
 * an exception is thrown immediately, making data corruption obvious.
 * 
 * This deserializer has no dependencies on DrawObjRoster or DrawObj, enabling
 * use in different modules (e.g., mosaic module).
 */
public class ShapeColorPairDeserializer extends StdDeserializer<ShapeColorPair> {
    
    public ShapeColorPairDeserializer() {
        super(ShapeColorPair.class);
    }
    
    @Override
    public ShapeColorPair deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        
        JsonNode node = p.getCodec().readTree(p);
        
        // Validate and read shapeName
        if (!node.has("shapeName")) {
            throw new IOException("Missing required field 'shapeName' in ShapeColorPair JSON");
        }
        String shapeName = node.get("shapeName").asText();
        if (shapeName == null || shapeName.trim().isEmpty()) {
            throw new IOException("Shape name cannot be null or empty");
        }
        
        // Validate and read codeHueNum
        if (!node.has("codeHueNum")) {
            throw new IOException("Missing required field 'codeHueNum' in ShapeColorPair JSON");
        }
        int codeHueNum;
        try {
            codeHueNum = node.get("codeHueNum").asInt();
        } catch (Exception e) {
            throw new IOException("Invalid codeHueNum value: " + e.getMessage());
        }
        if (codeHueNum < 0 || codeHueNum > 35) {
            throw new IOException("Invalid codeHueNum: " + codeHueNum + 
                                 ", must be in range 0-35");
        }
        
        // Validate and read seriesW
        if (!node.has("seriesW")) {
            throw new IOException("Missing required field 'seriesW' in ShapeColorPair JSON");
        }
        int seriesW;
        try {
            seriesW = node.get("seriesW").asInt();
        } catch (Exception e) {
            throw new IOException("Invalid seriesW value: " + e.getMessage());
        }
        if (seriesW < 0) {
            throw new IOException("Invalid seriesW: " + seriesW + 
                                 ", must be non-negative");
        }
        
        // Validate and read seriesB
        if (!node.has("seriesB")) {
            throw new IOException("Missing required field 'seriesB' in ShapeColorPair JSON");
        }
        int seriesB;
        try {
            seriesB = node.get("seriesB").asInt();
        } catch (Exception e) {
            throw new IOException("Invalid seriesB value: " + e.getMessage());
        }
        if (seriesB < 0) {
            throw new IOException("Invalid seriesB: " + seriesB + 
                                 ", must be non-negative");
        }
        
        // Validate and read paletteIndex
        if (!node.has("paletteIndex")) {
            throw new IOException("Missing required field 'paletteIndex' in ShapeColorPair JSON");
        }
        int paletteIndex;
        try {
            paletteIndex = node.get("paletteIndex").asInt();
        } catch (Exception e) {
            throw new IOException("Invalid paletteIndex value: " + e.getMessage());
        }
        if (paletteIndex < 1) {
            throw new IOException("Invalid paletteIndex: " + paletteIndex + 
                                 ", must be 1-based (>= 1)");
        }
        
        // Look up color using Ostwald coordinates
        MonotriBook monotriBook = MonotriBook.getInstance();
        MonotriPage page = monotriBook.getMonoTriPage(codeHueNum);
        
        if (page == null) {
            throw new IOException("Invalid hue number: " + codeHueNum);
        }
        
        ColorPtOstDot color = page.getColorByWseriesBseries(seriesW, seriesB);
        
        if (color == null) {
            throw new IOException("Color not found at coordinates: " +
                                 "codeHueNum=" + codeHueNum + 
                                 ", seriesW=" + seriesW + 
                                 ", seriesB=" + seriesB);
        }
        
        // Verify consistency: reconstructed color should have same coordinates
        int reconstructedHueNum = color.getOstHCS().getStdCodeHueNum();
        int reconstructedSeriesW = color.getOstHCS().getSeriesW();
        int reconstructedSeriesB = color.getOstHCS().getSeriesB();
        
        if (reconstructedHueNum != codeHueNum) {
            throw new IOException("Color consistency check failed: " +
                                 "expected codeHueNum=" + codeHueNum + 
                                 " but got " + reconstructedHueNum);
        }
        if (reconstructedSeriesW != seriesW) {
            throw new IOException("Color consistency check failed: " +
                                 "expected seriesW=" + seriesW + 
                                 " but got " + reconstructedSeriesW);
        }
        if (reconstructedSeriesB != seriesB) {
            throw new IOException("Color consistency check failed: " +
                                 "expected seriesB=" + seriesB + 
                                 " but got " + reconstructedSeriesB);
        }
        
        // Create and return the pair
        return new ShapeColorPair(shapeName, color, paletteIndex);
    }
}
