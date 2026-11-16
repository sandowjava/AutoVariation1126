# Ostwald MonoTri Controller Package

This package contains controller classes for managing the Ostwald MonoTri color system's interaction logic and state management.

## Key Components

### OstdotAssessPanelDraw (New in VERSION H)

A singleton service class that centralizes the display state calculation logic for OstdotPanels. This class is part of the OstdotPanel rendering redesign that aims to improve state management and rendering efficiency.

#### Display States
The class manages six distinct display states for OstdotPanels:
1. `OCCUPIED_UNSELECTED_IN_CURR_HUE`: Position has a color matching the frame's current hue (not selected)
2. `OCCUPIED_UNSELECTED_OTHER_HUE`: Position has a color from a different hue (normal theViewWindow, not selected)
3. `OCCUPIED_UNSELECTED_OTHER_HUE_SIMPLIFIED`: Position has a color from a different hue (simplified theViewWindow, not selected)
4. `OCCUPIED_SELECTED_IN_CURR_HUE`: Position has a color matching the frame's current hue (selected)
5. `OCCUPIED_SELECTED_OTHER_HUE`: Position has a color from a different hue (selected)
6. `UNOCCUPIED`: Position has no color assigned

#### Key Methods
- `assessDisplayState()`: Determines the current display state of an OstdotPanel
- `isInCurrentHue()`: Checks if a panel's color matches the frame's current hue

### MonoTriDrawController

Manages drawing and selection operations for the MonoTri display system. This class handles:
- Selection mode management
- Drawing operations coordination
- User interaction processing
- Selection state maintenance

## Design Principles

### Display State Pattern (VERSION H)
The new display state pattern introduces several key architectural improvements:

1. **Separation of Concerns**
   - State calculation logic centralized in OstdotAssessPanelDraw
   - Rendering logic remains in OstdotPanel
   - Clear boundaries between state and visualization

2. **Event Flow**
   - Color Recoloring Operations:
     * Template updates trigger frame notifications
     * Frames coordinate panel repaints
     * Panels query service for state updates
   
   - Hue Changes:
     * Frame updates trigger panel repaints
     * Panels get updated states from service

3. **Benefits**
   - Centralized state calculation
   - Improved maintainability
   - More efficient updates
   - Clear state transition paths
   - Preserved MVC architecture

## Implementation Strategy

The redesign is being implemented in phases:

1. **Phase 1: Core Components**
   - Implement OstdotDisplayState enum
   - Create OstdotAssessPanelDraw service

2. **Phase 2: Frame Updates**
   - Add simplified display toggle to MonoTriDumbFrame
   - Implement state management changes

3. **Phase 3: Panel Refactoring**
   - Update OstdotPanel to use new service
   - Remove direct state calculation

4. **Phase 4: Validation**
   - Comprehensive testing
   - Performance validation
   - Integration verification

## Usage Guidelines

1. **State Calculation**
   - Always use OstdotAssessPanelDraw for display state determination
   - Never implement state calculation logic in theViewWindow components
   - Maintain clear separation between state and rendering

2. **Event Handling**
   - Follow established event chain for updates
   - Use proper notification mechanisms
   - Maintain MVC pattern integrity

3. **Simplified Display**
   - Toggle only affects other-hue occupied positions
   - Maintain current hue and selection visibility
   - Use menu item for control
