package com.csurf.ostwald.domainmonotri.model;

import java.awt.Color;

import com.csurf.ostwald.domaingeo.model.HLSParamBundle;
import com.csurf.ostwald.domaingeo.model.ColorPtArb;
import com.csurf.ostwald.domaingeo.model.RGBCubeModel;

/**
 * Represents a color point in Ostwald dot notation.
 * This class extends ColorPtArb to add Ostwald-specific color representation.
 */
public class ColorPtOstDot extends ColorPtArb {
    private final OstHueColumnStep someOstHCS;
    // private final int stdHueNum; // TODO 061125 removed HUENUM field from ColorPtOstDot - redundant to OstHueColumnStep
    private static final int[] clrWdrawNums = {0, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66};
    private static final int[] clrBdrawNums = {0, 2, 4, 7, 11, 16, 22, 29, 37, 46, 56};
    private double theRGBLum;    // Luminance of the RGB color
    private double theCMYKLum;   // Luminance of the CMYK color
    protected final Color theCMYKDisplayColor;



    // TODO 111226 definitely seems that this constructor is used
    // TODO 091826 INVESTIGATE earlier assertion "this constructor not used" (CLARIFY: not used EXCEPT...)
    // NOTE: this constructor not used - DESIGN INTENT: MonotriBook is real "source" of ColorPtOstDot singleton objects
    public ColorPtOstDot(ColorPtArb someColorPtArb, OstHueColumnStep someOHCS, Color cmykDisplayColor) {
        // Double-ctor route: dot inherits the arb's full-precision decs so
        // downstream interpolation (edge + interior fills) reads decs, not
        // int/255 round-trips. Int channels unchanged — same (int)(dec*255)
        // truncation legacy uses.
        super(someColorPtArb.getTheRDec(), someColorPtArb.getTheGDec(), someColorPtArb.getTheBDec());
        this.someOstHCS = someOHCS;
        this.theCMYKDisplayColor = cmykDisplayColor;
        // this.stdHueNum = someOHCS.getStdHueNum(); // TODO 061125 removed HUENUM field from ColorPtOstDot - redundant to OstHueColumnStep
        // Initialize luminance values — full-precision dec input, same
        // formula; matches legacy getLum3DOfColorPlusPt (lum on stored decs,
        // not quantized int255 channels)
        this.theRGBLum = RGBCubeModel.calcLum3D(
                someColorPtArb.getTheRDec(), someColorPtArb.getTheGDec(), someColorPtArb.getTheBDec());
        // Temporary: set CMYK luminance equal to RGB until CMYK conversion is implemented
        this.theCMYKLum = CMYKValueCalc.calcCMYKValue(this);

    } // end constructor // DESIGN INTENT: MonotriPage uses this constructor (-> singletons)



    // TODO 062225 look at replacing the return type with OstPosition type
    // 051525 added // TODO 051525 rework: this method implicitly hard-coded for EDGESIZE ==12
    public static ColumnStepWhiteBlackParams createParamBundleFromFillOrderNum(int fillOrderNum) {
        ColumnStepWhiteBlackParams newParamBundle = null;

        if(fillOrderNum == 1) { // pureHue
            newParamBundle = new ColumnStepWhiteBlackParams(1, 1, 0, 0, 1);}

        if(fillOrderNum >= 2 && fillOrderNum <= 11) {
            newParamBundle = new ColumnStepWhiteBlackParams(fillOrderNum, 1, 0, fillOrderNum-1, clrBdrawNums[fillOrderNum-1]);
        } // end clear-B series

        if(fillOrderNum >= 12 && fillOrderNum <= 21) { // TODO 051925 FORMULA for each COLUMN is simple: columnNum plus stepNum (NOTE: except for column 1 & clearW series & grayscale)
            newParamBundle = // TODO 051925 drawOrderNum for clearW is "complex/confusing calc" ->SET INDEX derived from whatColumn = fillOrderNum-10, but "minus 1 further" NOTE: != colNum
                    new ColumnStepWhiteBlackParams(fillOrderNum-10, fillOrderNum-10, fillOrderNum-11, 0, clrWdrawNums[fillOrderNum-11]);
        } // end clear-W series

        if(fillOrderNum == 22) { // TODO 051525 must check these error-prone calcs done "per column" "per fillOrderNum"
            newParamBundle =
                    new ColumnStepWhiteBlackParams(3, fillOrderNum-20, fillOrderNum-21, fillOrderNum-21, fillOrderNum-17);
        } // end column 3

        if(fillOrderNum >= 23 && fillOrderNum <= 24) { // TODO 051525 pattern of incrementing that goes up by one each time (PREVIOUS increment +1)
            newParamBundle =
                    new ColumnStepWhiteBlackParams(4, fillOrderNum-21, fillOrderNum-22, 25-fillOrderNum, fillOrderNum-15);
        } // end column 4

        if(fillOrderNum >= 25 && fillOrderNum <= 27) { // column 5
            newParamBundle =
                    new ColumnStepWhiteBlackParams(5, fillOrderNum-23, fillOrderNum-24, 28-fillOrderNum, fillOrderNum-13);
        } // end column 5

        if(fillOrderNum >= 28 && fillOrderNum <= 31) { // column 6
            newParamBundle =
                    new ColumnStepWhiteBlackParams(6, fillOrderNum-26, fillOrderNum-27, 32-fillOrderNum, fillOrderNum-11);
        } // end column 6

        if(fillOrderNum >= 32 && fillOrderNum <= 36) { // column 7
            newParamBundle =
                    new ColumnStepWhiteBlackParams(7, fillOrderNum-30, fillOrderNum-31, 37-fillOrderNum, fillOrderNum-9);
        } // end column 7

        if(fillOrderNum >= 37 && fillOrderNum <= 42) { // column 8
            newParamBundle =
                    new ColumnStepWhiteBlackParams(8, fillOrderNum-35, fillOrderNum-36, 43-fillOrderNum, fillOrderNum-7);
        } // end column 8

        if(fillOrderNum >= 43 && fillOrderNum <= 49) { // column 9
            newParamBundle =
                    new ColumnStepWhiteBlackParams(9, fillOrderNum-41, fillOrderNum-42, 50-fillOrderNum, fillOrderNum-5);
        } // end column 9

        if(fillOrderNum >= 50 && fillOrderNum <= 57) { // column 10
            newParamBundle =
                    new ColumnStepWhiteBlackParams(10, fillOrderNum-48, fillOrderNum-49, 58-fillOrderNum, fillOrderNum-3);
        } // end column 10

        if(fillOrderNum >= 58 && fillOrderNum <= 66) { // column 11
            newParamBundle =
                    new ColumnStepWhiteBlackParams(11, fillOrderNum-56, fillOrderNum-57, 67-fillOrderNum, fillOrderNum-1);
        } // end column 11

        if(fillOrderNum >= 67 && fillOrderNum <= 78) { // this column departs from previous increment pattern because gray-scale includes pureB & pureW
            newParamBundle =
                    new ColumnStepWhiteBlackParams(12, fillOrderNum-66, fillOrderNum-67, 78-fillOrderNum, fillOrderNum);
        } // end column 12

        // 051925 for debugging only
        if (fillOrderNum ==22) {
            int kerflooey = 666;}

        return newParamBundle;
    } // end method createParamBundleFromFillOrderNum


    /**
     * Only legitimate use is to create a "pure hue" ColorPtOstDot from a hueDegrees360, lightness, and saturation value,
     * plus fillNum, stdHueNum, and cmykDisplayColor. Only variable in HLSParamBundle should be hueDegrees360, with invariants L = 0.5, S = 1.0
     * But this method is available for misuse, nothing prevents this. It can take any hue, lightness, and saturation value as inputs.
     * So adopt the convention that for truly arbitrary color ("non-std" ColorPtOstDots), fillNum = 999.
     * Flag all cases where fillNum != 1 and set up checkedFillNum = 999 to handle these
     * @param someHLSParamBundle
     * @param fillNum
     * @param stdHueNum
     * @param cmykDisplayColor
     * @return
     */
    public static ColorPtOstDot createPureHueOstdotFromHLSPlus(HLSParamBundle someHLSParamBundle, int fillNum, int stdHueNum, Color cmykDisplayColor) {
        int checkedFillNum = 1; // 062425 this matches the only legitimate case (with input param fillNum == 1)
        if(fillNum != 1) { // for truly arbitrary color ("non-std" ColorPtOstDots), need to setfillNum = 999
            checkedFillNum = 999; // 999 should produce IllegalArgumentException in OstHueColumnStep constructor
        } //
        ColumnStepWhiteBlackParams paramOHCSBundle = createParamBundleFromFillOrderNum(checkedFillNum);
        OstHueColumnStep someOHCS = new OstHueColumnStep(stdHueNum, paramOHCSBundle, checkedFillNum);
        ColorPtArb someColorPtArb
                = ColorPtArb.createColorPtArbFromHLSNums(someHLSParamBundle.getHueDegrees360(),
                                    someHLSParamBundle.getLightness(), someHLSParamBundle.getSaturation());
        
        return new ColorPtOstDot(someColorPtArb, someOHCS, cmykDisplayColor);
    } // end method createOstdotFromTooManyParams



    public final OstHueColumnStep getOstHCS() {
        return someOstHCS;
    } // end method getOstHCS



    /**
     * Gets the standard hue number for this color point.
     * Note: Returns internal hue number (0-35), not user-facing (1-36).
     * 
     * @return The standard hue number (0-35)
     */
    public final int getStdCodeHueNum() {
        int stdHueNum = someOstHCS.getStdCodeHueNum(); // Internal hue number (0-35)
        return stdHueNum;
    } // end method getStdCodeHueNum



    /**
     * Returns the HueNumGUI for this color's hue.
     *
     * Converts the internal code hue number (0-35) to user-facing format (1-36)
     * and returns the corresponding HueNumGUI instance.
     *
     * @return HueNumGUI representing this color's hue in user-facing format (1-36)
     */
    public HueNumGUI getHueNumGUI() {
        int codeHueNum = this.getStdCodeHueNum();
        HueNumGUI theHueNumGUI
                = HueNumGUI.getHueNumGUIInstance(codeHueNum+1);
        return theHueNumGUI;
    } // end method getHueNumGUI


    /**
     * Tests whether the "hue number matching" for this color point is correct.
     * Will be used by method to correct old saved Templates with "off-by-one" error inside ColorPtOstDot of DrawObj instances
     */
    public boolean testHueNumCodeCorrect() {
        boolean testResult = false;

        int storedHueNumCode = getStdCodeHueNum(); // Internal hue number (0-35)
        int closestHueNumCode = MonotriBook.getInstance().findClosestStandardCodeHue(this.getAsColor());
        if((this.getOstHCS().getFillOrderNum() <58) && (storedHueNumCode== closestHueNumCode)) { // 062425 don't test column 11 because unreliable
            testResult = true; // 062425 sorta quicky hack - does not clearly indicate rejection of column 11 colorPts because unreliable
        } // end IF
        return testResult;
    } // end method testHueNumCodeCorrect



    /*
     * Method returns the hueNumGUI for "true complementary" hue for this ColorPtOstDot
     * "True complementary" hue NOT necessarily hueNumGUI+18 for "apparent complementary" by hueCircle layout
     *
     */
    public HueNumGUI getTrueComplemHueNumGUI() { // TODO 062625 need to write the details of this method
        HueNumGUI thisColorPtHueNumGUI
                = HueNumGUI.getHueNumGUIInstance(this.getStdCodeHueNum() +1);
        HueNumGUI complemHueNumGUI
                = HueSpacingStdChoices.getSingleton().calcTrueComplemHueNumGUI(thisColorPtHueNumGUI);
        return complemHueNumGUI;
    } // end method getTrueComplemHueNumGUI



    /**
     * Converts this Ostwald color point to a Java AWT Color.
     * Since ColorPtOstDot extends Color, this simply returns this instance.
     *
     * @return This color as a Java AWT Color
     */
    public Color toAWTColor() {
        return new Color(getRed(), getGreen(), getBlue());
    }




    public Color toCMYKColor() {
        // TODO 052925 temporary implementation -method toCMYKColor() needs to be fixed
        return this.theCMYKDisplayColor;
    } // end method toCMYKColor



    public int getFillOrderNum() {
        int fillOrderNum = getOstHCS().getFillOrderNum();
        return fillOrderNum;
    } // end method getFillOrderNum



    /**
     * Returns the absolute color saturation of this color.
     * This is an alias for getAbSat() for clarity and consistency in the context of color saturation matching.
     * 
     * @return the absolute saturation value (0.0 to 1.0)
     *         where 0.0 = no saturation (gray), 1.0 = full saturation (pure hue)
     */
    public double getAbsColorSaturation() {
        return this.getAbSat();
    } // end method getAbsColorSaturation



    // 060425 copied from RS302DotDragCleanColr3D_DEBGv2Axxx
    public boolean canDoDragNewLocationYesNo(int dragOffsetW, int dragOffsetB) { // these from mouseDrag calc ("drag dist" +or- seriesW,seriesB to newDot)
        boolean dragResultsOK = false; // mouseDrag calc by: (newDotCoord - currDotCoord) e.g., pureHue to midTriangle ostDot -> positive "drag dist" results

        int whiteOffsetResult = this.getOstHCS().getSeriesW() +dragOffsetW; // this will be new seriesW coord -LOGIC: may not be within range 0-11??
        int blackOffsetResult = this.getOstHCS().getSeriesB() +dragOffsetB; // this will be new seriesB coord -LOGIC: may not be within range 0-11??

        boolean whiteOffsetResultTestOK = false;
        if(whiteOffsetResult<12 && whiteOffsetResult>-1) { whiteOffsetResultTestOK = true;}

        boolean blackOffsetResultTestOK = false;
        if(blackOffsetResult<12 && blackOffsetResult>-1) { blackOffsetResultTestOK = true;}

        if(whiteOffsetResultTestOK && blackOffsetResultTestOK) {dragResultsOK = true;}

        return dragResultsOK;
    } // end method canDoDragNewLocationYesNo



    public static int getFillOrderNumFromBWSeries(int seriesWCoord, int seriesBCoord) { // 042522 made STATIC for testing
        int theDotCellNum = -313; // flag value for IF coords somehow out of range

        // NOTE: these "IF"s should ALL be mutually exclusive (not tested)
        if((seriesBCoord ==0) && (seriesWCoord<11 && seriesWCoord >0)) { // ClearW series, but NOT including pure W - also excludes PURE HUE
            theDotCellNum = seriesWCoord+ 11; // NOTE: "&& seriesWCoord >0" necessary to exclude PURE HUE
        } else if((seriesBCoord ==0) && (seriesWCoord== 11)) { // pure W
            theDotCellNum = 78; // pure W
        } // end IF-ELSE

        if((seriesWCoord ==0) && (seriesBCoord<11)) { // ClearB series, including PURE HUE, but NOT including pure B
            theDotCellNum = seriesBCoord+ 1;
        } else if((seriesWCoord ==0) && (seriesBCoord== 11)) { // pure B
            theDotCellNum = 67; // pure B
        } // end IF-ELSE

        // NOTE: these "IF"s should ALL be mutually exclusive, and should cover all cases "within legal range" (untested)
        // 042322 it so happens that each column always has the same sum of the twp coords (Wcoord+ Bcoord)
        // 042522 for all these "column IFs" NEED LOGIC: "&& (seriesWCoord !=0) && (seriesBCoord !=0)" to EXCLUDE clearW & clearB dots
        if((seriesWCoord + seriesBCoord) == 2 && (seriesWCoord !=0) && (seriesBCoord !=0)) {
            theDotCellNum = 22; } // column3 -one case

        if((seriesWCoord + seriesBCoord) == 3 && (seriesWCoord !=0) && (seriesBCoord !=0)) {
            theDotCellNum = 22 +seriesWCoord; } // column4 -has 2 cases

        if((seriesWCoord + seriesBCoord) == 4 && (seriesWCoord !=0) && (seriesBCoord !=0)) {
            theDotCellNum = 24 +seriesWCoord; } // column5 -has 3 cases

        if((seriesWCoord + seriesBCoord) == 5 && (seriesWCoord !=0) && (seriesBCoord !=0)) {
            theDotCellNum = 27 +seriesWCoord; } // column6 -has 4 cases

        if((seriesWCoord + seriesBCoord) == 6 && (seriesWCoord !=0) && (seriesBCoord !=0)) {
            theDotCellNum = 31 +seriesWCoord; } // column7 -has 5 cases

        if((seriesWCoord + seriesBCoord) == 7 && (seriesWCoord !=0) && (seriesBCoord !=0)) {
            theDotCellNum = 36 +seriesWCoord; } // column8 -has 6 cases

        if((seriesWCoord + seriesBCoord) == 8 && (seriesWCoord !=0) && (seriesBCoord !=0)) {
            theDotCellNum = 42 +seriesWCoord; } // column9 -has 7 cases

        if((seriesWCoord + seriesBCoord) == 9 && (seriesWCoord !=0) && (seriesBCoord !=0)) {
            theDotCellNum = 49 +seriesWCoord; } // column10 -has 8 cases

        if((seriesWCoord + seriesBCoord) == 10 && (seriesWCoord !=0) && (seriesBCoord !=0)) {
            theDotCellNum = 57 +seriesWCoord; } // column11 -has 9 cases

        if((seriesWCoord + seriesBCoord) == 11 && (seriesBCoord!= 11) && (seriesWCoord != 11)) { // "overkill" logic to exclude pureB & pureW
            theDotCellNum = 67 +seriesWCoord; } // column12 -has 10 cases (grayscale)


        if (theDotCellNum == -313) { // 042322 simple PRELIM ERROR CASE -coords somehow "fell past" all the  mutually exclusive IFs above
            System.out.println("method -getDotCellNumFromBWCoordinates- in class -ClolorPlusPt2Dot- inputCoords were " +
                    "out of range seriesW = " +seriesWCoord + ", seriesB = " +seriesBCoord);
        } // end IF for PRELIM ERROR CASE


        return theDotCellNum;
    } // end method getDotCellNumFromBWCoordinates



    public Color getCMYKDisplayColor() {
        return theCMYKDisplayColor;
    }


    public float getCMYKLum() {
        return (float) theCMYKLum;
    } // end method getCMYKLum


    /**
     * Gets the RGB luminance of this dot's color, computed at construction via
     * RGBCubeModel.calcLum3D (sqrt-weighted 0.299/0.587/0.114 on normalized channels).
     * This is the metric used by the value-range candidate query — NOT getCMYKLum().
     *
     * @return The RGB luminance (0.0-1.0)
     */
    public double getRGBLum() {
        return theRGBLum;
    } // end method getRGBLum


} // end class ColorPtOstDot
