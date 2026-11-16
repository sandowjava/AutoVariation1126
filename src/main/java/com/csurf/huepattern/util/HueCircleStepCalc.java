package com.csurf.huepattern.util;

import java.util.logging.Logger;


public class HueCircleStepCalc {
    public final static int NUM_CIRCLE_STEPS = 36; // 103025 made final for general access
    private static final Logger logger = Logger.getLogger("myLogger");


    // constructor 022724
    public HueCircleStepCalc() {
    } // end constructor



    // 110725 Note interpretation of method name "should be simplest": from 10 to 12 is 2 steps CW, from 12 to 10 is 2 steps CCW
    // 022724 the arithmetic would be reversed if convention of param order, i.e., "CW- and CCW- LIMIT designations" were not in effect
    // 110725 FIXED: Corrected logic to properly calculate directional distance
    public static int calcNumStepsGoingCW(int stepNumForStart, int stepNumForEnd) {
        int stepsBetween = 0;

        // Calculate distance going clockwise (positive direction)
        // If start <= end: simple subtraction
        // If start > end: wrap around the circle
        if (stepNumForStart <= stepNumForEnd) {
            stepsBetween = stepNumForEnd - stepNumForStart;
        } else {
            stepsBetween = (stepNumForEnd + NUM_CIRCLE_STEPS) - stepNumForStart;
        }

        return stepsBetween; // always a positive result in range [0, 35]
    } // end method calcNumStepsGoingCW



    // 110725 Note interpretation of method name "should be simplest": from 10 to 12 is 2 steps CW, from 12 to 10 is 2 steps CCW
    // 030924 uses convention of param order w CCW limit first [method used in "calcEffectiveDistBetwA1andA2" in OHHDimsAnalyst cases past H19]
    // 110725 FIXED: Corrected logic to properly calculate directional distance
    public static int calcNumStepsGoingCCW(int stepNumForStart, int stepNumForEnd) {
        int stepsBetween = 0;

        // Calculate distance going counter-clockwise (negative direction)
        // If start >= end: simple subtraction
        // If start < end: wrap around the circle
        if (stepNumForStart >= stepNumForEnd) {
            stepsBetween = stepNumForStart - stepNumForEnd;
        } else {
            stepsBetween = (stepNumForStart + NUM_CIRCLE_STEPS) - stepNumForEnd;
        }

        return stepsBetween; // always a positive result in range [0, 35]
    } // end method calcNumStepsGoingCCW



    // 103025 added for general access to final constant
    public static int getNUM_STEPS_HUE_CIRCLE() {
        return NUM_CIRCLE_STEPS;
    } // end method getNUM_STEPS_HUE_CIRCLE


    public int someMethod() {
        return 29;
    } // end method xx



    public static int goStepsCWorCCW(int startStep, int numStepsPositvOrNegtiv) {
        int resultStep = 0;

        resultStep = (startStep + numStepsPositvOrNegtiv) % NUM_CIRCLE_STEPS;  // BUG POTENTIAL: modulus arithmetic can yield zero
        // BUG POTENTIAL: this arithmetic can yield zero, e.g., if startStep= 2, and numStepsPositvOrNegtiv= -2, yields ZERO instead of 36
        if (resultStep < 1) { resultStep += NUM_CIRCLE_STEPS;} // need this only for CASE where yields ZERO instead of 36, OK if >36

        return resultStep;
    } // end method goStepsCWorCCW


    // TODO 103025 order of params did NOT follow convention - fixed this so that CCW param comes first, then CW param
    public static int[] calcBothLimits(int anchor_step, int distCCW, int distCW) {
        // creates an int[] "limsCCWandCW" from two ints, calculated from the given limits
        int[] limsCCWandCW = new int[2];
        if (distCCW <= 0){
            // raise ValueError ("limit_ccw argument should be POSITIVE")
        }

        int stopCWstep = HueCircleStepCalc.goStepsCWorCCW(anchor_step, distCW); // 2nd HueCircleStepCalc method param can be negative ->"subtraction"
        int stopCCWstep = HueCircleStepCalc.goStepsCWorCCW(anchor_step, -1 * distCCW);  // -1 produces "subtraction"
        limsCCWandCW[0] = stopCCWstep;
        limsCCWandCW[1] = stopCWstep;
        return limsCCWandCW;

    } // end method calcBothLimits


} // end class HueCircleStepCalc
