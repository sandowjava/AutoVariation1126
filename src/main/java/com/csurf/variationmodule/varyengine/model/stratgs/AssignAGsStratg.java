package com.csurf.variationmodule.varyengine.model.stratgs;

import com.csurf.variationmodule.varyengine.model.*;

import java.util.EnumSet;


/**
 * Stratg 921 — per-shape AG assignment by preference level.
 * Faithful reimplementation of legacy
 * {@code StratgForSetAGsPG2v921.assignAGsToSmartObjs} and the
 * {@code DrawObjSmart2} pref accessors it called.
 *
 * <p><b>PureCore-eligible</b> (D5): reads only facts and params. The
 * shell delegates — {@code execute} is
 * {@code apply(compute(facts, params), scope, {AG}, REQUIRE_FULL)} — so
 * there is a single implementation, no parallel path to drift (§2.7).
 *
 * <p>Level semantics (legacy switch, replicated verbatim):
 * <ul>
 *   <li>4 → authored {@code defaultAG} ({@code prefsAmongAGs[0]} — the
 *       misnamed {@code getMaxPreferredAG}; no argmax, OQ9)</li>
 *   <li>3 → midMax: largest weight {@code < prefs[defaultAG]} and
 *       {@code > prefs[minPref]}; equal-value ties keep the LATER index;
 *       fallback index 1</li>
 *   <li>2 → midMin: smallest weight {@code > prefs[minPref]} and
 *       {@code < prefs[defaultAG]}; fallback index 1 (legacy's second
 *       branch assigns a dead variable — no effect)</li>
 *   <li>1 → min: smallest POSITIVE weight (0 = "never use"); last index
 *       wins on equal ties; fallback index 1</li>
 * </ul>
 * Legacy printed an error and left 0 for other levels; the clean version
 * fails loudly instead (fail-fast over silent UNDECIDED).
 */
public final class AssignAGsStratg implements Stratg, PureCore {

    @Override
    public StratgType type() {
        return StratgType.ASSIGN_AGS;
    } // end method type

    @Override
    public Proposal compute(Facts facts, StratgSpec params) {
        StratgSupport.requireType(params, type());
        int level = params.getIntParam(StratgParam.CONTROL_LEVEL);
        if (level < 1 || level > 4) {
            throw new IllegalArgumentException(
                    "921 controlLevel " + level + " out of range 1..4");
        } // end IF
        Proposal.Builder proposal = Proposal.builder();
        for (int shape : facts.shapeIds()) {
            proposal.ag(shape, agForLevel(facts, shape, level));
        } // end FOR
        return proposal.build();
    } // end method compute

    @Override
    public void execute(StratgContext ctx, StratgSpec params) {
        ctx.runState().apply(compute(ctx.facts(), params), ctx.runState().shapeIds(),
                EnumSet.of(Channel.AG), Coverage.REQUIRE_FULL);
    } // end method execute

    // ---------- the extracted legacy core ----------

    /** Legacy {@code assignAGsToSmartObjs} level switch. */
    static int agForLevel(Facts facts, int shape, int level) {
        switch (level) {
            case 4:
                return facts.defaultAG(shape);
            case 3:
                return midMaxPreferredAG(facts, shape);
            case 2:
                return midMinPreferredAG(facts, shape);
            case 1:
                return minPreferredAG(facts, shape);
            default:
                throw new IllegalArgumentException("921 controlLevel " + level + " out of range 1..4");
        } // end SWITCH
    } // end method agForLevel

    /**
     * Legacy {@code getMinPreferredAG}: smallest POSITIVE pref weight —
     * a zero weight means "never use this AG". An equal positive value
     * supersedes (later index wins) because the comparison is {@code <=}.
     */
    static int minPreferredAG(Facts facts, int shape) {
        int idx = 1;
        double best = facts.agPref(shape, 1);
        double midMin = best;
        for (int ag = 2; ag <= 4; ag++) {
            double w = facts.agPref(shape, ag);
            if (w <= best && w > 0.0) {
                midMin = best;
                best = w;
                idx = ag;
            } else if (w <= midMin && w > 0.0) {
                midMin = w;
            } // end IF-ELSE
        } // end FOR
        return idx;
    } // end method minPreferredAG

    /**
     * Legacy {@code getMidMinPreferredAG}: first index whose weight is
     * strictly {@code > prefs[minPref]} and strictly {@code < prefs[defaultAG]},
     * scanning low — the running bound drops as hits are found.
     * Fallback: index 1. Legacy's {@code else} branch writes a dead
     * variable; it has no effect and is not replicated.
     */
    static int midMinPreferredAG(Facts facts, int shape) {
        int idx = 1;
        double floor = facts.agPref(shape, minPreferredAG(facts, shape));
        double bound = facts.agPref(shape, facts.defaultAG(shape));
        for (int ag = 1; ag <= 4; ag++) {
            double w = facts.agPref(shape, ag);
            if (w < bound && w > floor) {
                bound = w;
                idx = ag;
            } // end IF
        } // end FOR
        return idx;
    } // end method midMinPreferredAG

    /**
     * Legacy {@code getMidMaxPreferredAG}: largest weight strictly
     * below {@code prefs[defaultAG]} and above {@code prefs[minPref]};
     * equal values keep the LATER index (legacy comment: arbitrary,
     * no artistic logic). Fallback: index 1.
     */
    static int midMaxPreferredAG(Facts facts, int shape) {
        int idx = 1;
        double best = facts.agPref(shape, minPreferredAG(facts, shape));
        double ceil = facts.agPref(shape, facts.defaultAG(shape));
        for (int ag = 1; ag <= 4; ag++) {
            double w = facts.agPref(shape, ag);
            if (w < ceil && w > best) {
                best = w;
                idx = ag;
            } else if (w == best) {
                idx = ag;   // legacy: equal values keep the later index
            } // end IF-ELSE
        } // end FOR
        return idx;
    } // end method midMaxPreferredAG

} // end class AssignAGsStratg
