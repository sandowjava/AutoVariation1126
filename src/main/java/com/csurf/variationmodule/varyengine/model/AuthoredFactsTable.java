package csurf.cleandesign.variationmodule.varyengine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * The authored per-shape fact table — transcribed constant-for-constant
 * from {@code SmartPicShapeRoster2.setColorFuncAttribsForAllShapes}
 * (the {@code setAnchorGroupPrefs} + {@code setValueTarget} literals,
 * lines ~568–694). This is slice-1's golden fixture: the step-8
 * fact-extraction seam must produce a {@link Facts} equal to this.
 *
 * <p>Row fields: {@code (shape, name, valueTarget, defaultAG, pref1..4)} —
 * prefs are the four AG weights in {@code prefsAmongAGs[1..4]}.
 *
 * <p>Notable: shape 18 (eyeBallWhite) has authored {@code defaultAG=1}
 * but {@code argmax(pref)=4} — {@link #driftWarnings()} reports it.
 * The default AG is authoritative regardless: 921 level 4 reads the
 * authored value and computes no argmax (OQ9).
 */
public final class AuthoredFactsTable implements Facts {

    /** One authored row: name, valueTarget, defaultAG, pref weights for AG 1–4. */
    public static final class Row {
        public final int shape;
        public final String name;
        public final double valueTarget;
        public final int defaultAG;
        public final double[] agPrefs;   // [ag-1] → weight for ag 1..4

        private Row(int shape, String name, double valueTarget, int defaultAG,
                    double pref1, double pref2, double pref3, double pref4) {
            this.shape = shape;
            this.name = name;
            this.valueTarget = valueTarget;
            this.defaultAG = defaultAG;
            this.agPrefs = new double[]{pref1, pref2, pref3, pref4};
        } // end private constructor
    } // end class Row

    /** Transcription of the roster's per-shape literals, in shape order 1..21. */
    private static final Row[] ROWS = {
            row(1,  "forehead",          0.88, 1, 0.95, 0.75, 0.40, 0.00),
            row(2,  "lidUpper",          0.64, 1, 0.95, 0.75, 0.40, 0.00),
            row(3,  "belowEyeLight",     0.83, 1, 0.95, 0.75, 0.40, 0.00),
            row(4,  "lightUnderEyeBrow", 0.91, 1, 0.95, 0.75, 0.40, 0.00),
            row(5,  "areaNearNose sloping away", 0.80, 2, 0.75, 0.95, 0.40, 0.10),
            row(6,  "darkUnderEyeBrow",  0.72, 2, 0.75, 0.95, 0.40, 0.10),
            row(7,  "lwrBag",            0.78, 2, 0.75, 0.95, 0.40, 0.10),
            row(8,  "templeDiagRect",    0.62, 2, 0.75, 0.95, 0.40, 0.10),
            row(9,  "aboveNoseForeHead", 0.82, 3, 0.55, 0.60, 0.90, 0.20),
            row(10, "noseDentCtr",       0.58, 3, 0.55, 0.60, 0.90, 0.20),
            row(11, "noseMainLwr",       0.71, 3, 0.55, 0.60, 0.90, 0.20),
            row(12, "noseSide verticalTriangle", 0.55, 2, 0.75, 0.95, 0.40, 0.10),
            row(13, "accentCrease",      0.72, 4, 0.35, 0.40, 0.60, 0.90),
            row(14, "cheekUprCurve",     0.70, 4, 0.35, 0.40, 0.60, 0.90),
            row(15, "eyeCornerTriangle", 0.57, 4, 0.35, 0.40, 0.60, 0.90),
            row(16, "cheekBtmDarker",    0.60, 4, 0.35, 0.40, 0.60, 0.90),
            row(17, "cheekFarCorner",    0.44, 4, 0.35, 0.40, 0.60, 0.90),
            row(18, "eyeBallWhite",      0.97, 1, 0.35, 0.40, 0.60, 0.90),
            row(19, "eyeBrow",           0.22, 4, 0.35, 0.40, 0.60, 0.90),
            row(20, "pupil",             0.33, 2, 0.75, 0.95, 0.00, 0.00),
            row(21, "upperLash",         0.05, 4, 0.35, 0.40, 0.60, 0.90),
    };

    private static final AuthoredFactsTable INSTANCE = new AuthoredFactsTable();

    public static AuthoredFactsTable instance() {
        return INSTANCE;
    } // end method instance

    private AuthoredFactsTable() {
    } // end private constructor

    private static Row row(int shape, String name, double valueTarget, int defaultAG,
                           double p1, double p2, double p3, double p4) {
        return new Row(shape, name, valueTarget, defaultAG, p1, p2, p3, p4);
    } // end method row

    /** The full transcribed rows — for table diffs and reports. */
    public Row[] rows() {
        return ROWS.clone();
    } // end method rows

    private Row rowFor(int shape) {
        if (shape < 1 || shape > ROWS.length) {
            throw new IllegalArgumentException("shape " + shape + " out of range 1.." + ROWS.length);
        } // end IF
        return ROWS[shape - 1];
    } // end method rowFor

    // ---------- Facts ----------

    @Override
    public int numShapes() {
        return ROWS.length;
    } // end method numShapes

    @Override
    public Set<Integer> shapeIds() {
        Set<Integer> ids = new LinkedHashSet<>();
        for (int i = 1; i <= ROWS.length; i++) {
            ids.add(i);
        } // end FOR
        return Collections.unmodifiableSet(ids);
    } // end method shapeIds

    @Override
    public int defaultAG(int shape) {
        return rowFor(shape).defaultAG;
    } // end method defaultAG

    @Override
    public double agPref(int shape, int ag) {
        if (ag < 1 || ag > 4) {
            throw new IllegalArgumentException("ag " + ag + " out of range 1..4");
        } // end IF
        return rowFor(shape).agPrefs[ag - 1];
    } // end method agPref

    @Override
    public double valueTarget(int shape) {
        return rowFor(shape).valueTarget;
    } // end method valueTarget

    @Override
    public String shapeName(int shape) {
        return rowFor(shape).name;
    } // end method shapeName

    // ---------- optional drift guard (warn-only; authored value stays authoritative) ----------

    /** The argmax AG over the four authored pref weights — for the drift guard only. */
    public int argmaxPrefAG(int shape) {
        double[] prefs = rowFor(shape).agPrefs;
        int best = 1;
        for (int ag = 2; ag <= 4; ag++) {
            if (prefs[ag - 1] > prefs[best - 1]) {
                best = ag;
            } // end IF
        } // end FOR
        return best;
    } // end method argmaxPrefAG

    /**
     * Shapes where authored {@code defaultAG} ≠ {@code argmax(prefs)} —
     * a warn-only drift guard for extraction (never a failure):
     * the authored default AG is the semantic value (OQ9).
     */
    public List<String> driftWarnings() {
        List<String> warnings = new ArrayList<>();
        for (Row r : ROWS) {
            int argmax = argmaxPrefAG(r.shape);
            if (argmax != r.defaultAG) {
                warnings.add("shape " + r.shape + " (" + r.name + "): authored defaultAG="
                        + r.defaultAG + " but argmax(prefs)=" + argmax
                        + " — authored value is authoritative (no argmax in 921)");
            } // end IF
        } // end FOR
        return warnings;
    } // end method driftWarnings

} // end class AuthoredFactsTable
