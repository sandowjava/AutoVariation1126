package com.csurf.templatecore.controller;

import com.csurf.templatecore.model.SelectionGroup;

/**
 * Callback interface for hue rotation color transformation operations.
 * 
 * This interface defines the contract for receiving results from hue rotation
 * service methods. It separates business logic (in TemplatePaletteShift) from event
 * handling (in the caller), following the callback pattern for dependency injection.
 * 
 * The service method performs the hue rotation transformation and returns the result
 * via this callback, allowing the caller to handle success/error cases and fire
 * appropriate events without the service depending on event infrastructure.
 * 
 * This callback is used for both standard hue rotation and equal-value hue rotation
 * operations.
 * 
 * @see TemplatePaletteShift#recolorByHueRotateShift
 * @see TemplatePaletteShift#recolorByEVHueRotateShift
 */
public interface HueRotateCallback {
    
    /**
     * Called when a hue rotation operation completes successfully.
     * 
     * The caller receives the SelectionGroup that was modified, allowing it to:
     * - Fire appropriate events to listeners
     * - Update UI components
     * - Perform additional operations based on the result
     * 
     * @param result The SelectionGroup that was transformed. Contains the DrawObjs
     *               with their new colors after the hue rotation.
     */
    void onRotateComplete(SelectionGroup result);
    
    /**
     * Called when a hue rotation operation fails.
     * 
     * The caller receives an error message describing what went wrong, allowing it to:
     * - Log the error
     * - Display error message to user
     * - Perform cleanup or recovery operations
     * 
     * @param errorMessage A descriptive message explaining why the rotation failed.
     *                     May include details about which DrawObj failed and why.
     */
    void onRotateError(String errorMessage);
}
