package com.csurf.ostwald.domainmonotri.controller;

import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.csurf.ostwald.domainmonotri.model.OstPosition;
import com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.view.TemplateWorkFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Controller responsible for composing the occupancy content shown in
 * MonotriDrawPanel's fixed toolTipPanel when the user hovers over an
 * OstDot position.
 *
 * Holds the MonotriWorkFrame as the path to the DrawObjRoster rather than
 * the roster itself, because MonotriWorkFrame.setTemplateWorkFrame() can
 * swap the roster after construction. The roster is fetched lazily per call.
 *
 * The content carries a headline composed from the OstHueColumnStep
 * ("fill# <N> COLM <col> step <step> — W: <w>, B: <b>") for both occupied
 * and unoccupied positions.
 *
 * For an occupied position, each occupying DrawObj contributes one string in
 * the form "h<N>-<name>" where N is the user-facing hueNum (HueNumGUI is the
 * only hue representation used here — no raw int hue variables). Entries are
 * emitted in four groups: selected+current-hue and selected+other-hue first
 * ("**"-prefixed, roster order — selection is read live via
 * DrawObjRoster.isSelected), then unselected+current-hue (alphabetical by
 * name), then unselected+other-hue (roster order). An unoccupied position
 * yields the headline with an empty name list.
 */
public class OstdotOccupiedListController {
    private static final Logger logger = Logger.getLogger(OstdotOccupiedListController.class.getName());

    private final MonotriWorkFrame monotriWorkFrame;

    /**
     * @param monotriWorkFrame the owning frame; provides the path to DrawObjRoster
     *                         and the current-hue context
     */
    public OstdotOccupiedListController(MonotriWorkFrame monotriWorkFrame) {
        this.monotriWorkFrame = monotriWorkFrame;
    } // end constructor


    /**
     * Composes the display content for a hovered position.
     * Always carries a headline composed from the OstHueColumnStep
     * (fillOrderNum, column, step, W/B series). When the position is occupied,
     * also carries one entry per occupying DrawObj in four groups: selected
     * ("**"-prefixed) current-hue entries in roster order, selected other-hue
     * entries in roster order, unselected current-hue entries sorted
     * alphabetically by name, then unselected other-hue entries in roster
     * order.
     *
     * @param ostHCS the hovered position, or null
     * @return the content to display; never null (null position yields empty content)
     */
    public OstdotOccupiedListContent getOccupiedListContent(OstHueColumnStep ostHCS) {
        if (ostHCS == null) {
            logger.fine("getOccupiedListContent called with null OstHueColumnStep");
            return new OstdotOccupiedListContent(null, null, Collections.emptyList());
        }

        String headline = composeHeadline(ostHCS);

        DrawObjRoster roster = getDrawObjRoster();
        OstPosition position = ostHCS.getOstPosition();
        if (roster == null || !roster.isOccupied(position)) {
            // Unoccupied (or no template loaded): headline only.
            return new OstdotOccupiedListContent(ostHCS, headline, Collections.emptyList());
        }

        // Current hue comes from the work frame, expressed as HueNumGUI.
        HueNumGUI currentHueNumGUI = HueNumGUI.getHueNumGUIInstance(
                monotriWorkFrame.getCurrHueNumCode() + 1);

        List<String> selectedCurrentHue = new ArrayList<>();
        List<String> selectedOtherHue = new ArrayList<>();
        List<String> unselectedCurrentHue = new ArrayList<>();
        List<String> unselectedOtherHue = new ArrayList<>();
        for (DrawObj drawObj : roster.getDrawObjsAtPosition(position)) {
            HueNumGUI drawObjHue = HueNumGUI.getHueNumGUIInstance(drawObj.getUserFacingHueNum());
            boolean selected = roster.isSelected(drawObj);
            String entry = (selected ? "**" : "")
                    + "h" + drawObjHue.getUserFacingHueNum() + "-" + drawObj.getName();
            if (selected) {
                (drawObjHue == currentHueNumGUI ? selectedCurrentHue : selectedOtherHue).add(entry);
            } else {
                (drawObjHue == currentHueNumGUI ? unselectedCurrentHue : unselectedOtherHue).add(entry);
            }
        }

        // Within the unselected current-hue group all entries share the same
        // "h<N>-" prefix, so sorting the strings sorts alphabetically by name.
        unselectedCurrentHue.sort(String.CASE_INSENSITIVE_ORDER);

        List<String> names = new ArrayList<>(selectedCurrentHue.size() + selectedOtherHue.size()
                + unselectedCurrentHue.size() + unselectedOtherHue.size());
        names.addAll(selectedCurrentHue);
        names.addAll(selectedOtherHue);
        names.addAll(unselectedCurrentHue);
        names.addAll(unselectedOtherHue);
        return new OstdotOccupiedListContent(ostHCS, headline, names);
    } // end method getOccupiedListContent


    /**
     * Composes the tooltip headline for a position, e.g.
     * "fill# 12 COLM 4 step 3 — W: 5, B: 8". Composed here (not in the
     * view) so the headline format can evolve independently of the name list.
     *
     * @param ostHCS the hovered position (non-null)
     * @return the headline string
     */
    private String composeHeadline(OstHueColumnStep ostHCS) {
        return "fill# " + ostHCS.getFillOrderNum()
                + " COLM " + ostHCS.getColumn()
                + " step " + ostHCS.getStep()
                + " — W: " + ostHCS.getSeriesW()
                + ", B: " + ostHCS.getSeriesB();
    } // end method composeHeadline


    /**
     * Fetches the current DrawObjRoster lazily, once per
     * getOccupiedListContent() invocation, so a swapped TemplateWorkFrame can
     * never leave the controller holding a stale roster.
     * @return the roster, or null if no template work frame is attached
     */
    private DrawObjRoster getDrawObjRoster() {
        TemplateWorkFrame templateWorkFrame = monotriWorkFrame.getTemplateWorkFrame();
        return templateWorkFrame != null ? templateWorkFrame.getDrawObjRoster() : null;
    } // end method getDrawObjRoster

} // end class OstdotOccupiedListController
