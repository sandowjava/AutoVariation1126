package com.csurf.templatecore.interfaces;

import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.templatecore.view.TemplateWorkFrame;

/**
 * Interface for components that can receive hue updates.
 * Typically implemented by HueRelativesFrame.
 */
public interface IHueReceiver {
    
    /**
     * Receives a hue update and updates the display accordingly.
     * 
     * @param hueNum The HueNumGUI instance containing the new hue value
     */
    void receiveHueUpdate(HueNumGUI hueNum);
    
    /**
     * Checks if this receiver is currently designated to receive hue updates.
     * Only one receiver should be designated at a time.
     * 
     * @return true if this receiver is designated, false otherwise
     */
    boolean isDesignatedReceiver();
    
    /**
     * Sets whether this receiver is designated to receive hue updates.
     * When set to true, all other receivers should be set to false.
     * 
     * @param designated true to designate this receiver, false otherwise
     */
    void setDesignatedReceiver(boolean designated);
    
    /**
     * Gets the parent TemplateWorkFrame for coordination between receivers.
     * 
     * @return The parent TemplateWorkFrame
     */
    TemplateWorkFrame getParentTemplateWorkFrame();
}
