package com.csurf.ostwald.domaingeo.model;


/**
 * Utility class to reduce number of parameters for methods that take
 * a set of HLS parameters (hueDegrees, Lightness, Saturation)
 */
public class HLSParamBundle {
    private int theHueDegrees360;
    private double theLightness;
    private double theSaturation;

    public HLSParamBundle(int hueDegrees360, double lightness, double saturation) {
        this.theHueDegrees360 = hueDegrees360;
        this.theLightness = lightness;
        this.theSaturation = saturation;
    } // end constructor

    public int getHueDegrees360() {
        return theHueDegrees360;
    } // end getHueDegrees360

    public double getLightness() {
        return theLightness;
    } // end getLightness

    public double getSaturation() {
        return theSaturation;
    } // end getSaturation

} // end class HLSParamBundle
