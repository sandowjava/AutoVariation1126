package com.csurf.templatecore.view;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.PaletteWShapeNames;
import com.csurf.templatecore.model.ShapeColorPair;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Quick and dirty test display for PaletteWShapeNames.
 * Shows all the hybrid pairs in a table with:
 * - paletteIndex (1-based position)
 * - shapeName (from DrawObj.getName())
 * - color (visual swatch)
 * 
 * Designed as a minimal test UI to verify that PaletteWShapeNames
 * correctly extracts and stores shape-color pairs from DrawObjRoster.
 *
 * Useful as auxiliary display for complex Templates with many shapes.
 * // TODO 102126: develop usefulness -original intent: use for color-source palette from Mosaic module
 */
public class PaletteWNamesDisplayFrame extends JFrame {
    private PaletteWShapeNames palette;
    private List<ShapeColorPair> displayPairs;
    private int windowWidth = 255; // 255 = minimum useful width
    
    public PaletteWNamesDisplayFrame(PaletteWShapeNames paletteWShapeNames) {
        this.palette = paletteWShapeNames;
        this.displayPairs = new ArrayList<>(palette.getPairs());
        
        initializeUI();
    }
    
    private void initializeUI() {
        String templInstanceFileNameString // previously used in composing title
                = palette.getPaletteName() != null ? palette.getPaletteName() : "Unnamed";
        String numPairsString // previously used in composing title
                = " (" + displayPairs.size() + " pairs)";
        // reminder that T instance has effect: holds selection-groups, drawObj names etc
        // note: T instance remains in effect despite palette changes (load, transfer)
        String explainTemplInstanceEffectString = "T instance holds groups etc";

        setTitle("PaletteWShapeNames " +
                explainTemplInstanceEffectString);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(windowWidth, 400);
        setLocationRelativeTo(null);
        
        // Create table model
        PaletteTableModel tableModel = new PaletteTableModel(displayPairs);
        JTable table = new JTable(tableModel);
        
        // Set selection colors to light gray
        table.setSelectionBackground(new Color(211, 211, 211)); // Light gray
        table.setSelectionForeground(Color.BLACK);

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(80);   // paletteIndex
        table.getColumnModel().getColumn(1).setPreferredWidth(300);  // shapeName
        table.getColumnModel().getColumn(2).setPreferredWidth(100);  // color
        
        // Set custom renderer for color column
        table.getColumnModel().getColumn(2).setCellRenderer(new ColorSwatchRenderer());
        
        // Set row height to accommodate color swatches
        table.setRowHeight(40);
        
        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        
        // Add status bar
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);
        
        setVisible(true);
    }
    
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        // removed first part WAS "Total pairs: " + palette.getSize() + [shorten string]
        String statusText = "T: " + (palette.getPaletteName() != null ? palette.getPaletteName() : "Unnamed");
        
        JLabel statusLabel = new JLabel(statusText);
        statusBar.add(statusLabel);
        
        return statusBar;
    }
    
    /**
     * Table model for displaying palette pairs.
     */
    private static class PaletteTableModel extends AbstractTableModel {
        private List<ShapeColorPair> pairs;
        private String[] columnNames = {"Index", "Shape Name", "Color"};
        
        public PaletteTableModel(List<ShapeColorPair> pairs) {
            this.pairs = pairs;
        }
        
        @Override
        public int getRowCount() {
            return pairs.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }
        
        @Override
        public Object getValueAt(int row, int col) {
            ShapeColorPair pair = pairs.get(row);
            
            switch (col) {
                case 0:  // paletteIndex
                    return pair.getPaletteIndex();
                case 1:  // shapeName
                    return pair.getShapeName();
                case 2:  // color
                    return pair.getColor();
                default:
                    return null;
            }
        }
        
        @Override
        public Class<?> getColumnClass(int col) {
            switch (col) {
                case 0:  // paletteIndex
                    return Integer.class;
                case 1:  // shapeName
                    return String.class;
                case 2:  // color
                    return ColorPtOstDot.class;
                default:
                    return Object.class;
            }
        }
    }
    
    /**
     * Custom renderer for color column that displays a colored swatch.
     */
    private static class ColorSwatchRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                      boolean isSelected, boolean hasFocus,
                                                      int row, int column) {
            
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            
            if (value instanceof ColorPtOstDot) {
                ColorPtOstDot color = (ColorPtOstDot) value;
                
                // Create color swatch
                JPanel swatch = new JPanel();
                swatch.setBackground(color.toAWTColor());
                swatch.setPreferredSize(new Dimension(60, 30));
                swatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                
                // Create info label with Ostwald coordinates
                String infoText = String.format("H:%d W:%d B:%d",
                    color.getHueNumGUI().getUserFacingHueNum(),
                    color.getOstHCS().getSeriesW(),
                    color.getOstHCS().getSeriesB());
                JLabel infoLabel = new JLabel(infoText);
                infoLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
                
                panel.add(swatch);
                panel.add(infoLabel);
            }
            
            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
            } else {
                panel.setBackground(table.getBackground());
            }
            
            return panel;
        }
    }
}
