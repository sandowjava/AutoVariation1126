package pkgVariationModule;

// import csurf.cleandesign.variationmodule.varyengine.model.*;
import com.csurf.variationmodule.varyengine.model.*;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;


import java.util.List;


/**
 * Step-6 verification: the cook emits the transcribed 12-row recipe —
 * row 1 = the 999-derived bootstrap spec (no freeze goal), rows 2–12 =
 * the 11 processed-case kernel sites with uniform hueSet/frozenSet/
 * valueRange bindings filled into open params.
 */
class CookTest {

    private static final int HUE_SET = 13;
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
        List<ArtisticGoalSpec> specs = new FrozenShapesCategory().cook(HUE_SET, FROZEN_SET);
        Assertions.assertEquals(VariationConstants.STANDARD_VARIATION_COUNT, specs.size());
        for (int i = 0; i < specs.size(); i++) {
            Assertions.assertEquals(i + 1, specs.get(i).getRecipeRow(), "row " + (i + 1));
            Assertions.assertTrue(specs.get(i).isFullySpecified(), "row " + (i + 1) + " fully specified");
        } // end FOR
    }

    @Test
    void row1_isBootstrapSpec_noFreezeGoal() {
        ArtisticGoalSpec row1 = new FrozenShapesCategory().cook(HUE_SET, FROZEN_SET).get(0);
        List<StratgSpec> flat = row1.flattenStratgSpecs();
        Assertions.assertEquals(3, flat.size());   // {921(4), 555(3), 255(1)} — no 031
        Assertions.assertTrue(flat.stream().noneMatch(s -> s.getStratgType() == StratgType.FREEZE_SHAPES));
        Assertions.assertTrue(row1.getPartialGoalSpecs().stream()
                .noneMatch(g -> g.getPartialGoalType() == PartialGoalType.PG1A));
    }

    @Test
    void rows2to12_carryFreezeGoal_withBoundFrozenSet() {
        List<ArtisticGoalSpec> specs = new FrozenShapesCategory().cook(HUE_SET, FROZEN_SET);
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
        List<ArtisticGoalSpec> specs = new FrozenShapesCategory().cook(HUE_SET, FROZEN_SET);
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
            Assertions.assertEquals(HUE_SET, hues.getIntParam(StratgParam.HUE_SET),
                    "row " + row + " hueSet");

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
                        new StratgSpec(StratgType.ASSIGN_ANCHOR_HUES).bind(StratgParam.HUE_SET, 20)))
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG1A).addStratgSpec(
                        new StratgSpec(StratgType.FREEZE_SHAPES).bind(StratgParam.FROZEN_SET, 5)))
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG4).addStratgSpec(
                        new StratgSpec(StratgType.ASSIGN_OST_DOTS).bind(StratgParam.VALUE_RANGE, 0.07)));

        List<ArtisticGoalSpec> specs = new FrozenShapesCategory().cook(template);
        Assertions.assertEquals(12, specs.size());

        List<StratgSpec> row12 = specs.get(11).flattenStratgSpecs();
        Assertions.assertEquals(20, find(row12, StratgType.ASSIGN_ANCHOR_HUES).getIntParam(StratgParam.HUE_SET));
        Assertions.assertEquals(5, find(row12, StratgType.FREEZE_SHAPES).getIntParam(StratgParam.FROZEN_SET));
        Assertions.assertEquals(0.07, find(row12, StratgType.ASSIGN_OST_DOTS).getDoubleParam(StratgParam.VALUE_RANGE), 1e-9);
    }

    private static StratgSpec find(List<StratgSpec> flat, StratgType type) {
        return flat.stream().filter(s -> s.getStratgType() == type).findFirst().get();
    }

    @Test
    void validationErrors_failLoudly() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FrozenShapesCategory(13));   // fixed 12-row table
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FrozenShapesCategory().cook(25, 0));   // hueSet out of range
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FrozenShapesCategory().cook(0, 6));    // frozenSet out of range
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FrozenShapesCategory().cook(new BroadCategorySpec(BroadCategoryType.FROZEN_SHAPES)));   // no bindings
    }

} // end class CookTest
