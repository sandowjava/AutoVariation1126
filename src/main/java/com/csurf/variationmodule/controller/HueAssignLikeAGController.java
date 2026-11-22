package com.csurf.variationmodule.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.templatecore.controller.PaletteOperationHub;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.view.TemplateWorkFrame;
import com.csurf.variationmodule.model.AnchorGroup;

/**
 * HueAssignLikeAGController holds the session state and commit logic for the
 * "Go to N hues" workflow driven by HueAssignLikeAGFrame.
 *
 * Session model (incremental approach):
 * - anchorPatternLevel (N): the number of hue groups the user intends to
 *   create, stated at session start (1-4). Chosen by the menu item that
 *   opened the frame.
 * - usedHueNums: the user-facing hue numbers (1-36) assigned by commits in
 *   this session. Because hues are unique per session, a DrawObj's group
 *   membership is derivable from its current color: hue == group identity.
 * - committedGroups: AnchorGroup records of each commit (transient; kept for
 *   potential future AnchorPattern use).
 *
 * Commit semantics:
 * - A "new" hue (not in usedHueNums) creates a new group and requires
 *   usedHueNums.size() < N.
 * - An "existing" hue (in usedHueNums) merges/moves DrawObjs into that group
 *   and is always allowed (last-write-wins re-assignment).
 * - "Remainder" commits recolor every DrawObj whose current hue is not in
 *   usedHueNums; typically used at usedHueNums.size() == N-1 so the user
 *   only selects N-1 groups explicitly.
 *
 * Session completion: usedHueNums.size() == N AND no unassigned DrawObjs
 * remain. The frame stays open after completion for re-assignment commits.
 */
public class HueAssignLikeAGController {

    private final int anchorPatternLevel;
    private final Set<Integer> usedHueNums;
    private final List<AnchorGroup> committedGroups;
    private final TemplateWorkFrame templateWorkFrame;

    /**
     * Creates a controller for one "Go to N hues" session.
     *
     * @param anchorPatternLevel the intended number of hue groups (1-4)
     * @param templateWorkFrame the main frame (source of roster, selection, hub)
     * @throws IllegalArgumentException if anchorPatternLevel is out of range or frame is null
     */
    public HueAssignLikeAGController(int anchorPatternLevel, TemplateWorkFrame templateWorkFrame) {
        if (anchorPatternLevel < 1 || anchorPatternLevel > 4) {
            throw new IllegalArgumentException(
                "AnchorPattern level must be between 1 and 4");
        }
        if (templateWorkFrame == null) {
            throw new IllegalArgumentException("TemplateWorkFrame cannot be null");
        }
        this.anchorPatternLevel = anchorPatternLevel;
        this.templateWorkFrame = templateWorkFrame;
        this.usedHueNums = new HashSet<>();
        this.committedGroups = new ArrayList<>();
    }

    // ============================================================================
    // SESSION STATE QUERIES
    // ============================================================================

    /**
     * @return the anchorPatternLevel (N) stated at session start
     */
    public int getAnchorPatternLevel() {
        return anchorPatternLevel;
    }

    /**
     * @return the number of distinct hues assigned so far in this session
     */
    public int getUsedHueCount() {
        return usedHueNums.size();
    }

    /**
     * @return an unmodifiable view of the user-facing hues assigned by this session
     */
    public Set<Integer> getUsedHueNums() {
        return Collections.unmodifiableSet(usedHueNums);
    }

    /**
     * @return the AnchorGroup commit records for this session
     */
    public List<AnchorGroup> getCommittedGroups() {
        return Collections.unmodifiableList(committedGroups);
    }

    /**
     * Computes the current remainder: every DrawObj whose current hue is not
     * one of the session's used hues. DrawObjs with no color (hue 0) count as
     * unassigned.
     *
     * @return the set of unassigned DrawObjs (may be empty)
     */
    public Set<DrawObj> computeRemainder() {
        Set<DrawObj> remainder = new HashSet<>();
        DrawObjRoster roster = templateWorkFrame.getDrawObjRoster();
        if (roster == null) {
            return remainder;
        }
        for (DrawObj drawObj : roster.getTemplateDrawObjs()) {
            if (drawObj == null) {
                continue;
            }
            if (!usedHueNums.contains(drawObj.getUserFacingHueNum())) {
                remainder.add(drawObj);
            }
        }
        return remainder;
    }

    /**
     * Whether the remainder option should be pre-checked/prompted: the user
     * has committed exactly N-1 groups, so the next commit would normally be
     * for the remainder.
     *
     * @return true if usedHueNums.size() == N-1
     */
    public boolean isRemainderExpected() {
        return usedHueNums.size() == anchorPatternLevel - 1;
    }

    /**
     * Whether the session is complete: all N hues are in use and no
     * unassigned DrawObjs remain (the roster is fully partitioned).
     *
     * @return true if the session is complete
     */
    public boolean isSessionComplete() {
        return usedHueNums.size() == anchorPatternLevel && computeRemainder().isEmpty();
    }

    // ============================================================================
    // COMMIT
    // ============================================================================

    /**
     * Commits one recoloring step: recolors either the current selection
     * (remainder == false) or the computed remainder (remainder == true) to
     * the given user-facing hue.
     *
     * Validation:
     * - A hue not in usedHueNums creates a new group; rejected if all N hues
     *   are already in use.
     * - A hue already in usedHueNums merges/moves the subset into that group
     *   (last-write-wins re-assignment) and is always allowed.
     *
     * @param userFacingHueNum the target hue (1-36)
     * @param useRemainder true to recolor all unassigned DrawObjs, false to
     *                     recolor the current selection snapshot
     * @return null on success, or an error message describing why the commit
     *         was rejected
     */
    public String commit(int userFacingHueNum, boolean useRemainder) {
        DrawObjRoster roster = templateWorkFrame.getDrawObjRoster();
        if (roster == null) {
            return "No template loaded.";
        }

        PaletteOperationHub hub = templateWorkFrame.getPaletteOperationHub();
        if (hub == null) {
            return "PaletteOperationHub is not initialized.";
        }

        if (userFacingHueNum < 1 || userFacingHueNum > 36) {
            return "Hue must be between 1 and 36.";
        }

        boolean newHue = !usedHueNums.contains(userFacingHueNum);
        if (newHue && usedHueNums.size() >= anchorPatternLevel) {
            return "All " + anchorPatternLevel + " hues are already assigned. " +
                   "Choose an existing hue to move DrawObjs between groups.";
        }

        // Determine the subset for this commit
        Set<DrawObj> subset;
        if (useRemainder) {
            subset = computeRemainder();
            if (subset.isEmpty()) {
                return "No unassigned DrawObjs remain.";
            }
        } else {
            if (roster.getSelectionGroup() == null) {
                return "No selection group available.";
            }
            Set<DrawObj> selection = roster.getSelectionGroup().getSelectedDrawObjsSet();
            if (selection == null || selection.isEmpty()) {
                return "No DrawObjs selected. Select DrawObjs in the main window first.";
            }
            // Snapshot so later selection changes cannot affect this commit
            subset = new HashSet<>(selection);
        }

        // Perform the recoloring through the hub (fires COLOR_CHANGE on success)
        HueNumGUI targetHue = HueNumGUI.getHueNumGUIInstance(userFacingHueNum);
        boolean delegated = hub.recolorSubsetToHue(subset, targetHue);
        if (!delegated) {
            return "Recoloring operation was not performed.";
        }

        // Record the commit and update session state
        committedGroups.add(AnchorGroup.fromDrawObjs(subset, targetHue));
        usedHueNums.add(userFacingHueNum);
        reclaimVacatedHues(roster);

        return null; // success
    }

    /**
     * Removes hues from usedHueNums that no longer have any members in the
     * roster (e.g., after a whole group was re-assigned to another hue).
     * A hue with zero members is freed for use as a new group.
     *
     * @param roster the roster to scan for surviving members
     */
    private void reclaimVacatedHues(DrawObjRoster roster) {
        Set<Integer> presentHues = new HashSet<>();
        for (DrawObj drawObj : roster.getTemplateDrawObjs()) {
            if (drawObj != null) {
                presentHues.add(drawObj.getUserFacingHueNum());
            }
        }
        usedHueNums.retainAll(presentHues);
    }
}
