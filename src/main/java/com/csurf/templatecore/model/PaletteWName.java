package com.csurf.templatecore.model;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

/*
 * Class purpose is to represent an array of colors that matches the number of DrawObjs in a DrawObjRoster instance
 * Each of the colors in the array correlates to a single DrawObj by its array index number
 * This class will eventually be used to load a new array of colors.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaletteWName implements IPalette {
    private ColorPtOstDot[] colorPtOstdotArray;
    private String paletteName;

    /**
     * Default constructor for Jackson deserialization.
     * Required by Jackson to deserialize JSON into PaletteWName objects.
     */
    public PaletteWName() {
        this.colorPtOstdotArray = new ColorPtOstDot[0];
    }




    /**
     * Constructor that extracts palette directly from an existing DrawObjRoster.
     * This constructor directly accesses the DrawObj list and extracts colors.
     * 
     * @param someDrawObjRoster The DrawObjRoster to extract colors from
     */
    public PaletteWName(DrawObjRoster someDrawObjRoster) {
        if (someDrawObjRoster == null) {
            throw new IllegalArgumentException("DrawObjRoster cannot be null");
        }
        
        int numColorPts = someDrawObjRoster.getNumDrawObjs();
        colorPtOstdotArray = new ColorPtOstDot[numColorPts];
        
        // Extract colors directly from the DrawObjs in the roster
        for (int i = 0; i < numColorPts; i++) {
            colorPtOstdotArray[i] = someDrawObjRoster.getTemplateDrawObjs().get(i).getCurrentColor();
        }
    } // end constructor




    /**
     * Constructor that creates a new palette from an array of colors.
     * This palette has no connection to any DrawObjRoster instance.
     * 
     * @param someColorPtOstdotArray The array of colors to use
     */
    public PaletteWName(ColorPtOstDot[] someColorPtOstdotArray) {
        if (someColorPtOstdotArray == null) {
            throw new IllegalArgumentException("Color array cannot be null");
        }
        
        // Create a defensive copy of the input array
        this.colorPtOstdotArray = Arrays.copyOf(someColorPtOstdotArray, someColorPtOstdotArray.length);
    } // end constructor




    /**
     * Constructor that creates a new palette from an array of colors with a specified name.
     * This palette has no connection to any DrawObjRoster instance.
     * 
     * @param someColorPtOstdotArray The array of colors to use
     * @param name The name to assign to this palette
     */
    public PaletteWName(ColorPtOstDot[] someColorPtOstdotArray, String name) {
        this(someColorPtOstdotArray);  // Call the existing constructor
        this.paletteName = name;
    } // end constructor




    /**
     * Returns a copy of the color array to prevent external modification.
     * 
     * @return A copy of the internal color array
     */
    public ColorPtOstDot[] getColorPtOstdotArray() {
        return Arrays.copyOf(colorPtOstdotArray, colorPtOstdotArray.length);
    } // end method getColorPtOstdotArray

    /**
     * Sets the color array. Used by Jackson during deserialization.
     * 
     * @param colorPtOstdotArray The array of colors to set
     */
    @JsonProperty("colorPtOstdotArray")
    public void setColorPtOstdotArray(ColorPtOstDot[] colorPtOstdotArray) {
        if (colorPtOstdotArray == null) {
            this.colorPtOstdotArray = new ColorPtOstDot[0];
        } else {
            this.colorPtOstdotArray = Arrays.copyOf(colorPtOstdotArray, colorPtOstdotArray.length);
        }
    }

    /**
     * Returns the size of the palette (number of colors).
     * 
     * @return The number of colors in this palette
     */
    public int getSize() {
        return colorPtOstdotArray.length;
    } // end method getSize


    @JsonProperty("paletteName")
    public String getPaletteName() {
        return paletteName;
    }

    public void setPaletteName(String paletteName) {
        this.paletteName = paletteName;
    }

    /**
     * Returns the color at the specified index.
     * 
     * @param index The index of the color to retrieve
     * @return The color at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public ColorPtOstDot getColorAt(int index) {
        if (index < 0 || index >= colorPtOstdotArray.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + colorPtOstdotArray.length);
        }
        return colorPtOstdotArray[index];
    }


} // end class PaletteWName
