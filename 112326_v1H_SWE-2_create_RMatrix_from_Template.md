# 112326_v1H_SWE-2_create_RMatrix_from_Template

**Date:** 11/23/26
**Status:** Design plan — analysis and decisions captured; no code written yet.
**Feature:** Automated creation of a RelationMatrix instance ("tailored RMatrix") from a saved Template instance, via a new UI window `createRMatrixFrame`, launched from a new menu item "Create RMatrix from Template" in the "Templates" menu of `ApplicFrame`.

---

## 1. Terminology and Mental Model

- **relationMatrix**: the totality of the `ShapeColorPrefs` instances in a `SmartPic` — the *graphic intelligence* of a SmartPic instance. Each `ShapeColorPrefs` is the graphic intelligence of a single `SmartDrawObj`.
- **`RelationMatrix` class** (`com.csurf.variationmodule.model.RelationMatrix`): already implemented as a **Facade** over the SmartPic's ordered `SmartDrawObj` set. Provides a row view (per-shape prefs), a column view (`defaultAgMembers`), and serialization (`toDocument`/`save`).
- **AnchorHueSet** (`com.csurf.variationmodule.varyengine.model.AnchorHueSet`): four anchor hues h1, h2, h3, h4 — the central hues of the four anchor groups (AGs). User-facing hue numbers 1–36 on the hue circle. Immutable; constructor enforces range and AG ordering.
- **hue pattern / anchorPattern**: the particular arrangement of the four AG center hues within the 36 hues of the hue circle.
- **closest distance** between two hueNums: deferred concept (see §8 Deferred Topics — "Spread arithmetic").

## 2. Background: From Hack to General Pattern

The "hack / proof of concept" is the `authored-defaults-v1.json` file (`src/main/resources/agprefs/`, extracted to `dev_storage/agprefs/` on first use): a pre-defined AGprefs document specifying `ShapeColorPrefs` for 21 drawObjs — in essence, a pre-defined RelationMatrix.

**Existing "Create SmartPic" flow** (`Templates` menu → `CreateSmartPicController` → `CreateSmartPicFrame`):

1. User picks a saved Template (`TemplateStorage.loadTemplate`).
2. User picks an AGprefs defaults file (`.json` in `dev_storage/agprefs`).
3. `SmartPicFactory.createSmartPic(template, agPrefsPath)` binds prefs rows to the Template's `DrawObj`s and produces a `SmartPic` of `SmartDrawObj`s.
4. `createVariations` runs `VariationPaletteExtractor.runCleanCycle(hueSet, frozenSet, facts)` → 12 variation palettes → applied positionally to 12 fresh copies of the Template → `VariationDisplayFrame`.

**Serialization format** (`SmartPicFactory.AgPrefsDocument`, JSON):

- **formatVersion 1 — positional contract**: row N of `shapes[]` binds to `getAllDrawObjs().get(N-1)`. `shape` field must equal its 1-based position. Used by `authored-defaults-v1`.
- **formatVersion 2 — UUID contract**: each row's `drawObjId` must equal a Template DrawObj's `uniqueId`; row order is display-only. `RelationMatrix.save` and `RelationMatrixDraft.toDocument` always emit v2.
- Row fields: `shape`, `shapeName` (informational only — Template name is authoritative), `drawObjId` (v2), `valueTarget` (double), `defaultAG` (1–4), `agPrefs` (exactly 4 doubles).

**New functionality** extends this pattern: instead of a pre-authored defaults file, `RMatrixFactory` *generates* the `AgPrefsDocument` automatically from any suitable saved Template plus a user-designated `AnchorHueSet`. The generated file is saved to `dev_storage/agprefs` and consumed **unchanged** by the existing Create SmartPic flow. This gives the user a "tailored RMatrix" for any Template.

## 3. Locked Decisions

### 3.1 Core simplifying assumption

- The user selects a Template instance that has **exactly four distinct hues "in use"** — `distinctHues == 4`, where a hue is "in use" if at least one DrawObj's fill color occupies it (at least one "occupied position" per hue). Number of drawObjs is irrelevant; the palette always contains colors in four hues only, never fewer.
- "Four distinct hues" = four distinct user-facing hueNums (1–36). A Template using hue 5 on two DrawObjs and hues 7, 9, 13 on others satisfies the assumption (4 distinct total); the number of DrawObjs sharing a hue is irrelevant.
- Assumption: the user created this Template specifically to build a tailored RMatrix.

### 3.2 Occupied-hues detection (guard logic)

- Existing precedent: `TemplateWorkFrame` "Hue Circle" menu → "See occupied hues" (`seeOccupiedHuesItem`) → `AnchorPatternFrame` (INFORMATIONAL mode) → `APDisplayPanel.setPaletteAndBuildOccupiedHues(template.getDrawObjRoster().getCurrPalette())`.
- `APDisplayPanel.buildOccupiedHuesArray(PaletteWName)` iterates `palette.getColorPtOstdotArray()`, reads `color.getOstHCS().getStdCodeHueNum()` (0–35), converts to user-facing hue (+1), marks `occupiedHues[userFacingHueNum-1] = true`. Result: `boolean[36]`.
- Equivalent per-DrawObj form for the new code: iterate `template.getAllDrawObjs()`, collect `drawObj.getUserFacingHueNum()` (1–36, or 0 if fill unset) into a `Set<Integer>`.
- **Guard**: `distinctHues == 4` required. If the count is 3 or 5+, or any DrawObj has an unset fill color: **hard block with an error message; the frame refuses to proceed.**

### 3.3 AnchorHueSet construction — user assigns the four hues to the four AGs

- The four occupied hues are detected automatically, but the `AnchorHueSet` can only be created after the user designates which hue is h1, h2, h3, h4 — i.e., assigns each of the four hues to AG1–AG4. This assignment is the essential UI step and probably the first thing the user does after selecting the Template.
- **UI**: four rows, each showing the detected hue's color swatch + hueNum, each with an AG1–AG4 selector enforcing a **bijection** (each AG used exactly once). The user can change their mind / correct mistakes until satisfied.
- **Confirmation**: a button (or equivalent) for the user to signal "OK" — final decision on the hue→AG assignment. Only then is `AnchorHueSet` instantiated.
- **User-facing guidance text, verbatim** (no edits/additions; displayed in the UI, not enforced by code):
  > "Choose the hue for AG1 as central to the graphic design. Choose the hue for AG2 as the hue which is closest to being complementary to AG1. Choose the hue for AG3 (from the remaining 2 hues) as the hue which is closest to AG1 (analogous considering warm/cool ranges). The last remaining hue will be AG4, and is likely to be analogous to AG2."
- The same rules inform the AGprefs value assignments (see §3.5): AG1↔AG2 is the near-complement pair; AG3 is analogous to AG1; AG4 is analogous to AG2.

### 3.4 Automated `defaultAG` assignment

- Once the `AnchorHueSet` exists, `defaultAG` for each `ShapeColorPrefs` is trivial and foolproof: every DrawObj's hue is one of the four anchor hues, so `defaultAG` = the AG whose anchor hue equals `drawObj.getUserFacingHueNum()`. Exact match — no distance arithmetic, no ties possible (four distinct hues).

### 3.5 AGprefs value rule (v1 — constant 4×4 table)

- The four AGpref values represent an "affinity" — the likelihood/desirability that the drawObj will be assigned to each of the four AGs. The same four doubles are always used, in different order per `defaultAG`: **0.95, 0.65, 0.35, 0.05** (highest → lowest affinity). No duplicates, no ties — each row is a permutation of the four values.
- Constant lookup table (row = `defaultAG`, columns = pref for AG1–AG4), applies in **all** cases:

  | defaultAG | AG1  | AG2  | AG3  | AG4  |
  |-----------|------|------|------|------|
  | 1         | 0.95 | 0.05 | 0.65 | 0.35 |
  | 2         | 0.05 | 0.95 | 0.35 | 0.65 |
  | 3         | 0.65 | 0.05 | 0.95 | 0.35 |
  | 4         | 0.05 | 0.65 | 0.35 | 0.95 |

- Structure: own AG gets 0.95; the near-complement partner gets 0.05 (AG2 for defaults 1/3, AG1 for defaults 2/4); the analogous partner gets 0.65 (AG3↔AG1, AG4↔AG2); the remaining cross pair gets 0.35.
- For v1 this is literally a constant table — "closest distance" enters only in later refinement.

### 3.6 valueTarget

- `valueTarget` = the RGB luminance of the DrawObj's fill color: `DrawObj.getRGBLum3D()` → `ColorPtOstDot.getLum3D()`, range [0.0, 1.0].

### 3.7 Output artifact

- `RMatrixFactory` returns/produces an **`AgPrefsDocument`** (formatVersion 2 — UUID-keyed rows in Template draw order; `drawObjId` = `DrawObj.getUniqueId()`, `shapeName` = `DrawObj.getName()`, `shape` = 1-based draw position).
- Saved as a new defaults file in **`dev_storage/agprefs`** (`StorageConfig.getAgPrefsPath()`), via the existing write path `SmartPicFactory.writeAgPrefs`.
- The existing **Create SmartPic flow consumes it unchanged** — no modifications to `CreateSmartPicController`/`SmartPicFactory` needed.
- **File naming**: fully editable by the user in a **save dialog**. Suggested default = first 20 characters derived from the Template name. User must see the name and agree. Document `name`/`description` fields should record provenance (source Template + the four AG hue assignments).
- **v1 scope**: file is saved and user interaction ends there. Coupling to the variation run (the `hueSet` 0–24 index into the fixed `AnchorHueSets` table vs. the tailored anchor set) is a deferred topic.

### 3.8 Acceptance direction

- "First variation of the 12 palette-variations ≈ the source Template" is the **general direction**, not a strict acceptance test for v1. To be discussed in detail as needed.

## 4. New Classes / Components

| Element | Package | Role |
|---------|---------|------|
| `RMatrixFactory` | `com.csurf.variationmodule.controller` (per user's choice; keeps `model` free of the varyengine `AnchorHueSet` dependency) | Logic of automated RelationMatrix creation |
| `CreateRMatrixController` | `com.csurf.variationmodule.controller` | Orchestrates template selection, hue detection, AG assignment, factory call, save dialog — mirrors `CreateSmartPicController` (one controller per open frame) |
| `CreateRMatrixFrame` ("createRMatrixFrame") | `com.csurf.variationmodule.view` | The new UI window — mirrors `CreateSmartPicFrame` (Actions interface pattern) |
| Menu item "Create RMatrix from Template" | `ApplicFrame.setupMenus()` → `templateMenu` | Triggers `new CreateRMatrixController(appMediator).launch()` + `registerFrame(...)` |

### `RMatrixFactory` API (per decisions)

- `createAllShapeColorPrefs(Template, AnchorHueSet)` — loops `template.getAllDrawObjs()`, calls `createOneShapeColorPrefs` per DrawObj; assembles the `AgPrefsDocument` (v2 rows, draw order, `drawObjId` binding).
- `createOneShapeColorPrefs(DrawObj, AnchorHueSet)` — for one DrawObj:
  - `defaultAG` = index (1–4) of `anchorHueSet.anchorHueForAG(ag)` whose `getUserFacingHueNum()` equals `drawObj.getUserFacingHueNum()` (exact match guaranteed by the 4-hue guard).
  - `agPrefs` = the constant row from the 4×4 table (§3.5).
  - `valueTarget` = `drawObj.getRGBLum3D()`.

## 5. `createRMatrixFrame` UI Flow (decided)

1. **Select Template** — file chooser on `StorageConfig.getTemplatesPath()` (same mechanism as `CreateSmartPicController.chooseTemplate`); `TemplateStorage.loadTemplate`.
2. **Guard** — detect distinct occupied hues; if ≠ 4 (or unset fills), hard-block with error.
3. **Assign hues to AGs** — four rows (swatch + hueNum) each with an AG1–4 selector enforcing bijection; verbatim guidance text shown; user may revise until pressing **OK** to finalize the `AnchorHueSet`.
4. **Generate + Save** — `RMatrixFactory` builds the `AgPrefsDocument`; save dialog with suggested name (first 20 chars of Template name, user-editable); write to `dev_storage/agprefs` via `SmartPicFactory.writeAgPrefs`. Interaction ends.

## 6. Existing Code to Reuse

- `SmartPicFactory.AgPrefsDocument` / `ShapeRow` / `writeAgPrefs` — output format + write path.
- `SmartPicFactory.loadAgPrefs` / `RelationMatrix.save` — validation/round-trip precedent.
- `TemplateStorage`, `StorageConfig.getAgPrefsPath()/getTemplatesPath()` — file locations.
- `AnchorHueSet`, `HueNumGUI` — anchor-hue model.
- `DrawObj.getUserFacingHueNum()`, `getRGBLum3D()`, `getUniqueId()`, `getName()` — per-shape inputs.
- Occupied-hue pattern from `APDisplayPanel.buildOccupiedHuesArray`.
- `CreateSmartPicController`/`CreateSmartPicFrame` and `RelationMatrixEditorController` — controller/frame patterns to mirror (menu → controller.launch() → registerFrame).

## 7. Edge Cases / Validation

- Template with ≠ 4 distinct occupied hues → block.
- Any DrawObj with null/unset fill (`getUserFacingHueNum()` == 0, `getRGBLum3D()` == 0.0 sentinel) → block (part of the guard).
- Bijection enforcement in the assignment widget (each AG exactly once) — invalid intermediate states tolerated until OK; OK disabled/blocked while assignment incomplete.
- DrawObj with null name — `SmartDrawObj`/`ShapeRow` name handling (`name` must not be null in `SmartDrawObj`; check DrawObjs have names at load).

## 8. Deferred Topics (Future Development)

- **Spread arithmetic / "closest distance"** in the hue circle — will inform a refined AGprefs rule and possibly `defaultAG` for Templates with >4 hues. Classes: `HueCircleStepCalc` (`com.csurf.huepattern.util`), `SpreadPrelim` (`com.csurf.huepattern.model`), `HueNumGUI` (`com.csurf.ostwald.domainmonotri.model`). Logic may need cleanup/consolidation into one class.
- **Coupling to variation runs** — the tailored `AnchorHueSet` (arbitrary Template hues) vs. the fixed 25-row `AnchorHueSets` table indexed at run time by `hueSet` 0–24.
- **Strict acceptance test** — "var1 ≈ source Template" as verifiable criterion.

## 9. Implementation Phases (Outline)

- **Phase 1 — Domain logic**: `RMatrixFactory` (`createAllShapeColorPrefs` / `createOneShapeColorPrefs`, occupied-hue guard helper, constant 4×4 AGprefs table, v2 `AgPrefsDocument` assembly).
- **Phase 2 — Frame + controller**: `CreateRMatrixFrame` + `CreateRMatrixController` — Template chooser, hue detection display, bijection-enforcing AG-assignment widget with OK confirmation, verbatim guidance text, save dialog with suggested name.
- **Phase 3 — Menu wiring**: "Create RMatrix from Template" item in `ApplicFrame`'s Templates menu → controller launch + `registerFrame`.
- **Phase 4 — Validation/verification**: unit tests for factory logic (guard, AG assignment, prefs table, v2 round-trip via `SmartPicFactory.loadAgPrefs`/`createSmartPic`); manual end-to-end — generate file → Create SmartPic flow consumes it → 12 variations.
