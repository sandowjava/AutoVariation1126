package com.csurf.templatecore.view;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.csurf.ostwald.domainmonotri.view.MonotriWorkFrame;
import com.csurf.templatecore.model.PaletteWShapeNames;
import com.csurf.templatecore.model.ShapeColorPair;

/**
 * Controller for managing color chip selection and interaction in PaletteColorSourceFrame.
 * 
 * This controller implements the MVC pattern by:
 * - Receiving mouse position events from the view (PaletteColorSourceFrame)
 * - Calculating which color chip was selected based on position
 * - Looking up the corresponding ShapeColorPair in the model (PaletteWShapeNames)
 * - Managing selection state (selectedIndex, lastSelectedPair)
 * - Invoking callbacks to notify listeners of selection
 * - Instructing the view to update visual feedback
 * 
 * The controller uses an index-based approach where mouse position is converted to
 * a grid index, which is then used to look up data in the model. This keeps the view
 * "dumb" (no business logic) and centralizes all interaction logic in the controller.
 */
public class ColorchipSourceController {
    
    private final PaletteWShapeNames paletteWShapeNames;
    private final ShapesPaletteColorSourceFrame view;
    private IColorChipSelectionListener selectionListener;
    
    private int selectedIndex = -1;
    private ShapeColorPair lastSelectedPair = null;
    
    // Layout constants (must match PaletteColorSourceFrame)
    private static final int COLORS_PER_ROW = 5;
    private static final int CHIP_SIZE = 26;
    private static final int CHIP_GAP = 5;
    private static final int CHIP_ROW_HEIGHT = CHIP_SIZE + CHIP_GAP;  // 31px
    
    /**
     * Creates a new ColorchipSourceController.
     * 
     * @param paletteWShapeNames The model containing color-string pairs
     * @param view The view (PaletteColorSourceFrame) to update
     * @param selectionListener The callback listener for selection events (can be null)
     */
    public ColorchipSourceController(PaletteWShapeNames paletteWShapeNames, 
                                     ShapesPaletteColorSourceFrame view,
                                     IColorChipSelectionListener selectionListener) {
        this.paletteWShapeNames = paletteWShapeNames;
        this.view = view;
        this.selectionListener = selectionListener;
    }
    
    /**
     * Handles a mouse click on the color chip grid.
     * 
     * Converts the mouse position to a grid index, validates it, looks up the
     * corresponding ShapeColorPair, updates state, and invokes callbacks.
     * 
     * If the leftCheckboxSend is checked and exactly one DrawObj is selected,
     * sends the selected color to that DrawObj via the PaletteOperationHub.
     * 
     * @param x The x-coordinate of the mouse click (relative to chip grid panel)
     * @param y The y-coordinate of the mouse click (relative to chip grid panel)
     */
    public void handleChipLeftClick(int x, int y) {
        // Calculate grid position
        int col = x / CHIP_ROW_HEIGHT;
        int row = y / CHIP_ROW_HEIGHT;
        
        // Calculate index
        int index = row * COLORS_PER_ROW + col;
        
        // Validate index
        if (index < 0 || index >= paletteWShapeNames.getSize()) {
            // Click was outside valid chip area
            return;
        }
        
        // Look up the ShapeColorPair
        ShapeColorPair pair = paletteWShapeNames.getPairs().get(index);
        
        // Extract the ColorPtOstDot from the pair
        ColorPtOstDot color = pair.getColor();
        
        // Update state
        selectedIndex = index;
        lastSelectedPair = pair;
        
        // Notify view to update visual feedback
        view.setSelectedIndex(index);
        
        // Update status bar
        view.updateStatusBarForLeftClick(pair, index);
        
        // Check if leftCheckboxSend is enabled
        if (view.getLeftCheckboxSend().isSelected()) {
            // Get the source TemplateWorkFrame
            TemplateWorkFrame templateWorkFrame = view.getSourceTemplateWorkFrame();
            
            // Only proceed if we have a valid template reference
            if (templateWorkFrame != null) {
                // Get the template from the TemplateWorkFrame
                com.csurf.templatecore.model.Template template = templateWorkFrame.getCurrTemplate();
                
                // Check if exactly one DrawObj is selected
                if (template != null && template.onlyOneDrawobjSelected()) {
                    // Get the PaletteOperationHub from the TemplateWorkFrame
                    com.csurf.templatecore.controller.PaletteOperationHub hub = 
                        templateWorkFrame.getPaletteOperationHub();
                    
                    // Send the color to the selected DrawObj
                    if (hub != null) {
                        hub.shiftOneDrawobjDirectOstdot(color);
                    }
                } else {
                    // Show error message: exactly one shape must be selected
                    // Still update with the pair info, but the message will be overridden by the error
                    view.updateStatusBarForLeftClick(pair, index);
                }
            }
        }
        
        // Invoke callback listener
        if (selectionListener != null) {
            selectionListener.onColorChipSelected(pair, index);
        }
    }
    
    /**
     * Handles a right-click on the color chip grid.
     * 
     * If the rightCheckboxMonotri is checked, opens a MonotriWorkFrame for the clicked color's hue.
     * Otherwise, displays the color properties in the status bar.
     * 
     * @param x The x-coordinate of the right-click (relative to chip grid panel)
     * @param y The y-coordinate of the right-click (relative to chip grid panel)
     */
    public void handleChipRightClick(int x, int y) {
        // Calculate grid position
        int col = x / CHIP_ROW_HEIGHT;
        int row = y / CHIP_ROW_HEIGHT;
        
        // Calculate index
        int index = row * COLORS_PER_ROW + col;
        
        // Validate index
        if (index < 0 || index >= paletteWShapeNames.getSize()) {
            // Click was outside valid chip area
            return;
        }
        
        // Look up the ShapeColorPair
        ShapeColorPair pair = paletteWShapeNames.getPairs().get(index);
        
        // Extract the ColorPtOstDot from the pair
        ColorPtOstDot color = pair.getColor();
        
        // Check if rightCheckboxMonotri is enabled
        if (view.getRightCheckboxMonotri().isSelected()) {
            // Open MonotriWorkFrame for this color's hue
            TemplateWorkFrame templateWorkFrame = view.getSourceTemplateWorkFrame();
            
            // Only proceed if we have a valid template reference
            if (templateWorkFrame != null) {
                // Get HueNumGUI directly from the color
                HueNumGUI hueNumGUI = color.getHueNumGUI();
                
                // Create and display MonotriWorkFrame
                MonotriWorkFrame monotriWorkFrame = new MonotriWorkFrame(
                    templateWorkFrame, 
                    hueNumGUI, 
                    color);
                monotriWorkFrame.setVisible(true);
            }
        } else {
            // Normal right-click behavior: update status bar with color properties
            view.updateStatusBarForRightClick(color);
        }
    }
    
    /**
     * Generates tooltip text for a color chip at the given mouse position.
     * 
     * Converts the mouse position to a grid index, validates it, looks up the
     * corresponding ShapeColorPair, and returns formatted tooltip text.
     * 
     * @param x The x-coordinate of the mouse position (relative to chip grid panel)
     * @param y The y-coordinate of the mouse position (relative to chip grid panel)
     * @return The formatted tooltip text, or null if mouse is outside valid chip area
     */
    public String getTooltipForChip(int x, int y) {
        // Calculate grid position
        int col = x / CHIP_ROW_HEIGHT;
        int row = y / CHIP_ROW_HEIGHT;
        
        // Calculate index
        int index = row * COLORS_PER_ROW + col;
        
        // Validate index
        if (index < 0 || index >= paletteWShapeNames.getSize()) {
            // Mouse is outside valid chip area
            return null;
        }
        
        // Look up the ShapeColorPair
        ShapeColorPair pair = paletteWShapeNames.getPairs().get(index);
        
        // Return formatted tooltip text (shape name and index, omitting total count)
        return view.getTooltipText(pair, index);
    }
    
    /**
     * Sets the selection listener for color chip selection events.
     * 
     * @param listener The listener to invoke on selection (can be null)
     */
    public void setSelectionListener(IColorChipSelectionListener listener) {
        this.selectionListener = listener;
    }
    
    /**
     * Gets the currently selected index.
     * 
     * @return The 0-based index of the selected chip, or -1 if no chip is selected
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    /**
     * Gets the last selected ShapeColorPair.
     * 
     * @return The ShapeColorPair that was last selected, or null if no chip has been selected
     */
    public ShapeColorPair getLastSelectedPair() {
        return lastSelectedPair;
    }
}
