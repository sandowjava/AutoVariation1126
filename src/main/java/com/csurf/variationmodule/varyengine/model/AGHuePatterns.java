package com.csurf.variationmodule.varyengine.model;

import com.csurf.templatecore.storage.config.StorageConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
 *
 * <p><b>Persistence (Phase 2).</b> User-authored patterns live in
 * {@link #USER_TABLE}, a separate map so built-ins are never shadowed at
 * load and user entries can be cleared for tests. JSON files in
 * {@code dev_storage/aghuepatterns} ({@link AGHuePatternDoc}) are folded
 * in lazily on first access; collision policy is existing-wins + loud
 * skip. Lookups consult {@code USER_TABLE} first, so a save-time
 * overwrite is honored in-session (a user file colliding with a built-in
 * is still skipped at next launch — see {@link #loadFrom}).
 */
public final class AGHuePatterns {

    private static final Logger LOG = Logger.getLogger(AGHuePatterns.class.getName());

    /** Count of built-in rows — 25 legacy rows minus rows 19 and 23. */
    public static final int BUILTIN_COUNT = 23;

    private static final Map<String, AGHuePattern> TABLE = new TreeMap<>();

    /** User-authored entries — loaded from disk or saved this session. */
    private static final Map<String, AGHuePattern> USER_TABLE = new TreeMap<>();

    /** True once the on-disk user patterns have been folded in. */
    private static boolean userLoaded;

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
        ensureUserLoaded();
        String key = canonicalKey(name);
        AGHuePattern pattern = USER_TABLE.get(key);
        if (pattern == null) {
            pattern = TABLE.get(key);
        } // end IF
        if (pattern == null) {
            throw new IllegalArgumentException(
                    "unknown AGHuePattern name: '" + name + "'"
                            + " — " + count() + " entries in catalog");
        } // end IF
        return pattern;
    } // end method get

    /** True when {@code name} resolves to a catalog entry. */
    public static boolean exists(String name) {
        if (name == null) {
            return false;
        } // end IF
        ensureUserLoaded();
        String key = canonicalKey(name);
        return USER_TABLE.containsKey(key) || TABLE.containsKey(key);
    } // end method exists

    /** True when {@code name} resolves to a built-in (non-user) entry. */
    public static boolean isBuiltIn(String name) {
        if (name == null) {
            return false;
        } // end IF
        return TABLE.containsKey(canonicalKey(name));
    } // end method isBuiltIn

    /** All entries, sorted by canonical name (lexicographic == hue order). */
    public static List<AGHuePattern> all() {
        ensureUserLoaded();
        return Collections.unmodifiableList(new ArrayList<>(merged().values()));
    } // end method all

    /** Sorted view of the canonical names — for pickers and diagnostics. */
    public static List<String> allNames() {
        ensureUserLoaded();
        List<String> names = new ArrayList<>();
        for (AGHuePattern pattern : merged().values()) {
            names.add(pattern.getName());
        } // end FOR
        return Collections.unmodifiableList(names);
    } // end method allNames

    public static int count() {
        ensureUserLoaded();
        return TABLE.size() + USER_TABLE.size();
    } // end method count

    /** Built-ins plus user entries; user entries win same-key conflicts. */
    private static Map<String, AGHuePattern> merged() {
        Map<String, AGHuePattern> merged = new TreeMap<>(TABLE);
        merged.putAll(USER_TABLE);
        return merged;
    } // end method merged

    // ============================================================================
    // Persistence — dev_storage/aghuepatterns/*.json (one file per pattern)
    // ============================================================================

    /**
     * Registers {@code pattern} (overwriting a same-key user entry — the
     * caller's overwrite-confirm is the policy) and writes it to
     * {@code dev_storage/aghuepatterns/{name}.json}.
     *
     * @param pattern     the pattern to save
     * @param description provenance note stored in the file, or null
     */
    public static void saveUserPattern(AGHuePattern pattern, String description) {
        saveTo(StorageConfig.getAGHuePatternsPath(), pattern, description);
    } // end method saveUserPattern

    /**
     * As {@link #saveUserPattern} but writing into {@code dir} — the seam
     * tests use with a {@code @TempDir}.
     */
    public static void saveTo(Path dir, AGHuePattern pattern, String description) {
        writeDoc(dir.resolve(pattern.getName() + ".json"), pattern, description);
        USER_TABLE.put(canonicalKey(pattern.getName()), pattern);
    } // end method saveTo

    /**
     * Loads every valid {@code *.json} in the default aghuepatterns dir
     * into the catalog; returns the number loaded. Idempotent — collisions
     * with existing entries (built-in or earlier) are skipped with a
     * warning, so a repeat call is a no-op.
     */
    public static int loadUserPatterns() {
        userLoaded = true;
        return loadFrom(StorageConfig.getAGHuePatternsPath());
    } // end method loadUserPatterns

    /**
     * Loads every valid {@code *.json} in {@code dir} — the seam tests use
     * with a {@code @TempDir}. Files are processed in sorted file-name
     * order; per-file failures warn and skip without blocking the rest.
     *
     * @return the number of patterns registered from this dir
     */
    public static int loadFrom(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return 0;
        } // end IF
        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.warning("AGHuePatterns: cannot list " + dir + ": " + e.getMessage());
            return 0;
        } // end try
        int loaded = 0;
        for (Path file : files) {
            if (loadOne(file)) {
                loaded++;
            } // end IF
        } // end FOR
        return loaded;
    } // end method loadFrom

    /** Loads one pattern file; false = skipped (reason logged). */
    private static boolean loadOne(Path file) {
        AGHuePatternDoc doc;
        try {
            doc = new ObjectMapper().readValue(file.toFile(), AGHuePatternDoc.class);
        } catch (IOException | RuntimeException e) {
            LOG.warning("AGHuePatterns: skipping unreadable file " + file
                    + ": " + e.getMessage());
            return false;
        } // end try
        AGHuePattern pattern;
        try {
            pattern = toPattern(doc);
        } catch (IllegalArgumentException e) {
            LOG.warning("AGHuePatterns: skipping invalid file " + file
                    + ": " + e.getMessage());
            return false;
        } // end try
        String key = canonicalKey(pattern.getName());
        if (TABLE.containsKey(key) || USER_TABLE.containsKey(key)) {
            // existing entry wins — built-ins and earlier files are never shadowed
            LOG.warning("AGHuePatterns: skipping " + file
                    + " — name '" + pattern.getName() + "' already in catalog");
            return false;
        } // end IF
        USER_TABLE.put(key, pattern);
        return true;
    } // end method loadOne

    /** Validates {@code doc} and builds the pattern; throws on any defect. */
    private static AGHuePattern toPattern(AGHuePatternDoc doc) {
        if (doc.formatVersion() != AGHuePatternDoc.FORMAT_VERSION) {
            throw new IllegalArgumentException("unsupported formatVersion "
                    + doc.formatVersion() + " (supported: "
                    + AGHuePatternDoc.FORMAT_VERSION + ")");
        } // end IF
        String name = canonicalize(doc.name); // throws on null/blank
        if (doc.anchorHues == null || doc.anchorHues.length != 4) {
            throw new IllegalArgumentException("anchorHues must have exactly 4 entries");
        } // end IF
        for (int hue : doc.anchorHues) {
            if (hue < 1 || hue > 36) {
                throw new IllegalArgumentException(
                        "anchorHues entries must be 1..36, got " + hue);
            } // end IF
        } // end FOR
        // integrity: the name's NN-NN-NN-NN prefix must match the hues
        String prefix = canonicalPrefix(doc.anchorHues[0], doc.anchorHues[1],
                doc.anchorHues[2], doc.anchorHues[3]);
        if (!name.startsWith(prefix)) {
            throw new IllegalArgumentException("name '" + name
                    + "' does not start with its anchorHues prefix " + prefix);
        } // end IF
        return new AGHuePattern(name, doc.anchorHues[0], doc.anchorHues[1],
                doc.anchorHues[2], doc.anchorHues[3]);
    } // end method toPattern

    private static void writeDoc(Path file, AGHuePattern pattern, String description) {
        AGHuePatternDoc doc = new AGHuePatternDoc();
        doc.formatVersion = AGHuePatternDoc.FORMAT_VERSION;
        doc.name = pattern.getName();
        doc.description = description;
        doc.anchorHues = pattern.hueNums();
        try {
            Files.createDirectories(file.getParent());
            new ObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValue(file.toFile(), doc);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to write AGHuePattern file: " + file, e);
        } // end try
    } // end method writeDoc

    /** Folds on-disk user patterns into the catalog — once per JVM. */
    private static void ensureUserLoaded() {
        if (!userLoaded) {
            loadUserPatterns();
        } // end IF
    } // end method ensureUserLoaded

    /**
     * Removes all user entries and marks the load as done, so tests see
     * the built-in catalog regardless of what lives in
     * {@code dev_storage/aghuepatterns} or what other tests registered.
     */
    public static void clearUserPatternsForTests() {
        USER_TABLE.clear();
        userLoaded = true;
    } // end method clearUserPatternsForTests

} // end class AGHuePatterns
