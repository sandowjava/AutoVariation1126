package com.csurf.ostwald.domainmonotri.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * OstPosition class represents the position of each dot in the 78-dot monotri, regardless of hueNum.
 * Inside each instance of OstHueColumnStep class, the abstract position is represented by an OstPosition instance.
 * Any two ColorPtOstDot instances in MonotriBook can be compared for position-equality by their OstPosition instances.
 * The INSTANCES array contains all 78 OstPosition instances in position order. These 78 instances are used
 * as the OstPosition instances for each OstHueColumnStep instance, with each instance functioning as a sort of singleton.
 * Design plan is to remove redundant fields in OstHueColumnStep instances and represent them by OstPosition fields only.
 * @author Firstname Lastname
 */
public class OstPosition {
    private int positionNum; // 1-78 representing unique position
    private static final OstPosition[] INSTANCES = new OstPosition[79];
    private int theColumn;
    private int theStep;
    private int theWhiteSeries;
    private int theBlackSeries;
    private int theDrawOrderNum;
    // see method "createOstPositionFromFillOrderNum" for use of these arrays (hardcoded calculation for "drawOrderNum")
    private static final int[] clrWSeriesDrawNums = {0, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66};
    private static final int[] clrBSeriesDrawNums = {0, 2, 4, 7, 11, 16, 22, 29, 37, 46, 56};


    // private constructor
    private OstPosition(int positionNum) {
        this.positionNum = positionNum;

        OstPosition dummyOstPosition = createOstPositionFromFillOrderNum(positionNum);
        this.theColumn = dummyOstPosition.getColumnNum();
        this.theStep = dummyOstPosition.getStepNum();
        this.theWhiteSeries = dummyOstPosition.getWhiteSeriesNum();
        this.theBlackSeries = dummyOstPosition.getBlackSeriesNum();
        this.theDrawOrderNum = dummyOstPosition.getDrawOrderNum();
    } // end constructor


    // constructor used by createOstPositionFromFillOrderNum
    private OstPosition(int whatColumn, int whatStep, int whatWhiteSeries, int whatBlackSeries, int whatDrawOrderNum) {
        theColumn = whatColumn;
        theStep = whatStep;
        theWhiteSeries = whatWhiteSeries;
        theBlackSeries = whatBlackSeries;
        theDrawOrderNum = whatDrawOrderNum;
    } // end constructor




    // TODO 061025: this method implicitly hard-coded for EDGESIZE ==12 [method copied from ColorPtOstDot]
    private OstPosition createOstPositionFromFillOrderNum(int fillOrderNum) {
        OstPosition newOstPosition = null;

        if(fillOrderNum == 1) { // pureHue
            newOstPosition = new OstPosition(1, 1, 0, 0, 1);}

        if(fillOrderNum >= 2 && fillOrderNum <= 11) {
            newOstPosition = new OstPosition(fillOrderNum, 1, 0, fillOrderNum-1, clrBSeriesDrawNums[fillOrderNum-1]);
        } // end clear-B series

        if(fillOrderNum >= 12 && fillOrderNum <= 21) { // TODO 051925 FORMULA for each COLUMN is simple: columnNum plus stepNum (NOTE: except for column 1 & clearW series & grayscale)
            newOstPosition = // TODO 051925 drawOrderNum for clearW is "complex/confusing calc" ->SET INDEX derived from whatColumn = fillOrderNum-10, but "minus 1 further" NOTE: != colNum
                    new OstPosition(fillOrderNum-10, fillOrderNum-10, fillOrderNum-11, 0, clrWSeriesDrawNums[fillOrderNum-11]);
        } // end clear-W series

        if(fillOrderNum == 22) { // TODO 051525 must check these error-prone calcs done "per column" "per fillOrderNum"
            newOstPosition =
                    new OstPosition(3, fillOrderNum-20, fillOrderNum-21, fillOrderNum-21, fillOrderNum-17);
        } // end column 3

        if(fillOrderNum >= 23 && fillOrderNum <= 24) { // TODO 051525 pattern of incrementing that goes up by one each time (PREVIOUS increment +1)
            newOstPosition =
                    new OstPosition(4, fillOrderNum-21, fillOrderNum-22, 25-fillOrderNum, fillOrderNum-15);
        } // end column 4

        if(fillOrderNum >= 25 && fillOrderNum <= 27) { // column 5
            newOstPosition =
                    new OstPosition(5, fillOrderNum-23, fillOrderNum-24, 28-fillOrderNum, fillOrderNum-13);
        } // end column 5

        if(fillOrderNum >= 28 && fillOrderNum <= 31) { // column 6
            newOstPosition =
                    new OstPosition(6, fillOrderNum-26, fillOrderNum-27, 32-fillOrderNum, fillOrderNum-11);
        } // end column 6

        if(fillOrderNum >= 32 && fillOrderNum <= 36) { // column 7
            newOstPosition =
                    new OstPosition(7, fillOrderNum-30, fillOrderNum-31, 37-fillOrderNum, fillOrderNum-9);
        } // end column 7

        if(fillOrderNum >= 37 && fillOrderNum <= 42) { // column 8
            newOstPosition =
                    new OstPosition(8, fillOrderNum-35, fillOrderNum-36, 43-fillOrderNum, fillOrderNum-7);
        } // end column 8

        if(fillOrderNum >= 43 && fillOrderNum <= 49) { // column 9
            newOstPosition =
                    new OstPosition(9, fillOrderNum-41, fillOrderNum-42, 50-fillOrderNum, fillOrderNum-5);
        } // end column 9

        if(fillOrderNum >= 50 && fillOrderNum <= 57) { // column 10
            newOstPosition =
                    new OstPosition(10, fillOrderNum-48, fillOrderNum-49, 58-fillOrderNum, fillOrderNum-3);
        } // end column 10

        if(fillOrderNum >= 58 && fillOrderNum <= 66) { // column 11
            newOstPosition =
                    new OstPosition(11, fillOrderNum-56, fillOrderNum-57, 67-fillOrderNum, fillOrderNum-1);
        } // end column 11

        if(fillOrderNum >= 67 && fillOrderNum <= 78) { // this column departs from previous increment pattern because gray-scale includes pureB & pureW
            newOstPosition =
                    new OstPosition(12, fillOrderNum-66, fillOrderNum-67, 78-fillOrderNum, fillOrderNum);
        } // end column 12

        // 051925 for debugging only
        if (fillOrderNum ==22) {
            int kerflooey = 666;}

        return newOstPosition;
    } // end method createOstPositionFromFillOrderNum



    static {
        // Create all 78 instances at class load time
        for (int i = 1; i < 79; i++) {
            INSTANCES[i] = new OstPosition(i);
        }
    } // end static block



    public static OstPosition getInstance(int fillOrderNum) {
        if (fillOrderNum < 1 || fillOrderNum >= 79) {
            throw new IllegalArgumentException("Position must be between 1 and 78");
        }
        return INSTANCES[fillOrderNum];
    } // end method getInstance



    /**
     * Converts (columnNum, stepNum) coordinates to (Wseries, Bseries) coordinates.
     * Both coordinate systems describe the same position in the Ostwald triangle.
     *
     * @param columnNum The column number (1-12)
     * @param stepNum The step number within the column (1 to columnNum, not 0-based)
     * @return An array of two integers: [Wseries, Bseries]
     *         Wseries = stepNum - 1
     *         Bseries = (columnNum - 1) - (stepNum - 1)
     */
    public static int[] convertColumnStepToWseriesBseries(int columnNum, int stepNum) {
        int wseries = stepNum - 1;
        int bseries = (columnNum - 1) - (stepNum - 1);
        return new int[]{wseries, bseries};
    } // end method convertColumnStepToWseriesBseries



    /**
     * Gets all 78 OstHueColumnStep instances in position order.
     * @return Array of all instances
     */
    public static OstPosition[] getAllInstances() {
        return INSTANCES.clone(); // Return copy to prevent modification
    } // end method getAllInstances



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OstPosition)) return false;
        OstPosition that = (OstPosition) o;
        return positionNum == that.positionNum;
    } // end method equals



    @Override
    public int hashCode() {
        return positionNum;
    } // end method hashCode



    @Override
    @JsonValue
    public String toString() {
        return String.format("{\"positionNum\":%d,\"column\":%d,\"step\":%d,\"whiteSeries\":%d,\"blackSeries\":%d,\"drawOrderNum\":%d}",
                getPositionNum(), getColumnNum(), getStepNum(), getWhiteSeriesNum(), getBlackSeriesNum(), getDrawOrderNum());
    } // end method toString

    /**
     * Creates an OstPosition from its JSON representation.
     * This method is used by Jackson for deserialization.
     * @param json The JSON string representation of the OstPosition
     * @return The deserialized OstPosition instance
     */
    @JsonCreator
    public static OstPosition fromString(String json) {
        try {
            // Extract the position number from the JSON string
            int start = json.indexOf("\"positionNum\":") + 14;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            int positionNum = Integer.parseInt(json.substring(start, end).trim());
            
            // Get the instance using the position number
            return getInstance(positionNum);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid OstPosition JSON: " + json, e);
        }
    }




    public int getPositionNum() {
        return this.positionNum;
    }

    public int getBlackSeriesNum() {
        return this.theBlackSeries;
    }

    public int getWhiteSeriesNum() {
        return this.theWhiteSeries;
    }

    public int getStepNum() {
        return this.theStep;
    }

    public int getColumnNum() {
        return this.theColumn;
    }

    public int getDrawOrderNum() {
        return this.theDrawOrderNum;
    }
} // end class OstPosition
