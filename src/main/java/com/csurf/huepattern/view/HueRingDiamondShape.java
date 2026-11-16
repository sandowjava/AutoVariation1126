package com.csurf.huepattern.view;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Shape displayed for each of the 36 hues in the Hue Circle ring.
 * Composed of smaller sub-shapes (squares) that can be individually colored.
 * Initial implementation: 4 squares (10x10 pixels each) forming a larger 20x20 square.
 * The overall shape is rotated 45 degrees for its basic orientation (diamond shape).
 */
public class HueRingDiamondShape {
    private static final int SUB_SHAPE_SIZE = 10;
    private static final int TOTAL_SIZE = SUB_SHAPE_SIZE * 2; // 20 pixels
    
    private int numShapeColors;
    private Color[] subShapeColors;
    
    // Four sub-shapes (small squares)
    private Rectangle2D.Double subShape1;
    private Rectangle2D.Double subShape2;
    private Rectangle2D.Double subShape3;
    private Rectangle2D.Double subShape4;

    /**
     * Constructor initializes the four sub-shapes in a 2x2 grid.
     * Top-left, top-right, bottom-left, bottom-right arrangement.
     */
    public HueRingDiamondShape() {
        // Initialize sub-shapes in 2x2 grid (relative coordinates)
        // Top-left
        subShape1 = new Rectangle2D.Double(0, 0, SUB_SHAPE_SIZE, SUB_SHAPE_SIZE);
        // Top-right
        subShape2 = new Rectangle2D.Double(SUB_SHAPE_SIZE, 0, SUB_SHAPE_SIZE, SUB_SHAPE_SIZE);
        // Bottom-left
        subShape3 = new Rectangle2D.Double(0, SUB_SHAPE_SIZE, SUB_SHAPE_SIZE, SUB_SHAPE_SIZE);
        // Bottom-right
        subShape4 = new Rectangle2D.Double(SUB_SHAPE_SIZE, SUB_SHAPE_SIZE, SUB_SHAPE_SIZE, SUB_SHAPE_SIZE);
        
        // Initialize with default number of colors
        numShapeColors = 4;
        subShapeColors = new Color[4];
        // Default to white
        for (int i = 0; i < 4; i++) {
            subShapeColors[i] = Color.WHITE;
        }
    }

    /**
     * Sets the number of colors for the shape.
     * @param numColors Number of colors (should equal number of sub-shapes)
     */
    public void setNumShapeColors(int numColors) {
        this.numShapeColors = numColors;
        this.subShapeColors = new Color[numColors];
        // Initialize with white
        for (int i = 0; i < numColors; i++) {
            subShapeColors[i] = Color.WHITE;
        }
    }

    /**
     * Sets the color for a specific sub-shape (1-indexed).
     * @param shapeIndex Index of the sub-shape (1-4)
     * @param color Color to set
     */
    public void setSubShapeColor(int shapeIndex, Color color) {
        if (shapeIndex >= 1 && shapeIndex <= numShapeColors) {
            subShapeColors[shapeIndex - 1] = color;
        }
    }

    /**
     * Sets all sub-shape colors at once.
     * @param colors Array of colors
     */
    public void setAllSubShapeColors(Color[] colors) {
        int count = Math.min(colors.length, numShapeColors);
        for (int i = 0; i < count; i++) {
            subShapeColors[i] = colors[i];
        }
    }

    /**
     * Draws the HuePaletteShape at the specified position with proper rotation.
     * The shape is first rotated 45 degrees (basic orientation as diamond),
     * then rotated according to the hue position.
     * 
     * @param g2d Graphics2D context
     * @param centerX X coordinate of center position
     * @param centerY Y coordinate of center position
     * @param hueNum Hue number (1-36)
     */
    public void draw(Graphics2D g2d, double centerX, double centerY, int hueNum) {
        // Save the original transform
        AffineTransform originalTransform = g2d.getTransform();
        
        // Calculate rotation angle
        // hueNum 1 is at 12 o'clock (top), which is -90 degrees from standard 0 degrees (3 o'clock)
        // Each hue step is 10 degrees clockwise
        // Base rotation: 45 degrees (to make it a diamond)
        // Position rotation: -90 + (hueNum - 1) * 10 degrees
        double baseRotation = Math.toRadians(45); // Diamond orientation
        double positionRotation = Math.toRadians(-90 + (hueNum - 1) * 10); // Position on circle
        double totalRotation = baseRotation + positionRotation;
        
        // Translate to center position
        g2d.translate(centerX, centerY);
        
        // Rotate around center
        g2d.rotate(totalRotation);
        
        // Translate so shape is centered at origin
        g2d.translate(-TOTAL_SIZE / 2.0, -TOTAL_SIZE / 2.0);
        
        // Draw each sub-shape with its color
        g2d.setColor(subShapeColors[0]);
        g2d.fill(subShape1);
        g2d.setColor(Color.BLACK);
        g2d.draw(subShape1);
        
        g2d.setColor(subShapeColors[1]);
        g2d.fill(subShape2);
        g2d.setColor(Color.BLACK);
        g2d.draw(subShape2);
        
        g2d.setColor(subShapeColors[2]);
        g2d.fill(subShape3);
        g2d.setColor(Color.BLACK);
        g2d.draw(subShape3);
        
        g2d.setColor(subShapeColors[3]);
        g2d.fill(subShape4);
        g2d.setColor(Color.BLACK);
        g2d.draw(subShape4);
        
        // Restore original transform
        g2d.setTransform(originalTransform);
    }

    /**
     * Gets the total size of the shape.
     * @return Total size in pixels
     */
    public static int getTotalSize() {
        return TOTAL_SIZE;
    }
} // end class HueRingDiamondShape
