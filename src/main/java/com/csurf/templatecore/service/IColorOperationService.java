package com.csurf.templatecore.service;

import java.util.Set;

import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.templatecore.model.BaselineValueSatModel;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.ShiftSpecify;

/**
 * Service interface for color operation requests.
 * 
 * This interface defines the contract for color operation services.
 * Implementations are responsible for orchestrating color transformations
 * on selected DrawObjs.
 * 
 * Implementations should:
 * - Accept color operation requests
 * - Delegate to appropriate service layer (e.g., TemplatePaletteShift)
 * - Fire events to notify listeners of changes
 * - Handle errors appropriately
 * 
 * @see com.csurf.templatecore.view.TemplateWorkFrame
 * @see com.csurf.templatecore.controller.PaletteOperationHub
 */
public interface IColorOperationService {
    
    /**
     * Shift selected DrawObjs by drag shift (W-series and B-series).
     * 
     * This operation shifts the colors of selected DrawObjs along the W-series
     * (white-black axis) and B-series (blue-yellow axis) in Ostwald color space.
     * 
     * @param shiftSpecify The shift specification containing W and B shifts
     * @throws IllegalArgumentException if shiftSpecify is null
     * @throws IllegalStateException if no DrawObjs are selected
     */
    void shiftSelectedForDragShift(ShiftSpecify shiftSpecify);
    
    /**
     * Shift selected DrawObjs by hue rotation.
     * 
     * This operation rotates the hue of selected DrawObjs while preserving
     * their position (fillOrderNum) in the color space.
     * 
     * @param someRawShiftDist The hue shift distance (in hue steps)
     * @throws IllegalStateException if no DrawObjs are selected
     */
    void shiftSelectedForHueRotation(int someRawShiftDist);
    
    /**
     * Shift selected DrawObjs by equal-value hue rotation.
     * 
     * This operation rotates the hue of selected DrawObjs while preserving
     * their value (luminance) and saturation. This is useful for maintaining
     * consistent brightness and saturation while changing hue.
     * 
     * @param someRawShiftDist The hue shift distance (in hue steps)
     * @param useRGB Whether to use RGB color space for calculations
     * @param tolerance The tolerance for color matching (0.0 to 1.0)
     * @param baseline The baseline value-saturation model for EV rotation
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IllegalStateException if no DrawObjs are selected
     */
    void shiftSelectedForEVHueRotation(int someRawShiftDist, boolean useRGB, 
                                       double tolerance, BaselineValueSatModel baseline);
    
    /**
     * Recolors all DrawObjs in an arbitrary subset to a single absolute target hue,
     * preserving each DrawObj's Ostwald fill-order position.
     *
     * This is the primitive behind the "Go to N hues" functionality. Unlike the
     * shift operations, the subset is supplied by the caller rather than taken
     * from the live selection — it may be a selection snapshot, a computed
     * remainder of unassigned DrawObjs, or the entire roster.
     *
     * @param subset The set of DrawObjs to recolor (any subset of the roster)
     * @param targetHueNumGUI The target hue (HueNumGUI wraps user-facing 1-36)
     * @return true if the operation was delegated, false if the subset was empty
     * @throws IllegalArgumentException if targetHueNumGUI is null
     */
    boolean recolorSubsetToHue(Set<DrawObj> subset, HueNumGUI targetHueNumGUI);
    
    /**
     * Load a new palette by applying colors to all DrawObjs in the template.
     * 
     * This operation loads a palette and applies its colors to the DrawObjs in order.
     * Colors are matched by index: the first color in the palette is applied to the first DrawObj,
     * and so on. If the palette has fewer colors than DrawObjs, only the matching DrawObjs are updated.
     * If the palette has more colors than DrawObjs, the extra colors are ignored.
     * 
     * This is the primary public interface for palette loading operations. All external callers
     * should use this method rather than calling DrawObjRoster.loadNewPalette() directly.
     * 
     * @param newPalette The palette containing colors to apply to DrawObjs
     * @return true if the palette was successfully applied, false otherwise
     * @throws IllegalArgumentException if newPalette is null
     * @throws IllegalStateException if no DrawObjs are available
     */
    boolean loadNewPalette(com.csurf.templatecore.model.PaletteWName newPalette);
    
    /**
     * Saves the palette currently on screen as a bookmark at the top of the bookmark list.
     * The screen does not change, so no event is fired.
     */
    void saveBookmark();
    
    /**
     * Moves one bookmark back and puts that palette on screen. Unsaved work on screen is saved
     * as a bookmark first.
     * 
     * @return true if a palette was restored, false if Undo is not available
     */
    boolean undoPalette();
    
    /**
     * Moves one bookmark forward and puts that palette on screen. Unsaved work on screen is saved
     * as a bookmark first.
     * 
     * @return true if a palette was restored, false if Redo is not available
     */
    boolean redoPalette();
    
    /**
     * @return true if Undo would move or save something, false otherwise
     */
    boolean canUndo();
    
    /**
     * @return true if Redo would move or save something, false otherwise
     */
    boolean canRedo();
    
    /**
     * Moves to the newest bookmark and puts that palette on screen. Unsaved work on screen is saved
     * as a bookmark first.
     * 
     * @return true if a palette was restored, false if not available
     */
    boolean goToTopPalette();
    
    /**
     * Moves to the oldest bookmark (B0, the palette the Template opened with) and puts it on screen.
     * Unsaved work on screen is saved as a bookmark first.
     * 
     * @return true if a palette was restored, false if not available
     */
    boolean goToBottomPalette();
    
    /**
     * @return true if "go to top" would move or save something, false otherwise
     */
    boolean canGoToTop();
    
    /**
     * @return true if "go to bottom" would move or save something, false otherwise
     */
    boolean canGoToBottom();
}
