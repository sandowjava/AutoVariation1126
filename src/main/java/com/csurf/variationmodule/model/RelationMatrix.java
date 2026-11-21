package com.csurf.variationmodule.model;

import com.csurf.variationmodule.model.SmartPicFactory.AgPrefsDocument;
import com.csurf.variationmodule.model.SmartPicFactory.ShapeRow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The relationMatrix: the totality of the {@link ShapeColorPrefs} instances
 * in a {@link SmartPic} — the graphic intelligence of a SmartPic instance.
 * Each {@code ShapeColorPrefs} is the graphic intelligence of a single
 * {@link SmartDrawObj}; the relationMatrix is the graphic intelligence of
 * the entire SmartPic.
 *
 * <p>"Matrix" names the user's mental model of a structure or mold that
 * shapes color assignment — not any specific implementation. This class may
 * expand in that direction in future development.
 *
 * <p>Implementation: an immutable façade over the SmartPic's ordered
 * {@link SmartDrawObj} set (the prefs remain contained in their
 * SmartDrawObjs — no data is copied). Provides both the row view
 * (per-shape prefs) and the column view (per-AG cohorts) that a matrix
 * editor needs, plus serialization to the AGprefs defaults file format.
 *
 * <p>Serialization convention: {@link #toDocument} always emits
 * {@code formatVersion} 2 (UUID-keyed) rows in Template draw order. Row
 * order is a human-readability convention only; readers bind by
 * {@code drawObjId}.
 *
 * <p>Domain object: no dependency on the variation engine or templatecore.
 *
 * @see SmartPicFactory
 */
public final class RelationMatrix {

    private final SmartPic smartPic;
    private final Map<UUID, SmartDrawObj> byDrawObjUuid; // LinkedHashMap: draw order

    /**
     * @param smartPic the SmartPic whose SmartDrawObjs carry the prefs;
     *                 must not be null
     */
    public RelationMatrix(SmartPic smartPic) {
        Objects.requireNonNull(smartPic, "smartPic must not be null");
        this.smartPic = smartPic;
        Map<UUID, SmartDrawObj> index = new LinkedHashMap<>();
        for (SmartDrawObj sdo : smartPic.smartDrawObjs().values()) {
            if (index.put(sdo.drawObjUuid(), sdo) != null) {
                throw new IllegalArgumentException(
                        "duplicate drawObjUuid " + sdo.drawObjUuid());
            }
        }
        this.byDrawObjUuid = Collections.unmodifiableMap(index);
    }

    /** The SmartPic this matrix belongs to. */
    public SmartPic smartPic() {
        return this.smartPic;
    }

    /** Number of shapes in this matrix. */
    public int numShapes() {
        return this.byDrawObjUuid.size();
    }

    /** All SmartDrawObjs in Template draw order (unmodifiable). */
    public List<SmartDrawObj> shapes() {
        return new ArrayList<>(this.byDrawObjUuid.values());
    }

    /** The SmartDrawObj bound to the given Template DrawObj UUID. */
    public SmartDrawObj shape(UUID drawObjUuid) {
        SmartDrawObj sdo = this.byDrawObjUuid.get(drawObjUuid);
        if (sdo == null) {
            throw new IllegalArgumentException("no shape bound to " + drawObjUuid);
        }
        return sdo;
    }

    /** Whether a row is bound to the given Template DrawObj UUID. */
    public boolean isBound(UUID drawObjUuid) {
        return this.byDrawObjUuid.containsKey(drawObjUuid);
    }

    /** The color prefs for the given SmartDrawObj (must belong to this matrix). */
    public ShapeColorPrefs prefsFor(SmartDrawObj sdo) {
        Objects.requireNonNull(sdo, "sdo must not be null");
        if (this.byDrawObjUuid.get(sdo.drawObjUuid()) != sdo) {
            throw new IllegalArgumentException(
                    "SmartDrawObj " + sdo + " does not belong to this RelationMatrix");
        }
        return sdo.colorPrefs();
    }

    /** The color prefs bound to the given Template DrawObj UUID. */
    public ShapeColorPrefs prefsFor(UUID drawObjUuid) {
        return shape(drawObjUuid).colorPrefs();
    }

    /**
     * Column view: the SmartDrawObjs whose authored default AG is
     * {@code ag} (1..{@link ShapeColorPrefs#NUM_AGS}), in draw order.
     */
    public List<SmartDrawObj> defaultAgMembers(int ag) {
        if (ag < 1 || ag > ShapeColorPrefs.NUM_AGS) {
            throw new IllegalArgumentException(
                    "ag " + ag + " out of range 1.." + ShapeColorPrefs.NUM_AGS);
        }
        List<SmartDrawObj> members = new ArrayList<>();
        for (SmartDrawObj sdo : this.byDrawObjUuid.values()) {
            if (sdo.colorPrefs().defaultAG() == ag) {
                members.add(sdo);
            }
        }
        return members;
    }

    /**
     * Builds the file-format DTO for this matrix: {@code formatVersion} 2,
     * rows in draw order, each keyed by its SmartDrawObj's
     * {@code drawObjUuid}. The {@code shape} field is written as the
     * 1-based draw-order position (display hint only).
     *
     * @param name        display name stored in the document
     * @param description provenance/notes stored in the document
     */
    public AgPrefsDocument toDocument(String name, String description) {
        AgPrefsDocument doc = new AgPrefsDocument();
        doc.name = name;
        doc.description = description;
        doc.formatVersion = AgPrefsDocument.FORMAT_UUID;
        doc.shapes = new ArrayList<>(this.byDrawObjUuid.size());
        int shapeNum = 0;
        for (SmartDrawObj sdo : this.byDrawObjUuid.values()) {
            shapeNum++;
            ShapeColorPrefs prefs = sdo.colorPrefs();
            ShapeRow row = new ShapeRow();
            row.shape = shapeNum;
            row.shapeName = sdo.name();
            row.drawObjId = sdo.drawObjUuid().toString();
            row.valueTarget = prefs.valueTarget();
            row.defaultAG = prefs.defaultAG();
            row.agPrefs = prefs.agPrefs();
            doc.shapes.add(row);
        }
        return doc;
    }

    /**
     * Writes this matrix to {@code file} as a formatVersion 2 AGprefs
     * defaults document.
     *
     * @param file        destination path
     * @param name        display name stored in the document
     * @param description provenance/notes stored in the document
     * @throws SmartPicFactoryException if the file cannot be written
     */
    public void save(Path file, String name, String description) {
        SmartPicFactory.writeAgPrefs(file, toDocument(name, description));
    }

    @Override
    public String toString() {
        return "RelationMatrix{numShapes=" + numShapes() + "}";
    }
}
