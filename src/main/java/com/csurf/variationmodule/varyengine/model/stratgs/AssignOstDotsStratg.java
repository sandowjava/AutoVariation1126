package com.csurf.variationmodule.varyengine.model.stratgs;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.variationmodule.varyengine.model.*;

import java.util.List;
import java.util.Optional;


/**
 * Stratg 255 — decided hue → {@code ColorPtOstDot} closest to the
 * shape's authored {@code valueTarget}.
 *
 * <p>Per shape: {@code MonotriBook.getOstDotsByValueRange(hue, target,
 * ±valueRange)} then <b>index 0</b> — the first candidate in scan order
 * (fill orders 1–66 via {@code LAST_NON_GRAY_FILL_ORDER}), exactly the
 * legacy {@code candidate[1]} pick ("first color in the set of
 * candidates"; {@code controlLevel} is inert, recorded for provenance).
 *
 * <p>Skips shapes whose hue is {@code UNDECIDED} (rule 2). An
 * <b>empty candidate set is an explicit error</b> naming shape + hue +
 * target (step-5 OQ5 recommendation — legacy NPE'd on {@code [1]};
 * parity argues error, legacy had no fallback).
 *
 * <p>Not {@code PureCore}-eligible: it depends on decided hues (D5).
 * The catalog is an immutable global table — a legitimate read under
 * the D5 "facts, params, and immutable global tables" rule, but the
 * decided-hue dependency rules 255 out anyway.
 */
public final class AssignOstDotsStratg implements Stratg {

    @Override
    public StratgType type() {
        return StratgType.ASSIGN_OST_DOTS;
    } // end method type

    @Override
    public void execute(StratgContext ctx, StratgSpec params) {
        StratgSupport.requireType(params, type());
        double range = params.getDoubleParam(StratgParam.VALUE_RANGE);
        Facts facts = ctx.facts();
        RunState state = ctx.runState();

        for (int shape : state.shapeIds()) {
            Optional<HueNumGUI> hue = state.hue(shape);
            if (hue.isEmpty()) {
                continue;   // UNDECIDED hue → skip, stays UNDECIDED (rule 2)
            } // end IF

            double target = facts.valueTarget(shape);
            List<ColorPtOstDot> candidates = MonotriBook.getInstance()
                    .getOstDotsByValueRange(hue.get(), target, range);
            if (candidates.isEmpty()) {
                throw new IllegalStateException(
                        "255: empty candidate set for shape " + shape + " (" + facts.shapeName(shape)
                                + "), hue " + hue.get().getUserFacingHueNum()
                                + ", target " + target + " ± " + range);
            } // end IF
            state.setOstDot(shape, candidates.get(0));   // legacy candidate[1] — first in scan order
        } // end FOR
    } // end method execute

} // end class AssignOstDotsStratg
