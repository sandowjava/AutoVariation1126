/**
 * Clean rebuild of the legacy color-variation module (csurf.cleandesign.variationmodule).
 *
 * <p>{@code model} holds the spec stack — {@link csurf.cleandesign.variationmodule.varyengine.model.StratgSpec}
 * → {@link csurf.cleandesign.variationmodule.varyengine.model.PartialGoalSpec} →
 * {@link csurf.cleandesign.variationmodule.varyengine.model.BroadCategorySpec} →
 * {@link csurf.cleandesign.variationmodule.varyengine.model.ArtisticGoalSpec} — plus the
 * {@link csurf.cleandesign.variationmodule.varyengine.model.StratgType} registry.
 * Specs are serializable descriptors only (spec-time objects); run state and
 * executables arrive in later steps per the Detailed Design Plan
 * ({@code 110826_v1B_VARIATIONMODULE_Detailed_DesignPlan.md}).
 */
package csurf.cleandesign.variationmodule.varyengine.model;
