package com.csurf.variationmodule.model;

/**
 * The authored per-shape color preferences: a value target, the default
 * anchor group (AG), and a preference weight for each of the four AGs.
 *
 * <p>Immutable. Instances are created by the SmartPicFactory from a parsed
 * AGprefs defaults file; the values correspond to the legacy literals
 * authored in {@code SmartPicShapeRoster2.setColorFuncAttribsForAllShapes}.
 *
 * <p>Domain object: no dependency on the variation engine.
 */
public final class ShapeColorPrefs {

    /** Number of anchor groups the prefs weigh. */
    public static final int NUM_AGS = 4;

    private final double valueTarget;
    private final int defaultAG;
    private final double[] agPrefs; // index 0 = pref for AG1 ... index 3 = pref for AG4

    /**
     * @param valueTarget the authored target value (lightness) for the shape
     * @param defaultAG   the authored default anchor group, 1..{@value #NUM_AGS}
     * @param agPrefs     exactly {@value #NUM_AGS} preference weights for AG1..AG4
     */
    public ShapeColorPrefs(double valueTarget, int defaultAG, double[] agPrefs) {
        if (defaultAG < 1 || defaultAG > NUM_AGS) {
            throw new IllegalArgumentException("defaultAG " + defaultAG + " out of range 1.." + NUM_AGS);
        }
        if (agPrefs == null || agPrefs.length != NUM_AGS) {
            throw new IllegalArgumentException("agPrefs must have exactly " + NUM_AGS + " weights");
        }
        this.valueTarget = valueTarget;
        this.defaultAG = defaultAG;
        this.agPrefs = agPrefs.clone();
    }

    /** The authored target value (lightness) for the shape. */
    public double valueTarget() {
        return this.valueTarget;
    }

    /** The authored default anchor group, 1..4. Authoritative; never argmax of the prefs. */
    public int defaultAG() {
        return this.defaultAG;
    }

    /** The authored preference weight for the given anchor group, 1..4. */
    public double agPref(int ag) {
        if (ag < 1 || ag > NUM_AGS) {
            throw new IllegalArgumentException("ag " + ag + " out of range 1.." + NUM_AGS);
        }
        return this.agPrefs[ag - 1];
    }

    /** Defensive copy of all four preference weights, in AG order 1..4. */
    public double[] agPrefs() {
        return this.agPrefs.clone();
    }

    @Override
    public String toString() {
        return "ShapeColorPrefs{valueTarget=" + valueTarget
                + ", defaultAG=" + defaultAG
                + ", agPrefs=[" + agPrefs[0] + ", " + agPrefs[1] + ", "
                + agPrefs[2] + ", " + agPrefs[3] + "]}";
    }
}
