package com.csurf.variationmodule.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.templatecore.model.DrawObj;

/**
 * AnchorGroup represents a group of DrawObjs that have been (or will be)
 * recolored to a single target hue.
 *
 * Purpose:
 * - Commit record for the "Go to N hues" session flow in HueAssignLikeAGFrame
 * - Names the concept of "a group of DrawObjs bound to one hue" for the
 *   future AnchorPattern work in the variation module
 *
 * Data:
 * - memberUUIDs: the DrawObjs in this group (stored by UUID for safety
 *   against roster reordering)
 * - assignedHue: the target hue for the group, as a HueNumGUI so the
 *   user-facing hue number (1-36) and internal code hue (0-35) are
 *   unambiguous
 *
 * Note: group membership is also derivable from each DrawObj's current
 * color hue at any time (hue identity = group identity), so instances of
 * this class may be transient within a session.
 */
public class AnchorGroup {

    private final Set<UUID> memberUUIDs;
    private HueNumGUI assignedHue;

    /**
     * Creates an AnchorGroup with the given member UUIDs and target hue.
     *
     * @param memberUUIDs the UUIDs of the DrawObjs in this group
     * @param assignedHue the target hue (HueNumGUI wraps user-facing 1-36)
     * @throws IllegalArgumentException if memberUUIDs is null or assignedHue is null
     */
    public AnchorGroup(Set<UUID> memberUUIDs, HueNumGUI assignedHue) {
        if (memberUUIDs == null) {
            throw new IllegalArgumentException("Member UUIDs cannot be null");
        }
        if (assignedHue == null) {
            throw new IllegalArgumentException("Assigned hue cannot be null");
        }
        this.memberUUIDs = new HashSet<>(memberUUIDs);
        this.assignedHue = assignedHue;
    }

    /**
     * Creates an AnchorGroup from a set of DrawObjs and a target hue.
     * DrawObjs with null UUIDs are skipped.
     *
     * @param members the DrawObjs in this group
     * @param assignedHue the target hue
     * @return a new AnchorGroup containing the UUIDs of the given DrawObjs
     */
    public static AnchorGroup fromDrawObjs(Set<DrawObj> members, HueNumGUI assignedHue) {
        Set<UUID> uuids = new HashSet<>();
        if (members != null) {
            for (DrawObj drawObj : members) {
                if (drawObj != null && drawObj.getUniqueId() != null) {
                    uuids.add(drawObj.getUniqueId());
                }
            }
        }
        return new AnchorGroup(uuids, assignedHue);
    }

    /**
     * @return an unmodifiable view of the member UUIDs
     */
    public Set<UUID> getMemberUUIDs() {
        return Collections.unmodifiableSet(memberUUIDs);
    }

    /**
     * @return the assigned hue for this group
     */
    public HueNumGUI getAssignedHue() {
        return assignedHue;
    }

    /**
     * Reassigns the target hue for this group (e.g., when a whole group is
     * re-assigned to a different hue during a session).
     *
     * @param assignedHue the new target hue
     */
    public void setAssignedHue(HueNumGUI assignedHue) {
        if (assignedHue == null) {
            throw new IllegalArgumentException("Assigned hue cannot be null");
        }
        this.assignedHue = assignedHue;
    }

    /**
     * @return the number of DrawObjs in this group
     */
    public int size() {
        return memberUUIDs.size();
    }
}
