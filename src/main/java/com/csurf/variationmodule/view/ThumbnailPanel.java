package com.csurf.variationmodule.view;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.svgconverter.view.CoordinateTransformer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Displays one Template variation as a non-interactive thumbnail.
 * Replicates ShapeDisplayPanel's fit-to-bounds rendering (minus selection
 * stroke and CMYK handling): draws each DrawObj's GeneralPath filled with
 * its AWT color, scaled to fit this panel with padding.
 */
public class ThumbnailPanel extends JPanel {
    public static final int THUMBNAIL_WIDTH = 145;
    public static final int THUMBNAIL_HEIGHT = 145;
    private static final double PADDING = 5.0;

    private DrawObj[] drawObjs;
    private Rectangle2D unionBounds;
    private final CoordinateTransformer transformer;

    public ThumbnailPanel() {
        setBackground(Color.WHITE);
        transformer = new CoordinateTransformer(PADDING, PADDING);
        setPreferredSize(new Dimension(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));
    }

    /**
     * Sets the DrawObjs to render and repaints.
     * @param drawObjs the shapes to display (in drawing order)
     */
    public void setDrawObjs(DrawObj[] drawObjs) {
        this.drawObjs = drawObjs;
        calculateUnionBounds();
        repaint();
    }

    private void calculateUnionBounds() {
        if (drawObjs == null || drawObjs.length == 0) {
            unionBounds = null;
            return;
        }
        unionBounds = null;
        for (DrawObj obj : drawObjs) {
            if (obj == null || obj.getPath() == null) {
                continue;
            }
            Rectangle2D b = obj.getBounds();
            unionBounds = (unionBounds == null) ? b : unionBounds.createUnion(b);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (drawObjs == null || unionBounds == null) {
            return;
        }
        // Guard: degenerate (zero-area) bounds would divide by zero in the fit calc
        if (unionBounds.getWidth() <= 0 || unionBounds.getHeight() <= 0) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            transformer.updateTransforms(unionBounds, getWidth(), getHeight());
            transformer.applyTransform(g2d);

            for (DrawObj obj : drawObjs) {
                if (obj == null || obj.getPath() == null) {
                    continue;
                }
                g2d.setColor(obj.getAWTColor());
                g2d.fill(obj.getPath());
            }
        } finally {
            g2d.dispose();
        }
    }
} // end class ThumbnailPanel
