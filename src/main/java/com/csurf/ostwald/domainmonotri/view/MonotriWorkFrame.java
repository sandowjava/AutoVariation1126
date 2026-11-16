package com.csurf.ostwald.domainmonotri.view;

import com.csurf.ostwald.domainmonotri.controller.MonotriRenderContrlr;
import com.csurf.ostwald.domainmonotri.controller.MouseListenerDotDrag;
import com.csurf.ostwald.domainmonotri.controller.OstdotOccupiedListController;
import com.csurf.ostwald.domainmonotri.controller.SelectionMode;
import com.csurf.ostwald.domainmonotri.model.*;
import com.csurf.templatecore.events.IPositionalPaletteEventListener;
import com.csurf.templatecore.events.PositionalPaletteEvent;
import com.csurf.templatecore.events.ColorSelectionEvent;
import com.csurf.templatecore.events.IColorSelectionListener;
import com.csurf.templatecore.events.IColorSource;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.model.BaselineValueSatModel;
import com.csurf.templatecore.controller.TemplatePaletteShift;
import com.csurf.templatecore.service.IColorOperationService;
import com.csurf.templatecore.view.TemplateWorkFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Frame container for MonoTri display components.
 * Hue numbers are handled in two ranges:
 * - Internal hue numbers (0-35) used for calculations and data storage
 * - User-facing hue numbers (1-36) used for display and user input
 */
public class MonotriWorkFrame extends JFrame implements IMonoTriDisplay, IColorSource, IPositionalPaletteEventListener {
    private static final Logger logger = Logger.getLogger(MonotriWorkFrame.class.getName());
    
    private MonotriDrawPanel drawPanel;
    private OstdotOccupiedListController ostdotOccupiedListController;

    // Dwell-commit for hover-driven tooltip updates (103026): rapid transit
    // across OstDots keeps replacing/restarting the pending commit, so the
    // toolTipPanel display only changes after the pointer rests on a dot
    // for HOVER_COMMIT_MS. This keeps the occupied list stable while the
    // user moves toward the scroll pane.
    private static final int HOVER_COMMIT_MS = 325;
    private final Timer hoverCommitTimer = createHoverCommitTimer();
    private Runnable pendingHoverCommit;
    private JTextField hueField;
    private int currentHueNumCode; // Internal hue number (0-35)
    private JMenu backgroundGrayMenu;
    private JMenu displayStateSimplifyMenu = new JMenu("Simplify Display");
    private boolean cmykDisplayMode = false;  // Default to RGB mode
    private boolean useSimpleStateDisplay = true;  // Default to simple state display
    private final List<IColorSelectionListener> colorListeners = new ArrayList<>();
    private ColorPtOstDot currentSelectedColor;
    private int chipSizeForMouseListener; // TODO 060425 this is a dummy value -- PLAN: rework this approach, probably JUNK
    private TemplateWorkFrame templateWorkFrame = null; // Reference to parent frame, may be null
    private String linkedYesNo = "-linked";
    private JMenuItem currentSelectedGrayItem = null; // Track the currently selected gray level menu item
    private JPanel bottomPanel; // Panel for the bottom of the frame

    // --- Color mode toggle UI fields ---
    private JRadioButton rgbModeButton;
    private JRadioButton cmykModeButton;

    public static int debugCounter = 0; // 062825
    private boolean hueRotateInEffect = false;// 072425 added for hue-rotation by hueRotationModeRadioButton - condition for functionality
    private int hueShiftDistCW = 0; // 072425 added for hue-rotation functionality for hueRotationModeRadioButton - provides input parameter
    private boolean hueRotateEVInEffect = false; // 112625 added for equal-value hue rotation - enabled when both hueRotationModeRadioButton AND EV checkbox are selected
    private JRadioButton hueRotationModeRadioButton; // 112625 stored as field for enable/disable logic
    private BaselineValueSatModel baselineValueSatModel = null; // 013026 baseline for EV rotation to prevent drift





    // Constructor added 062825- only purpose is to get "unlinked" String
    public MonotriWorkFrame(TemplateWorkFrame templateWorkFrame, HueNumGUI someHueNumGUI, ColorPtOstDot color, String linkedOrNot) {
        this(templateWorkFrame, someHueNumGUI, color);
        this.linkedYesNo = linkedOrNot; // TODO 062825 this is not hooked up yet, so it's a hidden dummy value for now
    } // end constructor


    /**
     * Sets the parent TemplateWorkFrame for this MonotriWorkFrame.
     * This enables access to the template's occupied positions.
     */
    public void setTemplateWorkFrame(TemplateWorkFrame frame) {
        // Unregister from old frame if exists
        if (this.templateWorkFrame != null) {
            this.templateWorkFrame.removePositionalPaletteEventListener(this);
        }
        
        this.templateWorkFrame = frame;
        
        // Register with new frame if exists
        if (frame != null) {
            frame.addPositionalPaletteEventListener(this);
        }
        
        // Update display with the new template using centralized method
        updateDisplay();
        
        // Select the pure hue color for the current page (position 1)
        MonotriPage page = MonotriBook.getInstance().getMonoTriPage(currentHueNumCode);
        ColorPtOstDot pureHueColor = page.getColorByFillOrder(1); // Position 1 is always pure hue
        drawPanel.getController().setSelectedColor(pureHueColor);
    } // end method setTemplateWorkFrame




    /**
     * Gets the parent TemplateWorkFrame, which provides occupied position information.
     */
    public TemplateWorkFrame getTemplateWorkFrame() {
        return templateWorkFrame;
    }




    /**
     * Updates the text in the hue field to show the current user-facing hue number (1-36).
     */
    private void updateHueFieldText() {
        // Convert internal hue (0-35) to user-facing (1-36) for display
        HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(currentHueNumCode+1);
        // int hueDegrees360 = userFacingHueNum * 10; // No, each hue is NOT 10 degrees. Never repeat this mistake, never make this incorrect assumption
        int hueDegrees360 = HueSpacingStdChoices.getSingleton().getHueDegrIntForHueNumGUI(someHueNumGUI); // Use correct hueDegrees360 for userFacingHueNum
        hueField.setText(String.format("Hue %d (%d°)", someHueNumGUI.getUserFacingHueNum(), hueDegrees360));
    } // end method updateHueFieldText




    /**
     * Changes the current hue by the given delta, wrapping around at the boundaries.
     * @param delta The amount to change the hue by (-1 for CCW, 1 for CW)
     */
    private void changeHue(int delta) { // TODO 072425 this is flow path for both CW orCCW buttons (simple or hue-rotation)
        // Wrap around using internal hue range (0-35) // 062625 NOTE: using HueNumGUI instance for "stick with it, it's convoluted but clear"
        HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(currentHueNumCode+1);
        int newHueNumCode = someHueNumGUI.circularAddOrSubtract(delta).getInternalCodeHueNum();

        if (newHueNumCode == 36) {
            int kerflooTraceMonotriFlow = 666; // TODO 072125 trace debug only
        }

        setCurrHueNumCode(newHueNumCode);
    } // end method changeHue




    /**
     * Updates the current hue based on user input in the hue field.
     * Expects user-facing hue numbers (1-36).
     */
    private void updateHueFromField() {
        String text = hueField.getText().replace("Hue", "").trim();
        if (text.contains("(")) {
            text = text.substring(0, text.indexOf("(")).trim();
        }
        try {
            int userFacingHue = Integer.parseInt(text);
            // Convert user-facing (1-36) to internal (0-35) range
            HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(userFacingHue);
            int hueNumCode = someHueNumGUI.getInternalCodeHueNum();
            setCurrHueNumCode(hueNumCode);
        } catch (NumberFormatException e) {
            // Restore field to current hue if invalid input
            updateHueFieldText(); // called in try-catch in -updateHueFromField- triggered by user action in hueField
        }
    } // end method updateHueFromField



    // TODO 062725 replace INT param with HueNumGUI param -"clear name" insufficient
    /**
     * Creates a new MonotriWorkFrame.
     * This is the constructor that all the other constructors use
     * @param someHueNumGUI The standard hue number in user-facing range (1-36)
     * @param initialColor The initial color to select, or null for no selection
     */
    public MonotriWorkFrame(HueNumGUI someHueNumGUI, ColorPtOstDot initialColor) {
        // Convert from user-facing (1-36) to internal (0-35) range
        // HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(stdGUIHueNum);
        int hueNumCode = someHueNumGUI.getInternalCodeHueNum();
        this.currentHueNumCode = hueNumCode;
        setTitle("MonotriWorkFrame - Hue " + someHueNumGUI.getUserFacingHueNum());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Create components
        drawPanel = new MonotriDrawPanel(this);
        ostdotOccupiedListController = new OstdotOccupiedListController(this);
        
        // Update controller with internal hue (0-35 range)
        MonotriPage page = MonotriBook.getInstance().getMonoTriPage(hueNumCode);
        drawPanel.getController().setCurrMonoTriPage(page);
        drawPanel.getController().setSimplifiedViewEnabled(useSimpleStateDisplay); // Initialize the controller's simplified view flag to match the frame's flag

        // Set default background to 60% gray (fifth menu item)
        int rgb = 255 - (int)(255 * (60 / 100.0));
        Color defaultGray = new Color(rgb, rgb, rgb);
        drawPanel.setBackground(defaultGray);
        
        // Create hue navigation controls
        JPanel hueControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

        // Create radio buttons for rotation mode
        JRadioButton simpleDisplayRadioButton = new JRadioButton("Simple View Rotation");
        this.hueRotationModeRadioButton = new JRadioButton("Selected Rotation Mode");
        ButtonGroup rotationModeGroup = new ButtonGroup();
        rotationModeGroup.add(simpleDisplayRadioButton);
        rotationModeGroup.add(this.hueRotationModeRadioButton);
        simpleDisplayRadioButton.setSelected(true); // Set simple rotation as default
        
        // Set radio button heights to 20 pixels
        simpleDisplayRadioButton.setPreferredSize(new Dimension(simpleDisplayRadioButton.getPreferredSize().width, 20));
        this.hueRotationModeRadioButton.setPreferredSize(new Dimension(this.hueRotationModeRadioButton.getPreferredSize().width, 20));
        
        // Add action listeners to toggle hueRotateInEffect flag and update EV controls enable/disable
        simpleDisplayRadioButton.addActionListener(e -> {
            hueRotateInEffect = false;
            hueRotateEVInEffect = false;
            updateEVRotateControlsEnableState();
            System.out.println("DEBUG: Simple View Rotation selected, hueRotateInEffect = " + hueRotateInEffect);
        });
        
        this.hueRotationModeRadioButton.addActionListener(e -> {
            hueRotateInEffect = true;
            updateEVRotateControlsEnableState();
            System.out.println("DEBUG: Selected Rotation Mode selected, hueRotateInEffect = " + hueRotateInEffect);
        });
        
        // Create navigation buttons
        JButton ccwButton = new JButton("<-CCW");
        JButton cwButton = new JButton("CW->");
        
        // Set button heights to 20 pixels
        ccwButton.setPreferredSize(new Dimension(ccwButton.getPreferredSize().width, 20));
        cwButton.setPreferredSize(new Dimension(cwButton.getPreferredSize().width, 20));

        // Create and configure hue field
        hueField = new JTextField();
        hueField.setPreferredSize(new Dimension(120, 20));
        ((AbstractDocument) hueField.getDocument()).setDocumentFilter(new HueNumberFilter());
        hueField.addActionListener(e -> updateHueFromField());
        updateHueFieldText(); // Initialize with current hue

        ccwButton.addActionListener(e -> changeHue(-1));
        cwButton.addActionListener(e -> changeHue(1));

        // Add components to hue control panel
        hueControlPanel.add(simpleDisplayRadioButton);
        hueControlPanel.add(hueRotationModeRadioButton);
        hueControlPanel.add(ccwButton);
        hueControlPanel.add(hueField);
        hueControlPanel.add(cwButton);

        // Set explicit width to 400 pixels and height to 40 pixels
        hueControlPanel.setPreferredSize(new Dimension(400, 40));

        // Layout
        bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        
        setLayout(new BorderLayout());
        add(hueControlPanel, BorderLayout.NORTH);
        add(drawPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Create menu
        JMenuBar menuBar = new JMenuBar();
        JMenuItem fullStateItem = new JMenuItem("Full-State OstDot Icons (complex)"); // TODO 060425 need code for implementation
        JMenuItem simpleStateItem = new JMenuItem("Simple-State OstDot Icons"); // TODO 060425 need code for implementation
        displayStateSimplifyMenu.add(fullStateItem);
        displayStateSimplifyMenu.add(simpleStateItem);
        
        // Set initial background for the default selected item (simple-state)
        simpleStateItem.setBackground(Color.LIGHT_GRAY);
        
        // Add action listeners for display state menu items
        fullStateItem.addActionListener(e -> {
            useSimpleStateDisplay = false;
            System.out.println("DEBUG: Full-State OstDot Icons selected");
            // Update menu item backgrounds
            fullStateItem.setBackground(Color.LIGHT_GRAY);
            simpleStateItem.setBackground(null); // Reset to default
            // Set simplified view to false in the controller
            drawPanel.getController().setSimplifiedViewEnabled(false);
            // Refresh the display
            drawPanel.updatePanelDisplayStates();
            drawPanel.invalidate();
            drawPanel.revalidate();
            drawPanel.repaint();
        });
        
        simpleStateItem.addActionListener(e -> {
            useSimpleStateDisplay = true;
            System.out.println("DEBUG: Simple-State OstDot Icons selected");
            // Update menu item backgrounds
            simpleStateItem.setBackground(Color.LIGHT_GRAY);
            fullStateItem.setBackground(null); // Reset to default
            // Set simplified view to true in the controller
            drawPanel.getController().setSimplifiedViewEnabled(true);
            // Refresh the display
            drawPanel.updatePanelDisplayStates();
            drawPanel.invalidate();
            drawPanel.revalidate();
            drawPanel.repaint();
        });
        
        createBackgroundMenu();
        menuBar.add(backgroundGrayMenu);
        menuBar.add(displayStateSimplifyMenu);
        setJMenuBar(menuBar);

        // Initialize with initial color if provided
        if (initialColor != null) {
            drawPanel.getController().setSelectedColor(initialColor);
        }
        
        // Pack and center
        pack();
        // Set explicit width to 400 pixels while preserving calculated height
        setSize(400, getHeight()); // 090425 width of 400 is minimum workable width
        setLocationRelativeTo(null);

        initColorModeControls();

        // Initialize MouseListenerDotDrag
        initMouseListenerDotDrag();
    } // end constructor

    /**
     * Updates the enable/disable state of EV rotation controls and manages baseline lifecycle.
     * Controls are enabled only when BOTH of these conditions are true:
     * 1. The "Selected Rotation Mode" radio button is selected
     * 2. Whether the "Equal-Value Rotation" checkbox is checked
     * 
     * Also manages BaselineValueSatModel lifecycle:
     * - When entering EV rotation mode: Creates baseline from current palette
     * - When exiting EV rotation mode: Clears baseline
     * - When switching templates: Clears baseline to end EV rotation session
     */
    public void updateEVRotateControlsEnableState() {
        boolean hueRotationModeSelected = hueRotationModeRadioButton.isSelected();
        boolean evCheckboxChecked = drawPanel.getEqualValueRotateCheckbox().isSelected();
        
        // Determine new EV rotation state
        boolean newEVRotateState = hueRotationModeSelected && evCheckboxChecked;
        boolean previousEVRotateState = hueRotateEVInEffect;
        
        // Update the flag
        hueRotateEVInEffect = newEVRotateState;
        
        // Manage baseline lifecycle
        if (newEVRotateState && !previousEVRotateState) {
            // Transitioning INTO EV rotation mode: Create baseline
            createAndSetBaseline();
        } else if (!newEVRotateState && previousEVRotateState) {
            // Transitioning OUT OF EV rotation mode: Clear baseline
            clearBaseline();
        }
        
        // Enable/disable the EV rotation controls based on both conditions
        boolean shouldEnable = hueRotationModeSelected && evCheckboxChecked;
        drawPanel.setEVRotateControlsEnabled(shouldEnable);
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("EV Rotate controls enable state updated: hueRotationMode=%b, evCheckbox=%b, enabled=%b, baseline=%s", 
                hueRotationModeSelected, evCheckboxChecked, shouldEnable, 
                newEVRotateState ? "ACTIVE" : "INACTIVE"));
        }
    } // end method updateEVRotateControlsEnableState
    
    
    /**
     * Creates a baseline value-saturation model from the current palette.
     * Called when user enters EV rotation mode (checks EV checkbox while in rotation mode).
     * Stores baseline in MonotriWorkFrame for access during EV rotation operations.
     */
    private void createAndSetBaseline() {
        try {
            if (templateWorkFrame == null) {
                logger.warning("Cannot create baseline: templateWorkFrame is null");
                return;
            }
            
            DrawObjRoster roster = templateWorkFrame.getDrawObjRoster();
            if (roster == null) {
                logger.warning("Cannot create baseline: DrawObjRoster is null");
                return;
            }
            
            // Create baseline from current palette
            baselineValueSatModel = new BaselineValueSatModel(roster, currentHueNumCode);
            
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(String.format("Baseline created: %s", baselineValueSatModel.getDebugInfo()));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating baseline for EV rotation: " + e.getMessage(), e);
        }
    }
    
    
    /**
     * Clears the baseline value-saturation model.
     * Called when user exits EV rotation mode (unchecks EV checkbox or deselects rotation mode).
     * Also called when user switches templates to end the EV rotation session.
     */
    private void clearBaseline() {
        try {
            baselineValueSatModel = null;
            
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Baseline cleared: EV rotation session ended");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error clearing baseline: " + e.getMessage(), e);
        }
    }



    /**
     * Initializes the MouseListenerDotDrag for color-drag operations and hover info display.
     * This method creates and registers the MouseListenerDotDrag with the MonotriDrawPanel
     * to handle color-drag operations, hover information display, and normal UI functionality.
     */
    private void initMouseListenerDotDrag() {
        MouseListenerDotDrag mouseListenerDotDrag = new MouseListenerDotDrag(this);
        
        // Add the listener to the MonotriDrawPanel
        // The listener implements both MouseListener and MouseMotionListener
        // to handle drag operations and hover information display
        drawPanel.addMouseListener(mouseListenerDotDrag);
        drawPanel.addMouseMotionListener(mouseListenerDotDrag);
        
        System.out.println("MouseListenerDotDrag registered with MonotriDrawPanel");
    }

    private void initColorModeControls() {
        // Panel radio buttons
        ButtonGroup group = new ButtonGroup();
        rgbModeButton = new JRadioButton("RGB", true);
        cmykModeButton = new JRadioButton("CMYK");
        rgbModeButton.addActionListener(e -> updateColorMode(ColorDisplayMode.RGB));
        cmykModeButton.addActionListener(e -> updateColorMode(ColorDisplayMode.CMYK));
        group.add(rgbModeButton);
        group.add(cmykModeButton);

        // Add label and radio buttons to the bottom panel
        JLabel colorModeLabel = new JLabel("Simulate value of CMYK print colors?");
        bottomPanel.add(colorModeLabel);
        bottomPanel.add(rgbModeButton);
        bottomPanel.add(cmykModeButton);
        bottomPanel.setPreferredSize(new Dimension(400, 23));
    } // end initColorModeControls

    /**
     * Updates the color mode.
     * 
     * @param mode The new color mode
     */
    private void updateColorMode(ColorDisplayMode mode) {
        drawPanel.setColorDisplayMode(mode);
        rgbModeButton.setSelected(mode == ColorDisplayMode.RGB);
        cmykModeButton.setSelected(mode == ColorDisplayMode.CMYK);
    } // end updateColorMode




    public MonotriWorkFrame(HueNumGUI someHueNumGUI) { // TODO 062725 replace INT param with HueNumGUI param -"clear name" insufficient
        this(someHueNumGUI, null);
    } // end constructor




    public MonotriWorkFrame(TemplateWorkFrame templateWorkFrame, HueNumGUI someHueNumGUI, ColorPtOstDot initialColor) {
        this(someHueNumGUI, initialColor);
        this.templateWorkFrame = templateWorkFrame;

        // debug only
        debugCounter +=1;
        if (debugCounter == 1 || debugCounter == 2) {
            int stopFlow = 666;
        }
        
        // Register for PositionalPaletteEvents
        if (templateWorkFrame != null) {
            System.out.println("DEBUG: MonotriWorkFrame registering as listener to TemplateWorkFrame");
            templateWorkFrame.addPositionalPaletteEventListener(this);
            
            // Verify registration
            System.out.println("DEBUG: MonotriWorkFrame registered as listener to TemplateWorkFrame");
            
            // Check if the roster is available
            DrawObjRoster roster = templateWorkFrame.getDrawObjRoster();
            if (roster != null) {
                System.out.println("DEBUG: DrawObjRoster available from TemplateWorkFrame");
                
                // Ensure the roster has a valid reference to the template
                if (roster.getTemplateDrawObjs() == null || roster.getTemplateDrawObjs().isEmpty()) {
                    System.out.println("WARNING: DrawObjRoster has null or empty templateDrawObjs reference");
                    
                    // Get the template from TemplateWorkFrame
                    if (templateWorkFrame.getCurrTemplate() != null) {
                        System.out.println("DEBUG: Attempting to fix DrawObjRoster reference");
                        
                        // Force the roster to update its state
                        roster.setTemplateDrawObjs(templateWorkFrame.getCurrTemplate().getAllDrawObjs());
                        roster.updateToCurrObjState();
                        
                        System.out.println("DEBUG: DrawObjRoster reference fixed, state updated");
                    } else {
                        System.out.println("ERROR: TemplateWorkFrame has no current template");
                    }
                } else {
                    System.out.println("DEBUG: DrawObjRoster has valid templateDrawObjs reference with " + 
                                      roster.getTemplateDrawObjs().size() + " objects");
                }
            } else {
                System.out.println("WARNING: DrawObjRoster not available from TemplateWorkFrame");
            }
        } else {
            System.out.println("WARNING: TemplateWorkFrame is null, cannot register as listener");
        }
        
        // Initialize display using the centralized method
        updateDisplay(); // TODO 070825 updates all OstdotPanels w current state (check: if accociated w TemplateWorkFrame)
    } // end constructor


    /**
     * Constructor for MonotriWorkFrame with a TemplateWorkFrame and HueNumGUI.
     * 
     * @param templateWorkFrame The parent TemplateWorkFrame
     * @param someHueNumGUI The HueNumGUI to use
     */
    public MonotriWorkFrame(TemplateWorkFrame templateWorkFrame, HueNumGUI someHueNumGUI) {
        this(templateWorkFrame, someHueNumGUI, null);
    } // end constructor




    /**
     * Gets the current internal hue number (0-35).
     */
    public int getCurrHueNumCode() {
        return currentHueNumCode;
    } // end method getCurrHueNumCode



    /**
     * Sets the current hue using an internal hue number (0-35).
     * Updates the UI and notifies the controller.
     */
    public void setCurrHueNumCode(int newHueNumCode) {
        if (newHueNumCode < 0 || newHueNumCode > 35) {
            throw new IllegalArgumentException("Invalid hue number " + newHueNumCode + ". Must be between 0 and 35");
        }

        // 072425 added variable -prevHueNumCode-for flow that starts with -getShiftedHueNumGUI- method call
        int prevHueNumCode = currentHueNumCode;
        int rawShiftDist = calcRawShiftDistance(newHueNumCode, prevHueNumCode);


        this.currentHueNumCode = newHueNumCode;
        updateHueFieldText(); // called by method -setCurrHueNumCode-
        
        // Update controller with internal hue (0-35 range)
        MonotriPage page = MonotriBook.getInstance().getMonoTriPage(newHueNumCode);
        drawPanel.getController().setCurrMonoTriPage(page);
        drawPanel.getController().setSimplifiedViewEnabled(useSimpleStateDisplay); // Initialize the controller's simplified view flag to match the frame's flag

        // Select the pure hue color for this page (position 1)
        ColorPtOstDot pureHueColor = page.getColorByFillOrder(1);
        drawPanel.getController().setSelectedColor(pureHueColor);

        if (newHueNumCode > 7) {
            int kerflooTraceMonotriFlow = 666; // TODO 072125 trace debug only
        }

        // Only perform hue rotation if the feature is enabled
        if (hueRotateInEffect) {
            // Hue-rotation CHANGE OF STATE in DrawObjRoster needs to happen BEFORE call to -updateDisplay-
            IColorOperationService colorService = templateWorkFrame.getColorOperationService();
            
            // Route to either EV-rotation or standard hue rotation based on hueRotateEVInEffect flag
            if (hueRotateEVInEffect) {
                // Equal-Value Hue Rotation: preserve value (luminance) and saturation
                boolean useRGB = drawPanel.getRgbRadioButton().isSelected();
                double tolerance = drawPanel.getCurrentToleranceValue();
                colorService.shiftSelectedForEVHueRotation(rawShiftDist, useRGB, tolerance, baselineValueSatModel);
                
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format("EV Hue Rotation applied: shift=%d, useRGB=%b, tolerance=%.2f", 
                        rawShiftDist, useRGB, tolerance));
                }
            } else {
                // Standard Hue Rotation: preserve position (fillOrderNum)
                colorService.shiftSelectedForHueRotation(rawShiftDist);
                
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format("Standard Hue Rotation applied: shift=%d", rawShiftDist));
                }
            }
        }

        // Update display to reflect the new hue
        updateDisplay();
        
        // Update title with user-facing hue (1-36)
        HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(newHueNumCode+1);
        int userFacingHue = someHueNumGUI.getUserFacingHueNum();

        setTitle("MonoTri Display - Hue " + userFacingHue);
    } // end method setCurrHueNumCode




    /**
     * Returns an INT that is CW distance, positive or negative, from the "prevHueNumCode" by the difference between "newHueNumCode" and "prevHueNumCode".
     * This return of method is used to calculate the new hue number after a hue rotation.
     *
     * @param newHueNumCode  The new hue number
     * @param prevHueNumCode The previous hue number
     * @return A HueNumGUI instance representing the shifted hue number
     */
    private static int calcRawShiftDistance(int newHueNumCode, int prevHueNumCode) {
        // HueNumGUI startHueNumGUI = HueNumGUI.getHueNumGUIInstance(prevHueNumCode +1);
        int rawHueShift = newHueNumCode - prevHueNumCode;
        return rawHueShift;
    } // end method calcRawShiftDistance




    /**
     * Gets the MonotriDrawPanel used by this frame.
     * This panel is used for color selection and display.
     *
     * @return The MonotriDrawPanel instance
     */
    public MonotriDrawPanel getDrawPanel() {
        return drawPanel;
    } // end method getDrawPanel




    public void setSelectedColor(ColorPtOstDot color) {
        System.out.println("DEBUG: MonotriWorkFrame.setSelectedColor called with color: " + color);
        if (color != null) {
            System.out.println("DEBUG: Setting color with hue: " + color.getOstHCS().getStdCodeHueNum() +
                             ", fillOrder: " + color.getOstHCS().getFillOrderNum());
            this.currentSelectedColor = color;
            // Delegate to controller to maintain consistency with getSelectedColor
            drawPanel.getController().setSelectedColor(color);
            handleColorSelection(color);
        } else {
            System.out.println("DEBUG: Not setting null color");
            this.currentSelectedColor = null;
            drawPanel.getController().setSelectedColor(null);
        }
    } // end method setSelectedColor




    public int getChipSizeForMouseListener() {
        int chipSize = 30; // TODO 060425 this is a dummy return -- PLAN: rework this class, probably JUNK
        return chipSize;
    }

    public void setChipSizeForMouseListener(int chipSizeForMouseListener) {
        this.chipSizeForMouseListener = 30; // TODO 060425 this is a dummy value -- PLAN: rework this class, probably JUNK
    }

    /**
     * Filter to ensure only valid hue numbers (1-36) can be entered
     */
    private static class HueNumberFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string == null) return;

            String newValue = fb.getDocument().getText(0, fb.getDocument().getLength())
                    + string;
            if (isValidHueInput(newValue)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text == null) return;

            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String newValue = currentText.substring(0, offset) + text
                    + currentText.substring(offset + length);
            if (isValidHueInput(newValue)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        private boolean isValidHueInput(String text) {
            if (text.isEmpty()) return true;

            // Allow partial format entry
            if (text.matches("^Hue \\d{0,2}$")) return true;
            if (text.matches("^Hue \\d{1,2} \\(\\d{0,3}°?\\)?$")) return true;

            // Extract just the hue number for validation
            String numStr = text;
            if (numStr.contains("(")) {
                numStr = numStr.substring(0, numStr.indexOf("(")).trim();
            }
            numStr = numStr.replace("Hue", "").trim();

            if (numStr.isEmpty()) return true;

            try {
                int value = Integer.parseInt(numStr);
                // Only allow user-facing hue numbers (1-36)
                // Note: Internal hue numbers are 0-35 but this filter validates user input
                return value >= 1 && value <= 36;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    private void createBackgroundMenu() {
        backgroundGrayMenu = new JMenu("Gray background choices");

        // Calculate RGB values for shades of gray
        // 20% black = (204,204,204), 80% black = (51,51,51)
        int[] grayLevels = {10, 20, 30, 40, 60, 70, 80, 90}; // 090425 added white and black to array

        for (int level : grayLevels) {
            int rgb = 255 - (int) (255 * (level / 100.0));
            Color color = new Color(rgb, rgb, rgb);
            JMenuItem item = new JMenuItem(level + "% black");
            item.addActionListener(e -> {
                drawPanel.setBackground(color);
                
                // Reset background of previously selected item
                if (currentSelectedGrayItem != null) {
                    currentSelectedGrayItem.setBackground(UIManager.getColor("Menu.background"));
                }
                
                // Set background color for the newly selected item
                item.setBackground(new Color(220, 220, 220)); // Light gray background
                
                currentSelectedGrayItem = item; // Update the currently selected gray level menu item
            });
            backgroundGrayMenu.add(item);
        }
        
        // Set initial selection if needed (assuming white background by default)
        // This will be called only once when the menu is created
        if (currentSelectedGrayItem == null) {
            // Get the current background color
            Color currentBg = drawPanel.getBackground();
            int grayLevel = 100 - (int)((currentBg.getRed() / 255.0) * 100);
            
            // Find and highlight the menu item that matches the current background
            for (Component comp : backgroundGrayMenu.getMenuComponents()) {
                if (comp instanceof JMenuItem) {
                    JMenuItem menuItem = (JMenuItem)comp;
                    if (menuItem.getText().startsWith(grayLevel + "%")) {
                        menuItem.setBackground(new Color(220, 220, 220)); // Light gray background
                        currentSelectedGrayItem = menuItem;
                        break;
                    }
                }
            }
        }
    }

    public void addColorSelectionListener(IColorSelectionListener listener) {
        if (listener != null) {
            colorListeners.add(listener);
        }
    }

    public void removeColorSelectionListener(IColorSelectionListener listener) {
        colorListeners.remove(listener);
    }

    private void notifyColorSelectionListeners(ColorPtOstDot color) {
        ColorSelectionEvent event = new ColorSelectionEvent(this, color);
        // Create a copy of the listeners list to avoid ConcurrentModificationException
        List<IColorSelectionListener> listenersCopy = new ArrayList<>(colorListeners);
        for (IColorSelectionListener listener : listenersCopy) {
            listener.colorSelected(event);
        }
    }

    private void handleColorSelection(ColorPtOstDot selectedColor) {
        // Notify listeners
        if (selectedColor != null) {
            notifyColorSelectionListeners(selectedColor);
        }
    } // end method handleColorSelection




    @Override
    public ColorPtOstDot getSelectedColor() {
        return drawPanel.getController().getSelectedColor();
    } // end method getSelectedColor




    @Override
    public void setMonoTriPage(MonotriPage page) {
        // This is now handled by the controller through setStandardHue
        if (drawPanel != null && drawPanel.getController() != null) {
            drawPanel.getController().setCurrMonoTriPage(page);
        }
    } // end method setMonoTriPage




    @Override
    public MonotriPage getMonoTriPage() {
        return drawPanel.getController().getCurrMonoTriPage();
    } // end method getMonoTriPage




    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        // Only update controller if drawPanel is initialized
        if (drawPanel != null && drawPanel.getController() != null) {
            drawPanel.getController().updateBackgroundColor(bg);
        }
    } // end method setBackground




    // Stub for future mode indicator
    protected void drawLumModeIcon(Graphics g) {
        // Placeholder for future implementation of mode indicator icon
    } // end method drawLumModeIcon




    // Mode management methods
    public void setHandPaintMode(boolean enabled) {
        // Since hand-paint mode is removed, always set to DEFAULT mode
        // This preserves the method signature for backward compatibility
        drawPanel.getController().setSelectionMode(SelectionMode.DEFAULT);
        
        // If we have a template work frame, update the display
        if (templateWorkFrame != null) {
            updateDisplay();
        }
    } // end method setHandPaintMode




    // Event notification methods
    public void notifyColorHover(ColorPtOstDot color) {
        // HOVER_EXIT passes null: cancel any pending commit; the display stays
        // sticky - it is only replaced when the pointer rests on another dot.
        if (color == null) {
            cancelHoverCommit();
            return;
        }
        OstHueColumnStep ostHCS = color.getOstHCS();
        if (ostHCS == null) {
            return; // null-dot guard: not a hover signal, leave pending commit alone
        }
        scheduleHoverCommit(() -> {
            if (drawPanel != null && ostdotOccupiedListController != null) {
                drawPanel.updateOccupiedListDisplay(
                        ostdotOccupiedListController.getOccupiedListContent(ostHCS));
            }
        });
    } // end method notifyColorHover

    /**
     * Creates the dwell-commit timer. Non-repeating: each firing applies the
     * single most recent pending commit.
     */
    private Timer createHoverCommitTimer() {
        Timer timer = new Timer(HOVER_COMMIT_MS, e -> commitPendingHover());
        timer.setRepeats(false);
        return timer;
    } // end method createHoverCommitTimer

    /**
     * Stores the pending display mutation and restarts the dwell timer.
     * The commit only runs if no further hover signal arrives within
     * HOVER_COMMIT_MS (i.e. the pointer has settled).
     */
    private void scheduleHoverCommit(Runnable commit) {
        pendingHoverCommit = commit;
        hoverCommitTimer.restart();
    } // end method scheduleHoverCommit

    /**
     * Cancels a pending commit without touching the display. Called on
     * hover-exit, gap-hover, drag, and panel-exit signals so that transient
     * mouse movement never erases or replaces the sticky display.
     */
    private void cancelHoverCommit() {
        pendingHoverCommit = null;
        hoverCommitTimer.stop();
    } // end method cancelHoverCommit

    private void commitPendingHover() {
        Runnable commit = pendingHoverCommit;
        pendingHoverCommit = null;
        if (commit != null) {
            commit.run();
        }
    } // end method commitPendingHover




    public void notifyColorSelected(ColorPtOstDot color) {
        currentSelectedColor = color;
        notifyColorSelectionListeners(color);
    } // end method notifyColorSelected




    // Controller access
    public MonotriRenderContrlr getController() {
        return drawPanel.getController();
    } // end method getTheController




    // IColorSource implementation
    @Override
    public ColorPtOstDot getCurrentColor() {
        return currentSelectedColor;
    } // end method getCurrentColor




    public void setCurrentColor(ColorPtOstDot color) {
        currentSelectedColor = color;
        // Update selection in controller if needed
    } // end method setCurrentColor




    /**
     * Updates the display based on the current state.
     * This is a centralized method to update all UI components.
     */
    public void updateDisplay() { // TODO 071425 important method - first called during MonotriWorkFrame init before it opens
        System.out.println("DEBUG: MonotriWorkFrame.updateDisplay called");
        
        // Get the DrawObjRoster from TemplateWorkFrame if available
        if (templateWorkFrame != null) {
            DrawObjRoster roster = templateWorkFrame.getDrawObjRoster();
            if (roster != null) {
                System.out.println("DEBUG: Got roster from TemplateWorkFrame in MonotriWorkFrame method -updateDisplay-");
                
                // Check if the roster has a valid reference to the template's objects
                if (roster.getTemplateDrawObjs() == null || roster.getTemplateDrawObjs().isEmpty()) {
                    System.out.println("WARNING: DrawObjRoster has null or empty templateDrawObjs reference in updateDisplay");
                    
                    // Get the template from TemplateWorkFrame
                    if (templateWorkFrame.getCurrTemplate() != null) {
                        System.out.println("DEBUG: Attempting to fix DrawObjRoster reference in updateDisplay");
                        
                        // Force the roster to update its state
                        roster.setTemplateDrawObjs(templateWorkFrame.getCurrTemplate().getAllDrawObjs());
                        roster.updateToCurrObjState();
                        
                        System.out.println("DEBUG: DrawObjRoster reference fixed in updateDisplay");
                    } else {
                        System.out.println("ERROR: TemplateWorkFrame has no current template in updateDisplay");
                    }
                } else {
                    System.out.println("DEBUG: DrawObjRoster has valid templateDrawObjs reference with " + 
                                      roster.getTemplateDrawObjs().size() + " objects in updateDisplay");
                }
                
                // Force a complete rebuild of the roster state
                System.out.println("DEBUG: Forcing roster to rebuild its state in updateDisplay");
                roster.updateToCurrObjState();
                
                // Update display from roster
                updateDisplayFromRoster(roster);
            } else {
                System.out.println("WARNING: DrawObjRoster is null in TemplateWorkFrame");
            }
        } else {
            System.out.println("DEBUG: No TemplateWorkFrame available, using default display");
            // Default display when no template is available
            if (drawPanel != null) {
                drawPanel.updatePanelDisplayStates();
            }
        }
    } // end method updateDisplay




    /**
     * Updates the display from the given DrawObjRoster.
     * This method centralizes the logic for updating the UI from the roster.
     * 
     * @param roster The DrawObjRoster to update from
     */
    public void updateDisplayFromRoster(DrawObjRoster roster) {
        if (roster == null) {
            System.out.println("ERROR: Null roster in updateDisplayFromRoster");
            return;
        }
        
        System.out.println("DEBUG: MonotriWorkFrame.updateDisplayFromRoster called");
        
        // Debug the roster's template reference
        System.out.println("DEBUG: Checking if DrawObjRoster has a valid templateDrawObjs reference");
        if (roster.getTemplateDrawObjs() == null) {
            System.out.println("ERROR: DrawObjRoster's templateDrawObjs is null!");
        } else {
            System.out.println("DEBUG: DrawObjRoster's templateDrawObjs size: " + roster.getTemplateDrawObjs().size());
        }
        
        // Debug the roster's selected objects
        System.out.println("DEBUG: Selected objects in DrawObjRoster: " + roster.getSelectedDrawObjs().size());
        for (DrawObj obj : roster.getSelectedDrawObjs()) {
            System.out.println("  - Selected object: " + obj);
        }
        
        // Force a complete rebuild of the roster state
        System.out.println("DEBUG: MonotriWorkFrame method -updateDisplayFromRoster- forcing roster to rebuild its state");
        roster.updateToCurrObjState();

        int kerflooCheckRosterState = 666;
        
        // Get the maps after ensuring state is updated
        Map<OstPosition, int[]> occupiedMap = roster.getUnmodifiableOccupiedMap(); // 072125 the map size seems OK
        Map<OstPosition, int[]> selectedMap = roster.getUnmodifiableSelectedMap();
        
        // Debug the maps  // TODO 072125 check this DEBUG output - look it over carefully
        System.out.println("DEBUG: MonotriWorkFrame method -updateDisplayFromRoster- has OccupiedMap size: " +
            (occupiedMap != null ? occupiedMap.size() : "null") + 
            ", SelectedMap size: " + (selectedMap != null ? selectedMap.size() : "null"));
        
        // Debug the occupied positions
        if (occupiedMap != null && !occupiedMap.isEmpty()) {
            System.out.println("DEBUG: Occupied positions in MonotriWorkFrame method -updateDisplayFromRoster-:");
            for (Map.Entry<OstPosition, int[]> entry : occupiedMap.entrySet()) {
                System.out.println("  Position: " + entry.getKey() + ", Hues: " + Arrays.toString(entry.getValue()));
            }
        }
        
        // Debug the selected positions
        if (selectedMap != null && !selectedMap.isEmpty()) {
            System.out.println("DEBUG: Selected positions in MonotriWorkFrame method -updateDisplayFromRoster-:");
            for (Map.Entry<OstPosition, int[]> entry : selectedMap.entrySet()) {
                System.out.println("  Position: " + entry.getKey() + ", Hues: " + Arrays.toString(entry.getValue()));
            }
        }
        
        // Update the panel with the maps
        if (drawPanel != null) {
            System.out.println("DEBUG: Updating drawPanel with maps");
            drawPanel.updateFromMaps(occupiedMap, selectedMap);
            
            // Force a complete UI refresh
            System.out.println("DEBUG: MonotriWorkFrame method -updateDisplayFromRoster- forcing complete UI refresh");
            
            // First refresh the drawPanel
            drawPanel.invalidate();
            drawPanel.revalidate();
            drawPanel.repaint();
            
            // Then refresh the parent frame
            invalidate();
            revalidate();
            repaint();
            
            // Request focus to ensure proper redraw
            drawPanel.requestFocusInWindow();
            
            // Add a small delay and then repaint again to ensure UI is updated
            // This helps with some Swing rendering issues
            SwingUtilities.invokeLater(() -> {
                drawPanel.repaint();
                repaint();
            });
        } else {
            System.out.println("ERROR: drawPanel is null in updateDisplayFromRoster");
        }
    } // end method updateDisplayFromRoster

    /**
     * Handles positional palette events by updating the display from the DrawObjRoster.
     * 
     * @param event The positional palette event
     */
    @Override
    public void handlePositionalPaletteEvent(PositionalPaletteEvent event) {
        if (event == null) {
            System.out.println("ERROR: MonotriWorkFrame received null PositionalPaletteEvent");
            return;
        }
        
        System.out.println("DEBUG: MonotriWorkFrame received PositionalPaletteEvent of type: " + event.getEventType());
        
        // Handle different event types appropriately
        switch (event.getEventType()) {
            case COLOR_CHANGE:
                System.out.println("DEBUG: Color change event received (includes drag and hue rotation)");
                break;
            case PALETTE_LOAD:
                System.out.println("DEBUG: Palette load event received");
                break;
            case SELECTION_CHANGE:
                System.out.println("DEBUG: Selection change event received");
                break;
            default:
                System.out.println("DEBUG: Unknown event type received: " + event.getEventType());
                break;
        }
        
        // Get the DrawObjRoster from the event
        DrawObjRoster roster = event.getDrawObjRoster();
        if (roster == null) {
            System.out.println("ERROR: PositionalPaletteEvent contains null DrawObjRoster");
            return;
        }
        
        // Debug the roster state
        System.out.println("DEBUG: Event DrawObjRoster - Selected objects: " + roster.getSelectedDrawObjs().size());
        
        // Check if the roster has a valid reference to the template's objects
        if (roster.getTemplateDrawObjs() == null || roster.getTemplateDrawObjs().isEmpty()) {
            System.out.println("WARNING: Event DrawObjRoster has null or empty templateDrawObjs reference");
            
            // Try to fix the reference if possible
            if (templateWorkFrame != null && templateWorkFrame.getCurrTemplate() != null) {
                System.out.println("DEBUG: Attempting to fix DrawObjRoster reference in event handler");
                
                // Force the roster to update its state
                roster.setTemplateDrawObjs(templateWorkFrame.getCurrTemplate().getAllDrawObjs());
                System.out.println("DEBUG: DrawObjRoster reference fixed in event handler");
            } else {
                System.out.println("ERROR: Cannot fix DrawObjRoster reference in event handler");
            }
        } else {
            System.out.println("DEBUG: Event DrawObjRoster has valid templateDrawObjs reference with " + 
                              roster.getTemplateDrawObjs().size() + " objects");
        }
        
        // Force a complete rebuild of the roster state
        System.out.println("DEBUG: Forcing roster to rebuild its state in event handler");
        roster.updateToCurrObjState();
        
        // Debug the maps after updating
        Map<OstPosition, int[]> occupiedMap = roster.getUnmodifiableOccupiedMap();
        Map<OstPosition, int[]> selectedMap = roster.getUnmodifiableSelectedMap();
        
        System.out.println("DEBUG: After updateToCurrObjState in event handler - OccupiedMap size: " + 
            (occupiedMap != null ? occupiedMap.size() : "null") + 
            ", SelectedMap size: " + (selectedMap != null ? selectedMap.size() : "null"));
        
        // Update the display from the roster regardless of event type
        // This ensures the UI is always in sync with the model
        updateDisplayFromRoster(roster);
        
        // Force a complete UI refresh to ensure all changes are visible
        drawPanel.invalidate();
        drawPanel.revalidate();
        drawPanel.repaint();
    } // end method handlePositionalPaletteEvent



    /**
     * Gets whether the simple state display is enabled.
     * When true, OstdotPanels will use simplified rendering for occupied but unselected dots in other hues.
     * 
     * @return true if simple state display is enabled, false otherwise
     */
    public boolean isUsingSimpleStateDisplay() {
        return useSimpleStateDisplay;
    }


    /**
     * Called when the mouse hovers a gap, exits the draw panel, or drags.
     * The occupied-list display is intentionally sticky: these signals only
     * cancel a pending hover commit, they never erase the display itself.
     */
    public void clearHoverInfo() {
        cancelHoverCommit();
    }
} // end class MonotriWorkFrame
