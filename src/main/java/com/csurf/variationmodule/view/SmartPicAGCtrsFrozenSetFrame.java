package com.csurf.variationmodule.view;

import com.csurf.variationmodule.model.SmartDrawObj;
import com.csurf.variationmodule.model.SmartPic;
import com.csurf.variationmodule.varyengine.model.AGHuePattern;
import com.csurf.variationmodule.varyengine.model.AGHuePatterns;
import com.csurf.variationmodule.varyengine.model.FrozenSets;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Set;
import java.util.TreeSet;

/**
 * Read-only diagnostic window for the variation-run control parameters.
 *
 * <p>The controlsPanel mirrors the run-parameters dialog: the user picks
 * an agHuePattern (a canonical {@link AGHuePatterns} name) and a frozenSet
 * (indexes {@link FrozenSets}); each selection immediately refreshes its
 * info panel.
 *
 * <ul>
 *   <li>hueSetInfoPanel — the four AG anchor hues of the chosen pattern,
 *       numbered by AG (AG1 → hue, … AG4 → hue). Catalog data.
 *   <li>frozenSetInfoPanel — the shape numbers and SmartPic names of the
 *       chosen frozen set, in table order. Names come from the SmartPic.
 *       When a user-selected set exists it appears as a seventh
 *       "user (N shapes)" combo entry, displayed in numeric order.
 * </ul>
 *
 * <p>View only — the controller supplies the SmartPic and the initial
 * selections (the last run's parameters, or the dialog defaults).
 */
public class SmartPicAGCtrsFrozenSetFrame extends JFrame {

    private final SmartPic smartPic;
    private final JComboBox<String> hueSetBox;
    private final JComboBox<Object> frozenSetBox;
    private final Set<Integer> userFrozenShapes;
    private final JTextArea hueInfoArea;
    private final JTextArea frozenInfoArea;
    private final JPanel hueSetInfoPanel;
    private final JPanel frozenSetInfoPanel;

    /**
     * @param smartPic      the last created SmartPic (provides shape names)
     * @param agHuePattern  initial agHuePattern selection (canonical name)
     * @param frozenSet     initial frozenSet selection (0..5)
     */
    public SmartPicAGCtrsFrozenSetFrame(SmartPic smartPic, String agHuePattern, int frozenSet) {
        this(smartPic, agHuePattern, frozenSet, null);
    } // end constructor

    /**
     * @param smartPic         the last created SmartPic (provides shape names)
     * @param agHuePattern     initial agHuePattern selection (canonical name)
     * @param frozenSet        initial frozenSet selection (0..5), or a negative
     *                         sentinel to select the user-set entry initially
     * @param userFrozenShapes the user-selected frozen set for this SmartPic,
     *                         or null when none exists
     */
    public SmartPicAGCtrsFrozenSetFrame(SmartPic smartPic, String agHuePattern,
                                        int frozenSet, Set<Integer> userFrozenShapes) {
        super("Hue anchors / frozen");
        this.smartPic = smartPic;
        this.userFrozenShapes = userFrozenShapes;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        hueSetBox = new JComboBox<>(AGHuePatterns.allNames().toArray(new String[0]));
        frozenSetBox = new JComboBox<>(frozenSetItems(userFrozenShapes));

        JPanel controlsPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        controlsPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        controlsPanel.add(new JLabel("agHuePattern (" + AGHuePatterns.count() + "):"));
        controlsPanel.add(hueSetBox);
        controlsPanel.add(new JLabel("frozenSet (0.." + (FrozenSets.FROZEN_SET_COUNT - 1)
                + (userFrozenShapes != null ? " or user" : "") + "):"));
        controlsPanel.add(frozenSetBox);

        hueInfoArea = newInfoArea();
        hueInfoArea.setRows(4); // 5 rows would give 4 AG lines + one blank row
        frozenInfoArea = newInfoArea();
        hueSetInfoPanel = infoPanel(hueInfoArea);
        frozenSetInfoPanel = infoPanel(frozenInfoArea);

        hueSetBox.addActionListener(e -> updateHueInfo());
        frozenSetBox.addActionListener(e -> updateFrozenInfo());

        JPanel infoPanels = new JPanel(new BorderLayout(0, 6));
        infoPanels.setBorder(new EmptyBorder(0, 8, 8, 8));
        infoPanels.add(hueSetInfoPanel, BorderLayout.NORTH);   // preferred height only
        infoPanels.add(frozenSetInfoPanel, BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(controlsPanel, BorderLayout.NORTH);
        getContentPane().add(infoPanels, BorderLayout.CENTER);

        hueSetBox.setSelectedItem(agHuePattern);
        if (frozenSet >= 0) {
            frozenSetBox.setSelectedIndex(frozenSet);
        } else if (userFrozenShapes != null) {
            frozenSetBox.setSelectedIndex(frozenSetBox.getItemCount() - 1); // the "user" entry
        } else {
            frozenSetBox.setSelectedIndex(0);
        } // end IF-ELSE
        // populate explicitly — a combo may not fire if its index is unchanged
        updateHueInfo();
        updateFrozenInfo();

        setPreferredSize(new Dimension(320, 480));
        pack();
    }

    // ---------- info refresh ----------

    private void updateHueInfo() {
        String name = (String) hueSetBox.getSelectedItem();
        AGHuePattern pattern = AGHuePatterns.get(name);
        ((TitledBorder) hueSetInfoPanel.getBorder()).setTitle("agHuePattern " + name);
        StringBuilder text = new StringBuilder();
        for (int ag = 1; ag <= 4; ag++) {
            text.append(String.format("AG%d → %d%n", ag, pattern.hueNumForAG(ag)));
        } // end FOR
        hueInfoArea.setText(text.toString());
        hueSetInfoPanel.repaint();
    }

    private void updateFrozenInfo() {
        Object selection = frozenSetBox.getSelectedItem();
        Set<Integer> shapes;
        if (selection instanceof Integer) {
            int setIndex = (Integer) selection;
            ((TitledBorder) frozenSetInfoPanel.getBorder()).setTitle("frozenSet " + setIndex);
            shapes = FrozenSets.get(setIndex);
        } else {
            ((TitledBorder) frozenSetInfoPanel.getBorder()).setTitle(
                    "user frozen set (" + userFrozenShapes.size() + ")");
            shapes = new TreeSet<>(userFrozenShapes); // numeric order — a user set has no table order
        } // end IF-ELSE
        StringBuilder text = new StringBuilder();
        for (int shape : shapes) {
            SmartDrawObj sdo = smartPic.smartDrawObjs().get(shape);
            String name = sdo != null ? sdo.name() : "(no shape " + shape + ")";
            text.append(String.format("%2d  %s%n", shape, name));
        } // end FOR
        frozenInfoArea.setText(text.toString());
        frozenSetInfoPanel.repaint();
    }

    // ---------- helpers ----------

    /** Combo items: table indices 0..5 plus a "user (N shapes)" entry when a user set exists. */
    private static Object[] frozenSetItems(Set<Integer> userFrozenShapes) {
        int count = FrozenSets.FROZEN_SET_COUNT;
        Object[] items = new Object[count + (userFrozenShapes != null ? 1 : 0)];
        for (int i = 0; i < count; i++) {
            items[i] = i;
        } // end FOR
        if (userFrozenShapes != null) {
            items[items.length - 1] = "user (" + userFrozenShapes.size() + " shapes)";
        } // end IF
        return items;
    } // end method frozenSetItems

    private static JTextArea newInfoArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBorder(new EmptyBorder(4, 8, 4, 8));
        return area;
    }

    private static JPanel infoPanel(JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(""));
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

} // end class SmartPicAGCtrsFrozenSetFrame
