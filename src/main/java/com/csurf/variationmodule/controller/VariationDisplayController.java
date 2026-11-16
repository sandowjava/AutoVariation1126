package com.csurf.variationmodule.controller;

import com.csurf.core.ApplicationMediator;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.storage.TemplateStorage;
import com.csurf.templatecore.storage.TemplateStorageException;
import com.csurf.templatecore.view.TemplateWorkFrame;
import com.csurf.variationmodule.view.VariationDisplayFrame;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the palette-variations feature: lets the user pick a serialized
 * Template, loads 12 independent Template instances, and opens one
 * VariationDisplayFrame rendering them.
 *
 * One controller instance is created per menu invocation, so each open
 * VariationDisplayFrame has its own controller (multi-window requirement).
 * Holds a reference to the TemplateWorkFrame that opened it for future
 * recoloring flows (palette applied back to the owner window).
 */
public class VariationDisplayController {
    private static final int VARIATION_COUNT = 12;

    private final ApplicationMediator mediator;
    private final TemplateWorkFrame ownerFrame;
    private final TemplateStorage storage;
    private final UUID windowId;
    private VariationDisplayFrame frame;

    /**
     * @param mediator the shared application mediator (message routing)
     * @param ownerFrame the TemplateWorkFrame whose menu opened this feature
     */
    public VariationDisplayController(ApplicationMediator mediator, TemplateWorkFrame ownerFrame) {
        this.mediator = mediator;
        this.ownerFrame = ownerFrame;
        this.storage = new TemplateStorage(mediator);
        this.windowId = UUID.randomUUID();
    }

    /**
     * Shows the file chooser, loads the selected Template 12 times, and opens
     * the VariationDisplayFrame. Called from the Color-sources menu action.
     */
    public void launch() {
        File selectedFile = chooseTemplateFile();
        if (selectedFile == null) {
            return; // user cancelled
        }

        String templateName = stripJsonExtension(selectedFile.getName());

        // V1 hack: 12 identical renderings -> load the same file 12 times.
        // Gives fully independent Template instances (no clone machinery needed).
        List<Template> variationTemplates = new ArrayList<>(VARIATION_COUNT);
        try {
            for (int i = 0; i < VARIATION_COUNT; i++) {
                variationTemplates.add(storage.loadTemplate(templateName));
            }
        } catch (TemplateStorageException e) {
            JOptionPane.showMessageDialog(ownerFrame,
                    "Error loading template: " + e.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String displayName = variationTemplates.get(0).getTheTemplateName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = templateName;
        }

        frame = new VariationDisplayFrame(variationTemplates, windowId,
                "Variations: " + displayName);
        frame.setLocationRelativeTo(ownerFrame);
        frame.setVisible(true);
    }

    private File chooseTemplateFile() {
        JFileChooser fileChooser = new JFileChooser(storage.getStorageDir().toFile());
        fileChooser.setDialogTitle("Select Template for Palette Variations");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Template Files (*.json)", "json"));

        int result = fileChooser.showOpenDialog(ownerFrame);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return fileChooser.getSelectedFile();
    }

    private String stripJsonExtension(String fileName) {
        if (fileName != null && fileName.endsWith(".json")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }

    public UUID getWindowId() {
        return windowId;
    }

    public VariationDisplayFrame getFrame() {
        return frame;
    }

    public TemplateWorkFrame getOwnerFrame() {
        return ownerFrame;
    }

    public ApplicationMediator getMediator() {
        return mediator;
    }
} // end class VariationDisplayController

