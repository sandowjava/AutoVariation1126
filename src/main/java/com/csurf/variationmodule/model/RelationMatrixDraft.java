package com.csurf.variationmodule.model;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.Template;
import com.csurf.variationmodule.model.SmartPicFactory.AgPrefsDocument;
import com.csurf.variationmodule.model.SmartPicFactory.ShapeRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Mutable working copy of a {@link RelationMatrix} for the editor:
 * a list of prefs rows plus an explicit binding map (row → Template
 * DrawObj UUID). Unlike {@link SmartPicFactory#createSmartPic}, loading a
 * draft never requires a matching Template — rows that cannot be resolved
 * stay <i>unbound</i> so the user can re-bind them interactively
 * (foreign-template workflow).
 *
 * <p>Binding semantics: a binding is exclusive — binding a row to a
 * DrawObj already claimed by another row unbinds that other row (move
 * semantics, never a duplicate binding).
 *
 * <p>Display order is Template draw order, unbound rows last — the
 * internal {@link #rows()} list stays in file order so bulk positional
 * binding has a stable reference. Serialization always emits
 * {@code formatVersion} 2 rows in draw order.
 *
 * <p>Design doc: {@code 112126_v0G_SWE-2_RelationMatrix_and_Editor-UI_Design.md}.
 */
public final class RelationMatrixDraft {

    /** One editable prefs row plus its (possibly absent) DrawObj binding. */
    public static final class DraftRow {
        private final int fileShape;        // original 1-based row number / display hint
        private final String fileShapeName; // informational label from the file
        private double valueTarget;
        private int defaultAG;
        private final double[] agPrefs;
        private final String drawObjIdText; // raw drawObjId text from file, may be null/malformed
        private UUID boundDrawObjUuid;      // null = unbound

        private DraftRow(ShapeRow row, int position) {
            this.fileShape = position;
            this.fileShapeName = row.shapeName;
            this.valueTarget = row.valueTarget;
            this.defaultAG = row.defaultAG;
            this.agPrefs = row.agPrefs.clone();
            this.drawObjIdText = row.drawObjId;
        }

        /** The row's original 1-based number from the file (informational). */
        public int fileShape() { return fileShape; }

        /** The informational shape label carried by the file, or null. */
        public String fileShapeName() { return fileShapeName; }

        /** The raw drawObjId text carried by the file, or null. */
        public String drawObjIdText() { return drawObjIdText; }

        public double valueTarget() { return valueTarget; }
        public void setValueTarget(double valueTarget) { this.valueTarget = valueTarget; }

        public int defaultAG() { return defaultAG; }

        /** @throws IllegalArgumentException if ag is out of 1..NUM_AGS */
        public void setDefaultAG(int ag) {
            if (ag < 1 || ag > ShapeColorPrefs.NUM_AGS) {
                throw new IllegalArgumentException("ag " + ag + " out of range 1.."
                        + ShapeColorPrefs.NUM_AGS);
            }
            this.defaultAG = ag;
        }

        /** Preference weight for AG {@code ag} (1..4). */
        public double agPref(int ag) { return agPrefs[ag - 1]; }

        /** @throws IllegalArgumentException if ag is out of 1..NUM_AGS */
        public void setAgPref(int ag, double weight) {
            if (ag < 1 || ag > ShapeColorPrefs.NUM_AGS) {
                throw new IllegalArgumentException("ag " + ag + " out of range 1.."
                        + ShapeColorPrefs.NUM_AGS);
            }
            agPrefs[ag - 1] = weight;
        }

        /** The bound Template DrawObj UUID, or null if unbound. */
        public UUID boundDrawObjUuid() { return boundDrawObjUuid; }

        /** Whether this row is bound to a DrawObj. */
        public boolean isBound() { return boundDrawObjUuid != null; }
    }

    private final int formatVersion;
    private String name;
    private String description;
    private final List<DraftRow> rows;          // file order — stable reference
    private List<DrawObj> templateDrawObjs;     // draw order; null = no template yet
    private Map<UUID, Integer> drawOrderIndex;  // uuid -> 0-based draw position

    private RelationMatrixDraft(AgPrefsDocument doc) {
        this.formatVersion = doc.formatVersion();
        this.name = doc.name;
        this.description = doc.description;
        this.rows = new ArrayList<>(doc.shapes.size());
        for (int i = 0; i < doc.shapes.size(); i++) {
            this.rows.add(new DraftRow(doc.shapes.get(i), i + 1));
        }
    }

    /**
     * Builds a draft from a parsed (already validated) defaults document.
     * All rows start unbound; call {@link #bindToTemplate} next.
     */
    public static RelationMatrixDraft fromDocument(AgPrefsDocument doc) {
        Objects.requireNonNull(doc, "doc must not be null");
        if (doc.shapes == null) {
            throw new IllegalArgumentException("doc has no shapes rows");
        }
        return new RelationMatrixDraft(doc);
    }

    /**
     * Binds the draft to a Template. First pass: every row whose
     * {@code drawObjId} parses and resolves to a Template DrawObj is bound
     * (v2 files). Second pass: remaining rows in a v1 file are bound
     * positionally (row i → drawObj i) where that DrawObj is still
     * uncovered. Everything else stays unbound.
     */
    public void bindToTemplate(Template template) {
        Objects.requireNonNull(template, "template must not be null");
        this.templateDrawObjs = template.getAllDrawObjs();
        this.drawOrderIndex = new LinkedHashMap<>();
        Map<UUID, DrawObj> drawObjsByUuid = new LinkedHashMap<>();
        for (int i = 0; i < templateDrawObjs.size(); i++) {
            DrawObj drawObj = templateDrawObjs.get(i);
            drawObjsByUuid.put(drawObj.getUniqueId(), drawObj);
            drawOrderIndex.put(drawObj.getUniqueId(), i);
        }

        for (DraftRow row : rows) {
            row.boundDrawObjUuid = null;
        }
        // pass 1: UUID resolution
        for (DraftRow row : rows) {
            if (row.drawObjIdText == null) {
                continue;
            }
            UUID id;
            try {
                id = UUID.fromString(row.drawObjIdText);
            } catch (IllegalArgumentException e) {
                continue; // malformed id — stays for positional/never bind
            }
            if (drawObjsByUuid.containsKey(id) && !isUuidClaimed(id)) {
                row.boundDrawObjUuid = id;
            }
        }
        // pass 2: positional fallback for v1 files
        if (formatVersion == AgPrefsDocument.FORMAT_POSITIONAL) {
            for (int i = 0; i < rows.size() && i < templateDrawObjs.size(); i++) {
                DraftRow row = rows.get(i);
                UUID candidate = templateDrawObjs.get(i).getUniqueId();
                if (!row.isBound() && !isUuidClaimed(candidate)) {
                    row.boundDrawObjUuid = candidate;
                }
            }
        }
    }

    private boolean isUuidClaimed(UUID uuid) {
        for (DraftRow row : rows) {
            if (uuid.equals(row.boundDrawObjUuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Binds {@code row} to the DrawObj with the given UUID. If another row
     * already holds that UUID it becomes unbound (move semantics).
     */
    public void bindRowTo(DraftRow row, UUID drawObjUuid) {
        Objects.requireNonNull(row, "row must not be null");
        if (drawObjUuid == null) {
            row.boundDrawObjUuid = null;
            return;
        }
        if (templateDrawObjs == null || !drawOrderIndex.containsKey(drawObjUuid)) {
            throw new IllegalArgumentException("no Template DrawObj with id " + drawObjUuid);
        }
        for (DraftRow other : rows) {
            if (other != row && drawObjUuid.equals(other.boundDrawObjUuid)) {
                other.boundDrawObjUuid = null;
            }
        }
        row.boundDrawObjUuid = drawObjUuid;
    }

    /** Unbinds {@code row}. */
    public void unbindRow(DraftRow row) {
        Objects.requireNonNull(row, "row must not be null");
        row.boundDrawObjUuid = null;
    }

    /**
     * Bulk action: unbinds everything, then binds row i (file order) to
     * drawObj i (draw order) for i &lt; min(rows, drawObjs) — the v1
     * positional contract as a starting guess for re-binding.
     */
    public void bindByPosition() {
        if (templateDrawObjs == null) {
            throw new IllegalStateException("no Template chosen");
        }
        for (DraftRow row : rows) {
            row.boundDrawObjUuid = null;
        }
        for (int i = 0; i < rows.size() && i < templateDrawObjs.size(); i++) {
            rows.get(i).boundDrawObjUuid = templateDrawObjs.get(i).getUniqueId();
        }
    }

    /** All rows in file order (stable reference for positional binding). */
    public List<DraftRow> rows() {
        return Collections.unmodifiableList(rows);
    }

    /**
     * Rows in display order: bound rows sorted by their DrawObj's Template
     * draw position, unbound rows last (in file order).
     */
    public List<DraftRow> displayRows() {
        List<DraftRow> bound = new ArrayList<>();
        List<DraftRow> unbound = new ArrayList<>();
        for (DraftRow row : rows) {
            (row.isBound() ? bound : unbound).add(row);
        }
        bound.sort(Comparator.comparingInt(r -> drawOrderIndex.get(r.boundDrawObjUuid)));
        bound.addAll(unbound);
        return bound;
    }

    /** The bound Template DrawObj for a row, or null. */
    public DrawObj boundDrawObj(DraftRow row) {
        if (templateDrawObjs == null || row == null || !row.isBound()) {
            return null;
        }
        Integer idx = drawOrderIndex.get(row.boundDrawObjUuid);
        return idx != null ? templateDrawObjs.get(idx) : null;
    }

    /** The template's DrawObjs in draw order, or empty list. */
    public List<DrawObj> templateDrawObjs() {
        return templateDrawObjs != null
                ? Collections.unmodifiableList(templateDrawObjs)
                : Collections.emptyList();
    }

    /** Number of rows with no DrawObj binding. */
    public int unboundCount() {
        int n = 0;
        for (DraftRow row : rows) {
            if (!row.isBound()) {
                n++;
            }
        }
        return n;
    }

    /** Number of Template DrawObjs with no row bound to them. */
    public int uncoveredCount() {
        if (templateDrawObjs == null) {
            return 0;
        }
        int n = 0;
        for (DrawObj drawObj : templateDrawObjs) {
            if (!isUuidClaimed(drawObj.getUniqueId())) {
                n++;
            }
        }
        return n;
    }

    /** True when every row is bound and every Template DrawObj is covered. */
    public boolean isFullyBound() {
        return templateDrawObjs != null && unboundCount() == 0 && uncoveredCount() == 0;
    }

    /** Document display name. */
    public String name() { return name; }
    public void setName(String name) { this.name = name; }

    /** Document description/provenance. */
    public String description() { return description; }
    public void setDescription(String description) { this.description = description; }

    /** The source file's format version (1 or 2). */
    public int formatVersion() { return formatVersion; }

    /**
     * Builds the file-format DTO: {@code formatVersion} 2, rows in draw
     * order keyed by bound DrawObj UUID.
     *
     * @throws IllegalStateException if the draft is not fully bound
     */
    public AgPrefsDocument toDocument() {
        if (!isFullyBound()) {
            throw new IllegalStateException("cannot save: " + unboundCount()
                    + " row(s) unbound, " + uncoveredCount()
                    + " DrawObj(s) uncovered");
        }
        AgPrefsDocument doc = new AgPrefsDocument();
        doc.name = name;
        doc.description = description;
        doc.formatVersion = AgPrefsDocument.FORMAT_UUID;
        doc.shapes = new ArrayList<>(rows.size());
        int shapeNum = 0;
        for (DraftRow row : displayRows()) { // all bound — draw order
            shapeNum++;
            DrawObj bound = boundDrawObj(row);
            ShapeRow out = new ShapeRow();
            out.shape = shapeNum;
            out.shapeName = bound != null ? bound.getName() : row.fileShapeName;
            out.drawObjId = row.boundDrawObjUuid.toString();
            out.valueTarget = row.valueTarget;
            out.defaultAG = row.defaultAG;
            out.agPrefs = new double[]{row.agPrefs[0], row.agPrefs[1],
                    row.agPrefs[2], row.agPrefs[3]};
            doc.shapes.add(out);
        }
        return doc;
    }
}
