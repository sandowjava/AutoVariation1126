package com.csurf.variationmodule.varyengine.model;

import com.csurf.variationmodule.model.ShapeColorPrefs;
import com.csurf.variationmodule.model.SmartDrawObj;
import com.csurf.variationmodule.model.SmartPic;

import java.util.Objects;
import java.util.Set;


/**
 * The {@link Facts} adapter over a {@link SmartPic} — the second stage
 * of the port-rehearsal boundary crossing (v1E §3): a SmartPic supplies
 * the authored {@link ShapeColorPrefs} for each shape, and this adapter
 * exposes them through the engine's read-only {@link Facts} contract.
 *
 * <p>Deliberately an <b>adapter, not {@code SmartPic implements
 * Facts}</b> (v1E D2): {@code SmartPic} is a domain object and must not
 * carry a {@code varyengine} dependency — the engine depends on facts,
 * the domain does not depend on the engine.
 *
 * <p>Behavior is identical to the retired {@code ExtractedSmartPicFacts}:
 * shape IDs keyed by {@code smartShapeNum}, {@code agPref} bounds check,
 * loud missing-shape error.
 */
public final class SmartPicFacts implements Facts {

    private final SmartPic smartPic;

    public SmartPicFacts(SmartPic smartPic) {
        this.smartPic = Objects.requireNonNull(smartPic, "smartPic");
    } // end constructor

    // ---------- Facts ----------

    @Override
    public int numShapes() {
        return smartPic.numShapes();
    } // end method numShapes

    @Override
    public Set<Integer> shapeIds() {
        return smartPic.smartDrawObjs().keySet();
    } // end method shapeIds

    @Override
    public int defaultAG(int shape) {
        return shapePrefsFor(shape).defaultAG();
    } // end method defaultAG

    @Override
    public double agPref(int shape, int ag) {
        return shapePrefsFor(shape).agPref(ag);
    } // end method agPref

    @Override
    public double valueTarget(int shape) {
        return shapePrefsFor(shape).valueTarget();
    } // end method valueTarget

    @Override
    public String shapeName(int shape) {
        return drawObjFor(shape).name();
    } // end method shapeName

    private ShapeColorPrefs shapePrefsFor(int shape) {
        return drawObjFor(shape).colorPrefs();
    } // end method shapePrefsFor

    private SmartDrawObj drawObjFor(int shape) {
        SmartDrawObj drawObj = smartPic.smartDrawObjs().get(shape);
        if (drawObj == null) {
            throw new IllegalArgumentException("shape " + shape + " not in SmartPic " + smartPic.smartDrawObjs().keySet());
        } // end IF
        return drawObj;
    } // end method drawObjFor

} // end class SmartPicFacts
