package csurf.cleandesign.variationmodule.varyengine.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * The six predefined frozen-shape sets — transcribed from legacy
 * {@code StratgForFreezePG1Av031.arrayofSetsOfFrozenDrawObjs}
 * (the authoritative data; its {@code getStringForMeaningOfControlLevel}
 * display strings are stale — they predate the 022123 additions of
 * shapes 13 and 7, and are not the source of truth).
 *
 * <p>031's {@code frozenSet} param (TABLE_KEY) indexes this table 0–5;
 * the stratg writes {@code frozen} flags for exactly these shape IDs.
 */
public final class FrozenSets {

    public static final int FROZEN_SET_COUNT = 6;

    private static final int[][] TABLE = {
            {8, 9, 10, 11, 13},                              // 0 — nose + accentCrease(+13)
            {1, 2, 3, 4, 7, 12, 18},                         // 1 — light center column (+7)
            {5, 6, 7, 19, 20, 21},                           // 2 — dark/mid center column
            {12, 13, 14, 15, 16, 17},                        // 3 — entire right column
            {1, 2, 3, 4, 5, 6, 7, 12, 18, 19, 20, 21},       // 4 — entire center column
            {15, 17},                                        // 5 — eyeCorner + cheekFarCorner accents
    };

    private FrozenSets() {
    } // end private constructor — static table only

    /** The shape IDs frozen by set {@code 0..5}, as an unmodifiable set. */
    public static Set<Integer> get(int setIndex) {
        if (setIndex < 0 || setIndex >= TABLE.length) {
            throw new IllegalArgumentException(
                    "frozenSet index " + setIndex + " out of range 0.." + (TABLE.length - 1));
        } // end IF
        Set<Integer> shapes = new LinkedHashSet<>();
        for (int shape : TABLE[setIndex]) {
            shapes.add(shape);
        } // end FOR
        return Collections.unmodifiableSet(shapes);
    } // end method get

    /** Raw row for a table diff — copy, so the table cannot be mutated. */
    public static int[] row(int setIndex) {
        if (setIndex < 0 || setIndex >= TABLE.length) {
            throw new IllegalArgumentException(
                    "frozenSet index " + setIndex + " out of range 0.." + (TABLE.length - 1));
        } // end IF
        return TABLE[setIndex].clone();
    } // end method row

} // end class FrozenSets
