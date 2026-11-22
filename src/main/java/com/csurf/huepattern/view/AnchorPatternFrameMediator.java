package com.csurf.huepattern.view;

import com.csurf.huepattern.controller.AnchorPatternEditController;
import com.csurf.huepattern.controller.DialMouseController;
import com.csurf.huepattern.model.AnchorPatternPrelim;
import com.csurf.huepattern.model.WorkingAnchorPatternState;

/**
 * Mediator for the AnchorPatternFrame that orchestrates all components.
 * 
 * This mediator:
 * 1. Creates and wires up all model, view, and controller components
 * 2. Manages the relationships between components
 * 3. Handles button actions (Update Display, Regenerate Palette, etc.)
 * 4. Coordinates state updates and display refreshes
 * 
 * The mediator implements the Hierarchy pattern (H in HMVC) to provide
 * centralized coordination and reduce coupling between components.
 */
public class AnchorPatternFrameMediator {
    
    // Model components
    private WorkingAnchorPatternState workingState;
    private AGRingDisplayState displayState;
    
    // View components
    private APDisplayPanel apDisplayPanel;
    private AGChoicePanel agChoicePanel;
    private AttributeChoicePanel attributeChoicePanel;
    
    // Controller components
    private AnchorPatternEditController editController;
    private DialMouseController dialMouseController;
    private DialComponent dialComponent;
    
    /**
     * Creates a new AnchorPatternFrameMediator and wires up all components.
     * 
     * @param apDisplayPanel The APDisplayPanel for rendering
     * @param agChoicePanel The AGChoicePanel for AG selection
     * @param attributeChoicePanel The AttributeChoicePanel for attribute selection
     * @param displayState The AGRingDisplayState for display state
     * @param initialAnchorPatternPrelim The initial AnchorPattern to load
     */
    public AnchorPatternFrameMediator(APDisplayPanel apDisplayPanel,
                                      AGChoicePanel agChoicePanel,
                                      AttributeChoicePanel attributeChoicePanel,
                                      AGRingDisplayState displayState,
                                      AnchorPatternPrelim initialAnchorPatternPrelim) {
        // Validate inputs
        if (apDisplayPanel == null) {
            throw new IllegalArgumentException("APDisplayPanel cannot be null");
        }
        if (agChoicePanel == null) {
            throw new IllegalArgumentException("AGChoicePanel cannot be null");
        }
        if (attributeChoicePanel == null) {
            throw new IllegalArgumentException("AttributeChoicePanel cannot be null");
        }
        if (displayState == null) {
            throw new IllegalArgumentException("AGRingDisplayState cannot be null");
        }
        
        // Store view components
        this.apDisplayPanel = apDisplayPanel;
        this.agChoicePanel = agChoicePanel;
        this.attributeChoicePanel = attributeChoicePanel;
        this.displayState = displayState;
        
        // Initialize display state with initial pattern
        if (initialAnchorPatternPrelim != null) {
            displayState.loadFromAnchorPattern(initialAnchorPatternPrelim);
        }
        
        // Create working state from display state
        this.workingState = new WorkingAnchorPatternState(displayState);
        
        // Create dial component with APDisplayPanel center
        // Note: Center will be updated when APDisplayPanel is laid out
        this.dialComponent = new DialComponent(230, 230);
        
        // Create edit controller
        this.editController = new AnchorPatternEditController(
            workingState,
            displayState,
            agChoicePanel,
            attributeChoicePanel
        );
        
        // Create dial mouse controller
        this.dialMouseController = new DialMouseController(
            dialComponent,
            editController
        );
        
        // Wire up dial mouse controller to APDisplayPanel
        apDisplayPanel.addMouseListener(dialMouseController);
        
        // Wire up AG selection listener
        agChoicePanel.addAGSelectionListener(e -> {
            editController.handleAGSelectionChanged(agChoicePanel.getSelectedAG());
        });
    }
    
    /**
     * Updates the center point of the dial component.
     * Call this after APDisplayPanel has been laid out to ensure correct positioning.
     * 
     * @param centerX The X coordinate of the APDisplayPanel center
     * @param centerY The Y coordinate of the APDisplayPanel center
     */
    public void updateDialCenter(int centerX, int centerY) {
        dialComponent.setCenter(centerX, centerY);
    }
    
    /**
     * Handles the "Update Display" button click.
     * 
     * This method:
     * 1. Commits working state to display state
     * 2. Triggers APDisplayPanel repaint
     */
    public void handleUpdateDisplayButton() {
        editController.handleUpdateDisplayButton(apDisplayPanel);
        apDisplayPanel.repaint();
    }
    
    /**
     * Handles the "Revert" button click (if implemented).
     * 
     * This method:
     * 1. Reverts working state to display state
     * 2. Clears selections
     */
    public void handleRevertButton() {
        editController.handleRevertButton();
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
    
    /**
     * Gets the edit controller.
     * @return The AnchorPatternEditController
     */
    public AnchorPatternEditController getEditController() {
        return editController;
    }
    
    /**
     * Gets the dial component.
     * @return The DialComponent
     */
    public DialComponent getDialComponent() {
        return dialComponent;
    }
    
    /**
     * Gets the dial mouse controller.
     * @return The DialMouseController
     */
    public DialMouseController getDialMouseController() {
        return dialMouseController;
    }
}
