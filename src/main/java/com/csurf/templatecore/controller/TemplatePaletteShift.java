package com.csurf.templatecore.controller;

import com.csurf.ostwald.domainmonotri.model.*;
import com.csurf.templatecore.model.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Class purpose is to handle recoloring of SelectionGroup from DrawObjRoster which is passed into recoloring methods
 * Class will function as Singleton service-class which has no state
 * The recoloring methods act on SelectionGroup according to ShiftSpecify parameter
 * The recoloring methods act directly on the DrawObjs in the SelectionGroup
 * The void return is misleading since the recoloring methods the change state of SelectionGroup
 */
public class TemplatePaletteShift {
    private static final Logger logger = Logger.getLogger(TemplatePaletteShift.class.getName());



    /**
     * Shifts the colors of all selected DrawObjs by changing their position in the Ostwald color system
     * (specifically by adjusting the Wseries and Bseries coordinates).
     * 
     * @param someSelectionGroup The group of selected DrawObjs to be shifted
     * @param someShiftSpecify The specification for the shift operation
     * @param drawObjRoster The DrawObjRoster that manages the DrawObjs
     * @param callback The callback to receive the result of the operation
     */
    public void recolorByDragShift(SelectionGroup someSelectionGroup, ShiftSpecify someShiftSpecify, DrawObjRoster drawObjRoster, ColorTransformCallback callback) {
        // Skip if no objects are selected
        if (someSelectionGroup == null || someSelectionGroup.getSelectedDrawObjsSet().isEmpty()) {
            if (callback != null) {
                callback.onTransformError("No objects selected for drag shift operation");
            } else {
                logger.warning("Callback is null for recolorByDragShift");
            }
            return;
        }

        // Check if the drag shift is possible for all selected objects
        if(groupDragShiftChecksOK(someSelectionGroup, someShiftSpecify)) {
            // Loop to shift each DrawObj in the SelectionGroup to new position
            for (DrawObj drawObj : someSelectionGroup.getSelectedDrawObjsSet()) {
                try {
                    drawObj = shiftOneDrawObj(drawObj, someShiftSpecify);
                    
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(String.format("Shifted DrawObj %s to new position: W=%d, B=%d", 
                            drawObj.getName(), 
                            drawObj.getCurrColorOstPosition().getWhiteSeriesNum(),
                            drawObj.getCurrColorOstPosition().getBlackSeriesNum()));
                    }
                } catch (Exception e) {
                    // Log any errors but continue processing other objects
                    logger.log(Level.SEVERE, String.format("Error shifting color for DrawObj %s: %s", 
                        drawObj.getName(), e.getMessage()), e);
                }
            }
            
            // Update internal state of the DrawObjRoster to reflect the changes
            drawObjRoster.updateToCurrObjState();
        } else {
            // Optionally notify the user that the drag operation is not possible
            logger.info("Drag shift not possible for the current selection with the specified shift values");
            if (callback != null) {
                callback.onTransformError("Drag shift not possible for the current selection with the specified shift values");
            }
            return;
        }
        
        // Operation succeeded, invoke callback with result
        if (callback != null) {
            callback.onTransformComplete(someSelectionGroup);
        } else {
            logger.warning("Callback is null for recolorByDragShift");
        }
    } // end method recolorByDragShift




    /**
     * Shifts the colors of all selected DrawObjs by rotating their hue.
     * 
     * @param selectionGroup The group of selected DrawObjs to be shifted
     * @param shiftSpecify The specification for the shift operation
     * @param drawObjRoster The DrawObjRoster that manages the DrawObjs
     * @param callback The callback to receive the result of the operation
     */
    public void recolorByHueRotateShift(SelectionGroup selectionGroup, ShiftSpecify shiftSpecify, DrawObjRoster drawObjRoster, HueRotateCallback callback) {
        // Skip if no objects are selected
        if (selectionGroup == null || selectionGroup.getSelectedDrawObjsSet().isEmpty()) {
            if (callback != null) {
                callback.onRotateError("No objects selected for hue rotation operation");
            } else {
                logger.warning("Callback is null for recolorByHueRotateShift");
            }
            return;
        }
        
        // Get the raw shift distance from the ShiftSpecify object
        int someRawShiftDist = shiftSpecify.getShiftHue();
        
        // for each of the selected drawObjs
        for (DrawObj drawObj : selectionGroup.getSelectedDrawObjsSet()) {
            try {
                // Get the current color and its properties
                ColorPtOstDot currentColor = drawObj.getCurrentColor();
                int currentCodeHueNum = currentColor.getStdCodeHueNum();
                int fillOrderNum = currentColor.getFillOrderNum();
                
                // Calculate the new hue by using HueNumGUI to add someRawShiftDist to the current hue
                HueNumGUI currentHueNumGUI = HueNumGUI.getHueNumGUIInstance(currentCodeHueNum+1); // expected input is user-facing

                if(currentCodeHueNum == 35) { // 082625 for debug only
                    int kerflooShiftHueNum = 666;
                }

                HueNumGUI newHueNumGUI = currentHueNumGUI.circularAddOrSubtract(someRawShiftDist);
                int newUserFacingHueNum = newHueNumGUI.getUserFacingHueNum();
                
                // Get the new color from MonotriBook by getting the same fillOrder in the new hue
                ColorPtOstDot newColor = MonotriBook.getInstance()
                    .getMonoTriPage(newUserFacingHueNum-1)// subtract 1, since expected input is CODE-facing
                    .getColorByFillOrder(fillOrderNum);
                
                // Update the DrawObj with the new color
                drawObj.setColor(newColor);
                
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format("Shifted DrawObj %s from hue %d to hue %d with fillOrder %d", 
                        drawObj.getName(), currentCodeHueNum, newUserFacingHueNum, fillOrderNum));
                }
            } catch (Exception e) {
                // Log any errors but continue processing other objects
                logger.log(Level.SEVERE, String.format("Error shifting hue for DrawObj %s: %s", 
                    drawObj.getName(), e.getMessage()), e);
            }
        }
        
        // Update internal state of the DrawObjRoster to reflect the changes
        drawObjRoster.updateToCurrObjState();
        
        // Operation succeeded, invoke callback with result
        if (callback != null) {
            callback.onRotateComplete(selectionGroup);
        } else {
            logger.warning("Callback is null for recolorByHueRotateShift");
        }
    } // end method recolorByHueRotateShift



    /**
     * Recolors all DrawObjs in an arbitrary subset to a single absolute target hue,
     * while keeping each DrawObj's position in the hue column (fill order) the same.
     *
     * This is the primitive operation behind the "Go to N hues" functionality:
     * each commit produces a subset (a selection snapshot, a computed remainder,
     * or the whole roster) and a target hue, and this method applies the recolor.
     * For each DrawObj this is equivalent to a hue-rotation recoloring, but the
     * overall operation is an absolute hue assignment, not a rotation.
     *
     * @param subset The set of DrawObjs to recolor (may be any subset of the roster)
     * @param targetHueNumGUI The target hue as a HueNumGUI (user-facing 1-36;
     *                        internal code hue is derived via getInternalCodeHueNum())
     * @param drawObjRoster The DrawObjRoster that manages the DrawObjs
     * @param callback The callback to receive the result of the operation
     */
    public void recolorSubsetToHue(Set<DrawObj> subset, HueNumGUI targetHueNumGUI,
                                   DrawObjRoster drawObjRoster, HueRotateCallback callback) {
        // Skip if the subset is empty or null
        if (subset == null || subset.isEmpty()) {
            if (callback != null) {
                callback.onRotateError("No DrawObjs provided for subset hue assignment");
            } else {
                logger.warning("Callback is null for recolorSubsetToHue");
            }
            return;
        }

        if (targetHueNumGUI == null || drawObjRoster == null) {
            if (callback != null) {
                callback.onRotateError("Null target hue or roster for subset hue assignment");
            } else {
                logger.warning("Callback is null for recolorSubsetToHue");
            }
            return;
        }

        // Resolve the target hue's MonotriPage once for the whole subset
        int targetCodeHueNum = targetHueNumGUI.getInternalCodeHueNum();
        MonotriPage targetPage = MonotriBook.getInstance().getMonoTriPage(targetCodeHueNum);
        if (targetPage == null) {
            if (callback != null) {
                callback.onRotateError("No MonotriPage found for hue " +
                    targetHueNumGUI.getUserFacingHueNum());
            } else {
                logger.warning("Callback is null for recolorSubsetToHue");
            }
            return;
        }

        // Snapshot the subset so caller-side changes cannot affect iteration
        Set<DrawObj> subsetSnapshot = new HashSet<>(subset);

        for (DrawObj drawObj : subsetSnapshot) {
            try {
                // Get the current color and its Ostwald fill-order position
                ColorPtOstDot currentColor = drawObj.getCurrentColor();
                int fillOrderNum = currentColor.getFillOrderNum();

                // Get the new color: same fill-order position in the target hue's page
                ColorPtOstDot newColor = targetPage.getColorByFillOrder(fillOrderNum);

                if (newColor != null) {
                    drawObj.setColor(newColor);
                } else {
                    logger.warning(String.format(
                        "No color at fillOrder %d on hue page %d; DrawObj %s unchanged",
                        fillOrderNum, targetCodeHueNum, drawObj.getName()));
                }

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format("Recolored DrawObj %s to hue %d with fillOrder %d",
                        drawObj.getName(), targetHueNumGUI.getUserFacingHueNum(), fillOrderNum));
                }
            } catch (Exception e) {
                // Log any errors but continue processing other objects
                logger.log(Level.SEVERE, String.format("Error recoloring DrawObj %s to hue %d: %s",
                    drawObj.getName(), targetHueNumGUI.getUserFacingHueNum(), e.getMessage()), e);
            }
        }

        // Update internal state of the DrawObjRoster to reflect the changes
        drawObjRoster.updateToCurrObjState();

        // Operation succeeded, invoke callback with result
        if (callback != null) {
            callback.onRotateComplete(new SelectionGroup(subsetSnapshot));
        } else {
            logger.warning("Callback is null for recolorSubsetToHue");
        }
    } // end method recolorSubsetToHue



    /**
     * Performs equal-value hue rotation on selected DrawObjs.
     * This method rotates the hue of selected objects while attempting to maintain
     * their value (luminance) by finding the best-fit color in the new hue that matches
     * the original value within the specified tolerance.
     * 
     * @param theSelectionGroup The group of selected DrawObjs to be shifted
     * @param shiftSpecify The specification for the hue shift operation
     * @param drawObjRoster The DrawObjRoster that manages the DrawObjs
     * @param evRotateParams The EV-rotation parameters (RGB/CMYK flag and tolerance)
     * @param callback The callback to receive the result of the operation
     */
    public void recolorByEVHueRotateShift(SelectionGroup theSelectionGroup, ShiftSpecify shiftSpecify,
                                          DrawObjRoster drawObjRoster, EVRotateParamBundle evRotateParams, HueRotateCallback callback) {
        // Skip if no objects are selected
        if (theSelectionGroup == null || theSelectionGroup.getSelectedDrawObjsSet().isEmpty()) {
            if (callback != null) {
                callback.onRotateError("No objects selected for EV hue rotation operation");
            } else {
                logger.warning("Callback is null for recolorByEVHueRotateShift");
            }
            return;
        }
        
        // Get the raw shift distance from the ShiftSpecify object
        int someRawShiftDist = shiftSpecify.getShiftHue();
        
        // Get the tolerance value from the EVRotateParamBundle
        double tolerance = evRotateParams.getValueTolerance();
        boolean useRGB = evRotateParams.usingRGBLumForHueRotate();
        BaselineValueSatModel baseline = evRotateParams.getBaseline();
        
        // For each of the selected drawObjs
        for (DrawObj drawObj : theSelectionGroup.getSelectedDrawObjsSet()) {
            try {
                // Get the current color and its properties
                ColorPtOstDot currentColor = drawObj.getCurrentColor();
                if (currentColor == null) {
                    logger.warning(String.format("EV-rotation skipped for DrawObj %s: currentColor is null", 
                        drawObj != null ? drawObj.getName() : "(null)"));
                    continue;
                }
                int currentCodeHueNum = currentColor.getStdCodeHueNum();
                int currentFillOrderNum = currentColor.getFillOrderNum();
                UUID drawObjUUID = drawObj.getUniqueId();
                
                // Determine target value: use baseline if available, otherwise use current color
                double targetValue;
                float targetSaturation;
                
                BaselineValueSatModel.OneDrawObjValueSatNums baselineAttrs = null;
                if (baseline != null && drawObjUUID != null && baseline.hasBaseline(drawObjUUID)) {
                    baselineAttrs = baseline.getBaseline(drawObjUUID);
                }

                if (baselineAttrs != null) {
                    // Use baseline targets (prevents drift) - per DrawObj
                    targetValue = useRGB ? baselineAttrs.getRGBValueTarget() : baselineAttrs.getCMYKValueTarget();
                    targetSaturation = baselineAttrs.getSaturationTarget();
                    
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest(String.format("Using baseline targets for DrawObj %s (uuid=%s): value=%.4f, sat=%.4f, baselineFillOrderNum=%d", 
                            drawObj.getName(), drawObjUUID, targetValue, targetSaturation, baselineAttrs.getBaselineFillOrderNum()));
                    }
                } else {
                    // Fallback: use current color attributes (legacy behavior if baseline not available)
                    targetValue = useRGB ? 
                        currentColor.getLum3D() : 
                        CMYKValueCalc.calcCMYKValue(currentColor);
                    targetSaturation = (float) currentColor.getAbsColorSaturation();
                    
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(String.format("Baseline not available for DrawObj %s (uuid=%s), using current color attributes (fillOrderNum %d)", 
                            drawObj.getName(), drawObjUUID, currentFillOrderNum));
                    }
                }
                
                // Calculate the new hue by using HueNumGUI to add someRawShiftDist to the current hue
                HueNumGUI currentHueNumGUI = HueNumGUI.getHueNumGUIInstance(currentCodeHueNum + 1); // expected input is user-facing
                HueNumGUI newHueNumGUI = currentHueNumGUI.circularAddOrSubtract(someRawShiftDist);
                int newUserFacingHueNum = newHueNumGUI.getUserFacingHueNum();
                
                // Get the MonotriPage for the new hue
                MonotriPage newHuePage = MonotriBook.getInstance()
                    .getMonoTriPage(newUserFacingHueNum - 1); // subtract 1, since expected input is CODE-facing
                
                // Check if the current color is a gray color - if so, preserve it as gray in the new hue
                ColorPtOstDot bestFitColor = null;
                if (currentColor instanceof GrayColorPtOstDot) {
                    // For gray colors, directly get the gray color at the same fillOrderNum in the new hue
                    bestFitColor = ifGrayOstdotKeepGray(currentColor, newUserFacingHueNum - 1);
                    
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(String.format("Preserving gray color for DrawObj %s in new hue %d", 
                            drawObj.getName(), newUserFacingHueNum));
                    }
                } else {
                    // For chromatic colors, find the best-fit color in the new hue that matches the target value within tolerance
                    // Pass targetSaturation so we can match saturation in the new hue
                    bestFitColor = findBestFitColorByValue(newHuePage, targetValue, targetSaturation, tolerance, useRGB);
                    
                    // If no acceptable candidate found within tolerance, fall back to same fillOrderNum in new hue
                    if (bestFitColor == null) {
                        bestFitColor = newHuePage.getColorByFillOrder(currentFillOrderNum);
                        
                        if (bestFitColor != null) {
                            logger.warning(String.format("No acceptable candidate found, used same fillOrderNum in new hue for DrawObj %s in hue %d with value %.3f; fallback fillOrderNum %d", 
                                drawObj.getName(), newUserFacingHueNum, targetValue, currentFillOrderNum));
                            // TODO 112725 Breakpoint location for debugging fallback cases
                            int breakpointMarker = 666;  // Set breakpoint here to debug fallback1 cases
                        }
                    }
                }
                
                // If a suitable color was found, update the DrawObj with the new color
                if (bestFitColor != null) {
                    drawObj.setColor(bestFitColor);
                    
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(String.format("EV-rotated DrawObj %s from hue %d to hue %d, value %.3f to %.3f", 
                            drawObj.getName(), currentCodeHueNum, newUserFacingHueNum, targetValue, 
                            useRGB ? bestFitColor.getLum3D() : CMYKValueCalc.calcCMYKValue(bestFitColor)));
                    }
                } else {
                    // If no suitable color found even with fallback, log warning but keep original color
                    logger.warning(String.format("No suitable color found for EV-rotation of DrawObj %s in hue %d with value %.3f", 
                        drawObj.getName(), newUserFacingHueNum, targetValue));
                }
            } catch (Exception e) {
                // Log any errors but continue processing other objects
                logger.log(Level.SEVERE, String.format("Error in EV-rotation for DrawObj %s: %s", 
                    drawObj.getName(), e.getMessage()), e);
            }
        }
        
        // Update internal state of the DrawObjRoster to reflect the changes
        drawObjRoster.updateToCurrObjState();
        
        // Operation succeeded, invoke callback with result
        if (callback != null) {
            callback.onRotateComplete(theSelectionGroup);
        } else {
            logger.warning("Callback is null for recolorByEVHueRotateShift");
        }
    } // end method recolorByEVHueRotateShift



    /**
     * Applies a single ColorPtOstDot directly to the selected DrawObj.
     * 
     * This method:
     * 1. Validates that the selection contains exactly one DrawObj
     * 2. Applies the input color directly to that DrawObj
     * 3. Updates the DrawObjRoster state
     * 4. Invokes the callback with the result
     * 
     * This is the simplest recoloring operation - it directly applies a color without
     * any transformation or calculation.
     * 
     * @param selectionGroup The group containing the single DrawObj to recolor
     * @param inputOstDot The ColorPtOstDot to apply to the DrawObj
     * @param drawObjRoster The DrawObjRoster that manages the DrawObjs
     * @param callback The callback to receive the result of the operation
     * 
     * @see ColorTransformCallback
     */
    public void shiftOneDrawobjDirectOstdot(SelectionGroup selectionGroup, 
                                            ColorPtOstDot inputOstDot, 
                                            DrawObjRoster drawObjRoster, 
                                            ColorTransformCallback callback) {
        // Validate selection
        if (selectionGroup == null || selectionGroup.getSelectedDrawObjsSet().isEmpty()) {
            if (callback != null) {
                callback.onTransformError("Selection is empty");
            } else {
                logger.warning("Callback is null for shiftOneDrawobjDirectOstdot");
            }
            return;
        }
        
        // Validate input color
        if (inputOstDot == null) {
            if (callback != null) {
                callback.onTransformError("Input color is null");
            } else {
                logger.warning("Input color is null for shiftOneDrawobjDirectOstdot");
            }
            return;
        }
        
        // Get the single DrawObj from the selection
        DrawObj drawObj = selectionGroup.getSelectedDrawObjsSet().iterator().next();
        
        // Apply the color directly
        try {
            drawObj.setColor(inputOstDot);
            
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(String.format("Applied color directly to DrawObj %s", drawObj.getName()));
            }
        } catch (Exception e) {
            // Log any errors but call error callback
            logger.log(Level.SEVERE, String.format("Error applying color to DrawObj %s: %s", 
                drawObj.getName(), e.getMessage()), e);
            if (callback != null) {
                callback.onTransformError("Error applying color: " + e.getMessage());
            }
            return;
        }
        
        // Update internal state of the DrawObjRoster to reflect the changes
        drawObjRoster.updateToCurrObjState();
        
        // Operation succeeded, invoke callback with result
        if (callback != null) {
            callback.onTransformComplete(selectionGroup);
        } else {
            logger.warning("Callback is null for shiftOneDrawobjDirectOstdot");
        }
    } // end method shiftOneDrawobjDirectOstdot


    /**
     * Finds the best-fit color in the given MonotriPage that matches the target value within tolerance.
     * Uses a two-stage filter approach:
     * Stage 1: Filter candidates whose value is within the tolerance of the target value
     * Stage 2: Among filtered candidates, select the one with saturation closest to the target saturation
     *          (All candidates that passed Stage 1 have equal value status; saturation is the only selection criterion)
     * 
     * @param newHuePage The MonotriPage containing colors for the new hue
     * @param targetValue The target luminance value to match
     * @param targetSaturation The target saturation to match (from baseline or current color)
     * @param tolerance The tolerance for value matching
     * @param useRGB true to use RGB luminance, false to use CMYK luminance
     * @return The best-fit ColorPtOstDot, or null if no suitable color found
     */
    private ColorPtOstDot findBestFitColorByValue(MonotriPage newHuePage, 
                                                   double targetValue, float targetSaturation,
                                                   double tolerance, boolean useRGB) {
        ColorPtOstDot bestFitColor = null;
        double bestValueDifference = Double.MAX_VALUE;
        double bestSaturationDifference = Double.MAX_VALUE;
        
        // Iterate through all colors in the new hue page (fillOrderNum 1-66)
        // Note: Gray colors (fillOrderNum 67-78) are handled separately by the caller via ifGrayOstdotKeepGray()
        for (int fillOrderNum = 1; fillOrderNum <= 66; fillOrderNum++) {
            try {
                ColorPtOstDot candidateColor = newHuePage.getColorByFillOrder(fillOrderNum);
                
                if (candidateColor == null) {
                    continue;
                }
                
                // Stage 1: Calculate candidate value and check if within tolerance
                double candidateValue = useRGB ? 
                    candidateColor.getLum3D() : 
                    CMYKValueCalc.calcCMYKValue(candidateColor);
                
                double valueDifference = Math.abs(candidateValue - targetValue);
                
                // Check if candidate is within tolerance
                if (valueDifference <= tolerance) {
                    // Stage 2: Among candidates within tolerance, select by closest saturation to start color
                    // All candidates that passed Stage 1 have equal value status
                    // Selection criterion: saturation match only (closest saturation to start color)
                    double candidateSaturation = candidateColor.getAbsColorSaturation();
                    double saturationDifference = Math.abs(candidateSaturation - targetSaturation);
                    
                    // Select if:
                    // 1. First valid candidate, OR
                    // 2. Closer saturation match (to start color's saturation)
                    if (bestFitColor == null || 
                        saturationDifference < bestSaturationDifference) {
                        
                        bestValueDifference = valueDifference;
                        bestSaturationDifference = saturationDifference;
                        bestFitColor = candidateColor;
                    }
                }
            } catch (Exception e) {
                // Log and continue if there's an error processing this color
                logger.log(Level.FINE, String.format("Error processing color at fillOrderNum %d: %s", 
                    fillOrderNum, e.getMessage()));
            }
        }
        
        return bestFitColor;
    } // end method findBestFitColorByValue


    // 112725 added method
    private ColorPtOstDot ifGrayOstdotKeepGray(ColorPtOstDot someOstDot, int newHue) {
        if (someOstDot instanceof GrayColorPtOstDot) {
            // Get the fillOrderNum from the current gray color
            int fillOrderNum = someOstDot.getFillOrderNum();
            
            // Get the MonotriPage for the new hue (newHue is code-facing, 0-35)
            MonotriPage newHuePage = MonotriBook.getInstance().getMonoTriPage(newHue);
            
            // Return the color at the same fillOrderNum in the new hue's MonotriPage
            return newHuePage.getColorByFillOrder(fillOrderNum);
        }
        return null;
    } // end method ifGrayOstdotKeepGray


    /**
     * This method checks whether all elements in the given SelectionGroup can be
     * dragged according to the given ShiftSpecify. The checks are as follows:
     *
     * 1. shift in Wseries is positive, check Wseries shift-result does not go over the max
     * 2. shift in Wseries is negative, check Wseries shift-result does not go under the min
     * 3. shift in Bseries is positive, check Bseries shift-result does not go over the max
     * 4. shift in Bseries is negative, check Bseries shift-result does not go under the min
     *
     * If any of the checks fail, the method will return false.
     *
     * @param someSelectionGroup the SelectionGroup to check
     * @param shiftSpecify the ShiftSpecify to check against
     * @return true if all elements in the SelectionGroup can be dragged according to the given ShiftSpecify, false otherwise
     */
    public boolean groupDragShiftChecksOK(SelectionGroup someSelectionGroup, ShiftSpecify shiftSpecify) {
        boolean isDragShiftPossible = false;

        // OstPosition[] allPositions = someSelectionGroup.getAllPositions();

        // Four cases to check. Any of these cases can return a FALSE for the entire selection group
        // 1. shift in Wseries is positive, check Wseries shift-result does not go over the max
        // 2. shift in Wseries is negative, check Wseries shift-result does not go under the min
        // 3. shift in Bseries is positive, check Bseries shift-result does not go over the max
        // 4. shift in Bseries is negative, check Bseries shift-result does not go under the min
        if (shiftSpecify.getShiftWSeries() > 0) {
            // shift in Wseries is positive, check Wseries shift-result does not go over the max
            int maxWSeries = someSelectionGroup.getHighestWseries();
            if (maxWSeries + shiftSpecify.getShiftWSeries() > 11) {
                return isDragShiftPossible; // isDragShiftPossible = false;
            } // end INNER IF
        } // end OUTER IF

        if (shiftSpecify.getShiftWSeries() < 0) {
            // shift in Wseries is negative, check Wseries shift-result does not go under the min
            int minWSeries = someSelectionGroup.getLowestWseries();
            if (minWSeries + shiftSpecify.getShiftWSeries() < 0) {
                return isDragShiftPossible; // isDragShiftPossible = false;
            } // end INNER IF
        } // end OUTER IF

        if (shiftSpecify.getShiftBSeries() > 0) {
            // shift in Bseries is positive, check Bseries shift-result does not go over the max
            int maxBSeries = someSelectionGroup.getHighestBseries();
            if (maxBSeries + shiftSpecify.getShiftBSeries() > 11) {
                return isDragShiftPossible; // isDragShiftPossible = false;
            } // end INNER IF
        } // end OUTER IF

        if (shiftSpecify.getShiftBSeries() < 0) {
            // shift in Bseries is negative, check Bseries shift-result does not go under the min
            int minBSeries = someSelectionGroup.getLowestBseries();
            if (minBSeries + shiftSpecify.getShiftBSeries() < 0) {
                return isDragShiftPossible; // isDragShiftPossible = false;
            } // end INNER IF
        } // end OUTER IF

        // If we get here, all checks passed, or both Wseries shift and Bseries shift are ZERO (= no shift)
        isDragShiftPossible = true;

        return isDragShiftPossible;
    } // end method groupDragShiftChecksOK



    private DrawObj shiftOneDrawObj(DrawObj someDrawObj, ShiftSpecify someShiftSpecify) {
        // these attributes do not change
        int hueNumUnchanged = someDrawObj.getCurrentColor().getStdCodeHueNum();
        MonotriPage theMonotriPage = MonotriBook.getInstance().getMonoTriPage(hueNumUnchanged);

        int newWseries = someDrawObj.getCurrColorOstPosition().getWhiteSeriesNum() + someShiftSpecify.getShiftWSeries();
        int newBseries = someDrawObj.getCurrColorOstPosition().getBlackSeriesNum() + someShiftSpecify.getShiftBSeries();

        // these lines run OK, but really do NOT want to construct ColorPtOstDot this way, since they are designed as singletons
        // much better to just get the new ColorPtOstDot from the MonotriPage, this is the proper way to do it
        // int someFillOrderNum = MonotriPage.calcFillOrderNumFromWseriesBseries(newWseries, newBseries);
        // ColumnStepWhiteBlackParams newPositionParamBundle = ColorPtOstDot.createParamBundleFromFillOrderNum(someFillOrderNum);
        // OstHueColumnStep newOstHCS = new OstHueColumnStep(hueNumUnchanged, newPositionParamBundle, someFillOrderNum);
        // then create a new ColorPtOstDot - but this is the WRONG way to do it

        // create new ColorPtOstDot with shifted Wseries and Bseries
        ColorPtOstDot newColor = theMonotriPage.getColorByWseriesBseries(newWseries, newBseries);
        // swap in the new color/position into the DrawObj that got passed in as parameter
        someDrawObj.setColor(newColor);

        // return the position-shifted DrawObj
        return someDrawObj;
    } // end method shiftOneDrawObj


} // end class TemplatePaletteShift
