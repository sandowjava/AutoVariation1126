package com.csurf.variationmodule.varyengine.model;


/**
 * The explicit domain sentinel for "no Stratg has written this slot"
 * (parent DesignPlan §4.2 — not 0, not 33, not 11081). One definition,
 * shared across the decided-layer channels, so run-state diffs are
 * unambiguous: {@code Undecided.UNDECIDED} never equals a real value.
 *
 * Read APIs expose absence as {@link java.util.Optional#empty()} — the
 * sentinel itself stays an internal representation detail.
 */
public enum Undecided {
    UNDECIDED
} // end enum Undecided
