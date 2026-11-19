package csurf.cleandesign.variationmodule.varyengine.model;


/**
 * The decided-layer channels, per the nested-execution design (D3):
 * the only channels a {@link Proposal} may carry and the units in which
 * {@link Seed}s and {@code apply} channel masks are expressed.
 * The {@code frozen} flag is deliberately not a channel — flag writes
 * cannot be harvested and freeze-type Stratgs cannot be nested (D4).
 */
public enum Channel {
    AG,
    HUE,
    OSTDOT
} // end enum Channel
