package com.csurf.ostwald.domainmonotri.model;

import com.csurf.ostwald.domaingeo.model.ColorPtArb;
import com.csurf.ostwald.domaingeo.model.RGBCubeModel;

import java.awt.*;

/**
 * Service class for CMYK value calculations.
 * Each MonotriPage uses this class to calculate CMYK values for each ColorPtOstDot in the triangle.
 * Each MonotriPage uses this class to create AWT colors for each ColorPtOstDot in the triangle.
 * Method calcNonPureHueCMYK returns the clear-black Color that defines the CMYK triangle
 * Method fillTriangle is similar to MonotriPage method fillTriangle.
 * Uses singleton pattern to ensure only one instance exists.
 */
public class CMYKValueCalc {
    private static final int NUM_OSTDOTS_PLUS_ONE = 79;
    private static final int EDGESIZE_PLUS_ONE = 13;
        // 062025 copied from project RS302DotDrag3DLumToCMYK_032023v0d unchanged, same NAME as in class CMYKValueCalcSingleton
    // luminance of each pure hue when printed to CMYK (adjusted 010423 from previous guesstimates done by eye -from printed screen-captures)
    // NOTE: array is indexed by user-facing hue number (1-36), not internal hue number (0-35), so first entry is filler (ZERO cell)
    private static double[] lumByHueNumArray9 = {0.93, // 0-0 // first entry "ZERO HUE" filler, not used (set = hue 36)
            0.94, 0.87, 0.82,  0.78,  0.71,  0.68, // 1-6
            0.69,  0.67,  0.57,  0.51, 0.48, // 7-11
            0.44, 0.46, 0.51, 0.49, 0.44, // 12-16
            0.35, 0.27, 0.24, 0.21, 0.18, // 17-21
            0.19, 0.32, 0.41, 0.55, 0.55, // 22-26
            0.50, 0.53, 0.54, 0.57, 0.62, 0.73,
            0.78, 0.84, 0.90, // 33-35
            0.93 // 36-36
    }; // end member array "lumByHueNumArray9"

    // 062025 copied from class MonotriPage in this project
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

    private static Color pureCMYKColor;
    private static final Color[] allCMYKColors = new Color[NUM_OSTDOTS_PLUS_ONE];



    // constructor
    private CMYKValueCalc() {
    } // end constructor



    /** Each color has a percentage of pure Hue, and a percentage of white and a percentage of black, and the CMYK
     * luminance can be calculated as the sum of these.
     * Actually the calculation is even simpler, since the black has no luminance.
     *
     * @param someOstDot
     * @return Decimal in the 0-1 range
     */
    public static double calcCMYKValue(ColorPtOstDot someOstDot) {
        // TODO 021123 I think the source of the BUG is the CAST to (Color)
        int stdGUIhueNum = someOstDot.getStdCodeHueNum() +1;
        double redCoord = someOstDot.getTheRDec();
        double greenCoord = someOstDot.getTheGDec();
        double blueCoord = someOstDot.getTheBDec();

        double lumFactor = getCMYKLumByHueNumArray()[stdGUIhueNum]; // comparative luminance level of physical pure hue (0-1.0)
        double minimumRGBCoordDecimal = minFunctionRGBCoords(redCoord, greenCoord, blueCoord); // for percentage amt of white, no adjustment
        double whiteAmtLum = minimumRGBCoordDecimal; // not multiplied by lumFactor or any other factor, since white lum = 1.0
        double someAbSat = someOstDot.getAbSat();
        double hueAmtLum = someAbSat*lumFactor; // amt of color but reduced by its luminance

        // for testing only - template heroEye072225BrownMagenta-v1 cheekCurveLwr has fillOrderNum = 26 hueNum = 8
        // for testing only - template heroEye081525yelloBukuCyan-v1 cheekCurveLwr has fillOrderNum = 4 hueNum = 27
        int theFillOrderNum = someOstDot.getFillOrderNum();
        if (stdGUIhueNum == 27 && theFillOrderNum == 4) {
            int kerflooeyLum = 666;
        }

        double totalLum = whiteAmtLum + hueAmtLum;

        // for testing only - totalLum should never exceed 1.0
        if (totalLum > 1.0) {
            int kerflooeyLumExceeds = 666;
        }
        return totalLum;

    } // end method calcCMYKValue



    private static double minFunctionRGBCoords(double redCoord, double greenCoord, double blueCoord) {
        double minValue;

        if (redCoord < greenCoord) { minValue = redCoord;}
        else { minValue = greenCoord;}
        if (blueCoord < minValue) { minValue = blueCoord;}
        return minValue;
    } // end method minFunctionRGBCoords


    private static double[] getCMYKLumByHueNumArray() {
        return lumByHueNumArray9;
    } // end method getCMYKLumByHueNumArray


    public static Color[] createAllCMYKColors(ColorPtOstDot pHueColorPt) {
        fillAllCMYKColors(pHueColorPt);
        return allCMYKColors;
    } // end method createAllCMYKColors



    /**
     * Skeleton method for creating all colors for the CMYK triangle for one hue
     * @param pHueColorPt
     */
    public static void fillAllCMYKColors(ColorPtOstDot pHueColorPt) {
        //pureCMYKColor = calcNonPureHueCMYK(pHueColorPt); // 062025 probably not needed at all

        allCMYKColors[0] = calcNonPureHueCMYK(pHueColorPt); // dummy cell
        allCMYKColors[1] = calcNonPureHueCMYK(pHueColorPt); // TODO 062225 already done in MonotriPage constructor
        int kerflooety = 666;
        // allCMYKColors[0] = allCMYKColors[1]; // dummy cell
        fillClearBlackColors(allCMYKColors[1]);
        fillClearWhiteColors(allCMYKColors[1]);
        fillGrayScaleColors();
        fillColumn3Colors();
        fillColumn4Colors();
        fillColumn5Colors();
        fillColumn6Colors();
        fillColumn7Colors();
        fillColumn8Colors();
        fillColumn9Colors();
        fillColumn10Colors();
        fillColumn11Colors();

        int kerflooey = 666;
    } // end method fillAllCMYKColors


    private static void fillClearBlackColors(Color pHueColorPt) {
        // use the EDGESIZE_PLUS_ONE constant to get upper limit for loop
        int lastClearBlackIndexNum = EDGESIZE_PLUS_ONE - 2; // = 11

        for (int i = 2; i<= lastClearBlackIndexNum; i++) // 051525 fillOrderNum = indexNum = i
        {
            int fillOrderNum = i;
            allCMYKColors[i] = returnColorGoingToBlack(pHueColorPt, clearBlackPercents[i-1]);
        } // end loop
    }

    private static void fillClearWhiteColors(Color nonPureHueStartPt) {
        // use the constant to get upper limit for loop (and "startNum" = lastClearBlakDotNum +1)
        int lastClearBlackDotNum = EDGESIZE_PLUS_ONE - 2; // = 11
        int upperLimOfLoop = EDGESIZE_PLUS_ONE - 3; // = 10

        // note
        for (int i = 1; i<= upperLimOfLoop; i++) // was i<= 6
        {
            int fillOrderNum = i+11; // 051525 fillOrderNum = indexNum+11 = i +11
            allCMYKColors[i+ lastClearBlackDotNum] =
                    returnColorGoingToWhite(nonPureHueStartPt, clearWhitePercents[i]);
        } // end loop

    } // end method fillClearWhiteColors




    /**
     * Fills the gray-scale series of colors from black to white.
     * Note that this fills the entire series, so clear-black and clear-white series end before reaching grayscale
     * Although these are the same colors for every hue, these colors are not cached, so they are recalculated each hue
     */
    private static void fillGrayScaleColors() {
        // use constant for upper limit of loop
        int upprLimForLoop = EDGESIZE_PLUS_ONE - 1; // = 12
        int startGrayNum = NUM_OSTDOTS_PLUS_ONE - EDGESIZE_PLUS_ONE; // = 66

        for (int i = 1; i<= upprLimForLoop; i++)
        {
            double lum = (double)grayScaleLumPercents[i]/100;
            int equalRGBCoords = (int) (255*lum);
            allCMYKColors[i+ startGrayNum] = new Color(equalRGBCoords, equalRGBCoords, equalRGBCoords);
        } // end loop

    } // end method fillGrayScaleColors



    private static void fillColumn11Colors() {
        allCMYKColors[58] = percentStepDarkToLight(allCMYKColors[11], allCMYKColors[21], column11steps[1]);
        allCMYKColors[59] = percentStepDarkToLight(allCMYKColors[11], allCMYKColors[21], column11steps[2]);
        allCMYKColors[60] = percentStepDarkToLight(allCMYKColors[11], allCMYKColors[21], column11steps[3]);
        allCMYKColors[61] = percentStepDarkToLight(allCMYKColors[11], allCMYKColors[21], column11steps[4]);
        allCMYKColors[62] = percentStepDarkToLight(allCMYKColors[11], allCMYKColors[21], column11steps[5]);
        allCMYKColors[63] = percentStepDarkToLight(allCMYKColors[11], allCMYKColors[21], column11steps[6]);
        allCMYKColors[64] = percentStepDarkToLight(allCMYKColors[11], allCMYKColors[21], column11steps[7]);
        allCMYKColors[65] = percentStepDarkToLight(allCMYKColors[11], allCMYKColors[21], column11steps[8]);
        allCMYKColors[66] = percentStepDarkToLight(allCMYKColors[11], allCMYKColors[21], column11steps[9]);
    } // end method fillColumn11Colors



    private static void fillColumn10Colors() {
        allCMYKColors[50] = percentStepDarkToLight(allCMYKColors[10], allCMYKColors[20], column10steps[1]);
        allCMYKColors[51] = percentStepDarkToLight(allCMYKColors[10], allCMYKColors[20], column10steps[2]);
        allCMYKColors[52] = percentStepDarkToLight(allCMYKColors[10], allCMYKColors[20], column10steps[3]);
        allCMYKColors[53] = percentStepDarkToLight(allCMYKColors[10], allCMYKColors[20], column10steps[4]);
        allCMYKColors[54] = percentStepDarkToLight(allCMYKColors[10], allCMYKColors[20], column10steps[5]);
        allCMYKColors[55] = percentStepDarkToLight(allCMYKColors[10], allCMYKColors[20], column10steps[6]);
        allCMYKColors[56] = percentStepDarkToLight(allCMYKColors[10], allCMYKColors[20], column10steps[7]);
        allCMYKColors[57] = percentStepDarkToLight(allCMYKColors[10], allCMYKColors[20], column10steps[8]);
    } // end method fillColumn10Colors



    private static void fillColumn9Colors() {
        allCMYKColors[43] = percentStepDarkToLight(allCMYKColors[9], allCMYKColors[19], column9steps[1]);
        allCMYKColors[44] = percentStepDarkToLight(allCMYKColors[9], allCMYKColors[19], column9steps[2]);
        allCMYKColors[45] = percentStepDarkToLight(allCMYKColors[9], allCMYKColors[19], column9steps[3]);
        allCMYKColors[46] = percentStepDarkToLight(allCMYKColors[9], allCMYKColors[19], column9steps[4]);
        allCMYKColors[47] = percentStepDarkToLight(allCMYKColors[9], allCMYKColors[19], column9steps[5]);
        allCMYKColors[48] = percentStepDarkToLight(allCMYKColors[9], allCMYKColors[19], column9steps[6]);
        allCMYKColors[49] = percentStepDarkToLight(allCMYKColors[9], allCMYKColors[19], column9steps[7]);
    } // end method fillColumn9Colors



    private static void fillColumn8Colors() {
        allCMYKColors[37] = percentStepDarkToLight(allCMYKColors[8], allCMYKColors[18], column8steps[1]);
        allCMYKColors[38] = percentStepDarkToLight(allCMYKColors[8], allCMYKColors[18], column8steps[2]);
        allCMYKColors[39] = percentStepDarkToLight(allCMYKColors[8], allCMYKColors[18], column8steps[3]);
        allCMYKColors[40] = percentStepDarkToLight(allCMYKColors[8], allCMYKColors[18], column8steps[4]);
        allCMYKColors[41] = percentStepDarkToLight(allCMYKColors[8], allCMYKColors[18], column8steps[5]);
        allCMYKColors[42] = percentStepDarkToLight(allCMYKColors[8], allCMYKColors[18], column8steps[6]);
    } // end method fillColumn8Colors



    private static void fillColumn7Colors() {
        allCMYKColors[32] = percentStepDarkToLight(allCMYKColors[7], allCMYKColors[17], column7steps[1]);
        allCMYKColors[33] = percentStepDarkToLight(allCMYKColors[7], allCMYKColors[17], column7steps[2]);
        allCMYKColors[34] = percentStepDarkToLight(allCMYKColors[7], allCMYKColors[17], column7steps[3]);
        allCMYKColors[35] = percentStepDarkToLight(allCMYKColors[7], allCMYKColors[17], column7steps[4]);
        allCMYKColors[36] = percentStepDarkToLight(allCMYKColors[7], allCMYKColors[17], column7steps[5]);
    } // end method fillColumn7Colors



    private static void fillColumn6Colors() {
        allCMYKColors[28] = percentStepDarkToLight(allCMYKColors[6], allCMYKColors[16], column6steps[1]);
        allCMYKColors[29] = percentStepDarkToLight(allCMYKColors[6], allCMYKColors[16], column6steps[2]);
        allCMYKColors[30] = percentStepDarkToLight(allCMYKColors[6], allCMYKColors[16], column6steps[3]);
        allCMYKColors[31] = percentStepDarkToLight(allCMYKColors[6], allCMYKColors[16], column6steps[4]);
    } // end method fillColumn6Colors



    private static void fillColumn5Colors() {
        allCMYKColors[25] = percentStepDarkToLight(allCMYKColors[5], allCMYKColors[15], column5steps[1]);
        allCMYKColors[26] = percentStepDarkToLight(allCMYKColors[5], allCMYKColors[15], column5steps[2]);
        allCMYKColors[27] = percentStepDarkToLight(allCMYKColors[5], allCMYKColors[15], column5steps[3]);
    } // end method fillColumn5Colors



    private static void fillColumn4Colors() {
        allCMYKColors[23] = percentStepDarkToLight(allCMYKColors[4], allCMYKColors[14], column4steps[1]);
        allCMYKColors[24] = percentStepDarkToLight(allCMYKColors[4], allCMYKColors[14], column4steps[2]);
    } // end method fillColumn4Colors



    private static void fillColumn3Colors() {
        int firstFillOrderNum = 22;
        allCMYKColors[22] = percentStepDarkToLight(allCMYKColors[3], allCMYKColors[13], column3steps[1]);
    } // end method fillColumn3Colors


    private static Color percentStepDarkToLight(Color startDarkPt, Color endLightPt, int stepPercent) {
        ColorPtArb darkPt = new ColorPtArb(startDarkPt);
        ColorPtArb lightPt = new ColorPtArb(endLightPt);
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
        Color returnColor = extColorPtArb.getAsColor();

        return returnColor;
    } // end method percentStepDarkToLight



    public static Color[] getAllCMYKColors() {
        return allCMYKColors;
    } // end method getAllCMYKColors


    /**
     * Creates a color point on the line from pure hue to black.
     * The position is determined by lumRealCMYK, which indirectly specifies how far along this line the color should be.
     * Variable lumRealCMYK is always in range 0 - 1 and reduces RGB coordinates linearly which is the same as
     * moving on the line from pure hue to black
     *
     * @param pHueColorPt
     * @return
     */
    private static Color calcNonPureHueCMYK(ColorPtOstDot pHueColorPt) {
        int stdGUIHueNum = pHueColorPt.getStdCodeHueNum() +1; // convert internal hue number (0-35) to user-facing (1-36)
        // TODO 062025 -REPLACE HACK with METHOD to get clearBlack pt with same 3D luminance (= pureHue3DLum)
        double pureHue3DLum = RGBCubeModel.calcLum3D(pHueColorPt.getAsColor());
        Color correctClearBColor = calcClearBColorFor3DLum(pHueColorPt);

        /*
        System.out.println("Need METHOD using pureHue3DLum = "+pureHue3DLum + ", for GUIHueNum = "+stdGUIHueNum +
                ", to get clearBlack pt w same 3D luminance");
        double lumRealCMYK = lumByHueNumArray9[stdGUIHueNum];
        // TODO 062025 -REPLACE THIS HACK with METHOD -bad HACK to get clear-Black color with same 3D luminance as pure hue color
        double percentBlack = 1.0 -lumRealCMYK; // TODO 062025 NOT CORRECT, result is WAY OFFreplace with method
        System.out.println("Instead using just percentBlack = "+ percentBlack + ", for GUIHueNum = "+stdGUIHueNum);
        // Color pureCMYKColor = getColorOnClearBlackLine(pHueColorPt, percentBlack);
        double nonPure3DLum = RGBCubeModel.calcLum3D(pureCMYKColor);
        System.out.println("Gives non-pureCMYKColor pt w 3D lum= "+ nonPure3DLum);
        Color correctClearBColor = calcClearBColorFor3DLum(pHueColorPt);
        double correctMethod3DLum = RGBCubeModel.calcLum3D(correctClearBColor);
        System.out.println("Method -calcClearBColorFor3DLum- gives correctClearBColor pt w 3D lum= "+ correctMethod3DLum);
         */

        return correctClearBColor;
    } // end method calcNonPureHueCMYK


    /**
     * Finds a color on the line from pure hue to black that has approximately the same 3D luminance
     * as the pure hue color, within a delta of 0.03.
     * 
     * @param pHueColorPt The pure hue color point
     * @return A color on the pure hue to black line with matching 3D luminance
     */
    private static Color calcClearBColorFor3DLum(ColorPtOstDot pHueColorPt) {
        Color pureHueColor = pHueColorPt.getAsColor();
        double targetLum = calcCMYKValue(pHueColorPt); // WAS incorrect "RGBCubeModel.calcLum3D(pureHueColor);"
        
        // Binary search to find the color with matching luminance
        float minPercent = 0f;
        float maxPercent = 1f;
        float currentPercent = 0.5f;
        final float TOLERANCE = 0.03f;
        
        int maxIterations = 20; // Prevent infinite loops
        int iteration = 0;
        Color bestMatch = null;
        double bestDelta = Double.MAX_VALUE;
        
        while (iteration++ < maxIterations) {
            // Create color at current percentage
            Color currentColor = getColorOnClearBlackLine(pureHueColor, currentPercent);
            double currentLum = RGBCubeModel.calcLum3D(currentColor);
            double delta = Math.abs(currentLum - targetLum);
            
            // Keep track of best match found so far
            if (delta < bestDelta) {
                bestDelta = delta;
                bestMatch = currentColor;
            }
            
            if (delta < TOLERANCE) {
                return currentColor;
            }
            
            // Adjust search range
            if (currentLum < targetLum) {
                // Need to move toward pure hue (less black)
                maxPercent = currentPercent;
            } else {
                // Need to move toward black (more black)
                minPercent = currentPercent;
            }
            
            // Calculate new percentage to test
            currentPercent = (minPercent + maxPercent) / 2;
        }
        
        // Return best match found (should be within tolerance, but this is a fallback)
        return bestMatch;
    }


    /**
     * Creates a color point on the line from pure hue to black. But there are two cases:
     * 1. Get the "non-pure" hue Color that has the same CMYK luminance as the pure hue color
     * 2. Get each Color in the clear-Black series that starts with the "non-pure" hue Color
     * The position is determined by percentBlack, which is conceptually clear and indirectly specifies the
     * reductionFactor for how far along this line the color should be.
     *
     * @param pHueColorPt The base color point
     * @param percentBlack The percentage of black to add (0.0 to 1.0)
     * @return A new color on the line from the input color to black
     */
    private static Color getColorOnClearBlackLine(Color pHueColorPt, double percentBlack) {
        double reductionFactor = 1.0 - percentBlack;
        int r255 = (int) (reductionFactor * pHueColorPt.getRed());
        int g255 = (int) (reductionFactor * pHueColorPt.getGreen());
        int b255 = (int) (reductionFactor * pHueColorPt.getBlue());
        Color clearBlackColor = new Color(r255, g255, b255);
        return clearBlackColor;
    } // end method getColorOnClearBlackLine



    /**
     * Once the startPt color is known, method returns other colors on the line from startPt color to black
     * Note that startPt color is NOT the pure hue color. The startPt color is the color matching CMYK luminance of the pure hue color.
     * The startPt color is also known as "non-pure" hue Color
     * @param nonPureHueStartPt
     * @param intAmtBlack
     * @return
     */
    public static Color returnColorGoingToBlack(Color nonPureHueStartPt, int intAmtBlack) {
        double percentBlack = intAmtBlack / 100.0; // one of the numbers must be a decimal (implicit double) to avoid truncation with loss of precision
        Color returnColor = getColorOnClearBlackLine(nonPureHueStartPt, percentBlack);
        return returnColor;
    } // end method returnColorGoingToBlack


    /**
     * Once the startPt color is known, method returns other colors on the line from startPt color to white
     * Note that startPt color is NOT the pure hue color. The startPt color is the color matching CMYK luminance of the pure hue color.
     * The startPt color is also known as "non-pure" hue Color
     * @param nonPureHueStartPt
     * @param intAmtWhite
     * @return
     */
    public static Color returnColorGoingToWhite(Color nonPureHueStartPt, int intAmtWhite) {
        ColorPtArb pHueColorPt = new ColorPtArb(nonPureHueStartPt);
        double whiteIncrement = (double)intAmtWhite/100;
        // this is for keeping "proportionality" in the mid-coordinate
        // the max coord is always 100 and the min coord is always zero
        double colorAboveWhiteFactor = 1.0 - whiteIncrement;

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
        Color returnColor = extColorPtArb.getAsColor();

        return returnColor;
    } // end method returnColorGoingToWhite


    // Static accessor for CMYK display color by fillOrderNum (1-based)
    public static Color getCMYKDisplayColor(int fillOrderNum) {
        return allCMYKColors[fillOrderNum];
    }

} // End class CMYKValueCalc
