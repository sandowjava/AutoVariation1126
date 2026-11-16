# HMVC Message Types and Communication Flow

## Message Types

### SVG Path Subunit Messages
- `CONVERT_FILE`: Convert an SVG file to DrawObj array
  - Payload: `File` object
  - Response: `DrawObj[]` or `Exception`
- `GET_CURRENT_OBJECTS`: Get currently loaded DrawObj array
  - Payload: none
  - Response: `DrawObj[]`

### Path Analysis Subunit Messages
- `ANALYZE_PATHS`: Analyze current DrawObj array
  - Payload: `DrawObj[]`
  - Response: `PathAnalysis` object
- `GET_ANALYSIS`: Get current analysis results
  - Payload: none
  - Response: `PathAnalysis` object

## Communication Flow

1. **Top-Down Communication**
   - Application Mediator → Subunit Mediator: Uses `sendMessage(String targetSubunit, Message message)`
   - Example: `appMediator.sendMessage("svg", new Message("CONVERT_FILE", file, null))`

2. **Subunit-to-Subunit Communication**
   - Must go through Application Mediator
   - Source subunit gets data via message
   - Source subunit sends data to target subunit via Application Mediator

3. **UI Updates**
   - Each subunit manages its own UI components
   - Subunits can notify others of changes through messages

## Adding New Subunits

1. Create new package under `com.csurf`
2. Implement `SubunitMediator` interface
3. Register with `ApplicationMediator` in `Main`
4. Define message types for communication
