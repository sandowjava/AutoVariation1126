package com.csurf.huepattern.view;

import com.csurf.huepattern.model.AnchorPattern;
import com.csurf.huepattern.model.Spread;

/**
 * Proxy data structure for holding the current display state of an AnchorPattern.
 * This acts as a temporary editing/display buffer between the model and the view.
 * Holds the spread information needed for drawing AG rings in APDisplayPanel.
 */
public class AGRingDisplayState {
    /**
     * Simple struct to hold spread data for one AG ring.
     */
    public static class SpreadData {
        public final int cCWLimitHue;
        public final int anchorHue;
        public final int cWLimitHue;



        // Constructor for SpreadData
        public SpreadData(int cCWLimitHue, int anchorHue, int cWLimitHue) {
            this.cCWLimitHue = cCWLimitHue;
            this.anchorHue = anchorHue;
            this.cWLimitHue = cWLimitHue;
        }
    }
    
    private int apLevel;
    private SpreadData[] spreadData;
    
    /**
     * Creates an empty DisplayState.
     */
    public AGRingDisplayState() {
        this.apLevel = 0;
        this.spreadData = new SpreadData[4];
    }
    
    /**
     * Creates a DisplayState from an AnchorPattern.
     * @param anchorPattern The AnchorPattern to load
     */
    public void loadFromAnchorPattern(AnchorPattern anchorPattern) {
        this.apLevel = anchorPattern.getTheAPLevel();
        this.spreadData = new SpreadData[4];
        
        Spread[] spreads = anchorPattern.getTheSpreadArray();
        for (int i = 0; i < apLevel; i++) {
            if (spreads[i] != null) {
                spreadData[i] = new SpreadData(
                    spreads[i].getCCWLimitHueNum(),
                    spreads[i].getAnchorHueNum(),
                    spreads[i].getCWLimitHueNum()
                );
            }
        }

        int kerfloocheckLoad = 666; // 110125 debug only
    } // end method loadFromAnchorPattern
    
    /**
     * Gets the APLevel (number of defined anchor groups, 0-4).
     */
    public int getApLevel() {
        return apLevel;
    }
    
    /**
     * Gets the spread data for a specific AG (1-4).
     * Returns null if the AG is not defined.
     */
    public SpreadData getSpreadData(int whichAG) {
        if (whichAG < 1 || whichAG > 4) {
            return null;
        }
        return spreadData[whichAG - 1];
    }
    
    /**
     * Checks if a specific AG is defined.
     */
    public boolean hasSpread(int whichAG) {
        return whichAG >= 1 && whichAG <= apLevel && spreadData[whichAG - 1] != null;
    }
    
} // end class DisplayState
