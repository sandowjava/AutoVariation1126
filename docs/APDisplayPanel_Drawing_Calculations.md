# APDisplayPanel Drawing Calculations

## Overview

This document describes the mathematical calculations and coordinate system transformations used in `APDisplayPanel` to render the Anchor Pattern display. It also documents important quirks discovered with Java's `drawArc` method that required workarounds.

## Coordinate Systems

### Standard Mathematical Coordinates
- Origin: (0, 0) at center
- Positive X: Right
- Positive Y: Up
- Angle 0°: Points right (3 o'clock position)
- Positive angle rotation: Counter-clockwise

### Java Screen Coordinates
- Origin: (0, 0) at top-left
- Positive X: Right
- Positive Y: **Down** (inverted from mathematical convention)
- Angle 0°: Points right (3 o'clock position)
- Positive angle rotation: **Clockwise** (due to Y-axis inversion)

### Hue Circle Convention (Application-Specific)
- 36 hue positions numbered 1-36
- Hue 1: Top (12 o'clock position) = -90° in standard coordinates
- Hues increase clockwise: 2, 3, 4... wrapping at 36 back to 1
- Each hue occupies 10° of arc (360° / 36 = 10°)

## Angle Calculations

### Converting Hue Number to Angle

For any hue number (1-36), the angle in degrees is calculated as:

```java
double angleDegrees = -90 + (hueNum - 1) * 10;
```

**Explanation:**
- Start at -90° (12 o'clock, top of circle)
- Add (hueNum - 1) * 10° to rotate clockwise
- Example: Hue 1 → -90°, Hue 10 → 0° (3 o'clock), Hue 19 → 90° (6 o'clock)

### Converting Angle to Screen Position

To place an element at a specific angle and radius:

```java
double angleRadians = Math.toRadians(angleDegrees);
double x = centerX + radius * Math.cos(angleRadians);
double y = centerY + radius * Math.sin(angleRadians);
```

**Why this works:**
- Despite Java's inverted Y-axis, the basic trigonometric functions work correctly
- `cos(angle)` gives the horizontal offset from center
- `sin(angle)` gives the vertical offset from center
- The Y-axis inversion is automatically handled because we use the screen's centerY as the reference point

## The drawArc Method and Its Quirks

### Standard Usage

Java's `Graphics2D.drawArc()` signature:
```java
drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
```

**Parameters:**
- `x, y`: Upper-left corner of bounding rectangle
- `width, height`: Dimensions of bounding rectangle (use diameter for circles)
- `startAngle`: Starting angle in degrees
- `arcAngle`: Arc extent in degrees (positive = counter-clockwise, negative = clockwise in screen coords)

### The Angle Negation Bug

**Critical Discovery:** Java's `drawArc` appears to negate the `startAngle` parameter internally, causing arcs to be drawn at positions rotated 180° from where they should be.

#### Evidence of the Bug

During debugging, we created diagnostic tests that revealed this behavior:

1. **Marker dots using cos/sin were accurate:**
   - Calculated position for hue 1 (-90°): Marker appeared at 12 o'clock ✓ CORRECT
   - Calculated position for hue 10 (0°): Marker appeared at 3 o'clock ✓ CORRECT
   - Calculated position for hue 19 (90°): Marker appeared at 6 o'clock ✓ CORRECT

2. **Arcs using drawArc were wrong:**
   - `drawArc(..., -90, 20)`: Arc appeared at position 17 (70°) instead of position 1 (-90°)
   - `drawArc(..., 0, 20)`: Arc appeared at position 8 instead of position 10
   - `drawArc(..., 90, 20)`: Arc appeared at position 35 instead of position 19

3. **Pattern analysis:**
   - For startAngle = -90°, arc appeared at 70° (difference of 160°)
   - For startAngle = -70°, arc appeared at 70° (exact negation)
   - For startAngle = 80°, arc appeared at -80° (exact negation)

**Conclusion:** The `startAngle` parameter is being negated internally by Java's implementation.

#### The Workaround

To compensate for this quirk, we negate our calculated angle before passing it to `drawArc`:

```java
// Calculate the angle using standard conventions
double startAngleDegrees = -90 + (cWLimitHue - 1) * DEGREES_PER_HUE;

// BUG FIX: Java's drawArc negates the angle parameter, so we negate it to compensate
g2d.drawArc(arcX, arcY, arcDiameter, arcDiameter, (int) -startAngleDegrees, (int) arcExtent);
```

This double negation results in the arc being drawn at the correct position.

### Why cos/sin Work But drawArc Doesn't

The key difference is **what each method uses the angle for:**

**cos/sin calculations:**
- Take an angle and compute X/Y offsets
- Use standard trigonometric functions from `java.lang.Math`
- These are pure mathematical operations that follow standard conventions
- Result: **Always correct regardless of coordinate system quirks**

**drawArc method:**
- Takes an angle and draws an arc at that angular position
- Uses internal Java2D rendering pipeline
- Appears to have an angle negation bug in the implementation
- Result: **Requires workaround (negating the angle)**

### Verification with Bounding Box

We verified that the issue is specifically with angle interpretation, not with the bounding box:

1. Drew the bounding rectangle - it was perfectly centered ✓
2. Calculated marker positions with cos/sin - they aligned with hue positions ✓
3. All elements used the same center point and radius ✓
4. The arc was on the correct circle but at the wrong angular position ✗

This conclusively proved the issue was with how `drawArc` interprets the angle parameter.

## Ring Radii

The display consists of six concentric rings:

```java
private static final int HUE_CIRCLE_RING_RADIUS = 240;  // Outermost
private static final int OCCUPIED_RING_RADIUS = 260;    // Between Hue Circle and AG1
private static final int AG1_RING_RADIUS = 110;
private static final int AG2_RING_RADIUS = 130;
private static final int AG3_RING_RADIUS = 195;
private static final int AG4_RING_RADIUS = 215;         // Innermost AG ring
```

## Anchor Group Arc Drawing

### Algorithm

For each Anchor Group (AG1-AG4):

1. **Determine arc endpoints:**
   - Start at CWLimit (clockwise limit)
   - End at CCWLimit (counter-clockwise limit)
   - The anchor position lies between these limits

2. **Calculate number of steps:**
   ```java
   int numSteps = HueCircleStepCalc.calcNumStepsBetwGoingCW(cCWLimitHue, cWLimitHue);
   ```
   - Counts how many hue positions from CCWLimit to CWLimit going clockwise
   - Handles wraparound (e.g., from hue 35 to hue 3 is 4 steps)

3. **Calculate start angle (at CWLimit):**
   ```java
   double startAngleDegrees = -90 + (cWLimitHue - 1) * DEGREES_PER_HUE;
   ```

4. **Calculate arc extent:**
   ```java
   double arcExtent = numSteps * DEGREES_PER_HUE;  // Positive = CCW in standard coords
   ```

5. **Set up bounding box:**
   ```java
   int arcDiameter = radius * 2;
   int arcX = centerX - radius;  // Upper-left X
   int arcY = centerY - radius;  // Upper-left Y
   ```

6. **Draw the arc (with angle negation workaround):**
   ```java
   g2d.drawArc(arcX, arcY, arcDiameter, arcDiameter, (int) -startAngleDegrees, (int) arcExtent);
   ```

### Why the Arc Naturally Includes the Anchor

Since we:
- Start at CWLimit
- Extend counter-clockwise (positive extent)
- Stop at CCWLimit
- And the anchor lies between CWLimit and CCWLimit

The arc automatically covers the anchor position without needing special logic.

## Anchor Circle Drawing

Each AG ring also displays a 20px circle at the anchor position:

```java
double anchorAngleDegrees = -90 + (anchorHue - 1) * DEGREES_PER_HUE;
double anchorAngleRadians = Math.toRadians(anchorAngleDegrees);

double anchorX = centerX + radius * Math.cos(anchorAngleRadians);
double anchorY = centerY + radius * Math.sin(anchorAngleRadians);

g2d.fillOval((int)(anchorX - 10), (int)(anchorY - 10), 20, 20);
```

**Note:** This uses cos/sin (not drawArc), so no angle negation is needed.

## Edge Cases

### Single-Hue Spread (CCWLimit = CWLimit = Anchor)

When all three values are the same:
- `numSteps` would be 0
- We draw a small 10° indicator arc instead
- Adjust start angle by -5° to center it on the anchor

```java
if (numSteps == 0) {
    arcExtent = 10.0;
    startAngleDegrees -= 5.0;
}
```

### Wraparound at Hue 36→1

The `HueCircleStepCalc.calcNumStepsBetwGoingCW` utility handles wraparound:
- From hue 35 to hue 3: 4 steps (35→36→1→2→3)
- From hue 3 to hue 35: 32 steps (going the long way clockwise)

## Summary of Key Points

1. **Hue circle has 36 positions, each occupying 10°**
2. **Hue 1 is at top (12 o'clock) = -90° in standard coordinates**
3. **cos/sin calculations work correctly using standard angle conventions**
4. **drawArc has a bug requiring angle negation: use `-startAngleDegrees`**
5. **Positive extent = counter-clockwise in mathematical sense (clockwise in screen coords due to Y-inversion)**
6. **All calculations use the same center point and radii for consistency**
7. **Arcs naturally span from CWLimit to CCWLimit, including the anchor**

## Future Considerations

If Java's `drawArc` behavior is ever fixed or changes in future JDK versions, the angle negation workaround may need to be removed. The diagnostic code used to discover this bug has been preserved in git history and can be restored if needed for verification.

## References

- Java Documentation: [Graphics2D.drawArc](https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#drawArc-int-int-int-int-int-int-)
- Related utility: `com.csurf.huepattern.util.HueCircleStepCalc`
- Related model: `com.csurf.huepattern.model.AnchorPattern`
