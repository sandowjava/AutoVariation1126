package com.csurf.templatecore.sorting.strategies;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.sorting.DrawObjSortStrategy;

/**
 * Sorts DrawObjs alphabetically from Z to A (descending).
 * 
 * Uses case-insensitive comparison for more intuitive ordering.
 */
public class AlphabeticalZAStrategy implements DrawObjSortStrategy {
    @Override
    public int compare(DrawObj obj1, DrawObj obj2) {
        return obj2.getName().compareToIgnoreCase(obj1.getName());
    }
    
    @Override
    public String getDisplayLabel() {
        return "Z-A";
    }
}
