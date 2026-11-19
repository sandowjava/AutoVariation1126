package csurf.cleandesign.variationmodule.varyengine.model;

import csurf.cleandesign.ostwald.domainmonotri.model.HueNumGUI;

import java.util.Objects;


/**
 * One row of the anchor-hue table: four AG "center hues" — the clean
 * counterpart of legacy {@code AnchorHuesForAGsSet}
 * ({@code CtrHueForAG{1-4}}). Hue numbers are user-facing 1–36;
 * accessors return typed {@link HueNumGUI}s. Immutable.
 */
public final class AnchorHueSet {

    private final HueNumGUI[] hues = new HueNumGUI[4];   // [ag-1]

    public AnchorHueSet(int hueAG1, int hueAG2, int hueAG3, int hueAG4) {
        hues[0] = HueNumGUI.getHueNumGUIInstance(requireHue(hueAG1));
        hues[1] = HueNumGUI.getHueNumGUIInstance(requireHue(hueAG2));
        hues[2] = HueNumGUI.getHueNumGUIInstance(requireHue(hueAG3));
        hues[3] = HueNumGUI.getHueNumGUIInstance(requireHue(hueAG4));
    } // end constructor

    private static int requireHue(int userFacingHueNum) {
        if (userFacingHueNum < 1 || userFacingHueNum > 36) {
            throw new IllegalArgumentException(
                    "user-facing hue " + userFacingHueNum + " out of range 1..36");
        } // end IF
        return userFacingHueNum;
    } // end method requireHue

    /** The anchor hue for {@code ag} (1–4). */
    public HueNumGUI anchorHueForAG(int ag) {
        if (ag < 1 || ag > 4) {
            throw new IllegalArgumentException("ag " + ag + " out of range 1..4");
        } // end IF
        return hues[ag - 1];
    } // end method anchorHueForAG

    /** Raw user-facing hue number for {@code ag} (1–4) — for table diffs. */
    public int hueNumForAG(int ag) {
        return anchorHueForAG(ag).getUserFacingHueNum();
    } // end method hueNumForAG

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } // end IF
        if (!(o instanceof AnchorHueSet)) {
            return false;
        } // end IF
        AnchorHueSet other = (AnchorHueSet) o;
        return hues[0] == other.hues[0] && hues[1] == other.hues[1]
                && hues[2] == other.hues[2] && hues[3] == other.hues[3];
    } // end method equals

    @Override
    public int hashCode() {
        return Objects.hash(hues[0], hues[1], hues[2], hues[3]);
    } // end method hashCode

    @Override
    public String toString() {
        return "AnchorHueSet(" + hues[0].getUserFacingHueNum() + ","
                + hues[1].getUserFacingHueNum() + ","
                + hues[2].getUserFacingHueNum() + ","
                + hues[3].getUserFacingHueNum() + ")";
    } // end method toString

} // end class AnchorHueSet
