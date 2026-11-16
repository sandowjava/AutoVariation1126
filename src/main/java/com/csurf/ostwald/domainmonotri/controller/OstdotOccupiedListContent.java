package com.csurf.ostwald.domainmonotri.controller;

import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result object carrying the content to display for a hovered OstDot position.
 * Holds the position model (which provides fillOrderNum and W/B series values),
 * a composed headline line, and the names of the DrawObjs occupying that
 * position across all hues (selected entries prefixed with "**").
 *
 * Display-agnostic: the view decides how to render the headline and the name
 * list. An empty name list means the position is unoccupied; the headline is
 * still carried so unoccupied positions show position info.
 */
public class OstdotOccupiedListContent {
    private final OstHueColumnStep ostHueColumnStep;
    private final String headline;
    private final List<String> drawObjNames;

    /**
     * Creates the content for one hovered OstDot position.
     * @param ostHueColumnStep the hovered position, or null if none
     * @param headline the composed headline line (may be null)
     * @param drawObjNames names of occupying DrawObjs, may be null (treated as empty)
     */
    public OstdotOccupiedListContent(OstHueColumnStep ostHueColumnStep,
                                   String headline,
                                   List<String> drawObjNames) {
        this.ostHueColumnStep = ostHueColumnStep;
        this.headline = headline;
        this.drawObjNames = drawObjNames != null
                ? Collections.unmodifiableList(new ArrayList<>(drawObjNames))
                : Collections.emptyList();
    } // end constructor


    /**
     * @return the hovered position model, or null if the call site had none
     */
    public OstHueColumnStep getOstHueColumnStep() {
        return ostHueColumnStep;
    } // end method getOstHueColumnStep


    /**
     * @return the composed headline line (e.g. "fillOrder 12 COLUMN 4 step 3 — W 5 B 8"),
     *         or null if the call site had no position
     */
    public String getHeadline() {
        return headline;
    } // end method getHeadline


    /**
     * @return unmodifiable list of DrawObj names occupying the position (empty if unoccupied)
     */
    public List<String> getDrawObjNames() {
        return drawObjNames;
    } // end method getDrawObjNames


    /**
     * @return true if at least one DrawObj occupies the position
     */
    public boolean isOccupied() {
        return !drawObjNames.isEmpty();
    } // end method isOccupied

} // end class OstdotOccupiedListContent
