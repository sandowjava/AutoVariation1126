package com.csurf.variationmodule.varyengine.model;

import com.csurf.variationmodule.varyengine.model.stratgs.DefaultNestedRunner;
import com.csurf.variationmodule.varyengine.model.stratgs.StratgResolver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


/**
 * The single-artGoalSpec execution engine (step 7): executes one
 * {@link ArtisticGoalSpec} on a fresh {@link VariationRunState} and
 * materializes a {@link VariationResult}.
 *
 * <p>Scheduling: the artGoalSpec's StratgSpecs are flattened and executed in
 * <b>descending Stratg-ID order</b> (parent §4.4) — for the slice-1
 * rows that is 921/920 → 555 → 255 → 031: AGs, hues, dots, freeze-last.
 * PartialGoal grouping is provenance only.
 *
 * <p>Each Stratg gets a {@link BasicStratgContext} bound to the same
 * live state, the run's inputs, and a {@link DefaultNestedRunner}
 * bound to live (Context-A). The decided layer accumulates writes; the
 * shared base supplies effective values for frozen shapes at read /
 * materialize time — which is also where the end-of-run completeness
 * check runs (missing effective values name shape + channel).
 */
public final class ColorChangeEngine {

    /**
     * Bootstrap run — row 1: no shared base exists yet. Frozen-flag
     * writes without a base would fail at read time; the bootstrap
     * artGoalSpec writes no flags.
     */
    public VariationResult execute(ArtisticGoalSpec someArtGoalSpec, Facts facts) {
        Objects.requireNonNull(someArtGoalSpec, "someArtGoalSpec");
        Objects.requireNonNull(facts, "facts");
        return run(someArtGoalSpec, facts, VariationRunState.noBase(facts.numShapes()), Inputs.noBase());
    } // end method execute

    /**
     * Processed run — rows 2–12: the shared read-only base (the
     * bootstrap result) is injected; freeze-flagged shapes' effective
     * values resolve to it.
     */
    public VariationResult execute(ArtisticGoalSpec someArtGoalSpec, Facts facts, VariationResult base) {
        Objects.requireNonNull(someArtGoalSpec, "someArtGoalSpec");
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(base, "base");
        return run(someArtGoalSpec, facts,
                VariationRunState.withBase(facts.numShapes(), base), Inputs.withBase(base));
    } // end method execute

    private VariationResult run(ArtisticGoalSpec someArtGoalSpec, Facts facts,
                                VariationRunState state, Inputs inputs) {
        List<StratgSpec> flat = new ArrayList<>(someArtGoalSpec.flattenStratgSpecs());
        flat.sort(Comparator.comparingInt((StratgSpec s) -> s.getStratgType().getId()).reversed());

        for (StratgSpec stratgSpec : flat) {
            if (!stratgSpec.isFullySpecified()) {
                throw new IllegalStateException(
                        "engine refuses the open stratgSpec " + stratgSpec
                                + " — the cook binds every required param");
            } // end IF
        } // end FOR

        NestedRunner runner = DefaultNestedRunner.boundTo(facts, state, inputs);
        StratgContext ctx = BasicStratgContext.of(facts, state, inputs, runner);
        for (StratgSpec stratgSpec : flat) {
            StratgResolver.resolve(stratgSpec.getStratgType()).execute(ctx, stratgSpec);
        } // end FOR

        return state.materialize(someArtGoalSpec);   // palette + provenance; completeness check names shape+channel
    } // end method run

} // end class ColorChangeEngine
