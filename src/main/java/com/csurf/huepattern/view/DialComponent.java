package com.csurf.huepattern.view;

import java.awt.Point;

/**
 * Pure logic component for calculating hue numbers from mouse click coordinates.
 * This is NOT a JComponent or JPanel - it has no rendering responsibility.
 * 
 * The DialComponent uses a sector-based approach with rounding tolerance to convert
 * mouse click positions on the APDisplayPanel to hue numbers (1-36).
 * 
 * The dial is conceptually divided into:
 * - 9 sectors of 40° each (for future visual feedback)
 * - 4 hues per sector (10° each)
 * - Fudge factor of -0.1 for rounding tolerance
 * 
 * Hue 1 is at 12 o'clock (-90°), and hues proceed clockwise at 10° intervals.
 */
public class DialComponent {
    
    // Center of the dial (same as APDisplayPanel center)
    private int centerX;
    private int centerY;
    
    // Hue circle parameters (36 hues, numbered 1-36)
    private static final int NUM_HUE_POSITIONS = 36;
    private static final double DEGREES_PER_HUE = 10.0;
    private static final double START_DEGREE = -90.0;  // Hue 1 at 12 o'clock
    
    // Sector parameters (from prototype pattern)
    private static final int NUM_SECTORS = 9;           // 360° / 40° per sector
    private static final double DEGREES_PER_SECTOR = 40.0;
    private static final int HUES_PER_SECTOR = 4;       // 40° / 10° per hue
    
    // Rounding tolerance
    private static final double FUDGE_FACTOR = 0.1;     // Creates tolerance zone
    
    // Ring radii (for hit detection)
    private static final int MIN_RADIUS = 100;  // Inner boundary
    private static final int MAX_RADIUS = 220;  // Outer boundary
    
    /**
     * Creates a new DialComponent with the specified center point.
     * @param centerX The X coordinate of the dial center
     * @param centerY The Y coordinate of the dial center
     */
    public DialComponent(int centerX, int centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }
    
    /**
     * Updates the center point of the dial.
     * Call this if the APDisplayPanel is resized.
     * @param centerX The new X coordinate
     * @param centerY The new Y coordinate
     */
    public void setCenter(int centerX, int centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }
    
    /**
     * Handles a mouse click and returns the corresponding hue number.
     * 
     * @param clickPoint The point where the user clicked on APDisplayPanel
     * @return The hue number (1-36), or -1 if click is outside the valid dial area
     */
    public int handleMouseClick(Point clickPoint) {
        // Validate input
        if (clickPoint == null) {
            return -1;
        }
        
        // Check if click is within the valid dial area
        if (!isClickWithinDial(clickPoint)) {
            return -1;
        }
        
        // Calculate angle from center to click point
        double angleDegrees = calculateAngleFromClick(clickPoint);
        
        // Convert angle to hue number using sector-based approach
        int hueNum = calculateHueNumFromAngleWithSectors(angleDegrees);
        
        return hueNum;
    }
    
    /**
     * Calculates the angle in degrees from the center to the click point.
     * 
     * @param clickPoint The click point
     * @return Angle in degrees (-180 to 180)
     *         -90° = 12 o'clock (top)
     *         0° = 3 o'clock (right)
     *         90° = 6 o'clock (bottom)
     *         180° or -180° = 9 o'clock (left)
     */
    private double calculateAngleFromClick(Point clickPoint) {
        double dx = clickPoint.x - centerX;
        double dy = clickPoint.y - centerY;
        
        // Math.atan2 returns angle in radians (-π to π)
        // 0 radians = 3 o'clock (East)
        // π/2 radians = 6 o'clock (South)
        // ±π radians = 9 o'clock (West)
        // -π/2 radians = 12 o'clock (North)
        double angleRadians = Math.atan2(dy, dx);
        
        // Convert to degrees
        double angleDegrees = Math.toDegrees(angleRadians);
        
        return angleDegrees;
    }
    
    /**
     * Calculates the radius (distance from center) to the click point.
     * 
     * @param clickPoint The click point
     * @return Distance from center to click point
     */
    private double calculateRadiusFromClick(Point clickPoint) {
        double dx = clickPoint.x - centerX;
        double dy = clickPoint.y - centerY;
        
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Checks if a click is within the valid dial area.
     * The valid area is an annulus (ring) between MIN_RADIUS and MAX_RADIUS.
     * 
     * @param clickPoint The click point
     * @return true if the click is within the valid dial area
     */
    private boolean isClickWithinDial(Point clickPoint) {
        double radius = calculateRadiusFromClick(clickPoint);
        return radius >= MIN_RADIUS && radius <= MAX_RADIUS;
    }
    
    /**
     * Converts an angle to a hue number using sector-based approach with rounding.
     * 
     * This method:
     * 1. Normalizes the angle so 0° = hue 1 at 12 o'clock
     * 2. Finds the closest sector (for future visual feedback)
     * 3. Calculates hueDecimal in 1-36 range
     * 4. Applies rounding with fudge factor for tolerance
     * 5. Handles wrap-around for the 1-36 range
     * 
     * @param angleDegrees The angle in degrees
     * @return The hue number (1-36)
     */
    private int calculateHueNumFromAngleWithSectors(double angleDegrees) {
        // Step 1: Normalize angle so 0° = hue 1 at 12 o'clock
        // Hue 1 is at -90° (12 o'clock), so we add 90 to shift the origin
        double normalizedAngle = angleDegrees + 90.0;
        
        // Step 1b: Normalize to 0-360 range to prevent overflow
        // This ensures hueDecimal stays in reasonable range before rounding
        while (normalizedAngle >= 360.0) {
            normalizedAngle -= 360.0;
        }
        while (normalizedAngle < 0.0) {
            normalizedAngle += 360.0;
        }
        
        // Step 2: Find closest sector (for future Phase 2 visual feedback)
        // Sectors are 40° each, with 9 sectors total
        int closestSector = findClosestSector(angleDegrees);
        
        // Step 3: Calculate hueDecimal in 1-36 range
        // Each hue is 10° apart, and we offset by 1 to get 1-36 range
        double hueDecimal = (normalizedAngle / DEGREES_PER_HUE) + 1.0;
        
        // Step 4: Apply rounding with fudge factor
        // Fudge factor creates tolerance zone and favors lower hue numbers
        double fudgedHueDecimal = hueDecimal - FUDGE_FACTOR;
        int hueNum = (int) Math.round(fudgedHueDecimal);
        
        // Step 5: Handle wrap-around for 1-36 range
        if (hueNum > 36) {
            hueNum -= 36;
        } else if (hueNum < 1) {
            hueNum += 36;
        }
        
        return hueNum;
    }
    
    /**
     * Finds the closest sector to the given angle.
     * Sectors are 40° each, with 9 sectors total (0-8).
     * 
     * @param angleDegrees The angle in degrees
     * @return The sector number (0-8)
     */
    private int findClosestSector(double angleDegrees) {
        // Normalize angle so 0° = hue 1 at 12 o'clock
        double normalizedAngle = angleDegrees + 90.0;
        
        // Calculate sector (40° per sector)
        double sectorAngle = normalizedAngle / DEGREES_PER_SECTOR;
        int sector = (int) Math.round(sectorAngle) % NUM_SECTORS;
        
        // Ensure sector is in valid range (0-8)
        if (sector < 0) {
            sector += NUM_SECTORS;
        }
        
        return sector;
    }
}
