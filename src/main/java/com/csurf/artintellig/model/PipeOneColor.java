package com.csurf.artintellig.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

import java.awt.geom.GeneralPath;
import java.util.List;

/**
 * Interface representing an entity that can receive and manage a single color from the color palette.
 * This interface is implemented by both individual DrawObj instances and PipeUnionDrawGroup instances
 * that contain multiple DrawObjs which should all receive the same color.
 * 
 * The term "pipe" here refers to the flow of color from the palette to the shapes, similar to how
 * water flows through a pipe. A PipeOneColor represents a single "endpoint" for this color flow,
 * whether that endpoint is a single shape or a group of shapes that should share the same color.
 */
public interface PipeOneColor {
    
    /**
     * Gets the current color assigned to this PipeOneColor.
     * @return The current ColorPtOstDot assigned to this PipeOneColor
     */
    ColorPtOstDot getCurrentColor();
    
    /**
     * Sets the color for this PipeOneColor.
     * For a DrawObj, this sets the color directly.
     * For a PipeUnionDrawGroup, this sets the same color for all contained DrawObjs.
     * 
     * @param color The ColorPtOstDot to assign
     */
    void setColor(ColorPtOstDot color);
    
    /**
     * Gets a unique identifier for this PipeOneColor.
     * This is used to maintain relationships between PipeOneColor instances and their
     * corresponding IQOnePipe instances in the RelationMatrix.
     * 
     * @return A string that uniquely identifies this PipeOneColor
     */
    String getPipeUnityId();
    
    /**
     * Gets all shapes associated with this PipeOneColor.
     * For a DrawObj, this returns a list containing a single shape.
     * For a PipeUnionDrawGroup, this returns a list of all shapes in the group.
     * 
     * @return A list of GeneralPath shapes
     */
    List<GeneralPath> getShapes();
}
