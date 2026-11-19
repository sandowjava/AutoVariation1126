package com.csurf.variationmodule.varyengine.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * The narrow seam Stratgs write through (D3), per §10.2 of the
 * nested-execution note. The live {@link VariationRunState}, every
 * scratch, and the {@link RecordingRunState} decorator implement it.
 *
 * Decided reads return {@link Optional}: empty = {@code UNDECIDED} —
 * "no Stratg has written this slot." Stratgs read the decided layer for
 * dependencies; {@code effective*} is for materialization, display, or a
 * Stratg that explicitly wants it (flag → base lookup, else decided).
 *
 * {@link #apply} is a default method so every implementation — live
 * state, scratch, recorder — applies through its own setters, which is
 * what lets the recorder log apply-writes as the outer Stratg's writes.
 */
public interface RunState {

    /** Shape IDs 1..N — the universe for scopes and coverage checks. */
    Set<Integer> shapeIds();

    // ---------- decided-layer reads (empty = UNDECIDED) ----------

    Optional<Integer> ag(int shape);
    Optional<HueNumGUI> hue(int shape);
    Optional<ColorPtOstDot> ostDot(int shape);

    // ---------- override layer ----------

    boolean frozen(int shape);

    /** Effective = base value when frozen, else decided. Empty = UNDECIDED decided with no freeze. */
    Optional<Integer> effectiveAG(int shape);
    Optional<HueNumGUI> effectiveHue(int shape);
    Optional<ColorPtOstDot> effectiveOstDot(int shape);

    // ---------- writes (decisions, never commits — see parent §4.2 invariants) ----------

    void setAG(int shape, int ag);
    void setHue(int shape, HueNumGUI hue);
    void setOstDot(int shape, ColorPtOstDot dot);
    void setFrozen(int shape, boolean frozen);

    // ---------- nested execution ----------

    /**
     * A throwaway run state for a nested run (§10.2): seeded decided
     * channels, a read-only copy of the live flags, the same base.
     * Discarded afterward; must never alias mutable parent data.
     */
    RunState newScratch(Seed seed);

    /**
     * Applies a proposal to this state (D2): writes only in-scope shapes
     * and listed decided channels; absent entries are never written
     * ("absent" means leave the live value alone, never write UNDECIDED).
     *
     * <p>{@link Coverage#REQUIRE_FULL} (the default) fails when the
     * proposal lacks any in-scope (shape, channel) entry —
     * {@link Coverage#ALLOW_PARTIAL} applies what is present.
     *
     * <p>Default implementation writes through {@code setX} so a
     * {@link RecordingRunState} logs apply-writes as the caller's writes.
     */
    default void apply(Proposal proposal, Set<Integer> scope, Set<Channel> channels, Coverage coverage) {
        if (coverage == Coverage.REQUIRE_FULL) {
            List<String> missing = new ArrayList<>();
            for (Channel channel : channels) {
                for (int shape : scope) {
                    if (proposal.get(channel, shape) == null) {
                        missing.add(channel + "@" + shape);
                    } // end IF
                } // end FOR
            } // end FOR
            if (!missing.isEmpty()) {
                throw new IllegalStateException(
                        "apply REQUIRE_FULL: proposal lacks entries for " + missing
                                + " — nothing was applied");
            } // end IF
        } // end IF
        for (Channel channel : channels) {
            for (int shape : scope) {
                Object value = proposal.get(channel, shape);
                if (value != null) {
                    writeChannel(channel, shape, value);
                } // end IF — absent entries are never written
            } // end FOR
        } // end FOR
    } // end method apply

    /** Channel-dispatched write used by {@link #apply}. */
    default void writeChannel(Channel channel, int shape, Object value) {
        switch (channel) {
            case AG:
                setAG(shape, (Integer) value);
                break;
            case HUE:
                setHue(shape, (HueNumGUI) value);
                break;
            case OSTDOT:
                setOstDot(shape, (ColorPtOstDot) value);
                break;
            default:
                throw new IllegalArgumentException("unknown channel " + channel);
        } // end SWITCH
    } // end method writeChannel

} // end interface RunState
