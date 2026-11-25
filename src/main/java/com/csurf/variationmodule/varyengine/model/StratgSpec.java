package com.csurf.variationmodule.varyengine.model;

import java.util.*;


/**
 * A serializable Stratg descriptor, per DesignPlan §3.1: a {@link StratgType}
 * plus a named-parameter map in which each declared param is either bound or open.
 *
 * Validation is requiredness-only (light schema): {@link #isFullySpecified()} is
 * {@code required ⊆ bound keys}. Binding an undeclared param, or a value whose
 * Java type mismatches the param's {@link ParamKind}, fails fast.
 *
 * v1 param values: Numbers (Integer for INT/TABLE_KEY, Double for DOUBLE)
 * and Strings (NAMED_KEY — a canonical name into a named catalog).
 */
public class StratgSpec {

    private final StratgType stratgType;
    private final Map<StratgParam, Object> boundParams = new EnumMap<>(StratgParam.class);

    public StratgSpec(StratgType stratgType) {
        this.stratgType = Objects.requireNonNull(stratgType, "stratgType");
    } // end constructor

    public StratgType getStratgType() {
        return stratgType;
    } // end method getStratgType

    /** Unmodifiable view of the bound parameters (open params are absent). */
    public Map<StratgParam, Object> getBoundParams() {
        return Collections.unmodifiableMap(boundParams);
    } // end method getBoundParams

    public Set<StratgParam> getBoundParamKeys() {
        return Collections.unmodifiableSet(boundParams.keySet());
    } // end method getBoundParamKeys

    public boolean isBound(StratgParam param) {
        return boundParams.containsKey(param);
    } // end method isBound

    /** Cook output contract: every required param is bound. */
    public boolean isFullySpecified() {
        return boundParams.keySet().containsAll(stratgType.getRequiredParams());
    } // end method isFullySpecified

    public Object getParam(StratgParam param) {
        return boundParams.get(param);
    } // end method getParam

    public int getIntParam(StratgParam param) {
        return ((Number) requireBound(param)).intValue();
    } // end method getIntParam

    public double getDoubleParam(StratgParam param) {
        return ((Number) requireBound(param)).doubleValue();
    } // end method getDoubleParam

    public String getStringParam(StratgParam param) {
        return (String) requireBound(param);
    } // end method getStringParam

    /**
     * Binds an int param (INT or TABLE_KEY kind). Returns this for chaining.
     * The cook binds open params; binding over a bound key replaces the value.
     */
    public StratgSpec bind(StratgParam param, int value) {
        checkDeclared(param);
        if (param.getKind() == ParamKind.DOUBLE) {
            throw new IllegalArgumentException(
                    param.getExternalName() + " is a DOUBLE param on " + stratgType.getExternalId());
        } // end IF
        boundParams.put(param, value);
        return this;
    } // end method bind

    /** Binds a double param (DOUBLE kind). Returns this for chaining. */
    public StratgSpec bind(StratgParam param, double value) {
        checkDeclared(param);
        if (param.getKind() != ParamKind.DOUBLE) {
            throw new IllegalArgumentException(
                    param.getExternalName() + " is a " + param.getKind()
                            + " param on " + stratgType.getExternalId());
        } // end IF
        boundParams.put(param, value);
        return this;
    } // end method bind

    /** Binds a String param (NAMED_KEY kind). Returns this for chaining. */
    public StratgSpec bind(StratgParam param, String value) {
        checkDeclared(param);
        if (param.getKind() != ParamKind.NAMED_KEY) {
            throw new IllegalArgumentException(
                    param.getExternalName() + " is a " + param.getKind()
                            + " param on " + stratgType.getExternalId());
        } // end IF
        boundParams.put(param, Objects.requireNonNull(value, "value"));
        return this;
    } // end method bind

    /**
     * Deep-enough copy for cooking: v1 param values are immutable Numbers,
     * so a new map over the same values is a safe copy.
     */
    public StratgSpec copyForCooking() {
        StratgSpec copy = new StratgSpec(stratgType);
        copy.boundParams.putAll(this.boundParams);
        return copy;
    } // end method copyForCooking

    private void checkDeclared(StratgParam param) {
        if (!stratgType.declares(param)) {
            throw new IllegalArgumentException(
                    param.getExternalName() + " is not a declared param of "
                            + stratgType.getExternalId() + " (" + stratgType + ")");
        } // end IF
    } // end method checkDeclared

    private Object requireBound(StratgParam param) {
        Object value = boundParams.get(param);
        if (value == null) {
            throw new IllegalStateException(
                    param.getExternalName() + " is open on " + stratgType.getExternalId()
                            + " spec — read only after isFullySpecified()");
        } // end IF
        return value;
    } // end method requireBound

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } // end IF
        if (!(o instanceof StratgSpec)) {
            return false;
        } // end IF
        StratgSpec other = (StratgSpec) o;
        return stratgType == other.stratgType
                && boundParams.equals(other.boundParams);
    } // end method equals

    @Override
    public int hashCode() {
        return Objects.hash(stratgType, boundParams);
    } // end method hashCode

    @Override
    public String toString() {
        return stratgType.getExternalId() + boundParams;
    } // end method toString

} // end class StratgSpec
