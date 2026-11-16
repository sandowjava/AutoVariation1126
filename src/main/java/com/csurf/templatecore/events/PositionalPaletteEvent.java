package com.csurf.templatecore.events;

import com.csurf.templatecore.model.DrawObjRoster;

import java.util.EventObject;

/**
 * High-level event that represents changes in the positional state of the color palette,
 * regardless of the user action that triggered the change. It unifies multiple triggers
 * such as color changes, palette loading, and selection changes.
 *
 * High-level event that encompasses selected-positional triggers and also palette-positional triggers.
 * PositionalPaletteEvent can be created at any time as the result of end-user actions.
 * Important USE CASES: color changes (including color-drag and hue-rotation) and palette loading scenarios.
 * Note that hue-rotation scenarios result in a change of palette but no change in positions.
 * All PositionalPaletteEvent will drive changes to "display state" for MonotriWorkFrame rendering.
 *
 * Future development will provide additional sources of PositionalPaletteEvents such as loading saved Palettes.
 * Future development will also include automated creation of Palettes that can be sources of PositionalPaletteEvents.
 */
public class PositionalPaletteEvent extends EventObject {
    
    /**
     * Enum representing the type of event that caused the positional palette change.
     */
    public enum EventType {
        COLOR_CHANGE,  // Replaces both COLOR_DRAG and HUE_ROTATION
        PALETTE_LOAD, // not implemented yet - planned for use in future development
        SELECTION_CHANGE
    }
    
    private final EventType eventType;
    private final DrawObjRoster drawObjRoster;
    
    /**
     * Creates a new PositionalPaletteEvent.
     * 
     * @param source The object that originated the event
     * @param eventType The type of event that caused the positional palette change
     * @param drawObjRoster The DrawObjRoster containing the current state
     * @throws IllegalArgumentException if source, eventType, or drawObjRoster is null
     */
    public PositionalPaletteEvent(Object source, EventType eventType, DrawObjRoster drawObjRoster) {
        super(source);
        
        // Validate parameters after super() call
        if (source == null) {
            throw new IllegalArgumentException("null source");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("null eventType");
        }
        if (drawObjRoster == null) {
            throw new IllegalArgumentException("null drawObjRoster");
        }
        
        this.eventType = eventType;
        this.drawObjRoster = drawObjRoster;
    }
    
    /**
     * Gets the type of event that caused the positional palette change.
     * 
     * @return The event type
     */
    public EventType getEventType() {
        return eventType;
    }
    
    /**
     * Gets the DrawObjRoster containing the current state.
     * 
     * @return The DrawObjRoster
     */
    public DrawObjRoster getDrawObjRoster() {
        return drawObjRoster;
    }
}
