package com.csurf.variationmodule.varyengine.model;


/**
 * The nested-execution service a Stratg reaches through its context
 * ({@code ctx.nested()} — Context-A, §7.2). Executes the real inner
 * Stratg and returns only the writes it made, as an immutable
 * {@link Proposal}.
 *
 * <p>Two implementations behind this call (§4.1): a {@code PureCore}
 * fast path (compute from facts and params — no scratch) and the
 * scratch path (recorded run on a seeded throwaway state). The outer
 * Stratg consumes the Proposal identically on both.
 *
 * <p>Contract notes (from the decisions):
 * <ul>
 *   <li>The one-arg form is the D1 default — {@code Seed.blank()}.</li>
 *   <li>The runner rejects a {@link StratgSpec} that is not
 *       {@code isFullySpecified()} (harness case 12).</li>
 *   <li>The runner given to an inner Stratg is bound to the scratch,
 *       not the live state — nested seeds come from the scratch (§7.2).</li>
 *   <li>A flag write inside a nested run is an error —
 *       {@link RecordingRunState#FLAG_WRITE_ERROR} (D4 residual).</li>
 * </ul>
 *
 * The implementation lands in step 5b ({@code StratgResolver} +
 * runner); this interface lands with run state in step 3 (P2, §7.3).
 */
public interface NestedRunner {

    Proposal run(StratgSpec inner, Seed seed);

    /** D1 default: a blank scratch — dependencies must be written out explicitly. */
    default Proposal run(StratgSpec inner) {
        return run(inner, Seed.blank());
    } // end method run

} // end interface NestedRunner
