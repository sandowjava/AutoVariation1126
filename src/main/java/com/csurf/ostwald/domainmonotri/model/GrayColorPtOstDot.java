package com.csurf.ostwald.domainmonotri.model;

import com.csurf.ostwald.domaingeo.model.ColorPtArb;

import java.awt.*;

// 042322 changed both the TYPE (extending class "ColorPlusPt2Dot" rather than class "ColorPlusPt2M2") and the INTERFACE (now "ColorPtOstDotType")
public class GrayColorPtOstDot extends ColorPtOstDot {

    private final String className= "GrayColorPtOstDot";
    private String name;
    private boolean doesHaveHue;

    /*
    // added 022622  // 051525 took out
    public GrayColorPtOstDot(double rDec, double gDec, double bDec) {
        super(rDec, gDec, bDec);
        className = "GrayColorPtOstDot";
    }
     */



    // Add constructor that accepts CMYK display color
    public GrayColorPtOstDot(ColorPtArb grayPtArb, OstHueColumnStep someOHCS, Color cmykDisplayColor) {
        super(grayPtArb, someOHCS, cmykDisplayColor);
        if(someOHCS.getFillOrderNum() == 67) {
            int kerflooey = 666; // 022622 for tracing flow only
        }
    } // end constructor



    /* // TODO 051525 took out -- some kinda prolonged confusion here - why do I need this??
    // added 022622 -identical to superClass method -not sure WHY, but this seems to do the trick for GrayPts showing their color etc in IDEA
    public static GrayColorPtOstDot createFromRGB255Ints(int gray255Coord){

        super(gray255Coord, gray255Coord, gray255Coord);
        // double rDec = (double) rInt/255;
        // double gDec = (double) gInt/255;
        // double bDec = (double) bInt/255;
        // double grayDublCoord = (double) grayIntCoord/255;

        return new GrayColorPtOstDot(grayDublCoord, grayDublCoord, grayDublCoord);

    } // end static factory createFromRGB255Ints

     */




    // 042322 I did not change this at all, other than the return TYPE. Seems to all be OK // 061725 changed method name to reflect hueNumCode
    // TODO 022622 static factory -NOTE: this is the ESTABLISHED METHOD that's called in current "MonoTriangleSet" code 022622
    // Update factory method to use new constructor
    public static GrayColorPtOstDot createFromCodeHueLumFillNum(int stdHueNumCode, double lum, int fillNum, Color cmykDisplayColor) {
        int gray255Coord = (int) (lum * 255);
        ColorPtArb grayPtArb = new ColorPtArb(gray255Coord, gray255Coord, gray255Coord);
        ColumnStepWhiteBlackParams paramBundle = ColorPtOstDot.createParamBundleFromFillOrderNum(fillNum);
        OstHueColumnStep someOHCS = new OstHueColumnStep(stdHueNumCode, paramBundle, fillNum);

        if(someOHCS.getFillOrderNum() == 67) {
            int kerflooey = 666; // 022622 for tracing flow only
        }
        return new GrayColorPtOstDot(grayPtArb, someOHCS, cmykDisplayColor);
    } // end static factory createFromLumAndHue



    // 071121 took out
    // TODO this entire class seems too skimpy, like it's a rush job. Maybe redo w DECORATOR PATTERN 070821
    /*
    // 070821 added -- needs to be static to be called in static factory method "createFromLumAndHue"
    private static void setDoesHaveHue(boolean hasHueTrueFalse){
        this.doesHaveHue = hasHueTrueFalse;
    } // end method setDoesHaveHue
     */




    public String toString(){
        return name;
    } // end method toString


/*
public boolean doesHaveHue(){
	return doesHaveHue;
} // end method doesHaveHue
*/

} // end class GrayColorPt2M2
