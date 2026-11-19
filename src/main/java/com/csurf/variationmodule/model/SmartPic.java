package com.csurf.variationmodule.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An ordered set of {@link SmartDrawObj}s keyed by 1-based shape number —
 * the graphic-intelligence view of a chosen Template.
 *
 * <p>Built by SmartPicFactory from a Template (shape order + names) plus a
 * parsed AGprefs defaults file (color preferences). Immutable after
 * construction.
 *
 * <p>Domain object: deliberately independent of the variation engine; an
 * adapter crosses the engine boundary when facts are needed.
 */
public final class SmartPic {

    private final Map<Integer, SmartDrawObj> smartDrawObjsByShapeNum;

    /**
     * @param smartDrawObjs shape objects in shape-number order; each is keyed
     *                      by its own {@link SmartDrawObj#shapeNum()}
     */
    public SmartPic(Iterable<SmartDrawObj> smartDrawObjs) {
        if (smartDrawObjs == null) {
            throw new IllegalArgumentException("smartDrawObjs must not be null");
        }
        Map<Integer, SmartDrawObj> map = new LinkedHashMap<>();
        for (SmartDrawObj sdo : smartDrawObjs) {
            if (sdo == null) {
                throw new IllegalArgumentException("smartDrawObjs contains a null element");
            }
            if (map.put(sdo.shapeNum(), sdo) != null) {
                throw new IllegalArgumentException("duplicate shapeNum " + sdo.shapeNum());
            }
        }
        if (map.isEmpty()) {
            throw new IllegalArgumentException("smartDrawObjs must not be empty");
        }
        this.smartDrawObjsByShapeNum = Collections.unmodifiableMap(map);
    }

    /** Number of shapes in this SmartPic. */
    public int numShapes() {
        return this.smartDrawObjsByShapeNum.size();
    }

    /** The smart draw object for the given 1-based shape number. */
    public SmartDrawObj smartDrawObj(int shapeNum) {
        SmartDrawObj sdo = this.smartDrawObjsByShapeNum.get(shapeNum);
        if (sdo == null) {
            throw new IllegalArgumentException("no shape numbered " + shapeNum);
        }
        return sdo;
    }

    /** All smart draw objects in shape-number order (unmodifiable). */
    public Map<Integer, SmartDrawObj> smartDrawObjs() {
        return this.smartDrawObjsByShapeNum;
    }

    @Override
    public String toString() {
        return "SmartPic{numShapes=" + numShapes() + "}";
    }
}
