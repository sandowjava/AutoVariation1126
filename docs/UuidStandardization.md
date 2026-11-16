# UUID Standardization in SelectionGroup Serialization

## Problem

When serializing and deserializing `SelectionGroup` objects, UUID strings were being stored and retrieved in inconsistent formats, causing reference resolution to fail. Specifically:

1. During serialization, UUIDs were stored using `UUID.toString()`, which includes hyphens and may use uppercase letters
2. During deserialization, these UUID strings were extracted as-is from the JSON
3. When resolving references in `Template.resolveSelectionGroup`, the lookup in `drawObjMap` failed because the keys in the map and the UUID strings being looked up had different formats

## Solution

We implemented a consistent UUID standardization approach across all components involved in the serialization/deserialization process:

1. Added a `standardizeUuid(String uuid)` utility method to:
   - `Template` class
   - `SelectionGroupSerializer` class
   - `SelectionGroupDeserializer` class

2. The standardization method:
   - Removes all hyphens from UUID strings
   - Converts all characters to lowercase
   - Ensures consistent format for all UUID comparisons

3. Updated key methods to use the standardized UUID format:
   - `Template.createDrawObjUuidMap`: Standardizes UUID strings before using them as map keys
   - `Template.resolveSelectionGroup`: Standardizes UUID strings before lookup in the map
   - `SelectionGroupSerializer.extractAndCountUniqueIds`: Standardizes UUID strings during serialization
   - `SelectionGroupDeserializer.extractAndCountUniqueIds`: Standardizes UUID strings during deserialization

## Implementation Details

The `standardizeUuid` method is implemented as follows:

```java
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
```

## Testing

The UUID standardization fix was tested using a dedicated test class (`UuidStandardizationTest`) that verifies:

1. All implementations of `standardizeUuid` produce consistent results
2. Various UUID formats (with/without hyphens, upper/lowercase) are correctly standardized
3. The standardized format is always lowercase with no hyphens

## Benefits

This fix ensures that:

1. UUID strings are consistently formatted throughout the serialization/deserialization process
2. Reference resolution works correctly regardless of the original UUID format
3. `SelectionGroup` objects can be properly loaded after template deserialization
4. The application maintains reference integrity across serialization boundaries

## Future Considerations

While this fix addresses the immediate issue with UUID format inconsistency, there are some additional considerations for future improvements:

1. Consider making the `standardizeUuid` method public and static in a utility class to avoid code duplication
2. Add unit tests that verify the end-to-end serialization/deserialization process with actual templates
3. Consider adding validation to ensure UUIDs are properly formatted before standardization
4. Monitor performance impact of UUID standardization on large templates with many DrawObj instances
