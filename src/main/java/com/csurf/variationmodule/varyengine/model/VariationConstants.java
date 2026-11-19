package com.csurf.variationmodule.varyengine.model;


/**
 * Slice-1 constants carried over as clean data (step 4).
 *
 * <p>{@link #VALUE_RANGE} is the luminance tolerance (±) legacy 255
 * passed to {@code getPrelimSetOfOstDotsByValueRange(hue, target, .055)}.
 * In the clean design it is bound per recipe row into 255's required
 * {@link StratgParam#VALUE_RANGE} — the constant is the table value the
 * cook binds, not a schema default (110926 luminance-resolution note:
 * defaults live at the binding layer).
 */
public final class VariationConstants {

    /** Half-range luminance tolerance for 255's candidate set. */
    public static final double VALUE_RANGE = 0.055;

    /**
     * Legacy {@code howManyVariations} = 12: recipe row 1 (the bootstrap
     * spec) plus the 11 processed-case kernel sites (rows 2–12). The
     * recipe table has exactly this many fixed rows — the cook errors
     * if asked for any other count.
     */
    public static final int STANDARD_VARIATION_COUNT = 12;

    private VariationConstants() {
    } // end private constructor — constants only

} // end class VariationConstants
