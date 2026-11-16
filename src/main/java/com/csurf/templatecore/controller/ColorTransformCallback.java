package com.csurf.templatecore.controller;

import com.csurf.templatecore.model.SelectionGroup;

/**
 * Callback interface for color transformation operations.
 * 
 * This interface defines the contract for receiving results from color transformation
 * service methods. It separates business logic (in TemplatePaletteShift) from event
 * handling (in the caller), following the callback pattern for dependency injection.
 * 
 * The service method performs the color transformation and returns the result via
 * this callback, allowing the caller to handle success/error cases and fire appropriate
 * events without the service depending on event infrastructure.
 * 
 * @see TemplatePaletteShift
 */
public interface ColorTransformCallback {
    
    /**
     * Called when a color transformation operation completes successfully.
     * 
     * The caller receives the SelectionGroup that was modified, allowing it to:
     * - Fire appropriate events to listeners
     * - Update UI components
     * - Perform additional operations based on the result
     * 
     * @param result The SelectionGroup that was transformed. Contains the DrawObjs
     *               with their new colors after the transformation.
     */
    void onTransformComplete(SelectionGroup result);
    
    /**
     * Called when a color transformation operation fails.
     * 
     * The caller receives an error message describing what went wrong, allowing it to:
     * - Log the error
     * - Display error message to user
     * - Perform cleanup or recovery operations
     * 
     * @param errorMessage A descriptive message explaining why the transformation failed.
     *                     May include details about which DrawObj failed and why.
     */
    void onTransformError(String errorMessage);
}
