package com.csurf.templatecore.controller;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.view.ShapeDisplayPanel;
import com.csurf.templatecore.view.ShapeListPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Controls interaction between ShapeDisplayPanel and ShapeListPanel.
 * Handles selection of shapes and synchronization between UI components.
 *
 * Click semantics (102926 design doc §4): a plain click on either surface
 * collapses the selection group to the clicked shape; a shift-click toggles
 * the clicked shape's group membership. List shift-clicks arrive on the
 * dedicated shift channel (ShapeListPanel.ShiftSelectionListener); canvas
 * shift-clicks are detected via MouseEvent.SHIFT_DOWN_MASK.
 */
public class ShapeSelectionController {

    /**
     * Provides read/write access to the frame's focused DrawObj (the
     * singleSelectedDrawObj channel) without coupling this controller to a
     * concrete frame class. Injected by the creator; may be null for callers
     * that do not participate in focus choreography.
     * (102926 design doc §4, D5 — callback injection chosen over a frame
     * reference to avoid coupling this controller to TemplateWorkFrame.)
     */
    public interface FocusAccessor {
        /**
         * Returns the currently focused DrawObj, or null if none.
         * 
         * @return The focused DrawObj, or null
         */
        DrawObj getFocus();

        /**
         * Sets the focused DrawObj (updates the focus field and color swatch).
         * 
         * @param drawObj The DrawObj to focus, or null to clear focus
         */
        void setFocus(DrawObj drawObj);
    }

    private DrawObj[] drawObjs;
    private final ShapeDisplayPanel shapesPanel;
    private final ShapeListPanel listPanel;
    private DrawObjRoster drawObjRoster; // Added reference to DrawObjRoster
    private final FocusAccessor focusAccessor;
    private static final float HIT_TEST_TOLERANCE = 5.0f;

    /**
     * Creates a new ShapeSelectionController.
     * 
     * @param shapesPanel The panel displaying shapes
     * @param listPanel The panel displaying a list of shapes
     * @param focusAccessor Read/write access to the frame's focused DrawObj; may be null
     */
    public ShapeSelectionController(ShapeDisplayPanel shapesPanel, ShapeListPanel listPanel,
                                    FocusAccessor focusAccessor) {
        this.shapesPanel = shapesPanel;
        this.listPanel = listPanel;
        this.focusAccessor = focusAccessor;
        this.drawObjs = new DrawObj[0];
        
        // Add mouse listener to shapes panel
        shapesPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleShapesPanelClick(e);
            }
        });
        
        // Add semantic selection listener — fires only on user-initiated selection changes
        listPanel.addDrawObjSelectionListener(selectedDrawObj -> handleListSelection(selectedDrawObj));
        
        // Add shift-selection listener — channel 2, fires only on shift-clicks (102926 design doc §7.2)
        listPanel.addDrawObjShiftSelectionListener(clickedDrawObj -> handleShiftToggle(clickedDrawObj));
    }
    
    /**
     * Sets the DrawObjRoster to use for selection management.
     * 
     * @param roster The DrawObjRoster to use
     */
    public void setDrawObjRoster(DrawObjRoster roster) {
        this.drawObjRoster = roster;
    }
    
    /**
     * Sets the array of DrawObjs when the template changes.
     * Clears the current selection because the old selection is no longer valid.
     * 
     * @param drawObjs The array of DrawObjs
     */
    public void setDrawObjs(DrawObj[] drawObjs) {
        this.drawObjs = drawObjs;
        clearSelection();
    }

    /**
     * Updates the array of DrawObjs when only the display order changes (e.g. sort order).
     * Preserves the current selection because the same template and same objects remain valid.
     * 
     * @param drawObjs The re-ordered array of DrawObjs
     */
    public void updateDrawObjsArray(DrawObj[] drawObjs) {
        this.drawObjs = drawObjs;
    }
    
    /**
     * Handles a mouse click on the shapes panel.
     * 
     * @param e The mouse event
     */
    private void handleShapesPanelClick(MouseEvent e) {
        System.out.println("ShapeSelectionController - Handling shapes panel click at: " + e.getPoint());
        
        // Transform screen point to SVG space
        Point2D svgPoint = shapesPanel.transformPoint(e.getPoint());
        System.out.println("  Transformed to SVG space: " + svgPoint);
        
        // Find clicked shape
        DrawObj clickedShape = findShapeAtPoint(svgPoint);
        
        if (clickedShape != null) {
            System.out.println("  Found clicked shape: " + clickedShape);
            
            // Check if shift key is pressed
            boolean isShiftDown = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
            System.out.println("  Shift key pressed: " + isShiftDown);
            
            if (isShiftDown) {
                // Toggle group membership with focus choreography — shared with
                // the list's shift-click channel (102926 design doc §8.2)
                handleShiftToggle(clickedShape);
            } else {
                // Replace selection with just the clicked shape
                updateSelection(clickedShape);
            }
        } else {
            System.out.println("  No shape found at click point, clearing selection");
            clearSelection();
        }
    }
    
    /**
     * Handles a plain (non-shift) selection event on the list channel —
     * collapses the group to the clicked DrawObj. Shift-clicks arrive on the
     * dedicated shift channel and are routed to handleShiftToggle instead.
     *
     * @param selectedDrawObj The DrawObj selected in the list, or null if the
     *        selection was cleared
     */
    private void handleListSelection(DrawObj selectedDrawObj) {
        System.out.println("ShapeSelectionController - Handling list selection: " + selectedDrawObj);
        
        if (selectedDrawObj != null) {
            updateSelection(selectedDrawObj);
        } else {
            clearSelection();
        }
    }
    
    /**
     * Handles a shift-click toggle from either interaction surface (list channel 2
     * or canvas shift-click). Follows the HueRelativesFrame +/- caller contract:
     * write focus BEFORE mutating the roster so the synchronous SelectionEvent
     * resolves to the intended focus (102926 design doc §4, decisions D2–D4).
     *
     * <ul>
     *   <li>Shift-add: focus = clicked shape, then roster.addToSelection.</li>
     *   <li>Shift-remove of the focused shape: relocate focus to a surviving group
     *       member (roster order, excluding the target) BEFORE the removal, so the
     *       swatch never goes blank while the group is non-empty.</li>
     *   <li>Shift-remove of a non-focused shape: no focus write — event resolution
     *       preserves the surviving focus.</li>
     * </ul>
     *
     * @param clicked The shift-clicked DrawObj
     */
    private void handleShiftToggle(DrawObj clicked) {
        if (clicked == null) {
            return;
        }
        if (drawObjRoster == null) {
            // No roster available: degrade to plain collapse semantics (fallback parity)
            updateSelection(clicked);
            return;
        }
        System.out.println("ShapeSelectionController - Handling shift-toggle for: " + clicked);
        if (drawObjRoster.isSelected(clicked)) {
            // Shift-remove: if the clicked shape currently holds focus, relocate focus
            // to a survivor BEFORE the removal (HRF '-' convention)
            if (focusAccessor != null && clicked == focusAccessor.getFocus()) {
                focusAccessor.setFocus(findSurvivor(clicked));
            }
            removeFromSelection(clicked);
        } else {
            // Shift-add: focus follows the clicked shape (HRF '+' convention)
            if (focusAccessor != null) {
                focusAccessor.setFocus(clicked);
            }
            addToSelection(clicked);
        }
    }
    
    /**
     * Finds a surviving group member to receive focus after removing the given target.
     * Uses template (roster) order, matching HueRelativesFrame's +/- survivor logic.
     *
     * @param removalTarget The DrawObj about to be removed
     * @return The first selected DrawObj other than the target, or null if none
     */
    private DrawObj findSurvivor(DrawObj removalTarget) {
        List<DrawObj> templateDrawObjs = drawObjRoster.getTemplateDrawObjs();
        if (templateDrawObjs == null) {
            return null;
        }
        for (DrawObj obj : templateDrawObjs) {
            if (obj != removalTarget && drawObjRoster.isSelected(obj)) {
                return obj;
            }
        }
        return null;
    }
    
    /**
     * Finds the shape at the given point.
     * 
     * @param point The point to check
     * @return The shape at the point, or null if no shape is found
     */
    private DrawObj findShapeAtPoint(Point2D point) {
        if (drawObjs == null) return null;
        
        // Check shapes in reverse order (top to bottom)
        for (int i = drawObjs.length - 1; i >= 0; i--) {
            DrawObj obj = drawObjs[i];
            if (isShapeHit(obj, point)) {
                return obj;
            }
        }
        return null;
    }
    
    /**
     * Checks if the given shape is hit by the given point.
     * 
     * @param obj The shape to check
     * @param point The point to check
     * @return True if the shape is hit, false otherwise
     */
    private boolean isShapeHit(DrawObj obj, Point2D point) {
        // First check if point is inside the shape
        if (obj.getPath().contains(point)) {
            System.out.println("  Hit test: shape contains point");
            return true;
        }
        
        // Then check if point is near the stroke
        Shape strokedShape = new BasicStroke(HIT_TEST_TOLERANCE)
            .createStrokedShape(obj.getPath());
        boolean hit = strokedShape.contains(point);
        System.out.println("  Hit test: point " + (hit ? "is" : "is not") + " near stroke");
        return hit;
    }
    
    /**
     * Adds a DrawObj to the current selection.
     * 
     * @param obj The DrawObj to add to the selection
     */
    private void addToSelection(DrawObj obj) {
        if (obj != null) {
            // Update DrawObjRoster if available
            if (drawObjRoster != null) {
                drawObjRoster.addToSelection(obj);
            } else {
                // Fallback if DrawObjRoster is not available
                obj.setSelected(true);
                
                // Update list panel to show the added selection
                updateListPanelSelection();
                
                // Repaint panel
                shapesPanel.repaint();
            }
        }
    }
    
    /**
     * Removes a DrawObj from the current selection.
     * 
     * @param obj The DrawObj to remove from the selection
     */
    private void removeFromSelection(DrawObj obj) {
        if (obj != null) {
            // Update DrawObjRoster if available
            if (drawObjRoster != null) {
                drawObjRoster.removeFromSelection(obj);
            } else {
                // Fallback if DrawObjRoster is not available
                obj.setSelected(false);
                
                // Update list panel to show the updated selection
                updateListPanelSelection();
                
                // Repaint panel
                shapesPanel.repaint();
            }
        }
    }
    
    /**
     * Updates the list panel selection to match the current selection set.
     * 
     * IMPORTANT: This method searches for the object in the LIST's model, not the controller's array.
     * This is critical because the controller's array may be in drawing order (unsorted) while
     * the list's array may be in display order (sorted). Using the list's indexOf() ensures
     * we get the correct index in the list's array, not the controller's array.
     */
    private void updateListPanelSelection() {
        // Clear current list selection
        listPanel.clearSelection();
        
        // If exactly one shape carries the selected flag, select it in the list.
        // (Fallback path only — the DrawObj.selected flag is the source of truth when
        // no roster is wired; the former selectedShapes mirror was removed since it
        // could drift from real state. See the 102926 design doc §7.3.)
        DrawObj soleSelected = null;
        int selectedCount = 0;
        if (drawObjs != null) {
            for (DrawObj obj : drawObjs) {
                if (obj != null && obj.isSelected()) {
                    selectedCount++;
                    soleSelected = obj;
                }
            }
        }
        if (selectedCount == 1) {
            // Search for the object in the LIST's model, not the controller's array
            // This ensures we get the correct index in the list's array
            DefaultListModel<DrawObj> listModel = 
                (DefaultListModel<DrawObj>) listPanel.getJList().getModel();
            int listIndex = listModel.indexOf(soleSelected);
            
            if (listIndex >= 0) {
                listPanel.setSelectedIndex(listIndex);
            }
        }
        // For multiple selections, we could implement a custom list selection model
        // but for now we'll leave the list unselected when multiple shapes are selected
    }
    
    /**
     * Replaces the current selection with a single DrawObj.
     * 
     * @param newSelection The DrawObj to select
     */
    private void updateSelection(DrawObj newSelection) {
        // If DrawObjRoster is available, use it to manage selection
        if (drawObjRoster != null) {
            // First clear the selection
            drawObjRoster.clearSelection();
            
            // Then add the new selection if not null
            if (newSelection != null) {
                drawObjRoster.addToSelection(newSelection);
            }
        } else {
            // Fallback if DrawObjRoster is not available — the DrawObj.selected flag
            // is the source of truth; clear it on every known shape
            if (drawObjs != null) {
                for (DrawObj obj : drawObjs) {
                    if (obj != null) {
                        obj.setSelected(false);
                    }
                }
            }
            
            // Add the new selection
            if (newSelection != null) {
                newSelection.setSelected(true);
                
                // Update list panel
                for (int i = 0; i < drawObjs.length; i++) {
                    if (drawObjs[i] == newSelection) {
                        listPanel.setSelectedIndex(i);
                        break;
                    }
                }
            } else {
                listPanel.clearSelection();
            }
            
            // Repaint panel
            shapesPanel.repaint();
        }
        
    }
    
    /**
     * Clears the current selection.
     */
    private void clearSelection() {
        System.out.println("ShapeSelectionController - Clearing selection");
        
        // If DrawObjRoster is available, use it to clear selection
        if (drawObjRoster != null) {
            drawObjRoster.clearSelection();
        } else {
            // Fallback if DrawObjRoster is not available — clear the DrawObj.selected
            // flag on every known shape (the flag is the source of truth here)
            if (drawObjs != null) {
                for (DrawObj obj : drawObjs) {
                    if (obj != null) {
                        obj.setSelected(false);
                    }
                }
            }
            
            // Clear list selection
            listPanel.clearSelection();
            
            // Repaint panel
            shapesPanel.repaint();
        }
    }
}
