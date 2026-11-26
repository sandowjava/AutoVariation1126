package pkgVariationModule;

// import csurf.cleandesign.variationmodule.varyengine.model.*;
import com.csurf.variationmodule.varyengine.model.*;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.Set;


/**
 * Step-6 verification: the cook emits the transcribed 12-row recipe —
 * row 1 = the 999-derived bootstrap spec (no freeze goal), rows 2–12 =
 * the 11 processed-case kernel sites with uniform agHuePattern/frozenSet/
 * valueRange bindings filled into open params.
 */
class CookTest {

    private static final String AGHUE_PATTERN = "05-03-06-08-legacyTableRowNum13";
    private static final int FROZEN_SET = 2;

    // row 1..12 → expected AG stratg + bindings: [type, first, second]
    private static final Object[][] EXPECTED_AG = {
            {StratgType.ASSIGN_AGS, 4, -1},
            {StratgType.ASSIGN_AGS, 2, -1},
            {StratgType.ASSIGN_AGS, 3, -1},
            {StratgType.ASSIGN_AGS, 1, -1},
            {StratgType.ALTERNATE_AG_POOLS, 1, 2},
            {StratgType.ALTERNATE_AG_POOLS, 2, 3},
            {StratgType.ALTERNATE_AG_POOLS, 3, 4},
            {StratgType.ALTERNATE_AG_POOLS, 4, 1},
            {StratgType.ALTERNATE_AG_POOLS, 1, 3},
            {StratgType.ALTERNATE_AG_POOLS, 2, 4},
            {StratgType.ALTERNATE_AG_POOLS, 3, 1},
            {StratgType.ALTERNATE_AG_POOLS, 4, 2},
    };

    // row 1..12 → expected 555 controlLevel (vestigial; recorded for provenance)
    private static final int[] EXPECTED_555_CTRL = {3, 2, 3, 4, 2, 2, 2, 2, 2, 2, 2, 2};

    @Test
    void cookProducesTwelveFullySpecifiedSpecs() {
        List<ArtisticGoalSpec> specs = new FrozenShapesCategory().cook(AGHUE_PATTERN, FROZEN_SET);
        Assertions.assertEquals(VariationConstants.STANDARD_VARIATION_COUNT, specs.size());
        for (int i = 0; i < specs.size(); i++) {
            Assertions.assertEquals(i + 1, specs.get(i).getRecipeRow(), "row " + (i + 1));
            Assertions.assertTrue(specs.get(i).isFullySpecified(), "row " + (i + 1) + " fully specified");
        } // end FOR
    }

    @Test
    void row1_isBootstrapSpec_noFreezeGoal() {
        ArtisticGoalSpec row1 = new FrozenShapesCategory().cook(AGHUE_PATTERN, FROZEN_SET).get(0);
        List<StratgSpec> flat = row1.flattenStratgSpecs();
        Assertions.assertEquals(3, flat.size());   // {921(4), 555(3), 255(1)} — no 031
        Assertions.assertTrue(flat.stream().noneMatch(s -> s.getStratgType() == StratgType.FREEZE_SHAPES));
        Assertions.assertTrue(row1.getPartialGoalSpecs().stream()
                .noneMatch(g -> g.getPartialGoalType() == PartialGoalType.PG1A));
    }

    @Test
    void rows2to12_carryFreezeGoal_withBoundFrozenSet() {
        List<ArtisticGoalSpec> specs = new FrozenShapesCategory().cook(AGHUE_PATTERN, FROZEN_SET);
        for (int i = 1; i < specs.size(); i++) {
            List<StratgSpec> flat = specs.get(i).flattenStratgSpecs();
            Assertions.assertEquals(4, flat.size(), "row " + (i + 1));
            StratgSpec freeze = flat.stream()
                    .filter(s -> s.getStratgType() == StratgType.FREEZE_SHAPES).findFirst().get();
            Assertions.assertEquals(FROZEN_SET, freeze.getIntParam(StratgParam.FROZEN_SET),
                    "row " + (i + 1) + " frozenSet");
        } // end FOR
    }

    @Test
    void recipeTable_agStratgsAnd555Controls() {
        List<ArtisticGoalSpec> specs = new FrozenShapesCategory().cook(AGHUE_PATTERN, FROZEN_SET);
        for (int i = 0; i < specs.size(); i++) {
            int row = i + 1;
            List<StratgSpec> flat = specs.get(i).flattenStratgSpecs();

            StratgSpec ag = flat.stream()
                    .filter(s -> s.getStratgType() == StratgType.ASSIGN_AGS
                            || s.getStratgType() == StratgType.ALTERNATE_AG_POOLS).findFirst().get();
            Assertions.assertEquals(EXPECTED_AG[i][0], ag.getStratgType(), "row " + row + " AG type");
            if (ag.getStratgType() == StratgType.ASSIGN_AGS) {
                Assertions.assertEquals(EXPECTED_AG[i][1], ag.getIntParam(StratgParam.CONTROL_LEVEL),
                        "row " + row + " 921 level");
            } else {
                Assertions.assertEquals(EXPECTED_AG[i][1], ag.getIntParam(StratgParam.FIRST_PREF_LEVEL),
                        "row " + row + " 920 first");
                Assertions.assertEquals(EXPECTED_AG[i][2], ag.getIntParam(StratgParam.SECOND_PREF_LEVEL),
                        "row " + row + " 920 second");
            } // end IF-ELSE

            StratgSpec hues = flat.stream()
                    .filter(s -> s.getStratgType() == StratgType.ASSIGN_ANCHOR_HUES).findFirst().get();
            Assertions.assertEquals(EXPECTED_555_CTRL[i], hues.getIntParam(StratgParam.CONTROL_LEVEL),
                    "row " + row + " 555 control");
            Assertions.assertEquals(AGHUE_PATTERN, hues.getStringParam(StratgParam.AGHUE_PATTERN),
                    "row " + row + " agHuePattern");

            StratgSpec dots = flat.stream()
                    .filter(s -> s.getStratgType() == StratgType.ASSIGN_OST_DOTS).findFirst().get();
            Assertions.assertEquals(1, dots.getIntParam(StratgParam.CONTROL_LEVEL), "row " + row + " 255 control");
            Assertions.assertEquals(VariationConstants.VALUE_RANGE, dots.getDoubleParam(StratgParam.VALUE_RANGE),
                    1e-9, "row " + row + " valueRange");
        } // end FOR
    }

    @Test
    void cookFromTemplate_extractsDialogBindings() {
        BroadCategorySpec template = new BroadCategorySpec(BroadCategoryType.FROZEN_SHAPES)
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG3).addStratgSpec(
                        new StratgSpec(StratgType.ASSIGN_ANCHOR_HUES)
                                .bind(StratgParam.AGHUE_PATTERN, "08-02-04-09-legacyTableRowNum20")))
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG1A).addStratgSpec(
                        new StratgSpec(StratgType.FREEZE_SHAPES).bind(StratgParam.FROZEN_SET, 5)))
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG4).addStratgSpec(
                        new StratgSpec(StratgType.ASSIGN_OST_DOTS).bind(StratgParam.VALUE_RANGE, 0.07)));

        List<ArtisticGoalSpec> specs = new FrozenShapesCategory().cook(template);
        Assertions.assertEquals(12, specs.size());

        List<StratgSpec> row12 = specs.get(11).flattenStratgSpecs();
        Assertions.assertEquals("08-02-04-09-legacyTableRowNum20",
                find(row12, StratgType.ASSIGN_ANCHOR_HUES).getStringParam(StratgParam.AGHUE_PATTERN));
        Assertions.assertEquals(5, find(row12, StratgType.FREEZE_SHAPES).getIntParam(StratgParam.FROZEN_SET));
        Assertions.assertEquals(0.07, find(row12, StratgType.ASSIGN_OST_DOTS).getDoubleParam(StratgParam.VALUE_RANGE), 1e-9);
    }

    private static StratgSpec find(List<StratgSpec> flat, StratgType type) {
        return flat.stream().filter(s -> s.getStratgType() == type).findFirst().get();
    }

    // ---------- userSelectFrozen: cook with an explicit shape set ----------

    private static final Set<Integer> USER_FROZEN = Set.of(3, 5, 12);

    @Test
    void cookWithUserFrozenSet_rows2to12_carryFrozenShapes() {
        List<ArtisticGoalSpec> specs = new FrozenShapesCategory().cook(AGHUE_PATTERN, USER_FROZEN);
        Assertions.assertEquals(VariationConstants.STANDARD_VARIATION_COUNT, specs.size());
        for (int i = 0; i < specs.size(); i++) {
            Assertions.assertEquals(i + 1, specs.get(i).getRecipeRow(), "row " + (i + 1));
            Assertions.assertTrue(specs.get(i).isFullySpecified(), "row " + (i + 1) + " fully specified");
        } // end FOR

        // row 1 = bootstrap — no freeze goal at all
        Assertions.assertTrue(specs.get(0).flattenStratgSpecs().stream()
                .noneMatch(s -> s.getStratgType() == StratgType.FREEZE_SHAPES));

        for (int i = 1; i < specs.size(); i++) {
            StratgSpec freeze = find(specs.get(i).flattenStratgSpecs(), StratgType.FREEZE_SHAPES);
            Assertions.assertFalse(freeze.isBound(StratgParam.FROZEN_SET),
                    "row " + (i + 1) + " must not bind frozenSet on the userSet path");
            Assertions.assertEquals(USER_FROZEN, freeze.getIntSetParam(StratgParam.FROZEN_SHAPES),
                    "row " + (i + 1) + " frozenShapes");
        } // end FOR
    }

    @Test
    void cookWithUserFrozenSet_nullSetFailsLoudly() {
        Assertions.assertThrows(NullPointerException.class,
                () -> new FrozenShapesCategory().cook(AGHUE_PATTERN, (Set<Integer>) null));
    }

    @Test
    void cookFromTemplate_extractsFrozenShapesBinding() {
        BroadCategorySpec template = new BroadCategorySpec(BroadCategoryType.FROZEN_SHAPES)
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG3).addStratgSpec(
                        new StratgSpec(StratgType.ASSIGN_ANCHOR_HUES)
                                .bind(StratgParam.AGHUE_PATTERN, "08-02-04-09-legacyTableRowNum20")))
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG1A).addStratgSpec(
                        new StratgSpec(StratgType.FREEZE_SHAPES)
                                .bind(StratgParam.FROZEN_SHAPES, USER_FROZEN)));

        List<ArtisticGoalSpec> specs = new FrozenShapesCategory().cook(template);
        Assertions.assertEquals(12, specs.size());
        Assertions.assertEquals(USER_FROZEN,
                find(specs.get(11).flattenStratgSpecs(), StratgType.FREEZE_SHAPES)
                        .getIntSetParam(StratgParam.FROZEN_SHAPES));
    }

    @Test
    void cookFromTemplate_freezeSpecBindsNeitherParamFailsLoudly() {
        // an 031 spec with neither frozenSet nor frozenShapes bound — the XOR check must fire
        BroadCategorySpec template = new BroadCategorySpec(BroadCategoryType.FROZEN_SHAPES)
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG3).addStratgSpec(
                        new StratgSpec(StratgType.ASSIGN_ANCHOR_HUES)
                                .bind(StratgParam.AGHUE_PATTERN, "08-02-04-09-legacyTableRowNum20")))
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG1A).addStratgSpec(
                        new StratgSpec(StratgType.FREEZE_SHAPES)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FrozenShapesCategory().cook(template));
    }

    @Test
    void validationErrors_failLoudly() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FrozenShapesCategory(13));   // fixed 12-row table
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FrozenShapesCategory().cook("no-such-pattern", 0));   // unknown pattern name
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FrozenShapesCategory().cook(AGHUE_PATTERN, 6));    // frozenSet out of range
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FrozenShapesCategory().cook(new BroadCategorySpec(BroadCategoryType.FROZEN_SHAPES)));   // no bindings
    }

} // end class CookTest
