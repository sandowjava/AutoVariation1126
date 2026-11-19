package com.csurf.variationmodule.controller;

import com.csurf.core.ApplicationMediator;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.storage.TemplateStorage;
import com.csurf.templatecore.storage.TemplateStorageException;
import com.csurf.templatecore.storage.config.StorageConfig;
import com.csurf.templatecore.view.TemplateWorkFrame;
import com.csurf.variationmodule.model.SmartPic;
import com.csurf.variationmodule.model.SmartPicFactory;
import com.csurf.variationmodule.model.SmartPicFactoryException;
import com.csurf.variationmodule.view.CreateSmartPicFrame;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Orchestrates the create-SmartPic feature: opens a CreateSmartPicFrame whose
 * menu items let the user pick a saved Template and an AGprefs defaults file,
 * then build a SmartPic via {@link SmartPicFactory}.
 *
 * <p>One controller per open frame (multi-window support), mirroring
 * {@link VariationDisplayController}. The bundled
 * {@code agprefs/authored-defaults-v1.json} classpath resource is extracted to
 * {@code dev_storage/agprefs} on first use so the chooser has a working
 * defaults file; additional defaults files may be dropped in that directory.
 */
public class CreateSmartPicController implements CreateSmartPicFrame.Actions {

    private static final Logger LOG = Logger.getLogger(CreateSmartPicController.class.getName());

    private static final String BUNDLED_AGPREFS_RESOURCE = "/agprefs/authored-defaults-v1.json";
    private static final String BUNDLED_AGPREFS_FILENAME = "authored-defaults-v1.json";

    private final ApplicationMediator mediator;
    private final TemplateWorkFrame ownerFrame;
    private final TemplateStorage storage;
    private final SmartPicFactory factory;
    private final CreateSmartPicFrame frame;

    private File chosenTemplateFile;
    private Template chosenTemplate;
    private Path chosenAgPrefsPath;
    private SmartPic smartPic;

    /**
     * Creates a controller with no owning TemplateWorkFrame — for launches
     * from the main ApplicFrame menu. Dialogs center on screen instead.
     *
     * @param mediator the shared application mediator (message routing)
     */
    public CreateSmartPicController(ApplicationMediator mediator) {
        this(mediator, null);
    }

    /**
     * @param mediator   the shared application mediator (message routing)
     * @param ownerFrame the TemplateWorkFrame whose menu opened this feature,
     *                   or null when launched from the main ApplicFrame menu
     */
    public CreateSmartPicController(ApplicationMediator mediator, TemplateWorkFrame ownerFrame) {
        this.mediator = mediator;
        this.ownerFrame = ownerFrame;
        this.storage = new TemplateStorage(mediator);
        this.factory = new SmartPicFactory();
        this.frame = new CreateSmartPicFrame(this, "Create SmartPic");
    }

    /** Opens the CreateSmartPicFrame. Called from the menu action. */
    public void launch() {
        frame.setLocation(CreateSmartPicFrame.OPEN_LOCATION);
        frame.setVisible(true);
    }

    // ---------- CreateSmartPicFrame.Actions ----------

    @Override
    public void chooseTemplate() {
        File selected = chooseFile(storage.getStorageDir().toFile(),
                "Select Template for SmartPic", "Template Files (*.json)");
        if (selected == null) {
            return; // user cancelled
        }
        String templateName = stripJsonExtension(selected.getName());
        try {
            chosenTemplate = storage.loadTemplate(templateName);
        } catch (TemplateStorageException e) {
            showError("Error loading template: " + e.getMessage());
            return;
        }
        chosenTemplateFile = selected;
        String displayName = chosenTemplate.getTheTemplateName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = templateName;
        }
        frame.setTemplateName(displayName);
    }

    @Override
    public void chooseAgPrefsFile() {
        File startDir = ensureAgPrefsDir();
        File selected = chooseFile(startDir,
                "Select AGprefs Defaults File", "AGprefs Files (*.json)");
        if (selected == null) {
            return; // user cancelled
        }
        try {
            SmartPicFactory.AgPrefsDocument doc = factory.loadAgPrefs(selected.toPath());
            chosenAgPrefsPath = selected.toPath();
            frame.setAgPrefsName(doc.displayName() + " (" + selected.getName() + ")");
        } catch (SmartPicFactoryException e) {
            showError(e.getMessage());
        }
    }

    @Override
    public void createSmartPic() {
        if (chosenTemplate == null) {
            showError("Choose a Template first (SmartPic menu).");
            return;
        }
        if (chosenAgPrefsPath == null) {
            showError("Choose an AGprefs defaults file first (SmartPic menu).");
            return;
        }
        try {
            smartPic = factory.createSmartPic(chosenTemplate, chosenAgPrefsPath);
        } catch (SmartPicFactoryException e) {
            showError(e.getMessage());
            return;
        }
        frame.setStatus("Created SmartPic: " + smartPic.numShapes() + " shapes");
        LOG.info("Created " + smartPic + " from template file " + chosenTemplateFile
                + " and AGprefs " + chosenAgPrefsPath);
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
     * Returns the AGprefs defaults directory, creating it and extracting the
     * bundled defaults file on first use. Falls back to the templates dir if
     * extraction fails so the chooser still opens somewhere sensible.
     */
    private File ensureAgPrefsDir() {
        Path dir = StorageConfig.getAgPrefsPath();
        try {
            Files.createDirectories(dir);
            // Always re-sync the bundled file: the classpath resource is the
            // single source of truth, and a stale extracted copy silently
            // serves old prefs. Other files in this dir (user variants) are
            // never touched.
            Path bundled = dir.resolve(BUNDLED_AGPREFS_FILENAME);
            try (InputStream in = getClass().getResourceAsStream(BUNDLED_AGPREFS_RESOURCE)) {
                if (in != null) {
                    Files.copy(in, bundled, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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
        JOptionPane.showMessageDialog(frame, message, "Create SmartPic", JOptionPane.ERROR_MESSAGE);
    }

    private String stripJsonExtension(String fileName) {
        if (fileName != null && fileName.endsWith(".json")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }

    // ---------- accessors ----------

    /** The created SmartPic, or null if not yet created. */
    public SmartPic getSmartPic() {
        return smartPic;
    }

    public CreateSmartPicFrame getFrame() {
        return frame;
    }

    public TemplateWorkFrame getOwnerFrame() {
        return ownerFrame;
    }

    public ApplicationMediator getMediator() {
        return mediator;
    }
} // end class CreateSmartPicController
