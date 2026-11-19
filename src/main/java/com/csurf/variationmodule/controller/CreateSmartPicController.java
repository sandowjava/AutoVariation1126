package com.csurf.variationmodule.controller;

import com.csurf.core.ApplicationMediator;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.PaletteWName;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.storage.TemplateStorage;
import com.csurf.templatecore.storage.TemplateStorageException;
import com.csurf.templatecore.storage.config.StorageConfig;
import com.csurf.templatecore.view.TemplateWorkFrame;
import com.csurf.variationmodule.model.SmartPic;
import com.csurf.variationmodule.model.SmartPicFactory;
import com.csurf.variationmodule.model.SmartPicFactoryException;
import com.csurf.variationmodule.varyengine.controller.PalettesThe12Variations;
import com.csurf.variationmodule.varyengine.controller.VariationPaletteExtractor;
import com.csurf.variationmodule.varyengine.model.AnchorHueSets;
import com.csurf.variationmodule.varyengine.model.Facts;
import com.csurf.variationmodule.varyengine.model.FrozenSets;
import com.csurf.variationmodule.varyengine.model.SmartPicFacts;
import com.csurf.variationmodule.varyengine.model.VariationResult;
import com.csurf.variationmodule.view.CreateSmartPicFrame;
import com.csurf.variationmodule.view.VariationDisplayFrame;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    @Override
    public void createVariations() {
        if (smartPic == null) {
            showError("Create a SmartPic first (SmartPic menu).");
            return;
        }
        int[] params = chooseRunParameters();
        if (params == null) {
            return; // user cancelled
        }
        int hueSet = params[0];
        int frozenSet = params[1];

        Facts facts = new SmartPicFacts(smartPic);
        PalettesThe12Variations palettes;
        try {
            List<VariationResult> results =
                    VariationPaletteExtractor.runCleanCycle(hueSet, frozenSet, facts);
            palettes = new VariationPaletteExtractor().extract12VariationPalettes(results);
        } catch (RuntimeException e) {
            LOG.warning("Variation run failed: " + e);
            showError("Variation run failed: " + e.getMessage());
            return;
        }

        String templateName = stripJsonExtension(chosenTemplateFile.getName());
        List<Template> variationTemplates;
        try {
            variationTemplates = loadPaletteColoredTemplates(templateName, palettes);
        } catch (RuntimeException e) {
            LOG.warning("Could not apply variation palettes: " + e);
            showError("Could not apply variation palettes: " + e.getMessage());
            return;
        }

        String displayName = chosenTemplate.getTheTemplateName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = templateName;
        }
        VariationDisplayFrame display = new VariationDisplayFrame(variationTemplates,
                UUID.randomUUID(), "Variations: " + displayName
                + " (hueSet " + hueSet + ", frozenSet " + frozenSet + ")");
        display.setLocation(frame.getX() + frame.getWidth() + 10, frame.getY());
        display.setVisible(true);

        frame.setStatus("Created 12 variations: hueSet " + hueSet
                + ", frozenSet " + frozenSet);
    }

    /**
     * Loads 12 independent copies of the saved Template (same mechanism as
     * {@link VariationDisplayController}) and writes palette k's colors onto
     * copy k positionally: dots[i] ↔ shape i+1 ↔
     * {@code getAllDrawObjs().get(i)} — the same positional contract the
     * factory uses for AGprefs rows.
     */
    private List<Template> loadPaletteColoredTemplates(String templateName,
                                                       PalettesThe12Variations palettes)
            throws TemplateStorageException {
        List<Template> copies = new ArrayList<>(PalettesThe12Variations.NUM_VARIATIONS);
        for (int v = 0; v < PalettesThe12Variations.NUM_VARIATIONS; v++) {
            Template copy = storage.loadTemplate(templateName);
            PaletteWName palette = palettes.getPaletteAt(v);
            List<DrawObj> drawObjs = copy.getAllDrawObjs();
            if (palette.getSize() != drawObjs.size()) {
                throw new IllegalStateException(
                        "palette " + palette.getPaletteName() + " has " + palette.getSize()
                                + " colors but Template " + templateName + " has "
                                + drawObjs.size() + " draw objects");
            }
            ColorPtOstDot[] dots = palette.getColorPtOstdotArray();
            for (int i = 0; i < drawObjs.size(); i++) {
                drawObjs.get(i).setFillColorOstdot(dots[i]);
            }
            copies.add(copy);
        }
        return copies;
    }

    /**
     * Minimal V1 chooser for the two cycle parameters the recipe needs:
     * hueSet (0..24, indexes {@link AnchorHueSets}) and frozenSet (0..5,
     * indexes {@link FrozenSets}). Returns {hueSet, frozenSet}, or null on
     * cancel.
     */
    private int[] chooseRunParameters() {
        Integer[] hueSetOptions = new Integer[AnchorHueSets.HUE_SET_COUNT];
        for (int i = 0; i < hueSetOptions.length; i++) {
            hueSetOptions[i] = i;
        }
        JComboBox<Integer> hueSetBox = new JComboBox<>(hueSetOptions);
        hueSetBox.setSelectedIndex(1); // legacy generated sets start at row 1

        Integer[] frozenSetOptions = new Integer[FrozenSets.FROZEN_SET_COUNT];
        for (int i = 0; i < frozenSetOptions.length; i++) {
            frozenSetOptions[i] = i;
        }
        JComboBox<Integer> frozenSetBox = new JComboBox<>(frozenSetOptions);
        frozenSetBox.setSelectedIndex(0);

        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.add(new JLabel("hueSet (0.." + (AnchorHueSets.HUE_SET_COUNT - 1) + "):"));
        panel.add(hueSetBox);
        panel.add(new JLabel("frozenSet (0.." + (FrozenSets.FROZEN_SET_COUNT - 1) + "):"));
        panel.add(frozenSetBox);

        int choice = JOptionPane.showConfirmDialog(frame, panel,
                "Variation run parameters", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }
        return new int[]{(Integer) hueSetBox.getSelectedItem(),
                (Integer) frozenSetBox.getSelectedItem()};
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
