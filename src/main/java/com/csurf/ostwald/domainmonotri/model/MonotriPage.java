package com.csurf.ostwald.domainmonotri.model;

import com.csurf.ostwald.domaingeo.model.HLSParamBundle;
import com.csurf.ostwald.domaingeo.model.ColorPtArb;
import com.csurf.ostwald.domaingeo.model.RGBCubeModel;

import java.awt.*;
// explicit java.util imports required: java.awt.* wildcard would otherwise resolve List to java.awt.List
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a triangular arrangement of colors for a single hue in the Ostwald color system.
 * The triangle is organized as follows:
 * 
 * - 12 columns of increasing height (EDGESIZE = 12)
 * - Column 1: Single pure-hue color
 * - Columns 2-11: Intermediate colors between clear-black and clear-white series
 * - Column 12: Gray-scale column (pure white to pure black)
 * 
 * Color positions are organized in three main series:
 * 1. Clear-Black Series: Edge from pure-hue to black
 * 2. Clear-White Series: Edge from pure-hue to white
 * 3. Gray-Scale Series: Edge from white to black
 */
public class MonotriPage {
    // Constants defining the triangle structure
    private static final int NUM_OSTDOTS_PLUS_ONE = 79;  // Total colors + 1 for indexing
    private static final int EDGESIZE_PLUS_ONE = 13;     // Triangle edge size + 1
    // Last fill order before the gray-scale column (67-78). The value-range query scans
    // 1-66 to reproduce the legacy candidate domain exactly; bump this bound to include grays.
    private static final int LAST_NON_GRAY_FILL_ORDER = 66;
    private final ColorPtOstDot[] allOstDots = new ColorPtOstDot[NUM_OSTDOTS_PLUS_ONE];
    private final ColorPtOstDot[] drawOrderColorPts = new ColorPtOstDot[NUM_OSTDOTS_PLUS_ONE];  // Array ordered by drawOrderNum

    private int hueNumCode;  // Stores hue in 0-35 range

    // Percentage steps for clear-black and clear-white series
    private static final int[] clearBlackPercents = {0, 10, 20, 32, 42, 54, 66, 74, 82, 90, 97, 0, 0}; 
    private static final int[] clearWhitePercents = {0, 10, 20, 32, 42, 54, 66, 71, 80, 88, 95, 0, 0}; 
    private static final int[] grayScaleLumPercents = {0, 0, 10, 18, 23, 29, 35, 41, 51, 63, 76, 88, 100, 0, 0}; 

    // Step percentages for intermediate columns between clear-black and clear-white
    private static final int[] column3steps = {0, 50};
    private static final int[] column4steps = {0, 32, 74};
    private static final int[] column5steps = {0, 22, 50, 83};
    private static final int[] column6steps = {0, 18, 40, 60, 86};
    private static final int[] column7steps = {0, 12, 30, 48, 66, 90};
    private static final int[] column8steps = {0, 9, 22, 36, 51, 69, 90};
    private static final int[] column9steps = {0, 9, 19, 30, 42, 58, 74, 91};
    private static final int[] column10steps = {0, 8, 19, 25, 35, 46, 61, 76, 92};
    private static final int[] column11steps = {0, 7, 16, 22, 30, 39, 50, 60, 74, 88};

    // constructor
    /**
     * Creates a new MonotriPage instance with the specified hue and standard hue number.
     * 
     * @param hueDegrees360 The hue angle in degrees (0-360)
     * @param stdHueNumCode The standard hue number (0-35)
     */
    // TODO: Consider using utility class to validate and convert hue numbers consistently
    public MonotriPage(int hueDegrees360, int stdHueNumCode) {
        hueNumCode = stdHueNumCode;
        if (stdHueNumCode ==0) {
            int kerflooey = 666; // 062225 trace debug only
        }
        // TODO 062225 call to createPureHueOstDot() needs pureHue ostdot already in place
        this.allOstDots[1] = createPureHueOstDot(hueDegrees360, stdHueNumCode);
        // TODO 062225 moved up here to correct sequence, but needs pureHue ostdot already in place
        Color[] sourceOfCMYKColors = CMYKValueCalc.createAllCMYKColors(this.allOstDots[1]);
        createAllOstDotsFromPureHueDegreeInt(hueDegrees360, stdHueNumCode);
        // assignCMYKColorToEachOstdot(); // TODO 062225 PROBLEM: This method does nothing - it's confusing - why is it here?
        organizeDrawOrder();

        // TODO 062225 PROBLEM: what does this get USED for? What then gets DONE with this? At first glance, it does nothing, but really....
        // TODO 062225 PROBLEM: method should be called BEFORE method createAllOstDotsFromPureHueDegreeInt (problem: null CMYKColors otherwise)
        // TODO 062225 PROBLEM: this would be in place for NEXT LOOP iteration -thus only hueNumCode==0 gets null CMYKColors
        // Color[] testCMYK = CMYKValueCalc.createAllCMYKColors(this.allOstDots[0]);

        if (stdHueNumCode == 13) {
            int kerflooey = 666;
        } // test trace only

    } // end constructor



    public int getHueNumCode() {
        return hueNumCode;
    }



    private void assignCMYKColorToEachOstdot() {
    } // end method assignCMYKColorToEachOstdot


    /**
     * Creates all color dots for this MonotriPage starting from a pure hue.
     * The process follows these steps:
     * 1. Create the pure hue dot
     * 2. Fill the clear-white series (pure hue to white)
     * 3. Fill the clear-black series (pure hue to black)
     * 4. Fill intermediate columns with calculated steps
     * 5. Fill the gray-scale column
     *
     * @param hueDegrees360 The hue angle in degrees (0-360)
     * @param stdHueNumCode The standard hue number (0-35)
     */
    private void createAllOstDotsFromPureHueDegreeInt(int hueDegrees360, int stdHueNumCode) {
        ColorPtOstDot pureHueOstDot = createPureHueOstDot(hueDegrees360, stdHueNumCode);

        fillAllOstDotsFromPureHuePt(pureHueOstDot);
        
    } // end method createAllOstDotsFromPureHueDegreeInt




    /**
     * Creates a pure hue color dot from the specified hue angle and standard hue number.
     * 
     * @param hueDegrees360 The hue angle in degrees (0-360)
     * @param stdHueNumCode The standard hue number (0-35)
     * @return A new ColorPtOstDot representing the pure hue color
     */
    private ColorPtOstDot createPureHueOstDot(int hueDegrees360, int stdHueNumCode) {
        int fillOrderNum = 1; // TODO 062225 don't want to just hard-code the parameter
        Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum); // TODO 062225 here is where cmykDisplayColor NEEDS to be set

        HLSParamBundle hlsParamBundle = new HLSParamBundle(hueDegrees360, 0.5, 1.0);
        ColorPtOstDot pureHueOstDot
                = ColorPtOstDot.createPureHueOstdotFromHLSPlus(hlsParamBundle, 1, stdHueNumCode, cmykDisplayColor);

        return pureHueOstDot;
    } // end method createPureHueOstDot




    /**
     * Fills all color dots for this MonotriPage starting from a pure hue color dot.
     * 
     * @param pureHueOstDot The pure hue color dot
     */
    private void fillAllOstDotsFromPureHuePt(ColorPtOstDot pureHueOstDot) {

        allOstDots[0] = pureHueOstDot; // dummy cell
        allOstDots[1] = pureHueOstDot;

        // now do all the columns - copied from MonoTriangleSetM2 in project RS301CleanModuleMouseDragxxx
        createShadowDotArray (pureHueOstDot); // TODO 051525 change method name, copied "as is"

    } // end method fillAllOstDotsFromPureHuePt




    /**
     * Creates the shadow dot array for this MonotriPage.
     * 
     * @param pureHuePt The pure hue color dot
     */
    private void createShadowDotArray (ColorPtOstDot pureHuePt) {

        fillClearWhites(pureHuePt);
        fillClearBlacks(pureHuePt);
        fillColumn3(pureHuePt);
        fillColumn4(pureHuePt);
        fillColumn5(pureHuePt);
        fillColumn6(pureHuePt);
        fillColumn7(pureHuePt);
        fillColumn8(pureHuePt);
        fillColumn9(pureHuePt);
        fillColumn10(pureHuePt);
        fillColumn11(pureHuePt);  // TODO 112725 examine: these are over-written by gray-scale ostDots?
        fillGrayScale(pureHuePt); // 051525 for now, creating DUPS in each hue // something missing 022622??

        int kerflooey = 666; // 022622 for tracing flow only

    } // end method createShadowDotArray

    /**
     * Fills the clear-white series of colors from pure hue to white.
     * Each color in this series represents a step towards white, with
     * percentages defined in clearWhitePercents array.
     * 
     * @param pureHuePt The pure hue color dot
     */
    private void fillClearWhites(ColorPtOstDot pureHuePt) {
        // use the constant to get upper limit for loop (and "startNum" = lastClearBlakDotNum +1)
        int lastClearBlackDotNum = EDGESIZE_PLUS_ONE - 2; // = 11
        int upperLimOfLoop = EDGESIZE_PLUS_ONE - 3; // = 10
        int stdHueNumCode = pureHuePt.getStdCodeHueNum();  // 051525 added

        // note
        for (int i = 1; i<= upperLimOfLoop; i++) // was i<= 6
        {
            int fillOrderNum = i+11; // 051525 fillOrderNum = indexNum+11 = i +11
            Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);  // TODO 062225 here is where cmykDisplayColor is set
            allOstDots[i+ lastClearBlackDotNum] = 
                    returnOstDotOnPure2White(fillOrderNum, pureHuePt, clearWhitePercents[i], stdHueNumCode, cmykDisplayColor);
        } // end loop
    } // end method fillClearWhites





    /**
     * Fills the clear-black series of colors from pure hue to black.
     * Each color in this series represents a step towards black, with
     * percentages defined in clearBlackPercents array.
     * 
     * @param pureHuePt The pure hue color dot
     */
    private void fillClearBlacks(ColorPtOstDot pureHuePt) {
        // use the constant to get upper limit for loop
        int lastClearBlackDotNum = EDGESIZE_PLUS_ONE - 2; // = 11
        int stdHueNumCode = pureHuePt.getStdCodeHueNum();  // 051525 added

        for (int i = 2; i<= lastClearBlackDotNum; i++) // 051525 fillOrderNum = indexNum = i
        {
            int fillOrderNum = i;
            Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum); // TODO 062225 here is where cmykDisplayColor is set
            allOstDots[i] =
                    returnOstDotOnPure2Black(fillOrderNum, pureHuePt, clearBlackPercents[i-1], stdHueNumCode, cmykDisplayColor);
        } // end loop
    } // end method fillClearBlacks




    /**
     * Fills the gray-scale column from white to black.
     * 
     * @param pureHuePt The pure hue color dot
     */
    private void fillGrayScale(ColorPtOstDot pureHuePt) {
        // use constant for upper limit of loop
        int upprLimForLoop = EDGESIZE_PLUS_ONE - 1; // = 12
        int startGrayNum = NUM_OSTDOTS_PLUS_ONE - EDGESIZE_PLUS_ONE;
        int someStdHueNumCode = pureHuePt.getStdCodeHueNum();
        //int startFillNum = 67-1; // because adding "index = i" in loop

        for (int i = 1; i<= upprLimForLoop; i++) // was i<=8,
        {
            double lum = (double)grayScaleLumPercents[i]/100;
            int fillOrderNum = i + startGrayNum;
            Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
            allOstDots[i+ startGrayNum] // revd 121615 // = i+66 = "one less than" first ostDotNum for grayscale (bottom-most)
                    = GrayColorPtOstDot.createFromCodeHueLumFillNum(someStdHueNumCode, lum, fillOrderNum, cmykDisplayColor);
        } // end loop
    } // end method fillGrayScale



    /**
     * Fills column 3 with intermediate colors between clear-black and clear-white.
     * Uses column3steps array to determine the position of colors in the column.
     *
     * @param pureHuePt The pure hue color dot used as reference
     */
    private void fillColumn3(ColorPtOstDot pureHuePt) {
        int firstFillOrderNum = 22;
        int someStdHueNumCode = pureHuePt.getStdCodeHueNum();
        int fillOrderNum = firstFillOrderNum;
        Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum); // TODO 062225 here is where cmykDisplayColor is set
        allOstDots[22]
                = percentStepDarkToLight(firstFillOrderNum, column3steps[1], allOstDots[3], allOstDots[13], someStdHueNumCode, cmykDisplayColor);
    }

    /**
     * Fills column 4 with intermediate colors between clear-black and clear-white.
     * Uses column4steps array to determine the positions of colors in the column.
     *
     * @param pureHuePt The pure hue color dot used as reference
     */
    private void fillColumn4(ColorPtOstDot pureHuePt) {
        int firstFillOrderNum = 23;
        int someStdHueNumCode = pureHuePt.getStdCodeHueNum();
        int fillOrderNum = firstFillOrderNum;
        Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[23]
                = percentStepDarkToLight(firstFillOrderNum, column4steps[1], allOstDots[4], allOstDots[14], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 1;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[24]
                = percentStepDarkToLight(firstFillOrderNum+1, column4steps[2], allOstDots[4], allOstDots[14], someStdHueNumCode, cmykDisplayColor);
    }

    /**
     * Fills column 5 with intermediate colors between clear-black and clear-white.
     * Uses column5steps array to determine the positions of colors in the column.
     *
     * @param pureHuePt The pure hue color dot used as reference
     */
    private void fillColumn5(ColorPtOstDot pureHuePt) {
        int firstFillOrderNum = 25;
        int someStdHueNumCode = pureHuePt.getStdCodeHueNum();
        int fillOrderNum = firstFillOrderNum;
        Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[25]
                = percentStepDarkToLight(firstFillOrderNum, column5steps[1], allOstDots[5], allOstDots[15], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 1;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[26]
                = percentStepDarkToLight(firstFillOrderNum+1, column5steps[2], allOstDots[5], allOstDots[15], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 2;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[27]
                = percentStepDarkToLight(firstFillOrderNum+2, column5steps[3], allOstDots[5], allOstDots[15], someStdHueNumCode, cmykDisplayColor);
    }

    /**
     * Fills column 6 with intermediate colors between clear-black and clear-white.
     * Uses column6steps array to determine the positions of colors in the column.
     *
     * @param pureHuePt The pure hue color dot used as reference
     */
    private void fillColumn6(ColorPtOstDot pureHuePt) {
        int firstFillOrderNum = 28;
        int someStdHueNumCode = pureHuePt.getStdCodeHueNum();
        int fillOrderNum = firstFillOrderNum;
        Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[28]
                = percentStepDarkToLight(firstFillOrderNum, column6steps[1], allOstDots[6], allOstDots[16], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 1;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[29]
                = percentStepDarkToLight(firstFillOrderNum+1, column6steps[2], allOstDots[6], allOstDots[16], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 2;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[30]
                = percentStepDarkToLight(firstFillOrderNum+2, column6steps[3], allOstDots[6], allOstDots[16], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 3;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[31]
                = percentStepDarkToLight(firstFillOrderNum+3, column6steps[4], allOstDots[6], allOstDots[16], someStdHueNumCode, cmykDisplayColor);
    }

    /**
     * Fills column 7 with intermediate colors between clear-black and clear-white.
     * Uses column7steps array to determine the positions of colors in the column.
     *
     * @param pureHuePt The pure hue color dot used as reference
     */
    private void fillColumn7(ColorPtOstDot pureHuePt) {
        int firstFillOrderNum = 32;
        int someStdHueNumCode = pureHuePt.getStdCodeHueNum();
        int fillOrderNum = firstFillOrderNum;
        Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[32]
                = percentStepDarkToLight(firstFillOrderNum, column7steps[1], allOstDots[7], allOstDots[17], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 1;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[33]
                = percentStepDarkToLight(firstFillOrderNum+1, column7steps[2], allOstDots[7], allOstDots[17], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 2;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[34]
                = percentStepDarkToLight(firstFillOrderNum+2, column7steps[3], allOstDots[7], allOstDots[17], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 3;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[35]
                = percentStepDarkToLight(firstFillOrderNum+3, column7steps[4], allOstDots[7], allOstDots[17], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 4;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[36]
                = percentStepDarkToLight(firstFillOrderNum+4, column7steps[5], allOstDots[7], allOstDots[17], someStdHueNumCode, cmykDisplayColor);
    }

    /**
     * Fills column 8 with intermediate colors between clear-black and clear-white.
     * Uses column8steps array to determine the positions of colors in the column.
     *
     * @param pureHuePt The pure hue color dot used as reference
     */
    private void fillColumn8(ColorPtOstDot pureHuePt) {
        int firstFillOrderNum = 37;
        int someStdHueNumCode = pureHuePt.getStdCodeHueNum();
        int fillOrderNum = firstFillOrderNum;
        Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[37]
                = percentStepDarkToLight(firstFillOrderNum, column8steps[1], allOstDots[8], allOstDots[18], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 1;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[38]
                = percentStepDarkToLight(firstFillOrderNum+1, column8steps[2], allOstDots[8], allOstDots[18], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 2;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[39]
                = percentStepDarkToLight(firstFillOrderNum+2, column8steps[3], allOstDots[8], allOstDots[18], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 3;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[40]
                = percentStepDarkToLight(firstFillOrderNum+3, column8steps[4], allOstDots[8], allOstDots[18], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 4;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[41]
                = percentStepDarkToLight(firstFillOrderNum+4, column8steps[5], allOstDots[8], allOstDots[18], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 5;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[42]
                = percentStepDarkToLight(firstFillOrderNum+5, column8steps[6], allOstDots[8], allOstDots[18], someStdHueNumCode, cmykDisplayColor);
    }

    /**
     * Fills column 9 with intermediate colors between clear-black and clear-white.
     * Uses column9steps array to determine the positions of colors in the column.
     *
     * @param pureHuePt The pure hue color dot used as reference
     */
    private void fillColumn9(ColorPtOstDot pureHuePt) {
        int firstFillOrderNum = 43;
        int someStdHueNumCode = pureHuePt.getStdCodeHueNum();
        int fillOrderNum = firstFillOrderNum;
        Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[43]
                = percentStepDarkToLight(firstFillOrderNum, column9steps[1], allOstDots[9], allOstDots[19], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 1;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[44]
                = percentStepDarkToLight(firstFillOrderNum+1, column9steps[2], allOstDots[9], allOstDots[19], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 2;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[45]
                = percentStepDarkToLight(firstFillOrderNum+2, column9steps[3], allOstDots[9], allOstDots[19], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 3;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[46]
                = percentStepDarkToLight(firstFillOrderNum+3, column9steps[4], allOstDots[9], allOstDots[19], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 4;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[47]
                = percentStepDarkToLight(firstFillOrderNum+4, column9steps[5], allOstDots[9], allOstDots[19], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 5;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[48]
                = percentStepDarkToLight(firstFillOrderNum+5, column9steps[6], allOstDots[9], allOstDots[19], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 6;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[49]
                = percentStepDarkToLight(firstFillOrderNum+6, column9steps[7], allOstDots[9], allOstDots[19], someStdHueNumCode, cmykDisplayColor);
    }

    /**
     * Fills column 10 with intermediate colors between clear-black and clear-white.
     * Uses column10steps array to determine the positions of colors in the column.
     *
     * @param pureHuePt The pure hue color dot used as reference
     */
    private void fillColumn10(ColorPtOstDot pureHuePt) {
        int firstFillOrderNum = 50;
        int someStdHueNumCode = pureHuePt.getStdCodeHueNum();
        int fillOrderNum = firstFillOrderNum;
        Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[50]
                = percentStepDarkToLight(firstFillOrderNum, column10steps[1], allOstDots[10], allOstDots[20], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 1;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[51]
                = percentStepDarkToLight(firstFillOrderNum+1, column10steps[2], allOstDots[10], allOstDots[20], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 2;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[52]
                = percentStepDarkToLight(firstFillOrderNum+2, column10steps[3], allOstDots[10], allOstDots[20], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 3;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[53]
                = percentStepDarkToLight(firstFillOrderNum+3, column10steps[4], allOstDots[10], allOstDots[20], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 4;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[54]
                = percentStepDarkToLight(firstFillOrderNum+4, column10steps[5], allOstDots[10], allOstDots[20], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 5;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[55]
                = percentStepDarkToLight(firstFillOrderNum+5, column10steps[6], allOstDots[10], allOstDots[20], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 6;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[56]
                = percentStepDarkToLight(firstFillOrderNum+6, column10steps[7], allOstDots[10], allOstDots[20], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 7;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[57]
                = percentStepDarkToLight(firstFillOrderNum+7, column10steps[8], allOstDots[10], allOstDots[20], someStdHueNumCode, cmykDisplayColor);
    }

    /**
     * Fills column 11 with intermediate colors between clear-black and clear-white.
     * Uses column11steps array to determine the positions of colors in the column.
     *
     * @param pureHuePt The pure hue color dot used as reference
     */
    private void fillColumn11(ColorPtOstDot pureHuePt) {
        int firstFillOrderNum = 58;
        int someStdHueNumCode = pureHuePt.getStdCodeHueNum();
        int fillOrderNum = firstFillOrderNum;
        Color cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[58]
                = percentStepDarkToLight(firstFillOrderNum, column11steps[1], allOstDots[11], allOstDots[21], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 1;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[59]
                = percentStepDarkToLight(firstFillOrderNum+1, column11steps[2], allOstDots[11], allOstDots[21], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 2;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[60]
                = percentStepDarkToLight(firstFillOrderNum+2, column11steps[3], allOstDots[11], allOstDots[21], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 3;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[61]
                = percentStepDarkToLight(firstFillOrderNum+3, column11steps[4], allOstDots[11], allOstDots[21], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 4;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[62]
                = percentStepDarkToLight(firstFillOrderNum+4, column11steps[5], allOstDots[11], allOstDots[21], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 5;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[63]
                = percentStepDarkToLight(firstFillOrderNum+5, column11steps[6], allOstDots[11], allOstDots[21], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 6;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[64]
                = percentStepDarkToLight(firstFillOrderNum+6, column11steps[7], allOstDots[11], allOstDots[21], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 7;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[65]
                = percentStepDarkToLight(firstFillOrderNum+7, column11steps[8], allOstDots[11], allOstDots[21], someStdHueNumCode, cmykDisplayColor);
        fillOrderNum = firstFillOrderNum + 8;
        cmykDisplayColor = CMYKValueCalc.getCMYKDisplayColor(fillOrderNum);
        allOstDots[66]
                = percentStepDarkToLight(firstFillOrderNum+8, column11steps[9], allOstDots[11], allOstDots[21], someStdHueNumCode, cmykDisplayColor);
    }

    /**
     * Creates a color at a specified percentage between a dark and light color.
     * This is used to fill intermediate positions in the triangle between
     * the clear-black and clear-white series.
     *
     * @param fillOrderNum Position number in the triangle (1-78)
     * @param stepPercent Percentage step from dark to light (0-100)
     * @param darkPt The darker color endpoint
     * @param lightPt The lighter color endpoint
     * @param someStdHueNumCode The standard hue number (0-35)
     * @param cmykDisplayColor The CMYK display color
     * @return A new ColorPtOstDot at the calculated position
     */
    private ColorPtOstDot percentStepDarkToLight(int fillOrderNum, int stepPercent, 
                                    ColorPtOstDot darkPt, ColorPtOstDot lightPt, int someStdHueNumCode, Color cmykDisplayColor) {
        // 051525 added
        ColumnStepWhiteBlackParams paramBundle = ColorPtOstDot.createParamBundleFromFillOrderNum(fillOrderNum);
        // Convert from MonotriPage hue (0-35) to ColorPtOstDot hue (1-36) // 061725 incrementing by 1 produces hueNum >35 -> error
        int ostDotHueNumCode = someStdHueNumCode; // TODO 061625 already corrected this RIGHT HERE: incrementing by 1 produces hueNum >35 -> error
        if (ostDotHueNumCode > 35) {
            int kerflooey = 666; // 061725for debug only
        }
        System.out.println("DEBUG: percentStepDarkToLight - fillOrderNum: " + fillOrderNum + ", MonotriPage hue: " + someStdHueNumCode + ", ColorPtOstDot hue: " + ostDotHueNumCode);
        OstHueColumnStep someOHCS = new OstHueColumnStep(ostDotHueNumCode, paramBundle, fillOrderNum);
        double baseR = darkPt.getTheRDec();
        double baseG = darkPt.getTheGDec();
        double baseB = darkPt.getTheBDec();

        double highR = lightPt.getTheRDec();
        double highG = lightPt.getTheGDec();
        double highB = lightPt.getTheBDec();


        double diffR = highR - baseR;
        double diffG = highG - baseG;
        double diffB = highB - baseB;

        double incrementR = diffR*(double)stepPercent/100;
        double incrementG = diffG*(double)stepPercent/100;
        double incrementB = diffB*(double)stepPercent/100;

        double newR = baseR+incrementR;
        double newG = baseG+incrementG;
        double newB = baseB+incrementB;
        if (newR > 1.0) {newR= 1.0;}
        if (newG > 1.0) {newG= 1.0;}
        if (newB > 1.0) {newB= 1.0;}

        if (newR < 0.0) {newR= 0.0;}
        if (newG < 0.0) {newG= 0.0;}
        if (newB < 0.0) {newB= 0.0;}

        ColorPtArb extColorPtArb = new ColorPtArb(newR, newG, newB);
        ColorPtOstDot extColorPtOstDot = new ColorPtOstDot(extColorPtArb, someOHCS, cmykDisplayColor);

        return extColorPtOstDot;
    } // end method percentStepDarkToLight

    /**
     * Creates a color point on the line from pure hue to black.
     * The position is determined by percentBlack, which specifies how
     * far along this line the color should be.
     *
     * @param fillOrderNum Position number in the triangle (1-78)
     * @param pHueColorPt The pure hue color point
     * @param percentBlack Percentage towards black (0-100)
     * @param stdHueNum The standard hue number (0-35)
     * @param cmykDisplayColor The CMYK display color
     * @return A new ColorPtOstDot at the calculated position
     */
    public ColorPtOstDot returnOstDotOnPure2Black(int fillOrderNum, ColorPtArb pHueColorPt, 
            int percentBlack, int stdHueNum, Color cmykDisplayColor) {
        // note that will accept minimal colorPts
        double lumRatio;                                // this NEVERgets used in calcs

        double blackFromOne = 0;
        blackFromOne = (double)percentBlack/100; // this is a real BUG if you don't cast to double!!
        double blackFactor = (1.0 - blackFromOne);     // this gets used in calcs
        // System.out.println ("blackFactor is:   " + blackFactor);
        // double arbColorPtLum = percentBlack/100;     // this gets used in calcs
        double pureHuePtLum = calcLum(pHueColorPt);     // this gets used in calcs // TODO 051525 DUP METHOD "calcLum" NEEDS FIX

        // should never happen that (pureHuePtLum == 0)
        if (pureHuePtLum == 0) {pureHuePtLum = .5;
            System.out.println ("error with returnPtOnPure2Black pureHuePtLum");}

        double rExt = blackFactor*pHueColorPt.getTheRDec();
        double gExt = blackFactor*pHueColorPt.getTheGDec();
        double bExt = blackFactor*pHueColorPt.getTheBDec();

        if (rExt > 1.0) {rExt= 1.0;}
        if (gExt > 1.0) {gExt= 1.0;}
        if (bExt > 1.0) {bExt= 1.0;}

        if (rExt < 0.0) {rExt= 0.0;}
        if (gExt < 0.0) {gExt= 0.0;}
        if (bExt < 0.0) {bExt= 0.0;}

        ColorPtArb extColorPtArb = new ColorPtArb(rExt, gExt, bExt);
        OstHueColumnStep someOHCS = new OstHueColumnStep(stdHueNum, ColorPtOstDot.createParamBundleFromFillOrderNum(fillOrderNum), fillOrderNum);
        ColorPtOstDot extColorPtOstDot = new ColorPtOstDot(extColorPtArb, someOHCS, cmykDisplayColor);

        return extColorPtOstDot;
    } // end method returnPtOnPure2Black

    /**
     * Creates a color point on the line from pure hue to white.
     * The position is determined by percentWhite, which specifies how
     * far along this line the color should be.
     *
     * @param fillOrderNum Position number in the triangle (1-78)
     * @param pHueColorPt The pure hue color point
     * @param percentWhite Percentage towards white (0-100)
     * @param stdHueNum The standard hue number (0-35)
     * @param cmykDisplayColor The CMYK display color
     * @return A new ColorPtOstDot at the calculated position
     */
    public ColorPtOstDot returnOstDotOnPure2White(int fillOrderNum, ColorPtArb pHueColorPt, 
            int percentWhite, int stdHueNum, Color cmykDisplayColor) {
        // note that will accept minimal colorPts
        double lumRatio;                                // this NEVERgets used in calcs

        double whiteIncrement = (double)percentWhite/100;
        // this is for keeping "proportionality" in the mid-coordinate
        // the max coord is always 100 and the min coord is always zero
        double colorAboveWhiteFactor = 1.0 - whiteIncrement;

        // System.out.println(" returnPtOnPure2White pureHuePtLum " +pureHuePtLum);
        // System.out.println(" whiteIncrement is: " + whiteIncrement);

        // colorAboveWhiteFactor keeps "proportionality" in the mid-coordinate
        // need to have mid coords increase w proportionate relation to max coord
        double rExt = whiteIncrement + colorAboveWhiteFactor*pHueColorPt.getTheRDec();
        double gExt = whiteIncrement + colorAboveWhiteFactor*pHueColorPt.getTheGDec();
        double bExt = whiteIncrement + colorAboveWhiteFactor*pHueColorPt.getTheBDec();

        if (rExt > 1.0) {rExt= 1.0;}
        if (gExt > 1.0) {gExt= 1.0;}
        if (bExt > 1.0) {bExt= 1.0;}

        if (rExt < 0.0) {rExt= 0.0;}
        if (gExt < 0.0) {gExt= 0.0;}
        if (bExt < 0.0) {bExt= 0.0;}

        ColorPtArb extColorPtArb = new ColorPtArb(rExt, gExt, bExt);
        OstHueColumnStep someOHCS = new OstHueColumnStep(stdHueNum, ColorPtOstDot.createParamBundleFromFillOrderNum(fillOrderNum), fillOrderNum);
        ColorPtOstDot extColorPtOstDot = new ColorPtOstDot(extColorPtArb, someOHCS, cmykDisplayColor);

        return extColorPtOstDot;
    } // end method returnPtOnPure2White

    /**
     * Calculates the luminance of a color point.
     * 
     * @param aColorPt The color point
     * @return The luminance value (0.0-1.0)
     */
    public double calcLum(ColorPtArb aColorPt) {
        double lumValue;

        lumValue = RGBCubeModel.calcLum(aColorPt.getTheRDec(), aColorPt.getTheGDec(), aColorPt.getTheBDec());
        return lumValue;

    } // end method calcLum

    /**
     * Gets a color by its fill order number.
     * Fill order numbers range from 1-78 and define positions in the triangle:
     * - 1: Pure hue color
     * - 2-11: Clear-black series
     * - 12-21: Clear-white series
     * - 22+: Intermediate columns
     * - 67-78: Gray-scale series
     *
     * @param fillOrder The fill order number (1-78)
     * @return The ColorPtOstDot at that position, or null if invalid
     */
    public ColorPtOstDot getColorByFillOrder(int fillOrder) {
        // 072025 took out - extreme clutter
        // System.out.println("DEBUG: MonotriPage.getColorByFillOrder called with fillOrder: " + fillOrder);
        if (fillOrder < 1 || fillOrder >= NUM_OSTDOTS_PLUS_ONE) {
            System.out.println("DEBUG: Invalid fillOrder: " + fillOrder + " (must be 1-" + (NUM_OSTDOTS_PLUS_ONE-1) + ")");
            return null;
        }
        ColorPtOstDot color = allOstDots[fillOrder];
        if (color != null) {
            /* // 072025 took out - extreme clutter
            System.out.println("DEBUG: Found color at fillOrder " + fillOrder + 
                " with hue " + color.getOstHCS().getStdCodeHueNum() +
                " and fillOrderNum " + color.getOstHCS().getFillOrderNum());
             */
        } else {
            System.out.println("DEBUG: No color found at fillOrder " + fillOrder);
        }
        return color;
    } // end method getColorByFillOrder


    /**
     * Gets all OstDots on this page whose RGB luminance falls within the given value
     * range, in ascending fill order.
     *
     * Scans fill orders 1-66 only, excluding the gray-scale column (67-78), which
     * reproduces the candidate domain of the legacy
     * FacadeDomainOstDot3D.getPrelimSetOfOstDotsByValueRange query.
     * The returned list is unpadded: index 0 is the first candidate in fill order.
     *
     * @param valueCtr The target RGB luminance (0.0-1.0)
     * @param valueRangePlusMinus The half-range tolerance around valueCtr
     * @return Ordered list of ColorPtOstDots within the range; empty if none
     */
    public List<ColorPtOstDot> getOstDotsByValueRange(double valueCtr, double valueRangePlusMinus) {
        List<ColorPtOstDot> closeValueOstDots = new ArrayList<ColorPtOstDot>();
        for (int i = 1; i <= LAST_NON_GRAY_FILL_ORDER; i++) {
            ColorPtOstDot candidate = allOstDots[i];
            if (candidate != null
                    && Math.abs(candidate.getRGBLum() - valueCtr) <= valueRangePlusMinus) {
                closeValueOstDots.add(candidate);
            }
        }
        return closeValueOstDots;
    } // end method getOstDotsByValueRange




    /**
     * Gets a color by its column and step coordinates (OHCS notation).
     * Converts the column and step to Wseries and Bseries, then retrieves the color.
     *
     * @param columnNum The column number (1-12)
     * @param stepNum The step number within the column (1 to columnNum, not 0-based)
     * @return The ColorPtOstDot at that position, or null if invalid
     */
    public ColorPtOstDot getColorByColumnStep(int columnNum, int stepNum) {
        // Convert column and step to Wseries and Bseries
        int[] wseriesBseries = OstPosition.convertColumnStepToWseriesBseries(columnNum, stepNum);
        int wseries = wseriesBseries[0];
        int bseries = wseriesBseries[1];

        // Use existing method to get color by Wseries and Bseries
        return getColorByWseriesBseries(wseries, bseries);
    } // end method getColorByColumnStep



    /**
     * Gets a color by its White and Black series coordinates (Wseries and Bseries).
     *
     * @param someWseries The White series coordinate (0-11)
     * @param someBseries The Black series coordinate (0-11)
     * @return The ColorPtOstDot at that position, or null if invalid
     */
    public ColorPtOstDot getColorByWseriesBseries(int someWseries, int someBseries) {
        // first get fillOrderNum
        int fillOrderNum = calcFillOrderNumFromWseriesBseries(someWseries, someBseries);

        return getColorByFillOrder(fillOrderNum);
    } // end method getColorByWseriesBseries



    public static int calcFillOrderNumFromWseriesBseries(int someWseries, int someBseries) {
        // check for valid input values
        if (someWseries < 0 || someWseries > 11) {
            System.out.println("DEBUG: Invalid someWseries: " + someWseries + " (must be 0-11)");
            return -1;
        }
        if (someBseries < 0 || someBseries > 11) {
            System.out.println("DEBUG: Invalid someBseries: " + someBseries + " (must be 0-11)");
            return -1;
        } //end IF - end tests for valid input

        int fillOrderNum = -1; // pre-condition: start calculation with invalid return value


        // Note that the fillOrderNum is an odd pattern mathematically - must refer to the triangle diagram to see how it goes "column by column"
        // TODO 080225 check method logic - approximately replicates methods for clearBlack series, clearWseries, and then filling columns
        if(someWseries == 0 && (someBseries <12)) { // clearBlack series but not including gray-scale (pure black)
            if(someWseries == 0 && (someBseries ==11)) { // 091125 added: handle return correctly for BLACK ONLY (Wseries=0, Bseries=11)
                fillOrderNum =67;
                return fillOrderNum;
            }
            fillOrderNum = someBseries + 1; // starts with pure hue
            return fillOrderNum;
        } // end IF
        if(someBseries == 0 && (someWseries <11) && (someWseries != 0)) { // clearWhite series but not including gray-scale (pure white)
            fillOrderNum = 11 + someWseries; // starts with someWseries == 1 (first color going up series after pure white)
            return fillOrderNum;
        } // end IF


        // need to work "column by column", so first need to get the column number. Note: clearB & clearW series above take care of columns 1 & 2
        int columnNum = Math.abs(someBseries + someWseries) +1;

        // series of IF blocks are mutually exclusive, handle columns 3-11, then gray-scale column 12 which gets its own math logic
        if((columnNum == 3) && (someWseries >0) && (someBseries >0)) { // Note: conditions 2 & 3 exclude clearB and clearW at bottom & top of column
            fillOrderNum = 21 + someWseries; // only one ostDot in column 3, but still true of all ostDots in column 4
            return fillOrderNum;
        } // end IF

        if((columnNum == 4) && (someWseries >0) && (someBseries >0)) { // Note: conditions 2 & 3 exclude clearB and clearW at bottom & top of column
            fillOrderNum = 22 + someWseries; // true of all ostDots in column 4
            return fillOrderNum;
        } // end IF

        if((columnNum == 5) && (someWseries >0) && (someBseries >0)) { // Note: conditions 2 & 3 exclude clearB and clearW at bottom & top of column
            fillOrderNum = 24 + someWseries; // true of all ostDots in column 5
            return fillOrderNum;
        } // end IF

        if((columnNum == 6) && (someWseries >0) && (someBseries >0)) { // Note: conditions 2 & 3 exclude clearB and clearW at bottom & top of column
            fillOrderNum = 27 + someWseries; // true of all ostDots in column 6
            return fillOrderNum;
        } // end IF

        if((columnNum == 7) && (someWseries >0) && (someBseries >0)) { // Note: conditions 2 & 3 exclude clearB and clearW at bottom & top of column
            fillOrderNum = 31 + someWseries; // true of all ostDots in column 7
            return fillOrderNum;
        } // end IF

        if((columnNum == 8) && (someWseries >0) && (someBseries >0)) { // Note: conditions 2 & 3 exclude clearB and clearW at bottom & top of column
            fillOrderNum = 36 + someWseries; // true of all ostDots in column 8
            return fillOrderNum;
        } // end IF

        if((columnNum == 9) && (someWseries >0) && (someBseries >0)) { // Note: conditions 2 & 3 exclude clearB and clearW at bottom & top of column
            fillOrderNum = 42 + someWseries; // true of all ostDots in column 9
            return fillOrderNum;
        } // end IF

        if((columnNum == 10) && (someWseries >0) && (someBseries >0)) { // Note: conditions 2 & 3 exclude clearB and clearW at bottom & top of column
            fillOrderNum = 49 + someWseries; // true of all ostDots in column 10
            return fillOrderNum;
        } // end IF

        if((columnNum == 11) && (someWseries >0) && (someBseries >0)) { // Note: conditions 2 & 3 exclude clearB and clearW at bottom & top of column
            fillOrderNum = 57 + someWseries; // true of all ostDots in column 11
            return fillOrderNum;
        } // end IF

        // 091125 NOTE: flow never reaches this IF-block for black (Wseries=0, Bseries=11) -can produce bug, see line 764 for bug-catch
        if(someWseries + someBseries ==11) { // true of all gray-scale ostDots (last column = column 12)
            fillOrderNum = 67 + someWseries; // starts with someWseries == 0 (for pure black, then going up ENTIRE SERIES including pure white)
        } // end IF

        return fillOrderNum;
    } // end method calcFillOrderNumFromWseriesBseries


    /**
     * Finds the closest ColorPtOstDot in this page to the given RGB color.
     * 
     * @param color The RGB color to match
     * @return The closest ColorPtOstDot in this page
     */
    public ColorPtOstDot findClosestColorPoint(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("Color cannot be null");
        }

        ColorPtOstDot closest = allOstDots[1]; // Start with pure hue
        double minDistance = Double.MAX_VALUE;

        for (ColorPtOstDot dot : allOstDots) {
            if (dot != null) {
                double distance = colorDistance(color, dot);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = dot;
                }
            }
        }

        return closest;
    }

    private double colorDistance(Color c1, Color c2) {
        double dr = c1.getRed() - c2.getRed();
        double dg = c1.getGreen() - c2.getGreen();
        double db = c1.getBlue() - c2.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    /**
     * Organizes colors by their draw order sequence.
     * This method populates the drawOrderColorPts array based on each color's
     * drawOrderNum, enabling systematic rendering of the triangle from:
     * - Pure hue at the top (drawOrderNum = 1)
     * - Through clear-black and clear-white series
     * - To the gray-scale series at the bottom
     */
    private void organizeDrawOrder() {
        // Sort colors by their draw order
        for (ColorPtOstDot color : allOstDots) {
            if (color != null) {
                OstHueColumnStep step = color.getOstHCS();
                if (step != null) {
                    int drawOrder = step.getDrawOrderNum();
                    System.out.println("Color at fill order " + color.getOstHCS().getFillOrderNum() +
                                    " has draw order " + drawOrder); // Debug print
                    if (drawOrder > 0 && drawOrder < NUM_OSTDOTS_PLUS_ONE) {
                        drawOrderColorPts[drawOrder] = color;
                    }
                }
            }
        }
    }

    public ColorPtOstDot[] getDrawOrderColorPts() {
        return drawOrderColorPts;
    } // end method getDrawOrderColorPts


    public ColorPtOstDot getPureHueOstDot() {
        return allOstDots[1];
    }

    /**
     * Gets the total number of OstDots in this MonotriPage.
     * @return The total number of OstDots (78)
     */
    public int getNumOstDots() {
        return NUM_OSTDOTS_PLUS_ONE - 1; // Subtract 1 since array is 1-based
    }

    /**
     * Checks if the input ColorPtOstDot has the same hue as this MonotriPage.
     * Both MonotriPage and ColorPtOstDot.getStdCodeHueNum() use 0-35 range internally.
     * @param someOstDot The ColorPtOstDot to check
     * @return true if the hues match, false otherwise
     */
    public boolean checkInputOstDotForHue(ColorPtOstDot someOstDot) {
        if (someOstDot == null) {
            return false;
        }
        // Both use internal hue numbers (0-35), so direct comparison
        return someOstDot.getStdCodeHueNum() == this.hueNumCode;
    }
} // end class MonotriPage
