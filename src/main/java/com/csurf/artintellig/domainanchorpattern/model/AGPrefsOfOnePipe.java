package com.csurf.artintellig.domainanchorpattern.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the preferences of a PipeOneColor object for each potential AnchorGroup.
 * This class is a component of IQOnePipe and helps determine how shapes
 * should be colored in relation to the AnchorGroups defined in the SmartPic's
 * AnchorPattern.
 * 
 * The preferences are stored as relative weights for each AnchorGroup, indicating
 * how strongly the shape prefers to be associated with each group. These preferences
 * influence the automatic color assignment process when generating palettes.
 *
 * @author robinsandow
 * 052325 see ColorVary8_ProgrammingLog_starts 010823 in projectCleanColorVariatStratg091821v0v_DEBGv2A
 * 052325 see class "ColorFunctionalAttributes2" variable, not class: double[] prefsAmongAGs = new double[5]; // dummy zero cell
 * in projectCleanColorVariatStratg091821v0v_DEBGv2A
 */
public class AGPrefsOfOnePipe {
    
    private final Map<Integer, Double> groupPreferences;
    
    /**
     * Creates a new AGPrefsOfOnePipe instance with no initial preferences.
     */
    public AGPrefsOfOnePipe() {
        this.groupPreferences = new HashMap<>();
    }



    
    /**
     * Sets the preference weight for a specific anchor group.
     * 
     * @param groupId The identifier of the anchor group
     * @param weight The preference weight (0.0 to 1.0, where 1.0 indicates strongest preference)
     */
    public void setPreference(int groupId, double weight) {
        if (weight < 0.0 || weight > 1.0) {
            throw new IllegalArgumentException("Weight must be between 0.0 and 1.0");
        }
        groupPreferences.put(groupId, weight);
    }



    
    /**
     * Gets the preference weight for a specific anchor group.
     * 
     * @param groupId The identifier of the anchor group
     * @return The preference weight, or 0.0 if no preference is set
     */
    public double getPreference(int groupId) {
        return groupPreferences.getOrDefault(groupId, 0.0);
    }



    
    /**
     * Gets the anchor group with the highest preference weight.
     * 
     * @return The ID of the most preferred group, or -1 if no preferences are set
     */
    public int getMostPreferredGroup() {
        if (groupPreferences.isEmpty()) {
            return -1;
        }
        
        return groupPreferences.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(-1);
    }



    
    /**
     * Copies preferences from another AGPrefsOfOnePipe instance.
     * Used when cloning a IQOnePipe instance.
     * 
     * @param other The AGPrefsOfOnePipe instance to copy from
     */
    public void copyFrom(AGPrefsOfOnePipe other) {
        this.groupPreferences.clear();
        this.groupPreferences.putAll(other.groupPreferences);
    }



    
    /**
     * Clears all preferences.
     */
    public void clearPreferences() {
        groupPreferences.clear();
    }


} // end class AGPrefsOfOnePipe
