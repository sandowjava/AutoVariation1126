package com.csurf.huepattern.view;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Marker displayed for each of the 36 hue positions in the Occupied ring.
 * Shows a dark gray circular shape indicating whether a hue position is occupied.
 * Each marker is logically connected to a specific hue number.
 * Will later include mouse-click methods for interaction.
 */
public class OccupiedMarker {
    private static final int MARKER_DIAMETER = 10;
    private static final Color MARKER_COLOR = Color.DARK_GRAY;
    private static final Color UNOCC_MARKER_COLOR = Color.LIGHT_GRAY.brighter();
    
    private final int hueNumUserGUI;

    /**
     * Constructor for OccupiedMarker.
     * @param userFacingHueNum The hue number (1-36) this marker represents
     */
    public OccupiedMarker(int userFacingHueNum) {
        this.hueNumUserGUI = userFacingHueNum;
    }
    
    /**
     * Gets the hue number associated with this marker.
     * @return The hue number (1-36)
     */
    public int getHueNumUserGUI() {
        return hueNumUserGUI;
    }

    /**
     * Draws the OccupiedMarker at the specified position.
     * 
     * @param g2d Graphics2D context
     * @param centerX X coordinate of center position
     * @param centerY Y coordinate of center position
     */
    public void draw(Graphics2D g2d, double centerX, double centerY) {
        // Default to occupied marker
        draw(g2d, centerX, centerY, true);
    }
    
    /**
     * Draws the OccupiedMarker at the specified position with occupied state.
     * Uses MARKER_COLOR for occupied hues and UNOCC_MARKER_COLOR for unoccupied hues.
     * 
     * @param g2d Graphics2D context
     * @param centerX X coordinate of center position
     * @param centerY Y coordinate of center position
     * @param isOccupied true if this hue is occupied in the palette, false otherwise
     */
    public void draw(Graphics2D g2d, double centerX, double centerY, boolean isOccupied) {
        // Create circle centered at the given position
        Ellipse2D.Double circle = new Ellipse2D.Double(
            centerX - MARKER_DIAMETER / 2.0,
            centerY - MARKER_DIAMETER / 2.0,
            MARKER_DIAMETER,
            MARKER_DIAMETER
        );
        
        // Fill with appropriate color based on occupied state
        Color fillColor = isOccupied ? MARKER_COLOR : UNOCC_MARKER_COLOR;
        g2d.setColor(fillColor);
        g2d.fill(circle);
        
        // Draw black outline
        g2d.setColor(Color.BLACK);
        g2d.draw(circle);
    }
    
    /**
     * Gets the diameter of the marker.
     * @return Marker diameter in pixels
     */
    public static int getMarkerDiameter() {
        return MARKER_DIAMETER;
    }
} // end class OccupiedMarker
