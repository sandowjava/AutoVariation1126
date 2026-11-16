package com.csurf.templatecore.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Chronological list of {@link PaletteBookmark} checkpoints with a marker. This class holds all of the
 * logic for UNDO and REDO of palette changes. It has no Swing, event, or DrawObjRoster dependencies,
 * so it can be tested in isolation. A controller (for example the PaletteOperationHub) is expected to
 * own one instance per Template, supply the live palette, and apply whichever bookmark UNDO or REDO
 * returns ("bookmark" = a palette snapshot).
 *
 * <h3>Structure</h3>
 * The entries are kept in chronological order. Index 0 is the BOTTOM of the list and holds the OLDEST
 * bookmark, which is always B0, the baseline taken when the Template was loaded. The highest index is
 * the TOP of the list and holds the NEWEST bookmark. The marker is an index into the list; it means
 * "the palette on screen is based on this bookmark". Notation used below: oldest to newest, with the
 * marker shown under its entry.
 * <pre>
 *   [B0, S1, S2, S3]
 *            ^           marker on S2 (middle of the list)
 * </pre>
 * Unlike a classic undo stack, the marker can sit anywhere, and bookmarks above the marker (the "future")
 * are never discarded just because the marker moved back.
 *
 * <h3>Rules</h3>
 * <ul>
 *   <li><b>Save</b> always appends the new bookmark at the TOP and moves the marker there, wherever the
 *       marker was, and even if the palette is identical to the bookmark under the marker. Bookmarks are
 *       always in chronological order.</li>
 *   <li><b>Capacity.</b> B0 plus {@code maxNonBaseline} others (default 9, so 10 in total). When an
 *       append would exceed capacity, the OLDEST bookmark OTHER THAN B0 (index 1) is evicted. This is the
 *       only deletion. B0 is permanently protected so the user can always return to the load-time
 *       palette.</li>
 *   <li><b>UNDO</b> moves the marker one slot toward the oldest end. <b>REDO</b> moves it one slot
 *       toward the newest end. Both always move relative to where the marker is.</li>
 *   <li><b>Dirty palette.</b> The live palette is "dirty" when it differs, by value, from the bookmark
 *       under the marker. If UNDO or REDO is used while dirty, the dirty palette is first saved at the
 *       TOP as an implicit bookmark, so nothing the user did is lost, and then the marker moves one
 *       slot from where it started. The same single rule applies whether the palette is dirty or clean.</li>
 *   <li><b>Eviction happens before the move.</b> If the implicit save evicts a bookmark, the marker
 *       moves one slot in the updated list. See the worked examples below.</li>
 * </ul>
 *
 * <h3>Enabling rules for menu items</h3>
 * <ul>
 *   <li>UNDO is available when the marker is not on B0, or when the palette is dirty. So UNDO on B0 is
 *       allowed while dirty: it saves the dirty palette at the top, leaves the marker on B0, and returns
 *       B0. This lets a user start a series of trial colorings, each from B0.</li>
 *   <li>REDO is available when the marker is not at the top, or when the palette is dirty. REDO at the
 *       top while dirty saves the dirty palette and moves the marker to it.</li>
 * </ul>
 *
 * <h3>Worked examples</h3>
 * Undo from a dirty state with the marker on S3 (D is the dirty palette). D is saved at the top and the
 * marker moves from S3 to S2:
 * <pre>
 *   before: [B0, S1, S2, S3]         marker on S3, screen shows D
 *   after:  [B0, S1, S2, S3, D]      marker on S2, screen shows S2
 * </pre>
 * Save from the middle. The bookmark goes at the top and S2, S3 are kept, so UNDO from X goes to S3 (the
 * previous bookmark in time), not back to S1:
 * <pre>
 *   before: [B0, S1, S2, S3]         marker on S1, screen shows X
 *   after:  [B0, S1, S2, S3, X]      marker on X (top)
 * </pre>
 * Full list (capacity 5 in this example), UNDO while dirty with the marker on S2. The implicit save evicts
 * S1 first, so the marker moves from S2 to B0:
 * <pre>
 *   before: [B0, S1, S2, S3, S4]     marker on S2, screen shows D
 *   save D, S1 evicted:  [B0, S2, S3, S4, D]   marker starts on S2
 *   after:  marker on B0, screen shows B0
 * </pre>
 * If the marker was on the very bookmark that gets evicted, treat the marker as sitting in the gap where
 * it used to be: UNDO lands on B0, and REDO lands on the bookmark that was above it.
 *
 * <h3>Safeguards</h3>
 * Every bookmark given to this class must have the same size as the baseline, because palettes are
 * matched to DrawObjs by index. A bookmark of a different size is rejected with an
 * {@link IllegalArgumentException}.
 *
 * <h3>Not handled here</h3>
 * This class does not apply colors to DrawObjs, fire events, update menus, or know about the selection or
 * the EV-rotation baseline. The caller applies the returned bookmark and performs those updates. Not
 * thread-safe; intended for use on the Swing event thread.
 *
 * @see PaletteBookmark
 */
public class PaletteBookmarkList {

    /** Default number of bookmarks that can be held in addition to the protected baseline B0. */
    public static final int DEFAULT_MAX_NON_BASELINE = 9;

    /** Bookmarks in chronological order: index 0 is B0 (oldest), the last index is the newest. */
    private final List<PaletteBookmark> entries = new ArrayList<>();

    /** Maximum number of bookmarks other than B0; the list never holds more than 1 plus this. */
    private final int maxNonBaseline;

    /** Index of the bookmark that the palette on screen is based on. */
    private int markerIndex;

    /**
     * Creates a list holding only the baseline B0, with the default capacity of
     * {@link #DEFAULT_MAX_NON_BASELINE} other bookmarks. The marker starts on B0.
     *
     * @param baseline The load-time baseline (B0); must not be null
     * @throws IllegalArgumentException if baseline is null
     */
    public PaletteBookmarkList(PaletteBookmark baseline) {
        this(baseline, DEFAULT_MAX_NON_BASELINE);
    }

    /**
     * Creates a list holding only the baseline B0, with a chosen capacity. The marker starts on B0.
     * B0 does not count toward {@code maxNonBaseline}.
     *
     * @param baseline The load-time baseline (B0); must not be null
     * @param maxNonBaseline How many bookmarks other than B0 may be held; must be at least 1
     * @throws IllegalArgumentException if baseline is null or maxNonBaseline is less than 1
     */
    public PaletteBookmarkList(PaletteBookmark baseline, int maxNonBaseline) {
        if (baseline == null) {
            throw new IllegalArgumentException("Baseline bookmark cannot be null");
        }
        if (maxNonBaseline < 1) {
            throw new IllegalArgumentException("maxNonBaseline must be at least 1");
        }
        this.maxNonBaseline = maxNonBaseline;
        this.entries.add(baseline);
        this.markerIndex = 0;
    }

    /**
     * Returns the number of bookmarks currently held, including B0.
     *
     * @return The number of bookmarks
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns the capacity for bookmarks other than B0.
     *
     * @return The maximum number of non-baseline bookmarks
     */
    public int getMaxNonBaseline() {
        return maxNonBaseline;
    }

    /**
     * Returns the total capacity, which is B0 plus the non-baseline capacity.
     *
     * @return The maximum number of bookmarks the list can hold
     */
    public int getMaxSize() {
        return 1 + maxNonBaseline;
    }

    /**
     * Returns the marker position. 0 is the bottom (B0, oldest) and size() minus 1 is the top (newest).
     *
     * @return The marker index
     */
    public int getMarkerIndex() {
        return markerIndex;
    }

    /**
     * Returns the bookmark at a list position, where 0 is the oldest (B0).
     *
     * @param index The list position
     * @return The bookmark at that position
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public PaletteBookmark get(int index) {
        return entries.get(index);
    }

    /**
     * Returns B0, the load-time baseline. It is always at index 0 and is never evicted.
     *
     * @return The baseline bookmark
     */
    public PaletteBookmark getBaseline() {
        return entries.get(0);
    }

    /**
     * Returns the bookmark under the marker, which is the one the palette on screen is based on.
     *
     * @return The bookmark at the marker
     */
    public PaletteBookmark getCurrentBookmark() {
        return entries.get(markerIndex);
    }

    /**
     * Returns a read-only copy of all bookmarks, oldest first. Intended for a future bookmark-list display.
     * Later changes to this list do not affect the returned copy.
     *
     * @return An unmodifiable list of the bookmarks, index 0 being B0
     */
    public List<PaletteBookmark> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * Tells whether the marker is on B0, the bottom of the list.
     *
     * @return true if the marker is on B0
     */
    public boolean isMarkerOnBaseline() {
        return markerIndex == 0;
    }

    /**
     * Tells whether the marker is on the newest bookmark, the top of the list.
     *
     * @return true if the marker is at the top
     */
    public boolean isMarkerAtTop() {
        return markerIndex == entries.size() - 1;
    }

    /**
     * Tells whether the live palette differs from the bookmark under the marker. The comparison is by
     * value (hue number and fill-order number of every color), so a palette that was changed and then
     * changed back is correctly reported as clean.
     *
     * @param currentPalette The live palette, captured with PaletteBookmark.fromRoster; must match the baseline size
     * @return true if the live palette differs from the bookmark at the marker
     * @throws IllegalArgumentException if currentPalette is null or has the wrong size
     */
    public boolean isDirty(PaletteBookmark currentPalette) {
        requireCompatible(currentPalette);
        return !currentPalette.sameColorsAs(entries.get(markerIndex));
    }

    /**
     * Tells whether UNDO should be enabled: the marker is not on B0, or the palette is dirty.
     *
     * @param currentPalette The live palette
     * @return true if {@link #undo(PaletteBookmark)} would move or save something
     */
    public boolean canUndo(PaletteBookmark currentPalette) {
        return markerIndex > 0 || isDirty(currentPalette);
    }

    /**
     * Tells whether REDO should be enabled: the marker is not at the top, or the palette is dirty.
     *
     * @param currentPalette The live palette
     * @return true if {@link #redo(PaletteBookmark)} would move or save something
     */
    public boolean canRedo(PaletteBookmark currentPalette) {
        return markerIndex < entries.size() - 1 || isDirty(currentPalette);
    }

    /**
     * Explicit user save. Always appends the bookmark at the TOP (newest end) and moves the marker to it.
     * There are no exceptions: a save from the middle keeps all bookmarks above the marker, and a save
     * of a palette identical to the one under the marker still adds a new entry. If the list is full,
     * the oldest bookmark other than B0 is evicted.
     *
     * @param bookmark The palette to save; must match the baseline size
     * @throws IllegalArgumentException if bookmark is null or has the wrong size
     */
    public void save(PaletteBookmark bookmark) {
        requireCompatible(bookmark);
        appendWithEviction(bookmark);
        markerIndex = entries.size() - 1;
    }

    /**
     * Moves the marker one slot toward the oldest end. If the live palette is dirty it is first saved
     * at the top as an implicit bookmark (which may evict the oldest non-B0 bookmark), then the marker
     * moves one slot from where it started. On B0 with a dirty palette, the palette is saved and the
     * marker stays on B0, so the caller shows B0 again.
     *
     * @param currentPalette The live palette, used to detect whether it is dirty
     * @return The bookmark the caller should now put on screen, or empty if UNDO is not available
     *         (marker on B0 and the palette is clean)
     * @throws IllegalArgumentException if currentPalette is null or has the wrong size
     */
    public Optional<PaletteBookmark> undo(PaletteBookmark currentPalette) {
        boolean dirty = isDirty(currentPalette);
        // Nothing older than B0 exists and there is nothing unsaved to preserve
        if (markerIndex == 0 && !dirty) {
            return Optional.empty();
        }
        if (dirty) {
            implicitSave(currentPalette);
        }
        // Never moves below B0; on B0 with a dirty palette the marker stays on B0
        markerIndex = Math.max(0, markerIndex - 1);
        return Optional.of(entries.get(markerIndex));
    }

    /**
     * Moves the marker one slot toward the newest end. If the live palette is dirty it is first saved
     * at the top as an implicit bookmark (which may evict the oldest non-B0 bookmark), then the marker
     * moves one slot from where it started. At the top with a dirty palette, the marker moves to the
     * newly saved bookmark, so REDO then acts like a save.
     *
     * @param currentPalette The live palette, used to detect whether it is dirty
     * @return The bookmark the caller should now put on screen, or empty if REDO is not available
     *         (marker at the top and the palette is clean)
     * @throws IllegalArgumentException if currentPalette is null or has the wrong size
     */
    public Optional<PaletteBookmark> redo(PaletteBookmark currentPalette) {
        boolean dirty = isDirty(currentPalette);
        // Already at the newest bookmark and there is nothing unsaved to preserve
        if (markerIndex == entries.size() - 1 && !dirty) {
            return Optional.empty();
        }
        if (dirty) {
            implicitSave(currentPalette);
        }
        // After an implicit save at the top, the newest bookmark is the newly saved dirty palette
        markerIndex = Math.min(entries.size() - 1, markerIndex + 1);
        return Optional.of(entries.get(markerIndex));
    }

    /**
     * Tells whether "go to top" should be enabled. Same rule as REDO: the marker is not at the top, or
     * the palette is dirty.
     *
     * @param currentPalette The live palette
     * @return true if {@link #goToTop(PaletteBookmark)} would move or save something
     */
    public boolean canGoToTop(PaletteBookmark currentPalette) {
        return canRedo(currentPalette);
    }

    /**
     * Tells whether "go to bottom" should be enabled. Same rule as UNDO: the marker is not on B0, or
     * the palette is dirty.
     *
     * @param currentPalette The live palette
     * @return true if {@link #goToBottom(PaletteBookmark)} would move or save something
     */
    public boolean canGoToBottom(PaletteBookmark currentPalette) {
        return canUndo(currentPalette);
    }

    /**
     * Moves the marker to the top (newest). If the live palette is dirty it is first saved at the top as
     * an implicit bookmark, exactly as for UNDO and REDO, and the marker lands on that new bookmark.
     *
     * @param currentPalette The live palette, used to detect whether it is dirty
     * @return The bookmark the caller should now put on screen, or empty if the marker is already at the
     *         top and the palette is clean
     * @throws IllegalArgumentException if currentPalette is null or has the wrong size
     */
    public Optional<PaletteBookmark> goToTop(PaletteBookmark currentPalette) {
        boolean dirty = isDirty(currentPalette);
        if (markerIndex == entries.size() - 1 && !dirty) {
            return Optional.empty();
        }
        if (dirty) {
            implicitSave(currentPalette);
        }
        markerIndex = entries.size() - 1;
        return Optional.of(entries.get(markerIndex));
    }

    /**
     * Moves the marker to the bottom (B0, the palette the Template opened with). If the live palette is
     * dirty it is first saved at the top as an implicit bookmark, exactly as for UNDO and REDO.
     *
     * @param currentPalette The live palette, used to detect whether it is dirty
     * @return The bookmark the caller should now put on screen (B0), or empty if the marker is already on
     *         B0 and the palette is clean
     * @throws IllegalArgumentException if currentPalette is null or has the wrong size
     */
    public Optional<PaletteBookmark> goToBottom(PaletteBookmark currentPalette) {
        boolean dirty = isDirty(currentPalette);
        if (markerIndex == 0 && !dirty) {
            return Optional.empty();
        }
        if (dirty) {
            implicitSave(currentPalette);
        }
        markerIndex = 0;
        return Optional.of(entries.get(markerIndex));
    }

    /**
     * Saves the dirty live palette at the top as an IMPLICIT_SAVE bookmark, without moving the marker
     * to it. Used by UNDO and REDO so that unsaved work is never lost. The marker is adjusted only if
     * an eviction shifts the entries.
     *
     * @param currentPalette The dirty live palette
     */
    private void implicitSave(PaletteBookmark currentPalette) {
        appendWithEviction(PaletteBookmark.fromColors(currentPalette.getColors(), PaletteBookmark.Kind.IMPLICIT_SAVE));
    }

    /**
     * Appends a bookmark at the top and, if that exceeds capacity, evicts the oldest bookmark other than
     * B0 (index 1). The marker keeps pointing at the same bookmark: when an older entry is removed, the
     * marker index drops by one. If the marker was on the evicted bookmark itself, it drops to B0 and
     * acts as if it sat in the gap between B0 and the next bookmark, so UNDO then lands on B0 and REDO
     * lands on the bookmark that was above the evicted one.
     *
     * @param bookmark The bookmark to append
     */
    private void appendWithEviction(PaletteBookmark bookmark) {
        entries.add(bookmark);
        if (entries.size() > getMaxSize()) {
            // Index 0 (B0) is never evicted, so the oldest evictable bookmark is index 1
            entries.remove(1);
            // Entries above the removed one slide down by one position
            if (markerIndex >= 1) {
                markerIndex--;
            }
        }
    }

    /**
     * Safeguard for the index-based palette convention. Palettes are matched to DrawObjs by position,
     * which is safe only while the number of DrawObjs never changes, so every bookmark must have the
     * same size as B0. A mismatch fails loudly instead of silently corrupting a restore.
     *
     * @param bookmark The bookmark to check
     * @throws IllegalArgumentException if the bookmark is null or its size differs from B0's
     */
    private void requireCompatible(PaletteBookmark bookmark) {
        if (bookmark == null) {
            throw new IllegalArgumentException("Bookmark cannot be null");
        }
        int expectedSize = entries.get(0).getSize();
        if (bookmark.getSize() != expectedSize) {
            throw new IllegalArgumentException("Bookmark size " + bookmark.getSize()
                    + " does not match baseline size " + expectedSize);
        }
    }
}
