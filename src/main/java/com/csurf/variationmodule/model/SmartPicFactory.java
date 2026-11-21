package com.csurf.variationmodule.model;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.Template;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Builds a {@link SmartPic} from a chosen {@link Template} plus an AGprefs
 * defaults file.
 *
 * <p>Two file formats are supported, distinguished by {@code formatVersion}
 * (absent = 1):
 *
 * <p><b>formatVersion 1 — positional contract.</b> Row N of the file's
 * {@code shapes} array applies to the Nth entry of
 * {@link Template#getAllDrawObjs()}.
 *
 * <p><b>formatVersion 2 — UUID contract.</b> Each row's {@code drawObjId}
 * must equal the {@code uniqueId} of a Template DrawObj. Row order is a
 * display convention only (files are written in draw order); the factory
 * ignores order and binds by UUID. See
 * {@code 112126_v0G_SWE-2_RelationMatrix_and_Editor-UI_Design.md}.
 *
 * <p>In both formats the SmartPic's {@link SmartDrawObj} names and
 * {@code shapeNum}s come from the Template's DrawObjs in draw order — the
 * file's {@code shapeName} fields are informational labels only; a mismatch
 * is logged as a warning (Template is authoritative) but does not fail.
 *
 * <p>File format (JSON):
 * <pre>{@code
 * { "name": "...", "description": "...", "formatVersion": 2,
 *   "shapes": [ { "shape": 1, "shapeName": "...",
 *                 "drawObjId": "<uuid>",   // required iff formatVersion 2
 *                 "valueTarget": 0.88,
 *                 "defaultAG": 1, "agPrefs": [p1, p2, p3, p4] }, ... ] }
 * }</pre>
 *
 * <p>Validation (both versions): {@code shapes} must be non-empty;
 * {@code defaultAG} must be 1..4; {@code agPrefs} must have exactly 4
 * weights. v1 additionally requires each row's {@code shape} to equal its
 * 1-based position and the row count to equal the Template's draw-object
 * count. v2 additionally requires every {@code drawObjId} to resolve to a
 * Template DrawObj and every Template DrawObj to have exactly one row.
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

        if (doc.formatVersion() == AgPrefsDocument.FORMAT_UUID) {
            return createSmartPicByUuid(template, drawObjs, doc, agPrefsFile);
        }
        return createSmartPicByPosition(template, drawObjs, doc, agPrefsFile);
    }

    /** v1 path: row N binds to drawObjs.get(N-1) — the positional contract. */
    private SmartPic createSmartPicByPosition(Template template, List<DrawObj> drawObjs,
                                              AgPrefsDocument doc, Path agPrefsFile) {
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
                    drawObj.getUniqueId(),
                    new ShapeColorPrefs(row.valueTarget, row.defaultAG, row.agPrefs)));
        }
        return new SmartPic(smartDrawObjs);
    }

    /**
     * v2 path: each row's {@code drawObjId} must resolve to a Template
     * DrawObj UUID; every DrawObj must have exactly one row. Output rows
     * follow Template draw order regardless of file row order.
     */
    private SmartPic createSmartPicByUuid(Template template, List<DrawObj> drawObjs,
                                          AgPrefsDocument doc, Path agPrefsFile) {
        // index rows by UUID (uniqueness checked in validate)
        Map<UUID, ShapeRow> rowsByUuid = new HashMap<>();
        for (ShapeRow row : doc.shapes) {
            rowsByUuid.put(UUID.fromString(row.drawObjId), row);
        }

        // index template DrawObjs by UUID; completeness = identical key sets
        Map<UUID, DrawObj> drawObjsByUuid = new LinkedHashMap<>();
        for (DrawObj drawObj : drawObjs) {
            drawObjsByUuid.put(drawObj.getUniqueId(), drawObj);
        }

        if (!rowsByUuid.keySet().equals(drawObjsByUuid.keySet())) {
            List<UUID> orphans = new ArrayList<>(rowsByUuid.keySet());
            orphans.removeAll(drawObjsByUuid.keySet());
            List<UUID> uncovered = new ArrayList<>(drawObjsByUuid.keySet());
            uncovered.removeAll(rowsByUuid.keySet());
            throw new SmartPicFactoryException("AGprefs file " + agPrefsFile
                    + " (formatVersion 2) does not match the Template's draw objects: "
                    + orphans.size() + " row(s) reference DrawObjs not in the Template"
                    + orphans + "; " + uncovered.size()
                    + " Template DrawObj(s) have no row" + uncovered);
        }

        List<SmartDrawObj> smartDrawObjs = new ArrayList<>(drawObjs.size());
        int shapeNum = 0;
        for (DrawObj drawObj : drawObjs) {
            shapeNum++;
            ShapeRow row = rowsByUuid.get(drawObj.getUniqueId());
            String templateName = drawObj.getName();
            if (row.shapeName != null && templateName != null
                    && !row.shapeName.equals(templateName)) {
                LOG.warning("AGprefs file " + agPrefsFile + " row for drawObjId "
                        + row.drawObjId + " named '" + row.shapeName
                        + "' but Template draw object is named '" + templateName
                        + "' — using Template name (UUID contract)");
            }
            smartDrawObjs.add(new SmartDrawObj(shapeNum, templateName,
                    drawObj.getUniqueId(),
                    new ShapeColorPrefs(row.valueTarget, row.defaultAG, row.agPrefs)));
        }
        return new SmartPic(smartDrawObjs);
    }

    /**
     * Writes an {@link AgPrefsDocument} to disk as pretty-printed JSON —
     * the single write path used by {@link RelationMatrix#save} and the
     * editor's save actions.
     *
     * @param file destination path
     * @param doc  the document to write
     * @throws SmartPicFactoryException if the file cannot be written
     */
    public static void writeAgPrefs(Path file, AgPrefsDocument doc) {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(doc, "doc must not be null");
        try {
            new ObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValue(file.toFile(), doc);
        } catch (IOException e) {
            throw new SmartPicFactoryException(
                    "Failed to write AGprefs file: " + file, e);
        }
    }

    private void validate(AgPrefsDocument doc, Path file) {
        int version = doc.formatVersion();
        if (version != AgPrefsDocument.FORMAT_POSITIONAL
                && version != AgPrefsDocument.FORMAT_UUID) {
            throw new SmartPicFactoryException("AGprefs file " + file
                    + " has unsupported formatVersion " + version
                    + " (supported: " + AgPrefsDocument.FORMAT_POSITIONAL
                    + " = positional, " + AgPrefsDocument.FORMAT_UUID + " = UUID-keyed)");
        }
        if (doc.shapes == null || doc.shapes.isEmpty()) {
            throw new SmartPicFactoryException("AGprefs file " + file + " has no 'shapes' rows");
        }
        List<UUID> seenDrawObjIds = version == AgPrefsDocument.FORMAT_UUID
                ? new ArrayList<>() : null;
        for (int i = 0; i < doc.shapes.size(); i++) {
            ShapeRow row = doc.shapes.get(i);
            if (row == null) {
                throw new SmartPicFactoryException("AGprefs file " + file + " has a null row at index " + i);
            }
            if (version == AgPrefsDocument.FORMAT_POSITIONAL) {
                int expected = i + 1;
                if (row.shape != expected) {
                    throw new SmartPicFactoryException("AGprefs file " + file + " row " + i
                            + " has shape=" + row.shape + ", expected " + expected
                            + " (rows must be ordered 1..N)");
                }
            } else {
                if (row.drawObjId == null || row.drawObjId.isEmpty()) {
                    throw new SmartPicFactoryException("AGprefs file " + file + " row " + i
                            + " has no drawObjId (required for formatVersion 2)");
                }
                UUID id;
                try {
                    id = UUID.fromString(row.drawObjId);
                } catch (IllegalArgumentException e) {
                    throw new SmartPicFactoryException("AGprefs file " + file + " row " + i
                            + " has malformed drawObjId '" + row.drawObjId + "'", e);
                }
                if (seenDrawObjIds.contains(id)) {
                    throw new SmartPicFactoryException("AGprefs file " + file + " row " + i
                            + " duplicates drawObjId " + row.drawObjId);
                }
                seenDrawObjIds.add(id);
            }
            if (row.defaultAG < 1 || row.defaultAG > ShapeColorPrefs.NUM_AGS) {
                throw new SmartPicFactoryException("AGprefs file " + file + " row " + i
                        + " has defaultAG=" + row.defaultAG + ", expected 1.." + ShapeColorPrefs.NUM_AGS);
            }
            if (row.agPrefs == null || row.agPrefs.length != ShapeColorPrefs.NUM_AGS) {
                throw new SmartPicFactoryException("AGprefs file " + file + " row " + i
                        + " must have exactly " + ShapeColorPrefs.NUM_AGS + " agPrefs weights");
            }
        }
    }

    // ---------- JSON DTOs (file representation only; names here are informational) ----------

    /** Parsed AGprefs defaults document. */
    public static final class AgPrefsDocument {
        /** Positional row binding (v1). */
        public static final int FORMAT_POSITIONAL = 1;
        /** UUID row binding (v2). */
        public static final int FORMAT_UUID = 2;

        /** Label for menus, e.g. "authored-defaults-v1". */
        public String name;
        /** Human-readable provenance/notes. */
        public String description;
        /** File format version; absent in JSON = {@link #FORMAT_POSITIONAL}. */
        public Integer formatVersion;
        /** Ordered shape rows; order is contract in v1, display-only in v2. */
        public List<ShapeRow> shapes;

        /** Format version, defaulting to {@link #FORMAT_POSITIONAL} when absent. */
        public int formatVersion() {
            return formatVersion != null ? formatVersion : FORMAT_POSITIONAL;
        }

        /** Display name for UI menus; falls back to the toString if absent. */
        public String displayName() {
            return name != null ? name : "(unnamed AGprefs)";
        }
    }

    /** One parsed shape row from the defaults file. */
    public static final class ShapeRow {
        /** 1-based shape number; must equal position in v1, display hint in v2. */
        public int shape;
        /** Informational label only — the Template's DrawObj name is authoritative. */
        public String shapeName;
        /** Template DrawObj UUID; required for formatVersion 2, ignored in v1. */
        public String drawObjId;
        /** Authored target value (lightness) for the shape. */
        public double valueTarget;
        /** Authored default anchor group, 1..4. */
        public int defaultAG;
        /** Preference weights for AG1..AG4. */
        public double[] agPrefs;
    }
}
