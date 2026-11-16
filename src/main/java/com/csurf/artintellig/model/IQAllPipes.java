package com.csurf.artintellig.model;

import com.csurf.artintellig.domainanchorpattern.model.IQAGSpread;

import java.util.*;

/**
 * Manages the collection of IQOnePipe instances in a RelationMatrix.
 * This class represents all of the color intelligence for the shapes in a SmartPic,
 * running parallel to the RosterOfPipeShapes that manages the actual shapes.
 * 
 * IQAllPipes is a core component of the RelationMatrix and provides the
 * foundation for intelligent color palette generation. It maintains the relationships
 * between shapes and their color preferences but does not directly create colors
 * or palettes (that responsibility belongs to a palette generator).
 */
public class IQAllPipes {
    
    private final Map<String, IQOnePipe> smartsMap;
    private final List<IQAGSpread> anchorGroups;
    
    /**
     * Creates a new IQAllPipes instance.
     */
    public IQAllPipes() {
        this.smartsMap = new HashMap<>();
        this.anchorGroups = new ArrayList<>();
    }
    
    /**
     * Adds a IQOnePipe instance to the collection.
     * 
     * @param smarts The IQOnePipe to add
     */
    public void addSmarts(IQOnePipe smarts) {
        smartsMap.put(smarts.getId(), smarts);
    }
    
    /**
     * Gets the IQOnePipe for a specific PipeOneColor.
     * 
     * @param pipeUnityId The ID of the PipeOneColor
     * @return The associated IQOnePipe, or null if not found
     */
    public IQOnePipe getSmarts(String pipeUnityId) {
        return smartsMap.get(pipeUnityId);
    }
    
    /**
     * Gets all IQOnePipe instances in this collection.
     * 
     * @return List of all IQOnePipe instances
     */
    public List<IQOnePipe> getAllSmarts() {
        return new ArrayList<>(smartsMap.values());
    }
    
    /**
     * Adds an AnchorGroup to this collection.
     * 
     * @param group The AnchorGroup to add
     */
    public void addAnchorGroup(IQAGSpread group) {
        if (!anchorGroups.contains(group)) {
            anchorGroups.add(group);
        }
    }
    
    /**
     * Gets all AnchorGroups in this collection.
     * 
     * @return Unmodifiable list of AnchorGroups
     */
    public List<IQAGSpread> getAnchorGroups() {
        return Collections.unmodifiableList(anchorGroups);
    }
}
