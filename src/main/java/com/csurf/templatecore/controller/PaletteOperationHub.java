package com.csurf.templatecore.controller;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.templatecore.events.IPositionalPaletteEventListener;
import com.csurf.templatecore.events.PositionalPaletteEvent;
import com.csurf.templatecore.model.*;
import com.csurf.templatecore.view.TemplateWorkFrame;

import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central orchestrator for all palette recoloring operations.
 * 
 * This class manages the coordination between UI controllers and the TemplatePaletteShift
 * service. It accepts recoloring requests, creates callbacks, delegates to the service,
 * and fires events to notify listeners of changes.
 * 
 * Responsibilities:
 * - Accept recoloring requests from controllers
 * - Create and manage callbacks for each operation
 * - Delegate to TemplatePaletteShift service
 * - Fire events to notify listeners
 * - Handle errors and exceptions
 * - Manage listener registration/deregistration
 * 
 * @see TemplatePaletteShift
 * @see DrawObjRoster
 * @see IPositionalPaletteEventListener
 */
public class PaletteOperationHub {
    private static final Logger logger = Logger.getLogger(PaletteOperationHub.class.getName());
    
    // Dependencies
    private final DrawObjRoster drawObjRoster;
    private final TemplatePaletteShift templatePaletteShift;
    private final TemplateWorkFrame templateWorkFrame;
    
    // Palette bookmarks for UNDO/REDO; null until initPaletteBookmarks() succeeds
    private PaletteBookmarkList bookmarkList;
    
    // Note: TemplateWorkFrame serves as the event dispatcher
    // Events are fired directly to templateWorkFrame via firePositionalPaletteEventPublic()
    
    /**
     * Creates a new PaletteOperationHub with the specified dependencies.
     * 
     * @param drawObjRoster The DrawObjRoster that manages the collection of DrawObjs
     * @param templatePaletteShift The TemplatePaletteShift service for color transformations
     * @param templateWorkFrame The TemplateWorkFrame that displays the template
     * 
     * @throws IllegalArgumentException if any parameter is null
     */
    public PaletteOperationHub(DrawObjRoster drawObjRoster,
                               TemplatePaletteShift templatePaletteShift,
                               TemplateWorkFrame templateWorkFrame) {
        // Validate dependencies
        if (drawObjRoster == null) {
            throw new IllegalArgumentException("DrawObjRoster cannot be null");
        }
        if (templatePaletteShift == null) {
            throw new IllegalArgumentException("TemplatePaletteShift cannot be null");
        }
        if (templateWorkFrame == null) {
            throw new IllegalArgumentException("TemplateWorkFrame cannot be null");
        }
        
        this.drawObjRoster = drawObjRoster;
        this.templatePaletteShift = templatePaletteShift;
        this.templateWorkFrame = templateWorkFrame;
        
        logger.fine("PaletteOperationHub initialized");
    }
    
    // ========== DRAG SHIFT OPERATION ==========
    
    /**
     * Shifts the colors of all selected DrawObjs by changing their position
     * in the Ostwald color system (W-series and B-series coordinates).
     * 
     * This method:
     * 1. Gets selected DrawObjs from the roster
     * 2. Creates a SelectionGroup
     * 3. Creates a callback to handle the result
     * 4. Delegates to TemplatePaletteShift.recolorByDragShift()
     * 5. Fires event to notify listeners
     * 
     * The event system handles all UI updates.
     * 
     * @param shiftSpecify The specification for the shift operation
     *                     (W-series and B-series shifts)
     * 
     * @throws IllegalArgumentException if shiftSpecify is null
     * @throws IllegalStateException if no DrawObjs are selected
     * 
     * @see ShiftSpecify
     * @see ColorTransformCallback
     */
    public void shiftSelectedForDragShift(ShiftSpecify shiftSpecify) {
        // Validate parameters
        if (shiftSpecify == null) {
            throw new IllegalArgumentException("ShiftSpecify cannot be null");
        }
        
        // Get selected DrawObjs
        Set<DrawObj> selectedDrawObjs = drawObjRoster.getSelectedDrawObjs();
        SelectionGroup selectionGroup = new SelectionGroup(selectedDrawObjs);
        
        // Skip if no objects are selected
        if (selectionGroup.getSelectedDrawObjsSet().isEmpty()) {
            logger.fine("No DrawObjs selected for drag shift operation");
            return;
        }
        
        // Create callback to fire event after drag shift completes
        ColorTransformCallback callback = new ColorTransformCallback() {
            @Override
            public void onTransformComplete(SelectionGroup result) {
                logger.fine("Drag shift completed for " + result.getSelectedDrawObjsSet().size() + " objects");
                
                // Fire event to notify listeners of color change
                fireEvent(new PositionalPaletteEvent(
                    drawObjRoster,
                    PositionalPaletteEvent.EventType.COLOR_CHANGE,
                    drawObjRoster
                ));
            }
            
            @Override
            public void onTransformError(String errorMessage) {
                logger.warning("Drag shift failed: " + errorMessage);
            }
        };
        
        // Delegate to TemplatePaletteShift to handle the actual color transformation
        templatePaletteShift.recolorByDragShift(selectionGroup, shiftSpecify, drawObjRoster, callback);
    }
    
    // ========== HUE ROTATION OPERATION ==========
    
    /**
     * Rotates the hue of all selected DrawObjs while keeping their position
     * in the hue column (fill order) the same.
     * 
     * This method:
     * 1. Gets selected DrawObjs from the roster
     * 2. Creates a SelectionGroup
     * 3. Creates a callback to handle the result
     * 4. Delegates to TemplatePaletteShift.recolorByHueRotateShift()
     * 5. Fires event to notify listeners
     * 
     * The event system handles all UI updates.
     * 
     * @param someRawShiftDist The hue shift distance
     *                         (positive for clockwise, negative for counter-clockwise)
     * 
     * @throws IllegalStateException if no DrawObjs are selected
     * 
     * @see HueRotateCallback
     */
    public void shiftSelectedForHueRotation(int someRawShiftDist) {
        // Get the selected DrawObjs
        Set<DrawObj> selectedDrawObjs = drawObjRoster.getSelectedDrawObjs();
        SelectionGroup selectionGroup = new SelectionGroup(selectedDrawObjs);
        
        // Skip if no objects are selected
        if (selectionGroup.getSelectedDrawObjsSet().isEmpty()) {
            logger.fine("No DrawObjs selected for hue rotation operation");
            return;
        }
        
        // Create a ShiftSpecify object to encapsulate the hue shift distance
        ShiftSpecify shiftSpecify = new ShiftSpecify();
        shiftSpecify.setHueShiftDistance(someRawShiftDist);
        
        // Create callback to fire event after hue rotation completes
        HueRotateCallback callback = new HueRotateCallback() {
            @Override
            public void onRotateComplete(SelectionGroup result) {
                logger.fine("Hue rotation completed for " + result.getSelectedDrawObjsSet().size() + " objects");
                
                // Fire event to notify listeners of color change
                fireEvent(new PositionalPaletteEvent(
                    drawObjRoster,
                    PositionalPaletteEvent.EventType.COLOR_CHANGE,
                    drawObjRoster
                ));
            }
            
            @Override
            public void onRotateError(String errorMessage) {
                logger.warning("Hue rotation failed: " + errorMessage);
            }
        };
        
        // Delegate to TemplatePaletteShift to handle the actual color transformation
        templatePaletteShift.recolorByHueRotateShift(selectionGroup, shiftSpecify, drawObjRoster, callback);
    }

    // ========== SUBSET-TO-HUE ASSIGNMENT OPERATION ==========

    /**
     * Recolors all DrawObjs in an arbitrary subset to a single absolute target hue,
     * preserving each DrawObj's Ostwald fill-order position.
     *
     * This is the primitive behind the "Go to N hues" functionality. Unlike the
     * shift operations above, the subset is supplied by the caller rather than
     * taken from the live selection — it may be a selection snapshot, a computed
     * remainder of unassigned DrawObjs, or the entire roster.
     *
     * This method:
     * 1. Validates the subset and target hue
     * 2. Creates a callback to handle the result
     * 3. Delegates to TemplatePaletteShift.recolorSubsetToHue()
     * 4. Fires the standard COLOR_CHANGE event on completion
     *
     * @param subset The set of DrawObjs to recolor (any subset of the roster)
     * @param targetHueNumGUI The target hue (HueNumGUI wraps user-facing 1-36)
     * @return true if the operation was delegated, false if the subset was empty
     * @throws IllegalArgumentException if targetHueNumGUI is null
     *
     * @see HueRotateCallback
     */
    public boolean recolorSubsetToHue(Set<DrawObj> subset, HueNumGUI targetHueNumGUI) {
        if (targetHueNumGUI == null) {
            throw new IllegalArgumentException("Target HueNumGUI cannot be null");
        }

        // Skip if the subset is empty
        if (subset == null || subset.isEmpty()) {
            logger.fine("No DrawObjs in subset for hue assignment operation");
            return false;
        }

        // Create callback to fire event after the assignment completes
        HueRotateCallback callback = new HueRotateCallback() {
            @Override
            public void onRotateComplete(SelectionGroup result) {
                logger.fine("Subset hue assignment completed for " +
                    result.getSelectedDrawObjsSet().size() + " objects");

                // Fire event to notify listeners of color change
                fireEvent(new PositionalPaletteEvent(
                    drawObjRoster,
                    PositionalPaletteEvent.EventType.COLOR_CHANGE,
                    drawObjRoster
                ));
            }

            @Override
            public void onRotateError(String errorMessage) {
                logger.warning("Subset hue assignment failed: " + errorMessage);
            }
        };

        // Delegate to TemplatePaletteShift with a defensive copy of the subset
        templatePaletteShift.recolorSubsetToHue(
            new java.util.HashSet<>(subset), targetHueNumGUI, drawObjRoster, callback);
        return true;
    }

    // ========== EQUAL-VALUE HUE ROTATION OPERATION ==========
    
    /**
     * Rotates the hue of all selected DrawObjs while attempting to maintain
     * their value (luminance) by finding the best-fit color in the new hue
     * that matches the original value within the specified tolerance.
     * 
     * This method:
     * 1. Gets selected DrawObjs from the roster
     * 2. Creates a SelectionGroup
     * 3. Creates EVRotateParamBundle with tolerance and baseline
     * 4. Creates a callback to handle the result
     * 5. Delegates to TemplatePaletteShift.recolorByEVHueRotateShift()
     * 6. Fires event to notify listeners
     * 
     * The event system handles all UI updates.
     * 
     * @param someRawShiftDist The hue shift distance
     *                         (positive for clockwise, negative for counter-clockwise)
     * @param useRGB true to use RGB luminance, false to use CMYK luminance
     * @param tolerance The value tolerance for matching (0.05, 0.08, 0.12, 0.15, or 0.20)
     * @param baseline The baseline value-saturation model (may be null for backward compatibility)
     * 
     * @throws IllegalStateException if no DrawObjs are selected
     * 
     * @see EVRotateParamBundle
     * @see BaselineValueSatModel
     * @see HueRotateCallback
     */
    public void shiftSelectedForEVHueRotation(int someRawShiftDist, 
                                             boolean useRGB, 
                                             double tolerance,
                                             BaselineValueSatModel baseline) {
        // Get the selected DrawObjs
        Set<DrawObj> selectedDrawObjs = drawObjRoster.getSelectedDrawObjs();
        SelectionGroup selectionGroup = new SelectionGroup(selectedDrawObjs);
        
        // Skip if no objects are selected
        if (selectionGroup.getSelectedDrawObjsSet().isEmpty()) {
            logger.fine("No DrawObjs selected for EV hue rotation operation");
            return;
        }
        
        // Create a ShiftSpecify object to encapsulate the hue shift distance
        ShiftSpecify shiftSpecify = new ShiftSpecify();
        shiftSpecify.setHueShiftDistance(someRawShiftDist);
        
        // Create callback to fire event after EV hue rotation completes
        HueRotateCallback callback = new HueRotateCallback() {
            @Override
            public void onRotateComplete(SelectionGroup result) {
                logger.fine("EV hue rotation completed for " + result.getSelectedDrawObjsSet().size() + " objects");
                
                // Fire event to notify listeners of color change
                fireEvent(new PositionalPaletteEvent(
                    drawObjRoster,
                    PositionalPaletteEvent.EventType.COLOR_CHANGE,
                    drawObjRoster
                ));
            }
            
            @Override
            public void onRotateError(String errorMessage) {
                logger.warning("EV hue rotation failed: " + errorMessage);
            }
        };
        
        // Create an EVRotateParamBundle to encapsulate EV-rotation parameters
        EVRotateParamBundle evRotateParams = new EVRotateParamBundle(useRGB, tolerance, baseline);
        
        // Delegate to TemplatePaletteShift to handle the actual EV-rotation color transformation
        templatePaletteShift.recolorByEVHueRotateShift(selectionGroup, shiftSpecify, drawObjRoster, evRotateParams, callback);
    }
    
    /**
     * Rotates the hue of all selected DrawObjs while maintaining value.
     * This is a convenience method that calls the full version with null baseline.
     * 
     * @param someRawShiftDist The hue shift distance
     * @param useRGB true to use RGB luminance, false to use CMYK luminance
     * @param tolerance The value tolerance for matching
     * 
     * @see #shiftSelectedForEVHueRotation(int, boolean, double, BaselineValueSatModel)
     */
    public void shiftSelectedForEVHueRotation(int someRawShiftDist, 
                                             boolean useRGB, 
                                             double tolerance) {
        shiftSelectedForEVHueRotation(someRawShiftDist, useRGB, tolerance, null);
    }
    
    // ========== LOAD PALETTE OPERATION ==========
    
    /**
     * Loads a new palette by applying colors from the provided palette to the DrawObjs.
     * 
     * This method:
     * 1. Validates the palette
     * 2. Applies colors to DrawObjs in order
     * 3. Updates roster state
     * 4. Fires event to notify listeners
     * 
     * @param newPalette The palette containing colors to apply
     * @return true if the palette was successfully applied, false otherwise
     * 
     * @throws IllegalArgumentException if newPalette is null
     */
    public boolean loadNewPalette(PaletteWName newPalette) {
        if (newPalette == null) {
            throw new IllegalArgumentException("Palette cannot be null");
        }
        
        if (drawObjRoster.getTemplateDrawObjs() == null || drawObjRoster.getTemplateDrawObjs().isEmpty()) {
            logger.warning("Cannot load palette: no DrawObjs in roster");
            return false;
        }
        
        // Get the color array from the palette
        ColorPtOstDot[] colorArray = newPalette.getColorPtOstdotArray();
        if (colorArray == null || colorArray.length == 0) {
            logger.warning("Cannot load empty palette");
            return false;
        }
        
        // Apply the colors to the DrawObjs
        int numDrawObjs = drawObjRoster.getTemplateDrawObjs().size();
        int numColors = colorArray.length;
        int minCount = Math.min(numDrawObjs, numColors);
        
        for (int i = 0; i < minCount; i++) {
            DrawObj drawObj = drawObjRoster.getTemplateDrawObjs().get(i);
            ColorPtOstDot newColor = colorArray[i];
            if (drawObj != null && newColor != null) {
                drawObj.setColor(newColor);
            }
        }
        
        // Update internal state to reflect the changes
        drawObjRoster.updateToCurrObjState();
        
        // Fire event to notify listeners of the palette change
        fireEvent(new PositionalPaletteEvent(
            drawObjRoster,
            PositionalPaletteEvent.EventType.COLOR_CHANGE,
            drawObjRoster
        ));
        
        return true;
    }
    
    // ========== DIRECT COLOR APPLICATION OPERATION ==========
    
    /**
     * Applies a single ColorPtOstDot directly to the currently selected DrawObj.
     * 
     * This method:
     * 1. Validates the input color is not null
     * 2. Gets the selected DrawObj from the roster
     * 3. Validates that exactly one DrawObj is selected
     * 4. Creates a callback to handle the result
     * 5. Delegates to TemplatePaletteShift.shiftOneDrawobjDirectOstdot()
     * 6. Fires event to notify listeners
     * 
     * The event system handles all UI updates.
     * 
     * This method is used by the PaletteColorSourceFrame when the user left-clicks
     * a color chip with the "Send" checkbox enabled and exactly one DrawObj is selected.
     * 
     * @param inputOstDot The ColorPtOstDot to apply to the selected DrawObj
     * 
     * @throws IllegalArgumentException if inputOstDot is null
     * 
     * @see ColorTransformCallback
     * @see TemplatePaletteShift#shiftOneDrawobjDirectOstdot
     */
    public void shiftOneDrawobjDirectOstdot(ColorPtOstDot inputOstDot) {
        // Validate parameters
        if (inputOstDot == null) {
            throw new IllegalArgumentException("ColorPtOstDot cannot be null");
        }
        
        // Get selected DrawObjs
        Set<DrawObj> selectedDrawObjs = drawObjRoster.getSelectedDrawObjs();
        SelectionGroup selectionGroup = new SelectionGroup(selectedDrawObjs);
        
        // Check if exactly one DrawObj is selected
        if (selectionGroup.size() != 1) {
            logger.fine("shiftOneDrawobjDirectOstdot requires exactly one selected DrawObj, but found " + selectionGroup.size());
            return;
        }
        
        // Create callback to fire event after color application completes
        ColorTransformCallback callback = new ColorTransformCallback() {
            @Override
            public void onTransformComplete(SelectionGroup result) {
                logger.fine("Direct color application completed for 1 object");
                
                // Update the click color square with the new color
                DrawObj recoloredDrawObj = result.getSelectedDrawObjsSet().iterator().next();
                if (recoloredDrawObj != null) {
                    ColorPtOstDot newColor = recoloredDrawObj.getCurrentColor();
                    templateWorkFrame.updateClickColorSquare(newColor);
                }
                
                // Fire event to notify listeners of color change
                fireEvent(new PositionalPaletteEvent(
                    drawObjRoster,
                    PositionalPaletteEvent.EventType.COLOR_CHANGE,
                    drawObjRoster
                ));
            }
            
            @Override
            public void onTransformError(String errorMessage) {
                logger.warning("Direct color application failed: " + errorMessage);
            }
        };
        
        // Delegate to TemplatePaletteShift to handle the actual color application
        templatePaletteShift.shiftOneDrawobjDirectOstdot(selectionGroup, inputOstDot, drawObjRoster, callback);
    } // end method shiftOneDrawobjDirectOstdot
    
    // ========== PALETTE BOOKMARKS (SAVE / UNDO / REDO) ==========
    
    /**
     * Creates the bookmark list with B0, the baseline holding the colors exactly as currently shown.
     * Call once per Template, after the DrawObjs are in the roster. If the roster has no DrawObjs,
     * no list is created and Undo/Redo stay unavailable. A second call is ignored so that B0 is
     * never replaced.
     */
    public void initPaletteBookmarks() {
        if (bookmarkList != null) {
            logger.warning("Palette bookmarks already initialized; B0 not replaced");
            return;
        }
        try {
            bookmarkList = new PaletteBookmarkList(
                    PaletteBookmark.fromRoster(drawObjRoster, PaletteBookmark.Kind.LOAD_BASELINE));
        } catch (IllegalArgumentException e) {
            logger.warning("Palette bookmarks not created: " + e.getMessage());
        }
    }
    
    /**
     * Saves the palette currently on screen as a bookmark at the top of the list. No event is fired
     * because the screen does not change.
     */
    public void saveBookmark() {
        if (bookmarkList == null) {
            logger.warning("Cannot save bookmark: palette bookmarks not initialized");
            return;
        }
        try {
            bookmarkList.save(PaletteBookmark.fromRoster(drawObjRoster, PaletteBookmark.Kind.USER_SAVE));
        } catch (IllegalArgumentException e) {
            logger.warning("Cannot save bookmark: " + e.getMessage());
        }
    }
    
    /**
     * Moves one bookmark back and puts that palette on screen. Unsaved work on screen is saved first.
     *
     * @return true if a palette was restored, false if Undo is not available
     */
    public boolean undoPalette() {
        if (bookmarkList == null) {
            logger.warning("Cannot undo: palette bookmarks not initialized");
            return false;
        }
        try {
            return restoreIfPresent(bookmarkList.undo(
                    PaletteBookmark.fromRoster(drawObjRoster, PaletteBookmark.Kind.IMPLICIT_SAVE)));
        } catch (IllegalArgumentException e) {
            logger.warning("Cannot undo: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Moves one bookmark forward and puts that palette on screen. Unsaved work on screen is saved first.
     *
     * @return true if a palette was restored, false if Redo is not available
     */
    public boolean redoPalette() {
        if (bookmarkList == null) {
            logger.warning("Cannot redo: palette bookmarks not initialized");
            return false;
        }
        try {
            return restoreIfPresent(bookmarkList.redo(
                    PaletteBookmark.fromRoster(drawObjRoster, PaletteBookmark.Kind.IMPLICIT_SAVE)));
        } catch (IllegalArgumentException e) {
            logger.warning("Cannot redo: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * @return true if Undo would move or save something; false if it would do nothing or there is no list
     */
    public boolean canUndo() {
        if (bookmarkList == null) {
            return false;
        }
        try {
            return bookmarkList.canUndo(
                    PaletteBookmark.fromRoster(drawObjRoster, PaletteBookmark.Kind.IMPLICIT_SAVE));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * @return true if Redo would move or save something; false if it would do nothing or there is no list
     */
    public boolean canRedo() {
        if (bookmarkList == null) {
            return false;
        }
        try {
            return bookmarkList.canRedo(
                    PaletteBookmark.fromRoster(drawObjRoster, PaletteBookmark.Kind.IMPLICIT_SAVE));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Moves the marker to the newest bookmark and puts that palette on screen. Unsaved work on screen is
     * saved first.
     *
     * @return true if a palette was restored, false if not available
     */
    public boolean goToTopPalette() {
        if (bookmarkList == null) {
            logger.warning("Cannot go to top: palette bookmarks not initialized");
            return false;
        }
        try {
            return restoreIfPresent(bookmarkList.goToTop(
                    PaletteBookmark.fromRoster(drawObjRoster, PaletteBookmark.Kind.IMPLICIT_SAVE)));
        } catch (IllegalArgumentException e) {
            logger.warning("Cannot go to top: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Moves the marker to B0 (the palette the Template opened with) and puts it on screen. Unsaved work
     * on screen is saved first.
     *
     * @return true if a palette was restored, false if not available
     */
    public boolean goToBottomPalette() {
        if (bookmarkList == null) {
            logger.warning("Cannot go to bottom: palette bookmarks not initialized");
            return false;
        }
        try {
            return restoreIfPresent(bookmarkList.goToBottom(
                    PaletteBookmark.fromRoster(drawObjRoster, PaletteBookmark.Kind.IMPLICIT_SAVE)));
        } catch (IllegalArgumentException e) {
            logger.warning("Cannot go to bottom: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * @return true if "go to top" would move or save something
     */
    public boolean canGoToTop() {
        if (bookmarkList == null) {
            return false;
        }
        try {
            return bookmarkList.canGoToTop(
                    PaletteBookmark.fromRoster(drawObjRoster, PaletteBookmark.Kind.IMPLICIT_SAVE));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * @return true if "go to bottom" would move or save something
     */
    public boolean canGoToBottom() {
        if (bookmarkList == null) {
            return false;
        }
        try {
            return bookmarkList.canGoToBottom(
                    PaletteBookmark.fromRoster(drawObjRoster, PaletteBookmark.Kind.IMPLICIT_SAVE));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Puts a bookmark's colors on screen through the existing loadNewPalette path (null colors are
     * skipped, the roster is updated and COLOR_CHANGE is fired).
     *
     * @return true if a bookmark was present and applied
     */
    private boolean restoreIfPresent(Optional<PaletteBookmark> bookmark) {
        if (!bookmark.isPresent()) {
            return false;
        }
        boolean applied = loadNewPalette(bookmark.get().toPaletteWName());
        if (applied) {
            onPaletteRestored();
        }
        return applied;
    }
    
    /**
     * Hook called after a bookmark is restored. Intentionally empty. Deferred Open Question 4: a
     * restore should end the current EV-rotation baseline (BaselineValueSatModel), the same as
     * exiting EV mode.
     */
    private void onPaletteRestored() {
    }
    
    // ========== EVENT MANAGEMENT ==========
    
    /**
     * Fires an event to all registered listeners via TemplateWorkFrame.
     * 
     * This method:
     * 1. Checks if templateWorkFrame is set
     * 2. Fires the event through the TemplateWorkFrame's public event dispatcher
     * 3. Logs any errors that occur
     * 
     * TemplateWorkFrame serves as the central event dispatcher for all UI listeners,
     * ensuring consistent event routing and preventing out-of-sync issues.
     * 
     * @param event The event to fire
     */
    private void fireEvent(PositionalPaletteEvent event) {
        if (templateWorkFrame == null) {
            logger.warning("TemplateWorkFrame is not set, cannot fire event");
            return;
        }
        
        try {
            templateWorkFrame.firePositionalPaletteEventPublic(event);
            logger.fine("Event fired: " + event.getEventType());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error firing event", e);
        }
    }
    
    // ========== GETTERS ==========
    
    /**
     * Gets the DrawObjRoster managed by this hub.
     * 
     * @return The DrawObjRoster
     */
    public DrawObjRoster getDrawObjRoster() {
        return drawObjRoster;
    }
    
    /**
     * Gets the TemplatePaletteShift service used by this hub.
     * 
     * @return The TemplatePaletteShift service
     */
    public TemplatePaletteShift getTemplatePaletteShift() {
        return templatePaletteShift;
    }
    
    /**
     * Gets the TemplateWorkFrame associated with this hub.
     * 
     * @return The TemplateWorkFrame
     */
    public TemplateWorkFrame getTemplateWorkFrame() {
        return templateWorkFrame;
    }
}
