# COMMIT_MESSAGE_version 0H (112326): "Create RMatrix from Template" — automated tailored RelationMatrix generation

## Summary

Adds the "Create RMatrix from Template" functionality: a new item in
the Templates menu of `ApplicFrame` that opens `CreateRMatrixFrame`
and automates what `authored-defaults-v1.json` did by hand —
generating a tailored RelationMatrix (an `AgPrefsDocument`,
`formatVersion` 2) for any saved Template that has exactly four
distinct hues "in use". The user picks a Template, assigns the four
detected hues to AG1–AG4 in a bijection-enforcing widget, confirms,
and saves the generated defaults file under `dev_storage/agprefs`.
The file is consumed **unchanged** by the existing Create SmartPic
flow — this version ends at the save. Design doc:
`112326_v1H_SWE-2_create_RMatrix_from_Template.md`.

## New classes

- **`RMatrixFactory`** (`variationmodule.controller`) — the automated
  RelationMatrix creation logic. `occupiedHues(Template)` collects
  each DrawObj's `getUserFacingHueNum()` into a `TreeSet` (the
  per-DrawObj equivalent of `APDisplayPanel.buildOccupiedHuesArray`).
  `eligibilityError(Template)` is the guard: hard-rejects Templates
  without exactly 4 distinct occupied hues or with any unset fill.
  `createOneShapeColorPrefs(DrawObj, AnchorHueSet)` assigns
  `defaultAG` by exact hue match to an anchor hue (no distance
  arithmetic, no ties), sets `agPrefs` from a constant 4×4 affinity
  table — each row a permutation of {0.95, 0.65, 0.35, 0.05}: own AG
  0.95, near-complement partner 0.05, analogous partner 0.65,
  remaining cross pair 0.35 — and `valueTarget` from
  `DrawObj.getRGBLum3D()`. `createAllShapeColorPrefs` loops the
  Template's draw order and emits a UUID-keyed `AgPrefsDocument`.
  Placed in `controller` so `model` stays free of the varyengine
  `AnchorHueSet` dependency.
- **`CreateRMatrixController`** (`variationmodule.controller`) —
  orchestrates the session, mirroring `CreateSmartPicController`
  (one controller per open frame): template chooser → guard →
  hue→AG assignment → confirm → save dialog → write. Any selector
  change after confirmation invalidates the `AnchorHueSet` and
  re-disables Create. The save dialog lives in
  `dev_storage/agprefs`, suggests a name derived from the first 20
  characters of the Template name, is fully editable, and confirms
  overwrites; `doc.description` records provenance (source Template
  file + the four AG anchor hues).
- **`CreateRMatrixFrame`** (`variationmodule.view`) — the display
  class: a fixed-size `JFrame` (480×300) mirroring
  `CreateSmartPicFrame`'s Actions-interface pattern. Displays a
  Template/status header; a word-wrapping, non-editable `JTextArea`
  carrying the AG-assignment guidance text (10px, wraps to window
  width); four rows each showing a pure-hue swatch
  (`MonotriBook.getPureHueColorByCodeHueNum`) + the detected hueNum +
  an AG1–AG4 combo selector — selection uses move semantics so each
  AG is held by exactly one row (bijection enforced); and a
  "Confirm anchor hues" button enabled only when the bijection is
  complete. Menus: "Template → Choose saved Template...", "RMatrix →
  Create RMatrix file..." (enabled after confirmation).

## Modified classes

- **`ApplicFrame`** — new Templates-menu item "Create RMatrix from
  Template" → `openCreateRMatrix()`, which launches a
  `CreateRMatrixController` and registers its frame (same pattern as
  `openCreateSmartPic`/`openRelationMatrixEditor`).

## Execution flow

```
Templates menu "Create RMatrix from Template"
  → new CreateRMatrixController(appMediator).launch()
    → CreateRMatrixFrame (480x300, at OPEN_LOCATION)
    → "Choose saved Template..."  → JFileChooser on dev_storage/templates
      → TemplateStorage.loadTemplate(name)
      → RMatrixFactory.eligibilityError(template)   [guard]
          fails → JOptionPane error; frame refuses to proceed
          ok    → Set<Integer> occupiedHues (sorted, size 4)
    → frame.showHueAssignment(hues) — 4 swatch+selector rows,
      move-semantics bijection; each change → assignmentChanged()
      (invalidates confirmed AnchorHueSet, disables Create)
    → "Confirm anchor hues" → frame.getAnchorAssignment() = int[4]
      ordered by AG → new AnchorHueSet(h1..h4) → Create enabled
    → "Create RMatrix file..." →
      RMatrixFactory.createAllShapeColorPrefs(template, anchorHueSet)
        per DrawObj: defaultAG = AG whose anchor hue ==
                     drawObj.getUserFacingHueNum()  (exact match)
                     agPrefs  = constant row for that defaultAG
                     valueTarget = drawObj.getRGBLum3D()
        → AgPrefsDocument {formatVersion 2, drawObjId = UUID per row,
                           Template draw order}
      → save dialog (suggested name = first 20 chars of Template
        name + "-rmatrix.json", editable; overwrite confirmed)
      → SmartPicFactory.writeAgPrefs(path, doc)
        → dev_storage/agprefs
      → user interaction ends; file consumed unchanged by
        "Create SmartPic" (v2 UUID binding)
```

## Design notes

- **Exact-match defaultAG**: the 4-distinct-hues guard guarantees
  every DrawObj's hue equals one of the four anchor hues, so
  `defaultAG` is a lookup — "closest distance"/Spread arithmetic is
  deferred (future: `HueCircleStepCalc`, `SpreadPrelim`, `HueNumGUI`).
- **Bijection by construction**: move semantics in the selectors make
  duplicate AGs impossible; the Confirm button only enables on a
  complete 4-AG assignment.
- **formatVersion 2 output**: rows carry `drawObjId` UUIDs in
  Template draw order; `SmartPicFactory.loadAgPrefs`/`createSmartPic`
  reads them unchanged — no changes to the existing pipeline.
- **Deferred**: coupling the tailored `AnchorHueSet` to the variation
  run (the `hueSet` 0–24 index into the fixed `AnchorHueSets` table),
  and the strict "var1 ≈ source Template" acceptance test.
- **New dev_storage artifacts**: generated `-Rmatrix` AGprefs
  defaults files plus their source 4-hue Templates/palettes, saved
  during manual end-to-end testing.
