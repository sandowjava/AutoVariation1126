package com.csurf.variationmodule.controller;

import com.csurf.core.ApplicationMediator;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.storage.TemplateStorage;
import com.csurf.templatecore.storage.TemplateStorageException;
import com.csurf.templatecore.storage.config.StorageConfig;
import com.csurf.templatecore.view.TemplateWorkFrame;
import com.csurf.variationmodule.model.RelationMatrixDraft;
import com.csurf.variationmodule.model.RelationMatrixDraft.DraftRow;
import com.csurf.variationmodule.model.ShapeColorPrefs;
import com.csurf.variationmodule.model.SmartPicFactory;
import com.csurf.variationmodule.model.SmartPicFactory.AgPrefsDocument;
import com.csurf.variationmodule.model.SmartPicFactoryException;
import com.csurf.variationmodule.view.RelationMatrixEditorFrame;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Orchestrates the relation-matrix editor: opens a
 * {@link RelationMatrixEditorFrame} whose File menu loads a saved Template
 * and an AGprefs defaults file into a {@link RelationMatrixDraft} — a
 * mutable working copy whose rows the user edits and re-binds, then saves
 * back as a formatVersion 2 (UUID-keyed) defaults file.
 *
 * <p>One controller per open frame (multi-window support), mirroring
 * {@link CreateSmartPicController}. File-level validation is inherited from
 * {@link SmartPicFactory#loadAgPrefs}; binding completeness is checked at
 * save time — a foreign-template file may be edited with rows unbound.
 *
 * <p>Design doc: {@code 112126_v0G_SWE-2_RelationMatrix_and_Editor-UI_Design.md}.
 */
public class RelationMatrixEditorController implements RelationMatrixEditorFrame.Actions {

    private static final Logger LOG =
            Logger.getLogger(RelationMatrixEditorController.class.getName());

    private static final String BUNDLED_AGPREFS_RESOURCE = "/agprefs/authored-defaults-v1.json";
    private static final String BUNDLED_AGPREFS_FILENAME = "authored-defaults-v1.json";

    private final ApplicationMediator mediator;
    private final TemplateWorkFrame ownerFrame;
    private final TemplateStorage storage;
    private final SmartPicFactory factory;
    private final RelationMatrixEditorFrame frame;

    private File chosenTemplateFile;
    private Template chosenTemplate;
    private Path sourcePath;
    private RelationMatrixDraft draft;
    private boolean dirty;

    /**
     * Creates a controller with no owning TemplateWorkFrame — for launches
     * from the main ApplicFrame menu. Dialogs center on screen instead.
     */
    public RelationMatrixEditorController(ApplicationMediator mediator) {
        this(mediator, null);
    }

    /**
     * @param mediator   the shared application mediator (message routing)
     * @param ownerFrame the TemplateWorkFrame whose menu opened this feature,
     *                   or null when launched from the main ApplicFrame menu
     */
    public RelationMatrixEditorController(ApplicationMediator mediator,
                                          TemplateWorkFrame ownerFrame) {
        this.mediator = mediator;
        this.ownerFrame = ownerFrame;
        this.storage = new TemplateStorage(mediator);
        this.factory = new SmartPicFactory();
        this.frame = new RelationMatrixEditorFrame(this, "Relation Matrix Editor");
    }

    /** Opens the editor frame. Called from the menu action. */
    public void launch() {
        frame.setLocation(RelationMatrixEditorFrame.OPEN_LOCATION);
        frame.setVisible(true);
    }

    // ---------- RelationMatrixEditorFrame.Actions ----------

    @Override
    public void chooseTemplate() {
        File selected = chooseFile(storage.getStorageDir().toFile(),
                "Select Template", "Template Files (*.json)");
        if (selected == null) {
            return; // user cancelled
        }
        String templateName = stripJsonExtension(selected.getName());
        Template loaded;
        try {
            loaded = storage.loadTemplate(templateName);
        } catch (TemplateStorageException e) {
            showError("Error loading template: " + e.getMessage());
            return;
        }
        chosenTemplate = loaded;
        chosenTemplateFile = selected;
        if (draft != null) {
            draft.bindToTemplate(chosenTemplate);
        }
        frame.setTemplateDrawObjs(chosenTemplate.getAllDrawObjs());
        String displayName = chosenTemplate.getTheTemplateName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = templateName;
        }
        frame.setTemplateName(displayName);
        refreshHeader();
        frame.setStatus("Template loaded: " + displayName);
    }

    @Override
    public void openMatrixFile() {
        File startDir = ensureAgPrefsDir();
        File selected = chooseFile(startDir,
                "Open Matrix File", "AGprefs Files (*.json)");
        if (selected == null) {
            return; // user cancelled
        }
        AgPrefsDocument doc;
        try {
            doc = factory.loadAgPrefs(selected.toPath());
        } catch (SmartPicFactoryException e) {
            showError(e.getMessage());
            return;
        }
        draft = RelationMatrixDraft.fromDocument(doc);
        sourcePath = selected.toPath();
        if (chosenTemplate != null) {
            draft.bindToTemplate(chosenTemplate);
        }
        dirty = false;
        frame.setDraft(draft);
        refreshHeader();
        frame.setStatus("Opened " + selected.getName()
                + " (formatVersion " + draft.formatVersion() + ")");
    }

    @Override
    public void save() {
        if (sourcePath == null) {
            saveAs();
            return;
        }
        writeDraftTo(sourcePath);
    }

    @Override
    public void saveAs() {
        if (draft == null) {
            showError("Open a matrix file first (File menu).");
            return;
        }
        if (!checkFullyBound()) {
            return;
        }
        File startDir = ensureAgPrefsDir();
        JFileChooser fileChooser = new JFileChooser(startDir);
        fileChooser.setDialogTitle("Save Relation Matrix As");
        fileChooser.setFileFilter(
                new FileNameExtensionFilter("AGprefs Files (*.json)", "json"));
        if (fileChooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = fileChooser.getSelectedFile();
        if (!target.getName().endsWith(".json")) {
            target = new File(target.getParentFile(), target.getName() + ".json");
        }
        if (target.exists()) {
            int choice = JOptionPane.showConfirmDialog(frame,
                    "Overwrite " + target.getName() + "?",
                    "Save As", JOptionPane.OK_CANCEL_OPTION);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }
        }
        if (!promptDocMeta()) {
            return; // user cancelled the name/description dialog
        }
        writeDraftTo(target.toPath());
    }

    @Override
    public void bindByPosition() {
        if (draft == null) {
            showError("Open a matrix file first (File menu).");
            return;
        }
        if (chosenTemplate == null) {
            showError("Choose a Template first (File menu).");
            return;
        }
        draft.bindByPosition();
        markDirty();
        refreshHeader();
        frame.refreshTable();
        frame.setStatus("Bound rows by position (row N → drawObj N)");
    }

    @Override
    public String cellEdited(DraftRow row, int column, String text) {
        try {
            if (column == 4) { // Default AG
                row.setDefaultAG(Integer.parseInt(text.trim()));
            } else {
                double value = Double.parseDouble(text.trim());
                if (column == 3) { // Value Target
                    if (value < 0.0 || value > 1.0) {
                        return "Value target must be 0.0–1.0";
                    }
                    row.setValueTarget(value);
                } else { // AG pref columns 5..8
                    if (value < 0.0 || value > 1.0) {
                        return "AG preference must be 0.0–1.0";
                    }
                    row.setAgPref(column - 4, value);
                }
            }
        } catch (NumberFormatException e) {
            return "Not a number: '" + text + "'";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        markDirty();
        return null;
    }

    @Override
    public void rowBoundTo(DraftRow row, UUID drawObjUuid) {
        if (draft == null) {
            return;
        }
        try {
            if (drawObjUuid == null) {
                draft.unbindRow(row);
            } else {
                draft.bindRowTo(row, drawObjUuid);
            }
        } catch (IllegalArgumentException e) {
            frame.setStatus(e.getMessage());
            return;
        }
        markDirty();
        refreshHeader();
        frame.refreshTable();
    }

    // ---------- internals ----------

    /** Writes the draft as a v2 document; reports success/failure. */
    private void writeDraftTo(Path target) {
        if (draft == null) {
            showError("Open a matrix file first (File menu).");
            return;
        }
        if (!checkFullyBound()) {
            return;
        }
        try {
            SmartPicFactory.writeAgPrefs(target, draft.toDocument());
        } catch (SmartPicFactoryException e) {
            showError(e.getMessage());
            return;
        }
        sourcePath = target;
        dirty = false;
        refreshHeader();
        frame.setStatus("Saved " + target.getFileName()
                + " (formatVersion 2, " + draft.rows().size() + " rows)");
        LOG.info("Saved relation matrix to " + target);
    }

    /** Gate for save paths; explains what's missing. */
    private boolean checkFullyBound() {
        if (chosenTemplate == null) {
            showError("Choose a Template before saving — the file's rows bind "
                    + "to its DrawObjs.");
            return false;
        }
        if (!draft.isFullyBound()) {
            showError("Cannot save: " + draft.unboundCount() + " row(s) unbound, "
                    + draft.uncoveredCount() + " Template DrawObj(s) uncovered.\n"
                    + "Bind rows via the 'Bind To' column or Binding → Bind by Position.");
            return false;
        }
        return true;
    }

    /**
     * Save-As dialog for the document's name/description fields, prefilled
     * from the draft.
     *
     * @return false if the user cancelled
     */
    private boolean promptDocMeta() {
        JTextField nameField = new JTextField(
                draft.name() != null ? draft.name() : "", 24);
        JTextField descField = new JTextField(
                draft.description() != null ? draft.description() : "", 24);
        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.add(new JLabel("name:"));
        panel.add(nameField);
        panel.add(new JLabel("description:"));
        panel.add(descField);
        int choice = JOptionPane.showConfirmDialog(frame, panel,
                "Document metadata", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) {
            return false;
        }
        draft.setName(nameField.getText().trim());
        draft.setDescription(descField.getText().trim());
        return true;
    }

    /** Recomputes the header labels from current state. */
    private void refreshHeader() {
        if (draft == null) {
            frame.setFileLabel("(none open)");
            frame.setBindingStatus(" ");
            return;
        }
        String fileText = sourcePath != null
                ? sourcePath.getFileName().toString() : "(unsaved)";
        if (draft.name() != null && !draft.name().isEmpty()) {
            fileText = draft.name() + " (" + fileText + ")";
        }
        if (dirty) {
            fileText += "  * unsaved changes";
        }
        frame.setFileLabel(fileText);

        if (chosenTemplate == null) {
            frame.setBindingStatus(draft.rows().size()
                    + " rows — no Template chosen; rows cannot bind yet");
        } else if (draft.isFullyBound()) {
            frame.setBindingStatus(draft.rows().size()
                    + " rows — all bound (formatVersion " + draft.formatVersion() + ")");
        } else {
            frame.setBindingStatus(draft.unboundCount() + " row(s) unbound, "
                    + draft.uncoveredCount() + " DrawObj(s) uncovered");
        }
    }

    private void markDirty() {
        dirty = true;
        refreshHeader();
    }

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
     * Returns the AGprefs defaults directory, creating it and extracting
     * the bundled defaults file on first use. Mirrors
     * {@link CreateSmartPicController#ensureAgPrefsDir} — same seam, same
     * bundled file. Falls back to the templates dir if preparation fails.
     */
    private File ensureAgPrefsDir() {
        Path dir = StorageConfig.getAgPrefsPath();
        try {
            Files.createDirectories(dir);
            Path bundled = dir.resolve(BUNDLED_AGPREFS_FILENAME);
            try (InputStream in = getClass().getResourceAsStream(BUNDLED_AGPREFS_RESOURCE)) {
                if (in != null) {
                    Files.copy(in, bundled,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } else {
                    LOG.warning("Bundled AGprefs resource not found: " + BUNDLED_AGPREFS_RESOURCE);
                }
            }
            return dir.toFile();
        } catch (IOException e) {
            LOG.warning("Could not prepare AGprefs dir " + dir + ": " + e.getMessage());
            return storage.getStorageDir().toFile();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message,
                "Relation Matrix Editor", JOptionPane.ERROR_MESSAGE);
    }

    private String stripJsonExtension(String fileName) {
        if (fileName != null && fileName.endsWith(".json")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }

    // ---------- accessors ----------

    /** The editor frame. */
    public RelationMatrixEditorFrame getFrame() {
        return frame;
    }

    /** The current draft, or null if no file is open. */
    public RelationMatrixDraft getDraft() {
        return draft;
    }
} // end class RelationMatrixEditorController
