package com.csurf.templatecore.view;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.PaletteWName;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

/**
 * PaletteDisplayFrame displays a palette from a saved Template in a persistent, resizable window.
 * 
 * This frame shows all colors in a palette in a grid layout (5 colors per row) with color swatches.
 * The window is non-modal and can be resized by the user.
 * 
 * Purpose: Allow users to view and examine palettes extracted from saved Templates without
 * blocking other windows or requiring modal interaction.
 * 
 * Future Enhancement: Color picking functionality to apply colors to DrawObjs in the source Template.
 */
public class PaletteDisplayFrame extends JFrame {
    
    private static final long serialVersionUID = 1L;
    
    // UI Constants
    private static final int COLORS_PER_ROW = 5;
    private static final int SWATCH_SIZE = 26;
    private static final int SWATCH_GAP = 5;
    private static final int windowWidth = 200;
    private static final int INITIAL_HEIGHT = 350;
    private static final int MIN_HEIGHT = 200;
    private static final int ROW_NUMBER_LINE_HEIGHT = 31;  // Matches swatch row height (26px + 5px gap)
    
    // Data
    private final PaletteWName palette;
    private final String sourceTemplateName;
    private static int instanceCounter = 1;
    
    // Minimize/Restore state management
    private int originalHeight;
    private boolean isMinimized = false;
    private JButton toggleButton;
    private JPanel statusBar;
    
    /**
     * Creates a new PaletteDisplayFrame to display the given palette.
     * 
     * @param palette The PaletteWName instance to display
     * @param sourceTemplateName Name of the source template (for window title)
     */
    public PaletteDisplayFrame(PaletteWName palette, String sourceTemplateName) {
        this.palette = palette;
        this.sourceTemplateName = sourceTemplateName;
        
        // Configure window
        setTitle(instanceCounter + ": " + sourceTemplateName);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(windowWidth, INITIAL_HEIGHT);
        setMinimumSize(new Dimension(windowWidth, MIN_HEIGHT));
        setMaximumSize(new Dimension(windowWidth, Integer.MAX_VALUE));
        setResizable(true); // Allow height resizing; width is constrained by setSize
        setLocationRelativeTo(null); // Center on screen
        
        // Override the default minimize behavior
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowIconified(java.awt.event.WindowEvent e) {
                // Prevent standard minimization and use our custom minimize instead
                setExtendedState(JFrame.NORMAL);
                toggleMinimized();
            }
        });
        
        // Build UI
        initializeUI();
        
        // Make visible
        setVisible(true);

        // Increment instance counter
        instanceCounter++;
    } // end constructor

    
    /**
     * Initializes the UI layout with title, color swatches, and status bar.
     */
    private void initializeUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Title panel with toggle button (North)
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Color swatches panel (Center)
        JScrollPane scrollPane = createColorSwatchesScrollPane();
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Status bar (South)
        statusBar = createStatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    /**
     * Creates the title panel with the template name and toggle button.
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
        
        // Add title label (template name with wrapping)
        JComponent titleLabel = createTitleLabel();
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        // Add toggle button to the right
        titlePanel.add(toggleButton, BorderLayout.EAST);
        
        return titlePanel;
    }
    
    /**
     * Creates a text field displaying the template name with text wrapping.
     */
    private JComponent createTitleLabel() {
        // Create a JTextArea with wrapping enabled for the template name
        JTextArea templateNameTextfield = new JTextArea(sourceTemplateName);
        templateNameTextfield.setFont(templateNameTextfield.getFont().deriveFont(Font.BOLD, 12f));
        templateNameTextfield.setLineWrap(true);
        templateNameTextfield.setWrapStyleWord(true);
        templateNameTextfield.setEditable(false);
        templateNameTextfield.setOpaque(false);
        templateNameTextfield.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        templateNameTextfield.setFocusable(false);
        
        // Wrap in a scroll pane to handle overflow
        JScrollPane scrollPane = new JScrollPane(templateNameTextfield);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        return scrollPane;
    }
    
    /**
     * Creates the scrollable panel containing color swatches in a grid layout.
     */
    private JScrollPane createColorSwatchesScrollPane() {
        int numColors = palette.getSize();
        
        // Calculate number of rows needed for 5 columns
        int numRows = (numColors + COLORS_PER_ROW - 1) / COLORS_PER_ROW;
        
        // Create panel with GridLayout (rows, columns)
        JPanel swatchesPanel = new JPanel(new GridLayout(numRows, COLORS_PER_ROW, SWATCH_GAP, SWATCH_GAP));
        swatchesPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Add color swatches
        for (int i = 0; i < numColors; i++) {
            ColorPtOstDot color = palette.getColorAt(i);
            JPanel swatch = createColorSwatch(color);
            swatchesPanel.add(swatch);
        }
        
        // Add empty panels to fill grid if needed
        int totalCells = numRows * COLORS_PER_ROW;
        for (int i = numColors; i < totalCells; i++) {
            swatchesPanel.add(new JPanel());
        }
        
        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(swatchesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        return scrollPane;
    }
    
    
    /**
     * Toggles between normal and minimized states.
     * When minimized, only the title bar is visible.
     * 
     * @return true if the frame is now minimized, false otherwise
     */
    public boolean toggleMinimized() {
        if (!isMinimized) {
            // Save current height before minimizing
            originalHeight = getHeight();
            
            // Get the height of just the title bar plus a little extra to ensure button is visible
            Insets insets = getInsets();
            int titleBarHeight = insets.top + 30;  // Add extra height to ensure button is visible
            
            // Set the frame height to show the title bar plus a bit more
            setSize(getWidth(), titleBarHeight);
            isMinimized = true;
            
            // Hide the status bar
            statusBar.setVisible(false);
            
            // Change button text to indicate restore action
            toggleButton.setText("▢");
            toggleButton.setToolTipText("Restore window");
            
            // Force a repaint to ensure the button is visible
            repaint();
        } else {
            // Restore original height
            setSize(getWidth(), originalHeight);
            isMinimized = false;
            
            // Show the status bar
            statusBar.setVisible(true);
            
            // Change button text to indicate minimize action
            toggleButton.setText("_");
            toggleButton.setToolTipText("Toggle minimize/restore");
        }
        
        return isMinimized;
    }
    
    /**
     * Checks if the frame is currently minimized.
     * 
     * @return true if minimized, false otherwise
     */
    public boolean isMinimized() {
        return isMinimized;
    }
    
    /**
     * Creates a single color swatch panel for the given color.
     */
    private JPanel createColorSwatch(ColorPtOstDot color) {
        JPanel swatch = new JPanel();
        
        // Set background color
        if (color != null) {
            try {
                Color awtColor = color.toAWTColor();
                swatch.setBackground(awtColor);
            } catch (Exception e) {
                // Fallback to gray if color conversion fails
                swatch.setBackground(Color.GRAY);
            }
        } else {
            swatch.setBackground(Color.GRAY);
        }
        
        // Set border
        swatch.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        
        // Set preferred size to 32x32 pixels
        swatch.setPreferredSize(new Dimension(SWATCH_SIZE, SWATCH_SIZE));
        
        return swatch;
    }
    
    /**
     * Creates the status bar showing palette information.
     */
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        
        // Total colors label
        JLabel totalLabel = new JLabel("Total colors: " + palette.getSize());
        statusBar.add(totalLabel);
        
        // Source template name label
        JLabel sourceLabel = new JLabel(" | Source: " + sourceTemplateName);
        statusBar.add(sourceLabel);
        
        return statusBar;
    }
    
    /**
     * Gets the palette displayed in this frame.
     */
    public PaletteWName getPalette() {
        return palette;
    }
    
    /**
     * Gets the source template name.
     */
    public String getSourceTemplateName() {
        return sourceTemplateName;
    }
    

} // end class PaletteDisplayFrame
