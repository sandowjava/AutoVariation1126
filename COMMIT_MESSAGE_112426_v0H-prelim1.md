# COMMIT_MESSAGE_version 0H-prelim1 (112426): Named AGHuePattern keys replace the fixed AnchorHueSets table

## Summary

Hard schema cut in the variation engine: the integer `hueSet` index
(0–24 into the fixed `AnchorHueSets` table) is replaced by a named
`agHuePattern` String key resolved through the new `AGHuePatterns`
catalog. Two new model classes are added — `AGHuePattern` (a named
wrapper composing `AnchorHueSet`) and `AGHuePatterns` (the static
name-keyed catalog) — and `AnchorHueSets` is deleted. The catalog
carries the 23 legacy-derived patterns: rows 0–18, 20–22, and 24 of
the old table, with duplicate rows 19 (≡0) and 23 (≡1) dropped, each
named `"NN-NN-NN-NN-legacyTableRowNumR"` — zero-padded AG-order hue
prefix plus row provenance. The run default is preserved as
`"13-10-06-08-legacyTableRowNum18"`. No persistence yet — this phase
is schema/catalog only; `FrozenSets` and its integer `TABLE_KEY`
behavior are unchanged. All 80 tests green.

## New classes

- **`AGHuePattern`** (`varyengine.model`) — `final` named value type:
  `name` + composed `AnchorHueSet`. Constructors take either four hue
  ints or an existing `AnchorHueSet`; delegates `hueNumForAG`/
  `anchorHueForAG`; `equals`/`hashCode` are name-based.
- **`AGHuePatterns`** (`varyengine.model`) — static catalog backed by
  a `TreeMap` on lowercased canonical keys (case-insensitive lookup).
  Canonicalization API: `canonicalPrefix(h1..h4)` produces the
  zero-padded `NN-NN-NN-NN` AG-order prefix (so lexicographic order =
  numeric order), `canonicalName` joins prefix + optional suffix,
  `canonicalize` trims/collapses whitespace and strips spaces around
  hyphens, `canonicalKey` lowercases for map keys. Accessors: `get`
  (loud `IllegalArgumentException` on unknown names), `exists`,
  `all`, `allNames` (original-cased, sorted), `count`, `BUILTIN_COUNT`.
- **`AGHuePatternsTest`** — catalog coverage: canonical naming,
  dedup count (23), row-provenance names, case-insensitive lookup,
  prefix-is-not-a-key, sorted `allNames`.

## Modified classes

- **`ParamKind`** — new `NAMED_KEY` kind for String catalog keys,
  distinguished from integer `TABLE_KEY` (`FROZEN_SET` keeps it).
- **`StratgParam`** — `HUE_SET("hueSet", TABLE_KEY)` replaced by
  `AGHUE_PATTERN("agHuePattern", NAMED_KEY)`.
- **`StratgSpec`** — added `bind(param, String)` (kind-guarded to
  `NAMED_KEY`, non-null) and `getStringParam`.
- **`StratgType`** — `ASSIGN_ANCHOR_HUES` (555) now requires
  `AGHUE_PATTERN` + `CONTROL_LEVEL`.
- **`AssignAnchorHuesStratg`** — resolves the bound name via
  `AGHuePatterns.get(...).getAnchorHueSet()` instead of indexing a table.
- **`FrozenShapesCategory`** — all `cook` overloads take
  `String agHuePattern`, fail-fast validate through the catalog, and
  bind the name into every recipe row's 555 spec.
- **`VariationPaletteExtractor.runCleanCycle`** — signature now
  `(String agHuePattern, int frozenSet, Facts)`.
- **`CreateSmartPicController`** — the run-parameters dialog's first
  combo is now a `JComboBox<String>` populated from
  `AGHuePatterns.allNames()` with the row-18 default; the controller's
  run state (`RunParams` record, `lastAgHuePattern`), status lines,
  and variation window titles carry the pattern name.
- **`SmartPicAGCtrsFrozenSetFrame`** — the diagnostic display class:
  its hue-side combo now lists the 23 canonical pattern names, its
  info-panel border title reads `agHuePattern <name>`, and it shows
  the same four `AGn → hue` lines resolved via
  `AGHuePatterns.get(name)`. The frozenSet side is unchanged
  (integer `FrozenSets` index).
- **Deleted**: `AnchorHueSets.java` (the 25-row int-indexed table).
- **Tests** — `CookTest`, `StratgTest`, `SpecStackTest`,
  `NestedExecutionTest`, `EngineTest`, `ExtractorTest`,
  `SmartPicCycleTest` migrated to `NAMED_KEY`/`AGHUE_PATTERN` String
  bindings and name arguments.

## Execution flow

```
CreateSmartPicController.chooseRunParameters()
  → JComboBox<String> over AGHuePatterns.allNames()
      (default "13-10-06-08-legacyTableRowNum18")
  → RunParams(String agHuePattern, int frozenSet)
→ VariationPaletteExtractor.runCleanCycle(name, frozenSet, facts)
  → FrozenShapesCategory.cook(name, frozenSet)
      → AGHuePatterns.get(name)           [fail-fast validation]
      → 12-row recipe: each row's 555 spec
        .bind(StratgParam.AGHUE_PATTERN, name)   [String, NAMED_KEY]
  → VariationDriver.run(recipe, facts)
    → ColorChangeEngine per row
      → AssignAnchorHuesStratg.execute
          → AGHuePatterns.get(spec.getStringParam(AGHUE_PATTERN))
              .getAnchorHueSet()
          → decided AG → anchorHueForAG → state.setHue
→ diagnostics: SmartPicAGCtrsFrozenSetFrame(smartPic, name, frozenSet)
    name combo + AGn→hue info panel (catalog-resolved)
```

## Design notes

- **Name = key, object stays out of the spec**: `StratgSpec` stores
  the String catalog key, not the live `AGHuePattern`; resolution
  happens at execution time in the strategy, mirroring how
  `TABLE_KEY` worked but with meaningful, self-describing keys.
- **Prefix carries the data**: the `NN-NN-NN-NN` prefix encodes the
  four AG anchor hues in AG order, so picker lists sort numerically
  and a pattern's hues are readable from its name; the
  `legacyTableRowNumN` suffix preserves provenance to the old table.
- **Dedup by canonical key**: legacy rows 19 and 23 were exact
  duplicates of rows 0 and 1 — dropping them takes the table from 25
  indices to 23 named patterns with no loss.
- **Deferred (Phase 2)**: persistence (`dev_storage/aghuepatterns/`
  JSON, save dialog) and four pattern-level doubles — the catalog's
  `canonicalName`/`canonicalKey` helpers already support user-added
  patterns.
- **New dev_storage artifacts**: generated `-Rmatrix` AGprefs
  defaults files plus their source 4-hue Templates/palettes, saved
  during manual end-to-end testing.
