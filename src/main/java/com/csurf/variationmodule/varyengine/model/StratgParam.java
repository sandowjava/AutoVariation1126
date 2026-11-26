package com.csurf.variationmodule.varyengine.model;


/**
 * The v1 named parameter keys, per DesignPlan §3.1 — positional control/aux is retired.
 * Each key carries a {@link ParamKind}; each {@link StratgType} declares which of these
 * keys it requires (requiredness-only validation: required ⊆ bound).
 */
public enum StratgParam {

    CONTROL_LEVEL("controlLevel", ParamKind.INT),
    FIRST_PREF_LEVEL("firstPrefLevel", ParamKind.INT),
    SECOND_PREF_LEVEL("secondPrefLevel", ParamKind.INT),
    AGHUE_PATTERN("agHuePattern", ParamKind.NAMED_KEY),
    FROZEN_SET("frozenSet", ParamKind.TABLE_KEY),
    FROZEN_SHAPES("frozenShapes", ParamKind.SHAPE_SET),
    VALUE_RANGE("valueRange", ParamKind.DOUBLE);

    private final String externalName;
    private final ParamKind kind;

    StratgParam(String externalName, ParamKind kind) {
        this.externalName = externalName;
        this.kind = kind;
    } // end constructor

    public String getExternalName() {
        return externalName;
    } // end method getExternalName

    public ParamKind getKind() {
        return kind;
    } // end method getKind

} // end enum StratgParam
