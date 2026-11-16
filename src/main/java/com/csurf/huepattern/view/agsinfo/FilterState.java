package com.csurf.huepattern.view.agsinfo;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.csurf.huepattern.model.RMatrixMini;
import com.csurf.huepattern.model.proxy.TemplateProxyData;

/**
 * FilterState manages the current filtering state for the three-step filtering process in AGsInfoFrame.
 * 
 * This class implements a state machine with three mutually exclusive modes:
 * - ONE_STEP: Only first criterion (Show menu) is active
 * - TWO_STEP: First and second criteria (Show + Issues menu) are active
 * - THREE_STEP: All three criteria (Show + Issues + AG selection) are active
 * 
 * Subsets are recalculated on-demand (not cached) for code clarity and to avoid stale data.
 * 
 * @author User
 */
public class FilterState {
    
    // ========== ENUMS ==========
    
    /**
     * Represents the current filtering mode
     */
    public enum FilterMode {
        ONE_STEP,    // Only first criterion active
        TWO_STEP,    // First and second criteria active
        THREE_STEP   // All three criteria active
    }
    
    /**
     * Represents the type of first criterion (Show menu selection)
     */
    public enum FilterType {
        AG,              // Single AG (filterValue = Integer)
        HUE_RANGE,       // Hue range (filterValue = HueRange object)
        SELECTED_AGs,    // Multiple AGs (filterValue = Set<Integer>)
        ALL,             // All shapes (filterValue = null)
        ALLAGS           // All shapes assigned to at least one AG (filterValue = null)
    }
    
    /**
     * Represents the type of second criterion (Issues menu selection)
     */
    public enum IssuesFilterType {
        MULTIPLE_AGs,    // Find shapes assigned to multiple AGs
        WITHIN_SPREADS,  // Find shapes outside spread limits
        NO_AG_ASSIGNED   // Find shapes with no AG assignment
    }
    
    // ========== FIELDS ==========
    
    /**
     * Current filtering mode (ONE_STEP, TWO_STEP, or THREE_STEP)
     */
    private FilterMode mode = FilterMode.ONE_STEP;
    
    /**
     * First criterion: type of filter from Show menu
     */
    private FilterType filterType;
    
    /**
     * First criterion: value associated with filterType
     * - For AG: Integer (AG number 1-4)
     * - For HUE_RANGE: HueRange object
     * - For SELECTED_AGs: Set<Integer>
     * - For ALL: null
     * - For ALLAGS: null
     */
    private Object filterValue;
    
    /**
     * Second criterion: type of validation filter from Issues menu
     * Only relevant in TWO_STEP and THREE_STEP modes
     */
    private IssuesFilterType issuesFilterType;
    
    /**
     * Third criterion: set of selected AGs from fourAGChoiceswChipsPanel
     * Only relevant in THREE_STEP mode
     */
    private Set<Integer> selectedAGs = new HashSet<>();
    
    /**
     * Third criterion: invert flag from showAGsButtonPanel
     * When true, subset3 = subset2 - subset3 (complement)
     * Only relevant in THREE_STEP mode
     */
    private boolean invert = false;
    
    // ========== CONSTRUCTORS ==========
    
    /**
     * Creates a new FilterState in default ONE_STEP mode with no filters applied
     */
    public FilterState() {
        this.mode = FilterMode.ONE_STEP;
        this.filterType = null;
        this.filterValue = null;
        this.issuesFilterType = null;
        this.selectedAGs = new HashSet<>();
        this.invert = false;
    }
    
    // ========== STATE TRANSITIONS ==========
    
    /**
     * Clear all filters and return to ONE_STEP mode (CLEAR button in Show menu)
     * Resets all criteria to default state
     */
    public void clearAllFilters() {
        this.mode = FilterMode.ONE_STEP;
        this.filterType = null;
        this.filterValue = null;
        this.issuesFilterType = null;
        this.selectedAGs.clear();
        this.invert = false;
    }
    
    /**
     * Set first criterion (Show menu selection)
     * Transitions to ONE_STEP mode
     * 
     * Note: If already in TWO_STEP or THREE_STEP, the Issues filter and AG selection
     * are preserved, allowing user to change Show filter while keeping other criteria
     * 
     * @param type The type of filter (AG, HUE_RANGE, SELECTED_AGs, ALL)
     * @param value The value associated with the filter type
     */
    public void setFirstCriterion(FilterType type, Object value) {
        this.mode = FilterMode.ONE_STEP;
        this.filterType = type;
        this.filterValue = value;
        // Note: issuesFilterType is NOT cleared, allowing user to stay in TWO_STEP if already there
    }
    
    /**
     * Set second criterion (Issues menu selection)
     * Transitions from ONE_STEP to TWO_STEP if not already in TWO_STEP or THREE_STEP
     * 
     * @param type The type of validation filter (MULTIPLE_AGs, WITHIN_SPREADS, NO_AG_ASSIGNED)
     */
    public void setSecondCriterion(IssuesFilterType type) {
        if (this.mode == FilterMode.ONE_STEP) {
            this.mode = FilterMode.TWO_STEP;
        }
        this.issuesFilterType = type;
    }
    
    /**
     * Clear second criterion (CLEAR - no ISSUE filter in Issues menu)
     * Transitions from TWO_STEP back to ONE_STEP
     * Issues filter is cleared but will be re-applied with default when entering THREE_STEP
     */
    public void clearSecondCriterion() {
        this.mode = FilterMode.ONE_STEP;
        this.issuesFilterType = null;
    }
    
    /**
     * Set third criterion (showSelectedAGsButton click)
     * Transitions to THREE_STEP mode
     * If no Issues filter is currently set, applies default (MULTIPLE_AGs)
     * 
     * @param selectedAGs Set of selected AG numbers from fourAGChoiceswChipsPanel
     * @param invert Whether to invert the result (show complement of subset3 in subset2)
     */
    public void setThirdCriterion(Set<Integer> selectedAGs, boolean invert) {
        // If no Issues filter selected yet, apply default
        if (this.issuesFilterType == null) {
            setDefaultIssuesFilter();
        }
        
        this.mode = FilterMode.THREE_STEP;
        this.selectedAGs = new HashSet<>(selectedAGs);
        this.invert = invert;
    }
    
    /**
     * Set a default Issues filter (used when entering THREE_STEP without explicit selection)
     * Default is MULTIPLE_AGs as it's the most common validation task
     */
    private void setDefaultIssuesFilter() {
        this.issuesFilterType = IssuesFilterType.MULTIPLE_AGs;
    }
    
    // ========== SUBSET GENERATION (On-Demand) ==========
    
    /**
     * Generate subset1 based on first criterion only
     * Called by controller when displaying subset1 in wideSelectionListPanel
     * 
     * @param model The TemplateProxyData model for querying
     * @return Set of UUIDs matching the first criterion
     */
    public Set<UUID> generateSubset1(TemplateProxyData model) {
        if (filterType == null) {
            return new HashSet<>();
        }
        
        switch (filterType) {
            case AG:
                return model.getDrawObjsForAG((Integer) filterValue);
            case HUE_RANGE:
                HueRange range = (HueRange) filterValue;
                return model.getDrawObjsForHueRange(range.ccw, range.cw);
            case SELECTED_AGs:
                return model.getDrawObjsForSelectedAGs((Set<Integer>) filterValue);
            case ALL:
                // Return all DrawObjs in template
                return model.getAllDrawObjs();
            case ALLAGS:
                // Return all DrawObjs assigned to at least one AG
                return model.getDrawObjsForAllAGs();
            default:
                return new HashSet<>();
        }
    }
    
    /**
     * Generate subset2 based on first and second criteria
     * Called by controller when displaying subset2 in narrowSelectionListPanel (TWO_STEP mode)
     * 
     * @param model The TemplateProxyData model for querying
     * @param rmatrix The RMatrixMini for AG and validation queries
     * @return Set of UUIDs matching both first and second criteria
     */
    public Set<UUID> generateSubset2(TemplateProxyData model, RMatrixMini rmatrix) {
        // Start with subset1
        Set<UUID> subset1 = generateSubset1(model);
        
        if (issuesFilterType == null) {
            return subset1;
        }
        
        // Apply second criterion filter
        switch (issuesFilterType) {
            case MULTIPLE_AGs:
                return filterForMultipleAGs(subset1, rmatrix);
            case WITHIN_SPREADS:
                return filterForWithinSpreads(subset1, rmatrix);
            case NO_AG_ASSIGNED:
                return filterForNoAGAssigned(subset1, rmatrix);
            default:
                return subset1;
        }
    }
    
    /**
     * Generate subset3 based on all three criteria
     * Called by controller when displaying subset3 in narrowSelectionListPanel (THREE_STEP mode)
     * 
     * @param model The TemplateProxyData model for querying
     * @param rmatrix The RMatrixMini for AG and validation queries
     * @return Set of UUIDs matching all three criteria (or inverted if invert flag is true)
     */
    public Set<UUID> generateSubset3(TemplateProxyData model, RMatrixMini rmatrix) {
        // Start with subset2
        Set<UUID> subset2 = generateSubset2(model, rmatrix);
        
        if (selectedAGs.isEmpty()) {
            return subset2;
        }
        
        // Filter to only shapes in selected AGs
        Set<UUID> subset3 = new HashSet<>();
        for (Integer ag : selectedAGs) {
            Set<UUID> agMembers = rmatrix.getAGMembersUUIDs(ag);
            if (agMembers != null) {
                subset3.addAll(agMembers);
            }
        }
        
        // Intersect with subset2
        subset3.retainAll(subset2);
        
        // Apply invert if needed: subset3_inverted = subset2 - subset3
        if (invert) {
            Set<UUID> inverted = new HashSet<>(subset2);
            inverted.removeAll(subset3);
            return inverted;
        }
        
        return subset3;
    }
    
    /**
     * Get the active subset based on current mode
     * This is what should be displayed in shapeDrawPanel
     * 
     * @param model The TemplateProxyData model for querying
     * @param rmatrix The RMatrixMini for AG and validation queries
     * @return The appropriate subset (subset1, subset2, or subset3) based on current mode
     */
    public Set<UUID> getActiveSubset(TemplateProxyData model, RMatrixMini rmatrix) {
        switch (mode) {
            case ONE_STEP:
                return generateSubset1(model);
            case TWO_STEP:
                return generateSubset2(model, rmatrix);
            case THREE_STEP:
                return generateSubset3(model, rmatrix);
            default:
                return new HashSet<>();
        }
    }
    
    /**
     * Get subset1 for display in wideSelectionListPanel
     * Always available regardless of current mode
     * 
     * @param model The TemplateProxyData model for querying
     * @return Set of UUIDs for subset1
     */
    public Set<UUID> getSubset1(TemplateProxyData model) {
        return generateSubset1(model);
    }
    
    /**
     * Get subset2 for display in narrowSelectionListPanel (TWO_STEP mode)
     * Only valid in TWO_STEP and THREE_STEP modes
     * 
     * @param model The TemplateProxyData model for querying
     * @param rmatrix The RMatrixMini for AG and validation queries
     * @return Set of UUIDs for subset2, or empty set if not in TWO_STEP/THREE_STEP
     */
    public Set<UUID> getSubset2(TemplateProxyData model, RMatrixMini rmatrix) {
        if (mode == FilterMode.ONE_STEP) {
            return new HashSet<>();
        }
        return generateSubset2(model, rmatrix);
    }
    
    /**
     * Get subset3 for display in narrowSelectionListPanel (THREE_STEP mode)
     * Only valid in THREE_STEP mode
     * 
     * @param model The TemplateProxyData model for querying
     * @param rmatrix The RMatrixMini for AG and validation queries
     * @return Set of UUIDs for subset3, or empty set if not in THREE_STEP
     */
    public Set<UUID> getSubset3(TemplateProxyData model, RMatrixMini rmatrix) {
        if (mode != FilterMode.THREE_STEP) {
            return new HashSet<>();
        }
        return generateSubset3(model, rmatrix);
    }
    
    // ========== GETTERS ==========
    
    /**
     * Get the current filtering mode
     */
    public FilterMode getMode() {
        return mode;
    }
    
    /**
     * Get the first criterion filter type
     */
    public FilterType getFilterType() {
        return filterType;
    }
    
    /**
     * Get the first criterion filter value
     */
    public Object getFilterValue() {
        return filterValue;
    }
    
    /**
     * Get the second criterion filter type
     */
    public IssuesFilterType getIssuesFilterType() {
        return issuesFilterType;
    }
    
    /**
     * Get the selected AGs for third criterion
     */
    public Set<Integer> getSelectedAGs() {
        return new HashSet<>(selectedAGs);
    }
    
    /**
     * Check if invert flag is set for third criterion
     */
    public boolean isInvert() {
        return invert;
    }
    
    // ========== PRIVATE HELPER METHODS FOR FILTERING ==========
    
    /**
     * Filter subset for shapes assigned to multiple AGs
     * 
     * @param subset The subset to filter
     * @param rmatrix The RMatrixMini for querying AG assignments
     * @return Filtered subset containing only shapes with multiple AG assignments
     */
    private Set<UUID> filterForMultipleAGs(Set<UUID> subset, RMatrixMini rmatrix) {
        Set<UUID> result = new HashSet<>();
        for (UUID uuid : subset) {
            Set<Integer> ags = rmatrix.getDrawObjAGs(uuid);
            if (ags.size() > 1) {
                result.add(uuid);
            }
        }
        return result;
    }
    
    /**
     * Filter subset for shapes outside spread limits
     * 
     * Note: This is a placeholder implementation. The actual spread checking logic
     * should be implemented based on AnchorPattern and Spread data structures.
     * For now, this returns the subset unchanged.
     * 
     * @param subset The subset to filter
     * @param rmatrix The RMatrixMini for querying spread violations
     * @return Filtered subset containing only shapes outside spreads
     */
    private Set<UUID> filterForWithinSpreads(Set<UUID> subset, RMatrixMini rmatrix) {
        // TODO: Implement spread checking logic using AnchorPattern
        // This requires access to the AnchorPattern and its Spread data
        // For now, return the subset unchanged
        return new HashSet<>(subset);
    }
    
    /**
     * Filter subset for shapes with no AG assignment
     * 
     * @param subset The subset to filter
     * @param rmatrix The RMatrixMini for querying AG assignments
     * @return Filtered subset containing only shapes with no AG assignment
     */
    private Set<UUID> filterForNoAGAssigned(Set<UUID> subset, RMatrixMini rmatrix) {
        Set<UUID> result = new HashSet<>();
        for (UUID uuid : subset) {
            Set<Integer> ags = rmatrix.getDrawObjAGs(uuid);
            if (ags.isEmpty()) {
                result.add(uuid);
            }
        }
        return result;
    }
    
} // end class FilterState
