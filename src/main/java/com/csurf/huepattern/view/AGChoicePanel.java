package com.csurf.huepattern.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Panel for selecting which AnchorGroup (AG1-AG4) to create or modify.
 * Contains 4 mutually-exclusive radio buttons arranged vertically,
 * each with a text field for hue number and a small square canvas for color display.
 */
public class AGChoicePanel extends JPanel {
    private static final int COLOR_SQUARE_SIZE = 20;
    
    private ButtonGroup buttonGroup;
    private JRadioButton ag1Radio;
    private JRadioButton ag2Radio;
    private JRadioButton ag3Radio;
    private JRadioButton ag4Radio;
    
    private JTextField ag1HueField;
    private JTextField ag2HueField;
    private JTextField ag3HueField;
    private JTextField ag4HueField;
    
    private ColorSquareCanvas ag1ColorSquare;
    private ColorSquareCanvas ag2ColorSquare;
    private ColorSquareCanvas ag3ColorSquare;
    private ColorSquareCanvas ag4ColorSquare;
    
    private ActionListener agSelectionListener;

    public AGChoicePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("AG to create/modify:"));
        setBackground(Color.LIGHT_GRAY);
        
        // Initialize button group
        buttonGroup = new ButtonGroup();
        
        // Initialize radio buttons
        ag1Radio = new JRadioButton("AG1");
        ag2Radio = new JRadioButton("AG2");
        ag3Radio = new JRadioButton("AG3");
        ag4Radio = new JRadioButton("AG4");
        
        // Add to button group
        buttonGroup.add(ag1Radio);
        buttonGroup.add(ag2Radio);
        buttonGroup.add(ag3Radio);
        buttonGroup.add(ag4Radio);
        
        // Initialize text fields (3 characters wide)
        ag1HueField = createHueField();
        ag2HueField = createHueField();
        ag3HueField = createHueField();
        ag4HueField = createHueField();
        
        // Initialize color squares
        ag1ColorSquare = new ColorSquareCanvas();
        ag2ColorSquare = new ColorSquareCanvas();
        ag3ColorSquare = new ColorSquareCanvas();
        ag4ColorSquare = new ColorSquareCanvas();
        
        // Add rows
        add(createAGRow(ag1Radio, ag1HueField, ag1ColorSquare));
        add(Box.createVerticalStrut(5));
        add(createAGRow(ag2Radio, ag2HueField, ag2ColorSquare));
        add(Box.createVerticalStrut(5));
        add(createAGRow(ag3Radio, ag3HueField, ag3ColorSquare));
        add(Box.createVerticalStrut(5));
        add(createAGRow(ag4Radio, ag4HueField, ag4ColorSquare));
    }
    
    private JTextField createHueField() {
        JTextField field = new JTextField(3);
        field.setEditable(false);
        field.setMaximumSize(new Dimension(40, 25));
        return field;
    }
    
    private JPanel createAGRow(JRadioButton radio, JTextField hueField, ColorSquareCanvas colorSquare) {
        JPanel row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setBackground(Color.LIGHT_GRAY);
        row.setMaximumSize(new Dimension(140, 30));
        
        radio.setBackground(Color.LIGHT_GRAY);
        row.add(radio);
        row.add(hueField);
        row.add(colorSquare);
        
        return row;
    }
    
    /**
     * Gets the currently selected AG (1-4).
     * @return The AG number (1-4), or 0 if none selected
     */
    public int getSelectedAG() {
        if (ag1Radio.isSelected()) return 1;
        if (ag2Radio.isSelected()) return 2;
        if (ag3Radio.isSelected()) return 3;
        if (ag4Radio.isSelected()) return 4;
        return 0;
    }
    
    /**
     * Registers a listener to be called when AG selection changes.
     * @param listener The ActionListener to call on AG selection change
     */
    public void addAGSelectionListener(ActionListener listener) {
        this.agSelectionListener = listener;
        
        ag1Radio.addActionListener(listener);
        ag2Radio.addActionListener(listener);
        ag3Radio.addActionListener(listener);
        ag4Radio.addActionListener(listener);
    }
    
    /**
     * Updates the Anchor hue and color for a specific AG.
     * Called when the Anchor value changes via dial input.
     * @param whichAG The AG number (1-4)
     * @param anchorHue The new Anchor hue number (1-36)
     * @param anchorColor The color for the Anchor hue
     */
    public void updateAGAnchorDisplay(int whichAG, int anchorHue, Color anchorColor) {
        if (whichAG < 1 || whichAG > 4) {
            return;
        }
        
        JTextField hueField = null;
        ColorSquareCanvas colorSquare = null;
        
        switch (whichAG) {
            case 1:
                hueField = ag1HueField;
                colorSquare = ag1ColorSquare;
                break;
            case 2:
                hueField = ag2HueField;
                colorSquare = ag2ColorSquare;
                break;
            case 3:
                hueField = ag3HueField;
                colorSquare = ag3ColorSquare;
                break;
            case 4:
                hueField = ag4HueField;
                colorSquare = ag4ColorSquare;
                break;
        }
        
        if (hueField != null && colorSquare != null) {
            hueField.setText(String.valueOf(anchorHue));
            colorSquare.setColor(anchorColor);
        }
    }
    
    /**
     * Small canvas for displaying a color square.
     */
    private class ColorSquareCanvas extends JPanel {
        public ColorSquareCanvas() {
            setPreferredSize(new Dimension(COLOR_SQUARE_SIZE, COLOR_SQUARE_SIZE));
            setMinimumSize(new Dimension(COLOR_SQUARE_SIZE, COLOR_SQUARE_SIZE));
            setMaximumSize(new Dimension(COLOR_SQUARE_SIZE, COLOR_SQUARE_SIZE));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        }
        
        public void setColor(Color color) {
            setBackground(color);
            repaint();
        }
    }
} // end class AGChoicePanel
