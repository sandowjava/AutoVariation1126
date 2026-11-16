package com.csurf.templatecore.controller;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.model.HueSpacingStdChoices;
import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.view.DrawObjStateFrame;
import com.csurf.templatecore.view.TemplateWorkFrame;

import javax.swing.*;

/**
 * Class purpose is to control the display of info in class DrawObjStateFrame and handle its interactions with
 * one DrawObj instance in the current Template instance in TemplateWorkFrame.
 * The second use case for this controller is to correct the color points of the DrawObj if needed (not implemented). This
 * relates to the problem of existing Template instances that have incorrectly composed ColorPtOstDots (incorrect hueNums).
 * The second use case for this controller is real but not a priority and is not implemented yet.
 */
public class DrawObjStateViewController {
    private final TemplateWorkFrame templateWorkFrame;
    private final DrawObj targetDrawObj;
    private final DrawObjStateFrame stateFrame;
    private final Template template;




    // Constructor
    public DrawObjStateViewController(TemplateWorkFrame templateWorkFrame, DrawObj selectedDrawObj) {
        this.templateWorkFrame = templateWorkFrame;
        this.targetDrawObj = selectedDrawObj;
        this.template = templateWorkFrame.getCurrTemplate();
        this.stateFrame = new DrawObjStateFrame(this);
        
        // Add window listener to handle frame closure
        stateFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // Unregister from TemplateWorkFrame when closing
                if (templateWorkFrame != null) {
                    templateWorkFrame.unregisterDrawObjStateController(DrawObjStateViewController.this);
                }
            }
        });
        
        // Initial display update
        updateDisplayedInfo();
        
        // Show the frame
        stateFrame.setLocationRelativeTo(templateWorkFrame);
        stateFrame.setVisible(true);
    } // end constructor



    
    public void updateDisplayedInfo() {
        // Verify template is still current
        if (template != templateWorkFrame.getCurrTemplate()) {
            JOptionPane.showMessageDialog(stateFrame,
                "Template has changed. Cannot update information.",
                "Update Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Build info text
        StringBuilder info = new StringBuilder();
        info.append("Pipe Unity ID: ").append(targetDrawObj.getPipeUnityId()).append("\n");
        
        ColorPtOstDot colorPt = targetDrawObj.getColorPtOstDotFillColor();
        OstHueColumnStep hcs = colorPt.getOstHCS();
        
        info.append("Fill Order Number: ").append(hcs.getFillOrderNum()).append("\n");
        // info.append("Std HueNum (Code): ").append(hcs.getStdCodeHueNum()).append("\n");
        info.append("COLUMN: ").append(hcs.getColumn()).append(" "); // note: no line break after column
        info.append("step: ").append(hcs.getStep()).append("\n");
        int hueNumCode = hcs.getStdCodeHueNum();
        HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(hueNumCode+1); // 062725 must increment hueNumCode for constructor
        // info.append("HueNumCode testCorrect? ").append(colorPt.testHueNumCodeCorrect()).append("\n"); // 081425 for testing only
        info.append("HueNumGUI = ").append(hueNumGUI.getUserFacingHueNum()).append(" [for 360degr  -> ")
                .append(HueSpacingStdChoices.getSingleton().getHueDegrIntForHueNumGUI(hueNumGUI)).append("]").append("\n");
        info.append("Selected: ").append(targetDrawObj.isSelected()).append("\n");
        info.append("EqWhite Series (parallel clearB = 0): ").append(hcs.getSeriesW()).append("\n");
        info.append("EqBlack Series (parallel clearW = 0): ").append(hcs.getSeriesB()).append("\n");

        info.append("").append("            ----    ").append("\n");
        float someRGBLum3D = (float) colorPt.getLum3D();
        float someAbSat = (float) colorPt.getAbSat();
        float someCMYKLum = colorPt.getCMYKLum();
        float somePercentWhite = colorPt.getPercentWhite();
        // info.append("Lum by planar RGB = ").append("unknown").append("\n");
        info.append("Lum by 3D RGB = ").append(String.format("%.3f",someRGBLum3D)).append("\n"); // Format to 3 decimal places
        info.append("Lum by CMYK = ").append(String.format("%.3f", someCMYKLum)).append("\n"); // Format to 3 decimal places
        info.append("Abs Color Saturation = ").append(String.format("%.3f", someAbSat)).append("\n"); // Format to 3 decimal places
        info.append("Percent White = ").append(String.format("%.3f", somePercentWhite)); // Format to 3 decimal places


        // Set the text size to 11 pixels
        stateFrame.setInfoTextFontSize(11);
        
        stateFrame.updateDisplay(
            targetDrawObj.getName(),
            template.getTheTemplateName(),
            info.toString()
        );
    } // end method updateDisplayedInfo



    
    public DrawObj getTargetDrawObj() {
        return targetDrawObj;
    }
    
    public void close() {
        // Unregister from TemplateWorkFrame
        if (templateWorkFrame != null) {
            templateWorkFrame.unregisterDrawObjStateController(this);
        }
        stateFrame.dispose();
    }
    
    public void toFront() {
        stateFrame.toFront();
    }
    
    /**
     * Gets the DrawObjStateFrame associated with this controller.
     * 
     * @return The DrawObjStateFrame instance
     */
    public DrawObjStateFrame getStateFrame() {
        return stateFrame;
    }
    
    /**
     * Gets the DrawObjRoster from the TemplateWorkFrame.
     * 
     * @return The DrawObjRoster instance
     */
    public DrawObjRoster getDrawObjRoster() {
        if (templateWorkFrame != null) {
            return templateWorkFrame.getDrawObjRoster();
        }
        return null;
    }
    
    /**
     * Gets the TemplateWorkFrame.
     * 
     * @return The TemplateWorkFrame instance
     */
    public TemplateWorkFrame getTemplateWorkFrame() {
        return templateWorkFrame;
    }
    
    /**
     * Toggles the selection state of the target DrawObj.
     * If the DrawObj is currently selected, it will be deselected.
     * If the DrawObj is not selected, it will be added to the selection.
     * This method ensures that proper selection events are fired to update the UI.
     * 
     * @return true if the DrawObj is now selected, false if it is now deselected
     */
    public boolean toggleDrawObjSelection() {
        if (templateWorkFrame != null && targetDrawObj != null) {
            DrawObjRoster roster = getDrawObjRoster();
            if (roster != null) {
                // Check current selection state
                boolean isCurrentlySelected = roster.isSelected(targetDrawObj);
                
                // Toggle selection state using DrawObjRoster methods which fire proper events
                if (isCurrentlySelected) {
                    roster.removeFromSelection(targetDrawObj);
                    return false;
                } else {
                    roster.addToSelection(targetDrawObj);
                    return true;
                }
            }
        }
        return false;
    }
    
    // Future implementation for color point correction
    public void handleCorrectColorPoints() {
        // To be implemented for second use case
    }
} // end class DrawObjStateViewController
