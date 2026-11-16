package com.csurf.templatecore.view;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShapeListPanel extends JPanel {
    public static final int LIST_PANEL_WIDTH = 150;
    public static final int LIST_PANEL_HEIGHT = 550;
    public static final int MIN_LIST_PANEL_WIDTH = 180;
    public static final int MAX_LIST_PANEL_WIDTH = 250;
    private static final int BUTTON_HEIGHT = 30;
    private static final int BORDER_PADDING = 10;
    
    private final JList<DrawObj> list;
    private final DefaultListModel<DrawObj> listModel;
    private DrawObjRoster drawObjRoster;
    private final Set<DrawObj> customSelectedItems = new HashSet<>();
    private boolean processingCustomSelection = false;
    private final List<SelectionListener> selectionListeners = new ArrayList<>();
    private final List<ShiftSelectionListener> shiftSelectionListeners = new ArrayList<>();
    private JButton openHueRelativesButton;
    private final List<HueRelativesButtonListener> hueRelativesButtonListeners = new ArrayList<>();
    private JPanel buttonPanel;
    private JLabel headerLabel;
    
    // Flag to prevent recursive event handling
    private boolean updatingFromRoster = false;
    
    // Flag to prevent recursive visual updates
    private boolean updatingVisualSelection = false;
    
    // Flag to suppress ListSelectionListener during programmatic model population
    private boolean populatingModel = false;
    
    // "Mailbox" flag for shift detection: mousePressed records the shift state and the
    // list's valueChanged consumes it exactly once. See 102926 design doc §7.1.
    private boolean shiftClickPending = false;
    
    /**
     * Interface for listeners that want to be notified of selection changes in this panel.
     */
    public interface SelectionListener {
        /**
         * Called when the selection in the panel changes.
         * 
         * @param selectedDrawObj The newly selected DrawObj, or null if selection cleared
         */
        void selectionChanged(DrawObj selectedDrawObj);
    }
    
    /**
     * Interface for listeners that want to be notified of shift-click selections.
     * A shift-click toggles selection-group membership instead of collapsing the
     * group to a single DrawObj. See 102926 design doc §7.2.
     */
    public interface ShiftSelectionListener {
        /**
         * Called when a DrawObj is selected via shift-click in the panel.
         * 
         * @param selectedDrawObj The shift-clicked DrawObj
         */
        void shiftSelectionChanged(DrawObj selectedDrawObj);
    }
    
    /**
     * Interface for listeners that want to be notified when the HueRelatives button is clicked.
     */
    public interface HueRelativesButtonListener {
        /**
         * Called when the HueRelatives button is clicked.
         */
        void hueRelativesButtonClicked();
    }
    
    /**
     * Adds a selection listener to be notified of selection changes.
     * 
     * @param listener The listener to add
     */
    public void addDrawObjSelectionListener(SelectionListener listener) {
        if (listener != null && !selectionListeners.contains(listener)) {
            selectionListeners.add(listener);
        }
    }
    
    /**
     * Removes a selection listener.
     * 
     * @param listener The listener to remove
     */
    public void removeDrawObjSelectionListener(SelectionListener listener) {
        selectionListeners.remove(listener);
    }
    
    /**
     * Adds a shift-selection listener to be notified of shift-click selections.
     * 
     * @param listener The listener to add
     */
    public void addDrawObjShiftSelectionListener(ShiftSelectionListener listener) {
        if (listener != null && !shiftSelectionListeners.contains(listener)) {
            shiftSelectionListeners.add(listener);
        }
    }
    
    /**
     * Removes a shift-selection listener.
     * 
     * @param listener The listener to remove
     */
    public void removeDrawObjShiftSelectionListener(ShiftSelectionListener listener) {
        shiftSelectionListeners.remove(listener);
    }
    
    /**
     * Notifies all selection listeners that the selection has changed.
     * 
     * @param selectedDrawObj The newly selected DrawObj, or null if selection cleared
     */
    private void fireSelectionChanged(DrawObj selectedDrawObj) {
        // Don't fire selection events if we're updating from the roster
        if (updatingFromRoster) {
            System.out.println("ShapeListPanel: Skipping selection event while updating from roster");
            return;
        }
        
        System.out.println("ShapeListPanel: Firing selection changed event for " + selectedDrawObj);
        for (SelectionListener listener : selectionListeners) {
            listener.selectionChanged(selectedDrawObj);
        }
    }
    
    /**
     * Notifies all shift-selection listeners that a shift-click selection occurred.
     * Fired on channel 2 (shift-clicks only); see 102926 design doc §7.2.
     * 
     * @param selectedDrawObj The shift-clicked DrawObj
     */
    private void fireShiftSelectionChanged(DrawObj selectedDrawObj) {
        // Don't fire selection events if we're updating from the roster
        if (updatingFromRoster) {
            System.out.println("ShapeListPanel: Skipping shift selection event while updating from roster");
            return;
        }
        
        System.out.println("ShapeListPanel: Firing shift selection changed event for " + selectedDrawObj);
        for (ShiftSelectionListener listener : shiftSelectionListeners) {
            listener.shiftSelectionChanged(selectedDrawObj);
        }
    }
    
    /**
     * Adds a HueRelatives button listener.
     * 
     * @param listener The listener to add
     */
    public void addHueRelativesButtonListener(HueRelativesButtonListener listener) {
        if (listener != null && !hueRelativesButtonListeners.contains(listener)) {
            hueRelativesButtonListeners.add(listener);
        }
    }
    
    /**
     * Removes a HueRelatives button listener.
     * 
     * @param listener The listener to remove
     */
    public void removeHueRelativesButtonListener(HueRelativesButtonListener listener) {
        hueRelativesButtonListeners.remove(listener);
    }
    
    /**
     * Notifies all registered HueRelatives button listeners.
     */
    private void fireHueRelativesButtonClicked() {
        for (HueRelativesButtonListener listener : hueRelativesButtonListeners) {
            listener.hueRelativesButtonClicked();
        }
    }

    /**
     * Sets the header label text displayed above the list. The label is created
     * lazily and placed in the NORTH region; panels that never call this method
     * have no header. Pass null or empty to hide it.
     *
     * @param text The header text to display
     */
    public void setHeaderText(String text) {
        if (text == null || text.isEmpty()) {
            if (headerLabel != null) {
                headerLabel.setVisible(false);
                revalidate();
            }
            return;
        }
        if (headerLabel == null) {
            headerLabel = new JLabel(text, SwingConstants.CENTER);
            add(headerLabel, BorderLayout.NORTH);
        } else {
            headerLabel.setText(text);
            headerLabel.setVisible(true);
        }
        revalidate();
    }

    public ShapeListPanel() {
        setLayout(new BorderLayout(0, BORDER_PADDING));
        setBorder(BorderFactory.createEmptyBorder(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING));
        
        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        
        // Default to SINGLE_SELECTION mode
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Configure list scrolling
        list.setVisibleRowCount(10); 
        list.setLayoutOrientation(JList.VERTICAL); 
        
        System.out.println("ShapeListPanel: Initializing with simplified selection handling");
        
        // Custom mouse listener for handling clicks
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Record shift state before BasicListUI processes the press. User-added
                // listeners are notified before the UI delegate's (Swing notifies in
                // reverse registration order), so this flag is always set before the
                // selection model changes and before valueChanged consumes it.
                // See 102926 design doc §7.1.
                shiftClickPending = e.isShiftDown();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("\n--- ShapeListPanel.mouseClicked ---");
                System.out.println("Mouse event: " + e);
                
                if (drawObjRoster == null) {
                    System.out.println("DrawObjRoster is null, ignoring click");
                    return;
                }
                
                int index = list.locationToIndex(e.getPoint());
                if (index == -1) {
                    System.out.println("Click not on any item, ignoring");
                    return;
                }
                
                DrawObj clickedObj = listModel.getElementAt(index);
                System.out.println("Clicked on item at index " + index + ": " + clickedObj);
                
                processingCustomSelection = true;
                
                try {
                    // For all clicks (regular or shift), select only this item
                    System.out.println("Selecting only " + clickedObj);
                    customSelectedItems.clear();
                    customSelectedItems.add(clickedObj);
                    
                    if (drawObjRoster != null) {
                        drawObjRoster.clearSelection();
                        drawObjRoster.addToSelection(clickedObj);
                    }
                    
                    // Update JList selection to match (for visual feedback)
                    list.setSelectedIndex(index);
                    
                    // Update visual display
                    updateVisualSelection();
                    
                    // Notify selection listeners
                    fireSelectionChanged(clickedObj);
                    
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
            list,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Calculate scroll pane height - don't account for button yet since it's not added by default
        int scrollPaneHeight = LIST_PANEL_HEIGHT - (BORDER_PADDING * 2);
        Dimension scrollPaneSize = new Dimension(LIST_PANEL_WIDTH - (BORDER_PADDING * 2), scrollPaneHeight);
        scrollPane.setPreferredSize(scrollPaneSize);
        scrollPane.setMinimumSize(scrollPaneSize);
        add(scrollPane, BorderLayout.CENTER);
        
        // Create the button panel but don't add the button yet
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        
        // Custom cell renderer to show selection state
        list.setCellRenderer(new DefaultListCellRenderer() {
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
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                // Consume the shift flag recorded by mousePressed — exactly once, whether
                // or not the event below fires. Keyboard navigation never sets it, so
                // arrow keys cannot accrete the selection group. See 102926 design doc §7.1.
                boolean shiftClick = shiftClickPending;
                shiftClickPending = false;
                if (!processingCustomSelection && !populatingModel && !updatingVisualSelection) {
                    System.out.println("\n--- List selection event ---");
                    DrawObj selected = list.getSelectedValue();
                    
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
                        
                        // Route to the appropriate channel: plain clicks use the existing
                        // selection channel (collapse semantics); shift-clicks use
                        // channel 2 (toggle semantics). See 102926 design doc §7.2.
                        if (shiftClick) {
                            fireShiftSelectionChanged(selected);
                        } else {
                            fireSelectionChanged(selected);
                        }
                    }
                    System.out.println("--- End of list selection event ---\n");
                }
            }
        });
        
        System.out.println("ShapeListPanel initialization complete");
    }
    
    /**
     * Updates the visual selection in the list to match our custom selection set.
     */
    private void updateVisualSelection() {
        // Prevent recursive visual updates
        if (updatingVisualSelection) {
            System.out.println("ShapeListPanel: Preventing recursive visual selection update");
            return;
        }
        
        System.out.println("Updating visual selection");
        
        updatingVisualSelection = true;
        try {
            // Clear current list selection
            list.clearSelection();
            
            // Set new selection indices based on customSelectedItems
            if (!customSelectedItems.isEmpty()) {
                DefaultListModel<DrawObj> model = (DefaultListModel<DrawObj>) list.getModel();
                for (DrawObj obj : customSelectedItems) {
                    int index = model.indexOf(obj);
                    if (index >= 0) {
                        list.addSelectionInterval(index, index);
                    }
                }
            }
            
            // Force the list to repaint to show the updated selection state
            list.repaint();
            
            System.out.println("Visual selection updated, " + customSelectedItems.size() + " items selected");
        } finally {
            updatingVisualSelection = false;
        }
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
            
            // Set flag to prevent recursive event handling
            updatingFromRoster = true;
            try {
                // Update our custom selection
                customSelectedItems.clear();
                customSelectedItems.addAll(selectedObjs);
                
                // Update the visual display
                updateVisualSelection();
                
                System.out.println("Selection synced with roster: " + customSelectedItems.size() + " items selected");
            } finally {
                // Reset flag
                updatingFromRoster = false;
            }
        } else {
            System.out.println("DrawObjRoster or SelectionGroup is null, cannot sync");
        }
        System.out.println("--- End of sync with roster ---\n");
    }
    
    /**
     * Updates the custom selection display based on a single selected object.
     * This is specifically for the listAllDrawObjsPanel which should only reflect
     * the singleSelectedDrawObj, even when the selection group contains multiple objects.
     * 
     * @param selectedObj The single selected object to sync with, or null to clear selection
     */
    public void syncWithSingleObject(DrawObj selectedObj) {
        System.out.println("\n--- Syncing with single object: " + selectedObj + " ---");
        
        // Set flag to prevent recursive event handling
        updatingFromRoster = true;
        try {
            // Update our custom selection
            customSelectedItems.clear();
            if (selectedObj != null) {
                customSelectedItems.add(selectedObj);
            }
            
            // Update the visual display
            updateVisualSelection();
            
            System.out.println("Selection synced with single object: " + 
                (selectedObj != null ? selectedObj.toString() : "null"));
        } finally {
            // Reset flag
            updatingFromRoster = false;
        }
        System.out.println("--- End of sync with single object ---\n");
    }
    
    @Override
    public void setBounds(int x, int y, int width, int height) {
        // Allow both width and height to be set by the layout manager
        super.setBounds(x, y, width, height);
    }
    
    @Override
    public void setSize(int width, int height) {
        // Allow both width and height to be set by the layout manager
        super.setSize(width, height);
    }
    
    @Override
    public Dimension getPreferredSize() {
        // Preferred size that works well with JSplitPane
        return new Dimension(LIST_PANEL_WIDTH, LIST_PANEL_HEIGHT);
    }
    
    @Override
    public Dimension getMinimumSize() {
        // Minimum size to ensure list is still usable
        return new Dimension(MIN_LIST_PANEL_WIDTH, 200);
    }
    
    @Override
    public Dimension getMaximumSize() {
        // No maximum size constraint - let the JSplitPane control it
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    
    public void setDrawObjs(DrawObj[] drawObjs) {
        System.out.println("ShapeListPanel - Setting draw objects:");
        populatingModel = true;
        try {
            listModel.clear();
            if (drawObjs != null) {
                for (DrawObj obj : drawObjs) {
                    System.out.println("  Adding draw object: " + obj.toString());
                    listModel.addElement(obj);
                }
            }
        } finally {
            populatingModel = false;
        }
    }


    
    /**
     * Clears the current selection in the list.
     */
    public void clearSelection() {
        list.clearSelection();
        customSelectedItems.clear();
        updateVisualSelection();
    }
    
    /**
     * Adds a selection listener to the list.
     * 
     * @param listener The listener to add
     */
    public void addSelectionListener(ListSelectionListener listener) {
        list.addListSelectionListener(listener);
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
     * Gets the index of the currently selected item in the list.
     * 
     * @return The selected index, or -1 if no selection
     */
    public int getSelectedIndex() {
        return list.getSelectedIndex();
    }
    
    /**
     * Sets the selected index in the list.
     * 
     * @param index The index to select
     */
    public void setSelectedIndex(int index) {
        System.out.println("ShapeListPanel - Setting selected index: " + index);
        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
    }
    
    /**
     * Gets the JList component used in this panel.
     * 
     * @return The JList component
     */
    public JList<DrawObj> getJList() {
        return list;
    }
    
    /**
     * Adds the "See HueRelatives" button to this panel.
     * This should only be called on the main ShapeListPanel, not on SelectedShapesListPanel.
     */
    public void addHueRelativesButton() {
        // Only add the button if it hasn't been added yet
        if (openHueRelativesButton == null) {
            // Create the "See HueRelatives" button
            openHueRelativesButton = new JButton("FOCUS HuRelativs");
            // button width WAS: LIST_PANEL_WIDTH - (BORDER_PADDING * 2 // TODO 102726 INVESTIGATE why no effect
            openHueRelativesButton.setPreferredSize(new Dimension(LIST_PANEL_WIDTH - 5, BUTTON_HEIGHT));
            openHueRelativesButton.addActionListener(e -> fireHueRelativesButtonClicked());
            
            // Add button to the button panel
            buttonPanel.add(openHueRelativesButton);
            
            // Add button panel to the main panel
            add(buttonPanel, BorderLayout.SOUTH);
            
            // Adjust the scroll pane size to make room for the button
            JScrollPane scrollPane = (JScrollPane) getComponent(0); // The scroll pane is the first component
            int scrollPaneHeight = LIST_PANEL_HEIGHT - (BORDER_PADDING * 2) - BUTTON_HEIGHT - BORDER_PADDING;
            Dimension scrollPaneSize = new Dimension(LIST_PANEL_WIDTH - (BORDER_PADDING * 2), scrollPaneHeight);
            scrollPane.setPreferredSize(scrollPaneSize);
            scrollPane.setMinimumSize(scrollPaneSize);
            
            // Force layout update
            revalidate();
            repaint();
        }
    }
}
