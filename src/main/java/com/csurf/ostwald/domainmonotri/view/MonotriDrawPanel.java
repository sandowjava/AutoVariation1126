package com.csurf.ostwald.domainmonotri.view;

import com.csurf.ostwald.domainmonotri.controller.MonotriRenderContrlr;
import com.csurf.ostwald.domainmonotri.controller.OstdotDisplayState;
import com.csurf.ostwald.domainmonotri.controller.OstdotOccupiedListContent;
import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.csurf.ostwald.domainmonotri.model.OstPosition;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame;
import com.csurf.templatecore.model.DrawObjRoster;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Panel responsible for drawing the color squares
 */
public class MonotriDrawPanel extends JPanel {
    private final MonotriRenderContrlr controller;
    private ColorDisplayMode currentColorMode = ColorDisplayMode.RGB;
    private MonotriWorkFrame monotriWorkFrame;
    
    // Equal-Value Hue Rotation UI components
    private JPanel choiceEVRotatePanel;
    private JCheckBox equalValueRotateCheckbox;
    private JRadioButton rgbRadioButton;
    private JRadioButton cmykRadioButton;
    private JComboBox<String> toleranceComboBox;
    
    // ToolTip Panel UI components
    private JPanel toolTipPanel;
    private JPanel namesListPanel;      // scrollable headline + DrawObj-name list
    private JScrollPane namesScrollPane;
    private Font toolTipFont;
    
    // Tolerance value mapping
    private static final double[] TOLERANCE_VALUES = {0.10, 0.15, 0.20, 0.25, 0.30}; // WAS {0.05, 0.08, 0.12, 0.15, 0.20};

    // constructor
    public MonotriDrawPanel(MonotriWorkFrame frame) {
        controller = new MonotriRenderContrlr(this, frame);
        monotriWorkFrame = frame;
        
        setPreferredSize(new Dimension(400, 430)); // 090425 WAS "Dimension(450, 450));"
        setBackground(Color.WHITE);
        setLayout(null); // Use absolute positioning for OstdotPanels
        
        // Create and add the Equal-Value Rotate panel
        createChoiceEVRotatePanel();
        
        // Create and add the ToolTip panel
        createToolTipPanel();
    } // end constructor
    
    
    /**
     * Creates the choiceEVRotatePanel with Equal-Value rotation controls.
     * Panel dimensions: 160x90, positioned in upper-left corner (0, 0).
     */
    private void createChoiceEVRotatePanel() {
        choiceEVRotatePanel = new JPanel();
        choiceEVRotatePanel.setBounds(0, 0, 160, 90);
        choiceEVRotatePanel.setLayout(new BoxLayout(choiceEVRotatePanel, BoxLayout.Y_AXIS));
        choiceEVRotatePanel.setBackground(new Color(240, 240, 240)); // Light gray background
        choiceEVRotatePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        
        // Set default font size to 10pt
        Font font10pt = new Font("Dialog", Font.PLAIN, 10);
        
        // Checkbox for Equal-Value Rotate (default unchecked)
        equalValueRotateCheckbox = new JCheckBox("Equal-Value Rotation");
        equalValueRotateCheckbox.setSelected(false);
        equalValueRotateCheckbox.setFont(font10pt);
        equalValueRotateCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Add listener to update enable/disable state when checkbox changes
        equalValueRotateCheckbox.addActionListener(e -> {
            monotriWorkFrame.updateEVRotateControlsEnableState();
        });
        choiceEVRotatePanel.add(equalValueRotateCheckbox);
        
        // Radio buttons for RGB vs CMYK (mutually exclusive)
        ButtonGroup luminanceGroup = new ButtonGroup();
        
        rgbRadioButton = new JRadioButton("RGB");
        rgbRadioButton.setSelected(true); // Default to RGB
        rgbRadioButton.setFont(font10pt);
        rgbRadioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        luminanceGroup.add(rgbRadioButton);
        choiceEVRotatePanel.add(rgbRadioButton);
        
        cmykRadioButton = new JRadioButton("CMYK");
        cmykRadioButton.setFont(font10pt);
        cmykRadioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        luminanceGroup.add(cmykRadioButton);
        choiceEVRotatePanel.add(cmykRadioButton);
        
        // Dropdown for search tolerance
        String[] toleranceOptions = {"tightest", "tight", "middle", "loose", "loosest"};
        toleranceComboBox = new JComboBox<>(toleranceOptions);
        toleranceComboBox.setSelectedIndex(2); // Default to "middle"
        toleranceComboBox.setFont(font10pt);
        toleranceComboBox.setMaximumSize(new Dimension(150, 20));
        toleranceComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        choiceEVRotatePanel.add(toleranceComboBox);
        
        // Add panel to this MonotriDrawPanel
        this.add(choiceEVRotatePanel);
    } // end method createChoiceEVRotatePanel
    
    
    /**
     * Creates the toolTipPanel with color information display.
     * Panel dimensions: 190x90, positioned in the lower-left corner (0, 317).
     * Layout: a single scrollable content panel holding the composed headline
     * (first row) followed by the DrawObj-name rows; nothing is pinned, so the
     * whole display updates atomically on each dwell-committed hover (103026).
     */
    private void createToolTipPanel() {
        // 1. Create main panel
        toolTipPanel = new JPanel();
        toolTipPanel.setBounds(0, 317, 190, 90);
        toolTipPanel.setLayout(new BorderLayout());
        toolTipPanel.setBackground(new Color(240, 240, 240));
        toolTipPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        
        // 2. Set font
        toolTipFont = new Font("Dialog", Font.PLAIN, 10);
        
        // 3. Scrollable content area: headline + one label per DrawObj name,
        //    populated dynamically by updateOccupiedListDisplay. Vertical
        //    scrollbar appears as needed; horizontal is never needed.
        namesListPanel = new JPanel();
        namesListPanel.setLayout(new BoxLayout(namesListPanel, BoxLayout.Y_AXIS));
        namesListPanel.setBackground(new Color(240, 240, 240));
        namesScrollPane = new JScrollPane(namesListPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        namesScrollPane.setBorder(null);
        namesScrollPane.getVerticalScrollBar().setUnitIncrement(12);
        toolTipPanel.add(namesScrollPane, BorderLayout.CENTER);
        
        // 4. Add to MonotriDrawPanel
        this.add(toolTipPanel);
        
        // 5. Global wheel-forwarding: OstdotPanels have no wheel listeners,
        //    so wheel events over the dot area bubble up to this panel;
        //    forward them to the occupied-list scroll pane so the list can
        //    be scrolled from anywhere on the draw panel (103026).
        this.addMouseWheelListener(this::forwardWheelToNamesList);
    } // end method createToolTipPanel
    
    
    /**
     * Forwards a mouse-wheel event to the occupied-list scroll pane.
     * Swing delivers wheel events both to the component under the cursor and
     * to ancestors with registered listeners, so events that already targeted
     * the scroll pane (or its contents) are skipped to avoid double-scrolling.
     *
     * @param e the wheel event received anywhere on this draw panel
     */
    private void forwardWheelToNamesList(MouseWheelEvent e) {
        if (namesScrollPane == null) {
            return;
        }
        if (SwingUtilities.isDescendingFrom(e.getComponent(), namesScrollPane)) {
            return; // already handled by the scroll pane itself
        }
        for (MouseWheelListener listener : namesScrollPane.getMouseWheelListeners()) {
            listener.mouseWheelMoved(e);
        }
    } // end method forwardWheelToNamesList
    
    
    /**
     * Gets the Equal-Value Rotate checkbox.
     * @return the checkbox for enabling/disabling EV rotation
     */
    public JCheckBox getEqualValueRotateCheckbox() {
        return equalValueRotateCheckbox;
    }
    
    
    /**
     * Gets the RGB radio button.
     * @return the RGB luminance selection button
     */
    public JRadioButton getRgbRadioButton() {
        return rgbRadioButton;
    }
    
    
    /**
     * Gets the CMYK radio button.
     * @return the CMYK luminance selection button
     */
    public JRadioButton getCmykRadioButton() {
        return cmykRadioButton;
    }
    
    
    /**
     * Gets the tolerance dropdown combo box.
     * @return the combo box for search tolerance selection
     */
    public JComboBox<String> getToleranceComboBox() {
        return toleranceComboBox;
    }
    
    
    /**
     * Gets the current numerical tolerance value based on the selected dropdown option.
     * @return the tolerance value (0.05, 0.08, 0.12, 0.15, or 0.20)
     */
    public double getCurrentToleranceValue() {
        int selectedIndex = toleranceComboBox.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < TOLERANCE_VALUES.length) {
            return TOLERANCE_VALUES[selectedIndex];
        }
        return TOLERANCE_VALUES[2]; // Default to "middle" (0.12) if invalid
    }
    
    
    /**
     * Enables or disables all controls in choiceEVRotatePanel.
     * Controls are enabled only when both conditions are met:
     * 1. hueRotationModeRadioButton is selected in MonotriWorkFrame
     * 2. Equal-Value Rotation checkbox is checked
     * 
     * @param enabled true to enable controls, false to disable
     */
    public void setEVRotateControlsEnabled(boolean enabled) {
        rgbRadioButton.setEnabled(enabled);
        cmykRadioButton.setEnabled(enabled);
        toleranceComboBox.setEnabled(enabled);
    }


    /**
     * Updates the fixed toolTipPanel with the occupancy content for a hovered
     * OstDot position. The whole display is scrollable: the composed headline
     * ("fill# <N> COLM <col> step <step> — W: <w>, B: <b>") is the first
     * row, followed by one label per occupying DrawObj entry (selected
     * entries arrive already "**"-prefixed from the controller). Unoccupied
     * positions show the headline alone.
     *
     * Calls to this method are dwell-debounced in MonotriWorkFrame, so a
     * display only appears once the pointer has settled on an OstDot.
     *
     * A null content or null position is ignored so the display stays sticky
     * (the display is only replaced when another OstDot is hovered).
     *
     * @param content the composed content from OstdotOccupiedListController
     */
    public void updateOccupiedListDisplay(OstdotOccupiedListContent content) {
        if (content == null || content.getOstHueColumnStep() == null) {
            return; // sticky: keep the current display
        }

        namesListPanel.removeAll();
        if (content.getHeadline() != null) {
            JLabel headlineLabel = new JLabel(content.getHeadline());
            headlineLabel.setFont(toolTipFont);
            headlineLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            namesListPanel.add(headlineLabel);
        }
        for (String name : content.getDrawObjNames()) {
            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(toolTipFont);
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            namesListPanel.add(nameLabel);
        }
        namesListPanel.revalidate();
        namesListPanel.repaint();
        // Each new hovered position starts its list scrolled to the top
        SwingUtilities.invokeLater(() ->
            namesScrollPane.getVerticalScrollBar().setValue(0));
    } // end method updateOccupiedListDisplay

    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        controller.drawDotPanels(g2d);
    } // end paintComponent



    
    public MonotriRenderContrlr getController() {
        return controller;
    }
    // end getTheController




    /**
     * Updates the panel display states from the current maps.
     */
    public void updatePanelDisplayStates() {
        // RS took out 020526 -console clutter
        // System.out.println("DEBUG: MonotriDrawPanel.updatePanelDisplayStates called");
        
        // Get the DrawObjRoster from the TemplateWorkFrame
        DrawObjRoster roster = null;
        if (monotriWorkFrame != null && monotriWorkFrame.getTemplateWorkFrame() != null) {
            roster = monotriWorkFrame.getTemplateWorkFrame().getDrawObjRoster();
        }
        
        if (roster == null) {
            System.out.println("ERROR: Could not get DrawObjRoster in updatePanelDisplayStates");
            return;
        }
        
        // Force a complete rebuild of the roster state
        roster.updateToCurrObjState();
        
        // Get the current maps from the roster
        Map<OstPosition, int[]> occupiedMap = roster.getUnmodifiableOccupiedMap();
        Map<OstPosition, int[]> selectedMap = roster.getUnmodifiableSelectedMap();
        
        // Debug the map sizes
        // RS took out 020526 -console clutter
        /*
        System.out.println("DEBUG: updatePanelDisplayStates - occupiedMap size: " + 
            (occupiedMap != null ? occupiedMap.size() : "null") + 
            ", selectedMap size: " + (selectedMap != null ? selectedMap.size() : "null"));

         */
        
        // Debug the map contents
        if (occupiedMap != null && occupiedMap.size() > 0) {
            // RS took out 020526 -console clutter
            // System.out.println("DEBUG: Occupied positions: " + occupiedMap.keySet());
        } else {
            System.out.println("WARNING: Occupied map is empty or null");
        }
        
        if (selectedMap != null && selectedMap.size() > 0) {
            // RS took out 020526 -console clutter
            // System.out.println("DEBUG: Selected positions: " + selectedMap.keySet());
        } else {
            System.out.println("WARNING: Selected map is empty or null");
        }
        
        // Update the panel display states
        updateFromMaps(occupiedMap, selectedMap);
        
        // Force a complete UI refresh
        invalidate();
        revalidate();
        repaint();
    } // end method updatePanelDisplayStates




    /**
     * Updates all OstdotPanels based on the provided occupied and selected maps.
     * This method is called directly by the Update Position State button.
     * 
     * @param occupiedMap Map of positions to arrays of hues that are occupied
     * @param selectedMap Map of positions to arrays of hues that are selected
     */
    public void updateFromMaps(Map<OstPosition, int[]> occupiedMap, Map<OstPosition, int[]> selectedMap) {
        // RS took out 020526 -console clutter
        // System.out.println("DEBUG: MonotriDrawPanel.updateFromMaps called");
        
        // Get the current internal hue (0-35)
        int currentHueNumCode;
        if (monotriWorkFrame != null) {
            currentHueNumCode = monotriWorkFrame.getCurrHueNumCode();
        } else {
            // Fallback to getting the parent frame
            MonotriWorkFrame frame = (MonotriWorkFrame) SwingUtilities.getWindowAncestor(this);
            if (frame == null) {
                System.out.println("ERROR: Could not get MonotriWorkFrame in updateFromMaps");
                return;
            }
            currentHueNumCode = frame.getCurrHueNumCode();
        }
        
        // Convert to user-facing hue (1-36) for logging and display
        HueNumGUI currentHueNumGUI = HueNumGUI.getHueNumGUIInstance(currentHueNumCode + 1);
        int userFacingHueNum = currentHueNumGUI.getUserFacingHueNum();
        // RS took out 020526 -console clutter
        /*
        System.out.println("DEBUG: Current hue number: " + userFacingHueNum + 
                           " (internal code: " + currentHueNumCode + ")");

         */
        
        // Debug the maps
        // RS took out 020526 -console clutter
        /*
        System.out.println("DEBUG: updateFromMaps - OccupiedMap size: " + 
            (occupiedMap != null ? occupiedMap.size() : "null") + 
            ", SelectedMap size: " + (selectedMap != null ? selectedMap.size() : "null"));

         */
        
        // Ensure maps are not null
        if (occupiedMap == null) {
            System.out.println("WARNING: occupiedMap is null in updateFromMaps, using empty map");
            occupiedMap = Collections.emptyMap();
        }
        
        if (selectedMap == null) {
            System.out.println("WARNING: selectedMap is null in updateFromMaps, using empty map");
            selectedMap = Collections.emptyMap();
        }
        
        // Debug the occupied positions
        if (!occupiedMap.isEmpty()) {
            // RS took out 020526 -console clutter
            // System.out.println("DEBUG: Occupied positions in MonotriDrawPanel method -updateFromMaps- has:");
            for (Map.Entry<OstPosition, int[]> entry : occupiedMap.entrySet()) {
                System.out.println("  Position: " + entry.getKey() + ", Hues: " + java.util.Arrays.toString(entry.getValue()));
            }
        } else {
            // RS took out 020526 -console clutter
            // System.out.println("DEBUG: No occupied positions in updateFromMaps");
        }
        
        // Debug the selected positions
        if (!selectedMap.isEmpty()) {
            // RS took out 020526 -console clutter
            // System.out.println("DEBUG: Selected positions in MonotriDrawPanel method -updateFromMaps- has:");
            for (Map.Entry<OstPosition, int[]> entry : selectedMap.entrySet()) {
                System.out.println("  Position: " + entry.getKey() + ", Hues: " + java.util.Arrays.toString(entry.getValue()));
            }
        } else {
            // RS took out 020526 -console clutter
            // System.out.println("DEBUG: No selected positions in updateFromMaps");
        }
        
        // Get the simplified view setting from the controller
        boolean isSimplifiedView = controller.isSimplifiedViewEnabled();

        // RS took out 020526 -console clutter
        // System.out.println("DEBUG: MonotriDrawPanel method -updateFromMaps- now does Updating panel states for hue " + userFacingHueNum);
        // System.out.println("DEBUG: updateFromMaps - Using simplified view: " + isSimplifiedView);
        System.out.println("   ");
        
        // Update each panel's display state
        List<OstdotPanel> panels = getAllOstdotPanels();
        // RS took out 020526 -console clutter
        // System.out.println("DEBUG: Found " + panels.size() + " OstdotPanels to update");
        
        for (OstdotPanel panel : panels) {
            if (panel.getColorPtOstDot() == null || panel.getColorPtOstDot().getOstHCS() == null) {
                System.out.println("WARNING: Panel has null ColorPtOstDot or OstHCS, skipping");
                continue;
            }
            
            OstPosition position = panel.getColorPtOstDot().getOstHCS().getOstPosition();
            if (position == null) {
                System.out.println("WARNING: Panel has null position, skipping");
                continue;
            }
            
            // Debug the hues for this position
            // RS took out 020526 -console clutter
            /*
            System.out.println("DEBUG: Position " + position + 
                " - Occupied hues: " + (occupiedMap.get(position) != null ? java.util.Arrays.toString(occupiedMap.get(position)) : "null") +
                ", Selected hues: " + (selectedMap.get(position) != null ? java.util.Arrays.toString(selectedMap.get(position)) : "null"));

             */
            
            // Calculate display state based on the current hue and the maps
            OstdotDisplayState state = OstdotPanel.calcDisplayStateFromMaps(
                position,
                userFacingHueNum,
                occupiedMap,
                selectedMap,
                isSimplifiedView
            );
            
            // Debug the state calculation
            // RS took out 020526 -console clutter
            // System.out.println("DEBUG: Panel at position " + position + " calculated state: " + state);
            
            // Update the panel's display state
            panel.updateDisplayState(state);
        }
        
        // Force a complete UI refresh
        System.out.println("   ");
        // RS maybe take out 020526 -console clutter
        System.out.println("DEBUG: MonotriDrawPanel method -updateFromMaps- forcing complete UI refresh");
        invalidate();
        revalidate();
        repaint();
        
        // Request focus to ensure the panel is properly redrawn
        requestFocusInWindow();
    }
    // end method updateFromMaps




    /**
     * Gets all OstdotPanels in this panel.
     */
    private List<OstdotPanel> getAllOstdotPanels() {
        List<OstdotPanel> panels = new ArrayList<>();
        for (Component comp : getComponents()) {
            if (comp instanceof OstdotPanel) {
                panels.add((OstdotPanel) comp);
            }
        }
        return panels;
    }

    // --- Color mode toggle support ---
    public void setColorDisplayMode(ColorDisplayMode mode) {
        if (this.currentColorMode != mode) {
            this.currentColorMode = mode;
            updateAllPanels();
        }
    }

    public ColorDisplayMode getColorDisplayMode() {
        return currentColorMode;
    }

    private void updateAllPanels() {
        for (Component comp : getComponents()) {
            if (comp instanceof OstdotPanel) {
                ((OstdotPanel) comp).setColorMode(currentColorMode);
            }
        }
    }
}
