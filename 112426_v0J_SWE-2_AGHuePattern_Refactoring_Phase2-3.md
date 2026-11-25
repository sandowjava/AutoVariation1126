# 112426 v0J SWE-2 — AGHuePattern Refactoring, Phases 2–3

Design and implementation plan for persisting user-defined AG hue patterns
(Phase 2) and integrating the save flow into the RMatrix feature (Phase 3).
Phase 1 (named-catalog schema cut) shipped in commit `d367e11`
(`COMMIT_MESSAGE_version 0H-prelim1 (112426)`).

## 1. Goals and rationale

### Goals

- **Replace opaque indices with self-describing names.** The deleted
  `AnchorHueSets` table addressed patterns as `int` 0–24 with no way to see
  what a row meant. Canonical names (`NN-NN-NN-NN[-suffix]`) encode the four
  AG anchor hues in the name itself and sort correctly in pickers.
- **Let users author and keep patterns.** Phase 2 adds JSON persistence so
  tailored hue sets created for specific RMatrix work survive restarts.
- **Keep the spec layer light.** `StratgSpec` binds the pattern *name*
  (String, `NAMED_KEY`), not the live object — recipe rows stay plain data.

### Rationale for the significant decisions

- **Name = key, object resolved at execution.** `AssignAnchorHuesStratg`
  resolves the bound name through the catalog per execution. This mirrors
  the old `TABLE_KEY` indirection but with meaningful keys, keeps
  `StratgSpec` serializable, and lets the strategy stay a shared stateless
  singleton.
- **Prefix carries the data.** `NN-NN-NN-NN` is zero-padded AG-order hues,
  so lexicographic order == numeric order and a pattern's content is
  readable from any picker, title, or spec dump. Suffix is free-form
  descriptive text (typically identifying the RMatrix/Template the pattern
  was tailored for — the user's stated naming scenario).
- **Composition over subclassing.** `AGHuePattern` wraps the sealed
  `AnchorHueSet` rather than extending it; naming/catalog behavior is added
  without opening the value type.
- **Case-insensitive canonical keys.** `TreeMap` on lowercased canonical
  keys gives sorted iteration and forgiving lookup while preserving
  original casing for display.
- **Existing wins on load.** A user file can never shadow a built-in or an
  already-loaded entry — the catalog's trust order is built-ins > first
  user file on disk > later user files, with every skip loud in the log.
- **One file per pattern.** Mirrors the agprefs convention (individual JSON
  docs in a directory), keeps diffs readable, and lets users drop files in
  or delete them directly.
- **`NAMED_KEY` distinct from `TABLE_KEY`.** `FROZEN_SET` legitimately stays
  an integer table index; only the hue pattern needed name semantics.

## 2. Phase 1 recap — what shipped

Hard schema cut, no persistence:

- **New:** `AGHuePattern` (name + composed `AnchorHueSet`, name-based
  equality), `AGHuePatterns` (23-entry static catalog: legacy rows 0–18,
  20–22, 24; duplicate rows 19≡0 and 23≡1 dropped; canonical naming,
  `canonicalize`/`canonicalKey`, case-insensitive `get`, `allNames`).
- **Schema:** `ParamKind.NAMED_KEY`; `StratgParam.AGHUE_PATTERN`
  (`"agHuePattern"`, String); `StratgSpec.bind(param,String)` +
  `getStringParam`; `StratgType.ASSIGN_ANCHOR_HUES` requires
  `AGHUE_PATTERN` + `CONTROL_LEVEL`.
- **Flow:** `CreateSmartPicController` (name picker, default
  `13-10-06-08-legacyTableRowNum18`) → `VariationPaletteExtractor
  .runCleanCycle(String,int,Facts)` → `FrozenShapesCategory.cook` (fail-fast
  validation, name bound into all 12 recipe rows) → `VariationDriver` →
  `ColorChangeEngine` → `AssignAnchorHuesStratg` (catalog resolve →
  `anchorHueForAG` → `state.setHue`).
- **Display:** `SmartPicAGCtrsFrozenSetFrame` — name combo from
  `allNames()`, `agHuePattern <name>` border title, `AGn → hue` panel.
- **Deleted:** `AnchorHueSets`. **Tests:** 80 green.

## 3. Phase 2 — persistence

### 3.1 Files

| File | Change |
|---|---|
| `templatecore/storage/config/StorageConfig.java` | M — add `AGHUE_PATTERNS_DIR = "aghuepatterns"` + `getAGHuePatternsPath()` |
| `varyengine/model/AGHuePatternDoc.java` | A — Jackson DTO, `AgPrefsDocument` style |
| `varyengine/model/AGHuePattern.java` | M — add `hueNums()` (`int[4]`, AG order) |
| `varyengine/model/AGHuePatterns.java` | M — `saveUserPattern`, lazy `loadUserPatterns`, `loadFrom(Path)` test seam, `isBuiltIn` |
| `view/AGHuePatternSaveDialog.java` | A — pick-4-hues + suffix + preview dialog |
| `view/CreateSmartPicFrame.java` | M — "Patterns" menu + `Actions.saveAGHuePattern()` |
| `controller/CreateSmartPicController.java` | M — `saveAGHuePattern()` + overwrite-confirm |
| `test/.../AGHuePatternsPersistenceTest.java` | A — round-trip + collision + malformed-file tests (`@TempDir`) |

### 3.2 DTO — `AGHuePatternDoc`

Public-field Jackson DTO mirroring `AgPrefsDocument` conventions:

```java
public static final int FORMAT_VERSION = 1;

public String name;             // canonical catalog key
public String description;      // provenance (e.g. "saved from pick-4-hues")
public Integer formatVersion;   // absent = 1
public int[] anchorHues;        // AG1..AG4 order, each 1..36

public int formatVersion() { return formatVersion != null ? formatVersion : FORMAT_VERSION; }
```

On-disk shape:

```json
{ "formatVersion": 1,
  "name": "13-10-06-08-heroEyeTailored",
  "description": "Saved from CreateSmartPic pick-4-hues dialog",
  "anchorHues": [13, 10, 6, 8] }
```

File name: `{canonicalName}.json` in `dev_storage/aghuepatterns/` — the
auto-derived `NN-NN-NN-NN` prefix plus the user's optional suffix phrase.
Canonical-key comparison governs collisions, not file names.

### 3.3 `AGHuePatterns` additions

```java
public static synchronized void saveUserPattern(AGHuePattern pattern);  // TABLE.put + write JSON
public static synchronized int loadUserPatterns();                      // sorted *.json, returns count
static int loadFrom(Path dir);                                          // test seam (also used by the above)
public static boolean isBuiltIn(String name);                           // key was present after static init
private static void ensureUserLoaded();                                 // lazy once-per-JVM flag
```

`ensureUserLoaded()` is invoked at the top of `get`, `exists`, `all`,
`allNames`, and `count` — first access in a session transparently folds
user files into the catalog. Snapshot built-in keys at the end of the
static init block for `isBuiltIn`.

### 3.4 Collision policy — DECIDED

- **Load:** *existing entry wins.* Files processed in sorted name order;
  when a file's `canonicalKey` is already in `TABLE` (built-in or earlier
  file) it is skipped with `LOG.warning` naming the file and the key.
  Built-ins and existing entries can never be shadowed.
- **Save:** name already in catalog → overwrite-confirm dialog in the
  calling UI before `saveUserPattern` runs; save then overwrites both the
  in-memory entry and the file.
- **Edge case:** overwriting a *built-in* name is permitted in-session
  (per the save rule), but on the next launch that file collides with the
  built-in under the load rule and is skipped + warned — the shadowing is
  session-only. The dialog shows an extra line when the name is a
  built-in so the user sees this consequence up front.

### 3.5 Load validation (per file, warn-and-skip on any failure)

1. `formatVersion` supported (== 1).
2. `name` canonicalizes, non-blank.
3. `anchorHues` non-null, length 4, each 1–36.
4. **Integrity:** `canonicalPrefix(anchorHues)` must equal the name's
   `NN-NN-NN-NN` prefix — catches hand-edited file drift.
5. Collision per §3.4.

Malformed JSON or IO errors: `LOG.warning(file)` + continue — one bad file
never blocks the picker.

### 3.6 Save UI — `AGHuePatternSaveDialog`

Static factory `showDialog(Component owner, AnchorHueSet prefill, String
suggestedSuffix)` returning the built `AGHuePattern` or null on cancel:

- Four `JSpinner`s, `SpinnerNumberModel(v, 1, 36, 1)`, labeled AG1–AG4.
- `JTextField` suffix (free text; normalized by `canonicalize`).
- Live `JLabel` preview of the canonical name `NN-NN-NN-NN[-suffix]`,
  updated from spinner/field listeners.
- `JTextField` description (optional → `AGHuePatternDoc.description`).

`CreateSmartPicFrame` gets a "Patterns" menu with "Save new
AGHuePattern…" → `CreateSmartPicController.saveAGHuePattern()`:

```
dialog(null prefill) → pattern
  → AGHuePatterns.exists(name)? → overwrite-confirm (built-in note if so)
  → AGHuePatterns.saveUserPattern(pattern)
  → frame.setStatus("Saved AGHuePattern '<name>'")
```

### 3.7 Tests

`AGHuePatternsPersistenceTest` (`@TempDir` + `loadFrom(Path)`):

- Round-trip: save → `loadFrom` → `get` returns equal pattern.
- Collision: file whose name matches a built-in → skipped, `get` still
  returns the built-in.
- Two files, same canonical key → first sorted wins.
- Prefix mismatch (`name` prefix ≠ `anchorHues`) → skipped.
- Malformed JSON + valid file in same dir → valid still loads.
- Overwrite: `saveUserPattern` on existing key replaces entry.
- **Note:** static `TABLE` is JVM-shared — tests must register uniquely
  suffixed names (e.g. `zzTest…`) to avoid cross-test pollution.

## 4. Phase 3 — integration

### 4.1 Files

| File | Change |
|---|---|
| `view/CreateRMatrixFrame.java` | M — `Actions.saveAnchorHuesAsPattern()`, "Patterns" menu item, `setSavePatternEnabled` |
| `controller/CreateRMatrixController.java` | M — `saveAnchorHuesAsPattern()` |

### 4.2 "Save tailored set" hook

`CreateRMatrixController` already holds the confirmed `anchorHueSet`
(invalidated by `assignmentChanged`). New action:

```
saveAnchorHuesAsPattern()
  → guard: anchorHueSet != null else "Confirm the hue→AG assignment first."
  → AGHuePatternSaveDialog.showDialog(frame, anchorHueSet,
        suggestedSuffix from template name, e.g. "heroEyeTailored")
  → exists? → overwrite-confirm (built-in note per §3.4)
  → AGHuePatterns.saveUserPattern → status + LOG
```

The saved pattern then appears in `CreateSmartPicController`'s
run-parameter picker and `SmartPicAGCtrsFrozenSetFrame` on next use —
no other wiring needed, which is the point of the catalog design.

### 4.3 Deferred — pattern-level doubles

Four `double` level members on `AGHuePattern` (the
analogous/complementary/triadic/quadratic relationship levels hinted in
its javadoc) land only when `anchorPattern` analysis integrates
(`huepattern` package, `AnchorPatternPrelim`,
`HueAssignLikeAGController.anchorPatternLevel`). `AGHuePatternDoc`'s
`formatVersion` reserves this: v2 adds an optional `levels` field; v1
files keep loading via the absent-field default. Phase 3 adds only a
javadoc note — no fields yet.

## 5. Out of scope (both phases)

- Pattern editing/deletion UI (files are user-manageable directly).
- `FrozenSets` persistence (separate concern; still int-indexed).
- Migrating saved recipes/specs from `hueSet` ints (none exist on disk).
