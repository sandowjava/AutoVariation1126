package com.csurf.templatecore.model.serialization;

import com.csurf.templatecore.model.DrawObj;

/**
 * A reference to a DrawObj instance used during serialization/deserialization.
 * This class stores the unique ID of a DrawObj instance, which can be used to
 * resolve the reference during deserialization.
 */
public class DrawObjReference {
    private String uniqueId;
    
    /**
     * Default constructor for Jackson deserialization.
     */
    public DrawObjReference() {
    }
    
    /**
     * Creates a reference to the specified DrawObj.
     * 
     * @param drawObj the DrawObj to reference
     */
    public DrawObjReference(DrawObj drawObj) {
        if (drawObj == null) {
            throw new IllegalArgumentException("DrawObj cannot be null");
        }
        this.uniqueId = drawObj.getUniqueId().toString();
    }
    
    /**
     * Gets the unique ID of the referenced DrawObj.
     * 
     * @return the unique ID
     */
    public String getUniqueId() {
        return uniqueId;
    }
    
    /**
     * Sets the unique ID of the referenced DrawObj.
     * 
     * @param uniqueId the unique ID
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DrawObjReference that = (DrawObjReference) o;
        return uniqueId != null ? uniqueId.equals(that.uniqueId) : that.uniqueId == null;
    }
    
    @Override
    public int hashCode() {
        return uniqueId != null ? uniqueId.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "DrawObjReference{uniqueId='" + uniqueId + "'}";
    }
}
