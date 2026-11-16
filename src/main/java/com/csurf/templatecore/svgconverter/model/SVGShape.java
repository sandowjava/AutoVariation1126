package com.csurf.templatecore.svgconverter.model;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class SVGShape {
    private final GeneralPath path;
    private final Color fillColor;
    private final Color strokeColor;
    private final float strokeWidth;
    private final float opacity;
    private final String elementType;
    private final String name;
    private boolean selected;
    private String id;
    private static int nextId = 1;

    public SVGShape(SVGShapeParamPacket params) {
        this.path = params.getPath();
        this.fillColor = params.getFillColor();
        this.strokeColor = params.getStrokeColor();
        this.strokeWidth = params.getStrokeWidth();
        this.opacity = params.getOpacity();
        this.elementType = params.getElementType();
        this.name = params.getName() != null && !params.getName().isEmpty() ? params.getName() : params.getElementType() + "-" + System.identityHashCode(this);
        this.selected = false;
        this.id = params.getElementType() + "_" + nextId++;
    }

    public GeneralPath getPath() { return path; }
    public Color getFillColor() { return fillColor; }
    public Color getStrokeColor() { return strokeColor; }
    public float getStrokeWidth() { return strokeWidth; }
    public float getOpacity() { return opacity; }
    public String getElementType() { return elementType; }
    public String getId() { return id; }
    public Rectangle2D getBounds() { return path.getBounds2D(); }
    public String getName() { return name; }
    
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    
    public boolean contains(Point2D point) {
        return path.contains(point);
    }
    
    @Override
    public String toString() {
        return name;
    }
}
