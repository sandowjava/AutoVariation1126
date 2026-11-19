package com.csurf.variationmodule.varyengine.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;


/**
 * The initial content of a scratch's decided layer (D1 + §7.1, Seed-B):
 * an immutable set of decided channels to copy from the caller's state
 * into the scratch before the nested run. The call site states the inner
 * Stratg's dependencies exactly — nothing is inherited by accident.
 *
 * Semantics: snapshot at call time (later live changes do not affect the
 * scratch); {@code UNDECIDED} copies as {@code UNDECIDED}; seeded values
 * never appear in the resulting proposal (only the inner Stratg's own
 * writes do); flags are always copied regardless of the seed (D4).
 *
 * {@code blank()} is the empty set; {@code all()} is {AG, HUE, OSTDOT}.
 * Small immutable value object so a deferred {@code Seed.fromProposal}
 * can be added without breaking callers.
 */
public final class Seed {

    private static final Seed BLANK = new Seed(EnumSet.noneOf(Channel.class));
    private static final Seed ALL = new Seed(EnumSet.allOf(Channel.class));

    private final Set<Channel> channels;

    private Seed(Set<Channel> channels) {
        this.channels = Collections.unmodifiableSet(channels);
    } // end private constructor

    public static Seed blank() {
        return BLANK;
    } // end method blank

    public static Seed all() {
        return ALL;
    } // end method all

    public static Seed of(Channel... channels) {
        EnumSet<Channel> set = EnumSet.noneOf(Channel.class);
        Collections.addAll(set, channels);
        return new Seed(set);
    } // end method of

    public static Seed of(Set<Channel> channels) {
        Objects.requireNonNull(channels, "channels");
        if (channels.isEmpty()) {
            return BLANK;
        } // end IF
        return new Seed(EnumSet.copyOf(channels));
    } // end method of

    public Set<Channel> channels() {
        return channels;
    } // end method channels

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } // end IF
        if (!(o instanceof Seed)) {
            return false;
        } // end IF
        return channels.equals(((Seed) o).channels);
    } // end method equals

    @Override
    public int hashCode() {
        return channels.hashCode();
    } // end method hashCode

    @Override
    public String toString() {
        return "Seed" + channels;
    } // end method toString

} // end class Seed
