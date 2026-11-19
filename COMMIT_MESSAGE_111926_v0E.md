# COMMIT_MESSAGE_version 0E (111926): Create 12 Variations flow + JUnit 5 test-suite port + frozenSet bug-analysis doc

## Summary

Implements the remaining-work plan from v0D: the full "Create 12
Variations" flow (parameter chooser → clean cycle → palette extraction →
positional apply onto 12 independent Template copies →
VariationDisplayFrame) is wired into the SmartPic menu. Ports the legacy
`pkgVariationModule` JUnit 5 test suite (6 classes, imports rewritten to
`com.csurf.*`), adds JUnit to `pom.xml`, and adds two new seam/diagnostic
tests written during the frozenSet investigation. Also adds the v0E
bug-analysis document.

**All 71 tests green.** Key outcome of the frozenSet investigation: the
freeze mechanism is verified correct end-to-end — including with real
`dev_storage` files — so no wiring bug exists in the current path. The
analysis doc captures the full evidence and the remaining (semantic vs.
environmental) explanations.

## New test classes — `src/test/java/pkgVariationModule`

- **`CookTest`** (6 tests) — `FrozenShapesCategory.cook` emits the 12-row
  recipe: row 1 bootstrap with no freeze goal; rows 2–12 carry
  `FREEZE_SHAPES` bound to the chosen `frozenSet`; AG/hue/dot stratg
  bindings per recipe row; `cook(BroadCategorySpec)` extracts dialog
  bindings; out-of-range keys fail loudly.
- **`SpecStackTest`** (15) — `StratgSpec` completeness predicate, param
  binding guards (undeclared/wrong-kind/open-read), phase confinement on
  `PartialGoalSpec`, `copyForCooking` deep-copy round trip, structural
  equality, external IDs ("921"…"031") and phase bands.
- **`RunStateTest`** (19) — `VariationRunState` channel semantics:
  effective reads resolve `frozen → base` vs decided layer underneath,
  UNDECIDED propagation, no-base effective reads throw, scratch
  seeding/isolation, `RecordingRunState` write-exactness + strict flag-
  write error, `apply` scope/channel-masking/coverage, materialization.
- **`StratgTest`** (12) — per-stratg units on stub facts: 921 pref-level
  semantics incl. legacy quirks (min/midMin/midMax, authored defaultAG
  beats argmax), 555 AG → anchor hue, 255 first-candidate dot pick +
  empty-set error, 031 writes only flags, shell-delegation invariant.
- **`NestedExecutionTest`** (7) — `PureCore` fast path vs forced-scratch
  path equivalence across seeds, seeded dependent inner runs,
  frozen-flag inertness, nested flag-write error, 920 odd/even pool
  alternation end-to-end.
- **`EngineTest`** (7) — descending-ID scheduling, open-spec rejection,
  processed-run freeze overlay (`frozenShapes` = selected set, frozen
  cells share the base dot), 12-run driver cycle with per-row base
  reinstatement, distinct rows, driver contract validation.
- **`ExtractorTest`** (2, new) — exercises the exact controller seam
  (`runCleanCycle` + `extract12VariationPalettes`): for all six
  frozenSets, slots 2–12 `assertSame` slot-1 dots at frozen positions;
  different frozenSets produce different palettes.
- **`SmartPicCycleTest`** (3, new) — real-data reproduction with a saved
  `heroEye*wAGs` template + `authored-defaults-v1.json`: real
  `TemplateStorage` → `SmartPicFactory` → `SmartPicFacts` → clean cycle →
  palettes → positional `setFillColorOstdot` on fresh template copies;
  frozen DrawObjs reinstate slot-1 colors; plus a per-shape slot1-vs-slot2
  Ostwald-location dump (`realTemplateCycle_dumpFreezeEvidence`).

Removed (unportable — need live legacy classes): `DomainDataTest`,
`FactExtractionTest`, `GoldenMasterTest`.

## Modified classes

- **`CreateSmartPicController`** — `createVariations()`: param dialog
  (`hueSet` 0–24, `frozenSet` 0–5) → `SmartPicFacts(smartPic)` →
  `VariationPaletteExtractor.runCleanCycle` → `extract12VariationPalettes`
  → `loadPaletteColoredTemplates` (12 fresh `TemplateStorage.loadTemplate`
  copies; `dots[i] → drawObjs.get(i)` positional apply with size check) →
  `VariationDisplayFrame` titled with chosen parameters.
- **`CreateSmartPicFrame`** — "Create 12 Variations" item under SmartPic
  menu → `actions.createVariations()`.
- **`pom.xml`** — JUnit 5 (jupiter) test dependency.

## New documents

- **`111826_v0E_SWE-2_frozenSet_BUG_analysis.md`** — the full frozenSet
  investigation: the 14-link verification chain, the per-shape evidence
  dump proving frozen DrawObjs hold slot-1 colors on real data, every
  ruled-out bug source, and the ranked remaining explanations (semantic
  expectation gap: frozen = slot-1 bootstrap colors, not original
  template colors; small-set subtlety; environmental staleness; and the
  unresolved design question of whether legacy 999's "start-state" was
  the original coloring).

## Execution flow

```
SmartPic menu → "Create 12 Variations"
  → Variation run parameters dialog (hueSet, frozenSet)
  → SmartPicFacts(smartPic)
  → FrozenShapesCategory.cook → VariationDriver (12 runs, shared base)
  → extract12VariationPalettes → PalettesThe12Variations
  → 12 fresh Template copies, positional palette apply
  → VariationDisplayFrame (4×3 thumbnails)
```

## Test data fixtures

- `dev_storage/agprefs/authored-defaults-v1.json` — extracted bundled
  AGprefs defaults (also re-synced at runtime from the classpath).
- `dev_storage/templates/heroEye111726purplsFewBrwns-v1wAGs.json` — the
  21-shape face template `SmartPicCycleTest` runs against.
