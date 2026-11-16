package com.csurf.huepattern.view.agsinfo;

/**
 * Simple data structure to hold a hue range with counter-clockwise and clockwise limits.
 * Used as the filterValue when FilterType is HUE_RANGE.
 * 
 * Hue values are in the range 1-36 (user-facing representation).
 * 
 * @author User
 */
public class HueRange {
    
    /**
     * Counter-clockwise limit of the hue range (1-36)
     */
    public final int ccw;
    
    /**
     * Clockwise limit of the hue range (1-36)
     */
    public final int cw;
    
    /**
     * Creates a new HueRange with the specified limits
     * 
     * @param ccw Counter-clockwise limit (1-36)
     * @param cw Clockwise limit (1-36)
     */
    public HueRange(int ccw, int cw) {
        this.ccw = ccw;
        this.cw = cw;
    }
    
    /**
     * Returns a string representation of this hue range
     */
    @Override
    public String toString() {
        return String.format("HueRange(ccw=%d, cw=%d)", ccw, cw);
    }
    
} // end class HueRange
