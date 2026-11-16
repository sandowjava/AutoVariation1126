package com.csurf.templatecore.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

/**
 * Interface defining the contract for palette implementations.
 * Enables polymorphic treatment of different palette types (PaletteWName, PaletteWShapeNames, etc.)
 * without requiring inheritance.
 */
public interface IPalette {
    
    /**
     * Returns the number of colors in this palette.
     * 
     * @return The number of colors
     */
    int getSize();
    
    /**
     * Returns the color at the specified 0-based index.
     * 
     * @param index The 0-based index of the color to retrieve
     * @return The color at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    ColorPtOstDot getColorAt(int index);
    
    /**
     * Returns the name of this palette.
     * 
     * @return The palette name, or null if not set
     */
    String getPaletteName();
    
    /**
     * Sets the name of this palette.
     * 
     * @param name The name to assign to this palette
     */
    void setPaletteName(String name);
}
