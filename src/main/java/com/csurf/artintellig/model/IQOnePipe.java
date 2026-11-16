package com.csurf.artintellig.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "intelligence" or metadata about a single drawable object.
 * This includes its relationships with other objects, color preferences,
 * and any artistic constraints that should influence its final appearance.
 */
public class IQOnePipe {
    private final String drawObjId;
    private final List<String> relatedObjIds;
    private ColorPtOstDot currColor;
    private double colorWeight; // TODO 052425 field "colorWeight" has no definite purpose yet. It's only Cascade's idea
    private double offsetInAG; // for calc of specific hue by AnchorGroup // range -1.0 <= offsetInAG <= 1.0

    /**
     * Creates a new IQOnePipe instance.
     *
     * @param drawObjId ID of the DrawObj this represents
     */
    public IQOnePipe(String drawObjId) {
        this.drawObjId = drawObjId;
        this.relatedObjIds = new ArrayList<>();
        this.colorWeight = 1.0;
    }

    /**
     * Gets the ID of the DrawObj this represents.
     */
    public String getId() {
        return drawObjId;
    }

    /**
     * Gets the list of related object IDs.
     */
    public List<String> getRelatedObjIds() {
        return new ArrayList<>(relatedObjIds);
    }

    /**
     * Adds a related object ID.
     */
    public void addRelatedObjId(String id) {
        if (!relatedObjIds.contains(id)) {
            relatedObjIds.add(id);
        }
    }

    /**
     * Gets the preferred color for this object.
     */
    public ColorPtOstDot getCurrColor() {
        return currColor;
    }

    /**
     * Sets the preferred color for this object.
     */
    public void setCurrColor(ColorPtOstDot color) {
        this.currColor = color;
    }

    /**
     * Gets the weight factor for color influence.
     */
    public double getColorWeight() {
        return colorWeight;
    }

    /**
     * Sets the weight factor for color influence.
     */
    public void setColorWeight(double weight) {
        this.colorWeight = weight;
    }
}
