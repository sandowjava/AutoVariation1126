package com.csurf.templatecore.events;

/**
 * Interface for listeners that want to be notified of positional palette events.
 * These events represent changes in the positional state of the color palette,
 * regardless of the user action that triggered the change.
 */
@FunctionalInterface
public interface IPositionalPaletteEventListener {
    
    /**
     * Called when a positional palette event occurs.
     * 
     * @param event The event containing information about the positional palette change
     */
    void handlePositionalPaletteEvent(PositionalPaletteEvent event);
}
