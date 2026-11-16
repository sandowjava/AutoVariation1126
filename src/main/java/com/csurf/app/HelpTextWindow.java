package com.csurf.app;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * A simple window that displays formatted help text loaded from a resource file.
 * Supports both HTML and RTF formats for the help text files.
 */
public class HelpTextWindow extends JFrame {
    
    private final JEditorPane textPane;
    
    /**
     * Creates a new help text window with the specified title and content from the given resource path.
     * 
     * @param title The window title
     * @param resourcePath The path to the help text resource file
     */
    public HelpTextWindow(String title, String resourcePath) {
        super(title);
        
        // Set up the window
        setSize(600, 500);
        setLocationRelativeTo(null); // Center on screen
        
        // Create a text pane
        textPane = new JEditorPane();
        textPane.setEditable(false);
        
        // Add scrolling capability
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // Add to frame
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        
        // Load the content
        loadContent(resourcePath);
    }
    
    /**
     * Loads the help text content from the specified resource path.
     * Automatically detects the file format based on extension.
     * 
     * @param resourcePath The path to the help text resource file
     */
    private void loadContent(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                textPane.setText("Error: Could not find help file: " + resourcePath);
                return;
            }
            
            if (resourcePath.toLowerCase().endsWith(".html") || resourcePath.toLowerCase().endsWith(".htm")) {
                // Handle HTML content
                setupHtmlSupport();
                String content = new BufferedReader(new InputStreamReader(is))
                        .lines().collect(Collectors.joining("\n"));
                textPane.setText(content);
            } else if (resourcePath.toLowerCase().endsWith(".rtf")) {
                // Handle RTF content
                setupRtfSupport();
                RTFEditorKit rtfKit = (RTFEditorKit) textPane.getEditorKit();
                Document doc = textPane.getDocument();
                rtfKit.read(is, doc, 0);
            } else {
                // Handle plain text content
                textPane.setContentType("text/plain");
                String content = new BufferedReader(new InputStreamReader(is))
                        .lines().collect(Collectors.joining("\n"));
                textPane.setText(content);
            }
            
            textPane.setCaretPosition(0); // Scroll to top
        } catch (IOException | BadLocationException e) {
            textPane.setText("Error loading help file: " + e.getMessage());
        }
    }
    
    /**
     * Sets up HTML support with styling
     */
    private void setupHtmlSupport() {
        textPane.setContentType("text/html");
        HTMLEditorKit kit = new HTMLEditorKit();
        textPane.setEditorKit(kit);
        
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body { font-family: Arial, sans-serif; margin: 10px; }");
        styleSheet.addRule("h1 { color: #003366; }");
        styleSheet.addRule("h2 { color: #006699; }");
        styleSheet.addRule("p { margin-bottom: 10px; }");
    }
    
    /**
     * Sets up RTF support
     */
    private void setupRtfSupport() {
        textPane.setContentType("text/rtf");
        textPane.setEditorKit(new RTFEditorKit());
    }
}
