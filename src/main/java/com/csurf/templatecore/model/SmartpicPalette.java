package com.csurf.templatecore.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

import java.util.*;

/**
 * Represents a color palette in the SmartPic system. This class is not used in this project.
 * Contains a collection of ColorPtOstDot instances and their mappings to ColorPipeUnion IDs.
 * This class is deprecated in this project.
 */
public class SmartpicPalette {
    private final List<ColorPtOstDot> colors;
    private final Map<String, ColorPtOstDot> colorMappings;

    /**
     * Creates a new empty SmartpicPalette.
     */
    public SmartpicPalette() {
        this.colors = new ArrayList<>();
        this.colorMappings = new HashMap<>();
    }

    /**
     * Creates a new SmartpicPalette with the specified colors and mappings.
     *
     * @param colors List of ColorPtOstDot instances
     * @param colorMappings Map of DrawObj IDs to their colors
     */
    public SmartpicPalette(List<ColorPtOstDot> colors, Map<String, ColorPtOstDot> colorMappings) {
        this.colors = new ArrayList<>(colors);
        this.colorMappings = new HashMap<>(colorMappings);
    }

    /**
     * Gets the list of colors in this palette.
     *
     * @return Unmodifiable list of colors
     */
    public List<ColorPtOstDot> getColors() {
        return Collections.unmodifiableList(colors);
    }

    /**
     * Gets the color mappings for DrawObj IDs.
     *
     * @return Unmodifiable map of color mappings
     */
    public Map<String, ColorPtOstDot> getColorMappings() {
        return Collections.unmodifiableMap(colorMappings);
    }

    /**
     * Gets the color for a specific DrawObj ID.
     *
     * @param drawObjId The ID to get the color for
     * @return The color for the ID, or null if not found
     */
    public ColorPtOstDot getColorForId(String drawObjId) {
        return colorMappings.get(drawObjId);
    }

    /**
     * Adds a color mapping for a DrawObj.
     *
     * @param drawObjId The ID to map the color to
     * @param color The color to map
     */
    public void addColorMapping(String drawObjId, ColorPtOstDot color) {
        if (!colors.contains(color)) {
            colors.add(color);
        }
        colorMappings.put(drawObjId, color);
    }
}
