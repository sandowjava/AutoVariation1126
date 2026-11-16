package com.csurf.ostwald.domainmonotri.controller;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.ostwald.domainmonotri.view.MonotriDrawPanel;
import com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame;
import com.csurf.ostwald.domainmonotri.view.OstdotPanel;
import com.csurf.templatecore.model.ShiftSpecify;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.service.IColorOperationService;
import com.csurf.templatecore.view.TemplateWorkFrame;

public class MouseListenerDotDrag extends MouseAdapter implements MouseMotionListener {
    private MonotriWorkFrame theMonoTriDisplayJFrame;
    private int mousePressDotNum;
    private int mouseReleaseDotNum;
    private int lastHoveredDotNum = -1;  // For caching to prevent excessive updates

    public MouseListenerDotDrag(MonotriWorkFrame theMonoTriDisplayJFrame) {
        this.theMonoTriDisplayJFrame = theMonoTriDisplayJFrame;
    } // end constructor




    @Override
    public void mouseClicked(MouseEvent e) {
        int whichDotNum = findWhichDotNum(e);
        saySomething("Mouse clicked", e);

        int hueNum = getMonotriCurrHue();
        // hackToHiliteDragHandle(hueNum, whichDotNum); // TODO 060425 implement method "hackToHiliteDragHandle"
        // hackForCMYKDisplay(hueNum, whichDotNum); // TODO 060425 implement method "hackForCMYKDisplay"

    } // end method mouseClicked




    private int getMonotriCurrHue() {
        return this.theMonoTriDisplayJFrame.getCurrHueNumCode();
    } // end method getMonotriCurrHue




    // for click and drag get the start point when pressed
    @Override
    public void mousePressed(MouseEvent e) {
        mousePressDotNum = findWhichDotNum(e);
        
        // Get the MonotriDrawPanel from the MonotriWorkFrame
        MonotriDrawPanel drawPanel = theMonoTriDisplayJFrame.getDrawPanel();
        
        // Convert mouse coordinates to be relative to the draw panel if needed
        Point mousePoint;
        if (e.getComponent() == drawPanel) {
            mousePoint = e.getPoint();
        } else {
            mousePoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), drawPanel);
        }
        
        System.out.println("=== MOUSE PRESSED ===");
        System.out.println("Mouse position: (" + mousePoint.x + "," + mousePoint.y + ")");
        System.out.println("Detected dot number: " + mousePressDotNum);
        
        String theMessage = "Mouse pressed on MonoTriDisplay, dotNum = " + mousePressDotNum;
        saySomething(theMessage, e);
    } // end method mousePressed




    // for click and drag get the end point when released
    @Override
    public void mouseReleased(MouseEvent e) {
        // Diagnostic: Print what component the event is coming from
        System.out.println("=== MOUSE RELEASED EVENT ===");
        System.out.println("Event source component: " + e.getComponent().getClass().getSimpleName());
        System.out.println("Event coordinates: (" + e.getX() + "," + e.getY() + ")");
        
        mouseReleaseDotNum = findWhichDotNum(e);
        
        // Get the MonotriDrawPanel from the MonotriWorkFrame
        MonotriDrawPanel drawPanel = theMonoTriDisplayJFrame.getDrawPanel();
        
        // Convert mouse coordinates to be relative to the draw panel if needed
        Point mousePoint;
        if (e.getComponent() == drawPanel) {
            mousePoint = e.getPoint();
        } else {
            mousePoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), drawPanel);
        }
        
        System.out.println("=== MOUSE RELEASED ===");
        System.out.println("Mouse position: (" + mousePoint.x + "," + mousePoint.y + ")");
        System.out.println("Detected dot number: " + mouseReleaseDotNum);
        
        if (mousePressDotNum > 0 && mouseReleaseDotNum > 0) {
            System.out.println("=== DRAG DETECTED ===");
            System.out.println("From dot: " + mousePressDotNum + " to dot: " + mouseReleaseDotNum);
            
            // Calculate the shift in W-series and B-series
            int[] shiftValues = getShiftInWseriesBseries();
            int shiftW = shiftValues[0];
            int shiftB = shiftValues[1];
            
            // Get the current template
            TemplateWorkFrame templateWorkFrame = theMonoTriDisplayJFrame.getTemplateWorkFrame();
            if (templateWorkFrame != null) {
                // Create a ShiftSpecify object with the calculated shifts
                ShiftSpecify shiftSpecify = new ShiftSpecify(shiftW, shiftB);
                
                // Delegate to color operation service to handle the color shift
                // The event system will handle all UI updates automatically
                IColorOperationService colorService = templateWorkFrame.getColorOperationService();
                colorService.shiftSelectedForDragShift(shiftSpecify);
                
                System.out.println("Applied color shift: W=" + shiftW + ", B=" + shiftB);
            }
        }
        
        String theMessage = "Mouse released on MonoTriDisplay, dotNum = " + mouseReleaseDotNum;
        saySomething(theMessage, e);
    } // end method mouseReleased




    @Override
    public void mouseEntered(MouseEvent e) {
        saySomething("Mouse entered MonoTriDisplay", e);
    } // end method mouseEntered

    @Override
    public void mouseExited(MouseEvent e) {
        saySomething("Mouse exited MonoTriDisplay", e);
        // Clear hover info when mouse exits the panel
        theMonoTriDisplayJFrame.clearHoverInfo();
        lastHoveredDotNum = -1;
    } // end method mouseExited


    @Override
    public void mouseMoved(MouseEvent e) {
        // Diagnostic: Print what component the event is coming from
        System.out.println("=== MOUSE MOVED EVENT ===");
        System.out.println("Event source component: " + e.getComponent().getClass().getSimpleName());
        System.out.println("Event coordinates: (" + e.getX() + "," + e.getY() + ")");
        
        // Adjust coordinates by reducing x and y by "someArbitraryAmt" pixels (works in range of someArbitraryAmt 5-10)
        int someArbitraryAmt = 10; // 020526 cursor must be at exact bottom center of ostDotPanel, i.e., six o'clock position
        int adjustedX = e.getX() - someArbitraryAmt;
        int adjustedY = e.getY() - someArbitraryAmt;
        
        // Create a new MouseEvent with adjusted coordinates
        MouseEvent adjustedEvent = new MouseEvent(
            e.getComponent(),
            e.getID(),
            e.getWhen(),
            e.getModifiersEx(),
            adjustedX,
            adjustedY,
            e.getClickCount(),
            e.isPopupTrigger(),
            e.getButton()
        );
        
        int whichDotNum = findWhichDotNum(adjustedEvent);
        
        // Only update if dot number changed (caching for performance).
        // The dot-pixel hover display is handled by OstdotPanel enter/exit
        // events; this listener only sees gap pixels (dot panels consume
        // their own mouse events), so its remaining job here is to cancel a
        // pending hover commit when the pointer moves between areas.
        if (whichDotNum != lastHoveredDotNum) {
            lastHoveredDotNum = whichDotNum;
            theMonoTriDisplayJFrame.clearHoverInfo();
        }
    } // end method mouseMoved





    @Override
    public void mouseDragged(MouseEvent e) {
        // Clear hover info during drag operation
        theMonoTriDisplayJFrame.clearHoverInfo();
        lastHoveredDotNum = -1;
    } // end method mouseDragged


    private int findWhichDotNum(MouseEvent e) {
        // Get the MonotriDrawPanel from the MonotriWorkFrame
        MonotriDrawPanel drawPanel = theMonoTriDisplayJFrame.getDrawPanel();

        // Get the MonotriRenderContrlr from the MonotriDrawPanel
        MonotriRenderContrlr controller = drawPanel.getController();

        // Get the list of OstdotPanels from the controller
        List<OstdotPanel> ostDotPanels = controller.getOstDotPanels();

        // Convert mouse coordinates to drawPanel's local coordinate system
        // SwingUtilities.convertPoint converts from source component to target component
        // accounting for all parent-child relationships
        Point mousePoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), drawPanel);
        
        System.out.println("=== COORDINATE CONVERSION DEBUG ===");
        System.out.println("Event from " + e.getComponent().getClass().getSimpleName());
        System.out.println("Original point (in event source coords): " + e.getPoint());
        System.out.println("DrawPanel location in frame: " + drawPanel.getLocation());
        System.out.println("DrawPanel bounds: " + drawPanel.getBounds());
        System.out.println("Converted point (drawPanel local coords): " + mousePoint);
        
        // Manual verification of coordinate conversion
        System.out.println("--- Manual Conversion Verification ---");
        System.out.println("Event source: " + e.getComponent().getClass().getSimpleName());
        System.out.println("Event source location: " + e.getComponent().getLocationOnScreen());
        System.out.println("DrawPanel location on screen: " + drawPanel.getLocationOnScreen());
        System.out.println("Expected Y offset: " + (e.getComponent().getLocationOnScreen().y - drawPanel.getLocationOnScreen().y));
        System.out.println("Actual converted Y: " + mousePoint.y);
        System.out.println("Expected converted Y: " + (e.getPoint().y - (drawPanel.getLocationOnScreen().y - e.getComponent().getLocationOnScreen().y)));

        System.out.println("Mouse event at point: " + mousePoint.x + "," + mousePoint.y);
        System.out.println("Number of panels to check: " + ostDotPanels.size());

        // Debug: Print bounds of int debugSomeNum panels
        int debugSomeNum = 5;
        System.out.println("=== DEBUG: Panel Bounds for first " + debugSomeNum + " panels ===");
        for (int i = 0; i < Math.min(debugSomeNum, ostDotPanels.size()); i++) {  // Print first 5 panels for debugging
            OstdotPanel panel = ostDotPanels.get(i);
            Rectangle bounds = panel.getBounds();
            ColorPtOstDot color = panel.getColorPtOstDot();
            int fillOrderNum = color != null ? color.getFillOrderNum() : -1;
            System.out.println("Panel " + i + ": Bounds=" + bounds + ", FillOrderNum=" + fillOrderNum);
        }
        
        // Iterate through panels to find which one contains the mouse point
        for (OstdotPanel someOstDotPanel : ostDotPanels) {
            Rectangle bounds = someOstDotPanel.getBounds();
            
            // Check if the panel contains the point
            if (bounds.contains(mousePoint)) {
                // Found the panel containing the mouse point
                ColorPtOstDot color = someOstDotPanel.getColorPtOstDot();
                if (color != null) {
                    int fillOrderNum = color.getFillOrderNum();
                    System.out.println("Found dot at position: " + fillOrderNum);
                    return fillOrderNum;
                } else {
                    System.out.println("Panel found but has null ColorPtOstDot");
                }
            }
        }

        System.out.println("No dot found at this position");
        // If no panel contains the point, return a default value
        // -1 indicates "no panel found"
        return -1;
    } // end method findWhichDotNum




    private int[] getShiftInWseriesBseries() {
        int startDotNum = this.mousePressDotNum;
        int endDotNum = this.mouseReleaseDotNum;
        int[] seriesWseriesBChange = new int[2]; // TODO 080125 method returns seriesWseriesBChange = {changeInWseries, changeInBseries}
        int currHue = getMonotriCurrHue();

        ColorPtOstDot startDot = MonotriBook.getInstance().getStandardColorByCodeHueNum(currHue, startDotNum);
        ColorPtOstDot endDot = MonotriBook.getInstance().getStandardColorByCodeHueNum(currHue, endDotNum);

        int startWseries = startDot.getOstHCS().getSeriesW();
        int startBseries = startDot.getOstHCS().getSeriesB();

        int endWseries = endDot.getOstHCS().getSeriesW();
        int endBseries = endDot.getOstHCS().getSeriesB();

        int changeInWseries = endWseries - startWseries;
        int changeInBseries = endBseries - startBseries;
        seriesWseriesBChange[0] = changeInWseries;
        seriesWseriesBChange[1] = changeInBseries;

        int newDotWseries = startWseries + changeInWseries;
        int newDotBseries = startBseries + changeInBseries;

        // TODO 080125 method already knows "resultDotNum" which is new position for ostDot used as "handle" for SelectionGroup
        int resultDotNum = ColorPtOstDot.getFillOrderNumFromBWSeries(newDotWseries, newDotBseries);

        System.out.println("Method -testResultOfMouseDrag- gives for mouseDrag: changeInWseries= " + changeInWseries
                + ", changeInBseries= " + changeInBseries + ", which results in new dotNum= " + resultDotNum);
        System.out.println("Method -testResultOfMouseDrag- new dotNum= " + resultDotNum + " shouldEQUAL " + endDotNum);
        System.out.println(" ");

        // TODO 080125 method returns int array shift to be used by each position in SelectionGroup
        return seriesWseriesBChange; // TODO 080125 method taken from method "method kickOffMouseDragChangeFlow"
    } // end method getShiftInWseriesBseries




    // here is the method that gets called by various mouse events - not required for MouseListener interface
    public void saySomething(String eventDescription, MouseEvent e) {
        System.out.println("Inside MouseListenerMonoTriDotDrag " + eventDescription);
    } // end method saySomething

    /**
     * Test method to verify the dot detection logic.
     * This method simulates mouse events at various positions and prints the detected dot numbers.
     */
    public void testDotDetection() {
        MonotriDrawPanel drawPanel = theMonoTriDisplayJFrame.getDrawPanel();
        int panelWidth = drawPanel.getWidth();
        int panelHeight = drawPanel.getHeight();
        
        System.out.println("=== TESTING DOT DETECTION ===");
        System.out.println("Panel dimensions: " + panelWidth + "x" + panelHeight);
        
        // Create a map to store positions and their corresponding dot numbers
        Map<Point, Integer> dotPositions = new HashMap<>();
        
        // Test a grid of points across the panel
        for (int x = 0; x < panelWidth; x += 20) {
            for (int y = 0; y < panelHeight; y += 20) {
                // Create a dummy MouseEvent at this position
                // Note: We're creating the event with the drawPanel as the source component
                // and setting the x,y coordinates directly relative to the drawPanel
                MouseEvent dummyEvent = new MouseEvent(
                    drawPanel, // source
                    MouseEvent.MOUSE_PRESSED, // id
                    System.currentTimeMillis(), // when
                    0, // modifiers
                    x, y, // x, y
                    1, // clickCount
                    false // popupTrigger
                );
                
                // Use our findWhichDotNum method directly with the simulated event
                int dotNum = findWhichDotNum(dummyEvent);
                if (dotNum > 0) {
                    dotPositions.put(new Point(x, y), dotNum);
                    System.out.println("Position (" + x + "," + y + ") -> Dot #" + dotNum);
                }
            }
        }
        
        System.out.println("Found " + dotPositions.size() + " dot positions");
        System.out.println("=== TEST COMPLETE ===");
    }

    /* // 073025 took out after creating - realized it's just a dupe method
    private int[] getChangeInWseriesBseries() {
        int startDotNum = this.lastMousePressDotNum;
        int endDotNum = this.lastMouseReleaseDotNum;
        int[] seriesBWDragChange = new int[2];

        int startWseries = OstHueColumnStep3D.getBWSeriesPerDotCellNum(startDotNum)[0];
        int startBseries = OstHueColumnStep3D.getBWSeriesPerDotCellNum(startDotNum)[1];

        int endWseries = OstHueColumnStep3D.getBWSeriesPerDotCellNum(endDotNum)[0];
        int endBseries = OstHueColumnStep3D.getBWSeriesPerDotCellNum(endDotNum)[1];

        int changeInWseries = endWseries - startWseries;
        int changeInBseries = endBseries - startBseries;
        seriesBWDragChange[0] = changeInWseries;
        seriesBWDragChange[1] = changeInBseries;

        int newDotWseries = startWseries + changeInWseries;
        int newDotBseries = startBseries + changeInBseries;
        // int resultDotNum = ColorPtOstDot.getFillOrderNumFromBWSeries(newDotWseries, newDotBseries);

        return seriesBWDragChange;
    } // end method getChangeInWseriesBseries
     */

    /////////////////////////////////////////////////////////////////////////////////////////
    /// INNER CLASS
    /////////////////////////////////////////////////////////////////////////////////////////

    private class OstDotShapeArray {
        public OstDotShapeArray(int dotSize) {
        }

        public int findWhichOstDotShape(int x, int y) {
            return 39; // TODO 060425 this is a dummy return
        } // end method findWhichOstDotShape
    } // end inner class OstDotShapeArray

} // end class MouseListenerDotDrag
