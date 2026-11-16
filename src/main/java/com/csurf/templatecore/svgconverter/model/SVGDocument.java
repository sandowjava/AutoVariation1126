package com.csurf.templatecore.svgconverter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SVGDocument {
    private final List<SVGShape> shapes;
    
    public SVGDocument() {
        shapes = new ArrayList<>();
    }
    
    public void setShapes(List<SVGShape> newShapes) {
        shapes.clear();
        shapes.addAll(newShapes);
    }
    
    public void addShape(SVGShape shape) {
        shapes.add(shape);
    }
    
    public List<SVGShape> getShapes() {
        return Collections.unmodifiableList(shapes);
    }
    
    public void clearSelection() {
        for (SVGShape shape : shapes) {
            shape.setSelected(false);
        }
    }
    
    public void selectShape(SVGShape shape) {
        clearSelection();
        if (shape != null && shapes.contains(shape)) {
            shape.setSelected(true);
        }
    }
}
