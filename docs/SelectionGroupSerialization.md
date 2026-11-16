# SelectionGroup Serialization Documentation

This document describes the approach used for serializing and deserializing SelectionGroups in the Template class.

## Overview

SelectionGroups are collections of DrawObj instances that can be named and saved with a Template. The main challenge in serializing SelectionGroups is maintaining the references to DrawObj instances across serialization/deserialization cycles.

## Implementation Details

### 1. Data Structure

SelectionGroups are stored in the Template class using a Map:

```java
@JsonProperty
private final Map<String, SelectionGroup> namedSelectionGroups = new HashMap<>();
```

### 2. Reference Handling

DrawObj instances in SelectionGroups are referenced by their unique IDs (UUID) during serialization:

- Each DrawObj has a uniqueId field of type UUID
- During serialization, only the unique IDs are stored in the SelectionGroup
- During deserialization, these IDs are used to find the corresponding DrawObj instances

### 3. Custom Serializers/Deserializers

Custom Jackson serializers and deserializers handle the conversion between DrawObj references and their unique IDs:

- **SelectionGroupSerializer**: Converts DrawObj references to their unique IDs
- **SelectionGroupDeserializer**: Creates SelectionGroup instances with lists of unique IDs

### 4. Post-Deserialization Reference Resolution

After a Template is loaded from storage, the `resolveSelectionGroupReferences` method is called to resolve the references:

```java
public void resolveSelectionGroupReferences() {
    // Create a map of unique IDs to DrawObj instances
    Map<String, DrawObj> drawObjMap = createDrawObjUuidMap();
    
    // Resolve references for each SelectionGroup
    for (Map.Entry<String, SelectionGroup> entry : namedSelectionGroups.entrySet()) {
        String groupName = entry.getKey();
        SelectionGroup group = entry.getValue();
        
        // Resolve the SelectionGroup
        SelectionGroup resolvedGroup = resolveSelectionGroup(group, drawObjMap);
        
        // Replace the original SelectionGroup with the resolved one
        namedSelectionGroups.put(groupName, resolvedGroup);
    }
}
```

### 5. Centralized Reference Resolution

The Template class provides a centralized mechanism for resolving references in SelectionGroups:

```java
/**
 * Resolves DrawObj references in a single SelectionGroup.
 * This method can be used to resolve references for any SelectionGroup, not just named ones.
 */
public SelectionGroup resolveSelectionGroup(SelectionGroup group) {
    return resolveSelectionGroup(group, createDrawObjUuidMap());
}

/**
 * Resolves DrawObj references in a single SelectionGroup using a provided UUID map.
 * This is an internal method used by both resolveSelectionGroupReferences() and resolveSelectionGroup().
 */
private SelectionGroup resolveSelectionGroup(SelectionGroup group, Map<String, DrawObj> drawObjMap) {
    if (group == null) {
        return null;
    }
    
    // If the group already has resolved references, return a copy
    if (!group.getSelectedDrawObjsSet().isEmpty()) {
        return new SelectionGroup(group.getSelectedDrawObjsSet());
    }
    
    // Create a new SelectionGroup with resolved DrawObj references
    SelectionGroup resolvedGroup = new SelectionGroup();
    
    // Resolve each reference
    for (String uniqueId : group.getDrawObjUniqueIds()) {
        DrawObj drawObj = drawObjMap.get(uniqueId);
        if (drawObj != null) {
            resolvedGroup.addToSelectionGroup(drawObj);
        }
    }
    
    return resolvedGroup;
}
```

### 6. UI Refresh

The SelectionGroupChoiceFrame UI is refreshed when a template is loaded:

- The TemplateWorkFrame closes and reopens the SelectionGroupChoiceFrame if it was previously open
- The `updateWithCurrentTemplate` method in SelectionGroupChoiceFrame refreshes the UI list model with named selection groups from the current template

### 7. Loading Selection Groups

The `loadSelectedGroup` method in SelectionGroupChoiceFrame uses the centralized reference resolution mechanism:

```java
private void loadSelectedGroup() {
    String name = savedGroupsList.getSelectedValue();
    if (name == null) {
        return;
    }
    
    // Get the selection group
    SelectionGroup group = template.getNamedSelectionGroup(name);
    if (group == null) {
        showStatus("Error: Could not load selection group", true);
        return;
    }
    
    // Use Template's centralized reference resolution mechanism
    SelectionGroup resolvedGroup = template.resolveSelectionGroup(group);
    Set<DrawObj> selectedObjs = resolvedGroup.getSelectedDrawObjsSet();
    
    // Clear the current selection
    template.getDrawObjRoster().clearSelection();
    
    // Add all objects from the selection group to the current selection
    for (DrawObj obj : selectedObjs) {
        template.getDrawObjRoster().addToSelection(obj);
    }
    
    // Fire a selection event to update the UI
    template.getDrawObjRoster().fireSelectionEventForGroup(
        SelectionEvent.SelectionChangeType.SELECTION_REPLACE,
        selectedObjs
    );
    
    // Force a UI update in the parent frame
    parentFrame.repaint();
}
```

## Serialization Invariants

1. **DrawObj References**: During serialization, only the UUIDs of DrawObj instances are serialized, not the actual DrawObj instances.
2. **Reference Resolution**: After deserialization, references must be resolved by finding the corresponding DrawObj instances in the Template.
3. **Centralized Resolution**: All reference resolution is handled by the Template class, not by individual components.
4. **UUID-Based Matching**: DrawObj instances are matched by their UUID strings, not by object identity or equality.

## Key Challenges Addressed

1. **Reference Integrity**: Maintaining references between DrawObj instances across serialization/deserialization
2. **Object Identity vs. Equality**: Ensuring that references point to the correct objects after deserialization
3. **Deserialization Order Dependencies**: Handling the order in which objects are deserialized
4. **Cyclic References**: Avoiding infinite recursion during serialization
5. **UI Synchronization**: Ensuring the UI reflects the current state of named selection groups
6. **UUID Comparison**: Using string-based UUID comparison for reliable object matching

## Future Improvements

1. **Versioning Support**: Add versioning for backward compatibility with older template formats
2. **Performance Optimization**: Improve the efficiency of reference resolution for large templates
3. **Error Handling**: Add more robust error handling for cases where references cannot be resolved
4. **Test Coverage**: Create comprehensive test cases to verify SelectionGroup persistence
5. **Debugging Support**: Add configurable logging for troubleshooting serialization issues
