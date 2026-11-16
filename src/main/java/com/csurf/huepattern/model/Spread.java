package com.csurf.huepattern.model;

import com.csurf.huepattern.util.HueCircleStepCalc;

public class Spread {
    private final int theGroupNum;
    private final int theAnchorHueNum;
    private final int theStepNumCCW;
    private final int theStepNumCW;
    private final int theNumStepsTraversingSpread;
    private final int[] theSpreadHueNumsArray;
    private final int theHueStepsToCCWLimit;
    private final int theHueStepsToCWLimit;




    public Spread(int someGroupNum, int anchorStepNum, int actualStepNumCCW, int actualStepNumCW) {
        // setTheAnchGroupNum(anchGroupNum);
        theGroupNum = someGroupNum; // 030424 changed to make the field immutable (see "final" in field declaration)
        theAnchorHueNum = anchorStepNum; // 030424 changed to make the field immutable (see "final" in field declaration)
        theStepNumCCW =actualStepNumCCW; // 030424 changed to make the field immutable (see "final" in field declaration)
        theStepNumCW =actualStepNumCW; // 030424 changed to make the field immutable (see "final" in field declaration)

        theNumStepsTraversingSpread = calcNumStepsTraversingSpread(); // 030424 changed to make the field immutable (see "final" in field declaration)
        // setTheNumStepsInSpread(spreadWidth);

        // 030724 added
        theSpreadHueNumsArray = createTheSpreadStepsArray(theNumStepsTraversingSpread);
        // 110725 added
        theHueStepsToCCWLimit = calcHueStepsToSpreadLimit(-1);
        theHueStepsToCWLimit = calcHueStepsToSpreadLimit(1);
    } // end constructor



    // 103025 note that numStepsTraversingSpread is NOT= numHues, since you start on one of the "limitHues" and it's not counted
    public int calcNumStepsTraversingSpread(){  // 031324 changed method name [INCORRECT for numHues, which needs "+1" ]
        int numStepsTraversingSpread = 0; // TODO 102825 examine rationale for numStepsTraversingSpread NOT= numHues
        numStepsTraversingSpread = HueCircleStepCalc.calcNumStepsGoingCW(this.theStepNumCCW, this.theStepNumCW); // 031324 took out INCORRECT nees "+1"
        return numStepsTraversingSpread;
    } // end method calcNumStepsInSpread


    // 031324 added for numHues, which needs "+1"
    public int calcNumHuesInSpread(){
        return calcNumStepsTraversingSpread() +1;
    } // end method calcNumHuesInSpread



    private int[] createTheSpreadStepsArray(int numStepsTraverseSpread) {
        int numHuesInSpreadWLims = numStepsTraverseSpread +1;
        int[] hueNumsArray = new int[numHuesInSpreadWLims]; // use in FOR loop
        int startStep = this.getTheStepNumCCW();
        int finishStep = this.getTheStepNumCW();

        int result = startStep;
        for(int i = 0; i <numHuesInSpreadWLims; i++) { // while(result != finishStep)
            hueNumsArray[i] = result;
            result +=1;
            if(result ==37) {result =1;} // do modulus by simple arithmetic for "crossing over 36 line"
            if(i == numHuesInSpreadWLims-1) {} //030924 EXTRANEOUS CODE: boolean checkResultsEquality = Integer.compare(finishStep, result);}
        } // end FOR loop

        return hueNumsArray;
    } // end method createTheSpreadStepsArray



    //
    public int[] getTheSpreadStepsArray() {
        return theSpreadHueNumsArray;
    } // end method getTheSpreadStepsArray



    // 110725 method not complete but not necessary - see method "calcNewHueBySmarts" in PaletteGenerator
    public int getCodeHueNumByProportion(double someProportionLocation) {
        int anchHueNum = this.getAnchorHueNum();
        int ccwLimitHueNum = this.getCCWLimitHueNum();
        int cwLimitHueNum = this.getCWLimitHueNum();
        int returnHueNum = -1;

        if(someProportionLocation<-1 || someProportionLocation>1) {
            // TODO 110725 add error message
        } // end if

        if(someProportionLocation==0) {
            returnHueNum = anchHueNum;
        } // end if

        if(someProportionLocation==1) {
            returnHueNum = cwLimitHueNum;
        } // end if

        if(someProportionLocation== -1) {
            returnHueNum = ccwLimitHueNum;
        } // end if

        // 110725 method not complete here - see method "calcNewHueBySmarts" in PaletteGenerator
        if(someProportionLocation>0 && someProportionLocation<1) {
            returnHueNum = cwLimitHueNum;
        } // end if

        // 110725 method not complete here - see method "calcNewHueBySmarts" in PaletteGenerator
        if(someProportionLocation<0 && someProportionLocation>-1) {
            returnHueNum = ccwLimitHueNum;
        } // end if

        return returnHueNum;
    } // end method getCodeHueNumByProportionLocation



    // 110725 expectation "should be simplest": from 10 to 12 is 2 steps CW, from 12 to 10 is 2 steps CCW
    private int calcHueStepsToSpreadLimit(int simplePositiveOrNegative) {
        int anchHueNum = this.getAnchorHueNum();
        int ccwLimitHueNum = this.getCCWLimitHueNum();
        int cwLimitHueNum = this.getCWLimitHueNum();
        int numStepsBetween = 666;

        if(simplePositiveOrNegative<0){
            numStepsBetween = HueCircleStepCalc.calcNumStepsGoingCCW(anchHueNum, ccwLimitHueNum);
        }
        if(simplePositiveOrNegative>0){
            numStepsBetween = HueCircleStepCalc.calcNumStepsGoingCW(anchHueNum, cwLimitHueNum);
        }
        return numStepsBetween; // always a positive result
    } // end method calcHueStepsToSpreadLimit




    // TODO 110725 deprecate/delete this method - duplicates method "getCWLimitHueNum"
    private int getTheStepNumCW() {
        return theStepNumCW;
    } // end method getTheStepNumCW


    // TODO 110725 deprecate/delete this method - duplicates method "getCCWLimitHueNum"
    private int getTheStepNumCCW() {
        return theStepNumCCW;
    } // end method getTheStepNumCCW


    // Public accessors for display purposes
    public int getAnchorHueNum() {
        return theAnchorHueNum;
    }

    public int getCCWLimitHueNum() {
        return theStepNumCCW;
    }

    public int getCWLimitHueNum() {
        return theStepNumCW;
    }

    public int getAnchGroupNum() {
        return theGroupNum;
    }

    public int getStepsAnchorToCWLimit() {
        return this.theHueStepsToCWLimit;
    }

    public int getStepsAnchorToCCWLimit() {
        return this.theHueStepsToCCWLimit;
    }
} // end class Spread
