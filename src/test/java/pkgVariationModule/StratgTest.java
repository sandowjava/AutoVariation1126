package pkgVariationModule;

// import csurf.cleandesign.ostwald.domainmonotri.model.ColorPtOstDot;
// import csurf.cleandesign.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
// import csurf.cleandesign.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
// import csurf.cleandesign.variationmodule.varyengine.model.*;
import com.csurf.variationmodule.varyengine.model.*;
// import csurf.cleandesign.variationmodule.varyengine.model.StratgParam;
import com.csurf.variationmodule.varyengine.model.StratgParam;
// import csurf.cleandesign.variationmodule.varyengine.model.StratgType;
import com.csurf.variationmodule.varyengine.model.StratgType;
// import csurf.cleandesign.variationmodule.varyengine.model.stratgs.AssignAGsStratg;
import com.csurf.variationmodule.varyengine.model.stratgs.AssignAGsStratg;
// import csurf.cleandesign.variationmodule.varyengine.model.stratgs.AssignAnchorHuesStratg;
import com.csurf.variationmodule.varyengine.model.stratgs.AssignAnchorHuesStratg;
// import csurf.cleandesign.variationmodule.varyengine.model.stratgs.AssignOstDotsStratg;
import com.csurf.variationmodule.varyengine.model.stratgs.AssignOstDotsStratg;
// import csurf.cleandesign.variationmodule.varyengine.model.stratgs.FreezeShapesStratg;
import com.csurf.variationmodule.varyengine.model.stratgs.FreezeShapesStratg;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * Step-5a verification: per-stratg unit tests on hand-built facts/state,
 * plus the shell-delegation invariant (921's execute = apply(compute)).
 * The 921 pref-level cases replicate legacy {@code DrawObjSmart2}
 * accessor semantics, quirks included.
 */
class StratgTest {

    private static final AssignAGsStratg ASSIGN_AGS = new AssignAGsStratg();
    private static final AssignAnchorHuesStratg ASSIGN_HUES = new AssignAnchorHuesStratg();
    private static final AssignOstDotsStratg ASSIGN_DOTS = new AssignOstDotsStratg();
    private static final FreezeShapesStratg FREEZE = new FreezeShapesStratg();

    // ---------- small hand-built Facts ----------

    /** Minimal Facts: per-shape defaultAG, four pref weights, valueTarget. */
    private static final class StubFacts implements Facts {
        private final int[] defaultAG;
        private final double[][] prefs;   // [shape-1][ag-1]
        private final double[] targets;
        private final Set<Integer> ids = new LinkedHashSet<>();

        StubFacts(int[] defaultAG, double[][] prefs, double[] targets) {
            this.defaultAG = defaultAG;
            this.prefs = prefs;
            this.targets = targets;
            for (int i = 1; i <= defaultAG.length; i++) {
                ids.add(i);
            } // end FOR
        }

        @Override
        public int numShapes() {
            return defaultAG.length;
        }

        @Override
        public Set<Integer> shapeIds() {
            return ids;
        }

        @Override
        public int defaultAG(int shape) {
            return defaultAG[shape - 1];
        }

        @Override
        public double agPref(int shape, int ag) {
            return prefs[shape - 1][ag - 1];
        }

        @Override
        public double valueTarget(int shape) {
            return targets[shape - 1];
        }
    } // end class StubFacts

    private static StratgSpec spec(StratgType type) {
        return new StratgSpec(type);
    }

    private static StratgContext ctx(Facts facts, VariationRunState state) {
        return BasicStratgContext.withoutRunner(facts, state, Inputs.noBase());
    }

    // ---------- 921 — PureCore semantics ----------

    @Test
    void assignAGs_level4_readsAuthoredDefault_neverArgmax() {
        AuthoredFactsTable facts = AuthoredFactsTable.instance();
        Proposal p = ASSIGN_AGS.compute(facts,
                spec(StratgType.ASSIGN_AGS).bind(StratgParam.CONTROL_LEVEL, 4));

        Assertions.assertEquals(4, p.ag(13).get().intValue());  // plan's walkthrough case
        Assertions.assertEquals(1, p.ag(1).get().intValue());
        Assertions.assertEquals(1, p.ag(18).get().intValue());  // authored 1 beats argmax 4 (OQ9)
        Assertions.assertEquals(21, p.writtenShapes(Channel.AG).size());
    }

    @Test
    void assignAGs_level1_smallestPositiveWeight() {
        StubFacts facts = new StubFacts(
                new int[]{1, 2},
                new double[][]{{0.9, 0.3, 0.5, 0.4}, {0.75, 0.95, 0.00, 0.00}},
                new double[]{0.5, 0.5});
        Proposal p = ASSIGN_AGS.compute(facts,
                spec(StratgType.ASSIGN_AGS).bind(StratgParam.CONTROL_LEVEL, 1));

        Assertions.assertEquals(2, p.ag(1).get().intValue()); // min positive 0.3 @ AG2
        Assertions.assertEquals(1, p.ag(2).get().intValue()); // zeros are "never" → 0.75 @ AG1
    }

    @Test
    void assignAGs_level2_midMin_legacyQuirkBoundIsDefaultAG() {
        StubFacts facts = new StubFacts(
                new int[]{2, 1},
                new double[][]{{0.75, 0.95, 0.00, 0.00}, {0.9, 0.5, 0.7, 0.3}},
                new double[]{0.5, 0.5});
        Proposal p = ASSIGN_AGS.compute(facts,
                spec(StratgType.ASSIGN_AGS).bind(StratgParam.CONTROL_LEVEL, 2));

        // shape 1: floor=prefs[min]=0.75, bound=prefs[defaultAG]=0.95 → nothing qualifies → fallback 1
        Assertions.assertEquals(1, p.ag(1).get().intValue());
        // shape 2: floor=0.3, bound=0.9 → AG2 (0.5) qualifies first → 2
        Assertions.assertEquals(2, p.ag(2).get().intValue());
    }

    @Test
    void assignAGs_level3_midMax_largestBelowDefault() {
        StubFacts facts = new StubFacts(
                new int[]{1, 2},
                new double[][]{{0.9, 0.5, 0.7, 0.3}, {0.75, 0.95, 0.00, 0.00}},
                new double[]{0.5, 0.5});
        Proposal p = ASSIGN_AGS.compute(facts,
                spec(StratgType.ASSIGN_AGS).bind(StratgParam.CONTROL_LEVEL, 3));

        // shape 1: below prefs[defaultAG]=0.9, above floor=0.3 → largest is 0.7 @ AG3
        Assertions.assertEquals(3, p.ag(1).get().intValue());
        // shape 2: only prefs[min]=0.75 qualifies via equal-tie → later index → 1
        Assertions.assertEquals(1, p.ag(2).get().intValue());
    }

    @Test
    void assignAGs_executeDelegatesToCore_shellDelegationInvariant() {
        StubFacts facts = new StubFacts(
                new int[]{4, 1},
                new double[][]{{0.35, 0.40, 0.60, 0.90}, {0.95, 0.75, 0.40, 0.00}},
                new double[]{0.5, 0.5});
        StratgSpec spec = spec(StratgType.ASSIGN_AGS).bind(StratgParam.CONTROL_LEVEL, 4);

        Proposal core = ASSIGN_AGS.compute(facts, spec);

        VariationRunState state = VariationRunState.noBase(2);
        ASSIGN_AGS.execute(ctx(facts, state), spec);

        for (int shape : state.shapeIds()) {
            Assertions.assertEquals(core.ag(shape).get().intValue(), state.ag(shape).get().intValue());
        } // end FOR
    }

    @Test
    void assignAGs_rejectsBadLevelAndWrongSpecType() {
        StubFacts facts = new StubFacts(new int[]{1}, new double[][]{{1, 1, 1, 1}}, new double[]{0.5});
        Assertions.assertThrows(IllegalArgumentException.class, () -> ASSIGN_AGS.compute(facts,
                spec(StratgType.ASSIGN_AGS).bind(StratgParam.CONTROL_LEVEL, 5)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ASSIGN_AGS.compute(facts,
                spec(StratgType.ASSIGN_ANCHOR_HUES)
                        .bind(StratgParam.CONTROL_LEVEL, 4)
                        .bind(StratgParam.AGHUE_PATTERN, "06-09-04-02-legacyTableRowNum0")));
    }

    // ---------- 555 — AG → anchor hue ----------

    @Test
    void assignHues_mapsDecidedAGsThroughHueSet_skipsUndecided() {
        AuthoredFactsTable facts = AuthoredFactsTable.instance();
        VariationRunState state = VariationRunState.noBase(4);
        state.setAG(1, 1);
        state.setAG(2, 2);
        state.setAG(3, 4);
        // shape 4 left UNDECIDED → skipped

        StratgSpec spec = spec(StratgType.ASSIGN_ANCHOR_HUES)
                .bind(StratgParam.CONTROL_LEVEL, 3)
                .bind(StratgParam.AGHUE_PATTERN, "06-09-04-02-legacyTableRowNum0");
        ASSIGN_HUES.execute(ctx(facts, state), spec);

        // '06-09-04-02-legacyTableRowNum0' = (6,9,4,2): AG1→6, AG2→9, AG4→2
        Assertions.assertEquals(6, state.hue(1).get().getUserFacingHueNum());
        Assertions.assertEquals(9, state.hue(2).get().getUserFacingHueNum());
        Assertions.assertEquals(2, state.hue(3).get().getUserFacingHueNum());
        Assertions.assertFalse(state.hue(4).isPresent());
    }

    @Test
    void assignHues_outOfRangeAGFails_notSkipped() {
        AuthoredFactsTable facts = AuthoredFactsTable.instance();
        VariationRunState state = VariationRunState.noBase(1);
        state.setAG(1, 9);   // corrupt decided value — a bug, not a skip
        StratgSpec spec = spec(StratgType.ASSIGN_ANCHOR_HUES)
                .bind(StratgParam.CONTROL_LEVEL, 1)
                .bind(StratgParam.AGHUE_PATTERN, "06-09-04-02-legacyTableRowNum0");
        Assertions.assertThrows(IllegalArgumentException.class, () -> ASSIGN_HUES.execute(ctx(facts, state), spec));
    }

    // ---------- 255 — hue → ost dot ----------

    @Test
    void assignDots_picksFirstCandidateByValueRange_skipsUndecidedHue() {
        AuthoredFactsTable facts = AuthoredFactsTable.instance();
        VariationRunState state = VariationRunState.noBase(2);
        HueNumGUI hue6 = HueNumGUI.getHueNumGUIInstance(6);
        state.setHue(1, hue6);
        // shape 2 hue UNDECIDED → skipped

        StratgSpec spec = spec(StratgType.ASSIGN_OST_DOTS)
                .bind(StratgParam.CONTROL_LEVEL, 1).bind(StratgParam.VALUE_RANGE, 0.055);
        ASSIGN_DOTS.execute(ctx(facts, state), spec);

        List<ColorPtOstDot> expected = MonotriBook.getInstance()
                .getOstDotsByValueRange(hue6, facts.valueTarget(1), 0.055);
        Assertions.assertFalse(expected.isEmpty());
        Assertions.assertEquals(expected.get(0), state.ostDot(1).get());
        Assertions.assertFalse(state.ostDot(2).isPresent());
    }

    @Test
    void assignDots_emptyCandidateSetErrorsLoudly() {
        StubFacts facts = new StubFacts(new int[]{1}, new double[][]{{1, 1, 1, 1}}, new double[]{-1.0});
        VariationRunState state = VariationRunState.noBase(1);
        state.setHue(1, HueNumGUI.getHueNumGUIInstance(6));

        StratgSpec spec = spec(StratgType.ASSIGN_OST_DOTS)
                .bind(StratgParam.CONTROL_LEVEL, 1).bind(StratgParam.VALUE_RANGE, 0.055);
        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> ASSIGN_DOTS.execute(ctx(facts, state), spec));
        Assertions.assertTrue(thrown.getMessage().contains("shape 1"));
        Assertions.assertTrue(thrown.getMessage().contains("hue 6"));
    }

    // ---------- 031 — frozen flags ----------

    @Test
    void freeze_writesOnlyFlags_nothingElse() {
        AuthoredFactsTable facts = AuthoredFactsTable.instance();
        VariationRunState state = VariationRunState.noBase(21);
        StratgSpec spec = spec(StratgType.FREEZE_SHAPES).bind(StratgParam.FROZEN_SET, 0);
        FREEZE.execute(ctx(facts, state), spec);

        Assertions.assertEquals(FrozenSets.get(0), state.whichFrozen());
        for (int shape : state.shapeIds()) {
            Assertions.assertFalse(state.ag(shape).isPresent());
            Assertions.assertFalse(state.hue(shape).isPresent());
            Assertions.assertFalse(state.ostDot(shape).isPresent());
        } // end FOR
    }

    @Test
    void freeze_outOfRangeShapeFailsLoudly() {
        AuthoredFactsTable facts = AuthoredFactsTable.instance();
        VariationRunState state = VariationRunState.noBase(3);  // set 0 references shape 8+
        StratgSpec spec = spec(StratgType.FREEZE_SHAPES).bind(StratgParam.FROZEN_SET, 0);
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class,
                () -> FREEZE.execute(ctx(facts, state), spec));
        Assertions.assertTrue(thrown.getMessage().contains("shape 8"));
    }

    // ---------- SHAPE_SET params — the FROZEN_SHAPES value door ----------

    @Test
    void shapeSetBind_bindsAndReadsBack() {
        StratgSpec spec = spec(StratgType.FREEZE_SHAPES)
                .bind(StratgParam.FROZEN_SHAPES, Set.of(3, 5, 12));
        Assertions.assertTrue(spec.isBound(StratgParam.FROZEN_SHAPES));
        Assertions.assertEquals(Set.of(3, 5, 12), spec.getIntSetParam(StratgParam.FROZEN_SHAPES));
    }

    @Test
    void shapeSetBind_rejectsNonSetKindParams() {
        // CONTROL_LEVEL is an INT kind param — bind(Set) must reject it
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                spec(StratgType.ASSIGN_AGS).bind(StratgParam.CONTROL_LEVEL, Set.of(1, 2)));
    }

    @Test
    void shapeSetBind_rejectsUndeclaredTypes() {
        // 255 does not declare FROZEN_SHAPES — checkDeclared fires before the kind check
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                spec(StratgType.ASSIGN_OST_DOTS).bind(StratgParam.FROZEN_SHAPES, Set.of(1, 2)));
    }

    @Test
    void shapeSetBind_storesImmutableCopy() {
        Set<Integer> source = new LinkedHashSet<>(Set.of(3, 5, 12));
        StratgSpec spec = spec(StratgType.FREEZE_SHAPES).bind(StratgParam.FROZEN_SHAPES, source);

        source.add(99);   // mutating the caller's set must not reach the bound value
        Assertions.assertEquals(Set.of(3, 5, 12), spec.getIntSetParam(StratgParam.FROZEN_SHAPES));
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> spec.getIntSetParam(StratgParam.FROZEN_SHAPES).add(7));
    }

    @Test
    void shapeSetBind_copyForCookingPreservesSet() {
        StratgSpec spec = spec(StratgType.FREEZE_SHAPES)
                .bind(StratgParam.FROZEN_SHAPES, Set.of(3, 5, 12));
        StratgSpec copy = spec.copyForCooking();
        Assertions.assertEquals(spec, copy);
        Assertions.assertEquals(spec.hashCode(), copy.hashCode());
        Assertions.assertEquals(Set.of(3, 5, 12), copy.getIntSetParam(StratgParam.FROZEN_SHAPES));
    }

    @Test
    void shapeSetBind_nullSetFailsFast() {
        Assertions.assertThrows(NullPointerException.class, () ->
                spec(StratgType.FREEZE_SHAPES).bind(StratgParam.FROZEN_SHAPES, (Set<Integer>) null));
    }

    // ---------- 031 — the frozenSet / frozenShapes XOR ----------

    @Test
    void freeze_neitherParamBoundFails() {
        AuthoredFactsTable facts = AuthoredFactsTable.instance();
        VariationRunState state = VariationRunState.noBase(21);
        Assertions.assertThrows(IllegalStateException.class,
                () -> FREEZE.execute(ctx(facts, state), spec(StratgType.FREEZE_SHAPES)));
    }

    @Test
    void freeze_bothParamsBoundFails() {
        AuthoredFactsTable facts = AuthoredFactsTable.instance();
        VariationRunState state = VariationRunState.noBase(21);
        StratgSpec spec = spec(StratgType.FREEZE_SHAPES)
                .bind(StratgParam.FROZEN_SET, 0)
                .bind(StratgParam.FROZEN_SHAPES, Set.of(1, 2));
        Assertions.assertThrows(IllegalStateException.class,
                () -> FREEZE.execute(ctx(facts, state), spec));
    }

    @Test
    void freeze_userSet_writesFlagsForBoundShapes() {
        AuthoredFactsTable facts = AuthoredFactsTable.instance();
        VariationRunState state = VariationRunState.noBase(21);
        StratgSpec spec = spec(StratgType.FREEZE_SHAPES)
                .bind(StratgParam.FROZEN_SHAPES, Set.of(2, 6, 9));
        FREEZE.execute(ctx(facts, state), spec);
        Assertions.assertEquals(Set.of(2, 6, 9), state.whichFrozen());
    }

    @Test
    void freeze_userSet_outOfRangeShapeFailsLoudly() {
        AuthoredFactsTable facts = AuthoredFactsTable.instance();
        VariationRunState state = VariationRunState.noBase(3);
        StratgSpec spec = spec(StratgType.FREEZE_SHAPES)
                .bind(StratgParam.FROZEN_SHAPES, Set.of(1, 8));
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class,
                () -> FREEZE.execute(ctx(facts, state), spec));
        Assertions.assertTrue(thrown.getMessage().contains("shape 8"));
    }

    @Test
    void freeze_userSet_emptySetIsLegal_freezesNothing() {
        AuthoredFactsTable facts = AuthoredFactsTable.instance();
        VariationRunState state = VariationRunState.noBase(21);
        StratgSpec spec = spec(StratgType.FREEZE_SHAPES)
                .bind(StratgParam.FROZEN_SHAPES, Set.of());
        FREEZE.execute(ctx(facts, state), spec);
        Assertions.assertTrue(state.whichFrozen().isEmpty());
    }

} // end class StratgTest
