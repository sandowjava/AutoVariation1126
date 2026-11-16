# Color Point Serialization Design

## ColorPtOstDot Serialization Strategy

### Overview
The serialization of `ColorPtOstDot` instances uses a hybrid approach:
- **Serialization**: Stores complete `OstHueColumnStep` information
- **Deserialization**: Uses `MonoTriBook.getMonoTriPage(hueNum).getColorByFillOrder(fillOrderNum)`

### Serialized Format
```json
{
    "hueNum": 12,          // Standard hue number (0-35)
    "column": 3,           // Column in MonotriPage (1-12)
    "step": 2,             // Step within column
    "fillOrderNum": 45,    // Fill order number (1-78)
    "seriesW": 1,         // White series value
    "seriesB": 2          // Black series value
}
```

### Design Decisions

1. **Full Information Storage**
   - Store all `OstHueColumnStep` integer fields even though only `hueNum` and `fillOrderNum` are used for deserialization
   - Provides data for verification and debugging
   - Maintains implementation independence
   - Supports future compatibility

2. **Simplified Deserialization**
   - Uses `getColorByFillOrder()` to reconstruct instances
   - Ensures proper instance creation through MonoTriBook
   - Maintains encapsulation of MonoTriPage internals

### Potential Issues

#### Implementation Dependency Risk
The current deserialization approach has a potential weakness:

1. **fillOrderNum Dependency**
   - `fillOrderNum` is implementation-dependent
   - Could change if MonoTriPage implementation changes:
     - Number of columns could vary
     - Total number of ColorPtOstDots could change
     - Fill order numbering scheme could be modified

2. **Mitigation Strategies**
   - Store complete OstHueColumnStep data for potential future use
   - Could add version information to serialized format
   - Could implement fallback deserialization using stored column/step data

### Future Considerations

1. **Version Support**
   - Consider adding version field to serialized format
   - Could support multiple deserialization strategies based on version

2. **Validation**
   - Could add cross-validation using stored column/step data
   - Would help catch any fillOrderNum inconsistencies

3. **Alternative Approaches**
   - Could switch to column/step-based reconstruction if fillOrderNum proves problematic
   - Could implement both strategies with runtime validation

### Common Serialization Challenges in Evolving Systems

### 1. Class Evolution Issues
- Field additions and removals
- Structural changes (e.g., flat to nested objects)
- Renamed fields
- Type changes
- New validation requirements

### 2. Backward Compatibility
- Need to support loading older saved files
- Multiple versions of serialized formats
- Migration strategies for legacy data
- Fallback deserialization paths

### 3. Typical Solutions
- Version-aware deserializers
- Fallback strategies for missing fields
- Nested structure handling
- Field mapping for renamed properties
- Default values for new required fields

### VERSION 2M Changes

#### Current Implementation
The OstHueColumnStep class has undergone significant structural changes:
```json
{
    "theHueDegrees": 268,
    "stdHueNum": 20,
    "ostPosition": {
        "positionNum": 16,
        "columnNum": 6,
        "whiteSeriesNum": 5,
        "blackSeriesNum": 0,
        "drawOrderNum": 21,
        "stepNum": 6
    },
    "drawOrderNum": 21,
    "fillOrderNum": 16
}
```

#### Backward Compatibility
The deserializer supports both:
1. New format with nested `ostPosition` object
2. Legacy format with flat structure:
```json
{
    "hueNum": 20,
    "column": 6,
    "step": 6,
    "fillOrderNum": 16,
    "seriesW": 5,
    "seriesB": 0
}
```

### Best Practices for Future Changes

1. **Version Tracking**
   - Consider adding explicit version fields
   - Document format changes in this README
   - Tag major structural changes with version identifiers

2. **Backward Compatibility**
   - Always maintain fallback paths for older formats
   - Document deprecated fields and formats
   - Implement graceful degradation for missing data

3. **Testing Requirements**
   - Test with sample files from all supported versions
   - Verify both old and new format handling
   - Include edge cases and migration scenarios

4. **Documentation**
   - Update this README with all format changes
   - Document version-specific behaviors
   - Include sample JSON for each supported format

### Testing Recommendations

1. **Serialization Tests**
   - Verify all OstHueColumnStep fields are correctly stored
   - Check edge cases (min/max values for each field)

2. **Deserialization Tests**
   - Verify correct ColorPtOstDot reconstruction
   - Test with various MonoTriPage configurations
   - Validate color properties match original

3. **Round-Trip Tests**
   - Verify serialize → deserialize preserves color identity
   - Check all color properties are maintained
