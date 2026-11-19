package csurf.cleandesign.variationmodule.varyengine.model.stratgs;

import csurf.cleandesign.variationmodule.varyengine.model.FrozenSets;
import csurf.cleandesign.variationmodule.varyengine.model.RunState;
import csurf.cleandesign.variationmodule.varyengine.model.Stratg;
import csurf.cleandesign.variationmodule.varyengine.model.StratgContext;
import csurf.cleandesign.variationmodule.varyengine.model.StratgParam;
import csurf.cleandesign.variationmodule.varyengine.model.StratgSpec;
import csurf.cleandesign.variationmodule.varyengine.model.StratgType;

import java.util.Set;


/**
 * Stratg 031 — writes {@code frozen} flags for the shapes named by its
 * {@code frozenSet} param (a TABLE_KEY index 0–5 into {@link FrozenSets};
 * in legacy this was the controlLevel indexing
 * {@code arrayofSetsOfFrozenDrawObjs}).
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
        Set<Integer> frozenSet = FrozenSets.get(params.getIntParam(StratgParam.FROZEN_SET));
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
