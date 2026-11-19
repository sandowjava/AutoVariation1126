/**
 * Clean rebuild of the legacy color-variation module (csurf.cleandesign.variationmodule).
 *
 * <p>{@code model} holds the spec stack — {@link com.csurf.variationmodule.varyengine.model.StratgSpec}
 * → {@link com.csurf.variationmodule.varyengine.model.PartialGoalSpec} →
 * {@link com.csurf.variationmodule.varyengine.model.BroadCategorySpec} →
 * {@link com.csurf.variationmodule.varyengine.model.ArtisticGoalSpec} — plus the
 * {@link com.csurf.variationmodule.varyengine.model.StratgType} registry.
 * Specs are serializable descriptors only (spec-time objects); run state and
 * executables arrive in later steps per the Detailed Design Plan
 * ({@code 110826_v1B_VARIATIONMODULE_Detailed_DesignPlan.md}).
 */
package com.csurf.variationmodule.varyengine.model;
