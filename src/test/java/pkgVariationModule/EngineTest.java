package pkgVariationModule;

// import csurf.cleandesign.variationmodule.varyengine.model.*;
import com.csurf.variationmodule.varyengine.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * Step-7 verification: the engine executes a spec on a fresh run state
 * (descending-ID scheduling), and the driver runs the cooked cycle —
 * row 1 bootstrap on {@code noBase}, rows 2–12 against the injected
 * shared base.
 */
class EngineTest {

    private static final Facts FACTS = AuthoredFactsTable.instance();
    private static final String AGHUE_PATTERN = "05-03-06-08-legacyTableRowNum13"; // "very nice w yellow-tan"
    private static final int FROZEN_SET = 2; // {5,6,7,19,20,21}

    private final ColorChangeEngine engine = new ColorChangeEngine();
    private final List<ArtisticGoalSpec> recipe = new FrozenShapesCategory().cook(AGHUE_PATTERN, FROZEN_SET);

    // ---------- engine: bootstrap run ----------

    @Test
    void bootstrapRun_producesCompleteResult_noFrozenShapes() {
        VariationResult result = engine.execute(recipe.get(0), FACTS);

        Assertions.assertEquals(FACTS.numShapes(), result.effectiveColors().size());
        Assertions.assertEquals(FACTS.numShapes(), result.decidedAG().size());
        Assertions.assertEquals(FACTS.numShapes(), result.decidedHue().size());
        Assertions.assertEquals(FACTS.numShapes(), result.decidedOstDot().size());
        Assertions.assertTrue(result.frozenShapes().isEmpty());
        Assertions.assertSame(recipe.get(0), result.getArtGoalSpec());

        // row 1 = {921(4), 555(3), 255(1)} → AGs = authored defaults; hues = AGHUE_PATTERN = (5,3,6,8)
        Assertions.assertEquals(4, result.decidedAG().get(13).intValue());      // authored defaultAG
        Assertions.assertEquals(8, result.decidedHue().get(13).getUserFacingHueNum()); // AG4 → hue 8
        Assertions.assertEquals(5, result.decidedHue().get(1).getUserFacingHueNum());  // AG1 → hue 5
    }

    // ---------- engine: scheduling by descending ID ----------

    @Test
    void engineSchedulesByIdNotAuthoringOrder() {
        // same stratgs as row 1 but goals authored in reverse order — engine must still
        // run 921 → 555 → 255 (descending ID), otherwise 255 finds no decided hues
        ArtisticGoalSpec scrambled = new ArtisticGoalSpec(Arrays.asList(
                new PartialGoalSpec(PartialGoalType.PG4).addStratgSpec(
                        new StratgSpec(StratgType.ASSIGN_OST_DOTS)
                                .bind(StratgParam.CONTROL_LEVEL, 1)
                                .bind(StratgParam.VALUE_RANGE, 0.055)),
                new PartialGoalSpec(PartialGoalType.PG3).addStratgSpec(
                        new StratgSpec(StratgType.ASSIGN_ANCHOR_HUES)
                                .bind(StratgParam.CONTROL_LEVEL, 3).bind(StratgParam.AGHUE_PATTERN, AGHUE_PATTERN)),
                new PartialGoalSpec(PartialGoalType.PG2).addStratgSpec(
                        new StratgSpec(StratgType.ASSIGN_AGS)
                                .bind(StratgParam.CONTROL_LEVEL, 4))));

        VariationResult result = engine.execute(scrambled, FACTS);
        Assertions.assertEquals(FACTS.numShapes(), result.effectiveColors().size());
    }

    @Test
    void engineRejectsOpenSpec() {
        ArtisticGoalSpec open = new ArtisticGoalSpec(java.util.Collections.singletonList(
                new PartialGoalSpec(PartialGoalType.PG2).addStratgSpec(
                        new StratgSpec(StratgType.ASSIGN_AGS))));   // controlLevel unbound
        Assertions.assertThrows(IllegalStateException.class, () -> engine.execute(open, FACTS));
    }

    // ---------- processed run: shared base + freeze overlay ----------

    @Test
    void processedRun_frozenShapesReadBase_decidedLayerIsProvenance() {
        VariationResult base = engine.execute(recipe.get(0), FACTS);
        VariationResult processed = engine.execute(recipe.get(1), FACTS, base);

        Set<Integer> expectedFrozen = FrozenSets.get(FROZEN_SET);
        Assertions.assertEquals(expectedFrozen, processed.frozenShapes());

        for (int shape : expectedFrozen) {
            // effective = the base's coloring (frozen overlay at read time)
            Assertions.assertSame(base.ostDot(shape).get(), processed.ostDot(shape).get(),
                    "frozen shape " + shape + " effective");
            // decided layer was still written (provenance parity — slice-1 stratgs write all shapes)
            Assertions.assertTrue(processed.decidedOstDot().containsKey(shape),
                    "frozen shape " + shape + " decided provenance");
        } // end FOR

        // every shape materialized — non-frozen use this run's decided dots
        Assertions.assertEquals(FACTS.numShapes(), processed.effectiveColors().size());
        Assertions.assertSame(processed.decidedOstDot().get(1), processed.ostDot(1).get());
    }

    // ---------- driver: the full 12-run cycle ----------

    @Test
    void driverCycle_runsBootstrapThenElevenProcessed() {
        List<VariationResult> results = new VariationDriver().run(recipe, FACTS);
        Assertions.assertEquals(12, results.size());

        VariationResult reference = results.get(0);
        Assertions.assertTrue(reference.frozenShapes().isEmpty(), "slot 1 = bootstrap reference");

        Set<Integer> expectedFrozen = FrozenSets.get(FROZEN_SET);
        for (int i = 1; i < results.size(); i++) {
            VariationResult r = results.get(i);
            Assertions.assertEquals(expectedFrozen, r.frozenShapes(), "row " + (i + 1) + " frozen set");
            for (int shape : expectedFrozen) {
                Assertions.assertSame(reference.ostDot(shape).get(), r.ostDot(shape).get(),
                        "row " + (i + 1) + " shape " + shape + " reinstates base");
            } // end FOR
        } // end FOR
    }

    @Test
    void driverCycle_rowsProduceDistinctPalettes() {
        List<VariationResult> results = new VariationDriver().run(recipe, FACTS);
        // rows 2–4 differ only in AG level → their decided AG maps must differ somewhere
        boolean differs = false;
        for (int i = 1; i < 4 && !differs; i++) {
            for (int j = i + 1; j <= 4; j++) {
                if (!results.get(i).decidedAG().equals(results.get(j).decidedAG())) {
                    differs = true;
                } // end IF
            } // end FOR
        } // end FOR
        Assertions.assertTrue(differs, "distinct recipe rows should produce distinct assignments");
    }

    @Test
    void driverValidation_bootstrapSignatureAndFreezeRequirement() {
        // first spec carrying a freeze Stratg → no base exists yet
        List<ArtisticGoalSpec> badOrder = recipe.subList(1, 4);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new VariationDriver().run(badOrder, FACTS));

        // a processed spec without freeze → violates the cycle contract
        List<ArtisticGoalSpec> twoBootstraps = Arrays.asList(recipe.get(0), recipe.get(0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new VariationDriver().run(twoBootstraps, FACTS));

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new VariationDriver().run(java.util.Collections.emptyList(), FACTS));
    }

} // end class EngineTest
