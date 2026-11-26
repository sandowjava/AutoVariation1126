package com.csurf.variationmodule.varyengine.model.stratgs;

import com.csurf.variationmodule.varyengine.model.*;

import java.util.Set;


/**
 * Stratg 031 — writes {@code frozen} flags for the shapes named by its
 * params. The shapes are named one of two ways — exactly one must be bound:
 * {@code frozenSet} (a TABLE_KEY index 0–5 into {@link FrozenSets};
 * in legacy this was the controlLevel indexing
 * {@code arrayofSetsOfFrozenDrawObjs}) or {@code frozenShapes} (a SHAPE_SET
 * of explicit 1-based shape IDs — the userSelectFrozen path).
 *
 * <p>Touches nothing else: the flag covers all three channels and
 * {@code effective} resolves against the base at read time — matching
 * legacy's frozen-shape reinstatement without copying any values
 * (parent §4.2 flag + base-lookup representation).
 *
 * <p>Writes flags directly through {@link RunState#setFrozen} — it is
 * an ordinary engine-level Stratg, not {@code PureCore}-eligible and
 * <b>not nestable</b> (D4: flag writes cannot be harvested; a nested
 * run would throw {@code RecordingRunState.FLAG_WRITE_ERROR}).
 */
public final class FreezeShapesStratg implements Stratg {

    @Override
    public StratgType type() {
        return StratgType.FREEZE_SHAPES;
    } // end method type

    @Override
    public void execute(StratgContext ctx, StratgSpec params) {
        StratgSupport.requireType(params, type());

        // exactly one of the two alternate params names the frozen shapes
        // (isFullySpecified is requiredness-only and cannot express the XOR)
        boolean hasTable = params.isBound(StratgParam.FROZEN_SET);
        boolean hasSet = params.isBound(StratgParam.FROZEN_SHAPES);
        if (hasTable == hasSet) {
            throw new IllegalStateException(
                    "031: exactly one of frozenSet / frozenShapes must be bound"
                            + " (frozenSet bound=" + hasTable
                            + ", frozenShapes bound=" + hasSet + ")");
        } // end IF
        Set<Integer> frozenSet = hasTable
                ? FrozenSets.get(params.getIntParam(StratgParam.FROZEN_SET))
                : params.getIntSetParam(StratgParam.FROZEN_SHAPES);
        RunState state = ctx.runState();

        for (int shape : frozenSet) {
            if (!state.shapeIds().contains(shape)) {
                throw new IllegalArgumentException(
                        "031: frozenSet references shape " + shape
                                + " which is outside this run " + state.shapeIds());
            } // end IF
            state.setFrozen(shape, true);
        } // end FOR
    } // end method execute

} // end class FreezeShapesStratg
