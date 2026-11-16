package com.csurf.ostwald.domainmonotri.controller;

import com.csurf.ostwald.domainmonotri.events.OstdotEvent;
import com.csurf.ostwald.domainmonotri.events.OstdotListener;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.ostwald.domainmonotri.model.MonotriPage;
import com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame;
import com.csurf.ostwald.domainmonotri.view.MonotriDrawPanel;
import com.csurf.ostwald.domainmonotri.view.OstdotPanel;
import com.csurf.templatecore.events.IColorSelectionListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * Lower-level controller for MonoTri display functionality. Works with higher-level MonotriDisplayTopContrlr
 * to do actual drawing operations and user interactions. This controller renders the OstdotPanels
 */
public class MonotriRenderContrlr implements OstdotListener {
    private final MonotriDrawPanel drawPanel;
    private final MonotriWorkFrame frame;
    private MonotriPage currMonoTriPage;
    private final List<OstdotPanel> panels;
    private final Set<ColorPtOstDot> selectedColors;
    private SelectionMode currentMode;
    private final Set<OstdotListener> ostdotListeners;
    private boolean simplifiedViewEnabled = false; // Default to non-simplified view

    public MonotriRenderContrlr(MonotriDrawPanel drawPanel, MonotriWorkFrame frame) {
        this.drawPanel = drawPanel;
        this.frame = frame;
        this.panels = new ArrayList<>();
        this.selectedColors = new HashSet<>();
        this.currentMode = SelectionMode.DEFAULT;
        this.ostdotListeners = new HashSet<>();
    }

    /**
     * Sets the current selection mode.
     * 
     * @param mode The new selection mode
     */
    public void setSelectionMode(SelectionMode mode) {
        if (this.currentMode != mode) {
            SelectionMode oldMode = this.currentMode;
            this.currentMode = mode;

            // Clear selection when entering hand-paint mode
            if (mode == SelectionMode.HAND_PAINT) {
                clearSelection();
            }
        }
    }

    /**
     * Updates the MonotriPage being displayed and recreates the panels.
     * 
     * @param page The new MonotriPage to display
     */
    public void setCurrMonoTriPage(MonotriPage page) {
        this.currMonoTriPage = page;
        initializePanels();
        drawPanel.repaint();
    }

    /**
     * Creates and initializes OstdotPanels for each color in the MonotriPage.
     */
    private void initializePanels() {
        // Remove existing panels and clear listeners
        panels.forEach(panel -> {
            panel.removeOstdotListener(this);
            drawPanel.remove(panel);
        });
        panels.clear();

        // Create new panels if we have a page
        if (currMonoTriPage != null) {
            for (int fillOrder = 1; fillOrder <= currMonoTriPage.getNumOstDots(); fillOrder++) {
                ColorPtOstDot color = currMonoTriPage.getColorByFillOrder(fillOrder);
                if (color != null) {
                    OstdotPanel panel = new OstdotPanel(color);
                    panel.addOstdotListener(this);
                    panels.add(panel);
                    drawPanel.add(panel);
                }
            }
        }
    } // end method initializePanels


    /**
     * Gets the list of OstdotPanel instances.
     * 
     * @return List of OstdotPanel instances
     */
    public List<OstdotPanel> getOstDotPanels() {
        return new ArrayList<>(panels);
    }


    /**
     * Draws all OstdotPanels in their correct positions.
     * 
     * @param g2d Graphics context to draw with
     */
    public void drawDotPanels(Graphics2D g2d) {
        if (currMonoTriPage == null) return;

        final int DOT_SIZE = 30;
        final int DOT_SPACING = 3; // 090425 was 5
        final int HALFHEIGHT_ADJ = 28; // 070425 Added = adjustment for centering - positive addition moves "halfheight"down
        // 090425 added for "fine-tuning" overall vertical placement
        int FINE_TUNE_Y_START = -7;
        int halfPanelHeight
                = FINE_TUNE_Y_START + HALFHEIGHT_ADJ +drawPanel.getHeight() / 2 - (int) (DOT_SIZE + DOT_SPACING) / 2 ; // 070425 this controls entire triangle
        final int OVERALL_Y_ADJ = 44; // 090425 vertical adjustment for entire monoTri
        final int OVERALL_X_ADJ = 10; // 090425 horizontal adjustment for entire monoTri

        int panelIndex = 0;
        for (int fillOrder = 1; fillOrder <= currMonoTriPage.getNumOstDots(); fillOrder++) {
            ColorPtOstDot color = currMonoTriPage.getColorByFillOrder(fillOrder);
            if (color == null) continue;

            int column = color.getOstHCS().getColumn();
            int step = color.getOstHCS().getStep();
            // 090425 adjusted X-spacing by "-1" for tighter X-spacing
            int currentX =(column - 1)*(DOT_SIZE + DOT_SPACING-1) +OVERALL_X_ADJ; // 090425 WAS "* (DOT_SIZE + DOT_SPACING) + OVERALL_X_ADJ;"
            int bottomY = (int) (halfPanelHeight + 17 * column) - OVERALL_Y_ADJ; // 090425 WAS "(halfPanelHeight + 13.5 * column);"
            int y = bottomY - (step - 1) * (DOT_SIZE + DOT_SPACING);

            if (panelIndex < panels.size()) {
                OstdotPanel panel = panels.get(panelIndex++);
                panel.setBounds(currentX, y, DOT_SIZE, DOT_SIZE);
            }
        }
    } // end method drawDotPanels




    @Override
    public void ostdotEventOccurred(OstdotEvent event) {
        switch (event.getType()) {
            case SELECTED:
                handleSelection(event.getColor(), true);
                break;
            case DESELECTED:
                handleSelection(event.getColor(), false);
                break;
            case HOVER_ENTER:
                notifyHoverChange(event.getColor());
                break;
            case HOVER_EXIT:
                notifyHoverChange(null);
                break;
        }
    }

    private void handleSelection(ColorPtOstDot color, boolean selected) {
        if (selected) {
            // In hand-paint mode, clear previous selection before adding new one
            if (currentMode == SelectionMode.HAND_PAINT) {
                clearSelection();
            }
            selectedColors.add(color);
        } else {
            selectedColors.remove(color);
        }

        // Notify frame of selection change
        if (selected) {
            frame.notifyColorSelected(color);
        }
    }

    private void notifyHoverChange(ColorPtOstDot color) {
        frame.notifyColorHover(color);
    }

    private void clearSelection() {
        // Clear internal selection set
        selectedColors.clear();

        // Update visual state of panels
        panels.forEach(panel -> panel.setSelected(false));
    }

    /**
     * Gets the currently selected colors.
     * 
     * @return Set of selected ColorPtOstDot objects
     */
    public Set<ColorPtOstDot> getSelectedColors() {
        return new HashSet<>(selectedColors);
    }

    /**
     * Gets the currently selected color.
     * 
     * @return The currently selected ColorPtOstDot, or null if none selected
     */
    public ColorPtOstDot getSelectedColor() {
        return selectedColors.isEmpty() ? null : selectedColors.iterator().next();
    }

    /**
     * Sets the selected color, clearing any previous selections.
     * 
     * @param color The color to select
     */
    public void setSelectedColor(ColorPtOstDot color) {
        System.out.println("DEBUG: MonotriRenderContrlr.setSelectedColor called with color: " + color);
        selectedColors.clear();
        if (color != null) {
            System.out.println("DEBUG: Adding color to selectedColors with hue: " + color.getOstHCS().getStdCodeHueNum() +
                             ", fillOrder: " + color.getOstHCS().getFillOrderNum());
            selectedColors.add(color);
            notifyColorSelected(color);
        } else {
            System.out.println("DEBUG: Not adding null color to selectedColors");
        }
        drawPanel.repaint();
    } // end method setSelectedColor



    /**
     * Gets the current MonotriPage being displayed.
     * 
     * @return The current MonotriPage instance
     */
    public MonotriPage getCurrMonoTriPage() {
        return currMonoTriPage;
    }

    /**
     * Sets the standard hue number (0-35 range) and updates the MonotriPage.
     * 
     * @param hue The hue number in 0-35 range
     */
    public void setStandardHue(int hue) {
        // Ensure hue is in valid range
        if (hue < 0 || hue > 35) {
            throw new IllegalArgumentException("Hue must be in range 0-35");
        }

        // Get the MonotriPage for this hue
        MonotriPage page = MonotriBook.getInstance().getMonoTriPage(hue);
        setCurrMonoTriPage(page);
    }

    /**
     * Gets the current standard hue number (0-35 range).
     * 
     * @return The current hue number in 0-35 range
     */
    public int getStandardHue() {
        return currMonoTriPage != null ? currMonoTriPage.getHueNumCode() : 0;
    }

    /**
     * Updates the background color of the display.
     * 
     * @param bg The new background color
     */
    public void updateBackgroundColor(Color bg) {
        drawPanel.setBackground(bg);
        drawPanel.repaint();
    }

    /**
     * Notifies listeners that a color has been selected.
     * 
     * @param color The selected ColorPtOstDot
     */
    protected void notifyColorSelected(ColorPtOstDot color) {
        // Find the panel containing this color
        OstdotPanel sourcePanel = panels.stream()
                .filter(p -> p.getColorPtOstDot() == color)
                .findFirst()
                .orElse(null);
                
        if (sourcePanel != null) {
            OstdotEvent event = new OstdotEvent(sourcePanel, color, OstdotEvent.OstdotEventType.SELECTED);
            for (OstdotListener listener : ostdotListeners) {
                listener.ostdotEventOccurred(event);
            }
        }
    }

    /**
     * Adds an OstdotListener to be notified of color events.
     * 
     * @param listener The listener to add
     */
    public void addOstdotListener(OstdotListener listener) {
        if (listener != null && !ostdotListeners.contains(listener)) {
            ostdotListeners.add(listener);
        }
    }

    /**
     * Removes an OstdotListener.
     * 
     * @param listener The listener to remove
     */
    public void removeOstdotListener(OstdotListener listener) {
        ostdotListeners.remove(listener);
    }

    /**
     * Adds a color selection listener to be notified of color selection events.
     * 
     * @param listener The listener to add
     */
    public void addColorSelectionListener(IColorSelectionListener listener) {
        frame.addColorSelectionListener(listener);
    }

    /**
     * Sets the MonotriPage to display.
     * Alias for setMonoTriPage to maintain compatibility.
     * 
     * @param page The MonotriPage to display
     */
    public void setPage(MonotriPage page) {
        setCurrMonoTriPage(page);
    }

    /**
     * Sets the colors that are currently in use in the palette.
     * These colors will be highlighted in the display.
     * 
     * @param colors Set of colors currently in the palette
     */
    public void setPaletteColors(Set<ColorPtOstDot> colors) {
        panels.forEach(panel -> {
            ColorPtOstDot panelColor = panel.getColorPtOstDot();
            panel.setInPalette(colors.contains(panelColor));
        });
        drawPanel.repaint();
    }

    public MonotriPage getMonotriPage() {
        return currMonoTriPage;
    } // end method getMonotriPage

    /**
     * Determines if the simplified view mode is enabled.
     * Simplified view affects how occupied positions in other hues are displayed.
     * 
     * @return true if simplified view is enabled, false otherwise
     */
    public boolean isSimplifiedViewEnabled() {
        return simplifiedViewEnabled;
    }
    
    /**
     * Toggles the simplified view mode.
     * 
     * @return the new state of simplified view mode
     */
    public boolean toggleSimplifiedView() {
        simplifiedViewEnabled = !simplifiedViewEnabled;
        return simplifiedViewEnabled;
    }
    
    /**
     * Sets the simplified view mode.
     * 
     * @param enabled true to enable simplified view, false to disable
     */
    public void setSimplifiedViewEnabled(boolean enabled) {
        simplifiedViewEnabled = enabled;
    }

} // end class
