package com.csurf.ostwald.domainmonotri.model;

import java.awt.Color;

/**
 * Stub class for monochromatic triangle operations.
 *  TODO 062025 this class is basically junk. But save method "findClosestStandardOstHue" for HLS hue360 calc (for use in test class)
 * Uses singleton pattern to ensure only one instance exists.
 */
public class MonoTriStub {
    private static MonoTriStub instance;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private MonoTriStub() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton instance of MonoTriStub.
     * Uses lazy initialization - singleton isn't created until first access.
     * @return the singleton instance
     */
    public static MonoTriStub getSingleton() {
        if (instance == null) {
            instance = new MonoTriStub();
        }
        return instance;
    }

    /**
     * Finds the closest standard Ostwald hue for a given color.
     * Converts RGB color to HSB and finds the closest match in the standard hue array.
     * 
     * @param arbColor The input color to find closest standard hue for
     * @return The index of the closest standard hue found
     */
    public int findClosestStandardOstHue(Color arbColor) {
        int closestHueIndexFound = 0;
        double closestDistanceSoFar = 50;

        // Convert to HSB color coordinates
        float[] hsbCoords = Color.RGBtoHSB(
            arbColor.getRed(), 
            arbColor.getGreen(),
            arbColor.getBlue(), 
            null
        );
        
        // Get hue value and convert to 0-360 range
        float hue = hsbCoords[0];
        double arbHue = 360 * hue; // TODO 062725 put this method in another class (maybe RGBCubeModel) - then delete this class

        // Get standard hue array from HueSpacingStdChoices
        int[] standardHues = HueSpacingStdChoices.getSingleton().getGUIStd36HueArray();

        // Find closest standard hue
        for (int counter = 1; counter < standardHues.length; counter++) {
            double currentHueDistance = Math.abs(arbHue - standardHues[counter]);
            if (currentHueDistance < closestDistanceSoFar) {
                closestDistanceSoFar = currentHueDistance;
                closestHueIndexFound = counter;
            }
        }

        return closestHueIndexFound;
    } // end method findClosestStandardOstHue


} // end class MonoTriStub
