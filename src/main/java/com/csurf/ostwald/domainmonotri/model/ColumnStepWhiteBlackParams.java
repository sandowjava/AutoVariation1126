package com.csurf.ostwald.domainmonotri.model;


// 051525 added
public class ColumnStepWhiteBlackParams {
    // members
    int theColumn;
    int theStep;
    int theWhiteSeries;
    int theBlackSeries;
    int theDrawOrderNum; // 051925



    // constructor
    public ColumnStepWhiteBlackParams(int whatColumn, int whatStep, int whatWhiteSeries, int whatBlackSeries, int whatDrawOrderNum) {
        theColumn = whatColumn;
        theStep = whatStep;
        theWhiteSeries = whatWhiteSeries;
        theBlackSeries = whatBlackSeries;
        theDrawOrderNum = whatDrawOrderNum;
    } // end constructor


    public void setTheBlackSeries(int theBlackSeries) {
        this.theBlackSeries = theBlackSeries;
    }

    public void setTheColumn(int theColumn) {
        this.theColumn = theColumn;
    }


    public void setTheStep(int theStep) {
        this.theStep = theStep;
    }

    public void setTheWhiteSeries(int theWhiteSeries) {
        this.theWhiteSeries = theWhiteSeries;
    }


    public int getTheBlackSeries() {
        return theBlackSeries;
    }


    public int getTheColumn() {
        return theColumn;
    }


    public int getTheStep() {
        return theStep;
    }


    public int getTheWhiteSeries() {
        return theWhiteSeries;
    }


    public int getTheDrawOrderNum() { return theDrawOrderNum; }
} // end class ColumnStepWhiteBlackParams
