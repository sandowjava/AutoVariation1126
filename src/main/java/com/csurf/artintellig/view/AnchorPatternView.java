package com.csurf.artintellig.view;

import com.csurf.artintellig.domainanchorpattern.model.IQAnchorPattern;
import com.csurf.templatecore.model.Template;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * A graphical interface for creating and editing IQAnchorPatterns. This view displays
 * four dials representing the rings of an anchor pattern, allowing users to adjust
 * the angles of each ring interactively.
 * 
 * Features:
 * - Four dials arranged in a grid (2x2)
 * - Each dial represents one ring of the anchor pattern
 * - Visual feedback of current angle values
 * - Send button to create a new anchor pattern
 * - Displays template information in a separate window
 */
public class AnchorPatternView extends JFrame {
    private static final int PADDING = 15;
    private static final int WINDOW_SIZE = 400;
    private final DialPanel[] dialPanels;
    private final IQAnchorPattern pattern;
    private Consumer<IQAnchorPattern> patternConsumer;
    private DialOutputDisplayFrame dialOutputDisplayFrame;
    
    /**
     * Creates a new AnchorPatternView without a pattern consumer.
     */
    public AnchorPatternView() {
        this(null);
    }
    
    /**
     * Creates a new AnchorPatternView with the specified pattern consumer.
     * 
     * @param patternConsumer Consumer that will receive the created anchor pattern
     */
    public AnchorPatternView(Consumer<IQAnchorPattern> patternConsumer) {
        super("AnchPattern 4Clocks -Pull Dial CW");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        this.patternConsumer = patternConsumer;
        pattern = new IQAnchorPattern();
        dialPanels = new DialPanel[4];

        // TODO 110325 this is all OLD stuff, never implemented, concept unclear
        // Initialize the template input window
        dialOutputDisplayFrame = new DialOutputDisplayFrame();
        
        // Create main container with BorderLayout
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        
        // Create grid panel for dials
        JPanel gridPanel = new JPanel(new GridLayout(2, 2, PADDING, PADDING));
        
        // Create dial panels first
        createDialPanel(1, 0); // Ring 1 (NW)
        createDialPanel(2, 1); // Ring 2 (NE)
        createDialPanel(3, 3); // Ring 3 (SE)
        createDialPanel(4, 2); // Ring 4 (SW)
        
        // Add panels to grid in specific positions
        gridPanel.add(dialPanels[0]); // Ring 1 - Top Left
        gridPanel.add(dialPanels[1]); // Ring 2 - Top Right
        gridPanel.add(dialPanels[3]); // Ring 4 - Bottom Left
        gridPanel.add(dialPanels[2]); // Ring 3 - Bottom Right
        
        // Add grid panel to container
        container.add(gridPanel, BorderLayout.CENTER);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton sendButton = new JButton("Send AnchorPattern");
        sendButton.addActionListener(e -> sendPattern());
        buttonPanel.add(sendButton);
        
        // Add button panel to container
        container.add(buttonPanel, BorderLayout.SOUTH);
        
        // Set up the frame
        setContentPane(container);
        setSize(WINDOW_SIZE, WINDOW_SIZE);
        setLocationRelativeTo(null); // Center on screen
    }
    
    /**
     * Creates a dial panel for a specific ring and adds it to the view.
     * 
     * @param ringNumber The ring number (1-4)
     * @param gridPosition The position in the grid (0-3)
     */
    private void createDialPanel(int ringNumber, int gridPosition) {
        DialPanel panel = new DialPanel(ringNumber);
        dialPanels[ringNumber - 1] = panel; // Store by ring number (1-based to 0-based)
        
        // Add value change listener
        panel.addDialListener(evt -> {
            double angle = (double) evt.getNewValue();
            pattern.setAngle(ringNumber, angle);
            panel.updateAngleDisplay(angle);
        });
    }
    
    /**
     * Sends the current anchor pattern to the consumer and displays the template input window.
     * Creates a new template with the current pattern's angles.
     *  // TODO 110325 this is all OLD stuff, never implemented, concept unclear
     */
    private void sendPattern() {
        if (patternConsumer != null) {
            // Create a new AnchorPattern instance with current values
            IQAnchorPattern newPattern = new IQAnchorPattern();
            for (int i = 1; i <= 4; i++) {
                newPattern.setAngle(i, pattern.getAngle(i));
            }
            patternConsumer.accept(newPattern);
        }
        
        // Create a new Template and update it with the current pattern
        Template template = new Template();
        template.updateFromAnchorPattern(pattern);
        
        // Update and show the template input window
        dialOutputDisplayFrame.updateFromTemplate(template);
        dialOutputDisplayFrame.setVisible(true);
    }
    
    /**
     * Sets the consumer that will receive the created anchor pattern.
     * 
     * @param consumer The consumer to receive the pattern
     */
    public void setPatternConsumer(Consumer<IQAnchorPattern> consumer) {
        this.patternConsumer = consumer;
    }
    
    public IQAnchorPattern getPattern() {
        return pattern;
    }
    
    public void setPattern(IQAnchorPattern newPattern) {
        for (int i = 0; i < 4; i++) {
            int ringNumber = i + 1;
            double angle = newPattern.getAngle(ringNumber);
            DialPanel panel = dialPanels[i];
            panel.getDial().setValue(angle);
            panel.updateAngleDisplay(angle);
        }
    }
}
