package com.csurf.templatecore.model;

/*
 * Class purpose is to simple data structure to hold two different types of shift parameters, hueShift and colorDragShift
 * hueShift is for a hue change with position unchanged
 * colorDragShift is for a position change with hue unchanged
 * colorDragShift always requires two parameters, shiftBSeries and shiftWSeries
 */
public class ShiftSpecify {
    private int shiftHue;
    private int shiftBSeries;
    private int shiftWSeries;


    // Default constructor // TODO 073125 INVESTIGATE: Cascade added this constructor and some methods that may not be needed
    public ShiftSpecify() {
        this.shiftHue = 0;
        this.shiftWSeries = 0;
        this.shiftBSeries = 0;
    } // end default constructor

    // Constructor for hueShift
    public ShiftSpecify(int shiftHueAmt) {
        this.shiftHue = shiftHueAmt;
        this.shiftWSeries = 0;
        this.shiftBSeries = 0;
    } // end constructor



    // Constructor for colorDragShift = position shift, no hue shift // 080225 "by convention" for parameters: first Wseries, then Bseries
    public ShiftSpecify(int shiftWSeriesAmt, int shiftBSeriesAmt) {
        this.shiftWSeries = shiftWSeriesAmt;
        this.shiftBSeries = shiftBSeriesAmt;
        this.shiftHue = 0;
    } // end constructor




    public int getShiftHue() {
        return shiftHue;
    }

    public void setHueShiftDistance(int shiftHueAmt) {
        this.shiftHue = shiftHueAmt;
    }

    public int getShiftBSeries() {
        return shiftBSeries;
    }

    public int getShiftWSeries() {
        return shiftWSeries;
    }



} // end class ShiftSpecify
