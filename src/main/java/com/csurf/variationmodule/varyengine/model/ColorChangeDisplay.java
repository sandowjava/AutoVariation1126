package com.csurf.variationmodule.varyengine.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

import java.util.*;


/**
 * The display-only surface for a finished cycle (step 8): holds the 12
 * {@link VariationResult}s in slot order — slot 0 is the reference
 * (bootstrap result) — and exposes what a display needs: per-slot
 * effective color maps, the frozen-shape set, and decided-layer
 * provenance. No Swing here (the JFrame wiring is open question OQ8);
 * this is the data seam any display consumes.
 */
public final class ColorChangeDisplay {

    private final List<VariationResult> slots;

    public ColorChangeDisplay(List<VariationResult> results) {
        Objects.requireNonNull(results, "results");
        if (results.isEmpty()) {
            throw new IllegalArgumentException("a display needs at least the reference result");
        } // end IF
        this.slots = Collections.unmodifiableList(new ArrayList<>(results));
    } // end constructor

    public int slotCount() {
        return slots.size();
    } // end method slotCount

    /** Slot 0 = the bootstrap reference (what legacy called the start state). */
    public VariationResult reference() {
        return slots.get(0);
    } // end method reference

    /** All results in slot order (reference first). */
    public List<VariationResult> results() {
        return slots;
    } // end method results

    /** The effective color map for a slot: {@code shape → ColorPtOstDot} (base colors on frozen shapes). */
    public Map<Integer, ColorPtOstDot> paletteFor(int slot) {
        return slots.get(slot).effectiveColors();
    } // end method paletteFor

    /** The frozen-shape set for a slot (empty for slot 0). */
    public Set<Integer> frozenShapesFor(int slot) {
        return slots.get(slot).frozenShapes();
    } // end method frozenShapesFor

    /** A compact textual summary — useful for verification dumps and logging. */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            VariationResult r = slots.get(i);
            sb.append("slot ").append(i + 1)
                    .append(" row=").append(r.getArtGoalSpec().getRecipeRow())
                    .append(" frozen=").append(r.frozenShapes())
                    .append('\n');
            for (Map.Entry<Integer, ColorPtOstDot> e : new LinkedHashMap<>(r.effectiveColors()).entrySet()) {
                sb.append("  shape ").append(e.getKey())
                        .append(" ag=").append(r.decidedAG().get(e.getKey()))
                        .append(" hue=").append(r.decidedHue().get(e.getKey()).getUserFacingHueNum())
                        .append(" -> ").append(e.getValue()).append('\n');
            } // end FOR
        } // end FOR
        return sb.toString();
    } // end method summary

} // end class ColorChangeDisplay
