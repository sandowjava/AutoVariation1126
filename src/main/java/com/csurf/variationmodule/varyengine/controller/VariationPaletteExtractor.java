package csurf.cleandesign.variationmodule.varyengine.controller;

import csurf.cleandesign.ostwald.domainmonotri.model.ColorPtOstDot;
import csurf.cleandesign.variationmodule.varyengine.hackhome.PaletteWName;
import csurf.cleandesign.variationmodule.varyengine.model.ArtisticGoalSpec;
import csurf.cleandesign.variationmodule.varyengine.model.Facts;
import csurf.cleandesign.variationmodule.varyengine.model.FrozenShapesCategory;
import csurf.cleandesign.variationmodule.varyengine.model.VariationConstants;
import csurf.cleandesign.variationmodule.varyengine.model.VariationDriver;
import csurf.cleandesign.variationmodule.varyengine.model.VariationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * 111626 added — the cycle-runner + palette-extraction seam for the
 * clean variation module (the portable counterpart of
 * {@code bridge.CleanCyclePalettes}; see design docs
 * {@code 111426_v1D} / {@code 111426_v1E} §7).
 *
 * <p>Responsibilities, mirroring the decided split:
 * <ul>
 *   <li>{@link #runCleanCycle} — static, stateless: cook + run,
 *       returning the raw {@link VariationResult}s (the primary,
 *       future-facing path).</li>
 *   <li>{@link #extract12VariationPalettes} — instance, recording:
 *       projects results into the {@link PalettesThe12Variations} DTO
 *       and stores it (plus the raw results) on this instance.</li>
 * </ul>
 *
 * <p>One instance per display frame ({@code hackhome.VariationDisplayFrame},
 * itself owned per window by {@code Vary12WorkClusterRoot}) — "last
 * results" state is scoped per window and stays out of static scope.
 *
 * <p>Portability rules: this class touches only {@code varyengine.model},
 * {@code cleandesign.ostwald}, and {@code hackhome.PaletteWName} — never
 * {@code csurf.domain} legacy types — so it copies to AutoVariation1126
 * with only the {@code PaletteWName} import to fix. {@code Facts} is a
 * parameter (not {@code AuthoredFactsTable.instance()} internally) so
 * the same seam serves the fixture, extraction, and stubs.
 */
public final class VariationPaletteExtractor {

    /** Upper bound for recipe-row indices in palette names (recipe rows are 1..12). */
    private static final int MAX_RECIPE_INDEX = VariationConstants.STANDARD_VARIATION_COUNT;


    /** Raw results of the last {@link #extract12VariationPalettes} call — kept for future provenance uses. */
    private List<VariationResult> lastVariationResults;

    /** DTO produced by the last {@link #extract12VariationPalettes} call; {@code null} until the first extract. */
    private PalettesThe12Variations last12PalettesResults;


    // 111626 instantiable — one instance per VariationDisplayFrame (per-window extractor state)
    public VariationPaletteExtractor() {
    } // end constructor


    /**
     * Runs the FrozenShapes cycle: {@code cook(hueSet, frozenSet)} then
     * {@code VariationDriver.run(artGoalSpecList, facts)}. Returns results in
     * recipe order — index 0 = the bootstrap result (recipe row 1,
     * display slot 1).
     */
    public static List<VariationResult> runCleanCycle(int hueSet, int frozenSet, Facts facts) {
        List<ArtisticGoalSpec> artGoalSpecList = new FrozenShapesCategory().cook(hueSet, frozenSet);
        return new VariationDriver().run(artGoalSpecList, facts);
    } // end method runCleanCycle


    /**
     * Projects a cycle's {@link VariationResult}s into a
     * {@link PalettesThe12Variations} DTO — pure extraction, no run.
     *
     * <p>Per result: {@code effectiveColors()} is keyed by shape ID,
     * dense 1..N; the {@link PaletteWName} array is dense 0..N-1 —
     * {@code arr[k-1] = colors.get(k)} — so array index i correlates to
     * shape/roster slotId i+1, the positional contract the palette
     * loaders assume. A gap OR a truncated key space fails loudly;
     * palette names carry the producing recipe row ("varyRecipe4").
     */
    public PalettesThe12Variations extract12VariationPalettes(List<VariationResult> results) {
        Objects.requireNonNull(results, "results");
        List<PaletteWName> palettes = new ArrayList<>(results.size());
        for (VariationResult result : results) {
            palettes.add(toPaletteWName(result));
        } // end FOR
        this.lastVariationResults = results;
        this.last12PalettesResults = new PalettesThe12Variations(palettes);
        return this.last12PalettesResults;
    } // end method extract12VariationPalettes


    /**
     * The {@link PalettesThe12Variations} produced by this instance's most
     * recent {@link #extract12VariationPalettes} call — {@code null} until
     * the first extract.
     */
    public PalettesThe12Variations getLast12PalettesResults() {
        return last12PalettesResults;
    } // end method getLast12PalettesResults


    /**
     * The raw {@link VariationResult}s passed to this instance's most recent
     * {@link #extract12VariationPalettes} call — {@code null} until the
     * first extract.
     */
    public List<VariationResult> getLastVariationResults() {
        return lastVariationResults;
    } // end method getLastVariationResults


    // ---------- per-result projection ----------

    private static PaletteWName toPaletteWName(VariationResult result) {
        Map<Integer, ColorPtOstDot> colors = result.effectiveColors();
        if (colors.isEmpty()) {
            throw new IllegalStateException(
                    "VariationResult (recipeRow " + result.getArtGoalSpec().getRecipeRow()
                            + ") has an empty effectiveColors map — a run produces 1..N colors");
        } // end IF

        // dense-keyspace check: catches interior gaps AND tail truncation (max key < size means
        // a low key is missing; size < maxKey means the map is keyed outside 1..N)
        int numShapes = colors.size();
        int maxKey = Collections.max(colors.keySet());
        if (maxKey != numShapes) {
            throw new IllegalStateException(
                    "VariationResult (recipeRow " + result.getArtGoalSpec().getRecipeRow()
                            + ") effectiveColors is not the dense key space 1.." + numShapes
                            + " — got " + numShapes + " keys with maxKey=" + maxKey);
        } // end IF

        ColorPtOstDot[] dots = new ColorPtOstDot[numShapes];
        for (int shape = 1; shape <= numShapes; shape++) {
            ColorPtOstDot dot = colors.get(shape);
            if (dot == null) {
                throw new IllegalStateException(
                        "VariationResult (recipeRow " + result.getArtGoalSpec().getRecipeRow()
                                + ") has no effective color for shape " + shape);
            } // end IF
            dots[shape - 1] = dot; // shape IDs are 1..N; the palette array is 0-based
        } // end FOR

        return new PaletteWName(dots, recipeName(result));
    } // end method toPaletteWName


    /** Palette name encodes the producing recipe row: "varyRecipe1".."varyRecipe12". */
    private static String recipeName(VariationResult result) {
        int recipeIndex = result.getArtGoalSpec().getRecipeRow();
        if (recipeIndex < 1 || recipeIndex > MAX_RECIPE_INDEX) {
            throw new IllegalStateException(
                    "artGoalSpec recipeRow " + recipeIndex + " is outside 1.." + MAX_RECIPE_INDEX
                            + " — only cook-produced artGoalSpecs carry a recipe index "
                            + "(recipeRow 0 = hand-authored)");
        } // end IF
        return "varyRecipe" + recipeIndex;
    } // end method recipeName


} // end class VariationPaletteExtractor
