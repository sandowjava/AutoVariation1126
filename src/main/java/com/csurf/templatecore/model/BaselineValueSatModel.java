package com.csurf.templatecore.model;

import com.csurf.ostwald.domainmonotri.model.CMYKValueCalc;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Captures and preserves the original value and saturation attributes of every color
 * in the palette when the user enters EV rotation mode. This baseline is used throughout
 * the EV rotation session to ensure consistent color matching and prevent drift over
 * multiple hue rotations.
 * 
 * Design:
 * - One-to-one correspondence with palette colors (indexed by fillOrderNum 1-78)
 * - Stores RGB value target, CMYK value target, and saturation target for each color
 * - Created when user enters EV rotation mode
 * - Destroyed when user exits EV rotation mode or switches templates
 * - Used by TemplatePaletteShift.recolorByEVHueRotateShift() for color matching
 * 
 * Purpose:
 * - Prevent drift: Each rotation targets the original baseline, not the current palette
 * - Consistency: 36 consecutive rotations (one hue step each) return to starting palette
 * - Flexibility: Supports both RGB and CMYK value models simultaneously
 */
public class BaselineValueSatModel {
    private static final Logger logger = Logger.getLogger(BaselineValueSatModel.class.getName());
    
    private final Map<UUID, OneDrawObjValueSatNums> baselineMap;
    private final Map<Integer, OneDrawObjValueSatNums> baselineMapByFillOrderNum;
    private final int captureHueNum;
    private final long creationTimeMillis;
    
    /**
     * Inner class to hold baseline attributes for a single color.
     * Immutable once created.
     */
    public static class OneDrawObjValueSatNums {
        private final double rgbValueTarget;
        private final double cmykValueTarget;
        private final float saturationTarget;
        private final int baselineFillOrderNum;
        private final int baselineWseries;
        private final int baselineBseries;
        
        /**
         * Creates baseline attributes for a single color.
         * 
         * @param rgbValueTarget RGB luminance value (0.0-1.0)
         * @param cmykValueTarget CMYK luminance value (0.0-1.0)
         * @param saturationTarget Color saturation (0.0-1.0)
         */
        public OneDrawObjValueSatNums(double rgbValueTarget, double cmykValueTarget, float saturationTarget,
                                      int baselineFillOrderNum, int baselineWseries, int baselineBseries) {
            this.rgbValueTarget = rgbValueTarget;
            this.cmykValueTarget = cmykValueTarget;
            this.saturationTarget = saturationTarget;
            this.baselineFillOrderNum = baselineFillOrderNum;
            this.baselineWseries = baselineWseries;
            this.baselineBseries = baselineBseries;
        }
        
        public double getRGBValueTarget() {
            return rgbValueTarget;
        }
        
        public double getCMYKValueTarget() {
            return cmykValueTarget;
        }
        
        public float getSaturationTarget() {
            return saturationTarget;
        }

        public int getBaselineFillOrderNum() {
            return baselineFillOrderNum;
        }

        public int getBaselineWseries() {
            return baselineWseries;
        }

        public int getBaselineBseries() {
            return baselineBseries;
        }
    } // end inner class OneDrawObjValueSatNums
    
    /**
     * Creates a baseline model by capturing the current value and saturation attributes
     * of all colors in the given DrawObjRoster's palette.
     * 
     * @param drawObjRoster The roster containing the palette to capture
     * @param currentHueNum The current hue number (for validation)
     */
    public BaselineValueSatModel(DrawObjRoster drawObjRoster, int currentHueNum) {
        this.baselineMap = new HashMap<>();
        this.baselineMapByFillOrderNum = new HashMap<>();
        this.captureHueNum = currentHueNum;
        this.creationTimeMillis = System.currentTimeMillis();
        
        captureBaseline(drawObjRoster);
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("BaselineValueSatModel created: captured %d colors at hue %d, timestamp=%d", 
                baselineMap.size(), captureHueNum, creationTimeMillis));
        }
    }
    
    /**
     * Captures baseline attributes for all colors in the palette.
     * Iterates through all DrawObjs and captures their color's value and saturation.
     * 
     * @param drawObjRoster The roster containing the palette
     */
    private void captureBaseline(DrawObjRoster drawObjRoster) {
        // Get the current palette from the roster
        PaletteWName palette = drawObjRoster.getCurrPalette();
        if (palette == null) {
            logger.warning("Cannot capture baseline: palette is null");
            return;
        }
        
        // Get the list of DrawObjs from the roster
        List<DrawObj> drawObjs = drawObjRoster.getTemplateDrawObjs();
        if (drawObjs == null || drawObjs.isEmpty()) {
            logger.warning("Cannot capture baseline: no DrawObjs in roster");
            return;
        }
        
        // Iterate through all DrawObjs and capture their color attributes
        for (int index = 0; index < drawObjs.size(); index++) {
            try {
                DrawObj drawObj = drawObjs.get(index);
                if (drawObj == null) {
                    logger.fine(String.format("No DrawObj found at index %d, skipping", index));
                    continue;
                }

                UUID drawObjUUID = drawObj.getUniqueId();
                if (drawObjUUID == null) {
                    logger.fine(String.format("No UUID found for DrawObj at index %d, skipping", index));
                    continue;
                }
                
                // Get the color from the DrawObj
                ColorPtOstDot color = drawObj.getCurrentColor();
                if (color == null) {
                    logger.fine(String.format("No color found for DrawObj at index %d, skipping", index));
                    continue;
                }
                
                // Get the fillOrderNum from the color
                int fillOrderNum = color.getFillOrderNum();

                OstHueColumnStep ohcs = color.getOstHCS();
                int baselineWseries = ohcs != null ? ohcs.getSeriesW() : -1;
                int baselineBseries = ohcs != null ? ohcs.getSeriesB() : -1;
                
                // Capture RGB value (hue-independent)
                double rgbValue = color.getLum3D();
                
                // Capture CMYK value (hue-dependent)
                double cmykValue = CMYKValueCalc.calcCMYKValue(color);
                
                // Capture saturation
                float saturation = (float) color.getAbsColorSaturation();
                
                // Store in baseline map indexed by UUID (per DrawObj)
                OneDrawObjValueSatNums attrs = new OneDrawObjValueSatNums(
                    rgbValue,
                    cmykValue,
                    saturation,
                    fillOrderNum,
                    baselineWseries,
                    baselineBseries
                );
                baselineMap.put(drawObjUUID, attrs);

                // Legacy compatibility: store by fillOrderNum for existing call sites.
                // Note: this is not semantically correct for per-DrawObj baselines when collisions exist.
                baselineMapByFillOrderNum.put(fillOrderNum, attrs);
                
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest(String.format("Captured baseline for fillOrderNum %d: RGB=%.4f, CMYK=%.4f, Sat=%.4f", 
                        fillOrderNum, rgbValue, cmykValue, saturation));
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, String.format("Error capturing baseline for DrawObj at index %d: %s", 
                    index, e.getMessage()), e);
            }
        }
    }
    
    /**
     * Gets the baseline attributes for a color at the specified fillOrderNum.
     * 
     * @param fillOrderNum The fill order number (1-78)
     * @return The baseline attributes, or null if not found
     */
    public OneDrawObjValueSatNums getBaseline(int fillOrderNum) {
        return baselineMapByFillOrderNum.get(fillOrderNum);
    }

    public OneDrawObjValueSatNums getBaseline(UUID drawObjUUID) {
        return baselineMap.get(drawObjUUID);
    }
    
    /**
     * Gets the RGB value target for a color at the specified fillOrderNum.
     * 
     * @param fillOrderNum The fill order number (1-78)
     * @return The RGB value target (0.0-1.0), or 0.0 if not found
     */
    public double getRGBValueTarget(int fillOrderNum) {
        OneDrawObjValueSatNums attrs = baselineMapByFillOrderNum.get(fillOrderNum);
        return attrs != null ? attrs.getRGBValueTarget() : 0.0;
    }
    
    /**
     * Gets the CMYK value target for a color at the specified fillOrderNum.
     * 
     * @param fillOrderNum The fill order number (1-78)
     * @return The CMYK value target (0.0-1.0), or 0.0 if not found
     */
    public double getCMYKValueTarget(int fillOrderNum) {
        OneDrawObjValueSatNums attrs = baselineMapByFillOrderNum.get(fillOrderNum);
        return attrs != null ? attrs.getCMYKValueTarget() : 0.0;
    }
    
    /**
     * Gets the saturation target for a color at the specified fillOrderNum.
     * 
     * @param fillOrderNum The fill order number (1-78)
     * @return The saturation target (0.0-1.0), or 0.0 if not found
     */
    public float getSaturationTarget(int fillOrderNum) {
        OneDrawObjValueSatNums attrs = baselineMapByFillOrderNum.get(fillOrderNum);
        return attrs != null ? attrs.getSaturationTarget() : 0.0f;
    }
    
    /**
     * Checks if a baseline exists for the specified fillOrderNum.
     * 
     * @param fillOrderNum The fill order number (1-78)
     * @return true if baseline exists, false otherwise
     */
    public boolean hasBaseline(int fillOrderNum) {
        return baselineMapByFillOrderNum.containsKey(fillOrderNum);
    }

    public boolean hasBaseline(UUID drawObjUUID) {
        return baselineMap.containsKey(drawObjUUID);
    }
    
    /**
     * Gets the hue number at which this baseline was captured.
     * 
     * @return The capture hue number
     */
    public int getCaptureHueNum() {
        return captureHueNum;
    }
    
    /**
     * Gets the number of colors captured in this baseline.
     * 
     * @return The count of captured colors
     */
    public int getBaselineCount() {
        return baselineMap.size();
    }
    
    /**
     * Gets the creation timestamp of this baseline.
     * 
     * @return The creation time in milliseconds since epoch
     */
    public long getCreationTimeMillis() {
        return creationTimeMillis;
    }
    
    /**
     * Gets debug information about this baseline model.
     * 
     * @return A string containing debug information
     */
    public String getDebugInfo() {
        return String.format("BaselineValueSatModel[captureHue=%d, colorCount=%d, createdAt=%d]", 
            captureHueNum, baselineMap.size(), creationTimeMillis);
    }
    
} // end class BaselineValueSatModel
