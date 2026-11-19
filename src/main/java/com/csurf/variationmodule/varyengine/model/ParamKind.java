package csurf.cleandesign.variationmodule.varyengine.model;


/**
 * The v1 param value kinds, per DesignPlan §8.1 ("value types int/double/table-key only").
 * TABLE_KEY is an int index into a named domain table (e.g., hue-set rows, frozen sets);
 * the kind is descriptive light-schema metadata, not a type-checking framework.
 */
public enum ParamKind {
    INT,
    DOUBLE,
    TABLE_KEY
} // end enum ParamKind
