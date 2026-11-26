# COMMIT_MESSAGE_version 0K (112626): User-selected frozen drawObj set — engine seam, run plumbing, and selection UI

The `userSelectFrozen` feature, complete in three phases: the user can now
pick which drawObjs (shapes) of a SmartPic are frozen during the
12-variations process, alongside the six pre-defined `FrozenSets` table
rows. The picked shapes appear as a seventh "user (N shapes)" entry in the
run-parameters combo; choosing it dispatches a clean variation cycle that
reinstates slot 1's colors on exactly those shapes. An empty set is legal
(freeze nothing) — a capability the predefined table cannot express — and
is guarded by a warning confirm.

## Flow of execution

1. **Selection (new UI):** `Controls-info → Select Frozen drawObjs`
   (enabled once a SmartPic exists) opens a scratch `TemplateWorkFrame` on
   a fresh `storage.loadTemplate` copy plus the modeless
   `UserSelectFrozenFrame`. The user clicks shapes in the scratch window
   and presses "Use current selection as the frozen set" once.
2. **Commit:** `UserSelectFrozenController.commit()` reads
   `roster.getSelectionGroup().getSelectedDrawObjsSet()`, maps each DrawObj
   **positionally** (`getAllDrawObjs().indexOf + 1` — the same 1-based
   shapeNum contract `SmartPicFactory` and `loadPaletteColoredTemplates`
   use), cross-checks DrawObj UUIDs against `SmartDrawObj.drawObjUuid`
   (log-only; pre-v2 files re-randomize UUIDs at load), validates ⊆
   `smartPic.smartDrawObjs().keySet()`, and calls
   `CreateSmartPicController.setUserFrozenShapes` (stored via `Set.copyOf`).
3. **Run:** `chooseRunParameters()` appends the seventh combo entry only
   when a user set exists; `createVariations()` dispatches to the new
   `runCleanCycle(String, Set<Integer>, Facts)` overload, which cooks the
   fixed 12-row `FrozenShapesCategory` recipe binding `FROZEN_SHAPES`
   instead of `FROZEN_SET`. The variation window is titled `Frzn:user`.
4. **Inspect:** the "Hue anchors/ frozen" info frame gets a user-set
   branch — its combo gains the same seventh entry and displays the user
   set's shapes sorted numerically by shape ID.

## New classes

- **`UserSelectFrozenFrame`** (view) — small modeless JFrame modeled on
  `HueAssignLikeAGFrame`. Displays: instructions ("select in the template
  window, then commit once"), the one-shot commit button, and a live
  status line "Selected: N of M shapes" refreshed when the frame regains
  focus. An empty selection triggers a warning confirm (freeze nothing is
  legal but possibly a mistake); a successful commit disposes the frame —
  the scratch work window stays open but is no longer consulted.
- **`UserSelectFrozenController`** (controller) — session state and commit
  logic for the selection: positional DrawObj→shapeNum mapping, UUID
  cross-check, SmartPic validation, and hand-off to
  `CreateSmartPicController.setUserFrozenShapes`. Returns error strings on
  rejection in the established `HueAssignLikeAGController` style.

## Modified classes

- **`varyengine/model` (Phase A — typed set seam):** `ParamKind` gains
  `SHAPE_SET`; `StratgParam` gains `FROZEN_SHAPES`; `StratgSpec` gains a
  `Set<Integer>` `bind` overload (stores `Set.copyOf` — keeps
  `copyForCooking`'s shallow copy safe) and `getIntSetParam`;
  `StratgType.FREEZE_SHAPES` declares both freeze params optional, with
  exactly-one-of (XOR) enforced in `FreezeShapesStratg.execute()` and by
  construction in `FrozenShapesCategory` (new set-cook overloads; the
  `BroadCategorySpec` path extracts whichever is bound).
- **`VariationPaletteExtractor`** — `runCleanCycle` overload taking the
  explicit frozen-shape set.
- **`CreateSmartPicController`** — session-only `userFrozenShapes` field
  (cleared when a new Template is chosen or SmartPic rebuilt — shape IDs
  are SmartPic-relative), immutable `setUserFrozenShapes`/
  `getUserFrozenShapes` seam, `RunParams` carrying the set, user-set
  dispatch/`Frzn:user` title, seventh combo entry, and the implemented
  `showUserSelectFrozenFrame()` (was a stub).
- **`CreateSmartPicFrame`** — `setUserSelectFrozenEnabled`; the menu item
  is enabled only after `createSmartPic()` succeeds.
- **`SmartPicAGCtrsFrozenSetFrame`** — 4-arg constructor taking the user
  set; `JComboBox<Object>` with a conditional seventh entry; user-set
  display branch sorting IDs numerically.

## New files

- **`src/main/java/.../UserSelectFrozenFrame.java`**,
  **`UserSelectFrozenController.java`** — above.
- **`112626_v0K_SWE-2_User-selected_Frozen_drawObj_set.md`** — design doc
  (decisions, XOR analysis, three-phase plan, deferred items).
- **`dev_storage/`** — new authored data: aghuepatterns
  (`07-26-10-17-slamDunk112526orngTealMgnta`, `08-27-11-18-TRIADIC-orngTeal-v1`),
  agprefs (`slamDunk112526orngTealMgnta-Rmtrx-v1/-v2`), palettes
  (`slamDunk112526bluMgnta-4hues-v1`, `slamDunk112526orngTealMgnta-4hues-v1`,
  `slamDunk112626bluMgntaRed-4hues-v1`, `slamDunk112626niceBluesRed-4hues-v1`),
  templates (`slamDunk112526bluMgnta-4hues-v1groups`,
  `slamDunk112526orngTealMgnta-4hues-v1groups`,
  `slamDunk112626bluMgntaRed-4hues-v1groups`,
  `slamDunk112626niceBluesRed-4hues-v1groups`).

## Tests

104 tests green (0 failures). Phase A adds 16 targeted tests across
`StratgTest`, `CookTest`, and `SmartPicCycleTest` covering set binding /
immutability, XOR violations, explicit-set freezing, out-of-range IDs, the
empty set, and a real-template user-set cycle. Phases B/C are Swing
plumbing verified by compile + suite; exercise manually via the flow
above.

## Notes

- Deferred (design doc §7): named frozen-set catalog, incremental commit
  UI (add/remove/seed-from-set), warn-on-stale lifecycle instead of
  clear-on-rebuild, and `SHAPE_SET` serialization if specs are persisted.
