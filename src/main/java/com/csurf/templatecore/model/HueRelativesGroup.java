package com.csurf.templatecore.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a group of DrawObj instances whose hues fall within a specified range
 * around a central hue. This class is used to identify and manage DrawObjs that have
 * related hues for color manipulation operations.
 */
public class HueRelativesGroup {
    private final int centralHue;
    private final int counterCWiseLimit;
    private final int clockWiseLimit;
    private final List<DrawObj> drawObjs;
    private final List<Integer> hueRange;
    
    /**
     * Creates a new HueRelativeGroup with the specified hue range parameters.
     *
     * @param centralHue The central hue number (code-facing, 0-35)
     * @param counterCWiseLimit How many hue steps to include in the counter-clockwise direction
     * @param clockWiseLimit How many hue steps to include in the clockwise direction
     */
    public HueRelativesGroup(int centralHue, int counterCWiseLimit, int clockWiseLimit) {
        this.centralHue = centralHue;
        this.counterCWiseLimit = counterCWiseLimit;
        this.clockWiseLimit = clockWiseLimit;
        this.drawObjs = new ArrayList<>();
        this.hueRange = calculateHueRange();
    }
    
    /**
     * Calculates the range of hues included in this group based on the central hue
     * and the counter-clockwise and clockwise limits.
     *
     * @return A list of integers representing the hues in the range
     */
    private List<Integer> calculateHueRange() {
        List<Integer> range = new ArrayList<>();
        final int TOTAL_HUES = 36;
        
        // Add the central hue first
        range.add(centralHue);
        
        // Add counter-clockwise hues
        for (int i = 1; i <= counterCWiseLimit; i++) {
            int hue = (centralHue - i + TOTAL_HUES) % TOTAL_HUES;
            range.add(hue);
        }
        
        // Add clockwise hues
        for (int i = 1; i <= clockWiseLimit; i++) {
            int hue = (centralHue + i) % TOTAL_HUES;
            range.add(hue);
        }
        
        // Sort the range for easier use
        Collections.sort(range);
        return range;
    }
    
    /**
     * Adds a DrawObj to this group.
     *
     * @param drawObj The DrawObj to add
     */
    public void addDrawObj(DrawObj drawObj) {
        if (drawObj != null && !drawObjs.contains(drawObj)) {
            drawObjs.add(drawObj);
        }
    }
    
    /**
     * Gets all DrawObjs in this group.
     *
     * @return An unmodifiable list of DrawObjs
     */
    public List<DrawObj> getDrawObjs() {
        return Collections.unmodifiableList(drawObjs);
    }
    
    /**
     * Gets the central hue of this group.
     *
     * @return The central hue number
     */
    public int getCentralHue() {
        return centralHue;
    }
    
    /**
     * Gets the counter-clockwise limit of this group.
     *
     * @return The counter-clockwise limit
     */
    public int getCounterCWiseLimit() {
        return counterCWiseLimit;
    }
    
    /**
     * Gets the clockwise limit of this group.
     *
     * @return The clockwise limit
     */
    public int getClockWiseLimit() {
        return clockWiseLimit;
    }
    
    /**
     * Gets the range of hues included in this group.
     *
     * @return An unmodifiable list of hue numbers
     */
    public List<Integer> getHueRange() {
        return Collections.unmodifiableList(hueRange);
    }
    
    /**
     * Gets the number of DrawObjs in this group.
     *
     * @return The number of DrawObjs
     */
    public int getSize() {
        return drawObjs.size();
    }
    
    /**
     * Checks if this group is empty.
     *
     * @return true if the group contains no DrawObjs, false otherwise
     */
    public boolean isEmpty() {
        return drawObjs.isEmpty();
    }
    
    /**
     * Checks if a hue is within the range of this group.
     *
     * @param hue The hue to check
     * @return true if the hue is within the range, false otherwise
     */
    public boolean isHueInRange(int hue) {
        return hueRange.contains(hue);
    }
} // end class HueRelativeGroup
