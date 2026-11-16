package com.csurf.huepattern.view;

import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.ostwald.domainmonotri.model.MonotriPage;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.templatecore.model.PaletteWName;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

/**
 * Display panel for the AnchorPattern visualization.
 * Shows 6 concentric rings representing the hue circle and anchor groups.
 * Fixed size: 460x460 pixels.
 */
public class APDisplayPanel extends JPanel {
    private final JLabel infoAG2Label; // Label to display RING LOCATION information
    private final JLabel infoAG4Label; // Label to display RING LOCATION information
    private final JLabel infoAG3Label; // Label to display RING LOCATION information
    private final JLabel infoAG1Label; // Label to display RING LOCATION information
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 460;
    private static final int CENTER_X = PANEL_WIDTH / 2;
    private static final int CENTER_Y = PANEL_HEIGHT / 2;
    
    private AGRingDisplayState AGRingDisplayState;
    private boolean[] occupiedHues; // Array tracking which hues (1-36) are occupied in the palette
    
    // Ring specifications
    private static final int OCCUPIED_RING_WIDTH = 15;
    private static final int HUE_CIRCLE_RING_WIDTH = 35;
    private static final int AG_RING_WIDTH = 12;
    private static final int NUM_HUE_POSITIONS = 36;
    private static final double DEGREES_PER_HUE = 10.0;
    
    // Ring radii (from center to middle of ring)
    // Ring structure from inside out: Innermost, Hue Circle, AG1, AG2, AG3, AG4
    private static final int OCCUPIED_RING_RADIUS = 150;
    private static final int HUE_CIRCLE_RING_RADIUS = 170;
    private static final int AG1_RING_RADIUS = 195;
    private static final int AG2_RING_RADIUS = 215; // 110225 WAS 130
    private static final int AG3_RING_RADIUS = 130; // 110225 WAS 195
    private static final int AG4_RING_RADIUS = 110; // 110225 WAS 215

    public APDisplayPanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.white);
        setLayout(null); // Using null layout for precise positioning

        int fontSize = 12;
        // Initialize the infoAG2Label label
        infoAG2Label = new JLabel("outermost= AG2");
        infoAG2Label.setFont(new Font("Serif", Font.BOLD, fontSize));
        infoAG2Label.setForeground(Color.GRAY);
        infoAG2Label.setBounds(1, 21, 150, 20);
        add(infoAG2Label);

        // Initialize the infoAG4Label label
        infoAG4Label = new JLabel("innermostARC= AG4");
        infoAG4Label.setFont(new Font("Serif", Font.BOLD, fontSize));
        infoAG4Label.setForeground(Color.GRAY);
        infoAG4Label.setBounds(10, 1, 150, 20);
        add(infoAG4Label);

        // Initialize the infoAG3Label label
        infoAG3Label = new JLabel("AG3 inside");
        infoAG3Label.setFont(new Font("Serif", Font.BOLD, fontSize));
        infoAG3Label.setForeground(Color.GRAY);
        infoAG3Label.setBounds(10, 41, 150, 20);
        add(infoAG3Label);

        // Initialize the infoAG1Label label
        infoAG1Label = new JLabel("AG1 outside");
        infoAG1Label.setFont(new Font("Serif", Font.BOLD, fontSize));
        infoAG1Label.setForeground(Color.GRAY);
        infoAG1Label.setBounds(10, 31, 150, 20);
        add(infoAG1Label);
    }
    
    /**
     * Sets the DisplayState for this panel.
     * @param AGRingDisplayState The DisplayState containing spread data to render
     */
    public void setDisplayState(AGRingDisplayState AGRingDisplayState) {
        this.AGRingDisplayState = AGRingDisplayState;
    }
    
    /**
     * Sets the palette and builds the occupiedHues array from it.
     * This should be called when the AnchorPatternFrame opens to show the actual palette state.
     * @param palette The PaletteWName to analyze for occupied hues
     */
    public void setPaletteAndBuildOccupiedHues(PaletteWName palette) {
        if (palette != null) {
            this.occupiedHues = buildOccupiedHuesArray(palette);
        } else {
            // If palette is null, initialize empty array (all unoccupied)
            this.occupiedHues = new boolean[36];
        }
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        // Keep the label in the top-left corner when panel is resized
        if (infoAG2Label != null) {
            infoAG2Label.setLocation(10, 10);
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable anti-aliasing for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Get actual center accounting for border insets
        Insets insets = getInsets();
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;
        int actualCenterX = insets.left + width / 2;
        int actualCenterY = insets.top + height / 2;
        
        // Draw ring boundaries for visualization
        drawRingBoundaries(g2d, actualCenterX, actualCenterY);

        // Draw the hue numbers
        drawDiameterHueNums(g2d, actualCenterX, actualCenterY);

        // 110225 "star" of diameters added for arc-length clarity
        drawDiameterLines(g2d, actualCenterX, actualCenterY);
        
        // Draw the Occupied ring
        drawOccupiedRing(g2d, actualCenterX, actualCenterY);
        
        // Draw the Hue Circle ring
        drawHueCircleRing(g2d, actualCenterX, actualCenterY);
        
        // Draw AG rings if DisplayState is available
        if (AGRingDisplayState != null) {
            drawAGRings(g2d, actualCenterX, actualCenterY);
        }
        

    }
    
    /**
     * Draws the boundaries of all six rings for visualization.
     * @param g2d Graphics context
     * @param centerX Actual center X coordinate accounting for insets
     * @param centerY Actual center Y coordinate accounting for insets
     */
    /**
     * Draws a set of diameters to help visualize hue positions.
     * @param g2d Graphics2D context
     * @param centerX X-coordinate of the center
     * @param centerY Y-coordinate of the center
     */
    private void drawDiameterLines(Graphics2D g2d, int centerX, int centerY) {
        // Save the original stroke and color
        Stroke originalStroke = g2d.getStroke();
        Color originalColor = g2d.getColor();
        
        // Set the drawing parameters
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(1.0f));
        
        // Calculate the number of diameters (36/4 = 9 diameters)
        int numDiams = NUM_HUE_POSITIONS / 4;
        double angleStep = 180.0 / numDiams;  // Angle between diameters in degrees
        
        // Calculate the radius for the diameters
        double radius = AG2_RING_RADIUS +5;
        
        // Draw each diameter
        for (int i = 0; i < numDiams; i++) {
            // Calculate the angle for this diameter (in radians)
            double angle = Math.toRadians(i * angleStep);
            
            // Calculate the end points of the diameter
            double x1 = centerX + radius * Math.sin(angle);
            double y1 = centerY - radius * Math.cos(angle);  // Negative because y increases downward
            double x2 = centerX - radius * Math.sin(angle);
            double y2 = centerY + radius * Math.cos(angle);
            
            // Draw the diameter
            g2d.drawLine((int)Math.round(x1), (int)Math.round(y1), 
                        (int)Math.round(x2), (int)Math.round(y2));
        }
        
        // Restore the original stroke and color
        g2d.setStroke(originalStroke);
        g2d.setColor(originalColor);
    }
    
    /**
     * Draws odd numbers 1-35 in a ring around the center.
     * @param g2d Graphics2D context
     * @param centerX X-coordinate of the center
     * @param centerY Y-coordinate of the center
     */
    private void drawDiameterHueNums(Graphics2D g2d, int centerX, int centerY) {
        // Save the original graphics context
        Color originalColor = g2d.getColor();
        Font originalFont = g2d.getFont();
        Object originalAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        
        try {
            // Set up the graphics context
            int fontSize = 12;
            g2d.setColor(Color.DARK_GRAY);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // Draw first set of numbers (inner ring)
            drawNumberRing(g2d, centerX, centerY, AG4_RING_RADIUS - 20, fontSize); // smallest ring
            
            // Draw second set of numbers (outer ring)
            drawNumberRing(g2d, centerX, centerY, AG1_RING_RADIUS + 7, fontSize);
        } finally {
            // Restore the original graphics context
            g2d.setColor(originalColor);
            g2d.setFont(originalFont);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAntialias);
        }
    }
    
    /**
     * Helper method to draw a ring of numbers at a given radius
     * @param g2d Graphics2D context
     * @param centerX X-coordinate of the center
     * @param centerY Y-coordinate of the center
     * @param radius Radius for the number ring
     * @param fontSize Font size for the numbers
     */
    private void drawNumberRing(Graphics2D g2d, int centerX, int centerY, int radius, int fontSize) {
        // Save the original graphics context
        Color originalColor = g2d.getColor();
        Font originalFont = g2d.getFont();
        Object originalAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        
        try {
            // Set up the graphics context for this ring
            g2d.setColor(Color.DARK_GRAY);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            int numDiams = 18; // 18 odd numbers from 1-35
            double angleStep = 2 * Math.PI / numDiams;
            
            for (int i = 0; i < numDiams; i++) {
                // Calculate the angle for this number (in radians)
                double angle = i * angleStep;
                
                // Calculate the position for this number
                int x = (int) Math.round(centerX + radius * Math.sin(angle));
                int y = (int) Math.round(centerY - radius * Math.cos(angle));
                
                // Get the number to display (1, 3, 5, ..., 35)
                int number = 1 + i * 2;
                
                // Save the current transform
                AffineTransform oldTransform = g2d.getTransform();
                
                try {
                    // Move to the number's position and rotate the context
                    g2d.translate(x, y);
                    // Add 90 degrees to make text follow the circle, and add 180 degrees (Math.PI) if number < 21
                    double rotation = angle + Math.PI/2;
                    if (number < 21) {
                        rotation += Math.PI; // Add 180 degrees for numbers < 21
                    }
                    g2d.rotate(rotation);
                    
                        // Draw the number centered at the point
                    String numStr = String.valueOf(number);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(numStr);
                    int textHeight = fm.getAscent();
                    g2d.drawString(numStr, -textWidth/2, textHeight/3); // Adjust Y for better centering
                } finally {
                    // Restore the transform
                    g2d.setTransform(oldTransform);
                }
            }
        } finally {
            // Restore the original graphics context
            g2d.setColor(originalColor);
            g2d.setFont(originalFont);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAntialias);
        }
    } // end method drawNumberRing



    
    private void drawRingBoundaries(Graphics2D g2d, int centerX, int centerY) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1.0f));
        
        // Draw occupied ring boundaries
        // drawCircle(g2d, centerX, centerY, OCCUPIED_RING_RADIUS + OCCUPIED_RING_WIDTH / 2.0); // 110225 took out
        // drawCircle(g2d, centerX, centerY, OCCUPIED_RING_RADIUS - OCCUPIED_RING_WIDTH / 2.0); // 110225 took out
        
        // Draw Hue Circle ring boundaries
        // drawCircle(g2d, centerX, centerY, HUE_CIRCLE_RING_RADIUS + HUE_CIRCLE_RING_WIDTH / 2.0); // 110225 took out
        drawCircle(g2d, centerX, centerY, HUE_CIRCLE_RING_RADIUS - HUE_CIRCLE_RING_WIDTH / 2.0);
        
        // Draw AG1 ring boundaries
        drawCircle(g2d, centerX, centerY, AG1_RING_RADIUS + AG_RING_WIDTH / 2.0);
        drawCircle(g2d, centerX, centerY, AG1_RING_RADIUS - AG_RING_WIDTH / 2.0);
        
        // Draw AG2 ring boundaries
        drawCircle(g2d, centerX, centerY, AG2_RING_RADIUS + AG_RING_WIDTH / 2.0);
        drawCircle(g2d, centerX, centerY, AG2_RING_RADIUS - AG_RING_WIDTH / 2.0);
        
        // Draw AG3 ring boundaries
        drawCircle(g2d, centerX, centerY, AG3_RING_RADIUS + AG_RING_WIDTH / 2.0);
        drawCircle(g2d, centerX, centerY, AG3_RING_RADIUS - AG_RING_WIDTH / 2.0);
        
        // Draw AG4 ring boundaries
        drawCircle(g2d, centerX, centerY, AG4_RING_RADIUS + AG_RING_WIDTH / 2.0);
        drawCircle(g2d, centerX, centerY, AG4_RING_RADIUS - AG_RING_WIDTH / 2.0);
    }
    
    /**
     * Helper method to draw a circle.
     */
    private void drawCircle(Graphics2D g2d, double centerX, double centerY, double radius) {
        Ellipse2D.Double circle = new Ellipse2D.Double(
            centerX - radius, centerY - radius,
            radius * 2, radius * 2
        );
        g2d.draw(circle);
    }
    
    /**
     * Draws the Hue Circle ring with HuePaletteShapes at each of the 36 hue positions.
     * HuePaletteShapes are rotated properly for each position.
     * The Hue Circle ring is the second ring from center at radius ~160 pixels.
     * @param g2d Graphics context
     * @param centerX Actual center X coordinate accounting for insets
     * @param centerY Actual center Y coordinate accounting for insets
     */
    private void drawHueCircleRing(Graphics2D g2d, int centerX, int centerY) {
        // Create HuePaletteShapes for each hue position
        for (int hueNum = 1; hueNum <= NUM_HUE_POSITIONS; hueNum++) {
            // Calculate position on the ring
            // hueNum 1 is at 12 o'clock (top)
            // Positions proceed clockwise
            double angleDegrees = -90 + (hueNum - 1) * DEGREES_PER_HUE; // -90 to start at top  // Note: "hueNum - 1"
            double angleRadians = Math.toRadians(angleDegrees);
            
            // Calculate center position for this hue on the Hue Circle ring
            double x = centerX + HUE_CIRCLE_RING_RADIUS * Math.cos(angleRadians);
            double y = centerY + HUE_CIRCLE_RING_RADIUS * Math.sin(angleRadians);
            
            // Create and draw HuePaletteShape
            HueRingDiamondShape shape = new HueRingDiamondShape();
            
            // For now, use placeholder colors (will be replaced with actual hue colors)
            // Using rainbow colors for demonstration
            // Color[] colors = getPlaceholderColors(hueNum);
            Color[] colors = getOstdotRGBColors(hueNum); // 110225 new method
            shape.setAllSubShapeColors(colors);
            
            // Draw the shape at this position with proper rotation
            shape.draw(g2d, x, y, hueNum);
        }
    }
    
    /**
     * Draws the Occupied ring with OccupiedMarkers at each of the 36 hue positions.
     * OccupiedMarkers show which hue positions are occupied in the palette.
     * Uses the occupiedHues array to determine which hues are occupied.
     * @param g2d Graphics context
     * @param centerX Actual center X coordinate accounting for insets
     * @param centerY Actual center Y coordinate accounting for insets
     */
    private void drawOccupiedRing(Graphics2D g2d, int centerX, int centerY) {
        // Create OccupiedMarkers for each hue position
        for (int hueNum = 1; hueNum <= NUM_HUE_POSITIONS; hueNum++) {
            // Calculate position on the ring
            // hueNum 1 is at 12 o'clock (top)
            // Positions proceed clockwise
            double angleDegrees = -90 + (hueNum - 1) * DEGREES_PER_HUE; // -90 to start at top // Note: "hueNum - 1"
            double angleRadians = Math.toRadians(angleDegrees);
            
            // Calculate center position for this hue on the Occupied ring
            double x = centerX + OCCUPIED_RING_RADIUS * Math.cos(angleRadians);
            double y = centerY + OCCUPIED_RING_RADIUS * Math.sin(angleRadians);
            
            // Determine if this hue is occupied
            boolean isOccupied = (occupiedHues != null && occupiedHues[hueNum - 1]);
            
            // Create and draw OccupiedMarker with its hue number and occupied state
            OccupiedMarker marker = new OccupiedMarker(hueNum);
            marker.draw(g2d, x, y, isOccupied);
        }
    }
    
    /**
     * Draws all AG rings based on the current DisplayState.
     * Iterates through the APLevel and draws each defined AG ring.
     * @param g2d Graphics context
     * @param centerX Actual center X coordinate accounting for insets
     * @param centerY Actual center Y coordinate accounting for insets
     */
    private void drawAGRings(Graphics2D g2d, int centerX, int centerY) {
        int apLevel = AGRingDisplayState.getApLevel();
        
        // Draw each AG ring up to the APLevel
        for (int whichAG = 1; whichAG <= apLevel; whichAG++) {
            if (AGRingDisplayState.hasSpread(whichAG)) {
                AGRingDisplayState.SpreadData spread = AGRingDisplayState.getSpreadData(whichAG);
                if (spread != null) {
                    drawAnchorGroupArc(g2d, centerX, centerY, whichAG, 
                                       spread.cCWLimitHue, spread.anchorHue, spread.cWLimitHue);
                }
            }
        }
    } // end method drawAGRings



    
    /**
     * Draws an anchor group arc for one of the four AG rings.
     * Arc starts at CWLimit and extends counter-clockwise to CCWLimit.
     * This naturally includes the anchor, which lies between the two limits.
     * Also draws a 20px circle at the anchorHue position.
     * 
     * Note: Java's drawArc has a quirk where the angle parameter appears to be negated.
     * We compensate by negating our calculated angle before passing it to drawArc.
     * 
     * @param g2d Graphics context
     * @param centerX Actual center X coordinate accounting for insets
     * @param centerY Actual center Y coordinate accounting for insets
     * @param whichAG Which anchor group (1-4)
     * @param cCWLimitHue Counter-clockwise limit hue number (1-36)
     * @param anchorHue Anchor hue number (1-36) - determines color
     * @param cWLimitHue Clockwise limit hue number (1-36)
     */
    private void drawAnchorGroupArc(Graphics2D g2d, int centerX, int centerY, 
                                    int whichAG, int cCWLimitHue, int anchorHue, int cWLimitHue) {
        // Determine radius based on which AG ring
        int radius;
        switch (whichAG) {
            case 1: radius = AG1_RING_RADIUS; break;
            case 2: radius = AG2_RING_RADIUS; break;
            case 3: radius = AG3_RING_RADIUS; break;
            case 4: radius = AG4_RING_RADIUS; break;
            default:
                throw new IllegalArgumentException("whichAG must be 1-4, got: " + whichAG);
        } // end switch


        
        // Get the color from the anchorHue
        Color[] colors = getOstdotRGBColors(anchorHue); // 110225 getting colors from MonotriBook
        Color arcColor = colors[0];
        
        // Calculate number of steps going CW from CCWLimit to CWLimit
        // This gives us how many steps clockwise (increasing hue numbers) from CCW to CW
        int numSteps = com.csurf.huepattern.util.HueCircleStepCalc.calcNumStepsGoingCW(cCWLimitHue, cWLimitHue);
        
        // Calculate start angle (CWLimit position)
        // hueNum 1 is at 12 o'clock (top), -90 degrees from standard 0 (3 o'clock)
        // NOTE: drawArc appears to draw from (startAngle+extent) back to startAngle,
        // so we set start to CWLimit and use positive extent to reach CCWLimit
        double startAngleDegrees = -90 + (cWLimitHue - 1) * DEGREES_PER_HUE; // Note: "cWLimitHue - 1"
        
        // Calculate arc extent
        // Positive extent will draw CCW from CWLimit toward CCWLimit
        double arcExtent;
        if (numSteps == 0) {
            // Edge case: single-hue spread (CCWLimit = CWLimit = Anchor)
            // Draw small indicator arc
            arcExtent = 10.0;
            startAngleDegrees -= 5.0;  // Adjust start to center it
        } else {
            // Normal case: positive extent to span from CWLimit to CCWLimit going CCW
            arcExtent = numSteps * DEGREES_PER_HUE;
        }
        
        // Draw the arc with 4px width
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g2d.setColor(arcColor);
        
        // Arc bounding box
        int arcDiameter = radius * 2;
        int arcX = centerX - radius;
        int arcY = centerY - radius;
        
        // important BUG FIX: Java's drawArc negates the angle parameter, so we negate it to compensate
        g2d.drawArc(arcX, arcY, arcDiameter, arcDiameter, (int) -startAngleDegrees, (int) arcExtent);
        
        // Restore original stroke
        g2d.setStroke(oldStroke);

        // TODO 110225 new method "createAnchorMarker(currAGNum)" for instance of class "AnchorMarker" at anchor position
        // Draw 20px circle at anchor position // TODO 110225 new class "AnchorMarker" w mouseListener for AG-drag
        double anchorAngleDegrees = -90 + (anchorHue - 1) * DEGREES_PER_HUE; // Note: "anchorHue - 1"
        double anchorAngleRadians = Math.toRadians(anchorAngleDegrees);
        
        double anchorX = centerX + radius * Math.cos(anchorAngleRadians);
        double anchorY = centerY + radius * Math.sin(anchorAngleRadians);
        
        g2d.setColor(arcColor);
        g2d.fillOval((int) (anchorX - 10), (int) (anchorY - 10), 20, 20);


    } // end method drawAnchorGroupArc



    
    /**
     * Gets placeholder colors for a hue position.
     * This will be replaced with actual hue colors from MonotriBook.
     */
    private Color[] getPlaceholderColors(int hueNum) {
        // Generate a rainbow color based on hue number
        float hue = (hueNum - 1) / (float) NUM_HUE_POSITIONS; // Note: "hueNum - 1"
        Color baseColor = Color.getHSBColor(hue, 0.8f, 0.9f);
        
        // For now, use variations of the base color
        return new Color[] {
            baseColor,
            baseColor.brighter(),
            baseColor.darker(),
            baseColor
        };
    } // end method getPlaceholderColors



    /**
     * Gets placeholder colors for a hue position.
     * This will be replaced with actual hue colors from MonotriBook.
     */
    private Color[] getOstdotRGBColors(int hueNum) {
        MonotriPage currMonoTriPage = MonotriBook.getInstance().getMonoTriPage(hueNum -1);

        Color closeToPureColor = currMonoTriPage.getColorByFillOrder(22).getAsColor();
        Color clearWhiteColor = currMonoTriPage.getColorByFillOrder(17).getAsColor();
        Color clearBlackColor = currMonoTriPage.getColorByFillOrder(5).getAsColor();
        // For now, use variations of the base color
        return new Color[] {
                closeToPureColor,
                clearWhiteColor,
                clearBlackColor,
                closeToPureColor
        };
    } // end method getPlaceholderColors

    /**
     * Builds a boolean array indicating which hues are occupied in the palette.
     * Analyzes the palette's colors and marks each hue position as occupied or unoccupied.
     * 
     * @param palette The PaletteWName to analyze
     * @return A boolean array of size 36, where index i represents hue number (i+1)
     *         true = occupied, false = unoccupied
     */
    private boolean[] buildOccupiedHuesArray(PaletteWName palette) {
        boolean[] occupiedHues = new boolean[36]; // Initialize all as unoccupied (false)
        
        // Get all colors from the palette
        ColorPtOstDot[] colors = palette.getColorPtOstdotArray();
        
        if (colors != null) {
            // For each color in the palette, mark its hue as occupied
            for (ColorPtOstDot color : colors) {
                // Get the hue information from the color
                int codeHueNum = color.getOstHCS().getStdCodeHueNum(); // Returns 0-35
                
                // Convert code-facing hue number to user-facing hue number (1-36)
                HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(codeHueNum + 1);
                int userFacingHueNum = hueNumGUI.getUserFacingHueNum();
                
                // Mark this hue as occupied in the array
                occupiedHues[userFacingHueNum - 1] = true;
            }
        }
        
        return occupiedHues;
    } // end method buildOccupiedHuesArray



} // end class APDisplayPanel
