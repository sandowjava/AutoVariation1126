package com.csurf.ostwald.domainmonotri.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Facade for managing the complete collection of standard colors in the Ostwald system.
 * This class maintains 36 MonoTriPages, one for each standard hue, and provides
 * centralized access to all standard ColorPtOstDot instances.
 */
public class MonotriBook {
    private static final int GUI_36_HUES = 36;
    private static MonotriBook instance; // Singleton instance
    
    private final MonotriPage[] allMonotriPages;
    
    /**
     * Gets the singleton instance of MonotriBook.
     * @return The singleton instance
     */
    public static synchronized MonotriBook getInstance() {
        if (instance == null) {
            instance = new MonotriBook();
        }
        return instance;
    }
    
    private MonotriBook() {
        allMonotriPages = new MonotriPage[GUI_36_HUES];
        fillAllMonoTriPages();
    }
    
    /**
     * Creates all 36 MonoTriPages using standard hues from HueSpacingStdChoices.
     */
    private void fillAllMonoTriPages() {
        HueSpacingStdChoices hueSpacing = HueSpacingStdChoices.getSingleton();

        for (int i = 0; i < GUI_36_HUES; i++) { // 062225 NOTE: index of loop is hueNumCode (range 0-35)
            HueNumGUI currentHueNumGUI = HueNumGUI.getHueNumGUIInstance(i+1); // 062625 NOTE: "i=1" since index of loop is hueNumCode
            int hueDegrees360 = hueSpacing.getHueDegrIntForHueNumGUI(currentHueNumGUI);
            allMonotriPages[i] = new MonotriPage(hueDegrees360, i);
        }
    }
    
    /**
     * Gets a MonotriPage for a specific standard hue.
     * @param stdHueNumCode Standard hue number (0-35)
     * @return The MonotriPage for that hue, or null if invalid
     */
    public MonotriPage getMonoTriPage(int stdHueNumCode) {
        if (stdHueNumCode < 0 || stdHueNumCode >= GUI_36_HUES) {
            return null;
        }
        return allMonotriPages[stdHueNumCode];
    }
    
    /**
     * Gets a standard color by its hue number and fill order.
     * @param stdHueNumCode Standard hue number (0-35)
     * @param fillOrder Fill order number within the hue's MonotriPage (1-78)
     * @return The standard ColorPtOstDot, or null if invalid
     */
    public ColorPtOstDot getStandardColorByCodeHueNum(int stdHueNumCode, int fillOrder) { // TODO 072025 method is hit many times, repeatedly
        // System.out.println("DEBUG: MonotriBook.getStandardColor called with hue: " + stdHueNumCode + ", fillOrder: " + fillOrder); // 072025 took out -clutter
        
        // Validate hue number is in valid internal range (0-35)
        if (stdHueNumCode < 0 || stdHueNumCode > 35) {
            throw new IllegalArgumentException(
                String.format("Invalid hue number %d. Must be between 0 and 35", stdHueNumCode));
        }
        
        // Validate fill order number is in valid range (1-78)
        if (fillOrder < 1 || fillOrder > 78) {
            throw new IllegalArgumentException(
                String.format("Invalid fill order number %d. Must be between 1 and 78", fillOrder));
        }
        
        MonotriPage page = allMonotriPages[stdHueNumCode];
        ColorPtOstDot color = page.getColorByFillOrder(fillOrder);

        /* // 072025 took out - clutter
        System.out.println("DEBUG: MonotriBook.getStandardColor returning color with hue: " + 
            (color != null ? color.getOstHCS().getStdCodeHueNum() : "null") +
            ", fillOrder: " + (color != null ? color.getOstHCS().getFillOrderNum() : "null"));
         */
        
        return color;
    } // end method getStandardColorByCodeHueNum


    /**
     * Gets a standard color by its OHCS (Ostwald Hue Column Step) coordinates.
     * This method uses HueNumGUI to eliminate ambiguity between user-facing and code-facing hue numbers.
     *
     * @param hueNum The hue number as a HueNumGUI instance (user-facing 1-36)
     * @param columnNum The column number (1-12)
     * @param stepNum The step number within the column (0 to columnNum)
     * @return The standard ColorPtOstDot at that OHCS position, or null if invalid
     */
    public ColorPtOstDot getColorByOHCS(HueNumGUI hueNum, int columnNum, int stepNum) {
        if (hueNum == null) {
            return null;
        }

        // Get the internal code hue number (0-35) from HueNumGUI
        int stdHueNumCode = hueNum.getInternalCodeHueNum();

        // Get the MonotriPage for this hue
        MonotriPage page = getMonoTriPage(stdHueNumCode);
        if (page == null) {
            return null;
        }

        // Get the color by column and step from the MonotriPage
        return page.getColorByColumnStep(columnNum, stepNum);
    } // end method getColorByOHCS



    /**
     * Gets a standard color by its hue number and W-series/B-series coordinates.
     * This method uses HueNumGUI to eliminate ambiguity between user-facing and code-facing hue numbers.
     *
     * @param hueNum The hue number as a HueNumGUI instance (user-facing 1-36, representing 0-35 internally)
     * @param someWseries The White-series coordinate (0-11)
     * @param someBseries The Black-series coordinate (0-11)
     * @return The standard ColorPtOstDot at that position, or null if invalid
     */
    public ColorPtOstDot getColorByHueWseriesBseries(HueNumGUI hueNum, int someWseries, int someBseries) {
        if (hueNum == null) {
            return null;
        }

        // Validate W-series and B-series are within bounds (0-11)
        if (someWseries < 0 || someWseries > 11) {
            return null;
        }
        if (someBseries < 0 || someBseries > 11) {
            return null;
        }

        // Get the internal code hue number (0-35) from HueNumGUI
        int stdHueNumCode = hueNum.getInternalCodeHueNum();

        // Get the MonotriPage for this hue
        MonotriPage page = getMonoTriPage(stdHueNumCode);
        if (page == null) {
            return null;
        }

        // Get the color by W-series and B-series from the MonotriPage
        return page.getColorByWseriesBseries(someWseries, someBseries);
    } // end method getColorByHueWseriesBseries



    /**
     * Finds the closest standard hue number for a given RGB color.
     * @param color The RGB color to match
     * @return The standard hue number (0-35) closest to the input color
     */
    public int findClosestStandardCodeHue(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float hue = hsb[0];  // This will be in range 0-1
        
        // Convert to degrees (0-360)
        int hueDegrees = Math.round(hue * 360);
        
        // Get standard hues array
        HueSpacingStdChoices hueSpacing = HueSpacingStdChoices.getSingleton();

        // Find closest standard hue
        int closestHueNumGUI = 0;
        int minDiff = 360;
        
        // Note: HueSpacingStdChoices array uses indices 1-36
        for (int i = 1; i <= GUI_36_HUES; i++) {
            HueNumGUI currentHueNumGUI = HueNumGUI.getHueNumGUIInstance(i);
            int stdHueDegrees = hueSpacing.getHueDegrIntForHueNumGUI(currentHueNumGUI);
            
            // Calculate smallest angular difference (considering wrap-around)
            int diff = Math.abs(hueDegrees - stdHueDegrees);
            if (diff > 180) {
                diff = 360 - diff;
            }
            
            if (diff < minDiff) {
                minDiff = diff;
                closestHueNumGUI = i;
            }
        } // end FOR LOOP

        // 061725 must convert from 1-36 to 0-35 range
        int closestHueNumCode = closestHueNumGUI-1;
        return closestHueNumCode;
    } // end method findClosestStandardCodeHue


    
    /**
     * Finds the closest ColorPtOstDot in the entire system to a given RGB color.
     * First finds the closest standard hue, then finds the closest color within that hue's page.
     * @param color The RGB color to match
     * @return The closest ColorPtOstDot in the system
     */
    public ColorPtOstDot findClosestOstDot(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("Color cannot be null");
        }

        // 061725 Find closest standard hue in 0-35 range, NOT in 1-36 range
        int closestHueNum = findClosestStandardCodeHue(color);
        // 061725 no need to convert from 1-36 range to 0-35 range
        int closestHueNumCode = closestHueNum;
        
        // Get corresponding MonotriPage using 0-35 range
        MonotriPage page = getMonoTriPage(closestHueNumCode);
        
        // Find closest color point in that page
        return page.findClosestColorPoint(color);
    } // end method findClosestOstDot


    
    /**
     * Gets the total number of standard hues.
     * @return Number of standard hues (36)
     */
    public int getStandardHueCount() {
        return GUI_36_HUES;
    }
    
    /**
     * Gets all MonoTriPages in standard hue order.
     * @return Array of all MonoTriPages
     */
    public MonotriPage[] getAllMonoTriPages() {
        return allMonotriPages.clone(); // Return copy to prevent modification
    }
    
    /**
     * Gets the hue angle in degrees for a standard hue number.
     * // TODO 062625 this method is never used - and until 062625, code was incorrect, using 0-35 range instead of 1-36
     * changed code in call to HueSpacingStdChoices to use HueNumGUI, which uses 1-36 range
     * @param stdHueNumCode Standard hue number (0-35)
     * @return Hue angle in degrees (0-360), or -1 if invalid
     */
    public int getStdHueAngleByHueNumCode(int stdHueNumCode) {
        if (stdHueNumCode < 0 || stdHueNumCode >= GUI_36_HUES) {
            return -1;
        }
        HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(stdHueNumCode+1);
        return HueSpacingStdChoices.getSingleton().getHueDegrIntForHueNumGUI(someHueNumGUI);
    } // end method getStdHueAngleByHueNumCode




    /**
     * Gets the pure hue color for a given standard hue number.
     * @param stdHueNumCode Standard hue number (0-35)
     * @return The pure hue ColorPtOstDot for that hue
     * @throws IllegalArgumentException if stdHueNumCode is outside valid range
     */
    public ColorPtOstDot getPureHueColorByCodeHueNum(int stdHueNumCode) {
        // Validate that we're in valid internal range (0-35)
        if (stdHueNumCode < 0 || stdHueNumCode > 35) {
            throw new IllegalArgumentException(
                String.format("Invalid hue number %d. Must be between 0 and 35", stdHueNumCode));
        }
        return allMonotriPages[stdHueNumCode].getPureHueOstDot();
    }


    /**
     * Gets all OstDots on the given hue's page whose RGB luminance falls within the
     * given value range, in ascending fill order.
     * This method uses HueNumGUI to eliminate ambiguity between user-facing and
     * code-facing hue numbers.
     *
     * Scans fill orders 1-66 only (gray-scale column excluded), matching the legacy
     * value-range candidate domain. The returned list is unpadded: index 0 is the
     * first candidate in fill order.
     *
     * @param hueNum The hue as a HueNumGUI instance (user-facing 1-36)
     * @param valueCtr The target RGB luminance (0.0-1.0)
     * @param valueRangePlusMinus The half-range tolerance around valueCtr
     * @return Ordered list of ColorPtOstDots within the range; empty if none
     */
    public List<ColorPtOstDot> getOstDotsByValueRange(HueNumGUI hueNum, double valueCtr, double valueRangePlusMinus) {
        if (hueNum == null) {
            throw new IllegalArgumentException("hueNum cannot be null");
        }
        int stdHueNumCode = hueNum.getInternalCodeHueNum();
        MonotriPage page = getMonoTriPage(stdHueNumCode);
        if (page == null) {
            return new ArrayList<ColorPtOstDot>();
        }
        return page.getOstDotsByValueRange(valueCtr, valueRangePlusMinus);
    } // end method getOstDotsByValueRange

} // end class MonotriBook
