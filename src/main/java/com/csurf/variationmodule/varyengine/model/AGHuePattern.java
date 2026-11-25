package com.csurf.variationmodule.varyengine.model;

import com.csurf.ostwald.domainmonotri.model.HueNumGUI;

import java.util.Objects;


/**
 * A named variation-domain anchor-hue pattern: an {@link AnchorHueSet}
 * (the four AG center hues) plus a canonical {@code name}.
 *
 * <p>Composition, not subclassing — {@link AnchorHueSet} stays a sealed
 * value type; this class adds naming (and, later, anchorPattern knowledge
 * such as analogous/complementary/triadic/quadratic levels). The name is
 * the instance's identity: canonical form is {@code "NN-NN-NN-NN[-suffix]"}
 * (zero-padded AG-order hue prefix, optional descriptive suffix) —
 * see {@link AGHuePatterns}.
 *
 * <p>Hues need not be distinct; the only validation is the inherited
 * {@link AnchorHueSet} range check (1–36).
 */
public final class AGHuePattern {

    private final String name;
    private final AnchorHueSet anchorHueSet;

    public AGHuePattern(String name, int hueAG1, int hueAG2, int hueAG3, int hueAG4) {
        this(name, new AnchorHueSet(hueAG1, hueAG2, hueAG3, hueAG4));
    } // end constructor

    public AGHuePattern(String name, AnchorHueSet anchorHueSet) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("AGHuePattern name cannot be null or blank");
        } // end IF
        this.name = name;
        this.anchorHueSet = Objects.requireNonNull(anchorHueSet, "anchorHueSet");
    } // end constructor

    /** The canonical name — the catalog key. */
    public String getName() {
        return name;
    } // end method getName

    /** The wrapped four-hue value object. */
    public AnchorHueSet getAnchorHueSet() {
        return anchorHueSet;
    } // end method getAnchorHueSet

    /** Convenience delegate: the anchor hue for {@code ag} (1–4). */
    public HueNumGUI anchorHueForAG(int ag) {
        return anchorHueSet.anchorHueForAG(ag);
    } // end method anchorHueForAG

    /** Convenience delegate: raw user-facing hue number for {@code ag} (1–4). */
    public int hueNumForAG(int ag) {
        return anchorHueSet.hueNumForAG(ag);
    } // end method hueNumForAG

    /** The four anchor hues in AG order — for JSON serialization. */
    public int[] hueNums() {
        return new int[]{hueNumForAG(1), hueNumForAG(2),
                hueNumForAG(3), hueNumForAG(4)};
    } // end method hueNums

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } // end IF
        if (!(o instanceof AGHuePattern)) {
            return false;
        } // end IF
        AGHuePattern other = (AGHuePattern) o;
        return name.equals(other.name);
    } // end method equals

    @Override
    public int hashCode() {
        return Objects.hash(name);
    } // end method hashCode

    @Override
    public String toString() {
        return name;
    } // end method toString

} // end class AGHuePattern
