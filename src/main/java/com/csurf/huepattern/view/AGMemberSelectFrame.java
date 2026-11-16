package com.csurf.huepattern.view;

import com.csurf.huepattern.controller.AGSelectFrameController;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.view.TemplateWorkFrame;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * AGMemberSelectFrame displays DrawObjs that are members of a specific AG
 * and fall within a specified hue range.
 * 
 * Layout (identical to HueRelativesFrame):
 * - List panel at top (titled "AG Members in Hue Range")
 * - Spinners panel (centralHueSpinner, counterCWiseSpinner, clockWiseSpinner)
 * - Button panel with two rows (MonoTri, OpenSelected, OpenAll, +/-SelectGr, clear+AllSelectGr)
 * - Color display panel at bottom
 * 
 * Functionality:
 * - Displays AG members filtered by hue range
 * - Allows user to adjust hue range via spinners
 * - Allows user to toggle DrawObjs in/out of selection group
 * - Allows user to open MonotriWorkFrame and DrawObjStateFrames
 */
public class AGMemberSelectFrame extends JFrame {
    private static final Logger logger = Logger.getLogger(AGMemberSelectFrame.class.getName());
    
    private static final int FRAME_WIDTH = 300;
    private static final int FRAME_HEIGHT = 500;
    
    private final AGSelectFrameController controller;
    private JList<DrawObj> drawObjList;
    private JSpinner centralHueSpinner;
    private JSpinner counterCWiseSpinner;
    private JSpinner clockWiseSpinner;
    private JButton openMonoTriButton;
    private JButton openOneDrawObjStateButton;
    private JButton openAllDrawObjStateButton;
    private JButton plusMinusOneToSelectGroupButton;
    private JButton plusAllSelectGroupButton;
    private JPanel drawObjColorSquarePanel;
    private JTextField hueNumTextField;
    private JCheckBox receiverCheckBox;
    
    /**
     * Creates a new AGMemberSelectFrame.
     * 
     * @param controller The AGSelectFrameController managing this frame
     */
    public AGMemberSelectFrame(AGSelectFrameController controller) {
        this.controller = controller;
        int someAGnum = controller.getAGNum();
        
        logger.fine("AGMemberSelectFrame: Creating frame");
        
        setTitle("Shapes in AG-" +someAGnum + " Hue Range");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setLocationRelativeTo(null);
        
        // Create main panel with BoxLayout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 1. drawObjListPanel - at the top
        JPanel drawObjListPanel = new JPanel();
        drawObjListPanel.setLayout(new BorderLayout());
        drawObjListPanel.setBorder(new TitledBorder("AG Members"));
        
        drawObjList = new JList<>();
        drawObjList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        drawObjList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    DrawObj selectedDrawObj = drawObjList.getSelectedValue();
                    controller.handleDrawObjSelection(selectedDrawObj);
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(drawObjList);
        drawObjListPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 2. spinnersPanel - hue range controls (matching HueRelativesFrame layout exactly)
        JPanel spinnersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        spinnersPanel.setBorder(new TitledBorder("Hue Range Controls"));
        
        // Counter-clockwise spinner
        JPanel ccwPanel = new JPanel(new BorderLayout());
        JLabel ccwLabel = new JLabel("CCW");
        ccwLabel.setHorizontalAlignment(JLabel.CENTER);
        counterCWiseSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 12, 1));
        counterCWiseSpinner.setPreferredSize(new Dimension(60, 25));
        ccwPanel.add(ccwLabel, BorderLayout.NORTH);
        ccwPanel.add(counterCWiseSpinner, BorderLayout.CENTER);
        
        // Central hue spinner
        JPanel centralPanel = new JPanel(new BorderLayout());
        JLabel centralLabel = new JLabel("CtrHue");
        centralLabel.setHorizontalAlignment(JLabel.CENTER);
        int desiredCentralHue = controller.getCurrUserFacingHue();
        centralHueSpinner = new JSpinner(new SpinnerNumberModel(desiredCentralHue, 1, 36, 1));
        centralHueSpinner.setPreferredSize(new Dimension(60, 25));
        centralPanel.add(centralLabel, BorderLayout.NORTH);
        centralPanel.add(centralHueSpinner, BorderLayout.CENTER);
        
        // Clockwise spinner
        JPanel cwPanel = new JPanel(new BorderLayout());
        JLabel cwLabel = new JLabel("CW");
        cwLabel.setHorizontalAlignment(JLabel.CENTER);
        clockWiseSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 12, 1));
        clockWiseSpinner.setPreferredSize(new Dimension(60, 25));
        cwPanel.add(cwLabel, BorderLayout.NORTH);
        cwPanel.add(clockWiseSpinner, BorderLayout.CENTER);
        
        // Add spinners to spinnersPanel
        spinnersPanel.add(ccwPanel);
        spinnersPanel.add(centralPanel);
        spinnersPanel.add(cwPanel);
        
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
        
        // 3. buttonPanel - action buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(new TitledBorder("Actions"));
        
        // First row of buttons
        JPanel topButtonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        
        openMonoTriButton = new JButton("MonoTri");
        openMonoTriButton.setMargin(new Insets(1, 0, 1, 0));
        openMonoTriButton.setFont(new Font(openMonoTriButton.getFont().getName(), Font.PLAIN, 10));
        openMonoTriButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DrawObj selectedDrawObj = drawObjList.getSelectedValue();
                
                if (selectedDrawObj != null) {
                    TemplateWorkFrame templateWorkFrame = getParentTemplateWorkFrame();
                    
                    if (templateWorkFrame != null) {
                        ColorPtOstDot color = selectedDrawObj.getColorPtOstDotFillColor();
                        if (color != null) {
                            HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(color.getStdCodeHueNum() + 1);
                            
                            logger.fine("AGMemberSelectFrame: Opening MonotriWorkFrame with hue: " + hueNumGUI.getUserFacingHueNum());
                            
                            com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame newFrame = new com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame(templateWorkFrame, hueNumGUI);
                            newFrame.setVisible(true);
                        }
                    }
                }
            }
        });
        
        openOneDrawObjStateButton = new JButton("OpenSelected");
        openOneDrawObjStateButton.setMargin(new Insets(1, 0, 1, 0));
        openOneDrawObjStateButton.setFont(new Font(openOneDrawObjStateButton.getFont().getName(), Font.PLAIN, 10));
        openOneDrawObjStateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.openDrawObjStateFrame();
            }
        });
        
        openAllDrawObjStateButton = new JButton("OpenAll");
        openAllDrawObjStateButton.setMargin(new Insets(1, 0, 1, 0));
        openAllDrawObjStateButton.setFont(new Font(openAllDrawObjStateButton.getFont().getName(), Font.PLAIN, 10));
        openAllDrawObjStateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.openAllDrawObjStateFrames();
            }
        });
        
        topButtonRow.add(openMonoTriButton);
        topButtonRow.add(openOneDrawObjStateButton);
        topButtonRow.add(openAllDrawObjStateButton);
        
        // Second row of buttons
        JPanel bottomButtonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        
        plusMinusOneToSelectGroupButton = new JButton("+/-SelectGr");
        plusMinusOneToSelectGroupButton.setMargin(new Insets(1, 1, 1, 1));
        plusMinusOneToSelectGroupButton.setEnabled(true);
        plusMinusOneToSelectGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DrawObj selectedDrawObj = drawObjList.getSelectedValue();
                if (selectedDrawObj != null && controller.getDrawObjRoster() != null) {
                    DrawObjRoster roster = controller.getDrawObjRoster();
                    boolean isCurrentlySelected = roster.isSelected(selectedDrawObj);
                    
                    if (isCurrentlySelected) {
                        roster.removeFromSelection(selectedDrawObj);
                    } else {
                        roster.addToSelection(selectedDrawObj);
                    }
                    
                    drawObjList.repaint();
                }
            }
        });
        
        plusAllSelectGroupButton = new JButton("clear+AllSelectGr");
        plusAllSelectGroupButton.setMargin(new Insets(1, 1, 1, 1));
        plusAllSelectGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DrawObjRoster roster = controller.getDrawObjRoster();
                if (roster != null) {
                    roster.clearSelection();
                    
                    DefaultListModel<DrawObj> model = controller.getListModel();
                    
                    if (model.size() > 0) {
                        for (int i = 0; i < model.size(); i++) {
                            DrawObj drawObj = model.getElementAt(i);
                            roster.addToSelection(drawObj);
                        }
                    }
                    
                    drawObjList.repaint();
                }
            }
        });
        
        bottomButtonRow.add(plusMinusOneToSelectGroupButton);
        bottomButtonRow.add(plusAllSelectGroupButton);
        
        buttonPanel.add(topButtonRow);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 1)));
        buttonPanel.add(bottomButtonRow);
        
        // 4. drawObjColorPanel - at the bottom
        JPanel drawObjColorPanel = new JPanel();
        drawObjColorPanel.setLayout(new BorderLayout());
        drawObjColorPanel.setBorder(new TitledBorder("Selected DrawObj"));
        
        JPanel colorSquareContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        drawObjColorSquarePanel = new JPanel();
        drawObjColorSquarePanel.setPreferredSize(new Dimension(30, 30));
        drawObjColorSquarePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        drawObjColorSquarePanel.setBackground(Color.WHITE);
        
        colorSquareContainer.add(drawObjColorSquarePanel);
        
        hueNumTextField = new JTextField();
        hueNumTextField.setEditable(false);
        
        receiverCheckBox = new JCheckBox("Receive Updates");
        receiverCheckBox.setToolTipText("Designate this frame to receive hue updates");
        receiverCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Placeholder for hue receiver functionality
                // AGMemberSelectFrame does not currently implement IHueReceiver
                logger.fine("AGMemberSelectFrame: Receive Updates checkbox toggled");
            }
        });
        
        JPanel textAndCheckboxPanel = new JPanel(new BorderLayout());
        textAndCheckboxPanel.add(hueNumTextField, BorderLayout.CENTER);
        textAndCheckboxPanel.add(receiverCheckBox, BorderLayout.NORTH);
        
        drawObjColorPanel.add(textAndCheckboxPanel, BorderLayout.CENTER);
        drawObjColorPanel.add(colorSquareContainer, BorderLayout.EAST);
        
        // Add all panels to main panel
        mainPanel.add(drawObjListPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(spinnersPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(drawObjColorPanel);
        
        setContentPane(mainPanel);
        
        // Set the controller's view reference
        controller.setTheViewWindow(this);
        
        // Initialize the list with default values
        int centralHue = (Integer) centralHueSpinner.getValue();
        int counterCWiseLimit = (Integer) counterCWiseSpinner.getValue();
        int clockWiseLimit = (Integer) clockWiseSpinner.getValue();
        controller.updateDrawObjList(centralHue, counterCWiseLimit, clockWiseLimit);
        
        pack();
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        
        logger.fine("AGMemberSelectFrame: Frame created successfully");
    }
    
    /**
     * Updates the list of DrawObjs based on the current spinner values.
     */
    private void updateDrawObjList() {
        int centralUserFacingHue = (Integer) centralHueSpinner.getValue();
        int counterCWiseLimit = (Integer) counterCWiseSpinner.getValue();
        int clockWiseLimit = (Integer) clockWiseSpinner.getValue();
        controller.updateDrawObjList(centralUserFacingHue, counterCWiseLimit, clockWiseLimit);
    }
    
    /**
     * Updates the JList with the provided list model.
     * Called by the controller when the list needs to be updated.
     * 
     * @param listModel The updated list model
     */
    public void updateDrawObjList(DefaultListModel<DrawObj> listModel) {
        drawObjList.setModel(listModel);
    }
    
    /**
     * Updates the color display panel with the selected DrawObj's color.
     * Called by the controller when a DrawObj is selected.
     * 
     * @param drawObj The selected DrawObj, or null if no selection
     */
    public void updateDrawObjColorPanel(DrawObj drawObj) {
        if (drawObj == null) {
            drawObjColorSquarePanel.setBackground(Color.WHITE);
            hueNumTextField.setText("");
            return;
        }
        
        try {
            ColorPtOstDot colorPt = drawObj.getColorPtOstDotFillColor();
            if (colorPt != null) {
                // Convert to AWT Color
                Color awtColor = colorPt.toAWTColor();
                drawObjColorSquarePanel.setBackground(awtColor);
                
                // Update hue text field
                int userFacingHue = colorPt.getStdCodeHueNum() + 1;
                hueNumTextField.setText("Hue: " + userFacingHue);
            } else {
                drawObjColorSquarePanel.setBackground(Color.WHITE);
                hueNumTextField.setText("(no color)");
            }
        } catch (Exception e) {
            logger.warning("AGMemberSelectFrame: Error updating color panel: " + e.getMessage());
            drawObjColorSquarePanel.setBackground(Color.WHITE);
            hueNumTextField.setText("(error)");
        }
    }
    
    /**
     * Sets the central hue spinner to the specified user-facing hue value.
     * This method is called after frame creation to update the spinner to the correct hue.
     * 
     * @param userFacingHue The user-facing hue number (1-36)
     */
    public void setCentralHue(int userFacingHue) {
        if (userFacingHue >= 1 && userFacingHue <= 36) {
            centralHueSpinner.setValue(userFacingHue);
            logger.fine("AGMemberSelectFrame: Set central hue to " + userFacingHue);
        } else {
            logger.warning("AGMemberSelectFrame: Invalid hue value: " + userFacingHue);
        }
    }
    
    /**
     * Gets the parent TemplateWorkFrame.
     * 
     * @return The parent TemplateWorkFrame, or null if not found
     */
    private TemplateWorkFrame getParentTemplateWorkFrame() {
        return controller.getTemplateWorkFrame();
    }
}
