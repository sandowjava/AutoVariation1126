package com.csurf.artintellig.model;

import com.csurf.artintellig.view.CircMouserView;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * A prototype class for experimenting with circular dial interactions.
 * Renders a basic circle along with hit points and sector points for
 * interaction testing and visualization.
 */
public class CircDialTryoutMouser extends JComponent {
    private final int PANEL_SQ_DIM;
    private final int OUTR_CIRC_DIAMETER;
    private final int HIT_CIRCLE_DIAMETER;
    private static final int START_DEGREE_OFFSET = 0;
    private static final double HIT_POINT_SPACING = 2.5; // degrees
    private static final double SECTOR_POINT_SPACING = 40.0; // degrees
    private static final int NUM_HIT_POINTS = 144; // 360/2.5
    private static final int NUM_SECTOR_POINTS = 9; // 360/40
    // Sector has "big offset" so that it is in the middle of the hit points e.g. sector 0 gets hitpoints 0-15, sector 1 gets hitpoints 16-31
    // Sector has "little offset adjustment" so that it is not right on top of any hit points
    private static final double START_SECTOR_OFFSET = (HIT_POINT_SPACING *8)-1;

    private final Ellipse2D.Double outerCircle;
    private final HitMarkerForMouse[] hitPointArray;
    private final SectorMarkerForMouse[] sectorPointArray;
    private final CircMouserView theViewFrame;
    private Point centerPoint;

    private static BufferedImage backgroundImage;
    private static final String IMAGE_PATH = "/images/hue_circle_051325_Cascade_DIAL_GUI_v1.png";
    
    private HitMarkerForMouse currentSelectedHit = null;

    static {
        try {
            // Try multiple ways to load the image
            java.net.URL imageUrl = CircDialTryoutMouser.class.getResource(IMAGE_PATH);
            if (imageUrl != null) {
                backgroundImage = ImageIO.read(imageUrl);
            } else {
                // Try class loader if direct resource loading fails
                imageUrl = CircDialTryoutMouser.class.getClassLoader().getResource(IMAGE_PATH);
                if (imageUrl != null) {
                    backgroundImage = ImageIO.read(imageUrl);
                } else {
                    System.err.println("Failed to load background image: " + IMAGE_PATH);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading background image: " + e.getMessage());
        }
    } // end static block



    /**
     * Immutable class representing a hit marker point with its location and number.
     * This is an inner class within the CircDialTryoutMouser class.
     */
    private static final class HitMarkerForMouse {
        private final Point locationPt;
        private final int markerPtNumber;
        
        private HitMarkerForMouse(Point locationPt, int markerPtNumber) {
            this.locationPt = new Point(locationPt);
            this.markerPtNumber = markerPtNumber;
        }



        public Point getLocationPt() {
            return new Point(locationPt);
        }
        
        public int getMarkerPtNumber() {
            return markerPtNumber;
        }
    }
    
    /**
     * Immutable class representing a sector marker point with its location and number.
     * This is an inner class within the CircDialTryoutMouser class.
     */
    private static final class SectorMarkerForMouse {
        private final Point locationSectorPt;
        private final int markerSectorPtNumber;
        private final HitMarkerForMouse[] localHitMarkerArray;
        
        private SectorMarkerForMouse(Point locationSectorPt, int markerSectorPtNumber, HitMarkerForMouse[] hitPointArray) {
            this.locationSectorPt = new Point(locationSectorPt);
            this.markerSectorPtNumber = markerSectorPtNumber;
            this.localHitMarkerArray = new HitMarkerForMouse[16];
            setHitMarkerArray(hitPointArray);
        } // end constructor

        private void setHitMarkerArray(HitMarkerForMouse[] hitPointArray) {
            int startIndex = markerSectorPtNumber * 16;
            for (int i = 0; i < 16; i++) {
                if (startIndex + i < hitPointArray.length) {
                    localHitMarkerArray[i] = hitPointArray[startIndex + i];
                }
            }
        } // end method setHitMarkerArray



        public Point getLocationSectorPt() {
            return new Point(locationSectorPt);
        }
        
        public int getMarkerSectorPtNumber() {
            return markerSectorPtNumber;
        }
    }
    
    public CircDialTryoutMouser(CircMouserView frameContainer, int width) {
        PANEL_SQ_DIM = width;
        OUTR_CIRC_DIAMETER = (int) (PANEL_SQ_DIM - .125 * PANEL_SQ_DIM);
        HIT_CIRCLE_DIAMETER = (int) (PANEL_SQ_DIM - .0625 * PANEL_SQ_DIM);
        theViewFrame = frameContainer;
        
        outerCircle = new Ellipse2D.Double(0, 0, OUTR_CIRC_DIAMETER, OUTR_CIRC_DIAMETER);
        hitPointArray = new HitMarkerForMouse[NUM_HIT_POINTS];
        sectorPointArray = new SectorMarkerForMouse[NUM_SECTOR_POINTS];
        setPreferredSize(new Dimension(HIT_CIRCLE_DIAMETER + 50, HIT_CIRCLE_DIAMETER + 50));
        
        // Initialize center point to prevent NPE during first paint
        centerPoint = new Point(PANEL_SQ_DIM/2, PANEL_SQ_DIM/2);
        calculateHitPoints();
        calculateSectorPoints();
        int kerflooey = 666; // 061825 for tracing flow only
    } // end constructor



    
    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        centerPoint = new Point(width/2, height/2);
        calculateHitPoints();
        calculateSectorPoints();
    } // end method setBounds


    
    private void calculateHitPoints() {
        if (centerPoint == null) return;
        for (int i = 0; i < NUM_HIT_POINTS; i++) {
            hitPointArray[i] = hitPointLocation(i);
        }
    } // end method calculateHitPoints


    
    private void calculateSectorPoints() {
        if (centerPoint == null) return;
        for (int i = 0; i < NUM_SECTOR_POINTS; i++) {
            sectorPointArray[i] = sectorPointLocation(i);
        }
    } // end method calculateSectorPoints


    
    private HitMarkerForMouse hitPointLocation(int index) {
        double angle = Math.toRadians((index * HIT_POINT_SPACING) + START_DEGREE_OFFSET);
        int radius = HIT_CIRCLE_DIAMETER / 2;
        int x = centerPoint.x + (int)(Math.sin(angle) * radius);
        int y = centerPoint.y - (int)(Math.cos(angle) * radius);
        return new HitMarkerForMouse(new Point(x, y), index);
    } // end method hitPointLocation


    
    private SectorMarkerForMouse sectorPointLocation(int index) {
        double angle = Math.toRadians(index * SECTOR_POINT_SPACING + START_SECTOR_OFFSET);
        int radius = HIT_CIRCLE_DIAMETER / 2;
        int x = centerPoint.x + (int)(Math.sin(angle) * radius);
        int y = centerPoint.y - (int)(Math.cos(angle) * radius);
        return new SectorMarkerForMouse(new Point(x, y), index, hitPointArray);
    } // end method sectorPointLocation


    
    private void drawHitPoints(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        for (HitMarkerForMouse marker : hitPointArray) {
            if (marker != null) {
                Point p = marker.getLocationPt();
                g2d.fillRect(p.x - 1, p.y - 1, 2, 2);
            }
        }
    } // end method drawHitPoints


    
    private void drawSectorPoints(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        for (SectorMarkerForMouse marker : sectorPointArray) {
            if (marker != null) {
                Point p = marker.getLocationSectorPt();
                g2d.fillRect(p.x - 2, p.y - 2, 4, 4);
            }
        }
    } // end method drawSectorPoints



    private void drawDialPointer(Graphics2D g2d, HitMarkerForMouse hitMarker) {
        // Save the current stroke and color
        Stroke originalStroke = g2d.getStroke();
        Color originalColor = g2d.getColor();
        
        // Set the stroke to 2 pixels width
        g2d.setStroke(new BasicStroke(2.0f));
        // Set the color to red
        g2d.setColor(Color.GRAY);
        
        // Draw the line from center to hit marker
        Point hitPoint = hitMarker.getLocationPt();
        g2d.drawLine(centerPoint.x, centerPoint.y, hitPoint.x, hitPoint.y);
        
        // Restore the original stroke and color
        g2d.setStroke(originalStroke);
        g2d.setColor(originalColor);
    }


    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (centerPoint == null) return;
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw the background image if available
        if (backgroundImage != null) {
            // Calculate the scale to fit the image to the outer circle diameter (with slight enlargement added in numerator)
            double scale = (OUTR_CIRC_DIAMETER + (0.075*OUTR_CIRC_DIAMETER)) / (double) backgroundImage.getWidth();
            int scaledWidth = (int) (backgroundImage.getWidth() * scale);
            int scaledHeight = (int) (backgroundImage.getHeight() * scale);
            
            // Center the image
            int x = centerPoint.x - scaledWidth / 2;
            int y = centerPoint.y - scaledHeight / 2;
            
            g2d.drawImage(backgroundImage, x, y, scaledWidth, scaledHeight, null);
        }
        
        // Draw the outer circle
        drawOuterCircle(g2d);
        
        // Draw the points
        drawHitPoints(g2d);
        drawSectorPoints(g2d);
        
        // Draw the dial pointer if we have a selected hit marker
        if (currentSelectedHit != null) {
            drawDialPointer(g2d, currentSelectedHit);
        }
    } // end method paintComponent


    /**
     * Draws the outer circle. Note that although variable outerCircle is already defined in its size, in needs to be translated
     * before it can be drawn. Translation is done before and after the circle is drawn.
     * The 2nd call to translate restores the original graphics transform by applying an inverse translation
     *
     * @param g2d
     */
    private void drawOuterCircle(Graphics2D g2d) {
        g2d.translate(centerPoint.x - OUTR_CIRC_DIAMETER/2, centerPoint.y - OUTR_CIRC_DIAMETER/2);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.draw(outerCircle);
        g2d.translate(-centerPoint.x + OUTR_CIRC_DIAMETER/2, -centerPoint.y + OUTR_CIRC_DIAMETER/2);
    } // end method drawOuterCircle


    
    public int getDiameter() {
        return OUTR_CIRC_DIAMETER;
    } // end method getDiameter


    /**
     * Handles a mouse click by finding the closest sector and hit marker.
     * This method is called by the CircDialMouseController.
     * This method is also called for mousePressed
     * 
     * @param clickPoint The point where the user clicked
     */
    public void handleMouseClick(Point clickPoint) {
        System.out.println("handleMouseClick called with point: " + clickPoint);
        
        // Find closest sector marker
        SectorMarkerForMouse closestSector = findClosestSectorMarker(clickPoint);
        System.out.println("Found closest sector: " + (closestSector != null ? 
            "Sector " + closestSector.getMarkerSectorPtNumber() : "none"));
            
        if (closestSector != null) {
            // Get associated hit markers for this sector
            HitMarkerForMouse closestHit = findClosestHitMarker(clickPoint, closestSector);
            System.out.println("Found closest hit marker: " + (closestHit != null ? 
                "Marker " + closestHit.getMarkerPtNumber() : "none"));
                
            if (closestHit != null) {
                // will be used for Anchor Groups
                double someHueDecimal = calcHueDecimal(closestHit.getMarkerPtNumber());
                System.out.println("Final result - Clicked near sector " + 
                    closestSector.getMarkerSectorPtNumber() + 
                    ", hit marker " + closestHit.getMarkerPtNumber() +
                        ", giving hueDecimal = " + someHueDecimal + ", hues 0-35 but NOTE:decimal range 0 - 35.75");
                currentSelectedHit = closestHit;  // Store the selected hit marker

                // Update the text field
                double fudgedHueDecimal = someHueDecimal-0.1; // NOTE: half-way between hues goes to lower hueNum; decimal range 0 - 35.75
                int roundedHueNum = (int) Math.round(fudgedHueDecimal);
                if(roundedHueNum > 35) { roundedHueNum = 0;}
                theViewFrame.getTestInfoTextfield().setText("Sector " + closestSector.getMarkerSectorPtNumber() +
                    ", hitNum " + closestHit.getMarkerPtNumber() +", w hueDecimal = dec " + someHueDecimal + " -> "+
                        "roundedHueNum = " + roundedHueNum );

                // store the hueDecimal and roundedHueNum values in the view for use in MonoTri
                theViewFrame.setLastHueDecimal(someHueDecimal);
                theViewFrame.setLastRoundHueNum(roundedHueNum);

                repaint(); // Will trigger paintComponent which includes drawDialPointer
            }
        }
    }

    private double calcHueDecimal(int markerPtNumber) {
        double hueDecimal = (double) markerPtNumber*36 / NUM_HIT_POINTS;
        return hueDecimal;
    }

    /**
     * Finds the closest sector marker to a given point.
     */
    private SectorMarkerForMouse findClosestSectorMarker(Point clickPoint) {
        SectorMarkerForMouse closest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (SectorMarkerForMouse sector : sectorPointArray) {
            if (sector != null) {
                Point sectorPt = sector.getLocationSectorPt();
                double distance = clickPoint.distance(sectorPt);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = sector;
                }
            }
        }
        
        return closest;
    }

    /**
     * Finds the closest hit marker within a sector's associated hit markers.
     */
    private HitMarkerForMouse findClosestHitMarker(Point clickPoint, SectorMarkerForMouse sector) {
        HitMarkerForMouse closest = null;
        double minDistance = Double.MAX_VALUE;
        
        // For now, search all hit markers - will optimize later to only search relevant ones
        for (HitMarkerForMouse hit : hitPointArray) {
            if (hit != null) {
                Point hitPt = hit.getLocationPt();
                double distance = clickPoint.distance(hitPt);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = hit;
                }
            }
        }
        
        return closest;
    }

} // end class CircDialTryoutMouser
