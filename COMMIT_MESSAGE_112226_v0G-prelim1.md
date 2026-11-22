# COMMIT_MESSAGE_version 0G-prelim1 (112226): "Go to N hues" — subset-to-hue recoloring + HueAssignLikeAGFrame session UI

## Summary

Adds the "Go to N hues" functionality: four new Hue Circle menu items
("Go to 1 hue" … "Go to 4 hues") that open a modeless
`HueAssignLikeAGFrame` alongside `TemplateWorkFrame` and drive an
incremental session in which the user assigns groups of DrawObjs to
distinct hues. Each commit recolors a subset of the roster to a single
absolute hue while preserving every DrawObj's Ostwald fill-order
position, and fires the standard `COLOR_CHANGE` event. The feature is
built on one new recoloring primitive, `recolorSubsetToHue`, exposed on
`IColorOperationService`; all new classes live in `variationmodule`.
The dormant `huepattern`/`artintellig` packages are untouched by the
new flow — `AnchorPattern`/`Spread` there were renamed to
`AnchorPatternPrelim`/`SpreadPrelim` to free the names for future
variationmodule work.

## New classes

- **`AnchorGroup`** (`variationmodule.model`) — minimal commit record
  for a hue group: `Set<UUID> memberUUIDs` + `HueNumGUI assignedHue`,
  with a `fromDrawObjs` factory. Names the concept "group of DrawObjs
  bound to one hue" for the future `AnchorPattern` work; session
  instances are transient (hue identity = group identity).
- **`HueAssignLikeAGController`** (`variationmodule.controller`) —
  session state + commit logic: `anchorPatternLevel` N (1–4, from the
  menu item), `usedHueNums` (user-facing 1–36), committed
  `AnchorGroup`s. Validation: a hue not yet used creates a new group
  (requires `|usedHues| < N`); a used hue merges/moves DrawObjs into
  that group (last-write-wins re-assignment, always allowed).
  `computeRemainder()` returns every DrawObj whose current hue is not
  in `usedHues`; `isRemainderExpected()` is true at N−1 hues used;
  `isSessionComplete()` = N hues used AND remainder empty;
  `reclaimVacatedHues()` frees a hue when its last member is moved away.
- **`HueAssignLikeAGFrame`** (`variationmodule.view`) — the display
  class: a modeless `JFrame` showing a hue spinner (1–36), an
  "Assign all remaining unassigned DrawObjs" checkbox (auto-checked
  at N−1 hues), a commit button that relabels between "Apply to
  selection" / "Apply to remainder", a bold status line ("k of N hues
  assigned" / "…— complete"), and a "Hues used:" list. Modeless by
  design so all of the main window's selection machinery stays usable
  between commits; stays open after completion for re-assignment.

## Modified classes

- **`TemplatePaletteShift`** — new `recolorSubsetToHue(Set<DrawObj>,
  HueNumGUI, DrawObjRoster, HueRotateCallback)`: resolves the target
  hue's `MonotriPage` once, then for each DrawObj looks up
  `getColorByFillOrder(fillOrderNum)` and calls `setColor`, then
  `drawObjRoster.updateToCurrObjState()`. Mirrors
  `recolorByHueRotateShift` minus `circularAddOrSubtract` — absolute
  hue, not rotation.
- **`PaletteOperationHub`** — new `recolorSubsetToHue(Set<DrawObj>,
  HueNumGUI)` orchestration: validates, snapshots the subset, delegates
  to `TemplatePaletteShift`, fires `COLOR_CHANGE` via
  `HueRotateCallback`. Unlike the shift operations, the subset is
  caller-supplied (selection snapshot, computed remainder, or whole
  roster).
- **`IColorOperationService`** — exposes
  `boolean recolorSubsetToHue(Set<DrawObj>, HueNumGUI)` — the single
  service method for all four menu items (N lives in session logic,
  not service logic).
- **`TemplateWorkFrame`** — `@Override recolorSubsetToHue` delegate to
  the hub; four "Go to N hues" menu items after a separator in the Hue
  Circle menu; `goToNHues(int)` validates template + hub and opens
  `HueAssignLikeAGFrame`.
- **Dormant renames (user)**: `huepattern`'s `AnchorPattern` →
  `AnchorPatternPrelim` and `Spread` → `SpreadPrelim`, with reference
  sites updated across the dormant package. These packages remain
  dormant; the renames free the names for future variationmodule
  classes.

## Execution flow

```
Hue Circle menu "Go to N hues"
  → TemplateWorkFrame.goToNHues(n)  [thin handler]
    → new HueAssignLikeAGFrame(n, twf)  [modeless JFrame]
      → HueAssignLikeAGController {anchorPatternLevel n, usedHueNums}
        → per commit:
            remainder checked → subset = {d : hue(d) ∉ usedHues}
            else              → subset = snapshot of SelectionGroup
            validate hue (new: |usedHues| < n; used: merge allowed)
          → PaletteOperationHub.recolorSubsetToHue(subset, hueNumGUI)
            → TemplatePaletteShift.recolorSubsetToHue(...)
              → per DrawObj: fillOrder → MonotriPage(targetHue)
                    .getColorByFillOrder → setColor
              → roster.updateToCurrObjState()
            → fireEvent(COLOR_CHANGE)
          → usedHues.add(hue); reclaimVacatedHues(); AnchorGroup record
        → remainder checkbox auto-checks at n−1 hues
        → completion: |usedHues| == n ∧ remainder empty
          (soft state — frame stays open for re-assignment)
```

## Design notes

- **Hue identity = group identity**: session hues are unique, so group
  membership is derivable from each DrawObj's current color — no
  membership tables (`RMatrixMiniPrelim`-style) needed. Re-assignment
  is inherent last-write-wins, not extra code.
- **Auto-remainder**: the user selects only N−1 groups; the last group
  is `{d : hue(d) ∉ usedHues}`, guaranteeing the full-partition
  invariant by construction. "Go to 1 hue" degenerates to this case
  immediately (0 == N−1 → pre-checked remainder = whole roster).
- **Modeless, not modal**: a modal dialog would block the main frame's
  selection controls; the `AGsInfoFrame` two-window precedent is
  reused without touching `anchorPatternPanel` or the dormant flow.
- **Early remainder**: allowed at any commit (confirm dialog warns),
  since it is just another way to compute the subset.
- **Mid-session safety**: user-driven via Edit → "Save palettebookmark"
  (`PaletteOperationHub.saveBookmark()` / `PaletteBookmarkList`).
- **`AnchorHueSet`** (`varyengine.model`) is the existing 4-AG
  hue-assignment type used by the 12-variation engine; the new
  `AnchorGroup` is the session-side counterpart, and a future
  `AnchorPattern` in `variationmodule` will unify the concepts.
