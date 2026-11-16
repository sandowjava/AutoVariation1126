package com.csurf.templatecore.model;

import com.csurf.ostwald.domainmonotri.model.OstPosition;

import java.util.Map;

/**
 * Interface for managing position-based occupancy and selection state maps.
 * Implemented by classes that need to track which positions are occupied
 * and selected, along with their associated hues.
 * 
 * IMPORTANT: All hue numbers in maps returned by this interface are user-facing (1-36),
 * not internal (0-35). Implementers must convert internal hue numbers to user-facing
 * before storing them in maps.
 */
public interface IMapOccupiedSelected {
    
    /**
     * Updates the internal state maps based on the current object state.
     * This should be called whenever the underlying object state changes.
     * 
     * Note: Implementers must convert internal hue numbers (0-35) to user-facing (1-36)
     * before storing them in maps.
     */
    void updateToCurrObjState();
    
    /**
     * Gets the map of occupied positions to their hues.
     * @return Map where keys are positions and values are arrays of user-facing hue numbers (1-36)
     */
    Map<OstPosition, int[]> getUnmodifiableOccupiedMap();
    
    /**
     * Gets the map of selected positions to their hues.
     * @return Map where keys are positions and values are arrays of user-facing hue numbers (1-36)
     */
    Map<OstPosition, int[]> getUnmodifiableSelectedMap();
}
