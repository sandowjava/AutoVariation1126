package com.csurf.variationmodule.varyengine.model;


/**
 * The v1 param value kinds (DesignPlan §8.1, revised for AGHuePattern):
 * int/double/table-key plus named-key. TABLE_KEY is an int index into a
 * fixed domain table (e.g., frozen sets); NAMED_KEY is a String canonical
 * name into a named catalog (e.g., {@link AGHuePatterns}) — self-describing
 * and stable across sessions. The kind is descriptive light-schema
 * metadata, not a type-checking framework.
 */
public enum ParamKind {
    INT,
    DOUBLE,
    TABLE_KEY,
    NAMED_KEY
} // end enum ParamKind
