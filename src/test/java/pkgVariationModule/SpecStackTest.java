package pkgVariationModule;

// import csurf.cleandesign.variationmodule.varyengine.model.*;
import com.csurf.variationmodule.varyengine.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;


/**
 * Step-2 verification, per Detailed Design Plan step 2:
 * completeness predicate, copy/fill round trip, spec equality.
 */
class SpecStackTest {

    // ---------- completeness predicate ----------

    @Test
    void openRequiredParamMeansNotFullySpecified() {
        StratgSpec spec = new StratgSpec(StratgType.ASSIGN_AGS); // 921 requires controlLevel
        Assertions.assertFalse(spec.isFullySpecified());

        spec.bind(StratgParam.CONTROL_LEVEL, 4);
        Assertions.assertTrue(spec.isFullySpecified());
        Assertions.assertEquals(4, spec.getIntParam(StratgParam.CONTROL_LEVEL));
    }

    @Test
    void allSlice1RequiredParamsDeclared() {
        // 920: firstPrefLevel + secondPrefLevel
        StratgSpec s920 = new StratgSpec(StratgType.ALTERNATE_AG_POOLS)
                .bind(StratgParam.FIRST_PREF_LEVEL, 4)
                .bind(StratgParam.SECOND_PREF_LEVEL, 2);
        Assertions.assertTrue(s920.isFullySpecified());

        // 555: hueSet + controlLevel (controlLevel recorded for provenance, inert in legacy)
        StratgSpec s555 = new StratgSpec(StratgType.ASSIGN_ANCHOR_HUES)
                .bind(StratgParam.HUE_SET, 0);
        Assertions.assertFalse(s555.isFullySpecified());
        s555.bind(StratgParam.CONTROL_LEVEL, 3);
        Assertions.assertTrue(s555.isFullySpecified());

        // 255: controlLevel + valueRange — recipe binds VALUE_RANGE in every row
        StratgSpec s255 = new StratgSpec(StratgType.ASSIGN_OST_DOTS)
                .bind(StratgParam.CONTROL_LEVEL, 1)
                .bind(StratgParam.VALUE_RANGE, 0.055);
        Assertions.assertTrue(s255.isFullySpecified());
        Assertions.assertEquals(0.055, s255.getDoubleParam(StratgParam.VALUE_RANGE), 0.0001);

        // 031: frozenSet
        StratgSpec s031 = new StratgSpec(StratgType.FREEZE_SHAPES)
                .bind(StratgParam.FROZEN_SET, 1);
        Assertions.assertTrue(s031.isFullySpecified());
    }

    @Test
    void paramKindsMatchSchema() {
        Assertions.assertEquals(ParamKind.INT, StratgParam.CONTROL_LEVEL.getKind());
        Assertions.assertEquals(ParamKind.DOUBLE, StratgParam.VALUE_RANGE.getKind());
        Assertions.assertEquals(ParamKind.TABLE_KEY, StratgParam.HUE_SET.getKind());
        Assertions.assertEquals(ParamKind.TABLE_KEY, StratgParam.FROZEN_SET.getKind());
    }

    // ---------- light-schema guards ----------

    @Test
    void bindingUndeclaredParamFails() {
        StratgSpec spec = new StratgSpec(StratgType.ASSIGN_AGS); // 921 declares controlLevel only
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> spec.bind(StratgParam.HUE_SET, 3));
    }

    @Test
    void bindingWrongKindFails() {
        StratgSpec spec = new StratgSpec(StratgType.ASSIGN_AGS);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> spec.bind(StratgParam.CONTROL_LEVEL, 4.0)); // double onto INT param
    }

    @Test
    void readingOpenParamFails() {
        StratgSpec spec = new StratgSpec(StratgType.ASSIGN_AGS);
        Assertions.assertThrows(IllegalStateException.class,
                () -> spec.getIntParam(StratgParam.CONTROL_LEVEL));
    }

    // ---------- phase confinement ----------

    @Test
    void stratgPhaseMustMatchGoalPhase() {
        PartialGoalSpec agGoal = new PartialGoalSpec(PartialGoalType.PG2); // AG phase
        StratgSpec hueSpec = new StratgSpec(StratgType.ASSIGN_ANCHOR_HUES); // 555 is HUE
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> agGoal.addStratgSpec(hueSpec));

        // correct phase adds fine
        agGoal.addStratgSpec(new StratgSpec(StratgType.ASSIGN_AGS));
        Assertions.assertEquals(1, agGoal.getStratgSpecs().size());
    }

    // ---------- copy / fill round trip ----------

    @Test
    void copyForCookingIsIndependentAndFillable() {
        StratgSpec open = new StratgSpec(StratgType.ASSIGN_ANCHOR_HUES)
                .bind(StratgParam.CONTROL_LEVEL, 3); // hueSet left open — the cook fills it

        StratgSpec copy = open.copyForCooking();
        Assertions.assertEquals(open, copy);
        Assertions.assertNotSame(open, copy);

        copy.bind(StratgParam.HUE_SET, 2); // fill the open param on the copy
        Assertions.assertTrue(copy.isFullySpecified());
        Assertions.assertFalse(open.isBound(StratgParam.HUE_SET)); // original untouched
        Assertions.assertNotEquals(open, copy);
    }

    @Test
    void deepCopyThroughTheStack() {
        BroadCategorySpec broad = buildFrozenShapesBroad();
        BroadCategorySpec copy = broad.copyForCooking();

        Assertions.assertEquals(broad, copy);
        Assertions.assertNotSame(broad.getPartialGoalSpecs().get(0),
                copy.getPartialGoalSpecs().get(0));
        Assertions.assertNotSame(
                broad.getPartialGoalSpecs().get(0).getStratgSpecs().get(0),
                copy.getPartialGoalSpecs().get(0).getStratgSpecs().get(0));

        // mutating the deep copy must not leak back
        copy.getPartialGoalSpecs().get(0).getStratgSpecs().get(0)
                .bind(StratgParam.CONTROL_LEVEL, 1);
        Assertions.assertNotEquals(broad, copy);
    }

    // ---------- spec equality ----------

    @Test
    void equalityIsStructural() {
        StratgSpec a = new StratgSpec(StratgType.ASSIGN_AGS).bind(StratgParam.CONTROL_LEVEL, 4);
        StratgSpec b = new StratgSpec(StratgType.ASSIGN_AGS).bind(StratgParam.CONTROL_LEVEL, 4);
        StratgSpec c = new StratgSpec(StratgType.ASSIGN_AGS).bind(StratgParam.CONTROL_LEVEL, 1);
        StratgSpec open = new StratgSpec(StratgType.ASSIGN_AGS);

        Assertions.assertEquals(a, b);
        Assertions.assertEquals(a.hashCode(), b.hashCode());
        Assertions.assertNotEquals(a, c);   // different binding
        Assertions.assertNotEquals(a, open); // bound vs. open
    }

    @Test
    void artisticGoalSpecEqualityAndFlattening() {
        ArtisticGoalSpec bootstrap = buildBootstrapSpec(1);
        ArtisticGoalSpec sameContentsDifferentRow = buildBootstrapSpec(2);

        // recipeRow is provenance, excluded from equality
        Assertions.assertEquals(bootstrap, sameContentsDifferentRow);
        Assertions.assertTrue(bootstrap.isFullySpecified());

        // flattening collects all member specs in authoring order
        Assertions.assertEquals(3, bootstrap.flattenStratgSpecs().size());
    }

    @Test
    void emptyArtisticGoalSpecRejected() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ArtisticGoalSpec(Arrays.asList()));
    }

    @Test
    void emptyPartialGoalIsNotFullySpecified() {
        PartialGoalSpec goal = new PartialGoalSpec(PartialGoalType.PG2);
        Assertions.assertFalse(goal.isFullySpecified());
    }

    // ---------- external IDs / phases ----------

    @Test
    void legacyIdsWrittenExactly() {
        Assertions.assertEquals("921", StratgType.ASSIGN_AGS.getExternalId());
        Assertions.assertEquals("920", StratgType.ALTERNATE_AG_POOLS.getExternalId());
        Assertions.assertEquals("555", StratgType.ASSIGN_ANCHOR_HUES.getExternalId());
        Assertions.assertEquals("255", StratgType.ASSIGN_OST_DOTS.getExternalId());
        Assertions.assertEquals("031", StratgType.FREEZE_SHAPES.getExternalId());
    }

    @Test
    void phasesMatchIdBands() {
        Assertions.assertEquals(Phase.AG, StratgType.ASSIGN_AGS.getPhase());       // 9xx
        Assertions.assertEquals(Phase.AG, StratgType.ALTERNATE_AG_POOLS.getPhase());
        Assertions.assertEquals(Phase.HUE, StratgType.ASSIGN_ANCHOR_HUES.getPhase()); // 5xx
        Assertions.assertEquals(Phase.COLOR, StratgType.ASSIGN_OST_DOTS.getPhase());  // 2xx
        Assertions.assertEquals(Phase.OVERRIDE, StratgType.FREEZE_SHAPES.getPhase()); // 0xx
    }

    // ---------- fixtures ----------

    /** The bootstrap spec: {921(4), 555(3, hueSet), 255(1)} — recipe row 1, no FREEZE goal. */
    private ArtisticGoalSpec buildBootstrapSpec(int recipeRow) {
        PartialGoalSpec agGoal = new PartialGoalSpec(PartialGoalType.PG2)
                .addStratgSpec(new StratgSpec(StratgType.ASSIGN_AGS)
                        .bind(StratgParam.CONTROL_LEVEL, 4));

        PartialGoalSpec hueGoal = new PartialGoalSpec(PartialGoalType.PG3)
                .addStratgSpec(new StratgSpec(StratgType.ASSIGN_ANCHOR_HUES)
                        .bind(StratgParam.CONTROL_LEVEL, 3)
                        .bind(StratgParam.HUE_SET, 0));

        PartialGoalSpec colorGoal = new PartialGoalSpec(PartialGoalType.PG4)
                .addStratgSpec(new StratgSpec(StratgType.ASSIGN_OST_DOTS)
                        .bind(StratgParam.CONTROL_LEVEL, 1)
                        .bind(StratgParam.VALUE_RANGE, 0.055));

        return new ArtisticGoalSpec(Arrays.asList(agGoal, hueGoal, colorGoal), recipeRow);
    }

    private BroadCategorySpec buildFrozenShapesBroad() {
        return new BroadCategorySpec(BroadCategoryType.FROZEN_SHAPES)
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG2)
                        .addStratgSpec(new StratgSpec(StratgType.ASSIGN_AGS)
                                .bind(StratgParam.CONTROL_LEVEL, 4)))
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG3)
                        .addStratgSpec(new StratgSpec(StratgType.ASSIGN_ANCHOR_HUES)
                                .bind(StratgParam.CONTROL_LEVEL, 3)))
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG4)
                        .addStratgSpec(new StratgSpec(StratgType.ASSIGN_OST_DOTS)
                                .bind(StratgParam.CONTROL_LEVEL, 1)))
                .addPartialGoalSpec(new PartialGoalSpec(PartialGoalType.PG1A)
                        .addStratgSpec(new StratgSpec(StratgType.FREEZE_SHAPES)
                                .bind(StratgParam.FROZEN_SET, 1)));
    }

} // end class SpecStackTest
