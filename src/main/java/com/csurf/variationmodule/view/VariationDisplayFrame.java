package com.csurf.variationmodule.view;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.Template;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Window that displays palette-variation renderings of a Template.
 * Holds a 4x3 grid of 12 ThumbnailPanels plus a controlPanel at the bottom.
 * Multiple instances may be open simultaneously (one per chosen template).
 */
public class VariationDisplayFrame extends JFrame {
    private static final int GRID_ROWS = 4;
    private static final int GRID_COLS = 3;
    private static final int VARIATION_COUNT = 12;
    private static final int GRID_GAP = 5;
    private static final int CONTROL_PANEL_HEIGHT = 60;

    private final UUID windowId;
    private final List<ThumbnailPanel> thumbnailPanels;
    private final JPanel controlPanel;

    /**
     * Creates the variation display window.
     * @param variationTemplates the Templates to render (one per thumbnail; 12 expected)
     * @param windowId unique ID for this window (for future mediator routing)
     * @param title window title (typically "Variations: <templateName>")
     */
    public VariationDisplayFrame(List<Template> variationTemplates, UUID windowId, String title) {
        super(title);
        this.windowId = windowId;
        this.thumbnailPanels = new ArrayList<>();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Main grid of 12 thumbnail panels, 5px gaps
        JPanel gridPanel = new JPanel(new GridLayout(GRID_ROWS, GRID_COLS, GRID_GAP, GRID_GAP));
        gridPanel.setBorder(new EmptyBorder(GRID_GAP, GRID_GAP, GRID_GAP, GRID_GAP));

        int count = (variationTemplates != null) ? variationTemplates.size() : 0;
        for (int i = 0; i < VARIATION_COUNT; i++) {
            ThumbnailPanel thumbnailPanel = new ThumbnailPanel();
            // Row-major numbering matches GridLayout add order:
            // 1-3 first row, 4-6 second row, 7-9 third row, 10-12 fourth row
            int panelNum = i + 1;
            thumbnailPanel.setName("thumbnailPanel" + panelNum);
            if (i < count) {
                Template template = variationTemplates.get(i);
                thumbnailPanel.setDrawObjs(template.getAllDrawObjs().toArray(new DrawObj[0]));
            }
            thumbnailPanels.add(thumbnailPanel);
            gridPanel.add(thumbnailPanel);
        }

        // controlPanel: placeholder for future user controls, 60px full-width strip
        controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(0, CONTROL_PANEL_HEIGHT));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(gridPanel, BorderLayout.CENTER);
        getContentPane().add(controlPanel, BorderLayout.SOUTH);

        pack();
        setResizable(false);
    }

    public UUID getWindowId() {
        return windowId;
    }

    /**
     * Gets a thumbnail panel by grid position (0-11, row-major add order).
     * Panel at index i has name "thumbnailPanel" + (i + 1).
     */
    public ThumbnailPanel getThumbnailPanel(int index) {
        return thumbnailPanels.get(index);
    }

    public List<ThumbnailPanel> getThumbnailPanels() {
        return thumbnailPanels;
    }

    public JPanel getControlPanel() {
        return controlPanel;
    }
} // end class VariationDisplayFrame

