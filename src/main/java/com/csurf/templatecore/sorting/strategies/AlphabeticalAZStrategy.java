package com.csurf.templatecore.sorting.strategies;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.sorting.DrawObjSortStrategy;

/**
 * Sorts DrawObjs alphabetically from A to Z (ascending).
 * 
 * Uses case-insensitive comparison for more intuitive ordering.
 */
public class AlphabeticalAZStrategy implements DrawObjSortStrategy {
    @Override
    public int compare(DrawObj obj1, DrawObj obj2) {
        return obj1.getName().compareToIgnoreCase(obj2.getName());
    }
    
    @Override
    public String getDisplayLabel() {
        return "A-Z";
    }
}
