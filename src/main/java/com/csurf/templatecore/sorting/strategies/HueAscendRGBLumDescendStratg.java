package com.csurf.templatecore.sorting.strategies;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.sorting.DrawObjSortStrategy;

/**
 * Sorts DrawObjs by hue in ascending order (0-35), with secondary RGB luminance sorting (descending),
 * and tertiary saturation sorting (descending).
 * 
 * Primary sort: by internal hue number (0-35) in ascending order
 * Secondary sort: by RGB luminance (descending - highest luminance first within same hue)
 * Tertiary sort: by absolute color saturation (descending - highest saturation first within same hue and luminance)
 * 
 * DrawObjs with invalid/unset colors (hue = -1) sort last.
 * 
 * This strategy is useful for organizing selected DrawObjs by their color's hue,
 * with RGB luminance and saturation grouping within each hue block for visual prominence.
 */
public class HueAscendRGBLumDescendStratg implements DrawObjSortStrategy {
    
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
        
        // Secondary sort: by RGB luminance (descending - highest luminance first)
        double lum1 = obj1.getRGBLum3D();
        double lum2 = obj2.getRGBLum3D();
        int lumCompare = Double.compare(lum2, lum1);  // Note: lum2, lum1 for descending order
        if (lumCompare != 0) {
            return lumCompare;
        }
        
        // Tertiary sort: by saturation (descending - highest saturation first)
        // Only applies when luminances are equal
        double sat1 = obj1.getAbsColorSat();
        double sat2 = obj2.getAbsColorSat();
        return Double.compare(sat2, sat1);  // Note: sat2, sat1 for descending order
    } // end method compare

    
    @Override
    public String getDisplayLabel() {
        return "by Hue RGBLum descend";
    }
}
