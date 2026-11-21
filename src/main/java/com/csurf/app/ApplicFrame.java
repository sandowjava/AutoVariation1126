package com.csurf.app;

import com.csurf.artintellig.view.CircMouserView;
import com.csurf.core.ApplicationMediator;
import com.csurf.core.Message;
import com.csurf.core.SubunitMediator;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame;
import com.csurf.templatecore.storage.config.StorageConfig;
import com.csurf.templatecore.svgconverter.view.SVGConverterFrame;
import com.csurf.templatecore.view.TemplateWorkFrame;
import com.csurf.variationmodule.controller.CreateSmartPicController;
import com.csurf.variationmodule.controller.RelationMatrixEditorController;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application window that provides access to all major functionality
 * through a menu-based interface.
 */
public class ApplicFrame extends JFrame {
    private final ApplicationMediator appMediator;
    private final JMenuBar menuBar;
    private final JMenu fileMenu;
    private final JMenu editMenu;
    private final JMenu viewMenu;
    private final JMenu importSVGMenu;
    private final JMenu templateMenu;
    private final JMenu windowMenu;
    private final JMenu helpMenu;
    private List<JFrame> openFrames;



    // constructor
    public ApplicFrame(ApplicationMediator appMediator) {
        super("AUTOVARIATION1126 Main Menu 600x200");
        this.appMediator = appMediator;
        this.openFrames = new ArrayList<>();
        
        // Initialize menu components
        this.menuBar = new JMenuBar();
        this.fileMenu = new JMenu("File");
        this.editMenu = new JMenu("Edit");
        this.viewMenu = new JMenu("View");
        this.importSVGMenu = new JMenu("SVG import");
        this.templateMenu = new JMenu("Templates");
        this.windowMenu = new JMenu("Windows open");
        this.helpMenu = new JMenu("Help");
        
        setupUI();
        setupMenus();
        
        // Register this frame to receive messages
        appMediator.registerSubunit("app", new SubunitMediator() {
            @Override
            public void initialize() {
                // No initialization needed
            }
            
            @Override
            public JComponent getVisualComponent() {
                // Return null since this mediator doesn't have its own visual component
                return null;
            }
            
            @Override
            public Object handleMessage(Message message) {
                if (message.getType().equals("REGISTER_FRAME") && message.getPayload() instanceof JFrame) {
                    registerFrame((JFrame) message.getPayload());
                    return true;
                }
                return null;
            }
        });
        
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    } // end constructor




    private void setupUI() {
        setPreferredSize(new Dimension(600, 200));
        setLayout(new BorderLayout());
        
        // Add menu bar
        setJMenuBar(menuBar);
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(importSVGMenu);
        menuBar.add(templateMenu);
        menuBar.add(windowMenu);
        menuBar.add(helpMenu);
    } // end method setupUI




    private void setupMenus() {
        // File menu items // 090525 took out
        // Edit menu items // 090525 took out

        // View menu items
        JMenuItem showMonoTriItem = new JMenuItem("Show MonoTri Display");
        showMonoTriItem.addActionListener(e -> {
            HueNumGUI arbHueNumGUI5 = HueNumGUI.getHueNumGUIInstance(5);
            MonotriWorkFrame monoTriDisplayView = new MonotriWorkFrame(arbHueNumGUI5);
            monoTriDisplayView.setVisible(true);
            registerFrame(monoTriDisplayView);
        });
        viewMenu.add(showMonoTriItem);

        JMenuItem showBadAnchPattern4ClocksItem = new JMenuItem("Show useless AnchPattern4Clocks");
        showBadAnchPattern4ClocksItem.addActionListener(e -> openAnchPattern4Clocks());
        viewMenu.add(showBadAnchPattern4ClocksItem);

        JMenuItem showCircMouserItem = new JMenuItem("Show CircMouserView");
        showCircMouserItem.addActionListener(e -> openCircMouserView());
        viewMenu.add(showCircMouserItem);

        // Import SVG menu items
        JMenuItem openSvgItem = new JMenuItem("Open SVG to convert");
        openSvgItem.addActionListener(e -> openSVGConverter());
        importSVGMenu.add(openSvgItem);


        // Template menu items
        JMenuItem openSavedTemplateItem = new JMenuItem("Open Saved Template");
        openSavedTemplateItem.addActionListener(e -> openTemplateList());
        templateMenu.add(openSavedTemplateItem);

        JMenuItem createSmartPicItem = new JMenuItem("Create SmartPic");
        createSmartPicItem.addActionListener(e -> openCreateSmartPic());
        templateMenu.add(createSmartPicItem);

        JMenuItem relationMatrixEditorItem = new JMenuItem("Relation Matrix Editor");
        relationMatrixEditorItem.addActionListener(e -> openRelationMatrixEditor());
        templateMenu.add(relationMatrixEditorItem);
        
        // Help menu items
        JMenuItem introToAppItem = new JMenuItem("Intro to the App");
        introToAppItem.addActionListener(e -> openHelpWindow("Introduction to the App", "help/intro_to_the_app.rtf"));
        helpMenu.add(introToAppItem);
        
        JMenuItem selectionMethodsItem = new JMenuItem("Selection Methods");
        selectionMethodsItem.addActionListener(e -> openHelpWindow("Selection Methods", "help/selection_methods.rtf"));
        helpMenu.add(selectionMethodsItem);
        
        JMenuItem recolorMethodsItem = new JMenuItem("Recolor Methods");
        recolorMethodsItem.addActionListener(e -> openHelpWindow("Recolor Methods", "help/recolor_methods.rtf"));
        helpMenu.add(recolorMethodsItem);
        
        // Add separator and Acrylic Paints by Hue menu item
        helpMenu.addSeparator();
        JMenuItem acrylicPaintsItem = new JMenuItem("Acrylic Paints by Hue");
        acrylicPaintsItem.addActionListener(e -> openHelpWindow("Acrylic Paints by Hue", "help/acrylic_paints_by_hue.rtf"));
        helpMenu.add(acrylicPaintsItem);
        
        // Add HLS Hue Circle w True Complements menu item
        JMenuItem hlsHueCircleItem = new JMenuItem("HLS Hue Circle w True Complements");
        hlsHueCircleItem.addActionListener(e -> openHelpWindow("HLS Hue Circle with True Complements", "help/hls_hue_circle_true_complements.rtf"));
        helpMenu.add(hlsHueCircleItem);
        
        // Add HLS Hue Circle w CMYK menu item
        JMenuItem hlsHueCircleCMYKItem = new JMenuItem("HLS Hue Circle w CMYK");
        hlsHueCircleCMYKItem.addActionListener(e -> openHelpDiagramWindow("HLS Hue Circle with CMYK", 
            "help/help_images/java OstHCSColorPt visual ref CMYK printValues 051519 new spacingv3d.png"));
        helpMenu.add(hlsHueCircleCMYKItem);
    } // end method setupMenus



    private void openCircMouserView() {
        int viewWidth = 200;
        CircMouserView view = new CircMouserView(viewWidth);
        view.setVisible(true);
        openFrames.add(view);
    } // end method openCircMouserView



    private void openSVGConverter() {
        SVGConverterFrame converterFrame = new SVGConverterFrame(appMediator);
        converterFrame.setVisible(true);
        registerFrame(converterFrame);
    }
    
    private void openTemplateList() {
        JFileChooser fileChooser = new JFileChooser(StorageConfig.getTemplatesPath().toFile());
        fileChooser.setDialogTitle("Open Template");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Template Files (*.json)", "json"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            TemplateWorkFrame workFrame = new TemplateWorkFrame(appMediator);
            if (workFrame.openTemplate(fileChooser.getSelectedFile())) {
                workFrame.setVisible(true);
                registerFrame(workFrame); // Register the frame for window management
            } else {
                workFrame.dispose(); // Close the frame if loading failed
            }
        }
    } // end method openTemplateList


    private void openCreateSmartPic() {
        CreateSmartPicController controller = new CreateSmartPicController(appMediator);
        controller.launch();
        registerFrame(controller.getFrame());
    } // end method openCreateSmartPic

    private void openRelationMatrixEditor() {
        RelationMatrixEditorController controller = new RelationMatrixEditorController(appMediator);
        controller.launch();
        registerFrame(controller.getFrame());
    } // end method openRelationMatrixEditor



    /* // TODO 072625 took out
    private void openBridgeTryout() {
        RMatrixUserSetupGUI bridgeSetup = new RMatrixUserSetupGUI();
        // Assuming RMatrixUserSetupGUI extends JFrame or has a show/setVisible method
        bridgeSetup.setVisible(true);
        registerFrame(bridgeSetup);
    }
     */
    
    private void openAnchPattern4Clocks() {
        Message message = new Message("SHOW_ANCHOR_PATTERN", null, null);
        appMediator.sendMessage("smartpic", message);
    }
    
    /**
     * Registers a frame to be tracked in the window menu
     * @param frame The frame to register
     */
    public void registerFrame(JFrame frame) {
        if (!openFrames.contains(frame) && frame != this) {  // Don't register self
            openFrames.add(frame);
            updateWindowMenu();
            
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    openFrames.remove(frame);
                    updateWindowMenu();
                }
            });
        }
    }
    
    /**
     * Updates the window menu with current open frames
     */
    private void updateWindowMenu() {
        windowMenu.removeAll();
        for (JFrame frame : openFrames) {
            JMenuItem item = new JMenuItem(frame.getTitle());
            item.addActionListener(e -> {
                frame.setVisible(true);
                frame.toFront();
                frame.requestFocus();
            });
            windowMenu.add(item);
        }
        if (openFrames.isEmpty()) {
            JMenuItem noFramesItem = new JMenuItem("No open windows");
            noFramesItem.setEnabled(false);
            windowMenu.add(noFramesItem);
        }
    }
    
    /**
     * Opens a help window displaying formatted text from the specified resource file.
     * 
     * @param title The title for the help window
     * @param resourcePath The path to the help text resource file
     */
    private void openHelpWindow(String title, String resourcePath) {
        HelpTextWindow helpWindow = new HelpTextWindow(title, resourcePath);
        helpWindow.setVisible(true);
        registerFrame(helpWindow);
    }
    
    /**
     * Opens a help diagram window displaying an image from the specified resource file.
     * The image will be automatically resized to fit within the window.
     * 
     * @param title The title for the help diagram window
     * @param resourcePath The path to the image resource file
     */
    private void openHelpDiagramWindow(String title, String resourcePath) {
        HelpDiagram helpDiagram = new HelpDiagram(title, resourcePath);
        helpDiagram.setVisible(true);
        registerFrame(helpDiagram);
    }
}
