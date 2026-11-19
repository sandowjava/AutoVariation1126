package com.csurf.variationmodule.varyengine.model.stratgs;

import com.csurf.variationmodule.varyengine.model.*;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Stratg 920 — "hopskotch" AG alternation: obtains two AG pools by
 * running 921 twice through the nested runner (blank seed — 921's
 * {@code PureCore} fast path, so no scratch is even built), then
 * writes pool 1 to odd-indexed shapes and pool 2 to even-indexed
 * shapes via two parity-scoped {@code apply} calls ({@code {AG}},
 * {@code REQUIRE_FULL}).
 *
 * <p>Faithful to legacy {@code StratgForSetAGsPG2v920}: pool 1 =
 * {@code firstPrefLevel} (legacy controlNum), pool 2 =
 * {@code secondPrefLevel} (legacy auxNum); {@code i % 2 == 1} →
 * {@code firstAGPool[i]}, else {@code secondAGPool[i]}. The legacy
 * shared-state hazard — the second 921 overwriting the first's writes
 * on the live roster — disappears: pools are immutable proposals.
 *
 * <p>This Stratg is itself not {@code PureCore}-eligible (it reads the
 * runner/context) and is never nested inside another Stratg in slice 1.
 */
public final class AlternateAGPoolsStratg implements Stratg {

    @Override
    public StratgType type() {
        return StratgType.ALTERNATE_AG_POOLS;
    } // end method type

    @Override
    public void execute(StratgContext ctx, StratgSpec params) {
        StratgSupport.requireType(params, type());
        int firstLevel = params.getIntParam(StratgParam.FIRST_PREF_LEVEL);
        int secondLevel = params.getIntParam(StratgParam.SECOND_PREF_LEVEL);

        // the two pools — each inner 921 spec is fully specified by construction
        Proposal firstPool = ctx.nested().run(innerSpec(firstLevel));
        Proposal secondPool = ctx.nested().run(innerSpec(secondLevel));

        RunState state = ctx.runState();
        Set<Integer> oddShapes = new LinkedHashSet<>();
        Set<Integer> evenShapes = new LinkedHashSet<>();
        for (int shape : state.shapeIds()) {
            (shape % 2 == 1 ? oddShapes : evenShapes).add(shape);
        } // end FOR

        state.apply(firstPool, oddShapes, EnumSet.of(Channel.AG), Coverage.REQUIRE_FULL);
        state.apply(secondPool, evenShapes, EnumSet.of(Channel.AG), Coverage.REQUIRE_FULL);
    } // end method execute

    private static StratgSpec innerSpec(int prefLevel) {
        return new StratgSpec(StratgType.ASSIGN_AGS).bind(StratgParam.CONTROL_LEVEL, prefLevel);
    } // end method innerSpec

} // end class AlternateAGPoolsStratg
