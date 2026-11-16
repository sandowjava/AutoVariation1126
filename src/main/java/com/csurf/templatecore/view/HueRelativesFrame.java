package com.csurf.templatecore.view;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame;
import com.csurf.templatecore.controller.HueRelativesViewController;
import com.csurf.templatecore.interfaces.IHueReceiver;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Class purpose is to display a list of DrawObjs from the associated TemplateWorkFrame and allow user control
 * The list of DrawObjs from the associated TemplateWorkFrame is determined by a hue range under user control
 * The class HueRelativesViewController creates the list of DrawObjs, manages the display, and responds to user input
 * Any DrawObj in the list can have its hueNum displayed by clicking on it, and its color will be displayed in drawObjColorPanel
 * The selected DrawObj in the list can have its DrawObjStateFrame can be opened by openOneDrawObjStateButton
 * All DrawObjs in the list can have their DrawObjStateFrames opened by clicking on the openAllDrawObjStateButton
 *
 */
public class HueRelativesFrame extends JFrame implements IHueReceiver {
    private static int nextFrameId = 1;
    private final int frameId;
    
    private final JPanel drawObjListPanel;
    private final JSpinner counterCWiseSpinner;
    private final JSpinner centralHueSpinner;
    private final JSpinner clockWiseSpinner;
    private final JButton openOneDrawObjStateButton;
    private final JButton openAllDrawObjStateButton;
    private final JButton openMonoTriButton;
    private final JButton plusMinusOneToSelectGroupButton;
    private final JButton plusAllSelectGroupButton;
    private final JTextField hueNumTextField;
    private final HueRelativesViewController controller;
    private final JPanel drawObjColorSquarePanel;
    private final JPanel drawObjColorPanel;
    private final JPanel buttonPanel;
    private final JPanel spinnersPanel;
    private final JList<DrawObj> drawObjList;
    private final JCheckBox receiverCheckBox;
    private final JRadioButton aToZButton;
    private final JRadioButton zToAButton;
    private final JRadioButton byHueButton;
    private boolean isAscendingOrder = true;
    private boolean isSortByHue = false;
    // True while setDesignatedReceiver() is applying a TemplateWorkFrame-driven
    // sync, so the checkbox listener can tell user clicks from programmatic changes
    private boolean suppressCheckBoxEvents = false;

    // constructor
    public HueRelativesFrame(HueRelativesViewController controller) {
        this.frameId = nextFrameId++;
        this.controller = controller;
        setTitle("HueRelativesFrame #" + frameId);
        setSize(300, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Main panel with vertical BoxLayout to stack the four panels
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // 1. drawObjListPanel - at the top
        drawObjListPanel = new JPanel(new BorderLayout());
        drawObjListPanel.setBorder(new TitledBorder("DrawObjs in Hue Range"));
        drawObjListPanel.setPreferredSize(new Dimension(280, 200));
        
        // Create JList with custom cell renderer
        drawObjList = new JList<>(controller.getListModel());
        drawObjList.setCellRenderer(new DrawObjListCellRenderer());
        drawObjList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        drawObjList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    DrawObj selectedDrawObj = drawObjList.getSelectedValue();
                    if (selectedDrawObj != null) {
                        controller.handleDrawObjSelection(selectedDrawObj);
                    }
                }
            }
        });
        
        // Add JList to a scroll pane
        JScrollPane scrollPane = new JScrollPane(drawObjList);
        drawObjListPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 2. spinnersPanel - below drawObjListPanel
        spinnersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
        spinnersPanel.setBorder(new TitledBorder("Hue Range Controls"));
        
        // Counter-clockwise spinner
        JPanel ccwPanel = new JPanel(new BorderLayout(0, 0));
        JLabel ccwLabel = new JLabel("CCW");
        ccwLabel.setHorizontalAlignment(JLabel.CENTER);
        ccwLabel.setFont(new Font(ccwLabel.getFont().getName(), Font.PLAIN, 12));
        // 091425 note that value setting here is overridden by the openHueRelativesFrame(int) creation factory in TemplateWorkFrame, for all creation paths
        counterCWiseSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 12, 1));
        counterCWiseSpinner.setPreferredSize(new Dimension(43, 25));
        ccwPanel.add(ccwLabel, BorderLayout.NORTH);
        ccwPanel.add(counterCWiseSpinner, BorderLayout.CENTER);
        
        // Central hue spinner
        JPanel centralPanel = new JPanel(new BorderLayout(0, 0));
        JLabel centralLabel = new JLabel("CtrHue");
        centralLabel.setHorizontalAlignment(JLabel.CENTER);
        centralLabel.setFont(new Font(centralLabel.getFont().getName(), Font.PLAIN, 12));
        centralHueSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 36, 1));
        centralHueSpinner.setPreferredSize(new Dimension(43, 25));
        centralPanel.add(centralLabel, BorderLayout.NORTH);
        centralPanel.add(centralHueSpinner, BorderLayout.CENTER);
        
        // Clockwise spinner
        JPanel cwPanel = new JPanel(new BorderLayout(0, 0));
        JLabel cwLabel = new JLabel("CW");
        cwLabel.setHorizontalAlignment(JLabel.CENTER);
        cwLabel.setFont(new Font(cwLabel.getFont().getName(), Font.PLAIN, 12));
        // 091425 note that value setting here is overridden by the openHueRelativesFrame(int) creation factory in TemplateWorkFrame, for all creation paths
        clockWiseSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 12, 1));
        clockWiseSpinner.setPreferredSize(new Dimension(43, 25));
        cwPanel.add(cwLabel, BorderLayout.NORTH);
        cwPanel.add(clockWiseSpinner, BorderLayout.CENTER);
        
        // Add spinners to spinnersPanel
        spinnersPanel.add(ccwPanel);
        spinnersPanel.add(centralPanel);
        spinnersPanel.add(cwPanel);
        
        // 2a. listOrderPanel - vertical radio button stack for A-Z / Z-A ordering
        JPanel listOrderPanel = new JPanel();
        listOrderPanel.setLayout(new BoxLayout(listOrderPanel, BoxLayout.Y_AXIS));
        listOrderPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        JLabel orderLabel = new JLabel("List Order");
        orderLabel.setFont(new Font(orderLabel.getFont().getName(), Font.PLAIN, 9));
        orderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        aToZButton = new JRadioButton("A-Z", true);
        aToZButton.setMargin(new Insets(0, 0, 0, 0));
        zToAButton = new JRadioButton("Z-A", false);
        zToAButton.setMargin(new Insets(0, 0, 0, 0));
        byHueButton = new JRadioButton("by Hue", false);
        byHueButton.setMargin(new Insets(0, 0, 0, 0));
        
        ButtonGroup orderGroup = new ButtonGroup();
        orderGroup.add(aToZButton);
        orderGroup.add(zToAButton);
        orderGroup.add(byHueButton);
        
        listOrderPanel.add(orderLabel);
        listOrderPanel.add(Box.createRigidArea(new Dimension(0, 1)));
        listOrderPanel.add(aToZButton);
        listOrderPanel.add(Box.createRigidArea(new Dimension(0, 0)));
        listOrderPanel.add(zToAButton);
        listOrderPanel.add(Box.createRigidArea(new Dimension(0, 0)));
        listOrderPanel.add(byHueButton);
        
        // Add list order panel to spinnersPanel
        spinnersPanel.add(listOrderPanel);
        
        // Add change listeners to spinners
        ChangeListener spinnerChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateDrawObjList();
            }
        };
        
        counterCWiseSpinner.addChangeListener(spinnerChangeListener);
        centralHueSpinner.addChangeListener(spinnerChangeListener);
        clockWiseSpinner.addChangeListener(spinnerChangeListener);
        
        // Add listeners to radio buttons for list ordering
        aToZButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (aToZButton.isSelected()) {
                    isAscendingOrder = true;
                    isSortByHue = false;
                    updateDrawObjList();
                }
            }
        });
        
        zToAButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (zToAButton.isSelected()) {
                    isAscendingOrder = false;
                    isSortByHue = false;
                    updateDrawObjList();
                }
            }
        });
        
        byHueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (byHueButton.isSelected()) {
                    isSortByHue = true;
                    updateDrawObjList();
                }
            }
        });
        
        // 3. buttonPanel - below spinnersPanel - now using BoxLayout for vertical stacking
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(new TitledBorder("Actions"));
        
        // First row of buttons - use FlowLayout with minimal gaps (hgap=0, vgap=0)
        JPanel topButtonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        
        // Create MonoTri button with minimal text and margins
        openMonoTriButton = new JButton("MonoTri");
        openMonoTriButton.setMargin(new Insets(1, 0, 1, 0)); // Minimal button margins
        openMonoTriButton.setFont(new Font(openMonoTriButton.getFont().getName(), Font.PLAIN, 10)); // Smaller font
        openMonoTriButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the selected DrawObj
                DrawObj selectedDrawObj = drawObjList.getSelectedValue();
                
                if (selectedDrawObj != null) {
                    // Get the TemplateWorkFrame using the existing method
                    TemplateWorkFrame templateWorkFrame = getParentTemplateWorkFrame();
                    
                    if (templateWorkFrame != null) {
                        // Get the hue from the selected DrawObj
                        ColorPtOstDot color = selectedDrawObj.getColorPtOstDotFillColor();
                        
                        // Create a HueNumGUI instance
                        HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(color.getStdCodeHueNum() + 1);
                        
                        System.out.println("DEBUG: Opening MonotriWorkFrame with hue: " + hueNumGUI.getUserFacingHueNum());
                        
                        // Create a new MonotriWorkFrame
                        MonotriWorkFrame newFrame = new MonotriWorkFrame(templateWorkFrame, hueNumGUI);
                        
                        // Make the frame visible - this was missing in our previous implementation
                        newFrame.setVisible(true);
                    }
                }
            }
        });
        
        openOneDrawObjStateButton = new JButton("OpenSelected");  // Shortened text
        openOneDrawObjStateButton.setMargin(new Insets(1, 0, 1, 0)); // Minimal button margins
        openOneDrawObjStateButton.setFont(new Font(openOneDrawObjStateButton.getFont().getName(), Font.PLAIN, 10)); // Smaller font
        openOneDrawObjStateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.openDrawObjStateFrame();
            }
        });
        
        openAllDrawObjStateButton = new JButton("OpenAll");
        openAllDrawObjStateButton.setMargin(new Insets(1, 0, 1, 0)); // Minimal button margins
        openAllDrawObjStateButton.setFont(new Font(openAllDrawObjStateButton.getFont().getName(), Font.PLAIN, 10)); // Smaller font
        openAllDrawObjStateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.openAllDrawObjStateFrames();
            }
        });
        
        // Add buttons to the top row with openMonoTriButton first (on the left)
        topButtonRow.add(openMonoTriButton);
        topButtonRow.add(openOneDrawObjStateButton);
        topButtonRow.add(openAllDrawObjStateButton);
        
        // Second row of buttons - use FlowLayout with minimal gaps (hgap=0, vgap=0)
        JPanel bottomButtonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        
        // Create and configure the plusMinusOneToSelectGroupButton
        plusMinusOneToSelectGroupButton = new JButton("+/-SelectGr");
        plusMinusOneToSelectGroupButton.setMargin(new Insets(1, 1, 1, 1)); // Minimal button margins
        plusMinusOneToSelectGroupButton.setEnabled(true); // Enable the button for display
        plusMinusOneToSelectGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DrawObj selectedDrawObj = drawObjList.getSelectedValue();
                if (selectedDrawObj != null && controller.getDrawObjRoster() != null) {
                    // Toggle the selection state of the DrawObj using the DrawObjRoster
                    DrawObjRoster roster = controller.getDrawObjRoster();
                    boolean isCurrentlySelected = roster.isSelected(selectedDrawObj);
                    TemplateWorkFrame templateWorkFrame = getParentTemplateWorkFrame();
                    
                    if (isCurrentlySelected) {
                        // If the removed object is the TWF focus, move focus BEFORE the
                        // removal so the SelectionEvent sees the resolved survivor: the
                        // first selected member in roster order excluding the removal
                        // target; null if none remain
                        if (templateWorkFrame != null
                                && templateWorkFrame.getSingleSelectedDrawObj() == selectedDrawObj) {
                            DrawObj survivor = null;
                            for (DrawObj candidate : roster.getTemplateDrawObjs()) {
                                if (candidate != selectedDrawObj && roster.isSelected(candidate)) {
                                    survivor = candidate;
                                    break;
                                }
                            }
                            templateWorkFrame.setSingleSelectedDrawObj(survivor);
                        }
                        roster.removeFromSelection(selectedDrawObj);
                    } else {
                        // Move focus to the new member BEFORE the addition so the
                        // SelectionEvent resolves the all-list highlight to it
                        if (templateWorkFrame != null) {
                            templateWorkFrame.setSingleSelectedDrawObj(selectedDrawObj);
                        }
                        roster.addToSelection(selectedDrawObj);
                    }
                    
                    // Force the list to repaint to show updated selection state
                    drawObjList.repaint();
                }
            }
        });
        
        plusAllSelectGroupButton = new JButton("clear+AllSelectGr");
        plusAllSelectGroupButton.setMargin(new Insets(1, 1, 1, 1)); // Minimal button margins
        plusAllSelectGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the DrawObjRoster from the controller
                DrawObjRoster roster = controller.getDrawObjRoster();
                if (roster != null) {
                    // First, clear all currently selected DrawObjs
                    roster.clearSelection();
                    
                    // Get the current list model
                    DefaultListModel<DrawObj> model = controller.getListModel();
                    
                    // If the list is not empty, add all DrawObjs to the selection
                    if (model.size() > 0) {
                        for (int i = 0; i < model.size(); i++) {
                            DrawObj drawObj = model.getElementAt(i);
                            roster.addToSelection(drawObj);
                        }
                    }
                    
                    // Force the list to repaint to show updated selection state
                    drawObjList.repaint();
                }
            }
        });
        
        // Add buttons to the bottom row with plusMinusOneToSelectGroupButton first (on the left)
        bottomButtonRow.add(plusMinusOneToSelectGroupButton);
        bottomButtonRow.add(plusAllSelectGroupButton);
        
        // Add button rows to buttonPanel with minimal spacing between rows
        buttonPanel.add(topButtonRow);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 1))); // Minimal spacing between rows
        buttonPanel.add(bottomButtonRow);
        
        // 4. drawObjColorPanel - at the bottom
        drawObjColorPanel = new JPanel();
        drawObjColorPanel.setLayout(new BorderLayout());
        drawObjColorPanel.setBorder(new TitledBorder("Selected DrawObj"));
        
        // Create a panel for the color square that will be positioned at the right
        JPanel colorSquareContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        // Enlarge the color square to 30x30 pixels
        drawObjColorSquarePanel = new JPanel();
        drawObjColorSquarePanel.setPreferredSize(new Dimension(30, 30));
        drawObjColorSquarePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        drawObjColorSquarePanel.setBackground(Color.WHITE);
        
        // Add the color square to its container
        colorSquareContainer.add(drawObjColorSquarePanel);
        
        // Create the text field with maximum width
        hueNumTextField = new JTextField();
        hueNumTextField.setEditable(false);
        
        // Create a checkbox for designating this frame as the receiver
        receiverCheckBox = new JCheckBox("Receive Updates");
        receiverCheckBox.setToolTipText("Designate this frame to receive hue updates");
        receiverCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selected = receiverCheckBox.isSelected();
                if (suppressCheckBoxEvents) {
                    // Change was driven by TemplateWorkFrame's designation sync, not a user click
                    return;
                }
                TemplateWorkFrame parentFrame = getParentTemplateWorkFrame();
                if (parentFrame == null) {
                    return;
                }
                if (selected) {
                    // Set this frame as the designated receiver
                    parentFrame.setDesignatedHueReceiver(HueRelativesFrame.this);
                } else {
                    // User-initiated uncheck: clear TWF's designation so the checkbox
                    // state and the designation model stay in sync
                    parentFrame.setDesignatedHueReceiver(null);
                }
            }
        }); // end receiverCheckBox ActionListener

        
        // Create a panel for the checkbox and text field
        JPanel textAndCheckboxPanel = new JPanel(new BorderLayout());
        textAndCheckboxPanel.add(hueNumTextField, BorderLayout.CENTER);
        textAndCheckboxPanel.add(receiverCheckBox, BorderLayout.NORTH);
        
        // Add components to the drawObjColorPanel
        drawObjColorPanel.add(textAndCheckboxPanel, BorderLayout.CENTER);
        drawObjColorPanel.add(colorSquareContainer, BorderLayout.EAST);
        
        // Add all panels to the main panel in the specified order (top to bottom)
        mainPanel.add(drawObjListPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(spinnersPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(drawObjColorPanel);
        
        setContentPane(mainPanel);
        
        // Set the controller's view reference
        controller.setView(this);
        
        // Initialize the list with default values - explicitly call with current spinner values
        int centralHue = (Integer) centralHueSpinner.getValue();
        int counterCWiseLimit = (Integer) counterCWiseSpinner.getValue();
        int clockWiseLimit = (Integer) clockWiseSpinner.getValue();
        controller.updateDrawObjList(centralHue, counterCWiseLimit, clockWiseLimit);
        
        // Make sure all components are properly laid out
        pack();
        setSize(300, 500); // Reset size after pack to ensure proper dimensions
    } // end constructor
    
    /**
     * Updates the list of DrawObjs based on the current spinner values and sort order.
     */
    private void updateDrawObjList() {
        int centralUserFacingHue = (Integer) centralHueSpinner.getValue();
        int counterCWiseLimit = (Integer) counterCWiseSpinner.getValue();
        int clockWiseLimit = (Integer) clockWiseSpinner.getValue();
        
        controller.updateDrawObjList(centralUserFacingHue, counterCWiseLimit, clockWiseLimit, isAscendingOrder, isSortByHue);
    }
    
    /**
     * Updates the DrawObj list with the provided list model.
     * 
     * @param listModel The list model containing DrawObjs
     */
    public void updateDrawObjList(DefaultListModel<DrawObj> listModel) {
        drawObjList.setModel(listModel);
        if (listModel.size() > 0) {
            drawObjList.setSelectedIndex(0);
        }
    }
    
    /**
     * Updates the color panel with information about the selected DrawObj.
     * 
     * @param drawObj The selected DrawObj
     */
    public void updateDrawObjColorPanel(DrawObj drawObj) {
        if (drawObj != null) {
            ColorPtOstDot colorPt = drawObj.getColorPtOstDotFillColor();
            OstHueColumnStep hcs = colorPt.getOstHCS();
            
            // Update the text field with DrawObj name and hue information
            String info = drawObj.getName() + " - Hue: " + (hcs.getStdCodeHueNum() + 1);
            hueNumTextField.setText(info);
            
            // Update the color square with the DrawObj's color
            Color color = drawObj.getColorPtOstDotFillColor().toAWTColor();
            drawObjColorSquarePanel.setBackground(color);
        } else {
            // Clear the display if no DrawObj is selected
            hueNumTextField.setText("");
            drawObjColorSquarePanel.setBackground(Color.WHITE);
        }
    }
    
    /**
     * Sets the central hue spinner value.
     * 
     * @param centralHue The central hue value (user-facing, 1-36)
     */
    public void setCentralHue(int centralHue) {
        centralHueSpinner.setValue(centralHue);
    }
    
    /**
     * Sets the counter-clockwise limit spinner value.
     * 
     * @param counterCWiseLimit The counter-clockwise limit value
     */
    public void setCounterCWiseLimit(int counterCWiseLimit) {
        counterCWiseSpinner.setValue(counterCWiseLimit);
    }
    
    /**
     * Sets the clockwise limit spinner value.
     * 
     * @param clockWiseLimit The clockwise limit value
     */
    public void setClockWiseLimit(int clockWiseLimit) {
        clockWiseSpinner.setValue(clockWiseLimit);
    }
    
    /**
     * Custom cell renderer for the DrawObj list.
     * Renders each list item with a background color based on the DrawObj's hue.
     * Unselected items use fillOrder=31 color, selected items use fillOrder=18 (lighter) color.
     */
    private class DrawObjListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof DrawObj) {
                DrawObj drawObj = (DrawObj) value;
                label.setText(drawObj.getName());
                
                // Set background color based on hue
                ColorPtOstDot colorPt = drawObj.getColorPtOstDotFillColor();
                if (colorPt != null) {
                    OstHueColumnStep hcs = colorPt.getOstHCS();
                    if (hcs != null) {
                        int codeHueNum = hcs.getStdCodeHueNum();
                        Color hueBackgroundColor;
                        
                        if (isSelected) {
                            // Use lighter color (fillOrder=18) for selected items
                            hueBackgroundColor = controller.getHueBackgroundColorSelected(codeHueNum);
                        } else {
                            // Use standard color (fillOrder=31) for unselected items
                            hueBackgroundColor = controller.getHueBackgroundColor(codeHueNum);
                        }
                        
                        label.setBackground(hueBackgroundColor);
                        label.setOpaque(true);
                    }
                }
            }
            
            return label;
        }
    }
    
    /**
     * Gets the parent TemplateWorkFrame for coordination between receivers.
     * 
     * @return The parent TemplateWorkFrame
     */
    @Override
    public TemplateWorkFrame getParentTemplateWorkFrame() {
        if (controller != null) {
            return controller.getTemplateWorkFrame();
        }
        return null;
    } // end method getParentTemplateWorkFrame
    


    
    /**
     * Receives a hue update and updates the display accordingly.
     * 
     * @param hueNum The HueNumGUI instance containing the new hue value
     */
    @Override
    public void receiveHueUpdate(HueNumGUI hueNum) {
        if (hueNum != null) {
            int userFacingHue = hueNum.getUserFacingHueNum();
            int currentHue = (Integer) centralHueSpinner.getValue();
            setCentralHue(userFacingHue);
            // The spinner fires its ChangeListener (which refreshes the list) only
            // when the value actually changes. When the hue is unchanged, refresh
            // explicitly so the list still picks up current roster state.
            if (currentHue == userFacingHue) {
                updateDrawObjList();
            }
        }
    }
    
    /**
     * Checks if this receiver is currently designated to receive hue updates.
     * 
     * @return true if this receiver is designated, false otherwise
     */
    @Override
    public boolean isDesignatedReceiver() {
        return receiverCheckBox.isSelected();
    }
    
    /**
     * Sets whether this receiver is designated to receive hue updates.
     * Called by TemplateWorkFrame's designation sync; the checkbox listener is
     * suppressed during the update so it is not mistaken for a user click.
     * 
     * @param designated true to designate this receiver, false otherwise
     */
    @Override
    public void setDesignatedReceiver(boolean designated) {
        suppressCheckBoxEvents = true;
        receiverCheckBox.setSelected(designated);
        suppressCheckBoxEvents = false;
    }
    
    /**
     * Gets the unique frame ID for this HueRelativesFrame instance.
     * 
     * @return the frame ID
     */
    public int getFrameId() {
        return frameId;
    }
}
