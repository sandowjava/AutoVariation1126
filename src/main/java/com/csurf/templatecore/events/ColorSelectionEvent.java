package com.csurf.templatecore.events;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

import java.util.EventObject;

// File: src/main/java/com/csurf/smartpictemplate/events/ColorSelectionEvent.java
// added 060125 per Cascade SWE-1 not Claude (copy/paste)
public class ColorSelectionEvent extends EventObject {
    private final ColorPtOstDot selectedColor;

    public ColorSelectionEvent(Object source, ColorPtOstDot selectedColor) {
        super(source);
        this.selectedColor = selectedColor;
    }

    public ColorPtOstDot getSelectedColor() {
        return selectedColor;
    }


} // end class ColorSelectionEvent



