package csurf.cleandesign.variationmodule.varyengine.model;

import csurf.cleandesign.ostwald.domainmonotri.model.ColorPtOstDot;
import csurf.cleandesign.ostwald.domainmonotri.model.HueNumGUI;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;


/**
 * The per-variation run state — "the sheet" (parent §4.2): one
 * {@link ShapeRunState} per shape plus the shared, read-only
 * <b>base</b> (the bootstrap-produced reference coloring).
 *
 * <p>Two construction forms (Option B bootstrap):
 * <ul>
 *   <li>{@link #withBase(int, VariationResult)} — runs 2–12; frozen
 *       shapes read their effective values from the base.</li>
 *   <li>{@link #noBase(int)} — the bootstrap run (row 1); a frozen
 *       shape's {@code effective*} read is an explicit error — a frozen
 *       flag with no base is illegal.</li>
 * </ul>
 *
 * <p>{@link #newScratch(Seed)} produces the throwaway state for a nested
 * run (§10.2): seeded decided channels, a copy of the flags, the same
 * base — and never an alias to mutable parent data.
 */
public class VariationRunState implements RunState {

    private final Map<Integer, ShapeRunState> shapes;   // keyed 1..N
    private final VariationResult base;                 // null under noBase()

    private VariationRunState(int numShapes, VariationResult base) {
        this.shapes = new LinkedHashMap<>();
        for (int i = 1; i <= numShapes; i++) {
            shapes.put(i, new ShapeRunState());
        } // end FOR
        this.base = base;
    } // end private constructor

    /** Run state for processed runs (2–12): base injected, frozen reads legal. */
    public static VariationRunState withBase(int numShapes, VariationResult base) {
        Objects.requireNonNull(base, "base");
        return new VariationRunState(numShapes, base);
    } // end method withBase

    /** Run state for the bootstrap run (row 1): no base exists yet. */
    public static VariationRunState noBase(int numShapes) {
        return new VariationRunState(numShapes, null);
    } // end method noBase

    /** For fixtures and tests that need direct shape access. */
    public ShapeRunState shape(int shape) {
        ShapeRunState state = shapes.get(shape);
        if (state == null) {
            throw new IllegalArgumentException("shape " + shape + " out of range 1.." + shapes.size());
        } // end IF
        return state;
    } // end method shape

    public boolean hasBase() {
        return base != null;
    } // end method hasBase

    public Optional<VariationResult> getBase() {
        return Optional.ofNullable(base);
    } // end method getBase

    /** Frozen shapes, in shape order — convenience for provenance and checks. */
    public Set<Integer> whichFrozen() {
        Set<Integer> frozen = new LinkedHashSet<>();
        for (Map.Entry<Integer, ShapeRunState> e : shapes.entrySet()) {
            if (e.getValue().isFrozen()) {
                frozen.add(e.getKey());
            } // end IF
        } // end FOR
        return frozen;
    } // end method whichFrozen

    // ---------- RunState: reads ----------

    @Override
    public Set<Integer> shapeIds() {
        return Collections.unmodifiableSet(shapes.keySet());
    } // end method shapeIds

    @Override
    public Optional<Integer> ag(int shape) {
        return shape(shape).getAssignedAG();
    } // end method ag

    @Override
    public Optional<HueNumGUI> hue(int shape) {
        return shape(shape).getAssignedHue();
    } // end method hue

    @Override
    public Optional<ColorPtOstDot> ostDot(int shape) {
        return shape(shape).getChosenOstDot();
    } // end method ostDot

    @Override
    public boolean frozen(int shape) {
        return shape(shape).isFrozen();
    } // end method frozen

    // ---------- RunState: effective reads (flag → base, else decided) ----------

    @Override
    public Optional<Integer> effectiveAG(int shape) {
        return frozen(shape) ? requireBase(shape).ag(shape) : ag(shape);
    } // end method effectiveAG

    @Override
    public Optional<HueNumGUI> effectiveHue(int shape) {
        return frozen(shape) ? requireBase(shape).hue(shape) : hue(shape);
    } // end method effectiveHue

    @Override
    public Optional<ColorPtOstDot> effectiveOstDot(int shape) {
        return frozen(shape) ? requireBase(shape).ostDot(shape) : ostDot(shape);
    } // end method effectiveOstDot

    // ---------- RunState: writes ----------

    @Override
    public void setAG(int shape, int ag) {
        shape(shape).setAssignedAG(ag);
    } // end method setAG

    @Override
    public void setHue(int shape, HueNumGUI hue) {
        shape(shape).setAssignedHue(hue);
    } // end method setHue

    @Override
    public void setOstDot(int shape, ColorPtOstDot dot) {
        shape(shape).setChosenOstDot(dot);
    } // end method setOstDot

    @Override
    public void setFrozen(int shape, boolean frozen) {
        shape(shape).setFrozen(frozen);
    } // end method setFrozen

    // ---------- nested execution ----------

    /**
     * A scratch seeded from this state (§7.1): the decided channels in
     * {@code seed} are copied as a snapshot (UNDECIDED copies as
     * UNDECIDED), flags are always copied (D4), and the scratch shares
     * the parent's base — or the same no-base form inside the bootstrap
     * run. Decided slots hold immutable values, so the copy is shallow.
     */
    @Override
    public RunState newScratch(Seed seed) {
        Objects.requireNonNull(seed, "seed");
        VariationRunState scratch = new VariationRunState(shapes.size(), base);
        for (Map.Entry<Integer, ShapeRunState> e : shapes.entrySet()) {
            int shape = e.getKey();
            ShapeRunState src = e.getValue();
            ShapeRunState dst = scratch.shape(shape);
            if (seed.channels().contains(Channel.AG)) {
                src.getAssignedAG().ifPresent(dst::setAssignedAG);
            } // end IF
            if (seed.channels().contains(Channel.HUE)) {
                src.getAssignedHue().ifPresent(dst::setAssignedHue);
            } // end IF
            if (seed.channels().contains(Channel.OSTDOT)) {
                src.getChosenOstDot().ifPresent(dst::setChosenOstDot);
            } // end IF
            dst.setFrozen(src.isFrozen());   // flags always copied (D4); read-only via recorder
        } // end FOR
        return scratch;
    } // end method newScratch

    // ---------- materialization (palette is a dead-end sink; end of run only) ----------

    /**
     * Builds the {@link VariationResult} from effective values — called
     * once at end of run. Enforces the completeness rule (parent §4.5):
     * every shape must have an effective color; the error names the
     * missing (shape, channel) pair.
     */
    public VariationResult materialize(ArtisticGoalSpec someArtGoalSpec) {
        Objects.requireNonNull(someArtGoalSpec, "someArtGoalSpec");
        Map<Integer, ColorPtOstDot> effective = new LinkedHashMap<>();
        Map<Integer, Integer> decidedAG = new LinkedHashMap<>();
        Map<Integer, HueNumGUI> decidedHue = new LinkedHashMap<>();
        Map<Integer, ColorPtOstDot> decidedDot = new LinkedHashMap<>();
        for (Map.Entry<Integer, ShapeRunState> e : shapes.entrySet()) {
            int shape = e.getKey();
            effectiveOstDot(shape).ifPresent(dot -> effective.put(shape, dot));
            if (!effective.containsKey(shape)) {
                throw new IllegalStateException(
                        "incomplete run: shape " + shape + " has no effective color"
                                + (frozen(shape) ? " (frozen — base lacks OSTDOT for it)" : ""));
            } // end IF
            ag(shape).ifPresent(v -> decidedAG.put(shape, v));
            hue(shape).ifPresent(v -> decidedHue.put(shape, v));
            ostDot(shape).ifPresent(v -> decidedDot.put(shape, v));
        } // end FOR
        return new VariationResult(effective, decidedAG, decidedHue, decidedDot,
                whichFrozen(), someArtGoalSpec);
    } // end method materialize

    private VariationResult requireBase(int shape) {
        if (base == null) {
            throw new IllegalStateException(
                    "frozen shape " + shape + " has no base — a frozen flag"
                            + " under noBase() (bootstrap run) is illegal");
        } // end IF
        return base;
    } // end method requireBase

    @Override
    public String toString() {
        return "VariationRunState{" + shapes.size() + " shapes"
                + (base == null ? ", noBase" : ", withBase") + "}";
    } // end method toString

} // end class VariationRunState
