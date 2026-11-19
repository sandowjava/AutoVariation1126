package pkgVariationModule;

// import csurf.cleandesign.variationmodule.varyengine.model.*;
import com.csurf.variationmodule.varyengine.model.*;
// import csurf.cleandesign.variationmodule.varyengine.model.stratgs.AlternateAGPoolsStratg;
import com.csurf.variationmodule.varyengine.model.stratgs.AlternateAGPoolsStratg;
// import csurf.cleandesign.variationmodule.varyengine.model.stratgs.AssignAGsStratg;
import com.csurf.variationmodule.varyengine.model.stratgs.AssignAGsStratg;
// import csurf.cleandesign.variationmodule.varyengine.model.stratgs.AssignAnchorHuesStratg;
import com.csurf.variationmodule.varyengine.model.stratgs.AssignAnchorHuesStratg;
// import csurf.cleandesign.variationmodule.varyengine.model.stratgs.DefaultNestedRunner;
import com.csurf.variationmodule.varyengine.model.stratgs.DefaultNestedRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;


/**
 * Step-5b equivalence harness (per the nested-execution decisions doc):
 * PureCore fast path vs. forced scratch path, seeded-dependent inner
 * runs, frozen-flag inertness, coverage/masking behavior, the nested
 * flag-write error, and 920 end-to-end alternation.
 */
class NestedExecutionTest {

    private static final Facts FACTS = AuthoredFactsTable.instance();

    private static StratgSpec spec(StratgType type) {
        return new StratgSpec(type);
    }

    private static StratgSpec spec921(int level) {
        return spec(StratgType.ASSIGN_AGS).bind(StratgParam.CONTROL_LEVEL, level);
    }

    private static StratgSpec spec555(int hueSet) {
        return spec(StratgType.ASSIGN_ANCHOR_HUES)
                .bind(StratgParam.CONTROL_LEVEL, 1).bind(StratgParam.HUE_SET, hueSet);
    }

    private static VariationRunState liveStateWithAGs() {
        VariationRunState live = VariationRunState.noBase(FACTS.numShapes());
        for (int shape : FACTS.shapeIds()) {
            live.setAG(shape, FACTS.defaultAG(shape));
        } // end FOR
        return live;
    }

    // ---------- differential: fast path vs. forced scratch path ----------

    @Test
    void pureCore_fastAndScratchPathsEquivalentAcrossSeeds() {
        VariationRunState live = liveStateWithAGs();   // decided AGs exist for seeding
        live.setFrozen(3, true);                        // flags inert for cores

        DefaultNestedRunner fast = DefaultNestedRunner.boundTo(FACTS, live, Inputs.noBase());
        DefaultNestedRunner scratch = DefaultNestedRunner.boundForcingScratch(FACTS, live, Inputs.noBase());

        StratgSpec spec = spec921(4);
        for (Seed seed : new Seed[]{Seed.blank(), Seed.of(Channel.AG), Seed.all()}) {
            Assertions.assertEquals(fast.run(spec, seed), scratch.run(spec, seed),
                    "fast vs scratch under seed " + seed.channels());
        } // end FOR
    }

    @Test
    void pureCore_proposalCarriesDecidedAGs_regardlessOfFlags() {
        VariationRunState live = liveStateWithAGs();
        live.setFrozen(1, true);
        live.setFrozen(2, true);

        Proposal p = DefaultNestedRunner.boundTo(FACTS, live, Inputs.noBase()).run(spec921(4));
        Assertions.assertEquals(FACTS.numShapes(),
                p.writtenShapes(Channel.AG).size());   // frozen flags are never harvested
        Assertions.assertEquals(FACTS.defaultAG(13), p.ag(13).get().intValue());
    }

    // ---------- scratch path: dependent inner, seeding ----------

    @Test
    void dependentInner_blankSeedFailsUnderRequireFull() {
        VariationRunState live = liveStateWithAGs();
        Proposal empty = DefaultNestedRunner.boundTo(FACTS, live, Inputs.noBase())
                .run(spec555(0));   // blank seed → no decided AGs in scratch → nothing written

        Assertions.assertTrue(empty.isEmpty());
        // the outer stratg consumes this via REQUIRE_FULL apply → fails naming channels
        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> live.apply(empty, live.shapeIds(), EnumSet.of(Channel.HUE), Coverage.REQUIRE_FULL));
        Assertions.assertTrue(thrown.getMessage().contains("HUE@1"));
    }

    @Test
    void dependentInner_seededWithAG_matchesLiveChain() {
        VariationRunState live = liveStateWithAGs();
        // live chain: the same 555 run directly on live (its decided AGs are the seed source)
        VariationRunState chained = liveStateWithAGs();
        BasicStratgContext ctx = BasicStratgContext.withoutRunner(FACTS, chained, Inputs.noBase());
        new AssignAnchorHuesStratg()
                .execute(ctx, spec555(0));

        Proposal nested = DefaultNestedRunner.boundTo(FACTS, live, Inputs.noBase())
                .run(spec555(0), Seed.of(Channel.AG));

        // the proposal equals what the live-chain 555 wrote — and live AGs were never touched
        Assertions.assertEquals(FACTS.numShapes(), nested.writtenShapes(Channel.HUE).size());
        for (int shape : FACTS.shapeIds()) {
            Assertions.assertEquals(chained.hue(shape).get(), nested.hue(shape).get(),
                    "nested hue shape " + shape);
        } // end FOR
        for (int shape : FACTS.shapeIds()) {
            Assertions.assertFalse(live.hue(shape).isPresent());   // scratch isolation
        } // end FOR
    }

    // ---------- 920 end-to-end ----------

    @Test
    void alternateAGPools_endToEndAlternation() {
        VariationRunState live = VariationRunState.noBase(FACTS.numShapes());
        DefaultNestedRunner runner = DefaultNestedRunner.boundTo(FACTS, live, Inputs.noBase());

        StratgSpec spec = spec(StratgType.ALTERNATE_AG_POOLS)
                .bind(StratgParam.FIRST_PREF_LEVEL, 4)     // odd shapes → defaultAG pool
                .bind(StratgParam.SECOND_PREF_LEVEL, 2);   // even shapes → midMin pool

        new AlternateAGPoolsStratg().execute(
                BasicStratgContext.of(FACTS, live, Inputs.noBase(), runner), spec);

        Proposal oddPool = new AssignAGsStratg()
                .compute(FACTS, spec921(4));
        Proposal evenPool = new AssignAGsStratg()
                .compute(FACTS, spec921(2));

        for (int shape : FACTS.shapeIds()) {
            int expected = (shape % 2 == 1 ? oddPool : evenPool).ag(shape).get().intValue();
            Assertions.assertEquals(expected, live.ag(shape).get().intValue(),
                    "shape " + shape + " parity pool");
        } // end FOR
    }

    // ---------- runner guards ----------

    @Test
    void runRejectsNotFullySpecifiedSpec() {
        VariationRunState live = VariationRunState.noBase(FACTS.numShapes());
        DefaultNestedRunner runner = DefaultNestedRunner.boundTo(FACTS, live, Inputs.noBase());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> runner.run(spec(StratgType.ASSIGN_AGS)));   // controlLevel open
    }

    @Test
    void nestedFlagWriteFailsLoudly() {
        VariationRunState live = VariationRunState.noBase(FACTS.numShapes());
        DefaultNestedRunner runner = DefaultNestedRunner.boundTo(FACTS, live, Inputs.noBase());

        StratgSpec freezeSpec = spec(StratgType.FREEZE_SHAPES).bind(StratgParam.FROZEN_SET, 0);
        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> runner.run(freezeSpec, Seed.blank()));
        Assertions.assertEquals(RecordingRunState.FLAG_WRITE_ERROR, thrown.getMessage());
    }

} // end class NestedExecutionTest
