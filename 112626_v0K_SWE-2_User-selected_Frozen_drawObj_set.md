# User-selected Frozen drawObj set — design & implementation plan

**Document:** `112626_v0K_SWE-2_User-selected_Frozen_drawObj_set`
**Date:** 11/26/26
**Status:** Feature complete — Phase A (engine seam) + Phase B (controller
plumbing) + Phase C (selection UI) implemented and verified (104 tests green,
16 new tests covering all 17 planned cases — item 12 is regression-covered by
an existing test). Phases B and C are Swing/controller code verified by
compile + the existing suite; exercise them manually: create a SmartPic →
"Select Frozen drawObjs" → pick shapes in the scratch window → commit →
"Create 12 Variations" → the seventh "user (N shapes)" combo entry.
**Scope:** the `userSelectFrozen` feature — let the user choose which drawObjs
(shapes) of a SmartPic are frozen during the 12-variations process, alongside
the six pre-defined frozen sets.

---

## 1. Feature overview

Today the "Create variations" run freezes shapes according to one of six
**pre-defined frozen sets** — rows of a static table `FrozenSets`, chosen by an
integer index **0–5** in the run-parameters dialog. The `userSelectFrozen`
feature adds a seventh option: an ad-hoc set of shape IDs the user picks by
selecting drawObjs in a `TemplateWorkFrame` using its full selection machinery
(the same machinery `HueAssignLikeAG` uses), committed through a new modeless
`UserSelectFrozenFrame`/`UserSelectFrozenController` pair.

The user-selected set is treated like a seventh frozen set: same engine path,
same freeze semantics, same 12-variation recipe — only the *source* of the
shape IDs differs.

### Existing code-path landmarks

- `CreateSmartPicFrame` — Controls-info menu has **"Select Frozen drawObjs"**
  (disabled until a SmartPic exists) → `Actions.showUserSelectFrozenFrame()`.
- `CreateSmartPicController.showUserSelectFrozenFrame()` — **empty stub**; also
  `UserSelectFrozenController` and `UserSelectFrozenFrame` are empty stubs.
- `CreateSmartPicController.chooseRunParameters()` — builds the dialog:
  canonical `AGHuePattern` name combo + frozenSet index combo (`0..5`, built
  from `FrozenSets.FROZEN_SET_COUNT = 6`). Returns `RunParams`.
- `CreateSmartPicController.createVariations()` →
  `VariationPaletteExtractor.runCleanCycle(agHuePattern, frozenSet, facts)`.
- `VariationPaletteExtractor.runCleanCycle(String, int, Facts)` →
  `new FrozenShapesCategory().cook(agHuePattern, frozenSet)` →
  `VariationDriver` → `extract12VariationPalettes(results)`.
- `FrozenShapesCategory.cook(...)` — validates keys (`AGHuePatterns.get`,
  `FrozenSets.get`), builds the fixed 12-row recipe. **Row 1 is the bootstrap
  spec (no freeze goal); rows 2–12** each carry a `FREEZE_SHAPES` `StratgSpec`
  with `FROZEN_SET` bound to the index.
- `FreezeShapesStratg.execute(ctx, params)` — stratg **031**, `Phase.OVERRIDE`:
  `FrozenSets.get(params.getIntParam(StratgParam.FROZEN_SET))`, then for each
  shape ID: verify `state.shapeIds().contains(shape)`, `state.setFrozen(shape, true)`.
  It is an ordinary engine-level stratg, **not nestable** (flag writes cannot
  be harvested into a nested run).
- `SmartPic` — immutable ordered map of `SmartDrawObj` keyed by **1-based
  shapeNum** (`smartPic.smartDrawObjs()` / `smartDrawObjs().keySet()` is the
  valid shape-ID universe).
- `SmartDrawObj` — `shapeNum`, `name`, `drawObjUuid` (the Template DrawObj's
  `getUniqueId()`), color prefs.
- `SmartPicFacts` — adapts `SmartPic` to the engine's `Facts` interface.

### Supporting UI precedent

`HueAssignLikeAGFrame`/`HueAssignLikeAGController` is the working pattern for
"a modeless auxiliary frame that stays open while the user selects DrawObjs in
a `TemplateWorkFrame`, then commits." Its controller obtains the current
selection via
`templateWorkFrame.getDrawObjRoster().getSelectionGroup().getSelectedDrawObjsSet()`
and snapshots it before committing. `UserSelectFrozenController` will mirror
this shape.

`SmartPicAGCtrsFrozenSetFrame` is the diagnostic window showing AGHuePattern
details and the chosen frozen set's shape numbers/names; it currently resolves
a predefined set index via `FrozenSets.get(index)` and will need a user-set
branch.

---

## 2. The central design question: how does a `Set<Integer>` travel into a StratgSpec?

### 2.1 The spec/param model today

The varyengine has a deliberately light param schema:

- `ParamKind` — `INT`, `DOUBLE`, `TABLE_KEY`, `NAMED_KEY`. Documented as
  *descriptive metadata, not a type-checking framework*.
- `StratgParam` — enum of all param labels; each carries an external name and
  a `ParamKind`. Today: `FROZEN_SET("frozenSet", ParamKind.TABLE_KEY)`.
- `StratgType` — the "form template": `(id, externalId, Phase, requiredParams,
  optionalParams)`. `FREEZE_SHAPES` today: `required={FROZEN_SET}`,
  `optional={}`.
- `StratgSpec` — the "filled form": `StratgType` + `EnumMap<StratgParam,Object>
  boundParams`. Key behaviors:
  - `bind(param, int|double|String)` — the **only three value doors**; each is
    loosely kind-checked (int bind rejects only `DOUBLE` kind; String bind
    requires `NAMED_KEY`).
  - `checkDeclared(param)` — binding a param not declared by the type throws.
    This is the *real* gate: adding `FROZEN_SHAPES` to `FREEZE_SHAPES`'s
    declared set is mandatory regardless of encoding.
  - `isFullySpecified()` — `required ⊆ bound`. **Requiredness-only** — it
    cannot express alternatives like "exactly one of A or B".
  - `copyForCooking()` — **shallow** copy (new map, same values), justified by
    a comment stating "v1 param values are immutable Numbers".

There is no collection-valued bind and no fourth `ParamKind` — a
`Set<Integer>` cannot enter a `StratgSpec` today.

### 2.2 Candidate carriers for the user set (where could it live?)

| Option | Verdict | Reason |
|---|---|---|
| Mutate `FrozenSets` (a "row 6") | **Rejected** | Static, immutable, process-global table; `CreateSmartPicController` is explicitly one-per-frame for multi-window support → cross-window leak + mutation-safety problem. |
| On `Facts`/`SmartPicFacts` | **Rejected** | `Facts` is the read-only per-shape prefs contract; a frozen set is a *run parameter*, not shape data. Layering inversion: the SmartPic would need to know the freeze choice. |
| Via `StratgContext.Inputs` | **Rejected** | The codebase's own convention is "uniform dialog bindings are filled into the rows' open params at cook time." `frozenSet` is a dialog binding; routing its successor through `Inputs` breaks the **specs-are-self-contained** property — the cooked spec list is the complete inspectable record of run parameters. |
| A second `StratgType` (e.g. "032") for explicit sets | **Rejected** | Strongest schema (each type keeps one required param) but heaviest machinery — registry entry, resolver registration, an invented legacy ID — and it forks the *semantic* ("freeze these shapes") rather than the *key encoding*. Wrong axis to split. |
| **Second `StratgParam` on `FREEZE_SHAPES`** | **Chosen** | Same phase (`OVERRIDE`), same stratg, same semantic — "which shapes are frozen" — expressed with a different key type. Preserves provenance: `StratgSpec.toString()` keeps the full parameter story inside the spec. |

### 2.3 The encoding decision: CSV string vs. `Set<Integer>` + `SHAPE_SET`

Both options carry the same semantic; they differ on *where errors surface* and
*what contracts they strain*.

**Option A — CSV via the existing String bind** (e.g. `"3,5,12"`):

- *Advantages:* zero new machinery; human-readable in logs/`toString`;
  trivially serializable.
- *Disadvantages:* no compile-time safety (`"3;5;12"`, `"three,five"`, `""`
  all bind happily); a parsing convention every caller must obey and every
  reader must know; **failure timing is the worst possible** — a malformed
  value surfaces at `execute()` time, inside recipe row 2 of a 12-row run,
  rather than at bind time; the `ParamKind` label misdescribes the content
  (`NAMED_KEY` is for catalog names, not number lists — a permanent "read the
  javadoc for the secret format" tax).
- *Cost:* ~30 fewer lines today, a permanent stringly-typed tunnel.

**Option B — `Set<Integer>` via new `ParamKind.SHAPE_SET`** (CHOSEN):

- *Advantages:* compile-time type safety; fail-fast at bind; no parsing code;
  self-documenting kind; the only remaining run-time check — shape-ID
  membership against `state.shapeIds()` — is one the stratg **already performs
  and must perform regardless** (only the run knows the valid IDs).
- *Costs:* touches four files (`ParamKind`, `StratgParam`, `StratgSpec`,
  `StratgType`); the **mutability hazard** — `copyForCooking()`'s shallow copy
  assumes immutable scalar values, so `bind` must store `Set.copyOf(value)`
  (this also makes the immutability contract *explicit* rather than assumed —
  arguably an improvement); serialization needs a decided JSON encoding if
  specs are ever persisted (a JSON array — easy, but a decision where CSV is
  its own wire format); sets a **precedent**: first non-scalar param, template
  for future collection params.
- *Key insight:* a new `ParamKind` constant is warranted **either way** (kinds
  are descriptive metadata; labeling a CSV `NAMED_KEY` is semantic drift). So
  the real choice was only the *carrier*: `String` vs. `Set<Integer>`.

**Decision (accepted):** `Set<Integer>` with `ParamKind.SHAPE_SET`.

**User rationale:** `FROZEN_SHAPES` is likely to become the *primary* param when
`userSelectFrozen` supersedes `FrozenSets` — make it a first-class typed
citizen now rather than migrate the encoding later. `Set<Integer>` is the
clear representation of "the drawObjs being processed / entering the variation
process" and will serve other future ArtisticGoals beyond freezing.

### 2.4 The XOR (exactly-one-of) schema decision

`isFullySpecified()` is requiredness-only and cannot express "exactly one of
`FROZEN_SET`, `FROZEN_SHAPES`". Three designs were enumerated:

1. `FROZEN_SET` stays required; `FROZEN_SHAPES` optional-override — forces
   user-set cooks to bind a *meaningless* index to pass the check; bakes in a
   silent-override rule. Dishonest.
2. **Both optional; XOR enforced at the boundary that consumes it** —
   `FreezeShapesStratg.execute()` throws when `hasTable == hasSet`; enforced
   *by construction* in `cook` (each overload binds exactly one).
3. New `StratgType` — rejected per §2.2.

**Decision (accepted):** option 2. The schema becomes honest ("031 accepts two
alternate ways to name the frozen shapes"). Consequence accepted: a
hand-authored spec binding *neither* passes `isFullySpecified()` and fails at
`execute()` — safe because `cook` overloads are the only producers, and the
exception names the missing param explicitly. `CookTest`'s existing
"all 12 rows `isFullySpecified()`" assertions stay green.

---

## 3. Selection-side findings

### 3.1 Obtaining the selection

`DrawObjRoster` owns selection truth (`selectedDrawObjs`, per-DrawObj
`selected` flags, `SelectionEvent`s). The read path is:
`roster.getSelectionGroup().getSelectedDrawObjsSet()` → unmodifiable
`Set<DrawObj>` — exactly the call `HueAssignLikeAGController.commit()` makes.

### 3.2 DrawObj → shapeNum mapping

Two keys were evaluated:

- **Positional (chosen as primary):** `shapeNum = getAllDrawObjs().indexOf(drawObj) + 1`.
  This is the same contract `SmartPicFactory` and `loadPaletteColoredTemplates`
  already use. It cannot go stale and works for a same-instance template or a
  freshly loaded copy (order is preserved by load).
- **UUID (cross-check only):** `DrawObj.getUniqueId()` ↔
  `SmartDrawObj.drawObjUuid`. Identity-level, but `DrawObj.uniqueId` is
  `UUID.randomUUID()` at construction and `SmartPicFactory`'s javadoc warns
  **"DrawObjs without a stored UUID receive a fresh one at Template load
  time"** — a pre-v2 template file produces *different* UUIDs on each load, so
  UUID matching is only reliable for v2-era files. Use it as a sanity
  cross-check/log when it resolves, never as the sole key.

### 3.3 Which TemplateWorkFrame hosts the selection

Three options evaluated: reuse `ownerFrame` if it shows `chosenTemplate`
(needs an identity check); open `chosenTemplate` itself in a new frame
(mutations possible; `smartPic` already built so harmless to it, but the user
could recolor/save); or a **fresh copy via `storage.loadTemplate`**.

**Decision (accepted):** fresh copy — cleanest isolation; positional mapping
unaffected since load preserves drawObj order.

---

## 4. Decisions confirmed by the user (11/26/26)

- **Encoding:** `Set<Integer>` + `ParamKind.SHAPE_SET`, `StratgParam.FROZEN_SHAPES`. First-class typed param.
- **XOR schema:** `FROZEN_SET` and `FROZEN_SHAPES` both **optional** on `FREEZE_SHAPES`; "exactly one bound" enforced in `execute()` and by-construction in `cook`.
- **Selection host window:** fresh `Template` copy via `storage.loadTemplate`, shown in a new `TemplateWorkFrame`.
- **Set lifecycle (v1):** reset the user set when `createSmartPic()` runs again (also clear on `chooseTemplate()`, since a new template implies a new SmartPic). Revisit after UX feedback.
- **Commit semantics (v1):** single-commit — "use current selection as the frozen set". Revisit after UX feedback.
- **Empty set:** allowed (freeze nothing — a genuinely new capability `FrozenSets` cannot express), but **explicitly labeled and guarded by a warning dialog** (could be user error).
- **Persistence:** session-only for v1. A named catalog (per the `AGHuePatterns` precedent) is a future discussion — a named set would resolve to `Set<Integer>` and bind through this same param (or `FROZEN_SET` migrates to `NAMED_KEY`); either way the stratg's set-consuming core is unchanged.
- `FrozenSets` stays untouched; `cook(BroadCategorySpec)` learns "FROZEN_SHAPES if bound, else FROZEN_SET" (XOR enforced there too); the run-params dialog shows the user set as a **seventh entry**, not a replacement.

---

## 5. Implementation outline — three phases

### Phase A — engine seam (headless, fully unit-testable)

`ParamKind.SHAPE_SET` → `StratgParam.FROZEN_SHAPES` → `StratgSpec` set
bind/getter → `StratgType.FREEZE_SHAPES` optional params → `FreezeShapesStratg`
XOR → `FrozenShapesCategory` set-cook overloads + template-path update →
`VariationPaletteExtractor.runCleanCycle` overload. **All verifiable via JUnit
before any UI exists** — pins the contract, unblocks B and C. (Detailed plan in
§6.)

### Phase B — controller plumbing (no selection UI needed)

- `RunParams` gains the user set; `createVariations()` dispatches to the
  matching `runCleanCycle` overload.
- `chooseRunParameters()` adds a "user set (N shapes)" combo entry — shown
  only when a user set exists.
- `CreateSmartPicController` fields: `userFrozenShapes` (+ bookkeeping for
  `lastFrozenSet` vs. "user" display); cleared in `createSmartPic()` and
  `chooseTemplate()`.
- `VariationDisplayFrame` title shows `"Frzn:user"` for user-set runs;
  `SmartPicAGCtrsFrozenSetFrame` gains a user-set branch (constructor taking
  `Set<Integer>`).
- Testable without Phase C by injecting a set programmatically.

### Phase C — selection UI

- `UserSelectFrozenFrame`/`UserSelectFrozenController` modeled on the
  `HueAssignLikeAG` pair: modeless frame with instructions, a commit button
  ("Use current selection as the frozen set"), a status line (N shapes mapped),
  and the empty-set warning dialog.
- `showUserSelectFrozenFrame()`: guard `smartPic != null`; `storage.loadTemplate`
  fresh copy → new `TemplateWorkFrame`; construct controller + frame.
- Mapping on commit: positional (`getAllDrawObjs().indexOf + 1`), UUID
  cross-check when it resolves; validate ⊆ `smartPic.smartDrawObjs().keySet()`;
  deliver `Set<Integer>` to `CreateSmartPicController`; enable the seventh
  combo entry.
- Enable the "Select Frozen drawObjs" menu item after `createSmartPic()`
  succeeds (currently never enabled).

**Ordering rationale:** Phase A is independently verifiable and fixes the
contract; Phase B is mechanical plumbing; Phase C holds the remaining UX risk
and can be scheduled last (or iterated separately).

---

## 6. Phase A — concrete implementation plan

### 6.1 `ParamKind` — add `SHAPE_SET`

```java
public enum ParamKind {
    INT, DOUBLE, TABLE_KEY, NAMED_KEY,
    /** An immutable set of 1-based shape IDs (shapeNums). */
    SHAPE_SET
}
```

### 6.2 `StratgParam` — add `FROZEN_SHAPES`

```java
FROZEN_SHAPES("frozenShapes", ParamKind.SHAPE_SET),
```

### 6.3 `StratgSpec` — set-valued bind + getter

```java
/**
 * Binds a shape-set param (SHAPE_SET kind). Stores an immutable copy —
 * bound param values must be immutable (see copyForCooking).
 * Returns this for chaining.
 */
public StratgSpec bind(StratgParam param, Set<Integer> value) {
    checkDeclared(param);
    if (param.getKind() != ParamKind.SHAPE_SET) {
        throw new IllegalArgumentException(
                param.getExternalName() + " is a " + param.getKind() + " param; bind(Set) requires SHAPE_SET");
    }
    boundParams.put(param, Set.copyOf(Objects.requireNonNull(value, param.getExternalName())));
    return this;
}

/** Reads a bound SHAPE_SET param (unmodifiable). */
@SuppressWarnings("unchecked")
public Set<Integer> getIntSetParam(StratgParam param) {
    return (Set<Integer>) requireBound(param);
}
```

- `Set.copyOf` at bind is the fix for the `copyForCooking()` shallow-copy
  hazard: every bound value is then immutable, preserving the existing
  "shallow is safe" argument. **Update `copyForCooking`'s javadoc**: "v1 param
  values are immutable scalars or immutable sets."
- `Set.copyOf` rejects null elements (NPE) and null sets (via the explicit
  requireNonNull) — acceptable fail-fast.
- Needs `import java.util.Set;` (and `Objects` already imported).
- `getIntSetParam` returns the already-unmodifiable stored set — no extra copy.

### 6.4 `StratgType.FREEZE_SHAPES` — both params optional

```java
FREEZE_SHAPES(31, "031", Phase.OVERRIDE,
        EnumSet.noneOf(StratgParam.class),
        EnumSet.of(StratgParam.FROZEN_SET, StratgParam.FROZEN_SHAPES));
```

`getDeclaredParams()`/`declares()` already compute required ∪ optional — no
other changes needed. XOR enforcement lives in the stratg (6.5) and cook (6.6).

### 6.5 `FreezeShapesStratg` — XOR resolution

```java
@Override
public void execute(StratgContext ctx, StratgSpec params) {
    StratgSupport.requireType(params, type());
    boolean hasTable = params.isBound(StratgParam.FROZEN_SET);
    boolean hasSet = params.isBound(StratgParam.FROZEN_SHAPES);
    if (hasTable == hasSet) {
        throw new IllegalStateException(
                "031: exactly one of frozenSet / frozenShapes must be bound"
                        + " (frozenSet bound=" + hasTable + ", frozenShapes bound=" + hasSet + ")");
    }
    Set<Integer> frozenSet = hasTable
            ? FrozenSets.get(params.getIntParam(StratgParam.FROZEN_SET))
            : params.getIntSetParam(StratgParam.FROZEN_SHAPES);
    RunState state = ctx.runState();
    for (int shape : frozenSet) {
        if (!state.shapeIds().contains(shape)) {
            throw new IllegalArgumentException(
                    "031: frozen set references shape " + shape
                            + " which is outside this run " + state.shapeIds());
        }
        state.setFrozen(shape, true);
    }
}
```

Update the class javadoc to describe both key types ("named by its
`frozenSet` param ... or its `frozenShapes` param (an explicit SHAPE_SET of
1-based shape IDs)").

### 6.6 `FrozenShapesCategory` — set-cook overloads + template path

New overloads mirroring the existing two:

```java
/** Cook with the standard valueRange default, using an explicit frozen-shape set. */
public List<ArtisticGoalSpec> cook(String agHuePattern, Set<Integer> frozenShapes) {
    return cook(agHuePattern, frozenShapes, VariationConstants.VALUE_RANGE);
}

/**
 * Standard cook — the fixed 12-row recipe with an explicit frozen-shape set
 * (the userSelectFrozen path). Shape-ID membership is NOT checked here —
 * only the run knows valid IDs; FreezeShapesStratg validates at execute time.
 */
public List<ArtisticGoalSpec> cook(String agHuePattern, Set<Integer> frozenShapes, double valueRange) {
    AGHuePatterns.get(agHuePattern);                       // table-key bounds still fail fast
    Set<Integer> shapes = Set.copyOf(Objects.requireNonNull(frozenShapes, "frozenShapes"));
    List<ArtisticGoalSpec> specs = new ArrayList<>(howManyVariations);
    for (RecipeRow row : RECIPE) {
        specs.add(buildRow(row, agHuePattern, null, shapes, valueRange));
    }
    return specs;
}
```

Refactor `buildRow` to carry both alternates (or extract a
`bindFreeze(StratgSpec, Integer frozenSet, Set<Integer> frozenShapes)` helper):

```java
private ArtisticGoalSpec buildRow(RecipeRow row, String agHuePattern,
                                  Integer frozenSet, Set<Integer> frozenShapes,
                                  double valueRange) {
    // ... unchanged goal construction ...
    // rows with a freeze goal (rows 2–12) get exactly one binding:
    StratgSpec freeze = new StratgSpec(StratgType.FREEZE_SHAPES);
    if (frozenShapes != null) {
        freeze.bind(StratgParam.FROZEN_SHAPES, frozenShapes);
    } else {
        freeze.bind(StratgParam.FROZEN_SET, frozenSet);
    }
}
```

(The existing `cook(String, int, double)` delegates with `frozenShapes = null`
— or keeps its own bind call; either shape of the refactor is fine, the
invariant is: **exactly one is non-null per cook call.**)

`cook(BroadCategorySpec)`: where it currently extracts `frozenSet` from the
template spec's bound params, change to "read `FROZEN_SHAPES` if bound, else
require `FROZEN_SET`" — the same XOR rule, and a missing-both case must fail
loudly here too.

### 6.7 `VariationPaletteExtractor` — overload

```java
public static List<VariationResult> runCleanCycle(String agHuePattern,
                                                  Set<Integer> frozenShapes,
                                                  Facts facts) {
    List<ArtisticGoalSpec> specs = new FrozenShapesCategory().cook(agHuePattern, frozenShapes);
    // ... identical tail to the existing overload (extract into a shared private helper)
}
```

### 6.8 Phase A test cases (JUnit 5, `src/test/java/pkgVariationModule`)

**`StratgSpec` / `StratgTest`-level:**

1. `bind(FROZEN_SHAPES, Set.of(3,5,12))` on a `FREEZE_SHAPES` spec →
   `isBound(FROZEN_SHAPES)` true, `getIntSetParam` equals `{3,5,12}`.
2. `bind(someIntParam, aSet)` → `IllegalArgumentException` (kind check).
3. `bind(FROZEN_SHAPES, set)` on a spec whose type doesn't declare it (e.g.
   `ASSIGN_OST_DOTS`) → `IllegalArgumentException` (`checkDeclared`).
4. Mutability contract: mutate the source `HashSet` after binding → bound
   value unchanged; `getIntSetParam(...).add(99)` → `UnsupportedOperationException`.
5. `copyForCooking()` of a spec with `FROZEN_SHAPES` → copy has equal bound
   set; `equals`/`hashCode` hold.
6. Null set → `NullPointerException`/`IllegalArgumentException` (document whichever).

**`FreezeShapesStratg` (in `StratgTest`, using the existing fake-RunState
pattern):**

7. Spec with **neither** bound → `IllegalStateException`.
8. Spec with **both** bound → `IllegalStateException`.
9. `FROZEN_SHAPES = {valid IDs}` → each ID frozen afterward (frozen-flag
   accessor — confirm `RunState.isFrozen(int)` or equivalent during impl).
10. `FROZEN_SHAPES` containing an out-of-run ID → `IllegalArgumentException`
    naming the shape.
11. `FROZEN_SHAPES = {}` → completes, freezes nothing (empty set is legal).
12. `FROZEN_SET` path still freezes the table row (regression — likely
    already covered by an existing test; extend if not).

**`CookTest` additions:**

13. `cook(AGHUE_PATTERN, Set.of(3,5,12))` → 12 specs, all `isFullySpecified()`;
    row 1 has no `FREEZE_SHAPES`; rows 2–12 have `FROZEN_SHAPES == {3,5,12}`
    bound and `isBound(FROZEN_SET)` false.
14. `cook(AGHUE_PATTERN, (Set<Integer>) null)` → NPE/IAE.
15. `cook(template)` where the template's `FREEZE_SHAPES` spec binds
    `FROZEN_SHAPES` → propagated into rows 2–12.
16. `cook(template)` where the template's `FREEZE_SHAPES` spec binds neither →
    `IllegalArgumentException`.

**Cycle-level (`SmartPicCycleTest`/`EngineTest`-adjacent):**

17. `runCleanCycle(pattern, userSet, facts)` → 12 `VariationResult`s; the
    user-set shapes are frozen in rows 2–12 output (assert against the
    cycle's observable frozen-flag state, mirroring how the predefined-set
    cycle test asserts today).

### 6.9 Phase A file-change checklist

| File | Change |
|---|---|
| `varyengine/model/ParamKind.java` | + `SHAPE_SET` |
| `varyengine/model/StratgParam.java` | + `FROZEN_SHAPES` |
| `varyengine/model/StratgSpec.java` | + `bind(param, Set<Integer>)`, + `getIntSetParam`, javadoc update on `copyForCooking` |
| `varyengine/model/StratgType.java` | `FREEZE_SHAPES`: required→none, optional→{FROZEN_SET, FROZEN_SHAPES} |
| `varyengine/model/stratgs/FreezeShapesStratg.java` | XOR resolution + javadoc |
| `varyengine/model/FrozenShapesCategory.java` | two set-cook overloads, `buildRow`/bind refactor, `cook(BroadCategorySpec)` XOR extraction |
| `varyengine/controller/VariationPaletteExtractor.java` | `runCleanCycle(String, Set<Integer>, Facts)` overload |
| `src/test/java/pkgVariationModule/*` | tests 1–17 above |

No changes to: `FrozenSets`, `Facts`, `SmartPicFacts`, `StratgResolver`,
`StratgContext`, `RunState`.

---

## 7. Deferred / future items

- **Named frozen-set catalog** (the `AGHuePatterns` precedent): named sets
  resolve to `Set<Integer>` bound via `FROZEN_SHAPES`, or `FROZEN_SET` migrates
  `TABLE_KEY`→`NAMED_KEY`; the stratg's set-consuming core is unchanged either
  way. Design deferred until after v1 UX feedback.
- **Incremental commit UI** (add/remove/"seed from pre-defined set" buttons).
- **User-set lifecycle:** re-validate against a rebuilt SmartPic instead of
  clearing; warn-on-stale rather than clear-on-rebuild.
- **StratgSpec serialization/wire format:** when specs are persisted,
  `SHAPE_SET` encodes as a JSON array; `copyForCooking`'s immutability
  convention becomes part of the serialization contract.
- **`SHAPE_SET` as precedent:** first non-scalar param — template for future
  collection params (shape subsets, per-shape overrides, anchor-shape groups)
  in other ArtisticGoals.
- **`FrozenSets` supersession:** if `userSelectFrozen` becomes the primary
  path, the predefined sets may later become entries in the catalog rather
  than a special-case table.
