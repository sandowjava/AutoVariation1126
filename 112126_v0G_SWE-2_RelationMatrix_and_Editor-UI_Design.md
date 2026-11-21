# RelationMatrix and Editor-UI Design (v0G, SWE-2) — 11/21/26

## Purpose

Introduce `RelationMatrix` as a named concept in the variationmodule domain model:
the totality of `ShapeColorPrefs` instances in a `SmartPic` — the graphic
intelligence of a SmartPic instance. Each `ShapeColorPrefs` is the graphic
intelligence of a single `SmartDrawObj`. "Matrix" is used in the general sense
of a structure or mold that shapes something — not any specific implementation;
the class may expand in that direction in future development.

Also specified: `RelationMatrixEditorFrame`, a UI window for editing relation
matrices and saving them as data files in the `authored-defaults-v1` family.

Note: `com.csurf.artintellig.model.RelationMatrixPrelim` and
`com.csurf.huepattern.model.RMatrixMiniPrelim` are renamed legacy classes from a
dormant architecture. They are design precedent only (notably RMatrixMiniPrelim's
UUID-keyed maps) and are not part of this work.

## Decisions Log

- **Ownership**: each `SmartPic` contains/exposes a `RelationMatrix`. Option B:
  `RelationMatrix` is a *façade* over the existing `SmartDrawObj` set — no
  restructuring of `SmartDrawObj`/`ShapeColorPrefs`. May be promoted to a
  contained structure later without touching callers.
- **Keying (v2 files)**: rows bind to Template DrawObjs by **DrawObj UUID**
  (`"drawObjId"`). `shapeName` remains a human-readable column. Verified:
  `DrawObjSerializer`/`Deserializer` persist `uniqueId`; 289/366 saved
  templates already carry UUIDs.
- **Row order**: serialization/display convention only — rows are written and
  shown in Template draw order for readability and diffs. The loader never
  relies on order; binding is by UUID.
- **Re-binding (not reorder)**: applying a matrix to a foreign Template is a
  per-row *re-bind* of `drawObjId`, not a reorder. Editor offers a per-row
  "bind to" dropdown plus a bulk "Bind by Position" action. The controller
  owns an explicit binding map (prefs row → DrawObj).
- **Orphan/unbound rows**: `SmartPicFactory` hard-fails on unresolvable rows
  (SmartPic path). The editor is allowed to load a foreign file with all rows
  unbound and re-bind interactively.
- **Schema evolution**: `formatVersion` field; absent = 1. v1 rows positional,
  v2 rows UUID-keyed. Future `ShapeColorPrefs` members (e.g. `spreadLocation`,
  location within the AGSpread hue range) deserialize with documented defaults
  (`0.0` = at anchor hue) rather than failing; `save` always writes the
  current version.
- **Validation**: `SmartPicFactory` remains the single validation gate; the
  editor loads through it and inherits validation for free.
- **Save semantics**: "Save" writes back to source path; "Save As" prompts for
  a new `dev_storage/agprefs/*.json` with editable `name`/`description`.
  Round-trip is `RelationMatrix.toDocument()` → Jackson.
- **Naming convention**: `<purpose-or-style>-<template-hint>-rmat<N>.json`,
  e.g. `warm-portrait-face-rmat2.json`. Doc `name` field remains the
  authoritative display name.
- **Templates without UUIDs** (~77 legacy files): editor loads template but
  all rows appear unbound; user uses "Bind by Position" then "Save As" v2.
  Re-saving the template once also stamps UUIDs.

## v2 File Schema

```json
{
  "name": "defaults-face-rmat2",
  "description": "...",
  "formatVersion": 2,
  "shapes": [
    {
      "shape": 1,
      "shapeName": "noseBottom",
      "drawObjId": "5abdc424-959d-4899-914b-6143bb55ccfc",
      "valueTarget": 0.71,
      "defaultAG": 3,
      "agPrefs": [0.55, 0.60, 0.90, 0.20]
    }
  ]
}
```

- `shape` is an informational display hint in v2 (no sequential-order contract).
- `drawObjId` required in v2; must resolve to a DrawObj of the chosen Template.
- Completeness rule (SmartPic path): every DrawObj must have exactly one row.

## Class Responsibilities

- **`SmartPicFactory`** — parses any version; dispatches v1=positional /
  v2=UUID binding; single validation point; builds `SmartPic`.
- **`SmartDrawObj`** — gains `drawObjUuid` (always populated in both paths:
  v1 binds positionally then reads the DrawObj's UUID).
- **`RelationMatrix`** (new, `variationmodule.model`) — immutable façade:
  `prefsFor(SmartDrawObj|UUID)`, `shapes()` in draw order,
  `defaultAgMembers(int)`, `toDocument(name, description)`, `save(Path)`.
- **`RelationMatrixEditorFrame` + controller** (new, `variationmodule.view`/
  `controller`) — file-open via factory (inherited validation), table editor,
  bind dropdown, Bind-by-Position, Save/Save-As, dirty/status tracking.

## Editor Appearance

Window "Relation Matrix Editor". Menu bar (File: Open Template…, Open Matrix
File…, Save, Save As…, Close). Header strip: template name, file name,
formatVersion, row count, binding status ("all rows bound" / "N rows unbound").
Center: scrollable table, one row per shape — columns `#`, `Shape` (read-only),
`Value Target`, `Default AG` (dropdown 1–4), `Pref AG1..4`. Later: a per-row
bind-target dropdown for unbound rows. Bottom: status bar with dirty flag and
validation messages. Phase 4 adds a 21×4 shaded preference grid where cell
intensity encodes weight and the defaultAG cell is outlined.

## Phased Implementation

1. `formatVersion` + v2 UUID schema in `SmartPicFactory`; `SmartDrawObj`
   gains `drawObjUuid`. (v1 path unchanged behavior.)
2. `RelationMatrix` façade (read views + `toDocument`/`save`).
3. `RelationMatrixEditorFrame` + controller.
4. Matrix visualization panel.

## Open Items / Future

- `spreadLocation` and other new `ShapeColorPrefs` members → schema v3+;
  defaults on deserialize.
- Per-template-type "family" metadata beyond filename hints.
- Possible promotion of façade to owned structure (option A).
- Editor launch point (ApplicFrame menu vs. CreateSmartPicFrame menu).
