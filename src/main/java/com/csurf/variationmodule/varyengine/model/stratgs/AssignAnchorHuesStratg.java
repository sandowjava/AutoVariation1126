package csurf.cleandesign.variationmodule.varyengine.model.stratgs;

import csurf.cleandesign.variationmodule.varyengine.model.AnchorHueSet;
import csurf.cleandesign.variationmodule.varyengine.model.AnchorHueSets;
import csurf.cleandesign.variationmodule.varyengine.model.RunState;
import csurf.cleandesign.variationmodule.varyengine.model.Stratg;
import csurf.cleandesign.variationmodule.varyengine.model.StratgContext;
import csurf.cleandesign.variationmodule.varyengine.model.StratgParam;
import csurf.cleandesign.variationmodule.varyengine.model.StratgSpec;
import csurf.cleandesign.variationmodule.varyengine.model.StratgType;

import java.util.Optional;


/**
 * Stratg 555 — decided AG → that AG's anchor hue from the selected
 * {@code hueSet} row of {@link AnchorHueSets} (OQ4: {@code hueSet} is
 * the aux-control kernel param indexing {@code AnchorHuesForAGsSet};
 * {@code controlLevel} is vestigial — bound for provenance, inert).
 *
 * <p>Reads the decided layer; skips shapes whose AG is still
 * {@code UNDECIDED} (rule 2 — the end-of-run completeness check names
 * any leftover gap). A decided AG outside 1–4 is a bug, not a skip:
 * {@link AnchorHueSet#anchorHueForAG} throws.
 *
 * <p>Not {@code PureCore}-eligible: it depends on decided AGs (D5).
 * Decides every shape legacy decided — slice-1 Stratgs write all
 * shapes including frozen ones (provenance parity).
 */
public final class AssignAnchorHuesStratg implements Stratg {

    @Override
    public StratgType type() {
        return StratgType.ASSIGN_ANCHOR_HUES;
    } // end method type

    @Override
    public void execute(StratgContext ctx, StratgSpec params) {
        StratgSupport.requireType(params, type());
        AnchorHueSet hueSet = AnchorHueSets.get(params.getIntParam(StratgParam.HUE_SET));

        RunState state = ctx.runState();
        for (int shape : state.shapeIds()) {
            Optional<Integer> ag = state.ag(shape);
            if (ag.isPresent()) {
                state.setHue(shape, hueSet.anchorHueForAG(ag.get()));
            } // end IF — UNDECIDED AG → skip, stays UNDECIDED (rule 2)
        } // end FOR
    } // end method execute

} // end class AssignAnchorHuesStratg
