package com.csurf.ostwald.domainmonotri.view;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.MonotriPage;

/**
 * Interface defining the contract for MonoTri display components
 */
public interface IMonoTriDisplay {
    /**
     * Gets the currently selected color
     * @return The currently selected ColorPtOstDot
     */
    ColorPtOstDot getSelectedColor();
    
    /**
     * Sets the current MonotriPage
     * @param page The MonotriPage to display
     */
    void setMonoTriPage(MonotriPage page);
    
    /**
     * Gets the current MonotriPage
     * @return The current MonotriPage
     */
    MonotriPage getMonoTriPage();
    
    /**
     * Sets the standard hue number
     * @param hue The standard hue number (1-12)
     */
    void setCurrHueNumCode(int hue);
    
    /**
     * Gets the standard hue number
     * @return The current standard hue number
     */
    int getCurrHueNumCode();
}
