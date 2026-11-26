package com.csurf.variationmodule.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Window for creating a SmartPic: the user picks a saved Template and an
 * AGprefs defaults file via menu items, then creates the SmartPic.
 *
 * <p>View only — menu actions delegate to the controller.
 */
public class CreateSmartPicFrame extends JFrame {

    /**
     * Screen position where this window opens: upper-left of the screen,
     * just below the ApplicFrame window. Edit this to move the window.
     */
    public static final Point OPEN_LOCATION = new Point(0, 140);

    /**
     * Callbacks the frame's menu items invoke; implemented by the controller.
     */
    public interface Actions {
        /** User chose "Choose saved Template...". */
        void chooseTemplate();
        /** User chose "Choose AGprefs defaults file...". */
        void chooseAgPrefsFile();
        /** User chose "Create SmartPic". */
        void createSmartPic();
        /** User chose "Create 12 Variations". */
        void createVariations();
        /** User chose "Template drawObj list" (Controls-info menu). */
        void showTemplateDrawObjList();
        /** User chose "Hue anchors/ frozen" (Controls-info menu). */
        void showHueAnchorsFrozen();
        /** User chose "Save new AGHuePattern..." (Patterns menu). */
        void saveAGHuePattern();
        /** User chose "Help -open help doc" (Controls-info menu). */
        void showHelpDoc();

        void showUserSelectFrozenFrame();
    } // end interface Actions

    private final JLabel templateLabel;
    private final JLabel relationMatrixLabel;
    private final JLabel statusLabel;
    private final JMenuItem drawObjListItem;
    private final JMenuItem hueAnchorsItem;
    private final JMenuItem userSelectFrozenItem;

    /**
     * @param actions menu-action callbacks (the controller)
     * @param title   window title
     */
    public CreateSmartPicFrame(Actions actions, String title) {
        super(title);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JMenu templateMenu = new JMenu("1)Template");
        JMenuItem chooseTemplateItem = new JMenuItem("Choose saved Template...");
        chooseTemplateItem.addActionListener(e -> actions.chooseTemplate());
        templateMenu.add(chooseTemplateItem);

        JMenu relationMatrixMenu = new JMenu("2)RMatrix");
        JMenuItem chooseAgPrefsItem = new JMenuItem("Choose AGprefs file (RMatrix)");
        chooseAgPrefsItem.addActionListener(e -> actions.chooseAgPrefsFile());
        relationMatrixMenu.add(chooseAgPrefsItem);

        JMenu smartPicMenu = new JMenu("3)SmartPic");
        JMenuItem createItem = new JMenuItem("Create SmartPic");
        createItem.addActionListener(e -> actions.createSmartPic());
        smartPicMenu.add(createItem);

        smartPicMenu.addSeparator();

        JMenuItem createVariationsItem = new JMenuItem("Create 12 Variations");
        createVariationsItem.addActionListener(e -> actions.createVariations());
        smartPicMenu.add(createVariationsItem);

        JMenu controlsInfoMenu = new JMenu("Controls-info");

        userSelectFrozenItem = new JMenuItem("Select Frozen drawObjs");
        userSelectFrozenItem.setEnabled(false); // enabled once a SmartPic is created
        userSelectFrozenItem.addActionListener(e -> actions.showUserSelectFrozenFrame());
        controlsInfoMenu.add(userSelectFrozenItem);

        drawObjListItem = new JMenuItem("Template drawObj list");
        drawObjListItem.setEnabled(false); // enabled once a Template is chosen
        drawObjListItem.addActionListener(e -> actions.showTemplateDrawObjList());
        controlsInfoMenu.add(drawObjListItem);

        JMenuItem saveAGHuePatternItem = new JMenuItem("Save AGHuePattern...");
        saveAGHuePatternItem.addActionListener(e -> actions.saveAGHuePattern());
        controlsInfoMenu.add(saveAGHuePatternItem);

        hueAnchorsItem = new JMenuItem("Hue anchors/ frozen");
        hueAnchorsItem.setEnabled(false); // enabled once a SmartPic is created
        hueAnchorsItem.addActionListener(e -> actions.showHueAnchorsFrozen());
        controlsInfoMenu.add(hueAnchorsItem);

        controlsInfoMenu.addSeparator();

        JMenuItem helpDocItem = new JMenuItem("Help -open help doc");
        helpDocItem.addActionListener(e -> actions.showHelpDoc());
        controlsInfoMenu.add(helpDocItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(templateMenu);
        menuBar.add(relationMatrixMenu);
        menuBar.add(smartPicMenu);
        menuBar.add(controlsInfoMenu);
        setJMenuBar(menuBar);

        JPanel content = new JPanel(new GridLayout(3, 1, 0, 6));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        templateLabel = new JLabel("Template for shapes: (none chosen)");
        relationMatrixLabel = new JLabel("RMatrix for AGprefs: (none chosen)");
        statusLabel = new JLabel(" ");
        content.add(templateLabel);
        content.add(relationMatrixLabel);
        content.add(statusLabel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(content, BorderLayout.CENTER);

        setPreferredSize(new Dimension(420, 150));
        pack();
        setResizable(false);
    }

    /** Displays the chosen Template's display name. */
    public void setTemplateName(String name) {
        templateLabel.setText("Template for shapes: " + name);
    }

    /** Enables/disables the "Template drawObj list" menu item. */
    public void setDrawObjListEnabled(boolean enabled) {
        drawObjListItem.setEnabled(enabled);
    }

    /** Enables/disables the "Hue anchors/ frozen" menu item. */
    public void setHueAnchorsEnabled(boolean enabled) {
        hueAnchorsItem.setEnabled(enabled);
    }

    /** Enables/disables the "Select Frozen drawObjs" menu item. */
    public void setUserSelectFrozenEnabled(boolean enabled) {
        userSelectFrozenItem.setEnabled(enabled);
    }

    /** Displays the chosen AGprefs defaults file's label. */
    public void setAgPrefsName(String name) {
        relationMatrixLabel.setText("RMatrix for AGprefs: " + name);
    }

    /** Displays a status line, e.g. the create result. */
    public void setStatus(String status) {
        statusLabel.setText(status);
    }
} // end class CreateSmartPicFrame
