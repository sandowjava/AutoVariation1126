package com.csurf.templatecore.sorting;

import com.csurf.templatecore.model.DrawObj;

/**
 * Strategy interface for sorting DrawObjs.
 * 
 * Each implementation defines a different sorting algorithm.
 * Strategies must implement a total ordering (transitive, consistent).
 * 
 * RESPONSIBILITIES:
 * - Compare two DrawObjs according to a specific criterion
 * - Return a consistent total ordering
 * - Be stateless and reusable
 * 
 * NON-RESPONSIBILITIES:
 * - Rendering or visual display
 * - Filtering or excluding DrawObjs
 * - Modifying DrawObj state
 * - Extracting complex derived properties (use DrawObj getters if needed)
 */
public interface DrawObjSortStrategy {
    /**
     * Compares two DrawObjs according to this strategy's sorting rules.
     * 
     * @param obj1 first DrawObj to compare
     * @param obj2 second DrawObj to compare
     * @return negative if obj1 < obj2, zero if equal, positive if obj1 > obj2
     */
    int compare(DrawObj obj1, DrawObj obj2);
    
    /**
     * Gets a display label for this strategy (for UI menus).
     * 
     * @return human-readable label for this sorting strategy
     */
    String getDisplayLabel();
}
