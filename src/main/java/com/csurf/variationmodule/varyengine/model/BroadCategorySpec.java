package com.csurf.variationmodule.varyengine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * A serializable BroadCategory descriptor, per DesignPlan §3.3: a
 * {@link BroadCategoryType} plus its {@link PartialGoalSpec}s. This is the
 * code-level realization of the user's "variation goal" — the completed
 * dialog state. User choices live uniformly inside member StratgSpecs as
 * bound params; open params are what the cook fills per recipe-table row.
 */
public class BroadCategorySpec {

    private final BroadCategoryType categoryType;
    private final List<PartialGoalSpec> partialGoalSpecs = new ArrayList<>();

    public BroadCategorySpec(BroadCategoryType categoryType) {
        this.categoryType = Objects.requireNonNull(categoryType, "categoryType");
    } // end constructor

    public BroadCategoryType getCategoryType() {
        return categoryType;
    } // end method getCategoryType

    /** Unmodifiable view; order is authoring order. */
    public List<PartialGoalSpec> getPartialGoalSpecs() {
        return Collections.unmodifiableList(partialGoalSpecs);
    } // end method getPartialGoalSpecs

    /** Adds a PartialGoalSpec in authoring order. Returns this for chaining. */
    public BroadCategorySpec addPartialGoalSpec(PartialGoalSpec partialGoalSpec) {
        partialGoalSpecs.add(Objects.requireNonNull(partialGoalSpec, "partialGoalSpec"));
        return this;
    } // end method addPartialGoalSpec

    public boolean isFullySpecified() {
        if (partialGoalSpecs.isEmpty()) {
            return false;
        } // end IF
        for (PartialGoalSpec goal : partialGoalSpecs) {
            if (!goal.isFullySpecified()) {
                return false;
            } // end IF
        } // end FOR
        return true;
    } // end method isFullySpecified

    /** Deep copy: member goals (and their StratgSpecs) are copied as well. */
    public BroadCategorySpec copyForCooking() {
        BroadCategorySpec copy = new BroadCategorySpec(categoryType);
        for (PartialGoalSpec goal : partialGoalSpecs) {
            copy.partialGoalSpecs.add(goal.copyForCooking());
        } // end FOR
        return copy;
    } // end method copyForCooking

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } // end IF
        if (!(o instanceof BroadCategorySpec)) {
            return false;
        } // end IF
        BroadCategorySpec other = (BroadCategorySpec) o;
        return categoryType == other.categoryType
                && partialGoalSpecs.equals(other.partialGoalSpecs);
    } // end method equals

    @Override
    public int hashCode() {
        return Objects.hash(categoryType, partialGoalSpecs);
    } // end method hashCode

    @Override
    public String toString() {
        return categoryType.getExternalId() + partialGoalSpecs;
    } // end method toString

} // end class BroadCategorySpec
