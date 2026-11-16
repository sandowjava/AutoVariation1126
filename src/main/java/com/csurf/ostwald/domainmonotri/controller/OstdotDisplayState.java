package com.csurf.ostwald.domainmonotri.controller;

/**
 * Represents the possible display states of an OstdotPanel.
 * These states determine how the panel should be rendered based on
 * occupation, selection, and current hue context.
 */
public enum OstdotDisplayState {
    /**
     * Position is occupied, color is in frame's current hue, not selected, normal icon
     */
    OCCUPIED_UNSELECTED_IN_CURR_HUE,

    /**
     * Position is occupied in a different hue, not selected, normal icon for occupied
     */
    OCCUPIED_UNSELECTED_OTHER_HUE,

    /**
     * Position has color from different hue, not selected, simplified icon for occupied
     */
    OCCUPIED_UNSELECTED_OTHER_HUE_SIMPLIFIED,

    /**
     * Position has color in frame's current hue only, selected, normal icon for selected
     */
    OCCUPIED_SELECTED_IN_CURR_HUE,

    /**
     * Position has color in frame's current hue and alo inat least one other hue, selected
     */
    OCCUPIED_SELECTED_IN_CURR_HUE_PLUS,

    /**
     * Position has color from different hue, selected
     */
    OCCUPIED_SELECTED_OTHER_HUE,

    /**
     * Position has no color assigned
     */
    UNOCCUPIED


} // end enum
