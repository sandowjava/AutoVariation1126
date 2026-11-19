package com.csurf.variationmodule.varyengine.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;

import java.util.*;


/**
 * The finished, immutable result of one variation run ("the plated
 * serving"), built once at materialization (parent §4.2):
 *
 * <ul>
 *   <li><b>effectiveColors</b> — the 21-color palette: for frozen
 *       shapes, the base's value (parity with legacy output).</li>
 *   <li><b>decided provenance</b> — the decided-layer AG / hue / dot
 *       maps, matching what legacy's palette arrays stamped (slice-1
 *       Stratgs decide every shape, so frozen shapes carry decided
 *       provenance even though their effective color is the base's).</li>
 *   <li><b>frozenShapes</b> — the override flags at materialization.</li>
 *   <li><b>artGoalSpec</b> — the {@link ArtisticGoalSpec} that produced this
 *       result (recipe identity for provenance).</li>
 * </ul>
 *
 * A {@code VariationResult} can become the next cycle's base without
 * cloning — it is already immutable.
 */
public final class VariationResult {

    private final Map<Integer, ColorPtOstDot> effectiveColors;
    private final Map<Integer, Integer> decidedAG;
    private final Map<Integer, HueNumGUI> decidedHue;
    private final Map<Integer, ColorPtOstDot> decidedOstDot;
    private final Set<Integer> frozenShapes;
    private final ArtisticGoalSpec artGoalSpec;

    public VariationResult(Map<Integer, ColorPtOstDot> effectiveColors,
                           Map<Integer, Integer> decidedAG,
                           Map<Integer, HueNumGUI> decidedHue,
                           Map<Integer, ColorPtOstDot> decidedOstDot,
                           Set<Integer> frozenShapes,
                           ArtisticGoalSpec someArtGoalSpec) {
        this.effectiveColors = Collections.unmodifiableMap(new LinkedHashMap<>(effectiveColors));
        this.decidedAG = Collections.unmodifiableMap(new LinkedHashMap<>(decidedAG));
        this.decidedHue = Collections.unmodifiableMap(new LinkedHashMap<>(decidedHue));
        this.decidedOstDot = Collections.unmodifiableMap(new LinkedHashMap<>(decidedOstDot));
        this.frozenShapes = Collections.unmodifiableSet(frozenShapes);
        this.artGoalSpec = Objects.requireNonNull(someArtGoalSpec, "artGoalSpec");
    } // end constructor

    // ---------- effective accessors (what a frozen shape's base read returns) ----------

    public Optional<ColorPtOstDot> ostDot(int shape) {
        return Optional.ofNullable(effectiveColors.get(shape));
    } // end method ostDot

    public Optional<Integer> ag(int shape) {
        return Optional.ofNullable(decidedAG.get(shape));
    } // end method ag

    public Optional<HueNumGUI> hue(int shape) {
        return Optional.ofNullable(decidedHue.get(shape));
    } // end method hue

    // ---------- whole-map views for diffs / materialization / display ----------

    public Map<Integer, ColorPtOstDot> effectiveColors() {
        return effectiveColors;
    } // end method effectiveColors

    public Map<Integer, Integer> decidedAG() {
        return decidedAG;
    } // end method decidedAG

    public Map<Integer, HueNumGUI> decidedHue() {
        return decidedHue;
    } // end method decidedHue

    public Map<Integer, ColorPtOstDot> decidedOstDot() {
        return decidedOstDot;
    } // end method decidedOstDot

    public Set<Integer> frozenShapes() {
        return frozenShapes;
    } // end method frozenShapes

    public ArtisticGoalSpec getArtGoalSpec() {
        return artGoalSpec;
    } // end method getArtGoalSpec

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } // end IF
        if (!(o instanceof VariationResult)) {
            return false;
        } // end IF
        VariationResult other = (VariationResult) o;
        return effectiveColors.equals(other.effectiveColors)
                && decidedAG.equals(other.decidedAG)
                && decidedHue.equals(other.decidedHue)
                && decidedOstDot.equals(other.decidedOstDot)
                && frozenShapes.equals(other.frozenShapes)
                && artGoalSpec.equals(other.artGoalSpec);
    } // end method equals

    @Override
    public int hashCode() {
        return Objects.hash(effectiveColors, decidedAG, decidedHue,
                decidedOstDot, frozenShapes, artGoalSpec);
    } // end method hashCode

    @Override
    public String toString() {
        return "VariationResult{" + effectiveColors.size() + " colors, "
                + frozenShapes.size() + " frozen, artGoalSpec=" + artGoalSpec + "}";
    } // end method toString

} // end class VariationResult
