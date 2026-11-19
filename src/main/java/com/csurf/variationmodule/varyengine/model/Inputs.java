package com.csurf.variationmodule.varyengine.model;

import java.util.Objects;
import java.util.Optional;


/**
 * The run-level inputs carried by {@link StratgContext#inputs()} —
 * currently just the shared read-only base (the bootstrap-produced
 * reference coloring; absent during the bootstrap run itself).
 * Room for future run-time inputs is intentional (the context object
 * pattern keeps the extension point open without new signatures).
 */
public interface Inputs {

    /** The cycle's base — empty under {@code VariationRunState.noBase()}. */
    Optional<VariationResult> base();

    /** Inputs for a processed run (rows 2–12): base injected. */
    static Inputs withBase(VariationResult base) {
        Objects.requireNonNull(base, "base");
        return () -> Optional.of(base);
    } // end method withBase

    /** Inputs for the bootstrap run (row 1): no base exists yet. */
    static Inputs noBase() {
        return Optional::empty;
    } // end method noBase

} // end interface Inputs
