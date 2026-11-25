package com.csurf.variationmodule.view;

import com.csurf.ostwald.domainmonotri.model.MonotriBook;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Window for creating a tailored RelationMatrix from a saved Template:
 * the user picks a Template with exactly four distinct hues in use, assigns
 * each detected hue to AG1–AG4 (bijection enforced), confirms, then saves
 * the generated AGprefs defaults file.
 *
 * <p>View only — menu/button actions delegate to the controller via
 * {@link Actions}. Design doc:
 * {@code 112326_v1H_SWE-2_create_RMatrix_from_Template.md}.
 */
public class CreateRMatrixFrame extends JFrame {

    /**
     * Screen position where this window opens: upper-left of the screen,
     * just below the ApplicFrame window. Edit this to move the window.
     */
    public static final Point OPEN_LOCATION = new Point(0, 140);

    /** Guidance text shown verbatim above the assignment rows. */
    private static final String GUIDANCE_TEXT =
            "Choose hue for AG1 as central to the graphic-design focus. "
            + "Choose hue for AG2 as hue which is closest to being "
            + "complementary to AG1. NOW choose hue for AG3 (from the "
            + "remaining 2 hues) as the hue which is closest to AG1 "
            + "(analogous considering warm/cool ranges). The last remaining "
            + "hue will be AG4, and is likely to be analogous to AG2.";

    /** Selector placeholder: hue not yet assigned to an AG. */
    private static final String UNASSIGNED = "--";

    /**
     * Callbacks the frame's menu items and buttons invoke; implemented by
     * the controller.
     */
    public interface Actions {
        /** User chose "Choose saved Template...". */
        void chooseTemplate();
        /** User pressed "Confirm anchor hues". */
        void confirmAnchorHues();
        /** User chose "Create RMatrix file...". */
        void createRMatrix();
        /** User chose "Save anchor hues as AGHuePattern..." (Patterns menu). */
        void saveAnchorHuesAsPattern();
        /** User changed a hue→AG selector (resets the confirmed assignment). */
        void assignmentChanged();
    }

    private final JLabel templateLabel;
    private final JLabel statusLabel;
    private final JMenuItem createItem;
    private final JMenuItem savePatternItem;
    private final JButton confirmButton;
    private final JPanel assignRowsPanel;
    private final JPanel assignPanel;
    private final JTextArea guidanceArea;

    private final List<JComboBox<String>> agSelectors = new ArrayList<>();
    private final List<Integer> shownHues = new ArrayList<>();

    /**
     * @param actions menu/button callbacks (the controller)
     * @param title   window title
     */
    public CreateRMatrixFrame(Actions actions, String title) {
        super(title);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Menus
        JMenu templateMenu = new JMenu("Template");
        JMenuItem chooseTemplateItem = new JMenuItem("Choose saved Template...");
        chooseTemplateItem.addActionListener(e -> actions.chooseTemplate());
        templateMenu.add(chooseTemplateItem);

        JMenu rMatrixMenu = new JMenu("RMatrix");
        createItem = new JMenuItem("Create RMatrix file...");
        createItem.setEnabled(false); // enabled once anchor hues are confirmed
        createItem.addActionListener(e -> actions.createRMatrix());
        rMatrixMenu.add(createItem);

        JMenu patternsMenu = new JMenu("Patterns");
        savePatternItem = new JMenuItem("Save anchor hues as AGHuePattern...");
        savePatternItem.setEnabled(false); // enabled once anchor hues are confirmed
        savePatternItem.addActionListener(e -> actions.saveAnchorHuesAsPattern());
        patternsMenu.add(savePatternItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(templateMenu);
        menuBar.add(rMatrixMenu);
        menuBar.add(patternsMenu);
        setJMenuBar(menuBar);

        // Top: template + status lines
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        headerPanel.setBorder(new EmptyBorder(10, 12, 6, 12));
        templateLabel = new JLabel("Template: (none chosen)");
        statusLabel = new JLabel(" ");
        headerPanel.add(templateLabel);
        headerPanel.add(statusLabel);

        // Center: guidance text + hue→AG assignment rows + confirm button
        assignPanel = new JPanel(new BorderLayout(0, 4));
        assignPanel.setBorder(new TitledBorder("Assign the four hues to anchor groups"));

        // Non-editable, word-wrapping text area: wraps to the window width
        // instead of a fixed pixel width, so the guidance text cannot be
        // clipped on the right.
        guidanceArea = new JTextArea(GUIDANCE_TEXT);
        guidanceArea.setFont(guidanceArea.getFont().deriveFont(10f));
        guidanceArea.setLineWrap(true);
        guidanceArea.setWrapStyleWord(true);
        guidanceArea.setEditable(false);
        guidanceArea.setFocusable(false);
        guidanceArea.setOpaque(false);
        guidanceArea.setBorder(new EmptyBorder(0, 4, 4, 4));
        assignPanel.add(guidanceArea, BorderLayout.NORTH);

        assignRowsPanel = new JPanel(new GridLayout(0, 1, 0, 5));
        assignRowsPanel.setBorder(new EmptyBorder(0, 8, 0, 8));
        assignPanel.add(assignRowsPanel, BorderLayout.CENTER);

        confirmButton = new JButton("Confirm anchor hues");
        confirmButton.setEnabled(false);
        confirmButton.addActionListener(e -> actions.confirmAnchorHues());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(confirmButton);
        assignPanel.add(buttonPanel, BorderLayout.SOUTH);

        assignPanel.setVisible(false); // shown once a valid Template is chosen

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(assignPanel, BorderLayout.CENTER);

        // Fixed size large enough for the full widget: the assignment panel is
        // hidden when pack() runs, so pack() alone would collapse the window
        // to just the header strip.
        setPreferredSize(new Dimension(480, 300));
        pack();
        setResizable(false);
    }

    /** Displays the chosen Template's display name. */
    public void setTemplateName(String name) {
        templateLabel.setText("Template: " + name);
    }

    /** Displays a status line, e.g. the create result. */
    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * Builds the four assignment rows for the detected hues. Each row shows
     * a swatch of the hue (pure-hue color) + the hueNum, and an AG1–AG4
     * selector. Assignment is a move: selecting an AG already shown on
     * another row clears that row (bijection enforcement).
     *
     * @param hues          the four distinct occupied user-facing hues
     * @param onAnyChange   invoked on every selector change
     */
    public void showHueAssignment(Set<Integer> hues, Runnable onAnyChange) {
        assignRowsPanel.removeAll();
        agSelectors.clear();
        shownHues.clear();

        String[] agChoices = {UNASSIGNED, "AG1", "AG2", "AG3", "AG4"};
        for (int hue : hues) {
            shownHues.add(hue);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

            JPanel swatch = new JPanel();
            swatch.setPreferredSize(new Dimension(32, 22));
            swatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            swatch.setBackground(MonotriBook.getInstance()
                    .getPureHueColorByCodeHueNum(hue - 1).toAWTColor());
            row.add(swatch);

            row.add(new JLabel("hue " + hue + "  →"));

            JComboBox<String> selector = new JComboBox<>(agChoices);
            selector.addActionListener(e -> {
                String chosen = (String) selector.getSelectedItem();
                if (chosen != null && !UNASSIGNED.equals(chosen)) {
                    // move semantics: clear the AG from any other row holding it
                    for (JComboBox<String> other : agSelectors) {
                        if (other != selector && chosen.equals(other.getSelectedItem())) {
                            other.setSelectedItem(UNASSIGNED);
                        }
                    }
                }
                confirmButton.setEnabled(isAssignmentComplete());
                onAnyChange.run();
            });
            agSelectors.add(selector);
            row.add(selector);
            assignRowsPanel.add(row);
        }

        confirmButton.setEnabled(false);
        assignPanel.setVisible(true);
        revalidate();
        repaint();
    }

    /** Hides the assignment panel (e.g. after a guard rejection). */
    public void clearHueAssignment() {
        assignRowsPanel.removeAll();
        agSelectors.clear();
        shownHues.clear();
        confirmButton.setEnabled(false);
        assignPanel.setVisible(false);
        revalidate();
        repaint();
    }

    /** Every row has a distinct AG selected — bijection over AG1–AG4. */
    private boolean isAssignmentComplete() {
        if (agSelectors.size() != 4) {
            return false;
        }
        Set<String> used = new LinkedHashSet<>();
        for (JComboBox<String> selector : agSelectors) {
            Object item = selector.getSelectedItem();
            if (item == null || UNASSIGNED.equals(item)) {
                return false;
            }
            used.add((String) item);
        }
        return used.size() == 4;
    }

    /**
     * The confirmed assignment as hueNums ordered by AG:
     * {@code result[ag - 1]} = the user-facing hue the user assigned to AG
     * {@code ag}. Valid only when {@link #isAssignmentComplete()}.
     *
     * @throws IllegalStateException if the assignment is incomplete
     */
    public int[] getAnchorAssignment() {
        if (!isAssignmentComplete()) {
            throw new IllegalStateException("hue→AG assignment is incomplete");
        }
        int[] byAG = new int[4];
        for (int i = 0; i < agSelectors.size(); i++) {
            String chosen = (String) agSelectors.get(i).getSelectedItem();
            int ag = Integer.parseInt(chosen.substring(2)); // "AGn" -> n
            byAG[ag - 1] = shownHues.get(i);
        }
        return byAG;
    }

    /** Enables/disables the "Create RMatrix file..." menu item. */
    public void setCreateEnabled(boolean enabled) {
        createItem.setEnabled(enabled);
    }

    /** Enables/disables the "Save anchor hues as AGHuePattern..." menu item. */
    public void setSavePatternEnabled(boolean enabled) {
        savePatternItem.setEnabled(enabled);
    }
} // end class CreateRMatrixFrame
