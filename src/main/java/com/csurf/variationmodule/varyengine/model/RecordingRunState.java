package com.csurf.variationmodule.varyengine.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;

import java.util.*;


/**
 * The write-tracking decorator over any {@link RunState} (D3). The
 * runner wraps a scratch with it before a nested run; every decided
 * write is logged into the {@link Proposal} the run produces.
 *
 * <p>Recorder exactness: it logs <i>writes</i>, not value changes —
 * rewriting a value equal to the seeded one still appears in the
 * proposal (harness case 4).
 *
 * <p>Flag writes (D4 residual): {@link #setFrozen} never reaches the
 * delegate — scratch flags are read-only context. The write is logged
 * and reported to the {@link FlagWriteHook} installed at construction:
 * the runner installs the strict hook, which throws
 * {@value #FLAG_WRITE_ERROR} at the offending write; the lenient
 * factory records silently for tests.
 *
 * <p>Reads pass through. {@code apply} needs no override: the
 * {@link RunState#apply} default writes through {@code setX}, so
 * apply-writes are recorded as the caller's writes (attribution §4.3/7).
 */
public final class RecordingRunState implements RunState {

    /** The specified nested flag-write error (D4 residual, §6.7). */
    public static final String FLAG_WRITE_ERROR =
            "flag writes cannot be harvested; freeze-type Stratgs cannot be nested";

    /** Notified on each {@code setFrozen} attempt against the decorated state. */
    @FunctionalInterface
    public interface FlagWriteHook {
        void onFlagWrite(int shape, boolean frozen);
    } // end interface FlagWriteHook

    private final RunState delegate;
    private final Proposal.Builder writes = Proposal.builder();
    private final List<int[]> flagWrites = new ArrayList<>();   // {shape, frozen?1:0}
    private final FlagWriteHook flagWriteHook;

    private RecordingRunState(RunState delegate, FlagWriteHook flagWriteHook) {
        this.delegate = delegate;
        this.flagWriteHook = flagWriteHook;
    } // end private constructor

    /** Lenient recorder: flag writes are logged, not thrown. For tests/harness. */
    public static RecordingRunState tracking(RunState delegate) {
        return new RecordingRunState(delegate, (shape, frozen) -> { });
    } // end method tracking

    /** Strict recorder — the runner's policy: throws {@link #FLAG_WRITE_ERROR} on any flag write. */
    public static RecordingRunState trackingStrict(RunState delegate) {
        return new RecordingRunState(delegate,
                (shape, frozen) -> { throw new IllegalStateException(FLAG_WRITE_ERROR); });
    } // end method trackingStrict

    /** The proposal: only entries this run actually wrote — no seeded values. */
    public Proposal toProposal() {
        return writes.build();
    } // end method toProposal

    /** Recorded {@code setFrozen} attempts as {shape, 0|1} pairs — never harvested. */
    public List<int[]> flagWrites() {
        return Collections.unmodifiableList(flagWrites);
    } // end method flagWrites

    // ---------- reads: pass through ----------

    @Override
    public Set<Integer> shapeIds() {
        return delegate.shapeIds();
    } // end method shapeIds

    @Override
    public Optional<Integer> ag(int shape) {
        return delegate.ag(shape);
    } // end method ag

    @Override
    public Optional<HueNumGUI> hue(int shape) {
        return delegate.hue(shape);
    } // end method hue

    @Override
    public Optional<ColorPtOstDot> ostDot(int shape) {
        return delegate.ostDot(shape);
    } // end method ostDot

    @Override
    public boolean frozen(int shape) {
        return delegate.frozen(shape);
    } // end method frozen

    @Override
    public Optional<Integer> effectiveAG(int shape) {
        return delegate.effectiveAG(shape);
    } // end method effectiveAG

    @Override
    public Optional<HueNumGUI> effectiveHue(int shape) {
        return delegate.effectiveHue(shape);
    } // end method effectiveHue

    @Override
    public Optional<ColorPtOstDot> effectiveOstDot(int shape) {
        return delegate.effectiveOstDot(shape);
    } // end method effectiveOstDot

    // ---------- decided writes: record + forward ----------

    @Override
    public void setAG(int shape, int ag) {
        writes.ag(shape, ag);
        delegate.setAG(shape, ag);
    } // end method setAG

    @Override
    public void setHue(int shape, HueNumGUI hue) {
        writes.hue(shape, hue);
        delegate.setHue(shape, hue);
    } // end method setHue

    @Override
    public void setOstDot(int shape, ColorPtOstDot dot) {
        writes.ostDot(shape, dot);
        delegate.setOstDot(shape, dot);
    } // end method setOstDot

    // ---------- flag writes: logged, reported, never forwarded (D4) ----------

    @Override
    public void setFrozen(int shape, boolean frozen) {
        flagWrites.add(new int[]{shape, frozen ? 1 : 0});
        flagWriteHook.onFlagWrite(shape, frozen);
    } // end method setFrozen

    // ---------- scratch creation: raw delegate scratch (the runner wraps its own recorder) ----------

    @Override
    public RunState newScratch(Seed seed) {
        return delegate.newScratch(seed);
    } // end method newScratch

    @Override
    public String toString() {
        return "RecordingRunState{delegate=" + delegate + "}";
    } // end method toString

} // end class RecordingRunState
