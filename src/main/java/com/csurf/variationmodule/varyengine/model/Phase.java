package com.csurf.variationmodule.varyengine.model;


/**
 * The execution phases of an ArtisticGoal, per DesignPlan §3.2.
 * ID bands de-facto declare phases (9xx = AG, 5xx = HUE, 2xx = COLOR, 0xx = OVERRIDE);
 * the phase enum remains the user's mental model and the UI's organizing axis.
 */
public enum Phase {
    PRE,
    AG,
    HUE,
    COLOR,
    OVERRIDE,
    POST
} // end enum Phase
