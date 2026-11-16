package com.csurf.templatecore.sorting.strategies;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.sorting.DrawObjSortStrategy;

/**
 * Sorts DrawObjs by hue in ascending order (0-35), with secondary alphabetical sorting.
 * 
 * Primary sort: by internal hue number (0-35) in ascending order
 * Secondary sort: by name A-Z (alphabetically within same hue)
 * 
 * DrawObjs with invalid/unset colors (hue = -1) sort last.
 * 
 * This strategy is useful for organizing selected DrawObjs by their color's hue,
 * with alphabetical grouping within each hue block for easy visual scanning.
 */
public class HueAscendAZAlphabetizeStratg implements DrawObjSortStrategy {
    
    @Override
    public int compare(DrawObj obj1, DrawObj obj2) {
        int hue1 = obj1.getCodeHueNum();
        int hue2 = obj2.getCodeHueNum();
        
        // Handle sentinel values: -1 (not set) sorts last
        if (hue1 == -1 && hue2 == -1) {
            return 0;  // Both unset, maintain order
        }
        if (hue1 == -1) {
            return 1;  // obj1 unset, sorts after obj2
        }
        if (hue2 == -1) {
            return -1;  // obj2 unset, sorts after obj1
        }
        
        // Primary sort: by hue number (ascending)
        int hueCompare = Integer.compare(hue1, hue2);
        if (hueCompare != 0) {
            return hueCompare;
        }
        
        // Secondary sort: by name A-Z (within same hue)
        return obj1.getName().compareToIgnoreCase(obj2.getName());
    }
    
    @Override
    public String getDisplayLabel() {
        return "by Hue > AZ";
    }
}
