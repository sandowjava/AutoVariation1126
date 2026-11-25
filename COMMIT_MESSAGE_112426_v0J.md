# COMMIT_MESSAGE_version 0J (112426): AGHuePattern JSON persistence and the RMatrix save-tailored-set hook (Phases 2–3)

Implements Phase 2 and Phase 3 of the AGHuePattern refactoring described
in `112426_v0J_SWE-2_AGHuePattern_Refactoring_Phase2-3.md`: user-authored
named patterns persist as JSON files in `dev_storage/aghuepatterns/`,
fold into the catalog at launch, and can be created either freehand from
the SmartPic frame or — the phase's reason for existing — directly from
a confirmed hue→AG assignment in the Create-RMatrix window, so a
tailored anchor set is reusable by name in the variation run dialog
without re-entering four hues.

## New functionality

- **JSON persistence for user patterns.** `AGHuePatterns` now keeps two
  maps: the immutable built-in `TABLE` (the 23 legacy-derived rows) and a
  `USER_TABLE` for loaded/saved entries, merged at access time. Every
  public accessor (`get`/`exists`/`all`/`allNames`/`count`) lazily folds
  `dev_storage/aghuepatterns/*.json` into `USER_TABLE` on first call —
  same-session saves are visible immediately, and files land in pickers
  at next launch with no caller changes.
- **Decided collision policies.** On load, an existing entry always
  wins: a file whose canonical key collides with a built-in or an
  earlier-loaded file is skipped with a `LOG.warning` naming the file —
  built-ins can never be shadowed. On save, a same-key user entry is
  overwritten only after the usual overwrite-confirm; overwriting a
  built-in name is allowed but the UI warns it applies only to the
  current session.
- **Per-file validation with warn-and-skip.** Each file must carry a
  supported `formatVersion` (absent = 1), a canonical `name`, exactly
  four `anchorHues` in 1..36, and a `NN-NN-NN-NN` name prefix matching
  the hues — a drifted, malformed, or truncated file is skipped with a
  warning without blocking the rest of the directory.
- **"Save new AGHuePattern..." in CreateSmartPicFrame.** A new
  `Patterns` menu offers freehand pattern authoring at any time.
- **"Save anchor hues as AGHuePattern..." in CreateRMatrixFrame.** A new
  `Patterns` menu item — enabled once the hue→AG assignment is
  confirmed, disabled again on any assignment change or new Template —
  opens the same dialog pre-filled with the confirmed `AnchorHueSet`
  and a suggested suffix derived from the Template name
  (`<name>+Tailored`, e.g. `heroEye112326TanMauvTailored`), recording
  which RMatrix the pattern was created for.

## New classes

- **`AGHuePatternDoc`** (`varyengine.model`) — the flat Jackson DTO for
  the v1 file format: `name`, `description`, `formatVersion` (absent
  defaults to 1 via `formatVersion()`), and `anchorHues[4]` in AG order.
  Javadoc reserves a `levels` field for formatVersion 2, where the four
  pattern-level doubles will live once `anchorPattern` analysis lands.
- **`AGHuePatternSaveDialog`** (`view`) — the modal "pick 4 hues" dialog:
  four AG1–AG4 `JSpinner`s (1–36, optionally pre-filled from an
  `AnchorHueSet`), a Suffix field appended to the canonical hue prefix,
  a Description field, and a bold live preview label showing the exact
  canonical name (`NN-NN-NN-NN-<suffix>`) that will key the catalog —
  updating on every spinner or keystroke. `showDialog` returns a
  `SaveResult(pattern, description)` record; `saveWithConfirm` hosts
  the shared save-side policy (overwrite-confirm + built-in session-only
  note) so both controllers behave identically.

## Modified classes

- **`AGHuePatterns`** — `USER_TABLE` + `userLoaded` flag;
  `ensureUserLoaded`/`loadUserPatterns`/`loadFrom` (sorted file order,
  per-file log-and-continue); `saveUserPattern`/`saveTo` (write then
  register — the `@TempDir` test seam); `isBuiltIn`;
  `clearUserPatternsForTests`; merged-view accessors.
- **`AGHuePattern`** — `hueNums()` (`int[4]` in AG order) for
  serialization.
- **`StorageConfig`** — `getAGHuePatternsPath()` →
  `dev_storage/aghuepatterns`.
- **`CreateSmartPicFrame`** — `Actions.saveAGHuePattern()` + the
  `Patterns` menu.
- **`CreateSmartPicController`** — `saveAGHuePattern()`: dialog →
  `saveWithConfirm` → status line.
- **`CreateRMatrixFrame`** — `Actions.saveAnchorHuesAsPattern()` +
  `Patterns` menu + `setSavePatternEnabled`.
- **`CreateRMatrixController`** — `saveAnchorHuesAsPattern()` +
  `suggestedPatternSuffix()`; menu enablement toggled at the three
  `anchorHueSet` lifecycle points (`chooseTemplate`, `confirmAnchorHues`,
  `assignmentChanged`).

## Flow of execution

- **Load:** first call to any catalog accessor → `ensureUserLoaded` →
  `loadFrom(dev_storage/aghuepatterns)` → for each `*.json` in sorted
  order: Jackson read → `toPattern` validation → key check
  (`TABLE`/`USER_TABLE`) → register or loud-skip.
- **Save (SmartPic frame):** `Patterns ▸ Save new AGHuePattern...` →
  `CreateSmartPicFrame.Actions.saveAGHuePattern` → controller →
  `AGHuePatternSaveDialog.showDialog(frame, null, null)` → user picks
  hues + suffix → `saveWithConfirm` → `AGHuePatterns.saveUserPattern` →
  pretty-printed `<name>.json` + `USER_TABLE` entry → status line shows
  catalog count.
- **Save (RMatrix frame):** Template → assign hues → `Confirm` →
  `Patterns ▸ Save anchor hues as AGHuePattern...` →
  `saveAnchorHuesAsPattern` → `showDialog(frame, anchorHueSet,
  suggestedPatternSuffix())` → same `saveWithConfirm` path — the
  tailored set is now pickable by name in the run dialog and the
  SmartPicAGCtrsFrozenSet diagnostic frame.

## Tests

- `AGHuePatternsPersistenceTest` (8 new): `saveTo`→`loadFrom`
  round-trip, built-in collision skip, sorted-first-wins on same-key
  files, prefix↔hues mismatch rejection, wrong-hue-count/unsupported-
  version rejection, malformed-file tolerance, missing-dir no-op, and
  save-side overwrite.
- `AGHuePatternsTest`: `@BeforeEach` → `clearUserPatternsForTests` so
  the 23-count assertions stay deterministic with a mutable catalog.

`mvn test`: **88 tests, 0 failures, 0 errors.**

## Notes

- The four pattern-level members are still deferred: `AGHuePatternDoc`
  reserves `levels` for formatVersion 2 rather than writing placeholders.
- Two real user patterns authored through the new flow are staged in
  `dev_storage/aghuepatterns/` (`08-13-10-11-heroEye112326TanMauvTailored`,
  `10-22-16-20-ehWhat112326orngPurp-v1`) along with their source AGprefs
  files — dogfooding evidence of the end-to-end Phase 3 path.
- Deferred: nothing from the plan — Phases 2 and 3 are complete.
