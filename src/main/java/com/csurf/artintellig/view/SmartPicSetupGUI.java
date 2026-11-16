package com.csurf.artintellig.view;

import javax.swing.*;
import java.awt.*;

public class SmartPicSetupGUI extends JFrame {
    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 400;
    private static final int PADDING = 10;
    private final JPanel mainPanel;
    
    public SmartPicSetupGUI() {
        setTitle("Smart Picture Setup");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setPreferredSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setLocationRelativeTo(null); // Center on screen
        
        // Main panel with padding
        mainPanel = new JPanel(new BorderLayout(PADDING, PADDING));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        
        // Center panel for main content
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Models: from Template to SmartPic idealState etc"));
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Bottom panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton anchorPatternButton = new JButton("Open AnchorPatternView");
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        // Add action listener for anchor pattern button
        anchorPatternButton.addActionListener(e -> {
            AnchorPatternView view = new AnchorPatternView();
            view.setVisible(true);
        });
        
        buttonPanel.add(anchorPatternButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add main panel to frame
        setContentPane(mainPanel);
        pack();
    }
    
    public JPanel getMainPanel() {
        return mainPanel;
    }
}
