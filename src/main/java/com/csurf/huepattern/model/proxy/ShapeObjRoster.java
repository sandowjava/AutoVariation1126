package com.csurf.huepattern.model.proxy;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Optional;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.model.PaletteWName;

/**
 * ShapeObjRoster represents a simplified version of DrawObjRoster for use within TemplateProxyData.
 * 
 * Purpose:
 * - Manages a collection of ShapeObj instances (working copies of DrawObj)
 * - Provides query and manipulation methods for ShapeObj collection
 * - Does NOT manage palette (palette is maintained separately in TemplateProxyData)
 * - Supports one-way data flow from TemplateWorkFrame to TemplateProxyData
 * 
 * Key differences from DrawObjRoster:
 * - No palette management (palette is separate entity in TemplateProxyData)
 * - Simplified structure optimized for working copy use case
 * - Focused on shape collection management only
 * 
 * Responsibilities:
 * - Store and retrieve ShapeObj instances
 * - Provide query methods for finding shapes by UUID or name
 * - Support iteration over all shapes
 * - Enable addition and removal of shapes
 * 
 * Lifecycle:
 * - Created when TemplateProxyData is initialized from Template
 * - Modified during AGsInfoFrame operations
 * - Converted back to DrawObjRoster when TemplateProxyData is converted to Template
 */
public class ShapeObjRoster {
	
	// ============================================================================
	// FIELDS
	// ============================================================================
	
	private final List<ShapeObj> shapeObjs;
	
	// ============================================================================
	// CONSTRUCTORS
	// ============================================================================
	
	/**
	 * Creates a new empty ShapeObjRoster.
	 */
	public ShapeObjRoster() {
		this.shapeObjs = new ArrayList<>();
	}
	
	/**
	 * Creates a new ShapeObjRoster from a DrawObjRoster.
	 * 
	 * Converts all DrawObj instances to ShapeObj instances (without color data).
	 * 
	 * @param drawObjRoster the DrawObjRoster to create from
	 * @throws IllegalArgumentException if drawObjRoster is null
	 */
	public ShapeObjRoster(DrawObjRoster drawObjRoster) {
		if (drawObjRoster == null) {
			throw new IllegalArgumentException("DrawObjRoster cannot be null");
		}
		
		this.shapeObjs = new ArrayList<>();
		
		// Convert each DrawObj to ShapeObj
		for (DrawObj drawObj : drawObjRoster.getTemplateDrawObjs()) {
			this.shapeObjs.add(new ShapeObj(drawObj));
		}
	}
	
	// ============================================================================
	// GETTERS
	// ============================================================================
	
	/**
	 * Returns the list of all ShapeObj instances.
	 * 
	 * @return unmodifiable list of ShapeObj instances
	 */
	public List<ShapeObj> getShapeObjList() {
		return new ArrayList<>(shapeObjs);
	}
	
	/**
	 * Returns the number of ShapeObj instances in this roster.
	 * 
	 * @return the count
	 */
	public int getCount() {
		return shapeObjs.size();
	}
	
	/**
	 * Finds a ShapeObj by UUID.
	 * 
	 * @param uuid the UUID to search for
	 * @return Optional containing the ShapeObj if found, empty otherwise
	 * @throws IllegalArgumentException if uuid is null
	 */
	public Optional<ShapeObj> findByUUID(UUID uuid) {
		if (uuid == null) {
			throw new IllegalArgumentException("UUID cannot be null");
		}
		
		return shapeObjs.stream()
			.filter(shape -> shape.getUUID().equals(uuid))
			.findFirst();
	}
	
	/**
	 * Finds a ShapeObj by name.
	 * 
	 * @param name the name to search for
	 * @return Optional containing the ShapeObj if found, empty otherwise
	 * @throws IllegalArgumentException if name is null
	 */
	public Optional<ShapeObj> findByName(String name) {
		if (name == null) {
			throw new IllegalArgumentException("Name cannot be null");
		}
		
		return shapeObjs.stream()
			.filter(shape -> shape.getName().equals(name))
			.findFirst();
	}
	
	/**
	 * Finds all ShapeObj instances with a given name prefix.
	 * 
	 * @param namePrefix the name prefix to search for
	 * @return list of matching ShapeObj instances
	 * @throws IllegalArgumentException if namePrefix is null
	 */
	public List<ShapeObj> findByNamePrefix(String namePrefix) {
		if (namePrefix == null) {
			throw new IllegalArgumentException("Name prefix cannot be null");
		}
		
		List<ShapeObj> results = new ArrayList<>();
		for (ShapeObj shape : shapeObjs) {
			if (shape.getName().startsWith(namePrefix)) {
				results.add(shape);
			}
		}
		return results;
	}
	
	// ============================================================================
	// MANIPULATION METHODS
	// ============================================================================
	
	/**
	 * Adds a ShapeObj to this roster.
	 * 
	 * @param shapeObj the ShapeObj to add
	 * @throws IllegalArgumentException if shapeObj is null
	 */
	public void addShapeObj(ShapeObj shapeObj) {
		if (shapeObj == null) {
			throw new IllegalArgumentException("ShapeObj cannot be null");
		}
		
		shapeObjs.add(shapeObj);
	}
	
	/**
	 * Removes a ShapeObj by UUID.
	 * 
	 * @param uuid the UUID of the ShapeObj to remove
	 * @return true if removed, false if not found
	 * @throws IllegalArgumentException if uuid is null
	 */
	public boolean removeByUUID(UUID uuid) {
		if (uuid == null) {
			throw new IllegalArgumentException("UUID cannot be null");
		}
		
		return shapeObjs.removeIf(shape -> shape.getUUID().equals(uuid));
	}
	
	/**
	 * Clears all ShapeObj instances from this roster.
	 */
	public void clear() {
		shapeObjs.clear();
	}
	
	// ============================================================================
	// CONVERSION METHODS
	// ============================================================================
	
	/**
	 * Converts this ShapeObjRoster back to a DrawObjRoster.
	 * 
	 * This method is a placeholder for future implementation.
	 * It will convert ShapeObj instances back to DrawObj instances with colors from palette.
	 * 
	 * @param palette the PaletteWName containing colors for each DrawObj
	 * @return a new DrawObjRoster with converted DrawObj instances
	 * @throws IllegalArgumentException if palette is null
	 */
	public DrawObjRoster convertToDrawObjRoster(PaletteWName palette) {
		if (palette == null) {
			throw new IllegalArgumentException("Palette cannot be null");
		}
		
		// Placeholder: return null for now
		// This will be implemented when we have full access to DrawObj constructor
		// and understand how to retrieve colors from palette
		return null;
	}
	
	// ============================================================================
	// UTILITY METHODS
	// ============================================================================
	
	/**
	 * Returns a string representation of this ShapeObjRoster.
	 * 
	 * @return string representation
	 */
	@Override
	public String toString() {
		return String.format("ShapeObjRoster(count=%d)", shapeObjs.size());
	}
	
} // end class ShapeObjRoster
