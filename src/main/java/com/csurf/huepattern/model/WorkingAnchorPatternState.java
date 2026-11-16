package com.csurf.huepattern.model;

import com.csurf.huepattern.view.AGRingDisplayState;

/**
 * Represents the working state of an AnchorPattern during editing.
 * This is a mutable model that holds user modifications before they are committed to the display state.
 * 
 * The working state includes:
 * - Current AG selection (1-4)
 * - Current attribute selection (CCWLim, Anchor, CWLim)
 * - Working spread data (mutable copy of hue values for each AG)
 * - Current hue selection from dial input
 * 
 * This state is separate from the display state (AGRingDisplayState) to allow users to make
 * multiple modifications before committing them via the "Update Display" button.
 */
public class WorkingAnchorPatternState {
    
    /**
     * Enumeration for the three attributes that can be modified for each AG.
     */
    public enum Attribute {
        CCW_LIMIT("CCW Limit"),
        ANCHOR("Anchor"),
        CW_LIMIT("CW Limit");
        
        private final String displayName;
        
        Attribute(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Mutable spread data for working state.
     */
    public static class WorkingSpreadData {
        public int cCWLimitHue;
        public int anchorHue;
        public int cWLimitHue;
        
        public WorkingSpreadData(int cCWLimitHue, int anchorHue, int cWLimitHue) {
            this.cCWLimitHue = cCWLimitHue;
            this.anchorHue = anchorHue;
            this.cWLimitHue = cWLimitHue;
        }
        
        /**
         * Creates a copy of this WorkingSpreadData.
         */
        public WorkingSpreadData copy() {
            return new WorkingSpreadData(this.cCWLimitHue, this.anchorHue, this.cWLimitHue);
        }
    }
    
    // Current selection state
    private int currentAG;                          // 1-4, or 0 if none selected
    private Attribute currentAttribute;             // CCWLim, Anchor, CWLim, or null if none selected
    
    // Working spread data (mutable copy)
    private WorkingSpreadData[] workingSpreadData;  // [4] - one for each AG
    
    // Current dial selection
    private int currentHueSelection;                // 1-36, or 0 if none selected
    
    /**
     * Creates a new WorkingAnchorPatternState initialized from a display state.
     * @param displayState The AGRingDisplayState to initialize from
     */
    public WorkingAnchorPatternState(AGRingDisplayState displayState) {
        this.currentAG = 0;
        this.currentAttribute = null;
        this.currentHueSelection = 0;
        this.workingSpreadData = new WorkingSpreadData[4];
        
        // Initialize working spread data from display state
        loadFromDisplayState(displayState);
    }
    
    /**
     * Loads working spread data from a display state.
     * @param displayState The AGRingDisplayState to load from
     */
    public void loadFromDisplayState(AGRingDisplayState displayState) {
        int apLevel = displayState.getApLevel();
        
        for (int i = 0; i < 4; i++) {
            AGRingDisplayState.SpreadData spreadData = displayState.getSpreadData(i + 1);
            if (spreadData != null) {
                workingSpreadData[i] = new WorkingSpreadData(
                    spreadData.cCWLimitHue,
                    spreadData.anchorHue,
                    spreadData.cWLimitHue
                );
            } else {
                workingSpreadData[i] = null;
            }
        }
    }
    
    /**
     * Sets the currently selected AG.
     * @param ag The AG number (1-4)
     */
    public void setCurrentAG(int ag) {
        if (ag < 0 || ag > 4) {
            throw new IllegalArgumentException("AG must be 0-4, got: " + ag);
        }
        this.currentAG = ag;
    }
    
    /**
     * Gets the currently selected AG.
     * @return The AG number (1-4), or 0 if none selected
     */
    public int getCurrentAG() {
        return currentAG;
    }
    
    /**
     * Sets the currently selected attribute.
     * @param attribute The attribute to select (CCWLim, Anchor, CWLim, or null)
     */
    public void setCurrentAttribute(Attribute attribute) {
        this.currentAttribute = attribute;
    }
    
    /**
     * Gets the currently selected attribute.
     * @return The attribute (CCWLim, Anchor, CWLim, or null)
     */
    public Attribute getCurrentAttribute() {
        return currentAttribute;
    }
    
    /**
     * Sets the current hue selection from dial input.
     * @param hueNum The hue number (1-36)
     */
    public void setCurrentHueSelection(int hueNum) {
        if (hueNum < 0 || hueNum > 36) {
            throw new IllegalArgumentException("Hue must be 0-36, got: " + hueNum);
        }
        this.currentHueSelection = hueNum;
    }
    
    /**
     * Gets the current hue selection.
     * @return The hue number (1-36), or 0 if none selected
     */
    public int getCurrentHueSelection() {
        return currentHueSelection;
    }
    
    /**
     * Updates the working spread data for a specific AG and attribute.
     * @param ag The AG number (1-4)
     * @param attribute The attribute to update (CCWLim, Anchor, CWLim)
     * @param hueNum The hue number (1-36)
     */
    public void updateWorkingSpread(int ag, Attribute attribute, int hueNum) {
        if (ag < 1 || ag > 4) {
            throw new IllegalArgumentException("AG must be 1-4, got: " + ag);
        }
        if (hueNum < 1 || hueNum > 36) {
            throw new IllegalArgumentException("Hue must be 1-36, got: " + hueNum);
        }
        if (attribute == null) {
            throw new IllegalArgumentException("Attribute cannot be null");
        }
        
        // Ensure working spread data exists for this AG
        if (workingSpreadData[ag - 1] == null) {
            workingSpreadData[ag - 1] = new WorkingSpreadData(1, 1, 1);
        }
        
        WorkingSpreadData spread = workingSpreadData[ag - 1];
        
        switch (attribute) {
            case CCW_LIMIT:
                spread.cCWLimitHue = hueNum;
                break;
            case ANCHOR:
                spread.anchorHue = hueNum;
                break;
            case CW_LIMIT:
                spread.cWLimitHue = hueNum;
                break;
        }
    }
    
    /**
     * Gets the working spread data for a specific AG.
     * @param ag The AG number (1-4)
     * @return The WorkingSpreadData, or null if not defined
     */
    public WorkingSpreadData getWorkingSpread(int ag) {
        if (ag < 1 || ag > 4) {
            return null;
        }
        return workingSpreadData[ag - 1];
    }
    
    /**
     * Commits the working state to the display state.
     * Copies all working spread data into the display state.
     * @param displayState The AGRingDisplayState to update
     */
    public void commitToDisplayState(AGRingDisplayState displayState) {
        // This is a placeholder - the actual implementation will be in the controller
        // The controller will create a new AnchorPattern from the working state
        // and load it into the display state via displayState.loadFromAnchorPattern()
    }
    
    /**
     * Reverts the working state to match the display state.
     * Discards all uncommitted changes.
     * @param displayState The AGRingDisplayState to revert to
     */
    public void revertFromDisplayState(AGRingDisplayState displayState) {
        loadFromDisplayState(displayState);
        this.currentAG = 0;
        this.currentAttribute = null;
        this.currentHueSelection = 0;
    }
    
    /**
     * Converts the working state to an AnchorPattern.
     * This is used when saving or applying the working state.
     * @return A new AnchorPattern created from the working state
     */
    public AnchorPattern toAnchorPattern() {
        AnchorPattern pattern = new AnchorPattern();
        
        // Count how many AGs have data
        int apLevel = 0;
        for (int i = 0; i < 4; i++) {
            if (workingSpreadData[i] != null) {
                apLevel = i + 1;
            }
        }
        
        // Create and set spreads for each AG
        for (int i = 0; i < apLevel; i++) {
            if (workingSpreadData[i] != null) {
                WorkingSpreadData data = workingSpreadData[i];
                Spread spread = new Spread(i + 1, data.anchorHue, data.cCWLimitHue, data.cWLimitHue);
                pattern.setSpread(i + 1, spread);
            }
        }
        
        // Set the AP level
        pattern.setAPLevel(apLevel);
        
        return pattern;
    }
}
