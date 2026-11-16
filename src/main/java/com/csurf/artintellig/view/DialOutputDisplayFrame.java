package com.csurf.artintellig.view;

import com.csurf.templatecore.model.Template;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Window that displays the template angles received from the AnchorPatternView
 */
public class DialOutputDisplayFrame extends JFrame {
    private final JLabel ring1Label;
    private final JLabel ring2Label;
    private final JLabel ring3Label;
    private final JLabel ring4Label;
    private final DecimalFormat df = new DecimalFormat("#.##");

    public DialOutputDisplayFrame() {
        setTitle("DialOutputDisplayFrame");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(4, 2, 10, 10));
        
        // Create labels for each ring
        add(new JLabel("Ring 1 Angle:"));
        ring1Label = new JLabel("0.0°");
        add(ring1Label);
        
        add(new JLabel("Ring 2 Angle:"));
        ring2Label = new JLabel("0.0°");
        add(ring2Label);
        
        add(new JLabel("Ring 3 Angle:"));
        ring3Label = new JLabel("0.0°");
        add(ring3Label);
        
        add(new JLabel("Ring 4 Angle:"));
        ring4Label = new JLabel("0.0°");
        add(ring4Label);
        
        // Set window properties
        setSize(250, 200);
        setLocationRelativeTo(null);  // Center on screen
        
        // Add padding
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * Updates the display with angles from a Template
     * @param template The template containing the angles to display
     */
    public void updateFromTemplate(Template template) {
        ring1Label.setText(df.format(template.getRing1Angle()) + "°");
        ring2Label.setText(df.format(template.getRing2Angle()) + "°");
        ring3Label.setText(df.format(template.getRing3Angle()) + "°");
        ring4Label.setText(df.format(template.getRing4Angle()) + "°");
    }
}
