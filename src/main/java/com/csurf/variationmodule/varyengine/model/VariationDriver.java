package com.csurf.variationmodule.varyengine.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * The 12-run cycle driver (step 7; Option B ordering): runs a cooked
 * {@link ArtisticGoalSpec} list <b>in order</b> and returns the
 * {@link VariationResult}s.
 *
 * <ul>
 *   <li><b>artGoalSpec[0] = the bootstrap artGoalSpec</b> — run on
 *       {@code noBase()} state; its result is promoted to the shared
 *       base <i>and</i> fills display slot 1 (the reference coloring).
 *       This replaces legacy 999's fake-start-frozen pipeline.</li>
 *   <li><b>artGoalSpecs[1..n] = processed runs</b> — each gets a fresh
 *       {@code withBase(n, base)} state so its freeze-flagged shapes
 *       reinstate the bootstrap coloring at read time.</li>
 * </ul>
 *
 * <p>Validation is structural: the first artGoalSpec must carry no
 * {@code FREEZE_SHAPES} Stratg (the bootstrap signature — a frozen
 * overlay needs a base that does not exist yet), and every later
 * artGoalSpec should carry one (the cycle's purpose). Violations fail
 * loudly.
 */
public final class VariationDriver {

    private final ColorChangeEngine engine = new ColorChangeEngine();

    /**
     * Runs the cooked {@link ArtisticGoalSpec} list (typically the 12
     * recipe rows from {@link FrozenShapesCategory#cook}) against
     * {@code facts}, returning one {@link VariationResult} per
     * artGoalSpec in order.
     */
    public List<VariationResult> run(List<ArtisticGoalSpec> artGoalSpecList, Facts facts) {
        Objects.requireNonNull(artGoalSpecList, "artGoalSpecList");
        Objects.requireNonNull(facts, "facts");
        if (artGoalSpecList.isEmpty()) {
            throw new IllegalArgumentException("a variation cycle needs at least the bootstrap artGoalSpec");
        } // end IF
        if (hasFreezeStratg(artGoalSpecList.get(0))) {
            throw new IllegalArgumentException(
                    "artGoalSpec[0] must be the bootstrap artGoalSpec — it carries a FREEZE_SHAPES Stratg "
                            + "but no base exists yet (Option B: row 1 = {921,555,255} only)");
        } // end IF

        List<VariationResult> results = new ArrayList<>(artGoalSpecList.size());
        results.add(engine.execute(artGoalSpecList.get(0), facts));       // bootstrap — the shared base
        VariationResult base = results.get(0);

        for (int i = 1; i < artGoalSpecList.size(); i++) {
            if (!hasFreezeStratg(artGoalSpecList.get(i))) {
                throw new IllegalArgumentException(
                        "artGoalSpec[" + i + "] carries no FREEZE_SHAPES Stratg — processed rows "
                                + "in a FrozenShapes cycle freeze a set against the base");
            } // end IF
            results.add(engine.execute(artGoalSpecList.get(i), facts, base));
        } // end FOR
        return results;
    } // end method run

    private static boolean hasFreezeStratg(ArtisticGoalSpec someArtGoalSpec) {
        return someArtGoalSpec.flattenStratgSpecs().stream()
                .anyMatch(stratgSpec -> stratgSpec.getStratgType() == StratgType.FREEZE_SHAPES);
    } // end method hasFreezeStratg

} // end class VariationDriver
