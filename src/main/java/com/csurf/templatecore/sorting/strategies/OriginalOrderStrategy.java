package com.csurf.templatecore.sorting.strategies;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.sorting.DrawObjSortStrategy;

/**
 * Maintains original order from Template (no sorting).
 * 
 * This strategy returns 0 for all comparisons, which preserves
 * the original order of DrawObjs as they appear in the template.
 */
public class OriginalOrderStrategy implements DrawObjSortStrategy {
    @Override
    public int compare(DrawObj obj1, DrawObj obj2) {
        return 0;  // No sorting - maintain original order
    }
    
    @Override
    public String getDisplayLabel() {
        return "original Template order";
    }
}
