package com.csurf.templatecore.model;

import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;
import java.util.logging.Logger;

/**
 * Manages scoped OstHueColumnStep equivalents for a single TemplateWorkFrame.
 * 
 * This class solves the critical design flaw where ColorPtOstDot instances are singletons,
 * causing shared OstHueColumnStep instances across multiple TemplateWorkFrame instances.
 * 
 * PaletteExplicit creates a second collection of OstHueColumnStep instances (scoped equivalents)
 * that are completely isolated to each TemplateWorkFrame. This enables:
 * - Proper listener attachment/detachment per frame
 * - Correct event firing to frame-scoped listeners
 * - Prevention of cross-TemplateWorkFrame event propagation
 * - Accurate occupancy tracking per frame
 * 
 * Key Responsibilities:
 * 1. Maintain scopedOtdotEquivsSet: Map from singleton OstHCS to scoped equivalent
 * 2. Maintain occupancyMap: Track how many DrawObj instances reference each scoped equivalent
 * 3. Create scoped equivalents on demand when DrawObj colors change
 * 4. Manage listener attachment/detachment to scoped equivalents
 * 5. Fire CREATION/DELETION events to scoped equivalents
 * 
 * Current operating model (pull model): the DrawObjRoster is the source of truth for the current palette.
 * PaletteExplicit is an explicit, derived representation. Its query methods first call
 * DrawObjRoster.updateToCurrObjState(), which refreshes the attached PaletteExplicit by recounting
 * from the roster's DrawObjs (see refreshFromRoster). No listeners or events are involved in this mode.
 * The event-driven methods (onDrawObjColorChanged, rebuildAfterDeserialization) are not used in this mode.
 * 
 * Scope: One instance per Template shown in a TemplateWorkFrame, created in the TemplateWorkFrame
 * constructor and whenever the frame loads a different Template
 */
public class PaletteExplicit {
    private static final Logger logger = Logger.getLogger(PaletteExplicit.class.getName());
    
    /**
     * Maps singleton OstHueColumnStep instances (from ColorPtOstDot) to their
     * scoped equivalent OstHueColumnStep instances.
     * 
     * Uses IdentityHashMap to distinguish between singleton and scoped instances.
     * Value-based equality would cause them to be treated as the same key.
     */
    @JsonIgnore
    private final Map<OstHueColumnStep, OstHueColumnStep> scopedOtdotEquivsSet;
    
    /**
     * Tracks occupancy count for each scoped OstHueColumnStep.
     * 
     * Key: Scoped OstHueColumnStep instance
     * Value: Number of DrawObj instances currently using this color
     * 
     * Used to determine when to attach/detach listeners.
     */
    @JsonIgnore
    private final Map<OstHueColumnStep, Integer> occupancyMap;
    
    /**
     * Reference to the DrawObjRoster that will listen to scoped equivalent events.
     * 
     * PaletteExplicit fires events to scoped equivalents, and DrawObjRoster
     * listens to those events to update its occupancy maps.
     */
    @JsonIgnore
    private final DrawObjRoster drawObjRoster;
    
    /**
     * Creates a new PaletteExplicit instance for a specific Template.
     * 
     * This constructor only creates empty maps and stores the Template's DrawObjRoster. It does NOT
     * scan the DrawObjs. The palette is filled by refreshFromRoster(), which runs when the roster
     * refreshes (DrawObjRoster.updateToCurrObjState) and before each query method answers.
     * 
     * @param template The Template whose DrawObjRoster this PaletteExplicit pulls from
     * @throws IllegalArgumentException if template is null
     */
    public PaletteExplicit(Template template) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }
        
        this.drawObjRoster = template.getDrawObjRoster();
        this.scopedOtdotEquivsSet = new IdentityHashMap<>();
        this.occupancyMap = new IdentityHashMap<>();
        
        logger.info("PaletteExplicit created for template: " + template.getTheTemplateName());
    }
    
    /**
     * Gets or creates a scoped OstHueColumnStep equivalent for a singleton instance.
     * 
     * If a scoped equivalent already exists for the given singleton, returns it.
     * Otherwise, creates a new scoped equivalent with the same (hue, column, step) values.
     * 
     * @param singletonOHCS The singleton OstHueColumnStep from ColorPtOstDot
     * @return The scoped equivalent OstHueColumnStep for this TemplateWorkFrame
     */
    public OstHueColumnStep getScopedOstHCS(OstHueColumnStep singletonOHCS) {
        if (singletonOHCS == null) {
            return null;
        }
        
        // Check if scoped equivalent already exists
        OstHueColumnStep scopedEquivalentOHCS = scopedOtdotEquivsSet.get(singletonOHCS);
        
        if (scopedEquivalentOHCS == null) {
            // Create new scoped equivalent using the singleton's factory method
            scopedEquivalentOHCS = singletonOHCS.createScopedEquivalentOHCS();
            
            // Store in map
            scopedOtdotEquivsSet.put(singletonOHCS, scopedEquivalentOHCS);
            
            // Initialize occupancy to 0
            occupancyMap.put(scopedEquivalentOHCS, 0);
            
            logger.fine("Created scoped equivalent for singleton OstHCS: " + 
                singletonOHCS.getOstPosition());
        }
        
        return scopedEquivalentOHCS;
    } // end method getScopedOstHCS


    
    /**
     * Handles a DrawObj color change notification (event-driven path, for future use).
     * 
     * NOTE: Nothing currently calls this method. Under the pull model the palette is kept current by
     * refreshFromRoster() instead. This method is intended to be called by a future color-change hook
     * (for example a DrawObj.setColor() listener), not by DrawObjRoster.
     * It manages:
     * 1. Decrementing occupancy for the old color
     * 2. Firing DELETION event to the old scoped equivalent
     * 3. Detaching listener if occupancy reaches 0
     * 4. Incrementing occupancy for the new color
     * 5. Attaching listener if occupancy was 0
     * 6. Firing CREATION event to the new scoped equivalent
     * 
     * @param drawObj The DrawObj whose color changed
     * @param oldOstHCS The old singleton OstHueColumnStep (may be null)
     * @param newOstHCS The new singleton OstHueColumnStep (may be null)
     */
    public void onDrawObjColorChanged(DrawObj drawObj, 
                                      OstHueColumnStep oldOstHCS, 
                                      OstHueColumnStep newOstHCS) {
        // Handle old color
        if (oldOstHCS != null) {
            OstHueColumnStep oldScopedEquivalentOHCS = getScopedOstHCS(oldOstHCS);
            
            // Decrement occupancy
            int oldOccupancy = occupancyMap.getOrDefault(oldScopedEquivalentOHCS, 0);
            if (oldOccupancy > 0) {
                occupancyMap.put(oldScopedEquivalentOHCS, oldOccupancy - 1);
            }
            
            // Fire DELETION event
            oldScopedEquivalentOHCS.fireDeletionEvent();
            
            // Detach listener if occupancy reaches 0
            if (occupancyMap.get(oldScopedEquivalentOHCS) == 0) {
                oldScopedEquivalentOHCS.removeOstHueColumnStepListener(drawObjRoster);
                logger.fine("Detached listener from scoped OstHCS: " + 
                    oldOstHCS.getOstPosition());
            }
        }
        
        // Handle new color
        if (newOstHCS != null) {
            OstHueColumnStep newScopedEquivalentOHCS = getScopedOstHCS(newOstHCS);
            
            // Check if we need to attach listener
            int newOccupancy = occupancyMap.getOrDefault(newScopedEquivalentOHCS, 0);
            if (newOccupancy == 0) {
                newScopedEquivalentOHCS.addOstHueColumnStepListener(drawObjRoster);
                logger.fine("Attached listener to scoped OstHCS: " + 
                    newOstHCS.getOstPosition());
            }
            
            // Increment occupancy
            occupancyMap.put(newScopedEquivalentOHCS, newOccupancy + 1);
            
            // Fire CREATION event
            newScopedEquivalentOHCS.fireCreationEvent();
        }
    } // end method onDrawObjColorChanged


    
    /**
     * Rebuilds scoped equivalents after Template deserialization.
     * 
     * During deserialization, PaletteExplicit is empty. This method rebuilds
     * the scoped equivalents by scanning all DrawObj instances and creating
     * scoped equivalents for their current colors.
     * 
     * This method should be called in Template.initAfterDeserialization() or
     * in TemplateWorkFrame after the Template is loaded.
     * 
     * @param drawObjRoster The DrawObjRoster containing the DrawObj instances
     */
    public void rebuildAfterDeserialization(DrawObjRoster drawObjRoster) {
        if (drawObjRoster == null) {
            logger.warning("Cannot rebuild PaletteExplicit: DrawObjRoster is null");
            return;
        }
        
        // Clear existing scoped equivalents
        scopedOtdotEquivsSet.clear();
        occupancyMap.clear();
        
        logger.info("Rebuilding PaletteExplicit after deserialization");
        
        // Iterate through all DrawObj instances and create scoped equivalents
        for (DrawObj drawObj : drawObjRoster.getTemplateDrawObjs()) {
            if (drawObj != null && drawObj.getCurrentColor() != null) {
                OstHueColumnStep singletonOHCS = drawObj.getCurrentColor().getOstHCS();
                if (singletonOHCS != null) {
                    // Get or create scoped equivalent
                    OstHueColumnStep scopedEquivalentOHCS = getScopedOstHCS(singletonOHCS);
                    
                    // Increment occupancy
                    int occupancy = occupancyMap.getOrDefault(scopedEquivalentOHCS, 0);
                    occupancyMap.put(scopedEquivalentOHCS, occupancy + 1);
                }
            }
        }
        
        // Attach listeners to all scoped equivalents that have occupancy > 0
        for (Map.Entry<OstHueColumnStep, Integer> entry : occupancyMap.entrySet()) {
            if (entry.getValue() > 0) {
                entry.getKey().addOstHueColumnStepListener(drawObjRoster);
                logger.fine("Attached listener to scoped OstHCS during rebuild: " + 
                    entry.getKey().getOstPosition());
            }
        }
    } // end method rebuildAfterDeserialization


    
    /**
     * Returns the number of DrawObj instances currently using a specific scoped OstHueColumnStep.
     * 
     * @param scopedOstHCS The scoped OstHueColumnStep
     * @return The occupancy count (0 if not found)
     */
    public int getOccupancy(OstHueColumnStep scopedOstHCS) {
        pullFromRoster();
        return occupancyMap.getOrDefault(scopedOstHCS, 0);
    }



    /**
     * Recounts the palette from the DrawObjRoster (the source of truth).
     * 
     * All occupancy counts are reset to 0 and then recounted, one per DrawObj. Existing scoped
     * equivalents are kept (same instances), so entries for locations no longer in use remain in the
     * maps with count 0. DrawObjs that are null, or have no color or no OstHCS, are skipped.
     * No listeners are attached and no events are fired.
     * 
     * Normally called by DrawObjRoster.updateToCurrObjState(); clients should use the query methods.
     */
    public void refreshFromRoster() {
        occupancyMap.replaceAll((location, count) -> 0);
        if (drawObjRoster == null) {
            return;
        }
        List<DrawObj> drawObjs = drawObjRoster.getTemplateDrawObjs();
        if (drawObjs == null) {
            return;
        }
        for (DrawObj drawObj : drawObjs) {
            if (drawObj == null || drawObj.getCurrentColor() == null) {
                continue;
            }
            OstHueColumnStep singletonOHCS = drawObj.getCurrentColor().getOstHCS();
            if (singletonOHCS == null) {
                continue;
            }
            occupancyMap.merge(getScopedOstHCS(singletonOHCS), 1, Integer::sum);
        }
    } // end method refreshFromRoster



    /**
     * Makes sure this palette is current before answering a query. The roster's refresh updates the
     * PaletteExplicit attached to it; if this instance is not the attached one, it refreshes itself.
     */
    private void pullFromRoster() {
        if (drawObjRoster == null) {
            return;
        }
        drawObjRoster.updateToCurrObjState();
        if (drawObjRoster.getPaletteExplicit() != this) {
            refreshFromRoster();
        }
    }



    /**
     * Checks whether a location is in the palette (used by at least one DrawObj).
     * Accepts either a singleton OstHueColumnStep (e.g. from a ColorPtOstDot) or a scoped equivalent.
     * Does not create scoped equivalents.
     * 
     * @param location singleton or scoped OstHueColumnStep; null returns false
     * @return true if at least one DrawObj currently uses this location
     */
    public boolean isOccupied(OstHueColumnStep location) {
        if (location == null) {
            return false;
        }
        pullFromRoster();
        OstHueColumnStep scoped = scopedOtdotEquivsSet.get(location);
        if (scoped == null && occupancyMap.containsKey(location)) {
            scoped = location;
        }
        return scoped != null && occupancyMap.getOrDefault(scoped, 0) > 0;
    }



    /**
     * Returns the palette: the scoped locations used by at least one DrawObj (occupancy count above 0).
     * The returned set is unmodifiable and compares by identity, like the maps in this class.
     * 
     * @return the occupied scoped OstHueColumnSteps
     */
    public Set<OstHueColumnStep> getOccupiedLocations() {
        pullFromRoster();
        Set<OstHueColumnStep> result = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Map.Entry<OstHueColumnStep, Integer> entry : occupancyMap.entrySet()) {
            if (entry.getValue() > 0) {
                result.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(result);
    }



    /**
     * @return the number of locations in the palette (not the size of occupancyMap, which also holds
     *         zero-count entries)
     */
    public int getOccupiedLocationCount() {
        return getOccupiedLocations().size();
    }
    
    /**
     * Returns the scoped equivalents map.
     * 
     * @return The scopedOtdotEquivsSet map
     */
    public Map<OstHueColumnStep, OstHueColumnStep> getScopedOtdotEquivsSet() {
        return scopedOtdotEquivsSet;
    }
    
    /**
     * Returns the occupancy map.
     * 
     * @return The occupancyMap
     */
    public Map<OstHueColumnStep, Integer> getOccupancyMap() {
        return occupancyMap;
    }


} // end class PaletteExplicit
