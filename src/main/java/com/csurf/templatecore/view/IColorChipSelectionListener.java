package com.csurf.templatecore.view;

import com.csurf.templatecore.model.ShapeColorPair;

/**
 * Listener interface for color chip selection events in PaletteColorSourceFrame.
 * 
 * Implementations of this interface receive callbacks when a user selects a color chip
 * in the PaletteColorSourceFrame. The callback provides the complete ShapeColorPair
 * (including shapeName, color, and paletteIndex) and the index of the selected chip.
 * 
 * This interface enables decoupled integration between PaletteColorSourceFrame and
 * other components (e.g., TemplateWorkFrame) that need to respond to color chip selection.
 */
public interface IColorChipSelectionListener {
    
    /**
     * Called when a user selects a color chip in PaletteColorSourceFrame.
     * 
     * @param pair The ShapeColorPair that was selected, containing:
     *             - shapeName: The name of the shape associated with this color
     *             - color: The ColorPtOstDot color value
     *             - paletteIndex: The 1-based position in the palette
     * @param index The 0-based index of the selected chip in the palette
     */
    void onColorChipSelected(ShapeColorPair pair, int index);
}
