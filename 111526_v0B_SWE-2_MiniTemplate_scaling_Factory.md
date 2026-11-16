# MiniTemplate Design Brainstorm — Scaling & Factory Analysis

**Date:** 2026-11-15
**Topic:** Design of a new `MiniTemplate` class — a simplified analogue of `Template` used to display palette variations in thumbnail panels (12 thumbnails in one window), derived by uniformly scaling all `DrawObj` shapes of an existing `Template` instance (e.g., 20%, 25%, 35%). Shape counts are arbitrary: typically 8–50, up to ~200. The MiniTemplate is intended to be "dumb": minimal intelligence, functionality, or responsibilities. A user-controllable scaling factor is desired.

---

## 1. Analysis of Existing Code

### `com.csurf.templatecore.model.Template`

- Holds `List<DrawObj> allDrawObjs` (line ~25) — the core collection of shapes.
- Holds a `DrawObjRoster` for selection tracking, which shares a reference to `allDrawObjs` and is kept in sync via `updateToCurrObjState()`.
- Holds `namedSelectionGroups` (`Map<String, SelectionGroup>`), ring angle fields (marked TODO for relocation), and SVG document metadata: `svgViewBox`, `svgWidth`, `svgHeight`, `svgDocumentAttributes`, `originalSvgFilePath`.
- Constructors all funnel through the no-arg `this()`, then `allDrawObjs.addAll(...)`, then `convertColorsToOstDots()`, then `drawObjRoster.updateToCurrObjState()`.
- `convertColorsToOstDots()` snaps every DrawObj's color to the nearest `ColorPtOstDot` via `MonotriBook.getInstance().findClosestOstDot(...)`.
- `buildPaletteFromTemplateDrawObjs()` (line ~887) demonstrates the palette-extraction pattern: iterates `getAllDrawObjs()`, collects each `DrawObj.getCurrentColor()` into a `ColorPtOstDot[]`, wraps it in a `PaletteWName`.
- `getAllDrawObjs()` returns an **unmodifiable** view of the live list.

### `com.csurf.templatecore.model.DrawObj`

- The shape is stored as a **`java.awt.geom.GeneralPath`** (`path` field, line ~22). This is the key fact enabling scaling.
- `getPath()` (line ~137) returns the **live, mutable** `GeneralPath` — not a defensive copy. Transforming it in place would corrupt the source.
- Fill color is a `ColorPtOstDot fillColorOstdot`; stroke color is an AWT `Color` derived from it.
- `drawShape(Graphics2D)` (line ~106) does `g2d.setColor(fillColorOstdot.toAWTColor()); g2d.fill(path);` and additionally strokes the path when `selected == true` — relevant because a thumbnail probably should not render selection strokes.
- Implements `PipeOneColor`: `getShapes()`, `getCurrentColor()`, `setColor(ColorPtOstDot)`, `getPipeUnityId()`.
- **Constructor side effect:** when passed an AWT `Color`, the constructor calls `MonotriBook.getInstance().findClosestOstDot(color)` to snap to the Ostwald palette. Passing a `ColorPtOstDot` directly bypasses the snapping. Every `new DrawObj` also generates a `UUID` and emits log lines.
- `setColor(null)` defaults the fill to internal hue 0 via `MonotriBook` — a hidden fallback worth knowing.
- Each DrawObj also carries SVG round-trip fields (`svgElementType`, `svgOriginalPathData`, `svgAttributes`) — irrelevant for MiniTemplate.

---

## 2. How DrawObj Shapes Are Represented and Contained

- Each `DrawObj` wraps exactly one `GeneralPath` — a serializable sequence of `PathIterator` segment types: `SEG_MOVETO`, `SEG_LINETO`, `SEG_QUADTO`, `SEG_CUBICTO`, `SEG_CLOSE`, each carrying float (x, y) coordinate pairs.
- There is **no arc segment type** and **no stored stroke width or transform** in a `GeneralPath`. Elliptical arcs imported from SVG were already flattened to cubic Béziers when appended. Stroking happens entirely at draw time via `Graphics2D.setStroke(...)`.
- `Template` is the container: a plain `ArrayList<DrawObj>` plus bookkeeping (roster, selection groups, SVG metadata). Shapes are identified by `UUID`, ordered by list position, and carry their own fill color.

---

## 3. Novice-Level Explanation

### 3.1 How a vector shape is stored

A `DrawObj` doesn't store pixels. It stores a `GeneralPath`, which is essentially a list of drawing instructions: *move to point (10, 20), draw a line to (50, 20), draw a curve through control points (60, 30) and (70, 40) ending at (80, 50), close the shape*. Every coordinate in those instructions is a floating-point (x, y) pair. Because the shape is described by math rather than colored dots, making it bigger or smaller is just arithmetic on those numbers — no quality is lost and no blurry edges appear.

### 3.2 What "scaling a closed vector shape" means, step by step

Scaling means multiplying every coordinate by a factor. If the factor is `0.25`:

- **Every point moves toward the origin (0, 0).** A vertex at (100, 200) becomes (25, 50); a vertex at (400, 40) becomes (100, 10).
- **Distances shrink uniformly.** A shape 80 units wide becomes 20 units wide. Crucially, the shape's *position* scales too — a shape sitting at x=100 moves to x=25. The whole collection shrinks *and* pulls together toward the top-left corner while preserving the relative arrangement. That is exactly what a thumbnail wants: a miniature photograph of the original layout.
- **Curves scale and stay smooth.** `GeneralPath` stores curves as quadratic and cubic Bézier segments — each defined by its endpoint plus one or two control points. Scaling multiplies the endpoints *and* the control points by the same factor, producing a curve that is mathematically identical in shape, just smaller. This is the fundamental advantage of vectors over bitmaps.
- **"Closed" shapes need nothing special.** A `closePath()` segment connects the last point back to the first; after scaling, the closure still works because both points were scaled consistently. The winding rule (`WIND_EVEN_ODD` vs `WIND_NON_ZERO`, which decides whether interior holes are filled) is unaffected by scaling.
- **Stroke widths are untouched.** Strokes are not part of `GeneralPath`; they are applied at draw time. A 2-pixel outline stays 2 pixels after scaling the geometry unless the stroke is scaled separately — usually desirable for thumbnails, since a hairline outline scaled to 0.5 px would look wispy.

### 3.3 The two ways to produce a scaled thumbnail in Java

**Option A — scale at paint time (no copies at all).** `Graphics2D` itself can be transformed:

```java
// Inside the thumbnail panel's paintComponent:
Graphics2D g2 = (Graphics2D) g.create();
g2.scale(0.25, 0.25);          // everything drawn after this is 25%
for (DrawObj d : template.getAllDrawObjs()) {
    d.drawShape(g2);           // draws at 1/4 size automatically
}
g2.dispose();
```

The original DrawObjs are never touched. Cheapest possible approach — zero new objects. Downsides: the thumbnail is coupled to the live Template (if the template's shapes move or change, the thumbnail changes), and `drawShape` also renders the selection stroke, which may not be wanted in a thumbnail.

**Option B — bake scaled copies into the MiniTemplate.** Java's `AffineTransform` does the arithmetic:

```java
AffineTransform shrink = AffineTransform.getScaleInstance(0.25, 0.25);
GeneralPath original = drawObj.getPath();
Shape scaled = shrink.createTransformedShape(original);  // new shape, original untouched
GeneralPath scaledPath = new GeneralPath(scaled);        // normalize back to GeneralPath
```

`createTransformedShape` walks every segment — lines, quads, cubics, closes — multiplies every coordinate by 0.25, and returns a brand-new shape. The original `DrawObj`'s path is untouched. The scaled path is then wrapped in a new lightweight object (either a `DrawObj` or a purpose-built `MiniShape`) and stored in the MiniTemplate's list. For 12 thumbnails, create 12 MiniTemplates, each with its own baked copies.

**Recommendation for this project: Option B**, because the spec says the MiniTemplate *contains* its shapes, and each of the 12 thumbnails may show a *different palette* — independent objects are needed so repainting palette A's thumbnail can't disturb palette B's.

### 3.4 Constructor / factory / builder approaches (novice view)

- **Plain constructor:** `new MiniTemplate(sourceTemplate, 0.25)` — the constructor loops the source's DrawObjs, scales each path, copies each color. Simple and sufficient if that is the *only* way a MiniTemplate is ever built. Downside: non-trivial scaling logic ends up inside a class meant to be a dumb data holder.
- **Static factory method:** `MiniTemplate.fromTemplate(source, 0.25)` — same work, but the name documents intent, validation lives in one place, and overloads like `fromTemplate(source, scale, paletteOverride)` can be added later without multiplying constructors.
- **Builder:** `new MiniTemplateBuilder().from(template).scale(0.25).palette(p).build()` — worth it only with 3+ optional inputs (scale factor, palette, name, source reference, padding). For a deliberately dumb class, a builder is probably overkill initially, though it is a clean way to support "user controls the scaling factor" because the caller sets only what it knows.
- **Factory class:** a separate `MiniTemplateFactory` — most useful when the creation logic (scaling) is substantial enough that it shouldn't live inside the data class. This matches the design goal: `MiniTemplate` stays dumb; the factory owns the smarts about deriving it from a `Template`.

---

## 4. Experienced-Level Analysis

### 4.1 Shape model and scaling mechanics

`DrawObj.path` is a `GeneralPath` (`Path2D.Float`). Scaling reduces to a pure affine transform of segment coordinates via `AffineTransform.getScaleInstance(s, s)` + `createTransformedShape(path)`:

- **Geometric fidelity is exact.** Bézier curves transform by transforming control points (affine maps commute with Bézier evaluation), so cubics remain cubics with zero tessellation error. No `FlatteningPathIterator` needed.
- **Topology is preserved.** Winding rule, subpath count, and closure flags carry through; holes stay holes.
- **Transform acts about the origin**, not each shape's own center — positions scale with sizes, preserving relative layout (desired for thumbnails). If scaling *in place* were ever needed (shrink each shape about its own centroid), the recipe is `translate(cx, cy) → scale(s) → translate(-cx, -cy)` per shape using `getBounds2D().getCenterX()/getCenterY()`. Not needed here, but it is the classic scaling gotcha.
- **Degenerate factor:** `s <= 0` is meaningless and `s == 0` makes the transform non-invertible. Validate `0 < s <= 1` in the factory (MiniTemplate is specified to always be smaller than its source).
- **Precision:** `Path2D.Float` at ~25% scale of typical SVG coordinates is fine; `AffineTransform` computes in double and `createTransformedShape` returns float precision — acceptable for thumbnails.
- **Return-type caveat:** `createTransformedShape` may return any `Shape` implementation (typically `Path2D.Float`), so wrap in `new GeneralPath(shape)` to normalize for `DrawObj`'s constructor.

### 4.2 Two relevant quirks of the existing classes

- `DrawObj.getPath()` returns the **mutable** `GeneralPath`, so `path.transform(at)` in place would silently corrupt the source template. Always use `createTransformedShape`, which allocates a new object.
- `new DrawObj(name, path, color)` snaps AWT `Color` inputs through `MonotriBook.findClosestOstDot`; pass the existing `ColorPtOstDot` (`d.getCurrentColor()`) to skip the snap and preserve the exact source color.

### 4.3 Design alternatives for MiniTemplate

**Alternative 1 — bake new DrawObjs (recommended).** MiniTemplate owns `List<DrawObj>` of scaled copies; palette is either a parallel `List<ColorPtOstDot>` or derived via `drawObjs.stream().map(DrawObj::getCurrentColor)`.

- Pros: reuses `drawShape`, `getAWTColor`, `setColor` and the `PipeOneColor` contract; fully decoupled from the source; each of the 12 thumbnails is independent.
- Cons: DrawObj carries baggage it doesn't need (selection flag/stroke, UUID generation, logging, SVG round-trip fields, `MonotriBook` default on `setColor(null)`). All harmless, but noise.

**Alternative 2 — a lean `MiniShape` record.** `record MiniShape(Shape path, ColorPtOstDot color)` — ~30 lines including a `fill(Graphics2D)` helper.

- Pros: purest "dumb data holder"; no DrawObj side effects.
- Cons: re-implements trivial drawing/color plumbing; introduces a parallel type in a codebase already full of DrawObj tooling (sorting strategies, palette builders, exporters) that MiniTemplate could then not reuse.

**Alternative 3 — view-time scaling (no copies).** MiniTemplate stores `(Template source, double scale)` and paints through `g2d.scale(s, s)`.

- Pros: minimal memory (though 200 paths × 12 panels is trivial either way); always in sync with the source.
- Cons: breaks the "contains a collection" requirement; couples thumbnail lifetime to the mutable template; selection stroke bleeds through `drawShape`. Rejected.

### 4.4 Construction strategies compared

| Strategy | Signature | When it pays off |
|---|---|---|
| Constructor | `new MiniTemplate(template, 0.25)` | One-shot, few params; but scaling logic lives in the data class |
| Static factory | `MiniTemplate.fromTemplate(t, s)` | Adds validation + intent-revealing name; cheap upgrade |
| Builder | `builder().from(t).scale(s).palette(p).build()` | Optional palette/name/padding params; supports partial user input; slightly heavier |
| Dedicated factory | `MiniTemplateFactory.create(t, s)` | Scaling algorithm (the one non-trivial piece) lives in one testable place; MiniTemplate stays a dumb container; natural home for `rescale()` support |

### 4.5 Supporting a user-controllable scaling factor

Two strategies for re-scaling:

- **Regenerate:** call `factory.create(template, newFactor)` each time. For ≤200 paths × 12 panels, `createTransformedShape` calls take microseconds — simple, stateless, the best default.
- **Mutate in place:** `miniTemplate.rescale(factor)` re-transforms from the *original* geometry. Requires keeping source references or storing unscaled path snapshots inside MiniTemplate. **Never re-scale an already-scaled path** — float rounding error accumulates across repeated transforms. If this route is chosen, keep a private `List<GeneralPath> sourcePaths` snapshot so the MiniTemplate is self-contained.

### 4.6 Palette design

- Recommended: let each baked DrawObj carry its `ColorPtOstDot`; expose the palette as a derived view (`drawObjs → getCurrentColor()`) consistent with `Template.buildPaletteFromTemplateDrawObjs()`.
- For palette-variation display, expose `applyPalette(List<ColorPtOstDot>)` doing `drawObjs.get(i).setColor(palette.get(i))` — aligned with `PipeOneColor` semantics already in the codebase. Prefer a single bulk `applyPalette` over per-shape setters for thumbnail workflows.
- Guard palette size vs. shape count (apply `min(palette.size(), drawObjs.size())` or throw on mismatch — decide policy explicitly).

### 4.7 Sketch of the factory core

```java
public static MiniTemplate create(Template source, double scaleFactor) {
    if (source == null) throw new IllegalArgumentException("source cannot be null");
    if (scaleFactor <= 0.0 || scaleFactor > 1.0)
        throw new IllegalArgumentException("scaleFactor must be in (0, 1]: " + scaleFactor);

    AffineTransform shrink = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
    List<DrawObj> minis = new ArrayList<>();
    for (DrawObj d : source.getAllDrawObjs()) {
        GeneralPath scaled = new GeneralPath(shrink.createTransformedShape(d.getPath()));
        minis.add(new DrawObj(d.getName(), scaled, d.getCurrentColor())); // OstDot avoids MonotriBook snap
    }
    return new MiniTemplate(minis, scaleFactor);
}
```

### 4.8 Sketch of the MiniTemplate class surface

```java
public class MiniTemplate {
    private final List<DrawObj> drawObjs;   // scaled, baked copies
    private final double scaleFactor;
    private final String sourceTemplateName; // provenance, optional

    // package-private or factory-only constructor
    MiniTemplate(List<DrawObj> scaledDrawObjs, double scaleFactor) { ... }

    public List<DrawObj> getDrawObjs()              { return Collections.unmodifiableList(drawObjs); }
    public double getScaleFactor()                  { return scaleFactor; }
    public int getShapeCount()                      { return drawObjs.size(); }
    public List<ColorPtOstDot> getPalette()         { /* derived from drawObjs */ }
    public void applyPalette(List<ColorPtOstDot> p) { /* setColor per index */ }
    public void drawAll(Graphics2D g2d)             { /* loop drawShape, or fill directly to skip selection stroke */ }
}
```

Notes on `drawAll`: calling `DrawObj.drawShape` strokes selected shapes; since baked copies default `selected=false`, this is safe, but filling `d.getPath()` directly is even dumber and guarantees no stroke leaks.

### 4.9 Additional considerations

- **Bounds/viewport:** after uniform scaling about the origin, the composite bounds shrink correspondingly. `Template`'s `svgViewBox` metadata can be carried over (scaled or kept as-is — the viewBox scales geometry visually anyway). For centering in a thumbnail cell, compute `getBounds2D()` of the union of scaled paths and translate, or simply `g2d.translate(padX, padY)` before drawing.
- **Anti-aliasing:** thumbnails benefit from `g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)` in the panel, not in the model.
- **Memory:** 200 `Path2D.Float` copies × 12 panels is on the order of hundreds of KB at worst — no pooling or sharing needed.
- **Provenance:** storing `sourceTemplateName` (and optionally the source `Template` reference or its `svgViewBox`) is cheap metadata that helps debugging and UI labeling.
- **Serialization:** if MiniTemplates ever need Jackson persistence, `GeneralPath` is not Jackson-friendly — the existing codebase already has `DrawObjSerializer`/`DrawObjDeserializer` machinery that could be reused, but keep MiniTemplate transient for now since it's trivially regenerable.

---

## 5. Recommendation Summary

- **Model:** `MiniTemplate` — dumb container: `List<DrawObj>` of baked scaled copies, `scaleFactor`, optional source name, `getPalette()`, `applyPalette(...)`, `drawAll(Graphics2D)`.
- **Creation:** dedicated `MiniTemplateFactory.create(Template, double)` — owns validation (`0 < s <= 1`), the `AffineTransform` loop, `GeneralPath` normalization, and `ColorPtOstDot` pass-through.
- **Re-scaling:** regenerate via the factory when the user changes the factor (simplest); `rescale(factor)` with stored source-path snapshots is the alternative if profiling ever demands it.
- **Rejected:** view-time-only scaling (violates "contains shapes" requirement, couples to mutable source); lean `MiniShape` record (loses DrawObj tooling reuse for marginal purity); builder pattern (overkill until optional params accumulate).
