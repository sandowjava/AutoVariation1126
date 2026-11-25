package com.csurf.variationmodule.varyengine.model;


/**
 * JSON file representation of one {@link AGHuePattern} — the persistence
 * DTO for {@code dev_storage/aghuepatterns/*.json}. Mirrors the
 * {@code AgPrefsDocument} conventions: public fields, {@code formatVersion}
 * defaulting when absent, and a human-readable {@code description} for
 * provenance.
 *
 * <p>Example:
 * <pre>{ "formatVersion": 1,
 *   "name": "13-10-06-08-heroEyeTailored",
 *   "description": "Saved from the pick-4-hues dialog",
 *   "anchorHues": [13, 10, 6, 8] }</pre>
 *
 * <p>{@code name} is the canonical catalog key; {@code anchorHues} is in
 * AG order (AG1..AG4). {@link AGHuePatterns} validates at load that the
 * name's {@code NN-NN-NN-NN} prefix matches {@code anchorHues} — the two
 * cannot drift apart without the file being rejected.
 *
 * <p><b>Reserved for formatVersion 2:</b> a {@code levels} field carrying
 * the four pattern-level doubles (analogous/complementary/triadic/
 * quadratic) once {@code anchorPattern} analysis lands — v1 files keep
 * loading via the absent-field default.
 */
public final class AGHuePatternDoc {

    /** Current file format version. */
    public static final int FORMAT_VERSION = 1;

    /** Canonical name — the catalog key. */
    public String name;

    /** Human-readable provenance; informational only. */
    public String description;

    /** File format version; absent in JSON = {@link #FORMAT_VERSION}. */
    public Integer formatVersion;

    /** The four anchor hues in AG order (AG1..AG4), each 1..36. */
    public int[] anchorHues;

    /** Format version, defaulting to {@link #FORMAT_VERSION} when absent. */
    public int formatVersion() {
        return formatVersion != null ? formatVersion : FORMAT_VERSION;
    } // end method formatVersion

} // end class AGHuePatternDoc
