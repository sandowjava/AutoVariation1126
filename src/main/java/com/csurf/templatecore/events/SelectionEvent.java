package com.csurf.templatecore.events;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.SelectionGroup;

import java.util.EventObject;
import java.util.Set;

/**
 * Event that represents changes in the selection state of DrawObj instances.
 * This event is fired when objects are selected, deselected, or when the selection
 * is otherwise modified (e.g., through shift-click, ctrl-click, or programmatic selection).
 * 
 * This class is separate from PositionalPaletteEvent to maintain clear separation of concerns
 * between color/palette operations and selection operations.
 */
public class SelectionEvent extends EventObject {
    
    /**
     * Interface for listeners that want to be notified of selection changes.
     */
    public interface SelectionListener {
        /**
         * Called when a selection event occurs.
         * 
         * @param event The selection event
         */
        void handleSelectionEvent(SelectionEvent event);
    }
    
    /**
     * Enum representing the type of selection change that occurred.
     */
    public enum SelectionChangeType {
        /** A new selection was made, replacing any previous selection */
        SELECTION_REPLACE,
        
        /** Objects were added to the existing selection (e.g., via shift-click) */
        SELECTION_ADD,
        
        /** Objects were removed from the existing selection */
        SELECTION_REMOVE,
        
        /** Selection was toggled (e.g., via ctrl-click) */
        SELECTION_TOGGLE,
        
        /** All objects were deselected */
        SELECTION_CLEAR
    }
    
    private final SelectionChangeType changeType;
    private final SelectionGroup selectionGroup;
    private final Set<DrawObj> affectedObjects;
    
    /**
     * Creates a new SelectionEvent.
     * 
     * @param source The object that originated the event
     * @param changeType The type of selection change that occurred
     * @param selectionGroup The current selection group after the change
     * @param affectedObjects The specific objects that were affected by this change
     * @throws IllegalArgumentException if any parameter is null
     */
    public SelectionEvent(Object source, SelectionChangeType changeType, 
                          SelectionGroup selectionGroup, Set<DrawObj> affectedObjects) {
        super(source);
        
        // Validate parameters after super() call
        if (source == null) {
            throw new IllegalArgumentException("null source");
        }
        if (changeType == null) {
            throw new IllegalArgumentException("null changeType");
        }
        if (selectionGroup == null) {
            throw new IllegalArgumentException("null selectionGroup");
        }
        if (affectedObjects == null) {
            throw new IllegalArgumentException("null affectedObjects");
        }
        
        this.changeType = changeType;
        this.selectionGroup = selectionGroup;
        this.affectedObjects = affectedObjects;
    }
    
    /**
     * Gets the type of selection change that occurred.
     * 
     * @return The selection change type
     */
    public SelectionChangeType getChangeType() {
        return changeType;
    }
    
    /**
     * Gets the current selection group after the change.
     * 
     * @return The selection group
     */
    public SelectionGroup getSelectionGroup() {
        return selectionGroup;
    }
    
    /**
     * Gets the specific objects that were affected by this selection change.
     * For example, in a SELECTION_ADD event, this would contain the objects that were added.
     * 
     * @return The set of affected DrawObj instances
     */
    public Set<DrawObj> getAffectedObjects() {
        return affectedObjects;
    }
}
