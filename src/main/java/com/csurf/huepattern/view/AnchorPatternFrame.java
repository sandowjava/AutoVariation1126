package com.csurf.huepattern.view;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;
import com.csurf.huepattern.model.AnchorPatternPrelim;
import com.csurf.huepattern.model.proxy.TemplateProxyData;
import com.csurf.templatecore.model.Template;

/**
 * Main frame for creating and displaying AnchorPattern instances.
 * 
 * Supports two modes:
 * - INFORMATIONAL: Display only (shows occupied hues from Template)
 * - OPERATIONAL: Full functionality (linked to TemplateProxyData)
 * 
 * Layout: APDisplayPanel (upper left), AGControlPanel (upper right), APChoicesPanel (bottom).
 * Fixed size with no resizing.
 */
public class AnchorPatternFrame extends JFrame {
    private static final Logger logger = Logger.getLogger(AnchorPatternFrame.class.getName());
    
    /**
     * Mode enum for AnchorPatternFrame.
     * INFORMATIONAL: Display only, no editing
     * OPERATIONAL: Full functionality, linked to TemplateProxyData
     */
    public enum Mode {
        INFORMATIONAL,
        OPERATIONAL
    }
    
    private static final int FRAME_WIDTH = 640;
    private static final int FRAME_HEIGHT = 560;
    
    private Mode mode;
    private Template template;
    private TemplateProxyData templateProxyData;
    
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu paletteMenu;
    private JMenu anchorPatternMenu;
    
    private APDisplayPanel apDisplayPanel;
    private AGControlPanel agControlPanel;
    private APChoicesPanel apChoicesPanel;
    private AnchorPatternFrameMediator mediator;

    /**
     * Creates an INFORMATIONAL mode AnchorPatternFrame.
     * Used for displaying occupied hues from a Template (display only).
     * 
     * @param template the Template to display hues from
     */
    public AnchorPatternFrame(Template template) {
        super("Anchor Pattern Control - View Only");
        
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }
        
        this.mode = Mode.INFORMATIONAL;
        this.template = template;
        this.templateProxyData = null;
        
        logger.info("AnchorPatternFrame: Creating INFORMATIONAL mode with Template");
        
        initializeFrame();
    }
    
    /**
     * Creates an OPERATIONAL mode AnchorPatternFrame.
     * Used for full functionality linked to TemplateProxyData.
     * 
     * @param templateProxyData the TemplateProxyData to work with
     */
    public AnchorPatternFrame(TemplateProxyData templateProxyData) {
        super("Anchor Pattern Control - Edit");
        
        if (templateProxyData == null) {
            throw new IllegalArgumentException("TemplateProxyData cannot be null");
        }
        
        this.mode = Mode.OPERATIONAL;
        this.templateProxyData = templateProxyData;
        this.template = templateProxyData.getOriginalTemplate();
        
        logger.info("AnchorPatternFrame: Creating OPERATIONAL mode with TemplateProxyData");
        
        initializeFrame();
    }
    
    /**
     * Gets the current mode of this frame.
     * 
     * @return the Mode (INFORMATIONAL or OPERATIONAL)
     */
    public Mode getMode() {
        return mode;
    }
    
    /**
     * Gets the Template associated with this frame.
     * 
     * @return the Template
     */
    public Template getTemplate() {
        return template;
    }
    
    /**
     * Gets the TemplateProxyData associated with this frame (OPERATIONAL mode only).
     * 
     * @return the TemplateProxyData, or null if in INFORMATIONAL mode
     */
    public TemplateProxyData getTemplateProxyData() {
        return templateProxyData;
    }
    
    /**
     * Gets the AnchorPattern from this frame.
     * In INFORMATIONAL mode: returns null
     * In OPERATIONAL mode: returns the TemplateProxyData's AnchorPattern
     * 
     * @return the AnchorPattern, or null if in INFORMATIONAL mode
     */
    public AnchorPatternPrelim getAnchorPattern() {
        if (mode == Mode.INFORMATIONAL) {
            return null;
        }
        return templateProxyData.getAnchorPattern();
    }
    
    /**
     * Initializes the frame components.
     */
    private void initializeFrame() {
        // Initialize menus
        setupMenus();
        
        // Initialize panels
        apDisplayPanel = new APDisplayPanel();
        agControlPanel = new AGControlPanel();
        apChoicesPanel = new APChoicesPanel();
        
        // Set up occupied hues display from the palette
        // This shows the actual state of the palette when the frame opens
        try {
            apDisplayPanel.setPaletteAndBuildOccupiedHues(
                template.getDrawObjRoster().getCurrPalette()
            );
            logger.fine("AnchorPatternFrame: Occupied hues initialized from palette");
        } catch (Exception e) {
            logger.warning("AnchorPatternFrame: Failed to initialize occupied hues: " + e.getMessage());
        }
        
        // In INFORMATIONAL mode, disable editing controls
        if (mode == Mode.INFORMATIONAL) {
            agControlPanel.setEnabled(false);
            apChoicesPanel.setEnabled(false);
            logger.fine("AnchorPatternFrame: Disabled editing controls for INFORMATIONAL mode");
        }
        
        // Create the mediator to orchestrate all components
        // The mediator will wire up the dial, controllers, and state management
        AnchorPatternPrelim initialPattern = (mode == Mode.OPERATIONAL)
            ? templateProxyData.getAnchorPattern() 
            : AnchorPatternPrelim.createDefault();
        
        mediator = new AnchorPatternFrameMediator(
            apDisplayPanel,
            agControlPanel.getAGChoicePanel(),
            agControlPanel.getAttributeChoicePanel(),
            agControlPanel.getDisplayState(),
            initialPattern
        );
        
        logger.fine("AnchorPatternFrame: Created AnchorPatternFrameMediator");
        
        // Connect DisplayState from control panel to display panel
        apDisplayPanel.setDisplayState(agControlPanel.getDisplayState());
        
        // Wire up Update Display button to use mediator
        agControlPanel.getUpdateDisplayButton().addActionListener(e -> {
            mediator.handleUpdateDisplayButton();
            updateDisplay();
        });
        
        setupLayout();
        
        // Configure frame
        setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setResizable(false);
        pack();
        
        // Update dial center point after layout is complete
        // APDisplayPanel center is at (230, 230) relative to the panel
        // We need to account for the panel's position in the frame
        mediator.updateDialCenter(230, 230);
        
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen
    }
    
    private void setupMenus() {
        menuBar = new JMenuBar();
        
        // File menu
        fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        
        // Edit menu
        editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        
        // AnchorPattern menu
        anchorPatternMenu = new JMenu("AnchorPattern");
        
        JMenuItem saveNamedAPItem = new JMenuItem("Save named AnchorPattern");
        saveNamedAPItem.addActionListener(e -> saveNamedAnchorPattern());
        anchorPatternMenu.add(saveNamedAPItem);
        
        JMenuItem loadSavedAPItem = new JMenuItem("Load saved AnchorPattern");
        loadSavedAPItem.addActionListener(e -> loadSavedAnchorPattern());
        anchorPatternMenu.add(loadSavedAPItem);
        
        JMenuItem seePaletteItem = new JMenuItem("See palette for current (displayed) AP");
        seePaletteItem.addActionListener(e -> seePaletteForCurrentAP());
        anchorPatternMenu.add(seePaletteItem);
        
        menuBar.add(anchorPatternMenu);
        
        // Palette menu
        paletteMenu = new JMenu("Palette");
        JMenuItem openSavedPaletteItem = new JMenuItem("Open saved palette");
        openSavedPaletteItem.addActionListener(e -> openSavedPalette());
        paletteMenu.add(openSavedPaletteItem);
        menuBar.add(paletteMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void openSavedPalette() {
        // Flow for navigating to and selecting a saved PaletteWName
        // To be implemented later
    }
    
    private void saveNamedAnchorPattern() {
        // Flow for saving the current AnchorPattern with a name
        // To be implemented later
    }
    
    private void loadSavedAnchorPattern() {
        // Flow for loading a previously saved AnchorPattern
        // To be implemented later
    }
    
    private void seePaletteForCurrentAP() {
        // Flow for displaying the palette that corresponds to the current (displayed) AnchorPattern
        // To be implemented later
    }
    
    /**
     * Updates the display panel to render the current DisplayState.
     * Called when the Update Display button is clicked.
     */
    private void updateDisplay() {
        // Trigger repaint of the display panel
        apDisplayPanel.repaint();
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Top panel containing display and control panels side by side
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(apDisplayPanel, BorderLayout.WEST);
        topPanel.add(agControlPanel, BorderLayout.CENTER);
        
        // Add panels to frame
        add(topPanel, BorderLayout.CENTER);
        add(apChoicesPanel, BorderLayout.SOUTH);
    }
} // end class AnchorPatternFrame
