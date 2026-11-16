package com.csurf.templatecore.svgconverter.view;

import com.csurf.core.ApplicationMediator;
import com.csurf.core.Message;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.storage.TemplateStorage;
import com.csurf.templatecore.storage.config.StorageConfig;
import com.csurf.templatecore.svgconverter.controller.SVGConverterSubunitMediator;
import com.csurf.templatecore.view.ShapeDisplayPanel;
import com.csurf.templatecore.view.TemplateWorkFrame;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Frame for converting SVG files to DrawObj instances and managing their properties.
 */
public class SVGConverterFrame extends JFrame {
    private final ApplicationMediator appMediator;
    private final DefaultListModel<DrawObj> listModel;
    private final JList<DrawObj> drawObjList;
    private final ShapeDisplayPanel theShapeDisplayPanel;
    private final JPanel colorControlsPanel;
    private final JPanel clickColorSquare;
    private final JButton templateWorkButton;
    private DrawObj editingDrawObj;
    private final List<MonotriWorkFrame> monoTriFrames;


    // constructor
    public SVGConverterFrame(ApplicationMediator appMediator) {
        this.appMediator = appMediator;
        this.monoTriFrames = new ArrayList<>();
        setTitle(this.getClass().getSimpleName());
        
        // Create list model and list
        listModel = new DefaultListModel<>();
        drawObjList = new JList<>(listModel);
        drawObjList.setCellRenderer(new DrawObjListCellRenderer());
        drawObjList.addListSelectionListener(e -> handleDrawObjSelection());
        
        // Set up list for editing
        drawObjList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setupListEditing();

        // Create list panel
        
        // Create display panel
        theShapeDisplayPanel = new ShapeDisplayPanel();
        // displayPanel.setMinimumSize(new Dimension(160, 320)); // 052825 added
        // displayPanel.setPreferredSize(new Dimension(160, 520)); // 052825 added
        //displayPanel.setMaximumSize(new Dimension(180, 620)); // 052825 added
        
        // Create color controls panel (120px wide)
        colorControlsPanel = new JPanel();
        colorControlsPanel.setMinimumSize(new Dimension(140, 120)); // 052825 added
        colorControlsPanel.setPreferredSize(new Dimension(160, 120)); // 052825 changed
        colorControlsPanel.setLayout(new BoxLayout(colorControlsPanel, BoxLayout.Y_AXIS));
        
        // Create click color square (100x100px)
        clickColorSquare = new JPanel();
        clickColorSquare.setPreferredSize(new Dimension(100, 100));
        clickColorSquare.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        clickColorSquare.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openMonoTriDisplay();
                }
            }
        });
        
        // Create template button
        templateWorkButton = new JButton("Template Work");
        templateWorkButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        templateWorkButton.setPreferredSize(new Dimension(180, 30)); // 052825 added
        templateWorkButton.setMinimumSize(new Dimension(160, 30)); // 055025 added
        templateWorkButton.setMaximumSize(new Dimension(180, 30)); // 055025 changed
        templateWorkButton.addActionListener(e -> {
            createTemplateFromSVGConversion(appMediator);
        });
        
        // Add components to color controls panel
        JPanel squareWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        squareWrapper.add(clickColorSquare);
        colorControlsPanel.add(squareWrapper);
        colorControlsPanel.add(Box.createVerticalStrut(10)); // Add some spacing
        colorControlsPanel.add(templateWorkButton);
        colorControlsPanel.add(Box.createVerticalGlue()); // Push components to top
        
        // Layout
        setLayout(new BorderLayout());
        add(new JScrollPane(drawObjList), BorderLayout.WEST);
        add(new JScrollPane(theShapeDisplayPanel), BorderLayout.CENTER);
        add(colorControlsPanel, BorderLayout.EAST);
        
        // Menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Open SVG File");
        JMenuItem openItem = new JMenuItem("Browse to file");
        openItem.addActionListener(e -> { // TODO 062425 actionListener kicks off complex flow with important class interactions
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("SVG files", "svg"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                openSVGFile(selectedFile);
            }
        });
        fileMenu.add(openItem);
        menuBar.add(fileMenu);

        // Add Templates menu
        JMenu templatesMenu = new JMenu("Templates");
        JMenuItem saveTemplateItem = new JMenuItem("Save Template");
        JMenuItem doNotOpenTemplateItem = new JMenuItem("Open Template -DO NOT"); // TODO 062425 this goes against DESIGN INTENT. Need to remove this menuItem

        saveTemplateItem.addActionListener(e -> {
            Template noNameTemplate = createNoNameTemplate();
            JFileChooser fileChooser = new JFileChooser(StorageConfig.getTemplatesPath().toFile());
            fileChooser.setDialogTitle("Save Template");
            fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
            fileChooser.setSelectedFile(new File("basicStartTemplate.json"));
            
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                // Ensure file has .json extension
                if (!file.getName().toLowerCase().endsWith(".json")) {
                    file = new File(file.getPath() + ".json");
                }
                // Extract name without extension for storage
                String name = file.getName();
                if (name.toLowerCase().endsWith(".json")) {
                    name = name.substring(0, name.length() - 5);
                }

                // 062425 added one line for new field theTemplateSavedInstanceName
                noNameTemplate.setTemplateInstanceName(name); // TODO: 062425 rework this. Doing this here is going to cause duplicate code
                new TemplateStorage(appMediator).saveTemplate(noNameTemplate, name);
            }
        });

        doNotOpenTemplateItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(StorageConfig.getTemplatesPath().toFile());
            fileChooser.setDialogTitle("Ill-Advised: Open Template");
            fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
            
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String name = file.getName();
                if (name.toLowerCase().endsWith(".json")) {
                    name = name.substring(0, name.length() - 5);
                }
                Template template = new TemplateStorage(appMediator).loadTemplate(name);
                if (template != null) {
                    // Clear existing list
                    listModel.clear();
                    // Add loaded template's DrawObjs
                    template.getAllDrawObjs().forEach(listModel::addElement);
                }
            }
        });

        templatesMenu.add(saveTemplateItem);
        templatesMenu.add(doNotOpenTemplateItem);
        menuBar.add(templatesMenu);
        
        setJMenuBar(menuBar);
        
        // Window properties
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
    } // end constructor




    private void createTemplateFromSVGConversion(ApplicationMediator appMediator) {
        // Get template name from user
        String templateNameString = JOptionPane.showInputDialog(this,
            "Enter name for new template:",
            "Create Template",
            JOptionPane.PLAIN_MESSAGE);

        if (templateNameString != null && !templateNameString.trim().isEmpty()) {
            // Create array of current DrawObjs
            DrawObj[] drawObjs = new DrawObj[listModel.getSize()];
            for (int i = 0; i < listModel.getSize(); i++) {
                drawObjs[i] = listModel.getElementAt(i);
            }

            // Create new Template with name and DrawObjs
            Template template = new Template(templateNameString, drawObjs);

            // Create and show new TemplateWorkFrame with template
            new TemplateWorkFrame(appMediator, template);

            System.out.println("Template created: " + templateNameString);
            int numDrawObjs = listModel.getSize();
            JOptionPane.showMessageDialog(this,
                String.format("Number of DrawObjs: %d", numDrawObjs),
                "Template Creation",
                JOptionPane.INFORMATION_MESSAGE);
        }
    } // end method createTemplateFromSVGConversion



    /**
     * Creates a Template from the current array of DrawObjs after initial conversion from SVG file, using simplest Template constructor.
     * This method is called by saveTemplateItem's actionListener in this class, SVGConverterFrame.
     * Convention is to give this Template instance a basic "startName" instead of a fully detailed name.
     * @return
     */
    private Template createNoNameTemplate() {
        int size = listModel.getSize();
        DrawObj[] drawObjs = new DrawObj[size];
        for (int i = 0; i < size; i++) {
            drawObjs[i] = listModel.getElementAt(i);
        }
        Template template = new Template(drawObjs);
        return template;
    } // end method createNoNameTemplate




    private void openMonoTriDisplay() {
        DrawObj selectedObj = drawObjList.getSelectedValue();
        ColorPtOstDot arbitraryColor = MonotriBook.getInstance().getPureHueColorByCodeHueNum(1);
        ColorPtOstDot initialColor = selectedObj != null ? 
            selectedObj.getColorPtOstDotFillColor() : arbitraryColor;
        
        // Get the hue number from the ColorPtOstDot
        int hueNumCode = initialColor.getStdCodeHueNum();
        HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(hueNumCode +1);
        
        // Convert from 1-36 range to 0-35 range for MonotriWorkFrame constructor
        MonotriWorkFrame monoTriFrame = new MonotriWorkFrame(someHueNumGUI, initialColor);
        monoTriFrames.add(monoTriFrame);
        monoTriFrame.setVisible(true);
        
        // Register the frame with ApplicFrame
        Message message = new Message("REGISTER_FRAME", monoTriFrame, null);
        appMediator.sendMessage("app", message);
        
        monoTriFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                monoTriFrames.remove(monoTriFrame);
            }
        });
    }
    
    private void setupListEditing() {
        drawObjList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = drawObjList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        Rectangle bounds = drawObjList.getCellBounds(index, index);
                        if (bounds != null && bounds.contains(e.getPoint())) {
                            editingDrawObj = listModel.getElementAt(index);
                            String newName = JOptionPane.showInputDialog(drawObjList,
                                "Enter new name:", editingDrawObj.getName());
                            if (newName != null && !newName.trim().isEmpty()) {
                                editingDrawObj.setName(newName.trim());
                                drawObjList.repaint();
                            }
                        }
                    }
                }
            }
        });
    }
    
    private void handleDrawObjSelection() {
        DrawObj selected = drawObjList.getSelectedValue();
        if (selected != null) {
            theShapeDisplayPanel.setSelectedDrawObj(selected);
            clickColorSquare.setBackground(selected.getCurrentColor().toAWTColor());
        }
    } // end method handleDrawObjSelection



    
    private void openSVGFile(File file) {
        SVGConverterSubunitMediator converterMediator = 
            (SVGConverterSubunitMediator) appMediator.getSubunit("svgconverter");
        Message message = new Message("CONVERT_FILE", file, converterMediator);
        Object result = appMediator.sendMessage("svgconverter", message);
        
        if (result instanceof DrawObj[]) {
            DrawObj[] drawObjs = (DrawObj[]) result;
            listModel.clear();
            for (DrawObj obj : drawObjs) {
                listModel.addElement(obj);
            }
            theShapeDisplayPanel.setDrawObjs(drawObjs);
        } else if (result instanceof Exception) {
            JOptionPane.showMessageDialog(this,
                "Error converting SVG: " + ((Exception) result).getMessage(),
                "Conversion Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Cell renderer for DrawObj list items
     */
    private static class DrawObjListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof DrawObj) {
                setText(((DrawObj) value).getName());
            }
            return this;
        }
    }
}
