package com.csurf.variationmodule.view;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.Template;
import com.csurf.variationmodule.model.TemplateVariationsResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    /** Called when the user clicks a thumbnail; receives the 0-based slot. */
    public interface VariationSelectionListener {
        void variationSelected(int slot);
    }

    private final UUID windowId;
    private final List<ThumbnailPanel> thumbnailPanels;
    private final JPanel controlPanel;
    private final TemplateVariationsResult variationsResult;
    private VariationSelectionListener selectionListener;

    /**
     * Creates the variation display window.
     * @param variationsResult the variation Templates to render (one per
     *                         thumbnail; 12 expected); retained so click
     *                         selection can recover the chosen Template
     * @param windowId unique ID for this window (for future mediator routing)
     * @param title window title (typically "Variations: <templateName>")
     */
    public VariationDisplayFrame(TemplateVariationsResult variationsResult,
                                 UUID windowId, String title) {
        super(title);
        this.windowId = windowId;
        this.variationsResult = variationsResult;
        this.thumbnailPanels = new ArrayList<>();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Main grid of 12 thumbnail panels, 5px gaps
        JPanel gridPanel = new JPanel(new GridLayout(GRID_ROWS, GRID_COLS, GRID_GAP, GRID_GAP));
        gridPanel.setBorder(new EmptyBorder(GRID_GAP, GRID_GAP, GRID_GAP, GRID_GAP));

        int count = (variationsResult != null) ? variationsResult.numVariations() : 0;
        for (int i = 0; i < VARIATION_COUNT; i++) {
            ThumbnailPanel thumbnailPanel = new ThumbnailPanel();
            // Row-major numbering matches GridLayout add order:
            // 1-3 first row, 4-6 second row, 7-9 third row, 10-12 fourth row
            int panelNum = i + 1;
            thumbnailPanel.setName("thumbnailPanel" + panelNum);
            if (i < count) {
                Template template = variationsResult.templateAt(i);
                thumbnailPanel.setDrawObjs(template.getAllDrawObjs().toArray(new DrawObj[0]));
                thumbnailPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                thumbnailPanel.setToolTipText("Click to open this variation in a TemplateWorkFrame");
            }
            final int slot = i;
            thumbnailPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (selectionListener != null && variationsResult != null
                            && slot < variationsResult.numVariations()) {
                        selectionListener.variationSelected(slot);
                    }
                }
            });
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
     * The variation Templates this frame renders, in slot order — the
     * handle a thumbnail mouseListener uses to recover the selected
     * Template ({@code templateAt(thumbnailIndex)}).
     */
    public TemplateVariationsResult getVariationsResult() {
        return variationsResult;
    }

    /**
     * Registers the handler for thumbnail clicks. The frame stays a dumb
     * view: it reports the clicked slot; the controller decides what to
     * do with it.
     */
    public void setVariationSelectionListener(VariationSelectionListener listener) {
        this.selectionListener = listener;
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

