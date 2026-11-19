package csurf.cleandesign.variationmodule.varyengine.model;

import java.util.Set;


/**
 * The read-only facts a Stratg may consult — the clean counterpart of
 * legacy's "GraphicSmarts-style" per-shape data (authored in
 * {@code SmartPicShapeRoster2.setAnchorGroupPrefs}/{@code setValueTarget}
 * and read through {@code DrawObjSmart2}/{@code ColorFunctionalAttributes2}).
 *
 * <p>Facts are never modified by Stratgs (stateless/deterministic
 * contract, parent §4.3). {@code PureCore.compute(facts, params)}
 * receives only this — no run state, no base, no flags (D5).
 *
 * <p>All indices are shape IDs 1..N and AG IDs 1–4. Hue numbers are
 * user-facing 1–36 (the legacy convention — see the 1–36-indexed
 * lum table and {@code getMonoTrianglePerStdOstHueNum}).
 */
public interface Facts {

    int numShapes();

    Set<Integer> shapeIds();

    /**
     * The authored default AG for a shape — legacy {@code prefsAmongAGs[0]},
     * written by {@code setAnchorGroupPrefs}. <b>Authored, never derived:</b>
     * stratg 921 level 4 reads this directly (OQ9); no argmax.
     */
    int defaultAG(int shape);

    /**
     * The authored preference weight for {@code ag} (1–4) — legacy
     * {@code prefsAmongAGs[ag]}, from the four weights in
     * {@code setAnchorGroupPrefs}. Used by 921's levels 1–3
     * (min / mid-min / mid-max derivation).
     */
    double agPref(int shape, int ag);

    /** The authored value target (luminance 0..1) that 255 matches against. */
    double valueTarget(int shape);

    /** Optional provenance: a name for the shape, when the fact source knows one. */
    default String shapeName(int shape) {
        return "shape-" + shape;
    } // end default method shapeName

} // end interface Facts
