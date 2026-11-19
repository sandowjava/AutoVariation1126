# 111826 v0E — frozenSet Bug Analysis

**Date:** Nov 19, 2026
**Status:** Analysis paused for commit; code verified correct at every tested level; symptom remains unexplained pending user-side re-check.

---

## 1. The Reported Bug

> "The user's chosen frozenSet in the 'Variation run parameters' UI does not seem to have any effect on the 12 variations that are produced."

Follow-up clarification from the user:

- The 12 variations are **NOT** identical — each of the 12 differs.
- Judged visually, there is **no evidence of any set of frozen drawObjs** being put into effect.

That second fact matters: since the variations differ, cooking, running, extraction, and the Template-apply path are all functioning. Only the freeze overlay is missing from the visible output.

## 2. The Mechanism Under Investigation

The intended freeze semantics (clean-design port of legacy 999/031):

- Recipe row 1 = the **bootstrap** run — no freeze goal; its result becomes the shared **base**.
- Recipe rows 2–12 = processed runs — each carries a `FREEZE_SHAPES` StratgSpec bound to the UI-chosen `frozenSet` index (0..5, indexing `FrozenSets`).
- `FreezeShapesStratg` (id 031) sets `frozen` flags on the live `RunState` for each shape in the selected set.
- `VariationRunState.effectiveOstDot(shape)` resolves `frozen(shape) ? base.ostDot(shape) : ostDot(shape)` — frozen shapes read the **base (slot-1)** color at effective-read time; the decided layer underneath remains for provenance.
- `materialize()` builds `effectiveColors` via `effectiveOstDot` — the palette projection point.
- `VariationPaletteExtractor.toPaletteWName` maps `effectiveColors` shape k → palette array index k−1.
- `CreateSmartPicController.loadPaletteColoredTemplates` applies `dots[i]` → `getAllDrawObjs().get(i).setFillColorOstdot(...)` on 12 independently loaded Template copies.
- `VariationDisplayFrame`/`ThumbnailPanel` renders each DrawObj via `getAWTColor()` ← `fillColorOstdot`.

## 3. Verification Chain — Every Link Checked

| # | Link | Evidence | Result |
|---|------|----------|--------|
| 1 | UI: `chooseRunParameters()` returns `{hueSet, frozenSet}`; `createVariations` passes both to `runCleanCycle` | code read | OK |
| 2 | `FrozenShapesCategory.cook` binds `StratgParam.FROZEN_SET` on rows 2–12; row 1 has no freeze goal | `CookTest` green | OK |
| 3 | `ArtisticGoalSpec.flattenStratgSpecs` includes the 031 spec; `PartialGoalSpec` stores all specs | `SpecStackTest` + reads | OK |
| 4 | Engine sorts descending by stratg id → 921/920 → 555 → 255 → **031 last** (correct for flag overlay) | `EngineTest` green | OK |
| 5 | `BasicStratgContext.runState()` returns the live `VariationRunState` — flag writes land on the real state | code read | OK |
| 6 | `FreezeShapesStratg` reads `FROZEN_SET`, looks up `FrozenSets.get(...)`, calls `setFrozen(shape, true)`; rejects unknown shape IDs loudly | `StratgTest` green | OK |
| 7 | `VariationDriver` gives row 1 a no-base run, rows 2–12 the shared base | `EngineTest` green | OK |
| 8 | `effectiveOstDot` returns base dot when frozen; `materialize` uses effective colors | `RunStateTest` green | OK |
| 9 | `VariationResult.effectiveColors()` carries reinstated base colors at frozen positions | `EngineTest` green | OK |
| 10 | `toPaletteWName` reads `effectiveColors` (not decided dots); `getPaletteAt` is 0-based recipe order | `ExtractorTest` (new) green | OK |
| 11 | `TemplateStorage.loadTemplate` = fresh `mapper.readValue` per call → 12 truly independent copies (no aliasing) | code read | OK |
| 12 | `DrawObj.setFillColorOstdot` sets the exact field `getAWTColor()` renders | code read | OK |
| 13 | `SmartPicFactory` numbers SmartDrawObjs positionally (`i+1` ↔ `drawObjs.get(i)`); same positional contract as palettes | code read | OK |
| 14 | Full seam on real files — real `TemplateStorage` + `SmartPicFactory` + `authored-defaults-v1.json` + `SmartPicFacts` + positional apply | `SmartPicCycleTest` (new) green | OK |

## 4. Tests Ported and Added

Ported (imports rewritten `csurf.cleandesign.*` → `com.csurf.*`): `CookTest`, `SpecStackTest`, `RunStateTest`, `StratgTest`, `NestedExecutionTest`, `EngineTest` — all green (66 tests).

Deleted (unportable — require live legacy classes): `DomainDataTest`, `FactExtractionTest`, `GoldenMasterTest`.

Added during this investigation:

- **`ExtractorTest`** — for all six frozenSets, slots 2–12 `assertSame` the slot-1 `ColorPtOstDot` at every frozen shape; frozenSet 0 vs 1 produce different palettes. **2/2 green.**
- **`SmartPicCycleTest`** — real-data reproduction of the controller seam plus a per-shape color dump. **3/3 green.**

## 5. Decisive Evidence — the freeze IS applied

Dump from `SmartPicCycleTest.realTemplateCycle_dumpFreezeEvidence` (real template `heroEye111726purplsFewBrwns-v1wAGs`, hueSet 1, frozenSet 0 = `{8,9,10,11,13}`), Ostwald locations `hue:fillOrder`:

```
shape  8 cheekLwrCurve     slot1=7:1   slot2=7:1   FROZEN(same)
shape  9 nearNoseSloping   slot1=5:13  slot2=5:13  FROZEN(same)
shape 10 nearNoseCrease    slot1=7:13  slot2=7:13  FROZEN(same)
shape 11 templeDiagUppr    slot1=5:2   slot2=5:2   FROZEN(same)
shape 13 underBrowInner    slot1=5:1   slot2=5:1   FROZEN(same)
... all 16 other shapes: varied
```

Slot 2 reinstates slot-1 colors at exactly the frozen positions while everything else varies. Identical invariants hold for all six sets.

## 6. Candidate Bug Sources — All Ruled Out

- **Frozen-set index not bound** — `CookTest` asserts `FROZEN_SET` param on every row 2–12.
- **031 spec dropped in flatten/schedule** — green tests; `hasFreezeStratg` driver check would throw anyway.
- **Flag writes lost on a scratch** — `BasicStratgContext` passes the live state; `RecordingRunState` is only used for nested runs (031 is never nested; `DefaultNestedRunner` would throw `FLAG_WRITE_ERROR`).
- **Materialize using decided instead of effective** — verified in code and by `EngineTest`/`RunStateTest`.
- **Extractor reading decided colors** — reads `effectiveColors()`; proven by `ExtractorTest`.
- **Template copies aliasing one instance** — fresh deserialization per `loadTemplate` call.
- **Wrong DrawObj color field** — `setFillColorOstdot` is exactly what `getAWTColor()`/`drawShape` render.
- **Positional misalignment** — `SmartPicFactory` numbers `i+1` ↔ `drawObjs.get(i)`; extractor maps shape k → index k−1; apply uses the same index. A fixed permutation could not hide freezing anyway (a permuted position would still hold a constant color across slots).
- **UI passing the wrong param** — `params[1]` = `frozenSetBox.getSelectedItem()`; bound through `cook(hueSet, frozenSet)`.

## 7. Remaining Explanations (ranked)

1. **Semantic expectation gap (most likely).** "Frozen" = *inherits the slot-1 bootstrap coloring*, NOT *keeps the template's original colors*. `Facts` carries no colors at all (AG prefs + value targets only), so the original template coloring cannot appear in any variation. If the user looked for shapes retaining original colors, no such evidence exists by design — but the freeze is present as positions identical to **panel 1** across panels 2–12.
2. **Subtle visual detection.** Small sets are easy to miss: set 5 = `{15,17}` (2 of 21 shapes); set 0 freezes five nose/brow-area shapes. Suggested check: **frozenSet 4** = `{1,2,3,4,5,6,7,12,18,19,20,21}` — 12 shapes, half the face should match panel 1.
3. **Environmental.** A stale build/frame or comparing frames from earlier runs. If frozenSet-4 shows no panel-1 constancy, re-investigate here.
4. **Design parity question (if "keep original colors" was intended).** Whether the legacy 999 "start-state" captured the *original* template colors rather than the bootstrap result is unresolved in this repo (`GoldenMasterTest` needed legacy classes and was deleted). If original-color freezing was intended, it requires a design change — feeding template colors into the run as the base — not a wiring fix.

## 8. Artifacts of This Investigation

- `src/test/java/pkgVariationModule/ExtractorTest.java` — extractor-seam freeze assertions (keep as regression test).
- `src/test/java/pkgVariationModule/SmartPicCycleTest.java` — real-data seam test + `realTemplateCycle_dumpFreezeEvidence` diagnostic dump.
- JUnit 5 added to `pom.xml` (user).

## 9. Next Steps When Analysis Resumes

- Ask the user to re-run with **frozenSet 4** and compare panels 2–12 to panel 1.
- If still invisible: instrument `loadPaletteColoredTemplates` to log `(hue, fillOrder)` per frozen position, or add a temporary visual marker to frozen DrawObjs.
- Decide the semantic question: should "frozen" reinstate the bootstrap result (current, test-verified) or the original template coloring (requires `Facts`/`VariationResult` extension)?
