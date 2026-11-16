package com.csurf.templatecore.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.storage.serialization.PaletteWShapeNamesDeserializer;
import com.csurf.templatecore.storage.serialization.PaletteWShapeNamesSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a palette as a list of shape-color pairs with explicit indexing.
 * This class uses a hybrid pairs approach where each pair contains:
 * - shapeName: The name of the shape (from DrawObj.getName())
 * - color: The ColorPtOstDot assigned to this shape
 * - paletteIndex: The 1-based position in the palette (for display)
 * 
 * This class is a pure data model with no dependencies on DrawObjRoster or DrawObj.
 * It can be serialized/deserialized and used in different modules (e.g., mosaic module)
 * without bringing template-specific dependencies.
 * 
 * The PaletteWShapeNamesFactory is responsible for creating instances from DrawObjRoster
 * in the HandColor module. Deserialization creates instances directly from JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = PaletteWShapeNamesSerializer.class)
@JsonDeserialize(using = PaletteWShapeNamesDeserializer.class)
public class PaletteWShapeNames implements IPalette {
    private List<ShapeColorPair> pairs;
    private String paletteName;
    
    /**
     * Default constructor for Jackson deserialization.
     * Required by Jackson to deserialize JSON into PaletteWShapeNames objects.
     */
    public PaletteWShapeNames() {
        this.pairs = Collections.unmodifiableList(new ArrayList<>());
        this.paletteName = null;
    }
    
    /**
     * Constructor that creates a PaletteWShapeNames from a list of ShapeColorPair instances.
     * This is the primary constructor used by both the Factory and deserialization.
     * 
     * @param pairs The list of ShapeColorPair instances
     * @param paletteName The name of this palette
     * @throws IllegalArgumentException if pairs is null or contains invalid data
     */
    public PaletteWShapeNames(List<ShapeColorPair> pairs, String paletteName) {
        if (pairs == null) {
            throw new IllegalArgumentException("Pairs list cannot be null");
        }
        
        // Validate pairs
        validatePairs(pairs);
        
        // Store as unmodifiable copy
        this.pairs = Collections.unmodifiableList(new ArrayList<>(pairs));
        this.paletteName = paletteName;
    }
    
    /**
     * Validates the pairs list for consistency and correctness.
     * Checks:
     * - No null pairs
     * - paletteIndex values are sequential (1, 2, 3, ...)
     * - No duplicate shape names
     * 
     * @param pairs The list of pairs to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validatePairs(List<ShapeColorPair> pairs) {
        if (pairs.isEmpty()) {
            return;  // Empty palette is valid
        }
        
        // Validate no null pairs
        for (int i = 0; i < pairs.size(); i++) {
            if (pairs.get(i) == null) {
                throw new IllegalArgumentException("Pair at index " + i + " is null");
            }
        }
        
        // Validate paletteIndex sequence (1, 2, 3, ...)
        for (int i = 0; i < pairs.size(); i++) {
            int expectedIndex = i + 1;
            int actualIndex = pairs.get(i).getPaletteIndex();
            if (actualIndex != expectedIndex) {
                throw new IllegalArgumentException(
                    "Invalid paletteIndex sequence at position " + i + 
                    ": expected " + expectedIndex + 
                    " but got " + actualIndex
                );
            }
        }
        
        // Validate no duplicate shape names
        java.util.Set<String> names = new java.util.HashSet<>();
        for (ShapeColorPair pair : pairs) {
            if (!names.add(pair.getShapeName())) {
                throw new IllegalArgumentException(
                    "Duplicate shape name: " + pair.getShapeName()
                );
            }
        }
    }
    
    /**
     * Returns the number of shape-color pairs in this palette.
     * Implements IPalette.getSize().
     * 
     * @return The number of pairs
     */
    @Override
    public int getSize() {
        return pairs.size();
    }
    
    /**
     * Returns the color at the specified 0-based index.
     * Implements IPalette.getColorAt().
     * 
     * @param index The 0-based index
     * @return The color at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public ColorPtOstDot getColorAt(int index) {
        if (index < 0 || index >= pairs.size()) {
            throw new IndexOutOfBoundsException(
                "Index: " + index + ", Size: " + pairs.size()
            );
        }
        return pairs.get(index).getColor();
    }
    
    /**
     * Returns the shape name at the specified 0-based index.
     * This method is specific to PaletteWShapeNames and not part of IPalette.
     * 
     * @param index The 0-based index
     * @return The shape name at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public String getShapeNameAt(int index) {
        if (index < 0 || index >= pairs.size()) {
            throw new IndexOutOfBoundsException(
                "Index: " + index + ", Size: " + pairs.size()
            );
        }
        return pairs.get(index).getShapeName();
    }
    
    /**
     * Returns the palette index (1-based) at the specified 0-based list position.
     * This provides native support for indexed display without computation.
     * 
     * @param index The 0-based list position
     * @return The 1-based palette index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public int getPaletteIndexAt(int index) {
        if (index < 0 || index >= pairs.size()) {
            throw new IndexOutOfBoundsException(
                "Index: " + index + ", Size: " + pairs.size()
            );
        }
        return pairs.get(index).getPaletteIndex();
    }
    
    /**
     * Returns the color for the specified shape name.
     * This method performs an O(n) lookup through the pairs list.
     * 
     * @param shapeName The name of the shape to find
     * @return The color for the shape, or null if not found
     */
    public ColorPtOstDot getColorForShapeName(String shapeName) {
        if (shapeName == null) {
            return null;
        }
        
        for (ShapeColorPair pair : pairs) {
            if (pair.getShapeName().equals(shapeName)) {
                return pair.getColor();
            }
        }
        
        return null;
    }
    
    /**
     * Returns the palette name.
     * Implements IPalette.getPaletteName().
     * 
     * @return The palette name, or null if not set
     */
    @Override
    @JsonProperty("paletteName")
    public String getPaletteName() {
        return paletteName;
    }
    
    /**
     * Sets the palette name.
     * Implements IPalette.setPaletteName().
     * 
     * @param paletteName The name to assign to this palette
     */
    @Override
    public void setPaletteName(String paletteName) {
        this.paletteName = paletteName;
    }
    
    /**
     * Returns an unmodifiable view of the pairs list.
     * Used by serializers and for testing.
     * 
     * @return Unmodifiable list of ShapeColorPair instances
     */
    @JsonProperty("pairs")
    public List<ShapeColorPair> getPairs() {
        return pairs;
    }
    
    /**
     * Sets the pairs list. Used by Jackson during deserialization.
     * 
     * @param pairs The list of pairs to set
     */
    @JsonProperty("pairs")
    public void setPairs(List<ShapeColorPair> pairs) {
        if (pairs == null) {
            this.pairs = Collections.unmodifiableList(new ArrayList<>());
        } else {
            validatePairs(pairs);
            this.pairs = Collections.unmodifiableList(new ArrayList<>(pairs));
        }
    }
    
    @Override
    public String toString() {
        return "PaletteWShapeNames{" +
                "paletteName='" + paletteName + '\'' +
                ", pairs=" + pairs.size() +
                '}';
    }
}
