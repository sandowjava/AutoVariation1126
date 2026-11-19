# Port Palette-Variations V1 — Remaining Work (analysis + plan)

Date: 111926 · Doc rev: v0D · Author: SWE-2 (Cascade)

Status snapshot: ~70% of the end-to-end path is in place. All ported engine
code compiles in AutoVariation1126. The remaining work is orchestration glue,
a positional palette-application step, and a small run-parameter UI — no
algorithmic porting remains.

---

## 1. Context

This document captures the state assessment and design considerations for
completing the palette-variation functionality in AutoVariation1126 — the
port of the legacy `csurf.cleandesign.variationmodule` engine (design docs
`111426_v1D` / `111426_v1E`, referenced in the ported sources' comments).

The intended end-to-end flow:

```
saved Template + AGprefs defaults file
    → SmartPicFactory
    → SmartPic (domain object, engine-independent)
    → SmartPicFacts (Facts adapter at the engine boundary)
    → VariationPaletteExtractor.runCleanCycle(hueSet, frozenSet, facts)
        → FrozenShapesCategory.cook(...) → VariationDriver.run(...)
    → extract12VariationPalettes(results)
    → PalettesThe12Variations (12 × PaletteWName, dense ColorPtOstDot[])
    → apply palettes positionally to 12 Template copies
    → VariationDisplayFrame renders 12 varied thumbnails
```

## 2. What is complete

### 2.1 Engine core — done
45 files under `com.csurf.variationmodule.varyengine.model` and
`...model.stratgs` are in the project and compile: `Facts`, `Proposal`,
`RunState`/`ShapeRunState`/`VariationRunState`/`RecordingRunState`,
`VariationDriver`, `FrozenShapesCategory`, `FrozenSets`, `AnchorHueSet(s)`,
`StratgSpec`/`StratgParam`/`StratgType`/`StratgResolver`, `NestedRunner`/
`DefaultNestedRunner`, `PureCore`, `Seed`, `Phase`, `Channel`, `Coverage`,
`Inputs`, `ColorChangeEngine`, `ColorChangeDisplay`, spec-stack
(`PartialGoalSpec` → `BroadCategorySpec` → `ArtisticGoalSpec`), and all five
stratgs (`AssignAGsStratg` 921, `AssignAnchorHuesStratg`,
`AssignOstDotsStratg`, `AlternateAGPoolsStratg` 920, `FreezeShapesStratg`).

### 2.2 Domain + facts boundary — done
- `com.csurf.variationmodule.model`: `SmartPic` (immutable ordered map
  keyed 1-based shape number), `SmartDrawObj` (shapeNum + Template-owned
  name + `ShapeColorPrefs`), `ShapeColorPrefs` (valueTarget, defaultAG 1..4,
  four AG preference weights — authored default is authoritative, no argmax),
  `SmartPicFactory`, `SmartPicFactoryException`.
- Positional contract: AGprefs row N ↔ Template `getAllDrawObjs()` position N.
  Defaults-file `shapeName` is informational only; the Template's
  `DrawObj.getName()` names the `SmartDrawObj`.
- `src/main/resources/agprefs/authored-defaults-v1.json` — 21 authored rows
  transcribed constant-for-constant from
  `SmartPicShapeRoster2.setColorFuncAttribsForAllShapes` (CleanColor), row
  order re-sorted to the current Template draw-object order; re-synced to
  `dev_storage/agprefs/` on every chooser open (stale-extract bug fixed).
- `SmartPicFacts` (varyengine.model) — the `Facts` adapter over `SmartPic`:
  `numShapes`, `shapeIds`, `defaultAG`, `agPref`, `valueTarget`, `shapeName`,
  loud missing-shape error. This is the decided adapter pattern (v1E D2):
  `SmartPic` does not implement `Facts`; the engine depends on facts, the
  domain does not depend on the engine.

### 2.3 Cycle runner + palette DTO — done
- `varyengine.controller.VariationPaletteExtractor`:
  `runCleanCycle(hueSet, frozenSet, facts)` (static, stateless — cook + run,
  returns `List<VariationResult>` in recipe order) and
  `extract12VariationPalettes(results)` (per-window instance state: last
  results + last DTO). Dense-keyspace and null checks fail loudly.
- `varyengine.controller.PalettesThe12Variations` — exactly-12, non-null
  validated DTO, 0-based (index 0 = recipe row 1 = display slot 1, the
  bootstrap/reference palette). Palette names: `varyRecipe1..12`.

### 2.4 Display path — done
`VariationDisplayFrame` renders 12 `ThumbnailPanel`s from
`Template.getAllDrawObjs()`. `DrawObj` carries `fillColorOstdot`
(`ColorPtOstDot`) with `setFillColorOstdot`/`setColor`. Applying a variation
is writing a `ColorPtOstDot` onto each DrawObj positionally — the render
path itself needs no new work.

### 2.5 MonotriBook — no work needed
`AssignOstDotsStratg` calls
`MonotriBook.getInstance().getOstDotsByValueRange(hue, target, ...)`;
`com.csurf.ostwald.domainmonotri.model.MonotriBook` already provides the
singleton in this project (also used by `DrawObj` color conversion).

### 2.6 UI scaffolding — done
`CreateSmartPicFrame` (menus: Template / Shape-AGprefs / SmartPic) +
`CreateSmartPicController` (selection, validation, `getSmartPic()`),
launched from both `ApplicFrame` (Templates menu) and `TemplateWorkFrame`
(Color-sources menu). Frame opens at `OPEN_LOCATION` (top-left, below
ApplicFrame).

## 3. What is missing

1. **Run glue** — nothing invokes `runCleanCycle`/`extract12VariationPalettes`.
   The built `SmartPic` sits in `CreateSmartPicController.getSmartPic()`.
2. **Palette→Template application** — the positional loop that writes each
   `PaletteWName`'s `ColorPtOstDot[]` onto a Template copy's DrawObjs.
3. **Run-parameter UI** — `hueSet` and `frozenSet` (the `cook` inputs) need a
   user choice. Legacy had dedicated controls; `VariationDisplayFrame`'s
   empty `controlPanel` is the designed landing spot, or a pick inside the
   SmartPic flow.
4. **Wiring decision** — where the run lives (see §4.3).

## 4. Design decisions and considerations

### 4.1 Reuse the existing 12-copy vehicle
`VariationDisplayController` currently implements the "V1 hack": it loads
the same saved Template 12 times (fully independent instances — no clone
machinery) and renders them identically. This is exactly the right vehicle:
load Template ×12, apply palette k to copy k, hand to
`VariationDisplayFrame`. No changes to the frame; the controller's launch
path gains a palette-application step.

### 4.2 Palette application is positional — same contract as facts
`VariationPaletteExtractor` produces a dense `ColorPtOstDot[]` where array
index i ↔ shape ID i+1 ↔ `Template.getAllDrawObjs().get(i)`. The same
positional contract that binds AGprefs rows to draw objects binds palette
cells to draw objects. Guard: assert palette array length == drawObj count
(the factory already enforces the analogous check for the defaults file).

### 4.3 Where the run lives — recommended option
Extend `CreateSmartPicController`/`CreateSmartPicFrame` (new SmartPic menu
item, e.g. "Create 12 Variations"): SmartPic + Facts already live there;
after the extractor produces the DTO, open a `VariationDisplayFrame` with 12
palette-colored Template copies. Keeps one feature entry point and matches
the controller-per-window pattern. Alternative: a separate
`VariationRunController` — cleaner separation but adds a second controller
that must receive the `SmartPic` across a hand-off.

### 4.4 Extractor instance scope
`VariationPaletteExtractor` is deliberately instantiable — "one instance per
display window" per its javadoc (last-results state stays out of static
scope). The owning controller should hold one instance per
`VariationDisplayFrame` it opens, same lifetime as the window.

### 4.5 hueSet / frozenSet parameters
`FrozenShapesCategory.cook(hueSet, frozenSet)` takes set indices drawn from
the `AnchorHueSets`/`FrozenSets` registries. For V1, a minimal chooser
(small dialog or two combo boxes in `VariationDisplayFrame`'s 60px
`controlPanel`) suffices — defer richer UX. Confirm the valid set indices
from the registry classes before wiring defaults.

### 4.6 AuthoredFactsTable is superseded, keep for reference
The ported `AuthoredFactsTable` (21-row transcription) still compiles but
the production path should use `SmartPicFacts` — the authored table exists
as a fixture/parity reference, not as the facts source for a user-chosen
Template.

### 4.7 Parity verification
Because `authored-defaults-v1.json` is a constant-for-constant transcription
of `SmartPicShapeRoster2`, a run over a Template whose draw-object order
matches the legacy roster should produce palettes identical to a CleanColor
run with the same hueSet/frozenSet — a strong regression check.

### 4.8 Open questions
- Which (hueSet, frozenSet) values are the intended V1 defaults?
- Should the variation run be re-runnable in place (new params → re-color
  the same 12 copies) or always open a new window? Per-window extractor
  state supports either; re-run-in-place is friendlier for comparing sets.
- Save support: `PaletteStorage`/`PaletteWShapeNamesStorage` exist for
  persisting `PaletteWName`s — out of V1 scope unless requested.

## 5. Plan (ordered steps)

1. **Parameter inventory** — read `AnchorHueSets`/`FrozenSets`/`FrozenShapesCategory`
   to enumerate valid (hueSet, frozenSet) inputs and pick V1 defaults.
2. **Run glue** — in `CreateSmartPicController`: `createVariations()` builds
   `SmartPicFacts`, calls `VariationPaletteExtractor.runCleanCycle` +
   `extract12VariationPalettes`, holds the `PalettesThe12Variations`.
3. **Palette→Template apply** — small method/helper: load Template ×12 (reuse
   `TemplateStorage` pattern from `VariationDisplayController`), write
   `dots[k]` to `drawObjs.get(k).setFillColorOstdot(...)` per copy, guard on
   count.
4. **Menu item** — add "Create 12 Variations" (or similar) to
   `CreateSmartPicFrame`'s SmartPic menu → `Actions` callback → step 2–3 →
   open `VariationDisplayFrame` with the 12 colored templates.
5. **Parameter UI** — minimal hueSet/frozenSet selection; start with V1
   defaults, add chooser in `controlPanel` only if needed.
6. **Verify** — run end-to-end, compare output against a CleanColor run for
   the same inputs (§4.7). Compile + visual check of the 12 thumbnails.
7. **Commit** — new COMMIT_MESSAGE doc per repo convention.

## 6. Difficulty estimate

Low-moderate overall. Every step is on an established pattern (controller
glue, JMenuItem callback, positional array loop). The only genuinely
uncertain item is step 6 — runtime parity depends on `MonotriBook`'s data
being complete in this project and on `cook` semantics matching legacy; both
are checkable the moment the path is wired. Estimated effort: one focused
session, no engine changes expected.
