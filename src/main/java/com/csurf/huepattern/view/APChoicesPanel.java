package com.csurf.huepattern.view;

import javax.swing.*;
import java.awt.*;

/**
 * Choices panel for AnchorPattern workflow controls.
 * Contains workflow buttons and APLevel selector.
 * Fixed size: 640x50 pixels (full width of frame).
 */
public class APChoicesPanel extends JPanel {
    private static final int PANEL_WIDTH = 640;
    private static final int PANEL_HEIGHT = 50;

    public APChoicesPanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setMinimumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setMaximumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(new Color(230, 230, 250));
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        // Controls will be added later
    }
} // end class APChoicesPanel
