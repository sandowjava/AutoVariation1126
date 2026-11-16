package com.csurf.huepattern.controller;

import com.csurf.huepattern.view.DialComponent;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Mouse event handler for the circular dial in APDisplayPanel.
 * 
 * This controller:
 * 1. Listens to mouse events on APDisplayPanel
 * 2. Delegates to DialComponent for hue calculation
 * 3. Passes the calculated hue to AnchorPatternEditController for state updates
 * 
 * Similar to CircDialMouseController from the prototype, but simpler:
 * - No sector/hit marker complexity
 * - Works directly with 36 hues (not 144 hit points)
 * - Delegates state updates to AnchorPatternEditController (not direct view updates)
 */
public class DialMouseController extends MouseAdapter {
    
    private final DialComponent dialComponent;
    private final AnchorPatternEditController editController;
    
    /**
     * Creates a new DialMouseController.
     * 
     * @param dialComponent The DialComponent for hue calculation
     * @param editController The AnchorPatternEditController for state updates
     */
    public DialMouseController(DialComponent dialComponent, 
                               AnchorPatternEditController editController) {
        if (dialComponent == null) {
            throw new IllegalArgumentException("DialComponent cannot be null");
        }
        if (editController == null) {
            throw new IllegalArgumentException("AnchorPatternEditController cannot be null");
        }
        
        this.dialComponent = dialComponent;
        this.editController = editController;
    }
    
    /**
     * Handles mouse click events on the dial.
     * 
     * @param e The mouse event
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        handleMouseEvent(e);
    }
    
    /**
     * Handles mouse pressed events on the dial.
     * 
     * @param e The mouse event
     */
    @Override
    public void mousePressed(MouseEvent e) {
        handleMouseEvent(e);
    }
    
    /**
     * Internal method to handle mouse events.
     * 
     * @param e The mouse event
     */
    private void handleMouseEvent(MouseEvent e) {
        if (e == null) {
            return;
        }
        
        // Get the click point
        Point clickPoint = e.getPoint();
        
        // Delegate to DialComponent for hue calculation
        int hueNum = dialComponent.handleMouseClick(clickPoint);
        
        // Validate hue number is in valid range (1-36)
        if (hueNum >= 1 && hueNum <= 36) {
            // Pass to edit controller for state updates
            editController.handleDialInput(hueNum);
        }
    }
}
