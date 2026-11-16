package com.csurf.artintellig.view;

import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame;

import javax.swing.*;
import java.awt.*;

/**
 * A graphical interface for testing methods that are related to handling user mouse input.
 * This view displays a dial and helpful text for data related to handling user mouse input.
 * Allows users to adjust the dial setting interactively.
 *
 * Features:
 * - One dial which is updated interactively
 * - One textfield which is updated interactively to display data related to the clicked location
 * - Method calcHueDecimal in class CircDialTryout is used to calculate the resulting hueDecimal value
 */
public class CircMouserView extends JFrame {
    private static final int PADDING = 2;
    private static int WINDOW_WIDTH;
    private final CircMouserPanel dialPanel;
    private JTextField testInfoTextfield;
    private JButton openMonoTriButton;
    private int lastRoundHueNum = 31;
    private double lastHueDecimal = 31.0;

    public CircMouserView(int windowWidth) {
        super("CircMouseTester");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        WINDOW_WIDTH = windowWidth;
        
        // Initialize components
        dialPanel = new CircMouserPanel(this, WINDOW_WIDTH);
        testInfoTextfield = new JTextField();
        testInfoTextfield.setPreferredSize(new Dimension(WINDOW_WIDTH + 30, 25));
        testInfoTextfield.setEditable(false);
        
        // Create and configure the Open MonoTri button
        openMonoTriButton = new JButton("Open MonoTri");
        openMonoTriButton.addActionListener(e -> {
            // Action to perform when button is clicked
            System.out.println("Open MonoTri button clicked with hue: " + getLastRoundHueNum());
            HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(getLastRoundHueNum());
            SwingUtilities.invokeLater(() -> {
                try {
                    MonotriWorkFrame someMonoTriFrame = new MonotriWorkFrame(someHueNumGUI);
                    someMonoTriFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    someMonoTriFrame.pack();
                    someMonoTriFrame.setLocationRelativeTo(null);
                    someMonoTriFrame.setVisible(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, 
                        "Error opening MonoTri: " + ex.getMessage(), 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            });
        });
        
        // Layout setup
        
        setLayout(new BorderLayout(PADDING, PADDING));
        add(dialPanel, BorderLayout.CENTER);
        
        // Create a panel for the button and text field
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Add button to NORTH and text field to CENTER
        controlPanel.add(openMonoTriButton, BorderLayout.NORTH);
        controlPanel.add(testInfoTextfield, BorderLayout.CENTER);
        
        // Add the control panel to the bottom of the frame
        add(controlPanel, BorderLayout.SOUTH);
        
        // Set frame properties
        pack();
        setLocationRelativeTo(null);
    } // end constructor

    private int getLastRoundHueNum() {
        return lastRoundHueNum;
    }


    public JTextField getTestInfoTextfield() {
        return testInfoTextfield;
    } // end method getTestInfoTextfield


    public void setLastHueDecimal(double someHueDecimal) {
        lastHueDecimal = someHueDecimal;
    }

    public void setLastRoundHueNum(int roundedHueNum) {
        lastRoundHueNum = roundedHueNum;
    }
} // end class
