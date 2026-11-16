package com.csurf.templatecore.events;


// added 060125 per Cascade SWE-1 not Claude (copy/paste)
@FunctionalInterface
public interface IColorSelectionListener {
    void colorSelected(ColorSelectionEvent event);
}