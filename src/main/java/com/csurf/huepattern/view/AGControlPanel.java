package com.csurf.huepattern.view;

import com.csurf.huepattern.model.AnchorPattern;

import javax.swing.*;
import java.awt.*;

/**
 * Control panel for AnchorGroup attributes.
 * Contains AGChoicePanel, AttributeChoicePanel, Regenerate Palette button, and Update Display button.
 * Holds the DisplayState proxy for the current editing/display state.
 * Fixed size: 150x460 pixels.
 */
public class AGControlPanel extends JPanel {
    private static final int PANEL_WIDTH = 180;
    private static final int PANEL_HEIGHT = 460;
    
    private AGChoicePanel agChoicePanel;
    private AttributeChoicePanel attributeChoicePanel;
    private JButton regeneratePaletteButton;
    private JButton updateDisplayButton;
    
    private AGRingDisplayState AGRingDisplayState;

    public AGControlPanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setMinimumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setMaximumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.LIGHT_GRAY);
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        // Initialize DisplayState with default AnchorPattern
        AGRingDisplayState = new AGRingDisplayState();
        AnchorPattern defaultPattern = AnchorPattern.createDefault();
        AGRingDisplayState.loadFromAnchorPattern(defaultPattern);
        
        // Initialize sub-panels
        agChoicePanel = new AGChoicePanel();
        attributeChoicePanel = new AttributeChoicePanel();
        
        // Initialize buttons
        regeneratePaletteButton = new JButton("Regenerate palette");
        regeneratePaletteButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        updateDisplayButton = new JButton("Update Display");
        updateDisplayButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add components
        add(agChoicePanel);
        add(Box.createVerticalStrut(10));
        add(attributeChoicePanel);
        add(Box.createVerticalGlue()); // Push buttons to bottom
        add(regeneratePaletteButton);
        add(Box.createVerticalStrut(5));
        add(updateDisplayButton);
        add(Box.createVerticalStrut(10));
    }
    
    public JButton getRegeneratePaletteButton() {
        return regeneratePaletteButton;
    }
    
    public JButton getUpdateDisplayButton() {
        return updateDisplayButton;
    }
    
    /**
     * Gets the AGChoicePanel.
     */
    public AGChoicePanel getAGChoicePanel() {
        return agChoicePanel;
    }
    
    /**
     * Gets the AttributeChoicePanel.
     */
    public AttributeChoicePanel getAttributeChoicePanel() {
        return attributeChoicePanel;
    }
    
    /**
     * Gets the current DisplayState for rendering.
     */
    public AGRingDisplayState getDisplayState() {
        return AGRingDisplayState;
    }
    
    /**
     * Loads a new AnchorPattern into the DisplayState.
     */
    public void loadAnchorPattern(AnchorPattern anchorPattern) {
        AGRingDisplayState.loadFromAnchorPattern(anchorPattern);
    }
} // end class AGControlPanel
