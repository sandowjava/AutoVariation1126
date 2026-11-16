package com.csurf.templatecore.view;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.PaletteWName;
import com.csurf.templatecore.model.ShapeColorPair;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * SimplePaletteColorSourceFrame displays a color palette as a grid of interactive color chips.
 * 
 * This frame shows colors from a PaletteWName instance (palette without shape names) in a grid layout
 * (5 colors per row). Each color chip is interactive, allowing users to select colors for application
 * to shapes in a template.
 * 
 * The window is non-modal and can be resized by the user. Multiple instances can be open
 * simultaneously, each displaying a different palette.
 * 
 * Architecture:
 * - Model: PaletteWName (colors without shape names)
 * - View: SimplePaletteColorSourceFrame (renders UI, forwards events)
 * - Controller: SimplePaletteColorSourceController (handles interaction logic)
 * 
 * Purpose: Allow users to view and select colors from a saved palette (typically created in the
 * Mosaic module), with color information displayed in the status bar.
 */
public class SimplePaletteColorSourceFrame extends JFrame {
    
    private static final long serialVersionUID = 1L;
    
    // UI Constants
    private static final int COLORS_PER_ROW = 5;
    private static final int CHIP_SIZE = 26;
    private static final int CHIP_GAP = 5;
    private static final int CHIP_ROW_HEIGHT = CHIP_SIZE + CHIP_GAP;  // 31px
    private static final int ROW_NUMBER_PANEL_WIDTH = 15;
    private static final int windowWidth = 205;
    private static final int INITIAL_HEIGHT = 350;
    private static final int MIN_HEIGHT = 200;
    private static final int STATUS_MSG_PTSIZE_SM = 8;
    private static final int STATUS_MSG_PTSIZE_LG = 12;
    
    // Data
    private final PaletteWName paletteWName;
    private final String paletteName;
    private static int instanceCounter = 1;
    
    // Controller
    private SimplePaletteColorSourceController controller;
    
    // UI Components
    private int originalHeight;
    private boolean isMinimized = false;
    private JButton toggleButton;
    private JPanel statusBar;
    private JTextField statusTextField;
    private ColorChipGridPanel chipGridPanel;
    private JCheckBox leftCheckboxSend;
    private JCheckBox rightCheckboxMonotri;
    private JPanel controlPanelMouseClicks;
    
    // Model and Controller
    private TemplateWorkFrame sourceTemplateWorkFrame;
    
    // State
    private int selectedIndex = -1;
    private ShapeColorPair lastSelectedPair = null;
    
    /**
     * Creates a new SimplePaletteColorSourceFrame to display the given palette.
     * 
     * @param paletteWName The PaletteWName instance to display
     * @param paletteName Name of the palette (for window title)
     * @param selectionListener Optional callback listener for color chip selection
     * @param sourceTemplateWorkFrame The TemplateWorkFrame that created this palette frame
     */
    public SimplePaletteColorSourceFrame(PaletteWName paletteWName,
                                         String paletteName,
                                         IColorChipSelectionListener selectionListener,
                                         TemplateWorkFrame sourceTemplateWorkFrame) {
        this.paletteWName = paletteWName;
        this.paletteName = paletteName;
        this.sourceTemplateWorkFrame = sourceTemplateWorkFrame;
        
        // Configure window
        setTitle("SimplPalet " + paletteName);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(windowWidth, INITIAL_HEIGHT);
        setMinimumSize(new Dimension(windowWidth, MIN_HEIGHT));
        setMaximumSize(new Dimension(windowWidth, Integer.MAX_VALUE));
        setResizable(true);
        setLocationRelativeTo(null);
        
        // Override the default minimize behavior
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowIconified(java.awt.event.WindowEvent e) {
                setExtendedState(JFrame.NORMAL);
                toggleMinimized();
            }
        });
        
        // Create controller
        this.controller = new SimplePaletteColorSourceController(paletteWName, this, selectionListener);
        
        // Build UI
        initializeUI();
        
        // Make visible
        setVisible(true);
        
        // Increment instance counter
        instanceCounter++;
    }
    
    /**
     * Initializes the UI layout with title, color chips, status bar, and control panel.
     */
    private void initializeUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Title panel with toggle button (North)
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Color chips panel (Center)
        JComponent chipPanel = createColorChipsPanel();
        mainPanel.add(chipPanel, BorderLayout.CENTER);
        
        // South panel with status bar and control panel
        JPanel southPanel = new JPanel(new BorderLayout(0, 2));
        
        // Status bar
        statusBar = createStatusBar();
        southPanel.add(statusBar, BorderLayout.NORTH);
        
        // Control panel for mouse click checkboxes
        controlPanelMouseClicks = createControlPanelMouseClicks();
        southPanel.add(controlPanelMouseClicks, BorderLayout.SOUTH);
        
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    /**
     * Creates the title panel with the palette name and toggle button.
     */
    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout(5, 0));
        
        // Create toggle button
        toggleButton = new JButton("_");
        toggleButton.setToolTipText("Toggle minimize/restore");
        toggleButton.setMargin(new Insets(0, 2, 0, 2));
        toggleButton.setPreferredSize(new Dimension(20, 20));
        toggleButton.setFont(new Font(toggleButton.getFont().getName(), Font.PLAIN, 10));
        toggleButton.addActionListener(e -> toggleMinimized());
        
        // Add title label (palette name with wrapping)
        JComponent titleLabel = createTitleLabel();
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        // Add toggle button to the right
        titlePanel.add(toggleButton, BorderLayout.EAST);
        
        return titlePanel;
    }
    
    /**
     * Creates a text field displaying the palette name with text wrapping.
     */
    private JComponent createTitleLabel() {
        JTextArea paletteNameTextfield = new JTextArea(paletteName);
        paletteNameTextfield.setFont(paletteNameTextfield.getFont().deriveFont(Font.BOLD, 12f));
        paletteNameTextfield.setLineWrap(true);
        paletteNameTextfield.setWrapStyleWord(true);
        paletteNameTextfield.setEditable(false);
        paletteNameTextfield.setOpaque(false);
        paletteNameTextfield.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        paletteNameTextfield.setFocusable(false);
        
        JScrollPane scrollPane = new JScrollPane(paletteNameTextfield);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        return scrollPane;
    }
    
    /**
     * Creates the color chips panel with row numbers on the left and chips in the center.
     */
    private JComponent createColorChipsPanel() {
        JPanel wrapperPanel = new JPanel(new BorderLayout(0, 0));
        
        // Row number panel (left, 15px wide, white background)
        JPanel rowNumberPanel = new RowNumberPanel();
        rowNumberPanel.setBackground(Color.WHITE);
        rowNumberPanel.setPreferredSize(new Dimension(ROW_NUMBER_PANEL_WIDTH, 0));
        wrapperPanel.add(rowNumberPanel, BorderLayout.WEST);
        
        // Color chip grid panel (center, gray background)
        chipGridPanel = new ColorChipGridPanel();
        wrapperPanel.add(chipGridPanel, BorderLayout.CENTER);
        
        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(wrapperPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        return scrollPane;
    }
    
    /**
     * Creates the status bar with a textfield for displaying selected color information.
     */
    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        
        statusTextField = new JTextField();
        statusTextField.setEditable(false);
        statusTextField.setText("Select a color chip");
        statusTextField.setFont(new Font("Monospaced", Font.PLAIN, STATUS_MSG_PTSIZE_SM));
        bar.add(statusTextField, BorderLayout.CENTER);
        
        return bar;
    }
    
    /**
     * Creates the control panel with checkboxes for mouse click behavior.
     * Panel is 30 pixels high and contains two checkboxes:
     * - "leftCheckboxSend" on the left (west) with label "L-clik Send"
     * - "rightCheckboxMonotri" on the right (east) with label "R Monotri"
     */
    private JPanel createControlPanelMouseClicks() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setPreferredSize(new Dimension(0, 30));
        controlPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        
        // Left side: "Send" checkbox
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        leftPanel.setOpaque(false);
        leftCheckboxSend = new JCheckBox("L-clik Send");
        leftCheckboxSend.setFocusable(false);
        leftPanel.add(leftCheckboxSend);
        controlPanel.add(leftPanel, BorderLayout.WEST);
        
        // Right side: "Monotri" checkbox
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        rightPanel.setOpaque(false);
        rightCheckboxMonotri = new JCheckBox("R Monotri");
        rightCheckboxMonotri.setFocusable(false);
        rightPanel.add(rightCheckboxMonotri);
        controlPanel.add(rightPanel, BorderLayout.EAST);
        
        return controlPanel;
    }
    
    /**
     * Updates the status bar with information about the selected color chip.
     * Format: "usrH=XX C=Y S=Z"
     * 
     * @param color The ColorPtOstDot that was selected
     * @param index The 0-based index of the selected chip
     */
    public void updateStatusBarForLeftClick(ColorPtOstDot color, int index) {
        if (color == null) {
            statusTextField.setText("Select a color chip");
            return;
        }

        // Format: "usrH=XX C=Y S=Z"
        String text = getOHCSString(color);
        statusTextField.setText(text);
        statusTextField.setFont(new Font("Monospaced", Font.PLAIN, STATUS_MSG_PTSIZE_LG));
    }
    
    /**
     * Updates the status bar with color information from a right-click on a color chip.
     * Displays hue number, white series, and black series from the ColorPtOstDot.
     * Format: "userHue:XX W=YY B=ZZ"
     * 
     * @param color The ColorPtOstDot that was right-clicked
     */
    public void updateStatusBarForRightClick(ColorPtOstDot color) {
        if (color == null) {
            statusTextField.setText("Select a color chip");
            return;
        }
        
        // Extract hue number (user-facing 1-36)
        int userHueNum = color.getHueNumGUI().getUserFacingHueNum();
        
        // Extract white and black series values
        int wSeries = color.getOstHCS().getSeriesW();
        int bSeries = color.getOstHCS().getSeriesB();
        
        // Format string with color information
        String text = String.format("userHueNum:%d  W= %d B= %d", userHueNum, wSeries, bSeries);
        statusTextField.setText(text);
        statusTextField.setFont(new Font("Monospaced", Font.PLAIN, STATUS_MSG_PTSIZE_LG));
    }
    
    /**
     * Generates tooltip text for a color chip showing hue, column, and step.
     * Format: "usrH=XX C=Y S=Z"
     * 
     * @param color The ColorPtOstDot for the chip
     * @param index The 0-based index of the chip
     * @return The formatted tooltip text
     */
    public String getTooltipText(ColorPtOstDot color, int index) {
        if (color == null) {
            return null;
        }
        
        // Format: "usrH=XX C=Y S=Z"
        return getOHCSString(color);
    }
    
    /**
     * Helper method to create the compact status bar and tooltip string.
     * Format: "usrH=XX C=Y S=Z"
     * 
     * @param color The ColorPtOstDot to extract information from
     * @return The formatted string
     */
    private String getOHCSString(ColorPtOstDot color) {
        int userHueNum = color.getHueNumGUI().getUserFacingHueNum();
        int column = color.getOstHCS().getColumn();
        int step = color.getOstHCS().getStep();
        
        return String.format("usrH=%d C=%d S=%d", userHueNum, column, step);
    }
    
    /**
     * Sets the selected chip index and triggers visual feedback.
     * 
     * @param index The 0-based index of the selected chip
     */
    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        chipGridPanel.repaint();
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
    
    /**
     * Toggles between normal and minimized states.
     * When minimized, only the title bar is visible.
     */
    public boolean toggleMinimized() {
        if (!isMinimized) {
            originalHeight = getHeight();
            Insets insets = getInsets();
            int titleBarHeight = insets.top + 30;
            setSize(getWidth(), titleBarHeight);
            isMinimized = true;
            statusBar.setVisible(false);
            controlPanelMouseClicks.setVisible(false);
            toggleButton.setText("▢");
            toggleButton.setToolTipText("Restore window");
            repaint();
        } else {
            setSize(getWidth(), originalHeight);
            isMinimized = false;
            statusBar.setVisible(true);
            controlPanelMouseClicks.setVisible(true);
            toggleButton.setText("_");
            toggleButton.setToolTipText("Toggle minimize/restore");
            repaint();
        }
        return isMinimized;
    }
    
    /**
     * Gets the palette displayed in this frame.
     */
    public PaletteWName getPaletteWName() {
        return paletteWName;
    }
    
    /**
     * Gets the palette name.
     */
    public String getPaletteName() {
        return paletteName;
    }
    
    /**
     * Gets the controller for this frame.
     */
    public SimplePaletteColorSourceController getController() {
        return controller;
    }
    
    /**
     * Gets the "Send" checkbox (left checkbox).
     * 
     * @return The leftCheckboxSend JCheckBox
     */
    public JCheckBox getLeftCheckboxSend() {
        return leftCheckboxSend;
    }
    
    /**
     * Gets the "Monotri" checkbox (right checkbox).
     * 
     * @return The rightCheckboxMonotri JCheckBox
     */
    public JCheckBox getRightCheckboxMonotri() {
        return rightCheckboxMonotri;
    }

    /**
     * Gets the source TemplateWorkFrame that created this palette frame.
     * 
     * @return The TemplateWorkFrame reference, or null if not set
     */
    public TemplateWorkFrame getSourceTemplateWorkFrame() {
        return sourceTemplateWorkFrame;
    }



    /**
     * Inner class for the row number panel.
     * Displays consecutive row numbers (1, 2, 3, ...) aligned with the color chip rows.
     */
    private class RowNumberPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        
        public RowNumberPanel() {
            setLayout(null);  // Use absolute positioning
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int numChips = paletteWName.getSize();
            int numRows = (numChips + COLORS_PER_ROW - 1) / COLORS_PER_ROW;
            
            // Set font for row numbers
            Font rowFont = new Font("Monospaced", Font.PLAIN, 9);
            g2d.setFont(rowFont);
            g2d.setColor(Color.BLACK);
            
            FontMetrics fm = g2d.getFontMetrics();
            int fontHeight = fm.getAscent();
            
            // Draw each row number
            for (int row = 0; row < numRows; row++) {
                String rowNumber = String.valueOf(row + 1);
                int rowWidth = fm.stringWidth(rowNumber);
                
                // Calculate y position: align with chip row
                int y = 5 + (row * CHIP_ROW_HEIGHT) + CHIP_SIZE / 2 + fontHeight / 2;
                
                // Calculate x position: center horizontally in the 15px panel
                int x = (ROW_NUMBER_PANEL_WIDTH - rowWidth) / 2;
                
                // Draw the row number
                g2d.drawString(rowNumber, x, y);
            }
        }
        
        @Override
        public Dimension getPreferredSize() {
            int numChips = paletteWName.getSize();
            int numRows = (numChips + COLORS_PER_ROW - 1) / COLORS_PER_ROW;
            int height = numRows * CHIP_ROW_HEIGHT + 10;  // 5px padding top and bottom
            return new Dimension(ROW_NUMBER_PANEL_WIDTH, height);
        }
    }
    
    /**
     * Inner class representing a single color chip in the grid.
     * A simple colored rectangle with a black border.
     */
    private static class ColorChip extends JPanel {
        private static final long serialVersionUID = 1L;
        
        /**
         * Creates a new ColorChip with the specified color.
         * 
         * @param color The ColorPtOstDot to display
         */
        public ColorChip(ColorPtOstDot color) {
            setPreferredSize(new Dimension(CHIP_SIZE, CHIP_SIZE));
            
            // Set background color
            if (color != null) {
                try {
                    Color awtColor = color.toAWTColor();
                    setBackground(awtColor);
                } catch (Exception e) {
                    setBackground(Color.GRAY);
                }
            } else {
                setBackground(Color.GRAY);
            }
            
            // Set border
            setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        }
    }
    
    /**
     * Inner class for the color chip grid panel.
     * Renders chips using code-based layout and handles mouse events.
     */
    private class ColorChipGridPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        
        public ColorChipGridPanel() {
            setBackground(new Color(200, 200, 200));  // Light gray background
            
            // Add mouse listener for chip selection (left-click) and right-click
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        controller.handleChipLeftClick(e.getX(), e.getY());
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        controller.handleChipRightClick(e.getX(), e.getY());
                    }
                }
            });
            
            // Add mouse motion listener for tooltip display on hover
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    String tooltip = controller.getTooltipForChip(e.getX(), e.getY());
                    setToolTipText(tooltip);
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int numChips = paletteWName.getSize();
            
            // Draw each color chip
            for (int i = 0; i < numChips; i++) {
                ColorPtOstDot color = paletteWName.getColorAt(i);
                
                // Calculate position
                int col = i % COLORS_PER_ROW;
                int row = i / COLORS_PER_ROW;
                int x = col * CHIP_ROW_HEIGHT + 5;  // 5px left padding
                int y = row * CHIP_ROW_HEIGHT + 5;  // 5px top padding
                
                // Draw chip background
                Color chipColor = color.toAWTColor();
                g2d.setColor(chipColor);
                g2d.fillRect(x, y, CHIP_SIZE, CHIP_SIZE);
                
                // Draw chip border
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRect(x, y, CHIP_SIZE, CHIP_SIZE);
                
                // Draw selection highlight if this chip is selected
                if (i == selectedIndex) {
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawRect(x - 2, y - 2, CHIP_SIZE + 4, CHIP_SIZE + 4);
                }
            }
        }
        
        @Override
        public Dimension getPreferredSize() {
            int numChips = paletteWName.getSize();
            int numRows = (numChips + COLORS_PER_ROW - 1) / COLORS_PER_ROW;
            int height = numRows * CHIP_ROW_HEIGHT + 10;  // 5px padding top and bottom
            int width = COLORS_PER_ROW * CHIP_ROW_HEIGHT + 10;  // 5px padding left and right
            return new Dimension(width, height);
        }
    }
}
