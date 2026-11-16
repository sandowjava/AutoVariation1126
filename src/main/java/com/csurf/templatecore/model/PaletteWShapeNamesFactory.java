package com.csurf.templatecore.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Factory for creating PaletteWShapeNames instances in the HandColor module.
 * 
 * This factory encapsulates the logic for extracting palette data from DrawObjRoster,
 * keeping DrawObjRoster and DrawObj dependencies isolated from the PaletteWShapeNames class.
 * This enables PaletteWShapeNames to be used as a pure data model in other modules (e.g., mosaic)
 * without bringing template-specific dependencies.
 * 
 * The factory is responsible for:
 * - Extracting shape names from DrawObj instances
 * - Extracting colors from DrawObj instances
 * - Creating ShapeColorPair instances with proper paletteIndex values
 * - Validating the extracted data
 * - Creating the final PaletteWShapeNames instance
 */
public class PaletteWShapeNamesFactory {
    private static final Logger logger = Logger.getLogger(PaletteWShapeNamesFactory.class.getName());
    
    /**
     * Creates a PaletteWShapeNames instance from a DrawObjRoster.
     * Extracts the current state of all shapes and their colors from the roster.
     * 
     * @param drawObjRoster The DrawObjRoster to extract palette data from
     * @return A new PaletteWShapeNames instance containing the current palette state
     * @throws IllegalArgumentException if drawObjRoster is null or contains invalid data
     */
    public PaletteWShapeNames createFromDrawObjRoster(DrawObjRoster drawObjRoster) {
        if (drawObjRoster == null) {
            throw new IllegalArgumentException("DrawObjRoster cannot be null");
        }
        
        List<DrawObj> drawObjs = drawObjRoster.getTemplateDrawObjs();
        if (drawObjs == null || drawObjs.isEmpty()) {
            logger.fine("DrawObjRoster is empty, creating empty palette");
            return new PaletteWShapeNames(new ArrayList<>(), null);
        }
        
        List<ShapeColorPair> pairs = new ArrayList<>();
        
        for (int i = 0; i < drawObjs.size(); i++) {
            DrawObj drawObj = drawObjs.get(i);
            
            // Validate DrawObj
            if (drawObj == null) {
                throw new IllegalArgumentException(
                    "DrawObj at index " + i + " is null"
                );
            }
            
            String shapeName = drawObj.getName();
            if (shapeName == null || shapeName.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "DrawObj at index " + i + " has no name or empty name"
                );
            }
            
            ColorPtOstDot color = drawObj.getCurrentColor();
            if (color == null) {
                throw new IllegalArgumentException(
                    "DrawObj '" + shapeName + "' at index " + i + " has no color"
                );
            }
            
            // Create pair with 1-based paletteIndex
            int paletteIndex = i + 1;
            ShapeColorPair pair = new ShapeColorPair(shapeName, color, paletteIndex);
            pairs.add(pair);
            
            logger.finest("Created pair for shape '" + shapeName + 
                         "' at paletteIndex " + paletteIndex);
        }
        
        logger.fine("Created PaletteWShapeNames with " + pairs.size() + " pairs");
        return new PaletteWShapeNames(pairs, null);
    }
    
    /**
     * Creates a PaletteWShapeNames instance from a list of ShapeColorPair instances.
     * This is a simple wrapper around the PaletteWShapeNames constructor.
     * Used by deserialization and other creation paths that already have pairs.
     * 
     * @param pairs The list of ShapeColorPair instances
     * @param paletteName The name of the palette
     * @return A new PaletteWShapeNames instance
     * @throws IllegalArgumentException if pairs is null or invalid
     */
    public PaletteWShapeNames createFromPairs(List<ShapeColorPair> pairs, String paletteName) {
        if (pairs == null) {
            throw new IllegalArgumentException("Pairs list cannot be null");
        }
        
        return new PaletteWShapeNames(pairs, paletteName);
    }
}
