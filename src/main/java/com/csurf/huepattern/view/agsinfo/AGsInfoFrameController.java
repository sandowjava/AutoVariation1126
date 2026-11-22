package com.csurf.huepattern.view.agsinfo;

import java.util.logging.Logger;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.Template;
import com.csurf.huepattern.model.RMatrixMiniPrelim;
import com.csurf.huepattern.model.AnchorPatternPrelim;
import com.csurf.huepattern.model.proxy.TemplateProxyData;

/**
 * AGsInfoFrameController handles user interactions and business logic for AGsInfoFrame.
 * 
 * Purpose:
 * - Encapsulates user interaction handling for AGsInfoFrame
 * - Implements business logic for AG assignment operations
 * - Coordinates between AGsInfoFrame GUI and TemplateProxyData model
 * - Follows MVC pattern with AGsInfoFrame as View and TemplateProxyData as Model
 * - Separates GUI logic from business logic
 * 
 * Key Responsibilities:
 * - Handle menu actions from AGsInfoFrame
 * - Manage AG assignment operations (assign, remove, reassign)
 * - Manage proportional location modifications
 * - Manage hue modifications
 * - Execute validation queries
 * - Update display based on user actions
 * - Coordinate with AnchorPatternFrame for spread adjustments
 * 
 * Key Methods:
 * - handleCheckForMultipleAGs(): Display DrawObjs in multiple AGs
 * - handleCheckWithinSpreads(): Display DrawObjs outside spread limits
 * - handleCheckNoAGAssigned(): Display DrawObjs with no AG assignment
 * - handleAssignToAG(UUID, int): Assign DrawObj to AG
 * - handleRemoveFromAG(UUID, int): Remove DrawObj from AG
 * - handleEditProportionalLocation(UUID, double): Modify proportional location
 * - handleEditHue(UUID, int): Modify DrawObj hue
 * - handleValidateSelectedAGs(): Validate selected AGs
 * - handleValidateAllAGs(): Validate all AGs
 * - handleValidateSpreadLocation(): Validate hues within spreads
 * 
 * Data Model:
 * - References TemplateProxyData for all data operations
 * - References RMatrixMini for AG assignments
 * - References AnchorPattern for spread definitions
 * - References original Template for visual comparison
 * 
 * Workflow Integration:
 * - UC-042: Handles AG assignments from TemplateWorkFrame
 * - UC-044: Handles hue changes and proportional location modifications
 * - UC-045: Handles validation operations
 * 
 * Design Pattern:
 * - MVC Controller: Mediates between View (AGsInfoFrame) and Model (TemplateProxyData)
 * - Command Pattern: Each handler method encapsulates a user action
 * - Observer Pattern: Listens for updates from TemplateWorkFrame
 * 
 * Related Classes:
 * - AGsInfoFrame: The View that uses this controller
 * - TemplateProxyData: The Model that stores data
 * - DrawObjsDisplayPanel: The display component managed by this controller
 * - AnchorPatternFrame: Coordinates for spread adjustments
 */
public class AGsInfoFrameController {
	private static final Logger logger = Logger.getLogger(AGsInfoFrameController.class.getName());
	
	private AGsInfoFrame frame;
	
	/**
	 * Creates a new AGsInfoFrameController.
	 * 
	 * @param frame the AGsInfoFrame to control
	 * @throws IllegalArgumentException if frame is null
	 */
	public AGsInfoFrameController(AGsInfoFrame frame) {
		if (frame == null) {
			throw new IllegalArgumentException("AGsInfoFrame cannot be null");
		}
		
		this.frame = frame;
		logger.info("AGsInfoFrameController: Created for AGsInfoFrame");
	}
	
	/**
	 * Validates that each DrawObj has exactly one AG assignment.
	 */
	public void validateSingleAGPerDrawObj() {
		logger.info("AGsInfoFrameController: Validating single AG per DrawObj");
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		RMatrixMiniPrelim rmatrix = proxyData.getRMatrixMini();
		
		boolean isValid = rmatrix.validateSingleAGPerDrawObj();
		
		if (isValid) {
			logger.info("AGsInfoFrameController: All DrawObjs have exactly one AG");
			showMessage("Validation passed: All DrawObjs have exactly one AG assignment");
		} else {
			logger.warning("AGsInfoFrameController: Some DrawObjs have multiple or no AG assignments");
			showMessage("Validation failed: Some DrawObjs have multiple or no AG assignments");
		}
	}
	
	/**
	 * Validates all AGs are properly configured.
	 */
	public void validateAllAGs() {
		logger.info("AGsInfoFrameController: Validating all AGs");
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		AnchorPatternPrelim anchorPatternPrelim = proxyData.getAnchorPattern();
		RMatrixMiniPrelim rmatrix = proxyData.getRMatrixMini();
		
		boolean isValid = anchorPatternPrelim.isValidForRMatrixMini(rmatrix);
		
		if (isValid) {
			logger.info("AGsInfoFrameController: All AGs are properly configured");
			showMessage("Validation passed: All AGs are properly configured");
		} else {
			logger.warning("AGsInfoFrameController: Some AGs are not properly configured");
			showMessage("Validation failed: Some AGs are not properly configured");
		}
	}
	
	/**
	 * Handles assigning a DrawObj to an AG.
	 * 
	 * @param drawObjUUID the UUID of the DrawObj
	 * @param agNum the AG number (1-4)
	 */
	public void handleAssignToAG(UUID drawObjUUID, int agNum) {
		logger.info("AGsInfoFrameController: Assigning DrawObj " + drawObjUUID + " to AG " + agNum);
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		proxyData.updateAGAssignment(drawObjUUID, agNum);
		
		logger.fine("AGsInfoFrameController: Assignment complete");
	}
	
	/**
	 * Handles removing a DrawObj from an AG.
	 * 
	 * @param drawObjUUID the UUID of the DrawObj
	 * @param agNum the AG number (1-4)
	 */
	public void handleRemoveFromAG(UUID drawObjUUID, int agNum) {
		logger.info("AGsInfoFrameController: Removing DrawObj " + drawObjUUID + " from AG " + agNum);
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		RMatrixMiniPrelim rmatrix = proxyData.getRMatrixMini();
		
		rmatrix.removeFromAG(drawObjUUID, agNum);
		
		logger.fine("AGsInfoFrameController: Removal complete");
	}
	
	/**
	 * Handles modifying proportional location for a DrawObj.
	 * 
	 * @param drawObjUUID the UUID of the DrawObj
	 * @param proportionalLocation the new proportional location (0.0-1.0)
	 */
	public void handleEditProportionalLocation(UUID drawObjUUID, double proportionalLocation) {
		logger.info("AGsInfoFrameController: Editing proportional location for DrawObj " + drawObjUUID);
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		proxyData.updateProportionalLocation(drawObjUUID, proportionalLocation);
		
		logger.fine("AGsInfoFrameController: Proportional location update complete");
	}
	
	/**
	 * Handles modifying hue for a DrawObj.
	 * 
	 * @param drawObjUUID the UUID of the DrawObj
	 * @param newHue the new hue value (0-359)
	 */
	public void handleEditHue(UUID drawObjUUID, int newHue) {
		logger.info("AGsInfoFrameController: Editing hue for DrawObj " + drawObjUUID);
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		proxyData.updateHue(drawObjUUID, newHue);
		
		logger.fine("AGsInfoFrameController: Hue update complete");
	}
	
	// ============================================================================
	// DISPLAY METHODS (Layer 2: Controller - Transformation & Orchestration)
	// ============================================================================

	/**
	 * Displays all DrawObjs in the template.
	 * 
	 * This is a display method that orchestrates the transformation of model data
	 * into view-ready format and updates the display.
	 * 
	 * Typical flow:
	 * 1. Query model for all DrawObjs
	 * 2. Transform UUIDs to DrawObj array
	 * 3. Update view with array
	 */
	public void displayAllDrawObjs() {
		logger.fine("AGsInfoFrameController: Displaying all DrawObjs");
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		Template template = proxyData.getOriginalTemplate();
		
		// Get all DrawObjs from template
		List<DrawObj> allDrawObjs = template.getAllDrawObjs();
		DrawObj[] drawObjArray = allDrawObjs != null ? allDrawObjs.toArray(new DrawObj[0]) : new DrawObj[0];
		
		// Update view
		frame.updateShapeDisplay(drawObjArray);
		logger.fine("AGsInfoFrameController: Displayed " + drawObjArray.length + " DrawObjs");
	}

	/**
	 * Displays DrawObjs in the specified hue range.
	 * 
	 * This is a display method that orchestrates:
	 * 1. Query model for hue range
	 * 2. Transform UUIDs to DrawObj array
	 * 3. Update view
	 * 
	 * @param ccwLimit Counter-clockwise hue limit (1-36)
	 * @param cwLimit Clockwise hue limit (1-36)
	 */
	public void displayDrawObjsForHueRange(int ccwLimit, int cwLimit) {
		logger.fine("AGsInfoFrameController: Displaying DrawObjs for hue range " + ccwLimit + "-" + cwLimit);
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		
		// Query model: Get UUIDs in hue range
		Set<UUID> uuids = proxyData.getDrawObjsForHueRange(ccwLimit, cwLimit);
		
		// Transform: Convert UUIDs to DrawObj array
		DrawObj[] drawObjs = convertUUIDsToDrawObjs(proxyData, uuids);
		
		// Update view
		frame.updateShapeDisplay(drawObjs);
		logger.fine("AGsInfoFrameController: Displayed " + drawObjs.length + " DrawObjs in hue range");
	}

	/**
	 * Displays DrawObjs assigned to a specific AG.
	 * 
	 * This is a display method that orchestrates:
	 * 1. Query model for AG members
	 * 2. Transform UUIDs to DrawObj array
	 * 3. Update view
	 * 
	 * @param agNum AG number (1-4)
	 */
	public void displayDrawObjsForAG(int agNum) {
		logger.fine("AGsInfoFrameController: Displaying DrawObjs for AG " + agNum);
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		
		// Query model: Get UUIDs for AG
		Set<UUID> uuids = proxyData.getDrawObjsForAG(agNum);
		
		// Transform: Convert UUIDs to DrawObj array
		DrawObj[] drawObjs = convertUUIDsToDrawObjs(proxyData, uuids);
		
		// Update view
		frame.updateShapeDisplay(drawObjs);
		logger.fine("AGsInfoFrameController: Displayed " + drawObjs.length + " DrawObjs for AG " + agNum);
	}

	/**
	 * Displays DrawObjs assigned to any of the selected AGs.
	 * 
	 * This is a display method that orchestrates:
	 * 1. Query model for selected AGs
	 * 2. Transform UUIDs to DrawObj array
	 * 3. Update view
	 * 
	 * @param agNums Set of AG numbers (1-4)
	 */
	public void displayDrawObjsForSelectedAGs(Set<Integer> agNums) {
		logger.fine("AGsInfoFrameController: Displaying DrawObjs for selected AGs");
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		
		// Query model: Get UUIDs for selected AGs
		Set<UUID> uuids = proxyData.getDrawObjsForSelectedAGs(agNums);
		
		// Transform: Convert UUIDs to DrawObj array
		DrawObj[] drawObjs = convertUUIDsToDrawObjs(proxyData, uuids);
		
		// Update view
		frame.updateShapeDisplay(drawObjs);
		logger.fine("AGsInfoFrameController: Displayed " + drawObjs.length + " DrawObjs for selected AGs");
	} // end method displayDrawObjsForSelectedAGs




	/**
	 * Displays DrawObjs assigned to multiple AGs (validation display).
	 * 
	 * This is a display method that orchestrates:
	 * 1. Query model for DrawObjs in multiple AGs
	 * 2. Transform DrawObj set to array
	 * 3. Update view
	 */
	public void displayDrawObjsInMultipleAGs() {
		logger.fine("AGsInfoFrameController: Displaying DrawObjs in multiple AGs");
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		RMatrixMiniPrelim rmatrix = proxyData.getRMatrixMini();
		
		// Query model: Get DrawObjs in multiple AGs
		Set<DrawObj> drawObjs = rmatrix.getDrawObjsInMultipleAGs();
		
		// Transform: Convert set to array
		DrawObj[] drawObjArray = convertDrawObjsToArray(drawObjs);
		
		// Update view
		frame.updateShapeDisplay(drawObjArray);
		logger.fine("AGsInfoFrameController: Displayed " + drawObjArray.length + " DrawObjs in multiple AGs");
	}

	/**
	 * Displays DrawObjs with no AG assignment (validation display).
	 * 
	 * This is a display method that orchestrates:
	 * 1. Query model for DrawObjs with no AG
	 * 2. Transform DrawObj set to array
	 * 3. Update view
	 */
	public void displayDrawObjsWithNoAG() {
		logger.fine("AGsInfoFrameController: Displaying DrawObjs with no AG assignment");
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		RMatrixMiniPrelim rmatrix = proxyData.getRMatrixMini();
		
		// Query model: Get DrawObjs with no AG
		Set<DrawObj> drawObjs = rmatrix.getDrawObjsWithNoAG();
		
		// Transform: Convert set to array
		DrawObj[] drawObjArray = convertDrawObjsToArray(drawObjs);
		
		// Update view
		frame.updateShapeDisplay(drawObjArray);
		logger.fine("AGsInfoFrameController: Displayed " + drawObjArray.length + " DrawObjs with no AG");
	}

	// ============================================================================
	// FILTERSTATE SUBSET DISPLAY METHODS (Phase 1: Display Update Logic)
	// ============================================================================

	/**
	 * Displays subset1 from FilterState (first criterion only).
	 * 
	 * This method orchestrates:
	 * 1. Query FilterState for subset1
	 * 2. Convert UUIDs to DrawObj array
	 * 3. Update view with subset1
	 * 
	 * Used by:
	 * - Show menu items (all 5 items)
	 * - CLEAR button
	 * - CLEAR - no ISSUE filter menu item
	 * 
	 * @param filterState the FilterState managing filtering criteria
	 */
	public void displaySubset1(FilterState filterState) {
		logger.fine("AGsInfoFrameController: Displaying subset1 (first criterion only)");
		
		if (filterState == null) {
			logger.warning("AGsInfoFrameController: FilterState is null");
			return;
		}
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		
		// Query FilterState: Get subset1
		Set<UUID> subset1 = filterState.getSubset1(proxyData);
		
		// Transform: Convert UUIDs to DrawObj array
		DrawObj[] drawObjs = convertUUIDsToDrawObjs(proxyData, subset1);
		
		// Update view
		frame.updateShapeDisplay(drawObjs);
		logger.fine("AGsInfoFrameController: Displayed " + drawObjs.length + " DrawObjs in subset1");
	}

	/**
	 * Displays subset2 from FilterState (first + second criteria).
	 * 
	 * This method orchestrates:
	 * 1. Query FilterState for subset2
	 * 2. Convert UUIDs to DrawObj array
	 * 3. Update view with subset2
	 * 
	 * Used by:
	 * - Issues menu items (all 3 items)
	 * 
	 * @param filterState the FilterState managing filtering criteria
	 */
	public void displaySubset2(FilterState filterState) {
		logger.fine("AGsInfoFrameController: Displaying subset2 (first + second criteria)");
		
		if (filterState == null) {
			logger.warning("AGsInfoFrameController: FilterState is null");
			return;
		}
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		RMatrixMiniPrelim rmatrix = frame.getRMatrix();
		
		// Query FilterState: Get subset2
		Set<UUID> subset2 = filterState.getSubset2(proxyData, rmatrix);
		
		// Transform: Convert UUIDs to DrawObj array
		DrawObj[] drawObjs = convertUUIDsToDrawObjs(proxyData, subset2);
		
		// Update view
		frame.updateShapeDisplay(drawObjs);
		logger.fine("AGsInfoFrameController: Displayed " + drawObjs.length + " DrawObjs in subset2");
	}

	/**
	 * Displays subset3 from FilterState (all three criteria).
	 * 
	 * This method orchestrates:
	 * 1. Query FilterState for subset3
	 * 2. Convert UUIDs to DrawObj array
	 * 3. Update view with subset3
	 * 
	 * Used by:
	 * - showSelectedAGsButton (enters THREE_STEP mode)
	 * 
	 * @param filterState the FilterState managing filtering criteria
	 */
	public void displaySubset3(FilterState filterState) {
		logger.fine("AGsInfoFrameController: Displaying subset3 (all three criteria)");
		
		if (filterState == null) {
			logger.warning("AGsInfoFrameController: FilterState is null");
			return;
		}
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		RMatrixMiniPrelim rmatrix = frame.getRMatrix();
		
		// Query FilterState: Get subset3
		Set<UUID> subset3 = filterState.getSubset3(proxyData, rmatrix);
		
		// Transform: Convert UUIDs to DrawObj array
		DrawObj[] drawObjs = convertUUIDsToDrawObjs(proxyData, subset3);
		
		// Update view
		frame.updateShapeDisplay(drawObjs);
		logger.fine("AGsInfoFrameController: Displayed " + drawObjs.length + " DrawObjs in subset3");
	}

	/**
	 * Orchestrates display of filtered subsets in both list panels based on FilterState mode.
	 * 
	 * This is a polymorphic display orchestration method that:
	 * 1. Checks the current FilterState mode
	 * 2. Retrieves the appropriate subset(s) from FilterState
	 * 3. Converts UUIDs to DrawObj arrays
	 * 4. Updates the appropriate list panels
	 * 
	 * Behavior is polymorphic based on FilterState.getMode():
	 * - ONE_STEP: Display subset1 in wideSelectionListPanel only
	 * - TWO_STEP: Display subset1 in wideSelectionListPanel, subset2 in narrowSelectionListPanel
	 * - THREE_STEP: PLACEHOLDER - uses TWO_STEP logic until subset3 generation is debugged
	 *   (Design intent: Display subset2 in wideSelectionListPanel, subset3 in narrowSelectionListPanel)
	 * 
	 * Called by:
	 * - Show menu item handlers (after setting first criterion)
	 * - Issues menu item handlers (after setting second criterion)
	 * - CLEAR button (after clearing all filters)
	 * - CLEAR - no ISSUE filter menu item (after clearing second criterion)
	 * - showSelectedAGsButton (after setting third criterion)
	 * 
	 * @param filterState the FilterState managing filtering criteria
	 */
	public void listFilterSubsets(FilterState filterState) {
		logger.fine("AGsInfoFrameController: listFilterSubsets called for mode: " + 
			(filterState != null ? filterState.getMode() : "null"));
		
		if (filterState == null) {
			logger.warning("AGsInfoFrameController: FilterState is null in listFilterSubsets");
			return;
		}
		
		TemplateProxyData proxyData = frame.getTemplateProxyData();
		RMatrixMiniPrelim rmatrix = frame.getRMatrix();
		
		switch (filterState.getMode()) {
			case ONE_STEP:
				// Display subset1 in wideSelectionListPanel only
				logger.fine("AGsInfoFrameController: listFilterSubsets - ONE_STEP mode");
				
				Set<UUID> subset1 = filterState.getSubset1(proxyData);
				DrawObj[] subset1Objs = convertUUIDsToDrawObjs(proxyData, subset1);
				
				frame.updateWideSelectionList(subset1Objs);
				frame.updateNarrowSelectionList(new DrawObj[0]); // Clear narrow list
				
				logger.fine("AGsInfoFrameController: listFilterSubsets - Displayed " + subset1Objs.length + 
					" DrawObjs in wideSelectionListPanel");
				break;
				
			case TWO_STEP:
				// Display subset1 in wideSelectionListPanel
				// Display subset2 in narrowSelectionListPanel
				logger.fine("AGsInfoFrameController: listFilterSubsets - TWO_STEP mode");
				
				Set<UUID> subset1_two = filterState.getSubset1(proxyData);
				DrawObj[] subset1Objs_two = convertUUIDsToDrawObjs(proxyData, subset1_two);
				
				Set<UUID> subset2 = filterState.getSubset2(proxyData, rmatrix);
				DrawObj[] subset2Objs = convertUUIDsToDrawObjs(proxyData, subset2);
				
				frame.updateWideSelectionList(subset1Objs_two);
				frame.updateNarrowSelectionList(subset2Objs);
				
				logger.fine("AGsInfoFrameController: listFilterSubsets - Displayed " + subset1Objs_two.length + 
					" DrawObjs in wideSelectionListPanel and " + subset2Objs.length + 
					" DrawObjs in narrowSelectionListPanel");
				break;
				
			case THREE_STEP:
				// PLACEHOLDER: Temporarily use TWO_STEP logic
				// This does NOT match the design intent
				// Design intent: Display subset2 in wideSelectionListPanel
				//                Display subset3 in narrowSelectionListPanel
				// TODO: Implement proper THREE_STEP logic after subset3 generation is debugged
				// For now, show subset1 and subset2 to provide relevant user feedback
				logger.fine("AGsInfoFrameController: listFilterSubsets - THREE_STEP mode (PLACEHOLDER)");
				logger.warning("AGsInfoFrameController: THREE_STEP mode using TWO_STEP logic - this is a placeholder");
				
				Set<UUID> subset1_three = filterState.getSubset1(proxyData);
				DrawObj[] subset1Objs_three = convertUUIDsToDrawObjs(proxyData, subset1_three);
				
				Set<UUID> subset2_three = filterState.getSubset2(proxyData, rmatrix);
				DrawObj[] subset2Objs_three = convertUUIDsToDrawObjs(proxyData, subset2_three);
				
				frame.updateWideSelectionList(subset1Objs_three);
				frame.updateNarrowSelectionList(subset2Objs_three);
				
				logger.fine("AGsInfoFrameController: listFilterSubsets - Displayed " + subset1Objs_three.length + 
					" DrawObjs in wideSelectionListPanel and " + subset2Objs_three.length + 
					" DrawObjs in narrowSelectionListPanel (PLACEHOLDER)");
				break;
				
			default:
				logger.warning("AGsInfoFrameController: Unknown FilterMode in listFilterSubsets");
				break;
		}
	}

	// ============================================================================
	// AUTO-UPDATE DISPLAY SKELETON (High-Level Orchestration)
	// ============================================================================

	/**
	 * High-level orchestration method for automatic display updates after state changes.
	 * 
	 * This method encapsulates the complete display update sequence that occurs when:
	 * 1. User changes filtering criteria (Show menu, Issues menu, AG selection)
	 * 2. AG state changes (assignment, removal, bulk operations)
	 * 3. Any other state change that affects what should be displayed
	 * 
	 * The method orchestrates three coordinated updates:
	 * 1. Update filter description text (filterMethodTextArea)
	 * 2. Update list panels (wideSelectionListPanel, narrowSelectionListPanel)
	 * 3. Update main drawing panel (shapeDrawPanel) based on current FilterMode
	 * 
	 * Behavior is polymorphic based on FilterState.getMode():
	 * - ONE_STEP: Display subset1 (first criterion only)
	 * - TWO_STEP: Display subset2 (first + second criteria)
	 * - THREE_STEP: Display subset3 (all three criteria) - PLACEHOLDER uses TWO_STEP logic
	 * 
	 * Called from:
	 * - All Show menu item handlers (after setting first criterion)
	 * - All Issues menu item handlers (after setting second criterion)
	 * - CLEAR button (after clearing all filters)
	 * - CLEAR - no ISSUE filter menu item (after clearing second criterion)
	 * - showSelectedAGsButton (after setting third criterion)
	 * - handleDoAssignAGButton (after AG assignment)
	 * - handleAddToAGButton / handleRemoveFromAGButton (after bulk AG operations)
	 * 
	 * Design Benefits:
	 * - Single point of control for all display updates
	 * - Ensures consistency across all update paths
	 * - Preserves user's filtering decisions via FilterState
	 * - Provides immediate visual feedback for all state changes
	 * - Simplifies menu handlers and AG operation handlers
	 * 
	 * @param filterState the FilterState managing filtering criteria and mode
	 */
	public void autoUpdateDrawSkeleton(FilterState filterState) {
		logger.fine("AGsInfoFrameController: autoUpdateDrawSkeleton called");
		
		// Validate input
		if (filterState == null) {
			logger.warning("AGsInfoFrameController: autoUpdateDrawSkeleton - FilterState is null");
			return;
		}
		
		if (frame == null) {
			logger.warning("AGsInfoFrameController: autoUpdateDrawSkeleton - AGsInfoFrame is null");
			return;
		}
		
		try {
			// Step 1: Update filter description text
			logger.fine("AGsInfoFrameController: autoUpdateDrawSkeleton - Step 1: Updating filter description");
			frame.updateFilterMethodDescript();
			
			// Step 2: Update list panels (subset distribution)
			logger.fine("AGsInfoFrameController: autoUpdateDrawSkeleton - Step 2: Updating list panels");
			listFilterSubsets(filterState);
			
			// Step 3: Update main drawing panel based on current mode
			logger.fine("AGsInfoFrameController: autoUpdateDrawSkeleton - Step 3: Updating drawing panel");
			FilterState.FilterMode mode = filterState.getMode();
			
			switch (mode) {
				case ONE_STEP:
					logger.fine("AGsInfoFrameController: autoUpdateDrawSkeleton - Displaying subset1 (ONE_STEP)");
					displaySubset1(filterState);
					break;
					
				case TWO_STEP:
					logger.fine("AGsInfoFrameController: autoUpdateDrawSkeleton - Displaying subset2 (TWO_STEP)");
					displaySubset2(filterState);
					break;
					
				case THREE_STEP:
					// PLACEHOLDER: Use TWO_STEP logic until subset3 generation is debugged
					logger.fine("AGsInfoFrameController: autoUpdateDrawSkeleton - Displaying subset2 (THREE_STEP placeholder)");
					logger.warning("AGsInfoFrameController: autoUpdateDrawSkeleton - THREE_STEP using placeholder logic");
					displaySubset2(filterState);
					// TODO: Change to displaySubset3(filterState) after subset3 generation is debugged
					break;
					
				default:
					logger.warning("AGsInfoFrameController: autoUpdateDrawSkeleton - Unknown FilterMode: " + mode);
					break;
			}
			
			logger.fine("AGsInfoFrameController: autoUpdateDrawSkeleton completed successfully");
			
		} catch (Exception e) {
			logger.warning("AGsInfoFrameController: autoUpdateDrawSkeleton - Error during update: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// ============================================================================
	// PRIVATE HELPER METHODS (Data Transformation)
	// ============================================================================

	/**
	 * Converts a set of UUIDs to a DrawObj array.
	 * 
	 * This helper method transforms model data (UUIDs) into view-ready format (DrawObj array).
	 * Made public for use by menu handlers and display methods.
	 * 
	 * @param proxyData The TemplateProxyData containing the template
	 * @param uuids Set of DrawObj UUIDs
	 * @return DrawObj[] array
	 */
	public DrawObj[] convertUUIDsToDrawObjs(TemplateProxyData proxyData, Set<UUID> uuids) {
		if (uuids == null || uuids.isEmpty()) {
			return new DrawObj[0];
		}
		
		Template template = proxyData.getOriginalTemplate();
		List<DrawObj> allDrawObjs = template.getAllDrawObjs();
		
		if (allDrawObjs == null || allDrawObjs.isEmpty()) {
			return new DrawObj[0];
		}
		
		List<DrawObj> result = new ArrayList<>();
		
		// For each DrawObj in template, check if its UUID is in the set
		for (DrawObj drawObj : allDrawObjs) {
			if (drawObj != null && uuids.contains(drawObj.getUniqueId())) {
				result.add(drawObj);
			}
		}
		
		return result.toArray(new DrawObj[0]);
	}

	/**
	 * Converts a set of DrawObjs to a DrawObj array.
	 * 
	 * This helper method transforms model data (DrawObj set) into view-ready format (DrawObj array).
	 * 
	 * @param drawObjs Set of DrawObj instances
	 * @return DrawObj[] array
	 */
	private DrawObj[] convertDrawObjsToArray(Set<DrawObj> drawObjs) {
		if (drawObjs == null || drawObjs.isEmpty()) {
			return new DrawObj[0];
		}
		
		return drawObjs.toArray(new DrawObj[0]);
	}

	/**
	 * Shows a message to the user.
	 * 
	 * @param message the message to show
	 */
	private void showMessage(String message) {
		logger.info("AGsInfoFrameController: Showing message: " + message);
		// Placeholder: Will be implemented with proper message dialog
	}
}
