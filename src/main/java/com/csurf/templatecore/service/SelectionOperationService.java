package com.csurf.templatecore.service;

import com.csurf.templatecore.events.SelectionEvent;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Service class for managing batch selection operations on DrawObjs.
 * 
 * This service encapsulates selection logic that was previously scattered across
 * views and controllers. It provides a centralized way to perform selection operations
 * with proper event firing and state management.
 * 
 * Key responsibilities:
 * - Add multiple DrawObjs to selection (filtering out already-selected)
 * - Remove multiple DrawObjs from selection (filtering out non-selected)
 * - Toggle selection state of a single DrawObj
 * - Replace entire selection with a new set
 * - Clear all selections
 * 
 * All operations fire appropriate SelectionEvents to notify listeners of changes.
 */
public class SelectionOperationService {
    private final DrawObjRoster roster;
    
    /**
     * Creates a new SelectionOperationService for the given DrawObjRoster.
     * 
     * @param roster The DrawObjRoster to manage selections for
     */
    public SelectionOperationService(DrawObjRoster roster) {
        if (roster == null) {
            throw new IllegalArgumentException("DrawObjRoster cannot be null");
        }
        this.roster = roster;
    }
    
    /**
     * Adds multiple DrawObjs to the selection, filtering out those already selected.
     * 
     * This method:
     * 1. Gets the current selection from the roster
     * 2. Filters the input collection to only include DrawObjs not already selected
     * 3. Adds each new DrawObj to the roster's selection
     * 4. Fires a single batch SelectionEvent for all additions
     * 
     * @param drawObjs The collection of DrawObjs to add
     * @return The number of DrawObjs that were newly added (not already selected)
     */
    public int addMultipleToSelection(Collection<DrawObj> drawObjs) {
        if (drawObjs == null || drawObjs.isEmpty()) {
            return 0;
        }
        
        // Get current selection
        Set<DrawObj> currentSelection = roster.getSelectionGroup().getSelectedDrawObjsSet();
        
        // Filter to only new members
        Set<DrawObj> newMembers = new HashSet<>();
        for (DrawObj obj : drawObjs) {
            if (obj != null && !currentSelection.contains(obj)) {
                newMembers.add(obj);
            }
        }
        
        if (newMembers.isEmpty()) {
            return 0;
        }
        
        // Add each new member to selection
        for (DrawObj obj : newMembers) {
            roster.addToSelection(obj);
        }
        
        // Fire a single batch event for all additions
        roster.fireSelectionEventForGroup(
            SelectionEvent.SelectionChangeType.SELECTION_ADD,
            newMembers
        );
        
        return newMembers.size();
    }
    
    /**
     * Removes multiple DrawObjs from the selection, filtering out those not selected.
     * 
     * This method:
     * 1. Gets the current selection from the roster
     * 2. Filters the input collection to only include DrawObjs currently selected
     * 3. Removes each selected DrawObj from the roster's selection
     * 4. Fires a single batch SelectionEvent for all removals
     * 
     * @param drawObjs The collection of DrawObjs to remove
     * @return The number of DrawObjs that were actually removed
     */
    public int removeMultipleFromSelection(Collection<DrawObj> drawObjs) {
        if (drawObjs == null || drawObjs.isEmpty()) {
            return 0;
        }
        
        // Get current selection
        Set<DrawObj> currentSelection = roster.getSelectionGroup().getSelectedDrawObjsSet();
        
        // Filter to only members that are currently selected
        Set<DrawObj> toRemove = new HashSet<>();
        for (DrawObj obj : drawObjs) {
            if (obj != null && currentSelection.contains(obj)) {
                toRemove.add(obj);
            }
        }
        
        if (toRemove.isEmpty()) {
            return 0;
        }
        
        // Remove each member from selection
        for (DrawObj obj : toRemove) {
            roster.removeFromSelection(obj);
        }
        
        // Fire a single batch event for all removals
        roster.fireSelectionEventForGroup(
            SelectionEvent.SelectionChangeType.SELECTION_REMOVE,
            toRemove
        );
        
        return toRemove.size();
    }
    
    /**
     * Toggles the selection state of a single DrawObj.
     * 
     * If the DrawObj is currently selected, it will be removed from selection.
     * If the DrawObj is not selected, it will be added to selection.
     * 
     * @param drawObj The DrawObj to toggle
     * @return true if the DrawObj is now selected, false if it is now deselected
     */
    public boolean toggleSelection(DrawObj drawObj) {
        if (drawObj == null) {
            return false;
        }
        
        Set<DrawObj> currentSelection = roster.getSelectionGroup().getSelectedDrawObjsSet();
        
        if (currentSelection.contains(drawObj)) {
            roster.removeFromSelection(drawObj);
            return false;
        } else {
            roster.addToSelection(drawObj);
            return true;
        }
    }
    
    /**
     * Replaces the entire selection with a new set of DrawObjs.
     * 
     * This method:
     * 1. Clears the current selection
     * 2. Adds all DrawObjs from the provided collection
     * 3. Fires a single batch SelectionEvent for the replacement
     * 
     * @param drawObjs The collection of DrawObjs to select
     * @return The number of DrawObjs in the new selection
     */
    public int replaceSelection(Collection<DrawObj> drawObjs) {
        // Clear current selection
        roster.clearSelection();
        
        if (drawObjs == null || drawObjs.isEmpty()) {
            return 0;
        }
        
        // Filter out null objects
        Set<DrawObj> validObjs = new HashSet<>();
        for (DrawObj obj : drawObjs) {
            if (obj != null) {
                validObjs.add(obj);
            }
        }
        
        if (validObjs.isEmpty()) {
            return 0;
        }
        
        // Add each object to selection
        for (DrawObj obj : validObjs) {
            roster.addToSelection(obj);
        }
        
        // Fire a single batch event for the replacement
        roster.fireSelectionEventForGroup(
            SelectionEvent.SelectionChangeType.SELECTION_REPLACE,
            validObjs
        );
        
        return validObjs.size();
    }
    
    /**
     * Clears all selections.
     * 
     * @return The number of DrawObjs that were deselected
     */
    public int clearSelection() {
        Set<DrawObj> currentSelection = new HashSet<>(
            roster.getSelectionGroup().getSelectedDrawObjsSet()
        );
        
        int count = currentSelection.size();
        roster.clearSelection();
        
        return count;
    }
    
    /**
     * Gets the DrawObjRoster this service operates on.
     * 
     * @return The DrawObjRoster
     */
    public DrawObjRoster getRoster() {
        return roster;
    }
}
