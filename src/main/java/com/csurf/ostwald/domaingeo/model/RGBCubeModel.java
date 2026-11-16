package com.csurf.ostwald.domaingeo.model;

import java.awt.Color;

/**
 * Models the RGB color space as a cube.
 * Uses singleton pattern to ensure only one instance exists.
 */
public class RGBCubeModel {
    private static RGBCubeModel theCubeSingleton;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private RGBCubeModel() {
        // Private constructor to prevent instantiation
    }

    // RGB luminance coefficients
    private static final double R_LUM = 0.299;
    private static final double G_LUM = 0.587;
    private static final double B_LUM = 0.114;

    /**
     * Gets the singleton instance of RGBCubeModel.
     * Uses lazy initialization - singleton isn't created until first access.
     * @return the singleton instance
     */
    public static RGBCubeModel getSingleton() {
        if (theCubeSingleton == null) {
            theCubeSingleton = new RGBCubeModel();
        }
        return theCubeSingleton;
    }

    /**
     * Calculates luminance using standard RGB coefficients.
     * This is the legacy method kept for compatibility with existing code.
     * For new code, prefer using calcLum3D which better approximates human perception.
     * @param arbColor The arbColor to calculate luminance for
     * @return The calculated luminance value between 0.0 and 1.0
     * @throws IllegalArgumentException if arbColor is null
     */
    public static double calcLum(Color arbColor) {
        if (arbColor == null) {
            throw new IllegalArgumentException("Color cannot be null");
        }
        double rDec = arbColor.getRed() / 255.0;
        double gDec = arbColor.getGreen() / 255.0;
        double bDec = arbColor.getBlue() / 255.0;
        return R_LUM * rDec + G_LUM * gDec + B_LUM * bDec;
    } // end method calcLum

    /**
     * Calculates luminance using standard RGB coefficients.
     * This is the legacy method kept for compatibility with existing code.
     * For new code, prefer using calcLum3D which better approximates human perception.
     * @param rDec Red component in decimal form (0.0-1.0)
     * @param gDec Green component in decimal form (0.0-1.0)
     * @param bDec Blue component in decimal form (0.0-1.0)
     * @return The calculated luminance value between 0.0 and 1.0
     */
    public static double calcLum(double rDec, double gDec, double bDec) {
        return R_LUM * rDec + G_LUM * gDec + B_LUM * bDec;
    } // end method calcLum



    /**
     * Calculates luminance using a square root calculation that better approximates
     * human perception of brightness.
     * @param arbColor The arbColor to calculate luminance for
     * @return The calculated luminance value between 0.0 and 1.0
     */
    public static double calcLum3D(Color arbColor) {
        double rDec = arbColor.getRed() / 255.0;
        double gDec = arbColor.getGreen() / 255.0;
        double bDec = arbColor.getBlue() / 255.0;
        return Math.sqrt(rDec*rDec*R_LUM + gDec*gDec*G_LUM + bDec*bDec*B_LUM);
    } // end method calcLum3D



    /**
     * Same weighted-square-root luminance as {@link #calcLum3D(Color)}, but on
     * full-precision decimal channels — matches the legacy
     * {@code getLum3DOfColorPlusPt} input, which computes luminance on the
     * color point's stored decimals rather than quantized int255 channels.
     * @param rDec Red component in decimal form (0.0-1.0)
     * @param gDec Green component in decimal form (0.0-1.0)
     * @param bDec Blue component in decimal form (0.0-1.0)
     * @return The calculated luminance value between 0.0 and 1.0
     */
    public static double calcLum3D(double rDec, double gDec, double bDec) {
        return Math.sqrt(rDec*rDec*R_LUM + gDec*gDec*G_LUM + bDec*bDec*B_LUM);
    } // end method calcLum3D (decimal overload)



    /**
     * Calculates the "percentage" of pure hue in any given color, which is the "absolute color saturation".
     * This is the color "remainder" which is not the black or white component.
     * In the case of pure colors, the abSat = 1.00, while all other colors have abSat < 1.00 (with Black or White = 0)
     * This percentage of color is used to calculate the "CMYK print luminance," which is different from the onscreen
     * RGB luminance.
     *
     * @param someColorPtArb is any arbitrary color but in ColorPtArb form
     * @return is a decimal in the range 0-1
     */
    public static double getAbSat(ColorPtArb someColorPtArb){
        double abSatValue = maxFunction(someColorPtArb) - minFunction(someColorPtArb);
        return abSatValue;
    } // end method getAbSat



    public static double maxFunction(ColorPtArb someColorPtArb) {
        double maxValue = maxRGBDecFunction(someColorPtArb.getTheRDec(),
                someColorPtArb.getTheGDec(), someColorPtArb.getTheBDec());
        return maxValue;
    } // end method maxFunction



    public static double maxRGBDecFunction(double red, double green, double blue) {
        double maxValue;

        if (red > green) { maxValue = red;}
        else { maxValue = green;}
        if (blue > maxValue) { maxValue = blue;}
        return maxValue;
    } // end method maxRGBDecFunction



    public static double minFunction(ColorPtArb someColorPtArb) {
        double minValue = minRGBDecFunction(someColorPtArb.getTheRDec(),
                someColorPtArb.getTheGDec(), someColorPtArb.getTheBDec());
        return minValue;
    } // end method minFunction



    public static double minRGBDecFunction(double red, double green, double blue) {
        double minValue;

        if (red < green) { minValue = red;}
        else { minValue = green;}
        if (blue < minValue) { minValue = blue;}
        return minValue;
    } // end method minRGBDecFunction


    public static float getPercentWhite(ColorPtArb colorPtArb) {
        float percentWhite = (float) minFunction(colorPtArb);
        return percentWhite;
    }
} // end class
