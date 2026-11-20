# COMMIT_MESSAGE_version 0F (112026): Controls-info displays, variation→TemplateWorkFrame click-open, TemplateWorkFrame partial-load fix

## Summary

Adds the `Controls-info` menu to `CreateSmartPicFrame` with two
read-only inspection windows (Template drawObj list, SmartPic hue-anchors
/ frozen-set info), makes the 12-variation thumbnails clickable so a
variation opens in a full `TemplateWorkFrame` for further work, and fixes
a pre-existing partial-load bug in `TemplateWorkFrame`'s two-arg
constructor that left the all-DrawObjs list empty for any in-memory
Template.

## New classes

- **`TemplateDrawObjListFrame`** (`view`) — simple JFrame listing the
  chosen Template's drawObj names in numbered order. Titled
  `DrawObjs: <templateName>`; disabled until a Template is selected.
- **`SmartPicAGCtrsFrozenSetFrame`** (`view`) — three-panel inspection
  window (controlsPanel / hueSetInfoPanel / frozenSetInfoPanel) opened
  with the last-run parameters (defaults hueSet 18, frozenSet 4). The
  hueSet combo lists all 25 sets and shows the four `AGn → hue`
  anchor mappings (5-row text area); the frozenSet combo lists all 6
  sets and shows each frozen shape's number + SmartPic-derived name.
  Disabled until a SmartPic exists.
- **`TemplateVariationsResult`** (`variationmodule.model`) — immutable
  domain result holding the 12 variation Templates in slot order (0 =
  bootstrap). `templates()`, `templateAt(slot)`, `numVariations()`.
  Slot index is the stable key for future per-variation provenance.

## Modified classes

- **`CreateSmartPicFrame`** — new `Controls-info` menu; both items are
  fields enabled by the controller (`setDrawObjListEnabled` /
  `setHueAnchorsEnabled`).
- **`CreateSmartPicController`** — `DEFAULT_HUE_SET = 18` /
  `DEFAULT_FROZEN_SET = 4` shared by the run dialog and info frame;
  `lastHueSet`/`lastFrozenSet` stash each confirmed run's params;
  implements both Controls-info actions; wraps variations in
  `TemplateVariationsResult`; wires thumbnail clicks to
  `openVariationInWorkFrame` → `new TemplateWorkFrame(mediator,
  template)`; names each variation `base(≤30 chars) + "--var" + N`.
- **`VariationDisplayFrame`** — constructor takes
  `TemplateVariationsResult` (retained, `getVariationsResult()`);
  `VariationSelectionListener` callback + per-thumbnail
  `MouseAdapter` (slot-indexed, empty cells inert, hand cursor +
  tooltip). Frame stays a dumb view: it reports the slot, the
  controller decides.
- **`VariationDisplayController`** — same `TemplateVariationsResult` wrap
  and click-open wiring for the V1 palette-variations flow.
- **`TemplateWorkFrame`** — the (mediator, template) constructor now
  delegates to `updateUIWithTemplate(template)` instead of a partial
  sequence. Root cause of the empty list: `displayDrawObjsInList()` is
  guarded by `displayFacade != null`, and the facade was only built in
  `updateUIWithTemplate` (the file-open path). The image still rendered
  because `displayDrawObjsInShapesPanel()` needs only
  `currTemplate.getAllDrawObjs()`. The full path additionally wires
  `selectionDisplayManager`, `controller.setDrawObjRoster`, selection
  listeners, `PaletteOperationHub` + B0 bookmarks, and the title bar —
  making opened variations fully workable. Same latent fix for
  `SVGConverterFrame`'s call site.

## Execution flow

```
Template menu → Select Template        → enables "Template drawObj list"
SmartPic menu → Choose AGprefs + Build → enables "Hue anchors/ frozen"
SmartPic menu → Create 12 Variations
  → run dialog (hueSet 18 / frozenSet 4 defaults; params stashed)
  → clean cycle → 12 palettes → 12 Template copies (positional apply,
    named base--varN) → TemplateVariationsResult
  → VariationDisplayFrame (4×3 thumbnails)
      click thumbnail i → variationSelected(i)
      → variationsResult.templateAt(i)
      → new TemplateWorkFrame(mediator, template)  [now fully loaded]
```

## Design notes

- `VariationDisplayFrame` keeps no Template list field — it reads
  through the retained `TemplateVariationsResult`, preserving the
  dumb-view role in MVC.
- Info-frame data comes from the static engine tables
  (`AnchorHueSets`, `FrozenSets`); `SmartPic` contributes only names —
  consistent with the domain-not-engine dependency rule.
