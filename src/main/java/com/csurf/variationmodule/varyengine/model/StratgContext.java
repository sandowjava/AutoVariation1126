package com.csurf.variationmodule.varyengine.model;


/**
 * The run-time context for {@link Stratg#execute} (Context-A, §7.2):
 * carries everything the Stratg may touch that is not params.
 *
 * <p>Kept deliberately small and immutable — the accepted caveat is that
 * a context can become a kitchen sink, so run-time inputs are added
 * only deliberately (§7.2, §8.9 keeps extension points open).
 *
 * <p>{@link #nested()} is the channel to the nested runner — bound per
 * execution: for an inner Stratg it is bound to the <i>scratch</i>, so a
 * nested-of-nested seed comes from the scratch, not the live state.
 */
public interface StratgContext {

    /** Read-only per-shape facts (legacy GraphicSmarts-style input). */
    Facts facts();

    /** The run state this Stratg writes decisions into (live, or a scratch when nested). */
    RunState runState();

    /** Run-level inputs — the shared base, and future run-time inputs. */
    Inputs inputs();

    /**
     * The nested runner bound to this context's state (Context-A).
     * Stratgs that never nest need not call it; contexts without a bound
     * runner throw {@link IllegalStateException} here.
     */
    NestedRunner nested();

} // end interface StratgContext
