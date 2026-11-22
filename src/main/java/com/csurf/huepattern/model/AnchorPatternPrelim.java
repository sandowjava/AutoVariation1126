package com.csurf.huepattern.model;

import java.util.logging.Logger;

/**
 * Part of a minimal color intelligence system which can optionally be associated with a Template instance.
 * Represents an AnchorPattern with up to 4 anchor groups (spreads).
 * APLevel indicates how many anchor groups are defined (0-4).
 *
 * This class is an abstract model for the distribution around the hue circle of hues in the palette
 * The actual distribution of hues in the palette is determined by working in combination with the RMatrixMini
 *
 * This class contains intelligence needed for automatic color assignment and palette generation,
 * by working with the RMatrixMini to determine the hue of each shape. It does not directly create a
 * palette but is used with the RMatrixMini by the palette generator.
 */
public class AnchorPatternPrelim {
    private static final Logger logger = Logger.getLogger(AnchorPatternPrelim.class.getName());
    
    SpreadPrelim[] theSpreadPrelimArray;
    int theAPLevel = 0;

    /**
     * Default constructor - creates empty AnchorPattern.
     */
    public AnchorPatternPrelim() {
        theSpreadPrelimArray = new SpreadPrelim[4];
        theAPLevel = 0;
    }

    /**
     * Constructor for creating a default AnchorPattern with 4 anchor groups.
     * Creates spreads: AG1={35,1,3}, AG2={17,19,23}, AG3={7,9,11}, AG4={26,28,30}
     */
    public static AnchorPatternPrelim createDefault() {
        AnchorPatternPrelim ap = new AnchorPatternPrelim();
        ap.theSpreadPrelimArray = new SpreadPrelim[4];
        
        // AG1: CCW=35, Anchor=1, CW=3
        ap.theSpreadPrelimArray[0] = new SpreadPrelim(1, 1, 35, 3);
        
        // AG2: CCW=17, Anchor=19, CW=23
        ap.theSpreadPrelimArray[1] = new SpreadPrelim(2, 19, 17, 23);
        
        // AG3: CCW=7, Anchor=9, CW=11
        ap.theSpreadPrelimArray[2] = new SpreadPrelim(3, 9, 7, 11);
        
        // AG4: CCW=26, Anchor=28, CW=30
        ap.theSpreadPrelimArray[3] = new SpreadPrelim(4, 28, 26, 30);
        
        ap.theAPLevel = 4;
        return ap;
    }

    public SpreadPrelim[] getTheSpreadArray() {
        return theSpreadPrelimArray;
    }

    public int getTheAPLevel() {
        return theAPLevel;
    }
    
    // ============================================================================
    // NEW METHODS for TemplateProxyData support
    // ============================================================================
    
    /**
     * Gets the Spread for a specific AG number.
     * 
     * @param agNum the AG number (1-4)
     * @return the Spread for that AG, or null if not set or invalid AG number
     */
    public SpreadPrelim getSpread(int agNum) {
        if (agNum < 1 || agNum > 4) {
            logger.warning("getSpread: Invalid AG number: " + agNum);
            return null;
        }
        
        SpreadPrelim spreadPrelim = theSpreadPrelimArray[agNum - 1];
        if (spreadPrelim != null) {
            logger.fine("getSpread: Retrieved Spread for AG " + agNum);
        } else {
            logger.fine("getSpread: No Spread defined for AG " + agNum);
        }
        return spreadPrelim;
    } // end method getSpread
    
    /**
     * Sets the Spread for a specific AG number.
     * 
     * @param agNum the AG number (1-4)
     * @param spreadPrelim the Spread to set
     * @throws IllegalArgumentException if agNum is invalid or spread is null
     */
    public void setSpread(int agNum, SpreadPrelim spreadPrelim) {
        if (agNum < 1 || agNum > 4) {
            throw new IllegalArgumentException("Invalid AG number: " + agNum);
        }
        if (spreadPrelim == null) {
            throw new IllegalArgumentException("Spread cannot be null");
        }
        
        theSpreadPrelimArray[agNum - 1] = spreadPrelim;
        
        // Update APLevel if this is a new spread
        if (agNum > theAPLevel) {
            theAPLevel = agNum;
        }
        
        logger.fine("setSpread: Set Spread for AG " + agNum);
    } // end method setSpread
    
    /**
     * Gets the AP level (number of defined AGs).
     * 
     * @return the AP level (0-4)
     */
    public int getAPLevel() {
        return theAPLevel;
    } // end method getAPLevel
    
    /**
     * Sets the AP level (number of defined AGs).
     * 
     * @param level the AP level (0-4)
     * @throws IllegalArgumentException if level is invalid
     */
    public void setAPLevel(int level) {
        if (level < 0 || level > 4) {
            throw new IllegalArgumentException("AP level must be between 0 and 4");
        }
        
        theAPLevel = level;
        logger.fine("setAPLevel: Set AP level to " + level);
    } // end method setAPLevel
    
    /**
     * Checks if a hue is within a Spread's limits.
     * 
     * @param hue the hue value (1-36)
     * @param spreadPrelim the Spread to check against
     * @return true if hue is within the spread limits (inclusive), false otherwise
     */
    public boolean isHueWithinSpread(int hue, SpreadPrelim spreadPrelim) {
        if (spreadPrelim == null) {
            logger.warning("isHueWithinSpread: Spread is null");
            return false;
        }
        
        int ccwLimit = spreadPrelim.getCCWLimitHueNum();
        int cwLimit = spreadPrelim.getCWLimitHueNum();
        
        // Handle wrap-around at hue circle boundary (1-36)
        if (ccwLimit <= cwLimit) {
            // Normal case: CCW limit is less than CW limit
            boolean withinSpread = hue >= ccwLimit && hue <= cwLimit;
            logger.fine("isHueWithinSpread: Hue " + hue + " is " + (withinSpread ? "within" : "outside") + 
                       " spread [" + ccwLimit + "-" + cwLimit + "]");
            return withinSpread;
        } else {
            // Wrap-around case: CCW limit is greater than CW limit
            boolean withinSpread = hue >= ccwLimit || hue <= cwLimit;
            logger.fine("isHueWithinSpread: Hue " + hue + " is " + (withinSpread ? "within" : "outside") + 
                       " wrap-around spread [" + ccwLimit + "-36, 1-" + cwLimit + "]");
            return withinSpread;
        }
    } // end method isHueWithinSpread
    
    /**
     * Validates that this AnchorPattern is compatible with an RMatrixMini.
     * 
     * Checks that all AGs with DrawObj assignments have defined Spreads.
     * 
     * @param rmatrix the RMatrixMini to validate against
     * @return true if valid, false otherwise
     */
    public boolean isValidForRMatrixMini(RMatrixMiniPrelim rmatrix) {
        if (rmatrix == null) {
            logger.warning("isValidForRMatrixMini: RMatrixMini is null");
            return false;
        }
        
        // Check each AG
        for (int agNum = 1; agNum <= 4; agNum++) {
            // If this AG has members in RMatrixMini, it must have a Spread defined
            java.util.Set<java.util.UUID> members = rmatrix.getAGMembersUUIDs(agNum);
            if (!members.isEmpty() && getSpread(agNum) == null) {
                logger.warning("isValidForRMatrixMini: AG " + agNum + " has members but no Spread defined");
                return false;
            }
        }
        
        logger.info("isValidForRMatrixMini: AnchorPattern is valid for RMatrixMini");
        return true;
    } // end method isValidForRMatrixMini

} // end class AnchorPattern
