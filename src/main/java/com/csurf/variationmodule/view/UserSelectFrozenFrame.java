package com.csurf.variationmodule.view;

import com.csurf.templatecore.view.TemplateWorkFrame;
import com.csurf.variationmodule.controller.CreateSmartPicController;
import com.csurf.variationmodule.controller.UserSelectFrozenController;
import com.csurf.variationmodule.model.SmartPic;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * UserSelectFrozenFrame is a modeless JFrame that drives a one-shot
 * "Select Frozen drawObjs" session against a scratch TemplateWorkFrame
 * (a fresh copy of the SmartPic's Template, opened alongside).
 *
 * <p>Workflow: the user selects DrawObjs in the template window, returns
 * here, and commits once with "Use current selection as the frozen set".
 * The committed set replaces any previous user set on the
 * CreateSmartPicController and becomes the seventh "user (N shapes)"
 * entry in the next run-parameters dialog. An empty selection commits an
 * empty set (freeze nothing) after a warning confirm.
 *
 * <p>One-shot (v1): a successful commit disposes this frame. The scratch
 * TemplateWorkFrame stays open — it is the user's window — but its
 * selection is no longer consulted; reopen the menu item for a new
 * selection session.
 */
public class UserSelectFrozenFrame extends JFrame {

    private static final int FRAME_WIDTH = 360;
    private static final int FRAME_HEIGHT = 220;

    private final UserSelectFrozenController controller;
    private JLabel statusLabel;

    /**
     * @param owner     the CreateSmartPicController that receives the committed set
     * @param smartPic  the current SmartPic (validation target)
     * @param workFrame the scratch TemplateWorkFrame hosting the selection
     */
    public UserSelectFrozenFrame(CreateSmartPicController owner, SmartPic smartPic,
                                 TemplateWorkFrame workFrame) {
        super("Select Frozen drawObjs");
        this.controller = new UserSelectFrozenController(owner, smartPic, workFrame);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setLocationRelativeTo(workFrame);

        initializeComponents();
        refreshStatus();
    } // end constructor

    /** The session controller for this frame. */
    public UserSelectFrozenController getController() {
        return controller;
    } // end method getController

    // ---------- UI ----------

    private void initializeComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel instructionsLabel = new JLabel(
                "<html>Select DrawObjs in the template window,<br/>" +
                "then commit once. The committed set replaces<br/>" +
                "any previous user set for this SmartPic.<br/>" +
                "Empty selection is allowed (freezes nothing).</html>");
        instructionsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(instructionsLabel);
        mainPanel.add(Box.createVerticalStrut(12));

        JButton commitButton = new JButton("Use current selection as the frozen set");
        commitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        commitButton.addActionListener(e -> handleCommit());
        mainPanel.add(commitButton);
        mainPanel.add(Box.createVerticalStrut(12));

        statusLabel = new JLabel();
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(statusLabel);

        getContentPane().add(mainPanel, BorderLayout.CENTER);

        // refresh the live count whenever focus returns from the scratch window
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                refreshStatus();
            }
        });
    } // end method initializeComponents

    // ---------- commit ----------

    private void handleCommit() {
        if (controller.selectedCount() == 0) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Commit an EMPTY frozen set — every shape will vary?\n" +
                            "(Select DrawObjs in the template window to freeze specific shapes.)",
                    "Confirm empty frozen set",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }
        } // end IF

        String error = controller.commit();
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Cannot commit",
                    JOptionPane.WARNING_MESSAGE);
            refreshStatus();
            return;
        } // end IF

        // one-shot: the set is installed on the owning controller — done here
        dispose();
    } // end method handleCommit

    private void refreshStatus() {
        statusLabel.setText("Selected: " + controller.selectedCount()
                + " of " + controller.totalShapes() + " shapes");
    } // end method refreshStatus

} // end class UserSelectFrozenFrame
