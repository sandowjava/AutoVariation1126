package com.csurf.ostwald.domaingeo.model;

import java.awt.Color;

/**
 * Represents a color point in arbitrary color space.
 * Extends Java Color class to provide additional functionality for color-related geometric operations.
 * This class is designed to be immutable - all fields are final and methods are final to prevent overriding.
 */
public class ColorPtArb extends Color {
    private final double lum3D;
    private final double lumPlanar;
    private final double theRDec;
    private final double theGDec;
    private final double theBDec;

    /**
     * Creates a new ColorPtArb instance from a Java Color.
     * @param color The Java Color to represent
     * @throws IllegalArgumentException if color is null
     */
    public ColorPtArb(final Color color) {
        super(color.getRGB());
        if (color == null) {
            throw new IllegalArgumentException("Color cannot be null");
        }
        
        // Calculate luminance values using RGBCubeModel
        double calculatedLum3D = RGBCubeModel.calcLum3D(this);
        double calculatedLumPlanar = RGBCubeModel.calcLum(this);
        
        // Validate results
        if (calculatedLum3D < 0.0 || calculatedLum3D > 1.0) {
            throw new IllegalStateException("Calculated 3D luminance " + calculatedLum3D + 
                " is outside valid range [0.0, 1.0]");
        }
        if (calculatedLumPlanar < 0.0 || calculatedLumPlanar > 1.0) {
            throw new IllegalStateException("Calculated planar luminance " + calculatedLumPlanar + 
                " is outside valid range [0.0, 1.0]");
        }
        
        this.lum3D = calculatedLum3D;
        this.lumPlanar = calculatedLumPlanar;

        // 051525 added
        this.theRDec = (double) color.getRed() / 255.0;
        this.theGDec = (double) color.getGreen() / 255.0;
        this.theBDec = (double) color.getBlue() / 255.0;
    } // end constructor



    /**
     * Creates a new ColorPtArb instance from RGB values.
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     */
    public ColorPtArb(final int r, final int g, final int b) {
        super(r, g, b);

        // 051525 added
        this.theRDec = (double) r / 255.0;
        this.theGDec = (double) g / 255.0;
        this.theBDec = (double) b / 255.0;
        
        // Calculate luminance values using RGBCubeModel
        double calculatedLum3D = RGBCubeModel.calcLum3D(this);
        double calculatedLumPlanar = RGBCubeModel.calcLum(this);
        
        // Validate results
        if (calculatedLum3D < 0.0 || calculatedLum3D > 1.0) {
            throw new IllegalStateException("Calculated 3D luminance " + calculatedLum3D + 
                " is outside valid range [0.0, 1.0]");
        }
        if (calculatedLumPlanar < 0.0 || calculatedLumPlanar > 1.0) {
            throw new IllegalStateException("Calculated planar luminance " + calculatedLumPlanar + 
                " is outside valid range [0.0, 1.0]");
        }
        
        this.lum3D = calculatedLum3D;
        this.lumPlanar = calculatedLumPlanar;
    } // end constructor



    // 051525 added
    public ColorPtArb(double rDec, double gDec, double bDec) {
        super((int) (rDec * 255), (int) (gDec * 255), (int) (bDec * 255));

        theRDec = rDec;
        theGDec = gDec;
        theBDec = bDec;

        // Calculate luminance values using RGBCubeModel
        double calculatedLum3D = RGBCubeModel.calcLum3D(this);
        double calculatedLumPlanar = RGBCubeModel.calcLum(this);

        // Validate results
        if (calculatedLum3D < 0.0 || calculatedLum3D > 1.0) {
            throw new IllegalStateException("Calculated 3D luminance " + calculatedLum3D +
                    " is outside valid range [0.0, 1.0]");
        }
        if (calculatedLumPlanar < 0.0 || calculatedLumPlanar > 1.0) {
            throw new IllegalStateException("Calculated planar luminance " + calculatedLumPlanar +
                    " is outside valid range [0.0, 1.0]");
        }

        this.lum3D = calculatedLum3D;
        this.lumPlanar = calculatedLumPlanar;
    } // end constructor


    public static ColorPtArb createColorPtArbFromHLSNums(int hDecInt, double lInput, double sInput) {

        // Convert HLS values to RGB // copied from RGBCubeSpaceM2
        double p1; // vectorworks
        double p2; // vectorworks
        double rDec;
        double gDec;
        double bDec;

        if (lInput <= 0.5) { p2 = lInput*(1+sInput);}
        else {p2 = lInput + sInput -(lInput*sInput); }

        p1 = 2*lInput -p2;

        if (sInput == 0) { rDec= lInput; gDec= lInput; bDec= lInput;}
        else { rDec = rgbFunction(p1, p2, (hDecInt + 120));  // 042322 waffled on "hDecInt" type: INT/double
            gDec = rgbFunction(p1, p2, hDecInt);
            bDec = rgbFunction(p1, p2, (hDecInt - 120));
        }

        ColorPtArb newColorPtArb = new ColorPtArb(rDec, gDec, bDec); // TODO 051525 check all final attributes handled/stored by this constructor call

        return newColorPtArb;
    } // end method createColorPtArbFromHLSNums


    // TODO 051525 should this method "rgbFunction" be in this class? Probably not (legacy code - used elsewhere)
    public static double rgbFunction(double q1, double q2, double hue)
    // vectorworks function inside HLStoRGB
    {
        double rgb;

        if (hue > 360) {
            hue = hue -360;
        }
        if (hue < 0) {
            hue = hue + 360;
        }
        if (hue < 60) {
            rgb = q1 + ((q2 - q1)* hue)/60;
        } else if (hue < 180) {
            rgb = q2;} else if (hue < 240) {
            rgb = q1 + ((q2 - q1)* (240-hue))/60;}
        else {rgb = q1;} // end compound if
        return rgb;

    } // end rgbFunction


    /**
     * Avoid duplicate code by going to RGBCubeModel and calling the method.
     * Return the absolute saturation value for this color, which is the "percentage" of pure hue in any given color.
     * This is the color "remainder" which is not the black or white component.
     * In the case of pure colors, the abSat = 1.00, while all other colors have abSat < 1.00 (with Black or White = 0)
     * This percentage of color is used to calculate the "CMYK print luminance," which is different from the onscreen
     * RGB luminance.
     * @return
     */
    public double getAbSat() {
        double abSat = RGBCubeModel.getAbSat(this);
        return abSat;
    } // end method getAbSat



    /**
     * Returns this color as a simple Java Color instance.
     * This is useful when you need to work with APIs that expect a basic Color object.
     * @return a new Color instance with the same RGB values
     */
    public final Color getAsColor() {
        return new Color(getRGB());
    }

    /**
     * Gets the 3D luminance value calculated for this color.
     * The value is guaranteed to be between 0.0 and 1.0 inclusive.
     * @return the 3D luminance value in range [0.0, 1.0]
     */
    public final double getLum3D() {
        return lum3D;
    }

    /**
     * Gets the planar luminance value calculated for this color.
     * The value is guaranteed to be between 0.0 and 1.0 inclusive.
     * @return the planar luminance value in range [0.0, 1.0]
     */
    public final double getLumPlanar() {
        return lumPlanar;
    }



    public double getTheRDec() {
        return theRDec;}

    public double getTheGDec() {
        return theGDec;}

    public double getTheBDec() {
        return theBDec;}


    public float getPercentWhite() {
        float percentWhite = RGBCubeModel.getPercentWhite(this);
        return percentWhite;
    }
} // end class ColorPtArb
