COMMIT_MESSAGE_version 0B (111626): Add Palette variations menu-item flow — variationmodule V1

New functionality
-----------------
Adds a "Palette variations" item to the Color-sources menu of TemplateWorkFrame.
Selecting it opens a file chooser (rooted at the template storage directory,
JSON filter), loads the chosen serialized Template, and opens a
VariationDisplayFrame showing 12 thumbnail renderings of that template.

V1 deliberately renders 12 identical copies of the loaded template — the
palette-variation engine (planned for the new variationmodule.varyengine
package) that will produce 12 different variation palettes is ported later.
V1 exists to prove the menu-item flow, orchestration, and window layout.

Flow of execution
-----------------
1. Menu action -> new VariationDisplayController(mediator, this).launch()
   (one controller + one frame per invocation; multi-window requirement).
2. Controller constructs its own TemplateStorage(mediator), shows a
   JFileChooser, strips the .json suffix from the selection.
3. storage.loadTemplate(name) is called 12 times -> 12 fully independent
   Template instances (no clone machinery needed).
4. VariationDisplayFrame is constructed with the 12 Templates, a
   UUID windowId, and title "Variations: <templateName>", then centered
   on the owning TemplateWorkFrame and shown.
5. Each of the 12 ThumbnailPanels renders its Template's DrawObj[] with
   a CoordinateTransformer fit-to-bounds transform (same technique as
   ShapeDisplayPanel: union bounds -> aspect-preserving scale + centering,
   5px padding), filling each GeneralPath with its AWT color.

New classes (all under com.csurf.variationmodule)
-------------------------------------------------
- view.ThumbnailPanel: 145x145 non-interactive panel; fit-to-bounds fill
  rendering via CoordinateTransformer; no selection stroke, no CMYK
  handling (always toAWTColor()); degenerate-bounds guard.
- view.VariationDisplayFrame: the new display JFrame. Shows the 12
  template renderings in a fixed GridLayout(4,3,5,5) of 145x145
  ThumbnailPanels, named thumbnailPanel1..12 in row-major order
  (1-3 row 1, 4-6 row 2, 7-9 row 3, 10-12 row 4), plus a 60px
  full-width controlPanel (placeholder for future variation-engine
  parameters). DISPOSE_ON_CLOSE, non-resizable, stores a UUID windowId.
- controller.VariationDisplayController: orchestrates the flow; holds
  the shared ApplicationMediator and the owning TemplateWorkFrame
  (ownerFrame link for future recolor-back flow); owns TemplateStorage.
- controller.VariationSubunitMediator: thin-shell SubunitMediator for
  future inbound message routing, with a UUID->controller registry
  (demultiplexing to multiple open windows). Not registered yet.

Modified
--------
- TemplateWorkFrame: added "Palette variations" JMenuItem +
  openPaletteVariations() launching the controller.

Notes
-----
- variationmodule.model and variationmodule.varyengine packages are
  reserved (empty) for MiniTemplate/Factory (deferred) and the
  palette-variation engine incl. the PalettesThe12Variations DTO.
- Design rationale captured in
  111626_v0B_SWE-2_PaletteVariation_menu-item_flow_v1.md.
