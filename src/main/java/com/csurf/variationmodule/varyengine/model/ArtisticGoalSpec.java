package com.csurf.variationmodule.varyengine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * A serializable ArtisticGoal descriptor, per DesignPlan §3.4: an ordered
 * list of fully-specified {@link PartialGoalSpec}s — sufficient and necessary
 * to produce one variation.
 *
 * List order is authoring/provenance order; the engine flattens all member
 * StratgSpecs and sorts by Stratg ID descending for execution (parent §4.4).
 *
 * {@code recipeRow} is cook provenance (which recipe-table row produced this
 * spec; 0 = not cook-produced). It is deliberately excluded from equality —
 * two specs describing the same goals are equal regardless of provenance.
 */
public class ArtisticGoalSpec {

    private final List<PartialGoalSpec> partialGoalSpecs;
    private final int recipeRow;

    public ArtisticGoalSpec(List<PartialGoalSpec> partialGoalSpecs) {
        this(partialGoalSpecs, 0);
    } // end constructor

    public ArtisticGoalSpec(List<PartialGoalSpec> partialGoalSpecs, int recipeRow) {
        Objects.requireNonNull(partialGoalSpecs, "partialGoalSpecs");
        if (partialGoalSpecs.isEmpty()) {
            throw new IllegalArgumentException("an ArtisticGoalSpec needs at least one PartialGoalSpec");
        } // end IF
        this.partialGoalSpecs = Collections.unmodifiableList(new ArrayList<>(partialGoalSpecs));
        this.recipeRow = recipeRow;
    } // end constructor

    /** Unmodifiable view; order is authoring order (provenance), not execution order. */
    public List<PartialGoalSpec> getPartialGoalSpecs() {
        return partialGoalSpecs;
    } // end method getPartialGoalSpecs

    public int getRecipeRow() {
        return recipeRow;
    } // end method getRecipeRow

    /** All member StratgSpecs, flattened in authoring order. The engine sorts by ID. */
    public List<StratgSpec> flattenStratgSpecs() {
        List<StratgSpec> flat = new ArrayList<>();
        for (PartialGoalSpec goal : partialGoalSpecs) {
            flat.addAll(goal.getStratgSpecs());
        } // end FOR
        return flat;
    } // end method flattenStratgSpecs

    public boolean isFullySpecified() {
        for (PartialGoalSpec goal : partialGoalSpecs) {
            if (!goal.isFullySpecified()) {
                return false;
            } // end IF
        } // end FOR
        return true;
    } // end method isFullySpecified

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } // end IF
        if (!(o instanceof ArtisticGoalSpec)) {
            return false;
        } // end IF
        ArtisticGoalSpec other = (ArtisticGoalSpec) o;
        return partialGoalSpecs.equals(other.partialGoalSpecs);
    } // end method equals

    @Override
    public int hashCode() {
        return Objects.hash(partialGoalSpecs);
    } // end method hashCode

    @Override
    public String toString() {
        return "ArtisticGoalSpec{row " + recipeRow + ": " + partialGoalSpecs + "}";
    } // end method toString

} // end class ArtisticGoalSpec
