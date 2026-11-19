package com.csurf.variationmodule.model;

/**
 * One smart draw object: the SmartPic-side view of a Template draw object,
 * carrying the shape's authored color preferences.
 *
 * <p>{@code shapeNum} is the 1-based positional index into the Template's
 * ordered draw-object list — the shape-order contract between the Template
 * and the AGprefs defaults file. {@code name} comes from the Template's
 * DrawObj (the user's mental model: the SmartPic belongs to the chosen
 * Template), never from the defaults file.
 *
 * <p>Domain object: no dependency on the variation engine.
 */
public final class SmartDrawObj {

    private final int shapeNum;
    private final String name;
    private final ShapeColorPrefs colorPrefs;

    /**
     * @param shapeNum   1-based positional shape index
     * @param name       the DrawObj name from the Template
     * @param colorPrefs the authored color preferences for this shape
     */
    public SmartDrawObj(int shapeNum, String name, ShapeColorPrefs colorPrefs) {
        if (shapeNum < 1) {
            throw new IllegalArgumentException("shapeNum " + shapeNum + " must be >= 1");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (colorPrefs == null) {
            throw new IllegalArgumentException("colorPrefs must not be null");
        }
        this.shapeNum = shapeNum;
        this.name = name;
        this.colorPrefs = colorPrefs;
    }

    /** The 1-based positional shape index. */
    public int shapeNum() {
        return this.shapeNum;
    }

    /** The shape name, taken from the Template's DrawObj. */
    public String name() {
        return this.name;
    }

    /** The authored color preferences for this shape. */
    public ShapeColorPrefs colorPrefs() {
        return this.colorPrefs;
    }

    @Override
    public String toString() {
        return "SmartDrawObj{shapeNum=" + shapeNum + ", name=" + name + ", " + colorPrefs + "}";
    }
}
