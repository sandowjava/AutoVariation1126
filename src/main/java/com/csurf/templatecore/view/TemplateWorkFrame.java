package com.csurf.templatecore.view;

import com.csurf.core.ApplicationMediator;
import com.csurf.core.Message;
import com.csurf.huepattern.model.RMatrixMiniPrelim;
import com.csurf.huepattern.model.proxy.TemplateProxyData;
import com.csurf.huepattern.service.TemplateProxyDataService;
import com.csurf.huepattern.view.AnchorPatternFrame;
import com.csurf.huepattern.view.agsinfo.AGsInfoFrame;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.csurf.ostwald.domainmonotri.model.OstPosition;
import com.csurf.ostwald.domainmonotri.view.ColorDisplayMode;
import com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame;
import com.csurf.templatecore.controller.*;
import com.csurf.templatecore.events.IPositionalPaletteEventListener;
import com.csurf.templatecore.events.ISelectionEventListener;
import com.csurf.templatecore.events.PositionalPaletteEvent;
import com.csurf.templatecore.events.SelectionEvent;
import com.csurf.templatecore.model.*;
import com.csurf.templatecore.service.IColorOperationService;
import com.csurf.templatecore.sorting.DrawObjSortStrategy;
import com.csurf.templatecore.sorting.strategies.*;
import com.csurf.templatecore.storage.PaletteStorage;
import com.csurf.templatecore.storage.PaletteWShapeNamesStorage;
import com.csurf.templatecore.storage.TemplateStorage;
import com.csurf.templatecore.storage.TemplateStorageException;
import com.csurf.variationmodule.controller.CreateSmartPicController;
import com.csurf.variationmodule.controller.RelationMatrixEditorController;
import com.csurf.variationmodule.controller.VariationDisplayController;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * The TemplateWorkFrame is the main frame for working with templates.
 * It contains panels for displaying shapes, selecting shapes, and working with colors.
 */
public class TemplateWorkFrame extends JFrame 
        implements IPositionalPaletteEventListener, 
                   ISelectionEventListener,
                   ShapeListPanel.SelectionListener,
                   ShapeListPanel.HueRelativesButtonListener,
                   IColorOperationService {
    // 090625 Static counter for frame instances
    private static int instanceCounter = 0;
    private final ShapeListPanel listAllDrawObjsPanel;
    private final SelectedShapesListPanel listSelectDrawObjsPanel;
    private final ShapeDisplayPanel shapesPanel;
    private final ShapeSelectionController controller;
    private final JPanel colorWorkPanel;
    private final JPanel clickColorSquare;
    private final ApplicationMediator mediator;
    private final TemplateStorage storage;
    private final PaletteStorage paletteStorage;
    private final PaletteWShapeNamesStorage paletteWShapeNamesStorage;
    private DrawObj singleSelectedDrawObj; 
    private OstPosition[] currOstPositions; 
    private Template currTemplate;
    private PaletteExplicit paletteExplicit;
    private PaletteOperationHub paletteOperationHub;
    private MonotriWorkFrame activeMonoTriFrame;
    private JButton focusShapeColorInfoButton;
    private JLabel alertLabel;
    private ColorDisplayMode colorDisplayMode = ColorDisplayMode.RGB;
    private JLabel selectionLabel;
    private final List<MonotriWorkFrame> linkedMonoTriFrames = new ArrayList<>();
    private final List<DrawObjStateViewController> drawObjStateControllers = new ArrayList<>();
    private final List<IPositionalPaletteEventListener> paletteEventListeners = new ArrayList<>();
    private JTextField currTemplateNameTextfield;
    private KeyEventDispatcher linkedFrameShortcutDispatcher;
    private boolean paletteWasLoaded = false;
    private String lastPaletteName = null;
    /**
     * Display string recording the source file of the most recent successful file-based palette
     * load since the current template was opened. Holds either the sentinel value "noLoad"
     * (meaning: no palette-load operation has occurred since the current template was opened)
     * or a composed display string of the form "filename_CODE" produced by
     * {@link #captureFileNameForBookmark(String, boolean)} and assigned by its callers.
     */
    private String lastPaletteLoadCameFromFile = "noLoad";
    // Horizontal offset (pixels) applied when opening PaletteWNamesDisplayFrame, so the
    // display isn't centered on screen where it can be hidden behind other windows — the
    // right shift makes it more visible.
    private int rightShiftPalttWShapeNames = 410;
    private SelectionGroupChoiceFrame selectionGroupChoiceFrame;
    private final List<HueRelativesFrame> hueRelativesFrames = new ArrayList<>();
    // Single designated receiver for hue updates; null when none is designated
    private HueRelativesFrame designatedHueReceiver;
    private final List<JFrame> colorSourceFrames = new ArrayList<>();
    @Deprecated
    private HandPaintState handPaintState;
    private JButton sendHueRSideButton;
    private JSpinner anchGroupChoiceSpinner; // 110525 added for huepattern functionality
    private JButton anchGroupAddButton; // 110525 added for huepattern AG functionality - add to chosen AG
    private JButton anchGroupSubtractButton; // 112125 placeholder for huepattern AG functionality - remove from chosen AG
    private TemplateProxyData currTemplateProxyData; // Reference to current working copy for AG assignment
    private AGsInfoFrame currAGsInfoFrame; // Reference to current AGsInfoFrame for display updates
    private SelectionGroupListFacade displayFacade; // Facade for sorting display lists
    private com.csurf.templatecore.model.SortOrder allDrawObjsSortOrder = com.csurf.templatecore.model.SortOrder.ORIGINAL_ORDER; // Sort order for all DrawObjs list
    private com.csurf.templatecore.model.SortOrder selectedDrawObjsSortOrder = com.csurf.templatecore.model.SortOrder.ORIGINAL_ORDER; // Sort order for selected DrawObjs list
    private DrawObjSortStrategy allDrawObjsStrategy = new OriginalOrderStrategy(); // Strategy for all DrawObjs list
    private final SelectedDrawObjsDisplayManager selectionDisplayManager; // Owns listSelectDrawObjsPanel updates
    private JPanel activeSelectDrawObjInfoPanel; // Panel displaying hue info of currently selected DrawObj in listSelectDrawObjsPanel
    private JLabel focusDrawObjHueLabel; // Label showing hue number
    private JPanel currChosenDrawObjSwatch; // Color swatch showing selected DrawObj's color
    private JButton focusDrawObjInfoButton; // Button to open info display for selected DrawObj in listSelectDrawObjsPanel
    private JPanel focusColorPanel; // Sub-panel holding clickColorSquare and its description label
    private JLabel colorClickSquareDescripLabel; // Label describing the focus color swatch
    private int colorClickSquareWidth = 65; // Width of the focus color swatch in pixels




    public Template getCurrTemplate() {
        return currTemplate;
    }

    /**
     * Gets the DrawObjRoster from the current Template.
     * @return The DrawObjRoster, or null if no template is loaded
     */
    public DrawObjRoster getDrawObjRoster() {
        return currTemplate != null ? currTemplate.getDrawObjRoster() : null;
    }
    
    /**
     * Gets the PaletteOperationHub for this TemplateWorkFrame.
     * 
     * @return The PaletteOperationHub, or null if no template is loaded
     */
    public PaletteOperationHub getPaletteOperationHub() {
        return paletteOperationHub;
    }

    /**
     * Provides access to the color operation service.
     * 
     * This method returns the color operation service implementation,
     * which is TemplateWorkFrame itself. This allows callers to access
     * color operations through the IColorOperationService interface.
     * 
     * @return The color operation service
     */
    public IColorOperationService getColorOperationService() {
        return this;
    }

    /**
     * Shift selected DrawObjs by drag shift (W-series and B-series).
     * 
     * Delegates to PaletteOperationHub to perform the actual color transformation.
     * 
     * @param shiftSpecify The shift specification containing W and B shifts
     */
    @Override
    public void shiftSelectedForDragShift(ShiftSpecify shiftSpecify) {
        if (paletteOperationHub != null) {
            paletteOperationHub.shiftSelectedForDragShift(shiftSpecify);
        } else {
            System.out.println("WARNING: PaletteOperationHub not initialized, cannot perform drag shift");
        }
    }

    /**
     * Shift selected DrawObjs by hue rotation.
     * 
     * Delegates to PaletteOperationHub to perform the actual color transformation.
     * 
     * @param someRawShiftDist The hue shift distance
     */
    @Override
    public void shiftSelectedForHueRotation(int someRawShiftDist) {
        if (paletteOperationHub != null) {
            paletteOperationHub.shiftSelectedForHueRotation(someRawShiftDist);
        } else {
            System.out.println("WARNING: PaletteOperationHub not initialized, cannot perform hue rotation");
        }
    }

    /**
     * Shift selected DrawObjs by equal-value hue rotation.
     * 
     * Delegates to PaletteOperationHub to perform the actual color transformation.
     * 
     * @param someRawShiftDist The hue shift distance
     * @param useRGB Whether to use RGB color space
     * @param tolerance The tolerance for color matching
     * @param baseline The baseline value-saturation model
     */
    @Override
    public void shiftSelectedForEVHueRotation(int someRawShiftDist, boolean useRGB, 
                                              double tolerance, BaselineValueSatModel baseline) {
        if (paletteOperationHub != null) {
            paletteOperationHub.shiftSelectedForEVHueRotation(someRawShiftDist, useRGB, tolerance, baseline);
        } else {
            System.out.println("WARNING: PaletteOperationHub not initialized, cannot perform EV hue rotation");
        }
    }

    /**
     * Load a new palette by applying colors to all DrawObjs in the template.
     * 
     * This is the primary public interface for palette loading operations.
     * All external callers should use this method rather than calling DrawObjRoster.loadNewPalette() directly.
     * 
     * @param newPalette The palette containing colors to apply to DrawObjs
     * @return true if the palette was successfully applied, false otherwise
     */
    @Override
    public boolean loadNewPalette(PaletteWName newPalette) {
        if (paletteOperationHub != null) {
            return paletteOperationHub.loadNewPalette(newPalette);
        } else {
            System.out.println("WARNING: PaletteOperationHub not initialized, cannot load palette");
            return false;
        }
    }

    /**
     * Saves the palette on screen as a bookmark. Delegates to PaletteOperationHub.
     */
    @Override
    public void saveBookmark() {
        if (paletteOperationHub != null) {
            paletteOperationHub.saveBookmark();
        } else {
            System.out.println("WARNING: PaletteOperationHub not initialized, cannot save bookmark");
        }
    }

    /**
     * Undoes to the previous palette bookmark. Delegates to PaletteOperationHub.
     *
     * @return true if a palette was restored
     */
    @Override
    public boolean undoPalette() {
        if (paletteOperationHub != null) {
            return paletteOperationHub.undoPalette();
        } else {
            System.out.println("WARNING: PaletteOperationHub not initialized, cannot undo palette");
            return false;
        }
    }

    /**
     * Redoes to the next palette bookmark. Delegates to PaletteOperationHub.
     *
     * @return true if a palette was restored
     */
    @Override
    public boolean redoPalette() {
        if (paletteOperationHub != null) {
            return paletteOperationHub.redoPalette();
        } else {
            System.out.println("WARNING: PaletteOperationHub not initialized, cannot redo palette");
            return false;
        }
    }

    /**
     * @return true if Undo is available. Delegates to PaletteOperationHub.
     */
    @Override
    public boolean canUndo() {
        return paletteOperationHub != null && paletteOperationHub.canUndo();
    }

    /**
     * @return true if Redo is available. Delegates to PaletteOperationHub.
     */
    @Override
    public boolean canRedo() {
        return paletteOperationHub != null && paletteOperationHub.canRedo();
    }

    /**
     * Goes to the newest palette bookmark. Delegates to PaletteOperationHub.
     *
     * @return true if a palette was restored
     */
    @Override
    public boolean goToTopPalette() {
        if (paletteOperationHub != null) {
            return paletteOperationHub.goToTopPalette();
        } else {
            System.out.println("WARNING: PaletteOperationHub not initialized, cannot go to top palette");
            return false;
        }
    }

    /**
     * Goes to the oldest palette bookmark (B0). Delegates to PaletteOperationHub.
     *
     * @return true if a palette was restored
     */
    @Override
    public boolean goToBottomPalette() {
        if (paletteOperationHub != null) {
            return paletteOperationHub.goToBottomPalette();
        } else {
            System.out.println("WARNING: PaletteOperationHub not initialized, cannot go to bottom palette");
            return false;
        }
    }

    /**
     * @return true if "go to top" is available. Delegates to PaletteOperationHub.
     */
    @Override
    public boolean canGoToTop() {
        return paletteOperationHub != null && paletteOperationHub.canGoToTop();
    }

    /**
     * @return true if "go to bottom" is available. Delegates to PaletteOperationHub.
     */
    @Override
    public boolean canGoToBottom() {
        return paletteOperationHub != null && paletteOperationHub.canGoToBottom();
    }

    private void addColorModeControls() {
        JPanel colorModePanel = new JPanel();
        colorModePanel.setLayout(new BoxLayout(colorModePanel, BoxLayout.Y_AXIS));
        colorModePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // No border, zero padding
        colorModePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        ButtonGroup colorModeGroup = new ButtonGroup();
        JRadioButton rgbRadioButton = new JRadioButton("RGB", true);
        JRadioButton cmykRadioButton = new JRadioButton("CMYK");
        
        rgbRadioButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cmykRadioButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        rgbRadioButton.addActionListener(e -> setColorMode(ColorDisplayMode.RGB));
        cmykRadioButton.addActionListener(e -> setColorMode(ColorDisplayMode.CMYK));
        
        colorModeGroup.add(rgbRadioButton);
        colorModeGroup.add(cmykRadioButton);
        colorModePanel.add(rgbRadioButton);
        colorModePanel.add(cmykRadioButton);
        
        // Set size to fit content (two radio buttons need ~45 pixels)
        colorModePanel.setMinimumSize(new Dimension(120, 40));
        colorModePanel.setPreferredSize(new Dimension(120, 40));
        colorModePanel.setMaximumSize(new Dimension(120, 40));
        
        // Add to top of color work panel, preserving BoxLayout
        colorWorkPanel.add(colorModePanel, 0);
    }

    private void createActiveSelectDrawObjInfoPanel() {
        activeSelectDrawObjInfoPanel = new JPanel();
        activeSelectDrawObjInfoPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        activeSelectDrawObjInfoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Set fixed height of 25 pixels, width of 120 pixels to accommodate all components
        activeSelectDrawObjInfoPanel.setMinimumSize(new Dimension(160, 25));
        activeSelectDrawObjInfoPanel.setPreferredSize(new Dimension(160, 25));
        activeSelectDrawObjInfoPanel.setMaximumSize(new Dimension(160, 25));
        
        // Create color swatch panel for currently selected DrawObj (60x20 pixels)
        currChosenDrawObjSwatch = new JPanel();
        currChosenDrawObjSwatch.setPreferredSize(new Dimension(40, 25));
        currChosenDrawObjSwatch.setBackground(Color.WHITE);
        currChosenDrawObjSwatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        // Add mouse listener to detect double-clicks on the color swatch
        currChosenDrawObjSwatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click detected, open MonotriWorkFrame with hue from listSelectDrawObjsPanel
                    openMonotriWorkFrameWCurrFocusSwatch();
                }
            }
        });
        
        // Set cursor to indicate the panel is clickable
        currChosenDrawObjSwatch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        currChosenDrawObjSwatch.setToolTipText("Double-click to open MonotriWorkFrame with this hue");
        
        activeSelectDrawObjInfoPanel.add(currChosenDrawObjSwatch);
        
        // Create label for hue display
        focusDrawObjHueLabel = new JLabel("h=");
        activeSelectDrawObjInfoPanel.add(focusDrawObjHueLabel);
        
        // Create info button for currently selected DrawObj in listSelectDrawObjsPanel
        focusDrawObjInfoButton = new JButton("[ostDot]"); // odd brackets suggest narrow SWATCH width (not good UI)
        int buttonWidth = 85;
        focusDrawObjInfoButton.setPreferredSize(new Dimension(buttonWidth, 25));
        focusDrawObjInfoButton.setMinimumSize(new Dimension(buttonWidth, 25));
        focusDrawObjInfoButton.setMaximumSize(new Dimension(buttonWidth, 25));
        focusDrawObjInfoButton.setToolTipText("Open info display for selected DrawObj");
        focusDrawObjInfoButton.addActionListener(e -> showCurrFocusObjectInfo());
        activeSelectDrawObjInfoPanel.add(focusDrawObjInfoButton);
    }

    private void createFocusColorPanel() {
        focusColorPanel = new JPanel();
        focusColorPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        focusColorPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Fixed height of 30 pixels to match the color square's height
        focusColorPanel.setMinimumSize(new Dimension(160, 30));
        focusColorPanel.setPreferredSize(new Dimension(160, 30));
        focusColorPanel.setMaximumSize(new Dimension(160, 30));

        // clickColorSquare is created by getColorClickPanel(); its width comes from colorClickSquareWidth
        focusColorPanel.add(clickColorSquare);

        // Create description label for the focus color square
        colorClickSquareDescripLabel = new JLabel("Focus [L list]");
        focusColorPanel.add(colorClickSquareDescripLabel);
    }

    /**
     * Updates the focus DrawObj info panel with the currently selected DrawObj's color and hue.
     * This method is called whenever the selection in listSelectDrawObjsPanel changes.
     */
    private void updateFocusDrawObjInfoPanel() {
        // Get the selected DrawObj from listSelectDrawObjsPanel
        DrawObj selectedDrawObj = listSelectDrawObjsPanel.getSelectedDrawObj();
        
        if (selectedDrawObj != null) {
            // Update the color swatch with the DrawObj's fill color
            ColorPtOstDot fillColor = selectedDrawObj.getColorPtOstDotFillColor();
            if (fillColor != null) {
                Color awtColor = fillColor.toAWTColor();
                currChosenDrawObjSwatch.setBackground(awtColor);
            }
            
            // Update the hue label with the DrawObj's user-facing hue number
            int userFacingHueNum = selectedDrawObj.getUserFacingHueNum();
            if (userFacingHueNum > 0) {
                focusDrawObjHueLabel.setText("h=" + userFacingHueNum);
            } else {
                focusDrawObjHueLabel.setText("h=");
            }
        } else {
            // No selection, reset to default
            currChosenDrawObjSwatch.setBackground(Color.WHITE);
            focusDrawObjHueLabel.setText(" h = ");
        }
        
        // Repaint to show the updated color
        currChosenDrawObjSwatch.repaint();
        focusDrawObjHueLabel.repaint();
    }

    /**
     * Creates a new TemplateWorkFrame with the given ApplicationMediator.
     * @param mediator The ApplicationMediator to use
     */
    public TemplateWorkFrame(ApplicationMediator mediator) {
        this.mediator = mediator;
        this.storage = new TemplateStorage(mediator);
        this.paletteStorage = new PaletteStorage(mediator);
        this.paletteWShapeNamesStorage = new PaletteWShapeNamesStorage();

        initComponents();

        // Create main components
        listAllDrawObjsPanel = new ShapeListPanel();
        listAllDrawObjsPanel.getJList().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shapesPanel = new ShapeDisplayPanel();
        // Focus access for the controller's shift-toggle choreography (focus
        // writes before roster mutations, survivor relocation on removal).
        // Injected as a callback interface so the controller stays decoupled
        // from this frame (102926 design doc §4, D5).
        controller = new ShapeSelectionController(shapesPanel, listAllDrawObjsPanel,
                new ShapeSelectionController.FocusAccessor() {
                    @Override
                    public DrawObj getFocus() {
                        return getSingleSelectedDrawObj();
                    }

                    @Override
                    public void setFocus(DrawObj drawObj) {
                        setSingleSelectedDrawObj(drawObj);
                    }
                });
        listSelectDrawObjsPanel = new SelectedShapesListPanel();
        selectionDisplayManager = new SelectedDrawObjsDisplayManager(listSelectDrawObjsPanel);
        
        // Add selection listener to update focus panel when DrawObj is selected in listSelectDrawObjsPanel
        listSelectDrawObjsPanel.addSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateFocusDrawObjInfoPanel();
            }
        });
        
        // Register as a selection listener for immediate color updates
        listAllDrawObjsPanel.addDrawObjSelectionListener(this);
        
        // Shift-click selections (channel 2): the controller writes focus and mutates the
        // roster first; here we only fire SELECTION_CHANGE so PositionalPaletteEvent
        // listeners (e.g. MonotriWorkFrame) see the change. No focus write on this
        // channel — the controller is the single focus-writer for shift gestures.
        // See 102926 design doc §7.2.
        listAllDrawObjsPanel.addDrawObjShiftSelectionListener(clickedDrawObj -> {
            if (currTemplate != null && currTemplate.getDrawObjRoster() != null) {
                firePositionalPaletteEvent(new PositionalPaletteEvent(
                    this,
                    PositionalPaletteEvent.EventType.SELECTION_CHANGE,
                    currTemplate.getDrawObjRoster()
                ));
            }
        });
        
        // Register as a HueRelatives button listener
        listAllDrawObjsPanel.addHueRelativesButtonListener(this);
        
        // Add the HueRelatives button to the main list panel only
        listAllDrawObjsPanel.addHueRelativesButton();

        // Header label above the all-DrawObjs list (this instance only)
        listAllDrawObjsPanel.setHeaderText("FOCUS [R big swatch]");
        
        // Set a smaller preferred size for the selection list panel
        listSelectDrawObjsPanel.setPreferredSize(new Dimension(150, 100)); // 110525 WAS "Dimension(150, 100)"

        // Create color work panel
        colorWorkPanel = getJColorWorkPanel();

        // Create click color square
        clickColorSquare = getColorClickPanel();

        addColorModeControls();
        
        // Create and add focus DrawObj info panel (shows hue of selected item in listSelectDrawObjsPanel)
        createActiveSelectDrawObjInfoPanel();
        colorWorkPanel.add(activeSelectDrawObjInfoPanel, 1);

        // Create and add focus color panel (clickColorSquare + label) at index 2
        createFocusColorPanel();
        colorWorkPanel.add(focusColorPanel, 2);
        
        // Add main components to frame
        initFrameLayout();
        initObjectInfoComponents();
        
        // Register this frame as a listener to its own events for self-updating
        addPositionalPaletteEventListener(this);
        
        pack();
    } 

    private void initFrameLayout() {
        // Create a JSplitPane to hold the list panel and shapes panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                                             listAllDrawObjsPanel, shapesPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(200); // Initial divider position
        splitPane.setResizeWeight(0.15); // Give most resize weight to the shapes panel
        
        // Add the split pane to the center
        add(splitPane, BorderLayout.CENTER);
        
        // Add color work panel to the east
        add(colorWorkPanel, BorderLayout.EAST);
        
        // Initialize and add the template name text field to the NORTH position
        currTemplateNameTextfield = new JTextField();
        currTemplateNameTextfield.setEditable(false);
        // Non-focusable: display-only status bar. If focusable, its JTextField
        // "select-all" binding consumes Cmd-A before Selection menu accelerators.
        currTemplateNameTextfield.setFocusable(false);
        currTemplateNameTextfield.setHorizontalAlignment(JTextField.CENTER);
        currTemplateNameTextfield.setFont(new Font("Arial", Font.BOLD, 14));
        currTemplateNameTextfield.setText("No Template Loaded");
        
        add(currTemplateNameTextfield, BorderLayout.NORTH);

        // Set frame size and location
        setSize(1000, 800);
        setLocationRelativeTo(null);

        // Add selection listener to list panel
        listAllDrawObjsPanel.addSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleDrawObjSelection();
            }
        });
    } // end method initFrameLayout

    /**
     * Displays the palette name in the template name text field.
     * This method is called when a palette is transferred into the TemplateWorkFrame.
     * 
     * @param palette The palette whose name should be displayed
     */
    private void displayPaletteName(PaletteWName palette) {
        if (palette != null && palette.getPaletteName() != null && !palette.getPaletteName().isEmpty()) {
            currTemplateNameTextfield.setText("Template: " + currTemplate.getTheTemplateName() + 
                                             " | Palette: " + palette.getPaletteName());
        } else {
            // If palette or palette name is null/empty, just display the template name
            if (currTemplate != null) {
                currTemplateNameTextfield.setText("Template: " + currTemplate.getTheTemplateName());
            }
        }
    } // end method displayPaletteName




    private JPanel getColorClickPanel() {
        JPanel colorPanel = new JPanel();
        colorPanel.setPreferredSize(new Dimension(colorClickSquareWidth, 30));
        colorPanel.setBackground(Color.WHITE);
        colorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        // Add mouse listener to detect double-clicks on the color chip
        colorPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click detected, open MonotriWorkFrame
                    openMonotriWorkFrameWithCurrentHue();
                }
            }
        });
        
        // Set cursor to indicate the panel is clickable
        colorPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        colorPanel.setToolTipText("Double-click to open MonotriWorkFrame with this hue");
        
        return colorPanel;
    }

    private JPanel getJColorWorkPanel() {
        final JPanel colorWorkPanel;
        colorWorkPanel = new JPanel();
        colorWorkPanel.setMinimumSize(new Dimension(140, 120));
        colorWorkPanel.setPreferredSize(new Dimension(160, 120));
        
        // Use BoxLayout with Y_AXIS to stack components vertically with zero gaps
        colorWorkPanel.setLayout(new BoxLayout(colorWorkPanel, BoxLayout.Y_AXIS));
        
        return colorWorkPanel;
    }

    private void initComponents() {
        initTitle();
        initMenuBar();
        installLinkedFrameShortcuts();
        
        // Add window listener to handle frame closure
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
            @Override
            public void windowClosed(WindowEvent e) {
                removeLinkedFrameShortcuts();
            }
        });
    } 

    /**
     * Registers this frame's command shortcuts with the keyboard focus manager:
     * Save bookmark, Undo, Redo, Select All DrawObjs and Deselect All DrawObjs.
     * Menu accelerators only work while this frame has focus, but recoloring and
     * selection work is mostly done in the linked frames (MonoTri frames, color-source
     * frames and so on), which have no menu. A dispatcher makes the shortcuts work
     * from this frame and from every frame linked to it. While an editable text
     * component owns focus the dispatcher defers, so native text editing wins.
     */
    private void installLinkedFrameShortcuts() {
        final int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        linkedFrameShortcutDispatcher = e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            int modifiers = e.getModifiersEx() & (shortcutMask | InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK);
            boolean plain = modifiers == shortcutMask;
            boolean shifted = modifiers == (shortcutMask | InputEvent.SHIFT_DOWN_MASK);
            if (!plain && !shifted) {
                return false;
            }
            int keyCode = e.getKeyCode();
            if (keyCode != KeyEvent.VK_B && keyCode != KeyEvent.VK_Z
                    && keyCode != KeyEvent.VK_A && keyCode != KeyEvent.VK_D) {
                return false;
            }
            if (!isLinkedFrameShortcutWindow(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow())) {
                return false;
            }
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner instanceof JTextComponent && ((JTextComponent) focusOwner).isEditable()) {
                return false;
            }
            if (keyCode == KeyEvent.VK_B && plain) {
                saveBookmark();
            } else if (keyCode == KeyEvent.VK_Z && plain) {
                undoPalette();
            } else if (keyCode == KeyEvent.VK_Z) {
                redoPalette();
            } else if (keyCode == KeyEvent.VK_A && plain) {
                selectAllDrawObjs();
            } else if (keyCode == KeyEvent.VK_D && plain) {
                deselectAllDrawObjs();
            } else {
                return false;
            }
            return true;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(linkedFrameShortcutDispatcher);
    }

    private void removeLinkedFrameShortcuts() {
        if (linkedFrameShortcutDispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(linkedFrameShortcutDispatcher);
            linkedFrameShortcutDispatcher = null;
        }
    }

    private boolean isLinkedFrameShortcutWindow(Window window) {
        return window != null
                && (window == this
                    || window == selectionGroupChoiceFrame
                    || linkedMonoTriFrames.contains(window)
                    || colorSourceFrames.contains(window)
                    || hueRelativesFrames.contains(window));
    }

    private void initObjectInfoComponents() {
        // "Single-select" FOCUS shape ostDot info button
        focusShapeColorInfoButton = new JButton("[Focus big] ostDot"); // odd brackets suggest wide SWATCH width (not good UI)
        // Fixed width: natural preferred width + 10px
        Dimension focusBtnPrefSize = focusShapeColorInfoButton.getPreferredSize();
        int focusBtnWidth = focusBtnPrefSize.width + 5;
        focusShapeColorInfoButton.setMinimumSize(new Dimension(focusBtnWidth, focusBtnPrefSize.height));
        focusShapeColorInfoButton.setPreferredSize(new Dimension(focusBtnWidth, focusBtnPrefSize.height));
        focusShapeColorInfoButton.setMaximumSize(new Dimension(focusBtnWidth, focusBtnPrefSize.height));
        focusShapeColorInfoButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        focusShapeColorInfoButton.addActionListener(e -> showSelectedObjectInfo());
        colorWorkPanel.add(focusShapeColorInfoButton);

        // Alert label
        alertLabel = new JLabel(" ");
        alertLabel.setForeground(Color.RED);
        alertLabel.setVisible(false);
        colorWorkPanel.add(alertLabel);
        
        // Add selection label
        selectionLabel = new JLabel("Active colorOps");
        selectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 110525 added - puts button to LEFT side (why/how?)
        colorWorkPanel.add(selectionLabel);
        
        // Add selection list panel
        colorWorkPanel.add(listSelectDrawObjsPanel);

        // Send Hue button
        sendHueRSideButton = new JButton("small [ ] HuRelativs");
        sendHueRSideButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sendHueRSideButton.addActionListener(e -> openHueRelativesFrameWithSelectedHue());
        colorWorkPanel.add(sendHueRSideButton);
        
        // Add AnchorPatternPanel below sendHueRSideButton
        JPanel anchorPatternPanel = createAnchorPatternPanel();
        colorWorkPanel.add(anchorPatternPanel);
    } // end method initObjectInfoComponents
    
    /**
     * Creates the AnchorPatternPanel with a spinner and button.
     * @return The AnchorPatternPanel
     */
    private JPanel createAnchorPatternPanel() {
        JPanel anchorPatternPanel = new JPanel();
        anchorPatternPanel.setLayout(new BoxLayout(anchorPatternPanel, BoxLayout.Y_AXIS));
        anchorPatternPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // No border, zero padding
        anchorPatternPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Create spinner panel with label
        JPanel spinnerPanel = new JPanel();
        spinnerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0)); // Zero horizontal and vertical gaps
        spinnerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel anchGroupLabel = new JLabel("AnchGroup:");
        anchGroupChoiceSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
        anchGroupChoiceSpinner.setPreferredSize(new Dimension(60, 25));
        
        spinnerPanel.add(anchGroupLabel);
        spinnerPanel.add(anchGroupChoiceSpinner);
        
        // Create button
        anchGroupAddButton = new JButton("Add to AG");
        anchGroupAddButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        anchGroupAddButton.addActionListener(e -> handleAddToAGButton());

        // Create button
        anchGroupSubtractButton = new JButton("Remove from AG");
        anchGroupSubtractButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        anchGroupSubtractButton.addActionListener(e -> handleRemoveFromAGButton());
        
        // Add components to panel
        anchorPatternPanel.add(spinnerPanel);
        anchorPatternPanel.add(anchGroupSubtractButton);
        anchGroupSubtractButton.setOpaque(true);
        anchGroupSubtractButton.setBackground(Color.ORANGE);
        anchorPatternPanel.add(anchGroupAddButton);
        
        // Set size constraints
        anchorPatternPanel.setMaximumSize(new Dimension(150, 100));
        anchorPatternPanel.setPreferredSize(new Dimension(150, 100));
        
        return anchorPatternPanel;
    } // end method createAnchorPatternPanel




    /**
     * Handles the "Remove from AG" button click.
     * Gets selected DrawObjs from the current SelectionGroup and removes them from the selected AG
     * in the TemplateProxyData's RMatrixMini.
     * Uses RMatrixMini.removeFromAG() for each DrawObj that is in the selected AG.
     */
    private void handleRemoveFromAGButton() {
        // Validate that a template is loaded
        if (currTemplate == null) {
            JOptionPane.showMessageDialog(this, 
                "No template loaded. Please load a template first.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validate that TemplateProxyData is available
        // This is only available after starting the UC-040 workflow
        if (currTemplateProxyData == null) {
            JOptionPane.showMessageDialog(this, 
                "TemplateProxyData not initialized. Please start the AnchorPattern workflow first.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Get the RMatrixMini from TemplateProxyData
        RMatrixMiniPrelim rmatrix = currTemplateProxyData.getRMatrixMini();
        if (rmatrix == null) {
            JOptionPane.showMessageDialog(this, 
                "RMatrixMini not found in TemplateProxyData.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Get selected DrawObjs from the current SelectionGroup (like handleAddToAGButton does)
        DrawObjRoster roster = currTemplate.getDrawObjRoster();
        Set<DrawObj> selectedObjs = roster.getSelectionGroup().getSelectedDrawObjsSet();
        
        if (selectedObjs == null || selectedObjs.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No DrawObjs selected. Please select one or more DrawObjs first.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Get the AG number from the spinner
        int agNum = (Integer) anchGroupChoiceSpinner.getValue();
        
        // Validate AG number
        if (agNum < 1 || agNum > 4) {
            JOptionPane.showMessageDialog(this, 
                "Invalid AG number: " + agNum + ". Must be between 1 and 4.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Remove each selected DrawObj from the AG in TemplateProxyData's RMatrixMini
        // Only remove if the DrawObj is actually in the selected AG
        int removedCount = 0;
        for (DrawObj drawObj : selectedObjs) {
            if (drawObj != null) {
                // Get the UUID of the DrawObj
                UUID drawObjUUID = drawObj.getUniqueId();
                if (drawObjUUID != null) {
                    // Check if the DrawObj is in the selected AG
                    Set<Integer> drawObjAGs = rmatrix.getDrawObjAGs(drawObjUUID);
                    if (drawObjAGs != null && drawObjAGs.contains(agNum)) {
                        rmatrix.removeFromAG(drawObjUUID, agNum);
                        removedCount++;
                        System.out.println("Removed DrawObj '" + drawObj.getName() + "' from AG " + agNum + 
                            " in TemplateProxyData");
                    }
                }
            }
        }
        
        // Show confirmation message
        JOptionPane.showMessageDialog(this, 
            "Removed " + removedCount + " DrawObj(s) from AG " + agNum,
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
        
        // Auto-update AGsInfoFrame display to show changes immediately
        // This ensures the user sees the updated display after bulk AG removal
        if (currAGsInfoFrame != null) {
            System.out.println("TemplateWorkFrame: Triggering auto-update of AGsInfoFrame display after bulk AG removal");
            currAGsInfoFrame.getTheController().autoUpdateDrawSkeleton(currAGsInfoFrame.getFilterState());
        }
    } // end method handleRemoveFromAGButton


    private void initTitle() {
        setTitle("Template Work Frame");
        instanceCounter++;
        setTitle("Template Work Frame" +" #" + instanceCounter);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
    } // end method initTitle




    /**
     * Builds the Edit menu: palette bookmarks (Save, Undo, Redo, then Go to top and Go to bottom).
     *
     * @return The Edit menu
     */
    private JMenu createEditMenu() {
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JMenu editMenu = new JMenu("Edit");
        JMenuItem saveBookmarkItem = new JMenuItem("Save palettebookmark");
        saveBookmarkItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, shortcutMask));
        saveBookmarkItem.addActionListener(e -> saveBookmark());
        String bookmarkLimitHelp = "The number of saved palettes is limited. When the limit is reached, the oldest one "
                + "(other than the original) is discarded, including when Undo or Redo saves unsaved work.";
        saveBookmarkItem.setToolTipText(bookmarkLimitHelp);
        JMenuItem undoItem = new JMenuItem("Undo <bkmrk back");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask));
        undoItem.addActionListener(e -> undoPalette());
        undoItem.setToolTipText(bookmarkLimitHelp);
        JMenuItem redoItem = new JMenuItem("Redo bkmrk fwrd>");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask | InputEvent.SHIFT_DOWN_MASK));
        redoItem.addActionListener(e -> redoPalette());
        redoItem.setToolTipText(bookmarkLimitHelp);
        JMenuItem goToTopItem = new JMenuItem("Top bkmrk (newest)>");
        goToTopItem.addActionListener(e -> goToTopPalette());
        goToTopItem.setToolTipText(bookmarkLimitHelp);
        JMenuItem goToBottomItem = new JMenuItem("<Bottom bkmrk (oldest)");
        goToBottomItem.addActionListener(e -> goToBottomPalette());
        goToBottomItem.setToolTipText(bookmarkLimitHelp);
        editMenu.add(saveBookmarkItem);
        editMenu.addSeparator();
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(goToTopItem);
        editMenu.add(goToBottomItem);
        // Availability is worked out when the menu opens. The Undo and Redo items are re-enabled when it
        // closes so that the keyboard shortcuts always work; the actions do nothing when not available.
        editMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                undoItem.setEnabled(canUndo());
                redoItem.setEnabled(canRedo());
                goToTopItem.setEnabled(canGoToTop());
                goToBottomItem.setEnabled(canGoToBottom());
            }
            @Override
            public void menuDeselected(MenuEvent e) {
                undoItem.setEnabled(true);
                redoItem.setEnabled(true);
            }
            @Override
            public void menuCanceled(MenuEvent e) {
                undoItem.setEnabled(true);
                redoItem.setEnabled(true);
            }
        });
        return editMenu;
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu and Edit menu come first, by convention
        JMenu fileMenu = new JMenu("File");

        JMenuItem saveItem = new JMenuItem("Save Template");
        saveItem.addActionListener(e -> saveTemplateMenuItem());
        fileMenu.add(saveItem);

        JMenuItem saveNamedPaletteItem = new JMenuItem("Save named palette");
        saveNamedPaletteItem.addActionListener(e -> saveNamedPalette());
        fileMenu.add(saveNamedPaletteItem);

        JMenuItem exportSvgItem = new JMenuItem("Export template to SVG");
        exportSvgItem.addActionListener(e -> exportToSVGMenuItem());
        fileMenu.add(exportSvgItem);

        menuBar.add(fileMenu);
        menuBar.add(createEditMenu());

        // MonoTri menu
        JMenu monoTriMenu = new JMenu("MonoTri");
        JMenuItem openNewMonoTriItem = new JMenuItem("Open new MonoTri");
        openNewMonoTriItem.addActionListener(e -> openMonoTriFrameLinked());
        monoTriMenu.add(openNewMonoTriItem);
        menuBar.add(monoTriMenu);

        // Templates menu
        JMenu templatesMenu = new JMenu("Templates");

        JMenuItem openItem = new JMenuItem("Flip thru saved templates");
        openItem.addActionListener(e -> openTemplateMenuItem());

        templatesMenu.add(openItem);

        menuBar.add(templatesMenu);

        // Create Palettes menu
        JMenu palettesMenu = new JMenu("Palettes");
        JMenuItem transferPaletteItem = new JMenuItem("Transfer palette from saved template");
        transferPaletteItem.addActionListener(e -> transferPalette());
        palettesMenu.add(transferPaletteItem);
        
        JMenuItem loadNamedPaletteItem = new JMenuItem("Load named palette");
        loadNamedPaletteItem.addActionListener(e -> loadNamedPalette());
        palettesMenu.add(loadNamedPaletteItem);

        JMenuItem showPaletteItem = new JMenuItem("(Show-only) palette from saved Template");
        showPaletteItem.addActionListener(e -> showPaletteFromSavedTemplate());
        palettesMenu.add(showPaletteItem);
        
        palettesMenu.addSeparator();
        
        JMenuItem testPaletteWShapeNamesItem = new JMenuItem("Immediate PaletteWShapeNames (show only)");
        testPaletteWShapeNamesItem.addActionListener(e -> testPaletteWShapeNames());
        palettesMenu.add(testPaletteWShapeNamesItem);
        
        JMenuItem savePaletteWShapeNamesItem = new JMenuItem("Save PaletteWShapeNames");
        savePaletteWShapeNamesItem.addActionListener(e -> savePaletteWShapeNames());
        palettesMenu.add(savePaletteWShapeNamesItem);
        
        JMenuItem openPaletteWShapeNamesItem = new JMenuItem("Open PaletteWShapeNames");
        openPaletteWShapeNamesItem.addActionListener(e -> openPaletteWShapeNames());
        palettesMenu.add(openPaletteWShapeNamesItem);
        
        menuBar.add(palettesMenu);

        // Create Color-sources menu
        JMenu colorSourcesMenu = new JMenu("Color-sources");
        
        JMenuItem savedPaletteItem = new JMenuItem("Saved Palette as color-source");
        savedPaletteItem.addActionListener(e -> colorSourceSavedPalette());
        colorSourcesMenu.add(savedPaletteItem);
        
        JMenuItem colorSourcePaletteItem = new JMenuItem("Palette from saved Template");
        colorSourcePaletteItem.addActionListener(e -> colorSourcePaletteFromSavedTemplate());
        colorSourcesMenu.add(colorSourcePaletteItem);
        
        JMenuItem mosaicPaletteBuilderItem = new JMenuItem("Open mosaic Palette-builder");
        mosaicPaletteBuilderItem.addActionListener(e -> openMosaicPaletteBuilder());
        colorSourcesMenu.add(mosaicPaletteBuilderItem);

        JMenuItem paletteVariationsItem = new JMenuItem("Palette variations");
        paletteVariationsItem.addActionListener(e -> openPaletteVariations());
        colorSourcesMenu.add(paletteVariationsItem);

        JMenuItem createSmartPicItem = new JMenuItem("Create SmartPic");
        createSmartPicItem.addActionListener(e -> openCreateSmartPic());
        colorSourcesMenu.add(createSmartPicItem);

        JMenuItem relationMatrixEditorItem = new JMenuItem("Relation Matrix Editor");
        relationMatrixEditorItem.addActionListener(e -> openRelationMatrixEditor());
        colorSourcesMenu.add(relationMatrixEditorItem);
        
        menuBar.add(colorSourcesMenu);


        // Create Selection menu
        JMenu selectionMenu = new JMenu("Selection");

        JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        selectAllItem.addActionListener(e -> selectAllDrawObjs());

        JMenuItem deselectAllItem = new JMenuItem("Deselect All");
        deselectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        deselectAllItem.addActionListener(e -> deselectAllDrawObjs());

        JMenuItem selectByHueRangeItem = new JMenuItem("Select by hue-range");
        selectByHueRangeItem.addActionListener(e -> openHueRelativesFrame());

        JMenuItem selectionGroupItem = new JMenuItem("Create or Replace Selection Group");
        selectionGroupItem.addActionListener(e -> openSelectionGroupChoiceFrame());

        selectionMenu.add(selectAllItem);
        selectionMenu.add(deselectAllItem);
        selectionMenu.add(new JSeparator());
        selectionMenu.add(selectByHueRangeItem);
        selectionMenu.add(new JSeparator());
        selectionMenu.add(selectionGroupItem);

        menuBar.add(selectionMenu);

        // Create SelectionSort menu for sorting display lists
        JMenu selectionSortMenu = new JMenu("SelectionSort");
        
        // Create strategy instances
        DrawObjSortStrategy azStrategy = new AlphabeticalAZStrategy();
        DrawObjSortStrategy zaStrategy = new AlphabeticalZAStrategy();
        DrawObjSortStrategy hueAscendingStrategy = new HueAscendAZAlphabetizeStratg();
        DrawObjSortStrategy hueAscendSaturDescendStrategy = new HueAscendSaturDescendStratg();
        DrawObjSortStrategy hueAscendRGBLumDescendStrategy = new HueAscendRGBLumDescendStratg();
        DrawObjSortStrategy originalStrategy = new OriginalOrderStrategy();
        
        // "All DrawObjs" submenu
        JMenu allDrawObjsMenu = new JMenu("All DrawObjs");
        JMenuItem allAZItem = new JMenuItem("A-Z");
        allAZItem.addActionListener(e -> setSortStrategyAllDrawObjs(azStrategy));
        JMenuItem allZAItem = new JMenuItem("Z-A");
        allZAItem.addActionListener(e -> setSortStrategyAllDrawObjs(zaStrategy));
        JMenuItem allOriginalItem = new JMenuItem("original Template order");
        allOriginalItem.addActionListener(e -> setSortStrategyAllDrawObjs(originalStrategy));
        allDrawObjsMenu.add(allAZItem);
        allDrawObjsMenu.add(allZAItem);
        allDrawObjsMenu.add(allOriginalItem);
        
        // "Selected DrawObjs" submenu
        JMenu selectedDrawObjsMenu = new JMenu("Selected DrawObjs");
        JMenuItem selectedAZItem = new JMenuItem("A-Z");
        selectedAZItem.addActionListener(e -> setSortStrategySelectedDrawObjs(azStrategy));
        JMenuItem selectedZAItem = new JMenuItem("Z-A");
        selectedZAItem.addActionListener(e -> setSortStrategySelectedDrawObjs(zaStrategy));
        JMenuItem selectedByHueItem = new JMenuItem("by Hue > AZ");
        selectedByHueItem.addActionListener(e -> setSortStrategySelectedDrawObjs(hueAscendingStrategy));
        JMenuItem selectedByHueSaturDescendItem = new JMenuItem("by Hue > Satur descend");
        selectedByHueSaturDescendItem.addActionListener(e -> setSortStrategySelectedDrawObjs(hueAscendSaturDescendStrategy));
        JMenuItem selectedByHueRGBLumDescendItem = new JMenuItem("by Hue RGBLum descend");
        selectedByHueRGBLumDescendItem.addActionListener(e -> setSortStrategySelectedDrawObjs(hueAscendRGBLumDescendStrategy));
        JMenuItem selectedOriginalItem = new JMenuItem("original Template order");
        selectedOriginalItem.addActionListener(e -> setSortStrategySelectedDrawObjs(originalStrategy));
        selectedDrawObjsMenu.add(selectedAZItem);
        selectedDrawObjsMenu.add(selectedZAItem);
        selectedDrawObjsMenu.add(selectedByHueItem);
        selectedDrawObjsMenu.add(selectedByHueSaturDescendItem);
        selectedDrawObjsMenu.add(selectedByHueRGBLumDescendItem);
        selectedDrawObjsMenu.add(selectedOriginalItem);
        
        selectionSortMenu.add(allDrawObjsMenu);
        selectionSortMenu.add(selectedDrawObjsMenu);
        
        menuBar.add(selectionSortMenu);

        // Create Hue Circle menu for AnchorPattern workflow
        JMenu hueCircleMenu = new JMenu("Hue Circle");
        
        JMenuItem seeOccupiedHuesItem = new JMenuItem("See occupied hues");
        seeOccupiedHuesItem.addActionListener(e -> openAnchorPatternFrameInstance1());
        hueCircleMenu.add(seeOccupiedHuesItem);
        
        JMenuItem createAnchorPatternItem = new JMenuItem("Create AnchorPattern intelligence");
        createAnchorPatternItem.addActionListener(e -> startUC040Workflow());
        hueCircleMenu.add(createAnchorPatternItem);
        
        menuBar.add(hueCircleMenu);

        setJMenuBar(menuBar);
    } // end method initMenuBar




    /**
     * Creates a PaletteExplicit for the given Template and attaches it to the Template's DrawObjRoster.
     * Sets paletteExplicit to null if there is no Template or roster.
     * @param template The Template the new PaletteExplicit will pull from
     */
    private void attachNewPaletteExplicit(Template template) {
        if (template == null || template.getDrawObjRoster() == null) {
            this.paletteExplicit = null;
            return;
        }
        // The PaletteExplicit is bound to one Template's roster, so a new one is needed for each Template
        this.paletteExplicit = new PaletteExplicit(template);
        template.getDrawObjRoster().setPaletteExplicit(this.paletteExplicit);
    } // end method attachNewPaletteExplicit



    /**
     * Creates a new TemplateWorkFrame with a Template.
     * @param template The Template to work with
     */
    public TemplateWorkFrame(ApplicationMediator mediator, Template template) {
        this(mediator);
        // Full load path (same as file-open): initializes displayFacade,
        // selectionDisplayManager, ShapeSelectionController, PaletteOperationHub
        // and the DrawObjs list — the previous partial sequence left the
        // all-DrawObjs list empty (displayFacade was null).
        updateUIWithTemplate(template);
        setVisible(true);
    } // end constructor




    private void showSelectedObjectInfo() {
        if (singleSelectedDrawObj != null) {
            DrawObjStateViewController controller = new DrawObjStateViewController(this, singleSelectedDrawObj);
            registerDrawObjStateController(controller);
        } else {
            showMessageAtBottom(this, "Please select an object first", "No Selection", JOptionPane.INFORMATION_MESSAGE);
        }
    } // end method showSelectedObjectInfo




    private void showCurrFocusObjectInfo() {
        DrawObj selectedDrawObj = listSelectDrawObjsPanel.getSelectedDrawObj();
        if (selectedDrawObj != null) {
            DrawObjStateViewController controller = new DrawObjStateViewController(this, selectedDrawObj);
            registerDrawObjStateController(controller);
        } else {
            showMessageAtBottom(this, "Please select an object first", "No Selection", JOptionPane.INFORMATION_MESSAGE);
        }
    } // end method showCurrFocusObjectInfo




    private void loadCurrTemplate() {
        if (currTemplate != null) {
            displayDrawObjsInList();
            displayDrawObjsInShapesPanel();
        }
    } // end method loadCurrTemplate




    /**
     * Displays the current Template's DrawObjs in the list panel.
     * Applies the current sort strategy for the all DrawObjs list.
     */
    private void displayDrawObjsInList() {
        if (currTemplate != null && displayFacade != null) {
            List<DrawObj> sortedDrawObjs = displayFacade.getAllDrawObjsSorted(allDrawObjsStrategy);
            DrawObj[] drawObjArray = sortedDrawObjs.toArray(new DrawObj[0]);
            System.out.println("021926 SELECTION SORT ORDER - displayDrawObjsInList() - Displaying " + drawObjArray.length + 
                " objects with strategy: " + allDrawObjsStrategy.getDisplayLabel());
            
            // Log current selection state before updating display
            if (currTemplate.getDrawObjRoster() != null) {
                Set<DrawObj> currentSelection = currTemplate.getDrawObjRoster().getSelectionGroup().getSelectedDrawObjsSet();
                System.out.println("021926 SELECTION SORT ORDER - displayDrawObjsInList() - Current selection has " + currentSelection.size() + " objects");
            }
            
            // DO NOT pass sorted array to controller - it needs drawing order for hit-testing
            // Only pass sorted array to list for display
            listAllDrawObjsPanel.setDrawObjs(drawObjArray);
            listAllDrawObjsPanel.syncWithRosterSelection();
            selectionDisplayManager.refreshDisplay();
        }
    }

    /**
     * Displays the current Template's DrawObjs in the shapes panel.
     */
    private void displayDrawObjsInShapesPanel() {
        if (currTemplate != null) {
            DrawObj[] drawObjs = currTemplate.getAllDrawObjs().toArray(new DrawObj[0]);
            shapesPanel.setDrawObjs(drawObjs);
            // Controller must have unsorted array (drawing order) for correct hit-testing
            controller.setDrawObjs(drawObjs);
            shapesPanel.repaint();
        }
    }

    /**
     * Sets the sort order for the "All DrawObjs" list and refreshes the display.
     * 
     * @param sortOrder The sort order to apply
     */
    private void setSortOrderAllDrawObjs(com.csurf.templatecore.model.SortOrder sortOrder) {
        if (this.allDrawObjsSortOrder != sortOrder) {
            System.out.println("021926 SELECTION SORT ORDER - setSortOrderAllDrawObjs() - Changing sort order from " + 
                this.allDrawObjsSortOrder + " to " + sortOrder);
            this.allDrawObjsSortOrder = sortOrder;
            displayDrawObjsInList();  // Refresh with new sort order
        }
    }

    /**
     * Sets the sort order for the "Selected DrawObjs" list and refreshes the display.
     * 
     * @param sortOrder The sort order to apply
     */
    private void setSortOrderSelectedDrawObjs(com.csurf.templatecore.model.SortOrder sortOrder) {
        if (this.selectedDrawObjsSortOrder != sortOrder) {
            this.selectedDrawObjsSortOrder = sortOrder;
            selectionDisplayManager.setSortOrder(sortOrder);
        }
    }

    /**
     * Sets the sort strategy for the "All DrawObjs" list and refreshes the display.
     * 
     * This is the new strategy-based API that replaces setSortOrderAllDrawObjs().
     * The frame owns the strategy and passes it explicitly to the facade.
     * 
     * @param strategy The DrawObjSortStrategy to apply
     */
    private void setSortStrategyAllDrawObjs(DrawObjSortStrategy strategy) {
        if (strategy != null) {
            this.allDrawObjsStrategy = strategy;
            displayDrawObjsInList();  // Refresh with new sort strategy
        }
    }

    /**
     * Sets the sort strategy for the "Selected DrawObjs" list and refreshes the display.
     * 
     * This is the new strategy-based API that replaces setSortOrderSelectedDrawObjs().
     * 
     * @param strategy The DrawObjSortStrategy to apply
     */
    private void setSortStrategySelectedDrawObjs(DrawObjSortStrategy strategy) {
        if (strategy != null) {
            selectionDisplayManager.setSortStrategy(strategy);
        }
    }

    private void createSmartPic() {
        // TODO: Implement SmartPic creation
    }

    private void openTemplateMenuItem() {
        JFileChooser fileChooser = new JFileChooser(storage.getStorageDir().toFile());
        fileChooser.setDialogTitle("Open Template");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Template Files (*.json)", "json"));
        
        positionDialogAtBottomRight(fileChooser);
        
        boolean keepDialogOpen = true;
        
        while (keepDialogOpen) {
            int result = fileChooser.showOpenDialog(this);
            
            if (result == JFileChooser.APPROVE_OPTION) {
                // User selected a file and clicked "Open"
                openTemplate(fileChooser.getSelectedFile());
                // Continue the loop to keep dialog open
            } else {
                // User clicked "Cancel" or closed the dialog
                keepDialogOpen = false;
            }
        }
    } // end method openTemplateMenuItem

    /**
     * Opens the palette-variations feature: user picks a serialized Template,
     * and a VariationDisplayFrame shows 12 renderings of it.
     * Each invocation creates a new controller + frame (multi-window support).
     */
    private void openPaletteVariations() {
        new VariationDisplayController(mediator, this).launch();
    } // end method openPaletteVariations


    /**
     * Opens the create-SmartPic feature: a CreateSmartPicFrame lets the user
     * pick a saved Template and an AGprefs defaults file, then build a
     * SmartPic via SmartPicFactory. Each invocation creates a new
     * controller + frame (multi-window support).
     */
    private void openCreateSmartPic() {
        new CreateSmartPicController(mediator, this).launch();
    } // end method openCreateSmartPic


    /**
     * Opens the relation-matrix editor: a RelationMatrixEditorFrame lets
     * the user load a saved Template plus an AGprefs defaults file, edit
     * and re-bind the prefs rows, and save a formatVersion 2
     * (UUID-keyed) defaults file. Each invocation creates a new
     * controller + frame (multi-window support).
     */
    private void openRelationMatrixEditor() {
        new RelationMatrixEditorController(mediator, this).launch();
    } // end method openRelationMatrixEditor



    public boolean openTemplate(File templateFile) {
        try {
            String fileName = templateFile.getName();
            if (fileName.endsWith(".json")) {
                fileName = fileName.substring(0, fileName.length() - 5);
            }
            Template template = storage.loadTemplate(fileName);
            updateUIWithTemplate(template); 
            return true;
        } catch (TemplateStorageException e) {
            showMessageAtBottom(this, "Error loading template: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    } // end method openTemplate



    private void saveTemplateMenuItem() {
        if (currTemplate == null) {
            showMessageAtBottom(this, "No template to save", "Save Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser(storage.getStorageDir().toFile());
        fileChooser.setDialogTitle("Save Template");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Template Files (*.json)", "json"));
        
        // Use the palette name as the default filename if a palette was loaded
        if (isPaletteWasLoaded() && getLastPaletteName() != null && !getLastPaletteName().isEmpty()) {
            fileChooser.setSelectedFile(new File(getLastPaletteName() + ".json"));
        } else {
            fileChooser.setSelectedFile(new File(currTemplate.getTheTemplateName() + ".json"));
        }
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                // Get selected file and ensure it has .json extension
                File selectedFile = fileChooser.getSelectedFile();
                if (!selectedFile.getName().toLowerCase().endsWith(".json")) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + ".json");
                }
                
                // Check if file already exists and confirm overwrite
                if (selectedFile.exists()) {
                    int overwrite = JOptionPane.showConfirmDialog(this,
                            "File already exists. Do you want to overwrite it?",
                            "Confirm Overwrite",
                            JOptionPane.YES_NO_OPTION);
                    if (overwrite != JOptionPane.YES_OPTION) {
                        return; // User declined overwrite
                    }
                }
                
                String filePath = selectedFile.getPath();
                String name = selectedFile.getName();
                if (name.endsWith(".json")) {
                    name = name.substring(0, name.length() - 5); 
                }
                storage.saveTemplate(currTemplate, name);
                showMessageAtBottom(this, "Template saved successfully", "Save Success", JOptionPane.INFORMATION_MESSAGE);
                
                // Update palette name to match the saved template name
                setLastPaletteName(name);
                setPaletteWasLoaded(true);
                
                // Create a temporary PaletteWName to display the palette name
                PaletteWName tempPalette = new PaletteWName();
                tempPalette.setPaletteName(name);
                displayPaletteName(tempPalette);
            } catch (TemplateStorageException e) {
                showMessageAtBottom(this, "Error saving template: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void exportToSVGMenuItem() {
        if (currTemplate == null) {
            showMessageAtBottom(this, "No template to export", "Export Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Template to SVG");
        fileChooser.setFileFilter(new FileNameExtensionFilter("SVG Files (*.svg)", "svg"));
        
        // Use the palette name as the default filename if a palette was loaded
        if (isPaletteWasLoaded() && getLastPaletteName() != null && !getLastPaletteName().isEmpty()) {
            fileChooser.setSelectedFile(new File(getLastPaletteName() + ".svg"));
        } else {
            fileChooser.setSelectedFile(new File(currTemplate.getTheTemplateName() + ".svg"));
        }
        
        positionDialogAtBottomRight(fileChooser);
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = fileChooser.getSelectedFile();
                // Ensure the file has .svg extension
                if (!selectedFile.getName().toLowerCase().endsWith(".svg")) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + ".svg");
                }
                
                currTemplate.exportToSVG(selectedFile);
                
                showMessageAtBottom(this, "Template exported to SVG successfully", "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                showMessageAtBottom(this, "Error exporting template to SVG: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void updateUIWithTemplate(Template template) { 
        // Store old template to properly handle cleanup
        Template oldTemplate = currTemplate;
        
        // Update the SelectionGroupChoiceFrame if it's open
        boolean selectionGroupFrameWasOpen = false;
        if (selectionGroupChoiceFrame != null && selectionGroupChoiceFrame.isDisplayable()) {
            selectionGroupFrameWasOpen = true;
            // We'll update it after setting the new template
        } else {
            // Close it if it exists but isn't displayable
            closeSelectionGroupChoiceFrame();
        }
        
        // Close any open HueRelativesFrame to ensure it's not displaying
        // data from the previous Template
        closeHueRelativesFrames();
        
        // Detach PaletteExplicit from the previous Template's roster, then attach a new one to the new Template
        if (paletteExplicit != null && oldTemplate != null && oldTemplate.getDrawObjRoster() != null
                && oldTemplate.getDrawObjRoster().getPaletteExplicit() == paletteExplicit) {
            oldTemplate.getDrawObjRoster().setPaletteExplicit(null);
        }
        attachNewPaletteExplicit(template);
        
        // Update current template reference 
        currTemplate = template;
        
        // A new template is now current, so the palette-load provenance clock restarts:
        // "noLoad" means no palette-load operation has occurred since this template was opened.
        lastPaletteLoadCameFromFile = "noLoad";
        
        // Update template name text field
        if (currTemplate != null) {
            currTemplateNameTextfield.setText("Template: " + currTemplate.getTheTemplateName());
        } else {
            currTemplateNameTextfield.setText("No Template Loaded");
        }
        
        // Initialize display facade for sorting support
        if (currTemplate != null && currTemplate.getDrawObjRoster() != null) {
            displayFacade = new SelectionGroupListFacade(currTemplate.getDrawObjRoster(), currTemplate);
        } else {
            displayFacade = null;
        }
        selectionDisplayManager.attachToRoster(
            currTemplate != null ? currTemplate.getDrawObjRoster() : null,
            displayFacade
        );
        
        // Reset sort orders to default when template changes
        allDrawObjsSortOrder = com.csurf.templatecore.model.SortOrder.ORIGINAL_ORDER;
        selectedDrawObjsSortOrder = com.csurf.templatecore.model.SortOrder.ORIGINAL_ORDER;
        
        // Register this frame as a listener for PositionalPaletteEvent
        if (currTemplate != null && currTemplate.getDrawObjRoster() != null) {
            DrawObjRoster roster = currTemplate.getDrawObjRoster();
            roster.addPositionalPaletteEventListener(this);
            roster.clearSelection();
            
            // Connect ShapeSelectionController with DrawObjRoster for shift-click multi-selection
            controller.setDrawObjRoster(roster);
            
            // Register this frame as a selection event listener
            roster.addSelectionListener(this);
        }
        
        // Create and initialize PaletteOperationHub for orchestrating recoloring operations
        if (currTemplate != null && currTemplate.getDrawObjRoster() != null) {
            DrawObjRoster roster = currTemplate.getDrawObjRoster();
            
            // Create hub with dependencies
            paletteOperationHub = new PaletteOperationHub(
                roster,
                new TemplatePaletteShift(),
                this
            );
            
            // Set hub in roster so it can delegate recoloring operations
            roster.setPaletteOperationHub(paletteOperationHub);
        }
        
        // Update UI components with template data
        DrawObj[] drawObjArray = template.getAllDrawObjs().toArray(new DrawObj[0]);
        
        // First add all DrawObjs to the roster
        if (currTemplate != null && currTemplate.getDrawObjRoster() != null) {
            DrawObjRoster roster = currTemplate.getDrawObjRoster();
            for (DrawObj obj : drawObjArray) {
            }
        }
        
        controller.setDrawObjs(drawObjArray);    
        listAllDrawObjsPanel.setDrawObjs(drawObjArray);
        shapesPanel.setDrawObjs(drawObjArray);   

        // Update currOstPositions from DrawObjRoster
        if (currTemplate != null && currTemplate.getDrawObjRoster() != null) {
            currOstPositions = currTemplate.getDrawObjRoster().getAllOstPositionsForCurrPalette();
        }
        
        // Update all associated MonotriWorkFrames
        for (MonotriWorkFrame frame : linkedMonoTriFrames) {
            frame.getDrawPanel().updatePanelDisplayStates();
        }

        shapesPanel.repaint();                  
        
        // Update the SelectionGroupChoiceFrame with the new template if it was open
        if (selectionGroupFrameWasOpen && selectionGroupChoiceFrame != null) {
            selectionGroupChoiceFrame.updateWithCurrentTemplate();
            System.out.println("DEBUG: Updated SelectionGroupChoiceFrame with new template: " + 
                              (currTemplate != null ? currTemplate.getTheTemplateName() : "null"));
        }
        
        // Capture the load-time baseline (B0) last, once the DrawObjs are in place and show their starting colors
        if (paletteOperationHub != null) {
            paletteOperationHub.initPaletteBookmarks();
        }
    } 

    /**
     * Opens a new MonotriWorkFrame linked to this TemplateWorkFrame.
     * Uses the hue of the currently selected DrawObj, or defaults to internal hue 29 (displayed as 30).
     */
    private void openMonoTriFrameLinked() {
        int hueNumCode = 29; 

        if (singleSelectedDrawObj != null && singleSelectedDrawObj.getCurrentColor() != null) {
            ColorPtOstDot color = singleSelectedDrawObj.getCurrentColor();
            hueNumCode = color.getStdCodeHueNum(); 
            System.out.println("DEBUG: Opening MonotriWorkFrame with internal hue: " + hueNumCode + " (GUI hue: " + (hueNumCode + 1) + ")");
        } 

        HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(hueNumCode + 1);
        MonotriWorkFrame newFrame = new MonotriWorkFrame(this, someHueNumGUI);
        
        activeMonoTriFrame = newFrame;
        System.out.println("DEBUG: New MonotriWorkFrame created and set as active");
        
        newFrame.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                activeMonoTriFrame = newFrame;
                System.out.println("DEBUG: MonotriWorkFrame gained focus, now active");
            }
            @Override
            public void windowLostFocus(WindowEvent e) {
            }
        });
        
        newFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                linkedMonoTriFrames.remove(newFrame);
                if (activeMonoTriFrame == newFrame) {
                    activeMonoTriFrame = linkedMonoTriFrames.isEmpty() ? null : linkedMonoTriFrames.get(0);
                }
                System.out.println("DEBUG: MonotriWorkFrame closed, active frame updated");
            }
        });
        
        linkedMonoTriFrames.add(newFrame);
        newFrame.setVisible(true);
        System.out.println("DEBUG: New MonotriWorkFrame created and shown");
    } 

    /**
     * Opens a MonotriWorkFrame with the hue of the currently selected DrawObj.
     * If no DrawObj is selected, uses default hue 31 and shows a message.
     * This is called when the user double-clicks on the clickColorSquare.
     */
    private void openMonotriWorkFrameWithCurrentHue() {
        if (singleSelectedDrawObj != null) {
            // Get color from selected DrawObj
            ColorPtOstDot colorPt = singleSelectedDrawObj.getColorPtOstDotFillColor();
            if (colorPt != null) {
                // Extract hue and open MonotriWorkFrame
                OstHueColumnStep hcs = colorPt.getOstHCS();
                int stdCodeHueNum = hcs.getStdCodeHueNum();
                HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(stdCodeHueNum + 1);
                
                // Open MonotriWorkFrame with this hue
                MonotriWorkFrame monotriWorkFrame = new MonotriWorkFrame(this, hueNumGUI, colorPt);
                monotriWorkFrame.setVisible(true);
            }
        } else {
            // No selection, use default hue 31
            showMessageAtBottom(this, "No shape is currently selected/active. Using default hue = 31", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            
            // Create HueNumGUI for hue 31
            HueNumGUI defaultHueNumGUI = HueNumGUI.getHueNumGUIInstance(31);
            
            // Open MonotriWorkFrame with default hue
            MonotriWorkFrame monotriWorkFrame = new MonotriWorkFrame(this, defaultHueNumGUI);
            monotriWorkFrame.setVisible(true);
        }
    }  // end method openMonotriWorkFrameWithCurrentHue



    /**
     * Opens a MonotriWorkFrame with the hue of the currently selected DrawObj in listSelectDrawObjsPanel
     * If no DrawObj is selected, uses default hue 31 and shows a message.
     * This is called when the user double-clicks on the currChosenDrawObjSwatch.
     */
    private void openMonotriWorkFrameWCurrFocusSwatch() {
        // Get the DrawObj from listSelectDrawObjsPanel (not singleSelectedDrawObj)
        DrawObj selectedDrawObj = listSelectDrawObjsPanel.getSelectedDrawObj();
        
        if (selectedDrawObj != null) {
            // Get color from selected DrawObj
            ColorPtOstDot colorPt = selectedDrawObj.getColorPtOstDotFillColor();
            if (colorPt != null) {
                // Extract hue and open MonotriWorkFrame
                OstHueColumnStep hcs = colorPt.getOstHCS();
                int stdCodeHueNum = hcs.getStdCodeHueNum();
                HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(stdCodeHueNum + 1);

                // Open MonotriWorkFrame with this hue
                MonotriWorkFrame monotriWorkFrame = new MonotriWorkFrame(this, hueNumGUI, colorPt);
                monotriWorkFrame.setVisible(true);
            }
        } else {
            // No selection, use default hue 31
            showMessageAtBottom(this, "No shape is currently selected/active. Using default hue = 31", "No Selection", JOptionPane.INFORMATION_MESSAGE);

            // Create HueNumGUI for hue 31
            HueNumGUI defaultHueNumGUI = HueNumGUI.getHueNumGUIInstance(31);

            // Open MonotriWorkFrame with default hue
            MonotriWorkFrame monotriWorkFrame = new MonotriWorkFrame(this, defaultHueNumGUI);
            monotriWorkFrame.setVisible(true);
        }
    }  // end method openMonotriWorkFrameWCurrFocusSwatch




    /**
     * Closes all linked MonoTriDumbFrames when this frame is closing.
     */
    private void closeAllLinkedFrames() {
        List<MonotriWorkFrame> framesToClose = new ArrayList<>(linkedMonoTriFrames);
        for (MonotriWorkFrame frame : framesToClose) {
            frame.dispose();
        }
        linkedMonoTriFrames.clear();
        activeMonoTriFrame = null;
        
        // Close all DrawObjStateFrames
        List<DrawObjStateViewController> controllersToClose = new ArrayList<>(drawObjStateControllers);
        for (DrawObjStateViewController controller : controllersToClose) {
            controller.close();
        }
        drawObjStateControllers.clear();
    }

    /**
     * Gets the currently active MonotriWorkFrame that serves as the ColorSource.
     * @return The active MonotriWorkFrame instance, or null if none exists
     */
    public MonotriWorkFrame getActiveMonoTriFrame() {
        return activeMonoTriFrame;
    }

    /**
     * Gets the array of currently occupied OstPositions for this Template.
     * Used by associated MonotriWorkFrames to determine which positions to display.
     */
    public OstPosition[] getCurrOstPositions() {
        return currOstPositions;
    }

    private void cleanup() {
        closeAllLinkedFrames();
        if (activeMonoTriFrame != null) {
            activeMonoTriFrame.dispose();
            activeMonoTriFrame = null;
        }
        
        // Close any open SelectionGroupChoiceFrame
        closeSelectionGroupChoiceFrame();
        
        // Close any open HueRelativesFrame
        closeHueRelativesFrames();
        
        dispose();
    }

    /**
     * Sets the color display mode (RGB or CMYK) for this TemplateWorkFrame.
     * Updates the UI state and triggers a repaint of the shapes panel.
     * @param mode The color display mode to set
     */
    public void setColorMode(ColorDisplayMode mode) {
        this.colorDisplayMode = mode;
        
        if (shapesPanel != null) {
            shapesPanel.repaint();
        }
    } // end method setColorMode




    /**
     * Gets the current color display mode.
     * @return The current ColorDisplayMode
     */
    public ColorDisplayMode getColorMode() {
        return colorDisplayMode;
    }

    /**
     * Adds a listener for positional palette events.
     * 
     * @param listener The listener to add
     */
    public void addPositionalPaletteEventListener(IPositionalPaletteEventListener listener) {
        if (listener != null && !paletteEventListeners.contains(listener)) {
            paletteEventListeners.add(listener);
        }
    }

    /**
     * Removes a listener for positional palette events.
     * 
     * @param listener The listener to remove
     */
    public void removePositionalPaletteEventListener(IPositionalPaletteEventListener listener) {
        paletteEventListeners.remove(listener);
    }

    /**
     * Public method to fire a positional palette event.
     * This allows model classes like DrawObjRoster to fire events.
     * 
     * @param event The event to fire
     */
    public void firePositionalPaletteEventPublic(PositionalPaletteEvent event) {
        firePositionalPaletteEvent(event);
    }

    /**
     * Fires a positional palette event to all registered listeners.
     * 
     * @param event The event to fire
     */
    private void firePositionalPaletteEvent(PositionalPaletteEvent event) {
        System.out.println("DEBUG: TemplateWorkFrame firing PositionalPaletteEvent of type: " + event.getEventType());
        List<IPositionalPaletteEventListener> listenersCopy = 
            new ArrayList<>(paletteEventListeners);
        
        for (IPositionalPaletteEventListener listener : listenersCopy) {
            listener.handlePositionalPaletteEvent(event);
        }
    }

    /**
     * Selects all DrawObjs in the current template.
     */
    private void selectAllDrawObjs() {
        if (currTemplate == null) {
            return;
        }
        
        DrawObjRoster roster = currTemplate.getDrawObjRoster();
        if (roster == null) {
            return;
        }
        
        roster.clearSelection();
        
        for (DrawObj drawObj : currTemplate.getAllDrawObjs()) {
            roster.addToSelection(drawObj);
        }
        
        roster.updateToCurrObjState();
        
        listAllDrawObjsPanel.clearSelection();
        shapesPanel.repaint(); 
        
        firePositionalPaletteEvent(new PositionalPaletteEvent(
            this, 
            PositionalPaletteEvent.EventType.SELECTION_CHANGE, 
            roster
        ));
    }

    /**
     * Deselects all currently selected DrawObjs.
     */
    private void deselectAllDrawObjs() {
        if (currTemplate == null) {
            return;
        }
        
        DrawObjRoster roster = currTemplate.getDrawObjRoster();
        if (roster == null) {
            return;
        }
        
        roster.clearSelection();
        
        roster.updateToCurrObjState();
        
        listAllDrawObjsPanel.clearSelection();
        setSingleSelectedDrawObj(null); 
        
        firePositionalPaletteEvent(new PositionalPaletteEvent(
            this, 
            PositionalPaletteEvent.EventType.SELECTION_CHANGE, 
            roster
        ));
        
        shapesPanel.repaint();
    } // end method deselectAllDrawObjs

    @Override
    public void handlePositionalPaletteEvent(PositionalPaletteEvent event) {
        // Update the UI based on the event type
        switch (event.getEventType()) {
            case COLOR_CHANGE:
                // Update the shape display panel to reflect color changes
                shapesPanel.repaint();
                
                // Update the shape list panel to reflect color changes
                listAllDrawObjsPanel.repaint();
                break;
                
            case SELECTION_CHANGE:
                // Already handled by other mechanisms
                // Events from DrawObjRoster (e.g. HueRelativesFrame plus/minus buttons) must be forwarded
                // to this frame's listeners such as MonotriWorkFrame. Events fired by this frame were already dispatched.
                if (event.getSource() != this) {
                    for (IPositionalPaletteEventListener listener : new ArrayList<>(paletteEventListeners)) {
                        if (listener != this) {
                            listener.handlePositionalPaletteEvent(event);
                        }
                    }
                }
                break;
                
            default:
                // Handle other event types as needed
                break;
        }
    }

    /**
     * Delivers a hue update to the designated HueRelativesFrame receiver.
     * This is the single delivery path for hue updates: callers supply the hue
     * and this TemplateWorkFrame owns receiver selection.
     * If no frame is currently designated (no receiverCheckBox is checked),
     * this method has no effect.
     * 
     * @param hueNum The HueNumGUI instance containing the hue value to deliver
     */
    public void deliverHueUpdate(HueNumGUI hueNum) {
        if (hueNum == null) {
            return;
        }
        
        // Deliver only to the designated receiver, if it is still registered
        if (designatedHueReceiver != null && hueRelativesFrames.contains(designatedHueReceiver)) {
            designatedHueReceiver.receiveHueUpdate(hueNum);
            return;
        }
        designatedHueReceiver = null;  // Clear stale designation reference
        // No designated receiver - the update is intentionally dropped
    } // end method deliverHueUpdate()




    /**
     * Opens a new HueRelativesFrame using the hue of the currently focused DrawObj.
     * Called when sendHueRSideButton is clicked.
     * Uses the selection-list selection if present, otherwise singleSelectedDrawObj.
     */
    private void openHueRelativesFrameWithSelectedHue() {
        // First check if there's a selection in the selected objects list
        DrawObj selectedFromList = getSelectedDrawObjFromSelectionList();
        
        // Use the selected object from the list if available, otherwise fall back to singleSelectedDrawObj
        DrawObj drawObjToUse = (selectedFromList != null) ? selectedFromList : singleSelectedDrawObj;
        
        if (drawObjToUse != null) {
            openHueRelativesFrame(drawObjToUse);
        }
    } // end method openHueRelativesFrameWithSelectedHue()




    /**
     * Registers a DrawObjStateViewController with this TemplateWorkFrame.
     * This allows automatic cleanup when the TemplateWorkFrame is closed.
     * 
     * @param controller The DrawObjStateViewController to register
     */
    public void registerDrawObjStateController(DrawObjStateViewController controller) {
        if (controller != null && !drawObjStateControllers.contains(controller)) {
            drawObjStateControllers.add(controller);
        }
    }
    
    /**
     * Unregisters a DrawObjStateViewController from this TemplateWorkFrame.
     * This should be called when the DrawObjStateFrame is closed.
     * 
     * @param controller The DrawObjStateViewController to unregister
     */
    public void unregisterDrawObjStateController(DrawObjStateViewController controller) {
        drawObjStateControllers.remove(controller);
    }

    /**
     * Gets the list of HueRelativesFrames currently managed by this TemplateWorkFrame.
     * The returned list is unmodifiable; registration is handled internally by
     * openHueRelativesFrame(int).
     * 
     * @return An unmodifiable view of the HueRelativesFrames list
     */
    public List<HueRelativesFrame> getHueRelativesFrames() {
        return Collections.unmodifiableList(hueRelativesFrames);
    }

    /**
     * Implementation of ShapeListPanel.HueRelativesButtonListener
     * Called when the "See HueRelatives" button is clicked
     */
    @Override
    public void hueRelativesButtonClicked() {
        openHueRelativesFrame();
    }

    /**
     * Implementation of ShapeListPanel.SelectionListener
     * Called when selection changes in the listAllDrawObjsPanel
     */
    @Override
    public void selectionChanged(DrawObj selectedDrawObj) {
        System.out.println("TemplateWorkFrame: Selection changed to " + selectedDrawObj);
        setSingleSelectedDrawObj(selectedDrawObj);
        if (selectedDrawObj != null) {
            System.out.println("TemplateWorkFrame: Updated clickColorSquare with color " + selectedDrawObj.getCurrentColor());
        } else {
            System.out.println("TemplateWorkFrame: Cleared clickColorSquare (no selection)");
        }
        
        // Fire SELECTION_CHANGE event to notify listeners (e.g., MonotriWorkFrame) of the selection change
        if (currTemplate != null && currTemplate.getDrawObjRoster() != null) {
            firePositionalPaletteEvent(new PositionalPaletteEvent(
                this,
                PositionalPaletteEvent.EventType.SELECTION_CHANGE,
                currTemplate.getDrawObjRoster()
            ));
        }
    }

    /**
     * Updates the clickColorSquare with the given color.
     * If the color is null, resets the square to white.
     * 
     * @param color The color to update the square with, or null to reset to white
     */
    public void updateClickColorSquare(ColorPtOstDot color) {
        if (color != null) {
            clickColorSquare.setBackground(color.getAsColor());
        } else {
            clickColorSquare.setBackground(Color.WHITE);
        }
        clickColorSquare.repaint();
    } // end method updateClickColorSquare

    /**
     * Encapsulated update of the single-object focus: sets singleSelectedDrawObj
     * and updates clickColorSquare to match the object's current color.
     * Does not fire any events.
     *
     * @param drawObj The DrawObj to focus, or null to clear focus
     */
    public void setSingleSelectedDrawObj(DrawObj drawObj) {
        this.singleSelectedDrawObj = drawObj;
        updateClickColorSquare(drawObj != null ? drawObj.getCurrentColor() : null);
    } // end method setSingleSelectedDrawObj

    /**
     * Returns the currently focused single DrawObj, or null if none.
     *
     * @return The focused DrawObj, or null
     */
    public DrawObj getSingleSelectedDrawObj() {
        return singleSelectedDrawObj;
    } // end method getSingleSelectedDrawObj




    /**
     * Opens a new HueRelativesFrame using the hue of the currently focused
     * DrawObj (or hue 1 if none is focused).
     */
    public void openHueRelativesFrame() {
        // Default values
        int userFacingHue = 1; // Default hue number (1-36)
        
        // If a DrawObj is selected, use its hue instead of the default
        if (singleSelectedDrawObj != null) {
            ColorPtOstDot colorPt = singleSelectedDrawObj.getColorPtOstDotFillColor();
            OstHueColumnStep hcs = colorPt.getOstHCS();
            int stdCodeHueNum = hcs.getStdCodeHueNum();
            
            // Use HueNumGUI to get the user-facing hue number (1-36) from the code-facing hue (0-35)
            HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(stdCodeHueNum + 1);
            userFacingHue = hueNumGUI.getUserFacingHueNum();
        }
        
        openHueRelativesFrame(userFacingHue);
    }

    /**
     * Overloaded method to open a HueRelativesFrame with a specific DrawObj's hue.
     * This is used when the caller wants to specify which DrawObj's hue to use,
     * rather than using the currently selected DrawObj.
     * 
     * @param drawObj the DrawObj whose hue should be used for the new HueRelativesFrame
     */
    public void openHueRelativesFrame(DrawObj drawObj) {
        // Default values
        int userFacingHue = 1; // Default hue number (1-36)
        
        // Use the provided DrawObj's hue
        if (drawObj != null) {
            ColorPtOstDot colorPt = drawObj.getColorPtOstDotFillColor();
            OstHueColumnStep hcs = colorPt.getOstHCS();
            int stdCodeHueNum = hcs.getStdCodeHueNum();
            
            // Use HueNumGUI to get the user-facing hue number (1-36) from the code-facing hue (0-35)
            HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(stdCodeHueNum + 1);
            userFacingHue = hueNumGUI.getUserFacingHueNum();
        }
        
        openHueRelativesFrame(userFacingHue);
    }

    /**
     * The single creation path for HueRelativesFrames: creates the controller
     * and frame, registers it, wires close cleanup, sets initial spinner
     * values, positions, and shows the frame. All other frame-opening paths
     * delegate here.
     * 
     * @param userFacingHue The central hue (1-36) to initialize the frame with
     * @return The new HueRelativesFrame, or null if no DrawObjRoster is available
     */
    public HueRelativesFrame openHueRelativesFrame(int userFacingHue) {
        // Get the DrawObjRoster from the current template
        DrawObjRoster roster = getDrawObjRoster();
        
        // Ensure the roster has a valid reference to the template's DrawObjs
        if (roster != null && (roster.getTemplateDrawObjs() == null || roster.getTemplateDrawObjs().isEmpty())) {
            if (currTemplate != null) {
                System.out.println("Initializing DrawObjRoster's templateDrawObjs before creating HueRelativesViewController");
                
                // Force the roster to update its state
                roster.setTemplateDrawObjs(currTemplate.getAllDrawObjs());
                roster.updateToCurrObjState();
            }
        }
        
        // Only proceed if we have a valid DrawObjRoster
        if (roster != null) {
            // Create a new HueRelativesViewController with the DrawObjRoster
            // Pass the user-facing hue number (1-36) to the controller
            HueRelativesViewController hueController = new HueRelativesViewController(userFacingHue, roster, this);
            
            // Create and show the HueRelativesFrame
            HueRelativesFrame hueRelativesFrame = new HueRelativesFrame(hueController);
            hueRelativesFrames.add(hueRelativesFrame);
            
            // Add window listener to remove from collection when closed by user
            hueRelativesFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    hueRelativesFrames.remove(hueRelativesFrame);
                    if (designatedHueReceiver == hueRelativesFrame) {
                        designatedHueReceiver = null;
                    }
                }
            });
            
            // Set the initial values for the spinners
            hueRelativesFrame.setCentralHue(userFacingHue);
            hueRelativesFrame.setCounterCWiseLimit(1);
            hueRelativesFrame.setClockWiseLimit(1);
            
            // Position the frame relative to this frame
            hueRelativesFrame.setLocationRelativeTo(this);
            
            // Show the frame
            hueRelativesFrame.setVisible(true);
            return hueRelativesFrame;
        } else {
            showMessageAtBottom(this, "No template loaded or DrawObjRoster is unavailable", "Cannot Open Hue Relatives", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private void openSelectionGroupChoiceFrame() {
        if (selectionGroupChoiceFrame == null || !selectionGroupChoiceFrame.isDisplayable()) {
            selectionGroupChoiceFrame = new SelectionGroupChoiceFrame(this);
            selectionGroupChoiceFrame.setVisible(true);
        } else {
            selectionGroupChoiceFrame.toFront();
            selectionGroupChoiceFrame.requestFocus();
        }
    }

    /**
     * Closes the HueRelativesFrame if it is open.
     * This is called when a new Template is loaded to ensure that HueRelativesFrame
     * from the previous Template is not displayed.
     */
    private void closeHueRelativesFrames() {
        for (HueRelativesFrame frame : hueRelativesFrames) {
            if (frame.isDisplayable()) {
                frame.dispose();
            }
        }
        hueRelativesFrames.clear();
        designatedHueReceiver = null;
    }

    /**
     * Closes the SelectionGroupChoiceFrame if it is open.
     * This is called when a new Template is loaded to ensure that SelectionGroups
     * from the previous Template are not displayed.
     */
    private void closeSelectionGroupChoiceFrame() {
        if (selectionGroupChoiceFrame != null && selectionGroupChoiceFrame.isDisplayable()) {
            selectionGroupChoiceFrame.dispose();
        }
        selectionGroupChoiceFrame = null;
    }

    /**
     * Sets the designated HueRelativesFrame to receive hue updates.
     * Records the designation in the designatedHueReceiver field and syncs each
     * registered frame's receiver checkbox so that at most one frame is designated.
     * 
     * @param designatedFrame The HueRelativesFrame to designate as the receiver,
     *        or null to clear the designation so no frame receives updates
     */
    public void setDesignatedHueReceiver(HueRelativesFrame designatedFrame) {
        this.designatedHueReceiver = designatedFrame;
        // Sync each registered frame's receiver checkbox with the designation model
        for (HueRelativesFrame frame : hueRelativesFrames) {
            frame.setDesignatedReceiver(frame == designatedFrame);
        }
    }
    
    /**
     * Handles the "Transfer palette" menu item click.
     * This method allows the user to select a template file to transfer its palette.
     */
    private void transferPalette() {
        // Check if there's a current template loaded
        if (currTemplate == null) {
            showMessageAtBottom(this,
                    "No template is currently open. Please open a template first.",
                    "No Template",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create file chooser dialog
        JFileChooser fileChooser = new JFileChooser(storage.getStorageDir().toFile());
        fileChooser.setFileFilter(new FileNameExtensionFilter("Template Files", "json"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        File selectedFile = fileChooser.getSelectedFile();
        String templateName = selectedFile.getName().replace(".json", "");
        
        try {
            // Load the selected template
            Template sourceTemplate = storage.loadTemplate(templateName);
            
            // Extract palette from source template using Template's method
            PaletteWName sourcePalette = sourceTemplate.buildPaletteFromTemplateDrawObjs();
            System.out.println("DEBUG: Source palette: " + (sourcePalette != null ? "created successfully" : "null"));
            System.out.println("DEBUG: Source palette size: " + (sourcePalette != null ? sourcePalette.getSize() : "N/A"));
            
            // Check if palette size matches the number of DrawObjs in the template
            if (!checkPaletteSizeMatches(sourcePalette)) {
                return;
            }
            
            // Debug: Check if templateDrawObjs is properly initialized in the target template
            System.out.println("DEBUG: Target template DrawObjRoster templateDrawObjs size: " + 
                (currTemplate.getDrawObjRoster().getTemplateDrawObjs() != null ? 
                currTemplate.getDrawObjRoster().getTemplateDrawObjs().size() : "null"));
            
            // Apply palette to target template using the service interface
            boolean success = this.loadNewPalette(sourcePalette);
            System.out.println("DEBUG: Palette loading " + (success ? "succeeded" : "failed"));
            
            if (success) {
                /*
                // TODO 102526: look for better way to disable this message
                showMessageAtBottom(this, 
                    "Palette transferred successfully from " + templateName,
                    "Palette Transfer", 
                    JOptionPane.INFORMATION_MESSAGE);

                 */
                displayPaletteName(sourcePalette);
                setLastPaletteName(sourcePalette.getPaletteName());
                setPaletteWasLoaded(true);
                // Capture the source filename for palette-bookmark provenance
                // (true = the palette source was a template file)
                String fileNameForPaletteBookmark = captureFileNameForBookmark(templateName, true);
                lastPaletteLoadCameFromFile = fileNameForPaletteBookmark;
                repaint();
            } else {
                showMessageAtBottom(this, 
                    "Failed to transfer palette from " + templateName,
                    "Palette Transfer Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showMessageAtBottom(this, 
                "Error loading template: " + e.getMessage(),
                "Palette Transfer Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    } // end method transferPalette



    
    /**
     * Checks if the source template has the correct number of DrawObjs.
     * 
     * @param correctNumDrawObjs The expected number of DrawObjs (from the target template)
     * @param sourceTemplate The source template to check
     * @return true if the source template has the correct number of DrawObjs, false otherwise
     */
    private boolean checkTargetTemplateNumDrawObjs(int correctNumDrawObjs, Template sourceTemplate) {
        if (sourceTemplate == null) {
            return false;
        }
        
        int sourceNumDrawObjs = sourceTemplate.getAllDrawObjs().size();
        return sourceNumDrawObjs == correctNumDrawObjs;
    }

    /**
     * Shows a message dialog at the bottom right of the screen instead of the center.
     * 
     * @param parentComponent the parent component
     * @param message the message to display
     * @param title the title of the dialog
     * @param messageType the type of message (ERROR_MESSAGE, INFORMATION_MESSAGE, etc.)
     */
    private void showMessageAtBottom(Component parentComponent, Object message, String title, int messageType) {
        // Create the JOptionPane
        JOptionPane optionPane = new JOptionPane(message, messageType);
        optionPane.setOptions(new Object[] { "OK" });
        
        // Create a dialog to display the JOptionPane
        JDialog dialog = optionPane.createDialog(parentComponent, title);
        
        // Calculate position at the bottom right of the screen
        int x = parentComponent.getLocationOnScreen().x + parentComponent.getWidth() - dialog.getWidth() - 20 + 700; // 700px further right (400+300)
        int y = parentComponent.getLocationOnScreen().y + parentComponent.getHeight() - dialog.getHeight() - 50 + 300; // 350px from bottom (50+300)
        
        // Set the dialog position
        dialog.setLocation(x, y);
        
        // Show the dialog
        dialog.setVisible(true);
    }
    
    /**
     * Positions a dialog at the bottom-right corner of the screen.
     */
    private void positionDialogAtBottomRight(JComponent dialog) {
        // Get the screen dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        // Estimate dialog size - typical file chooser is around 700x500
        int dialogWidth = 700;
        int dialogHeight = 500;
        
        // Set margins to -30 to move dialog further right and down
        int horizontalMargin = -30;
        int verticalMargin = -30;
        
        // Calculate position with negative margins to move dialog further right and down
        int xPos = screenSize.width - dialogWidth - horizontalMargin;
        int yPos = screenSize.height - dialogHeight - verticalMargin;
        
        // Set the preferred location - this will be used when the dialog is shown
        dialog.setLocation(xPos, yPos);
        
        // For JFileChooser specifically, we need to set the location on its parent dialog
        // This will be applied after the dialog is created but before it's shown
        dialog.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (dialog.isShowing() && dialog.getTopLevelAncestor() != null) {
                    Window window = SwingUtilities.getWindowAncestor(dialog);
                    if (window != null) {
                        window.setLocation(xPos, yPos);
                    }
                }
            }
        });
    }

    public boolean isPaletteWasLoaded() {
        return paletteWasLoaded;
    }

    public void setPaletteWasLoaded(boolean paletteLoadedYesNo) {
        this.paletteWasLoaded = paletteLoadedYesNo;
    }

    public String getLastPaletteName() {
        return lastPaletteName;
    }

    public void setLastPaletteName(String lastPaletteName) {
        this.lastPaletteName = lastPaletteName;
    }
    
    /**
     * Handles the "Save named palette" menu item click.
     * This method allows the user to save the current palette with a custom name.
     */
    private void saveNamedPalette() {
        // Check if there's a current template loaded
        if (currTemplate == null) {
            showMessageAtBottom(this,
                    "No template is currently open. Please open a template first.",
                    "No Template",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Extract palette from current template
        PaletteWName currentPalette = currTemplate.buildPaletteFromTemplateDrawObjs();
        
        if (currentPalette == null || currentPalette.getSize() == 0) {
            showMessageAtBottom(this,
                    "Failed to create palette from current template.",
                    "Palette Creation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Determine suggested palette name
        String suggestedName;
        
        // If a palette was previously loaded, use its name as the default
        if (paletteWasLoaded && lastPaletteName != null && !lastPaletteName.trim().isEmpty()) {
            suggestedName = lastPaletteName;
        } else {
            // Otherwise use the template name
            suggestedName = currTemplate.getTheTemplateName();
        }
        
        // Create file chooser dialog
        JFileChooser fileChooser = new JFileChooser(paletteStorage.getStorageDir().toFile());
        fileChooser.setDialogTitle("Save Palette");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Palette Files (*.json)", "json"));
        
        // Set suggested file name
        File suggestedFile = new File(suggestedName + ".json");
        fileChooser.setSelectedFile(suggestedFile);
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                // Get selected file and ensure it has .json extension
                File selectedFile = fileChooser.getSelectedFile();
                if (!selectedFile.getName().toLowerCase().endsWith(".json")) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + ".json");
                }
                
                // Check if file already exists and confirm overwrite
                if (selectedFile.exists()) {
                    int overwrite = JOptionPane.showConfirmDialog(this,
                            "File already exists. Do you want to overwrite it?",
                            "Confirm Overwrite",
                            JOptionPane.YES_NO_OPTION);
                    if (overwrite != JOptionPane.YES_OPTION) {
                        return; // User declined overwrite
                    }
                }
                
                String filePath = selectedFile.getPath();
                String name = selectedFile.getName();
                if (name.endsWith(".json")) {
                    name = name.substring(0, name.length() - 5); 
                }
                currentPalette.setPaletteName(name);
                
                // Save the palette
                paletteStorage.savePalette(currentPalette, name);
                
                showMessageAtBottom(this,
                        "Palette saved successfully as '" + name + "'",
                        "Palette Saved",
                        JOptionPane.INFORMATION_MESSAGE);
                
            } catch (TemplateStorageException e) {
                e.printStackTrace();
                showMessageAtBottom(this,
                        "Error saving palette: " + e.getMessage(),
                        "Palette Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    } // end method saveNamedPalette



    
    private void loadNamedPalette() {
        // Check if there's a current template loaded
        if (currTemplate == null) {
            showMessageAtBottom(this,
                    "No template is currently open. Please open a template first.",
                    "No Template",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Create file chooser dialog
            JFileChooser fileChooser = new JFileChooser(paletteStorage.getStorageDir().toFile());
            fileChooser.setDialogTitle("Load Palette");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Palette Files (*.json)", "json"));
            
            // Show the file chooser dialog
            int result = fileChooser.showOpenDialog(this);
            
            // If user cancelled, return
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            
            // Get the selected file
            File selectedFile = fileChooser.getSelectedFile();
            
            // Extract palette name from file name (remove .json extension)
            String fileName = selectedFile.getName();
            String selectedPaletteName = fileName.substring(0, fileName.lastIndexOf('.'));
            
            // Load the selected palette
            PaletteWName loadedPalette = paletteStorage.loadPalette(selectedPaletteName);
            
            if (loadedPalette == null) {
                showMessageAtBottom(this,
                        "Failed to load palette: " + selectedPaletteName,
                        "Palette Load Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check if the palette has colors
            if (loadedPalette.getSize() == 0) {
                showMessageAtBottom(this,
                        "The selected palette '" + selectedPaletteName + "' is empty.",
                        "Empty Palette",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Show palette preview and ask for confirmation
            boolean confirmLoad = showPalettePreview(loadedPalette);
            if (!confirmLoad) {
                // User cancelled after seeing the preview
                return;
            }
            
            // Check if palette size matches the number of DrawObjs in the template
            if (!checkPaletteSizeMatches(loadedPalette)) {
                return;
            }
            
            // Apply palette to current template using the service interface
            boolean success = this.loadNewPalette(loadedPalette);
            
            if (success) {
                /*
                // TODO 102526: look for better way to disable this message
                showMessageAtBottom(this,
                        "Palette '" + selectedPaletteName + "' loaded successfully",
                        "Palette Loaded",
                        JOptionPane.INFORMATION_MESSAGE);

                 */
                displayPaletteName(loadedPalette);
                setLastPaletteName(loadedPalette.getPaletteName());
                setPaletteWasLoaded(true);
                // Capture the source filename for palette-bookmark provenance
                // (false = the palette source was a named-palette file)
                String fileNameForPaletteBookmark = captureFileNameForBookmark(selectedPaletteName, false);
                lastPaletteLoadCameFromFile = fileNameForPaletteBookmark;
                
                // Flash the color panels to indicate the palette has changed
                flashColorPanels();
                
                // Trigger a full repaint to show the new colors
                repaint();
                
                // Notify any listeners that the palette has changed
                notifyPaletteEventListeners();
            } else {
                showMessageAtBottom(this,
                        "Failed to apply palette '" + selectedPaletteName + "'. The palette may not be compatible with the current template.",
                        "Palette Load Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (TemplateStorageException e) {
            e.printStackTrace();
            showMessageAtBottom(this,
                    "Error loading palette: " + e.getMessage(),
                    "Palette Load Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            showMessageAtBottom(this,
                    "Unexpected error while loading palette: " + e.getMessage(),
                    "Palette Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    } // end method loadNamedPalette

    /**
     * Composes the provenance display string that callers assign to
     * {@link #lastPaletteLoadCameFromFile}. The {@code isTemplateFile} flag is consumed
     * transiently: it is not stored separately, but is appended to the filename as a short
     * source-type code, producing a string of the form "filename_CODE" — "filename_TEMPL"
     * when the palette was transferred from a saved template file, or "filename_PALTT" when
     * it was loaded from a named-palette file.
     *
     * <p>This method does not modify {@link #lastPaletteLoadCameFromFile}; each caller
     * assigns the returned string to the field. It prints a one-line diagnostic to the
     * console so the capture can be verified while the app runs.
     *
     * @param fileName The source file's basename with its file extension already removed
     * @param isTemplateFile true if the palette source was a template file,
     *                       false if it was a named-palette file
     * @return The composed display string "filename_TEMPL" or "filename_PALTT"
     */
    private String captureFileNameForBookmark(String fileName, boolean isTemplateFile) {
        // Append the source-type code so the composed string preserves where the palette came from
        String composedFileName = fileName + "_" + (isTemplateFile ? "TEMPL" : "PALTT");
        // Short verification diagnostic: the method name plus the composed value being returned
        System.out.println("Method captureFileNameForBookmark works -used to set variable lastPaletteLoadCameFromFile to " + composedFileName);
        return composedFileName;
    } // end method captureFileNameForBookmark

    /**
     * Test method for PaletteWShapeNames.
     * Creates a PaletteWShapeNames instance from the current template's DrawObjRoster
     * and displays the first 10 hybrid pairs in a test window.
     */
    private void testPaletteWShapeNames() {
        // Check if there's a current template loaded
        if (currTemplate == null) {
            showMessageAtBottom(this,
                    "No template is currently open. Please open a template first.",
                    "No Template",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Get the DrawObjRoster from the current template
            DrawObjRoster drawObjRoster = currTemplate.getDrawObjRoster();
            
            if (drawObjRoster == null) {
                showMessageAtBottom(this,
                        "Template has no DrawObjRoster.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create factory and build PaletteWShapeNames
            PaletteWShapeNamesFactory factory =
                new PaletteWShapeNamesFactory();
            PaletteWShapeNames paletteWShapeNames =
                factory.createFromDrawObjRoster(drawObjRoster);
            
            // Set palette name from template if available
            if (currTemplate.getTheTemplateName() != null) {
                paletteWShapeNames.setPaletteName(currTemplate.getTheTemplateName());
            }
            
            // Create and display the test window
            PaletteWNamesDisplayFrame displayFrame = new PaletteWNamesDisplayFrame(paletteWShapeNames);
            // Shift right of screen center so the display frame is more visible
            displayFrame.setLocation(displayFrame.getX() + rightShiftPalttWShapeNames, displayFrame.getY());

            /*
            // TODO 102526: look for better way to disable this message
            showMessageAtBottom(this,
                    "PaletteWShapeNames test window opened. Showing all shapes.",
                    "Test Window",
                    JOptionPane.INFORMATION_MESSAGE);

             */
            
        } catch (Exception e) {
            e.printStackTrace();
            showMessageAtBottom(this,
                    "Error testing PaletteWShapeNames: " + e.getMessage(),
                    "Test Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    } // end method testPaletteWShapeNames

    /**
     * Saves a PaletteWShapeNames instance to a file.
     * Creates a PaletteWShapeNames from the current template's DrawObjRoster,
     * opens a file save dialog, and saves to dev_storage/palettes/wshapenames/.
     */
    private void savePaletteWShapeNames() {
        // Check if there's a current template loaded
        if (currTemplate == null) {
            showMessageAtBottom(this,
                    "No template is currently open. Please open a template first.",
                    "No Template",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Get the DrawObjRoster from the current template
            DrawObjRoster drawObjRoster = currTemplate.getDrawObjRoster();
            
            if (drawObjRoster == null) {
                showMessageAtBottom(this,
                        "Template has no DrawObjRoster.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create factory and build PaletteWShapeNames
            PaletteWShapeNamesFactory factory =
                new PaletteWShapeNamesFactory();
            PaletteWShapeNames paletteWShapeNames =
                factory.createFromDrawObjRoster(drawObjRoster);
            
            // Set palette name from template if available
            if (currTemplate.getTheTemplateName() != null) {
                paletteWShapeNames.setPaletteName(currTemplate.getTheTemplateName());
            }
            
            // Open file save dialog
            JFileChooser fileChooser = new JFileChooser(paletteWShapeNamesStorage.getStorageDir().toFile());
            fileChooser.setDialogTitle("Save PaletteWShapeNames");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Palette Files (*.json)", "json"));
            
            // Set default filename to template name
            String templateName = currTemplate.getTheTemplateName();
            if (templateName != null && !templateName.trim().isEmpty()) {
                fileChooser.setSelectedFile(new File(paletteWShapeNamesStorage.getStorageDir().toFile(), templateName + ".json"));
            }
            
            // Show the file chooser dialog
            int result = fileChooser.showSaveDialog(this);
            
            // If user cancelled, return
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            
            // Get the selected file
            File selectedFile = fileChooser.getSelectedFile();
            String filename = selectedFile.getName();
            
            // Save the palette
            paletteWShapeNamesStorage.savePaletteWShapeNames(paletteWShapeNames, filename);
            
            // Show success message
            showMessageAtBottom(this,
                    "PaletteWShapeNames saved successfully to: " + selectedFile.getAbsolutePath(),
                    "Save Successful",
                    JOptionPane.INFORMATION_MESSAGE);
            
        } catch (TemplateStorageException e) {
            e.printStackTrace();
            showMessageAtBottom(this,
                    "Error saving PaletteWShapeNames: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            showMessageAtBottom(this,
                    "Unexpected error while saving PaletteWShapeNames: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    } // end method savePaletteWShapeNames

    /**
     * Opens a saved PaletteWShapeNames instance from a file.
     * Opens a file dialog to select a saved palette file from dev_storage/palettes/wshapenames/
     * and displays it in a PaletteWNamesDisplayJFrame.
     */
    private void openPaletteWShapeNames() {
        try {
            // Create file chooser dialog
            JFileChooser fileChooser = new JFileChooser(paletteWShapeNamesStorage.getStorageDir().toFile());
            fileChooser.setDialogTitle("Open PaletteWShapeNames");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Palette Files (*.json)", "json"));
            
            // Show the file chooser dialog
            int result = fileChooser.showOpenDialog(this);
            
            // If user cancelled, return
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            
            // Get the selected file
            File selectedFile = fileChooser.getSelectedFile();
            
            // Extract palette name from file name (remove .json extension)
            String fileName = selectedFile.getName();
            String selectedPaletteName = fileName.substring(0, fileName.lastIndexOf('.'));
            
            // Load the selected palette
            PaletteWShapeNames loadedPalette =
                paletteWShapeNamesStorage.loadPaletteWShapeNames(selectedPaletteName);
            
            if (loadedPalette == null) {
                showMessageAtBottom(this,
                        "Failed to load palette: " + selectedPaletteName,
                        "Palette Load Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check if the palette has any pairs
            if (loadedPalette.getSize() == 0) {
                showMessageAtBottom(this,
                        "The selected palette '" + selectedPaletteName + "' is empty.",
                        "Empty Palette",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Create and display the palette window
            PaletteWNamesDisplayFrame displayFrame = new PaletteWNamesDisplayFrame(loadedPalette);
            // Shift right of screen center so the display frame is more visible
            displayFrame.setLocation(displayFrame.getX() + rightShiftPalttWShapeNames, displayFrame.getY());
            
            showMessageAtBottom(this,
                    "PaletteWShapeNames '" + selectedPaletteName + "' opened successfully. Showing all Template shapes.",
                    "Palette Opened",
                    JOptionPane.INFORMATION_MESSAGE);
            
        } catch (TemplateStorageException e) {
            e.printStackTrace();
            showMessageAtBottom(this,
                    "Error loading palette: " + e.getMessage(),
                    "Palette Load Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            showMessageAtBottom(this,
                    "Unexpected error while opening palette: " + e.getMessage(),
                    "Palette Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    } // end method openPaletteWShapeNames

    /**
     * Shows a preview of the palette colors in a dialog.
     * 
     * @param palette The palette to preview
     * @return true if the user wants to load the palette, false otherwise
     */
    private boolean showPalettePreview(PaletteWName palette) {
        if (palette == null || palette.getSize() == 0) {
            return false;
        }
        
        // Create a panel to display the palette colors
        JPanel previewPanel = new JPanel();
        previewPanel.setLayout(new BorderLayout());
        
        // Add a label with the palette name
        JLabel nameLabel = new JLabel("Palette: " + palette.getPaletteName());
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        previewPanel.add(nameLabel, BorderLayout.NORTH);
        
        // Create a panel for the color swatches
        JPanel colorsPanel = new JPanel(new GridLayout(0, 5, 5, 5)); // 5 columns, 5px horizontal and vertical gaps
        
        // Create the main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(0, 5)); // No horizontal gap, keep vertical gap of 5 pixels
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0)); // No right border, keep other borders at 5 pixels
        for (int i = 0; i < palette.getSize(); i++) {
            ColorPtOstDot colorPt = palette.getColorAt(i);
            Color color = colorPt.getAsColor();
            JPanel swatch = new JPanel();
            swatch.setBackground(color);
            swatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            colorsPanel.add(swatch);
        }
        
        // Add the colors panel to a scroll pane in case there are many colors
        JScrollPane scrollPane = new JScrollPane(colorsPanel);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        previewPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Show the dialog with OK and Cancel options
        int result = JOptionPane.showConfirmDialog(
                this,
                previewPanel,
                "Palette Preview",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        
        // Return true if the user clicked OK
        return result == JOptionPane.OK_OPTION;
    }

    /**
     * Shows a palette from a saved Template in a persistent, resizable window.
     * 
     * This method:
     * 1. Prompts user to select a saved template file
     * 2. Loads the template from disk
     * 3. Extracts the palette from the template's DrawObjs
     * 4. Opens a PaletteDisplayFrame to display the palette
     * 
     * The palette is displayed in a non-modal window that can be resized and moved.
     * The user can examine the palette without blocking other windows.
     */
    private void showPaletteFromSavedTemplate() {
        // Step 1: Create file chooser with templates directory as default
        JFileChooser fileChooser = new JFileChooser(storage.getStorageDir().toFile());
        fileChooser.setFileFilter(new FileNameExtensionFilter("Template files (*.json)", "json"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            // User cancelled
            return;
        }
        
        // Step 2: Get selected file and extract template name
        File selectedFile = fileChooser.getSelectedFile();
        String templateName = selectedFile.getName();
        if (templateName.endsWith(".json")) {
            templateName = templateName.substring(0, templateName.length() - 5);
        }
        
        // Step 3: Load the template from disk
        try {
            Template sourceTemplate = storage.loadTemplate(templateName);
            
            if (sourceTemplate == null) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to load template: " + templateName, 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Step 4: Extract palette from the loaded template
            PaletteWName palette = sourceTemplate.buildPaletteFromTemplateDrawObjs();
            
            if (palette == null || palette.getSize() == 0) {
                JOptionPane.showMessageDialog(this, 
                    "Template has no colors or failed to extract palette", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Step 5: Open PaletteDisplayFrame to display the palette
            new PaletteDisplayFrame(palette, templateName);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading template: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            System.err.println("Error in showPaletteFromSavedTemplate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shows a color-source palette from a saved Template in an interactive window.
     * 
     * This method:
     * 1. Prompts user to select a saved template file
     * 2. Loads the template from disk
     * 3. Creates a PaletteWShapeNames instance from the template's DrawObjs
     * 4. Opens a PaletteColorSourceFrame to display the color-source palette
     * 
     * The color-source palette is displayed in a non-modal window with interactive
     * color chips that can be selected. Each color chip is linked to a shape name.
     * Multiple instances can be open simultaneously.
     */
    private void colorSourcePaletteFromSavedTemplate() {
        // Step 1: Create file chooser with templates directory as default
        JFileChooser fileChooser = new JFileChooser(storage.getStorageDir().toFile());
        fileChooser.setFileFilter(new FileNameExtensionFilter("Template files (*.json)", "json"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            // User cancelled
            return;
        }
        
        // Step 2: Get selected file and extract template name
        File selectedFile = fileChooser.getSelectedFile();
        String templateName = selectedFile.getName();
        if (templateName.endsWith(".json")) {
            templateName = templateName.substring(0, templateName.length() - 5);
        }
        
        // Step 3: Load the template from disk
        try {
            Template sourceTemplate = storage.loadTemplate(templateName);
            
            if (sourceTemplate == null) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to load template: " + templateName, 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Step 4: Create PaletteWShapeNames from the loaded template
            PaletteWShapeNamesFactory factory = new PaletteWShapeNamesFactory();
            PaletteWShapeNames paletteWShapeNames = factory.createFromDrawObjRoster(
                sourceTemplate.getDrawObjRoster());
            
            if (paletteWShapeNames == null || paletteWShapeNames.getSize() == 0) {
                JOptionPane.showMessageDialog(this, 
                    "Template has no shapes or failed to extract palette", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Step 5: Create callback listener (placeholder for now)
            IColorChipSelectionListener selectionListener = (pair, index) -> {
                // Placeholder: callback will be enhanced later
                int oneBasedIndex = index + 1;
                System.out.println("Color chip selected: " + pair.getShapeName()
                                                            + " at one-basedindex " + oneBasedIndex);
            };
            
            // Step 6: Open PaletteColorSourceFrame to display the color-source palette
            ShapesPaletteColorSourceFrame frame = new ShapesPaletteColorSourceFrame(
                paletteWShapeNames, 
                templateName, 
                selectionListener,
                this);
            
            // Add to list of open color source frames
            colorSourceFrames.add(frame);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading template: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            System.err.println("Error in colorSourcePaletteFromSavedTemplate: " + e.getMessage());
            e.printStackTrace();
        }
    } // end method colorSourcePaletteFromSavedTemplate()

    /**
     * Shows a color-source palette from a saved PaletteWName in an interactive window.
     * 
     * This method:
     * 1. Prompts user to select a saved palette file
     * 2. Loads the palette from disk using PaletteStorage
     * 3. Validates that the palette is not empty
     * 4. Opens a SimplePaletteColorSourceFrame to display the color-source palette
     * 
     * The color-source palette is displayed in a non-modal window with interactive
     * color chips that can be selected. Each color chip shows hue, column, and step information.
     * Multiple instances can be open simultaneously.
     */
    private void colorSourceSavedPalette() {
        // Step 1: Create file chooser with palettes directory as default
        JFileChooser fileChooser = new JFileChooser(paletteStorage.getStorageDir().toFile());
        fileChooser.setFileFilter(new FileNameExtensionFilter("Palette files (*.json)", "json"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            // User cancelled
            return;
        }
        
        // Step 2: Get selected file and extract palette name
        File selectedFile = fileChooser.getSelectedFile();
        String paletteName = selectedFile.getName();
        if (paletteName.endsWith(".json")) {
            paletteName = paletteName.substring(0, paletteName.length() - 5);
        }
        
        // Step 3: Load the palette from disk
        try {
            PaletteWName paletteWName = paletteStorage.loadPalette(paletteName);
            
            if (paletteWName == null) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to load palette: " + paletteName, 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Step 4: Validate that palette is not empty
            if (paletteWName.getSize() == 0) {
                JOptionPane.showMessageDialog(this, 
                    "Palette is empty: " + paletteName, 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Step 5: Create callback listener (placeholder for now)
            IColorChipSelectionListener selectionListener = (pair, index) -> {
                // Placeholder: callback will be enhanced later
                int oneBasedIndex = index + 1;
                System.out.println("Color chip selected: " + pair.getShapeName()
                                                            + " at one-based index " + oneBasedIndex);
            };
            
            // Step 6: Open SimplePaletteColorSourceFrame to display the color-source palette
            SimplePaletteColorSourceFrame frame = new SimplePaletteColorSourceFrame(
                paletteWName, 
                paletteName, 
                selectionListener,
                this);
            
            // Add to list of open color source frames
            colorSourceFrames.add(frame);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading palette: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            System.err.println("Error in colorSourceSavedPalette: " + e.getMessage());
            e.printStackTrace();
        }
    } // end method colorSourceSavedPalette()

    /**
     * Opens the Mosaic palette-builder module.
     * 
     * This method sends a message through the ApplicationMediator to open the
     * mosaic module, which provides interactive palette building and mosaic 
     * generation capabilities. The frame is shown with its state reset to initial values.
     */
    private void openMosaicPaletteBuilder() {
        try {
            Message message = new Message("OPEN_MOSAIC_MODULE", null, null);
            Object result = mediator.sendMessage("mosaic", message);
            
            if (result == null) {
                JOptionPane.showMessageDialog(this, 
                    "Mosaic module not available", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error opening Mosaic palette-builder: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            System.err.println("Error in openMosaicPaletteBuilder: " + e.getMessage());
            e.printStackTrace();
        }
    } // end method openMosaicPaletteBuilder()

    /**
     * Notifies all registered palette event listeners that a palette has been loaded.
     * This creates a PALETTE_LOAD event and sends it to all listeners.
     */
    private void notifyPaletteEventListeners() {
        if (currTemplate == null) {
            return;
        }
        
        // Create a palette loaded event
        PositionalPaletteEvent event = new PositionalPaletteEvent(
                this,
                PositionalPaletteEvent.EventType.PALETTE_LOAD,
                currTemplate.getDrawObjRoster()
        );
        
        // Fire the event to all listeners
        firePositionalPaletteEvent(event);
    }

    private void flashColorPanels() {
        // Flash the color panels by changing their background color and then changing it back
        Color originalColor = clickColorSquare.getBackground();
        clickColorSquare.setBackground(Color.YELLOW);
        
        // Use a timer to revert the color after a short delay
        Timer timer = new Timer(300, e -> {
            clickColorSquare.setBackground(originalColor);
            
            // Use another timer for a second flash
            Timer timer2 = new Timer(300, e2 -> {
                clickColorSquare.setBackground(Color.YELLOW);
                
                // Final timer to revert back to original
                Timer timer3 = new Timer(300, e3 -> {
                    clickColorSquare.setBackground(originalColor);
                });
                timer3.setRepeats(false);
                timer3.start();
            });
            timer2.setRepeats(false);
            timer2.start();
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Checks if the number of colors in a palette matches the number of DrawObjs in the current template.
     * 
     * @param palette The palette to check
     * @return true if the sizes match, false otherwise
     */
    private boolean checkPaletteSizeMatches(PaletteWName palette) {
        if (currTemplate == null || palette == null) {
            return false;
        }
        
        int numDrawObjs = currTemplate.getAllDrawObjs().size();
        int numColors = palette.getSize();
        
        System.out.println("DEBUG: Checking palette size match - DrawObjs: " + numDrawObjs + ", Colors: " + numColors);
        
        if (numDrawObjs != numColors) {
            showMessageAtBottom(this,
                    "Palette size (" + numColors + " colors) does not match template size (" + numDrawObjs + " objects).",
                    "Palette Size Mismatch",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets the currently selected DrawObj from the selected objects list panel.
     * If no object is selected in that panel, returns null.
     * 
     * @return The selected DrawObj from the selected objects list, or null if none selected
     */
    private DrawObj getSelectedDrawObjFromSelectionList() {
        JList<DrawObj> list = listSelectDrawObjsPanel.getJList();
        return list.getSelectedValue();
    }

    /**
     * Handles SelectionEvent from DrawObjRoster. Refreshes the selected-objects
     * list panel, then resolves the all-list highlight and focus to: the current
     * focus if it remains a group member, else the sole member of a singleton
     * group, else null. Because roster events fire synchronously, callers that
     * need a specific post-mutation focus must write it BEFORE mutating the
     * roster (the focus-before-mutation caller contract — same convention as
     * HRF +/- and SSC.handleShiftToggle; 102926 design doc §4).
     */
    @Override
    public void handleSelectionEvent(SelectionEvent event) {
        // Update the selection display panel
        if (event.getSource() instanceof DrawObjRoster) {
            DrawObjRoster roster = (DrawObjRoster) event.getSource();
            Set<DrawObj> selectedObjs = roster.getSelectionGroup().getSelectedDrawObjsSet();
            
            System.out.println("HUE-ROTATE: handleSelectionEvent() called with " + selectedObjs.size() + " objects");
            System.out.println("HUE-ROTATE:   Event type: " + event.getChangeType());
            System.out.println("HUE-ROTATE:   Roster's SelectionGroup instance: " + 
                System.identityHashCode(roster.getSelectionGroup()));
            
            // Update the selection list panel via the manager
            selectionDisplayManager.refreshDisplay();
            
            // Resolve the single highlight for the all-list: the focus object
            // when it survives in the group, otherwise the single member of a
            // singleton group (e.g., after a canvas click collapses the group),
            // otherwise clear.
            DrawObj highlight = selectedObjs.contains(singleSelectedDrawObj)
                    ? singleSelectedDrawObj
                    : (selectedObjs.size() == 1 ? selectedObjs.iterator().next() : null);
            setSingleSelectedDrawObj(highlight);
            listAllDrawObjsPanel.syncWithSingleObject(highlight);
        }
    }

    /**
     * Handles DrawObj selection events from the list panel.
     * Updates the color square and singleSelectedDrawObj.
     */
    private void handleDrawObjSelection() {
        // Get all selected DrawObjs from the list panel
        DrawObj[] selectedDrawObjs = listAllDrawObjsPanel.getSelectedDrawObjs();
        
        if (selectedDrawObjs != null && selectedDrawObjs.length > 0) {
            // For backward compatibility, set the first selected object as singleSelectedDrawObj
            DrawObj firstSelected = selectedDrawObjs[0];
            setSingleSelectedDrawObj(firstSelected);
            
            System.out.println("TemplateWorkFrame: Selection updated with " + 
                selectedDrawObjs.length + " objects, color: " + firstSelected.getCurrentColor());
        } else {
            // No selection, clear everything
            setSingleSelectedDrawObj(null);
        }
    }

    /**
     * Handles the "Add to AG" button click (UC-026 step 1.21.6.4).
     * Gets selected DrawObjs from the current SelectionGroup and adds them to the selected AG
     * in the TemplateProxyData's RMatrixMini.
     * Uses RMatrixMini.addToAG() for each DrawObj.
     */
    private void handleAddToAGButton() {
        // Validate that a template is loaded
        if (currTemplate == null) {
            JOptionPane.showMessageDialog(this, 
                "No template loaded. Please load a template first.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validate that TemplateProxyData is available
        // This is only available after starting the UC-040 workflow
        if (currTemplateProxyData == null) {
            JOptionPane.showMessageDialog(this, 
                "TemplateProxyData not initialized. Please start the AnchorPattern workflow first.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Get the RMatrixMini from TemplateProxyData
        RMatrixMiniPrelim rmatrix = currTemplateProxyData.getRMatrixMini();
        if (rmatrix == null) {
            JOptionPane.showMessageDialog(this, 
                "RMatrixMini not found in TemplateProxyData.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Get selected DrawObjs from the current SelectionGroup (like handleSelectionEvent does)
        DrawObjRoster roster = currTemplate.getDrawObjRoster();
        Set<DrawObj> selectedObjs = roster.getSelectionGroup().getSelectedDrawObjsSet();
        
        if (selectedObjs == null || selectedObjs.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No DrawObjs selected. Please select one or more DrawObjs first.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Get the AG number from the spinner
        int agNum = (Integer) anchGroupChoiceSpinner.getValue();
        
        // Validate AG number
        if (agNum < 1 || agNum > 4) {
            JOptionPane.showMessageDialog(this, 
                "Invalid AG number: " + agNum + ". Must be between 1 and 4.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Add each selected DrawObj to the AG in TemplateProxyData's RMatrixMini // 111325 no duplicate added if already in AG
        // TODO 111325 "addedCount" is accurate for number processed, not number actually added (no adjustment if already in the AG)
        int addedCount = 0; // TODO 111325 not all of these are necessarily new (may be in the AG already)
        for (DrawObj drawObj : selectedObjs) {
            if (drawObj != null) {
                rmatrix.addToAG(drawObj, agNum);
                addedCount++;
                System.out.println("Added DrawObj '" + drawObj.getName() + "' to AG " + agNum + 
                    " in TemplateProxyData");
            }
        }

        // Auto-update AGsInfoFrame display to show changes immediately
        // This ensures the user sees the updated display after bulk AG removal
        if (currAGsInfoFrame != null) {
            System.out.println("TemplateWorkFrame: Triggering auto-update of AGsInfoFrame display after bulk AG removal");
            currAGsInfoFrame.getTheController().autoUpdateDrawSkeleton(currAGsInfoFrame.getFilterState());
        }
        
        // Show confirmation message
        JOptionPane.showMessageDialog(this, 
            "Added " + addedCount + " DrawObj(s) to AG " + agNum,
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
    } // end method handleAddToAGButton




    /**
     * Opens AnchorPatternFrame in INFORMATIONAL mode (UC-040.5).
     * Displays occupied hues from the current template.
     * User can view but not edit.
     */
    private void openAnchorPatternFrameInstance1() {
        if (currTemplate == null) {
            JOptionPane.showMessageDialog(this, "No template loaded. Please load a template first.");
            return;
        }
        
        try {
            // Create INFORMATIONAL mode frame
            AnchorPatternFrame frame = new AnchorPatternFrame(currTemplate);
            frame.setVisible(true);
            // Frame is independent, user closes when done
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error opening AnchorPatternFrame: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Returns the PaletteExplicit instance for this TemplateWorkFrame.
     * 
     * @return The PaletteExplicit instance, or null if no template is loaded
     */
    public PaletteExplicit getPaletteExplicit() {
        return paletteExplicit;
    }

    /**
     * Starts UC-040 workflow (UC-041).
     * Creates TemplateProxyData, opens AnchorPatternFrame (OPERATIONAL mode),
     * and opens AGsInfoFrame for AG assignment and validation.
     */
    private void startUC040Workflow() {
        if (currTemplate == null) {
            JOptionPane.showMessageDialog(this, "No template loaded. Please load a template first.");
            return;
        }
        
        try {
            // Create TemplateProxyData (working copy)
            TemplateProxyData proxyData = TemplateProxyDataService.createTemplateProxyData(currTemplate);
            
            // Store reference for AG assignment operations
            this.currTemplateProxyData = proxyData;
            
            // Create OPERATIONAL mode AnchorPatternFrame (Instance 2)
            AnchorPatternFrame apFrame = new AnchorPatternFrame(proxyData);
            
            // Create AGsInfoFrame (main workflow window)
            AGsInfoFrame agsFrame = new AGsInfoFrame(proxyData, apFrame, proxyData.getRMatrixMini(), this);
            
            // Store reference for display updates after AG operations
            this.currAGsInfoFrame = agsFrame;
            
            // Display frames
            apFrame.setVisible(true);
            agsFrame.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error starting UC-040 workflow: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    } // end method startUC040Workflow


} // end class TemplateWorkFrame
