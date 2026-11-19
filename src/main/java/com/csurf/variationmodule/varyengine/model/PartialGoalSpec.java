package csurf.cleandesign.variationmodule.varyengine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * A serializable PartialGoal descriptor, per DesignPlan §3.2: a
 * {@link PartialGoalType} (carrying its phase) plus 1..n {@link StratgSpec}s.
 * List order is authoring order — execution order is by Stratg ID, so this
 * order is provenance, not scheduling.
 *
 * Stratg placement is phase-confined: a StratgSpec may be added only if its
 * type's phase equals this goal's phase (the "scheduling entirety" guarantee
 * — parent §4.4 — enforced at spec level here and backstopped by the engine).
 */
public class PartialGoalSpec {

    private final PartialGoalType partialGoalType;
    private final boolean required;          // requirement metadata (light; a fuller form is deferred)
    private final List<StratgSpec> stratgSpecs = new ArrayList<>();

    public PartialGoalSpec(PartialGoalType partialGoalType) {
        this(partialGoalType, true);
    } // end constructor

    public PartialGoalSpec(PartialGoalType partialGoalType, boolean required) {
        this.partialGoalType = Objects.requireNonNull(partialGoalType, "partialGoalType");
        this.required = required;
    } // end constructor

    public PartialGoalType getPartialGoalType() {
        return partialGoalType;
    } // end method getPartialGoalType

    public Phase getPhase() {
        return partialGoalType.getPhase();
    } // end method getPhase

    public boolean isRequired() {
        return required;
    } // end method isRequired

    /** Unmodifiable view; order is authoring order. */
    public List<StratgSpec> getStratgSpecs() {
        return Collections.unmodifiableList(stratgSpecs);
    } // end method getStratgSpecs

    /**
     * Adds a StratgSpec in authoring order. Phase-confined: the spec's
     * StratgType phase must equal this goal's phase. Returns this for chaining.
     */
    public PartialGoalSpec addStratgSpec(StratgSpec stratgSpec) {
        Objects.requireNonNull(stratgSpec, "stratgSpec");
        Phase stratgPhase = stratgSpec.getStratgType().getPhase();
        if (stratgPhase != getPhase()) {
            throw new IllegalArgumentException(
                    stratgSpec.getStratgType().getExternalId() + " is a " + stratgPhase
                            + " Stratg; goal " + partialGoalType.getExternalId()
                            + " is " + getPhase() + " phase (scheduling entirety)");
        } // end IF
        stratgSpecs.add(stratgSpec);
        return this;
    } // end method addStratgSpec

    /** Fully specified = non-empty and every member spec fully specified. */
    public boolean isFullySpecified() {
        if (stratgSpecs.isEmpty()) {
            return false;
        } // end IF
        for (StratgSpec spec : stratgSpecs) {
            if (!spec.isFullySpecified()) {
                return false;
            } // end IF
        } // end FOR
        return true;
    } // end method isFullySpecified

    /** Deep copy for cooking: member StratgSpecs are copied as well. */
    public PartialGoalSpec copyForCooking() {
        PartialGoalSpec copy = new PartialGoalSpec(partialGoalType, required);
        for (StratgSpec spec : stratgSpecs) {
            copy.stratgSpecs.add(spec.copyForCooking());
        } // end FOR
        return copy;
    } // end method copyForCooking

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } // end IF
        if (!(o instanceof PartialGoalSpec)) {
            return false;
        } // end IF
        PartialGoalSpec other = (PartialGoalSpec) o;
        return partialGoalType == other.partialGoalType
                && required == other.required
                && stratgSpecs.equals(other.stratgSpecs);
    } // end method equals

    @Override
    public int hashCode() {
        return Objects.hash(partialGoalType, required, stratgSpecs);
    } // end method hashCode

    @Override
    public String toString() {
        return partialGoalType.getExternalId() + stratgSpecs;
    } // end method toString

} // end class PartialGoalSpec
