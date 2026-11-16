package com.csurf.artintellig.view;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;

/**
 * A panel that contains a CircularDial and its associated angle label.
 * Keeps the dial and its display tightly coupled.
 */
public class DialPanel extends JPanel {
    private final CircularDial dial;
    private final JLabel angleLabel;
    private final int ringNumber;
    
    public DialPanel(int ringNumber) {
        super(new BorderLayout(5, 5));
        this.ringNumber = ringNumber;
        
        setBorder(BorderFactory.createTitledBorder("Ring " + ringNumber));
        
        // Create and configure the dial
        dial = new CircularDial();
        dial.setRange(0.0, 360.0);
        
        // Create the angle label
        angleLabel = new JLabel("Angle: 0.0°", SwingConstants.CENTER);
        
        // Add components
        add(dial, BorderLayout.CENTER);
        add(angleLabel, BorderLayout.SOUTH);
    }
    
    public void addDialListener(PropertyChangeListener listener) {
        dial.addPropertyChangeListener("value", listener);
    }
    
    public CircularDial getDial() {
        return dial;
    }
    
    public void updateAngleDisplay(double angle) {
        angleLabel.setText(String.format("Angle: %.1f°", angle));
    }
    
    public int getRingNumber() {
        return ringNumber;
    }
}
