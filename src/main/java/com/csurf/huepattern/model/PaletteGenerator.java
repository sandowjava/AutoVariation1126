package com.csurf.huepattern.model;


import com.csurf.huepattern.util.HueCircleStepCalc;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.PaletteWName;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;


/**
 * Controller for putting into effect the minimal color intelligence system consisting of RMatrixMini and AnchorPattern.
 * PaletteGenerator can be seen as a "service class" since it does not maintain any state outside the service it performs.

 * This class is the active component of "artistic intelligence" and translates the RMatrixMini and AnchorPattern specifications
 * into an actual palette.
 * PaletteGenerator is designed to work "one-by-one" generating colors for each DrawObj instance.

 * This class uses data from both the RMatrixMini and AnchorPattern instances.
 * This class contains intelligence for automatic palette generation, including "AG accordion" and hue-rotation methods,
 * by working with both RMatrixMini and AnchorPattern to determine the hue of each shape, and then the ColorPtOstDot.
 * It directly creates a palette this way.
 * 
 * 110725 REFACTORED: Updated to work with UUID-based RMatrixMini instead of index-based arrays.
 */
public class PaletteGenerator {
    private static final Logger logger = Logger.getLogger(PaletteGenerator.class.getName());
    
    RMatrixMini theRMatrixMini;
    AnchorPattern theAnchorPattern;


    // Constructor
    public PaletteGenerator(RMatrixMini someRMatrixMini, AnchorPattern someAnchPattern) {
        this.theRMatrixMini = someRMatrixMini;
        this.theAnchorPattern = someAnchPattern;
    } // end constructor



    /**
     * Creates a new palette by applying hue transformations to selected AG members.
     * 110725 REFACTORED: Now works with UUID-based RMatrixMini instead of index-based arrays.
     * 
     * @param someChangedAGs Array of AG numbers (1-4) that have changed
     * @return A new PaletteWName with updated colors for changed AG members
     */
    public PaletteWName createNewPalette(int[] someChangedAGs) {
        logger.info("createNewPalette: Starting palette generation for " + someChangedAGs.length + " changed AGs");
        
        // Get start palette as UUID-based map
        Map<UUID, ColorPtOstDot> startPaletteMap = this.theRMatrixMini.getStartPaletteMap();
        Map<UUID, ColorPtOstDot> newPaletteMap = new HashMap<>(startPaletteMap);

        // For each changed AG, process each member of that AG
        for (int i = 0; i < someChangedAGs.length; i++) {
            int agNum = someChangedAGs[i];
            logger.fine("createNewPalette: Processing AG " + agNum);
            
            // Get AG members as DrawObj instances (not indices)
            Set<DrawObj> currAGmembers = this.theRMatrixMini.getAGMembersSet(agNum);
            logger.fine("createNewPalette: AG " + agNum + " has " + currAGmembers.size() + " members");
            
            // Get the spread for this AG
            Spread agSpread = this.theAnchorPattern.getTheSpreadArray()[agNum - 1];  // AG numbers are 1-based
            
            // Process each member of the AG
            for (DrawObj drawObj : currAGmembers) {
                if (drawObj == null || drawObj.getUniqueId() == null) {
                    logger.warning("createNewPalette: DrawObj or UUID is null, skipping");
                    continue;
                }
                
                UUID drawObjUUID = drawObj.getUniqueId();
                
                // Get proportion location for this DrawObj
                double proportionLocation = this.theRMatrixMini.getProportionLocation(drawObj);
                
                // Calculate new hue based on proportion location and spread
                int newCodeFacingHueNum = calcNewHueBySmarts(proportionLocation, agSpread);
                
                // Get the current color to preserve fill order (brightness/saturation)
                ColorPtOstDot currentColor = startPaletteMap.get(drawObjUUID);
                if (currentColor == null) {
                    logger.warning("createNewPalette: No start palette color for " + drawObj);
                    continue;
                }
                
                // 110725 note: new colors do NOT change OstHueColumnStep position, so fillOrderNum stays unchanged
                int fillOrderNum = currentColor.getFillOrderNum();
                ColorPtOstDot newCurrOstdot = MonotriBook.getInstance()
                        .getMonoTriPage(newCodeFacingHueNum)
                        .getColorByFillOrder(fillOrderNum);
                
                newPaletteMap.put(drawObjUUID, newCurrOstdot);
                logger.fine("createNewPalette: Updated color for " + drawObj + " to hue " + newCodeFacingHueNum);
            } // end INNER FOR loop
        } // end OUTER FOR loop

        // Convert UUID-based map back to array for PaletteWName
        ColorPtOstDot[] newPaletteOstdots = convertMapToArray(newPaletteMap);
        
        PaletteWName newPalette = new PaletteWName(newPaletteOstdots, "New AnchPattern Palette");
        logger.info("createNewPalette: Palette generation complete");
        return newPalette;
    } // end createNewPalette
    
    /**
     * Converts a UUID-based palette map back to an array for PaletteWName.
     * 110725 NEW: Helper method for converting UUID-based map to array format.
     * 
     * @param paletteMap The UUID-based palette map
     * @return An array of ColorPtOstDot objects in the same order as the original palette
     */
    private ColorPtOstDot[] convertMapToArray(Map<UUID, ColorPtOstDot> paletteMap) {
        // Get the start palette to determine the correct order and size
        Map<UUID, ColorPtOstDot> startPaletteMap = this.theRMatrixMini.getStartPaletteMap();
        ColorPtOstDot[] result = new ColorPtOstDot[startPaletteMap.size()];
        
        int index = 0;
        for (UUID uuid : startPaletteMap.keySet()) {
            result[index] = paletteMap.getOrDefault(uuid, startPaletteMap.get(uuid));
            index++;
        }
        
        return result;
    } // end method convertMapToArray




    private int calcNewHueBySmarts(double proportionLocation, Spread someSpread) {
        int anchorHueNum = someSpread.getAnchorHueNum();
        int spreadStepsCW = someSpread.getStepsAnchorToCWLimit();
        int spreadStepsCCW = someSpread.getStepsAnchorToCCWLimit();

        int newCodeFacingHueNum = anchorHueNum;
        if(proportionLocation==0) { newCodeFacingHueNum = anchorHueNum;}
        if(proportionLocation==1) { newCodeFacingHueNum = someSpread.getCWLimitHueNum();}
        if(proportionLocation==-1) { newCodeFacingHueNum = someSpread.getCCWLimitHueNum();}

        // 110725 move to hueNum inside CCW spread - HueCircleStepCalc handles circular addition
        if(proportionLocation<0 && proportionLocation> -1) {
            int delta = (int) Math.round(proportionLocation * spreadStepsCCW); // proportionLocation is negative ->negative result
            newCodeFacingHueNum = HueCircleStepCalc.goStepsCWorCCW(anchorHueNum, delta); // note: ADDING because delta is negative
        }

        // 110725 move to hueNum inside CW spread - HueCircleStepCalc handles circular addition
        if(proportionLocation>0 && proportionLocation< 1) {
            int delta = (int) Math.round(proportionLocation * spreadStepsCW); // proportionLocation is positive ->positive result
            newCodeFacingHueNum = HueCircleStepCalc.goStepsCWorCCW(anchorHueNum, delta);
        }

        return newCodeFacingHueNum;
    } // end method calcNewHueBySmarts




    private void fillNewPaletteOstdotsWithStartPaletteOstdots(ColorPtOstDot[] newPaletteOstdots, ColorPtOstDot[] startPaletteOstdots) {
        for (int i = 0; i < newPaletteOstdots.length; i++) {
            newPaletteOstdots[i] = startPaletteOstdots[i];
        } // end FOR
    } // end method fillNewPaletteOstdotsWithStartPaletteOstdots


} // end class PaletteGenerator
