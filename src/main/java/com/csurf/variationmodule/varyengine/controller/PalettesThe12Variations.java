package csurf.cleandesign.variationmodule.varyengine.controller;

import csurf.cleandesign.variationmodule.varyengine.hackhome.PaletteWName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * 111626 added — DTO for the 12 palettes produced by one clean-cycle
 * run. Holds 12 {@link PaletteWName}s in recipe order, as extracted by
 * {@link VariationPaletteExtractor#extract12VariationPalettes}.
 *
 * <p>Index convention: 0-based. {@code getPaletteAt(0)} is the
 * bootstrap/reference palette (recipe row 1, display slot 1);
 * {@code getPaletteAt(11)} is the last variation (display slot 12).
 * The legacy 1-based 13-cell {@code PaletteArray} layout is NOT
 * reproduced here — that translation is the bridge's job.
 *
 * <p>Portable by design: imports only {@code hackhome.PaletteWName}
 * (copies with the AutoVariation display DTO) and {@code java.util} —
 * no {@code csurf.domain}/legacy types. The "exactly 12, non-null"
 * invariant is enforced at construction, not assumed.
 */
public final class PalettesThe12Variations {

    public static final int NUM_VARIATIONS = 12;

    private final List<PaletteWName> palettes;


    public PalettesThe12Variations(List<PaletteWName> palettes) {
        Objects.requireNonNull(palettes, "palettes");
        if (palettes.size() != NUM_VARIATIONS) {
            throw new IllegalArgumentException(
                    "a variation cycle produces exactly " + NUM_VARIATIONS
                            + " palettes; got " + palettes.size());
        } // end IF
        for (int i = 0; i < palettes.size(); i++) {
            if (palettes.get(i) == null) {
                throw new IllegalArgumentException(
                        "palettes[" + i + "] is null — a variation cycle has no empty slots");
            } // end IF
        } // end FOR
        this.palettes = Collections.unmodifiableList(new ArrayList<>(palettes));
    } // end constructor


    /**
     * Returns the palette at the specified 0-based index
     * (index 0 = recipe row 1 = display slot 1, the reference coloring).
     */
    public PaletteWName getPaletteAt(int index) {
        return palettes.get(index);
    } // end method getPaletteAt

    /** Unmodifiable view, recipe order. */
    public List<PaletteWName> asList() {
        return palettes;
    } // end method asList

    public int size() {
        return palettes.size();
    } // end method size


} // end class PalettesThe12Variations
