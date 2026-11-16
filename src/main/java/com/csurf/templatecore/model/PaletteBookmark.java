package com.csurf.templatecore.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Immutable snapshot of one complete palette (one color per DrawObj), used as a single entry
 * (a "bookmark" or "checkpoint") in the UNDO/REDO checkpoint list.
 *
 * <h3>Role in the UNDO/REDO architecture</h3>
 * UNDO/REDO is built from two new classes:
 * <ul>
 *   <li>{@code PaletteBookmark} (this class) - ONE saved palette. It knows nothing about ordering,
 *       the marker, or how UNDO/REDO navigates.</li>
 *   <li>{@link PaletteBookmarkList} - the chronological list of bookmarks plus a marker. It holds the
 *       navigation, saving, and eviction logic.</li>
 * </ul>
 * Bookmarks are saved only by deliberate user action (for example a keyboard shortcut), by the
 * automatic load-time baseline (B0), or implicitly when UNDO/REDO is used from a dirty state.
 * Intermediate color changes (hue rotations, single-DrawObj edits) are never captured automatically.
 *
 * <h3>Bookmarks are NOT named saved palettes</h3>
 * A bookmark is transient, session-scoped, and has an auto-generated label. It is deliberately a
 * different class from {@link PaletteWName}, which represents a persistent, named, serializable palette.
 * It has no Jackson annotations and no setters, so it cannot be serialized or modified by accident.
 * Use {@link #toPaletteWName()} if a bookmark is ever promoted to a saved palette.
 *
 * <h3>Index convention</h3>
 * The color at array position {@code i} belongs to the DrawObj at position {@code i} of
 * {@link DrawObjRoster#getTemplateDrawObjs()}. This is the same convention used by
 * {@link PaletteWName} and by the roster's palette-loading code. It is safe because DrawObjs are never
 * added, removed, or reordered in the model list after a Template is loaded. That invariant is true by
 * convention and is not enforced, so {@link PaletteBookmarkList} checks that every bookmark has the same
 * size as the baseline and fails loudly if not.
 *
 * <h3>Immutability</h3>
 * The color array is copied on the way in and on the way out. Neither the source array nor any array
 * returned by {@link #getColors()} can alter a saved bookmark. The label can only be changed by creating
 * a modified copy with {@link #withLabel(String)}.
 *
 * <h3>Value-based comparison</h3>
 * {@link #sameColorsAs(PaletteBookmark)} decides whether the live palette is "dirty" relative to a
 * bookmark. Colors are compared by VALUE (hue number and fill-order number), not by object reference,
 * because colors loaded from saved palettes may be different instances of the same logical color.
 * Both numbers come from the color's {@code OstHueColumnStep}, so the comparison works for any
 * {@link ColorPtOstDot}, including gray colors.
 *
 * <h3>Null colors</h3>
 * A DrawObj can briefly have a null current color before {@code setColor} has been called. A bookmark
 * accepts null entries, and comparison treats null as equal only to null.
 */
public final class PaletteBookmark {

    /**
     * Records why a bookmark was created. Used for default labels now, and available to a future
     * bookmark-list display.
     */
    public enum Kind {
        /** The automatic checkpoint ("B0") taken when a Template is first loaded or opened. */
        LOAD_BASELINE,
        /** A checkpoint created by an explicit user save action. */
        USER_SAVE,
        /** A checkpoint created automatically because UNDO or REDO was used while the palette was dirty. */
        IMPLICIT_SAVE
    }

    /** Formats creation times for the auto-generated labels, in the system time zone. */
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    /** Private copy of the palette; position i is the color of the DrawObj at position i. */
    private final ColorPtOstDot[] colors;

    /** Why this bookmark was created. */
    private final Kind kind;

    /** Creation time in milliseconds since the epoch. */
    private final long createdMillis;

    /** Display label; auto-generated at creation and replaceable only through {@link #withLabel(String)}. */
    private final String label;

    /**
     * Private constructor used by the factory methods and {@link #withLabel(String)}.
     * The caller is responsible for passing an array that nothing else references.
     *
     * @param colorsAlreadyCopied An array owned exclusively by this bookmark
     * @param kind Why the bookmark was created
     * @param createdMillis Creation time in milliseconds since the epoch
     * @param label The display label
     */
    private PaletteBookmark(ColorPtOstDot[] colorsAlreadyCopied, Kind kind, long createdMillis, String label) {
        this.colors = colorsAlreadyCopied;
        this.kind = kind;
        this.createdMillis = createdMillis;
        this.label = label;
    }

    /**
     * Captures the current palette of a roster: the current color of each DrawObj, in roster order.
     * This is how the load-time baseline (B0) and the palette passed to UNDO/REDO and dirty checks
     * are normally created. The roster is only read, never modified.
     *
     * @param roster The roster to capture; must contain at least one DrawObj
     * @param kind Why the bookmark is being created
     * @return A new bookmark holding the roster's current colors
     * @throws IllegalArgumentException if the roster is null or has no DrawObjs, or kind is null
     */
    public static PaletteBookmark fromRoster(DrawObjRoster roster, Kind kind) {
        if (roster == null) {
            throw new IllegalArgumentException("DrawObjRoster cannot be null");
        }
        List<DrawObj> drawObjs = roster.getTemplateDrawObjs();
        if (drawObjs == null || drawObjs.isEmpty()) {
            throw new IllegalArgumentException("DrawObjRoster has no DrawObjs");
        }
        // Position i of the array corresponds to position i of the roster's DrawObj list
        ColorPtOstDot[] captured = new ColorPtOstDot[drawObjs.size()];
        for (int i = 0; i < captured.length; i++) {
            captured[i] = drawObjs.get(i).getCurrentColor();
        }
        return create(captured, kind);
    }

    /**
     * Creates a bookmark from an existing array of colors. The array is copied, so later changes to the
     * caller's array do not affect the bookmark.
     *
     * @param someColors Colors indexed like the roster's DrawObj list; must not be null or empty
     * @param kind Why the bookmark is being created
     * @return A new bookmark holding a copy of the colors
     * @throws IllegalArgumentException if the array is null or empty, or kind is null
     */
    public static PaletteBookmark fromColors(ColorPtOstDot[] someColors, Kind kind) {
        if (someColors == null || someColors.length == 0) {
            throw new IllegalArgumentException("Color array cannot be null or empty");
        }
        return create(Arrays.copyOf(someColors, someColors.length), kind);
    }

    /**
     * Shared creation step for the factory methods: validates the kind, stamps the creation time,
     * and generates the default label.
     *
     * @param colorsAlreadyCopied An array owned exclusively by the new bookmark
     * @param kind Why the bookmark is being created
     * @return A new bookmark
     */
    private static PaletteBookmark create(ColorPtOstDot[] colorsAlreadyCopied, Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("Kind cannot be null");
        }
        // One timestamp is used for both the creation time and the default label
        long now = System.currentTimeMillis();
        return new PaletteBookmark(colorsAlreadyCopied, kind, now, defaultLabel(kind, now));
    }

    /**
     * Builds the auto-generated label: "B0 (loaded)" for the baseline, "Auto-saved HH:mm:ss" for implicit
     * saves, and "Bookmark HH:mm:ss" for explicit user saves. A future editable bookmark list can
     * replace these through {@link #withLabel(String)}.
     *
     * @param kind The bookmark kind
     * @param createdMillis Creation time in milliseconds since the epoch
     * @return The default label
     */
    private static String defaultLabel(Kind kind, long createdMillis) {
        String time = TIME_FORMAT.format(Instant.ofEpochMilli(createdMillis));
        switch (kind) {
            case LOAD_BASELINE:
                return "B0 (loaded)";
            case IMPLICIT_SAVE:
                return "Auto-saved " + time;
            default:
                return "Bookmark " + time;
        }
    }

    /**
     * Returns a copy of this bookmark with a different label. Colors, kind, and creation time are kept.
     * The original bookmark is unchanged. Intended for the future "edit the automatically-generated name"
     * feature.
     *
     * @param newLabel The new label
     * @return A new bookmark with the same colors and the new label
     */
    public PaletteBookmark withLabel(String newLabel) {
        return new PaletteBookmark(colors, kind, createdMillis, newLabel);
    }

    /**
     * Returns the number of colors, which equals the number of DrawObjs in the roster this bookmark
     * was captured from.
     *
     * @return The number of colors
     */
    public int getSize() {
        return colors.length;
    }

    /**
     * Returns the color for the DrawObj at the given roster position.
     *
     * @param index The 0-based DrawObj position
     * @return The color at that position; may be null if the DrawObj had no color when captured
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public ColorPtOstDot getColorAt(int index) {
        if (index < 0 || index >= colors.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + colors.length);
        }
        return colors[index];
    }

    /**
     * Returns a copy of the color array. Changing the returned array does not affect this bookmark.
     * This is the array a restore operation applies to the DrawObjs, position by position.
     *
     * @return A copy of the colors, indexed like the roster's DrawObj list
     */
    public ColorPtOstDot[] getColors() {
        return Arrays.copyOf(colors, colors.length);
    }

    /**
     * Returns why this bookmark was created.
     *
     * @return The bookmark kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the creation time.
     *
     * @return Milliseconds since the epoch
     */
    public long getCreatedMillis() {
        return createdMillis;
    }

    /**
     * Returns the display label.
     *
     * @return The auto-generated or user-supplied label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Tests whether another bookmark holds the same palette, comparing colors by value.
     * Kind, label, and creation time are ignored.
     *
     * @param other The bookmark to compare with; may be null
     * @return true if other is non-null and has the same size and the same color at every position
     */
    public boolean sameColorsAs(PaletteBookmark other) {
        return other != null && sameColorsAs(other.colors);
    }

    /**
     * Tests whether an array of colors matches this bookmark, comparing colors by value.
     * This is the comparison behind the "dirty" test in {@link PaletteBookmarkList#isDirty(PaletteBookmark)}:
     * the live palette is dirty exactly when it does NOT match the bookmark under the marker.
     * It is more computation than a flag, but it never reports a false "dirty" (for example after a
     * rotation that was later reversed).
     *
     * @param otherColors The colors to compare with; may be null
     * @return true if the array is non-null, has the same length, and matches at every position
     */
    public boolean sameColorsAs(ColorPtOstDot[] otherColors) {
        if (otherColors == null || otherColors.length != colors.length) {
            return false;
        }
        for (int i = 0; i < colors.length; i++) {
            if (!sameColor(colors[i], otherColors[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two colors by value. Two colors are the same when they have the same hue number and the
     * same fill-order number, both of which come from the color's OstHueColumnStep. Null equals only null.
     *
     * @param a The first color; may be null
     * @param b The second color; may be null
     * @return true if the colors are the same by value
     */
    private static boolean sameColor(ColorPtOstDot a, ColorPtOstDot b) {
        // Same instance (or both null) needs no further comparison
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        // Hue number plus fill-order number identify a color uniquely
        return a.getStdCodeHueNum() == b.getStdCodeHueNum()
                && a.getFillOrderNum() == b.getFillOrderNum();
    }

    /**
     * Converts this bookmark to a named saved palette, using the bookmark's label as the palette name.
     * Bookmarks and saved palettes are separate concepts; this method exists so a bookmark can later be
     * promoted to a saved palette without any other conversion code.
     *
     * @return A new PaletteWName holding a copy of the colors
     */
    public PaletteWName toPaletteWName() {
        return new PaletteWName(colors, label);
    }

    /**
     * Returns a short description for logging and debugging.
     *
     * @return The label, kind, and size
     */
    @Override
    public String toString() {
        return "PaletteBookmark[" + label + ", kind=" + kind + ", size=" + colors.length + "]";
    }
}
