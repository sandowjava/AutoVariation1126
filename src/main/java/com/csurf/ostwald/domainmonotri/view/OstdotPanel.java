package com.csurf.ostwald.domainmonotri.view;

import com.csurf.ostwald.domaingeo.model.HLSParamBundle;
import com.csurf.ostwald.domainmonotri.controller.OstdotDisplayState;
import com.csurf.ostwald.domainmonotri.model.*;
import com.csurf.ostwald.domainmonotri.events.OstdotEvent;
import com.csurf.ostwald.domainmonotri.events.OstdotListener;
import com.csurf.ostwald.domainmonotri.controller.MouseListenerDotDrag;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OstdotPanel extends JPanel {
    private ColorPtOstDot theColorPtOstDot;
    private boolean isHovered = false;
    private boolean isSelected = false; // TODO 060525 will work with OstdotPanelAssessState class
    private boolean isOccupied = false; // TODO 060525 will work with OstdotPanelAssessState class
    private boolean isCMYKMode = false;
    private boolean inPalette = false;
    private Color backgroundColor = Color.WHITE;
    private static final int BORDER_WIDTH = 2;
    private final List<OstdotListener> listeners = new ArrayList<>();
    private OstdotDisplayState currentDisplayState = OstdotDisplayState.UNOCCUPIED;
    private ColorDisplayMode colorMode = ColorDisplayMode.RGB;

    /**
     * Creates a new OstdotPanel with the specified color.
     * 
     * @param initialColor The initial ColorPtOstDot to display, can be null
     */
    public OstdotPanel(ColorPtOstDot initialColor) {
        this.theColorPtOstDot = initialColor;
        this.isOccupied = (initialColor != null); // TODO 061225 NOT setting isOccupied true in all cases. Examine flow for this?
        
        // Set to non-opaque to allow proper event handling
        setOpaque(false);
        
        // Enable mouse events for this panel
        enableEvents(MouseEvent.MOUSE_EVENT_MASK | MouseEvent.MOUSE_MOTION_EVENT_MASK);
        
        setupMouseListeners();
    } // end constructor


    
    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setHovered(true);
                fireEvent(new OstdotEvent(OstdotPanel.this, theColorPtOstDot, OstdotEvent.OstdotEventType.HOVER_ENTER));
                // Don't consume the event - allow it to propagate to parent components
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                setHovered(false);
                fireEvent(new OstdotEvent(OstdotPanel.this, theColorPtOstDot, OstdotEvent.OstdotEventType.HOVER_EXIT));
                // Don't consume the event - allow it to propagate to parent components
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("OstdotPanel mouseClicked: " + e.getPoint());
                toggleSelected();
                
                // Explicitly forward the event to the parent component
                Container parent = getParent();
                if (parent != null) {
                    // Convert point to parent's coordinate system
                    Point parentPoint = SwingUtilities.convertPoint(OstdotPanel.this, e.getPoint(), parent);
                    
                    // Create a new event for the parent
                    MouseEvent parentEvent = new MouseEvent(
                        parent, 
                        e.getID(), 
                        e.getWhen(), 
                        e.getModifiers(), 
                        parentPoint.x, 
                        parentPoint.y,
                        e.getClickCount(), 
                        e.isPopupTrigger(),
                        e.getButton()
                    );
                    
                    // Dispatch the event to the parent
                    System.out.println("Forwarding mouseClicked to parent: " + parent.getClass().getSimpleName());
                    for (MouseListener listener : parent.getMouseListeners()) {
                        System.out.println("  Parent has listener: " + listener.getClass().getSimpleName());
                        if (listener instanceof MouseListenerDotDrag) {
                            System.out.println("  Found MouseListenerDotDrag, calling mouseClicked");
                            listener.mouseClicked(parentEvent);
                        }
                    }
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                System.out.println("OstdotPanel mousePressed: " + e.getPoint());
                
                // Explicitly forward the event to the parent component
                Container parent = getParent();
                if (parent != null) {
                    // Convert point to parent's coordinate system
                    Point parentPoint = SwingUtilities.convertPoint(OstdotPanel.this, e.getPoint(), parent);
                    
                    // Create a new event for the parent
                    MouseEvent parentEvent = new MouseEvent(
                        parent, 
                        e.getID(), 
                        e.getWhen(), 
                        e.getModifiers(), 
                        parentPoint.x, 
                        parentPoint.y,
                        e.getClickCount(), 
                        e.isPopupTrigger(),
                        e.getButton()
                    );
                    
                    // Dispatch the event to the parent
                    System.out.println("Forwarding mousePressed to parent: " + parent.getClass().getSimpleName());
                    for (MouseListener listener : parent.getMouseListeners()) {
                        System.out.println("  Parent has listener: " + listener.getClass().getSimpleName());
                        if (listener instanceof MouseListenerDotDrag) {
                            System.out.println("  Found MouseListenerDotDrag, calling mousePressed");
                            listener.mousePressed(parentEvent);
                        }
                    }
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                System.out.println("OstdotPanel mouseReleased: " + e.getPoint());
                
                // Explicitly forward the event to the parent component
                Container parent = getParent();
                if (parent != null) {
                    // Convert point to parent's coordinate system
                    Point parentPoint = SwingUtilities.convertPoint(OstdotPanel.this, e.getPoint(), parent);
                    
                    // Create a new event for the parent
                    MouseEvent parentEvent = new MouseEvent(
                        parent, 
                        e.getID(), 
                        e.getWhen(), 
                        e.getModifiers(), 
                        parentPoint.x, 
                        parentPoint.y,
                        e.getClickCount(), 
                        e.isPopupTrigger(),
                        e.getButton()
                    );
                    
                    // Dispatch the event to the parent
                    System.out.println("Forwarding mouseReleased to parent: " + parent.getClass().getSimpleName());
                    for (MouseListener listener : parent.getMouseListeners()) {
                        System.out.println("  Parent has listener: " + listener.getClass().getSimpleName());
                        if (listener instanceof MouseListenerDotDrag) {
                            System.out.println("  Found MouseListenerDotDrag, calling mouseReleased");
                            listener.mouseReleased(parentEvent);
                        }
                    }
                }
            }
        });
    } // end method setupMouseListeners




    /**
     * Adds an OstdotListener to this panel.
     *
     * @param listener The listener to add
     */
    public void addOstdotListener(OstdotListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    } // end method addOstdotListener



    
    /**
     * Removes an OstdotListener from this panel.
     * 
     * @param listener The listener to remove
     */
    public void removeOstdotListener(OstdotListener listener) {
        listeners.remove(listener);
    } // end method removeOstdotListener


    protected void fireEvent(OstdotEvent event) {
        for (OstdotListener listener : listeners) {
            listener.ostdotEventOccurred(event);
        }
    } // end method fireEvent


    
    /**
     * Gets the ColorPtOstDot associated with this panel.
     * 
     * @return The associated ColorPtOstDot
     */
    public ColorPtOstDot getColorPtOstDot() {
        return theColorPtOstDot;
    } // end method getColorPtOstDot


    
    /**
     * Sets the ColorPtOstDot for this panel.
     * 
     * @param colorPtDot The new ColorPtOstDot to display
     */
    public void setColorPtOstDot(ColorPtOstDot colorPtDot) {
        this.theColorPtOstDot = colorPtDot;
        this.isOccupied = (colorPtDot != null);
        repaint();
    } // end method setColorPtOstDot



    
    /**
     * Sets whether this panel is hovered.
     * 
     * @param hovered The new hover state
     */
    public void setHovered(boolean hovered) {
        if (this.isHovered != hovered) {
            this.isHovered = hovered;
            repaint();
        }
    }
    
    /**
     * Sets whether this panel is selected.
     * 
     * @param selected The new selection state
     */
    public void setSelected(boolean selected) {
        if (this.isSelected != selected) {
            this.isSelected = selected;
            fireEvent(new OstdotEvent(this, theColorPtOstDot,
                selected ? OstdotEvent.OstdotEventType.SELECTED : OstdotEvent.OstdotEventType.DESELECTED));
            repaint();
        }
    } // end method setSelected




    public void toggleSelected() {
        setSelected(!isSelected);
    }
    
    /**
     * Sets whether to display colors in CMYK mode.
     * 
     * @param cmykMode True to display in CMYK mode, false for RGB
     */
    public void setCMYKMode(boolean cmykMode) {
        if (this.isCMYKMode != cmykMode) {
            this.isCMYKMode = cmykMode;
            repaint();
        }
    } // end method toggleSelected



    
    /**
     * Sets the background color for this panel.
     * 
     * @param color The new background color
     */
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        repaint();
    } // end method setBackgroundColor



    
    /**
     * Sets whether this color is currently in use in the palette.
     * This affects the visual display of the panel.
     *
     * @param inPalette true if the color is in the palette, false otherwise
     */
    public void setInPalette(boolean inPalette) {
        if (this.inPalette != inPalette) {
            this.inPalette = inPalette;
            repaint();
        }
    } // end method setInPalette




    /**
     * Returns whether this color is currently in use in the palette.
     *
     * @return true if the color is in the palette, false otherwise
     */
    public boolean isInPalette() {
        return inPalette;
    }
    // end method isInPalette




    /** // TODO 062825 getting called from where?? class MonotriDrawPanel method updateFromMaps, called from TemplateWorkFrame?? This was OK
     * Updates the panel's display state based on the current context.
     * This should be called whenever the panel's context changes (selection, occupation, etc).
     */
    public void updateDisplayState(OstdotDisplayState newState) { // TODO 062825 but still getting called from class OstdotPanelAssessState??
        if (this.currentDisplayState != newState) {
            this.currentDisplayState = newState;
            repaint();
        }
    } // end method updateDisplayState




    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (currentDisplayState) {
            case UNOCCUPIED:
                renderUnoccupied(g2);
                break;
            case OCCUPIED_UNSELECTED_IN_CURR_HUE:
                renderOccupiedUnselectedInCurrentHue(g2); // 070625 added call
                break;
            case OCCUPIED_UNSELECTED_OTHER_HUE:
                boolean dummyBoolean = false; // TODO 070625 added dummyBoolean - examine this
                renderOccupiedUnselectedOtherHue(g2, dummyBoolean); // 070625 added call
                break;
            case OCCUPIED_UNSELECTED_OTHER_HUE_SIMPLIFIED:
                renderSimpleOccupiedUnselectedOtherHue(g2, true);
                break;
            case OCCUPIED_SELECTED_IN_CURR_HUE:
                renderOccupiedSelectedInCurrentHue(g2, false);
                break;
            case OCCUPIED_SELECTED_IN_CURR_HUE_PLUS:
                renderOccupiedSelectedInCurrentHuePlus(g2, true);
                break;
            case OCCUPIED_SELECTED_OTHER_HUE:
                renderOccupiedSelectedOtherHue(g2);
                break;
        }

        g2.dispose();
    } // end method paintComponent




    private void renderUnoccupied(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();
        Shape circle = new Ellipse2D.Double(BORDER_WIDTH, BORDER_WIDTH, 
                                          w - 2*BORDER_WIDTH, h - 2*BORDER_WIDTH);

        // Draw background
        g2.setColor(backgroundColor);
        g2.fill(circle);

        // Draw color dot
        if (theColorPtOstDot != null) {
            g2.setColor(getDisplayColor());
            g2.fill(circle);
        }

        // Draw border
        g2.setStroke(new BasicStroke(BORDER_WIDTH));
        if (isSelected) {
            g2.setColor(Color.BLUE);
        } else if (isHovered) {
            g2.setColor(Color.BLUE);
        } else if (inPalette) {
            g2.setColor(Color.GREEN);
        } else {
            g2.setColor(Color.BLACK);
        }
        g2.draw(circle);
    }

    private void renderOccupiedUnselectedInCurrentHue(Graphics2D g2) { // TODO 070625 not called - fix this
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        setOpaque(true);

        int w = getWidth();
        int h = getHeight();

        // Step 1: Draw background rectangles
        Rectangle topRect = new Rectangle(0, 0, w, h/2);
        Rectangle btmRect = new Rectangle(0, h/2, w, h/2);

        g2.setColor(Color.GRAY);
        g2.fill(topRect);
        g2.setColor(Color.LIGHT_GRAY); // sorta "gray version" of selected
        g2.fill(btmRect);

        // Step 2: Draw main circle
        Shape circle = new Ellipse2D.Double(BORDER_WIDTH, BORDER_WIDTH,
                w - 2*BORDER_WIDTH, h - 2*BORDER_WIDTH);
        if (theColorPtOstDot != null) {
            g2.setColor(getDisplayColor());
            g2.fill(circle);
        }

        // Step 3: Draw small circle in upper-left corner
        MonotriWorkFrame frame = (MonotriWorkFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) {
            double mainRadius = (w - 2*BORDER_WIDTH) / 2.0; // radius of main circle
            double smallRadius = mainRadius * 0.4; // radius of small circle
            Shape smallCircle = new Ellipse2D.Double(0, 0, smallRadius*2, smallRadius*2);
            int hueNumCode = frame.getCurrHueNumCode();
            HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(hueNumCode+1);
            int hueDegrees360 = HueSpacingStdChoices.getSingleton().getHueDegrIntForHueNumGUI(someHueNumGUI);
            // TODO 062225 Use cleaner, better method through MonotriBook to create fill color for small circle
            HLSParamBundle pureHueParamBundle = new HLSParamBundle(hueDegrees360, 0.5, 1.0);
            Color smallCircleColor = MonotriBook.getInstance().getStandardColorByCodeHueNum(hueNumCode, 1).toAWTColor();
            g2.setColor(smallCircleColor);
            g2.fill(smallCircle);
        } // end IF
    } // end method renderOccupiedUnselectedInCurrentHue




    private void renderOccupiedUnselectedOtherHue(Graphics2D g2, boolean simplified) { // TODO 070625 not called - fix this
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        setOpaque(true);

        int w = getWidth();
        int h = getHeight();

        // Step 1: Draw background rectangles
        Rectangle topRect = new Rectangle(0, 0, w, h/2);
        Rectangle btmRect = new Rectangle(0, h/2, w, h/2);

        g2.setColor(Color.GRAY);
        g2.fill(topRect);
        g2.setColor(Color.LIGHT_GRAY); // 090425WAS "g2.setColor(Color.DARK_GRAY);"
        g2.fill(btmRect);

        // Step 2: Draw main circle
        Shape circle = new Ellipse2D.Double(BORDER_WIDTH, BORDER_WIDTH,
                w - 2*BORDER_WIDTH, h - 2*BORDER_WIDTH);
        if (theColorPtOstDot != null) {
            g2.setColor(getDisplayColor());
            g2.fill(circle);
        }

        // Step 3: Draw small GRAY circle in upper-RIGHT corner
        MonotriWorkFrame frame = (MonotriWorkFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) {
            double mainRadius = (w - 2*BORDER_WIDTH) / 2.0; // radius of main circle
            double smallRadius = mainRadius * 0.4; // radius of small circle
            Shape smallCircle = new Ellipse2D.Double(w-2*smallRadius, 0, smallRadius*2, smallRadius*2);

            Color smallCircleColor = Color.GRAY;
            g2.setColor(smallCircleColor);
            g2.fill(smallCircle);
        } // end IF
    } // end method renderOccupiedUnselectedOtherHue




    private void renderOccupiedSelectedInCurrentHue(Graphics2D g2, boolean hasOtherHues) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        setOpaque(true);

        int w = getWidth();
        int h = getHeight();

        // Step 1: Draw background rectangles
        Rectangle topRect = new Rectangle(0, 0, w, h/2);
        Rectangle btmRect = new Rectangle(0, h/2, w, h/2);
        
        g2.setColor(Color.BLACK);
        g2.fill(topRect);
        g2.setColor(Color.WHITE);
        g2.fill(btmRect);

        // Step 2: Draw main circle
        Shape circle = new Ellipse2D.Double(BORDER_WIDTH, BORDER_WIDTH, 
                                          w - 2*BORDER_WIDTH, h - 2*BORDER_WIDTH);
        if (theColorPtOstDot != null) {
            g2.setColor(getDisplayColor());
            g2.fill(circle);
        }

        // Step 3: Draw small circle in upper-left corner
        MonotriWorkFrame frame = (MonotriWorkFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) {
            double mainRadius = (w - 2*BORDER_WIDTH) / 2.0; // radius of main circle
            double smallRadius = mainRadius * 0.4; // radius of small circle
            Shape smallCircle = new Ellipse2D.Double(0, 0, smallRadius*2, smallRadius*2);
            int hueNumCode = frame.getCurrHueNumCode();
            HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(hueNumCode+1);
            int hueDegrees360 = HueSpacingStdChoices.getSingleton().getHueDegrIntForHueNumGUI(someHueNumGUI);
            // TODO 062225 Use cleaner, better method through MonotriBook to create fill color for small circle
            HLSParamBundle pureHueParamBundle = new HLSParamBundle(hueDegrees360, 0.5, 1.0);
            Color smallCircleColor = MonotriBook.getInstance().getStandardColorByCodeHueNum(hueNumCode, 1).toAWTColor();
            g2.setColor(smallCircleColor);
            g2.fill(smallCircle);
        } // end IF

    } // end method renderOccupiedSelectedInCurrentHue



    private void renderOccupiedSelectedOtherHue(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        setOpaque(true);

        int w = getWidth();
        int h = getHeight();

        // Step 1: Draw background rectangles
        Rectangle topRect = new Rectangle(0, 0, w, h/2);
        Rectangle btmRect = new Rectangle(0, h/2, w, h/2);

        g2.setColor(Color.BLACK);
        g2.fill(topRect);
        g2.setColor(Color.GRAY);
        g2.fill(btmRect);

        // Step 2: Draw main circle
        Shape circle = new Ellipse2D.Double(BORDER_WIDTH, BORDER_WIDTH,
                w - 2*BORDER_WIDTH, h - 2*BORDER_WIDTH);
        if (theColorPtOstDot != null) {
            g2.setColor(getDisplayColor());
            g2.fill(circle);
        }

        // Step 3: Draw small GRAY circle in upper-RIGHT corner
        MonotriWorkFrame frame = (MonotriWorkFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) {
            double mainRadius = (w - 2*BORDER_WIDTH) / 2.0; // radius of main circle
            double smallRadius = mainRadius * 0.4; // radius of small circle
            Shape smallCircle = new Ellipse2D.Double(w-2*smallRadius, 0, smallRadius*2, smallRadius*2);

            Color smallCircleColor = new Color(211, 211, 211);
            g2.setColor(smallCircleColor);
            g2.fill(smallCircle);
        } // end IF
    } // end method renderOccupiedSelectedOtherHue



    private void renderOccupiedSelectedInCurrentHuePlus(Graphics2D g2, boolean hasOtherHues) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        setOpaque(true);

        int w = getWidth();
        int h = getHeight();

        // Step 1: Draw background rectangles
        Rectangle topRect = new Rectangle(0, 0, w, h/2);
        Rectangle btmRect = new Rectangle(0, h/2, w, h/2);

        g2.setColor(Color.BLACK);
        g2.fill(topRect);
        g2.setColor(Color.WHITE);
        g2.fill(btmRect);

        // Step 2: Draw main circle
        Shape circle = new Ellipse2D.Double(BORDER_WIDTH, BORDER_WIDTH,
                w - 2*BORDER_WIDTH, h - 2*BORDER_WIDTH);
        if (theColorPtOstDot != null) {
            g2.setColor(getDisplayColor());
            g2.fill(circle);
        }

        // Step 3: Draw small circle in upper-LEFT corner - same as in renderOccupiedSelectedInCurrentHue
        MonotriWorkFrame frame = (MonotriWorkFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) {
            double mainRadius = (w - 2*BORDER_WIDTH) / 2.0; // radius of main circle
            double smallRadius = mainRadius * 0.4; // radius of small circle
            Shape smallCircle = new Ellipse2D.Double(0, 0, smallRadius*2, smallRadius*2);
            int hueNumCode = frame.getCurrHueNumCode();
            HueNumGUI someHueNumGUI = HueNumGUI.getHueNumGUIInstance(hueNumCode+1);
            int hueDegrees360 = HueSpacingStdChoices.getSingleton().getHueDegrIntForHueNumGUI(someHueNumGUI);
            // TODO 062225 Use cleaner, better method through MonotriBook to create fill color for small circle
            HLSParamBundle pureHueParamBundle = new HLSParamBundle(hueDegrees360, 0.5, 1.0);
            Color smallCircleColor = MonotriBook.getInstance().getStandardColorByCodeHueNum(hueNumCode, 1).toAWTColor();
            g2.setColor(smallCircleColor);
            g2.fill(smallCircle);
        } // end IF

        // Step 4: Draw small GRAY circle in upper-RIGHT corner - shows there are other hues
        // MonotriWorkFrame frame = (MonotriWorkFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) {
            double mainRadius = (w - 2*BORDER_WIDTH) / 2.0; // radius of main circle
            double smallRadius = mainRadius * 0.4; // radius of small circle
            Shape smallCircle = new Ellipse2D.Double(w-2*smallRadius, 0, smallRadius*2, smallRadius*2);

            Color smallCircleColor = Color.GRAY;
            g2.setColor(smallCircleColor);
            g2.fill(smallCircle);
        } // end IF

        // Draw CIRCLE OUTLINE ONLY
        g2.setStroke(new BasicStroke(BORDER_WIDTH));
        if (isSelected) {
            g2.setColor(Color.BLUE);
        } else if (isHovered) {
            g2.setColor(Color.BLUE);
        } else if (inPalette) {
            g2.setColor(Color.GREEN);
        } else {
            g2.setColor(Color.BLACK);
        }
        g2.draw(circle);
    } // end method renderOccupiedSelectedInCurrentHuePlus



    private void renderSimpleOccupiedUnselectedOtherHue(Graphics2D g2, boolean simplified) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        setOpaque(true);

        int w = getWidth();
        int h = getHeight();

        // Step 1: Draw background rectangle
        Rectangle fullRect = new Rectangle(0, 0, w, h);
        // g2.setStroke(new BasicStroke(BORDER_WIDTH)); // 090425 took out
        g2.setColor(Color.GRAY); // 090425 I guess this looks OK
        g2.fill(fullRect);  // Only fill the rectangle

        // Step 2: Draw main circle
        Shape circle = new Ellipse2D.Double(BORDER_WIDTH, BORDER_WIDTH,
                w - 2*BORDER_WIDTH, h - 2*BORDER_WIDTH);
        if (theColorPtOstDot != null) {
            g2.setColor(getDisplayColor());
            g2.fill(circle);
            g2.draw(circle);
        }
    } // end method renderSimpleOccupiedUnselectedOtherHue



    @Override
    public Dimension getPreferredSize() {
        return new Dimension(30, 30);
    }
    
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public boolean getIsOccupiedFromOstdot() {
        return this.isOccupied;
    }

    public boolean getIsSelectedFromOstdot() {
        return this.isSelected;
    }

    /**
     * Calculates the display state for a panel based on the current hue and position maps.
     * This is a helper method that can be called directly without an OstdotPanel instance - no usage?
     * 
     * @param currentHueNumCode The current hue number in internal format (0-35)
     * @param occupiedHues Array of hues that occupy this position, or null if not occupied
     * @param selectedHues Array of hues that are selected at this position, or null if not selected
     * @return The calculated display state
     */
    public static OstdotDisplayState calcDisplayStateFromArrays(
            int currentHueNumCode,
            int[] occupiedHues,
            int[] selectedHues) {

        // RS took out 020526 -console clutter
        /*
        System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromArrays - Current hue: " + currentHueNumCode + 
                           ", Occupied hues: " + (occupiedHues != null ? java.util.Arrays.toString(occupiedHues) : "null") + 
                           ", Selected hues: " + (selectedHues != null ? java.util.Arrays.toString(selectedHues) : "null"));

         */
        
        // Convert internal hue (0-35) to user-facing (1-36) for comparison with map values
        HueNumGUI currentHueNumGUI = HueNumGUI.getHueNumGUIInstance(currentHueNumCode + 1);
        int currentHueUserFacing = currentHueNumGUI.getUserFacingHueNum();
        
        // Check if position is selected in the current hue
        boolean selectedInCurrentHue = false;
        if (selectedHues != null) {
            for (int hue : selectedHues) {
                if (hue == currentHueUserFacing) {
                    selectedInCurrentHue = true;
                    break;
                }
            }
        }
        
        // Check if position is selected in any other hue
        boolean selectedInOtherHue = false;
        if (selectedHues != null) {
            for (int hue : selectedHues) {
                if (hue != currentHueUserFacing) {
                    selectedInOtherHue = true;
                    break;
                }
            }
        }
        
        // Check if position is occupied in the current hue
        boolean occupiedInCurrentHue = false;
        if (occupiedHues != null) {
            for (int hue : occupiedHues) {
                if (hue == currentHueUserFacing) {
                    occupiedInCurrentHue = true;
                    break;
                }
            }
        }
        
        // Determine the display state based on selection and occupation
        if (selectedInCurrentHue) {
            System.out.println("DEBUG: Position is selected in current hue " + currentHueUserFacing);
            if (selectedInOtherHue || (occupiedHues != null && occupiedHues.length > 1)) {
                return OstdotDisplayState.OCCUPIED_SELECTED_IN_CURR_HUE_PLUS;
            } else {
                return OstdotDisplayState.OCCUPIED_SELECTED_IN_CURR_HUE;
            }
        } else if (selectedInOtherHue) {
            System.out.println("DEBUG: Position is selected in other hue");
            return OstdotDisplayState.OCCUPIED_SELECTED_OTHER_HUE;
        } else if (occupiedInCurrentHue) {
            System.out.println("DEBUG: Position is occupied in current hue " + currentHueUserFacing);
            return OstdotDisplayState.OCCUPIED_UNSELECTED_IN_CURR_HUE;
        } else if (occupiedHues != null && occupiedHues.length > 0) {
            System.out.println("DEBUG: Position is occupied in other hue");
            return OstdotDisplayState.OCCUPIED_UNSELECTED_OTHER_HUE;
        } else {
            System.out.println("DEBUG: Position is empty");
            return OstdotDisplayState.UNOCCUPIED;
        }
    } // end method calcDisplayStateFromArrays



    
    /**
     * Helper method to check if an array of hues contains a specific hue
     */
    private static boolean containsHue(int[] hues, int targetHue) {
        if (hues == null) {
            return false;
        }
        for (int hue : hues) {
            if (hue == targetHue) {
                return true;
            }
        }
        return false;
    } // end method containsHue




    /**
     * Calculates the display state for a panel based on maps of occupied and selected positions.
     * This method is useful when working with the full maps from DrawObjRoster.
     * 
     * @param position The position to calculate the state for
     * @param currHueNumGUI The MonotriWorkFrame's current hue number in user-facing format (1-36)
     * @param occupiedMap Map of positions to arrays of hue numbers that occupy them
     * @param selectedMap Map of positions to arrays of hue numbers that are selected at them
     * @param simplifiedView Whether to use simplified view mode
     * @return The calculated display state
     */
    public static OstdotDisplayState calcDisplayStateFromMaps(
            OstPosition position,
            int currHueNumGUI,
            Map<OstPosition, int[]> occupiedMap,
            Map<OstPosition, int[]> selectedMap,
            boolean simplifiedView) {

        // 072125 added for better clarity in CONSOLE DEBUG print-out
        // boolean instancePositionNum = occupiedMap.containsKey(position);
        if(position.getPositionNum() == 1) {
            System.out.println("   ");
            // RS took out 020526 -console clutter
            // System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps now GOES THRU ALL 78 POSITIONS for DISPLAY STATE");
        } // end IF
        
        // Debug the inputs
        // RS took out 020526 -console clutter
        /*
        System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position: " + position +
                           ", Current hue: " + currHueNumGUI +
                           ", Occupied map size: " + (occupiedMap != null ? occupiedMap.size() : "null") + 
                           ", Selected map size: " + (selectedMap != null ? selectedMap.size() : "null") + 
                           ", Simplified view: " + simplifiedView);

         */
        
        // Check if position is occupied
        if (occupiedMap == null || !occupiedMap.containsKey(position)) {
            // RS took out 020526 -console clutter
            // System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position " + position + " is UNOCCUPIED");
            return OstdotDisplayState.UNOCCUPIED;
        }
        
        // Get the hues for this position
        int[] occupiedHues = occupiedMap.get(position);
        // RS took out 020526 -console clutter
        /*
        System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position " + position + 
                          " has occupied hues: " + java.util.Arrays.toString(occupiedHues));

         */
        
        // Check if the position has the current hue
        boolean hasCurrentHue = containsHue(occupiedHues, currHueNumGUI);
        // RS took out 020526 -console clutter
        /*
        System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position " + position + 
                          " has current hue " + currHueNumGUI + "? " + hasCurrentHue);

         */
        
        // Check if position is selected
        boolean isSelected = selectedMap != null && selectedMap.containsKey(position);
        // RS took out 020526 -console clutter
        /*
        System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position " + position + 
                          " is selected? " + isSelected);

         */
        
        // If the position is selected, check if it's selected in the current hue
        if (isSelected) {
            int[] selectedHues = selectedMap.get(position);
            // RS took out 020526 -console clutter
            /*
            System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position " + position + 
                              " selected hues: " + java.util.Arrays.toString(selectedHues));

             */
            
            boolean isSelectedWithCurrentHue = containsHue(selectedHues, currHueNumGUI);
            // RS took out 020526 -console clutter
            /*
            System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position " + position + 
                              " selected with current hue? " + isSelectedWithCurrentHue);

             */
            
            // If the position is selected with the current hue, show it as selected in current hue
            if (isSelectedWithCurrentHue) {
                // Check if it's also selected or occupied in other hues
                boolean positionIsSelectedInOtherHues = false;
                for (int hue : selectedHues) {
                    if (hue != currHueNumGUI) {
                        positionIsSelectedInOtherHues = true;
                        break;
                    }
                }
                
                if (!positionIsSelectedInOtherHues && occupiedHues.length > 1) {
                    for (int hue : occupiedHues) {
                        if (hue != currHueNumGUI) {
                            positionIsSelectedInOtherHues = true;
                            break;
                        }
                    }
                }
                
                if (positionIsSelectedInOtherHues) {
                    // RS took out 020526 -console clutter
                    /*
                    System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position " + position + 
                                      " RESULT: OCCUPIED_SELECTED_IN_CURR_HUE_PLUS");

                     */
                    return OstdotDisplayState.OCCUPIED_SELECTED_IN_CURR_HUE_PLUS;
                } else {
                    // RS took out 020526 -console clutter
                    /*
                    System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position " + position + 
                                      " RESULT: OCCUPIED_SELECTED_IN_CURR_HUE");

                     */
                    return OstdotDisplayState.OCCUPIED_SELECTED_IN_CURR_HUE;
                }
            } else { // end IF (isSelectedWithCurrentHue)
                // Selected in other hue
                // RS took out 020526 -console clutter
                /*
                System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position " + position + 
                                  " RESULT: OCCUPIED_SELECTED_OTHER_HUE");

                 */
                return OstdotDisplayState.OCCUPIED_SELECTED_OTHER_HUE;
            }
        }
        
        // If the position has the current hue but is not selected
        if (hasCurrentHue) {
            // RS took out 020526 -console clutter
            /*
            System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position " + position + 
                              " RESULT: OCCUPIED_UNSELECTED_IN_CURR_HUE");

             */
            return OstdotDisplayState.OCCUPIED_UNSELECTED_IN_CURR_HUE;
        }
        
        // Position is occupied in other hues but not in current hue and not selected
        if (simplifiedView) {
            // RS took out 020526 -console clutter
            /*
            System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position " + position + 
                              " RESULT: OCCUPIED_UNSELECTED_OTHER_HUE_SIMPLIFIED");

             */
            return OstdotDisplayState.OCCUPIED_UNSELECTED_OTHER_HUE_SIMPLIFIED;
        } else {
            // RS took out 020526 -console clutter
            /*
            System.out.println("DEBUG: OstdotPanel.calcDisplayStateFromMaps - Position " + position + 
                              " RESULT: OCCUPIED_UNSELECTED_OTHER_HUE");

             */
            return OstdotDisplayState.OCCUPIED_UNSELECTED_OTHER_HUE;
        }
    } // end method calcDisplayStateFromMaps




    public void setColorMode(ColorDisplayMode mode) {
        if (this.colorMode != mode) {
            this.colorMode = mode;
            repaint();
        }
    } // end method setColorMode




    protected Color getDisplayColor() {
        if (theColorPtOstDot == null) {
            return Color.WHITE;
        }
        return (colorMode == ColorDisplayMode.CMYK)
                ? theColorPtOstDot.toCMYKColor()
                : theColorPtOstDot.toAWTColor();
    } // end method getDisplayColor



} // end class OstdotPanel
