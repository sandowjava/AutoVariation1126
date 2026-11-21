# COMMIT_MESSAGE_version 0G (112126): RelationMatrix model + v2 UUID-keyed schema + Relation Matrix Editor UI

## Summary

Introduces the `relationMatrix` concept for SmartPic: the totality of a
SmartPic's `ShapeColorPrefs` — the graphic intelligence of the whole
SmartPic, where each `ShapeColorPrefs` is the intelligence of one
`SmartDrawObj`. The AGprefs defaults file gains a `formatVersion` 2
schema that binds each row to a Template DrawObj by `uniqueId` (UUID)
instead of array position, while v1 positional files keep working
unchanged. A new Relation Matrix Editor (menu + JFrame) lets the user
open a Template plus a defaults file, edit the prefs table, re-bind
rows to DrawObjs — including foreign-template files whose rows load
unbound — and save a v2 UUID-keyed file. Design rationale is recorded
in `112126_v0G_SWE-2_RelationMatrix_and_Editor-UI_Design.md`.

## New classes

- **`RelationMatrix`** (`variationmodule.model`) — immutable façade over
  a SmartPic's `SmartDrawObj` set; the prefs stay contained in their
  SmartDrawObjs (no copying). Row view: `shapes()`, `shape(UUID)`,
  `prefsFor(UUID)`, `isBound`. Column view: `defaultAgMembers(ag)` —
  the per-AG cohorts. `toDocument(name, description)` emits a
  formatVersion-2 `AgPrefsDocument` (rows in draw order, keyed by
  `drawObjId`); `save(file, name, description)` delegates to the
  factory's single write path.
- **`RelationMatrixDraft`** (`variationmodule.model`) — the mutable
  working copy behind the editor: a list of editable `DraftRow`s
  (valueTarget / defaultAG / agPrefs write-through setters) plus an
  explicit row→DrawObj-UUID binding map. `bindToTemplate` resolves
  UUIDs first, then falls back to positional binding for v1 files;
  unresolved rows stay unbound (foreign-template workflow).
  `bindRowTo` is exclusive — claiming a DrawObj unbinds the row that
  held it (move semantics). `bindByPosition` is the bulk starting
  guess. `displayRows()` orders bound rows by Template draw position,
  unbound rows last. `isFullyBound()` gates `toDocument()`.
- **`RelationMatrixEditorFrame`** (`variationmodule.view`) — the
  display class: a `JFrame` whose center is a `JTable` over the draft.
  Columns: `#` (the row's bound draw-order position, `–` when
  unbound), `Shape` (the Template DrawObj's name; the file's
  parenthesized `shapeName` for unbound rows), `Bind To` (a combo of
  the Template's DrawObjs — name + 8-char UUID prefix — the per-row
  re-bind mechanism), `Value Target`, `Default AG` (combo 1..4), and
  `AG1..AG4 Pref` weight cells. Menus: File (Choose saved Template,
  Open matrix file, Save, Save As, Close) and Binding (Bind by
  Position). Header shows Template name, file name (+ dirty marker),
  and binding status; a status bar reports validation feedback.
  Dumb view — every action delegates to the controller's `Actions`
  interface.
- **`RelationMatrixEditorController`** (`variationmodule.controller`) —
  orchestrates the editor: template/file JFileChoosers, draft
  lifecycle, dirty flag, `cellEdited` (numeric parse + 0..1 range
  validation), `rowBoundTo`, Save (to source path) / Save As (chooser
  in `dev_storage/agprefs` + name/description dialog). Launched from
  both `ApplicFrame` → Templates and `TemplateWorkFrame` →
  Color-sources menus; one controller per frame (multi-window).
- **`RelationMatrixRoundTripTest`** (`test`) — loads the v1 file,
  binds a draft to the real `heroEye…v1wAGs` Template, writes a v2
  file, reloads via the factory's UUID path, and asserts the v2
  SmartPic carries identical prefs to the v1 SmartPic; also exercises
  the façade's row/column views and `toDocument`.

## Modified classes

- **`SmartDrawObj`** — gains `drawObjUuid`: the identity of the
  Template DrawObj it wraps, and the v2 binding key. Always populated
  (DrawObjs without stored UUIDs get fresh ones at Template load).
- **`SmartPicFactory`** — `AgPrefsDocument` gains `formatVersion`
  (absent = 1; `FORMAT_POSITIONAL`/`FORMAT_UUID` constants) and
  `ShapeRow` gains `drawObjId`. `createSmartPic` now dispatches: the
  v1 path keeps the positional contract + count check; the new v2 path
  binds each row by `drawObjId` to a DrawObj `uniqueId`, requires the
  row and DrawObj key sets to be identical (hard-fail on
  orphan/uncovered), and emits SmartDrawObjs in draw order regardless
  of file order. `validate` is version-aware (v1: sequential `shape`
  numbers; v2: required/parseable/unique `drawObjId`s; both: defaultAG
  1..4, four agPrefs). New `writeAgPrefs(path, doc)` is the single
  write path for `RelationMatrix.save` and the editor.
- **`ApplicFrame`, `TemplateWorkFrame`** — "Relation Matrix Editor"
  menu items wired alongside "Create SmartPic" (registered as an open
  window from ApplicFrame).
- **Dormant renames (user)**: `artintellig`'s `RelationMatrix` →
  `RelationMatrixPrelim` and `huepattern`'s `RMatrixMini` →
  `RMatrixMiniPrelim` — historical-context classes renamed to free the
  names; their reference sites were updated accordingly.
- **New data**: `dev_storage/agprefs/stella-defaults-v1.json`, three
  `stella112126*` templates in `dev_storage/templates`, and three
  `stella112126*` palettes in `dev_storage/palettes`.

## Execution flow

```
Factory (Create SmartPic path, unchanged entry point):
  loadAgPrefs(path) → Jackson → AgPrefsDocument → validate(version)
  formatVersion 1 → rows pair positionally with getAllDrawObjs()
  formatVersion 2 → key set of row drawObjIds must equal key set of
                    DrawObj uniqueIds → emit in Template draw order
  → List<SmartDrawObj> → SmartPic → RelationMatrix façade on demand

Editor (ApplicFrame Templates / TemplateWorkFrame Color-sources):
  Choose saved Template → TemplateStorage.loadTemplate →
      draft.bindToTemplate (if a file is open)
  Open matrix file → factory.loadAgPrefs (inherited validation) →
      RelationMatrixDraft.fromDocument → bindToTemplate:
        pass 1 resolve drawObjId UUIDs, pass 2 v1 positional fill;
        unresolved rows stay unbound → table shows them last,
        "(shapeName)" in Shape column, Bind To combo for re-binding
  Edit cells → cellEdited → DraftRow setters (0..1 range-checked)
  Bind To change → rowBoundTo → bindRowTo (move semantics) →
      row moves to its new DrawObj's draw-order position
  Bind by Position → unbind all, row N → drawObj N (v1-style guess)
  Save → isFullyBound gate → draft.toDocument() (v2, draw order) →
      SmartPicFactory.writeAgPrefs → source path
  Save As → chooser in dev_storage/agprefs + name/description dialog
      → same write
```

## Design notes

- Binding is by DrawObj **identity** (UUID), not row order: file row
  order is a display/serialization convention only (draw order).
  Re-binding a row — not moving it — is what changes where its prefs
  apply.
- The factory hard-fails on unresolved bindings (SmartPic must be
  complete); the editor tolerates unbound rows so foreign-template
  files can be loaded, inspected, and re-bound before saving.
- `RelationMatrix` is Template-free by design; the draft (editor
  working copy) is the only model type that knows the Template —
  via `bindToTemplate`.
- `formatVersion` governs migration: v1 files never need rewriting,
  and future fields (e.g. `spreadLocation`) will default when absent.
