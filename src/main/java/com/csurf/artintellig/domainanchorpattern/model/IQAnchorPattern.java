package com.csurf.artintellig.domainanchorpattern.model;

/**
 * Represents an Anchor Pattern in the IQ color system, managing the angular positions
 * of four rings that define color relationships. Each ring has its own angle value
 * and associated AGSpread (Artistic Goal Spread).
 * 
 * The pattern is used to:
 * 1. Define the angular relationships between AnchorGroups
 * 2. Store the AnchorGroup spreads = preads for each ring
 * 3. Provide a foundation for color harmony calculations
 */
public class IQAnchorPattern {
    private final double[] ringAngles; // TODO 052425 replace/clarify the "rings" idea = AnchorGroups each with their own AGSpread and some Anchor location
    private IQAGSpread[] arrayOfAGSpreads;
    private IQAnchorPatternAbstr theAnchorPatternAbstr;



    /**
     * Creates a new IQAnchorPattern with default angles (0 degrees) for all rings.
     * This constructor is temporary until the code is in place to create IQAGSpread[] someArrayOfAGSpreads.
     */
    public IQAnchorPattern() {
        ringAngles = new double[4]; // Initialize with 4 rings
    }

    /**
     * Creates a new IQAnchorPattern with the specified array of AGSpreads.
     * 
     * @param someArrayOfAGSpreads Array of AGSpreads, one for each ring
     */
    public IQAnchorPattern(IQAGSpread[] someArrayOfAGSpreads) {
        this();
        arrayOfAGSpreads = someArrayOfAGSpreads;
    }



    private void createAnchorPatternAbstr() {
        // TODO 052425 method stub - to be implemented -calculated from concrete APattern w AG1 always in 12:00 position
        // used for hueRotation display etc
    } // end method createAnchorPatternAbstr

    /**
     * Returns the complete array of AGSpreads.
     * 
     * @return Array of AGSpreads for all rings
     */
    public IQAGSpread[] getEntireArrayOfAGSpreads() {
        return arrayOfAGSpreads;
    }

    /**
     * Gets the AGSpread for ring 1.
     * 
     * @return AGSpread for ring 1, or null if not initialized
     */
    public IQAGSpread getAGSpread1() {
        return arrayOfAGSpreads != null ? arrayOfAGSpreads[0] : null;
    }

    /**
     * Gets the AGSpread for ring 2.
     * 
     * @return AGSpread for ring 2, or null if not initialized
     */
    public IQAGSpread getAGSpread2() {
        return arrayOfAGSpreads != null ? arrayOfAGSpreads[1] : null;
    }

    /**
     * Gets the AGSpread for ring 3.
     * 
     * @return AGSpread for ring 3, or null if not initialized
     */
    public IQAGSpread getAGSpread3() {
        return arrayOfAGSpreads != null ? arrayOfAGSpreads[2] : null;
    }

    /**
     * Gets the AGSpread for ring 4.
     * 
     * @return AGSpread for ring 4, or null if not initialized
     */
    public IQAGSpread getAGSpread4() {
        return arrayOfAGSpreads != null ? arrayOfAGSpreads[3] : null;
    }

    /**
     * Sets the angle for a specific ring.
     * 
     * @param ringNumber The ring number (1-4)
     * @param angle The angle in degrees
     * @throws IllegalArgumentException if ringNumber is not between 1 and 4
     */
    public void setAngle(int ringNumber, double angle) {
        if (ringNumber < 1 || ringNumber > 4) {
            throw new IllegalArgumentException("Ring number must be between 1 and 4");
        }
        ringAngles[ringNumber - 1] = angle;
    }

    /**
     * Gets the angle for a specific ring.
     * 
     * @param ringNumber The ring number (1-4)
     * @return The angle in degrees
     * @throws IllegalArgumentException if ringNumber is not between 1 and 4
     */
    public double getAngle(int ringNumber) {
        if (ringNumber < 1 || ringNumber > 4) {
            throw new IllegalArgumentException("Ring number must be between 1 and 4");
        }
        return ringAngles[ringNumber - 1];
    }
}
