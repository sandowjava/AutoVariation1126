package com.csurf.variationmodule.controller;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.Template;
import com.csurf.variationmodule.model.ShapeColorPrefs;
import com.csurf.variationmodule.model.SmartPicFactory;
import com.csurf.variationmodule.model.SmartPicFactory.AgPrefsDocument;
import com.csurf.variationmodule.model.SmartPicFactory.ShapeRow;
import com.csurf.variationmodule.model.SmartPicFactoryException;
import com.csurf.variationmodule.varyengine.model.AnchorHueSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.Set;

/**
 * Automated creation of a tailored RelationMatrix: given a Template that has
 * exactly four distinct hues "in use" and a user-designated {@link AnchorHueSet}
 * (which hue is h1..h4), produces an {@link AgPrefsDocument} —
 * {@code formatVersion} 2, UUID-keyed rows in Template draw order — suitable
 * for saving to {@code dev_storage/agprefs} and consuming unchanged by the
 * existing Create SmartPic flow.
 *
 * <p>Core simplifying assumption (v1): the Template has exactly four distinct
 * occupied hues, so every DrawObj's hue exactly matches one of the four
 * anchor hues. This makes {@code defaultAG} assignment a direct lookup —
 * no hue-distance arithmetic. See
 * {@code 112326_v1H_SWE-2_create_RMatrix_from_Template.md}.
 *
 * <p>Per-shape rule ({@link #createOneShapeColorPrefs}):
 * <ul>
 *   <li>{@code defaultAG} = the AG (1..4) whose anchor hue equals the
 *       DrawObj's user-facing hue (exact match; guaranteed by the
 *       four-occupied-hues guard).</li>
 *   <li>{@code agPrefs} = the constant row of {@link #AGPREFS_BY_DEFAULT_AG}
 *       for that default AG — a permutation of {0.95, 0.65, 0.35, 0.05}
 *       (no ties).</li>
 *   <li>{@code valueTarget} = {@link DrawObj#getRGBLum3D()} — the RGB
 *       luminance of the DrawObj's fill color, [0.0, 1.0].</li>
 * </ul>
 *
 * <p>Placed in {@code controller} rather than {@code model} so the
 * model package stays free of the varyengine {@link AnchorHueSet} dependency.
 */
public final class RMatrixFactory {

    /** Number of hues a Template must have "in use" to be eligible. */
    public static final int REQUIRED_DISTINCT_HUES = ShapeColorPrefs.NUM_AGS;

    /**
     * Constant affinity table, indexed {@code [defaultAG - 1][ag - 1]} —
     * the AGpref weight a ShapeColorPrefs gives each AG when its defaultAG
     * is the row index. Every row is a permutation of {0.95, 0.65, 0.35, 0.05}:
     * own AG = 0.95, near-complement partner = 0.05, analogous partner = 0.65,
     * remaining cross pair = 0.35. Applies in all cases (v1 simplification).
     */
    private static final double[][] AGPREFS_BY_DEFAULT_AG = {
            {0.95, 0.05, 0.65, 0.35}, // defaultAG 1
            {0.05, 0.95, 0.35, 0.65}, // defaultAG 2
            {0.65, 0.05, 0.95, 0.35}, // defaultAG 3
            {0.05, 0.65, 0.35, 0.95}  // defaultAG 4
    };

    /**
     * The distinct user-facing hues (1..36) in use by the Template's DrawObjs,
     * in ascending order — the occupied-hue test, mirroring
     * {@code APDisplayPanel.buildOccupiedHuesArray} but computed per DrawObj.
     * DrawObjs with unset fill contribute hue 0, which the guard rejects.
     */
    public static Set<Integer> occupiedHues(Template template) {
        Objects.requireNonNull(template, "template must not be null");
        Set<Integer> hues = new TreeSet<>();
        for (DrawObj drawObj : template.getAllDrawObjs()) {
            if (drawObj != null) {
                hues.add(drawObj.getUserFacingHueNum());
            }
        }
        return hues;
    }

    /**
     * Whether the Template is eligible for tailored-RMatrix creation:
     * exactly {@value #REQUIRED_DISTINCT_HUES} distinct occupied hues and no
     * DrawObj with an unset fill color (hue 0).
     *
     * @return null if eligible, otherwise a user-facing error message
     */
    public static String eligibilityError(Template template) {
        Objects.requireNonNull(template, "template must not be null");
        List<DrawObj> drawObjs = template.getAllDrawObjs();
        if (drawObjs.isEmpty()) {
            return "Template has no draw objects.";
        }
        int unsetFills = 0;
        Set<Integer> hues = new TreeSet<>();
        for (DrawObj drawObj : drawObjs) {
            if (drawObj == null) {
                unsetFills++;
                continue;
            }
            int hue = drawObj.getUserFacingHueNum();
            if (hue == 0) {
                unsetFills++;
            } else {
                hues.add(hue);
            }
        }
        if (unsetFills > 0) {
            return "Template has " + unsetFills + " draw object(s) with no fill color. "
                    + "Every draw object must have a fill color in one of exactly "
                    + REQUIRED_DISTINCT_HUES + " hues.";
        }
        if (hues.size() != REQUIRED_DISTINCT_HUES) {
            return "Template uses " + hues.size() + " distinct hue(s) " + hues
                    + " — a tailored RMatrix requires a Template with exactly "
                    + REQUIRED_DISTINCT_HUES + " distinct hues in use.";
        }
        return null; // eligible
    }

    /**
     * Creates the {@link ShapeColorPrefs} for one DrawObj: default AG by exact
     * hue match against the anchor hues, prefs from the constant affinity
     * table, valueTarget from the fill color's RGB luminance.
     *
     * @param drawObj      the DrawObj (its hue must equal one of the anchor hues)
     * @param anchorHueSet the four anchor hues designated by the user
     * @throws SmartPicFactoryException if the DrawObj's hue matches no anchor hue
     */
    public ShapeColorPrefs createOneShapeColorPrefs(DrawObj drawObj, AnchorHueSet anchorHueSet) {
        Objects.requireNonNull(drawObj, "drawObj must not be null");
        Objects.requireNonNull(anchorHueSet, "anchorHueSet must not be null");
        int hue = drawObj.getUserFacingHueNum();
        int defaultAG = 0;
        for (int ag = 1; ag <= ShapeColorPrefs.NUM_AGS; ag++) {
            if (anchorHueSet.hueNumForAG(ag) == hue) {
                defaultAG = ag;
                break;
            }
        }
        if (defaultAG == 0) {
            throw new SmartPicFactoryException("DrawObj '" + drawObj.getName()
                    + "' has hue " + hue + " which matches no anchor hue "
                    + anchorHueSet);
        }
        return new ShapeColorPrefs(
                drawObj.getRGBLum3D(),
                defaultAG,
                AGPREFS_BY_DEFAULT_AG[defaultAG - 1].clone());
    }

    /**
     * Creates the tailored RelationMatrix as a {@code formatVersion} 2
     * {@link AgPrefsDocument}: one UUID-keyed row per DrawObj, in Template
     * draw order.
     *
     * @param template     the source Template (must satisfy
     *                     {@link #eligibilityError})
     * @param anchorHueSet the four anchor hues designated by the user
     * @return the document; caller sets {@code name}/{@code description}
     *         and writes via {@link SmartPicFactory#writeAgPrefs}
     * @throws SmartPicFactoryException if the Template is ineligible or a
     *                                  DrawObj's hue matches no anchor hue
     */
    public AgPrefsDocument createAllShapeColorPrefs(Template template, AnchorHueSet anchorHueSet) {
        Objects.requireNonNull(template, "template must not be null");
        String error = eligibilityError(template);
        if (error != null) {
            throw new SmartPicFactoryException(error);
        }

        AgPrefsDocument doc = new AgPrefsDocument();
        doc.formatVersion = AgPrefsDocument.FORMAT_UUID;
        doc.shapes = new ArrayList<>(template.getAllDrawObjs().size());

        int shapeNum = 0;
        for (DrawObj drawObj : template.getAllDrawObjs()) {
            shapeNum++;
            ShapeColorPrefs prefs = createOneShapeColorPrefs(drawObj, anchorHueSet);
            ShapeRow row = new ShapeRow();
            row.shape = shapeNum;
            row.shapeName = drawObj.getName();
            row.drawObjId = drawObj.getUniqueId().toString();
            row.valueTarget = prefs.valueTarget();
            row.defaultAG = prefs.defaultAG();
            row.agPrefs = prefs.agPrefs();
            doc.shapes.add(row);
        }
        return doc;
    }
} // end class RMatrixFactory
