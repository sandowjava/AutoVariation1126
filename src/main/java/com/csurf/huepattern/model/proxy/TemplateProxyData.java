package com.csurf.huepattern.model.proxy;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.huepattern.model.RMatrixMini;
import com.csurf.huepattern.model.AnchorPattern;
import com.csurf.templatecore.model.PaletteWName;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;

/**
 * TemplateProxyData represents a working copy of Template data for use within AGsInfoFrame.
 * 
 * Purpose:
 * - Holds a mutable copy of Template data (shapes, palette, RMatrixMini, AnchorPattern)
 * - Serves as the Model in MVC architecture for AGsInfoFrame
 * - Enables independent modification without affecting original Template
 * - Supports one-way data flow from TemplateWorkFrame to TemplateProxyData
 * - Allows TemplateProxyData to diverge from TemplateWorkFrame during UC-044 (fine-tuning phase)
 * 
 * Data Structure:
 * - shapeObjRoster: Collection of simplified ShapeObj instances (no color data)
 * - palette: PaletteWName maintained separately (updated dynamically during UC-044)
 * - rmatrix: NEW RMatrixMini instance (not a reference) for AG assignments and proportional locations
 * - anchorPattern: NEW AnchorPattern instance (not a reference) for spread definitions
 * - validatedAllAGs: Flag indicating whether all AGs have been validated
 * 
 * Key Design Decisions:
 * - NEW instances of RMatrixMini and AnchorPattern (not references to existing instances)
 * - Template in TemplateWorkFrame has NO RMatrixMini or AnchorPattern initially
 * - TemplateProxyData creates new instances during initialization
 * - One-way data flow: TemplateWorkFrame → TemplateProxyData (never back)
 * - Palette is separate entity, updated dynamically during UC-044
 * 
 * Lifecycle (Three Phases):
 * - Phase 1 (UC-041): Created as copy of TemplateWorkFrame, hue pattern identical
 * - Phase 2 (UC-042): Receives AG assignments from TemplateWorkFrame, hue pattern still identical
 * - Phase 3 (UC-044): Diverges from TemplateWorkFrame, hue pattern changes, palette updated dynamically
 * 
 * Conversion:
 * - Can be converted back to Template via TemplateProxyDataService.convertToTemplate()
 * - Includes RMatrixMini and AnchorPattern in converted Template
 * - Enables saving of new Template with artistic intelligence
 */
public class TemplateProxyData {
	
	// ============================================================================
	// FIELDS
	// ============================================================================
	
	private final Template originalTemplate;
	private ShapeObjRoster shapeObjRoster;
	private PaletteWName palette;
	private RMatrixMini rmatrix;
	private AnchorPattern anchorPattern;
	private boolean validatedAllAGs;
	
	// ============================================================================
	// CONSTRUCTOR
	// ============================================================================
	
	/**
	 * Creates a new TemplateProxyData as a working copy of the given Template.
	 * 
	 * Initializes:
	 * - NEW ShapeObjRoster (copy of DrawObjRoster, without colors)
	 * - NEW PaletteWName (copy of original palette)
	 * - NEW RMatrixMini instance (not a reference)
	 * - NEW AnchorPattern instance (not a reference)
	 * - validatedAllAGs flag (initially false)
	 * 
	 * @param template The original Template to create a working copy from
	 * @throws IllegalArgumentException if template is null
	 */
	public TemplateProxyData(Template template) {
		if (template == null) {
			throw new IllegalArgumentException("Template cannot be null");
		}
		
		this.originalTemplate = template;
		this.shapeObjRoster = new ShapeObjRoster(template.getDrawObjRoster());
		this.palette = template.getDrawObjRoster().getCurrPalette();
		this.rmatrix = new RMatrixMini(template);
		this.anchorPattern = new AnchorPattern();
		this.validatedAllAGs = false;
	}
	
	// ============================================================================
	// GETTERS
	// ============================================================================
	
	/**
	 * Returns the original Template that this proxy was created from.
	 * 
	 * @return the original Template (immutable reference)
	 */
	public Template getOriginalTemplate() {
		return originalTemplate;
	}
	
	/**
	 * Returns the ShapeObjRoster containing simplified shape objects.
	 * 
	 * @return the ShapeObjRoster
	 */
	public ShapeObjRoster getShapeObjRoster() {
		return shapeObjRoster;
	}
	
	/**
	 * Returns the current palette.
	 * 
	 * @return the PaletteWName
	 */
	public PaletteWName getPalette() {
		return palette;
	}
	
	/**
	 * Returns the RMatrixMini instance holding AG assignments and proportional locations.
	 * 
	 * @return the RMatrixMini
	 */
	public RMatrixMini getRMatrixMini() {
		return rmatrix;
	}
	
	/**
	 * Returns the AnchorPattern instance holding spread definitions.
	 * 
	 * @return the AnchorPattern
	 */
	public AnchorPattern getAnchorPattern() {
		return anchorPattern;
	}
	
	/**
	 * Returns whether all AGs have been validated.
	 * 
	 * @return true if all AGs have been validated, false otherwise
	 */
	public boolean isValidatedAllAGs() {
		return validatedAllAGs;
	}
	
	// ============================================================================
	// SETTERS
	// ============================================================================
	
	/**
	 * Sets the RMatrixMini instance.
	 * 
	 * @param rmatrix the new RMatrixMini
	 * @throws IllegalArgumentException if rmatrix is null
	 */
	public void setRMatrixMini(RMatrixMini rmatrix) {
		if (rmatrix == null) {
			throw new IllegalArgumentException("RMatrixMini cannot be null");
		}
		this.rmatrix = rmatrix;
	}
	
	/**
	 * Sets the AnchorPattern instance.
	 * 
	 * @param anchorPattern the new AnchorPattern
	 * @throws IllegalArgumentException if anchorPattern is null
	 */
	public void setAnchorPattern(AnchorPattern anchorPattern) {
		if (anchorPattern == null) {
			throw new IllegalArgumentException("AnchorPattern cannot be null");
		}
		this.anchorPattern = anchorPattern;
	}
	
	/**
	 * Sets the validatedAllAGs flag.
	 * 
	 * @param validated true if all AGs have been validated, false otherwise
	 */
	public void setValidatedAllAGs(boolean validated) {
		this.validatedAllAGs = validated;
	}
	
	// ============================================================================
	// UPDATE METHODS
	// ============================================================================
	
	/**
	 * Updates the AG assignment for a DrawObj.
	 * 
	 * This method updates the RMatrixMini with a new AG assignment for the specified DrawObj.
	 * During initial organization phase (UC-042), multiple AG assignments are allowed.
	 * During validation phase (UC-045), single AG per DrawObj is enforced.
	 * 
	 * @param drawObjUUID the UUID of the DrawObj to update
	 * @param agNum the AG number to assign (1-4)
	 * @throws IllegalArgumentException if drawObjUUID is null or agNum is invalid
	 */
	public void updateAGAssignment(UUID drawObjUUID, int agNum) {
		if (drawObjUUID == null) {
			throw new IllegalArgumentException("DrawObj UUID cannot be null");
		}
		if (agNum < 1 || agNum > 4) {
			throw new IllegalArgumentException("AG number must be between 1 and 4");
		}
		
		// Delegate to RMatrixMini to update AG assignment
		rmatrix.setCurrAGAssignment(drawObjUUID, agNum);
	}
	
	/**
	 * Updates the proportional location for a DrawObj.
	 * 
	 * The proportional location determines where within the AG's spread the hue is calculated.
	 * Valid range is 0.0 to 1.0, where 0.0 is CCW limit and 1.0 is CW limit.
	 * 
	 * @param drawObjUUID the UUID of the DrawObj to update
	 * @param proportionalLocation the proportional location (0.0 to 1.0)
	 * @throws IllegalArgumentException if drawObjUUID is null or proportionalLocation is invalid
	 */
	public void updateProportionalLocation(UUID drawObjUUID, double proportionalLocation) {
		if (drawObjUUID == null) {
			throw new IllegalArgumentException("DrawObj UUID cannot be null");
		}
		if (proportionalLocation < 0.0 || proportionalLocation > 1.0) {
			throw new IllegalArgumentException("Proportional location must be between 0.0 and 1.0");
		}
		
		// Delegate to RMatrixMini to update proportional location
		rmatrix.setProportionLocation(drawObjUUID, proportionalLocation);
	}
	
	/**
	 * Updates the hue for a DrawObj in the palette.
	 * 
	 * This method updates the palette with a new hue for the specified DrawObj.
	 * Used during UC-044 (fine-tuning phase) when user manually adjusts hues.
	 * 
	 * @param drawObjUUID the UUID of the DrawObj to update
	 * @param newHue the new hue value (0-359)
	 * @throws IllegalArgumentException if drawObjUUID is null or hue is invalid
	 */
	public void updateHue(UUID drawObjUUID, int newHue) {
		if (drawObjUUID == null) {
			throw new IllegalArgumentException("DrawObj UUID cannot be null");
		}
		if (newHue < 0 || newHue > 359) {
			throw new IllegalArgumentException("Hue must be between 0 and 359");
		}
		
		// Update the palette with the new hue for this DrawObj
		// Note: PaletteWName must have updateHueForDrawObj(UUID, int) method
		// palette.updateHueForDrawObj(drawObjUUID, newHue);
		// TODO: Implement when PaletteWName.updateHueForDrawObj() is available
	}
	
	// ============================================================================
	// QUERY METHODS (Layer 1: Model - Pure Data Queries)
	// ============================================================================

	/**
	 * Queries all DrawObjs whose hues fall within the specified hue range.
	 * 
	 * This is a pure query method that returns raw data (UUIDs) without transformation.
	 * Used by AGsInfoFrameController to get DrawObjs in a specific hue range.
	 * 
	 * @param ccwLimit Counter-clockwise limit (1-36)
	 * @param cwLimit Clockwise limit (1-36)
	 * @return Set<UUID> of DrawObjs in the hue range
	 * @throws IllegalArgumentException if hue limits are invalid
	 */
	public Set<UUID> getDrawObjsForHueRange(int ccwLimit, int cwLimit) {
		if (ccwLimit < 1 || ccwLimit > 36 || cwLimit < 1 || cwLimit > 36) {
			throw new IllegalArgumentException("Hue limits must be between 1 and 36");
		}
		
		Set<UUID> result = new HashSet<>();

        // TODO 111825 INVESTIGATE: decide cut-off in process: when to stop any reference to original template vs "new" template
		// Get the palette from the original template
		PaletteWName templatePalette = originalTemplate.getDrawObjRoster().getCurrPalette();
		if (templatePalette == null) {
			return result; // Return empty set if no palette
		}
		
		// Get all colors from the palette
		ColorPtOstDot[] colors = templatePalette.getColorPtOstdotArray();
		if (colors == null || colors.length == 0) {
			return result; // Return empty set if no colors
		}
		
		// Get all DrawObjs from the template
		List<DrawObj> allDrawObjs = originalTemplate.getAllDrawObjs();
		if (allDrawObjs == null || allDrawObjs.isEmpty()) {
			return result; // Return empty set if no DrawObjs
		}
		
		// For each DrawObj, check if its hue is in the specified range
		for (int i = 0; i < allDrawObjs.size() && i < colors.length; i++) {
			DrawObj drawObj = allDrawObjs.get(i);
			ColorPtOstDot color = colors[i];
			
			if (drawObj != null && color != null) {
				// Get the hue from the color (0-35, code-facing)
				int codeHueNum = color.getOstHCS().getStdCodeHueNum();
				
				// Convert to user-facing hue number (1-36)
				HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(codeHueNum + 1);
				int userFacingHueNum = hueNumGUI.getUserFacingHueNum();
				
				// Check if hue is in range (handle wrap-around at 36→1)
				boolean inRange = isHueInRange(userFacingHueNum, ccwLimit, cwLimit); // 111825 method seems OK for correct logic
				
				if (inRange) {
					result.add(drawObj.getUniqueId());
				}
			}
		}
		
		return result;
	}

	/**
	 * Queries all DrawObjs assigned to a specific AG.
	 * 
	 * This is a pure query method that returns raw data (UUIDs) without transformation.
	 * Delegates to RMatrixMini which maintains AG membership information.
	 * 
	 * @param agNum AG number (1-4)
	 * @return Set<UUID> of DrawObjs in the AG
	 * @throws IllegalArgumentException if agNum is invalid
	 */
	public Set<UUID> getDrawObjsForAG(int agNum) {
		if (agNum < 1 || agNum > 4) {
			throw new IllegalArgumentException("AG number must be between 1 and 4");
		}
		
		// Delegate to RMatrixMini which has UUID-based query methods
		return rmatrix.getAGMembersUUIDs(agNum);
	} // end method getDrawObjsForAG




	/**
	 * Queries all DrawObjs assigned to any of the selected AGs.
	 * 
	 * This is a pure query method that returns raw data (UUIDs) without transformation.
	 * Returns the union of all DrawObjs in the selected AGs.
	 * 
	 * @param agNums Set of AG numbers (1-4)
	 * @return Set<UUID> of DrawObjs in any selected AG
	 * @throws IllegalArgumentException if agNums is null or contains invalid AG numbers
	 */
	public Set<UUID> getDrawObjsForSelectedAGs(Set<Integer> agNums) {
		if (agNums == null) {
			throw new IllegalArgumentException("AG numbers set cannot be null");
		}
		
		Set<UUID> result = new HashSet<>();
		
		// For each AG number in the set, get its members and add to result (union)
		for (Integer agNum : agNums) {
			if (agNum != null && agNum >= 1 && agNum <= 4) {
				Set<UUID> agMembers = rmatrix.getAGMembersUUIDs(agNum);
				if (agMembers != null) {
					result.addAll(agMembers);
				}
			}
		}
		
		return result;
	}

	/**
	 * Queries all DrawObjs in the template.
	 * 
	 * This is a pure query method that returns raw data (UUIDs) without transformation.
	 * Returns the UUIDs of all DrawObjs regardless of AG assignment.
	 * 
	 * @return Set<UUID> of all DrawObjs in the template
	 */
	public Set<UUID> getAllDrawObjs() {
		Set<UUID> result = new HashSet<>();
		
		// Get all ShapeObj instances from the roster and extract their UUIDs
		List<ShapeObj> allShapes = shapeObjRoster.getShapeObjList();
		for (ShapeObj shape : allShapes) {
			result.add(shape.getUUID());
		}
		
		return result;
	}

	/**
	 * Queries all DrawObjs that have been assigned to at least one AG.
	 * 
	 * This is a pure query method that returns raw data (UUIDs) without transformation.
	 * Returns the UUIDs of all DrawObjs that have at least one AG assignment.
	 * This is distinct from getAllDrawObjs() which includes unassigned DrawObjs.
	 * 
	 * @return Set<UUID> of all DrawObjs assigned to at least one AG
	 */
	public Set<UUID> getDrawObjsForAllAGs() {
		Set<UUID> result = new HashSet<>();
		
		// Get all ShapeObj instances from the roster
		List<ShapeObj> allShapes = shapeObjRoster.getShapeObjList();
		
		// For each shape, check if it has any AG assignments
		for (ShapeObj shape : allShapes) {
			UUID uuid = shape.getUUID();
			if (uuid != null) {
				// Get all AGs this DrawObj is assigned to
				Set<Integer> ags = rmatrix.getDrawObjAGs(uuid);
				// If it has at least one AG assignment, include it
				if (ags != null && !ags.isEmpty()) {
					result.add(uuid);
				}
			}
		}
		
		return result;
	}

	/**
	 * Helper method to check if a hue is within a specified range.
	 * Handles wrap-around at the hue circle boundary (36→1).
	 * 
	 * @param hue The hue to check (1-36)
	 * @param ccwLimit Counter-clockwise limit (1-36)
	 * @param cwLimit Clockwise limit (1-36)
	 * @return true if hue is in range, false otherwise
	 */
	private boolean isHueInRange(int hue, int ccwLimit, int cwLimit) {
		if (ccwLimit <= cwLimit) { // Note "AND" logic - satisfies both conditions
			// Normal case: no wrap-around (e.g., CCW=5, CW=10)
			return hue >= ccwLimit && hue <= cwLimit; // Note concise syntax
		} else { // Note "OR" logic - satisfies either condition
			// Wrap-around case: range crosses 36→1 boundary (e.g., CCW=30, CW=5)
			return hue >= ccwLimit || hue <= cwLimit; // Note concise syntax
		}
	} // end method isHueInRange




	// ============================================================================
	// CONVERSION METHODS
	// ============================================================================
	
	/**
	 * Converts this TemplateProxyData back to a Template.
	 * 
	 * This method creates a new Template from the current state of TemplateProxyData,
	 * including the RMatrixMini and AnchorPattern. The new Template can be saved as
	 * a new instance with artistic intelligence.
	 * 
	 * Conversion process:
	 * 1. Convert ShapeObj instances back to DrawObj instances
	 * 2. Retrieve colors from the palette
	 * 3. Create new DrawObjRoster with converted DrawObjs
	 * 4. Create new Template with converted data
	 * 5. Attach RMatrixMini and AnchorPattern to new Template
	 * 
	 * @param templateName the name for the new Template
	 * @return a new Template created from this TemplateProxyData
	 * @throws IllegalArgumentException if templateName is null or empty
	 */
	public Template createTemplate(String templateName) {
		if (templateName == null || templateName.trim().isEmpty()) {
			throw new IllegalArgumentException("Template name cannot be null or empty");
		}
		
		// Convert ShapeObj instances back to DrawObj instances with colors from palette
		// Note: ShapeObjRoster must have convertToDrawObjRoster(PaletteWName) method
		// DrawObjRoster newDrawObjRoster = shapeObjRoster.convertToDrawObjRoster(palette);
		
		// Create new Template with converted data
		// Note: Template constructor must accept (String, DrawObjRoster, PaletteWName)
		// Template newTemplate = new Template(templateName, newDrawObjRoster, palette);
		
		// Attach RMatrixMini and AnchorPattern to new Template
		// Note: Template must have setRMatrixMini() and setAnchorPattern() methods
		// newTemplate.setRMatrixMini(rmatrix);
		// newTemplate.setAnchorPattern(anchorPattern);
		
		// Placeholder: return null for now
		return null;
	}
	
} // end class TemplateProxyData
