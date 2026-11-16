package com.csurf.huepattern.model.proxy;

import java.util.UUID;
import com.csurf.templatecore.model.DrawObj;

/**
 * ShapeObj represents a simplified version of DrawObj for use within TemplateProxyData.
 * 
 * Purpose:
 * - Holds shape information without color data (color is stored separately in PaletteWName)
 * - Used as the working copy model for DrawObj during AGsInfoFrame operations
 * - Supports one-way data flow from TemplateWorkFrame to TemplateProxyData
 * - Enables independent modification of shape attributes without affecting original DrawObj
 * 
 * Key differences from DrawObj:
 * - No color field (palette is maintained separately)
 * - Immutable UUID for tracking shape identity
 * - Simplified structure optimized for working copy use case
 * 
 * Lifecycle:
 * - Created when TemplateProxyData is initialized from Template
 * - Modified during AGsInfoFrame operations (AG assignments, proportional locations)
 * - Converted back to DrawObj when TemplateProxyData is converted to Template
 */
public class ShapeObj {
	
	// ============================================================================
	// FIELDS
	// ============================================================================
	
	private final UUID uuid;
	private final String name;
	private final String path;
	
	// ============================================================================
	// CONSTRUCTOR
	// ============================================================================
	
	/**
	 * Creates a new ShapeObj from a DrawObj.
	 * 
	 * Extracts shape information (UUID, name, path) from DrawObj without color data.
	 * 
	 * @param drawObj the DrawObj to create a ShapeObj from
	 * @throws IllegalArgumentException if drawObj is null
	 */
	public ShapeObj(DrawObj drawObj) {
		if (drawObj == null) {
			throw new IllegalArgumentException("DrawObj cannot be null");
		}
		
		this.uuid = drawObj.getUniqueId();
		this.name = drawObj.getName();
		this.path = drawObj.getName(); // Use name as path identifier (DrawObj.getPath() returns GeneralPath, not String)
	}
	
	/**
	 * Creates a new ShapeObj with explicit parameters.
	 * 
	 * @param uuid the unique identifier for this shape
	 * @param name the name of the shape
	 * @param path the path of the shape
	 * @throws IllegalArgumentException if uuid, name, or path is null
	 */
	public ShapeObj(UUID uuid, String name, String path) {
		if (uuid == null) {
			throw new IllegalArgumentException("UUID cannot be null");
		}
		if (name == null) {
			throw new IllegalArgumentException("Name cannot be null");
		}
		if (path == null) {
			throw new IllegalArgumentException("Path cannot be null");
		}
		
		this.uuid = uuid;
		this.name = name;
		this.path = path;
	}
	
	// ============================================================================
	// GETTERS
	// ============================================================================
	
	/**
	 * Returns the unique identifier for this shape.
	 * 
	 * @return the UUID (immutable)
	 */
	public UUID getUUID() {
		return uuid;
	}
	
	/**
	 * Returns the name of this shape.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the path of this shape.
	 * 
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
	
	// ============================================================================
	// UTILITY METHODS
	// ============================================================================
	
	/**
	 * Returns a string representation of this ShapeObj.
	 * 
	 * Format: "ShapeObj(uuid=..., name=..., path=...)"
	 * 
	 * @return string representation
	 */
	@Override
	public String toString() {
		return String.format("ShapeObj(uuid=%s, name=%s, path=%s)", uuid, name, path);
	}
	
	/**
	 * Checks equality based on UUID.
	 * 
	 * Two ShapeObj instances are equal if they have the same UUID.
	 * 
	 * @param obj the object to compare
	 * @return true if equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ShapeObj other = (ShapeObj) obj;
		return uuid.equals(other.uuid);
	}
	
	/**
	 * Returns hash code based on UUID.
	 * 
	 * @return hash code
	 */
	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
	
} // end class ShapeObj
