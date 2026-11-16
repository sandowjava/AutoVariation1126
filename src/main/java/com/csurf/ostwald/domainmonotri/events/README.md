# Ostwald Color System Event Architecture

## Overview
The event system manages color selection and interaction events across the Ostwald color system, implementing a robust observer pattern that separates UI interactions from business logic.

## Core Components

### Events
1. **OstdotEvent**
   - Represents low-level color dot interactions
   - Types: HOVER_ENTER, HOVER_EXIT, SELECTED
   - Carries: source panel, color, event type
   - Used by: OstdotPanel → OstdotListener chain

2. **ColorSelectionEvent**
   - Represents high-level color selection decisions
   - Carries: source component, selected ColorPtOstDot
   - Used by: MonoTriDumbFrame → IColorSelectionListener chain

### Listeners
1. **OstdotListener**
   - Handles low-level dot interactions
   - Method: ostdotEventOccurred(OstdotEvent)
   - Primary implementer: MonoTriDrawController

2. **IColorSelectionListener**
   - Handles high-level color selections
   - Method: colorSelected(ColorSelectionEvent)
   - Primary implementer: MonoTriDisplayController

## Event Flow

### UI Level (Bottom)
1. OstdotPanel
   - Generates mouse events
   - Creates OstdotEvents
   - Notifies registered OstdotListeners

### Controller Level (Middle)
1. MonoTriDrawController
   - Listens to OstdotEvents
   - Manages selection state
   - Generates ColorSelectionEvents
   - Notifies IColorSelectionListeners

### Application Level (Top)
1. MonoTriDisplayController
   - Listens to ColorSelectionEvents
   - Integrates with broader application logic
   - Updates UI state based on selections

## Key Design Features

### 1. Event Type Separation
- Low-level UI events (OstdotEvent) kept separate from high-level selection events (ColorSelectionEvent)
- Enables different handling strategies at different architectural levels

### 2. Singleton Color Management
- Events carry ColorPtOstDot instances which are singletons
- Enables efficient reference comparison in event handling

### 3. Visual State Management
- OstdotPanel maintains multiple visual states:
  * Selected (blue border)
  * Hovered (blue border)
  * In Palette (green border)
  * Normal (black border)

### 4. Controller Hierarchy
- MonoTriDrawController: Low-level UI interaction management
- MonoTriDisplayController: High-level application logic integration

## Usage Example
```java
// UI Level: Generate event
OstdotEvent event = new OstdotEvent(panel, color, OstdotEvent.OstdotEventType.SELECTED);
notifyListeners(event);

// Controller Level: Handle and propagate
public void ostdotEventOccurred(OstdotEvent event) {
    if (event.getType() == OstdotEvent.OstdotEventType.SELECTED) {
        ColorSelectionEvent selEvent = new ColorSelectionEvent(this, event.getColor());
        notifyColorSelectionListeners(selEvent);
    }
}

// Application Level: Process selection
public void colorSelected(ColorSelectionEvent event) {
    ColorPtOstDot color = event.getSelectedColor();
    processColorSelection(color);
}
```

## Best Practices
1. Always use appropriate event type for the architectural layer
2. Maintain clear separation between UI events and selection events
3. Leverage singleton nature of ColorPtOstDot for efficient comparisons
4. Update visual state through proper controller methods
