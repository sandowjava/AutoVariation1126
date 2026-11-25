package com.csurf.variationmodule.controller;

import com.csurf.core.ApplicationMediator;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.storage.TemplateStorage;
import com.csurf.templatecore.storage.TemplateStorageException;
import com.csurf.templatecore.storage.config.StorageConfig;
import com.csurf.templatecore.view.TemplateWorkFrame;
import com.csurf.variationmodule.model.SmartPicFactory;
import com.csurf.variationmodule.model.SmartPicFactory.AgPrefsDocument;
import com.csurf.variationmodule.model.SmartPicFactoryException;
import com.csurf.variationmodule.varyengine.model.AGHuePatterns;
import com.csurf.variationmodule.varyengine.model.AnchorHueSet;
import com.csurf.variationmodule.view.AGHuePatternSaveDialog;
import com.csurf.variationmodule.view.CreateRMatrixFrame;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Orchestrates the create-tailored-RMatrix feature: opens a
 * {@link CreateRMatrixFrame} whose menu lets the user pick a saved Template
 * (must have exactly four distinct hues in use), assign the four detected
 * hues to AG1–AG4, then generate and save an AGprefs defaults file
 * (formatVersion 2) via {@link RMatrixFactory} +
 * {@link SmartPicFactory#writeAgPrefs}.
 *
 * <p>The saved file is consumed unchanged by the existing Create SmartPic
 * flow — user interaction ends at the save. One controller per open frame,
 * mirroring {@link CreateSmartPicController}. Design doc:
 * {@code 112326_v1H_SWE-2_create_RMatrix_from_Template.md}.
 */
public class CreateRMatrixController implements CreateRMatrixFrame.Actions {

    private static final Logger LOG = Logger.getLogger(CreateRMatrixController.class.getName());

    /** Suggested file name is derived from the first N chars of the Template name. */
    private static final int SUGGESTED_NAME_CHARS = 20;
    private static final String FILE_SUFFIX = "-rmatrix";

    private final ApplicationMediator mediator;
    private final TemplateWorkFrame ownerFrame;
    private final TemplateStorage storage;
    private final RMatrixFactory rMatrixFactory;
    private final CreateRMatrixFrame frame;

    private File chosenTemplateFile;
    private Template chosenTemplate;
    private AnchorHueSet anchorHueSet;

    /**
     * Creates a controller with no owning TemplateWorkFrame — for launches
     * from the main ApplicFrame menu. Dialogs center on screen instead.
     *
     * @param mediator the shared application mediator (message routing)
     */
    public CreateRMatrixController(ApplicationMediator mediator) {
        this(mediator, null);
    }

    /**
     * @param mediator   the shared application mediator (message routing)
     * @param ownerFrame the TemplateWorkFrame whose menu opened this feature,
     *                   or null when launched from the main ApplicFrame menu
     */
    public CreateRMatrixController(ApplicationMediator mediator, TemplateWorkFrame ownerFrame) {
        this.mediator = mediator;
        this.ownerFrame = ownerFrame;
        this.storage = new TemplateStorage(mediator);
        this.rMatrixFactory = new RMatrixFactory();
        this.frame = new CreateRMatrixFrame(this, "Create RMatrix from Template");
    }

    /** Opens the CreateRMatrixFrame. Called from the menu action. */
    public void launch() {
        frame.setLocation(CreateRMatrixFrame.OPEN_LOCATION);
        frame.setVisible(true);
    }

    // ---------- CreateRMatrixFrame.Actions ----------

    @Override
    public void chooseTemplate() {
        File selected = chooseFile(StorageConfig.getTemplatesPath().toFile(),
                "Select Template for RMatrix", "Template Files (*.json)");
        if (selected == null) {
            return; // user cancelled
        }
        String templateName = stripJsonExtension(selected.getName());
        Template candidate;
        try {
            candidate = storage.loadTemplate(templateName);
        } catch (TemplateStorageException e) {
            showError("Error loading template: " + e.getMessage());
            return;
        }

        // Guard: exactly four distinct hues in use, no unset fills
        String error = RMatrixFactory.eligibilityError(candidate);
        if (error != null) {
            showError(error);
            return;
        }

        chosenTemplate = candidate;
        chosenTemplateFile = selected;
        anchorHueSet = null;
        frame.setCreateEnabled(false);
        frame.setSavePatternEnabled(false);

        String displayName = chosenTemplate.getTheTemplateName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = templateName;
        }
        frame.setTemplateName(displayName);

        Set<Integer> hues = RMatrixFactory.occupiedHues(chosenTemplate);
        frame.showHueAssignment(hues, this::assignmentChanged);
        frame.setStatus("Assign the four hues to AG1-AG4, then confirm.");
    }

    @Override
    public void confirmAnchorHues() {
        if (chosenTemplate == null) {
            showError("Choose a Template first (Template menu).");
            return;
        }
        int[] byAG;
        try {
            byAG = frame.getAnchorAssignment();
        } catch (IllegalStateException e) {
            showError("Assign each of the four hues to a distinct AG first.");
            return;
        }
        anchorHueSet = new AnchorHueSet(byAG[0], byAG[1], byAG[2], byAG[3]);
        frame.setCreateEnabled(true);
        frame.setSavePatternEnabled(true);
        frame.setStatus("Anchor hues confirmed: " + anchorHueSet
                + " — ready to create the RMatrix file.");
        LOG.info("AnchorHueSet confirmed for template file " + chosenTemplateFile
                + ": " + anchorHueSet);
    }

    @Override
    public void createRMatrix() {
        if (chosenTemplate == null) {
            showError("Choose a Template first (Template menu).");
            return;
        }
        if (anchorHueSet == null) {
            showError("Confirm the hue→AG assignment first.");
            return;
        }

        AgPrefsDocument doc;
        try {
            doc = rMatrixFactory.createAllShapeColorPrefs(chosenTemplate, anchorHueSet);
        } catch (SmartPicFactoryException e) {
            showError(e.getMessage());
            return;
        }

        Path destination = chooseSaveDestination();
        if (destination == null) {
            return; // user cancelled
        }

        String docName = stripJsonExtension(destination.getFileName().toString());
        doc.name = docName;
        doc.description = "Tailored RMatrix generated from Template '"
                + chosenTemplateFile.getName() + "' with anchor hues "
                + anchorHueSet + " (AG1-AG4).";

        try {
            SmartPicFactory.writeAgPrefs(destination, doc);
        } catch (SmartPicFactoryException e) {
            showError(e.getMessage());
            return;
        }

        frame.setStatus("Saved " + doc.shapes.size() + " shape rows to "
                + destination.getFileName());
        LOG.info("Saved tailored RMatrix (" + doc.shapes.size() + " rows) to "
                + destination);
    }

    /**
     * "Save anchor hues as AGHuePattern..." — Phase 3 hook: persists the
     * confirmed tailored set into the named catalog so the same four
     * AG-order hues can be selected by name in the variation run dialog
     * and the diagnostic frame. Reuses the Phase 2 dialog pre-filled with
     * the confirmed {@link AnchorHueSet} and a Template-derived suffix
     * suggestion (e.g. {@code heroEyeTailored}) so the saved name records
     * which RMatrix the pattern was tailored for.
     */
    @Override
    public void saveAnchorHuesAsPattern() {
        if (anchorHueSet == null) {
            showError("Confirm the hue→AG assignment first.");
            return;
        }
        AGHuePatternSaveDialog.SaveResult result = AGHuePatternSaveDialog
                .showDialog(frame, anchorHueSet, suggestedPatternSuffix());
        if (result == null) {
            return; // user cancelled
        }
        String name = result.pattern().getName();
        try {
            if (!AGHuePatternSaveDialog.saveWithConfirm(frame, result)) {
                return; // user declined the overwrite
            }
        } catch (IllegalStateException e) {
            LOG.warning("Could not save AGHuePattern: " + e);
            showError("Could not save AGHuePattern: " + e.getMessage());
            return;
        }
        frame.setStatus("Saved AGHuePattern '" + name + "' — "
                + AGHuePatterns.count() + " patterns in catalog");
        LOG.info("Saved tailored AGHuePattern '" + name + "' (from "
                + chosenTemplateFile.getName() + ") to "
                + StorageConfig.getAGHuePatternsPath());
    }

    @Override
    public void assignmentChanged() {
        // Any selector change invalidates the confirmed AnchorHueSet
        if (anchorHueSet != null) {
            anchorHueSet = null;
            frame.setCreateEnabled(false);
            frame.setSavePatternEnabled(false);
            frame.setStatus("Assignment changed — confirm the anchor hues again.");
        }
    }

    // ---------- internals ----------

    private File chooseFile(File startDir, String dialogTitle, String filterDescription) {
        JFileChooser fileChooser = new JFileChooser(startDir);
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setFileFilter(new FileNameExtensionFilter(filterDescription, "json"));
        int result = fileChooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return fileChooser.getSelectedFile();
    }

    /**
     * Save dialog in {@code dev_storage/agprefs} (created if needed), name
     * fully editable with a suggested default derived from the first
     * {@value #SUGGESTED_NAME_CHARS} characters of the Template name plus
     * {@value #FILE_SUFFIX}.
     */
    private Path chooseSaveDestination() {
        Path dir = StorageConfig.getAgPrefsPath();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOG.warning("Could not create AGprefs dir " + dir + ": " + e.getMessage());
        }

        String base = chosenTemplate.getTheTemplateName();
        if (base == null || base.isEmpty()) {
            base = stripJsonExtension(chosenTemplateFile.getName());
        }
        if (base.length() > SUGGESTED_NAME_CHARS) {
            base = base.substring(0, SUGGESTED_NAME_CHARS);
        }

        JFileChooser fileChooser = new JFileChooser(dir.toFile());
        fileChooser.setDialogTitle("Save tailored RMatrix defaults file");
        fileChooser.setFileFilter(new FileNameExtensionFilter("AGprefs Files (*.json)", "json"));
        fileChooser.setSelectedFile(new File(base + FILE_SUFFIX + ".json"));

        int result = fileChooser.showSaveDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File chosen = fileChooser.getSelectedFile();
        if (!chosen.getName().endsWith(".json")) {
            chosen = new File(chosen.getParentFile(), chosen.getName() + ".json");
        }
        if (chosen.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(frame,
                    chosen.getName() + " already exists. Overwrite?",
                    "Save RMatrix", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.OK_OPTION) {
                return null;
            }
        }
        return chosen.toPath();
    }

    /**
     * Suggested pattern-name suffix: the Template display name (or file
     * stem) truncated to {@value #SUGGESTED_NAME_CHARS} chars plus
     * "Tailored" — records which RMatrix the pattern was created for.
     */
    private String suggestedPatternSuffix() {
        String base = chosenTemplate.getTheTemplateName();
        if (base == null || base.isEmpty()) {
            base = stripJsonExtension(chosenTemplateFile.getName());
        }
        if (base.length() > SUGGESTED_NAME_CHARS) {
            base = base.substring(0, SUGGESTED_NAME_CHARS);
        }
        return base + "Tailored";
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message,
                "Create RMatrix from Template", JOptionPane.ERROR_MESSAGE);
    }

    private String stripJsonExtension(String fileName) {
        if (fileName != null && fileName.endsWith(".json")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }

    // ---------- accessors ----------

    /** The created AnchorHueSet, or null if not yet confirmed. */
    public AnchorHueSet getAnchorHueSet() {
        return anchorHueSet;
    }

    public CreateRMatrixFrame getFrame() {
        return frame;
    }

    public TemplateWorkFrame getOwnerFrame() {
        return ownerFrame;
    }

    public ApplicationMediator getMediator() {
        return mediator;
    }
} // end class CreateRMatrixController
