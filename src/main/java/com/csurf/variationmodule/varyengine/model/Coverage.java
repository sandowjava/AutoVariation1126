package csurf.cleandesign.variationmodule.varyengine.model;


/**
 * Coverage modes for {@code RunState.apply}, per D2.
 * {@link #REQUIRE_FULL} is the default: it turns an inner Stratg that
 * silently skipped shapes (empty or partial proposal) into a loud
 * failure instead of a silent no-op.
 */
public enum Coverage {
    REQUIRE_FULL,
    ALLOW_PARTIAL
} // end enum Coverage
