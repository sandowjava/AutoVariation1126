package csurf.cleandesign.variationmodule.varyengine.model.stratgs;

import csurf.cleandesign.variationmodule.varyengine.model.BasicStratgContext;
import csurf.cleandesign.variationmodule.varyengine.model.Facts;
import csurf.cleandesign.variationmodule.varyengine.model.Inputs;
import csurf.cleandesign.variationmodule.varyengine.model.NestedRunner;
import csurf.cleandesign.variationmodule.varyengine.model.Proposal;
import csurf.cleandesign.variationmodule.varyengine.model.PureCore;
import csurf.cleandesign.variationmodule.varyengine.model.RecordingRunState;
import csurf.cleandesign.variationmodule.varyengine.model.RunState;
import csurf.cleandesign.variationmodule.varyengine.model.Seed;
import csurf.cleandesign.variationmodule.varyengine.model.Stratg;
import csurf.cleandesign.variationmodule.varyengine.model.StratgContext;
import csurf.cleandesign.variationmodule.varyengine.model.StratgSpec;

import java.util.Objects;


/**
 * The real {@link NestedRunner} (§5b/§4.1): a context object, <b>bound
 * to one run state</b> — live at engine level, a scratch when bound for
 * an inner Stratg (Context-A: a nested seed comes from the scratch the
 * inner Stratg runs on, not the live state).
 *
 * <p>Two paths behind {@link #run}, invisible to callers:
 * <ul>
 *   <li><b>Fast path</b> — the executable implements {@link PureCore}:
 *       {@code compute(facts, params)} runs directly; no scratch, no
 *       recorder. (The seed is inert there — a core cannot read state,
 *       so blank vs. seeded produce the same proposal.)</li>
 *   <li><b>Scratch path</b> — everything else: a seeded throwaway
 *       scratch wrapped in a strict {@link RecordingRunState}; the
 *       harvested writes become the {@link Proposal}. A flag write
 *       fails with {@link RecordingRunState#FLAG_WRITE_ERROR} (D4).</li>
 * </ul>
 *
 * <p>{@code run} rejects specs that are not {@code isFullySpecified()}
 * (harness case 12). The {@code forceScratch} constructor flag exists
 * for the differential harness: it routes even {@code PureCore}
 * Stratgs through the scratch path so the two paths can be compared.
 */
public final class DefaultNestedRunner implements NestedRunner {

    private final Facts facts;
    private final RunState boundState;
    private final Inputs inputs;
    private final boolean forceScratch;

    private DefaultNestedRunner(Facts facts, RunState boundState, Inputs inputs, boolean forceScratch) {
        this.facts = Objects.requireNonNull(facts, "facts");
        this.boundState = Objects.requireNonNull(boundState, "boundState");
        this.inputs = Objects.requireNonNull(inputs, "inputs");
        this.forceScratch = forceScratch;
    } // end private constructor

    /** Normal binding — used by the engine for live, and internally for scratches. */
    public static DefaultNestedRunner boundTo(Facts facts, RunState state, Inputs inputs) {
        return new DefaultNestedRunner(facts, state, inputs, false);
    } // end method boundTo

    /** Differential-test binding — forces the scratch path even for PureCore Stratgs. */
    public static DefaultNestedRunner boundForcingScratch(Facts facts, RunState state, Inputs inputs) {
        return new DefaultNestedRunner(facts, state, inputs, true);
    } // end method boundForcingScratch

    @Override
    public Proposal run(StratgSpec inner, Seed seed) {
        Objects.requireNonNull(inner, "inner");
        Objects.requireNonNull(seed, "seed");
        if (!inner.isFullySpecified()) {
            throw new IllegalArgumentException(
                    "nested spec " + inner.getStratgType() + " is not fully specified — "
                            + "required params must be bound before run()");
        } // end IF

        Stratg stratg = StratgResolver.resolve(inner.getStratgType());

        // fast path — the core sees only facts + params (D5); seed is inert
        if (!forceScratch && stratg instanceof PureCore) {
            return ((PureCore) stratg).compute(facts, inner);
        } // end IF

        // scratch path — seeded throwaway state + strict write harvest
        RunState scratch = boundState.newScratch(seed);
        RecordingRunState recorder = RecordingRunState.trackingStrict(scratch);
        StratgContext innerCtx = BasicStratgContext.of(facts, recorder, inputs,
                DefaultNestedRunner.boundTo(facts, scratch, inputs));
        stratg.execute(innerCtx, inner);
        return recorder.toProposal();
    } // end method run

} // end class DefaultNestedRunner
