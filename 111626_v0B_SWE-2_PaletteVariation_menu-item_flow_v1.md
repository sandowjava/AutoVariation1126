# Palette Variations Feature — Menu-Item Flow, V1 Design

Date: 2026-11-16
Status: V1 implemented and compiling. This document is the basis for continued design discussion.
Supersedes context from: `111526_v0B_SWE-2_MiniTemplate_scaling_Factory.md` (MiniTemplate approach deferred — see §5.4)

---

## 1. Feature Overview

A new menu item, **"Palette variations"**, in the `Color-sources` menu of `TemplateWorkFrame` opens a `VariationDisplayFrame` window showing **12 renderings** of a user-selected serialized `Template`, laid out in a fixed 4×3 grid of 145×145 thumbnail panels above a 60-pixel `controlPanel`.

**V1 goal:** verify the orchestration flow and window layout. All 12 renderings are intentionally identical (the "hack" — each panel renders a copy of the same loaded template with its own colors). The palette-variation engine that will produce 12 *different* palettes is ported from another project and deferred until V1 works.

**Future goal:** the `controlPanel` will hold user parameters that feed a palette-variation engine; each thumbnail will render the template recolored with one of 12 generated palettes; eventually a clicked thumbnail's palette will be applied back to the `TemplateWorkFrame` that opened the window.

---

## 2. V1 Implemented Architecture

All new classes live under `com.csurf.variationmodule`:

| File | Role |
|------|------|
| `variationmodule/view/ThumbnailPanel.java` | 145×145 `JPanel`; fit-to-bounds rendering of a `DrawObj[]` (no stroke, no interactivity, always `toAWTColor()`). |
| `variationmodule/view/VariationDisplayFrame.java` | `JFrame`; `GridLayout(4, 3, 5, 5)` of 12 `ThumbnailPanel`s + 60px `controlPanel`; `DISPOSE_ON_CLOSE`; stores `UUID windowId`. |
| `variationmodule/controller/VariationDisplayController.java` | Per-window orchestrator; owns `TemplateStorage`; chooser → load → open frame. |
| `variationmodule/controller/VariationSubunitMediator.java` | Thin-shell `SubunitMediator` for future inbound message routing; **not registered** yet. |
| `templatecore/view/TemplateWorkFrame.java` (edited) | `"Palette variations"` `JMenuItem` added to `Color-sources` menu → `openPaletteVariations()`. |

`variationmodule/model/` exists but is **empty** in V1 (`MiniTemplate`/`MiniTemplateFactory` deferred). A new `variationmodule/varyengine/` package has been created for the future variation engine (see §6).

---

## 3. Menu-Item Flow (V1, as implemented)

1. User clicks **Color-sources → Palette variations** in a `TemplateWorkFrame`.
2. `openPaletteVariations()` executes `new VariationDisplayController(mediator, this).launch()` — one controller per invocation, passing the shared `ApplicationMediator` and the owner frame (`this`).
3. The controller constructs `new TemplateStorage(mediator)` (see §5.2 for why the mediator is passed).
4. `launch()` shows a `JFileChooser` rooted at `storage.getStorageDir()` (`dev_storage/templates`) with a `.json` filter.
5. On selection, the filename's `.json` suffix is stripped.
6. `storage.loadTemplate(name)` is called **12 times**, producing 12 fully independent `Template` instances (no clone machinery needed).
7. A `VariationDisplayFrame` is constructed with the 12 templates, a `UUID.randomUUID()` window ID, and title `"Variations: <templateName>"`.
8. The frame is centered on the owner (`setLocationRelativeTo(ownerFrame)`) and shown. Each `ThumbnailPanel` receives `template.getAllDrawObjs().toArray(new DrawObj[0])` and renders immediately via its fit-to-bounds `paintComponent`.

Result: 12 identical thumbnails (each an independent `Template` copy), one `VariationDisplayFrame` window. Repeating the menu action opens additional independent windows — the multi-window requirement.

---

## 4. V1 Design Decisions & Rationale

- **Load from disk via chooser — always.** V1 does not prefer the currently loaded `currTemplate`; the chooser flow matches the existing `openTemplate` pattern and keeps the feature self-contained.
- **12 independent `Template` instances, not one shared** — each panel will eventually hold a differently-recolored template; independence now avoids retrofit. Obtained by calling `loadTemplate` 12× (simplest possible; side effects noted in §5.2).
- **`MiniTemplate`/`MiniTemplateFactory` NOT used for thumbnails** — see §5.4. Their future use is deferred for separate discussion.
- **Grid:** `GridLayout(4, 3, 5, 5)` — 4 rows, 3 columns, 5px hgap/vgap; grid panel has a 5px `EmptyBorder`. Cells are 145×145.
- **Panel naming — row-major:** `thumbnailPanel1`–`thumbnailPanel3` = row 1, `4`–`6` = row 2, `7`–`9` = row 3, `10`–`12` = row 4. Panel at grid index `i` gets name `"thumbnailPanel" + (i + 1)` — identical to `GridLayout` add order. (Originally column-major; changed to row-major because it removes the index-remapping quirk and simplifies palette↔panel mapping.)
- **`controlPanel`:** 60px high, full window width, `BorderLayout.SOUTH`. Contents deferred — will hold parameters feeding the variation engine.
- **Thumbnails are completely non-interactive in V1** — no click selection, no listeners.
- **Color mode:** always `toAWTColor()`; no CMYK handling in thumbnails.
- **Multiple windows allowed** — one controller + one frame per menu invocation; each frame has `DISPOSE_ON_CLOSE` so closing it doesn't exit the app.
- **Window ID:** `UUID` per frame — stronger than a counter; needed for future mediator message routing.
- **Owner link:** the controller stores `TemplateWorkFrame ownerFrame` — future requirement: a variation window's chosen palette gets applied back to the specific window that opened it. A direct reference is needed because mediator routing cannot address a specific `TemplateWorkFrame` instance (there is only one `"smartpic"` subunit for all of them). Precedent: `TemplateWorkFrame.linkedMonoTriFrames` already tracks linked child frames by direct reference.
- **Cell padding:** 5px inside each thumbnail's fit transform (10px would eat too much of a 145px cell). Verified safe: `CoordinateTransformer` math gives `scale = (145 − 10)/bounds` — positive for any nonzero bounds; a degenerate-bounds guard was added for the zero-width/height case.
- **Fixed size:** frame is non-resizable (`pack()` then `setResizable(false)`), consistent with the fixed-cell requirement.

---

## 5. Analysis Findings (basis for the design)

### 5.1 Rendering: `ShapeDisplayPanel.paintComponent` is the model

The template image in `TemplateWorkFrame` is rendered by `ShapeDisplayPanel` (620×820). Its `paintComponent`:

- Enables `KEY_ANTIALIASING` → `VALUE_ANTIALIAS_ON`.
- Applies `CoordinateTransformer` to `Graphics2D` — the key behavior: it computes the **union bounds** of all `DrawObj` paths, then builds a fit transform: `scale = min((w − 2·pad)/boundsW, (h − 2·pad)/boundsH)`, aspect-preserving and centered, with padding.
- For each `DrawObj`: `g2d.fill(obj.getPath())` with `getDisplayColor(obj)` (AWT or CMYK depending on the frame's color mode), then `g2d.draw(path)` with a 1px normal or 4px dashed selection stroke.
- It does **not** call `DrawObj.drawShape()` — it fills paths directly and strokes separately. This made it trivial to satisfy "no selection stroke": `ThumbnailPanel` simply omits the stroke block.

**Implication:** `ShapeDisplayPanel` does fit-to-bounds scaling *at paint time* — source geometry size is irrelevant because the transform always normalizes union bounds into the panel. `ThumbnailPanel` replicates exactly this (same `CoordinateTransformer`, reused from `templatecore.svgconverter.view`), minus the selection stroke and CMYK check.

`ShapeDisplayPanel` could not be reused directly (hardcoded 620×820 preferred size; `ThumbnailPanel` will also gain responsibilities later that justify a dedicated class).

### 5.2 `TemplateStorage(ApplicationMediator)` — why the dependency exists

`ApplicationMediator` (`com.csurf.core`) is a name-keyed message router: `Map<String, SubunitMediator>` with `registerSubunit(name, sub)` and `sendMessage(targetName, msg)` → `sub.handleMessage(msg)` (returns `Object` synchronously). `Message` carries `type`, `payload`, `source`.

`TemplateStorage` stores the mediator for **one line** in `loadTemplate()`:

```java
mediator.sendMessage("template", new Message("TEMPLATE_LOADED", template, null));
```

It publishes a "template loaded" notification — the constructor dependency exists purely for this. **Finding: no `"template"` subunit is ever registered** (registered subunits in `ApplicClass.main()`: `"svg"`, `"svgconverter"`, `"smartpic"`, plus `"app"` from `ApplicFrame`), so the call is currently a silent no-op returning `null`. Passing the *shared* mediator (not `new ApplicationMediator()`) is still required: it costs nothing, and the day a `"template"` subunit exists, routing works. Passing the real one is also the only mechanism that makes any future inter-module messaging possible.

**V1 caveat:** 12× `loadTemplate` fires `TEMPLATE_LOADED` 12× (harmless today), plus 12× the deserialization debug prints — accepted trade-off for zero clone machinery. Each deserialized copy shares DrawObj UUIDs — harmless, they are separate containers.

**Non-obvious fact:** `ApplicationMediator` is **unicast** — `sendMessage("template", …)` goes only to the subunit named `"template"`. There is no broadcast. A variation window will *not* hear about other windows' template loads unless a sender targets `"variationmodule"` explicitly.

### 5.3 Mediator functions involving `VariationDisplayController`

The controller is a **mediator client** (holds the mediator, calls `sendMessage`), not yet a **mediated subunit** (receives messages). Full list:

1. **Constructor injection (V1):** `new VariationDisplayController(mediator, this)` — enabler for all below.
2. **Indirect publish `TEMPLATE_LOADED` (V1, incidental):** each `loadTemplate` publishes the message — currently dead (§5.2).
3. **Outbound `GENERATE_VARIATIONS` (future):** controller sends controlPanel parameters to the variation engine; **the call returns `PalettesThe12Variations` synchronously as the `handleMessage` return value** — simplest approach, fits the `Object` return contract (the interface returns `Object`; the caller casts to `PalettesThe12Variations`).
4. **Outbound `VARIATION_PALETTE_SELECTED` / `APPLY_PALETTE` (future):** when thumbnails become interactive, the chosen `PaletteWName` is sent toward `templatecore`/`"smartpic"` for recoloring — combined with `ownerFrame` to identify the target window.
5. **Inbound commands (future):** e.g., regenerate-with-parameters — arrive at `VariationSubunitMediator.handleMessage`, routed to the right window's controller.
6. **Registration/demultiplexing (design):** `ApplicationMediator` maps **one name → one subunit**, but N variation windows can be open. Resolution: a single module-level `VariationSubunitMediator` (registered as `"variationmodule"` later) keeps `Map<UUID, VariationDisplayController>` and demultiplexes inbound messages by `windowId`. The thin shell + `registerController`/`unregisterController` exist now; registration deferred until the first inbound message type is needed.
7. **`getVisualComponent()` (future):** returns `null` now; exists for embedded-component subunits — standalone `JFrame`s don't need it.
8. **Non-interaction:** `TEMPLATE_LOADED` is unicast to `"template"` — the variation module does not receive it (see §5.2).

### 5.4 Why `MiniTemplate` was dropped for display

The earlier brainstorm doc proposed `MiniTemplate` — a light container of `GeneralPath` copies pre-scaled by a factor (~0.25) via `AffineTransform.createTransformedShape(...)`, built by `MiniTemplateFactory`.

The analysis in §5.1 changed this: `ShapeDisplayPanel`-style rendering does fit-to-bounds scaling at paint time, so a *baked* scale factor is display-neutral — the panel normalizes bounds into the cell regardless. Baking 0.25 would only be safe with a "raw draw" panel, which is fragile (a 2000px template overflows 145px; a 100px template becomes a speck).

Once fit-to-bounds panels are used, `MiniTemplate` adds a copying step that buys nothing for display — so V1 uses 12 full `Template` instances directly, which also carry their own `DrawObj` colors (ready for per-panel recoloring later). `MiniTemplate`/`MiniTemplateFactory` may still be useful elsewhere (deferred discussion), but play **no role** in this feature's V1.

---

## 6. The Palette Gap — What V1 Does NOT Have, and Required Changes

**Explicit statement:** V1 contains **no mechanism to obtain or apply the 12 variation palettes.** The 12 thumbnails are identical because each renders its own loaded `Template` with that template's original `DrawObj` colors — nothing fetches, stores, or applies any variation palette.

Required modifications to close the gap:

### 6.1 The variation engine (`variationmodule/varyengine`)

- The palette-variation method exists in another project but depends on many interacting classes — porting is deferred until after V1.
- A new package `com.csurf.variationmodule.varyengine` has been created; **all** engine classes go there, mostly under `varyengine.model`.
- **`PalettesThe12Variations` lives in `varyengine.model`** (see §7).

### 6.2 Orchestration change (frame-first order)

Confirmed order (matches the original outline):

1. Chooser → load → **open `VariationDisplayFrame` first**, with the 12 identical loaded templates as placeholders (exactly what V1 does today — V1's end state *is* the future "placeholder" state).
2. `controlPanel` gains parameter controls (contents deferred — part of engine port).
3. Controller sends `GENERATE_VARIATIONS` (parameters as payload) through the mediator to the engine subunit → receives `PalettesThe12Variations` **synchronously** as the `handleMessage` return value (`Object` → cast to `PalettesThe12Variations`).
4. Controller applies palette `i` to `Template` `i`, then repaints panels.

### 6.3 Palette application mechanism

- `DrawObj.setColor(ColorPtOstDot)` (`DrawObj.java:169`) is the mutation point — updates fill and stroke color directly, no AWT `Color` snapping.
- `Template.buildPaletteFromTemplateDrawObjs()` produces a `PaletteWName` whose index matches DrawObj order — the same index-based correspondence `applyPalette` will use in reverse: `drawObjs.get(i).setColor(palette.getColorAt(i))`.
- After `setColor`, the panel just `repaint()`s — `ThumbnailPanel` needs **no change**; it always paints each DrawObj's current color.
- The frame (or controller) must **retain the `List<Template>`** so application can target each panel's DrawObjs — V1 currently passes them into `setDrawObjs` without retaining; a field will be added.
- Palette index `i` ↔ `thumbnailPanel(i+1)` directly (row-major naming now matches add order — no remapping).

### 6.4 Messaging & registration work

- Register `VariationSubunitMediator` as `"variationmodule"` once the first inbound type exists (likely engine replies or external refresh).
- The engine will presumably register its own subunit under `varyengine`; `GENERATE_VARIATIONS` is sent from controller → engine through the shared `ApplicationMediator`.
- `controller.registerController(windowId, controller)` (already in `VariationSubunitMediator`) wires each window into routing.

---

## 7. `PalettesThe12Variations` — Container Analysis

**Decision:** a dedicated class in `com.csurf.variationmodule.varyengine.model`, used as the **DTO returned when requesting the 12 variation palettes**. Implementation details are **not fully decided yet**; this section records the analysis.

### 7.1 Dedicated class vs. `PaletteWName[]`

**For the dedicated class (chosen direction):**

- **Fixed-size invariant:** the feature is defined around *exactly* 12 — a class can enforce `count == 12` at construction; an array cannot.
- **Semantic clarity:** `PalettesThe12Variations` is self-describing in signatures (`handleMessage` returns it, controller consumes it) — `PaletteWName[]` carries no meaning.
- **Index ↔ panel contract:** the class is the natural place to document/centralize "palette `i` ↔ thumbnail panel `i` ↔ `thumbnailPanel(i+1)`" — one place instead of every consumer re-deriving it.
- **Metadata room (key advantage):** future needs — generation parameters (what the `controlPanel` produced), source template name, timestamp, per-variation names/labels — all have a home without changing the type's identity. An array would force a parallel data structure anyway.
- **Message-payload typing:** mediator `Message.payload` is `Object`; a named DTO gives `instanceof`-safe routing (`payload instanceof PalettesThe12Variations`).
- **Consistency with codebase style:** `PaletteWName` is itself a named wrapper around `ColorPtOstDot[]` (defensive copies, `getSize()`, `getColorAt(i)`); a named wrapper around 12 `PaletteWName`s follows the same pattern.
- **Iteration/copies:** single place for defensive-copy semantics and iteration.

**For a plain `PaletteWName[]`:** only simplicity — but since the class is needed anyway as the stable API surface between engine and view, the array offers no real advantage. The class may *internally* still be backed by `PaletteWName[]` or `List<PaletteWName>`.

### 7.2 Undecided implementation details (open for next discussion)

- Internal storage: `PaletteWName[12]` vs `List<PaletteWName>`; mutability (defensive copies vs immutable).
- Whether it holds per-variation metadata (name, generation params) or is a bare ordered container.
- Naming of accessors: `getPaletteAt(int)` / `getVariationCount()` / iterator — TBD.
- Whether it is also the DTO for *saving* a variation set to disk (serialization needs are unexplored).

---

## 8. Known Trade-offs & Caveats (V1)

- **12× load side effects:** 12 `TEMPLATE_LOADED` messages (dead today), 12× deserialization debug output to console. Acceptable for V1.
- **Shared UUIDs:** all 12 copies' DrawObjs carry identical UUIDs — separate containers, no global registry → harmless.
- **Degenerate geometry:** `ThumbnailPanel` renders blank when union bounds have zero width/height (division-by-zero guard).
- **`controlPanel` is an empty `JPanel`** — 60px reserved strip, contents deferred.
- **`VariationSubunitMediator` unregistered** — dead until the engine port wires it.

---

## 9. Open Questions / Deferred Items

- **Palette-size vs. DrawObj-count mismatch policy** — what does `applyPalette` do when a variation palette has fewer/more colors than the template's DrawObj count? Deferred.
- **Engine internals** — which classes the ported method needs; what `controlPanel` parameters will be; whether generation is fast enough for synchronous `handleMessage` return (assumed yes).
- **Subunit registration timing** — `"variationmodule"` registers when the first real inbound message exists.
- **`MiniTemplate`/`MiniTemplateFactory`** — future use discussion deferred; no role in this feature.
- **Thumbnail interactivity** — click-to-select a palette (`VARIATION_PALETTE_SELECTED` → owner frame) — future.
- **Serialization of a `PalettesThe12Variations` set** — unexplored.
- **Orphaned variation windows** when their `TemplateWorkFrame` owner closes — currently independent; cascade-close TBD.

---

## 10. File Inventory (V1)

New files:
- `src/main/java/com/csurf/variationmodule/view/ThumbnailPanel.java`
- `src/main/java/com/csurf/variationmodule/view/VariationDisplayFrame.java`
- `src/main/java/com/csurf/variationmodule/controller/VariationDisplayController.java`
- `src/main/java/com/csurf/variationmodule/controller/VariationSubunitMediator.java`

Modified files:
- `src/main/java/com/csurf/templatecore/view/TemplateWorkFrame.java` — `"Palette variations"` menu item + `openPaletteVariations()` + import.

New empty packages: `com.csurf.variationmodule.model`, `com.csurf.variationmodule.varyengine` (+ `varyengine.model` planned).
