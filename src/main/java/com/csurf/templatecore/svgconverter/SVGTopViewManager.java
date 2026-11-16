package com.csurf.templatecore.svgconverter;

import com.csurf.templatecore.controller.ShapeSelectionMediator;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.svgconverter.view.ShapeNamesListPanel;
import com.csurf.templatecore.view.ShapeDisplayPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Top-level view manager for the SVG subunit. Manages and coordinates all SVG-related UI components.
 */
public class SVGTopViewManager extends JPanel {
    private final ShapeNamesListPanel listDrawObjsPanel;
    private final ShapeDisplayPanel shapesPanel;
    private final ShapeSelectionMediator selectionMediator;
    
    public SVGTopViewManager() {
        setLayout(new BorderLayout());
        
        // Create components
        listDrawObjsPanel = new ShapeNamesListPanel();
        shapesPanel = new ShapeDisplayPanel();
        
        // Set up selection mediator
        selectionMediator = new ShapeSelectionMediator(shapesPanel, listDrawObjsPanel);
        
        // Add components to layout
        add(shapesPanel, BorderLayout.CENTER);
        add(listDrawObjsPanel, BorderLayout.WEST);
    }
    
    public void setDrawObjs(DrawObj[] drawObjs) {
        shapesPanel.setDrawObjs(drawObjs);
        listDrawObjsPanel.setDrawObjs(drawObjs);
        selectionMediator.setDrawObjs(drawObjs);
    }
    
    public ShapeDisplayPanel getShapesPanel() {
        return shapesPanel;
    }
    
    public ShapeNamesListPanel getListDrawObjsPanel() {
        return listDrawObjsPanel;
    }
    
    public ShapeSelectionMediator getSelectionMediator() {
        return selectionMediator;
    }
}
