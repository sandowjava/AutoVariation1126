package com.csurf.templatecore.model.serialization;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.SelectionGroup;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Custom serializer for SelectionGroup.
 * Converts DrawObj references to their unique IDs during serialization.
 * 
 * This serializer follows the invariant that during serialization of SelectionGroups,
 * only the UUIDs should be serialized, not the actual DrawObj instances.
 * DrawObj instances should only be created during DrawObjRoster deserialization.
 * 
 * The serialized form of a SelectionGroup is a JSON object with a single field "drawObjUniqueIds",
 * which is an array of strings representing the unique IDs of the DrawObj instances in the group.
 */
public class SelectionGroupSerializer extends StdSerializer<SelectionGroup> {
    
    // Counter for tracking serialization statistics
    private static int totalSerializedGroups = 0;
    private static int totalSerializedUUIDs = 0;
    
    public SelectionGroupSerializer() {
        this(null);
    }
    
    public SelectionGroupSerializer(Class<SelectionGroup> t) {
        super(t);
    }
    
    /**
     * Standardizes UUID string format for consistent comparison.
     * This ensures that UUIDs are compared in a consistent format regardless of
     * how they were originally formatted (with/without hyphens, upper/lowercase).
     * 
     * @param uuid The UUID string to standardize
     * @return The standardized UUID string (lowercase with no hyphens)
     */
    private String standardizeUuid(String uuid) {
        if (uuid == null) return null;
        // Remove hyphens and convert to lowercase for consistent comparison
        return uuid.replace("-", "").toLowerCase();
    }
    
    /**
     * Helper method to count and track UUIDs during serialization
     * @param drawObjs Set of DrawObj instances to extract UUIDs from
     * @return List of UUID strings extracted from the DrawObj instances
     */
    private List<String> extractAndCountUniqueIds(Set<DrawObj> drawObjs) {
        List<String> uniqueIds = new ArrayList<>();
        int validUuidCount = 0;
        int nullUuidCount = 0;
        
        System.out.println("\n==== SelectionGroupSerializer: UUID Extraction Start ====");
        System.out.println("Processing " + drawObjs.size() + " DrawObj instances for UUID extraction");
        
        // Extract unique IDs from DrawObj instances
        for (DrawObj drawObj : drawObjs) {
            if (drawObj.getUniqueId() != null) {
                String uniqueId = standardizeUuid(drawObj.getUniqueId().toString());
                uniqueIds.add(uniqueId);
                validUuidCount++;
                System.out.println("  - Added UUID: " + uniqueId + " for DrawObj: " + drawObj);
            } else {
                nullUuidCount++;
                System.out.println("  - WARNING: DrawObj has null UUID: " + drawObj);
            }
        }
        
        // Update global statistics
        totalSerializedUUIDs += validUuidCount;
        totalSerializedGroups++;
        
        // Print summary for this extraction
        System.out.println("\nUUID Extraction Summary:");
        System.out.println("  - DrawObj instances processed: " + drawObjs.size());
        System.out.println("  - Valid UUIDs extracted: " + validUuidCount);
        System.out.println("  - Null UUIDs encountered: " + nullUuidCount);
        System.out.println("==== SelectionGroupSerializer: UUID Extraction Complete ====\n");
        
        return uniqueIds;
    }
    
    @Override
    public void serialize(SelectionGroup selectionGroup, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        
        // Get the DrawObj references
        Set<DrawObj> drawObjs = selectionGroup.getSelectedDrawObjsSet();
        
        System.out.println("\n===== SelectionGroupSerializer: Serializing SelectionGroup =====");
        System.out.println("SelectionGroup contains " + drawObjs.size() + " DrawObj references");
        
        // Check if the SelectionGroup already has UUID references
        List<String> existingUuids = selectionGroup.getDrawObjUniqueIds();
        System.out.println("SelectionGroup already has " + existingUuids.size() + " UUID references");
        
        // Extract and count unique IDs
        List<String> uniqueIds = extractAndCountUniqueIds(drawObjs);
        
        // Write the unique IDs as a simple array
        gen.writeArrayFieldStart("drawObjUniqueIds");
        for (String uniqueId : uniqueIds) {
            gen.writeString(uniqueId);
        }
        gen.writeEndArray();
        
        gen.writeEndObject();
        
        // Print global statistics
        System.out.println("\n***** SelectionGroupSerializer: Global Statistics *****");
        System.out.println("Total SelectionGroups serialized: " + totalSerializedGroups);
        System.out.println("Total UUIDs serialized: " + totalSerializedUUIDs);
        System.out.println("Average UUIDs per SelectionGroup: " + 
                          (totalSerializedGroups > 0 ? (float)totalSerializedUUIDs/totalSerializedGroups : 0));
        System.out.println("Current SelectionGroup UUID count: " + uniqueIds.size());
        System.out.println("===== SelectionGroupSerializer: Serialization Complete =====\n");
    }
}
