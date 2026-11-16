# MonoTri Display System

The MonoTri display system is responsible for rendering and managing the Ostwald color system's monochromatic triangles. This document describes the key components and process flow.

## Key Components

1. `MonoTriDumbFrame`
   - Main container frame for the MonoTri display
   - Manages hue selection and navigation
   - Uses HueSpacingStdChoices for accurate hue degree values

2. `MonoTriDrawController`
   - Handles the drawing logic and color positioning
   - Uses fillOrderNum from OstHueColumnStep for correct color ordering
   - Maintains proper triangular layout with columns and steps

3. `MonoTriPage`
   - Represents a single hue page in the Ostwald system
   - Provides colors via getColorByFillOrder(int fillOrder)
   - Each color is represented by ColorPtOstDot

4. `HueSpacingStdChoices`
   - Singleton class providing standard hue spacing values
   - Contains the correct degree values for each hue number
   - Used by MonoTriDumbFrame for accurate hue display

## Drawing Process

1. Color Ordering
   - Colors are drawn in sequence based on fillOrderNum (1-78)
   - Each ColorPtOstDot contains an OstHueColumnStep with:
     - column: position from left to right
     - step: position from bottom to top
     - fillOrderNum: determines draw order

2. Layout Calculation
   - Uses halfPanelHeight for vertical centering
   - Column spacing: 25 pixels
   - Square size: 20x20 pixels
   - Each column starts 12.5 pixels lower than the previous

3. Color Positioning
   - Colors are positioned by column and step
   - Bottom Y position = halfPanelHeight + (12.5 * column)
   - Y position = bottomY - ((step-1) * (SQUARE_SIZE + SPACING))
   - X position = (column-1) * COLUMN_SPACING

## Usage Notes

1. Hue Navigation
   - Use CCW/CW buttons to change hues
   - Hue field shows both number (1-36) and correct degree value
   - Degree values come from HueSpacingStdChoices.NUM36_CURRENT_SET

2. Color Selection
   - Click to select a color
   - Double-click for detailed color information
   - Hover for tooltip showing fill order, column, and step

3. Background Options
   - Menu provides different gray levels for background
   - Gray level is displayed at bottom of frame
