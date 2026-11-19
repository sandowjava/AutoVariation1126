package com.csurf.variationmodule.varyengine.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;


/**
 * The slice-1 Stratg registry, per DesignPlan §3 and Detailed Plan step 2.
 * Each type carries: {@code id} (the legacy number — the sequencing contract,
 * sorts descending at run time), {@link Phase}, and a light param schema
 * (declared = required ∪ optional; requiredness-only validation).
 *
 * The legacy numbers are the stable external IDs; enum names are the clean
 * class names (OQ6). A registry that can grow (parallel per-phase shells,
 * ID-space widening) remains favored long-term — see Detailed Plan step 2.
 */
public enum StratgType {

    ASSIGN_AGS(921, "921", Phase.AG,
            EnumSet.of(StratgParam.CONTROL_LEVEL),
            EnumSet.noneOf(StratgParam.class)),

    ALTERNATE_AG_POOLS(920, "920", Phase.AG,
            EnumSet.of(StratgParam.FIRST_PREF_LEVEL, StratgParam.SECOND_PREF_LEVEL),
            EnumSet.noneOf(StratgParam.class)),

    ASSIGN_ANCHOR_HUES(555, "555", Phase.HUE,
            EnumSet.of(StratgParam.HUE_SET, StratgParam.CONTROL_LEVEL),
            EnumSet.noneOf(StratgParam.class)),

    ASSIGN_OST_DOTS(255, "255", Phase.COLOR,
            EnumSet.of(StratgParam.CONTROL_LEVEL, StratgParam.VALUE_RANGE),
            EnumSet.noneOf(StratgParam.class)),

    FREEZE_SHAPES(31, "031", Phase.OVERRIDE,
            EnumSet.of(StratgParam.FROZEN_SET),
            EnumSet.noneOf(StratgParam.class));

    private final int id;
    private final String externalId;   // written exactly as legacy: "031", "921", ...
    private final Phase phase;
    private final Set<StratgParam> requiredParams;
    private final Set<StratgParam> optionalParams;

    StratgType(int id, String externalId, Phase phase,
               Set<StratgParam> requiredParams, Set<StratgParam> optionalParams) {
        this.id = id;
        this.externalId = externalId;
        this.phase = phase;
        this.requiredParams = Collections.unmodifiableSet(requiredParams);
        this.optionalParams = Collections.unmodifiableSet(optionalParams);
    } // end constructor

    public int getId() {
        return id;
    } // end method getId

    /** The legacy Stratg number written exactly — the stable external ID. */
    public String getExternalId() {
        return externalId;
    } // end method getExternalId

    public Phase getPhase() {
        return phase;
    } // end method getPhase

    public Set<StratgParam> getRequiredParams() {
        return requiredParams;
    } // end method getRequiredParams

    public Set<StratgParam> getOptionalParams() {
        return optionalParams;
    } // end method getOptionalParams

    /** All declared params: required ∪ optional. Binding a key outside this set is an error. */
    public Set<StratgParam> getDeclaredParams() {
        Set<StratgParam> declared = EnumSet.noneOf(StratgParam.class);
        declared.addAll(requiredParams);
        declared.addAll(optionalParams);
        return Collections.unmodifiableSet(declared);
    } // end method getDeclaredParams

    public boolean declares(StratgParam param) {
        return requiredParams.contains(param) || optionalParams.contains(param);
    } // end method declares

} // end enum StratgType
