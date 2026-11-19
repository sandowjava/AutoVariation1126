package pkgVariationModule;

// import csurf.cleandesign.ostwald.domainmonotri.model.ColorPtOstDot;
// import csurf.cleandesign.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
// import csurf.cleandesign.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.variationmodule.varyengine.model.RunState;
import com.csurf.variationmodule.varyengine.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;


/**
 * Step-3 verification, per Detailed Design Plan step 3:
 * override on/off per channel, decided-under-override, UNDECIDED
 * propagation, recorder exactness, scratch isolation, apply
 * scope/channel masking + coverage, and the nested flag-write error.
 */
class RunStateTest {

    private static final Set<Channel> AG_ONLY = EnumSet.of(Channel.AG);
    private static final Set<Channel> AG_HUE = EnumSet.of(Channel.AG, Channel.HUE);

    // ---------- UNDECIDED + decided reads ----------

    @Test
    void freshStateReadsUndecided() {
        VariationRunState state = VariationRunState.noBase(3);
        Assertions.assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), state.shapeIds());
        Assertions.assertFalse(state.ag(1).isPresent());
        Assertions.assertFalse(state.hue(1).isPresent());
        Assertions.assertFalse(state.ostDot(1).isPresent());
        Assertions.assertFalse(state.frozen(1));
    }

    @Test
    void decidedWritesReadBack() {
        VariationRunState state = VariationRunState.noBase(2);
        HueNumGUI hue9 = HueNumGUI.getHueNumGUIInstance(9);
        state.setAG(1, 4);
        state.setHue(1, hue9);

        Assertions.assertEquals(4, state.ag(1).get().intValue());
        Assertions.assertEquals(hue9, state.hue(1).get());
        Assertions.assertFalse(state.ag(2).isPresent()); // UNDECIDED propagates per shape
    }

    // ---------- effective: flag → base, else decided ----------

    @Test
    void effectiveIsDecidedWhenNotFrozen() {
        VariationResult base = baseResult(2);
        VariationRunState state = VariationRunState.withBase(2, base);
        state.setAG(1, 3);
        state.setHue(1, HueNumGUI.getHueNumGUIInstance(11));

        Assertions.assertEquals(3, state.effectiveAG(1).get().intValue());
        Assertions.assertEquals(11, state.effectiveHue(1).get().getUserFacingHueNum());
    }

    @Test
    void effectiveIsBaseWhenFrozen_decidedLayerPreservedUnderneath() {
        VariationResult base = baseResult(2);   // base decided AG=1, hue=6, dot D1/D2
        VariationRunState state = VariationRunState.withBase(2, base);

        // slice-1 semantics: stratgs decide every shape, then freeze overlays
        state.setAG(1, 4);
        state.setHue(1, HueNumGUI.getHueNumGUIInstance(20));
        state.setOstDot(1, dot(10, 2));
        state.setFrozen(1, true);

        // effective reads come from the base across all three channels
        Assertions.assertEquals(1, state.effectiveAG(1).get().intValue());
        Assertions.assertEquals(6, state.effectiveHue(1).get().getUserFacingHueNum());
        Assertions.assertEquals(base.ostDot(1).get(), state.effectiveOstDot(1).get());

        // decided layer preserved underneath — visible on unfreeze
        state.setFrozen(1, false);
        Assertions.assertEquals(4, state.effectiveAG(1).get().intValue());
        Assertions.assertEquals(20, state.effectiveHue(1).get().getUserFacingHueNum());

        Assertions.assertTrue(state.whichFrozen().isEmpty());
    }

    @Test
    void whichFrozenTracksFlags() {
        VariationRunState state = VariationRunState.noBase(4);
        state.setFrozen(2, true);
        state.setFrozen(4, true);
        Assertions.assertEquals(new HashSet<>(Arrays.asList(2, 4)), state.whichFrozen());
    }

    // ---------- noBase: frozen effective read is illegal ----------

    @Test
    void frozenEffectiveReadUnderNoBaseThrows() {
        VariationRunState bootstrap = VariationRunState.noBase(2);
        bootstrap.setFrozen(1, true);      // anomalous input, but the read must fail loudly
        Assertions.assertThrows(IllegalStateException.class, () -> bootstrap.effectiveAG(1));
        Assertions.assertThrows(IllegalStateException.class, () -> bootstrap.effectiveHue(1));
        Assertions.assertThrows(IllegalStateException.class, () -> bootstrap.effectiveOstDot(1));

        // unfrozen effective still reads the decided layer
        Assertions.assertFalse(bootstrap.effectiveAG(2).isPresent());
    }

    // ---------- scratch: seeding + isolation ----------

    @Test
    void scratchSeedsOnlyTheListedChannels_copiesFlags_sharesBase() {
        VariationResult base = baseResult(3);
        VariationRunState live = VariationRunState.withBase(3, base);
        live.setAG(1, 4);
        live.setAG(2, 2);
        live.setHue(1, HueNumGUI.getHueNumGUIInstance(9));
        live.setOstDot(1, dot(5, 1));
        live.setFrozen(3, true);

        RunState scratch = live.newScratch(Seed.of(Channel.AG));

        // AG seeded (snapshot); HUE/OSTDOT stay UNDECIDED; flags copied regardless of seed
        Assertions.assertEquals(4, scratch.ag(1).get().intValue());
        Assertions.assertEquals(2, scratch.ag(2).get().intValue());
        Assertions.assertFalse(scratch.hue(1).isPresent());
        Assertions.assertFalse(scratch.ostDot(1).isPresent());
        Assertions.assertTrue(scratch.frozen(3));
        Assertions.assertFalse(scratch.frozen(1));

        // blank seed: nothing decided, flags still copied
        RunState blank = live.newScratch(Seed.blank());
        Assertions.assertFalse(blank.ag(1).isPresent());
        Assertions.assertTrue(blank.frozen(3));
    }

    @Test
    void scratchIsolation_writesNeverReachLive() {
        VariationRunState live = VariationRunState.noBase(2);
        live.setAG(1, 4);

        RunState scratch = live.newScratch(Seed.of(Channel.AG));
        scratch.setAG(1, 1);                     // rewrite seeded value in scratch
        scratch.setAG(2, 2);                     // write a new slot in scratch
        scratch.setHue(2, HueNumGUI.getHueNumGUIInstance(30));

        Assertions.assertEquals(4, live.ag(1).get().intValue()); // live untouched
        Assertions.assertFalse(live.ag(2).isPresent());
        Assertions.assertFalse(live.hue(2).isPresent());
    }

    @Test
    void scratchInsideBootstrapInheritsNoBase() {
        VariationRunState bootstrap = VariationRunState.noBase(1);
        RunState scratch = bootstrap.newScratch(Seed.all());
        scratch.setFrozen(1, true);
        Assertions.assertThrows(IllegalStateException.class, () -> scratch.effectiveAG(1));
    }

    // ---------- recorder exactness (D3) ----------

    @Test
    void recorderLogsWritesNotValueChanges_equalRewriteStillAppears() {
        VariationRunState live = VariationRunState.noBase(3);
        live.setAG(1, 4);                          // seed source
        live.setAG(2, 2);
        live.setFrozen(3, true);

        RunState scratch = live.newScratch(Seed.of(Channel.AG));
        RecordingRunState recorder = RecordingRunState.tracking(scratch);

        recorder.setAG(1, 4);                      // rewrite a value EQUAL to the seed — must still appear
        // shape 2 seeded but never rewritten → absent
        recorder.setHue(2, HueNumGUI.getHueNumGUIInstance(9));

        Proposal p = recorder.toProposal();
        Assertions.assertEquals(4, p.ag(1).get().intValue());   // exact write, not a diff
        Assertions.assertFalse(p.ag(2).isPresent());            // seeded ≠ written
        Assertions.assertEquals(9, p.hue(2).get().getUserFacingHueNum());
        Assertions.assertFalse(p.ostDot(2).isPresent());

        // copied flag is context only — never in a proposal
        Assertions.assertFalse(p.has(Channel.AG, 3));
        Assertions.assertTrue(p.ag(3).isEmpty());
        Assertions.assertEquals(EnumSet.of(Channel.AG, Channel.HUE), p.writtenChannels());
    }

    // ---------- nested flag writes (D4 residual) ----------

    @Test
    void strictRecorderThrowsOnFlagWrite_andScratchFlagsUntouched() {
        VariationRunState live = VariationRunState.noBase(2);
        RunState scratch = live.newScratch(Seed.blank());
        RecordingRunState strict = RecordingRunState.trackingStrict(scratch);

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> strict.setFrozen(1, true));
        Assertions.assertEquals(RecordingRunState.FLAG_WRITE_ERROR, thrown.getMessage());

        Assertions.assertFalse(scratch.frozen(1));   // write never reached the scratch
        Assertions.assertEquals(1, strict.flagWrites().size()); // but it was logged
    }

    // ---------- apply: scope / channels / coverage (D2) ----------

    @Test
    void applyRequireFullFailsAndAppliesNothing() {
        VariationRunState live = VariationRunState.noBase(2);
        Proposal partial = Proposal.builder().ag(1, 4).build();   // shape 2 missing

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> live.apply(partial, live.shapeIds(), AG_ONLY, Coverage.REQUIRE_FULL));
        Assertions.assertTrue(thrown.getMessage().contains("AG@2"));

        Assertions.assertFalse(live.ag(1).isPresent());   // nothing applied on failure
    }

    @Test
    void applyAllowPartialAppliesPresentEntries_leavesAbsentAlone() {
        VariationRunState live = VariationRunState.noBase(2);
        live.setAG(2, 3);                                   // pre-existing live value
        Proposal partial = Proposal.builder().ag(1, 4).build();

        live.apply(partial, live.shapeIds(), AG_ONLY, Coverage.ALLOW_PARTIAL);

        Assertions.assertEquals(4, live.ag(1).get().intValue());
        Assertions.assertEquals(3, live.ag(2).get().intValue()); // absent → left alone, never UNDECIDED-written
    }

    @Test
    void applyChannelMaskingDropsUnlistedChannels() {
        VariationRunState live = VariationRunState.noBase(1);
        Proposal rich = Proposal.builder()
                .ag(1, 4)
                .hue(1, HueNumGUI.getHueNumGUIInstance(9))
                .build();

        live.apply(rich, live.shapeIds(), AG_ONLY, Coverage.REQUIRE_FULL); // mask: AG only

        Assertions.assertEquals(4, live.ag(1).get().intValue());
        Assertions.assertFalse(live.hue(1).isPresent());   // HUE present in proposal, dropped by mask
    }

    @Test
    void applyScopeRestrictsWhichShapesAreTaken() {
        VariationRunState live = VariationRunState.noBase(2);
        Proposal pool = Proposal.builder().ag(1, 4).ag(2, 2).build();

        live.apply(pool, Collections.singleton(2), AG_ONLY, Coverage.REQUIRE_FULL);

        Assertions.assertFalse(live.ag(1).isPresent());
        Assertions.assertEquals(2, live.ag(2).get().intValue());
    }

    @Test
    void proposalAccessorsNeverReturnPlausibleValuesForAbsent() {
        Proposal p = Proposal.builder().ag(1, 4).build();
        Assertions.assertTrue(p.ag(2).isEmpty());
        Assertions.assertTrue(p.hue(1).isEmpty());
        Assertions.assertTrue(p.covers(Collections.singleton(1), AG_ONLY));
        Assertions.assertFalse(p.covers(new HashSet<>(Arrays.asList(1, 2)), AG_ONLY));
        Assertions.assertFalse(p.covers(Collections.singleton(1), AG_HUE));
    }

    // ---------- materialization ----------

    @Test
    void materializeFailsNamingShapeWithoutEffectiveColor() {
        VariationRunState state = VariationRunState.noBase(2);
        state.setOstDot(1, dot(0, 1));
        // shape 2 left undecided and unfrozen

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
                () -> state.materialize(spec(1)));
        Assertions.assertTrue(thrown.getMessage().contains("shape 2"));
    }

    @Test
    void materializeProducesEffectiveAndDecidedLayers() {
        VariationRunState state = VariationRunState.noBase(2);
        ColorPtOstDot d1 = dot(0, 1), d2 = dot(10, 2);
        state.setAG(1, 4);
        state.setAG(2, 2);
        state.setHue(1, HueNumGUI.getHueNumGUIInstance(9));
        state.setOstDot(1, d1);
        state.setOstDot(2, d2);
        state.setFrozen(2, false);

        VariationResult result = state.materialize(spec(1));

        Assertions.assertEquals(2, result.effectiveColors().size());
        Assertions.assertEquals(d1, result.ostDot(1).get());
        Assertions.assertEquals(4, result.ag(1).get().intValue());
        Assertions.assertEquals(9, result.hue(1).get().getUserFacingHueNum());
        Assertions.assertTrue(result.frozenShapes().isEmpty());
        Assertions.assertEquals(spec(1), result.getArtGoalSpec());
    }

    @Test
    void frozenShapeMaterializesBaseColorWithDecidedProvenance() {
        VariationResult base = baseResult(2);
        VariationRunState state = VariationRunState.withBase(2, base);
        state.setAG(1, 4);                       // decided underneath the freeze
        state.setFrozen(1, true);
        state.setOstDot(2, dot(10, 2));          // shape 2 decided normally

        VariationResult result = state.materialize(spec(1));

        // effective color of the frozen shape = the base's (parity)
        Assertions.assertEquals(base.ostDot(1).get(), result.ostDot(1).get());
        // provenance = the decided value underneath (matches legacy palette arrays)
        Assertions.assertEquals(4, result.ag(1).get().intValue());
        Assertions.assertEquals(Collections.singleton(1), result.frozenShapes());
    }

    // ---------- fixtures ----------

    /** A real catalog dot: codeHue 0-35, fillOrder. Catalog builds once (singleton). */
    private static ColorPtOstDot dot(int codeHue, int fillOrder) {
        return MonotriBook.getInstance().getStandardColorByCodeHueNum(codeHue, fillOrder);
    }

    private static ArtisticGoalSpec spec(int row) {
        PartialGoalSpec goal = new PartialGoalSpec(PartialGoalType.PG2)
                .addStratgSpec(new StratgSpec(StratgType.ASSIGN_AGS)
                        .bind(StratgParam.CONTROL_LEVEL, 4));
        return new ArtisticGoalSpec(Arrays.asList(goal), row);
    }

    /** A base result for two shapes: decided AG=1, hue=6, dots by fixture. */
    private static VariationResult baseResult(int numShapes) {
        Map<Integer, ColorPtOstDot> effective = new LinkedHashMap<>();
        Map<Integer, Integer> decidedAG = new LinkedHashMap<>();
        Map<Integer, HueNumGUI> decidedHue = new LinkedHashMap<>();
        Map<Integer, ColorPtOstDot> decidedDot = new LinkedHashMap<>();
        for (int i = 1; i <= numShapes; i++) {
            ColorPtOstDot dot = dot(i - 1, 1);
            effective.put(i, dot);
            decidedAG.put(i, 1);
            decidedHue.put(i, HueNumGUI.getHueNumGUIInstance(6));
            decidedDot.put(i, dot);
        } // end FOR
        return new VariationResult(effective, decidedAG, decidedHue, decidedDot,
                Collections.emptySet(), spec(1));
    }

} // end class RunStateTest
