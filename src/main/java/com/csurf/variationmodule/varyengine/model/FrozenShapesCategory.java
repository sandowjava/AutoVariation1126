package com.csurf.variationmodule.varyengine.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * The "cook" for the FrozenShapes broad category (step 6): the
 * category is a factory that produces
 * {@link VariationConstants#STANDARD_VARIATION_COUNT} fully-specified
 * {@link ArtisticGoalSpec}s — <b>specs only, it runs nothing</b>.
 *
 * <p>The recipe table is a faithful transcription of
 * {@code BroadCategoryFrozenShapes.createAndRun12Variations} plus
 * {@code StratgForSetAFakeStartFrozen999}'s verified inner kernel set:
 * <ul>
 *   <li><b>Row 1 = the bootstrap spec</b> {@code {921(4), 555(3,agHuePattern),
 *       255(1)}} — the 999 decomposition (Option B): it consumes no base
 *       and no freeze goal; the driver runs it first and promotes the
 *       result to the shared base.</li>
 *   <li><b>Rows 2–12 = the 11 processed-case kernel sites</b>:
 *       rows 2–4 → {@code 921(2)/921(3)/921(1)} with 555 controls
 *       {@code 2/3/4}; rows 5–12 → {@code 920(first,second)} alternating
 *       {@code (1,2)(2,3)(3,4)(4,1)(1,3)(2,4)(3,1)(4,2)} with 555
 *       control 2. Every row carries the same {@code 255(1,valueRange)}
 *       and the same {@code 031(frozenSet)}.</li>
 * </ul>
 *
 * <p>Uniform dialog bindings — {@code agHuePattern}, {@code frozenSet},
 * {@code valueRange} — are filled into the rows' open params at cook
 * time (defaults live at the binding layer: {@code valueRange} defaults
 * to {@link VariationConstants#VALUE_RANGE}). Legacy 999 is not
 * represented: row 1's spec + driver ordering + injected base replace it.
 */
public final class FrozenShapesCategory {

    /** The partial-goal types this category produces (row 1 omits PG1A). */
    public static final Set<PartialGoalType> REQUIRED_GOAL_TYPES = EnumSet.of(
            PartialGoalType.PG2, PartialGoalType.PG3, PartialGoalType.PG4, PartialGoalType.PG1A);

    private final int howManyVariations;

    /** Standard cook — the fixed 12-row recipe. */
    public FrozenShapesCategory() {
        this(VariationConstants.STANDARD_VARIATION_COUNT);
    } // end constructor

    /**
     * Explicit count — the recipe table is fixed at 12 rows
     * (the bootstrap spec + 11 processed-case kernel sites), so any
     * other count is an explicit error, per step 6.
     */
    public FrozenShapesCategory(int howManyVariations) {
        if (howManyVariations != VariationConstants.STANDARD_VARIATION_COUNT) {
            throw new IllegalArgumentException(
                    "the FrozenShapes recipe table is fixed at "
                            + VariationConstants.STANDARD_VARIATION_COUNT
                            + " rows; got howManyVariations=" + howManyVariations);
        } // end IF
        this.howManyVariations = howManyVariations;
    } // end constructor



    /**
     * Cook from a dialog-produced {@link BroadCategorySpec}: validates
     * the category type, extracts the uniform bindings from the
     * template's member StratgSpecs — {@code AGHUE_PATTERN} from the
     * 555 spec, {@code FROZEN_SET} or {@code FROZEN_SHAPES} from the 031 spec,
     * {@code VALUE_RANGE} from the
     * 255 spec (default {@link VariationConstants#VALUE_RANGE} when the
     * template leaves it open).
     */
    public List<ArtisticGoalSpec> cook(BroadCategorySpec categorySpec) {
        if (categorySpec == null
                || categorySpec.getCategoryType() != BroadCategoryType.FROZEN_SHAPES) {
            throw new IllegalArgumentException(
                    "FrozenShapesCategory can only cook a FROZEN_SHAPES BroadCategorySpec");
        } // end IF

        String agHuePattern = null;
        Integer frozenSet = null;
        Set<Integer> frozenShapes = null;
        double valueRange = VariationConstants.VALUE_RANGE;
        for (PartialGoalSpec goal : categorySpec.getPartialGoalSpecs()) {
            for (StratgSpec stratg : goal.getStratgSpecs()) {
                if (stratg.getStratgType() == StratgType.ASSIGN_ANCHOR_HUES
                        && stratg.isBound(StratgParam.AGHUE_PATTERN)) {
                    agHuePattern = stratg.getStringParam(StratgParam.AGHUE_PATTERN);
                } else if (stratg.getStratgType() == StratgType.FREEZE_SHAPES) {
                    if (stratg.isBound(StratgParam.FROZEN_SET)) {
                        frozenSet = stratg.getIntParam(StratgParam.FROZEN_SET);
                    } // end IF
                    if (stratg.isBound(StratgParam.FROZEN_SHAPES)) {
                        frozenShapes = stratg.getIntSetParam(StratgParam.FROZEN_SHAPES);
                    } // end IF
                } else if (stratg.getStratgType() == StratgType.ASSIGN_OST_DOTS
                        && stratg.isBound(StratgParam.VALUE_RANGE)) {
                    valueRange = stratg.getDoubleParam(StratgParam.VALUE_RANGE);
                } // end IF-ELSE
            } // end FOR
        } // end FOR

        if (agHuePattern == null) {
            throw new IllegalArgumentException(
                    "the category spec binds no AGHUE_PATTERN — the dialog must bind "
                            + "a canonical AGHuePatterns name into its 555 StratgSpec "
                            + "(" + AGHuePatterns.count() + " entries in catalog)");
        } // end IF
        if ((frozenSet == null) == (frozenShapes == null)) {
            throw new IllegalArgumentException(
                    "the category spec must bind exactly one of FROZEN_SET (a frozenSet "
                            + "index 0.." + (FrozenSets.FROZEN_SET_COUNT - 1) + ") or "
                            + "FROZEN_SHAPES (an explicit shape set) into its 031 StratgSpec");
        } // end IF
        return frozenShapes != null
                ? cook(agHuePattern, frozenShapes, valueRange)
                : cook(agHuePattern, frozenSet, valueRange);
    } // end method cook(BroadCategorySpec)



    /** Cook with the standard valueRange default. */
    public List<ArtisticGoalSpec> cook(String agHuePattern, int frozenSet) {
        return cook(agHuePattern, frozenSet, VariationConstants.VALUE_RANGE);
    } // end method cook(String, int)



    /** Cook with the standard valueRange default, using an explicit frozen-shape set. */
    public List<ArtisticGoalSpec> cook(String agHuePattern, Set<Integer> frozenShapes) {
        return cook(agHuePattern, frozenShapes, VariationConstants.VALUE_RANGE);
    } // end method cook(String, Set)



    /**
     * Emits the 12 fully-specified {@link ArtisticGoalSpec}s in recipe
     * order (row 1 = bootstrap). Table-key bounds are validated here —
     * bad keys fail loudly at cook time rather than inside a run.
     */
    public List<ArtisticGoalSpec> cook(String agHuePattern, int frozenSet, double valueRange) {
        // fail-fast on table keys (specs remain self-contained — these are bound values, not defaults)
        AGHuePatterns.get(agHuePattern);
        FrozenSets.get(frozenSet);

        List<ArtisticGoalSpec> results = new ArrayList<>(howManyVariations);
        for (RecipeRow row : RECIPE) {
            results.add(buildRow(row, agHuePattern, frozenSet, null, valueRange));
        } // end FOR
        return results;
    } // end method cook(String, int, double)



    /**
     * Emits the 12 fully-specified {@link ArtisticGoalSpec}s in recipe
     * order (row 1 = bootstrap), binding an explicit frozen-shape set
     * (the userSelectFrozen path) instead of a {@link FrozenSets} table key.
     * Shape-ID membership is NOT checked here — only the run knows valid
     * IDs; {@code FreezeShapesStratg} validates them at execute time.
     */
    public List<ArtisticGoalSpec> cook(String agHuePattern, Set<Integer> frozenShapes, double valueRange) {
        // the named key still fails fast; set membership is a run-time concern
        AGHuePatterns.get(agHuePattern);
        Set<Integer> shapes = Set.copyOf(Objects.requireNonNull(frozenShapes, "frozenShapes"));

        List<ArtisticGoalSpec> results = new ArrayList<>(howManyVariations);
        for (RecipeRow row : RECIPE) {
            results.add(buildRow(row, agHuePattern, null, shapes, valueRange));
        } // end FOR
        return results;
    } // end method cook(String, Set, double)



    // ---------- the recipe table ----------

    /** Builds one recipe row; exactly one of frozenSet / frozenShapes is non-null (XOR by construction). */
    private ArtisticGoalSpec buildRow(RecipeRow row, String agHuePattern,
                                      Integer frozenSet, Set<Integer> frozenShapes,
                                      double valueRange) {
        List<PartialGoalSpec> goals = new ArrayList<>();

        StratgSpec ag = new StratgSpec(row.agType);
        if (row.agType == StratgType.ASSIGN_AGS) {
            ag.bind(StratgParam.CONTROL_LEVEL, row.first);
        } else {
            ag.bind(StratgParam.FIRST_PREF_LEVEL, row.first)
                    .bind(StratgParam.SECOND_PREF_LEVEL, row.second);
        } // end IF-ELSE
        goals.add(new PartialGoalSpec(PartialGoalType.PG2).addStratgSpec(ag));

        goals.add(new PartialGoalSpec(PartialGoalType.PG3).addStratgSpec(
                new StratgSpec(StratgType.ASSIGN_ANCHOR_HUES)
                        .bind(StratgParam.CONTROL_LEVEL, row.hueControl)
                        .bind(StratgParam.AGHUE_PATTERN, agHuePattern)));

        goals.add(new PartialGoalSpec(PartialGoalType.PG4).addStratgSpec(
                new StratgSpec(StratgType.ASSIGN_OST_DOTS)
                        .bind(StratgParam.CONTROL_LEVEL, 1)
                        .bind(StratgParam.VALUE_RANGE, valueRange)));

        if (row.freeze) {
            StratgSpec freeze = new StratgSpec(StratgType.FREEZE_SHAPES);
            if (frozenShapes != null) {
                freeze.bind(StratgParam.FROZEN_SHAPES, frozenShapes);
            } else {
                freeze.bind(StratgParam.FROZEN_SET, frozenSet);
            } // end IF-ELSE
            goals.add(new PartialGoalSpec(PartialGoalType.PG1A).addStratgSpec(freeze));
        } // end IF

        return new ArtisticGoalSpec(goals, row.recipeRow);
    } // end method buildRow



    /** One recipe row: the AG stratg + its bindings, the 555 control, and the freeze flag. */
    private static final class RecipeRow {
        final int recipeRow;
        final StratgType agType;
        final int first;      // 921: controlLevel; 920: firstPrefLevel
        final int second;     // 920 only: secondPrefLevel
        final int hueControl; // 555's vestigial controlLevel (recorded for provenance)
        final boolean freeze; // rows 2–12 carry the freeze goal; row 1 (bootstrap) does not

        RecipeRow(int recipeRow, StratgType agType, int first, int second, int hueControl, boolean freeze) {
            this.recipeRow = recipeRow;
            this.agType = agType;
            this.first = first;
            this.second = second;
            this.hueControl = hueControl;
            this.freeze = freeze;
        } // end constructor
    } // end class RecipeRow



    private static final StratgType AG = StratgType.ASSIGN_AGS;
    private static final StratgType ALT = StratgType.ALTERNATE_AG_POOLS;

    /** Transcription of the 11 kernel sites + the 999 inner kernel set (row 1). */
    private static final RecipeRow[] RECIPE = {
            new RecipeRow(1,  AG, 4, -1, 3, false),   // bootstrap — 999's inner {921(4),555(3),255(1)}
            new RecipeRow(2,  AG, 2, -1, 2, true),
            new RecipeRow(3,  AG, 3, -1, 3, true),
            new RecipeRow(4,  AG, 1, -1, 4, true),
            new RecipeRow(5,  ALT, 1, 2, 2, true),
            new RecipeRow(6,  ALT, 2, 3, 2, true),
            new RecipeRow(7,  ALT, 3, 4, 2, true),
            new RecipeRow(8,  ALT, 4, 1, 2, true),
            new RecipeRow(9,  ALT, 1, 3, 2, true),
            new RecipeRow(10, ALT, 2, 4, 2, true),
            new RecipeRow(11, ALT, 3, 1, 2, true),
            new RecipeRow(12, ALT, 4, 2, 2, true),
    };

} // end class FrozenShapesCategory
