package com.csurf.artintellig.view;

import com.csurf.artintellig.controller.CircDialMouseController;
import com.csurf.artintellig.model.CircDialTryoutMouser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A panel that displays the CircDialTryoutMouser circle.
 * Centers the circle in the panel and handles basic painting.
 */
public class CircMouserPanel extends JPanel {
    private final CircDialTryoutMouser dial;
    private static final int PADDING = 2;
    
    public CircMouserPanel(CircMouserView frameContainer, int width) {
        dial = new CircDialTryoutMouser(frameContainer, width);
        setLayout(new GridBagLayout()); // Use GridBagLayout for perfect centering
        
        // Add the dial with some padding around it
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        add(dial, gbc);
        
        // Set preferred size to accommodate the dial plus padding
        Dimension dialSize = dial.getPreferredSize();
        setPreferredSize(new Dimension(
            dialSize.width + (PADDING * 4),
            dialSize.height + (PADDING * 4)
        ));
        
        // Add mouse controller
        CircDialMouseController mouseController = new CircDialMouseController(dial);
        dial.addMouseListener(mouseController);
        System.out.println("CircMouserPanel: Added mouse listener to dial component");
        
        // Also add mouse listener to panel for debugging
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Panel received mouse click at: " + e.getPoint());
            }
        });
    }
}
