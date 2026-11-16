package com.csurf.artintellig.model;

import com.csurf.templatecore.model.DrawObj;

import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the collection of PipeOneColor objects in a SmartPic.
 * This class is responsible for organizing and maintaining the shapes that
 * will be rendered, whether they are individual DrawObj instances or
 * PipeUnionDrawGroup instances containing multiple DrawObjs that share
 * the same color.
 * 
 * The RosterOfPipeShapes runs parallel to the IQAllPipes in the
 * RelationMatrix, but focuses on shape management and rendering rather
 * than color intelligence.
 */
public class RosterOfPipeShapes {
    
    private final List<PipeOneColor> pipeUnities;
    
    /**
     * Creates a new RosterOfPipeShapes instance.
     */
    public RosterOfPipeShapes() {
        this.pipeUnities = new ArrayList<>();
    }
    
    /**
     * Creates a new RosterOfPipeShapes instance with the given list.
     * Used internally for cloning.
     */
    private RosterOfPipeShapes(List<PipeOneColor> pipeUnities) {
        this.pipeUnities = pipeUnities;
    }
    
    /**
     * Adds a PipeOneColor object to the roster.
     * This can be either a DrawObj or a PipeUnionDrawGroup.
     * 
     * @param pipeOneColor The PipeOneColor to add
     */
    public void addPipeUnity(PipeOneColor pipeOneColor) {
        if (!pipeUnities.contains(pipeOneColor)) {
            pipeUnities.add(pipeOneColor);
        }
    }
    
    /**
     * Removes a PipeOneColor object from the roster.
     * 
     * @param pipeOneColor The PipeOneColor to remove
     */
    public void removePipeUnity(PipeOneColor pipeOneColor) {
        pipeUnities.remove(pipeOneColor);
    }
    
    /**
     * Gets a PipeOneColor by its unique identifier.
     * 
     * @param pipeUnityId The ID of the PipeOneColor to find
     * @return The matching PipeOneColor, or null if not found
     */
    public PipeOneColor getPipeUnity(String pipeUnityId) {
        return pipeUnities.stream()
            .filter(pu -> pu.getPipeUnityId().equals(pipeUnityId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Gets an unmodifiable view of all PipeOneColor objects in this roster.
     * 
     * @return List of all PipeOneColor objects
     */
    public List<PipeOneColor> getAllPipeUnities() {
        return Collections.unmodifiableList(pipeUnities);
    }
    
    /**
     * Gets all DrawObj instances from this roster, including those
     * contained within PipeUnityDrawGroups.
     * 
     * @return List of all DrawObj instances
     */
    public List<DrawObj> getAllDrawObjs() {
        List<DrawObj> allDrawObjs = new ArrayList<>();
        
        for (PipeOneColor pu : pipeUnities) {
            if (pu instanceof DrawObj) {
                allDrawObjs.add((DrawObj) pu);
            } else if (pu instanceof PipeUnionDrawGroup) {
                // Get the shapes and find their corresponding DrawObjs
                List<GeneralPath> shapes = pu.getShapes();
                for (GeneralPath shape : shapes) {
                    // Find the DrawObj that owns this shape
                    DrawObj drawObj = findDrawObjByShape(shape);
                    if (drawObj != null) {
                        allDrawObjs.add(drawObj);
                    }
                }
            }
        }
        
        return allDrawObjs;
    }
    
    /**
     * Gets all shapes from this roster, including those contained within
     * PipeUnityDrawGroups.
     * 
     * @return List of all shapes
     */
    public List<GeneralPath> getAllShapes() {
        return pipeUnities.stream()
            .flatMap(pu -> pu.getShapes().stream())
            .collect(Collectors.toList());
    }
    
    /**
     * Finds a DrawObj that owns a specific shape.
     * 
     * @param shape The shape to find the owner for
     * @return The DrawObj that owns the shape, or null if not found
     */
    private DrawObj findDrawObjByShape(GeneralPath shape) {
        for (PipeOneColor pu : pipeUnities) {
            if (pu instanceof DrawObj) {
                DrawObj drawObj = (DrawObj) pu;
                if (drawObj.getPath().equals(shape)) {
                    return drawObj;
                }
            }
        }
        return null;
    }
}
