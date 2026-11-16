package com.csurf.templatecore.view;

import com.csurf.templatecore.controller.SelectionGroupChoiceController;
import com.csurf.templatecore.events.SelectionEvent;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.SelectionGroup;
import com.csurf.templatecore.model.Template;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;

/**
 * A frame that allows the user to create, save, load, and delete named SelectionGroups.
 * This frame is opened from the TemplateWorkFrame's Selection menu.
 */
public class SelectionGroupChoiceFrame extends JFrame {
    private final TemplateWorkFrame parentFrame;
    private final JTextField nameField;
    private final JList<String> savedGroupsList;
    private final DefaultListModel<String> listModel;
    private final JButton saveButton;
    private final JButton addSelectionsButton;
    private final JButton replaceSelectionsButton;
    private final JButton subtrSelectionsButton;
    private final JButton deListGroupButton;
    private final JLabel statusLabel;
    private Template template;
    private SelectionGroupChoiceController controller;

    /**
     * Creates a new SelectionGroupChoiceFrame.
     *
     * @param parentFrame The parent TemplateWorkFrame
     */
    public SelectionGroupChoiceFrame(TemplateWorkFrame parentFrame) {
        super("Selection Group Manager");
        this.parentFrame = parentFrame;
        this.template = parentFrame.getCurrTemplate();
        
        // Initialize the controller
        this.controller = new SelectionGroupChoiceController(this.template);
        this.controller.setCallback(new SelectionGroupChoiceController.SelectionGroupChoiceFrameCallback() {
            @Override
            public void showStatus(String message, boolean isError) {
                SelectionGroupChoiceFrame.this.showStatus(message, isError);
            }
        });

        // Set up the frame
        setSize(400, 500);
        setLocationRelativeTo(parentFrame);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Create the main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Create the top panel for saving
        JPanel savePanel = new JPanel(new BorderLayout(5, 5));
        savePanel.setBorder(BorderFactory.createTitledBorder("Save Current Selection"));
        
        // Add name field and save button
        nameField = new JTextField(20);
        saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveCurrentSelection());
        
        JPanel namePanel = new JPanel(new BorderLayout(5, 5));
        namePanel.add(new JLabel("Name:"), BorderLayout.WEST);
        namePanel.add(nameField, BorderLayout.CENTER);
        
        savePanel.add(namePanel, BorderLayout.CENTER);
        savePanel.add(saveButton, BorderLayout.EAST);
        
        // Create the center panel for the list
        JPanel listPanel = new JPanel(new BorderLayout(5, 5));
        listPanel.setBorder(BorderFactory.createTitledBorder("Saved Selection Groups"));
        
        // Create the list model and list
        listModel = new DefaultListModel<>();
        savedGroupsList = new JList<>(listModel);
        savedGroupsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        savedGroupsList.addListSelectionListener(e -> updateButtonStates());
        
        // Add the list to a scroll pane
        JScrollPane scrollPane = new JScrollPane(savedGroupsList);
        listPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Create the button panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 1, 0));
        replaceSelectionsButton = new JButton("Replace");
        replaceSelectionsButton.addActionListener(e -> loadSelectedGroup());
        replaceSelectionsButton.setEnabled(false);

        addSelectionsButton = new JButton("Add");
        addSelectionsButton.addActionListener(e -> addSelectedGroup());
        addSelectionsButton.setEnabled(false);

        subtrSelectionsButton = new JButton("Subtract");
        subtrSelectionsButton.addActionListener(e -> subtrSelectedGroup());
        subtrSelectionsButton.setEnabled(false);
        
        deListGroupButton = new JButton("X-Group");
        deListGroupButton.addActionListener(e -> deleteSelectedGroup());
        deListGroupButton.setEnabled(false);
        
        buttonPanel.add(replaceSelectionsButton);
        buttonPanel.add(addSelectionsButton);
        buttonPanel.add(subtrSelectionsButton);
        buttonPanel.add(deListGroupButton);
        listPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Create the status label
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        
        // Add all panels to the main panel
        mainPanel.add(savePanel, BorderLayout.NORTH);
        mainPanel.add(listPanel, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        // Add the main panel to the frame
        add(mainPanel);
        
        // Add window listener to refresh the list when the frame is activated
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                refreshSavedGroupsList();
            }
        });
        
        // Initial refresh of the list
        refreshSavedGroupsList();
    }



    // 031626 added method
    private void subtrSelectedGroup() {
        String name = savedGroupsList.getSelectedValue();
        if (name == null) {
            return;
        }
        
        System.out.println("\n==== SelectionGroupChoiceFrame: subtrSelectedGroup ====");
        System.out.println("Subtracting members of group: " + name);
        
        controller.subtractSelectedGroupFromSelection(name);
        
        parentFrame.repaint();
        System.out.println("==== SelectionGroupChoiceFrame: Finished subtracting selected group ====\n");
    }


    // 031626 added method
    private void addSelectedGroup() {
        String name = savedGroupsList.getSelectedValue();
        if (name == null) {
            return;
        }
        
        System.out.println("\n==== SelectionGroupChoiceFrame: addSelectedGroup ====");
        System.out.println("Adding members of group: " + name);
        
        controller.addSelectedGroupToSelection(name);
        
        parentFrame.repaint();
        System.out.println("==== SelectionGroupChoiceFrame: Finished adding selected group ====\n");
    }

    /**
     * Saves the current selection with the specified name.
     */
    private void saveCurrentSelection() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showStatus("Please enter a name for the selection group", true);
            return;
        }
        
        SelectionGroup currentSelection = template.getDrawObjRoster().getSelectionGroup();
        if (currentSelection == null || currentSelection.isEmpty()) {
            showStatus("No objects are currently selected", true);
            return;
        }
        
        boolean replaced = template.saveNamedSelectionGroup(name, currentSelection);
        if (replaced) {
            showStatus("Replaced existing selection group: " + name, false);
        } else {
            showStatus("Saved new selection group: " + name, false);
        }
        
        refreshSavedGroupsList();
        nameField.setText("");
    }
    
    /**
     * Loads the selected group from the list.
     */
    private void loadSelectedGroup() {
        String name = savedGroupsList.getSelectedValue();
        if (name == null) {
            return;
        }
        
        System.out.println("\n==== SelectionGroupChoiceFrame: Loading selection group: " + name + " ====");
        
        // Get the selection group
        SelectionGroup group = template.getNamedSelectionGroup(name);
        if (group == null) {
            showStatus("Error: Could not load selection group", true);
            System.out.println("SelectionGroupChoiceFrame: Error - group is null");
            return;
        }
        
        // Debug the state of the selection group before resolution
        System.out.println("SelectionGroupChoiceFrame: Before resolution - Group has " + 
                          group.getSelectedDrawObjsSet().size() + " DrawObj references and " + 
                          group.getDrawObjUniqueIds().size() + " UUID references");
        
        // Use Template's centralized reference resolution mechanism
        SelectionGroup resolvedGroup = template.resolveSelectionGroup(group);
        Set<DrawObj> selectedObjs = resolvedGroup.getSelectedDrawObjsSet();
        
        System.out.println("SelectionGroupChoiceFrame: After resolution - Group found and resolved, contains " + 
                          selectedObjs.size() + " objects");
        
        // Debug the DrawObj references in the selection group
        System.out.println("\nSelectionGroupChoiceFrame: DrawObjs in selection group:");
        for (DrawObj obj : selectedObjs) {
            System.out.println("  - DrawObj: " + obj + ", UUID: " + obj.getUniqueId() + 
                              ", Class: " + obj.getClass().getName());
        }
        
        // Clear the current selection
        template.getDrawObjRoster().clearSelection();
        System.out.println("SelectionGroupChoiceFrame: Cleared current selection");
        
        // Add all objects from the selection group to the current selection
        for (DrawObj obj : selectedObjs) {
            template.getDrawObjRoster().addToSelection(obj);
            System.out.println("  - Added " + obj + " to selection");
        }
        
        // Fire a selection event to update the UI
        System.out.println("\nSelectionGroupChoiceFrame: Firing selection event for " + selectedObjs.size() + " objects");
        template.getDrawObjRoster().fireSelectionEventForGroup(
            SelectionEvent.SelectionChangeType.SELECTION_REPLACE,
            selectedObjs
        );
        
        // Force a UI update in the parent frame
        parentFrame.repaint();
        System.out.println("SelectionGroupChoiceFrame: Repainted parent frame");
        
        // Display the number of UUIDs in the selected SelectionGroup
        System.out.println("\n*** SELECTION GROUP UUID COUNT: The SelectionGroup '" + name + 
                          "' contains " + group.getDrawObjUniqueIds().size() + " UUIDs ***");
        
        showStatus("Loaded selection group: " + name + " (" + selectedObjs.size() + " objects)", false);
        System.out.println("==== SelectionGroupChoiceFrame: Finished loading selection group ====\n");
    } // end method loadSelectedGroup


    
    /**
     * Deletes the selected group from the list.
     */
    private void deleteSelectedGroup() {
        String name = savedGroupsList.getSelectedValue();
        if (name == null) {
            return;
        }
        
        boolean removed = template.removeNamedSelectionGroup(name);
        if (removed) {
            showStatus("Deleted selection group: " + name, false);
            refreshSavedGroupsList();
        } else {
            showStatus("Error: Could not delete selection group", true);
        }
    }
    
    /**
     * Updates the frame with the current template from the parent frame.
     * This method should be called when a new template is loaded in the parent frame.
     */
    public void updateWithCurrentTemplate() {
        this.template = parentFrame.getCurrTemplate();
        System.out.println("SelectionGroupChoiceFrame: updateWithCurrentTemplate called, template=" + 
                          (template != null ? template.getTheTemplateName() : "null"));
        
        // Reinitialize the controller with the new template
        if (template != null) {
            this.controller = new SelectionGroupChoiceController(this.template);
            this.controller.setCallback(new SelectionGroupChoiceController.SelectionGroupChoiceFrameCallback() {
                @Override
                public void showStatus(String message, boolean isError) {
                    SelectionGroupChoiceFrame.this.showStatus(message, isError);
                }
            });
            
            Set<String> groupNames = template.getNamedSelectionGroupNames();
            System.out.println("SelectionGroupChoiceFrame: Found " + groupNames.size() + 
                              " named selection groups: " + groupNames);
        }
        
        refreshSavedGroupsList();
        
        // Clear the name field
        nameField.setText("");
        
        // Clear any status message
        showStatus(" ", false);
    }
    
    /**
     * Refreshes the list of saved selection groups.
     */
    private void refreshSavedGroupsList() {
        listModel.clear();
        if (template != null) {
            Set<String> groupNames = template.getNamedSelectionGroupNames();
            System.out.println("SelectionGroupChoiceFrame: refreshSavedGroupsList - Adding " + 
                              groupNames.size() + " groups to list model");
            
            for (String name : groupNames) {
                listModel.addElement(name);
                System.out.println("SelectionGroupChoiceFrame: Added group '" + name + "' to list model");
            }
        }
        updateButtonStates();
    }
    
    /**
     * Updates the enabled state of the buttons based on the current selection.
     */
    private void updateButtonStates() {
        boolean hasSelection = savedGroupsList.getSelectedIndex() != -1;
        replaceSelectionsButton.setEnabled(hasSelection);
        addSelectionsButton.setEnabled(hasSelection);
        subtrSelectionsButton.setEnabled(hasSelection);
        deListGroupButton.setEnabled(hasSelection);
    }
    
    /**
     * Shows a status message.
     *
     * @param message The message to show
     * @param isError Whether the message is an error
     */
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? Color.RED : Color.BLACK);
    }
}
