package com.csurf.artintellig.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.DrawObj;

import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents a group of DrawObj instances that should all receive the same color.
 * This class implements PipeOneColor, making it a valid target for color assignment
 * in the SmartPic color management system.
 * 
 * When a color is assigned to a PipeUnionDrawGroup, that same color is automatically
 * assigned to all DrawObj instances contained within the group. This enables efficient
 * management of repeating or related elements that should share the same color in the
 * final rendered image.
 */
public class PipeUnionDrawGroup implements PipeOneColor {
    
    private final String id;
    private final List<DrawObj> drawObjs;
    private ColorPtOstDot currentColor;
    
    /**
     * Creates a new PipeUnionDrawGroup with a unique identifier.
     */
    public PipeUnionDrawGroup() {
        this.id = UUID.randomUUID().toString();
        this.drawObjs = new ArrayList<>();
    }
    
    /**
     * Adds a DrawObj to this group. The DrawObj will immediately receive the current
     * color of this group if one has been set.
     * 
     * @param drawObj The DrawObj to add to this group
     */
    public void addDrawObj(DrawObj drawObj) {
        drawObjs.add(drawObj);
        if (currentColor != null) {
            drawObj.setColor(currentColor);
        }
    }
    
    /**
     * Gets all shapes in this group.
     * 
     * @return A list of all GeneralPath shapes from all DrawObjs in this group
     */
    @Override
    public List<GeneralPath> getShapes() {
        return drawObjs.stream()
                      .map(DrawObj::getPath)
                      .collect(Collectors.toList());
    }
    
    @Override
    public ColorPtOstDot getCurrentColor() {
        return currentColor;
    }
    
    @Override
    public void setColor(ColorPtOstDot color) {
        this.currentColor = color;
        drawObjs.forEach(obj -> obj.setColor(color));
    }
    
    @Override
    public String getPipeUnityId() {
        return id;
    }
}
