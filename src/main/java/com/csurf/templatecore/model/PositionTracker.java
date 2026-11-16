package com.csurf.templatecore.model;

import com.csurf.ostwald.domainmonotri.model.OstPosition;
import com.fasterxml.jackson.annotation.JsonIgnoreType;

import java.util.*;

/**
 * Encapsulates positional intelligence for a specific position in the template.
 * Each PositionTracker instance tracks which DrawObjs occupy a position and which of those are selected.
 * It maintains the fundamental invariant that the truth about occupation and selection
 * comes from the DrawObj collection in DrawObjRoster.
 * <p>
 * This class supports queries about occupied hues and selected DrawObjs at its position,
 * facilitating improved separation of concerns and debugging compared to the legacy approach.
 */
@JsonIgnoreType
public class PositionTracker {
    // The position this tracker is responsible for
    private final OstPosition position;
    
    // Maps from hue number (internal 0-35) to DrawObjs at this position with that hue
    private final Map<Integer, Set<DrawObj>> drawObjsByCodeHue = new HashMap<>();
    
    // Maps from hue number (internal 0-35) to selected DrawObjs at this position with that hue
    private final Map<Integer, Set<DrawObj>> selectedDrawObjsByCodeHue = new HashMap<>();



    
    /**
     * Creates a new PositionTracker for the specified position.
     *
     * @param position The position this tracker will monitor
     */
    public PositionTracker(OstPosition position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        this.position = position;
    } // end constructor



    
    /**
     * Gets the position this tracker is monitoring.
     *
     * @return The OstPosition
     */
    public OstPosition getPosition() {
        return position;
    } // end getPosition



    
    /**
     * Adds a DrawObj to this position tracker.
     * The DrawObj must have a position that matches this tracker's position.
     *
     * @param drawObj The DrawObj to add
     * @param isSelected Whether the DrawObj is currently selected
     * @throws IllegalArgumentException if the DrawObj's position doesn't match this tracker's position
     */
    public void addDrawObj(DrawObj drawObj, boolean isSelected) {
        if (drawObj == null || drawObj.getCurrentColor() == null || 
            drawObj.getCurrentColor().getOstHCS() == null || 
            drawObj.getCurrentColor().getOstHCS().getOstPosition() == null) {
            throw new IllegalArgumentException("DrawObj or its position is null");
        }
        
        OstPosition drawObjPosition = drawObj.getCurrentColor().getOstHCS().getOstPosition();
        if (!drawObjPosition.equals(this.position)) {
            throw new IllegalArgumentException("DrawObj position " + drawObjPosition + 
                " doesn't match tracker position " + this.position);
        }
        
        // Get the internal hue number (0-35)
        int hue = drawObj.getCurrentColor().getStdCodeHueNum();
        
        // Add to the appropriate collections
        drawObjsByCodeHue.computeIfAbsent(hue, k -> new HashSet<>()).add(drawObj);
        
        if (isSelected) {
            selectedDrawObjsByCodeHue.computeIfAbsent(hue, k -> new HashSet<>()).add(drawObj);
        }
    } // end method addDrawObj



    
    /**
     * Removes a DrawObj from this position tracker.
     *
     * @param drawObj The DrawObj to remove
     * @return true if the DrawObj was found and removed, false otherwise
     */
    public boolean removeDrawObj(DrawObj drawObj) {
        if (drawObj == null || drawObj.getCurrentColor() == null) {
            return false;
        }
        
        // Get the internal hue number (0-35)
        int hue = drawObj.getCurrentColor().getStdCodeHueNum();
        
        // Remove from collections
        boolean removedFromDrawObjs = false;
        Set<DrawObj> drawObjs = drawObjsByCodeHue.get(hue);
        if (drawObjs != null) {
            removedFromDrawObjs = drawObjs.remove(drawObj);
            if (drawObjs.isEmpty()) {
                drawObjsByCodeHue.remove(hue);
            }
        }
        
        boolean removedFromSelected = false;
        Set<DrawObj> selectedDrawObjs = selectedDrawObjsByCodeHue.get(hue);
        if (selectedDrawObjs != null) {
            removedFromSelected = selectedDrawObjs.remove(drawObj);
            if (selectedDrawObjs.isEmpty()) {
                selectedDrawObjsByCodeHue.remove(hue);
            }
        }
        
        return removedFromDrawObjs || removedFromSelected;
    } // end method removeDrawObj



    
    /**
     * Updates the selection state of a DrawObj in this tracker.
     *
     * @param drawObj The DrawObj to update
     * @param isSelected The new selection state
     * @return true if the DrawObj was found and its selection state was updated, false otherwise
     */
    public boolean updateSelectionState(DrawObj drawObj, boolean isSelected) {
        if (drawObj == null || drawObj.getCurrentColor() == null) {
            return false;
        }
        
        // Get the internal hue number (0-35)
        int hue = drawObj.getCurrentColor().getStdCodeHueNum();
        
        // Check if the DrawObj is tracked by this tracker
        Set<DrawObj> drawObjs = drawObjsByCodeHue.get(hue);
        if (drawObjs == null || !drawObjs.contains(drawObj)) {
            return false;
        }
        
        // Update selection state
        Set<DrawObj> selectedDrawObjs = selectedDrawObjsByCodeHue.computeIfAbsent(hue, k -> new HashSet<>());
        if (isSelected) {
            return selectedDrawObjs.add(drawObj);
        } else {
            boolean removed = selectedDrawObjs.remove(drawObj);
            if (selectedDrawObjs.isEmpty()) {
                selectedDrawObjsByCodeHue.remove(hue);
            }
            return removed;
        }
    } // end method updateSelectionState



    
    /**
     * Clears all selection state in this tracker.
     */
    public void clearSelection() {
        selectedDrawObjsByCodeHue.clear();
    }
    
    /**
     * Gets all DrawObjs at this position, regardless of hue.
     *
     * @return A set of all DrawObjs at this position
     */
    public Set<DrawObj> getAllDrawObjs() {
        Set<DrawObj> result = new HashSet<>();
        for (Set<DrawObj> objs : drawObjsByCodeHue.values()) {
            result.addAll(objs);
        }
        return Collections.unmodifiableSet(result);
    } // end method getAllDrawObjs



    
    /**
     * Gets all selected DrawObjs at this position, regardless of hue.
     *
     * @return A set of all selected DrawObjs at this position
     */
    public Set<DrawObj> getSelectedDrawObjs() {
        Set<DrawObj> result = new HashSet<>();
        for (Set<DrawObj> objs : selectedDrawObjsByCodeHue.values()) {
            result.addAll(objs);
        }
        return Collections.unmodifiableSet(result);
    } // end method getSelectedDrawObjs



    
    /**
     * Gets all DrawObjs at this position with the specified hue.
     *
     * @param internalHue The internal hue number (0-35)
     * @return A set of DrawObjs with the specified hue, or an empty set if none
     */
    public Set<DrawObj> getDrawObjsByCodeHue(int internalHue) {
        Set<DrawObj> result = drawObjsByCodeHue.get(internalHue);
        return result != null ? Collections.unmodifiableSet(result) : Collections.emptySet();
    } // end method getDrawObjsByCodeHue



    
    /**
     * Gets all selected DrawObjs at this position with the specified hue.
     *
     * @param internalHue The internal hue number (0-35)
     * @return A set of selected DrawObjs with the specified hue, or an empty set if none
     */
    public Set<DrawObj> getSelectedDrawObjsByCodeHue(int internalHue) {
        Set<DrawObj> result = selectedDrawObjsByCodeHue.get(internalHue);
        return result != null ? Collections.unmodifiableSet(result) : Collections.emptySet();
    } // end method getSelectedDrawObjsByCodeHue



    
    /**
     * Gets all hues (internal 0-35) that are occupied at this position.
     *
     * @return An array of internal hue numbers, sorted in ascending order
     */
    public int[] getOccupiedHueIntsCodeNums() {
        return getSortedCodeHues(drawObjsByCodeHue.keySet());
    } // end method getOccupiedHueIntsCodeNums



    
    /**
     * Gets all hues (internal 0-35) that are selected at this position.
     *
     * @return An array of internal hue numbers, sorted in ascending order
     */
    public int[] getSelectedHueIntsCodeNums() {
        return getSortedCodeHues(selectedDrawObjsByCodeHue.keySet());
    } // end method getSelectedHueIntsCodeNums



    
    /**
     * Gets all hues (user-facing 1-36) that are occupied at this position.
     *
     * @return An array of user-facing hue numbers, sorted in ascending order
     */
    public int[] getOccupiedUserFacingHueInts() {
        return convertToUserFacingHueInts(getOccupiedHueIntsCodeNums());
    } // end method getOccupiedUserFacingHueInts



    
    /**
     * Gets all hues (user-facing 1-36) that are selected at this position.
     *
     * @return An array of user-facing hue numbers, sorted in ascending order
     */
    public int[] getSelectedUserFacingHueInts() {
        return convertToUserFacingHueInts(getSelectedHueIntsCodeNums());
    } // end method getSelectedUserFacingHueInts



    
    /**
     * Checks if this position is occupied in any hue.
     *
     * @return true if this position is occupied, false otherwise
     */
    public boolean isPositionOccupied() { // 071125 instance member "drawObjsByCodeHue" specific to this position
        return !drawObjsByCodeHue.isEmpty();
    } // end method isPositionOccupied



    
    /**
     * Checks if this position is occupied in the specified internal hue.
     *
     * @param internalHue The internal hue number (0-35)
     * @return true if the position is occupied in the specified hue, false otherwise
     */
    public boolean isPositionOccupiedInCodeHue(int internalHue) {
        Set<DrawObj> objs = drawObjsByCodeHue.get(internalHue);
        return objs != null && !objs.isEmpty();
    } // end method isPositionOccupiedInHue



    
    /**
     * Checks if this position is selected in any hue.
     *
     * @return true if this position is selected, false otherwise
     */
    public boolean positionHasSelected() { // 071125 instance member "selectedDrawObjsByCodeHue" specific to this position
        return !selectedDrawObjsByCodeHue.isEmpty();
    } // end method positionHasSelected



    
    /**
     * Checks if this position is selected in the specified hue.
     *
     * @param internalHue The internal hue number (0-35)
     * @return true if this position is selected in the specified hue, false otherwise
     */
    public boolean positionHasSelectedInCodeHue(int internalHue) {
        Set<DrawObj> objs = selectedDrawObjsByCodeHue.get(internalHue);
        return objs != null && !objs.isEmpty();
    } // end method positionHasSelectedInHue



    
    /**
     * Checks if this tracker is empty (no DrawObjs at this position).
     *
     * @return true if this tracker is empty, false otherwise
     */
    public boolean isTrackerEmpty() {
        return drawObjsByCodeHue.isEmpty();
    } // end method isTrackerEmpty



    
    /**
     * Converts a collection of internal hue numbers to a sorted array.
     *
     * @param hues The collection of internal hue numbers
     * @return A sorted array of internal hue numbers
     */
    private int[] getSortedCodeHues(Collection<Integer> hues) {
        if (hues.isEmpty()) {
            return new int[0];
        }
        
        int[] result = new int[hues.size()];
        int i = 0;
        for (Integer hue : hues) {
            result[i++] = hue;
        }
        Arrays.sort(result);
        return result;
    } // end method getSortedCodeHues



    
    /**
     * Converts an array of internal hue numbers (0-35) to user-facing hue numbers (1-36).
     *
     * @param internalHues The array of internal hue numbers
     * @return An array of user-facing hue numbers
     */
    private int[] convertToUserFacingHueInts(int[] internalHues) {
        if (internalHues.length == 0) {
            return new int[0];
        }
        
        int[] result = new int[internalHues.length];
        for (int i = 0; i < internalHues.length; i++) {
            result[i] = internalHues[i] + 1; // Convert from 0-35 to 1-36
        }
        return result;
    } // end method convertToUserFacingHueInts



    
    @Override
    public String toString() {
        return "PositionTracker{" +
               "position=" + position +
               ", occupiedHues=" + Arrays.toString(getOccupiedHueIntsCodeNums()) +
               ", selectedHues=" + Arrays.toString(getSelectedHueIntsCodeNums()) +
               ", drawObjCount=" + getAllDrawObjs().size() +
               ", selectedDrawObjCount=" + getSelectedDrawObjs().size() +
               '}';
    } // end method toString



} // end class PositionTracker
