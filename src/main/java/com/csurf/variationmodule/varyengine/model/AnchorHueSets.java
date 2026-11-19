package csurf.cleandesign.variationmodule.varyengine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * The 25-row anchor-hue table — transcribed constant-for-constant from
 * legacy {@code UserInputs.fillTheArrayOfAGhueSets()} (rows 0–24).
 * Rows 1–12 were generated there as {@code (4,5,2,7) + hueIncr} for
 * increments 0,2,…,22; they are transcribed as the literal result values,
 * matching what the legacy array actually holds.
 *
 * <p>555's {@code hueSet} param (TABLE_KEY) indexes this table:
 * "AG → that AG's center hue from the selected set" (OQ4 — {@code hueIncr}
 * does not exist; 555's controlLevel is vestigial).
 */
public final class AnchorHueSets {

    public static final int HUE_SET_COUNT = 25;

    private static final AnchorHueSet[] TABLE = {
            set(6, 9, 4, 2),    //  0 — literal (unchanged in legacy)
            set(4, 5, 2, 7),    //  1 — hueCtr(4,5,2,7) + 0
            set(6, 7, 4, 9),    //  2 — +2
            set(8, 9, 6, 11),   //  3 — +4
            set(10, 11, 8, 13), //  4 — +6
            set(12, 13, 10, 15),//  5 — +8
            set(14, 15, 12, 17),//  6 — +10
            set(16, 17, 14, 19),//  7 — +12
            set(18, 19, 16, 21),//  8 — +14
            set(20, 21, 18, 23),//  9 — +16
            set(22, 23, 20, 25),// 10 — +18
            set(24, 25, 22, 27),// 11 — +20
            set(26, 27, 24, 29),// 12 — +22
            set(5, 3, 6, 8),    // 13 — literal: "very nice w yellow-tan"
            set(3, 6, 8, 5),    // 14 — literal: "nice w yellow-red"
            set(10, 6, 8, 13),  // 15 — literal
            set(6, 8, 13, 10),  // 16 — literal: "nice w tan-magenta"
            set(8, 13, 10, 6),  // 17 — literal: "nice w magenta"
            set(13, 10, 6, 8),  // 18 — literal: "nice w yellow-magenta"
            set(6, 9, 4, 2),    // 19 — literal (dup of 0)
            set(8, 2, 4, 9),    // 20 — literal: "nice w yellow"
            set(8, 5, 3, 6),    // 21 — literal (dup of 12's base family)
            set(6, 9, 4, 11),   // 22 — literal
            set(4, 5, 2, 7),    // 23 — literal (dup of 1)
            set(4, 2, 6, 9),    // 24 — literal
    };

    private AnchorHueSets() {
    } // end private constructor — static table only

    private static AnchorHueSet set(int h1, int h2, int h3, int h4) {
        return new AnchorHueSet(h1, h2, h3, h4);
    } // end method set

    /** The hue set at index {@code 0..24}; throws for an out-of-range key. */
    public static AnchorHueSet get(int setIndex) {
        if (setIndex < 0 || setIndex >= TABLE.length) {
            throw new IllegalArgumentException(
                    "hueSet index " + setIndex + " out of range 0.." + (TABLE.length - 1));
        } // end IF
        return TABLE[setIndex];
    } // end method get

    /** All 25 rows, in index order — for table diffs. */
    public static List<AnchorHueSet> all() {
        List<AnchorHueSet> list = new ArrayList<>();
        Collections.addAll(list, TABLE);
        return Collections.unmodifiableList(list);
    } // end method all

} // end class AnchorHueSets
