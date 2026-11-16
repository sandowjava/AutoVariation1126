package com.csurf.templatecore.model;

import com.csurf.artintellig.domainanchorpattern.model.IQAnchorPattern;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.templatecore.svgconverter.controller.SVGExporter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Template class that stores a collection of DrawObjs and their colors.
 * The ring angle fields are temporary and will be moved to a more appropriate class.
 */
public class Template {
    // TODO: Ring angles will be moved to a more appropriate class
    private double ring1Angle;
    private double ring2Angle;
    private double ring3Angle;
    private double ring4Angle;
    private final List<DrawObj> allDrawObjs;
    private String theTemplateName;
    private String theTemplateSavedInstanceName;
    private final DrawObjRoster drawObjRoster;
    
    // SVG document metadata
    private String svgViewBox;
    private double svgWidth;
    private double svgHeight;
    private Map<String, String> svgDocumentAttributes;
    private String originalSvgFilePath;
    
    // Map to store named SelectionGroups
    @JsonProperty("namedSelectionGroups")
    private final Map<String, SelectionGroup> namedSelectionGroups = new HashMap<>();

    /**
     * Creates an empty Template. Examine this constructor to see where it might be used.
     * Constructor may never be used.
     */
    public Template() {
        this.ring1Angle = 0.0;
        this.ring2Angle = 0.0;
        this.ring3Angle = 0.0;
        this.ring4Angle = 0.0;
        this.allDrawObjs = new ArrayList<>();
        this.drawObjRoster = new DrawObjRoster();
        // Initialize DrawObjRoster with a reference to Template's allDrawObjs collection
        this.drawObjRoster.setTemplateDrawObjs(this.allDrawObjs);
        // 071425 Enable position trackers
        this.drawObjRoster.setUsePositionTrackers(true);
        
        // Initialize SVG document metadata
        this.svgViewBox = "0 0 100 100"; // Default viewBox
        this.svgWidth = 100;
        this.svgHeight = 100;
        this.svgDocumentAttributes = new HashMap<>();
    } // end constructor




    /**
     * Creates a Template from an array of DrawObjs.
     * This is the constructor for creating Templates from SVG conversion output. Essential and still used.
     * Gets called - in flow of OPENING an SVG file by method createNoNameTemplate of SVGConverterFrame?? YES.
     * No, it's NOT in actionListener in flow from  menu item to save template - in SVGConverterFrame
     * 
     * @param drawObjs array of DrawObjs from SVG conversion
     * @throws IllegalArgumentException if drawObjs is null
     */
    public Template(DrawObj[] drawObjs) {
        this();
        if (drawObjs == null) {
            throw new IllegalArgumentException("DrawObj array cannot be null");
        }
        this.allDrawObjs.addAll(Arrays.asList(drawObjs));
        convertColorsToOstDots();
        
        // Update the DrawObjRoster's state based on the DrawObjs
        drawObjRoster.updateToCurrObjState();
    } // end constructor



    
    /**
     * Creates a Template from an array of DrawObjs.
     * This is the PRIMARY CONSTRUCTOR for creating Templates from SVG conversion output.
     *
     * @param drawObjs array of DrawObjs from SVG conversion
     * @throws IllegalArgumentException if drawObjs is null
     */
    public Template(String someName, DrawObj[] drawObjs) {
        this();
        if (drawObjs == null) {
            throw new IllegalArgumentException("DrawObj array cannot be null");
        }
        this.allDrawObjs.addAll(Arrays.asList(drawObjs));
        this.theTemplateName = someName; // TODO 062425 NOTE: this is name from little dialog - not from fileBrowser
        convertColorsToOstDots();
        
        // Update the DrawObjRoster's state based on the DrawObjs
        drawObjRoster.updateToCurrObjState();
    } // end PRIMARY CONSTRUCTOR




    /**
     * Creates a Template from a List of DrawObjs.
     * This constructor is useful for TESTING and when DrawObjs are already in a List.
     *
     * @param someName name for the template
     * @param drawObjs List of DrawObjs
     * @throws IllegalArgumentException if drawObjs is null
     */
    public Template(String someName, List<DrawObj> drawObjs) {
        this();
        if (drawObjs == null) {
            throw new IllegalArgumentException("DrawObj list cannot be null");
        }
        this.allDrawObjs.addAll(drawObjs);
        this.theTemplateName = someName;
        convertColorsToOstDots();
        
        // Update the DrawObjRoster's state based on the DrawObjs
        drawObjRoster.updateToCurrObjState();
    } // end constructor




    /**
     * Gets an unmodifiable view of the draw objects list.
     * @return unmodifiable list of draw objects
     */
    public List<DrawObj> getAllDrawObjs() {
        return Collections.unmodifiableList(allDrawObjs);
    } // end method getAllDrawObjs



    /**
     * Gets the DrawObjRoster for this Template
     */
    public DrawObjRoster getDrawObjRoster() {
        return drawObjRoster;
    } // end method getDrawObjRoster

    /**
     * Checks if exactly one DrawObj is currently selected in this template.
     * 
     * @return true if exactly one DrawObj is selected, false otherwise
     */
    public boolean onlyOneDrawobjSelected() {
        Set<DrawObj> selectedDrawObjs = drawObjRoster.getSelectedDrawObjs();
        return selectedDrawObjs.size() == 1;
    } // end method onlyOneDrawobjSelected

    /**
     * Adds a DrawObj to the Template.
     * This method adds the DrawObj to the Template's collection and updates the DrawObjRoster.
     * 
     * @param drawObj The DrawObj to add
     */
    public void addDrawObj(DrawObj drawObj) {
        if (drawObj != null) {
            allDrawObjs.add(drawObj);
            // Since DrawObjRoster now references allDrawObjs, we just need to update its state
            drawObjRoster.updateToCurrObjState();
        }
    } // end method addDrawObj




    /**
     * Removes a DrawObj from the Template.
     * This method removes the DrawObj from the Template's collection and updates the DrawObjRoster.
     * 
     * @param drawObj The DrawObj to remove
     */
    public void removeDrawObj(DrawObj drawObj) {
        if (drawObj != null && allDrawObjs.remove(drawObj)) {
            // Since DrawObjRoster now references allDrawObjs, we just need to update its state
            drawObjRoster.updateToCurrObjState();
        }
    } // end method removeDrawObj



    /**
     * Clears all DrawObjs from the Template.
     * This method clears the Template's collection and updates the DrawObjRoster.
     */
    public void clearDrawObjs() {
        allDrawObjs.clear();
        // Since DrawObjRoster now references allDrawObjs, we just need to update its state
        drawObjRoster.updateToCurrObjState();
    } // end method clearDrawObjs



    public String getTheTemplateName() {
        return theTemplateName;
    } // end method getTheTemplateName




    // TODO: Ring angle getters/setters will be moved to a more appropriate class
    public double getRing1Angle() {
        return ring1Angle;
    }

    public void setRing1Angle(double ring1Angle) {
        this.ring1Angle = ring1Angle;
    }

    public double getRing2Angle() {
        return ring2Angle;
    }

    public void setRing2Angle(double ring2Angle) {
        this.ring2Angle = ring2Angle;
    }

    public double getRing3Angle() {
        return ring3Angle;
    }

    public void setRing3Angle(double ring3Angle) {
        this.ring3Angle = ring3Angle;
    }

    public double getRing4Angle() {
        return ring4Angle;
    }

    public void setRing4Angle(double ring4Angle) {
        this.ring4Angle = ring4Angle;
    }




    private void convertColorsToOstDots() {
        for (DrawObj drawObj : allDrawObjs) {
            ColorPtOstDot ostDot = MonotriBook.getInstance().findClosestOstDot(drawObj.getCurrentColor());
            drawObj.setColor(ostDot);
        }
    } // end method convertColorsToOstDots




    /**
     * Updates all ring angles from an AnchorPattern.
     * @param pattern The AnchorPattern containing the new angles
     */
    public void updateFromAnchorPattern(IQAnchorPattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("AnchorPattern cannot be null");
        }
        this.ring1Angle = pattern.getAngle(1);
        this.ring2Angle = pattern.getAngle(2);
        this.ring3Angle = pattern.getAngle(3);
        this.ring4Angle = pattern.getAngle(4);
    }

    public void setTemplateName(String name) {
        this.theTemplateName = name;
    } // end method setTemplateName


    public void setTemplateInstanceName(String name) {
        this.theTemplateSavedInstanceName = name;
    } // end method setTemplateInstanceName



    /**
     * This method is called by Jackson after deserialization to ensure the DrawObjRoster
     * has the correct reference to the allDrawObjs list.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private void initAfterDeserialization() {
        System.out.println("Template.initAfterDeserialization called, allDrawObjs size: " + 
            (allDrawObjs != null ? allDrawObjs.size() : "null"));
            
        // Re-link the DrawObjRoster to the populated allDrawObjs list
        if (drawObjRoster != null && allDrawObjs != null) {
            drawObjRoster.setTemplateDrawObjs(allDrawObjs);
            drawObjRoster.updateToCurrObjState();
            System.out.println("Re-linked DrawObjRoster to allDrawObjs and updated state");
        }
        
        // Resolve DrawObj references in SelectionGroups
        resolveSelectionGroupReferences();
    }
    
    /**
     * Public method to initialize the template after deserialization.
     * This should be called after loading a template from storage to ensure
     * all transient fields and references are properly restored.
     * 
     * This method is necessary because Jackson's automatic post-deserialization
     * processing via annotations like @JsonProperty(access = JsonProperty.Access.READ_ONLY)
     * may not work reliably with private methods or complex object graphs.
     * 
     * Specifically, this method ensures that:
     * 1. The DrawObjRoster's transient templateDrawObjs field is re-linked to the Template's allDrawObjs list
     * 2. The DrawObjRoster's internal state is updated with the current objects
     * 3. SelectionGroup references are properly resolved
     * 
     * Without this initialization, operations that depend on DrawObjRoster's templateDrawObjs
     * (such as palette loading) will fail because the field remains null after deserialization.
     */
    public void initializeAfterLoading() {
        System.out.println("Template.initializeAfterLoading called for template: " + theTemplateName);
        initAfterDeserialization();
    }
    
    /**
     * Resolves DrawObj references in SelectionGroups after deserialization.
     * This method should be called after a Template is loaded from storage.
     * It uses the unique IDs stored in each SelectionGroup to find the corresponding DrawObj instances.
     */
    public void resolveSelectionGroupReferences() {
        System.out.println("\n===== Template[" + theTemplateName + "]: resolveSelectionGroupReferences =====");
        
        if (namedSelectionGroups == null) {
            System.out.println("ERROR: namedSelectionGroups is null");
            System.out.println("Resolution aborted - no selection groups to process");
            System.out.println("===== End Template[" + theTemplateName + "]: resolveSelectionGroupReferences =====\n");
            return;
        }
        
        System.out.println("Processing " + namedSelectionGroups.size() + " named selection groups");
        
        if (namedSelectionGroups.isEmpty()) {
            System.out.println("No named selection groups to process");
            System.out.println("===== End Template[" + theTemplateName + "]: resolveSelectionGroupReferences =====\n");
            return;
        }
        
        // Create a map of unique IDs to DrawObj instances for quick lookup
        Map<String, DrawObj> drawObjMap = createDrawObjUuidMap();
        
        if (drawObjMap.isEmpty()) {
            System.out.println("WARNING: No DrawObj instances with valid UUIDs found in the template");
            System.out.println("This may indicate a problem with the template or the DrawObj instances");
        }
        
        System.out.println("Created UUID map with " + drawObjMap.size() + " entries");
        
        int successCount = 0;
        int failureCount = 0;
        
        // Resolve references for each SelectionGroup
        for (Map.Entry<String, SelectionGroup> entry : namedSelectionGroups.entrySet()) {
            String groupName = entry.getKey();
            SelectionGroup group = entry.getValue();
            
            if (group == null) {
                System.out.println("WARNING: Group '" + groupName + "' is null, skipping");
                failureCount++;
                continue;
            }
            
            List<String> uniqueIds = group.getDrawObjUniqueIds();
            Set<DrawObj> existingDrawObjs = group.getSelectedDrawObjsSet();
            
            System.out.println("\nProcessing group: '" + groupName + "'");
            System.out.println("  - Unique IDs: " + (uniqueIds != null ? uniqueIds.size() : "null"));
            System.out.println("  - Existing DrawObj references: " + (existingDrawObjs != null ? existingDrawObjs.size() : "null"));
            
            if (uniqueIds == null) {
                System.out.println("WARNING: Group '" + groupName + "' has null uniqueIds list, skipping");
                failureCount++;
                continue;
            }
            
            // Resolve the SelectionGroup
            SelectionGroup resolvedGroup = resolveSelectionGroup(group, drawObjMap);
            
            if (resolvedGroup == null) {
                System.out.println("ERROR: Failed to resolve group '" + groupName + "'");
                failureCount++;
                continue;
            }
            
            // Replace the original SelectionGroup with the resolved one
            namedSelectionGroups.put(groupName, resolvedGroup);
            
            int resolvedSize = resolvedGroup.getSelectedDrawObjsSet().size();
            if (resolvedSize > 0) {
                System.out.println("SUCCESS: Group '" + groupName + "' now has " + resolvedSize + " resolved DrawObj references");
                successCount++;
            } else {
                System.out.println("WARNING: Group '" + groupName + "' has 0 resolved DrawObj references after resolution");
                failureCount++;
            }
        }
        
        System.out.println("\nReference resolution summary:");
        System.out.println("  - Total groups processed: " + namedSelectionGroups.size());
        System.out.println("  - Successfully resolved: " + successCount);
        System.out.println("  - Failed to resolve: " + failureCount);
        System.out.println("===== End Template[" + theTemplateName + "]: resolveSelectionGroupReferences =====\n");
    } // end method resolveSelectionGroupReferences
    
    /**
     * Creates a map of unique IDs to DrawObj instances for quick lookup.
     * 
     * @return A map of UUID strings to DrawObj instances
     */
    private Map<String, DrawObj> createDrawObjUuidMap() {
        System.out.println("\n--- Template[" + theTemplateName + "]: createDrawObjUuidMap ---");
        
        Map<String, DrawObj> drawObjMap = new HashMap<>();
        
        if (allDrawObjs == null) {
            System.out.println("ERROR: allDrawObjs is null, returning empty map");
            System.out.println("--- End Template[" + theTemplateName + "]: createDrawObjUuidMap ---\n");
            return drawObjMap;
        }
        
        System.out.println("Processing " + allDrawObjs.size() + " DrawObj instances");
        
        int validUuidCount = 0;
        int nullUuidCount = 0;
        
        for (DrawObj drawObj : allDrawObjs) {
            if (drawObj == null) {
                System.out.println("WARNING: Null DrawObj in allDrawObjs list, skipping");
                continue;
            }
            
            UUID uniqueIdUUID = drawObj.getUniqueId();
            if (uniqueIdUUID != null) {
                String uniqueId = standardizeUuid(uniqueIdUUID.toString());
                
                // Check for duplicate UUIDs
                if (drawObjMap.containsKey(uniqueId)) {
                    System.out.println("WARNING: Duplicate UUID detected: " + uniqueId);
                    System.out.println("  - Existing: " + drawObjMap.get(uniqueId));
                    System.out.println("  - New: " + drawObj);
                    // Keep the new one (or implement a different strategy if needed)
                }
                
                drawObjMap.put(uniqueId, drawObj);
                validUuidCount++;
                
                // Only print detailed info for the first few entries to avoid log spam
                if (validUuidCount <= 5) {
                    System.out.println("Mapped DrawObj ID: " + uniqueId + " to " + drawObj);
                } else if (validUuidCount == 6) {
                    System.out.println("... (additional mappings not shown for brevity)");
                }
            } else {
                nullUuidCount++;
                if (nullUuidCount <= 5) {
                    System.out.println("WARNING - DrawObj has null uniqueId: " + drawObj);
                } else if (nullUuidCount == 6) {
                    System.out.println("... (additional null UUID warnings not shown for brevity)");
                }
            }
        }
        
        System.out.println("UUID mapping summary:");
        System.out.println("  - Total DrawObj instances: " + allDrawObjs.size());
        System.out.println("  - Valid UUIDs mapped: " + validUuidCount);
        System.out.println("  - Null UUIDs encountered: " + nullUuidCount);
        System.out.println("--- End Template[" + theTemplateName + "]: createDrawObjUuidMap ---\n");
        
        return drawObjMap;
    }
    
    /**
     * Resolves DrawObj references in a single SelectionGroup.
     * This method can be used to resolve references for any SelectionGroup, not just named ones.
     * 
     * @param group The SelectionGroup to resolve references for
     * @return The SelectionGroup with resolved DrawObj references
     */
    public SelectionGroup resolveSelectionGroup(SelectionGroup group) {
        return resolveSelectionGroup(group, createDrawObjUuidMap());
    }
    
    /**
     * Resolves DrawObj references in a single SelectionGroup using a provided UUID map.
     * This is an internal method used by both resolveSelectionGroupReferences() and resolveSelectionGroup().
     * 
     * IMPORTANT: This method resolves UUID references to DrawObj instances WITHOUT modifying
     * the DrawObj's selection state (selected flag and stroke color). This separation ensures that:
     * 1. SelectionGroups are properly resolved for data integrity
     * 2. DrawObjs don't appear selected until explicitly added to the DrawObjRoster
     * 3. Templates open with no visual selection appearance
     * 4. SelectionGroupChoiceFrame can properly load groups by explicitly adding to roster
     * 
     * @param group The SelectionGroup to resolve references for
     * @param drawObjMap A map of UUID strings to DrawObj instances
     * @return The SelectionGroup with resolved DrawObj references (without modifying DrawObj state)
     */
    private SelectionGroup resolveSelectionGroup(SelectionGroup group, Map<String, DrawObj> drawObjMap) {
        System.out.println("\n==== Template[" + theTemplateName + "]: resolveSelectionGroup ====");
        
        if (group == null) {
            System.out.println("WARNING: Group is null, returning null");
            return null;
        }
        
        // Get the unique IDs from the group
        List<String> uniqueIds = group.getDrawObjUniqueIds();
        Set<DrawObj> existingDrawObjs = group.getSelectedDrawObjsSet();
        
        System.out.println("Group has " + uniqueIds.size() + " unique IDs and " + 
                          existingDrawObjs.size() + " existing DrawObj references");
        
        // Check if the group already has resolved references AND has no unique IDs to resolve
        // This ensures we don't skip resolution if the set is empty but there are IDs to resolve
        if (!existingDrawObjs.isEmpty() && uniqueIds.isEmpty()) {
            System.out.println("Group already has resolved references and no unique IDs to resolve");
            System.out.println("Returning a copy of the existing group");
            return new SelectionGroup(existingDrawObjs);
        }
        
        // Create a new SelectionGroup with resolved DrawObj references
        // Use the helper method to add references WITHOUT modifying DrawObj state
        SelectionGroup resolvedGroup = new SelectionGroup();
        int resolvedCount = 0;
        int failedCount = 0;
        
        // Resolve each reference
        for (String uniqueId : uniqueIds) {
            String standardizedUuid = standardizeUuid(uniqueId);
            DrawObj drawObj = drawObjMap.get(standardizedUuid);
            if (drawObj != null) {
                addDrawObjToSelectionGroupWithoutStateChange(resolvedGroup, drawObj);
                resolvedCount++;
                System.out.println("Resolved reference: " + uniqueId + " to " + drawObj);
            } else {
                failedCount++;
                System.out.println("ERROR - Failed to resolve reference: " + uniqueId);
                System.out.println("  - Standardized UUID: " + standardizedUuid);
                
                // Try alternative resolution methods
                boolean resolved = false;
                
                // Method 1: Try case-insensitive comparison
                for (Map.Entry<String, DrawObj> entry : drawObjMap.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(uniqueId)) {
                        DrawObj matchedDrawObj = entry.getValue();
                        addDrawObjToSelectionGroupWithoutStateChange(resolvedGroup, matchedDrawObj);
                        System.out.println("Resolved via case-insensitive match: " + uniqueId + 
                                          " to " + matchedDrawObj + " (actual UUID: " + entry.getKey() + ")");
                        resolvedCount++;
                        failedCount--;
                        resolved = true;
                        break;
                    }
                }
                
                // Method 2: Try partial UUID match (last 8 characters)
                if (!resolved && uniqueId.length() > 8) {
                    String lastPart = uniqueId.substring(uniqueId.length() - 8);
                    for (Map.Entry<String, DrawObj> entry : drawObjMap.entrySet()) {
                        if (entry.getKey().endsWith(lastPart)) {
                            DrawObj matchedDrawObj = entry.getValue();
                            addDrawObjToSelectionGroupWithoutStateChange(resolvedGroup, matchedDrawObj);
                            System.out.println("Resolved via partial UUID match: " + uniqueId + 
                                              " to " + matchedDrawObj + " (actual UUID: " + entry.getKey() + ")");
                            resolvedCount++;
                            failedCount--;
                            resolved = true;
                            break;
                        }
                    }
                }
            }
        }
        
        System.out.println("Resolution summary:");
        System.out.println("  - Total references: " + uniqueIds.size());
        System.out.println("  - Successfully resolved: " + resolvedCount);
        System.out.println("  - Failed to resolve: " + failedCount);
        System.out.println("  - Resolved group size: " + resolvedGroup.size());
        System.out.println("==== End Template[" + theTemplateName + "]: resolveSelectionGroup ====\n");
        
        return resolvedGroup;
    }
    
    /**
     * Helper method to add a DrawObj to a SelectionGroup WITHOUT modifying the DrawObj's selection state.
     * This is used during reference resolution to preserve DrawObj state until the group is explicitly
     * loaded via SelectionGroupChoiceFrame.
     * 
     * This method directly adds the DrawObj to the SelectionGroup's internal set without:
     * - Calling setSelected(true)
     * - Changing the stroke color
     * - Firing any events
     * 
     * @param selectionGroup The SelectionGroup to add to
     * @param drawObj The DrawObj to add
     */
    private void addDrawObjToSelectionGroupWithoutStateChange(SelectionGroup selectionGroup, DrawObj drawObj) {
        if (drawObj == null) {
            return;
        }
        
        // Get the internal selectedDrawObjsSet and add directly
        // We use reflection to access the private field since there's no public method
        // that adds without calling setSelected(true)
        try {
            java.lang.reflect.Field field = SelectionGroup.class.getDeclaredField("selectedDrawObjsSet");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<DrawObj> selectedSet = (Set<DrawObj>) field.get(selectionGroup);
            
            if (selectedSet != null) {
                selectedSet.add(drawObj);
                System.out.println("Added DrawObj to SelectionGroup without state change: " + drawObj);
            }
        } catch (Exception e) {
            System.out.println("ERROR: Failed to add DrawObj to SelectionGroup without state change: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Standardizes UUID string format for consistent comparison.
     * This ensures that UUIDs are compared in a consistent format regardless of
     * how they were originally formatted (with/without hyphens, upper/lowercase).
     * 
     * @param uuid The UUID string to standardize
     * @return The standardized UUID string (lowercase with no hyphens)
     */
    private String standardizeUuid(String uuid) {
        if (uuid == null) return null;
        // Remove hyphens and convert to lowercase for consistent comparison
        return uuid.replace("-", "").toLowerCase();
    }
    
    /**
     * Saves the current SelectionGroup with the specified name.
     * If a SelectionGroup with the same name already exists, it will be replaced.
     * 
     * @param name The name to save the SelectionGroup under
     * @param selectionGroup The SelectionGroup to save
     * @return true if a previous SelectionGroup was replaced, false otherwise
     */
    public boolean saveNamedSelectionGroup(String name, SelectionGroup selectionGroup) {
        if (name == null || name.trim().isEmpty() || selectionGroup == null) {
            return false;
        }
        
        // Debug the state of the SelectionGroup before saving
        System.out.println("Template[" + theTemplateName + "]: saveNamedSelectionGroup - Before saving '" + name + 
                          "', SelectionGroup has " + selectionGroup.getSelectedDrawObjsSet().size() + 
                          " DrawObj references and " + selectionGroup.getDrawObjUniqueIds().size() + " UUIDs");
        
        // Create a new SelectionGroup with a copy of the selected DrawObjs set
        // This ensures that changes to the original SelectionGroup don't affect the saved one
        SelectionGroup copy = new SelectionGroup(selectionGroup.getSelectedDrawObjsSet());
        
        // Ensure UUIDs are populated in the copy
        List<String> uniqueIds = new ArrayList<>();
        for (DrawObj drawObj : copy.getSelectedDrawObjsSet()) {
            if (drawObj.getUniqueId() != null) {
                String uniqueId = drawObj.getUniqueId().toString();
                uniqueIds.add(uniqueId);
                System.out.println("Template[" + theTemplateName + "]: saveNamedSelectionGroup - Added UUID: " + 
                                  uniqueId + " for DrawObj: " + drawObj);
            } else {
                System.out.println("Template[" + theTemplateName + "]: saveNamedSelectionGroup - WARNING: DrawObj has null UUID: " + 
                                  drawObj);
            }
        }
        
        // Set the UUIDs in the copy
        copy.setDrawObjUniqueIds(uniqueIds);
        
        // Check if we're replacing an existing SelectionGroup
        boolean replaced = namedSelectionGroups.containsKey(name);
        
        // Save the copy
        namedSelectionGroups.put(name, copy);
        
        System.out.println("Template[" + theTemplateName + "]: saveNamedSelectionGroup - After saving '" + name + 
                          "', SelectionGroup has " + copy.getSelectedDrawObjsSet().size() + 
                          " DrawObj references and " + copy.getDrawObjUniqueIds().size() + " UUIDs");
        
        System.out.println("Template[" + theTemplateName + "]: saveNamedSelectionGroup called for '" + name + 
                          "', total groups now: " + namedSelectionGroups.size());
        return replaced;
    }
    
    /**
     * Gets a named SelectionGroup.
     * 
     * @param name The name of the SelectionGroup to retrieve
     * @return The SelectionGroup with the specified name, or null if not found
     */
    public SelectionGroup getNamedSelectionGroup(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        SelectionGroup saved = namedSelectionGroups.get(name);
        if (saved == null) {
            return null;
        }
        
        // Return a copy of the saved SelectionGroup to prevent modifications to the saved one
        System.out.println("Template[" + theTemplateName + "]: getNamedSelectionGroup called for '" + name + 
                          "', found: " + (saved != null));
        
        // Create a new SelectionGroup with the DrawObj references
        SelectionGroup copy = new SelectionGroup(saved.getSelectedDrawObjsSet());
        
        // Copy the UUID information
        copy.setDrawObjUniqueIds(new ArrayList<>(saved.getDrawObjUniqueIds()));
        
        System.out.println("Template[" + theTemplateName + "]: Copied SelectionGroup '" + name + 
                          "' with " + copy.getSelectedDrawObjsSet().size() + " DrawObj references and " +
                          copy.getDrawObjUniqueIds().size() + " UUIDs");
        
        return copy;
    }
    
    /**
     * Gets the names of all saved SelectionGroups.
     * 
     * @return A Set containing the names of all saved SelectionGroups
     */
    @JsonIgnore
    public Set<String> getNamedSelectionGroupNames() {
        Set<String> names = namedSelectionGroups.keySet();
        System.out.println("Template[" + theTemplateName + "]: getNamedSelectionGroupNames called, found " + 
                          names.size() + " groups: " + names);
        return names;
    }
    
    /**
     * Removes a named SelectionGroup.
     * 
     * @param name The name of the SelectionGroup to remove
     * @return true if the SelectionGroup was removed, false if it didn't exist
     */
    public boolean removeNamedSelectionGroup(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        boolean removed = namedSelectionGroups.remove(name) != null;
        System.out.println("Template[" + theTemplateName + "]: removeNamedSelectionGroup called for '" + name + 
                          "', removed: " + removed + ", total groups now: " + namedSelectionGroups.size());
        return removed;
    }
    
    /**
     * Exports this Template to an SVG file.
     * 
     * @param file the file to write the SVG to
     * @throws IOException if an I/O error occurs
     */
    public void exportToSVG(File file) throws IOException {
        SVGExporter.exportToSVG(this, file);
    }
    
    /**
     * Exports this Template to an SVG document using the provided Writer.
     * 
     * @param writer the Writer to write the SVG to
     * @throws IOException if an I/O error occurs
     */
    public void exportToSVG(Writer writer) throws IOException {
        SVGExporter.exportToSVG(this, writer);
    }
    
    /**
     * Sets the SVG viewBox attribute.
     * @param viewBox the SVG viewBox attribute value
     */
    public void setSvgViewBox(String viewBox) {
        this.svgViewBox = viewBox;
    }
    
    /**
     * Gets the SVG viewBox attribute.
     * @return the SVG viewBox attribute value
     */
    public String getSvgViewBox() {
        return svgViewBox;
    }
    
    /**
     * Sets the SVG width.
     * @param width the SVG width
     */
    public void setSvgWidth(double width) {
        this.svgWidth = width;
    }
    
    /**
     * Gets the SVG width.
     * @return the SVG width
     */
    public double getSvgWidth() {
        return svgWidth;
    }
    
    /**
     * Sets the SVG height.
     * @param height the SVG height
     */
    public void setSvgHeight(double height) {
        this.svgHeight = height;
    }
    
    /**
     * Gets the SVG height.
     * @return the SVG height
     */
    public double getSvgHeight() {
        return svgHeight;
    }
    
    /**
     * Sets an SVG document attribute.
     * @param name the attribute name
     * @param value the attribute value
     */
    public void setSvgDocumentAttribute(String name, String value) {
        if (svgDocumentAttributes == null) {
            svgDocumentAttributes = new HashMap<>();
        }
        svgDocumentAttributes.put(name, value);
    }
    
    /**
     * Gets an SVG document attribute.
     * @param name the attribute name
     * @return the attribute value, or null if not found
     */
    public String getSvgDocumentAttribute(String name) {
        return svgDocumentAttributes != null ? svgDocumentAttributes.get(name) : null;
    }
    
    /**
     * Gets all SVG document attributes.
     * @return an unmodifiable map of all SVG document attributes
     */
    public Map<String, String> getSvgDocumentAttributes() {
        return svgDocumentAttributes != null ? Collections.unmodifiableMap(svgDocumentAttributes) : Collections.emptyMap();
    }
    
    /**
     * Sets the original SVG file path.
     * @param filePath the original SVG file path
     */
    public void setOriginalSvgFilePath(String filePath) {
        this.originalSvgFilePath = filePath;
    }
    
    /**
     * Gets the original SVG file path.
     * @return the original SVG file path
     */
    public String getOriginalSvgFilePath() {
        return originalSvgFilePath;
    }

    /**
     * Builds a palette directly from the Template's DrawObjs using getAllDrawObjs().
     * This method creates a TemplatePalette by iterating through all DrawObjs in the template
     * and collecting their colors.
     * 
     * @return A TemplatePalette containing colors from all DrawObjs in the template
     */
    public PaletteWName buildPaletteFromTemplateDrawObjs() {
        List<DrawObj> drawObjs = getAllDrawObjs();
        System.out.println("DEBUG: buildPaletteFromTemplateDrawObjs - Number of DrawObjs: " + drawObjs.size());
        
        if (drawObjs.isEmpty()) {
            System.out.println("DEBUG: buildPaletteFromTemplateDrawObjs - No DrawObjs found, returning empty palette");
            return new PaletteWName(new ColorPtOstDot[0], this.theTemplateName);
        }
        
        // Create array to hold colors
        ColorPtOstDot[] colorPtOstdotArray = new ColorPtOstDot[drawObjs.size()];
        System.out.println("DEBUG: buildPaletteFromTemplateDrawObjs - Created color array of size: " + drawObjs.size());
        
        // Iterate through all DrawObjs to get their colors
        for (int i = 0; i < drawObjs.size(); i++) {
            DrawObj drawObj = drawObjs.get(i);
            if (drawObj != null) {
                ColorPtOstDot color = drawObj.getCurrentColor();
                colorPtOstdotArray[i] = color;
                System.out.println("DEBUG: buildPaletteFromTemplateDrawObjs - Color at index " + i + ": " + 
                    (color != null ? "not null" : "null"));
            } else {
                System.out.println("DEBUG: buildPaletteFromTemplateDrawObjs - DrawObj at index " + i + " is null");
                colorPtOstdotArray[i] = null;
            }
        }
        
        // Create and return the palette
        PaletteWName palette = new PaletteWName(colorPtOstdotArray, this.theTemplateName);
        System.out.println("DEBUG: buildPaletteFromTemplateDrawObjs - Created palette with " + colorPtOstdotArray.length + " colors");
        return palette;
    }

} // end class Template
