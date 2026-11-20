package com.csurf.variationmodule.view;

import com.csurf.variationmodule.model.SmartDrawObj;
import com.csurf.variationmodule.model.SmartPic;
import com.csurf.variationmodule.varyengine.model.AnchorHueSet;
import com.csurf.variationmodule.varyengine.model.AnchorHueSets;
import com.csurf.variationmodule.varyengine.model.FrozenSets;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Read-only diagnostic window for the variation-run control parameters.
 *
 * <p>The controlsPanel mirrors the run-parameters dialog: the user picks a
 * hueSet (indexes {@link AnchorHueSets}) and a frozenSet (indexes
 * {@link FrozenSets}); each selection immediately refreshes its info panel.
 *
 * <ul>
 *   <li>hueSetInfoPanel — the four AG anchor hues of the chosen set,
 *       numbered by AG (AG1 → hue, … AG4 → hue). Static table data.
 *   <li>frozenSetInfoPanel — the shape numbers and SmartPic names of the
 *       chosen frozen set, in table order. Names come from the SmartPic.
 * </ul>
 *
 * <p>View only — the controller supplies the SmartPic and the initial
 * selections (the last run's parameters, or the dialog defaults).
 */
public class SmartPicAGCtrsFrozenSetFrame extends JFrame {

    private final SmartPic smartPic;
    private final JComboBox<Integer> hueSetBox;
    private final JComboBox<Integer> frozenSetBox;
    private final JTextArea hueInfoArea;
    private final JTextArea frozenInfoArea;
    private final JPanel hueSetInfoPanel;
    private final JPanel frozenSetInfoPanel;

    /**
     * @param smartPic   the last created SmartPic (provides shape names)
     * @param hueSet     initial hueSet selection (0..24)
     * @param frozenSet  initial frozenSet selection (0..5)
     */
    public SmartPicAGCtrsFrozenSetFrame(SmartPic smartPic, int hueSet, int frozenSet) {
        super("Hue anchors / frozen");
        this.smartPic = smartPic;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        hueSetBox = new JComboBox<>(range(AnchorHueSets.HUE_SET_COUNT));
        frozenSetBox = new JComboBox<>(range(FrozenSets.FROZEN_SET_COUNT));

        JPanel controlsPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        controlsPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        controlsPanel.add(new JLabel("hueSet (0.." + (AnchorHueSets.HUE_SET_COUNT - 1) + "):"));
        controlsPanel.add(hueSetBox);
        controlsPanel.add(new JLabel("frozenSet (0.." + (FrozenSets.FROZEN_SET_COUNT - 1) + "):"));
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

        hueSetBox.setSelectedIndex(hueSet);
        frozenSetBox.setSelectedIndex(frozenSet);
        // populate explicitly — a combo may not fire if its index is unchanged
        updateHueInfo();
        updateFrozenInfo();

        setPreferredSize(new Dimension(320, 480));
        pack();
    }

    // ---------- info refresh ----------

    private void updateHueInfo() {
        int setIndex = (Integer) hueSetBox.getSelectedItem();
        AnchorHueSet set = AnchorHueSets.get(setIndex);
        ((TitledBorder) hueSetInfoPanel.getBorder()).setTitle("hueSet " + setIndex);
        StringBuilder text = new StringBuilder();
        for (int ag = 1; ag <= 4; ag++) {
            text.append(String.format("AG%d → %d%n", ag, set.hueNumForAG(ag)));
        } // end FOR
        hueInfoArea.setText(text.toString());
        hueSetInfoPanel.repaint();
    }

    private void updateFrozenInfo() {
        int setIndex = (Integer) frozenSetBox.getSelectedItem();
        ((TitledBorder) frozenSetInfoPanel.getBorder()).setTitle("frozenSet " + setIndex);
        StringBuilder text = new StringBuilder();
        for (int shape : FrozenSets.get(setIndex)) {
            SmartDrawObj sdo = smartPic.smartDrawObjs().get(shape);
            String name = sdo != null ? sdo.name() : "(no shape " + shape + ")";
            text.append(String.format("%2d  %s%n", shape, name));
        } // end FOR
        frozenInfoArea.setText(text.toString());
        frozenSetInfoPanel.repaint();
    }

    // ---------- helpers ----------

    private static Integer[] range(int count) {
        Integer[] items = new Integer[count];
        for (int i = 0; i < count; i++) {
            items[i] = i;
        } // end FOR
        return items;
    }

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
