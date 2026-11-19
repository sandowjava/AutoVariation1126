package csurf.cleandesign.variationmodule.varyengine.model;


/**
 * The BroadCategory identifier carried by {@link BroadCategorySpec} —
 * the stable external ID for serialization (DesignPlan §3.3).
 * Slice 1 has exactly one category: FrozenShapes, reproducing the legacy
 * 12-variation output. Additional categories are deferred (parent §8).
 */
public enum BroadCategoryType {

    FROZEN_SHAPES("FrozenShapes");

    private final String externalId;

    BroadCategoryType(String externalId) {
        this.externalId = externalId;
    } // end constructor

    /** The category name — the stable external ID. */
    public String getExternalId() {
        return externalId;
    } // end method getExternalId

} // end enum BroadCategoryType
