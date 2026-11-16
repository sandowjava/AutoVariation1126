package com.csurf.templatecore.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.storage.serialization.ShapeColorPairDeserializer;
import com.csurf.templatecore.storage.serialization.ShapeColorPairSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents an atomic pairing of a shape name, its color, and its palette index.
 * This is an immutable data holder used by PaletteWShapeNames to represent
 * the mapping between shape names and colors with explicit indexing.
 * 
 * The paletteIndex is 1-based for human-friendly display purposes.
 */
@JsonSerialize(using = ShapeColorPairSerializer.class)
@JsonDeserialize(using = ShapeColorPairDeserializer.class)
public class ShapeColorPair {
    private final String shapeName;
    private final ColorPtOstDot color;
    private final int paletteIndex;
    
    /**
     * Constructs a new ShapeColorPair with the specified shape name, color, and palette index.
     * 
     * @param shapeName The name of the shape (from DrawObj.getName())
     * @param color The color assigned to this shape
     * @param paletteIndex The 1-based position of this pair in the palette
     * @throws IllegalArgumentException if shapeName is null or empty, color is null, or paletteIndex is invalid
     */
    public ShapeColorPair(String shapeName, ColorPtOstDot color, int paletteIndex) {
        if (shapeName == null || shapeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Shape name cannot be null or empty");
        }
        if (color == null) {
            throw new IllegalArgumentException("Color cannot be null");
        }
        if (paletteIndex < 1) {
            throw new IllegalArgumentException("Palette index must be 1-based (>= 1), got: " + paletteIndex);
        }
        
        this.shapeName = shapeName;
        this.color = color;
        this.paletteIndex = paletteIndex;
    }
    
    /**
     * Returns the name of the shape in this pair.
     * 
     * @return The shape name
     */
    @JsonProperty("shapeName")
    public String getShapeName() {
        return shapeName;
    }
    
    /**
     * Returns the color in this pair.
     * 
     * @return The color
     */
    @JsonProperty("color")
    public ColorPtOstDot getColor() {
        return color;
    }
    
    /**
     * Returns the palette index (1-based) of this pair.
     * This represents the position of this pair in the palette for display purposes.
     * 
     * @return The 1-based palette index
     */
    @JsonProperty("paletteIndex")
    public int getPaletteIndex() {
        return paletteIndex;
    }
    
    @Override
    public String toString() {
        return "ShapeColorPair{" +
                "shapeName='" + shapeName + '\'' +
                ", color=" + color +
                ", paletteIndex=" + paletteIndex +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ShapeColorPair that = (ShapeColorPair) o;
        
        if (paletteIndex != that.paletteIndex) return false;
        if (!shapeName.equals(that.shapeName)) return false;
        return color != null ? color.equals(that.color) : that.color == null;
    }
    
    @Override
    public int hashCode() {
        int result = shapeName.hashCode();
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + paletteIndex;
        return result;
    }
}
