package com.csurf.variationmodule.varyengine.model;


/**
 * The Stratg executable contract (parent §4.3): {@code execute(ctx, params)}
 * writes <b>decisions</b> — never commits — into the decided layer via
 * the narrow {@link RunState} seam.
 *
 * <p>{@code params} is the {@link StratgSpec}: the spec-time,
 * serializable argument carrying the Stratg's bound named params
 * (the plan's params/context split — the sketch's {@code Params}
 * placeholder is realized concretely by {@code StratgSpec}, which the
 * {@link NestedRunner} already names).
 *
 * <p>Stratgs are stateless between runs and deterministic: same facts +
 * same incoming state + same params → same writes. They read the
 * decided layer for dependencies and skip shapes that lack inputs.
 */
public interface Stratg {

    /** The registry identity — for resolution and spec type-checks. */
    StratgType type();

    /** Reads facts + decided state; writes its own decision slots. */
    void execute(StratgContext ctx, StratgSpec params);

} // end interface Stratg
