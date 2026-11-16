package com.csurf.artintellig.model;

import com.csurf.artintellig.domainanchorpattern.model.IQAnchorPattern;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

/**
 * Strategy interface for different artistic coloring approaches.
 * Implementations will define specific algorithms for generating colors
 * based on RelationMatrix data and AnchorPattern configurations.
 */
public interface ArtisticGoalStrategy {
    /**
     * Generate a color for a specific shape based on its IQOnePipe data
     * and the current AnchorPattern.
     *
     * @param smarts The IQOnePipe containing relationship data for the shape
     * @param anchorPattern The current AnchorPattern guiding color selection
     * @return The generated color for this shape
     */
    ColorPtOstDot generateColor(IQOnePipe smarts, IQAnchorPattern anchorPattern);
}
