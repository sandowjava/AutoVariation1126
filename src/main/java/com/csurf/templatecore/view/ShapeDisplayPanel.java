package com.csurf.templatecore.view;

import com.csurf.ostwald.domainmonotri.view.ColorDisplayMode;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.svgconverter.view.CoordinateTransformer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class ShapeDisplayPanel extends JPanel {
    private DrawObj[] drawObjs;
    private Rectangle2D totalBounds;
    private Rectangle2D fixedTotalBounds;  // Fixed bounds for consistent scaling (independent copy)
    private final CoordinateTransformer transformer;
    public static final int SHAPE_PANEL_WIDTH = 620;
    public static final int SHAPE_PANEL_HEIGHT = 820;
    public static final int MAX_SVG_WIDTH = 600;
    public static final int MAX_SVG_HEIGHT = 800;
    private static final double PADDING = 10.0;
    private static final float SELECTION_STROKE_WIDTH = 4.0f;
    private static final float NORMAL_STROKE_WIDTH = 1.0f;
    private static final float[] SELECTION_DASH = {5.0f, 5.0f};
    
    public ShapeDisplayPanel() {
        setBackground(Color.WHITE);
        transformer = new CoordinateTransformer(PADDING, PADDING);
        setPreferredSize(new Dimension(SHAPE_PANEL_WIDTH, SHAPE_PANEL_HEIGHT));
        setMinimumSize(new Dimension(SHAPE_PANEL_WIDTH, SHAPE_PANEL_HEIGHT));
        setMaximumSize(new Dimension(SHAPE_PANEL_WIDTH, SHAPE_PANEL_HEIGHT));
    }
    
    /**
     * Sets a single object as selected, clearing any previous selections.
     * This method is maintained for backward compatibility.
     */
    public void setSelectedDrawObj(DrawObj obj) {
        if (drawObjs != null) {
            // Clear previous selections
            for (DrawObj drawObj : drawObjs) {
                drawObj.setSelected(false);
            }
            
            // Set new selection
            if (obj != null) {
                obj.setSelected(true);
            }
            
            repaint();
        }
    }
    
    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, SHAPE_PANEL_WIDTH, SHAPE_PANEL_HEIGHT);
        updateTransform();
    }
    
    @Override
    public void setSize(int width, int height) {
        super.setSize(SHAPE_PANEL_WIDTH, SHAPE_PANEL_HEIGHT);
        updateTransform();
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(SHAPE_PANEL_WIDTH, SHAPE_PANEL_HEIGHT);
    }
    
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
    
    /**
     * Sets the fixed total bounds for consistent scaling across all subsets of DrawObjs.
     * This bounds is calculated once from all DrawObjs in the original Template and stored independently.
     * Once set, it is used for all scaling calculations regardless of which subset is displayed.
     * 
     * @param bounds the fixed bounds to use for scaling (independent copy, not a reference)
     */
    public void setFixedTotalBounds(Rectangle2D bounds) {
        if (bounds != null) {
            // Create an independent copy of the bounds
            this.fixedTotalBounds = new Rectangle2D.Double(
                bounds.getX(), 
                bounds.getY(), 
                bounds.getWidth(), 
                bounds.getHeight()
            );
        } else {
            this.fixedTotalBounds = null;
        }
        updateTransform();
    }
    
    public void setDrawObjs(DrawObj[] drawObjs) {
        this.drawObjs = drawObjs;
        calculateTotalBounds();
        updateTransform();
        repaint();
    }
    
    public Point2D transformPoint(Point point) {
        return transformer.toSVGSpace(new Point2D.Double(point.x, point.y));
    }
    
    private void calculateTotalBounds() {
        if (drawObjs == null || drawObjs.length == 0) {
            totalBounds = null;
            return;
        }
        
        totalBounds = drawObjs[0].getBounds();
        for (int i = 1; i < drawObjs.length; i++) {
            totalBounds = totalBounds.createUnion(drawObjs[i].getBounds());
        }
        
        System.out.println("SVGShapesDisplayPanel - Calculated bounds: " + totalBounds);
    }
    
    private void updateTransform() {
        // Use fixed bounds if available (for consistent scaling), otherwise use current bounds
        Rectangle2D boundsToUse = (fixedTotalBounds != null) ? fixedTotalBounds : totalBounds;
        transformer.updateTransforms(boundsToUse, getWidth(), getHeight());
    }
    
    private Color getDisplayColor(DrawObj obj) {
        if (obj == null || obj.getCurrentColor() == null) return Color.WHITE;
        Window ancestor = SwingUtilities.getWindowAncestor(this);
        if (ancestor instanceof TemplateWorkFrame) {
            TemplateWorkFrame frame = (TemplateWorkFrame) ancestor;
            return (frame.getColorMode() == ColorDisplayMode.CMYK) 
                ? obj.getCurrentColor().toCMYKColor() 
                : obj.getCurrentColor().toAWTColor();
        }
        return obj.getCurrentColor().toAWTColor();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (drawObjs == null || totalBounds == null) return;
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                        RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Save original transform
        AffineTransform originalTransform = g2d.getTransform();
        
        // Apply coordinate transformation
        transformer.applyTransform(g2d);
        
        // Draw shapes
        for (DrawObj obj : drawObjs) {
            // Draw shape fill
            g2d.setColor(getDisplayColor(obj));
            g2d.fill(obj.getPath());
            
            // Draw shape stroke
            g2d.setColor(obj.getStrokeColor());
            float strokeWidth = obj.isSelected() ? SELECTION_STROKE_WIDTH : NORMAL_STROKE_WIDTH;
            g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, 
                BasicStroke.JOIN_MITER, 10.0f, obj.isSelected() ? SELECTION_DASH : null, 0.0f));
            g2d.draw(obj.getPath());
        }
        
        // Restore original transform
        g2d.setTransform(originalTransform);
    }
}
