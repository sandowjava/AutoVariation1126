# COMMIT_MESSAGE_version 0D (111926): Port varyengine + SmartPicFacts adapter + CreateSmartPicFrame updates + remaining-work doc

## Summary

Ports the legacy `csurf.cleandesign.variationmodule.varyengine` package into
`com.csurf.variationmodule.varyengine` (imports rewritten to this project's
`com.csurf.ostwald` / `com.csurf.templatecore` equivalents) and adds the
`Facts` adapter that feeds a user-built `SmartPic` into the engine. All
ported code compiles. Also adds the palette-extraction seam and a design doc
capturing the remaining-work analysis and plan.

## New classes

### Engine model — `com.csurf.variationmodule.varyengine.model` (37 files)

- **`Facts`** — read-only facts contract a Stratg consults (numShapes,
  shapeIds, defaultAG, agPref, valueTarget, shapeName); authored defaultAG is
  authoritative, never argmax'd.
- **`Proposal`** — immutable partial map (channel, shape) → value; absent =
  not-written (Optional-returning accessors), resolving legacy stale reads.
- **Run-state stack** — `RunState`, `ShapeRunState`, `VariationRunState`,
  `RecordingRunState`, `Seed`, `Phase`, `Channel`, `Coverage`, `Undecided`.
- **Spec stack** — `StratgSpec`/`StratgParam`/`ParamKind` →
  `PartialGoalSpec`/`PartialGoalType` → `BroadCategorySpec`/
  `BroadCategoryType` → `ArtisticGoalSpec`; `StratgType` registry,
  `Stratg`/`StratgContext`/`BasicStratgContext`, `NestedRunner`, `PureCore`,
  `Inputs`, `FrozenSets`, `AnchorHueSet(s)`, `VariationConstants`.
- **Orchestration** — `VariationDriver` (runs cooked specs →
  `List<VariationResult>`), `VariationResult` (effectiveColors keyed by
  shape ID), `FrozenShapesCategory.cook(hueSet, frozenSet)` (produces the 12
  ArtisticGoalSpecs), `ColorChangeEngine`, `ColorChangeDisplay`.
- **`SmartPicFacts`** — `Facts` adapter over `SmartPic` (v1E D2 adapter
  pattern: SmartPic stays engine-independent). Loud missing-shape errors;
  positional 1-based shape IDs keyed to `smartShapeNum`.
- **`AuthoredFactsTable`** — ported 21-row transcription, retained as a
  fixture/parity reference (production path uses `SmartPicFacts`).

### Stratgs — `...model.stratgs` (8 files)

- **`AssignAGsStratg`** (921), **`AlternateAGPoolsStratg`** (920 — two
  nested 921 runs applied to odd/even shapes via parity-scoped
  `apply` calls), **`AssignAnchorHuesStratg`**, **`AssignOstDotsStratg`**
  (uses `MonotriBook.getInstance().getOstDotsByValueRange`),
  **`FreezeShapesStratg`**, plus `DefaultNestedRunner`, `StratgResolver`,
  `StratgSupport`.

### Cycle runner + DTO — `com.csurf.variationmodule.varyengine.controller`

- **`VariationPaletteExtractor`** — `runCleanCycle(hueSet, frozenSet, facts)`
  (static, stateless: `FrozenShapesCategory.cook` → `VariationDriver.run`,
  returns `List<VariationResult>` in recipe order);
  `extract12VariationPalettes(results)` projects results into the DTO with
  dense-keyspace/null validation; per-window instance holds last results.
- **`PalettesThe12Variations`** — exactly-12 non-null DTO of `PaletteWName`
  (0-based; index 0 = recipe row 1 = display slot 1); names `varyRecipe1..12`;
  dense `ColorPtOstDot[]` where index i ↔ shape i+1 ↔ DrawObj position i.

## Modified classes

- **`CreateSmartPicFrame`** — single SmartPic menu split into three menus
  (Template / Shape-AGprefs / SmartPic), one item each; added
  `OPEN_LOCATION` constant so the window opens upper-left below
  ApplicFrame. Menu-item flows unchanged: choose Template (JFileChooser on
  dev_storage/templates → TemplateStorage.load), choose AGprefs file
  (JFileChooser on dev_storage/agprefs → SmartPicFactory.loadAgPrefs
  validation), Create SmartPic (factory.createSmartPic → status line).
- **`CreateSmartPicController`** — launch() now opens the frame at
  `CreateSmartPicFrame.OPEN_LOCATION`; bundled `authored-defaults-v1.json`
  re-syncs from the classpath resource on every chooser open (fixes stale
  extracted copy).

## New document

- **`111926_v0D_SWE-2_port_Palette-Variations-V1_remaining_work.md`** —
  remaining-work analysis + plan: ~70% complete; what remains is run glue
  (SmartPicFacts → runCleanCycle → extract), a positional palette→Template
  application loop, hueSet/frozenSet parameter UI, and a wiring decision
  (recommended: "Create 12 Variations" item in CreateSmartPicFrame's
  SmartPic menu). Design notes cover the shared positional contract,
  per-window extractor scope, AuthoredFactsTable as fixture-only, CleanColor
  parity verification, and an ordered 7-step plan. Difficulty: low-moderate.

## Execution flow

```
CreateSmartPicFrame menus → CreateSmartPicController
    Template    → choose saved Template → TemplateStorage → Template
    Shape-AGprefs → choose defaults file → SmartPicFactory.loadAgPrefs
    SmartPic    → createSmartPic → SmartPicFactory → SmartPic (in memory)
NEXT (planned): SmartPic → SmartPicFacts → runCleanCycle
    → extract12VariationPalettes → PalettesThe12Variations
    → positional apply to 12 Template copies → VariationDisplayFrame
```
