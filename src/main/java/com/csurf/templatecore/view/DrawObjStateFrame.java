package com.csurf.templatecore.view;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame;
import com.csurf.templatecore.controller.DrawObjStateViewController;
import com.csurf.templatecore.model.DrawObj;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Class purpose is to provide a view of the current state of a selected DrawObj instance
 * Immediate use: assess and then update Templates that may have subtle "off-by-one" errors in ColorPtOstDot hueNumCode
 *
 */
public class DrawObjStateFrame extends JFrame {
    private final JTextField drawObjNameTextField;
    private final JTextArea drawObjInfoTextArea;
    private final JButton updateThisDisplayButton;
    private final JButton hueRelativesButton;
    private final JButton sendHueButton;
    private final JButton plusMinusSelectGroupButton;
    private final JTextField templateNameTextField;
    private final DrawObjStateViewController controller;
    
    // Store the original height to restore when un-minimizing
    private int originalHeight;
    private boolean isMinimized = false;
    
    // Toggle button for minimize/restore
    private JButton toggleButton;

    // Color chip panel to display the DrawObj's color
    private JPanel drawObjColorChip;

    /**
     * Constructor for DrawObjStateFrame
     * @param controller the controller for this view
     */
    public DrawObjStateFrame(DrawObjStateViewController controller) {
        this.controller = controller;
        setTitle("DrawObj State Info");
        setSize(250, 315);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Override the default minimize behavior
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowIconified(java.awt.event.WindowEvent e) {
                // Prevent standard minimization and use our custom iconify instead
                setExtendedState(JFrame.NORMAL);
                toggleMinimized();
            }
        });

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Initialize components
        drawObjNameTextField = new JTextField();
        drawObjNameTextField.setEditable(false);
        drawObjNameTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, drawObjNameTextField.getPreferredSize().height));

        // Create a single toggle button that changes its text based on state
        toggleButton = new JButton("_");
        toggleButton.setToolTipText("Toggle minimize/restore");
        toggleButton.setMargin(new Insets(0, 2, 0, 2));
        toggleButton.setPreferredSize(new Dimension(20, 20));
        toggleButton.setFont(new Font(toggleButton.getFont().getName(), Font.PLAIN, 10));
        toggleButton.addActionListener(e -> toggleMinimized());

        // Create color chip panel to display the DrawObj's color
        drawObjColorChip = new JPanel();
        drawObjColorChip.setPreferredSize(new Dimension(20, 20));
        drawObjColorChip.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        drawObjColorChip.setBackground(Color.WHITE); // Default color
        
        // Add mouse listener to detect double-clicks on the color chip
        drawObjColorChip.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click detected, open MonotriWorkFrame
                    openMonotriWorkFrameWithCurrentHue();
                }
            }
        });
        
        // Set cursor to indicate the panel is clickable
        drawObjColorChip.setCursor(new Cursor(Cursor.HAND_CURSOR));
        drawObjColorChip.setToolTipText("Double-click to open MonotriWorkFrame with this hue");

        // Create a panel for the title area with the toggle button
        JPanel titlePanel = new JPanel(new BorderLayout(5, 0)); // Add horizontal gap between components
        titlePanel.add(drawObjNameTextField, BorderLayout.WEST);
        titlePanel.add(drawObjColorChip, BorderLayout.CENTER);
        titlePanel.add(toggleButton, BorderLayout.EAST);
        
        // Adjust the size of the drawObjNameTextField to make room for the color chip
        drawObjNameTextField.setPreferredSize(new Dimension(150, drawObjNameTextField.getPreferredSize().height));
        
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, drawObjNameTextField.getPreferredSize().height));

        drawObjInfoTextArea = new JTextArea();
        drawObjInfoTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(drawObjInfoTextArea);
        scrollPane.setPreferredSize(new Dimension(200, 230));

        updateThisDisplayButton = new JButton("Update This Info");
        updateThisDisplayButton.addActionListener(e -> controller.updateDisplayedInfo());

        hueRelativesButton = new JButton("see hueRelatives");
        hueRelativesButton.addActionListener(e -> {
            if (controller != null) {
                // Get the target DrawObj from the controller
                DrawObj targetDrawObj = controller.getTargetDrawObj();
                
                // Get the TemplateWorkFrame from the controller
                TemplateWorkFrame templateWorkFrame = controller.getTemplateWorkFrame();

                // If we have a valid TemplateWorkFrame, call openHueRelativesFrame() with the target DrawObj
                if (templateWorkFrame != null) {
                    // Use the overloaded method that accepts a DrawObj to ensure we use the correct hue
                    templateWorkFrame.openHueRelativesFrame(targetDrawObj);
                } else {
                    System.out.println("Warning: Cannot open HueRelativesFrame, TemplateWorkFrame is null");
                }
            }
        }); // end action listener for hueRelativesButton


        sendHueButton = new JButton("Update HueRelatives");
        sendHueButton.addActionListener(e -> sendHueUpdate());

        plusMinusSelectGroupButton = new JButton("+/- selectGroup");
        plusMinusSelectGroupButton.addActionListener(e -> {
            if (controller != null) {
                boolean isNowSelected = controller.toggleDrawObjSelection();
                // Update button appearance based on selection state if needed
                // This could be enhanced later to show visual feedback
            }
        }); //

        templateNameTextField = new JTextField();
        templateNameTextField.setEditable(false);
        templateNameTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, templateNameTextField.getPreferredSize().height));

        // Add components to panel
        mainPanel.add(titlePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(scrollPane);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // Create a simple button panel with FlowLayout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.setPreferredSize(new Dimension(buttonPanel.getPreferredSize().width, 40));
        
        // Set button height to 20px and adjust font size
        Font smallerFont = new Font(updateThisDisplayButton.getFont().getName(), Font.PLAIN, 10);
        
        // Calculate minimum width based on text
        FontMetrics metrics = updateThisDisplayButton.getFontMetrics(smallerFont);
        
        // Set button sizes with minimal padding (just 4 pixels on each side)
        int updateWidth = metrics.stringWidth(updateThisDisplayButton.getText()) + 8;
        updateThisDisplayButton.setPreferredSize(new Dimension(updateWidth, 20));
        updateThisDisplayButton.setMargin(new Insets(0, 2, 0, 2));
        updateThisDisplayButton.setFont(smallerFont);
        
        int hueRelativesWidth = metrics.stringWidth(hueRelativesButton.getText()) + 8;
        hueRelativesButton.setPreferredSize(new Dimension(hueRelativesWidth, 20));
        hueRelativesButton.setMargin(new Insets(0, 2, 0, 2));
        hueRelativesButton.setFont(smallerFont);
        
        int sendHueWidth = metrics.stringWidth(sendHueButton.getText()) + 8;
        sendHueButton.setPreferredSize(new Dimension(sendHueWidth, 20));
        sendHueButton.setMargin(new Insets(0, 2, 0, 2));
        sendHueButton.setFont(smallerFont);
        
        int selectGroupWidth = metrics.stringWidth(plusMinusSelectGroupButton.getText()) + 8;
        plusMinusSelectGroupButton.setPreferredSize(new Dimension(selectGroupWidth, 20));
        plusMinusSelectGroupButton.setMargin(new Insets(0, 2, 0, 2));
        plusMinusSelectGroupButton.setFont(smallerFont);
        
        // Add buttons directly to the buttonPanel
        buttonPanel.add(updateThisDisplayButton);
        buttonPanel.add(hueRelativesButton);
        buttonPanel.add(sendHueButton);
        buttonPanel.add(plusMinusSelectGroupButton);
        
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(templateNameTextField);

        setContentPane(mainPanel);
    } // end method initializeUI



    public void updateDisplay(String drawObjName, String templateName, String infoText) {
        drawObjNameTextField.setText(drawObjName);
        templateNameTextField.setText(templateName);
        drawObjInfoTextArea.setText(infoText);
        
        // Update the color chip with the current DrawObj's color
        if (controller != null && controller.getTargetDrawObj() != null) {
            ColorPtOstDot colorPt = controller.getTargetDrawObj().getColorPtOstDotFillColor();
            if (colorPt != null) {
                drawObjColorChip.setBackground(colorPt.getAsColor());
            } else {
                drawObjColorChip.setBackground(Color.WHITE);
            }
        } else {
            drawObjColorChip.setBackground(Color.WHITE);
        }
        
        // Set the frame title to the DrawObj name for better identification when iconified
        setTitle(drawObjName);
    }

    public JButton getPlusMinusSelectGroupButton() {
        return plusMinusSelectGroupButton;
    }

    /**
     * Sets the font size for the information text area.
     * 
     * @param fontSize the font size in pixels
     */
    public void setInfoTextFontSize(int fontSize) {
        Font font = new Font(drawObjInfoTextArea.getFont().getName(), Font.PLAIN, fontSize);
        drawObjInfoTextArea.setFont(font);
    }
    
    /**
     * Toggles between normal and minimized states.
     * When minimized, only the title bar is visible.
     * 
     * @return true if the frame is now minimized, false otherwise
     */
    public boolean toggleMinimized() {
        if (!isMinimized) {
            // Save current height before minimizing
            originalHeight = getHeight();
            
            // Get the height of just the title bar plus a little extra to ensure buttons are visible
            Insets insets = getInsets();
            int titleBarHeight = insets.top + 30;  // Add extra height to ensure buttons are visible
            
            // Set the frame height to show the title bar plus a bit more
            setSize(getWidth(), titleBarHeight);
            isMinimized = true;
            
            // Change button text to indicate restore action
            toggleButton.setText("▢");
            toggleButton.setToolTipText("Restore window");
            
            // Force a repaint to ensure the button is visible
            repaint();
        } else {
            // Restore original height
            setSize(getWidth(), originalHeight);
            isMinimized = false;
            
            // Change button text to indicate minimize action
            toggleButton.setText("_");
            toggleButton.setToolTipText("Minimize window");
        }
        
        return isMinimized;
    }
    
    /**
     * Checks if the frame is currently minimized.
     * 
     * @return true if minimized, false otherwise
     */
    public boolean isMinimized() {

        return isMinimized;
    } // end method isMinimized()



    /**
     * Sends the current DrawObj's hue to the designated HueRelativesFrame receiver.
     * Called when the sendHueButton is clicked.
     * Reads the live hue from the target DrawObj (not this frame's possibly-stale
     * display) and delegates delivery to TemplateWorkFrame.deliverHueUpdate(), which
     * owns receiver selection and designation policy.
     */
    private void sendHueUpdate() {
        if (controller == null || controller.getTargetDrawObj() == null) {
            return;
        }
        
        // Get the live hue from the target DrawObj
        ColorPtOstDot colorPt = controller.getTargetDrawObj().getColorPtOstDotFillColor();
        OstHueColumnStep hcs = colorPt.getOstHCS();
        int stdCodeHueNum = hcs.getStdCodeHueNum();
        HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(stdCodeHueNum + 1);
        
        TemplateWorkFrame templateWorkFrame = controller.getTemplateWorkFrame();
        if (templateWorkFrame != null) {
            templateWorkFrame.deliverHueUpdate(hueNumGUI);
        }
    } // end method sendHueUpdate()




    /**
     * Opens a MonotriWorkFrame with the current DrawObj's hue.
     * This is called when the user double-clicks on the color chip.
     */
    private void openMonotriWorkFrameWithCurrentHue() {
        if (controller != null && controller.getTargetDrawObj() != null) {
            // Get the current hue from the target DrawObj
            ColorPtOstDot colorPt = controller.getTargetDrawObj().getColorPtOstDotFillColor();
            if (colorPt != null) {
                OstHueColumnStep hcs = colorPt.getOstHCS();
                int stdCodeHueNum = hcs.getStdCodeHueNum();
                
                // Convert internal hue (0-35) to user-facing (1-36) for HueNumGUI
                HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(stdCodeHueNum + 1);
                
                // Get the TemplateWorkFrame from the controller
                TemplateWorkFrame templateWorkFrame = controller.getTemplateWorkFrame();
                if (templateWorkFrame != null) {
                    // Create and show the MonotriWorkFrame with the current hue
                    MonotriWorkFrame monotriWorkFrame = new MonotriWorkFrame(templateWorkFrame, hueNumGUI, colorPt);
                    monotriWorkFrame.setVisible(true);
                }
            }
        }
    }
}
