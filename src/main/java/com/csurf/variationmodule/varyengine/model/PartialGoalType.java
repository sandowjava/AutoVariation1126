package csurf.cleandesign.variationmodule.varyengine.model;


/**
 * The slice-1 PartialGoal types, named after the legacy PartialGoal classes
 * for traceability. Each type carries its {@link Phase}, which confines which
 * Stratg types may be placed in goals of this type (parent DesignPlan §3.2/§4.4
 * "scheduling entirety" — enforced at add time in {@link PartialGoalSpec} and
 * backstopped by the engine in step 7).
 *
 * Mapping from the legacy class names: PG2 = AG phase, PG3 = HUE, PG4 = COLOR,
 * PG1A = OVERRIDE (freeze).
 */
public enum PartialGoalType {

    PG2("PG2", Phase.AG),
    PG3("PG3", Phase.HUE),
    PG4("PG4", Phase.COLOR),
    PG1A("PG1A", Phase.OVERRIDE);

    private final String externalId;
    private final Phase phase;

    PartialGoalType(String externalId, Phase phase) {
        this.externalId = externalId;
        this.phase = phase;
    } // end constructor

    /** The legacy PartialGoal name — the stable external ID. */
    public String getExternalId() {
        return externalId;
    } // end method getExternalId

    public Phase getPhase() {
        return phase;
    } // end method getPhase

} // end enum PartialGoalType
