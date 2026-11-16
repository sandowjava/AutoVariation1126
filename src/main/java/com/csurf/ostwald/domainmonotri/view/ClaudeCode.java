package com.csurf.ostwald.domainmonotri.view;

import javax.swing.*;
import java.awt.*;

/**
 * Custom drawing component that displays a pattern of squares in Triangle layout. For REF only
 */
public class ClaudeCode extends JFrame {
    private final DrawingPanel drawingPanel;
    
    public ClaudeCode() {
        setTitle("Claude Code");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        drawingPanel = new DrawingPanel();
        drawingPanel.setPreferredSize(new Dimension(600, 600));
        
        add(drawingPanel);
        pack();
        setLocationRelativeTo(null); // Center on screen
    }
    
    private class DrawingPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw pattern from left to right - 2nd param = edgeSize
            drawSquares(g2d, 9, true, getWidth(), getHeight());
        }
        
        public void drawSquares(Graphics2D g2d, int edgeSize, boolean leftRight, int panelWidth, int panelHeight) {

            final int SQUARE_SIZE = 20;
            final int COLUMN_SPACING = 25;
            final int VERTICAL_OFFSET = 10;
            final int SQUARE_SPACING = 5;
            int halfPanelHeight = panelHeight/2 - (int)COLUMN_SPACING/2; // moved up: compensate for calc in WHILE
            
            int counter = 1;
            
            while (counter <= edgeSize) {
                // Calculate column position
                int columnX;
                if (leftRight) {
                    // Left to right: first column at left edge, then move right
                    columnX = (counter - 1) * COLUMN_SPACING;
                } else {
                    // Right to left: first column at right edge, then move left
                    columnX = panelWidth - SQUARE_SIZE - (counter - 1) * COLUMN_SPACING;
                }
                
                // Calculate starting Y position for bottom square of this column
                // Each column starts 12.5 pixels lower than preceding column, splits COLUMN_SPACING
                int bottomY = (int) (halfPanelHeight +12.5*counter); // half COLUMN_SPACING lower
                // if(counter>1) {bottomY+=2.5*counter;} // then further w multiple of 5 pixels lower
                
                // Draw squares in this column from bottom to top
                for (int i = 0; i < counter; i++) {
                    // bottomY += 5*(i+1);
                    // Calculate Y position for each square (going upward from bottom)
                    int squareY = bottomY - i * (SQUARE_SIZE + SQUARE_SPACING);
                    g2d.drawRect(columnX, squareY, SQUARE_SIZE, SQUARE_SIZE);
                }
                
                counter++;
            }
        }
    }
}
