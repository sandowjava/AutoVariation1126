package com.csurf.templatecore.view;

import com.csurf.templatecore.model.DrawObj;

import javax.swing.*;

/**
 * Panel that displays a list of selected DrawObj items.
 * This is a specialized version of ShapeListPanel specifically for displaying the current selection.
 */
public class SelectedShapesListPanel extends ShapeListPanel {
    
    public SelectedShapesListPanel() {
        super();
        System.out.println("SelectedShapesListPanel: Initialized as extension of ShapeListPanel");
        
        // Set a smaller preferred size for the selection list panel
        setPreferredSize(new java.awt.Dimension(150, 100));
        
        // Use MULTIPLE_INTERVAL_SELECTION to allow multiple items to be selected
        getJList().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }
    
    @Override
    public void setDrawObjs(DrawObj[] drawObjs) {
        System.out.println("SelectedShapesListPanel - Setting draw objects:");
        // Call parent's setDrawObjs() which has proper flag protection
        // to prevent ListSelectionListener from modifying roster during model update
        super.setDrawObjs(drawObjs);
    }
}
