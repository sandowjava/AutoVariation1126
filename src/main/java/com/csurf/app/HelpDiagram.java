package com.csurf.app;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * A window that displays an image loaded from a resource file.
 */
public class HelpDiagram extends JFrame {
    
    private final JPanel helpDiagramPanel;
    private BufferedImage originalImage;
    private BufferedImage resizedImage;
    
    /**
     * Creates a new help diagram window with the specified title and image from the given resource path.
     * 
     * @param title The window title
     * @param resourcePath The path to the image resource file
     */
    public HelpDiagram(String title, String resourcePath) {
        super(title);
        
        // Set up the window
        setSize(850, 1100);
        setLocationRelativeTo(null); // Center on screen
        
        // Load the original image
        originalImage = loadImage(resourcePath);
        
        // Create the panel for displaying the image
        helpDiagramPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (resizedImage != null) {
                    // Draw the resized image centered in the panel
                    int x = (getWidth() - resizedImage.getWidth()) / 2;
                    int y = (getHeight() - resizedImage.getHeight()) / 2;
                    g.drawImage(resizedImage, x, y, this);
                }
            }
            
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(850, 1100);
            }
        };
        
        // Add the panel to the frame
        getContentPane().add(helpDiagramPanel, BorderLayout.CENTER);
        
        // Add a component listener to resize the image when the panel size changes
        helpDiagramPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                if (originalImage != null) {
                    resizedImage = resizeImage(originalImage, helpDiagramPanel.getWidth(), helpDiagramPanel.getHeight());
                    helpDiagramPanel.repaint();
                }
            }
        });
    }
    
    /**
     * Resizes an image to fit within the specified dimensions while maintaining aspect ratio.
     * 
     * @param img The original image to resize
     * @param targetWidth The target width
     * @param targetHeight The target height
     * @return The resized image
     */
    private BufferedImage resizeImage(BufferedImage img, int targetWidth, int targetHeight) {
        if (img == null) {
            return null;
        }
        
        // Calculate the scaling factor to maintain aspect ratio
        double widthRatio = (double) targetWidth / img.getWidth();
        double heightRatio = (double) targetHeight / img.getHeight();
        double scaleFactor = Math.min(widthRatio, heightRatio);
        
        // Calculate new dimensions
        int scaledWidth = (int) (img.getWidth() * scaleFactor);
        int scaledHeight = (int) (img.getHeight() * scaleFactor);
        
        // Create a new BufferedImage with the calculated dimensions
        BufferedImage resized = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw the original image to the new, resized image
        g2d.drawImage(img, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();
        
        return resized;
    }
    
    /**
     * Loads the image from the specified resource path.
     * 
     * @param resourcePath The path to the image resource file
     * @return The loaded image, or null if loading failed
     */
    private BufferedImage loadImage(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                JOptionPane.showMessageDialog(this, 
                    "Error: Could not find image file: " + resourcePath,
                    "Error Loading Image",
                    JOptionPane.ERROR_MESSAGE);
                return null;
            }
            
            BufferedImage img = ImageIO.read(is);
            
            // Resize the image immediately after loading
            if (img != null && helpDiagramPanel != null) {
                resizedImage = resizeImage(img, 850, 1100);
            }
            
            return img;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading image file: " + e.getMessage(),
                "Error Loading Image",
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
}
