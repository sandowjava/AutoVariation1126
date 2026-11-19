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
    }

    private final JLabel templateLabel;
    private final JLabel agPrefsLabel;
    private final JLabel statusLabel;

    /**
     * @param actions menu-action callbacks (the controller)
     * @param title   window title
     */
    public CreateSmartPicFrame(Actions actions, String title) {
        super(title);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JMenu templateMenu = new JMenu("Template");
        JMenuItem chooseTemplateItem = new JMenuItem("Choose saved Template...");
        chooseTemplateItem.addActionListener(e -> actions.chooseTemplate());
        templateMenu.add(chooseTemplateItem);

        JMenu agPrefsMenu = new JMenu("Shape-AGprefs");
        JMenuItem chooseAgPrefsItem = new JMenuItem("Choose AGprefs defaults file...");
        chooseAgPrefsItem.addActionListener(e -> actions.chooseAgPrefsFile());
        agPrefsMenu.add(chooseAgPrefsItem);

        JMenu smartPicMenu = new JMenu("SmartPic");
        JMenuItem createItem = new JMenuItem("Create SmartPic");
        createItem.addActionListener(e -> actions.createSmartPic());
        smartPicMenu.add(createItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(templateMenu);
        menuBar.add(agPrefsMenu);
        menuBar.add(smartPicMenu);
        setJMenuBar(menuBar);

        JPanel content = new JPanel(new GridLayout(3, 1, 0, 6));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        templateLabel = new JLabel("Template: (none chosen)");
        agPrefsLabel = new JLabel("AGprefs defaults: (none chosen)");
        statusLabel = new JLabel(" ");
        content.add(templateLabel);
        content.add(agPrefsLabel);
        content.add(statusLabel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(content, BorderLayout.CENTER);

        setPreferredSize(new Dimension(420, 150));
        pack();
        setResizable(false);
    }

    /** Displays the chosen Template's display name. */
    public void setTemplateName(String name) {
        templateLabel.setText("Template: " + name);
    }

    /** Displays the chosen AGprefs defaults file's label. */
    public void setAgPrefsName(String name) {
        agPrefsLabel.setText("AGprefs defaults: " + name);
    }

    /** Displays a status line, e.g. the create result. */
    public void setStatus(String status) {
        statusLabel.setText(status);
    }
} // end class CreateSmartPicFrame
