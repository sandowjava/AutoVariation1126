package com.csurf.ostwald.domainmonotri.model;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Represents a step in an Ostwald "shadow-series" column, the "position" in the triangle.
 * This class defines the position of a color within the Ostwald color system's
 * triangular arrangement and 3D color-space.
 * OstHueColumnStep contains "redundant" OstPosition singleton (theOstPosition) for "abstract" position (no hueNum).
 * OstHueColumnStep instance is contained in ColorPtOstDot and GrayColorPtOstDot.
 * Each position is defined by:
 * - A column number (1-12)
 * - A step within that column
 * - The hue number (0-35) which is the third dimension of the 3D color-space
 * - Series information for white and black series, which are the 2D dimensions of the triangle
 * 
 * CALLBACK PATTERN: Each OstHueColumnStep instance maintains its own listener list for event propagation.
 * This ensures that listeners are tied to individual OstHueColumnStep instances, not to the shared
 * OstPosition singleton, avoiding the complexity of multiple listeners on shared singleton state.
 */
public class OstHueColumnStep {
    private final int theColumn;
    private final int theStep;
    private final int theHueNumCode;
    private final int theFillOrderNum;
    private final int theHueDegrees;

    /**
     * The draw order number for this step, determining its position in the visual rendering sequence.
     * Values range from 1 to 78, with:
     * - 1 being the pure hue (first to draw)
     * - Clear-black series using values from clrBdrawNums array
     * - Clear-white series using values from clrWdrawNums array
     * - Other columns following a systematic progression up through the triangle
     * <p>
     * Note: The total number of positions depends on the edgeSize of the triangle.
     */
    private final int theDrawOrderNum;
    private final int theSeriesW;
    private final int theSeriesB;

    private static final int MAX_COLUMN = 12;
    private static final int MIN_COLUMN = 1;
    private static final int MAX_HUE = 35;
    private static final int MIN_HUE = 0;
    private final OstPosition theOstPosition; // TODO 061025 creates redundancies but first step toward moving fields to OstPosition immutables
    
    // Callback pattern: Each OstHueColumnStep maintains its own listener list
    private final List<OstHueColumnStepEventListener> ostHueColumnStepListeners = new ArrayList<>();



    // added 101026 for use by class OstHueColumnStepHitWeight - NONSTANDARD constructor
    public OstHueColumnStep (int stdHueNumCode, int fillOrderNum) {
        // 082026 calls standard constructor with paramBundle created from fillOrderNum
        this(stdHueNumCode, ColorPtOstDot.createParamBundleFromFillOrderNum(fillOrderNum), fillOrderNum);
    } // end constructor



    /**
     * Creates a new OstHueColumnStep instance.
     *
     * @param stdHueNumCode    The standard hue number (0-35)
     * @param paramBundle  Parameters defining column and series information
     * @param fillOrderNum The fill order number for this position
     * @throws IllegalArgumentException if any parameters are invalid
     */
    public OstHueColumnStep(int stdHueNumCode, ColumnStepWhiteBlackParams paramBundle, int fillOrderNum) {
        // TODO 061025 creates redundancies but first step toward moving fields to OstPosition immutables
        this.theOstPosition = OstPosition.getInstance(fillOrderNum);
        this.theDrawOrderNum = paramBundle.getTheDrawOrderNum();
        // Validate hue number
        if (stdHueNumCode < MIN_HUE || stdHueNumCode > MAX_HUE) {
            throw new IllegalArgumentException(
                    String.format("Invalid hue number %d. Must be between %d and %d",
                            stdHueNumCode, MIN_HUE, MAX_HUE));
        }

        // Validate column number
        int column = paramBundle.getTheColumn();
        if (column < MIN_COLUMN || column > MAX_COLUMN) {
            throw new IllegalArgumentException(
                    String.format("Invalid column number %d. Must be between %d and %d",
                            column, MIN_COLUMN, MAX_COLUMN));
        }

        // Validate step number (must be positive and less than column size)
        int step = paramBundle.getTheStep();
        if (step < 0 || step > column) {
            throw new IllegalArgumentException(
                    String.format("Invalid step number %d for column %d", step, column));
        }

        // Validate fill order number
        if (fillOrderNum < 1 || fillOrderNum > 78) {
            throw new IllegalArgumentException(
                    String.format("Invalid fill order number %d. Must be between 1 and 78",
                            fillOrderNum));
        }

        // Validate consistency between params and OstPosition
        int whiteSeries = paramBundle.getTheWhiteSeries();
        int blackSeries = paramBundle.getTheBlackSeries();
        int drawOrderNum = paramBundle.getTheDrawOrderNum();

        // Validate column consistency
        if (column != theOstPosition.getColumnNum()) {
            throw new IllegalArgumentException(
                    String.format("Column mismatch for fillOrderNum %d: " +
                            "params specify column %d, but OstPosition derives column %d",
                            fillOrderNum, column, theOstPosition.getColumnNum()));
        }

        // Validate step consistency
        if (step != theOstPosition.getStepNum()) {
            throw new IllegalArgumentException(
                    String.format("Step mismatch for fillOrderNum %d: " +
                            "params specify step %d, but OstPosition derives step %d",
                            fillOrderNum, step, theOstPosition.getStepNum()));
        }

        // Validate white series consistency
        if (whiteSeries != theOstPosition.getWhiteSeriesNum()) {
            throw new IllegalArgumentException(
                    String.format("White series mismatch for fillOrderNum %d: " +
                            "params specify whiteSeries %d, but OstPosition derives whiteSeries %d",
                            fillOrderNum, whiteSeries, theOstPosition.getWhiteSeriesNum()));
        }

        // Validate black series consistency
        if (blackSeries != theOstPosition.getBlackSeriesNum()) {
            throw new IllegalArgumentException(
                    String.format("Black series mismatch for fillOrderNum %d: " +
                            "params specify blackSeries %d, but OstPosition derives blackSeries %d",
                            fillOrderNum, blackSeries, theOstPosition.getBlackSeriesNum()));
        }

        // Validate draw order consistency
        if (drawOrderNum != theOstPosition.getDrawOrderNum()) {
            throw new IllegalArgumentException(
                    String.format("Draw order mismatch for fillOrderNum %d: " +
                            "params specify drawOrderNum %d, but OstPosition derives drawOrderNum %d",
                            fillOrderNum, drawOrderNum, theOstPosition.getDrawOrderNum()));
        }

        this.theHueNumCode = stdHueNumCode;
        this.theColumn = column;
        this.theStep = step;
        this.theSeriesW = paramBundle.getTheWhiteSeries();
        this.theSeriesB = paramBundle.getTheBlackSeries();
        this.theFillOrderNum = fillOrderNum;
        HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(stdHueNumCode +1);
        theHueDegrees = HueSpacingStdChoices.getSingleton().getHueDegrIntForHueNumGUI(someHueNumGUI);
    } // end constructor

    /**
     * Creates a new OstHueColumnStep instance from simplified parameters.
     * This constructor is used to create scoped equivalents of OstHueColumnStep instances
     * for frame-specific event isolation in PaletteExplicit
     * // TODO 101726 -this constructor only used internally -this is probably bad "sub-optimal" design
     * This constructor only used by method createScopedEquivalentOHCS.
     * Input params for this constructor make its code overly-complicated (could use ColorPtOstDot input directly instead)
     *
     * @param someHueNumGUI The hue number in user-facing format (HueNumGUI)
     * @param someColumnNum The column number (1-12)
     * @param someStepNum The step number within the column
     * @throws IllegalArgumentException if any parameters are invalid
     */
    public OstHueColumnStep(HueNumGUI someHueNumGUI, int someColumnNum, int someStepNum) {
        // Convert HueNumGUI to internal hue code (0-35)
        int stdHueNumCode = someHueNumGUI.getInternalCodeHueNum();
        
        // Validate hue number
        if (stdHueNumCode < MIN_HUE || stdHueNumCode > MAX_HUE) {
            throw new IllegalArgumentException(
                    String.format("Invalid hue number %d. Must be between %d and %d",
                            stdHueNumCode, MIN_HUE, MAX_HUE));
        }
        
        // Validate column number
        if (someColumnNum < MIN_COLUMN || someColumnNum > MAX_COLUMN) {
            throw new IllegalArgumentException(
                    String.format("Invalid column number %d. Must be between %d and %d",
                            someColumnNum, MIN_COLUMN, MAX_COLUMN));
        }
        
        // Validate step number: must be >= 1 and <= columnNum
        if (someStepNum < 1 || someStepNum > someColumnNum) {
            throw new IllegalArgumentException(
                    String.format("Invalid step number %d for column %d. Step must be >= 1 and <= column number",
                            someStepNum, someColumnNum));
        }

        // ALTERNATIVE: this could be used as the only input param to this constructor
        ColorPtOstDot someColorPtOstDot
                = MonotriBook.getInstance().getColorByOHCS(someHueNumGUI, someColumnNum, someStepNum);
        int fillOrderNum = someColorPtOstDot.getFillOrderNum();
        
        this.theOstPosition = OstPosition.getInstance(fillOrderNum);
        this.theHueNumCode = stdHueNumCode;
        this.theColumn = someColumnNum;
        this.theStep = someStepNum;
        
        // Get parameters from OstPosition
        this.theFillOrderNum = fillOrderNum;
        this.theSeriesW = theOstPosition.getWhiteSeriesNum();
        this.theSeriesB = theOstPosition.getBlackSeriesNum();
        this.theDrawOrderNum = theOstPosition.getDrawOrderNum();
        
        HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(stdHueNumCode + 1);
        this.theHueDegrees = HueSpacingStdChoices.getSingleton().getHueDegrIntForHueNumGUI(hueNumGUI);
    } // end constructor

    /**
     * Creates a scoped equivalent OstHueColumnStep from this singleton instance.
     * 
     * This method is used by PaletteExplicit to create frame-scoped OstHueColumnStep instances
     * that are independent from the singleton. The scoped equivalent has the same
     * (hue, column, step) values but is a completely separate instance with its own
     * listener list, enabling frame-specific event isolation.
     * 
     * @return A new OstHueColumnStep instance with the same values as this singleton
     */
    public OstHueColumnStep createScopedEquivalentOHCS() {
        // Convert internal hue code to HueNumGUI (user-facing)
        HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(this.theHueNumCode + 1);
        
        // Create new scoped equivalent with same (hue, column, step) values
        return new OstHueColumnStep(
            hueNumGUI,
            this.theColumn,
            this.theStep
        );
    } // end method createScopedEquivalentOHCS



    // added 101026 - this method called many times in CENSUS methods
    public Color getAsSimpleJavaColor() {
        int codeHueNum = this.theHueNumCode;
        int fillOrderNum = this.theFillOrderNum;
        ColorPtOstDot someColorPtOstDot
                = MonotriBook.getInstance().getMonoTriPage(codeHueNum).getColorByFillOrder(fillOrderNum);
        return someColorPtOstDot.getAsColor();
    } // end getAsSimpleJavaColor()


    /**
     * Gets the standard hue number for this step.
     * Note: Returns internal hue number (0-35), not user-facing (1-36).
     * 
     * @return The standard hue number (0-35)
     */
    public int getStdCodeHueNum() {
        return theHueNumCode; // TODO 061125 this is good -this is right place for hueNum to be stored
    }


    /**
     * @return The column number (1-12)
     */
    public int getColumn() {
        // TODO 061025 creates redundancies but first step toward moving fields to OstPosition immutables
        if (theColumn != theOstPosition.getColumnNum()) {
            System.out.println("RS DEBUG: Error in OstHueColumnStep.getColumn() VS OstPosition.getColumn()");
        }
        return theColumn;
    }

    /**
     * @return The step number within the column
     */
    public int getStep() {
        // TODO 061025 creates redundancies but first step toward moving fields to OstPosition immutables
        if (theStep != theOstPosition.getStepNum()) {
            System.out.println("RS DEBUG: Error in OstHueColumnStep.getStep() VS OstPosition.getStep()");
        }
        return theStep;
    }

    /**
     * @return The fill order number
     */
    public int getFillOrderNum() {
        // TODO 061025 creates redundancies but first step toward moving fields to OstPosition immutables
        if (theFillOrderNum != theOstPosition.getPositionNum()) {
            System.out.println("RS DEBUG: Error in OstHueColumnStep.getFillOrderNum() VS OstPosition.getPositionNum()");
        }
        return theFillOrderNum;
    }

    /**
     * @return The white series value
     */
    public int getSeriesW() {
        // TODO 061025 creates redundancies but first step toward moving fields to OstPosition immutables
        if (theSeriesW != theOstPosition.getWhiteSeriesNum()) {
            System.out.println("RS DEBUG: Error in OstHueColumnStep.getSeriesW() VS OstPosition.getSeriesW()");
        }
        return theSeriesW;
    }

    /**
     * @return The black series value
     */
    public int getSeriesB() {
        // TODO 061025 creates redundancies but first step toward moving fields to OstPosition immutables
        if (theSeriesB != theOstPosition.getBlackSeriesNum()) {
            System.out.println("RS DEBUG: Error in OstHueColumnStep.getSeriesB() VS OstPosition.getSeriesB()");
        }
        return theSeriesB;
    }

    /**
     * Gets the draw order number for this step.
     * This number determines the sequence in which colors are drawn in the MonoTriDisplay,
     * ensuring a consistent and visually coherent rendering order from bottom to top
     * and left to right across the triangle.
     *
     * @return the draw order number (1-78)
     */
    public int getDrawOrderNum() {
        // TODO 061025 creates redundancies but first step toward moving fields to OstPosition immutables
        if (theDrawOrderNum != theOstPosition.getDrawOrderNum()) {
            System.out.println("RS DEBUG: Error in OstHueColumnStep.getDrawOrderNum() VS OstPosition.getDrawOrderNum()");
        }
        return theDrawOrderNum;
    }

    /**
     * Returns a string representation of this OstHueColumnStep in the format:
     * "{fillOrderNum}C{column}S{step}"
     *
     * @return The string representation
     */
    @Override
    public String toString() {
        return "#" + theFillOrderNum + "=C" + theColumn + ",S" + theStep + "=W" + theSeriesW + ",B" + theSeriesB;
    }

    public int getTheHueDegrees() {
        return theHueDegrees;
    } // end method getTheHueDegrees

    /**
     * Gets the OstPosition instance for this step.
     * Since OstPosition uses the singleton pattern, this can be used for identity comparison.
     *
     * @return The OstPosition singleton instance for this step's position
     */
    public OstPosition getOstPosition() {
        return theOstPosition;
    }

    /**
     * Compares this OstHueColumnStep with another object for equality.
     * Two OstHueColumnStep objects are considered equal if they have the same hue, column, step,
     * and position number.
     *
     * @param obj The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OstHueColumnStep other = (OstHueColumnStep) obj;
        return theHueNumCode == other.theHueNumCode &&
               theColumn == other.theColumn &&
               theStep == other.theStep &&
               theOstPosition.getPositionNum() == other.theOstPosition.getPositionNum();
    }

    /**
     * Generates a hash code for this OstHueColumnStep.
     * The hash code is based on the hue, column, step, and position number.
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + theHueNumCode;
        result = 31 * result + theColumn;
        result = 31 * result + theStep;
        result = 31 * result + theOstPosition.getPositionNum();
        return result;
    }

    // ========== CALLBACK PATTERN METHODS ==========
    
    /**
     * Adds a listener for OstHueColumnStep events.
     * Each OstHueColumnStep maintains its own listener list, ensuring that listeners
     * are tied to individual instances, not to the shared OstPosition singleton.
     * 
     * @param listener The listener to add (must implement OstHueColumnStepEventListener)
     */
    public void addOstHueColumnStepListener(OstHueColumnStepEventListener listener) {
        if (listener != null && !ostHueColumnStepListeners.contains(listener)) {
            ostHueColumnStepListeners.add(listener);
        }
    }
    
    /**
     * Removes a listener for OstHueColumnStep events.
     * 
     * @param listener The listener to remove
     */
    public void removeOstHueColumnStepListener(OstHueColumnStepEventListener listener) {
        ostHueColumnStepListeners.remove(listener);
    }
    
    /**
     * Checks if a listener is registered on this OstHueColumnStep.
     * 
     * @param listener The listener to check
     * @return true if the listener is registered, false otherwise
     */
    public boolean hasListener(OstHueColumnStepEventListener listener) {
        return ostHueColumnStepListeners.contains(listener);
    }
    
    /**
     * Gets the number of registered listeners.
     * 
     * @return The number of listeners
     */
    public int getListenerCount() {
        return ostHueColumnStepListeners.size();
    }
    
    /**
     * Fires a creation event to all registered listeners.
     */
    public void fireCreationEvent() {
        for (OstHueColumnStepEventListener listener : new ArrayList<>(ostHueColumnStepListeners)) {
            try {
                listener.onOstHueColumnStepEvent(
                        new OstHueColumnStepEventImpl(this, "CREATION")
                );
            } catch (Exception e) {
                // Continue propagating to other listeners even if one throws exception
            }
        }
    }
    
    /**
     * Fires a modification event to all registered listeners.
     * 
     * @param oldValue The old value
     * @param newValue The new value
     */
    public void fireModificationEvent(Object oldValue, Object newValue) {
        for (OstHueColumnStepEventListener listener : new ArrayList<>(ostHueColumnStepListeners)) {
            try {
                listener.onOstHueColumnStepEvent(
                        new OstHueColumnStepEventImpl(this, "MODIFICATION", oldValue, newValue)
                );
            } catch (Exception e) {
                // Continue propagating to other listeners even if one throws exception
            }
        }
    }
    
    /**
     * Fires a deletion event to all registered listeners.
     */
    public void fireDeletionEvent() {
        for (OstHueColumnStepEventListener listener : new ArrayList<>(ostHueColumnStepListeners)) {
            try {
                listener.onOstHueColumnStepEvent(
                        new OstHueColumnStepEventImpl(this, "DELETION")
                );
            } catch (Exception e) {
                // Continue propagating to other listeners even if one throws exception
            }
        }
    }
    
    // ========== EVENT LISTENER INTERFACE ==========
    
    /**
     * Interface for listening to OstHueColumnStep events.
     */
    public interface OstHueColumnStepEventListener {
        /**
         * Called when an OstHueColumnStep event occurs.
         * 
         * @param event The event that occurred
         */
        void onOstHueColumnStepEvent(OstHueColumnStepEvent event);
    }
    
    /**
     * Interface for OstHueColumnStep events.
     */
    public interface OstHueColumnStepEvent {
        /**
         * Gets the source OstHueColumnStep that fired the event.
         * 
         * @return The source OstHueColumnStep
         */
        OstHueColumnStep getSource();
        
        /**
         * Gets the type of event (CREATION, MODIFICATION, DELETION).
         * 
         * @return The event type
         */
        String getEventType();
        
        /**
         * Gets the old value (for modification events).
         * 
         * @return The old value, or null if not applicable
         */
        Object getOldValue();
        
        /**
         * Gets the new value (for modification events).
         * 
         * @return The new value, or null if not applicable
         */
        Object getNewValue();
        
        /**
         * Gets the timestamp when the event was created.
         * 
         * @return The timestamp in milliseconds
         */
        long getTimestamp();
    }
    
    /**
     * Internal implementation of OstHueColumnStepEvent.
     */
    private static class OstHueColumnStepEventImpl implements OstHueColumnStepEvent {
        private final OstHueColumnStep source;
        private final String eventType;
        private final Object oldValue;
        private final Object newValue;
        private final long timestamp;
        
        OstHueColumnStepEventImpl(OstHueColumnStep source, String eventType) {
            this.source = source;
            this.eventType = eventType;
            this.oldValue = null;
            this.newValue = null;
            this.timestamp = System.currentTimeMillis();
        }
        
        OstHueColumnStepEventImpl(OstHueColumnStep source, String eventType, Object oldValue, Object newValue) {
            this.source = source;
            this.eventType = eventType;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public OstHueColumnStep getSource() {
            return source;
        }
        
        @Override
        public String getEventType() {
            return eventType;
        }
        
        @Override
        public Object getOldValue() {
            return oldValue;
        }
        
        @Override
        public Object getNewValue() {
            return newValue;
        }
        
        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }

} // end class
