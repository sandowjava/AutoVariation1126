package com.csurf.huepattern.service;

import java.util.logging.Logger;
import com.csurf.templatecore.model.Template;
import com.csurf.huepattern.model.RMatrixMiniPrelim;
import com.csurf.huepattern.model.proxy.TemplateProxyData;

/**
 * TemplateProxyDataService provides service controller methods for TemplateProxyData operations.
 * 
 * Purpose:
 * - Encapsulates business logic for creating and manipulating TemplateProxyData
 * - Provides stateless, reusable methods for TemplateProxyData operations
 * - Follows Service Controller Pattern (similar to TemplatePaletteShift)
 * - Separates business logic from GUI components
 * 
 * Key Responsibilities:
 * - Create TemplateProxyData from Template (copying data)
 * - Convert TemplateProxyData back to Template (with artistic intelligence)
 * - Synchronize AG assignments from TemplateWorkFrame to TemplateProxyData
 * - Validate TemplateProxyData state
 * - Handle data transformations between Template and TemplateProxyData
 * 
 * Key Methods:
 * - createTemplateProxyData(Template): Create new TemplateProxyData as copy
 * - convertToTemplate(TemplateProxyData, String): Convert TemplateProxyData back to Template
 * - synchronizeWithTemplateWorkFrame(TemplateProxyData, RMatrixMini): Update AG assignments
 * 
 * Design Pattern:
 * - Stateless service controller (all methods are static)
 * - Reusable across multiple TemplateProxyData instances
 * - Clear separation of concerns from GUI and data model
 * 
 * Data Flow:
 * - TemplateWorkFrame → TemplateProxyDataService → TemplateProxyData
 * - TemplateProxyData → TemplateProxyDataService → new Template
 * 
 * Related Classes:
 * - TemplateProxyData: The working copy data model
 * - Template: The committed data model
 * - AGsInfoFrame: Uses TemplateProxyDataService to create and convert TemplateProxyData
 */
public class TemplateProxyDataService {
	private static final Logger logger = Logger.getLogger(TemplateProxyDataService.class.getName());
	
	/**
	 * Creates a new TemplateProxyData from a Template.
	 * 
	 * This method creates a working copy of the Template with:
	 * - NEW ShapeObjRoster (copy of DrawObjRoster)
	 * - NEW PaletteWName (copy of palette)
	 * - NEW RMatrixMini instance (not a reference)
	 * - NEW AnchorPattern instance (not a reference)
	 * 
	 * @param template the Template to create TemplateProxyData from
	 * @return a new TemplateProxyData instance
	 * @throws IllegalArgumentException if template is null
	 */
	public static TemplateProxyData createTemplateProxyData(Template template) {
		if (template == null) {
			throw new IllegalArgumentException("Template cannot be null");
		}
		
		logger.info("TemplateProxyDataService: Creating TemplateProxyData from Template");
		
		TemplateProxyData proxyData = new TemplateProxyData(template);
		
		logger.info("TemplateProxyDataService: TemplateProxyData created successfully");
		return proxyData;
	}
	
	/**
	 * Converts TemplateProxyData back to a Template.
	 * 
	 * This method creates a new Template from the current state of TemplateProxyData,
	 * including the RMatrixMini and AnchorPattern. The new Template can be saved as
	 * a new instance with artistic intelligence.
	 * 
	 * @param proxyData the TemplateProxyData to convert
	 * @param templateName the name for the new Template
	 * @return a new Template created from TemplateProxyData
	 * @throws IllegalArgumentException if proxyData or templateName is null/empty
	 */
	public static Template convertToTemplate(TemplateProxyData proxyData, String templateName) {
		if (proxyData == null) {
			throw new IllegalArgumentException("TemplateProxyData cannot be null");
		}
		if (templateName == null || templateName.trim().isEmpty()) {
			throw new IllegalArgumentException("Template name cannot be null or empty");
		}
		
		logger.info("TemplateProxyDataService: Converting TemplateProxyData to Template");
		
		Template newTemplate = proxyData.createTemplate(templateName);
		
		logger.info("TemplateProxyDataService: Conversion complete, new Template created");
		return newTemplate;
	}
	
	/**
	 * Synchronizes AG assignments from TemplateWorkFrame to TemplateProxyData.
	 * 
	 * This method updates the TemplateProxyData's RMatrixMini with AG assignments
	 * from the TemplateWorkFrame's RMatrixMini. This implements the one-way data flow
	 * from TemplateWorkFrame to TemplateProxyData during UC-042.
	 * 
	 * @param proxyData the TemplateProxyData to update
	 * @param sourceRMatrix the source RMatrixMini from TemplateWorkFrame
	 * @throws IllegalArgumentException if proxyData or sourceRMatrix is null
	 */
	public static void synchronizeWithTemplateWorkFrame(TemplateProxyData proxyData, RMatrixMiniPrelim sourceRMatrix) {
		if (proxyData == null) {
			throw new IllegalArgumentException("TemplateProxyData cannot be null");
		}
		if (sourceRMatrix == null) {
			throw new IllegalArgumentException("Source RMatrixMini cannot be null");
		}
		
		logger.info("TemplateProxyDataService: Synchronizing AG assignments from TemplateWorkFrame");
		
		// Copy AG assignments from source to destination
		RMatrixMiniPrelim destRMatrix = proxyData.getRMatrixMini();
		
		// For each AG, copy the members
		for (int agNum = 1; agNum <= 4; agNum++) {
			java.util.Set<com.csurf.templatecore.model.DrawObj> sourceMembers = sourceRMatrix.getAGMembersSet(agNum);
			for (com.csurf.templatecore.model.DrawObj drawObj : sourceMembers) {
				destRMatrix.addToAG(drawObj.getUniqueId(), agNum);
			}
		}
		
		logger.info("TemplateProxyDataService: Synchronization complete");
	}
	
	/**
	 * Validates that TemplateProxyData is in a valid state.
	 * 
	 * This method checks:
	 * - TemplateProxyData is not null
	 * - RMatrixMini is not null
	 * - AnchorPattern is not null
	 * - Palette is not null
	 * - ShapeObjRoster is not null
	 * 
	 * @param proxyData the TemplateProxyData to validate
	 * @return true if valid, false otherwise
	 */
	public static boolean validateTemplateProxyData(TemplateProxyData proxyData) {
		if (proxyData == null) {
			logger.warning("TemplateProxyDataService: TemplateProxyData is null");
			return false;
		}
		
		if (proxyData.getRMatrixMini() == null) {
			logger.warning("TemplateProxyDataService: RMatrixMini is null");
			return false;
		}
		
		if (proxyData.getAnchorPattern() == null) {
			logger.warning("TemplateProxyDataService: AnchorPattern is null");
			return false;
		}
		
		if (proxyData.getPalette() == null) {
			logger.warning("TemplateProxyDataService: Palette is null");
			return false;
		}
		
		if (proxyData.getShapeObjRoster() == null) {
			logger.warning("TemplateProxyDataService: ShapeObjRoster is null");
			return false;
		}
		
		logger.info("TemplateProxyDataService: TemplateProxyData validation passed");
		return true;
	}
}
