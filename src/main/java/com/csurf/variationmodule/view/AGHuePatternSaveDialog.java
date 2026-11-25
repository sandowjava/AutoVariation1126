package com.csurf.variationmodule.view;

import com.csurf.variationmodule.varyengine.model.AGHuePattern;
import com.csurf.variationmodule.varyengine.model.AGHuePatterns;
import com.csurf.variationmodule.varyengine.model.AnchorHueSet;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Minimal "pick 4 hues + save" dialog for authoring an
 * {@link AGHuePattern}: four AG-order hue spinners (1–36), an optional
 * descriptive suffix, and a live preview of the canonical name that the
 * catalog will key on.
 *
 * <p>{@link #showDialog} returns a {@link SaveResult} carrying the built
 * pattern plus the optional description — the caller owns the
 * overwrite-confirm policy and the actual {@code AGHuePatterns} save.
 * Used by {@code CreateSmartPicController} (Phase 2) and reused by
 * {@code CreateRMatrixController} with a pre-filled {@link AnchorHueSet}
 * (Phase 3).
 */
public final class AGHuePatternSaveDialog {

    /** The user's choices: the pattern plus an optional description. */
    public record SaveResult(AGHuePattern pattern, String description) {
    } // end record SaveResult

    private AGHuePatternSaveDialog() {
    } // end private constructor — static dialog only

    /**
     * Shows the modal save dialog.
     *
     * @param owner           parent component for centering, may be null
     * @param prefill         hues to pre-fill the spinners with, or null
     *                        for the row-18 defaults
     * @param suggestedSuffix initial suffix text (e.g. a Template-derived
     *                        phrase), or null/blank
     * @return the chosen {@link SaveResult}, or null on cancel
     */
    public static SaveResult showDialog(Component owner, AnchorHueSet prefill,
                                        String suggestedSuffix) {
        int[] hues = new int[]{13, 10, 6, 8}; // legacy row-18 defaults
        if (prefill != null) {
            for (int ag = 1; ag <= 4; ag++) {
                hues[ag - 1] = prefill.hueNumForAG(ag);
            } // end FOR
        } // end IF

        JSpinner[] spinners = new JSpinner[4];
        JPanel huesPanel = new JPanel(new GridLayout(2, 4, 6, 4));
        for (int i = 0; i < 4; i++) {
            huesPanel.add(new JLabel("AG" + (i + 1), SwingConstants.CENTER));
        } // end FOR
        for (int i = 0; i < 4; i++) {
            spinners[i] = new JSpinner(new SpinnerNumberModel(hues[i], 1, 36, 1));
            huesPanel.add(spinners[i]);
        } // end FOR

        JTextField suffixField = new JTextField(
                suggestedSuffix != null ? suggestedSuffix : "", 16);
        JTextField descriptionField = new JTextField(16);

        JLabel previewLabel = new JLabel(" ");
        previewLabel.setFont(previewLabel.getFont().deriveFont(Font.BOLD));

        // live canonical-name preview from spinners + suffix
        Runnable refresh = () -> {
            try {
                String name = AGHuePatterns.canonicalName(
                        spinnerHue(spinners[0]), spinnerHue(spinners[1]),
                        spinnerHue(spinners[2]), spinnerHue(spinners[3]),
                        suffixField.getText());
                previewLabel.setText(name);
            } catch (RuntimeException e) {
                previewLabel.setText("(invalid hues)");
            } // end try
        };
        for (JSpinner spinner : spinners) {
            spinner.addChangeListener(e -> refresh.run());
        } // end FOR
        suffixField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh.run(); }
            public void removeUpdate(DocumentEvent e) { refresh.run(); }
            public void changedUpdate(DocumentEvent e) { refresh.run(); }
        });
        refresh.run();

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.gridx = 0;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.WEST;
        form.add(new JLabel("Anchor hues (AG1–AG4):"), gc);
        gc.gridx = 1;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(huesPanel, gc);
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.gridy++;
        gc.gridx = 0;
        form.add(new JLabel("Suffix:"), gc);
        gc.gridx = 1;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(suffixField, gc);
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.gridy++;
        gc.gridx = 0;
        form.add(new JLabel("Description:"), gc);
        gc.gridx = 1;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(descriptionField, gc);
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.gridy++;
        gc.gridx = 0;
        form.add(new JLabel("Name:"), gc);
        gc.gridx = 1;
        gc.gridwidth = 2;
        form.add(previewLabel, gc);

        int choice = JOptionPane.showConfirmDialog(owner, form,
                "Save new AGHuePattern", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        } // end IF

        String name = AGHuePatterns.canonicalName(
                spinnerHue(spinners[0]), spinnerHue(spinners[1]),
                spinnerHue(spinners[2]), spinnerHue(spinners[3]),
                suffixField.getText());
        AGHuePattern pattern = new AGHuePattern(name,
                spinnerHue(spinners[0]), spinnerHue(spinners[1]),
                spinnerHue(spinners[2]), spinnerHue(spinners[3]));
        String description = descriptionField.getText();
        if (description != null && description.isBlank()) {
            description = null;
        } // end IF
        return new SaveResult(pattern, description);
    } // end method showDialog

    private static int spinnerHue(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    } // end method spinnerHue

    /**
     * The save-side collision policy shared by all save flows: when the
     * {@link SaveResult}'s name already exists in the catalog, the usual
     * overwrite-confirm is shown (with an extra note for built-in names,
     * whose overwrite applies only to this session — the load rule skips
     * the file at the next launch and restores the built-in). On confirm,
     * the pattern is registered and written via
     * {@link AGHuePatterns#saveUserPattern}.
     *
     * @return true when the pattern was saved; false on user cancel
     * @throws IllegalStateException if the file cannot be written
     */
    public static boolean saveWithConfirm(Component owner, SaveResult result) {
        String name = result.pattern().getName();
        if (AGHuePatterns.exists(name)) {
            String message = "AGHuePattern '" + name + "' already exists. Overwrite?";
            if (AGHuePatterns.isBuiltIn(name)) {
                message += "\n(It is a built-in name — the overwrite applies only\n"
                        + "to this session; the built-in returns on next launch.)";
            } // end IF
            int overwrite = JOptionPane.showConfirmDialog(owner, message,
                    "Save AGHuePattern", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.OK_OPTION) {
                return false;
            } // end IF
        } // end IF
        AGHuePatterns.saveUserPattern(result.pattern(), result.description());
        return true;
    } // end method saveWithConfirm

} // end class AGHuePatternSaveDialog
