package com.csurf.huepattern.view.agsinfo;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import com.csurf.huepattern.model.proxy.TemplateProxyData;
import com.csurf.huepattern.model.proxy.ShapeObj;
import com.csurf.huepattern.model.proxy.ShapeObjRoster;
import com.csurf.templatecore.model.DrawObj;

/**
 * DrawObjsDisplayPanel is a GUI component that displays DrawObjs (as ShapeObj) in context.
 * 
 * Purpose:
 * - Displays a filtered list of ShapeObj instances based on context
 * - Enables user selection of DrawObjs for modification
 * - Supports context-dependent display (selected AG, multiple AGs, outside spreads, etc.)
 * - Provides visual representation of DrawObj collection for interactive work
 * 
 * Key Responsibilities:
 * - Display ShapeObj instances in a scrollable list
 * - Support filtering based on context (AG membership, hue location, etc.)
 * - Enable user selection of single or multiple DrawObjs
 * - Provide methods to retrieve selected DrawObjs
 * - Refresh display when underlying data changes
 * - Show DrawObj information (name, path, current AG assignment, hue, etc.)
 * 
 * Display Contexts:
 * - Show AG1, Show AG2, Show AG3, Show AG4: Display DrawObjs in specific AG
 * - Show selected AGs: Display DrawObjs in user-selected AGs
 * - Show full image: Display all DrawObjs
 * - Show selected DrawObj info: Display detailed info for selected DrawObj
 * - Check for multiple AGs: Display DrawObjs in multiple AGs
 * - Check within Spreads: Display DrawObjs outside spread limits
 * - Check no AG assigned: Display DrawObjs with no AG assignment
 * 
 * Data Model:
 * - References TemplateProxyData for access to ShapeObjRoster and RMatrixMini
 * - Displays ShapeObj instances from ShapeObjRoster
 * - Shows AG assignments from TemplateProxyData.rmatrix
 * - Shows hue information from TemplateProxyData.palette
 * 
 * User Interactions:
 * - Select DrawObj(s) for modification
 * - Trigger context menu for DrawObj operations
 * - Double-click to edit DrawObj properties
 * 
 * Related Classes:
 * - AGsInfoFrame: Parent frame that uses this panel
 * - TemplateProxyData: Data model for displayed DrawObjs
 * - ShapeObj: Individual shape being displayed
 * - ShapeObjRoster: Collection of shapes being displayed
 */
public class DrawObjsDisplayPanel extends JPanel {
	private static final Logger logger = Logger.getLogger(DrawObjsDisplayPanel.class.getName());
	
	private TemplateProxyData templateProxyData;
	private JList<String> displayList;
	private DefaultListModel<String> listModel;
	private JScrollPane scrollPane;
	
	/**
	 * Creates a new DrawObjsDisplayPanel.
	 * 
	 * @param templateProxyData the TemplateProxyData to display
	 * @throws IllegalArgumentException if templateProxyData is null
	 */
	public DrawObjsDisplayPanel(TemplateProxyData templateProxyData) {
		if (templateProxyData == null) {
			throw new IllegalArgumentException("TemplateProxyData cannot be null");
		}
		
		this.templateProxyData = templateProxyData;
		
		logger.info("DrawObjsDisplayPanel: Creating with TemplateProxyData");
		
		initializePanel();
	}
	
	/**
	 * Displays all DrawObjs.
	 */
	public void displayAllDrawObjs() {
		logger.info("DrawObjsDisplayPanel: Displaying all DrawObjs");
		listModel.clear();
		
		List<ShapeObj> allShapes = templateProxyData.getShapeObjRoster().getShapeObjList();
		for (ShapeObj shape : allShapes) {
			String displayText = formatShapeDisplay(shape);
			listModel.addElement(displayText);
		}
	}
	
	/**
	 * Displays DrawObjs for a specific AG.
	 * 
	 * @param agNum the AG number (1-4)
	 */
	public void displayDrawObjsForAG(int agNum) {
		logger.info("DrawObjsDisplayPanel: Displaying DrawObjs for AG " + agNum);
		listModel.clear();
		
		Set<DrawObj> agMembers = templateProxyData.getRMatrixMini().getAGMembersSet(agNum);
		for (DrawObj drawObj : agMembers) {
			ShapeObj shape = templateProxyData.getShapeObjRoster().findByUUID(drawObj.getUniqueId()).orElse(null);
			if (shape != null) {
				String displayText = formatShapeDisplay(shape);
				listModel.addElement(displayText);
			}
		}
	}
	
	/**
	 * Displays DrawObjs in multiple AGs.
	 */
	public void displayDrawObjsInMultipleAGs() {
		logger.info("DrawObjsDisplayPanel: Displaying DrawObjs in multiple AGs");
		listModel.clear();
		
		Set<DrawObj> multipleAGs = templateProxyData.getRMatrixMini().getDrawObjsInMultipleAGs();
		for (DrawObj drawObj : multipleAGs) {
			ShapeObj shape = templateProxyData.getShapeObjRoster().findByUUID(drawObj.getUniqueId()).orElse(null);
			if (shape != null) {
				String displayText = formatShapeDisplay(shape);
				listModel.addElement(displayText);
			}
		}
	}
	
	/**
	 * Displays DrawObjs outside spread limits.
	 */
	public void displayDrawObjsOutsideSpreads() {
		logger.info("DrawObjsDisplayPanel: Displaying DrawObjs outside spreads");
		listModel.clear();
		
		// Placeholder: Will be implemented when we have hue validation logic
		logger.fine("DrawObjsDisplayPanel: Outside spreads display not yet implemented");
	}
	
	/**
	 * Displays DrawObjs with no AG assignment.
	 */
	public void displayDrawObjsWithNoAG() {
		logger.info("DrawObjsDisplayPanel: Displaying DrawObjs with no AG assignment");
		listModel.clear();
		
		Set<DrawObj> noAG = templateProxyData.getRMatrixMini().getDrawObjsWithNoAG();
		for (DrawObj drawObj : noAG) {
			ShapeObj shape = templateProxyData.getShapeObjRoster().findByUUID(drawObj.getUniqueId()).orElse(null);
			if (shape != null) {
				String displayText = formatShapeDisplay(shape);
				listModel.addElement(displayText);
			}
		}
	}
	
	/**
	 * Gets the currently selected ShapeObj UUIDs.
	 * 
	 * @return list of selected UUIDs
	 */
	public List<UUID> getSelectedUUIDs() {
		List<UUID> selectedUUIDs = new ArrayList<>();
		int[] selectedIndices = displayList.getSelectedIndices();
		
		for (int index : selectedIndices) {
			String displayText = listModel.getElementAt(index);
			// Extract UUID from display text (format: "Name (AG: X)")
			// For now, return empty list as placeholder
		}
		
		return selectedUUIDs;
	}
	
	/**
	 * Initializes the panel components.
	 */
	private void initializePanel() {
		setLayout(new BorderLayout());
		
		// Create list model and list
		listModel = new DefaultListModel<>();
		displayList = new JList<>(listModel);
		displayList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		// Create scroll pane
		scrollPane = new JScrollPane(displayList);
		
		// Add to panel
		add(scrollPane, BorderLayout.CENTER);
		
		logger.fine("DrawObjsDisplayPanel: Panel initialization complete");
	}
	
	/**
	 * Formats a ShapeObj for display.
	 * 
	 * @param shape the ShapeObj to format
	 * @return formatted display string
	 */
	private String formatShapeDisplay(ShapeObj shape) {
		int agNum = templateProxyData.getRMatrixMini().getCurrAGAssignment(shape.getUUID());
		String agText = (agNum > 0) ? "AG: " + agNum : "No AG";
		return shape.getName() + " (" + agText + ")";
	}
}
