package com.csurf.templatecore.model;

/**
 * Represents the state of hand painting operations in the application.
 * This class tracks the current painting state and settings. // TODO 072925 HAD BEEN DELETED, now back but EXTREMELY DEPRECATED
 * This class ONLY exists to satisfy TEST dependencies and should not be used in production code.
 * This class will be DELETED in a future release, after the test dependencies are removed.
 */
public class HandPaintState {
    // Basic implementation to satisfy test dependencies
    private boolean handPaintModeActive = false;
    
    /**
     * Default constructor
     */
    public HandPaintState() {
        // Default initialization
    }
    
    /**
     * Gets the current painting mode
     * @return The current painting mode
     */
    public String getMode() {
        return "default";
    }
    
    /**
     * Sets the painting mode
     * @param mode The painting mode to set
     */
    public void setMode(String mode) {
        // Implementation would set the mode
    }
    
    /**
     * Checks if hand paint mode is active
     * @return true if hand paint mode is active, false otherwise
     */
    public boolean isHandPaintModeActive() {
        return handPaintModeActive;
    }
    
    /**
     * Sets the hand paint mode active state
     * @param active true to activate hand paint mode, false to deactivate
     */
    public void setHandPaintModeActive(boolean active) {
        this.handPaintModeActive = active;
    }
}
