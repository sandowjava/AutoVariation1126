package com.csurf.variationmodule.model;

import java.util.UUID;

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
 * <p>{@code drawObjUuid} is the identity of the Template DrawObj this
 * object wraps — the binding key for formatVersion 2 defaults files. It is
 * always populated: DrawObjs without a stored UUID receive a fresh one at
 * Template load time.
 *
 * <p>Domain object: no dependency on the variation engine.
 */
public final class SmartDrawObj {

    private final int shapeNum;
    private final String name;
    private final UUID drawObjUuid;
    private final ShapeColorPrefs colorPrefs;

    /**
     * @param shapeNum    1-based positional shape index (draw order)
     * @param name        the DrawObj name from the Template
     * @param drawObjUuid the UUID of the Template DrawObj this object wraps
     * @param colorPrefs  the authored color preferences for this shape
     */
    public SmartDrawObj(int shapeNum, String name, UUID drawObjUuid,
                        ShapeColorPrefs colorPrefs) {
        if (shapeNum < 1) {
            throw new IllegalArgumentException("shapeNum " + shapeNum + " must be >= 1");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (drawObjUuid == null) {
            throw new IllegalArgumentException("drawObjUuid must not be null");
        }
        if (colorPrefs == null) {
            throw new IllegalArgumentException("colorPrefs must not be null");
        }
        this.shapeNum = shapeNum;
        this.name = name;
        this.drawObjUuid = drawObjUuid;
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

    /** The UUID of the Template DrawObj this object wraps. */
    public UUID drawObjUuid() {
        return this.drawObjUuid;
    }

    /** The authored color preferences for this shape. */
    public ShapeColorPrefs colorPrefs() {
        return this.colorPrefs;
    }

    @Override
    public String toString() {
        return "SmartDrawObj{shapeNum=" + shapeNum + ", name=" + name
                + ", drawObjUuid=" + drawObjUuid + ", " + colorPrefs + "}";
    }
}
