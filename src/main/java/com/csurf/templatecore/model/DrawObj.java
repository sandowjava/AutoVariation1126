package com.csurf.templatecore.model;

import com.csurf.artintellig.model.PipeOneColor;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.ostwald.domainmonotri.model.OstPosition;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a drawable object with a name, path, and color.
 * Used for rendering and user interaction.
 */
public class DrawObj implements PipeOneColor {
    private static final Logger logger = Logger.getLogger(DrawObj.class.getName());
    private String name;  
    private final GeneralPath path;
    private ColorPtOstDot fillColorOstdot;
    private Color strokeColor;  
    private boolean selected;
    private final UUID uniqueId;
    private final String pipeUnityID;

    // SVG-specific fields for round-trip conversion
    private String svgElementType;  // e.g., "path", "rect", "circle", etc.
    private String svgOriginalPathData;  // Original 'd' attribute for path elements
    private Map<String, String> svgAttributes;  // Store all original SVG attributes

    /**
     * Constructs a new DrawObj with the specified name, path, and fill color.
     * @param name the name of the object
     * @param path the path of the object
     * @param fillColorOstdot the fill color of the object
     */
    public DrawObj(String name, GeneralPath path, Object fillColorOstdot) {
        this.name = name;
        this.path = path;
        // Convert AWT Color to proper ColorPtOstDot using MonotriBook
        if (fillColorOstdot instanceof ColorPtOstDot) {
            this.fillColorOstdot = (ColorPtOstDot) fillColorOstdot;
        } else if (fillColorOstdot instanceof Color) {
            this.fillColorOstdot = MonotriBook.getInstance().findClosestOstDot((Color) fillColorOstdot);
        } else {
            throw new IllegalArgumentException("fillColor must be either Color or ColorPtOstDot");
        }
        this.strokeColor = this.fillColorOstdot.toAWTColor();
        this.selected = false;
        this.uniqueId = UUID.randomUUID(); // Generate a unique ID
        logger.fine("Generated new UUID: " + this.uniqueId + " for DrawObj: " + name);
        logger.info("DrawObj created with name: " + name + " and UUID: " + this.uniqueId);
        this.pipeUnityID = name; // Using name as ID for now

        // Initialize SVG-specific fields
        this.svgElementType = "path"; // Default element type
        this.svgOriginalPathData = null;
        this.svgAttributes = new HashMap<>();
    } // Constructor

    /**
     * Constructs a new DrawObj with the specified name, path, fill color, and unique ID.
     * This constructor is primarily used during deserialization to preserve object identity.
     * 
     * @param name the name of the object
     * @param path the path of the object
     * @param fillColorOstdot the fill color of the object
     * @param uniqueId the unique identifier for this object
     */
    public DrawObj(String name, GeneralPath path, Object fillColorOstdot, UUID uniqueId) {
        this.name = name;
        this.path = path;
        // Convert AWT Color to proper ColorPtOstDot using MonotriBook
        if (fillColorOstdot instanceof ColorPtOstDot) {
            this.fillColorOstdot = (ColorPtOstDot) fillColorOstdot;
        } else if (fillColorOstdot instanceof Color) {
            this.fillColorOstdot = MonotriBook.getInstance().findClosestOstDot((Color) fillColorOstdot);
        } else {
            throw new IllegalArgumentException("fillColor must be either Color or ColorPtOstDot");
        }
        this.strokeColor = this.fillColorOstdot.toAWTColor();
        this.selected = false;
        this.uniqueId = uniqueId != null ? uniqueId : UUID.randomUUID();
        this.pipeUnityID = name; // Using name as ID for now
        
        if (uniqueId != null) {
            logger.fine("Using provided UUID: " + uniqueId + " for DrawObj: " + name);
            logger.info("DrawObj created with existing UUID: " + uniqueId + " and name: " + name);
        } else {
            logger.fine("Generated new UUID: " + this.uniqueId + " for DrawObj: " + name);
            logger.info("DrawObj created with name: " + name + " and UUID: " + this.uniqueId);
        }
        
        // Initialize SVG-specific fields
        this.svgElementType = "path"; // Default element type
        this.svgOriginalPathData = null;
        this.svgAttributes = new HashMap<>();
    } // Constructor

    /**
     * Draws this object using the specified graphics context.
     */
    public void drawShape(Graphics2D g2d) {
        if (g2d != null) {
            g2d.setColor(fillColorOstdot.toAWTColor());
            g2d.fill(path);
            if (selected) {
                g2d.setColor(strokeColor);
                g2d.draw(path);
            }
        }
    } // end method drawShape

    /**
     * Gets the name of this object.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this object.
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the path of this object.
     * @return the path
     */
    public GeneralPath getPath() {
        return path;
    }

    /**
     * Gets the fill color of this object as a ColorPtOstDot.
     * @return the fill color
     */
    public ColorPtOstDot getColorPtOstDotFillColor() {
        return fillColorOstdot;
    }

    /**
     * Sets the fill color of this object.
     * @param fillColorOstdot the new fill color
     */
    public void setFillColorOstdot(ColorPtOstDot fillColorOstdot) {
        this.fillColorOstdot = fillColorOstdot;
        this.strokeColor = fillColorOstdot.toAWTColor();
    } // end method setFillColor

    @Override
    public List<GeneralPath> getShapes() {
        return Collections.singletonList(path);
    }

    @Override
    public ColorPtOstDot getCurrentColor() {
        return fillColorOstdot;
    }

    @Override
    public void setColor(ColorPtOstDot color) {
        System.out.println("DEBUG: DrawObj.setColor called with color: " + color);
        if (color == null) {
            System.out.println("DEBUG: Color is null, defaulting to internal hue 0 (user-facing hue 1)");
            ColorPtOstDot defaultFillColor = MonotriBook.getInstance().getPureHueColorByCodeHueNum(0); // Internal hue 0 = user-facing hue 1
            this.fillColorOstdot = defaultFillColor;
        } else {
            System.out.println("DEBUG: Setting color with hue: " + color.getOstHCS().getStdCodeHueNum() +
                    ", fillOrder: " + color.getOstHCS().getFillOrderNum());
            this.fillColorOstdot = color;
        }
        this.strokeColor = this.fillColorOstdot.toAWTColor();
    } // end method setColor

    @Override
    public String getPipeUnityId() {
        return pipeUnityID;
    }

    /**
     * Returns the unique identifier for this DrawObj.
     * This UUID is used for reference resolution during serialization/deserialization.
     * 
     * @return the unique identifier
     */
    public UUID getUniqueId() {
        return uniqueId;
    }

    /**
     * Gets the stroke color of this object.
     * @return the stroke color
     */
    public Color getStrokeColor() {
        return strokeColor;
    }

    /**
     * Sets the stroke color of this object.
     * @param strokeColor the new stroke color
     */
    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor != null ? strokeColor : Color.BLACK;
    }

    /**
     * Checks if this object is selected.
     * @return true if selected, false otherwise
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the selection state of this object.
     * @param selected true to select, false to deselect
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            this.strokeColor = getComplementaryStrokeColor().toAWTColor();
        } else {
            this.strokeColor = this.fillColorOstdot.toAWTColor();
        }
    } // end method setSelected

    /**
     * Gets the complementary stroke color for this object.
     * @return the complementary stroke color
     */
    private ColorPtOstDot getComplementaryStrokeColor() {
        // Get current hue as internal code number (0-35)
        int currentHue = fillColorOstdot.getOstHCS().getStdCodeHueNum();

        // Add 18 (half of 36) to get complementary hue
        int complementaryHue = (currentHue + 18) % 36;  // Use modulo to handle wrap-around

        // Validate that we're still in valid internal range (0-35)
        if (complementaryHue < 0 || complementaryHue > 35) {
            throw new IllegalStateException(
                    String.format("Complementary hue %d is outside valid range 0-35", complementaryHue));
        }

        return MonotriBook.getInstance().getPureHueColorByCodeHueNum(complementaryHue);
    } // end method getComplementaryStrokeColor

    /**
     * Gets the bounds of this object.
     * @return the bounds
     */
    public Rectangle2D getBounds() {
        return path.getBounds2D();
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Gets the current color's OstPosition.
     * @return the OstPosition
     */
    public OstPosition getCurrColorOstPosition() {
        OstPosition ostPosition = this.getCurrentColor().getOstHCS().getOstPosition();
        return ostPosition;
    } // end method getCurrColorOstPosition

    /**
     * Gets the internal (code-facing) hue number for this DrawObj's fill color.
     * 
     * Internal hue numbers range from 0-35 (36 total hues).
     * This is the hue system used internally by the color model.
     * 
     * @return the internal hue number (0-35), or -1 if the fill color is not set
     */
    public int getCodeHueNum() {
        if (fillColorOstdot == null || fillColorOstdot.getOstHCS() == null) {
            return -1;  // Sentinel value for "not set"
        }
        return fillColorOstdot.getOstHCS().getStdCodeHueNum();
    }

    /**
     * Gets the user-facing hue number for this DrawObj's fill color.
     * 
     * User-facing hue numbers range from 1-36 (36 total hues).
     * This is the hue system displayed to users in the UI.
     * Converts from internal code-facing hues (0-35) by adding 1.
     * 
     * @return the user-facing hue number (1-36), or 0 if the fill color is not set
     */
    public int getUserFacingHueNum() {
        int codeHue = getCodeHueNum();
        return codeHue == -1 ? 0 : codeHue + 1;  // Convert 0-35 to 1-36
    }

    /**
     * Gets the fill order number for this DrawObj's fill color.
     * 
     * Fill order numbers range from 1-78 and represent the position
     * in the complete color palette (across all hues and values/saturations).
     * 
     * @return the fill order number (1-78), or -1 if the fill color is not set
     */
    public int getFillOrderNum() {
        if (fillColorOstdot == null || fillColorOstdot.getOstHCS() == null) {
            return -1;  // Sentinel value for "not set"
        }
        return fillColorOstdot.getOstHCS().getFillOrderNum();
    }

    /**
     * Gets the AWT Color representation of this DrawObj's fill color.
     * 
     * This is useful for rendering and color display operations.
     * 
     * @return the AWT Color for rendering, or black if the fill color is not set
     */
    public Color getAWTColor() {
        if (fillColorOstdot == null) {
            return Color.BLACK;  // Sensible default for rendering
        }
        return fillColorOstdot.toAWTColor();
    }

    /**
     * Gets the absolute color saturation for this DrawObj's fill color.
     * 
     * Absolute saturation is a measure of the color's saturation level in the Ostwald color system.
     * This value is useful for sorting and filtering DrawObjs by color saturation.
     * 
     * @return the absolute color saturation value, or 0.0 if the fill color is not set
     */
    public double getAbsColorSat() {
        if (fillColorOstdot == null) {
            return 0.0;  // Sensible default for unset color
        }
        return fillColorOstdot.getAbsColorSaturation();
    }

    /**
     * Gets the RGB luminance (3D) value for this DrawObj's fill color.
     * 
     * RGB luminance is a measure of the color's brightness in the RGB color space.
     * The value is guaranteed to be in the range [0.0, 1.0], where 0.0 is black and 1.0 is white.
     * This value is useful for sorting and filtering DrawObjs by brightness.
     * 
     * @return the RGB 3D luminance value in range [0.0, 1.0], or 0.0 if the fill color is not set
     */
    public double getRGBLum3D() {
        if (fillColorOstdot == null) {
            return 0.0;  // Sensible default for unset color
        }
        return fillColorOstdot.getLum3D();
    }

    /**
     * Sets the SVG element type for this DrawObj.
     * @param elementType the SVG element type (e.g., "path", "rect", "circle")
     */
    public void setSvgElementType(String elementType) {
        this.svgElementType = elementType;
    }

    /**
     * Gets the SVG element type for this DrawObj.
     * @return the SVG element type
     */
    public String getSvgElementType() {
        return svgElementType;
    }

    /**
     * Sets the original SVG path data for this DrawObj.
     * @param pathData the original SVG path data (d attribute)
     */
    public void setSvgOriginalPathData(String pathData) {
        this.svgOriginalPathData = pathData;
    }

    /**
     * Gets the original SVG path data for this DrawObj.
     * @return the original SVG path data
     */
    public String getSvgOriginalPathData() {
        return svgOriginalPathData;
    }

    /**
     * Sets an SVG attribute for this DrawObj.
     * @param name the attribute name
     * @param value the attribute value
     */
    public void setSvgAttribute(String name, String value) {
        if (svgAttributes == null) {
            svgAttributes = new HashMap<>();
        }
        svgAttributes.put(name, value);
    }

    /**
     * Gets an SVG attribute for this DrawObj.
     * @param name the attribute name
     * @return the attribute value, or null if not found
     */
    public String getSvgAttribute(String name) {
        return svgAttributes != null ? svgAttributes.get(name) : null;
    }

    /**
     * Gets all SVG attributes for this DrawObj.
     * @return an unmodifiable map of all SVG attributes
     */
    public Map<String, String> getSvgAttributes() {
        return svgAttributes != null ? Collections.unmodifiableMap(svgAttributes) : Collections.emptyMap();
    }
} // end class DrawObj
