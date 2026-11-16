package com.csurf.templatecore.controller;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.model.HueRelativesGroup;
import com.csurf.templatecore.view.HueRelativesFrame;
import com.csurf.templatecore.view.TemplateWorkFrame;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Controller class for the HueRelativesFrame.
 * Responsible for:
 * - Creating the list of DrawObjs based on hue range
 * - Managing the display of the list
 * - Responding to user input for changing the list
 * - Opening DrawObjStateFrames for selected DrawObjs
 */
public class HueRelativesViewController {
    private final DrawObjRoster drawObjRoster;
    private final TemplateWorkFrame templateWorkFrame;
    private HueRelativesFrame view;
    private HueRelativesGroup currentGroup;
    private DrawObj selectedDrawObj;
    private DefaultListModel<DrawObj> listModel;
    private Color[] hueColorLookup;
    private boolean hueColorLookupInitialized = false;
    private Color[] hueColorLookupSelected;
    private boolean hueColorLookupSelectedInitialized = false;
    
    /**
     * Creates a new HueRelativesViewController.
     *
     * @param currUserFacingHue The current user-facing hue number (1-36)
     * @param drawObjRoster The DrawObjRoster to get DrawObjs from
     * @param templateWorkFrame The TemplateWorkFrame for creating DrawObjStateViewControllers
     */
    public HueRelativesViewController(int currUserFacingHue, DrawObjRoster drawObjRoster, TemplateWorkFrame templateWorkFrame) {
        this.drawObjRoster = drawObjRoster;
        this.templateWorkFrame = templateWorkFrame;
        this.listModel = new DefaultListModel<>();
        
        // Initialize the listModel with DrawObjs from a default hue range
        HueNumGUI currHueNumGUI = HueNumGUI.getHueNumGUIInstance(currUserFacingHue);
        int centralCodeHueNum = currHueNumGUI.getInternalCodeHueNum(); // Code-facing hue (0-35) for hue 1
        int defaultCounterCWiseLimit = 1;
        int defaultClockWiseLimit = 1;
        
        // Get the initial HueRelativesGroup
        currentGroup = getHueRelativesGroup(centralCodeHueNum, defaultCounterCWiseLimit, defaultClockWiseLimit);
        
        // Populate the listModel with DrawObjs from the group
        if (currentGroup != null && currentGroup.getDrawObjs() != null && !currentGroup.getDrawObjs().isEmpty()) {
            for (DrawObj drawObj : currentGroup.getDrawObjs()) {
                listModel.addElement(drawObj);
            }
        } else {
            System.out.println("Warning: No DrawObjs found in the specified hue range during HueRelativesViewController initialization");
        }
    } // end constructor



    
    /**
     * Sets the view associated with this controller.
     * 
     * @param view The HueRelativesFrame view
     */
    public void setView(HueRelativesFrame view) {
        this.view = view;
        
        // Update the view with the current list if it's not empty
        if (view != null && !listModel.isEmpty()) {
            view.updateDrawObjList(listModel);
            // Select the first item if available
            if (listModel.size() > 0) {
                selectedDrawObj = listModel.getElementAt(0);
                view.updateDrawObjColorPanel(selectedDrawObj);
            }
        }
    }
    
    /**
     * Creates a HueRelativesGroup containing all DrawObj instances whose hue falls within
     * a specified range around a central hue.
     * 
     * @param centralCodeStdHue The central hue number (code-facing, 0-35)
     * @param counterCWiseLimit How many hue steps to include in the counter-clockwise direction
     * @param clockWiseLimit How many hue steps to include in the clockwise direction
     * @return A HueRelativesGroup containing all DrawObj instances with hues in the specified range
     */
    public HueRelativesGroup getHueRelativesGroup(int centralCodeStdHue, int counterCWiseLimit, int clockWiseLimit) {
        HueRelativesGroup hueRelativesGroup = new HueRelativesGroup(centralCodeStdHue, counterCWiseLimit, clockWiseLimit);
        
        if (drawObjRoster != null) {
            List<DrawObj> allDrawObjs = drawObjRoster.getTemplateDrawObjs();
            if (allDrawObjs != null) {
                for (DrawObj drawObj : allDrawObjs) {
                    if (drawObj != null) {
                        ColorPtOstDot colorPt = drawObj.getColorPtOstDotFillColor();
                        if (colorPt != null) {
                            OstHueColumnStep hcs = colorPt.getOstHCS();
                            if (hcs != null) {
                                int hueNum = hcs.getStdCodeHueNum();
                                if (hueRelativesGroup.isHueInRange(hueNum)) {
                                    hueRelativesGroup.addDrawObj(drawObj);
                                }
                            }
                        }
                    }
                }
            } else {
                System.out.println("Warning: templateDrawObjs is null in DrawObjRoster when called from HueRelativesViewController.getHueRelativesGroup");
            }
        } else {
            System.out.println("Warning: drawObjRoster is null when called from HueRelativesViewController.getHueRelativesGroup");
        }
        
        return hueRelativesGroup;
    }
    
    /**
     * Updates the list of DrawObjs based on the current hue range parameters.
     * 
     * @param centralUserFacingHue The central hue number (user-facing, 1-36)
     * @param counterCWiseLimit How many hue steps to include in the counter-clockwise direction
     * @param clockWiseLimit How many hue steps to include in the clockwise direction
     */
    public void updateDrawObjList(int centralUserFacingHue, int counterCWiseLimit, int clockWiseLimit) {
        updateDrawObjList(centralUserFacingHue, counterCWiseLimit, clockWiseLimit, true);
    }
    
    /**
     * Updates the list of DrawObjs based on the current hue range parameters and sort order.
     * 
     * @param centralUserFacingHue The central hue number (user-facing, 1-36)
     * @param counterCWiseLimit How many hue steps to include in the counter-clockwise direction
     * @param clockWiseLimit How many hue steps to include in the clockwise direction
     * @param isAscendingOrder true for A-Z (ascending) order, false for Z-A (descending) order
     */
    public void updateDrawObjList(int centralUserFacingHue, int counterCWiseLimit, int clockWiseLimit, boolean isAscendingOrder) {
        updateDrawObjList(centralUserFacingHue, counterCWiseLimit, clockWiseLimit, isAscendingOrder, false);
    }
    
    /**
     * Updates the list of DrawObjs based on the current hue range parameters and sort order.
     * 
     * @param centralUserFacingHue The central hue number (user-facing, 1-36)
     * @param counterCWiseLimit How many hue steps to include in the counter-clockwise direction
     * @param clockWiseLimit How many hue steps to include in the clockwise direction
     * @param isAscendingOrder true for A-Z (ascending) order, false for Z-A (descending) order
     * @param isSortByHue true to sort by hue number, false to sort by name
     */
    public void updateDrawObjList(int centralUserFacingHue, int counterCWiseLimit, int clockWiseLimit, boolean isAscendingOrder, boolean isSortByHue) {
        // Use HueNumGUI to get the code-facing hue (0-35) from the user-facing hue (1-36)
        HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(centralUserFacingHue);
        int codeFacingHue = hueNumGUI.getInternalCodeHueNum();
        
        // Get the HueRelativesGroup for the specified range
        currentGroup = getHueRelativesGroup(codeFacingHue, counterCWiseLimit, clockWiseLimit);
        
        // Update the list model
        listModel.clear();
        List<DrawObj> drawObjs = new java.util.ArrayList<>(currentGroup.getDrawObjs());
        
        // Sort the list based on the sort criteria
        if (isSortByHue) {
            // Sort by hue number (primary), then by name A-Z (secondary)
            drawObjs.sort((obj1, obj2) -> {
                int hue1 = obj1.getColorPtOstDotFillColor().getOstHCS().getStdCodeHueNum();
                int hue2 = obj2.getColorPtOstDotFillColor().getOstHCS().getStdCodeHueNum();
                
                // Primary sort: by hue
                int hueCompare = Integer.compare(hue1, hue2);
                if (hueCompare != 0) {
                    return hueCompare;
                }
                
                // Secondary sort: by name A-Z (within same hue)
                return obj1.getName().compareToIgnoreCase(obj2.getName());
            });
        } else {
            // Sort by name
            if (isAscendingOrder) {
                drawObjs.sort((obj1, obj2) -> obj1.getName().compareToIgnoreCase(obj2.getName()));
            } else {
                drawObjs.sort((obj1, obj2) -> obj2.getName().compareToIgnoreCase(obj1.getName()));
            }
        }
        
        // Add sorted DrawObjs to the list model
        for (DrawObj drawObj : drawObjs) {
            listModel.addElement(drawObj);
        }
        
        // Update the view if it exists
        if (view != null) {
            view.updateDrawObjList(listModel);
            // Clear selection if the list is empty
            if (listModel.isEmpty()) {
                selectedDrawObj = null;
                view.updateDrawObjColorPanel(null);
            }
        }
    } // end method updateDrawObjList


    
    /**
     * Handles selection of a DrawObj from the list.
     * 
     * @param drawObj The selected DrawObj
     */
    public void handleDrawObjSelection(DrawObj drawObj) {
        selectedDrawObj = drawObj;
        if (view != null) {
            view.updateDrawObjColorPanel(drawObj);
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
        }
    }
    
    /**
     * Opens DrawObjStateFrames for all DrawObjs in the current group.
     */
    public void openAllDrawObjStateFrames() {
        if (currentGroup != null && !currentGroup.getDrawObjs().isEmpty() && templateWorkFrame != null) {
            // Create a DrawObjStateViewController for each DrawObj in the group
            for (DrawObj drawObj : currentGroup.getDrawObjs()) {
                DrawObjStateViewController controller = new DrawObjStateViewController(templateWorkFrame, drawObj);
                // Position each frame slightly offset from the previous one
                if (view != null && controller.getStateFrame() != null) {
                    Point viewLocation = view.getLocation();
                    controller.getStateFrame().setLocation(
                        viewLocation.x + 20 + (int)(Math.random() * 40),
                        viewLocation.y + 20 + (int)(Math.random() * 40)
                    );
                }
            }
        }
    }
    
    /**
     * Gets the list model for the DrawObj list.
     * 
     * @return The DefaultListModel containing DrawObjs
     */
    public DefaultListModel<DrawObj> getListModel() {
        return listModel;
    }
    
    /**
     * Gets the currently selected DrawObj.
     * 
     * @return The selected DrawObj, or null if none is selected
     */
    public DrawObj getSelectedDrawObj() {
        return selectedDrawObj;
    }
    
    /**
     * Gets the DrawObjRoster associated with this controller.
     * 
     * @return The DrawObjRoster
     */
    public DrawObjRoster getDrawObjRoster() {
        return drawObjRoster;
    }
    
    /**
     * Gets the TemplateWorkFrame associated with this controller.
     * 
     * @return The TemplateWorkFrame
     */
    public TemplateWorkFrame getTemplateWorkFrame() {
        return templateWorkFrame;
    }
    
    /**
     * Initializes the hue color lookup table on first use.
     * Creates a Color array with 36 elements (one for each hue, 0-35).
     * Each element contains the color at fillOrder=31 for that hue.
     * This is a one-time initialization for efficient color lookup.
     */
    private void initializeHueColorLookup() {
        if (hueColorLookupInitialized) {
            return;
        }
        
        hueColorLookup = new Color[36];
        MonotriBook monotriBook = MonotriBook.getInstance();
        
        for (int codeHueNum = 0; codeHueNum < 36; codeHueNum++) {
            try {
                ColorPtOstDot colorPt = monotriBook.getStandardColorByCodeHueNum(codeHueNum, 31);
                if (colorPt != null) {
                    hueColorLookup[codeHueNum] = colorPt.toAWTColor();
                } else {
                    hueColorLookup[codeHueNum] = Color.LIGHT_GRAY;
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not get color for hue " + codeHueNum + " at fillOrder 31: " + e.getMessage());
                hueColorLookup[codeHueNum] = Color.LIGHT_GRAY;
            }
        }
        
        hueColorLookupInitialized = true;
    }
    
    /**
     * Initializes the selected highlight color lookup table on first use.
     * Creates a Color array with 36 elements (one for each hue, 0-35).
     * Each element contains the color at fillOrder=22 for that hue.
     * This is a one-time initialization for efficient color lookup.
     * The color at fillOrder=22 is darker than fillOrder=31, very saturated, providing good contrast for selection highlighting.
     */
    private void initializeHueColorLookupSelected() {
        if (hueColorLookupSelectedInitialized) {
            return;
        }
        
        hueColorLookupSelected = new Color[36];
        MonotriBook monotriBook = MonotriBook.getInstance();
        
        for (int codeHueNum = 0; codeHueNum < 36; codeHueNum++) {
            try {
                ColorPtOstDot colorPt = monotriBook.getStandardColorByCodeHueNum(codeHueNum, 22);
                if (colorPt != null) {
                    hueColorLookupSelected[codeHueNum] = colorPt.toAWTColor();
                } else {
                    hueColorLookupSelected[codeHueNum] = Color.LIGHT_GRAY;
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not get color for hue " + codeHueNum + " at fillOrder 22: " + e.getMessage());
                hueColorLookupSelected[codeHueNum] = Color.LIGHT_GRAY;
            }
        }
        
        hueColorLookupSelectedInitialized = true;
    }
    
    /**
     * Gets the background color for a DrawObj list item based on its hue.
     * The color is the standard color at fillOrder=31 for that hue.
     * Uses a lookup table for efficient O(1) access.
     * 
     * @param codeHueNum The code-facing hue number (0-35)
     * @return The Color to use as background for list items with this hue
     */
    public Color getHueBackgroundColor(int codeHueNum) {
        if (!hueColorLookupInitialized) {
            initializeHueColorLookup();
        }
        
        if (codeHueNum < 0 || codeHueNum >= 36) {
            return Color.LIGHT_GRAY;
        }
        
        return hueColorLookup[codeHueNum];
    }
    
    /**
     * Gets the selected highlight color for a DrawObj list item based on its hue.
     * The color is the standard color at fillOrder=18 for that hue.
     * This is a lighter shade than fillOrder=31, providing good visual contrast for selection highlighting.
     * Uses a lookup table for efficient O(1) access.
     * 
     * @param codeHueNum The code-facing hue number (0-35)
     * @return The Color to use as background for selected list items with this hue
     */
    public Color getHueBackgroundColorSelected(int codeHueNum) {
        if (!hueColorLookupSelectedInitialized) {
            initializeHueColorLookupSelected();
        }
        
        if (codeHueNum < 0 || codeHueNum >= 36) {
            return Color.LIGHT_GRAY;
        }
        
        return hueColorLookupSelected[codeHueNum];
    }
}
