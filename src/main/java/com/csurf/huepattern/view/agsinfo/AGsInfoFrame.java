package com.csurf.huepattern.view.agsinfo;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.Set;
import java.util.UUID;
import com.csurf.app.HelpTextWindow;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.view.ShapeDisplayPanel;
import com.csurf.templatecore.view.ShapeListPanel;
import com.csurf.templatecore.view.SelectedShapesListPanel;
import com.csurf.templatecore.view.TemplateWorkFrame;
import com.csurf.templatecore.view.HueRelativesFrame;
import com.csurf.templatecore.controller.HueRelativesViewController;
import com.csurf.huepattern.model.proxy.TemplateProxyData;
import com.csurf.huepattern.model.RMatrixMiniPrelim;
import com.csurf.huepattern.view.AnchorPatternFrame;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

/**
 * AGsInfoFrame is the main GUI window for detailed AG assignment work and validation.
 * 
 * Purpose:
 * - Provides central hub for iterative refinement of AG assignments and AnchorPattern definitions
 * - Displays DrawObjs in context for detailed interactive work
 * - Supports validation and refinement of AG assignments
 * - Handles hue adjustments and proportional location modifications
 * - Manages user signals for workflow control ("Check for multiple AGs", "Check within Spreads", etc.)
 * 
 * Key Responsibilities:
 * - Display DrawObjs based on context (selected AG, multiple AGs, outside spreads, etc.)
 * - Allow user to assign DrawObjs to AGs
 * - Allow user to modify proportional locations and hues
 * - Provide validation displays (multiple AG assignments, hues outside spreads, etc.)
 * - Coordinate with AnchorPatternFrame for spread adjustments
 * - Manage menu system for user interactions
 * 
 * Data Model:
 * - References TemplateProxyData (working copy of Template data)
 * - References original Template (for visual comparison)
 * - References AnchorPatternFrame (Instance 2) for spread adjustments
 * 
 * Menu System:
 * - Show Menu: Show AG1-4, Show selected AGs, Show full image, Show selected DrawObj info
 * - Issues Menu: Check for multiple AGs, Check within Spreads, Check no AG assigned
 * - Validate Menu: Validate selected AGs, Validate all AGs, Validate Spread location
 * - AnchorPattern Menu: Open AnchorPatternFrame, Shrink to fit (placeholder)
 * 
 * Workflow Integration:
 * - UC-041: Created during initial setup
 * - UC-042: Receives AG assignments from TemplateWorkFrame (one-way data flow)
 * - UC-044: Supports fine-tuning with hue changes and proportional location modifications
 * - UC-045: Final validation of all AG assignments
 * 
 * Key Characteristics:
 * - Avoids "swiss-army-knife syndrome" by focusing on AG assignment work
 * - Supports iterative, interactive decision-making with 25-60 DrawObjs
 * - Enables visual feedback for user decisions
 * - Allows fuzzy logic during initial organization phase
 * - Enforces single AG per DrawObj only after user signal
 * 
 * Related Classes:
 * - TemplateProxyData: Working copy data model
 * - DrawObjsDisplayPanel: Displays DrawObjs in context
 * - AGsInfoFrameController: Handles user interactions
 * - AnchorPatternFrame: Manages AnchorPattern spreads
 */
public class AGsInfoFrame extends JFrame {
	private static final Logger logger = Logger.getLogger(AGsInfoFrame.class.getName());
	
	// Frame dimensions (matching TemplateWorkFrame)
	private static final int FRAME_WIDTH = 1000;
	private static final int FRAME_HEIGHT = 800;
	
	// General info panel height
	private static final int GENERAL_INFO_PANEL_HEIGHT = 105;
	
	// Font for filter method text area (10-point for compact display)
	private static final Font FILTER_METHOD_FONT = new Font("Monospaced", Font.PLAIN, 10);
	
	private TemplateProxyData templateProxyData;
	private Template originalTemplate;
	private AnchorPatternFrame anchorPatternFrame;
	private FilterState filterState;
	private RMatrixMiniPrelim rmatrix;
	private TemplateWorkFrame templateWorkFrame;
	
	// Currently selected shape from either wideSelectionListPanel or narrowSelectionListPanel
	private DrawObj currSingleSelectShape;
	
	// Main panels (renamed from TemplateWorkFrame)
	private ShapeListPanel wideSelectionListPanel;
	private ShapeDisplayPanel shapeDrawPanel;
	private SelectedShapesListPanel narrowSelectionListPanel;
	private JPanel narrowSelectionsPanel;
	
	// General info panel and components
	private JPanel generalInfoPanel;
	private JTextArea filterMethodTextArea;
	private JPanel filterTogglePanel;
	private JButton filterToggleButton;
	private JCheckBox filterShowImmediately;
	private JCheckBox useSelectAGsForToggle;
	private JPanel fourAGChoiceswChipsPanel;
	private JPanel showAGsButtonPanel;
	private JButton showSelectedAGsButton;
	private JSpinner rangeLimitCCWSpinner;
	
	// Store references to AG radio buttons for querying selected state
	private Map<Integer, JRadioButton> agRadioButtons = new HashMap<>();
	private JSpinner rangeLimitCWSpinner;
	private JButton showAllShapesButton;
    private JCheckBox invertFinalResultCheckbox;
	private JPanel assignAGForOneShapePanel;
	private JTextField currAGsTextfield;
	private JSpinner assignAGSpinner;
	private JCheckBox exclusiveAssignCheckbox;
	private JButton doAssignAGButton;
	
	// Color display
	private JPanel clickColorSquare;
	
	// Buttons
	private JButton shapeAGInfoButton;
	private JButton showCloseHueButton;
	
	// Labels
	private JLabel narrowSelectionsLabel;
	private JLabel alertLabel;
	
	// Menu system
	private JMenuBar menuBar;
	private JMenu showMenu;
	private JMenu issuesMenu;
	private JMenu validateMenu;
	private JMenu anchorPatternMenu;
    private JMenu helpMenu;
	
	// Display and its controller
	private DrawObjsDisplayPanel displayShapesPanel;
	private AGsInfoFrameController theController;
	
	/**
	 * Creates a new AGsInfoFrame for AG assignment work.
	 * 
	 * @param templateProxyData the TemplateProxyData to work with
	 * @param anchorPatternFrame the AnchorPatternFrame (Instance 2) for spread adjustments
	 * @param rmatrix the RMatrixMini for AG assignment queries and filtering
	 * @param templateWorkFrame the TemplateWorkFrame for HueRelativesFrame integration
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public AGsInfoFrame(TemplateProxyData templateProxyData, AnchorPatternFrame anchorPatternFrame, RMatrixMiniPrelim rmatrix, TemplateWorkFrame templateWorkFrame) {
		super("AGsInfoFrame - AG Assignment etc");
		
		if (templateProxyData == null) {
			throw new IllegalArgumentException("TemplateProxyData cannot be null");
		}
		if (anchorPatternFrame == null) {
			throw new IllegalArgumentException("AnchorPatternFrame cannot be null");
		}
		if (rmatrix == null) {
			throw new IllegalArgumentException("RMatrixMini cannot be null");
		}
		if (templateWorkFrame == null) {
			throw new IllegalArgumentException("TemplateWorkFrame cannot be null");
		}
		
		this.templateProxyData = templateProxyData;
		this.originalTemplate = templateProxyData.getOriginalTemplate();
		this.anchorPatternFrame = anchorPatternFrame;
		this.rmatrix = rmatrix;
		this.templateWorkFrame = templateWorkFrame;
		this.filterState = new FilterState();
		
		logger.info("AGsInfoFrame: Creating with TemplateProxyData, AnchorPatternFrame, RMatrixMini, and TemplateWorkFrame");
		
		initializeFrame();
	}
	
	/**
	 * Gets the TemplateProxyData associated with this frame.
	 * 
	 * @return the TemplateProxyData
	 */
	public TemplateProxyData getTemplateProxyData() {
		return templateProxyData;
	}
	
	/**
	 * Gets the original Template for visual comparison.
	 * 
	 * @return the original Template
	 */
	public Template getOriginalTemplate() {
		return originalTemplate;
	}
	
	/**
	 * Gets the AnchorPatternFrame (Instance 2) for spread adjustments.
	 * 
	 * @return the AnchorPatternFrame
	 */
	public AnchorPatternFrame getAnchorPatternFrame() {
		return anchorPatternFrame;
	}

	/**
	 * Gets the TemplateWorkFrame for HueRelativesFrame integration.
	 * 
	 * @return the TemplateWorkFrame
	 */
	public TemplateWorkFrame getTemplateWorkFrame() {
		return templateWorkFrame;
	}
	
	/**
	 * Gets the display panel.
	 * 
	 * @return the DrawObjsDisplayPanel
	 */
	public DrawObjsDisplayPanel getDisplayShapesPanel() {
		return displayShapesPanel;
	}
	
	/**
	 * Gets the controller.
	 * 
	 * @return the AGsInfoFrameController
	 */
	public AGsInfoFrameController getTheController() {
		return theController;
	}

	/**
	 * Gets the filter show immediately checkbox.
	 * 
	 * @return the filterShowImmediately checkbox
	 */
	public JCheckBox getFilterShowImmediately() {
		return filterShowImmediately;
	}

	/**
	 * Gets the use selected AGs for toggle checkbox.
	 * 
	 * @return the useSelectAGsForToggle checkbox
	 */
	public JCheckBox getUseSelectAGsForToggle() {
		return useSelectAGsForToggle;
	}

	/**
	 * Gets the filter toggle button.
	 * 
	 * @return the filterToggleButton
	 */
	public JButton getFilterToggleButton() {
		return filterToggleButton;
	}

	/**
	 * Gets the show selected AGs button.
	 * 
	 * @return the showSelectedAGsButton
	 */
	public JButton getShowSelectedAGsButton() {
		return showSelectedAGsButton;
	}

	/**
	 * Gets the CCW range limit spinner.
	 * 
	 * @return the rangeLimitCCWSpinner
	 */
	public JSpinner getRangeLimitCCWSpinner() {
		return rangeLimitCCWSpinner;
	}

	/**
	 * Gets the CW range limit spinner.
	 * 
	 * @return the rangeLimitCWSpinner
	 */
	public JSpinner getRangeLimitCWSpinner() {
		return rangeLimitCWSpinner;
	}

	/**
	 * Gets the show all shapes button.
	 * 
	 * @return the showAllShapesButton
	 */
	public JButton getShowAllShapesButton() {
		return showAllShapesButton;
	}

	/**
	 * Gets the FilterState managing the filtering state machine.
	 * 
	 * @return the FilterState
	 */
	public FilterState getFilterState() {
		return filterState;
	}

	/**
	 * Gets the RMatrixMini for AG assignment queries and filtering.
	 * 
	 * @return the RMatrixMini
	 */
	public RMatrixMiniPrelim getRMatrix() {
		return rmatrix;
	}

	/**
	 * Gets the currently selected shape from either list panel.
	 * 
	 * @return the currently selected DrawObj, or null if no shape is selected
	 */
	public DrawObj getCurrSingleSelectShape() {
		return currSingleSelectShape;
	}

	/**
	 * Gets the set of selected AG numbers from the radio buttons.
	 * Queries the stored radio button references to determine which AGs are selected.
	 * 
	 * @return Set of selected AG numbers (1-4), empty if none selected
	 */
	public Set<Integer> getSelectedAGs() {
		Set<Integer> selected = new HashSet<>();
		for (int i = 1; i <= 4; i++) {
			JRadioButton button = agRadioButtons.get(i);
			if (button != null && button.isSelected()) {
				selected.add(i);
			}
		}
		return selected;
	}

    /**
     * Gets the invertFinalResultCheckbox.
     *
     * @return the invertFinalResultCheckbox
     */
    public JCheckBox getInvertFinalResultCheckbox() {
        return invertFinalResultCheckbox;
    }

	/**
	 * Gets the current AGs textfield.
	 * 
	 * @return the currAGsTextfield
	 */
	public JTextField getCurrAGsTextfield() {
		return currAGsTextfield;
	}

	/**
	 * Gets the assign to AG spinner.
	 * 
	 * @return the assignAGSpinner
	 */
	public JSpinner getAssignAGSpinner() {
		return assignAGSpinner;
	}

	/**
	 * Gets the exclusive assign checkbox.
	 * 
	 * @return the exclusiveAssignCheckbox
	 */
	public JCheckBox getExclusiveAssignCheckbox() {
		return exclusiveAssignCheckbox;
	}

	/**
	 * Gets the do assign AG button.
	 * 
	 * @return the doAssignAGButton
	 */
	public JButton getDoAssignAGButton() {
		return doAssignAGButton;
	}

	/**
	 * Calculates the fixed bounds from all DrawObjs in the original Template.
	 * This bounds is independent and will not change, ensuring consistent scaling
	 * regardless of which subset of DrawObjs is displayed.
	 * 
	 * @return Rectangle2D representing the bounds of all DrawObjs, or null if template is empty
	 */
	private Rectangle2D calculateFixedBoundsFromTemplate() {
		if (originalTemplate == null) {
			logger.warning("AGsInfoFrame: Original template is null, cannot calculate fixed bounds");
			return null;
		}
		
		java.util.List<DrawObj> allDrawObjs = originalTemplate.getAllDrawObjs();
		if (allDrawObjs == null || allDrawObjs.isEmpty()) {
			logger.warning("AGsInfoFrame: No DrawObjs in template, cannot calculate fixed bounds");
			return null;
		}
		
		// Calculate bounds from all DrawObjs
		Rectangle2D bounds = allDrawObjs.get(0).getBounds();
		for (int i = 1; i < allDrawObjs.size(); i++) {
			bounds = bounds.createUnion(allDrawObjs.get(i).getBounds());
		}
		
		logger.fine("AGsInfoFrame: Calculated fixed bounds from " + allDrawObjs.size() + " DrawObjs: " + bounds);
		return bounds;
	}

	// ============================================================================
	// UPDATE METHODS (Layer 3: View - Pure Display/Rendering)
	// ============================================================================

	/**
	 * Updates the shape display panel with a new array of DrawObjs.
	 * 
	 * This is a package-private update method called only by the controller.
	 * It performs pure rendering with no business logic.
	 * 
	 * @param drawObjs Array of DrawObj instances to display
	 */
	void updateShapeDisplay(DrawObj[] drawObjs) {
		logger.fine("AGsInfoFrame: Updating shape display with " + (drawObjs != null ? drawObjs.length : 0) + " DrawObjs");
		
		if (shapeDrawPanel != null) {
			shapeDrawPanel.setDrawObjs(drawObjs);
			shapeDrawPanel.repaint();
		}
	}

	/**
	 * Updates the wide selection list panel with a new array of DrawObjs.
	 * 
	 * This is a package-private update method called only by the controller.
	 * It performs pure rendering with no business logic.
	 * 
	 * @param drawObjs Array of DrawObj instances to display in wideSelectionListPanel
	 */
	void updateWideSelectionList(DrawObj[] drawObjs) {
		logger.fine("AGsInfoFrame: Updating wide selection list with " + (drawObjs != null ? drawObjs.length : 0) + " DrawObjs");
		
		if (wideSelectionListPanel != null) {
			wideSelectionListPanel.setDrawObjs(drawObjs);
		}
	}

	/**
	 * Updates the narrow selection list panel with a new array of DrawObjs.
	 * 
	 * This is a package-private update method called only by the controller.
	 * It performs pure rendering with no business logic.
	 * 
	 * @param drawObjs Array of DrawObj instances to display in narrowSelectionListPanel
	 */
	void updateNarrowSelectionList(DrawObj[] drawObjs) {
		logger.fine("AGsInfoFrame: Updating narrow selection list with " + (drawObjs != null ? drawObjs.length : 0) + " DrawObjs");
		
		if (narrowSelectionListPanel != null) {
			narrowSelectionListPanel.setDrawObjs(drawObjs);
		}
	}

	/**
	 * Updates the selection list panels with selected DrawObj UUIDs.
	 * 
	 * This is a package-private update method called only by the controller.
	 * It performs pure rendering with no business logic.
	 * 
	 * @param selectedUUIDs Set of selected DrawObj UUIDs
	 */
	void updateSelectionLists(Set<UUID> selectedUUIDs) {
		logger.fine("AGsInfoFrame: Updating selection lists with " + (selectedUUIDs != null ? selectedUUIDs.size() : 0) + " selected UUIDs");
		
		// Placeholder: Will be implemented when selection list functionality is needed
		// This would update wideSelectionListPanel and narrowSelectionListPanel
	}

	/**
	 * Shows a message to the user.
	 * 
	 * This is a package-private method called only by the controller.
	 * It performs pure display with no business logic.
	 * 
	 * @param message The message to display
	 */
	void showMessage(String message) {
		logger.info("AGsInfoFrame: Showing message: " + message);
		
		// Placeholder: Will be implemented with proper message display
		// Could use filterMethodTextArea or a dialog box
	}

	/**
	 * Updates UI components based on the currently selected shape.
	 * Called when user selects a shape in either wideSelectionListPanel or narrowSelectionListPanel.
	 * 
	 * Updates:
	 * - clickColorSquare: Shows the color of the selected shape
	 * - currAGsTextfield: Shows current AG assignments for the shape
	 * - Other UI elements as needed
	 * 
	 * @param selectedShape the DrawObj that was selected, or null if selection cleared
	 */
	private void updateUIForSelectedShape(DrawObj selectedShape) {
		logger.fine("AGsInfoFrame: Updating UI for selected shape: " + (selectedShape != null ? selectedShape.toString() : "null"));
		
		// Update the current selection
		currSingleSelectShape = selectedShape;
		
		if (selectedShape == null) {
			// Clear UI when no shape is selected
			if (clickColorSquare != null) {
				clickColorSquare.setBackground(Color.WHITE);
				clickColorSquare.repaint();
			}
			if (currAGsTextfield != null) {
				currAGsTextfield.setText("");
			}
			return;
		}
		
		// Update color square with shape's color
		updateColorSquareForShape(selectedShape);
		
		// Update AG assignments textfield
		updateAGAssignmentsForShape(selectedShape);
	}

	/**
	 * Updates the clickColorSquare to display the color of the given shape.
	 * 
	 * @param shape the DrawObj whose color should be displayed
	 */
	private void updateColorSquareForShape(DrawObj shape) {
		if (clickColorSquare == null || shape == null) {
			return;
		}
		
		try {
			// Get the color from the shape
			ColorPtOstDot ostColor = shape.getColorPtOstDotFillColor();
			if (ostColor != null) {
				Color shapeColor = ostColor.toAWTColor();
				clickColorSquare.setBackground(shapeColor);
				logger.fine("AGsInfoFrame: Updated color square with shape color: " + shapeColor);
			} else {
				clickColorSquare.setBackground(Color.WHITE);
				logger.fine("AGsInfoFrame: Shape has no color, using white");
			}
			clickColorSquare.repaint();
		} catch (Exception e) {
			logger.warning("AGsInfoFrame: Error updating color square: " + e.getMessage());
			clickColorSquare.setBackground(Color.WHITE);
		}
	}

	/**
	 * Updates the currAGsTextfield to display AG assignments for the given shape.
	 * 
	 * @param shape the DrawObj whose AG assignments should be displayed
	 */
	private void updateAGAssignmentsForShape(DrawObj shape) {
		if (currAGsTextfield == null || shape == null) {
			return;
		}
		
		try {
			// Get AG assignments from RMatrixMini using the shape's UUID
			UUID shapeUUID = shape.getUniqueId();
			Set<Integer> ags = rmatrix.getDrawObjAGs(shapeUUID);
			
			if (ags == null || ags.isEmpty()) {
				currAGsTextfield.setText("(none)");
				logger.fine("AGsInfoFrame: Shape has no AG assignments");
			} else {
				// Sort AG numbers for consistent display
				java.util.List<Integer> sortedAGs = new java.util.ArrayList<>(ags);
				java.util.Collections.sort(sortedAGs);
				String agString = sortedAGs.stream()
					.map(String::valueOf)
					.reduce((a, b) -> a + ", " + b)
					.orElse("(none)");
				currAGsTextfield.setText(agString);
				logger.fine("AGsInfoFrame: Updated AG assignments: " + agString);
			}
		} catch (Exception e) {
			logger.warning("AGsInfoFrame: Error updating AG assignments: " + e.getMessage());
			currAGsTextfield.setText("(error)");
		}
	}

	/**
	 * Displays all DrawObjs in the template.
	 * Wired to: Show Menu > Show All Shapes
	 * 
	 * Sets first criterion to FilterType.ALL (show all shapes including unassigned).
	 * Transitions to ONE_STEP mode.
	 * Displays subset1.
	 */
	public void displayAllDrawObjs() {
		logger.fine("AGsInfoFrame: Menu action - Show All Shapes");
		
		// Update FilterState: first criterion = ALL (show all shapes)
		filterState.setFirstCriterion(FilterState.FilterType.ALL, null);
		
		// Auto-update display (orchestrates all display updates)
		if (theController != null) {
			theController.autoUpdateDrawSkeleton(filterState);
		}
	}
	
	/**
	 * Updates the display to show DrawObjs for a specific AG.
	 * 
	 * @param agNum the AG number (1-4)
	 */
	public void displayDrawObjsForAG(int agNum) {
		logger.info("AGsInfoFrame: Displaying DrawObjs for AG " + agNum);
		if (displayShapesPanel != null) {
			displayShapesPanel.displayDrawObjsForAG(agNum);
		}
	}
	
	/**
	 * Updates the display to show DrawObjs in multiple AGs.
	 */
	public void displayDrawObjsInMultipleAGs() {
		logger.info("AGsInfoFrame: Displaying DrawObjs in multiple AGs");
		if (displayShapesPanel != null) {
			displayShapesPanel.displayDrawObjsInMultipleAGs();
		}
	}
	
	/**
	 * Updates the display to show DrawObjs outside spread limits.
	 */
	public void displayDrawObjsOutsideSpreads() {
		logger.info("AGsInfoFrame: Displaying DrawObjs outside spreads");
		if (displayShapesPanel != null) {
			displayShapesPanel.displayDrawObjsOutsideSpreads();
		}
	}
	
	/**
	 * Updates the display to show DrawObjs with no AG assignment.
	 */
	public void displayDrawObjsWithNoAG() {
		logger.info("AGsInfoFrame: Displaying DrawObjs with no AG assignment");
		if (displayShapesPanel != null) {
			displayShapesPanel.displayDrawObjsWithNoAG();
		}
	}
	
	/**
	 * Initializes the frame components.
	 */
	private void initializeFrame() {
		// Initialize theController
		theController = new AGsInfoFrameController(this);
		
		// Initialize display panel
		displayShapesPanel = new DrawObjsDisplayPanel(templateProxyData);
		
		// Setup menus
		setupMenus();
		
		// Create main panels
		initializeMainPanels();
		
		// Wire button listeners
		wireButtonListeners();
		
		// Setup layout
		setupLayout();
		
		// Configure frame
		setSize(FRAME_WIDTH, FRAME_HEIGHT);
		setResizable(true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null); // Center on screen
		
		logger.fine("AGsInfoFrame: Frame initialization complete");
	}
	
	/**
	 * Initializes the main panels.
	 */
	private void initializeMainPanels() {
		// Create wide selection list panel
		wideSelectionListPanel = new ShapeListPanel();
		wideSelectionListPanel.getJList().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// Create shape draw panel
		shapeDrawPanel = new ShapeDisplayPanel();
		
		// Calculate and set fixed bounds from all DrawObjs in the original Template
		// This ensures consistent scaling regardless of which subset is displayed
		Rectangle2D fixedBounds = calculateFixedBoundsFromTemplate();
		if (fixedBounds != null) {
			shapeDrawPanel.setFixedTotalBounds(fixedBounds);
			logger.fine("AGsInfoFrame: Fixed bounds set for consistent scaling: " + fixedBounds);
		} else {
			logger.warning("AGsInfoFrame: Could not calculate fixed bounds from template");
		}
		
		// Create narrow selection list panel
		narrowSelectionListPanel = new SelectedShapesListPanel();
		narrowSelectionListPanel.setPreferredSize(new Dimension(150, 100));
		
		// Create narrow selections panel (color work panel)
		narrowSelectionsPanel = createNarrowSelectionsPanel();
		
		// Create general info panel
		generalInfoPanel = createGeneralInfoPanel();
		
		logger.fine("AGsInfoFrame: Main panels initialized");
	}
	
	/**
	 * Creates the narrow selections panel (equivalent to colorWorkPanel in TemplateWorkFrame).
	 */
	private JPanel createNarrowSelectionsPanel() {
		JPanel panel = new JPanel();
		panel.setMinimumSize(new Dimension(140, 120));
		panel.setPreferredSize(new Dimension(160, 120));
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		// Create click color square
		clickColorSquare = new JPanel();
		clickColorSquare.setPreferredSize(new Dimension(100, 50));
		clickColorSquare.setBackground(Color.WHITE);
		clickColorSquare.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		panel.add(clickColorSquare);
		
		// Create shape AG info button
		shapeAGInfoButton = new JButton("Spread location");
		shapeAGInfoButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(shapeAGInfoButton);
		
		// Create alert label
		alertLabel = new JLabel(" ");
		alertLabel.setForeground(Color.RED);
		alertLabel.setVisible(false);
		panel.add(alertLabel);
		
		// Create narrow selections label
		narrowSelectionsLabel = new JLabel("Filtered results:");
		narrowSelectionsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(narrowSelectionsLabel);
		
		// Add narrow selection list panel
		panel.add(narrowSelectionListPanel);
		
		// Create show hue relatives button
		showCloseHueButton = new JButton("show closeHue");
		showCloseHueButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(showCloseHueButton);
		
		return panel;
	}
	
	/**
	 * Creates the general info panel at the bottom.
	 * Contains filterMethodTextArea (left), filterTogglePanel, fourAGChoiceswChipsPanel, and showAGsButtonPanel (right).
	 */
	private JPanel createGeneralInfoPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		panel.setPreferredSize(new Dimension(160, GENERAL_INFO_PANEL_HEIGHT));
		panel.setMaximumSize(new Dimension(160, GENERAL_INFO_PANEL_HEIGHT));
		panel.setMinimumSize(new Dimension(160, GENERAL_INFO_PANEL_HEIGHT));
		panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		
		// Create filter method text area (left side, flush against left edge)
		filterMethodTextArea = new JTextArea();
		filterMethodTextArea.setPreferredSize(new Dimension(280, GENERAL_INFO_PANEL_HEIGHT));
		filterMethodTextArea.setFont(FILTER_METHOD_FONT);
		filterMethodTextArea.setText("Filtering method description will display here.\nTwo short sentences to start.");
		filterMethodTextArea.setLineWrap(true);
		filterMethodTextArea.setWrapStyleWord(true);
		filterMethodTextArea.setEditable(false);
		filterMethodTextArea.setOpaque(true);
		panel.add(filterMethodTextArea);
		
		// Create filter toggle panel (center, fixed 100px width)
		filterTogglePanel = createFilterTogglePanel();
		panel.add(filterTogglePanel);
		
		// Create four AG choices panel
		fourAGChoiceswChipsPanel = createFourAGChoicesPanel();
		panel.add(fourAGChoiceswChipsPanel);
		
		// Create show AGs button panel (right side, same size as filterTogglePanel)
		showAGsButtonPanel = createShowAGsButtonPanel();
		panel.add(showAGsButtonPanel);
		
		// Create assign AG for one shape panel
		assignAGForOneShapePanel = createAssignAGForOneShapePanel();
		panel.add(assignAGForOneShapePanel);
		
		return panel;
	}
	
	/**
	 * Creates the show AGs button panel with one button, hue range spinners, and checkboxes stacked vertically.
	 * Panel is 190 pixels wide and expands to fill available height in generalInfoPanel.
	 */
	private JPanel createShowAGsButtonPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setPreferredSize(new Dimension(190, GENERAL_INFO_PANEL_HEIGHT));
		panel.setMaximumSize(new Dimension(190, Integer.MAX_VALUE));
		panel.setMinimumSize(new Dimension(190, GENERAL_INFO_PANEL_HEIGHT));
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		// Create show selected AGs button
		showSelectedAGsButton = new JButton("showSelected AGs");
		showSelectedAGsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		showSelectedAGsButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 17));
		panel.add(showSelectedAGsButton);
		
		// Create hue range spinners row
		JPanel spinnersRow = new JPanel();
		spinnersRow.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
		spinnersRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		
		// CCW label
		JLabel ccwLabel = new JLabel("CCW");
		ccwLabel.setPreferredSize(new Dimension(30, 16));
		spinnersRow.add(ccwLabel);
		
		// CCW spinner
		rangeLimitCCWSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 36, 1));
		rangeLimitCCWSpinner.setPreferredSize(new Dimension(50, 20));
		spinnersRow.add(rangeLimitCCWSpinner);
		
		// CW spinner
		rangeLimitCWSpinner = new JSpinner(new SpinnerNumberModel(36, 1, 36, 1));
		rangeLimitCWSpinner.setPreferredSize(new Dimension(50, 20));
		spinnersRow.add(rangeLimitCWSpinner);
		
		// CW label
		JLabel cwLabel = new JLabel("CW");
		cwLabel.setPreferredSize(new Dimension(30, 16));
		spinnersRow.add(cwLabel);
		
		panel.add(spinnersRow);
		
		// Create show all shapes button
		showAllShapesButton = new JButton("filter1: show All Shapes");
		showAllShapesButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		showAllShapesButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 17));
		panel.add(showAllShapesButton);

        // Create invertFinalResultCheckbox
        invertFinalResultCheckbox = new JCheckBox("invert finalResult");
        invertFinalResultCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(invertFinalResultCheckbox);
		
		// Add vertical glue to push components to top
		panel.add(Box.createVerticalGlue());
		
		return panel;
	}

	/**
	 * Creates the filter toggle panel with button and checkboxes stacked vertically.
	 * Panel is 170 pixels wide and expands to fill available height in generalInfoPanel.
	 */
	private JPanel createFilterTogglePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setPreferredSize(new Dimension(190, GENERAL_INFO_PANEL_HEIGHT));
		panel.setMaximumSize(new Dimension(190, Integer.MAX_VALUE));
		panel.setMinimumSize(new Dimension(190, GENERAL_INFO_PANEL_HEIGHT));
		
		// Create filter toggle button
		filterToggleButton = new JButton("Toggle Filter On/Off");
		filterToggleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		filterToggleButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		panel.add(filterToggleButton);
		
		// Add vertical spacing
		panel.add(Box.createVerticalStrut(5));
		
		// Create filter show immediately checkbox
		filterShowImmediately = new JCheckBox("Show immediately");
		filterShowImmediately.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(filterShowImmediately);
		
		// Add vertical spacing
		panel.add(Box.createVerticalStrut(5));
		
		// Create use selected AGs for toggle checkbox
		useSelectAGsForToggle = new JCheckBox("Toggle w selectAGs");
		useSelectAGsForToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(useSelectAGsForToggle);
		
		// Add vertical glue to push components to top
		panel.add(Box.createVerticalGlue());
		
		return panel;
	}

	/**
	 * Creates the assign AG for one shape panel with textfield, spinner, checkbox, and button stacked vertically.
	 * Panel is 190 pixels wide and expands to fill available height in generalInfoPanel.
	 */
	private JPanel createAssignAGForOneShapePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setPreferredSize(new Dimension(190, GENERAL_INFO_PANEL_HEIGHT));
		panel.setMaximumSize(new Dimension(190, Integer.MAX_VALUE));
		panel.setMinimumSize(new Dimension(190, GENERAL_INFO_PANEL_HEIGHT));
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		// Create textfield row with label
		JPanel textfieldRow = new JPanel();
		textfieldRow.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
		textfieldRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		
		JLabel currAGLabel = new JLabel("currAG assigns");
		currAGLabel.setPreferredSize(new Dimension(100, 16));
		textfieldRow.add(currAGLabel);
		
		currAGsTextfield = new JTextField();
		currAGsTextfield.setPreferredSize(new Dimension(80, 20));
		currAGsTextfield.setEditable(false);
		textfieldRow.add(currAGsTextfield);
		
		panel.add(textfieldRow);
		
		// Create spinner row with label
		JPanel spinnerRow = new JPanel();
		spinnerRow.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
		spinnerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		
		JLabel assignAGLabel = new JLabel("assign to AG");
		assignAGLabel.setPreferredSize(new Dimension(90, 16));
		spinnerRow.add(assignAGLabel);
		
		assignAGSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
		assignAGSpinner.setPreferredSize(new Dimension(50, 20));
		spinnerRow.add(assignAGSpinner);
		
		panel.add(spinnerRow);
		
		// Create exclusive assign checkbox
		exclusiveAssignCheckbox = new JCheckBox("clear other AG assigns");
		exclusiveAssignCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(exclusiveAssignCheckbox);
		
		// Create do assign AG button
		doAssignAGButton = new JButton("do the AG assign");
		doAssignAGButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		doAssignAGButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 17));
		panel.add(doAssignAGButton);
		
		// Add vertical glue to push components to top
		panel.add(Box.createVerticalGlue());
		
		return panel;
	}

	/**
	 * Creates the four AG choices panel with four rows for AG1-AG4.
	 * Expands to fill available height in generalInfoPanel.
	 */
	private JPanel createFourAGChoicesPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		// Create rows for each AG - each row expands equally to fill available height
		for (int i = 1; i <= 4; i++) {
			JPanel agRow = createAGRow(i);
			panel.add(agRow);
		}
		
		return panel;
	}
	
	/**
	 * Creates a single row for an AG with label, color panels, radio button, and text field.
	 * Each row expands to fill its share of the available height.
	 * Uses GridBagLayout to prevent component overlap.
	 */
	private JPanel createAGRow(int agNum) {
		JPanel row = new JPanel();
		GridBagLayout gbl = new GridBagLayout();
		row.setLayout(gbl);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets = new Insets(0, 1, 0, 1);
		
		// Label
		JLabel agLabel = new JLabel("AG" + agNum);
		agLabel.setPreferredSize(new Dimension(30, 16));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		row.add(agLabel, gbc);
		
		// Anchor hue color panel
		JPanel anchorColorPanel = new JPanel();
		anchorColorPanel.setPreferredSize(new Dimension(16, 16));
		anchorColorPanel.setBackground(Color.GRAY);
		anchorColorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		gbc.gridx = 1;
		row.add(anchorColorPanel, gbc);
		
		// CW limit color panel
		JPanel cwLimitColorPanel = new JPanel();
		cwLimitColorPanel.setPreferredSize(new Dimension(16, 16));
		cwLimitColorPanel.setBackground(Color.LIGHT_GRAY);
		cwLimitColorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		gbc.gridx = 2;
		row.add(cwLimitColorPanel, gbc);
		
		// CCW limit color panel
		JPanel ccwLimitColorPanel = new JPanel();
		ccwLimitColorPanel.setPreferredSize(new Dimension(16, 16));
		ccwLimitColorPanel.setBackground(Color.LIGHT_GRAY);
		ccwLimitColorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		gbc.gridx = 3;
		row.add(ccwLimitColorPanel, gbc);
		
		// Radio button
		JRadioButton agRadioButton = new JRadioButton();
		agRadioButton.setPreferredSize(new Dimension(16, 16));
		gbc.gridx = 4;
		row.add(agRadioButton, gbc);
		
		// Store reference to radio button for later querying
		agRadioButtons.put(agNum, agRadioButton);
		
		// Hue number text field
		JTextField hueTextField = new JTextField();
		hueTextField.setPreferredSize(new Dimension(30, 16));
		hueTextField.setText("0");
		gbc.gridx = 5;
		gbc.weightx = 1.0;
		row.add(hueTextField, gbc);
		
		return row;
	}
	
	/**
	 * Sets up the menu system.
	 */
	private void setupMenus() {
		menuBar = new JMenuBar();
		
		// Show Menu
		showMenu = new JMenu("Show (filter1)");
        showMenu.add(new JMenuItem("Show Hue Range")).addActionListener(e -> displayDrawObjsForHueRange());
        showMenu.addSeparator();
		showMenu.add(new JMenuItem("Show AG1")).addActionListener(e -> handleShowAGMenuSelection(1));
		showMenu.add(new JMenuItem("Show AG2")).addActionListener(e -> handleShowAGMenuSelection(2));
		showMenu.add(new JMenuItem("Show AG3")).addActionListener(e -> handleShowAGMenuSelection(3));
		showMenu.add(new JMenuItem("Show AG4")).addActionListener(e -> handleShowAGMenuSelection(4));
		showMenu.addSeparator();
        showMenu.add(new JMenuItem("Show Selected AGs")).addActionListener(e -> displaySelectedAGsDrawObjs());
        showMenu.add(new JMenuItem("Show All AGs")).addActionListener(e -> displayAllAGsDrawObjs());
		showMenu.add(new JMenuItem("Show All Shapes")).addActionListener(e -> displayAllDrawObjs());
        showMenu.addSeparator();
        showMenu.add(new JMenuItem("CLEAR")).addActionListener(e -> clearFilterState());
		menuBar.add(showMenu);
		
		// Issues Menu
		issuesMenu = new JMenu("Issues (filter2)");
		issuesMenu.add(new JMenuItem("Check for multiple AGs")).addActionListener(e -> handleCheckMultipleAGs());
		issuesMenu.add(new JMenuItem("Check within Spreads")).addActionListener(e -> handleCheckWithinSpreads());
		issuesMenu.add(new JMenuItem("Check no AG assigned")).addActionListener(e -> handleCheckNoAGAssigned());
        issuesMenu.addSeparator();
        issuesMenu.add(new JMenuItem("CLEAR - no ISSUE filter")).addActionListener(e -> handleClearIssuesFilter());
		menuBar.add(issuesMenu);
		
		// Validate Menu
		validateMenu = new JMenu("Validate");
		validateMenu.add(new JMenuItem("Validate single AG per DrawObj")).addActionListener(e -> theController.validateSingleAGPerDrawObj());
		validateMenu.add(new JMenuItem("Validate all AGs")).addActionListener(e -> theController.validateAllAGs());
		menuBar.add(validateMenu);
		
		// AnchorPattern Menu
		anchorPatternMenu = new JMenu("AnchorPattern");
		anchorPatternMenu.add(new JMenuItem("Open AnchorPatternFrame")).addActionListener(e -> anchorPatternFrame.setVisible(true));
		menuBar.add(anchorPatternMenu);

		// AGMembers Menu
		JMenu agMembersMenu = new JMenu("AGMembers");
		agMembersMenu.add(new JMenuItem("select from AG1")).addActionListener(e -> openAGMemberSelectFrame(1));
		agMembersMenu.add(new JMenuItem("select from AG2")).addActionListener(e -> openAGMemberSelectFrame(2));
		agMembersMenu.add(new JMenuItem("select from AG3")).addActionListener(e -> openAGMemberSelectFrame(3));
		agMembersMenu.add(new JMenuItem("select from AG4")).addActionListener(e -> openAGMemberSelectFrame(4));
		menuBar.add(agMembersMenu);
		// Help Menu
		helpMenu = new JMenu("Help");
		helpMenu.add(new JMenuItem("Using AGsInfoFrame")).addActionListener(e -> displayUsingAGsInfoFrameHelp());
		menuBar.add(helpMenu);


		setJMenuBar(menuBar);
		logger.fine("AGsInfoFrame: Menus setup complete");
	}




    /**
     * Clears the filter state and resets display.
     * Wired to: Show Menu > CLEAR
     * 
     * Resets FilterState to ONE_STEP mode with no filters applied.
     * Displays subset1 (first criterion only).
     */
    private void clearFilterState() {
        logger.info("AGsInfoFrame: Menu action - CLEAR (Show menu)");
        
        // Reset FilterState to initial state
        filterState.clearAllFilters();
        
        // Auto-update display (orchestrates all display updates)
        if (theController != null) {
            theController.autoUpdateDrawSkeleton(filterState);
        }
        
        logger.fine("AGsInfoFrame: Filter state cleared, returned to ONE_STEP mode");
    } // end method clearFilterState




    private void displayUsingAGsInfoFrameHelp() {
        try {
            HelpTextWindow helpWindow = new HelpTextWindow("Using AGsInfoFrame", "help/usingAGsInfoFrame.rtf");
            helpWindow.setVisible(true);
            logger.fine("AGsInfoFrame: Opened 'Using AGsInfoFrame' help window");
        } catch (Exception e) {
            logger.warning("AGsInfoFrame: Failed to open help window: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Could not open help file: " + e.getMessage(), 
                "Help Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

	/**
	 * Opens an AGMemberSelectFrame for the specified AG.
	 * Wired to: AGMembers Menu > select from AG1/AG2/AG3/AG4
	 * 
	 * Validates that the AG has members before opening the frame.
	 * If the AG has no members, displays an error message and does not open the frame.
	 * 
	 * @param agNum the AG number (1-4)
	 */
	private void openAGMemberSelectFrame(int agNum) {
		logger.fine("AGsInfoFrame: Menu action - select from AG" + agNum);
		
		// Validate that the AG has members
		if (rmatrix == null) {
			logger.warning("AGsInfoFrame: RMatrixMini is null");
			JOptionPane.showMessageDialog(this,
				"Error: RMatrixMini is not initialized",
				"Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// Check if the AG has any members
		Set<UUID> agMembers = rmatrix.getAGMembersUUIDs(agNum);
		if (agMembers == null || agMembers.isEmpty()) {
			logger.fine("AGsInfoFrame: AG" + agNum + " has no members");
			JOptionPane.showMessageDialog(this,
				"AG" + agNum + " has no members",
				"No Members in AG",
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		try {
			// Get the currently selected shape's hue from AGsInfoFrame's list selection, or use default hue 1
			int userFacingHue = 1;
			if (currSingleSelectShape != null) {
				ColorPtOstDot shapeColor = currSingleSelectShape.getColorPtOstDotFillColor();
				if (shapeColor != null) {
					userFacingHue = shapeColor.getOstHCS().getStdCodeHueNum() + 1;
				}
			}
			
			// Create AGSelectFrameController
			com.csurf.huepattern.controller.AGSelectFrameController controller = 
				new com.csurf.huepattern.controller.AGSelectFrameController(
					agNum,
					userFacingHue,
					rmatrix,
					templateWorkFrame.getDrawObjRoster(),
				templateWorkFrame
				);
			
			// Create AGMemberSelectFrame
			com.csurf.huepattern.view.AGMemberSelectFrame frame = 
				new com.csurf.huepattern.view.AGMemberSelectFrame(controller);
			
			// Set the central hue spinner to the correct value (two-step process)
			// frame.setCentralHue(userFacingHue); // 112225 took out - set in constructor from controller
			
			frame.setVisible(true);
			
			logger.fine("AGsInfoFrame: Opened AGMemberSelectFrame for AG" + agNum);
		} catch (Exception e) {
			logger.warning("AGsInfoFrame: Error opening AGMemberSelectFrame: " + e.getMessage());
			JOptionPane.showMessageDialog(this,
				"Error opening AGMemberSelectFrame: " + e.getMessage(),
				"Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}

    /**
     * Displays all DrawObjs that have been assigned to at least one AG.
     * Wired to: Show Menu > Show All AGs
     * 
     * Sets first criterion to FilterType.ALLAGS (show all shapes with AG assignments).
     * Transitions to ONE_STEP mode.
     * Displays subset1.
     */
    private void displayAllAGsDrawObjs() {
        logger.fine("AGsInfoFrame: Menu action - Show All AGs");
        
        // Update FilterState: first criterion = ALLAGS (show all shapes assigned to at least one AG)
        filterState.setFirstCriterion(FilterState.FilterType.ALLAGS, null);
        
        // Auto-update display (orchestrates all display updates)
        if (theController != null) {
            theController.autoUpdateDrawSkeleton(filterState);
        }
    }

    /**
     * Displays DrawObjs from selected AGs.
     * Wired to: Show Menu > Show Selected AGs
     * 
     * Sets first criterion to FilterType.SELECTED_AGs with selected AG numbers.
     * Transitions to ONE_STEP mode.
     * Displays subset1.
     */
    private void displaySelectedAGsDrawObjs() {
        logger.fine("AGsInfoFrame: Menu action - Show Selected AGs");
        
        // Get selected AG numbers from the radio buttons
        Set<Integer> selectedAGs = getSelectedAGs();
        
        if (selectedAGs.isEmpty()) {
            logger.warning("AGsInfoFrame: No AGs selected for 'Show Selected AGs'");
            JOptionPane.showMessageDialog(this, 
                "Please select one or more AGs first", 
                "No AGs Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Update FilterState: first criterion = SELECTED_AGs
        filterState.setFirstCriterion(FilterState.FilterType.SELECTED_AGs, selectedAGs);
        
        // Auto-update display (orchestrates all display updates)
        if (theController != null) {
            theController.autoUpdateDrawSkeleton(filterState);
        }
    }

    /**
     * Displays DrawObjs in the specified hue range.
     * Wired to: Show Menu > Show Hue Range
     * Uses rangeLimitCCWSpinner and rangeLimitCWSpinner for hue limits
     * 
     * Sets first criterion to FilterType.HUE_RANGE with CCW and CW limits.
     * Transitions to ONE_STEP mode.
     * Displays subset1.
     */
    private void displayDrawObjsForHueRange() {
        logger.fine("AGsInfoFrame: Menu action - Show Hue Range");
        
        if (rangeLimitCCWSpinner == null || rangeLimitCWSpinner == null) {
            logger.warning("AGsInfoFrame: Hue range spinners not initialized");
            return;
        }
        
        int ccwLimit = ((Number) rangeLimitCCWSpinner.getValue()).intValue();
        int cwLimit = ((Number) rangeLimitCWSpinner.getValue()).intValue();
        
        // Validate hue range (1-36)
        if (ccwLimit < 1 || ccwLimit > 36 || cwLimit < 1 || cwLimit > 36) {
            logger.warning("AGsInfoFrame: Invalid hue range: CCW=" + ccwLimit + ", CW=" + cwLimit);
            JOptionPane.showMessageDialog(this, 
                "Hue values must be between 1 and 36", 
                "Invalid Hue Range", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Create HueRange object for filter value
        HueRange hueRange = new HueRange(ccwLimit, cwLimit);
        
        // Update FilterState: first criterion = HUE_RANGE
        filterState.setFirstCriterion(FilterState.FilterType.HUE_RANGE, hueRange);
        
        // Auto-update display (orchestrates all display updates)
        if (theController != null) {
            theController.autoUpdateDrawSkeleton(filterState);
        }
    }

    /**
     * Displays DrawObjs for a specific AG.
     * Wired to: Show Menu > Show AG1, Show AG2, Show AG3, Show AG4
     * 
     * Sets first criterion to FilterType.AG with AG number.
     * Transitions to ONE_STEP mode.
     * Displays subset1.
     * 
     * @param agNum the AG number (1-4)
     */
    private void handleShowAGMenuSelection(int agNum) {
        logger.fine("AGsInfoFrame: Menu action - Show AG" + agNum);
        
        // Validate AG number
        if (agNum < 1 || agNum > 4) {
            logger.warning("AGsInfoFrame: Invalid AG number: " + agNum);
            return;
        }
        
        // Update FilterState: first criterion = AG
        filterState.setFirstCriterion(FilterState.FilterType.AG, agNum);
        
        // Auto-update display (orchestrates all display updates)
        if (theController != null) {
            theController.autoUpdateDrawSkeleton(filterState);
        }
    }

    /**
     * Handles "Check for multiple AGs" from Issues menu.
     * Wired to: Issues Menu > Check for multiple AGs
     * 
     * Sets second criterion to IssuesFilterType.MULTIPLE_AGs.
     * Transitions from ONE_STEP to TWO_STEP mode.
     * Displays subset2.
     */
    private void handleCheckMultipleAGs() {
        logger.fine("AGsInfoFrame: Menu action - Check for multiple AGs");
        
        // Update FilterState: second criterion = MULTIPLE_AGs
        filterState.setSecondCriterion(FilterState.IssuesFilterType.MULTIPLE_AGs);
        
        // Auto-update display (orchestrates all display updates)
        if (theController != null) {
            theController.autoUpdateDrawSkeleton(filterState);
        }
    }

    /**
     * Handles "Check within Spreads" from Issues menu.
     * Wired to: Issues Menu > Check within Spreads
     * 
     * Sets second criterion to IssuesFilterType.WITHIN_SPREADS.
     * Transitions from ONE_STEP to TWO_STEP mode.
     * Displays subset2.
     */
    private void handleCheckWithinSpreads() {
        logger.fine("AGsInfoFrame: Menu action - Check within Spreads");
        
        // Update FilterState: second criterion = WITHIN_SPREADS
        filterState.setSecondCriterion(FilterState.IssuesFilterType.WITHIN_SPREADS);
        
        // Auto-update display (orchestrates all display updates)
        if (theController != null) {
            theController.autoUpdateDrawSkeleton(filterState);
        }
    }

    /**
     * Handles "Check no AG assigned" from Issues menu.
     * Wired to: Issues Menu > Check no AG assigned
     * 
     * Sets second criterion to IssuesFilterType.NO_AG_ASSIGNED.
     * Transitions from ONE_STEP to TWO_STEP mode.
     * Displays subset2.
     */
    private void handleCheckNoAGAssigned() {
        logger.fine("AGsInfoFrame: Menu action - Check no AG assigned");
        
        // Update FilterState: second criterion = NO_AG_ASSIGNED
        filterState.setSecondCriterion(FilterState.IssuesFilterType.NO_AG_ASSIGNED);
        
        // Auto-update display (orchestrates all display updates)
        if (theController != null) {
            theController.autoUpdateDrawSkeleton(filterState);
        }
    }

    /**
     * Handles "CLEAR - no ISSUE filter" from Issues menu.
     * Wired to: Issues Menu > CLEAR - no ISSUE filter
     * 
     * Clears second criterion (Issues filter).
     * Transitions from TWO_STEP back to ONE_STEP mode.
     * Displays subset1.
     * Issues filter will be re-applied with default (MULTIPLE_AGs) if user enters THREE_STEP.
     */
    private void handleClearIssuesFilter() {
        logger.fine("AGsInfoFrame: Menu action - CLEAR - no ISSUE filter");
        
        // Update FilterState: clear second criterion
        filterState.clearSecondCriterion();
        
        // Auto-update display (orchestrates all display updates)
        if (theController != null) {
            theController.autoUpdateDrawSkeleton(filterState);
        }
        
        logger.fine("AGsInfoFrame: Issues filter cleared, returned to ONE_STEP mode");
    }

    /**
     * Handles the showSelectedAGsButton action.
     * Wired to: showSelectedAGsButton (in generalInfoPanel)
     * 
     * Gets selected AGs and invert checkbox state, then enters THREE_STEP mode.
     * If no Issues filter is currently set, applies default (MULTIPLE_AGs).
     * Displays subset3.
     * 
     * TODO: Get selected AGs from fourAGChoiceswChipsPanel
     * TODO: Get invert state from invertFinalResultCheckbox
     */
    private void handleShowSelectedAGsButton() {
        logger.fine("AGsInfoFrame: showSelectedAGsButton clicked");
        
        // Get selected AGs from the radio buttons
        Set<Integer> selectedAGs = getSelectedAGs();
        
        if (selectedAGs.isEmpty()) {
            logger.warning("AGsInfoFrame: No AGs selected for showSelectedAGsButton");
            JOptionPane.showMessageDialog(this, 
                "Please select one or more AGs first", 
                "No AGs Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get invert checkbox state
        boolean invert = invertFinalResultCheckbox.isSelected();
        
        // Update FilterState: enter THREE_STEP mode
        // If no Issues filter is set, default will be applied (MULTIPLE_AGs)
        filterState.setThirdCriterion(selectedAGs, invert);
        
        logger.fine("AGsInfoFrame: Entered THREE_STEP mode with " + selectedAGs.size() + " selected AGs, invert=" + invert);
        
        // Auto-update display (orchestrates all display updates)
        if (theController != null) {
            theController.autoUpdateDrawSkeleton(filterState);
        }
    }

    /**
     * Wires button listeners to theController methods.
     * This connects UI buttons to the theController display methods.
     */
    private void wireButtonListeners() {
        logger.fine("AGsInfoFrame: Wiring button listeners");
        
        // Wire selection listeners for wideSelectionListPanel
        if (wideSelectionListPanel != null) {
            wideSelectionListPanel.addDrawObjSelectionListener(selectedShape -> {
                logger.fine("AGsInfoFrame: Selection changed in wideSelectionListPanel: " + 
                    (selectedShape != null ? selectedShape.toString() : "null"));
                updateUIForSelectedShape(selectedShape);
            });
            logger.fine("AGsInfoFrame: Wired wideSelectionListPanel selection listener");
        }
        
        // Wire selection listeners for narrowSelectionListPanel
        if (narrowSelectionListPanel != null) {
            narrowSelectionListPanel.addDrawObjSelectionListener(selectedShape -> {
                logger.fine("AGsInfoFrame: Selection changed in narrowSelectionListPanel: " + 
                    (selectedShape != null ? selectedShape.toString() : "null"));
                updateUIForSelectedShape(selectedShape);
            });
            logger.fine("AGsInfoFrame: Wired narrowSelectionListPanel selection listener");
        }
        
        // Wire showSelectedAGsButton
        if (showSelectedAGsButton != null) {
            showSelectedAGsButton.addActionListener(e -> handleShowSelectedAGsButton());
            logger.fine("AGsInfoFrame: Wired showSelectedAGsButton");
        }
        
        // Wire showAllShapesButton
        if (showAllShapesButton != null) {
            showAllShapesButton.addActionListener(e -> displayAllDrawObjs());
            logger.fine("AGsInfoFrame: Wired showAllShapesButton");
        }
        
        // Wire filterToggleButton
        if (filterToggleButton != null) {
            filterToggleButton.addActionListener(e -> handleFilterToggle());
            logger.fine("AGsInfoFrame: Wired filterToggleButton");
        }
        
        // Wire showCloseHueButton to open HueRelativesFrame
        if (showCloseHueButton != null) {
            showCloseHueButton.addActionListener(e -> handleShowCloseHueButton());
            logger.fine("AGsInfoFrame: Wired showCloseHueButton");
        }
        
        // Wire doAssignAGButton to handle AG assignment
        if (doAssignAGButton != null) {
            doAssignAGButton.addActionListener(e -> handleDoAssignAGButton());
            logger.fine("AGsInfoFrame: Wired doAssignAGButton");
        }
        
        logger.fine("AGsInfoFrame: Button listener wiring complete");
    }

    /**
     * Handles the filter toggle button action.
     * Placeholder for filter toggle logic.
     */
    private void handleFilterToggle() {
        logger.fine("AGsInfoFrame: Filter toggle button clicked");
        // Placeholder: Will be implemented with filter logic
    }

    /**
     * Handles the showCloseHueButton action.
     * Opens a HueRelativesFrame for the currently selected shape's hue.
     * The frame is linked to TemplateWorkFrame for selection group updates.
     * 
     * Wired to: showCloseHueButton (in narrowSelectionsPanel)
     */
    private void handleShowCloseHueButton() {
        logger.fine("AGsInfoFrame: showCloseHueButton clicked");
        
        // Check if a shape is selected
        if (currSingleSelectShape == null) {
            logger.warning("AGsInfoFrame: No shape selected for showCloseHueButton");
            JOptionPane.showMessageDialog(this, 
                "Please select a shape first", 
                "No Shape Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Get the currently selected shape's hue from AGsInfoFrame's list selection, or use default hue 1
            int userFacingHue = 1;
            if (currSingleSelectShape != null) {
                ColorPtOstDot shapeColor = currSingleSelectShape.getColorPtOstDotFillColor();
                if (shapeColor != null) {
                    userFacingHue = shapeColor.getOstHCS().getStdCodeHueNum() + 1;
                }
            }
            
            // Create HueRelativesViewController with the shape's hue
            HueRelativesViewController hueController = new HueRelativesViewController(userFacingHue, 
                templateWorkFrame.getDrawObjRoster(), templateWorkFrame);
            
            // Create HueRelativesFrame with custom title
            HueRelativesFrame hueFrame = new HueRelativesFrame(hueController);
            hueFrame.setCentralHue(userFacingHue);
            hueFrame.setTitle("AG-selected CloseHues");
            
            // Make the frame visible
            hueFrame.setVisible(true);
            
            logger.fine("AGsInfoFrame: Opened HueRelativesFrame for hue " + userFacingHue);
        } catch (Exception e) {
            logger.warning("AGsInfoFrame: Error opening HueRelativesFrame: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Error opening HueRelativesFrame: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    } // end method handleShowCloseHueButton




    /**
     * Handles the doAssignAGButton action.
     * Assigns the currently selected shape to the AG specified in assignAGSpinner.
     * 
     * Workflow:
     * 1. Validate a shape is selected
     * 2. Get the target AG number from assignAGSpinner
     * 3. Check if exclusiveAssignCheckbox is selected
     * 4. If exclusive: clear all current AG assignments for the shape
     * 5. Add the shape to the target AG in RMatrixMini
     * 6. Update currAGsTextfield to show new assignments
     * 7. Provide user feedback
     * 
     * Wired to: doAssignAGButton (in assignAGForOneShapePanel)
     */
    private void handleDoAssignAGButton() {
        logger.fine("AGsInfoFrame: doAssignAGButton clicked");
        
        // Step 1: Validate a shape is selected
        if (currSingleSelectShape == null) {
            logger.warning("AGsInfoFrame: No shape selected for AG assignment");
            JOptionPane.showMessageDialog(this, 
                "Please select a shape first", 
                "No Shape Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Step 2: Get the target AG number from spinner
            int targetAG = ((Number) assignAGSpinner.getValue()).intValue();
            
            // Validate AG number
            if (targetAG < 1 || targetAG > 4) {
                logger.warning("AGsInfoFrame: Invalid AG number for assignment: " + targetAG);
                JOptionPane.showMessageDialog(this, 
                    "AG number must be between 1 and 4", 
                    "Invalid AG Number", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Step 3: Check if exclusive assign checkbox is selected
            boolean exclusiveAssign = exclusiveAssignCheckbox.isSelected();
            
            // Get the shape's UUID
            UUID shapeUUID = currSingleSelectShape.getUniqueId();
            
            // Step 4: If exclusive, clear all current AG assignments
            if (exclusiveAssign) {
                logger.fine("AGsInfoFrame: Clearing all AG assignments for shape (exclusive mode)");
                Set<Integer> currentAGs = rmatrix.getDrawObjAGs(shapeUUID);
                if (currentAGs != null && !currentAGs.isEmpty()) {
                    for (Integer ag : currentAGs) {
                        rmatrix.removeFromAG(shapeUUID, ag);
                        logger.fine("AGsInfoFrame: Removed shape from AG " + ag);
                    }
                }
            }
            
            // Step 5: Add the shape to the target AG
            rmatrix.addToAG(shapeUUID, targetAG);
            logger.fine("AGsInfoFrame: Added shape to AG " + targetAG);
            
            // Step 6: Update currAGsTextfield to show new assignments
            updateAGAssignmentsForShape(currSingleSelectShape);
            
            // Step 7: Provide user feedback
            String message = exclusiveAssign 
                ? "Shape assigned to AG " + targetAG + " (other assignments cleared)"
                : "Shape added to AG " + targetAG;
            logger.info("AGsInfoFrame: " + message);
            
            // Show brief success message in a tooltip-like way
            JOptionPane.showMessageDialog(this, 
                message, 
                "Assignment Successful", 
                JOptionPane.INFORMATION_MESSAGE);
            
            // Step 8: Auto-update display to show changes immediately
            // This ensures the user sees the updated display after AG assignment
            logger.fine("AGsInfoFrame: Triggering auto-update of display after AG assignment");
            if (theController != null) {
                theController.autoUpdateDrawSkeleton(filterState);
            }
            
        } catch (Exception e) {
            logger.warning("AGsInfoFrame: Error during AG assignment: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Error assigning shape to AG: " + e.getMessage(), 
                "Assignment Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * Sets up the layout to match TemplateWorkFrame structure.
	 * Layout: 
	 * - NORTH: Title/header
	 * - CENTER: Split pane with wideSelectionListPanel and shapeDrawPanel
	 * - EAST: narrowSelectionsPanel with buttons and controls
	 * - SOUTH: generalInfoPanel
	 */
	private void setupLayout() {
		setLayout(new BorderLayout());
		
		// Create a JSplitPane to hold the wide selection list panel and shape draw panel
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
		                                       wideSelectionListPanel, shapeDrawPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(200); // Initial divider position (matching TemplateWorkFrame)
		splitPane.setResizeWeight(0.15); // Give most resize weight to the shapes panel
		
		// Add the split pane to the center
		add(splitPane, BorderLayout.CENTER);
		
		// Add narrow selections panel to the east
		add(narrowSelectionsPanel, BorderLayout.EAST);
		
		// Add general info panel to the south
		add(generalInfoPanel, BorderLayout.SOUTH);
		
		logger.fine("AGsInfoFrame: Layout setup complete");
	}

	// ========== FILTER METHOD DISPLAY METHODS ==========
	
	/**
	 * Updates the filterMethodTextArea with current filter state information.
	 * Assembles strings from individual builder methods to create a complete display.
	 * 
	 * Call this method after any state transition:
	 * - After Show menu selection
	 * - After Issues menu selection
	 * - After showSelectedAGsButton click
	 * - After CLEAR button
	 * - After CLEAR - no ISSUE filter
	 */
	public void updateFilterMethodDescript() {
		String modeStr = buildModeString();
		String firstStr = buildFirstCriterionString();
		String secondStr = buildSecondCriterionString();
		String thirdStr = buildThirdCriterionString();
		
		// Assemble final string, skipping empty lines for inapplicable criteria
		StringBuilder sb = new StringBuilder();
		sb.append(modeStr).append("\n");
		sb.append(firstStr).append("\n");
		if (!secondStr.isEmpty()) {
			sb.append(secondStr).append("\n");
		}
		if (!thirdStr.isEmpty()) {
			sb.append(thirdStr);
		}
		
		filterMethodTextArea.setText(sb.toString());
		logger.fine("AGsInfoFrame: Updated filter method display");
	}
	
	/**
	 * Builds a string representing the current operational mode.
	 * 
	 * @return String like "Mode: ONE_STEP" or "Mode: TWO_STEP" or "Mode: THREE_STEP"
	 */
	private String buildModeString() {
		if (filterState == null) {
			return "Mode: (uninitialized)";
		}
		
		FilterState.FilterMode mode = filterState.getMode();
		switch (mode) {
			case ONE_STEP:
				return "Mode: ONE_STEP (Show menu only)";
			case TWO_STEP:
				return "Mode: TWO_STEP (Show + Issues)";
			case THREE_STEP:
				return "Mode: THREE_STEP (Show + Issues + AGs)";
			default:
				return "Mode: (unknown)";
		}
	}
	
	/**
	 * Builds a string describing the Show menu selection and its configuration.
	 * 
	 * @return String like "Show: AG 1" or "Show: Hue Range (5-10)" or "Show: (none)"
	 */
	private String buildFirstCriterionString() {
		if (filterState == null) {
			return "Show: (uninitialized)";
		}
		
		FilterState.FilterType filterType = filterState.getFilterType();
		if (filterType == null) {
			return "Show: (none selected)";
		}
		
		Object filterValue = filterState.getFilterValue();
		
		switch (filterType) {
			case AG:
				Integer agNum = (Integer) filterValue;
				return String.format("Show: AG %d", agNum);
			
			case HUE_RANGE:
				HueRange range = (HueRange) filterValue;
				return String.format("Show: Hue Range (%d-%d)", range.ccw, range.cw);
			
			case SELECTED_AGs:
				@SuppressWarnings("unchecked")
				Set<Integer> ags = (Set<Integer>) filterValue;
				if (ags.isEmpty()) {
					return "Show: Selected AGs (none)";
				}
				// Sort for consistent display
				java.util.List<Integer> sortedAgs = new java.util.ArrayList<>(ags);
				java.util.Collections.sort(sortedAgs);
				return String.format("Show: Selected AGs (%s)", 
					sortedAgs.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse(""));

            case ALL:
                return "Show: All Shapes";

            case ALLAGS:
                return "Show: All AGs";
			
			default:
				return "Show: (unknown -needs case)";
		}
	}
	
	/**
	 * Builds a string describing the Issues menu strategy and its configuration.
	 * Returns empty string if not applicable (ONE_STEP mode).
	 * 
	 * @return String like "Issues: Check for multiple AGs" or "" if not applicable
	 */
	private String buildSecondCriterionString() {
		if (filterState == null) {
			return "";
		}
		
		// Issues filter only applies in TWO_STEP and THREE_STEP modes
		FilterState.FilterMode mode = filterState.getMode();
		if (mode == FilterState.FilterMode.ONE_STEP) {
			return "";
		}
		
		FilterState.IssuesFilterType issuesFilterType = filterState.getIssuesFilterType();
		if (issuesFilterType == null) {
			return "Issues: (none selected)";
		}
		
		switch (issuesFilterType) {
			case MULTIPLE_AGs:
				return "Issues: Check for multiple AGs";
			case WITHIN_SPREADS:
				return "Issues: Check within Spreads";
			case NO_AG_ASSIGNED:
				return "Issues: Check no AG assigned";
			default:
				return "Issues: (unknown)";
		}
	}
	
	/**
	 * Builds a string describing the AG selection and invert flag.
	 * Returns empty string if not applicable (not in THREE_STEP mode).
	 * 
	 * @return String like "Selected AGs: 1, 3" or "Selected AGs: 1, 3 (inverted)" or ""
	 */
	private String buildThirdCriterionString() {
		if (filterState == null) {
			return "";
		}
		
		// AG selection only applies in THREE_STEP mode
		FilterState.FilterMode mode = filterState.getMode();
		if (mode != FilterState.FilterMode.THREE_STEP) {
			return "";
		}
		
		Set<Integer> selectedAGs = filterState.getSelectedAGs();
		if (selectedAGs.isEmpty()) {
			return "";
		}
		
		// Sort for consistent display
		java.util.List<Integer> sortedAgs = new java.util.ArrayList<>(selectedAGs);
		java.util.Collections.sort(sortedAgs);
		String agList = sortedAgs.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("");
		
		// Check if inverted
		boolean invert = filterState.isInvert();
		if (invert) {
			return String.format("Selected AGs: %s (inverted)", agList);
		} else {
			return String.format("Selected AGs: %s", agList);
		}
	}

} // end class AGsInfoFrame
