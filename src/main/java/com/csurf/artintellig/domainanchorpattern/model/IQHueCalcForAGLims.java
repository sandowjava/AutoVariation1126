package com.csurf.artintellig.domainanchorpattern.model;

public class IQHueCalcForAGLims {



    // constructor // TODO 052425 refactor into new IQAGSpread class -THEN DELETE- do NOT have separate utility class
    public IQHueCalcForAGLims() {

        // TODO 052325 needs calc code
    } // end constructor




    public int calcClosestStdHue(double pipeDistFromAnchor, double someCWLimitIn360Degr, double someCCWLimitIn360Degr) {
        double zeroForCalcPurposes = 16.0;
        int closestStdHue = 0;
        // TODO 052325 needs calc code

        double exactNewHue = 0.511; // TODO 052325 needs calc code
        double entirelyBogusCalc = zeroForCalcPurposes + 2.22 + exactNewHue;
        double moreBogus = adjustDoubleTo36StepLimits(entirelyBogusCalc);
        closestStdHue = (int)moreBogus;

        return closestStdHue;
    } // end method calcClosestStdHue



    private double adjustDoubleTo36StepLimits(double doubleToAdjust) {
        double returnDouble = 0;
        // TODO 052325 needs calc code
        return returnDouble;
    } // end method adjustDoubleTo36StepLimits


} // end class IQHueCalcForAGLims
