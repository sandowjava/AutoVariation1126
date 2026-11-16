package com.csurf.core;

/**
 * Base interface for all subunit mediators in the HMVC architecture.
 */
public interface SubunitMediator {
    /**
     * Initializes the subunit
     */
    void initialize();
    
    /**
     * Gets the visual component for this subunit if it has one
     */
    javax.swing.JComponent getVisualComponent();
    
    /**
     * Handles messages from the top-level mediator
     * @param message The message to handle
     * @return Response data or null
     */
    Object handleMessage(Message message);
}
