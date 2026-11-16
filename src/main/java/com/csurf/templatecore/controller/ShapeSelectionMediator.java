package com.csurf.templatecore.controller;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.view.ShapeDisplayPanel;
import com.csurf.templatecore.view.ShapeListPanel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * Mediates interactions between ShapeDisplayPanel and ShapeListPanel components.
 * Handles synchronization of selection state and user interactions.
 */
public class ShapeSelectionMediator {
    private DrawObj[] drawObjs;
    private final ShapeDisplayPanel shapesPanel;
    private final ShapeListPanel listPanel;
    
    public ShapeSelectionMediator(ShapeDisplayPanel shapesPanel, ShapeListPanel listPanel) {
        this.shapesPanel = shapesPanel;
        this.listPanel = listPanel;
        this.drawObjs = new DrawObj[0];
        setupListeners();
    }

    private void setupListeners() {
        // Listen for list selection changes
        listPanel.addSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = listPanel.getSelectedIndex();
                updateSelection(index >= 0 && index < drawObjs.length ? drawObjs[index] : null);
            }
        });

        // Listen for mouse clicks on shapes
        shapesPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point2D clickPoint = e.getPoint();
                DrawObj clickedObj = null;
                
                // Search for clicked object in reverse order (top to bottom)
                for (int i = drawObjs.length - 1; i >= 0; i--) {
                    DrawObj obj = drawObjs[i];
                    if (obj.getPath() != null && obj.getPath().contains(clickPoint)) {
                        clickedObj = obj;
                        break;
                    }
                }
                
                // Update selection
                if (clickedObj != null) {
                    int index = java.util.Arrays.asList(drawObjs).indexOf(clickedObj);
                    listPanel.setSelectedIndex(index);
                } else {
                    listPanel.clearSelection();
                }
                updateSelection(clickedObj);
            }
        });
    }

    private void updateSelection(DrawObj selectedObj) {
        // Update selection state of all objects
        for (DrawObj obj : drawObjs) {
            obj.setSelected(obj == selectedObj);
        }
        shapesPanel.repaint();
    }

    public void setDrawObjs(DrawObj[] drawObjs) {
        this.drawObjs = drawObjs;
        listPanel.clearSelection();
        updateSelection(null);
    }
}
