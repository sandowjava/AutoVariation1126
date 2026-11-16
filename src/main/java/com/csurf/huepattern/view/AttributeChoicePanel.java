package com.csurf.huepattern.view;

import com.csurf.huepattern.model.WorkingAnchorPatternState;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.csurf.ostwald.domainmonotri.model.MonotriPage;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for selecting which attribute (CCW Limit, Anchor, CW Limit) to modify.
 * Contains 3 mutually-exclusive radio buttons arranged vertically,
 * each with a text field for hue number display and a color square.
 */
public class AttributeChoicePanel extends JPanel {
    private static final int COLOR_SQUARE_SIZE = 20;
    
    private ButtonGroup buttonGroup;
    private JRadioButton ccwLimitRadio;
    private JRadioButton anchorRadio;
    private JRadioButton cwLimitRadio;
    
    private JTextField ccwLimitHueField;
    private JTextField anchorHueField;
    private JTextField cwLimitHueField;
    
    private ColorSquareCanvas ccwLimitColorSquare;
    private ColorSquareCanvas anchorColorSquare;
    private ColorSquareCanvas cwLimitColorSquare;

    public AttributeChoicePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Attribute:"));
        setBackground(Color.LIGHT_GRAY);
        
        // Initialize button group
        buttonGroup = new ButtonGroup();
        
        // Initialize radio buttons
        ccwLimitRadio = new JRadioButton("CCWLim");
        anchorRadio = new JRadioButton("Anchor");
        cwLimitRadio = new JRadioButton("CWLim");
        
        // Add to button group
        buttonGroup.add(ccwLimitRadio);
        buttonGroup.add(anchorRadio);
        buttonGroup.add(cwLimitRadio);
        
        // Initialize text fields (3 characters wide)
        ccwLimitHueField = createHueField();
        anchorHueField = createHueField();
        cwLimitHueField = createHueField();
        
        // Initialize color squares
        ccwLimitColorSquare = new ColorSquareCanvas();
        anchorColorSquare = new ColorSquareCanvas();
        cwLimitColorSquare = new ColorSquareCanvas();
        
        // Add rows
        add(createAttributeRow(ccwLimitRadio, ccwLimitHueField, ccwLimitColorSquare));
        add(Box.createVerticalStrut(5));
        add(createAttributeRow(anchorRadio, anchorHueField, anchorColorSquare));
        add(Box.createVerticalStrut(5));
        add(createAttributeRow(cwLimitRadio, cwLimitHueField, cwLimitColorSquare));
    }
    
    private JTextField createHueField() {
        JTextField field = new JTextField(3);
        field.setEditable(false);
        field.setMaximumSize(new Dimension(40, 25));
        return field;
    }
    
    private JPanel createAttributeRow(JRadioButton radio, JTextField hueField, ColorSquareCanvas colorSquare) {
        JPanel row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setBackground(Color.LIGHT_GRAY);
        row.setMaximumSize(new Dimension(180, 30));
        
        radio.setBackground(Color.LIGHT_GRAY);
        row.add(radio);
        row.add(hueField);
        row.add(colorSquare);
        
        return row;
    }
    
    /**
     * Gets the currently selected attribute.
     * @return The attribute (CCW_LIMIT, ANCHOR, CW_LIMIT), or null if none selected
     */
    public WorkingAnchorPatternState.Attribute getSelectedAttribute() {
        if (ccwLimitRadio.isSelected()) return WorkingAnchorPatternState.Attribute.CCW_LIMIT;
        if (anchorRadio.isSelected()) return WorkingAnchorPatternState.Attribute.ANCHOR;
        if (cwLimitRadio.isSelected()) return WorkingAnchorPatternState.Attribute.CW_LIMIT;
        return null;
    }
    
    /**
     * Updates the hue field for the specified attribute.
     * @param attribute The attribute (CCW_LIMIT, ANCHOR, CW_LIMIT)
     * @param hueNum The hue number (1-36)
     */
    public void updateHueField(WorkingAnchorPatternState.Attribute attribute, int hueNum) {
        if (attribute == null) {
            return;
        }
        
        String hueText = String.valueOf(hueNum);
        
        switch (attribute) {
            case CCW_LIMIT:
                ccwLimitHueField.setText(hueText);
                break;
            case ANCHOR:
                anchorHueField.setText(hueText);
                break;
            case CW_LIMIT:
                cwLimitHueField.setText(hueText);
                break;
        }
    }
    
    /**
     * Updates the color square for a specific attribute.
     * @param attribute The attribute (CCW_LIMIT, ANCHOR, CW_LIMIT)
     * @param hueNum The hue number (1-36)
     */
    public void updateColorSquare(WorkingAnchorPatternState.Attribute attribute, int hueNum) {
        if (attribute == null) {
            return;
        }
        
        Color color = getColorForHueNum(hueNum);
        
        switch (attribute) {
            case CCW_LIMIT:
                ccwLimitColorSquare.setColor(color);
                break;
            case ANCHOR:
                anchorColorSquare.setColor(color);
                break;
            case CW_LIMIT:
                cwLimitColorSquare.setColor(color);
                break;
        }
    }
    
    /**
     * Updates all three hue fields and color squares at once.
     * Called when AG selection changes.
     * @param ccwHue CCW Limit hue (1-36, or 0 for empty)
     * @param anchorHue Anchor hue (1-36, or 0 for empty)
     * @param cwHue CW Limit hue (1-36, or 0 for empty)
     */
    public void updateAllHueFields(int ccwHue, int anchorHue, int cwHue) {
        ccwLimitHueField.setText(ccwHue > 0 ? String.valueOf(ccwHue) : "");
        anchorHueField.setText(anchorHue > 0 ? String.valueOf(anchorHue) : "");
        cwLimitHueField.setText(cwHue > 0 ? String.valueOf(cwHue) : "");
        
        updateColorSquareIfValid(ccwLimitColorSquare, ccwHue);
        updateColorSquareIfValid(anchorColorSquare, anchorHue);
        updateColorSquareIfValid(cwLimitColorSquare, cwHue);
    }
    
    /**
     * Helper method to update color square only if hue is valid.
     */
    private void updateColorSquareIfValid(ColorSquareCanvas colorSquare, int hueNum) {
        if (hueNum > 0 && hueNum <= 36) {
            Color color = getColorForHueNum(hueNum);
            colorSquare.setColor(color);
        } else {
            colorSquare.setColor(Color.WHITE);
        }
    }
    
    /**
     * Gets the color for a hue number from MonotriBook.
     * @param hueUserFacingNum The hue number (1-36)
     * @return The color, or white if invalid
     */
    private Color getColorForHueNum(int hueUserFacingNum) {
        if (hueUserFacingNum < 1 || hueUserFacingNum > 36) {
            return Color.WHITE;
        }
        
        try {
            MonotriPage page = MonotriBook.getInstance().getMonoTriPage(hueUserFacingNum - 1);
            if (page == null) {
                return Color.WHITE;
            }
            
            ColorPtOstDot colorDot = page.getColorByFillOrder(22);  // TODO: fillOrderNum =22 is OK, near-pure high-sat, mid-column 3
            if (colorDot == null) {
                return Color.WHITE;
            }
            
            return colorDot.getAsColor();
        } catch (Exception e) {
            return Color.WHITE;
        }
    }
    
    /**
     * Clears all selections and text fields.
     */
    public void clearSelection() {
        buttonGroup.clearSelection();
        ccwLimitHueField.setText("");
        anchorHueField.setText("");
        cwLimitHueField.setText("");
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
} // end class AttributeChoicePanel
