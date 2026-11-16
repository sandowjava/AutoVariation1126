package com.csurf.ostwald.domainmonotri.model;

/**
 * Provides standard hue spacing choices for color operations.
 * Uses singleton pattern to ensure only one instance exists.
 */
public class HueSpacingStdChoices {
    private static HueSpacingStdChoices theHueSpacingSingleton;

    /**
     * Standard hue array for 36 hues optimized for even spacing in CMYK.
     * Values are specified as 360-degree integers compatible with HSL and HSV colorspaces.
     * First entry (ZERO HUE) is just a filler, not used (= hue 36).
     * Keeps backwards compatibility w range 1-36. Loops should NOT use hueNumCode for indexing.
     * Class does NOT use hue range 0-35, uses range 1-36. Should NOT use hueNumCode for indexing.
     */
    private static final int[] standardHueArrayFor36v5CMYK = {
            60, 56, 50, 46, 42, 36, 31,     // 0-6
            26, 19, 9, 355, 343,            // 7-11
            330, 317, 304, 298, 293,        // 12-16
            288, 283, 275, 268, 255,        // 17-21
            241, 218, 204, 196, 189,        // 22-26
            179, 170, 159, 139, 120, 90,    // 27-32
            76, 67, 63,                     // 33-35
            60                              // 36-36
    };

    /**
     * The current standard hue array used for operations.
     */
    public final int[] NUM36_CURRENT_SET = standardHueArrayFor36v5CMYK;

    /**
     * The minimum hue degree value in NUM36_CURRENT_SET.
     * This represents one edge of the gap in the hue circle.
     * The gap (where 360° wraps to 0°) is located between maxHueDegrees and minHueDegrees.
     *
     * For example, if minHueDegrees = 9 and maxHueDegrees = 355,
     * the gap is between 355° and 9° (going through 360°/0°).
     *
     * This value is computed dynamically from NUM36_CURRENT_SET to ensure
     * correctness if the hue array is modified in the future.
     */
    private final int minHueDegrees;

    /**
     * The maximum hue degree value in NUM36_CURRENT_SET.
     * This represents the other edge of the gap in the hue circle.
     * See minHueDegrees for explanation of the gap location.
     *
     * This value is computed dynamically from NUM36_CURRENT_SET to ensure
     * correctness if the hue array is modified in the future.
     */
    private final int maxHueDegrees;

    /**
     * The size of the gap in the hue circle, measured in degrees.
     * The gap is the region between maxHueDegrees and minHueDegrees (going through 360°/0°).
     *
     * Calculated as: minHueDegrees + (360 - maxHueDegrees)
     *
     * For example, if minHueDegrees = 9 and maxHueDegrees = 355:
     * gapSize = 9 + (360 - 355) = 9 + 5 = 14 degrees
     *
     * This value is useful for understanding the hue space structure and for debugging.
     */
    private final int gapSize;

    /**
     * Private constructor to enforce singleton pattern.
     * Initializes the dynamic min/max hue degree values and gap size
     * by analyzing NUM36_CURRENT_SET.
     */
    private HueSpacingStdChoices() {
        this.minHueDegrees = findMinHueInArray();
        this.maxHueDegrees = findMaxHueInArray();
        this.gapSize = minHueDegrees + (360 - maxHueDegrees);
    }



    /**
     * Gets the singleton instance of HueSpacingStdChoices.
     * Uses lazy initialization - singleton isn't created until first access.
     * @return the singleton instance
     */
    public static HueSpacingStdChoices getSingleton() {
        if (theHueSpacingSingleton == null) {
            theHueSpacingSingleton = new HueSpacingStdChoices();
        }
        return theHueSpacingSingleton;
    }



    /**
     * Gets current standard hue array used for finding closest-hue. Do NOT use hueNumCode to index array.
     * The array contains 37 hue values (0-36) specified in 360-degree format. Range of 1-36 for indexing.
     * The first entry (index 0) is a filler value equal to the last hue (60 degrees).
     * Keeps backwards compatibility w range 1-36. Loops should NOT use hueNumCode for indexing.
     * @return Array of standard hue values
     */
    public int[] getGUIStd36HueArray() { // TODO 062625 replaced this with new method that uses HueNumGUI param for index
        return NUM36_CURRENT_SET; // TODO 062625 need method "getHueDegrIntForHueNumGUI" that ENFORCES using HueNumGUI param for index
    } // end method getGUIStd36HueArray



    /*
     * TODO 062625 need to replace all calls to method "getGUIStd36HueArray" with calls to this method that uses HueNumGUI param for index
     * This method ENFORCES using HueNumGUI param for index
     */
    public int getHueDegrIntForHueNumGUI(HueNumGUI someHueNumGUI) {
        return NUM36_CURRENT_SET[someHueNumGUI.getUserFacingHueNum()];
    } // end method getHueDegrIntForHueNumGUI



    /**
     * Gets the minimum hue degree value in NUM36_CURRENT_SET.
     * This represents one edge of the gap in the hue circle.
     * The gap is located between maxHueDegrees and minHueDegrees (going through 360°/0°).
     *
     * @return The minimum hue degree value (e.g., 9 for the current array)
     */
    public int getMinHueDegrees() {
        return minHueDegrees;
    } // end method getMinHueDegrees



    /**
     * Gets the maximum hue degree value in NUM36_CURRENT_SET.
     * This represents the other edge of the gap in the hue circle.
     * The gap is located between maxHueDegrees and minHueDegrees (going through 360°/0°).
     *
     * @return The maximum hue degree value (e.g., 355 for the current array)
     */
    public int getMaxHueDegrees() {
        return maxHueDegrees;
    } // end method getMaxHueDegrees



    /**
     * Gets the size of the gap in the hue circle, measured in degrees.
     * The gap is the region between maxHueDegrees and minHueDegrees (going through 360°/0°).
     *
     * @return The gap size in degrees (e.g., 14 for the current array where min=9, max=355)
     */
    public int getGapSize() {
        return gapSize;
    } // end method getGapSize



    /**
     * Finds the minimum hue degree value in NUM36_CURRENT_SET.
     * Iterates through indices 1-36 (skipping the dummy cell at index 0).
     *
     * @return The minimum hue degree value found in the array
     */
    private int findMinHueInArray() {
        int min = Integer.MAX_VALUE;
        for (int i = 1; i <= 36; i++) {
            if (NUM36_CURRENT_SET[i] < min) {
                min = NUM36_CURRENT_SET[i];
            }
        }
        return min;
    } // end method findMinHueInArray



    /**
     * Finds the maximum hue degree value in NUM36_CURRENT_SET.
     * Iterates through indices 1-36 (skipping the dummy cell at index 0).
     *
     * @return The maximum hue degree value found in the array
     */
    private int findMaxHueInArray() {
        int max = Integer.MIN_VALUE;
        for (int i = 1; i <= 36; i++) {
            if (NUM36_CURRENT_SET[i] > max) {
                max = NUM36_CURRENT_SET[i];
            }
        }
        return max;
    } // end method findMaxHueInArray



    // 091926 added
    public HueNumGUI calcTrueComplemHueNumGUI(HueNumGUI someHueNumGUI) {
        int hueDegrStart = getHueDegrIntForHueNumGUI(someHueNumGUI);

        int hueDegrComplem = calcComplementHueZeroto360Degree(hueDegrStart);
        HueNumGUI complemHueNumGUI = findClosestHueNumGUI(hueDegrComplem);

        return complemHueNumGUI;
    } // end method getTrueComplemHueNumGUI


    /**
     * Calculates the true complementary hue angle for a given hue angle.
     * A complementary hue is 180 degrees away on the color wheel.
     *
     * The calculation is simple: complementary = (input + 180) % 360
     * This works because the hue circle is continuous and wraps at 360°.
     *
     * Examples:
     *   Input 60° → Output 240° (60 + 180 = 240)
     *   Input 200° → Output 20° ((200 + 180) % 360 = 20)
     *   Input 0° → Output 180°
     *   Input 180° → Output 0° (360 % 360 = 0)
     *
     * @param someIntZeroto360degree The input hue angle in the range 0-360 degrees
     * @return The complementary hue angle in the range 0-360 degrees
     */
    public int calcComplementHueZeroto360Degree(int someIntZeroto360degree) {
        return (someIntZeroto360degree + 180) % 360;
    } // end method calcComplementHueZeroto360Degree



    /**
     * Finds the closest hue in NUM36_CURRENT_SET to a given hue angle.
     * Returns the result as a HueNumGUI instance.
     *
     * This method handles the circular nature of the hue space, including the gap
     * between maxHueDegrees and minHueDegrees (where 360° wraps to 0°).
     *
     * The algorithm:
     * 1. Iterate through all hues in NUM36_CURRENT_SET (indices 1-36)
     * 2. For each hue, calculate the circular distance to the input
     * 3. Track the hue with the minimum circular distance
     * 4. Return the closest hue as a HueNumGUI instance
     *
     * Circular distance calculation:
     * The hue circle is continuous, so 360° = 0°. To find the true circular distance,
     * we consider three possible interpretations:
     *   - Direct distance: |hue - input|
     *   - Wrapped distance 1: |hue - (input + 360)| (handles input near 0°)
     *   - Wrapped distance 2: |hue - (input - 360)| (handles input near 360°)
     * We take the minimum of these three values.
     *
     * This approach automatically handles edge cases where the input is close to
     * the gap between maxHueDegrees and minHueDegrees.
     *
     * @param someIntZeroto360degree The input hue angle in the range 0-360 degrees
     * @return A HueNumGUI instance representing the closest hue in NUM36_CURRENT_SET
     */
    public HueNumGUI findClosestHueNumGUI(int someIntZeroto360degree) {
        int closestIndex = 1;  // Start with index 1 (first real hue, not dummy cell at index 0)
        int minDistance = Integer.MAX_VALUE;

        // Iterate through all hues in the array (indices 1-36)
        for (int i = 1; i <= 36; i++) {
            int hueAtIndex = NUM36_CURRENT_SET[i];

            // Calculate the circular distance from this hue to the input
            int circularDistance = calculateCircularDistance(hueAtIndex, someIntZeroto360degree);

            // Track the hue with minimum distance
            if (circularDistance < minDistance) {
                minDistance = circularDistance;
                closestIndex = i;
            }
        }

        // Convert array index to HueNumGUI (array index 1-36 corresponds to user-facing hueNum 1-36)
        return HueNumGUI.getHueNumGUIInstance(closestIndex);
    } // end method findClosestHueNumGUI



    /**
     * Calculates the circular distance between two hue angles on the color wheel.
     *
     * The hue circle is continuous and wraps at 360°. This method accounts for
     * the circular nature by considering three possible distance interpretations:
     *   1. Direct distance: |hue - target|
     *   2. Wrapped distance 1: |hue - (target + 360)| (for target near 0°)
     *   3. Wrapped distance 2: |hue - (target - 360)| (for target near 360°)
     *
     * The minimum of these three values is the true circular distance.
     *
     * Examples (assuming hue = 355°):
     *   target = 1°:
     *     Direct: |355 - 1| = 354°
     *     Wrapped1: |355 - (1 + 360)| = |355 - 361| = 6° ← minimum (correct!)
     *     Wrapped2: |355 - (1 - 360)| = |355 - (-359)| = 714°
     *     Result: 6°
     *
     *   target = 358°:
     *     Direct: |355 - 358| = 3° ← minimum (correct!)
     *     Wrapped1: |355 - (358 + 360)| = |355 - 718| = 363°
     *     Wrapped2: |355 - (358 - 360)| = |355 - (-2)| = 357°
     *     Result: 3°
     *
     * @param hue The hue degree value from NUM36_CURRENT_SET
     * @param target The target hue angle to compare against
     * @return The circular distance in degrees
     */
    private int calculateCircularDistance(int hue, int target) {
        // Calculate all three possible distance interpretations
        int directDistance = Math.abs(hue - target);
        int wrappedDistance1 = Math.abs(hue - (target + 360));
        int wrappedDistance2 = Math.abs(hue - (target - 360));

        // Return the minimum (the true circular distance)
        return Math.min(directDistance, Math.min(wrappedDistance1, wrappedDistance2));
    } // end method calculateCircularDistance



} // end class
