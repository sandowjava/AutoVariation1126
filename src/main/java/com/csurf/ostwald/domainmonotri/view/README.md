# MonoTri View Components

This package contains the theViewWindow components for the MonoTri color selection system, which enables users to select and manipulate Ostwald colors through a visual interface.

## Key Components

### OstdotPanel
A specialized panel component for displaying individual Ostwald color dots. Each OstdotPanel represents a single ColorPtOstDot instance and handles its own rendering and interaction state.

#### Features
- Circular dot display using antialiased graphics
- Support for both RGB and CMYK color modes
- Interactive hover effects
- Consistent 30x30 pixel size
- Background color support for transparency
- Efficient graphics handling with proper resource management

### Component Interactions
The theViewWindow components follow this hierarchy:
```
MonoTriDumbFrame
    └── MonoTriDrawPanel
        └── MonoTriDrawController
            └── OstdotPanel (multiple instances)
```

## Manual SmartPic Coloring Flow

The "manual SmartPic coloring" use case involves several components working together to enable users to recolor individual shapes in the SmartPic display. Here's how the components interact:

### Color Selection Flow
1. **User Selection in SmartPic Display**
   - User selects a PipeOneColor shape (DrawObj or PipeUnionDrawGroup)
   - SmartPic's RosterOfPipeShapes tracks the selected shape
   - SmartPicDisplay notifies SmartPicSubunitMediator of selection

2. **Color Selection in MonoTri Display**
   - User clicks an OstdotPanel in MonoTriDumbFrame
   - OstdotPanel triggers event in MonoTriDrawController
   - Controller notifies registered ColorSelectionListeners
   - MonoTriDumbFrame receives notification and forwards to mediator

3. **Color Application**
   - SmartPicSubunitMediator receives new color
   - Mediator calls SmartPic.handleColorChange()
   - SmartPic updates selected shape's color
   - SmartPic notifies state listeners for UI update

### Component Responsibilities

#### OstdotPanel
- Displays individual ColorPtOstDot instances
- Handles mouse interaction and hover effects
- Reports color selection events to controller

#### MonoTriDrawController
- Manages collection of OstdotPanel instances
- Handles panel layout and positioning
- Routes color selection events to listeners

#### MonoTriDumbFrame
- Provides container for MonoTri display
- Manages color selection listener registration
- Bridges between MonoTri display and mediator

#### SmartPicSubunitMediator
- Coordinates between MonoTri and SmartPic components
- Handles color selection events
- Updates SmartPic with new colors

#### SmartPic
- Maintains RosterOfPipeShapes
- Tracks currently selected shape
- Applies color changes to selected shapes
- Notifies listeners of state changes

### Color Flow Pattern
```
[OstdotPanel] -> [MonoTriDrawController] -> [MonoTriDumbFrame]
     |                                            |
     v                                           v
[ColorPtOstDot] -> [SmartPicSubunitMediator] -> [SmartPic]
                                                  |
                                                  v
                                          [PipeOneColor]
```

## Implementation Notes

- Each MonoTriDrawController maintains its own set of OstdotPanel instances
- Panels are created once and reused for efficiency
- Multiple MonoTri displays can exist independently
- Color selection events use the Observer pattern
- SmartPic maintains single source of truth for shape colors
