# COMMIT_MESSAGE_version 0C (111926): Create SmartPic flow — variationmodule domain classes, SmartPicFactory, and CreateSmartPicFrame

## Summary

Adds the initial "Create SmartPic" feature: a minimal end-to-end path that
builds a `SmartPic` — the graphic-intelligence domain object for a chosen
Template — from a saved `Template` plus an AGprefs defaults file. This is the
data-contract foundation for the palette-variation engine port: the SmartPic
carries each shape's authored color preferences (`ShapeColorPrefs`), which
will feed the `Facts` interface at the engine boundary in a later step.

## New classes

### Domain model — `com.csurf.variationmodule.model`

- **`SmartPic`** — immutable ordered map (1-based shape number → `SmartDrawObj`),
  preserving the Template's draw-object order. Deliberately independent of the
  variation engine; an adapter will implement the engine-facing `Facts`
  interface at the boundary in a later step.
- **`SmartDrawObj`** — one smart draw object: `shapeNum` (1-based positional
  index), `name` (taken from the chosen Template's `DrawObj`, per the user's
  mental model that the SmartPic belongs to that Template), and
  `ShapeColorPrefs`.
- **`ShapeColorPrefs`** — immutable authored per-shape color facts:
  `valueTarget`, `defaultAG` (validated 1..4), and four AG preference weights
  with validated `agPref(ag)` access. The authored `defaultAG` is
  authoritative; the engine must never argmax the prefs.
- **`SmartPicFactory`** — parses an AGprefs defaults JSON file with Jackson,
  validates it (rows ordered 1..N, `defaultAG` 1..4, exactly 4 weights), and
  zips rows positionally to `Template.getAllDrawObjs()` — the positional
  contract between file order and Template draw-object order. Fails on row/
  drawObj count mismatch; logs a warning on `shapeName` vs `DrawObj.getName()`
  mismatch (Template name is authoritative, file names are informational).
- **`SmartPicFactoryException`** — unchecked exception for file-load,
  validation, and Template/file mismatch failures (mirrors
  `TemplateStorageException`).

### View + controller — `com.csurf.variationmodule.view` / `.controller`

- **`CreateSmartPicFrame`** — the feature's UI window. A "SmartPic" menu holds
  three items driving the flow:
  - *Choose saved Template...* — `JFileChooser` rooted at
    `dev_storage/templates`; the controller loads the file via
    `TemplateStorage` and displays the Template's name.
  - *Choose AGprefs defaults file...* — `JFileChooser` rooted at
    `dev_storage/agprefs`; the controller validates the pick via
    `SmartPicFactory.loadAgPrefs` and displays the file's `name` label.
  - *Create SmartPic* — calls `SmartPicFactory.createSmartPic(template, file)`;
    success shows "Created SmartPic: N shapes", failures show error dialogs.
  The frame is view-only: menu actions delegate to an `Actions` interface
  implemented by the controller.
- **`CreateSmartPicController`** — orchestrates the feature, mirroring
  `VariationDisplayController` (one controller per window; holds the built
  `SmartPic` via `getSmartPic()` for the next stage). On first use of the
  AGprefs chooser it extracts the bundled `authored-defaults-v1.json` from
  classpath resources into `dev_storage/agprefs/` — now re-synced on every
  open so edits to the source-of-truth resource propagate (fixes the
  stale-extract bug where the chooser silently served the first run's copy).
  Two constructors: `(mediator, ownerFrame)` for launches from
  `TemplateWorkFrame`, `(mediator)` for launches from `ApplicFrame`.

## New data file

- **`src/main/resources/agprefs/authored-defaults-v1.json`** — the authored AG
  preferences for all 21 SmartDrawObjs, transcribed constant-for-constant from
  `SmartPicShapeRoster2.setColorFuncAttribsForAllShapes` in the
  CleanColorVariation1126 rehearsal project. Rows carry
  `{ shape, shapeName, valueTarget, defaultAG, agPrefs[4] }`; `shapeName` is
  informational only. Row order has been re-sorted to match the current
  Template's draw-object order (positional contract). Future defaults files
  (e.g., darker/lighter valueTargets) drop into `dev_storage/agprefs/`.

## Modified files

- **`StorageConfig`** — added `getAgPrefsPath()` → `dev_storage/agprefs`,
  alongside `templates`/`palettes`.
- **`TemplateWorkFrame`** — "Create SmartPic" item added to the Color-sources
  menu after "Palette variations"; `openCreateSmartPic()` launches a new
  controller + frame (multi-window support).
- **`ApplicFrame`** — "Create SmartPic" item added to the Templates menu after
  "Open Saved Template"; `openCreateSmartPic()` launches the controller with
  no owning TemplateWorkFrame and registers the frame in the Windows-open
  list.
- **`SmartPic` → `SmartPicPrelimVestige`** (`com.csurf.artintellig.model`) —
  renamed the months-old exploratory placeholder to free the `SmartPic` name
  for the new variation-module domain class; the package is retained for
  reference only.

## Execution flow

Templates menu (ApplicFrame) or Color-sources menu (TemplateWorkFrame) →
`openCreateSmartPic()` → `CreateSmartPicController.launch()` →
`CreateSmartPicFrame` opens → user picks Template + AGprefs file via the
frame's SmartPic menu → `SmartPicFactory` validates the file and zips it
positionally to `Template.getAllDrawObjs()` → `SmartPic` of `SmartDrawObj`s
with `ShapeColorPrefs` held in the controller, ready for the engine-side
`Facts` adapter.
