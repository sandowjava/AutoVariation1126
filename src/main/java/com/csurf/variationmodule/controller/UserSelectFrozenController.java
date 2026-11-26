package com.csurf.variationmodule.controller;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.view.TemplateWorkFrame;
import com.csurf.variationmodule.model.SmartDrawObj;
import com.csurf.variationmodule.model.SmartPic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Session state and commit logic for the "Select Frozen drawObjs" workflow
 * driven by {@link com.csurf.variationmodule.view.UserSelectFrozenFrame}.
 *
 * <p>The user selects DrawObjs in a scratch TemplateWorkFrame (a fresh copy
 * of the SmartPic's Template); a single commit maps the selection to
 * 1-based shape IDs and delivers it to
 * {@link CreateSmartPicController#setUserFrozenShapes}.
 *
 * <p>Mapping (design doc §3.2): <b>positional</b> — {@code shapeNum =
 * getAllDrawObjs().indexOf(drawObj) + 1}, the same contract SmartPicFactory
 * and loadPaletteColoredTemplates use. The DrawObj UUID ↔
 * {@link SmartDrawObj#drawObjUuid()} match is a cross-check only: pre-v2
 * template files assign fresh UUIDs at load time, so a mismatch is logged,
 * never treated as authoritative.
 *
 * <p>Commit semantics (v1): one-shot — the committed set replaces any
 * previous set. An empty selection commits an empty set (freeze nothing);
 * the frame guards that case with a warning dialog first.
 */
public class UserSelectFrozenController {

    private static final Logger LOG = Logger.getLogger(UserSelectFrozenController.class.getName());

    private final CreateSmartPicController owner;
    private final SmartPic smartPic;
    private final TemplateWorkFrame workFrame;

    /**
     * @param owner     the CreateSmartPicController that receives the committed set
     * @param smartPic  the current SmartPic (validation target for shape IDs)
     * @param workFrame the scratch TemplateWorkFrame hosting the selection
     */
    public UserSelectFrozenController(CreateSmartPicController owner, SmartPic smartPic,
                                      TemplateWorkFrame workFrame) {
        if (owner == null || smartPic == null || workFrame == null) {
            throw new IllegalArgumentException("owner, smartPic, and workFrame are required");
        }
        this.owner = owner;
        this.smartPic = smartPic;
        this.workFrame = workFrame;
    } // end constructor

    /** The number of DrawObjs currently selected in the scratch window. */
    public int selectedCount() {
        Set<DrawObj> selection = currentSelection();
        return selection == null ? 0 : selection.size();
    } // end method selectedCount

    /** The SmartPic's total shape count (for the status line). */
    public int totalShapes() {
        return smartPic.numShapes();
    } // end method totalShapes

    /**
     * Commits the current selection as the frozen set: maps each selected
     * DrawObj to its 1-based shape ID, validates the result against the
     * SmartPic, and hands it to the owning controller.
     *
     * @return null on success, or an error message describing the rejection
     */
    public String commit() {
        if (workFrame.getDrawObjRoster() == null || workFrame.getCurrTemplate() == null) {
            return "No template loaded in the selection window.";
        }
        Set<DrawObj> selection = currentSelection();
        if (selection == null) {
            return "No selection available in the selection window.";
        }

        List<DrawObj> all = workFrame.getCurrTemplate().getAllDrawObjs();
        Map<UUID, Integer> uuidToShape = uuidToShapeNum();
        Set<Integer> shapeNums = new HashSet<>();
        int uuidMismatches = 0;
        for (DrawObj drawObj : selection) {
            int shapeNum = all.indexOf(drawObj) + 1;
            if (shapeNum <= 0) {
                return "A selected DrawObj is not part of this template copy.";
            }
            shapeNums.add(shapeNum);
            Integer byUuid = uuidToShape.get(drawObj.getUniqueId());
            if (byUuid != null && byUuid.intValue() != shapeNum) {
                uuidMismatches++; // positional wins; UUIDs on pre-v2 files are load-time random
            }
        } // end FOR

        if (uuidMismatches > 0) {
            LOG.warning("userSelectFrozen: " + uuidMismatches
                    + " UUID/positional mismatches (pre-v2 template?); positional mapping used");
        }
        if (!smartPic.smartDrawObjs().keySet().containsAll(shapeNums)) {
            return "Selected shapes fall outside the current SmartPic"
                    + " (the template may have changed on disk since it was created).";
        }

        owner.setUserFrozenShapes(shapeNums);
        return null; // success
    } // end method commit

    private Set<DrawObj> currentSelection() {
        DrawObjRoster roster = workFrame.getDrawObjRoster();
        if (roster == null || roster.getSelectionGroup() == null) {
            return null;
        }
        return roster.getSelectionGroup().getSelectedDrawObjsSet();
    } // end method currentSelection

    /** UUID cross-check table: SmartDrawObj.drawObjUuid → shapeNum. */
    private Map<UUID, Integer> uuidToShapeNum() {
        Map<UUID, Integer> map = new HashMap<>();
        for (SmartDrawObj sdo : smartPic.smartDrawObjs().values()) {
            map.put(sdo.drawObjUuid(), sdo.shapeNum());
        } // end FOR
        return map;
    } // end method uuidToShapeNum

} // end class UserSelectFrozenController
