package com.csurf.variationmodule.model;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.Template;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Builds a {@link SmartPic} from a chosen {@link Template} plus an AGprefs
 * defaults file.
 *
 * <p>Positional contract: row N of the file's {@code shapes} array applies
 * to the Nth entry of {@link Template#getAllDrawObjs()}. The SmartPic's
 * {@link SmartDrawObj} names come from the Template's DrawObjs — the file's
 * {@code shapeName} fields are informational labels only; a mismatch is
 * logged as a warning (Template is authoritative) but does not fail.
 *
 * <p>File format (JSON):
 * <pre>{@code
 * { "name": "...", "description": "...",
 *   "shapes": [ { "shape": 1, "shapeName": "...", "valueTarget": 0.88,
 *                 "defaultAG": 1, "agPrefs": [p1, p2, p3, p4] }, ... ] }
 * }</pre>
 *
 * <p>Validation: {@code shapes} must be non-empty; each row's {@code shape}
 * must equal its 1-based position; {@code defaultAG} must be 1..4;
 * {@code agPrefs} must have exactly 4 weights; and the row count must equal
 * the Template's draw-object count.
 */
public final class SmartPicFactory {

    private static final Logger LOG = Logger.getLogger(SmartPicFactory.class.getName());

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Loads and validates an AGprefs defaults file.
     *
     * @param agPrefsFile path to the JSON defaults file
     * @return the parsed document
     * @throws SmartPicFactoryException if the file cannot be read or fails validation
     */
    public AgPrefsDocument loadAgPrefs(Path agPrefsFile) {
        Objects.requireNonNull(agPrefsFile, "agPrefsFile must not be null");
        AgPrefsDocument doc;
        try {
            doc = mapper.readValue(agPrefsFile.toFile(), AgPrefsDocument.class);
        } catch (IOException e) {
            throw new SmartPicFactoryException("Failed to read AGprefs file: " + agPrefsFile, e);
        }
        validate(doc, agPrefsFile);
        return doc;
    }

    /**
     * Creates a SmartPic for the given Template using the given defaults file.
     *
     * @param template    the chosen Template; provides shape order and names
     * @param agPrefsFile path to the JSON defaults file
     * @return the new SmartPic
     * @throws SmartPicFactoryException if the file is invalid or its row
     *                                  count differs from the Template's draw-object count
     */
    public SmartPic createSmartPic(Template template, Path agPrefsFile) {
        Objects.requireNonNull(template, "template must not be null");
        AgPrefsDocument doc = loadAgPrefs(agPrefsFile);

        List<DrawObj> drawObjs = template.getAllDrawObjs();
        if (drawObjs.size() != doc.shapes.size()) {
            throw new SmartPicFactoryException("AGprefs file " + agPrefsFile
                    + " has " + doc.shapes.size() + " shape rows but Template has "
                    + drawObjs.size() + " draw objects");
        }

        List<SmartDrawObj> smartDrawObjs = new ArrayList<>(doc.shapes.size());
        for (int i = 0; i < doc.shapes.size(); i++) {
            ShapeRow row = doc.shapes.get(i);
            DrawObj drawObj = drawObjs.get(i);
            String templateName = drawObj.getName();
            if (row.shapeName != null && templateName != null
                    && !row.shapeName.equals(templateName)) {
                LOG.warning("AGprefs file " + agPrefsFile + " shape " + row.shape
                        + " named '" + row.shapeName + "' but Template draw object is named '"
                        + templateName + "' — using Template name (positional contract)");
            }
            smartDrawObjs.add(new SmartDrawObj(i + 1, templateName,
                    new ShapeColorPrefs(row.valueTarget, row.defaultAG, row.agPrefs)));
        }
        return new SmartPic(smartDrawObjs);
    }

    private void validate(AgPrefsDocument doc, Path file) {
        if (doc.shapes == null || doc.shapes.isEmpty()) {
            throw new SmartPicFactoryException("AGprefs file " + file + " has no 'shapes' rows");
        }
        for (int i = 0; i < doc.shapes.size(); i++) {
            ShapeRow row = doc.shapes.get(i);
            if (row == null) {
                throw new SmartPicFactoryException("AGprefs file " + file + " has a null row at index " + i);
            }
            int expected = i + 1;
            if (row.shape != expected) {
                throw new SmartPicFactoryException("AGprefs file " + file + " row " + i
                        + " has shape=" + row.shape + ", expected " + expected
                        + " (rows must be ordered 1..N)");
            }
            if (row.defaultAG < 1 || row.defaultAG > ShapeColorPrefs.NUM_AGS) {
                throw new SmartPicFactoryException("AGprefs file " + file + " shape " + row.shape
                        + " has defaultAG=" + row.defaultAG + ", expected 1.." + ShapeColorPrefs.NUM_AGS);
            }
            if (row.agPrefs == null || row.agPrefs.length != ShapeColorPrefs.NUM_AGS) {
                throw new SmartPicFactoryException("AGprefs file " + file + " shape " + row.shape
                        + " must have exactly " + ShapeColorPrefs.NUM_AGS + " agPrefs weights");
            }
        }
    }

    // ---------- JSON DTOs (file representation only; names here are informational) ----------

    /** Parsed AGprefs defaults document. */
    public static final class AgPrefsDocument {
        /** Label for menus, e.g. "authored-defaults-v1". */
        public String name;
        /** Human-readable provenance/notes. */
        public String description;
        /** Ordered shape rows; position is the contract with the Template. */
        public List<ShapeRow> shapes;

        /** Display name for UI menus; falls back to the toString if absent. */
        public String displayName() {
            return name != null ? name : "(unnamed AGprefs)";
        }
    }

    /** One parsed shape row from the defaults file. */
    public static final class ShapeRow {
        /** 1-based shape number; must equal the row's position in the array. */
        public int shape;
        /** Informational label only — the Template's DrawObj name is authoritative. */
        public String shapeName;
        /** Authored target value (lightness) for the shape. */
        public double valueTarget;
        /** Authored default anchor group, 1..4. */
        public int defaultAG;
        /** Preference weights for AG1..AG4. */
        public double[] agPrefs;
    }
}
