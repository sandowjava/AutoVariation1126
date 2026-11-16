package com.csurf.templatecore.events;

/**
 * Interface for listeners that want to be notified of selection events.
 * These events represent changes in the selection state of DrawObj instances,
 * such as objects being selected, deselected, or the selection being modified.
 */
@FunctionalInterface
public interface ISelectionEventListener {
    
    /**
     * Called when a selection event occurs.
     * 
     * @param event The event containing information about the selection change
     */
    void handleSelectionEvent(SelectionEvent event);
}
