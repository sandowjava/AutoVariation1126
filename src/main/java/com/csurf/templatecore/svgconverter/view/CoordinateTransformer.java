package com.csurf.templatecore.svgconverter.view;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Handles coordinate transformations between SVG space and screen space.
 * Accounts for padding, scaling, and viewBox transformations.
 */
public class CoordinateTransformer {
    private final double paddingX;
    private final double paddingY;
    private AffineTransform transform;
    private AffineTransform inverseTransform;
    
    public CoordinateTransformer(double paddingX, double paddingY) {
        this.paddingX = paddingX;
        this.paddingY = paddingY;
        this.transform = new AffineTransform();
        this.inverseTransform = new AffineTransform();
        updateTransforms();
    }
    
    /**
     * Updates transforms based on SVG bounds and panel size
     */
    public void updateTransforms(Rectangle2D svgBounds, int panelWidth, int panelHeight) {
        if (svgBounds == null) {
            updateTransforms();
            return;
        }
        
        // Calculate scale to fit SVG in panel while maintaining aspect ratio
        double scaleX = (panelWidth - 2 * paddingX) / svgBounds.getWidth();
        double scaleY = (panelHeight - 2 * paddingY) / svgBounds.getHeight();
        double scale = Math.min(scaleX, scaleY);
        
        // Calculate centering translation
        double tx = paddingX + (panelWidth - 2 * paddingX - svgBounds.getWidth() * scale) / 2;
        double ty = paddingY + (panelHeight - 2 * paddingY - svgBounds.getHeight() * scale) / 2;
        
        // Create transform: translate to origin, scale, translate to center
        transform = new AffineTransform();
        transform.translate(tx, ty);
        transform.scale(scale, scale);
        transform.translate(-svgBounds.getX(), -svgBounds.getY());
        
        try {
            inverseTransform = transform.createInverse();
        } catch (Exception e) {
            System.err.println("Error creating inverse transform: " + e);
            inverseTransform = new AffineTransform();
        }
    }
    
    private void updateTransforms() {
        transform = new AffineTransform();
        transform.translate(paddingX, paddingY);
        inverseTransform = new AffineTransform();
        inverseTransform.translate(-paddingX, -paddingY);
    }
    
    /**
     * Transforms a point from screen coordinate space to SVG space
     */
    public Point2D toSVGSpace(Point2D screenPoint) {
        return inverseTransform.transform(screenPoint, null);
    }
    
    /**
     * Transforms a point from SVG coordinate space to screen space
     */
    public Point2D toScreenSpace(Point2D svgPoint) {
        return transform.transform(svgPoint, null);
    }
    
    /**
     * Applies the transformation to a Graphics2D context
     */
    public void applyTransform(Graphics2D g2d) {
        g2d.transform(transform);
    }
    
    /**
     * Gets the current transform
     */
    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }
    
    /**
     * Gets the current inverse transform
     */
    public AffineTransform getInverseTransform() {
        return new AffineTransform(inverseTransform);
    }
}
