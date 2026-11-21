package com.csurf.huepattern.controller;

import com.csurf.huepattern.model.RMatrixMiniPrelim;
import com.csurf.huepattern.view.AGMemberSelectFrame;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.controller.DrawObjStateViewController;
import com.csurf.templatecore.view.TemplateWorkFrame;

import javax.swing.*;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Controller class for the AGMemberSelectFrame.
 * Responsible for:
 * - Creating the list of DrawObjs that are members of a specific AG and within a hue range
 * - Managing the display of the list
 * - Responding to user input for changing the list
 * - Opening DrawObjStateFrames for selected DrawObjs
 * - Managing selection group operations (add/remove from selection)
 */
public class AGSelectFrameController {
    private static final Logger logger = Logger.getLogger(AGSelectFrameController.class.getName());
    
    private final int agNum;
    private final int currUserFacingHue;
    private final RMatrixMiniPrelim rmatrix;
    private final DrawObjRoster drawObjRoster;
    private final TemplateWorkFrame templateWorkFrame;
    private AGMemberSelectFrame theViewWindow;
    private DrawObj selectedDrawObj;
    private DefaultListModel<DrawObj> listModel;
    
    /**
     * Creates a new AGSelectFrameController.
     *
     * @param agNum The AG number (1-4)
     * @param currUserFacingHue The current user-facing hue (1-36)
     * @param rmatrix The RMatrixMini containing AG assignments
     * @param drawObjRoster The DrawObjRoster to get DrawObjs from
     * @param templateWorkFrame The TemplateWorkFrame for creating DrawObjStateViewControllers
     */
    public AGSelectFrameController(int agNum, int currUserFacingHue, RMatrixMiniPrelim rmatrix, DrawObjRoster drawObjRoster, TemplateWorkFrame templateWorkFrame) {
        this.agNum = agNum;
        this.currUserFacingHue = currUserFacingHue;
        this.rmatrix = rmatrix;
        this.drawObjRoster = drawObjRoster;
        this.templateWorkFrame = templateWorkFrame;
        this.listModel = new DefaultListModel<>();
        
        logger.fine("AGSelectFrameController: Created for AG" + agNum + " with initial hue " + currUserFacingHue);
        
        // Initialize the listModel with AG members in the default hue range
        int defaultCounterCWiseLimit = 2;
        int defaultClockWiseLimit = 2;
        
        // Populate the listModel with AG members in the hue range
        updateDrawObjList(currUserFacingHue, defaultCounterCWiseLimit, defaultClockWiseLimit);
    }
    
    /**
     * Sets the theViewWindow associated with this controller.
     * 
     * @param theViewWindow The AGMemberSelectFrame theViewWindow
     */
    public void setTheViewWindow(AGMemberSelectFrame theViewWindow) {
        this.theViewWindow = theViewWindow;
        
        logger.fine("AGSelectFrameController: View set");
        
        // Update the theViewWindow with the current list if it's not empty
        if (theViewWindow != null && !listModel.isEmpty()) {
            theViewWindow.updateDrawObjList(listModel);
            // Select the first item if available
            if (listModel.size() > 0) {
                selectedDrawObj = listModel.getElementAt(0);
                theViewWindow.updateDrawObjColorPanel(selectedDrawObj);
            }
        }
    }
    
    /**
     * Updates the list of DrawObjs based on the current hue range parameters.
     * Filters AG members by hue range.
     * 
     * @param centralUserFacingHue The central hue number (user-facing, 1-36)
     * @param counterCWiseLimit How many hue steps to include in the counter-clockwise direction
     * @param clockWiseLimit How many hue steps to include in the clockwise direction
     */
    public void updateDrawObjList(int centralUserFacingHue, int counterCWiseLimit, int clockWiseLimit) {
        logger.fine("AGSelectFrameController: Updating list for AG" + agNum + 
                   " with hue " + centralUserFacingHue + 
                   ", CCW=" + counterCWiseLimit + ", CW=" + clockWiseLimit);
        
        // Use HueNumGUI to get the code-facing hue (0-35) from the user-facing hue (1-36)
        HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(centralUserFacingHue);
        int codeFacingHue = hueNumGUI.getInternalCodeHueNum();
        
        // Get AG members and filter by hue range
        List<DrawObj> agMembers = getAGMembersInHueRange(agNum, codeFacingHue, counterCWiseLimit, clockWiseLimit);
        
        // Update the list model
        listModel.clear();
        for (DrawObj drawObj : agMembers) {
            listModel.addElement(drawObj);
        }
        
        logger.fine("AGSelectFrameController: List updated with " + listModel.size() + " AG members in hue range");
        
        // Update the theViewWindow if it exists
        if (theViewWindow != null) {
            theViewWindow.updateDrawObjList(listModel);
            // Clear selection if the list is empty
            if (listModel.isEmpty()) {
                selectedDrawObj = null;
                theViewWindow.updateDrawObjColorPanel(null);
            }
        }
    }
    
    /**
     * Gets all DrawObjs that are members of the specified AG and fall within the hue range.
     * 
     * @param agNum The AG number (1-4)
     * @param centralCodeHue The central hue number (code-facing, 0-35)
     * @param counterCWiseLimit How many hue steps to include in the counter-clockwise direction
     * @param clockWiseLimit How many hue steps to include in the clockwise direction
     * @return A list of DrawObjs that are AG members in the specified hue range
     */
    private List<DrawObj> getAGMembersInHueRange(int agNum, int centralCodeHue, int counterCWiseLimit, int clockWiseLimit) {
        List<DrawObj> result = new java.util.ArrayList<>();
        
        if (rmatrix == null) {
            logger.warning("AGSelectFrameController: RMatrixMini is null");
            return result;
        }
        
        // Get AG members first
        Set<DrawObj> agMembers = rmatrix.getAGMembersSet(agNum);
        
        // Filter: keep only AG members whose hue is in range
        for (DrawObj drawObj : agMembers) {
            if (drawObj != null) {
                // Check if hue is in range
                ColorPtOstDot colorPt = drawObj.getColorPtOstDotFillColor();
                if (colorPt != null) {
                    OstHueColumnStep hcs = colorPt.getOstHCS();
                    if (hcs != null) {
                        int hueNum = hcs.getStdCodeHueNum();
                        if (isHueInRange(hueNum, centralCodeHue, counterCWiseLimit, clockWiseLimit)) {
                            result.add(drawObj);
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Checks if a hue is within the specified range around a central hue.
     * Handles wrap-around at the hue circle boundary (0-35).
     * 
     * @param hueNum The hue to check (code-facing, 0-35)
     * @param centralCodeHue The central hue (code-facing, 0-35)
     * @param counterCWiseLimit How many steps counter-clockwise from central
     * @param clockWiseLimit How many steps clockwise from central
     * @return true if hueNum is within the range, false otherwise
     */
    private boolean isHueInRange(int hueNum, int centralCodeHue, int counterCWiseLimit, int clockWiseLimit) {
        // Calculate the CCW and CW limits
        int ccwLimit = (centralCodeHue - counterCWiseLimit + 36) % 36;
        int cwLimit = (centralCodeHue + clockWiseLimit) % 36;
        
        // Handle normal case (no wrap-around)
        if (ccwLimit <= cwLimit) {
            return hueNum >= ccwLimit && hueNum <= cwLimit;
        } else {
            // Handle wrap-around case (range crosses the 35->0 boundary)
            return hueNum >= ccwLimit || hueNum <= cwLimit;
        }
    }
    
    /**
     * Handles selection of a DrawObj from the list.
     * 
     * @param drawObj The selected DrawObj
     */
    public void handleDrawObjSelection(DrawObj drawObj) {
        selectedDrawObj = drawObj;
        if (theViewWindow != null) {
            theViewWindow.updateDrawObjColorPanel(drawObj);
        }
    }
    
    /**
     * Opens a DrawObjStateFrame for the currently selected DrawObj.
     */
    public void openDrawObjStateFrame() {
        if (selectedDrawObj != null && templateWorkFrame != null) {
            // Create a new DrawObjStateViewController for the selected DrawObj
            DrawObjStateViewController controller = new DrawObjStateViewController(templateWorkFrame, selectedDrawObj);
            // The controller will create and show the frame
            logger.fine("AGSelectFrameController: Opened DrawObjStateFrame for selected DrawObj");
        }
    }
    
    /**
     * Opens DrawObjStateFrames for all DrawObjs in the current list.
     */
    public void openAllDrawObjStateFrames() {
        if (!listModel.isEmpty() && templateWorkFrame != null) {
            // Create a DrawObjStateViewController for each DrawObj in the list
            for (int i = 0; i < listModel.size(); i++) {
                DrawObj drawObj = listModel.getElementAt(i);
                DrawObjStateViewController controller = new DrawObjStateViewController(templateWorkFrame, drawObj);
                // Position each frame slightly offset from the previous one
                if (theViewWindow != null && controller.getStateFrame() != null) {
                    java.awt.Point viewLocation = theViewWindow.getLocation();
                    controller.getStateFrame().setLocation(
                        viewLocation.x + 20 + (int)(Math.random() * 40),
                        viewLocation.y + 20 + (int)(Math.random() * 40)
                    );
                }
            }
            logger.fine("AGSelectFrameController: Opened DrawObjStateFrames for all AG members");
        }
    }


    public int  getAGNum() {
        return agNum;
    } // end method getAGNum()


    
    /**
     * Gets the current user-facing hue.
     * 
     * @return The user-facing hue (1-36)
     */
    public int getCurrUserFacingHue() {
        return currUserFacingHue;
    }
    
    /**
     * Gets the DrawObjRoster.
     * 
     * @return The DrawObjRoster
     */
    public DrawObjRoster getDrawObjRoster() {
        return drawObjRoster;
    }
    
    /**
     * Gets the list model.
     * 
     * @return The DefaultListModel containing the current list of DrawObjs
     */
    public DefaultListModel<DrawObj> getListModel() {
        return listModel;
    }
    
    /**
     * Gets the TemplateWorkFrame.
     * 
     * @return The TemplateWorkFrame
     */
    public TemplateWorkFrame getTemplateWorkFrame() {
        return templateWorkFrame;
    }
}
