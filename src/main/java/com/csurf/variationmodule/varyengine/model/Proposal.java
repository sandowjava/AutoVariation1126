package com.csurf.variationmodule.varyengine.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;

import java.util.*;


/**
 * An immutable partial map (shape, channel) → value holding only the
 * entries an inner Stratg actually wrote (D3). Decided channels only —
 * AG, HUE, OSTDOT; there is no flags channel (D4).
 *
 * An absent entry means "the inner Stratg did not write this slot" — it
 * must never be readable as a value: typed accessors return
 * {@link Optional#empty()}. This is what resolves stale reads (H3):
 * skipped shapes are absent, not stale.
 *
 * Value types: Integer for AG, {@link HueNumGUI} for HUE,
 * {@link ColorPtOstDot} for OSTDOT.
 */
public final class Proposal {

    private final Map<Channel, Map<Integer, Object>> entries;

    private Proposal(Map<Channel, Map<Integer, Object>> entries) {
        this.entries = entries;
    } // end private constructor

    // ---------- typed accessors: absent means not-written, never a value ----------

    public Optional<Integer> ag(int shape) {
        return Optional.ofNullable((Integer) get(Channel.AG, shape));
    } // end method ag

    public Optional<HueNumGUI> hue(int shape) {
        return Optional.ofNullable((HueNumGUI) get(Channel.HUE, shape));
    } // end method hue

    public Optional<ColorPtOstDot> ostDot(int shape) {
        return Optional.ofNullable((ColorPtOstDot) get(Channel.OSTDOT, shape));
    } // end method ostDot

    /** Raw channel/shape lookup used by {@code apply}. Absent → null. */
    public Object get(Channel channel, int shape) {
        Map<Integer, Object> channelEntries = entries.get(channel);
        return channelEntries == null ? null : channelEntries.get(shape);
    } // end method get

    public boolean has(Channel channel, int shape) {
        return get(channel, shape) != null;
    } // end method has

    /** True when every in-scope shape has an entry on every listed channel. */
    public boolean covers(Set<Integer> shapes, Set<Channel> channels) {
        for (Channel channel : channels) {
            for (int shape : shapes) {
                if (!has(channel, shape)) {
                    return false;
                } // end IF
            } // end FOR
        } // end FOR
        return true;
    } // end method covers

    public Set<Channel> writtenChannels() {
        return Collections.unmodifiableSet(entries.keySet());
    } // end method writtenChannels

    /** Shapes written on the given channel; empty set when the channel is absent. */
    public Set<Integer> writtenShapes(Channel channel) {
        Map<Integer, Object> channelEntries = entries.get(channel);
        return channelEntries == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(channelEntries.keySet());
    } // end method writtenShapes

    public boolean isEmpty() {
        for (Map<Integer, Object> channelEntries : entries.values()) {
            if (!channelEntries.isEmpty()) {
                return false;
            } // end IF
        } // end FOR
        return true;
    } // end method isEmpty

    public int size() {
        int count = 0;
        for (Map<Integer, Object> channelEntries : entries.values()) {
            count += channelEntries.size();
        } // end FOR
        return count;
    } // end method size

    // ---------- builder ----------

    public static Builder builder() {
        return new Builder();
    } // end method builder

    public static final class Builder {

        private final Map<Channel, Map<Integer, Object>> building = new EnumMap<>(Channel.class);

        private Builder() {
        } // end private constructor

        public Builder put(Channel channel, int shape, Object value) {
            Objects.requireNonNull(value, "proposal entries never hold null — absent means not-written");
            building.computeIfAbsent(channel, c -> new LinkedHashMap<>())
                    .put(shape, value);
            return this;
        } // end method put

        public Builder ag(int shape, int ag) {
            return put(Channel.AG, shape, ag);
        } // end method ag

        public Builder hue(int shape, HueNumGUI hue) {
            return put(Channel.HUE, shape, hue);
        } // end method hue

        public Builder ostDot(int shape, ColorPtOstDot dot) {
            return put(Channel.OSTDOT, shape, dot);
        } // end method ostDot

        public Proposal build() {
            Map<Channel, Map<Integer, Object>> snapshot = new EnumMap<>(Channel.class);
            for (Map.Entry<Channel, Map<Integer, Object>> e : building.entrySet()) {
                snapshot.put(e.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(e.getValue())));
            } // end FOR
            return new Proposal(Collections.unmodifiableMap(snapshot));
        } // end method build

    } // end class Builder

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } // end IF
        if (!(o instanceof Proposal)) {
            return false;
        } // end IF
        return entries.equals(((Proposal) o).entries);
    } // end method equals

    @Override
    public int hashCode() {
        return entries.hashCode();
    } // end method hashCode

    @Override
    public String toString() {
        return "Proposal" + entries;
    } // end method toString

} // end class Proposal
