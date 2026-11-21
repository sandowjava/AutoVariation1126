package com.csurf.huepattern.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.PaletteWName;
import com.csurf.templatecore.model.Template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;



/**
 * Part of a minimal color intelligence system which can optionally be associated with a Template instance.
 * The RMatrixMini functions in a minimal RelationMatrix role which maintains the relationships
 * between shapes and their color preferences in two areas:
 * 1. AnchorPattern-related state and preferences:
 * - current assigned AnchorGroup
 * - current proportionalLocation of the shape in the AnchorGroup in the range -1.0 - 1.0
 * - AGPrefs which represent the suitability of each AnchorGroup for automated palette variations
 * 2. Value target preferences:
 * - a value target for each shape in the range 0-1.0
 * - a flexibility factor for each shape's value target in the range 0-1.0
 *
 * The primary use of the RMatrixMini is to enable use of the AnchorPattern that defines
 * how colors are organized around the hue circle. AGPrefs and value target are placeholders for future development.
 *
 * This class is a core component of "artistic intelligence" but is designed to be loosely coupled.
 * This class is coupled with the Template class, since it maintains information for color preferences for the shapes in the template.
 * This class is not coupled with the AnchorPattern class, since different instances of AnchorPattern can be applied.
 * This class contains intelligence needed for automatic color assignment and palette generation,
 * by working with the AnchorPattern to determine the hue of each shape. It does not directly create a
 * palette but is used with the AnchorPattern by the palette generator.
 * 
 * 110725 REFACTORED: Converted from index-based arrays to UUID-based maps to eliminate index-based coupling.
 * All data is now keyed by DrawObj UUID instead of array indices, making the system safe from reordering,
 * deletion, and composition operations.
 */
public class RMatrixMiniPrelim {
    private static final Logger logger = Logger.getLogger(RMatrixMiniPrelim.class.getName());
    
    Template theTemplate;
    int numDrawObjs = 0;
    
    // 110725 REFACTORED: Replaced index-based arrays with UUID-based maps
    // This eliminates the index-based coupling vulnerability
    private Map<UUID, Double> valueTargets = new HashMap<>();
    private Map<UUID, Double> valueFlexPossible = new HashMap<>();
    private Map<UUID, ColorPtOstDot> startPalette = new HashMap<>();
    private Map<UUID, Integer> currAGAssignments = new HashMap<>();
    private Map<UUID, Double> proportionLocations = new HashMap<>();
    
    // AG members are now stored as Sets of UUIDs instead of ArrayList<Integer>
    private Set<UUID> membersAG1 = new HashSet<>();
    private Set<UUID> membersAG2 = new HashSet<>();
    private Set<UUID> membersAG3 = new HashSet<>();
    private Set<UUID> membersAG4 = new HashSet<>();
    
    // Legacy support: maintain prefsAmongAGs for future development
    private Map<UUID, double[]> prefsAmongAGs = new HashMap<>();



    // constructor
    public RMatrixMiniPrelim(Template theTemplate) {
        this.theTemplate = theTemplate;
        numDrawObjs = theTemplate.getDrawObjRoster().getNumDrawObjs();
        logger.info("RMatrixMini constructor: Creating with " + numDrawObjs + " DrawObjs");
        
        initStartPalette();
        initValueTargets();
        initValueFlexPossible();
        initProportionLocations();
        initAGMembers();
        
        logger.info("RMatrixMini constructor: Initialization complete");
    } // end constructor



    /**
     * Initializes the start palette by mapping each DrawObj UUID to its color.
     * 110725 REFACTORED: Now uses UUID-based mapping instead of array indices.
     */
    private void initStartPalette() {
        logger.fine("initStartPalette: Starting palette initialization");
        
        PaletteWName templtPalette = theTemplate.buildPaletteFromTemplateDrawObjs();
        ColorPtOstDot[] paletteArray = templtPalette.getColorPtOstdotArray();
        
        List<DrawObj> allDrawObjs = theTemplate.getAllDrawObjs();
        
        if (paletteArray.length != allDrawObjs.size()) {
            logger.warning("initStartPalette: Palette size (" + paletteArray.length + 
                          ") doesn't match DrawObj count (" + allDrawObjs.size() + ")");
        }
        
        for (int i = 0; i < allDrawObjs.size() && i < paletteArray.length; i++) {
            DrawObj drawObj = allDrawObjs.get(i);
            UUID uuid = drawObj.getUniqueId();
            if (uuid != null) {
                startPalette.put(uuid, paletteArray[i]);
                logger.fine("initStartPalette: Mapped UUID " + uuid + " to color");
            } else {
                logger.warning("initStartPalette: DrawObj has null UUID: " + drawObj);
            }
        }
        
        logger.info("initStartPalette: Initialized " + startPalette.size() + " palette entries");
    } // end method initStartPalette

    /**
     * Initializes value targets for each DrawObj based on their current palette color.
     * Each drawObj's value target is initialized to its current luminance value.
     * 110725 REFACTORED: Now uses UUID-based mapping instead of array indices.
     */
    private void initValueTargets() {
        logger.fine("initValueTargets: Starting value target initialization");
        
        List<DrawObj> allDrawObjs = theTemplate.getAllDrawObjs();
        
        for (int i = 0; i < allDrawObjs.size(); i++) {
            DrawObj drawObj = allDrawObjs.get(i);
            UUID uuid = drawObj.getUniqueId();
            if (uuid != null && i < startPalette.size()) {
                ColorPtOstDot color = startPalette.get(uuid);
                if (color != null) {
                    double lumPlanar = color.getLumPlanar();
                    valueTargets.put(uuid, lumPlanar);
                    logger.fine("initValueTargets: Set value target for UUID " + uuid + " to " + lumPlanar);
                }
            }
        }
        
        logger.info("initValueTargets: Initialized " + valueTargets.size() + " value targets");
    } // end method initValueTargets

    /**
     * Initializes value flexibility for each DrawObj to a default value.
     * 110725 REFACTORED: Now uses UUID-based mapping instead of array indices.
     */
    private void initValueFlexPossible() {
        logger.fine("initValueFlexPossible: Starting value flexibility initialization");
        
        List<DrawObj> allDrawObjs = theTemplate.getAllDrawObjs();
        double defaultFlexibility = 0.33;
        
        for (DrawObj drawObj : allDrawObjs) {
            UUID uuid = drawObj.getUniqueId();
            if (uuid != null) {
                valueFlexPossible.put(uuid, defaultFlexibility);
                logger.fine("initValueFlexPossible: Set flexibility for UUID " + uuid + " to " + defaultFlexibility);
            }
        }
        
        logger.info("initValueFlexPossible: Initialized " + valueFlexPossible.size() + " flexibility values");
    } // end method initValueFlexPossible

    /**
     * Initializes proportion locations for each DrawObj to default value (0.0 = at anchor hue).
     * 110725 REFACTORED: Now uses UUID-based mapping instead of array indices.
     */
    private void initProportionLocations() {
        logger.fine("initProportionLocations: Starting proportion location initialization");
        
        List<DrawObj> allDrawObjs = theTemplate.getAllDrawObjs();
        double defaultProportion = 0.0;
        
        for (DrawObj drawObj : allDrawObjs) {
            UUID uuid = drawObj.getUniqueId();
            if (uuid != null) {
                proportionLocations.put(uuid, defaultProportion);
                logger.fine("initProportionLocations: Set proportion for UUID " + uuid + " to " + defaultProportion);
            }
        }
        
        logger.info("initProportionLocations: Initialized " + proportionLocations.size() + " proportion locations");
    } // end method initProportionLocations
    
    /**
     * Initializes AG (AnchorGroup) members as empty sets.
     * 110725 NEW: Initializes AG members as Sets of UUIDs instead of ArrayList<Integer>.
     */
    private void initAGMembers() {
        logger.fine("initAGMembers: Initializing AG member sets");
        
        membersAG1 = new HashSet<>();
        membersAG2 = new HashSet<>();
        membersAG3 = new HashSet<>();
        membersAG4 = new HashSet<>();
        
        logger.info("initAGMembers: AG member sets initialized (all empty)");
    } // end method initAGMembers



    /**
     * Gets the current AG assignment for a DrawObj.
     * 110725 REFACTORED: Now uses DrawObj instead of index.
     * 
     * @param drawObj The DrawObj to get the AG assignment for
     * @return The AG number (1-4), or 0 if not assigned
     */
    public int getCurrAGAssignment(DrawObj drawObj) {
        if (drawObj == null || drawObj.getUniqueId() == null) {
            logger.warning("getCurrAGAssignment: DrawObj or UUID is null");
            return 0;
        }
        return currAGAssignments.getOrDefault(drawObj.getUniqueId(), 0);
    } // end method getCurrAGAssignment

    /**
     * Sets the current AG assignment for a DrawObj.
     * 110725 REFACTORED: Now uses DrawObj instead of index.
     * 
     * @param drawObj The DrawObj to set the AG assignment for
     * @param agNum The AG number (1-4)
     */
    public void setCurrAGAssignment(DrawObj drawObj, int agNum) {
        if (drawObj == null || drawObj.getUniqueId() == null) {
            logger.warning("setCurrAGAssignment: DrawObj or UUID is null");
            return;
        }
        currAGAssignments.put(drawObj.getUniqueId(), agNum);
        logger.fine("setCurrAGAssignment: Set AG assignment for " + drawObj + " to " + agNum);
    } // end method setCurrAGAssignment

    /**
     * Gets the value target for a DrawObj.
     * 110725 REFACTORED: Now uses DrawObj instead of index.
     * 
     * @param drawObj The DrawObj to get the value target for
     * @return The value target (0.0-1.0), or 0.5 if not set
     */
    public double getValueTarget(DrawObj drawObj) {
        if (drawObj == null || drawObj.getUniqueId() == null) {
            logger.warning("getValueTarget: DrawObj or UUID is null");
            return 0.5;
        }
        return valueTargets.getOrDefault(drawObj.getUniqueId(), 0.5);
    } // end method getValueTarget

    /**
     * Sets the value target for a DrawObj.
     * 110725 REFACTORED: Now uses DrawObj instead of index.
     * 
     * @param drawObj The DrawObj to set the value target for
     * @param valueTarget The value target (0.0-1.0)
     */
    public void setValueTarget(DrawObj drawObj, double valueTarget) {
        if (drawObj == null || drawObj.getUniqueId() == null) {
            logger.warning("setValueTarget: DrawObj or UUID is null");
            return;
        }
        valueTargets.put(drawObj.getUniqueId(), valueTarget);
        logger.fine("setValueTarget: Set value target for " + drawObj + " to " + valueTarget);
    } // end method setValueTarget

    /**
     * Gets the value flexibility for a DrawObj.
     * 110725 REFACTORED: Now uses DrawObj instead of index.
     * 
     * @param drawObj The DrawObj to get the value flexibility for
     * @return The value flexibility (0.0-1.0), or 0.33 if not set
     */
    public double getValueFlexPossible(DrawObj drawObj) {
        if (drawObj == null || drawObj.getUniqueId() == null) {
            logger.warning("getValueFlexPossible: DrawObj or UUID is null");
            return 0.33;
        }
        return valueFlexPossible.getOrDefault(drawObj.getUniqueId(), 0.33);
    } // end method getValueFlexPossible

    /**
     * Sets the value flexibility for a DrawObj.
     * 110725 REFACTORED: Now uses DrawObj instead of index.
     * 
     * @param drawObj The DrawObj to set the value flexibility for
     * @param someValueFlex The value flexibility (0.0-1.0)
     */
    public void setValueFlexPossible(DrawObj drawObj, double someValueFlex) {
        if (drawObj == null || drawObj.getUniqueId() == null) {
            logger.warning("setValueFlexPossible: DrawObj or UUID is null");
            return;
        }
        valueFlexPossible.put(drawObj.getUniqueId(), someValueFlex);
        logger.fine("setValueFlexPossible: Set flexibility for " + drawObj + " to " + someValueFlex);
    } // end method setValueFlexPossible

    /**
     * Gets the proportion location for a DrawObj.
     * 110725 REFACTORED: Now uses DrawObj instead of index.
     * 
     * @param drawObj The DrawObj to get the proportion location for
     * @return The proportion location (-1.0 to 1.0), or 0.0 if not set
     */
    public double getProportionLocation(DrawObj drawObj) {
        if (drawObj == null || drawObj.getUniqueId() == null) {
            logger.warning("getProportionLocation: DrawObj or UUID is null");
            return 0.0;
        }
        return proportionLocations.getOrDefault(drawObj.getUniqueId(), 0.0);
    } // end method getProportionLocation

    /**
     * Sets the proportion location for a DrawObj.
     * 110725 REFACTORED: Now uses DrawObj instead of index.
     * 
     * @param drawObj The DrawObj to set the proportion location for
     * @param someProportionLocation The proportion location (-1.0 to 1.0)
     */
    public void setProportionLocation(DrawObj drawObj, double someProportionLocation) {
        if (drawObj == null || drawObj.getUniqueId() == null) {
            logger.warning("setProportionLocation: DrawObj or UUID is null");
            return;
        }
        proportionLocations.put(drawObj.getUniqueId(), someProportionLocation);
        logger.fine("setProportionLocation: Set proportion for " + drawObj + " to " + someProportionLocation);
    } // end method setProportionLocation

    /**
     * Gets the start palette as a map of UUID to color.
     * 110725 REFACTORED: Now returns UUID-based map instead of array.
     * 
     * @return A map of DrawObj UUIDs to their start palette colors
     */
    public Map<UUID, ColorPtOstDot> getStartPaletteMap() {
        return new HashMap<>(startPalette);
    } // end method getStartPaletteMap
    
    /**
     * Gets the start palette color for a specific DrawObj.
     * 110725 NEW: Convenience method for getting a single color.
     * 
     * @param drawObj The DrawObj to get the color for
     * @return The start palette color, or null if not found
     */
    public ColorPtOstDot getStartPaletteColor(DrawObj drawObj) {
        if (drawObj == null || drawObj.getUniqueId() == null) {
            logger.warning("getStartPaletteColor: DrawObj or UUID is null");
            return null;
        }
        return startPalette.get(drawObj.getUniqueId());
    } // end method getStartPaletteColor

    /**
     * Gets the AG members as a Set of DrawObjs.
     * 110725 REFACTORED: Now returns Set of DrawObjs instead of int array.
     * 
     * @param whichAG The AG number (1-4)
     * @return A set of DrawObj instances in the specified AG
     */
    public Set<DrawObj> getAGMembersSet(int whichAG) {
        Set<UUID> memberUUIDs;
        switch(whichAG) {
            case 1:
                memberUUIDs = membersAG1; break;
            case 2:
                memberUUIDs = membersAG2; break;
            case 3:
                memberUUIDs = membersAG3; break;
            case 4:
                memberUUIDs = membersAG4; break;
            default:
                logger.warning("getAGMembersSet: Invalid AG number: " + whichAG);
                return new HashSet<>();
        }
        
        // Convert UUIDs to DrawObj instances
        Set<DrawObj> members = new HashSet<>();
        List<DrawObj> allDrawObjs = theTemplate.getAllDrawObjs();
        
        for (DrawObj drawObj : allDrawObjs) {
            if (drawObj.getUniqueId() != null && memberUUIDs.contains(drawObj.getUniqueId())) {
                members.add(drawObj);
            }
        }
        
        logger.fine("getAGMembersSet: AG " + whichAG + " has " + members.size() + " members");
        return members;
    } // end method getAGMembersSet
    
    /**
     * Adds a DrawObj to an AG.
     * 110725 NEW: UUID-based method for adding members to AG.
     * 
     * @param drawObj The DrawObj to add
     * @param agNum The AG number (1-4)
     */
    public void addToAG(DrawObj drawObj, int agNum) {
        if (drawObj == null || drawObj.getUniqueId() == null) {
            logger.warning("addToAG: DrawObj or UUID is null");
            return;
        }
        
        Set<UUID> memberSet = getAGMemberSet(agNum);
        if (memberSet != null) {
            memberSet.add(drawObj.getUniqueId());
            logger.fine("addToAG: Added " + drawObj + " to AG " + agNum);
        }
    } // end method addToAG
    
    /**
     * Removes a DrawObj from an AG.
     * 110725 NEW: UUID-based method for removing members from AG.
     * 
     * @param drawObj The DrawObj to remove
     * @param agNum The AG number (1-4)
     */
    public void removeFromAG(DrawObj drawObj, int agNum) {
        if (drawObj == null || drawObj.getUniqueId() == null) {
            logger.warning("removeFromAG: DrawObj or UUID is null");
            return;
        }
        
        Set<UUID> memberSet = getAGMemberSet(agNum);
        if (memberSet != null) {
            memberSet.remove(drawObj.getUniqueId());
            logger.fine("removeFromAG: Removed " + drawObj + " from AG " + agNum);
        }
    } // end method removeFromAG
    
    /**
     * Gets the internal AG member set for a given AG number.
     * 110725 INTERNAL: Helper method for accessing AG member sets.
     * 
     * @param agNum The AG number (1-4)
     * @return The Set of UUIDs for that AG, or null if invalid AG number
     */
    private Set<UUID> getAGMemberSet(int agNum) {
        switch(agNum) {
            case 1: return membersAG1;
            case 2: return membersAG2;
            case 3: return membersAG3;
            case 4: return membersAG4;
            default:
                logger.warning("getAGMemberSet: Invalid AG number: " + agNum);
                return null;
        }
    } // end method getAGMemberSet
    
    // ============================================================================
    // UUID-BASED METHODS (NEW for TemplateProxyData support)
    // ============================================================================
    
    /**
     * Gets the current AG assignment for a DrawObj by UUID.
     * 110725 NEW: UUID-based version for TemplateProxyData.
     * 
     * @param drawObjUUID The UUID of the DrawObj
     * @return The AG number (1-4), or 0 if not assigned
     */
    public int getCurrAGAssignment(UUID drawObjUUID) {
        if (drawObjUUID == null) {
            logger.warning("getCurrAGAssignment(UUID): UUID is null");
            return 0;
        }
        return currAGAssignments.getOrDefault(drawObjUUID, 0);
    } // end method getCurrAGAssignment(UUID)
    
    /**
     * Sets the current AG assignment for a DrawObj by UUID.
     * 110725 NEW: UUID-based version for TemplateProxyData.
     * 
     * @param drawObjUUID The UUID of the DrawObj
     * @param agNum The AG number (1-4)
     */
    public void setCurrAGAssignment(UUID drawObjUUID, int agNum) {
        if (drawObjUUID == null) {
            logger.warning("setCurrAGAssignment(UUID): UUID is null");
            return;
        }
        currAGAssignments.put(drawObjUUID, agNum);
        logger.fine("setCurrAGAssignment(UUID): Set AG assignment for UUID " + drawObjUUID + " to " + agNum);
    } // end method setCurrAGAssignment(UUID)
    
    /**
     * Gets the proportion location for a DrawObj by UUID.
     * 110725 NEW: UUID-based version for TemplateProxyData.
     * 
     * @param drawObjUUID The UUID of the DrawObj
     * @return The proportion location (-1.0 to 1.0), or 0.0 if not set
     */
    public double getProportionLocation(UUID drawObjUUID) {
        if (drawObjUUID == null) {
            logger.warning("getProportionLocation(UUID): UUID is null");
            return 0.0;
        }
        return proportionLocations.getOrDefault(drawObjUUID, 0.0);
    } // end method getProportionLocation(UUID)
    
    /**
     * Sets the proportion location for a DrawObj by UUID.
     * 110725 NEW: UUID-based version for TemplateProxyData.
     * 
     * @param drawObjUUID The UUID of the DrawObj
     * @param someProportionLocation The proportion location (-1.0 to 1.0)
     */
    public void setProportionLocation(UUID drawObjUUID, double someProportionLocation) {
        if (drawObjUUID == null) {
            logger.warning("setProportionLocation(UUID): UUID is null");
            return;
        }
        proportionLocations.put(drawObjUUID, someProportionLocation);
        logger.fine("setProportionLocation(UUID): Set proportion for UUID " + drawObjUUID + " to " + someProportionLocation);
    } // end method setProportionLocation(UUID)
    
    /**
     * Gets all AG assignments for a DrawObj by UUID.
     * 110725 NEW: Returns all AGs that contain this DrawObj.
     * 
     * @param drawObjUUID The UUID of the DrawObj
     * @return A set of AG numbers (1-4) that contain this DrawObj
     */
    public Set<Integer> getDrawObjAGs(UUID drawObjUUID) {
        if (drawObjUUID == null) {
            logger.warning("getDrawObjAGs: UUID is null");
            return new HashSet<>();
        }
        
        Set<Integer> ags = new HashSet<>();
        if (membersAG1.contains(drawObjUUID)) ags.add(1);
        if (membersAG2.contains(drawObjUUID)) ags.add(2);
        if (membersAG3.contains(drawObjUUID)) ags.add(3);
        if (membersAG4.contains(drawObjUUID)) ags.add(4);
        
        logger.fine("getDrawObjAGs: UUID " + drawObjUUID + " is in AGs: " + ags);
        return ags;
    } // end method getDrawObjAGs
    
    /**
     * Gets all DrawObjs that are assigned to multiple AGs.
     * 110725 NEW: Used for validation during UC-042.
     * 
     * @return A set of DrawObj instances that have multiple AG assignments
     */
    public Set<DrawObj> getDrawObjsInMultipleAGs() {
        Set<DrawObj> result = new HashSet<>();
        List<DrawObj> allDrawObjs = theTemplate.getAllDrawObjs();
        
        for (DrawObj drawObj : allDrawObjs) {
            UUID uuid = drawObj.getUniqueId();
            if (uuid != null) {
                Set<Integer> ags = getDrawObjAGs(uuid);
                if (ags.size() > 1) {
                    result.add(drawObj);
                }
            }
        }
        
        logger.info("getDrawObjsInMultipleAGs: Found " + result.size() + " DrawObjs in multiple AGs");
        return result;
    } // end method getDrawObjsInMultipleAGs
    
    /**
     * Gets all DrawObjs that have no AG assignment.
     * 110725 NEW: Used for validation during UC-042.
     * 
     * @return A set of DrawObj instances with no AG assignment
     */
    public Set<DrawObj> getDrawObjsWithNoAG() {
        Set<DrawObj> result = new HashSet<>();
        List<DrawObj> allDrawObjs = theTemplate.getAllDrawObjs();
        
        for (DrawObj drawObj : allDrawObjs) {
            UUID uuid = drawObj.getUniqueId();
            if (uuid != null) {
                Set<Integer> ags = getDrawObjAGs(uuid);
                if (ags.isEmpty()) {
                    result.add(drawObj);
                }
            }
        }
        
        logger.info("getDrawObjsWithNoAG: Found " + result.size() + " DrawObjs with no AG assignment");
        return result;
    } // end method getDrawObjsWithNoAG
    
    /**
     * Validates that each DrawObj has exactly one AG assignment.
     * 110725 NEW: Used for validation at UC-045.
     * 
     * @return true if all DrawObjs have exactly one AG, false otherwise
     */
    public boolean validateSingleAGPerDrawObj() {
        List<DrawObj> allDrawObjs = theTemplate.getAllDrawObjs();
        
        for (DrawObj drawObj : allDrawObjs) {
            UUID uuid = drawObj.getUniqueId();
            if (uuid != null) {
                Set<Integer> ags = getDrawObjAGs(uuid);
                if (ags.size() != 1) {
                    logger.warning("validateSingleAGPerDrawObj: DrawObj " + drawObj + " has " + ags.size() + " AG assignments");
                    return false;
                }
            }
        }
        
        logger.info("validateSingleAGPerDrawObj: All DrawObjs have exactly one AG assignment");
        return true;
    } // end method validateSingleAGPerDrawObj
    
    /**
     * Gets the AG member set as a Set of UUIDs.
     * 110725 NEW: UUID-based version for TemplateProxyData.
     * 
     * @param whichAG The AG number (1-4)
     * @return A set of UUIDs for DrawObjs in the specified AG
     */
    public Set<UUID> getAGMembersUUIDs(int whichAG) {
        Set<UUID> memberUUIDs;
        switch(whichAG) {
            case 1:
                memberUUIDs = membersAG1; break;
            case 2:
                memberUUIDs = membersAG2; break;
            case 3:
                memberUUIDs = membersAG3; break;
            case 4:
                memberUUIDs = membersAG4; break;
            default:
                logger.warning("getAGMembersUUIDs: Invalid AG number: " + whichAG);
                return new HashSet<>();
        }
        
        logger.fine("getAGMembersUUIDs: AG " + whichAG + " has " + memberUUIDs.size() + " members");
        return new HashSet<>(memberUUIDs);
    } // end method getAGMembersUUIDs
    
    /**
     * Adds a DrawObj to an AG by UUID.
     * 110725 NEW: UUID-based version for TemplateProxyData.
     * 
     * @param drawObjUUID The UUID of the DrawObj
     * @param agNum The AG number (1-4)
     */
    public void addToAG(UUID drawObjUUID, int agNum) {
        if (drawObjUUID == null) {
            logger.warning("addToAG(UUID): UUID is null");
            return;
        }
        
        Set<UUID> memberSet = getAGMemberSet(agNum);
        if (memberSet != null) {
            memberSet.add(drawObjUUID);
            logger.fine("addToAG(UUID): Added UUID " + drawObjUUID + " to AG " + agNum);
        }
    } // end method addToAG(UUID)
    
    /**
     * Removes a DrawObj from an AG by UUID.
     * 110725 NEW: UUID-based version for TemplateProxyData.
     * 
     * @param drawObjUUID The UUID of the DrawObj
     * @param agNum The AG number (1-4)
     */
    public void removeFromAG(UUID drawObjUUID, int agNum) {
        if (drawObjUUID == null) {
            logger.warning("removeFromAG(UUID): UUID is null");
            return;
        }
        
        Set<UUID> memberSet = getAGMemberSet(agNum);
        if (memberSet != null) {
            memberSet.remove(drawObjUUID);
            logger.fine("removeFromAG(UUID): Removed UUID " + drawObjUUID + " from AG " + agNum);
        }
    } // end method removeFromAG(UUID)



} // end class RMatrixMini
