package com.csurf.templatecore.model;

import com.csurf.ostwald.domainmonotri.model.OstPosition;
import com.csurf.templatecore.model.serialization.SelectionGroupDeserializer;
import com.csurf.templatecore.model.serialization.SelectionGroupSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.*;

/*
* Class purpose: to represent the group of DrawObj instances that are currently selected
* Immediate use: to help debug the group of DrawObj instances that are currently selected
* Future use: entity for representing all changes of coloring
*/
@JsonDeserialize(using = SelectionGroupDeserializer.class)
@JsonSerialize(using = SelectionGroupSerializer.class)
public class SelectionGroup {
    // members
    private Set<DrawObj> selectedDrawObjsSet;
    
    // Used during deserialization to store DrawObj unique IDs before they can be resolved
    private transient List<String> drawObjUniqueIds;

    /**
     * Constructor that takes a set of selected DrawObj instances
     * @param selectedDrawObjs the set of DrawObj instances to be marked as selected
     */
    public SelectionGroup(Set<DrawObj> selectedDrawObjs) {
        this.selectedDrawObjsSet = selectedDrawObjs != null ? new HashSet<>(selectedDrawObjs) : new HashSet<>();
        
        // Automatically populate the drawObjUniqueIds list
        if (selectedDrawObjs != null && !selectedDrawObjs.isEmpty()) {
            this.drawObjUniqueIds = new ArrayList<>();
            for (DrawObj drawObj : selectedDrawObjs) {
                if (drawObj.getUniqueId() != null) {
                    this.drawObjUniqueIds.add(drawObj.getUniqueId().toString());
                    System.out.println("SelectionGroup constructor: Added UUID " + drawObj.getUniqueId() + 
                                      " for DrawObj " + drawObj);
                } else {
                    System.out.println("SelectionGroup constructor: WARNING - DrawObj has null UUID: " + drawObj);
                }
            }
            System.out.println("SelectionGroup constructor: Populated " + this.drawObjUniqueIds.size() + 
                              " UUIDs from " + selectedDrawObjs.size() + " DrawObj instances");
            
            // Verify all UUIDs are valid
            verifyUuids(this.drawObjUniqueIds);
        }
    } // end constructor

    /**
     * Default constructor that creates an empty selection group
     */
    public SelectionGroup() {
        this.selectedDrawObjsSet = new HashSet<>();
        this.drawObjUniqueIds = new ArrayList<>();
        System.out.println("SelectionGroup default constructor: Initialized empty drawObjUniqueIds list");
    } // end constructor

    /**
     * Adds a DrawObj to the selection group.
     * NOTE: This method does NOT set the DrawObj's selected flag. That responsibility
     * is handled exclusively by DrawObjRoster to maintain centralized flag management.
     * 
     * @param drawObj The DrawObj to add to the selection
     * @return true if the object was added, false if it was already in the selection
     */
    public boolean addToSelectionGroup(DrawObj drawObj) {
        if (drawObj == null) {
            return false;
        }
        
        // Ensure selectedDrawObjsSet is initialized
        if (selectedDrawObjsSet == null) {
            selectedDrawObjsSet = new HashSet<>();
        }
        
        boolean added = selectedDrawObjsSet.add(drawObj);
        if (added) {
            // Update the drawObjUniqueIds list
            if (drawObj.getUniqueId() != null) {
                if (drawObjUniqueIds == null) {
                    drawObjUniqueIds = new ArrayList<>();
                }
                String uniqueId = drawObj.getUniqueId().toString();
                if (!drawObjUniqueIds.contains(uniqueId)) {
                    drawObjUniqueIds.add(uniqueId);
                    System.out.println("SelectionGroup.addToSelectionGroup: Added UUID " + uniqueId + " to drawObjUniqueIds");
                }
            }
        }
        return added;
    } // end method addToSelectionGroup
    
    /**
     * Adds multiple DrawObjs to the selection group and sets their selection flags to true.
     * @param drawObjs The collection of DrawObjs to add to the selection
     * @return The number of objects that were newly added to the selection
     */
    public int addAllToSelectionGroup(Set<DrawObj> drawObjs) {
        if (drawObjs == null) {
            return 0;
        }
        
        int addedCount = 0;
        for (DrawObj obj : drawObjs) {
            if (addToSelectionGroup(obj)) {
                addedCount++;
            }
        }
        return addedCount;
    } // end method addAllToSelectionGroup
    
    /**
     * Removes a DrawObj from the selection group.
     * NOTE: This method does NOT set the DrawObj's selected flag. That responsibility
     * is handled exclusively by DrawObjRoster to maintain centralized flag management.
     * 
     * @param drawObj The DrawObj to remove from the selection
     * @return true if the object was removed, false if it wasn't in the selection
     */
    public boolean removeFromSelectionGroup(DrawObj drawObj) {
        if (drawObj == null) {
            return false;
        }
        
        boolean removed = selectedDrawObjsSet.remove(drawObj);
        if (removed) {
            // Update the drawObjUniqueIds list
            if (drawObj.getUniqueId() != null && drawObjUniqueIds != null) {
                String uniqueId = drawObj.getUniqueId().toString();
                boolean removedUuid = drawObjUniqueIds.remove(uniqueId);
                System.out.println("SelectionGroup.removeFromSelectionGroup: " + 
                                  (removedUuid ? "Removed" : "Failed to remove") + 
                                  " UUID " + uniqueId + " from drawObjUniqueIds");
            }
        }
        return removed;
    } // end method removeFromSelectionGroup
    
    /**
     * Removes multiple DrawObjs from the selection group and sets their selection flags to false.
     * @param drawObjs The collection of DrawObjs to remove from the selection
     * @return The number of objects that were removed from the selection
     */
    public int removeAllFromSelectionGroup(Set<DrawObj> drawObjs) {
        if (drawObjs == null) {
            return 0;
        }
        
        int removedCount = 0;
        for (DrawObj obj : drawObjs) {
            if (removeFromSelectionGroup(obj)) {
                removedCount++;
            }
        }
        return removedCount;
    } // end method removeAllFromSelectionGroup
    
    /**
     * Toggles the selection state of a DrawObj. If it's currently selected, it will be
     * deselected. If it's not selected, it will be added to the selection.
     * @param drawObj The DrawObj to toggle selection for
     * @return true if the object is now selected, false if it is now deselected
     */
    public boolean toggleDrawObjSelectionState(DrawObj drawObj) {
        if (drawObj == null) {
            return false;
        }
        
        if (selectedDrawObjsSet.contains(drawObj)) {
            removeFromSelectionGroup(drawObj);
            return false;
        } else {
            addToSelectionGroup(drawObj);
            return true;
        }
    } // end method toggleDrawObjSelectionState
    
    /**
     * Clears the selection group.
     * NOTE: This method does NOT set DrawObj selected flags. That responsibility
     * is handled exclusively by DrawObjRoster to maintain centralized flag management.
     */
    public void clearSelectionGroup() {
        selectedDrawObjsSet.clear();
        
        // Clear the drawObjUniqueIds list
        if (drawObjUniqueIds != null) {
            int size = drawObjUniqueIds.size();
            drawObjUniqueIds.clear();
            System.out.println("SelectionGroup.clearSelectionGroup: Cleared " + size + " UUIDs from drawObjUniqueIds");
        }
    } // end method clearSelectionGroup
    
    /**
     * Checks if a DrawObj is currently in the selection group.
     * @param drawObj The DrawObj to check
     * @return true if the object is selected, false otherwise
     */
    public boolean containsDrawObjYesNo(DrawObj drawObj) {
        return drawObj != null && selectedDrawObjsSet.contains(drawObj);
    } // end method containsDrawObjYesNo
    
    /**
     * Returns the number of DrawObjs in the selection group.
     * @return The size of the selection
     */
    public int size() {
        return selectedDrawObjsSet.size();
    } // end method size
    
    /**
     * Checks if the selection group is empty.
     * @return true if no objects are selected, false otherwise
     */
    public boolean isEmpty() {
        return selectedDrawObjsSet.isEmpty();
    } // end method isEmpty

    /**
     * Returns an unmodifiable view of the selected DrawObjs set.
     * This prevents external code from modifying the selection without
     * properly updating the DrawObj selection flags.
     * 
     * @return An unmodifiable view of the selected DrawObjs set
     */
    public Set<DrawObj> getSelectedDrawObjsSet() {
        return Collections.unmodifiableSet(selectedDrawObjsSet);
    } // end method getSelectedDrawObjsSet

    /**
     * Sets the list of DrawObj unique IDs.
     * This is used during deserialization to store the IDs before they can be resolved.
     * 
     * @param drawObjUniqueIds the list of DrawObj unique IDs
     */
    public void setDrawObjUniqueIds(List<String> drawObjUniqueIds) {
        System.out.println("\n==== SelectionGroup.setDrawObjUniqueIds ====");
        if (drawObjUniqueIds == null) {
            System.out.println("WARNING: Received null drawObjUniqueIds list");
            this.drawObjUniqueIds = new ArrayList<>();
            return;
        }
        
        // Create a new list to avoid external modifications
        this.drawObjUniqueIds = new ArrayList<>(drawObjUniqueIds);
        
        System.out.println("Set " + this.drawObjUniqueIds.size() + " UUIDs in SelectionGroup");
        
        // Verify all UUIDs are valid
        verifyUuids(this.drawObjUniqueIds);
        
        // Print the first few UUIDs for debugging
        int maxToShow = Math.min(5, this.drawObjUniqueIds.size());
        if (maxToShow > 0) {
            System.out.println("First " + maxToShow + " UUIDs:");
            for (int i = 0; i < maxToShow; i++) {
                System.out.println("  - " + this.drawObjUniqueIds.get(i));
            }
        }
        
        System.out.println("==== End SelectionGroup.setDrawObjUniqueIds ====\n");
    } // end method setDrawObjUniqueIds

    /**
     * Gets the list of DrawObj unique IDs.
     * This is used during post-deserialization reference resolution.
     * 
     * @return the list of DrawObj unique IDs
     */
    public List<String> getDrawObjUniqueIds() {
        System.out.println("SelectionGroup.getDrawObjUniqueIds: Returning " + 
                          (drawObjUniqueIds != null ? drawObjUniqueIds.size() : "null") + " UUIDs");
        
        // If drawObjUniqueIds is null but we have selectedDrawObjsSet, populate it
        if (drawObjUniqueIds == null && selectedDrawObjsSet != null && !selectedDrawObjsSet.isEmpty()) {
            System.out.println("WARNING: drawObjUniqueIds is null but selectedDrawObjsSet is not empty");
            System.out.println("Populating drawObjUniqueIds from selectedDrawObjsSet");
            
            drawObjUniqueIds = new ArrayList<>();
            for (DrawObj drawObj : selectedDrawObjsSet) {
                if (drawObj.getUniqueId() != null) {
                    drawObjUniqueIds.add(drawObj.getUniqueId().toString());
                    System.out.println("  - Added UUID: " + drawObj.getUniqueId());
                } else {
                    System.out.println("  - WARNING: DrawObj has null UUID: " + drawObj);
                }
            }
            
            System.out.println("Populated " + drawObjUniqueIds.size() + " UUIDs from " + 
                              selectedDrawObjsSet.size() + " DrawObj instances");
        }
        
        // Return a defensive copy to prevent external modifications
        return drawObjUniqueIds != null ? new ArrayList<>(drawObjUniqueIds) : new ArrayList<>();
    } // end method getDrawObjUniqueIds

    /**
     * Helper method to verify that all UUIDs in the list are valid
     * @param uuidList List of UUID strings to verify
     */
    private void verifyUuids(List<String> uuidList) {
        if (uuidList == null) {
            System.out.println("UUID verification: List is null");
            return;
        }
        
        int validCount = 0;
        int invalidCount = 0;
        
        for (String uuidStr : uuidList) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                validCount++;
            } catch (IllegalArgumentException e) {
                invalidCount++;
                System.out.println("WARNING: Invalid UUID format: " + uuidStr);
            }
        }
        
        System.out.println("UUID verification: " + validCount + " valid, " + invalidCount + " invalid");
    }

    /**
     * Returns an array of all positions in the selection group.
     * This is useful for checking if a given position is in the selection group.
     * Also useful as intermediate step for getting lowest BSeries and lowest WSeries for group
     * Also useful as intermediate step for getting highest BSeries and highest WSeries for group
     *
     * @return An array of all positions in the selection group
     */
    public OstPosition[] getAllPositions() {
        OstPosition[] allPositions = null;

        // create a Set of all positions in selectedDrawObjsSet
        // go through all elements of selectedDrawObjsSet, if position is not in the Set, add it
        Set<OstPosition> positions = new HashSet<>();
        for (DrawObj drawObj : selectedDrawObjsSet) {
            OstPosition position = drawObj.getCurrColorOstPosition();
            if (position != null) {
                positions.add(position);
            }
        } // end FOR

        // convert the Set to an array
        allPositions = positions.toArray(new OstPosition[0]);

        return allPositions;
    } // end method getAllPositions

    public int getLowestBseries() {
        OstPosition[] positions = getAllPositions();
        int lowestBseries = 313;
        for (OstPosition position : positions) {
            if (position.getBlackSeriesNum() < lowestBseries) {
                lowestBseries = position.getBlackSeriesNum();
            }
        } // end FOR

        return lowestBseries;
    } // end method getLowestBseries

    public int getHighestBseries() {
        OstPosition[] positions = getAllPositions();
        int highestBseries = -313;
        for (OstPosition position : positions) {
            if (position.getBlackSeriesNum() > highestBseries) {
                highestBseries = position.getBlackSeriesNum();
            }
        } // end FOR

        return highestBseries;
    } // end method getHighestBseries

    public int getLowestWseries() {
        OstPosition[] positions = getAllPositions();
        int lowestWseries = 313;
        for (OstPosition position : positions) {
            if (position.getWhiteSeriesNum() < lowestWseries) {
                lowestWseries = position.getWhiteSeriesNum();
            }
        } // end FOR

        return lowestWseries;
    } // end method getLowestWseries

    public int getHighestWseries() {
        OstPosition[] positions = getAllPositions();
        int highestWseries = -313;
        for (OstPosition position : positions) {
            if (position.getWhiteSeriesNum() > highestWseries) {
                highestWseries = position.getWhiteSeriesNum();
            }
        } // end FOR

        return highestWseries;
    } // end method getHighestWseries
} // end class SelectionGroup
