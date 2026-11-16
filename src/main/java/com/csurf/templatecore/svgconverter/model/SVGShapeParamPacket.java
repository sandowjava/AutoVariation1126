package com.csurf.templatecore.svgconverter.model;

import java.awt.*;
import java.awt.geom.GeneralPath;

public class SVGShapeParamPacket {
    private final GeneralPath path;
    private final Color fillColor;
    private final Color strokeColor;
    private final float strokeWidth;
    private final float opacity;
    private final String elementType;
    private final String name;




    public SVGShapeParamPacket(GeneralPath path, Color fillColor, Color strokeColor,
                               float strokeWidth, float opacity, String elementType, String name) {
        this.path = path;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.opacity = opacity;
        this.elementType = elementType;
        this.name = name;
    }

    public GeneralPath getPath() { return path; }
    public Color getFillColor() { return fillColor; }
    public Color getStrokeColor() { return strokeColor; }
    public float getStrokeWidth() { return strokeWidth; }
    public float getOpacity() { return opacity; }
    public String getElementType() { return elementType; }
    public String getName() { return name; }



} // end class SVGShapeParamPacket
