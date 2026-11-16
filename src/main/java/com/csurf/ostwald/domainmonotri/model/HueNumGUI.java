package com.csurf.ostwald.domainmonotri.model;


/**
 * Provides a standard set of hue number choices in range 1-36 for user-facing hueNum operations.
 * Uses a sort of singleton pattern for instances to ensure only one instance exists for each INT value.
 * Design intent is to replace the use of simple INT params for clarity in hueNum operations,
 * especially for user-facing versus code-based hueNums.
 */
public class HueNumGUI {
    private final int userFacingHueNum; // 1-36 representing unique user-facing hueNums
    private final int internalCodeHueNum; // 0-35 representing internal hueNums
    private static final HueNumGUI[] INSTANCES = new HueNumGUI[36]; // Note: zero-based array index - "off by one" to prevent test failures

    static {
        // Create all 36 instances at class load time. Note param passed in is user-facing (1-36), but array is zero-based (0-35)
        for (int i = 1; i <= 36; i++) {
            INSTANCES[i-1] = new HueNumGUI(i);
        }
    } // end static block


    // constructor
    private HueNumGUI(int userFacingHueNum) {
        if(userFacingHueNum < 1 || userFacingHueNum > 36) {
            throw new IllegalArgumentException("User-facing hue number must be in range 1-36");
        } // end IF
        this.userFacingHueNum = userFacingHueNum;
        internalCodeHueNum = userFacingHueNum-1;
    } // end constructor



    // factory method
    /**
     * Note that this method uses a zero-based index for the hue number, so hue number 1 uses an index of 0
     * So the zero-based index is another potential source of "off-by-one" errors. Should param be user-facing or code-facing?
     * Purpose of class is to assert primacy of user-facing hueNums, so its interface params should also reflect that
     * @param someUserFacingHueNum
     * @return
     */
    public static HueNumGUI getHueNumGUIInstance(int someUserFacingHueNum) {
        return INSTANCES[someUserFacingHueNum-1]; // do the adjustment for the zero-based index INTERNALLY, not in the param passed in
    } // end method getHueNumGUIInstance



    // getter
    public int getUserFacingHueNum() {
        return userFacingHueNum;
    } // end method getUserFacingHueNum


    // getter
    public int getInternalCodeHueNum() {
        return internalCodeHueNum;
    } // end method getInternalCodeHueNum


    /**
     * This method adds or subtracts a shiftDistance from the user-facing hue number of an instance of HueNumGUI.
     * The result is wrapped around to the range 1-36.
     * This method cannot be static with the single input parameter because it uses the instance variable userFacingHueNum
     * @param shiftDistance
     * @return
     */
    /* // 082625 commented out -ensuring userFacingHueNum "within range" not needed, makes the code too complex, hard to read and understand
    public HueNumGUI circularAddOrSubtract(int shiftDistance) {
        // Convert to 0-based for arithmetic, handle negative values, then back to user-facing range 1-36
        // first make sure that userFacingHueNum is in range 1-36, using modular arithmetic [subtract 1 makes it 0-based for modular arithmetic]
        int userFacingNumInRange = ((userFacingHueNum - 1) % 36 + 36) % 36 +1; // add 1 at end to make it user-facing
        int newUserFacingHueNum = ((userFacingNumInRange - 1 + shiftDistance) % 36 + 36) % 36 + 1; // design intent: newUserFacingHueNum is user-facing
        // Special case due to adding 1: when exactly at 36, we want to wrap to 1, not 0
        if (newUserFacingHueNum == 37) {
            newUserFacingHueNum = 1;
        } // end IF

        return HueNumGUI.getHueNumGUIInstance(newUserFacingHueNum);
    } // end method circularAddOrSubtract
    */



    public HueNumGUI circularAddOrSubtract(int shiftDistance) {
        int codeFacingStartHueNum = getInternalCodeHueNum(); // uses range 0-35 for modular arithmetic, so use code-facing hueNum
        // Handle the shift with modular arithmetic
        int codeFacingResult = Math.floorMod(codeFacingStartHueNum + shiftDistance, 36);

        // The above handles positive shifts correctly
        // For negative shifts, Math.floorMod ensures the codeFacingResult is positive

        // handle special case of codeFacingResult +1 == 37
        if (codeFacingResult +1 == 37) {
            codeFacingResult = 0;
        } // end IF

        // add 1 to convert codeFacingResult to user-facing hueNum for method param, since method expects user-facing hueNum
        return HueNumGUI.getHueNumGUIInstance(codeFacingResult +1);

    } // end method circularAddOrSubtract




    // toString
    public String toString() {
        return "HueNumGUI{" +
                "userFacingHueNum=" + userFacingHueNum +
                ", internalCodeHueNum=" + internalCodeHueNum +
                '}';
    } // end toString



} // end class HueNumGUI
