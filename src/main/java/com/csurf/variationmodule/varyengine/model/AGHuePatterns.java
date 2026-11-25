package com.csurf.variationmodule.varyengine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * The name-keyed catalog of {@link AGHuePattern} instances — the successor
 * of the deleted {@code AnchorHueSets} index table. Built-in rows are
 * transcribed constant-for-constant from the legacy
 * {@code UserInputs.fillTheArrayOfAGhueSets()} table (rows 0–24), minus the
 * two exact duplicates (legacy row 19 ≡ row 0; legacy row 23 ≡ row 1) —
 * 23 entries, each named {@code "NN-NN-NN-NN-legacyTableRowNum" + row}.
 *
 * <p>Names are canonical ({@link #canonicalPrefix} + optional suffix);
 * the full canonical name is the catalog key — the hue prefix is
 * descriptive only and is never assumed unique.
 *
 * <p>555's {@code agHuePattern} param (NAMED_KEY) resolves a canonical
 * name here: "AG → that AG's center hue from the named pattern" (OQ4).
 */
public final class AGHuePatterns {

    /** Count of built-in rows — 25 legacy rows minus rows 19 and 23. */
    public static final int BUILTIN_COUNT = 23;

    private static final Map<String, AGHuePattern> TABLE = new TreeMap<>();

    static {
        put(legacy(0, 6, 9, 4, 2));      //  0 — literal (unchanged in legacy)
        put(legacy(1, 4, 5, 2, 7));      //  1 — hueCtr(4,5,2,7) + 0
        put(legacy(2, 6, 7, 4, 9));      //  2 — +2
        put(legacy(3, 8, 9, 6, 11));     //  3 — +4
        put(legacy(4, 10, 11, 8, 13));   //  4 — +6
        put(legacy(5, 12, 13, 10, 15));  //  5 — +8
        put(legacy(6, 14, 15, 12, 17));  //  6 — +10
        put(legacy(7, 16, 17, 14, 19));  //  7 — +12
        put(legacy(8, 18, 19, 16, 21));  //  8 — +14
        put(legacy(9, 20, 21, 18, 23));  //  9 — +16
        put(legacy(10, 22, 23, 20, 25)); // 10 — +18
        put(legacy(11, 24, 25, 22, 27)); // 11 — +20
        put(legacy(12, 26, 27, 24, 29)); // 12 — +22
        put(legacy(13, 5, 3, 6, 8));     // 13 — literal: "very nice w yellow-tan"
        put(legacy(14, 3, 6, 8, 5));     // 14 — literal: "nice w yellow-red"
        put(legacy(15, 10, 6, 8, 13));   // 15 — literal
        put(legacy(16, 6, 8, 13, 10));   // 16 — literal: "nice w tan-magenta"
        put(legacy(17, 8, 13, 10, 6));   // 17 — literal: "nice w magenta"
        put(legacy(18, 13, 10, 6, 8));   // 18 — literal: "nice w yellow-magenta"
        // legacy row 19 (6,9,4,2) skipped — duplicate of row 0
        put(legacy(20, 8, 2, 4, 9));     // 20 — literal: "nice w yellow"
        put(legacy(21, 8, 5, 3, 6));     // 21 — literal
        put(legacy(22, 6, 9, 4, 11));    // 22 — literal
        // legacy row 23 (4,5,2,7) skipped — duplicate of row 1
        put(legacy(24, 4, 2, 6, 9));     // 24 — literal
    } // end static init

    private AGHuePatterns() {
    } // end private constructor — static catalog only

    private static AGHuePattern legacy(int row, int h1, int h2, int h3, int h4) {
        return new AGHuePattern(
                canonicalPrefix(h1, h2, h3, h4) + "-legacyTableRowNum" + row,
                h1, h2, h3, h4);
    } // end method legacy

    private static void put(AGHuePattern pattern) {
        if (TABLE.put(canonicalKey(pattern.getName()), pattern) != null) {
            throw new IllegalStateException(
                    "duplicate canonical AGHuePattern name: " + pattern.getName());
        } // end IF
    } // end method put

    // ============================================================================
    // Canonical naming — the single definition used by save/load/bind/lookup
    // ============================================================================

    /** Zero-padded {@code "NN-NN-NN-NN"} hue prefix in AG order. */
    public static String canonicalPrefix(int h1, int h2, int h3, int h4) {
        return String.format("%02d-%02d-%02d-%02d", h1, h2, h3, h4);
    } // end method canonicalPrefix

    /** Zero-padded {@code "NN-NN-NN-NN"} hue prefix from a value object. */
    public static String canonicalPrefix(AnchorHueSet anchorHueSet) {
        return canonicalPrefix(anchorHueSet.hueNumForAG(1), anchorHueSet.hueNumForAG(2),
                anchorHueSet.hueNumForAG(3), anchorHueSet.hueNumForAG(4));
    } // end method canonicalPrefix

    /**
     * Canonical name = prefix + optional {@code "-" + suffix}
     * (suffix trimmed; null/blank suffix → prefix alone is a legal name).
     */
    public static String canonicalName(int h1, int h2, int h3, int h4, String suffix) {
        String prefix = canonicalPrefix(h1, h2, h3, h4);
        if (suffix == null || suffix.isBlank()) {
            return prefix;
        } // end IF
        return prefix + "-" + suffix.trim();
    } // end method canonicalName

    /**
     * Normalizes a raw name to canonical form for comparison/lookup:
     * trims, collapses runs of whitespace to single spaces, and strips
     * spaces around hyphens. Case is preserved for display (the catalog
     * compares case-insensitively via {@link #canonicalKey}).
     */
    public static String canonicalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("AGHuePattern name cannot be null");
        } // end IF
        String s = raw.trim().replaceAll("\\s+", " ").replaceAll("\\s*-\\s*", "-");
        if (s.isEmpty()) {
            throw new IllegalArgumentException("AGHuePattern name cannot be blank");
        } // end IF
        return s;
    } // end method canonicalize

    /** The case-insensitive comparison key for {@code name}. */
    public static String canonicalKey(String name) {
        return canonicalize(name).toLowerCase(java.util.Locale.ROOT);
    } // end method canonicalKey

    // ============================================================================
    // Catalog access
    // ============================================================================

    /** The pattern bound to {@code name}; throws for an unknown key. */
    public static AGHuePattern get(String name) {
        AGHuePattern pattern = TABLE.get(canonicalKey(name));
        if (pattern == null) {
            throw new IllegalArgumentException(
                    "unknown AGHuePattern name: '" + name + "'"
                            + " — " + TABLE.size() + " entries in catalog");
        } // end IF
        return pattern;
    } // end method get

    /** True when {@code name} resolves to a catalog entry. */
    public static boolean exists(String name) {
        if (name == null) {
            return false;
        } // end IF
        return TABLE.containsKey(canonicalKey(name));
    } // end method exists

    /** All entries, sorted by canonical name (lexicographic == hue order). */
    public static List<AGHuePattern> all() {
        return Collections.unmodifiableList(new ArrayList<>(TABLE.values()));
    } // end method all

    /** Sorted view of the canonical names — for pickers and diagnostics. */
    public static List<String> allNames() {
        List<String> names = new ArrayList<>(TABLE.size());
        for (AGHuePattern pattern : TABLE.values()) {
            names.add(pattern.getName());
        } // end FOR
        return Collections.unmodifiableList(names);
    } // end method allNames

    public static int count() {
        return TABLE.size();
    } // end method count

} // end class AGHuePatterns
