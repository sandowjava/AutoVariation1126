package com.csurf.templatecore.svgconverter.view;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.view.ShapeListPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Panel that displays a list of DrawObj items with selection capabilities.
 * This is a specialized version of ShapeListPanel specifically for use in SVGTopViewManager.
 */
public class ShapeNamesListPanel extends ShapeListPanel {
    public static final int LIST_PANEL_WIDTH = 150;
    public static final int LIST_PANEL_HEIGHT = 550;
    public static final int MIN_LIST_PANEL_WIDTH = 180;
    public static final int MAX_LIST_PANEL_WIDTH = 250;
    private static final int BUTTON_HEIGHT = 30;
    private static final int BORDER_PADDING = 10;
    
    private final JButton addButton;
    private DrawObjRoster drawObjRoster;
    private final Set<DrawObj> customSelectedItems = new HashSet<>();
    private boolean processingCustomSelection = false;
    
    public ShapeNamesListPanel() {
        super();
        System.out.println("ShapeNamesListPanel: Initialized as extension of ShapeListPanel");
        
        setLayout(new BorderLayout(0, BORDER_PADDING));
        setBorder(BorderFactory.createEmptyBorder(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING));
        
        // Custom mouse listener for handling shift-click
        getJList().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("\n--- ShapeNamesListPanel.mouseClicked ---");
                System.out.println("Mouse event: " + e);
                System.out.println("Modifiers: " + InputEvent.getModifiersExText(e.getModifiersEx()));
                
                if (drawObjRoster == null) {
                    System.out.println("DrawObjRoster is null, ignoring click");
                    return;
                }
                
                int index = getJList().locationToIndex(e.getPoint());
                if (index == -1) {
                    System.out.println("Click not on any item, ignoring");
                    return;
                }
                
                DrawObj clickedObj = (DrawObj) getJList().getModel().getElementAt(index);
                System.out.println("Clicked on item at index " + index + ": " + clickedObj);
                
                // Check if shift key is pressed
                boolean isShiftDown = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
                System.out.println("Shift key is " + (isShiftDown ? "DOWN" : "not down"));
                
                processingCustomSelection = true;
                
                try {
                    if (isShiftDown) {
                        // Shift-click: toggle selection
                        if (customSelectedItems.contains(clickedObj)) {
                            // Remove from selection
                            customSelectedItems.remove(clickedObj);
                            if (drawObjRoster != null) {
                                drawObjRoster.removeFromSelection(clickedObj);
                            }
                            System.out.println("Removed from selection: " + clickedObj);
                        } else {
                            // Add to selection
                            customSelectedItems.add(clickedObj);
                            if (drawObjRoster != null) {
                                drawObjRoster.addToSelection(clickedObj);
                            }
                            System.out.println("Added to selection: " + clickedObj);
                        }
                    } else {
                        // Regular click: select only this item
                        System.out.println("Regular click: selecting only " + clickedObj);
                        customSelectedItems.clear();
                        customSelectedItems.add(clickedObj);
                        
                        if (drawObjRoster != null) {
                            drawObjRoster.clearSelection();
                            drawObjRoster.addToSelection(clickedObj);
                        }
                        
                        // Update JList selection to match (for visual feedback)
                        getJList().setSelectedIndex(index);
                    }
                    
                    // Update visual display
                    updateVisualSelection();
                    
                    // Print current selection state
                    System.out.println("Current selection after handling click:");
                    System.out.println("- customSelectedItems size: " + customSelectedItems.size());
                    System.out.println("- customSelectedItems contents: " + customSelectedItems);
                    if (drawObjRoster != null && drawObjRoster.getSelectionGroup() != null) {
                        System.out.println("- DrawObjRoster selection size: " + 
                            drawObjRoster.getSelectionGroup().getSelectedDrawObjsSet().size());
                    }
                } finally {
                    processingCustomSelection = false;
                }
                
                System.out.println("--- End of mouseClicked handling ---\n");
            }
        });
        
        // Create scroll pane with both scrollbars
        JScrollPane scrollPane = new JScrollPane(
            getJList(),
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Calculate scroll pane height to leave room for button
        int scrollPaneHeight = LIST_PANEL_HEIGHT - BUTTON_HEIGHT - (BORDER_PADDING * 3);
        Dimension scrollPaneSize = new Dimension(LIST_PANEL_WIDTH - (BORDER_PADDING * 2), scrollPaneHeight);
        scrollPane.setPreferredSize(scrollPaneSize);
        scrollPane.setMinimumSize(scrollPaneSize);
        add(scrollPane, BorderLayout.CENTER);

        // Add button at the bottom
        addButton = new JButton("Create SmartPic w Bridge");
        addButton.setPreferredSize(new Dimension(LIST_PANEL_WIDTH - (BORDER_PADDING * 2), BUTTON_HEIGHT));
        add(addButton, BorderLayout.SOUTH);
        
        // Custom cell renderer to show selection state
        getJList().setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                
                // Get default component
                Component c = super.getListCellRendererComponent(
                        list, value, index, false, cellHasFocus);
                
                // Apply custom selection highlighting
                if (value instanceof DrawObj) {
                    DrawObj drawObj = (DrawObj) value;
                    if (customSelectedItems.contains(drawObj)) {
                        c.setBackground(list.getSelectionBackground());
                        c.setForeground(list.getSelectionForeground());
                    } else {
                        c.setBackground(list.getBackground());
                        c.setForeground(list.getForeground());
                    }
                }
                
                return c;
            }
        });
        
        // Add list selection listener to handle standard selection
        getJList().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && !processingCustomSelection) {
                    System.out.println("\n--- List selection event ---");
                    DrawObj selected = getJList().getSelectedValue();
                    
                    if (selected != null) {
                        System.out.println("Standard list selection: " + selected);
                        
                        // Only update if not in the middle of our custom selection process
                        customSelectedItems.clear();
                        customSelectedItems.add(selected);
                        
                        if (drawObjRoster != null) {
                            drawObjRoster.clearSelection();
                            drawObjRoster.addToSelection(selected);
                        }
                        
                        updateVisualSelection();
                    }
                    System.out.println("--- End of list selection event ---\n");
                }
            }
        });
        
        System.out.println("ShapeNamesListPanel initialization complete");
    }
    
    /**
     * Updates the visual selection in the list to match our custom selection set.
     */
    private void updateVisualSelection() {
        System.out.println("Updating visual selection");
        
        // Force the list to repaint to show the updated selection state
        getJList().repaint();
        
        System.out.println("Visual selection updated, " + customSelectedItems.size() + " items selected");
    }
    
    /**
     * Sets the DrawObjRoster to use for selection management.
     * 
     * @param roster The DrawObjRoster to use
     */
    public void setDrawObjRoster(DrawObjRoster roster) {
        System.out.println("Setting DrawObjRoster: " + roster);
        this.drawObjRoster = roster;
        
        // Sync our custom selection with the roster's selection
        syncWithRosterSelection();
    }
    
    /**
     * Updates the custom selection display based on the current selection in the DrawObjRoster.
     */
    public void syncWithRosterSelection() {
        System.out.println("\n--- Syncing with roster selection ---");
        if (drawObjRoster != null && drawObjRoster.getSelectionGroup() != null) {
            // Get all selected objects from the roster
            Set<DrawObj> selectedObjs = drawObjRoster.getSelectionGroup().getSelectedDrawObjsSet();
            
            System.out.println("DrawObjRoster has " + selectedObjs.size() + " selected items");
            
            // Update our custom selection
            customSelectedItems.clear();
            customSelectedItems.addAll(selectedObjs);
            
            // Update the visual display
            updateVisualSelection();
            
            System.out.println("Selection synced with roster: " + customSelectedItems.size() + " items selected");
        } else {
            System.out.println("DrawObjRoster or SelectionGroup is null, cannot sync");
        }
        System.out.println("--- End of sync with roster ---\n");
    }
    
    @Override
    public void setBounds(int x, int y, int width, int height) {
        // Maintain fixed height while allowing width to vary within bounds
        width = Math.max(MIN_LIST_PANEL_WIDTH, Math.min(width, MAX_LIST_PANEL_WIDTH));
        super.setBounds(x, y, width, LIST_PANEL_HEIGHT);
    }
    
    @Override
    public void setSize(int width, int height) {
        // Maintain fixed height while allowing width to vary within bounds
        width = Math.max(MIN_LIST_PANEL_WIDTH, Math.min(width, MAX_LIST_PANEL_WIDTH));
        super.setSize(width, LIST_PANEL_HEIGHT);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(LIST_PANEL_WIDTH, LIST_PANEL_HEIGHT);
    }
    
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(MIN_LIST_PANEL_WIDTH, LIST_PANEL_HEIGHT);
    }
    
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(MAX_LIST_PANEL_WIDTH, LIST_PANEL_HEIGHT);
    }
    
    public void setDrawObjs(DrawObj[] drawObjs) {
        System.out.println("ShapeNamesListPanel - Setting draw objects:");
        DefaultListModel<DrawObj> model = (DefaultListModel<DrawObj>) getJList().getModel();
        model.clear();
        if (drawObjs != null) {
            for (DrawObj obj : drawObjs) {
                System.out.println("  Adding draw object: " + obj.toString());
                model.addElement(obj);
            }
        }
    }
    
    /**
     * Clears the current selection in the list.
     */
    public void clearSelection() {
        getJList().clearSelection();
        customSelectedItems.clear();
        updateVisualSelection();
    }
    
    /**
     * Adds a selection listener to the list.
     * 
     * @param listener The listener to add
     */
    public void addSelectionListener(ListSelectionListener listener) {
        getJList().addListSelectionListener(listener);
    }
    
    /**
     * Gets all currently selected DrawObj items.
     * 
     * @return Array of selected DrawObj items
     */
    public DrawObj[] getSelectedDrawObjs() {
        DrawObj[] result = customSelectedItems.toArray(new DrawObj[0]);
        System.out.println("getSelectedDrawObjs returning " + result.length + " items");
        return result;
    }
    
    /**
     * Gets the first selected DrawObj (for backward compatibility).
     * 
     * @return The first selected DrawObj, or null if none selected
     */
    public DrawObj getSelectedDrawObj() {
        if (!customSelectedItems.isEmpty()) {
            DrawObj result = customSelectedItems.iterator().next();
            System.out.println("getSelectedDrawObj returning: " + result);
            return result;
        }
        System.out.println("getSelectedDrawObj returning null (no selection)");
        return null;
    }
    
    /**
     * Gets the JList component used in this panel.
     * 
     * @return The JList component
     */
    public JList<DrawObj> getJList() {
        return super.getJList();
    }
}
