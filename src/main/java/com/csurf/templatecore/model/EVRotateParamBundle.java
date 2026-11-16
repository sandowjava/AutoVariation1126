package com.csurf.templatecore.model;

public class EVRotateParamBundle {
    private final boolean useRGBLumYesNo;
    private final double valueTolerance;
    private final BaselineValueSatModel baseline;

    // Constructor without baseline (for backward compatibility)
    public EVRotateParamBundle(boolean useRGB, double valueTolerance) {
        this.useRGBLumYesNo = useRGB;
        this.valueTolerance = valueTolerance;
        this.baseline = null;
    }

    // Constructor with baseline
    public EVRotateParamBundle(boolean useRGB, double valueTolerance, BaselineValueSatModel baseline) {
        this.useRGBLumYesNo = useRGB;
        this.valueTolerance = valueTolerance;
        this.baseline = baseline;
    }

    // Getters
    public boolean usingRGBLumForHueRotate() {
        return useRGBLumYesNo;
    }

    public double getValueTolerance() {
        return valueTolerance;
    }

    public BaselineValueSatModel getBaseline() {
        return baseline;
    }

} // end class EVRotateParamBundle
