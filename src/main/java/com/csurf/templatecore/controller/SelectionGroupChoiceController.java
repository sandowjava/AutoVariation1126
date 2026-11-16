package com.csurf.templatecore.controller;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.model.SelectionGroup;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.service.SelectionOperationService;

import java.util.HashSet;
import java.util.Set;

/**
 * Controller for SelectionGroupChoiceFrame.
 * 
 * Handles all selection group operations including:
 * - Loading (replacing) selection with a saved group
 * - Adding members of a saved group to current selection
 * - Subtracting members of a saved group from current selection
 * 
 * This controller properly handles:
 * - Reference resolution for saved SelectionGroups
 * - Filtering of duplicate/non-selected DrawObjs
 * - User feedback for various scenarios
 * - Proper event firing through SelectionOperationService
 */
public class SelectionGroupChoiceController {
    private final Template template;
    private final SelectionOperationService selectionService;
    private SelectionGroupChoiceFrameCallback callback;
    
    /**
     * Interface for callbacks to SelectionGroupChoiceFrame.
     * Used to update UI and show status messages.
     */
    public interface SelectionGroupChoiceFrameCallback {
        void showStatus(String message, boolean isError);
    }
    
    /**
     * Creates a new SelectionGroupChoiceController.
     * 
     * @param template The Template containing named SelectionGroups
     */
    public SelectionGroupChoiceController(Template template) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }
        this.template = template;
        this.selectionService = new SelectionOperationService(template.getDrawObjRoster());
    }
    
    /**
     * Sets the callback for UI updates.
     * 
     * @param callback The callback to use for status messages
     */
    public void setCallback(SelectionGroupChoiceFrameCallback callback) {
        this.callback = callback;
    }


    // TODO 041926 INVESTIGATE: method "loadSelectedGroup" in SelectionGroupChoiceFrame is used instead of this method
    /**
     * Replaces the current selection with the members of a saved SelectionGroup.
     * 
     * This operation:
     * 1. Retrieves the named SelectionGroup from the template
     * 2. Resolves DrawObj references from UUIDs
     * 3. Clears the current selection
     * 4. Adds all members of the saved group
     * 5. Fires appropriate events
     * 
     * @param groupName The name of the saved SelectionGroup
     * @return true if successful, false otherwise
     */
    public boolean replaceSelectionWithGroup(String groupName) {
        System.out.println("\n==== SelectionGroupChoiceController: replaceSelectionWithGroup ====");
        System.out.println("Group name: " + groupName);
        
        // Validate input
        if (groupName == null || groupName.trim().isEmpty()) {
            showStatus("No group selected", true);
            return false;
        }
        
        // Get saved group
        SelectionGroup group = template.getNamedSelectionGroup(groupName);
        if (group == null) {
            showStatus("Error: Could not find group '" + groupName + "'", true);
            System.out.println("ERROR: Group '" + groupName + "' not found in template");
            return false;
        }
        
        System.out.println("Found group: " + groupName);
        
        // Resolve references
        SelectionGroup resolved = template.resolveSelectionGroup(group);
        if (resolved == null) {
            showStatus("Error: Could not resolve group references", true);
            System.out.println("ERROR: Failed to resolve group references");
            return false;
        }
        
        Set<DrawObj> toSelect = resolved.getSelectedDrawObjsSet();
        if (toSelect.isEmpty()) {
            showStatus("Group is empty or all references could not be resolved", false);
            System.out.println("WARNING: Resolved group is empty");
            return false;
        }
        
        System.out.println("Resolved group has " + toSelect.size() + " DrawObjs");
        
        // Perform replacement
        int count = selectionService.replaceSelection(toSelect);
        
        System.out.println("Replaced selection with " + count + " DrawObjs");
        showStatus("Loaded selection group: " + groupName + " (" + count + " objects)", false);
        
        System.out.println("==== End SelectionGroupChoiceController: replaceSelectionWithGroup ====\n");
        return true;
    }
    
    /**
     * Adds members of a saved SelectionGroup to the current selection.
     * 
     * This operation:
     * 1. Retrieves the named SelectionGroup from the template
     * 2. Resolves DrawObj references from UUIDs
     * 3. Filters out DrawObjs already in the current selection
     * 4. Adds only the new DrawObjs
     * 5. Fires appropriate events
     * 
     * @param groupName The name of the saved SelectionGroup
     * @return true if successful, false otherwise
     */
    public boolean addSelectedGroupToSelection(String groupName) {
        System.out.println("\n==== SelectionGroupChoiceController: addSelectedGroupToSelection ====");
        System.out.println("Group name: " + groupName);
        
        // Validate input
        if (groupName == null || groupName.trim().isEmpty()) {
            showStatus("No group selected", true);
            return false;
        }
        
        // Get saved group
        SelectionGroup group = template.getNamedSelectionGroup(groupName);
        if (group == null) {
            showStatus("Error: Could not find group '" + groupName + "'", true);
            System.out.println("ERROR: Group '" + groupName + "' not found in template");
            return false;
        }
        
        System.out.println("Found group: " + groupName);
        
        // Resolve references
        SelectionGroup resolved = template.resolveSelectionGroup(group);
        if (resolved == null) {
            showStatus("Error: Could not resolve group references", true);
            System.out.println("ERROR: Failed to resolve group references");
            return false;
        }
        
        Set<DrawObj> toAdd = resolved.getSelectedDrawObjsSet();
        if (toAdd.isEmpty()) {
            showStatus("Group is empty or all references could not be resolved", false);
            System.out.println("WARNING: Resolved group is empty");
            return false;
        }
        
        System.out.println("Resolved group has " + toAdd.size() + " DrawObjs");
        
        // Check for duplicates
        DrawObjRoster roster = template.getDrawObjRoster();
        Set<DrawObj> current = roster.getSelectionGroup().getSelectedDrawObjsSet();
        Set<DrawObj> newMembers = new HashSet<>(toAdd);
        newMembers.removeAll(current);
        
        System.out.println("Current selection has " + current.size() + " DrawObjs");
        System.out.println("New members to add: " + newMembers.size());
        
        if (newMembers.isEmpty()) {
            showStatus("All DrawObjs in this group are already selected", false);
            System.out.println("INFO: All members already selected");
            return true;
        }
        
        // Perform addition
        int count = selectionService.addMultipleToSelection(toAdd);
        
        System.out.println("Added " + count + " new DrawObjs to selection");
        showStatus("Added " + count + " new DrawObjs to selection", false);
        
        System.out.println("==== End SelectionGroupChoiceController: addSelectedGroupToSelection ====\n");
        return true;
    }
    
    /**
     * Removes members of a saved SelectionGroup from the current selection.
     * 
     * This operation:
     * 1. Retrieves the named SelectionGroup from the template
     * 2. Resolves DrawObj references from UUIDs
     * 3. Filters to only DrawObjs currently in the selection
     * 4. Removes only the selected DrawObjs
     * 5. Warns if removing a large portion of the selection
     * 6. Fires appropriate events
     * 
     * @param groupName The name of the saved SelectionGroup
     * @return true if successful, false otherwise
     */
    public boolean subtractSelectedGroupFromSelection(String groupName) {
        System.out.println("\n==== SelectionGroupChoiceController: subtractSelectedGroupFromSelection ====");
        System.out.println("Group name: " + groupName);
        
        // Validate input
        if (groupName == null || groupName.trim().isEmpty()) {
            showStatus("No group selected", true);
            return false;
        }
        
        // Get saved group
        SelectionGroup group = template.getNamedSelectionGroup(groupName);
        if (group == null) {
            showStatus("Error: Could not find group '" + groupName + "'", true);
            System.out.println("ERROR: Group '" + groupName + "' not found in template");
            return false;
        }
        
        System.out.println("Found group: " + groupName);
        
        // Resolve references
        SelectionGroup resolved = template.resolveSelectionGroup(group);
        if (resolved == null) {
            showStatus("Error: Could not resolve group references", true);
            System.out.println("ERROR: Failed to resolve group references");
            return false;
        }
        
        Set<DrawObj> toRemove = resolved.getSelectedDrawObjsSet();
        if (toRemove.isEmpty()) {
            showStatus("Group is empty or all references could not be resolved", false);
            System.out.println("WARNING: Resolved group is empty");
            return false;
        }
        
        System.out.println("Resolved group has " + toRemove.size() + " DrawObjs");
        
        // Check what can actually be removed
        DrawObjRoster roster = template.getDrawObjRoster();
        Set<DrawObj> current = roster.getSelectionGroup().getSelectedDrawObjsSet();
        Set<DrawObj> canRemove = new HashSet<>(toRemove);
        canRemove.retainAll(current);
        
        System.out.println("Current selection has " + current.size() + " DrawObjs");
        System.out.println("Can remove: " + canRemove.size());
        
        if (canRemove.isEmpty()) {
            showStatus("None of these DrawObjs are currently selected", false);
            System.out.println("INFO: No members currently selected");
            return true;
        }
        
        // Warn if removing most/all of current selection
        if (canRemove.size() >= current.size() * 0.8 && current.size() > 1) {
            showStatus("Warning: This will remove " + canRemove.size() + " of " + current.size() + 
                      " selected DrawObjs", false);
            System.out.println("WARNING: Removing " + canRemove.size() + " of " + current.size() + 
                             " selected DrawObjs");
        }
        
        // Perform removal
        int count = selectionService.removeMultipleFromSelection(toRemove);
        
        System.out.println("Removed " + count + " DrawObjs from selection");
        
        // Build user message
        String message = "Removed " + count + " DrawObjs from selection";
        Set<DrawObj> notFound = new HashSet<>(toRemove);
        notFound.removeAll(current);
        if (!notFound.isEmpty()) {
            message += " (" + notFound.size() + " were not selected)";
        }
        showStatus(message, false);
        
        System.out.println("==== End SelectionGroupChoiceController: subtractSelectedGroupFromSelection ====\n");
        return true;
    }
    
    /**
     * Shows a status message through the callback.
     * 
     * @param message The message to show
     * @param isError Whether this is an error message
     */
    private void showStatus(String message, boolean isError) {
        if (callback != null) {
            callback.showStatus(message, isError);
        }
    }
}
