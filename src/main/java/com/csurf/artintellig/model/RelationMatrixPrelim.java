package com.csurf.artintellig.model;


import com.csurf.artintellig.domainanchorpattern.model.IQAnchorPattern;

/**
 * Represents the color intelligence system in a SmartPic.
 * The RelationMatrix maintains the relationships between shapes and their
 * color preferences through the IQAllPipes system, and manages the
 * AnchorPattern that defines how colors are organized around the hue circle.
 * 
 * This class is a core component of SmartPic but is designed to be
 * loosely coupled. It contains all the intelligence needed for automatic
 * color assignment and palette generation, though it does not directly create
 * palettes (that is handled by a separate palette generator).
 */
public class RelationMatrixPrelim {
    
    private final IQAllPipes IQAllPipes;
    private IQAnchorPattern anchorPattern;
    
    /**
     * Creates a new RelationMatrix instance.
     */
    public RelationMatrixPrelim() {
        this.IQAllPipes = new IQAllPipes();
    }
    
    /**
     * Gets the IQAllPipes instance that manages color intelligence
     * for all shapes in the SmartPic.
     * 
     * @return The IQAllPipes instance
     */
    public IQAllPipes getCollectiveSmarts() {
        return IQAllPipes;
    }
    
    /**
     * Gets the current AnchorPattern that defines how color groups are
     * arranged around the hue circle.
     * 
     * @return The current AnchorPattern, or null if not set
     */
    public IQAnchorPattern getAnchorPattern() {
        return anchorPattern;
    }
    
    /**
     * Sets the AnchorPattern for this RelationMatrix.
     * 
     * @param pattern The new AnchorPattern to use
     */
    public void setAnchorPattern(IQAnchorPattern pattern) {
        this.anchorPattern = pattern;
    }
    
    /**
     * Gets the IQOnePipe instance associated with a specific PipeOneColor.
     * 
     * @param pipeUnityId The ID of the PipeOneColor
     * @return The associated IQOnePipe, or null if not found
     */
    public IQOnePipe getSmarts(String pipeUnityId) {
        return IQAllPipes.getSmarts(pipeUnityId);
    }
    
    /**
     * Adds a new IQOnePipe instance to this RelationMatrix.
     * 
     * @param smarts The IQOnePipe to add
     */
    public void addSmarts(IQOnePipe smarts) {
        IQAllPipes.addSmarts(smarts);
    }
}
