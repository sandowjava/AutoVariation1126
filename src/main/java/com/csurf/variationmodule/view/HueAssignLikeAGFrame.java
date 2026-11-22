package com.csurf.variationmodule.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.csurf.templatecore.view.TemplateWorkFrame;
import com.csurf.variationmodule.controller.HueAssignLikeAGController;

/**
 * HueAssignLikeAGFrame is a modeless JFrame that drives one "Go to N hues"
 * session for a TemplateWorkFrame.
 *
 * The frame stays open alongside TemplateWorkFrame so the user can keep using
 * all of the main window's selection machinery between commits. The frame
 * only handles designation: pick a hue number, then commit either the current
 * selection or the computed remainder of unassigned DrawObjs.
 *
 * Workflow:
 * - The session's anchorPatternLevel (N, 1-4) comes from the menu item that
 *   opened the frame.
 * - Normal commit: select DrawObjs in the main window, choose a hue, click
 *   "Apply to selection".
 * - Remainder commit: check "Assign all remaining DrawObjs" (auto-checked
 *   when N-1 hues are used), choose the last hue, click the button.
 * - Re-assignment: select already-grouped DrawObjs and commit them to an
 *   existing hue (last-write-wins).
 * - The frame remains open after the session completes for re-assignment.
 */
public class HueAssignLikeAGFrame extends JFrame {

    private static final int FRAME_WIDTH = 340;
    private static final int FRAME_HEIGHT = 260;

    private final HueAssignLikeAGController controller;
    private final TemplateWorkFrame templateWorkFrame;

    private JSpinner hueSpinner;
    private JCheckBox remainderCheckBox;
    private JButton commitButton;
    private JLabel statusLabel;
    private JLabel usedHuesLabel;

    /**
     * Creates the modeless hue-assignment frame for one session.
     *
     * @param anchorPatternLevel the number of hue groups for this session (1-4)
     * @param templateWorkFrame the main frame (source of roster, selection, hub)
     * @throws IllegalArgumentException if arguments are invalid
     */
    public HueAssignLikeAGFrame(int anchorPatternLevel, TemplateWorkFrame templateWorkFrame) {
        super("Assign DrawObjs to " + anchorPatternLevel +
              (anchorPatternLevel == 1 ? " hue" : " hues"));
        this.templateWorkFrame = templateWorkFrame;
        this.controller = new HueAssignLikeAGController(anchorPatternLevel, templateWorkFrame);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setLocationRelativeTo(templateWorkFrame);

        initializeComponents();
        refreshStatus();
    }

    /**
     * @return the session controller for this frame
     */
    public HueAssignLikeAGController getController() {
        return controller;
    }

    // ============================================================================
    // UI CONSTRUCTION
    // ============================================================================

    private void initializeComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // Instructions
        JLabel instructionsLabel = new JLabel(
            "<html>Select DrawObjs in the main window,<br/>" +
            "choose a hue, then commit.<br/>" +
            "Each hue may only be used once.</html>");
        instructionsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(instructionsLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Hue chooser
        JPanel huePanel = new JPanel();
        huePanel.setLayout(new BoxLayout(huePanel, BoxLayout.X_AXIS));
        huePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        huePanel.add(new JLabel("Hue number (1-36): "));
        hueSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 36, 1));
        hueSpinner.setMaximumSize(new Dimension(60, 24));
        huePanel.add(hueSpinner);
        mainPanel.add(huePanel);
        mainPanel.add(Box.createVerticalStrut(8));

        // Remainder checkbox
        remainderCheckBox = new JCheckBox("Assign all remaining unassigned DrawObjs");
        remainderCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(remainderCheckBox);
        mainPanel.add(Box.createVerticalStrut(8));

        // Commit button
        commitButton = new JButton("Apply to selection");
        commitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        commitButton.addActionListener(e -> handleCommit());
        mainPanel.add(commitButton);
        remainderCheckBox.addActionListener(e ->
            commitButton.setText(remainderCheckBox.isSelected()
                ? "Apply to remainder" : "Apply to selection"));
        mainPanel.add(Box.createVerticalStrut(12));

        // Status area
        statusLabel = new JLabel();
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(statusLabel);

        usedHuesLabel = new JLabel();
        usedHuesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(usedHuesLabel);

        getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

    // ============================================================================
    // COMMIT HANDLING
    // ============================================================================

    /**
     * Handles the commit button: validates the early-remainder case with a
     * confirm dialog, delegates to the controller, and refreshes the status.
     */
    private void handleCommit() {
        int hueNum = (Integer) hueSpinner.getValue();
        boolean useRemainder = remainderCheckBox.isSelected();

        // Remainder used before N-1 hues are assigned requires confirmation,
        // since it assigns every unassigned DrawObj in one step
        if (useRemainder && !controller.isRemainderExpected()
                && controller.getUsedHueCount() < controller.getAnchorPatternLevel()) {
            int choice = JOptionPane.showConfirmDialog(this,
                "This will assign every unassigned DrawObj to hue " + hueNum +
                ".\nContinue?",
                "Confirm remainder assignment",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }
        }

        String error = controller.commit(hueNum, useRemainder);
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Cannot apply",
                JOptionPane.WARNING_MESSAGE);
        }

        refreshStatus();
    }

    /**
     * Refreshes the status line, used-hues display, and the remainder
     * checkbox's auto-check behavior (pre-checked at N-1 hues assigned).
     */
    private void refreshStatus() {
        int used = controller.getUsedHueCount();
        int n = controller.getAnchorPatternLevel();

        if (controller.isSessionComplete()) {
            statusLabel.setText(used + " of " + n + " hues assigned - complete");
        } else {
            statusLabel.setText(used + " of " + n + " hues assigned");
        }

        if (controller.getUsedHueNums().isEmpty()) {
            usedHuesLabel.setText("Hues used: none");
        } else {
            String hues = controller.getUsedHueNums().stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
            usedHuesLabel.setText("Hues used: " + hues);
        }

        // Auto-check the remainder option once the user reaches N-1 hues;
        // the user can still uncheck it to select the last group manually
        remainderCheckBox.setSelected(controller.isRemainderExpected());
        commitButton.setText(remainderCheckBox.isSelected()
            ? "Apply to remainder" : "Apply to selection");
    }
}
