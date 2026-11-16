package com.csurf.templatecore.controller;

import com.csurf.artintellig.domainanchorpattern.model.IQAnchorPattern;
import com.csurf.artintellig.model.*;
import com.csurf.ostwald.domainmonotri.controller.MonotriRenderContrlr;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.MonotriPage;
import com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame;
import com.csurf.templatecore.model.SmartpicPalette;

import java.util.Collection;
import java.util.HashSet;

/**
 * Higher-level controller for MonoTri display functionality. Works with MonotriRenderContrlr
 * for actual drawing operations and user interactions. This controller focuses on
 * coordinating with the broader SmartPic subsystem.
 */
public class MonotriDisplayTopContrlr implements ISmartPicStateListener {
    private final MonotriWorkFrame display;
    private final MonotriRenderContrlr drawController;
    private ArtisticGoal currentGoal;
    private IQAnchorPattern currentPattern;




    public MonotriDisplayTopContrlr(MonotriWorkFrame display, MonotriRenderContrlr drawController) {
        this.display = display;
        this.drawController = drawController;
        
        // Set up interaction between controllers
        drawController.addColorSelectionListener(event -> handleColorSelection(event.getSelectedColor()));
    } // end constructor



    /**
     * Updates the selection state and refreshes the display.
     */
    public void updateSelection() {
        display.repaint();
    } // end method



    /**
     * Handles color selection events from MonotriRenderContrlr.
     * This method bridges between low-level selection events and high-level
     * artistic goals.
     *
     * @param color The selected ColorPtOstDot
     */
    public void handleColorSelection(ColorPtOstDot color) {
        if (currentGoal != null && currentPattern != null) {
            // Process selection according to current artistic context
            processColorSelection(color);
        }
    } // end method handleColorSelection



    /**
     * Processes a color selection in the context of current artistic goals
     * and anchor patterns.
     *
     * @param color The selected color
     */
    private void processColorSelection(ColorPtOstDot color) {
        // Implementation will integrate with artistic goal logic
        drawController.setSelectedColor(color);
        display.repaint();
    } // end method processColorSelection



    /**
     * Changes the displayed MonotriPage.
     *
     * @param page The new MonotriPage to display
     */
    public void setMonoTriPage(MonotriPage page) {
        drawController.setPage(page);
        display.repaint();
    } // end method setMonoTriPage



    @Override
    public void onColorChanged(Collection<PipeOneColor> shapes) {
        // Update any relevant selections or highlights
        updateSelection();
    } // end method onColorChanged



    @Override
    public void onPaletteGenerated(SmartpicPalette newPalette) {
        // Update display to show new palette colors in context
        drawController.setPaletteColors(new HashSet<>(newPalette.getColors()));
        display.repaint();
    } // end method onPaletteGenerated



    @Override
    public void onArtisticGoalChanged(ArtisticGoal newGoal) {
        this.currentGoal = newGoal;
        // Update constraints and visual feedback
        display.repaint();
    } // end method onArtisticGoalChanged



    @Override
    public void onAnchorPatternChanged(IQAnchorPattern newPattern) {
        this.currentPattern = newPattern;
        // Update constraints and visual feedback
        display.repaint();
    } // end method onAnchorPatternChanged


} // end class MonotriDisplayTopContrlr
