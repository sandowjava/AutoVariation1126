package com.csurf.huepattern.controller;

import com.csurf.huepattern.model.AnchorPatternPrelim;
import com.csurf.huepattern.model.WorkingAnchorPatternState;
import com.csurf.huepattern.view.AGChoicePanel;
import com.csurf.huepattern.view.AGRingDisplayState;
import com.csurf.huepattern.view.AttributeChoicePanel;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.ostwald.domainmonotri.model.MonotriPage;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

import java.awt.Color;

/**
 * Main controller for editing an AnchorPattern in the AnchorPatternFrame.
 * 
 * This controller:
 * 1. Handles dial input (hue numbers from DialMouseController)
 * 2. Coordinates with AG and attribute selection panels
 * 3. Updates the working state with user modifications
 * 4. Updates text fields to show current values
 * 5. Commits working state to display state when "Update Display" button is clicked
 * 
 * The controller implements the HMVC pattern:
 * - Model: WorkingAnchorPatternState (working state) + AGRingDisplayState (display state)
 * - View: AGChoicePanel, AttributeChoicePanel, APDisplayPanel
 * - Controller: This class (AnchorPatternEditController)
 * - Hierarchy: AnchorPatternFrameMediator (future)
 */
public class AnchorPatternEditController {
    
    private final WorkingAnchorPatternState workingState;
    private final AGRingDisplayState displayState;
    private final AGChoicePanel agChoicePanel;
    private final AttributeChoicePanel attributeChoicePanel;
    
    /**
     * Creates a new AnchorPatternEditController.
     * 
     * @param workingState The working state model
     * @param displayState The display state model
     * @param agChoicePanel The AG selection panel
     * @param attributeChoicePanel The attribute selection panel
     */
    public AnchorPatternEditController(WorkingAnchorPatternState workingState,
                                       AGRingDisplayState displayState,
                                       AGChoicePanel agChoicePanel,
                                       AttributeChoicePanel attributeChoicePanel) {
        if (workingState == null) {
            throw new IllegalArgumentException("WorkingAnchorPatternState cannot be null");
        }
        if (displayState == null) {
            throw new IllegalArgumentException("AGRingDisplayState cannot be null");
        }
        if (agChoicePanel == null) {
            throw new IllegalArgumentException("AGChoicePanel cannot be null");
        }
        if (attributeChoicePanel == null) {
            throw new IllegalArgumentException("AttributeChoicePanel cannot be null");
        }
        
        this.workingState = workingState;
        this.displayState = displayState;
        this.agChoicePanel = agChoicePanel;
        this.attributeChoicePanel = attributeChoicePanel;
    }
    
    /**
     * Handles dial input (hue number from DialMouseController).
     * 
     * This method:
     * 1. Gets the currently selected AG from AGChoicePanel
     * 2. Gets the currently selected attribute from AttributeChoicePanel
     * 3. Updates the working state with the new hue value
     * 4. Updates the text field and color square to show the new value
     * 5. If Anchor changed, also updates AGChoicePanel to keep data consistent
     * 
     * Note: The display is NOT updated yet. User must click "Update Display" button.
     * 
     * @param hueNum The hue number (1-36) from the dial
     */
    public void handleDialInput(int hueNum) {
        // Validate hue number
        if (hueNum < 1 || hueNum > 36) {
            return;
        }
        
        // Get currently selected AG
        int selectedAG = agChoicePanel.getSelectedAG();
        if (selectedAG < 1 || selectedAG > 4) {
            // No AG selected, ignore input
            return;
        }
        
        // Get currently selected attribute
        WorkingAnchorPatternState.Attribute selectedAttribute = attributeChoicePanel.getSelectedAttribute();
        if (selectedAttribute == null) {
            // No attribute selected, ignore input
            return;
        }
        
        // Update working state
        workingState.setCurrentAG(selectedAG);
        workingState.setCurrentAttribute(selectedAttribute);
        workingState.setCurrentHueSelection(hueNum);
        workingState.updateWorkingSpread(selectedAG, selectedAttribute, hueNum);
        
        // Update AttributeChoicePanel text field and color square
        attributeChoicePanel.updateHueField(selectedAttribute, hueNum);
        attributeChoicePanel.updateColorSquare(selectedAttribute, hueNum);
        
        // If Anchor attribute was changed, also update AGChoicePanel to keep data consistent
        if (selectedAttribute == WorkingAnchorPatternState.Attribute.ANCHOR) {
            Color anchorColor = getColorForHueNum(hueNum);
            agChoicePanel.updateAGAnchorDisplay(selectedAG, hueNum, anchorColor);
        }
    }
    
    /**
     * Handles the "Update Display" button click.
     * 
     * This method:
     * 1. Converts working state to an AnchorPattern
     * 2. Loads the new pattern into the display state
     * 3. Triggers APDisplayPanel repaint
     * 
     * This is called when the user clicks the "Update Display" button.
     * 
     * @param apDisplayPanel The APDisplayPanel to repaint (will be passed by mediator)
     */
    public void handleUpdateDisplayButton(Object apDisplayPanel) {
        // Convert working state to AnchorPattern
        AnchorPatternPrelim newPattern = workingState.toAnchorPattern();
        
        // Load into display state
        displayState.loadFromAnchorPattern(newPattern);
        
        // Trigger repaint (mediator will handle this)
        // For now, we just update the display state
        // The mediator will call apDisplayPanel.repaint()
    }
    
    /**
     * Reverts the working state to the display state.
     * This discards all uncommitted changes.
     */
    public void handleRevertButton() {
        workingState.revertFromDisplayState(displayState);
        
        // Clear selections
        attributeChoicePanel.clearSelection();
    }
    
    /**
     * Handles AG selection change.
     * Updates AttributeChoicePanel to show data for the selected AG.
     * @param selectedAG The newly selected AG (1-4)
     */
    public void handleAGSelectionChanged(int selectedAG) {
        if (selectedAG < 1 || selectedAG > 4) {
            return;
        }
        
        // Get WorkingSpreadData for the selected AG
        WorkingAnchorPatternState.WorkingSpreadData spreadData = workingState.getWorkingSpread(selectedAG);
        
        if (spreadData != null) {
            // Update AttributeChoicePanel with the AG's current values
            attributeChoicePanel.updateAllHueFields(
                spreadData.cCWLimitHue,
                spreadData.anchorHue,
                spreadData.cWLimitHue
            );
        } else {
            // AG has no data yet, clear the fields
            attributeChoicePanel.clearSelection();
        }
    }
    
    /**
     * Gets the color for a hue number from MonotriBook.
     * @param hueUserFacingNum The hue number (1-36)
     * @return The color, or white if invalid
     */
    private Color getColorForHueNum(int hueUserFacingNum) {
        if (hueUserFacingNum < 1 || hueUserFacingNum > 36) {
            return Color.WHITE;
        }
        
        try {
            MonotriPage page = MonotriBook.getInstance().getMonoTriPage(hueUserFacingNum - 1);
            if (page == null) {
                return Color.WHITE;
            }
            
            ColorPtOstDot colorDot = page.getColorByFillOrder(22); // TODO: fillOrderNum =22 is OK, near-pure high-sat, mid-column 3
            if (colorDot == null) {
                return Color.WHITE;
            }
            
            return colorDot.getAsColor();
        } catch (Exception e) {
            return Color.WHITE;
        }
    }
    
    /**
     * Gets the working state.
     * @return The WorkingAnchorPatternState
     */
    public WorkingAnchorPatternState getWorkingState() {
        return workingState;
    }
    
    /**
     * Gets the display state.
     * @return The AGRingDisplayState
     */
    public AGRingDisplayState getDisplayState() {
        return displayState;
    }
}
