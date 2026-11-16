package com.csurf.templatecore.model.serialization;

import com.csurf.templatecore.model.SelectionGroup;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Custom deserializer for SelectionGroup.
 * 
 * This deserializer follows the invariant that during deserialization of SelectionGroups,
 * only the UUIDs should be deserialized, not creating any DrawObj instances.
 * DrawObj instances should only be created during DrawObjRoster deserialization.
 * 
 * During deserialization, this class creates a SelectionGroup with only UUID references.
 * The actual DrawObj references need to be resolved after the entire object graph is deserialized
 * using Template.resolveSelectionGroupReferences().
 * 
 * The expected serialized form of a SelectionGroup is a JSON object with a single field
 * "drawObjUniqueIds", which is an array of strings representing the unique IDs of the
 * DrawObj instances in the group.
 */
public class SelectionGroupDeserializer extends StdDeserializer<SelectionGroup> {
    
    // Counter for tracking deserialization statistics
    private static int totalDeserializedGroups = 0;
    private static int totalDeserializedUUIDs = 0;
    
    public SelectionGroupDeserializer() {
        this(null);
    }
    
    public SelectionGroupDeserializer(Class<?> vc) {
        super(vc);
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
     * Helper method to extract and count UUIDs from JSON
     * @param uniqueIdsNode JsonNode containing the array of UUID strings
     * @return List of UUID strings extracted from the JSON
     */
    private List<String> extractAndCountUniqueIds(JsonNode uniqueIdsNode) {
        List<String> drawObjUniqueIds = new ArrayList<>();
        
        System.out.println("\n==== SelectionGroupDeserializer: UUID Extraction Start ====");
        
        if (uniqueIdsNode != null) {
            if (uniqueIdsNode.isArray()) {
                int nodeCount = uniqueIdsNode.size();
                System.out.println("Found array with " + nodeCount + " elements");
                
                for (JsonNode idNode : uniqueIdsNode) {
                    if (idNode.isTextual()) {
                        String uniqueId = standardizeUuid(idNode.asText());
                        drawObjUniqueIds.add(uniqueId);
                        System.out.println("  - Added UUID: " + uniqueId);
                    } else if (idNode.isObject() && idNode.has("value")) {
                        // Handle case where UUID might be in a nested object with a "value" field
                        String uniqueId = standardizeUuid(idNode.get("value").asText());
                        drawObjUniqueIds.add(uniqueId);
                        System.out.println("  - Added UUID from object: " + uniqueId);
                    } else {
                        System.out.println("  - WARNING: Unexpected node type in array: " + idNode.getNodeType());
                        System.out.println("  - Node content: " + idNode.toString());
                        
                        // Try to extract as string anyway
                        try {
                            String uniqueId = standardizeUuid(idNode.asText());
                            if (uniqueId != null && !uniqueId.isEmpty()) {
                                drawObjUniqueIds.add(uniqueId);
                                System.out.println("  - Attempted to add as string: " + uniqueId);
                            }
                        } catch (Exception e) {
                            System.out.println("  - Failed to extract as string: " + e.getMessage());
                        }
                    }
                }
            } else if (uniqueIdsNode.isObject()) {
                System.out.println("Node is an object, trying to extract UUIDs from its fields");
                
                // Try to find any array fields in the object
                Iterator<String> fieldNames = uniqueIdsNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    JsonNode fieldValue = uniqueIdsNode.get(fieldName);
                    
                    System.out.println("  - Checking field: " + fieldName);
                    
                    if (fieldValue.isArray()) {
                        System.out.println("  - Found array in field: " + fieldName);
                        // Recursively extract UUIDs from this array
                        List<String> nestedUuids = extractAndCountUniqueIds(fieldValue);
                        drawObjUniqueIds.addAll(nestedUuids);
                    } else if (fieldValue.isTextual()) {
                        // Try to parse as UUID
                        try {
                            String uniqueId = standardizeUuid(fieldValue.asText());
                            UUID.fromString(uniqueId); // Validate UUID format
                            drawObjUniqueIds.add(uniqueId);
                            System.out.println("  - Added UUID from field " + fieldName + ": " + uniqueId);
                        } catch (IllegalArgumentException e) {
                            System.out.println("  - Field " + fieldName + " is not a valid UUID: " + fieldValue.asText());
                        }
                    }
                }
            } else if (uniqueIdsNode.isTextual()) {
                // Handle case where we might have a single UUID as a string
                String uniqueId = standardizeUuid(uniqueIdsNode.asText());
                try {
                    UUID.fromString(uniqueId); // Validate UUID format
                    drawObjUniqueIds.add(uniqueId);
                    System.out.println("  - Added single UUID: " + uniqueId);
                } catch (IllegalArgumentException e) {
                    System.out.println("  - Not a valid UUID: " + uniqueId);
                }
            } else {
                System.out.println("WARNING - Node is neither an array nor an object");
                System.out.println("Node type: " + uniqueIdsNode.getNodeType());
                System.out.println("Node content: " + uniqueIdsNode.toString());
            }
            
            // Verify all UUIDs are valid
            int validUuidCount = 0;
            int invalidUuidCount = 0;
            
            for (String uuidStr : drawObjUniqueIds) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    validUuidCount++;
                } catch (IllegalArgumentException e) {
                    invalidUuidCount++;
                    System.out.println("  - WARNING: Invalid UUID format: " + uuidStr);
                }
            }
            
            System.out.println("\nUUID Validation Summary:");
            System.out.println("  - Total UUIDs: " + drawObjUniqueIds.size());
            System.out.println("  - Valid UUIDs: " + validUuidCount);
            System.out.println("  - Invalid UUIDs: " + invalidUuidCount);
            
        } else {
            System.out.println("WARNING - Node is null");
        }
        
        // Update global statistics
        totalDeserializedUUIDs += drawObjUniqueIds.size();
        totalDeserializedGroups++;
        
        System.out.println("==== SelectionGroupDeserializer: UUID Extraction Complete ====\n");
        
        return drawObjUniqueIds;
    }
    
    /**
     * Helper method to recursively search for arrays in the JSON structure
     * @param node The JSON node to search
     * @return The first array node found, or null if none
     */
    private JsonNode findFirstArrayNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        
        if (node.isArray()) {
            System.out.println("Found array node with " + node.size() + " elements");
            return node;
        }
        
        // Check all fields for arrays
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode fieldValue = node.get(fieldName);
            
            System.out.println("Checking field: " + fieldName + " (type: " + fieldValue.getNodeType() + ")");
            
            if (fieldValue.isArray()) {
                System.out.println("Found array in field: " + fieldName + " with " + fieldValue.size() + " elements");
                return fieldValue;
            }
            
            // Recursively check objects
            if (fieldValue.isObject()) {
                JsonNode result = findFirstArrayNode(fieldValue);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Helper method to debug the JSON structure
     * @param node The JSON node to debug
     * @param prefix Prefix for indentation
     */
    private void debugJsonStructure(JsonNode node, String prefix) {
        if (node == null) {
            System.out.println(prefix + "NULL");
            return;
        }
        
        if (node.isObject()) {
            System.out.println(prefix + "Object with " + node.size() + " fields:");
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = node.get(fieldName);
                System.out.println(prefix + "  Field: " + fieldName + " (type: " + fieldValue.getNodeType() + ")");
                debugJsonStructure(fieldValue, prefix + "    ");
            }
        } else if (node.isArray()) {
            System.out.println(prefix + "Array with " + node.size() + " elements:");
            int i = 0;
            for (JsonNode element : node) {
                System.out.println(prefix + "  Element[" + i + "] (type: " + element.getNodeType() + ")");
                debugJsonStructure(element, prefix + "    ");
                i++;
            }
        } else if (node.isValueNode()) {
            System.out.println(prefix + "Value: " + node.asText());
        } else {
            System.out.println(prefix + "Unknown node type: " + node.getNodeType());
        }
    }
    
    @Override
    public SelectionGroup deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        System.out.println("\n===== SelectionGroupDeserializer: Deserializing SelectionGroup =====");
        
        // Create an empty SelectionGroup
        SelectionGroup selectionGroup = new SelectionGroup();
        
        try {
            // Parse the JSON
            JsonNode rootNode = p.getCodec().readTree(p);
            System.out.println("Root node type: " + rootNode.getNodeType());
            
            // Dump the entire JSON structure for debugging
            System.out.println("JSON structure:");
            debugJsonStructure(rootNode, "  ");
            
            // Read from the drawObjUniqueIds array
            JsonNode uniqueIdsNode = rootNode.get("drawObjUniqueIds");
            
            if (uniqueIdsNode == null) {
                System.out.println("WARNING: 'drawObjUniqueIds' field not found in JSON");
                System.out.println("Attempting to find UUID array through alternative methods...");
                
                // Try to find any array in the JSON as a fallback
                if (rootNode.isArray()) {
                    System.out.println("Root node is an array, using it as fallback");
                    uniqueIdsNode = rootNode;
                } else if (rootNode.isObject()) {
                    // Check if there's only one field and it's an array
                    if (rootNode.size() == 1) {
                        Iterator<String> fields = rootNode.fieldNames();
                        if (fields.hasNext()) {
                            String firstField = fields.next();
                            JsonNode firstFieldValue = rootNode.get(firstField);
                            if (firstFieldValue.isArray()) {
                                System.out.println("Found single array field '" + firstField + "', using as fallback");
                                uniqueIdsNode = firstFieldValue;
                            }
                        }
                    }
                    
                    // If still not found, try to find any array field
                    if (uniqueIdsNode == null) {
                        System.out.println("Searching for any array field as fallback...");
                        Iterator<String> fields = rootNode.fieldNames();
                        while (fields.hasNext() && uniqueIdsNode == null) {
                            String fieldName = fields.next();
                            JsonNode fieldValue = rootNode.get(fieldName);
                            if (fieldValue.isArray()) {
                                System.out.println("Found array field '" + fieldName + "', using as fallback");
                                uniqueIdsNode = fieldValue;
                                break;
                            }
                        }
                    }
                    
                    // If still not found, try to find any array in the structure
                    if (uniqueIdsNode == null) {
                        uniqueIdsNode = findFirstArrayNode(rootNode);
                        if (uniqueIdsNode != null) {
                            System.out.println("Found an array node to use as fallback");
                        } else {
                            System.out.println("No array found in JSON structure");
                            
                            // Check if the root node might be a single UUID
                            if (rootNode.isValueNode()) {
                                String value = rootNode.asText();
                                System.out.println("Root node is a value: " + value);
                                
                                // If it looks like a UUID, add it
                                try {
                                    UUID.fromString(value);
                                    List<String> singleUuid = new ArrayList<>();
                                    singleUuid.add(value);
                                    selectionGroup.setDrawObjUniqueIds(singleUuid);
                                    System.out.println("Added single UUID from value node: " + value);
                                    
                                    // Print global statistics
                                    printGlobalStatistics(selectionGroup);
                                    return selectionGroup;
                                } catch (IllegalArgumentException e) {
                                    System.out.println("Value is not a valid UUID");
                                }
                            }
                            
                            // Create a default empty list
                            selectionGroup.setDrawObjUniqueIds(new ArrayList<>());
                            System.out.println("Using empty UUID list as last resort");
                            
                            // Print global statistics
                            printGlobalStatistics(selectionGroup);
                            return selectionGroup;
                        }
                    }
                }
            }
            
            // Extract UUIDs from the node
            List<String> drawObjUniqueIds = extractAndCountUniqueIds(uniqueIdsNode);
            
            // Store the references in the SelectionGroup for later resolution
            selectionGroup.setDrawObjUniqueIds(drawObjUniqueIds);
            System.out.println("Set " + drawObjUniqueIds.size() + " UUIDs in SelectionGroup");
            
        } catch (Exception e) {
            System.out.println("ERROR during deserialization: " + e.getMessage());
            e.printStackTrace();
            
            // Ensure we always have a valid list, even if empty
            selectionGroup.setDrawObjUniqueIds(new ArrayList<>());
        }
        
        // Print global statistics
        printGlobalStatistics(selectionGroup);
        
        return selectionGroup;
    } // end method deserialize
    
    /**
     * Helper method to print global statistics
     * @param selectionGroup The SelectionGroup being deserialized
     */
    private void printGlobalStatistics(SelectionGroup selectionGroup) {
        System.out.println("\n***** SelectionGroupDeserializer: Global Statistics *****");
        System.out.println("Total SelectionGroups deserialized: " + totalDeserializedGroups);
        System.out.println("Total UUIDs deserialized: " + totalDeserializedUUIDs);
        System.out.println("Average UUIDs per SelectionGroup: " + 
                          (totalDeserializedGroups > 0 ? (float)totalDeserializedUUIDs/totalDeserializedGroups : 0));
        System.out.println("Current SelectionGroup UUID count: " + selectionGroup.getDrawObjUniqueIds().size());
        System.out.println("===== SelectionGroupDeserializer: Deserialization Complete =====\n");
    }
}
