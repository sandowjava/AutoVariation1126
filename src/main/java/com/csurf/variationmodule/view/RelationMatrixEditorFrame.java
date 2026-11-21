package com.csurf.variationmodule.view;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.variationmodule.model.RelationMatrixDraft;
import com.csurf.variationmodule.model.RelationMatrixDraft.DraftRow;
import com.csurf.variationmodule.model.ShapeColorPrefs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;
import java.util.UUID;

/**
 * Window for editing a relation matrix (the totality of a SmartPic's
 * ShapeColorPrefs): a table of prefs rows bound to a Template's DrawObjs.
 *
 * <p>View only — menu and cell actions delegate to the controller via
 * {@link Actions}. The table model wraps the controller-owned
 * {@link RelationMatrixDraft}; cells write through to it.
 *
 * <p>Columns: {@code #} (draw-order position), {@code Shape} (bound
 * DrawObj name), {@code Bind To} (DrawObj dropdown — the re-bind
 * mechanism), {@code Value Target}, {@code Default AG} (dropdown 1..4),
 * and one editable column per AG preference weight. Unbound rows sort to
 * the bottom and show their file-carried shapeName in parentheses.
 *
 * <p>Design doc: {@code 112126_v0G_SWE-2_RelationMatrix_and_Editor-UI_Design.md}.
 */
public class RelationMatrixEditorFrame extends JFrame {

    /**
     * Screen position where this window opens: upper-left of the screen,
     * just below the CreateSmartPicFrame window. Edit this to move it.
     */
    public static final Point OPEN_LOCATION = new Point(0, 300);

    /** Combo item index 0 in the Bind To column: no binding. */
    private static final String UNBOUND_ITEM = "(unbound)";

    /**
     * Callbacks the frame's menu items and table edits invoke;
     * implemented by the controller.
     */
    public interface Actions {
        /** User chose "Choose saved Template...". */
        void chooseTemplate();
        /** User chose "Open matrix file...". */
        void openMatrixFile();
        /** User chose "Save". */
        void save();
        /** User chose "Save As...". */
        void saveAs();
        /** User chose "Bind by Position" (Binding menu). */
        void bindByPosition();
        /**
         * A numeric or AG cell was edited.
         *
         * @return an error message to show in the status bar, or null on success
         */
        String cellEdited(DraftRow row, int column, String text);
        /**
         * The Bind To selection changed for {@code row}.
         *
         * @param drawObjUuid the chosen DrawObj's UUID, or null for unbound
         */
        void rowBoundTo(DraftRow row, UUID drawObjUuid);
    }

    private final Actions actions;
    private final MatrixTableModel tableModel;
    private final JTable table;
    private final DefaultComboBoxModel<String> bindComboModel;
    private final JComboBox<String> bindCombo;
    private final JComboBox<Integer> defaultAgCombo;

    private final JLabel templateLabel;
    private final JLabel fileLabel;
    private final JLabel bindingLabel;
    private final JLabel statusLabel;

    private List<DrawObj> templateDrawObjs = List.of();

    /**
     * @param actions menu/table-action callbacks (the controller)
     * @param title   window title
     */
    public RelationMatrixEditorFrame(Actions actions, String title) {
        super(title);
        this.actions = actions;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // ---------- menus ----------
        JMenu fileMenu = new JMenu("File");
        JMenuItem chooseTemplateItem = new JMenuItem("Choose saved Template...");
        chooseTemplateItem.addActionListener(e -> actions.chooseTemplate());
        fileMenu.add(chooseTemplateItem);
        JMenuItem openMatrixItem = new JMenuItem("Open matrix file...");
        openMatrixItem.addActionListener(e -> actions.openMatrixFile());
        fileMenu.add(openMatrixItem);
        fileMenu.addSeparator();
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> actions.save());
        fileMenu.add(saveItem);
        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.addActionListener(e -> actions.saveAs());
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(e -> dispose());
        fileMenu.add(closeItem);

        JMenu bindingMenu = new JMenu("Binding");
        JMenuItem bindByPositionItem = new JMenuItem("Bind by Position");
        bindByPositionItem.addActionListener(e -> actions.bindByPosition());
        bindingMenu.add(bindByPositionItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(bindingMenu);
        setJMenuBar(menuBar);

        // ---------- header ----------
        JPanel header = new JPanel(new GridLayout(3, 1, 0, 2));
        header.setBorder(new EmptyBorder(8, 10, 8, 10));
        templateLabel = new JLabel("Template: (none chosen)");
        fileLabel = new JLabel("Matrix file: (none open)");
        bindingLabel = new JLabel(" ");
        header.add(templateLabel);
        header.add(fileLabel);
        header.add(bindingLabel);

        // ---------- table ----------
        tableModel = new MatrixTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);

        bindComboModel = new DefaultComboBoxModel<>();
        bindCombo = new JComboBox<>(bindComboModel);
        defaultAgCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4});

        // ---------- status ----------
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(new EmptyBorder(4, 10, 6, 10));

        JPanel content = new JPanel(new BorderLayout());
        content.add(header, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        content.add(statusLabel, BorderLayout.SOUTH);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(content, BorderLayout.CENTER);

        setPreferredSize(new Dimension(900, 560));
        pack();
    }

    // ---------- controller-facing setters ----------

    /** The draft to display and edit; re-reads all rows. */
    public void setDraft(RelationMatrixDraft draft) {
        tableModel.draft = draft;
        refreshTable();
    }

    /** The chosen Template's DrawObjs — populates the Bind To dropdown. */
    public void setTemplateDrawObjs(List<DrawObj> drawObjs) {
        this.templateDrawObjs = drawObjs != null ? drawObjs : List.of();
        bindComboModel.removeAllElements();
        bindComboModel.addElement(UNBOUND_ITEM);
        for (DrawObj drawObj : this.templateDrawObjs) {
            bindComboModel.addElement(drawObjLabel(drawObj));
        }
        refreshTable();
    }

    /** Displays the chosen Template's name. */
    public void setTemplateName(String name) {
        templateLabel.setText("Template: " + name);
    }

    /** Displays the open file's name (and dirty marker if given). */
    public void setFileLabel(String text) {
        fileLabel.setText("Matrix file: " + text);
    }

    /** Displays binding status, e.g. "all rows bound" / "3 rows unbound". */
    public void setBindingStatus(String text) {
        bindingLabel.setText(text);
    }

    /** Displays a status bar message. */
    public void setStatus(String text) {
        statusLabel.setText(" " + text);
    }

    /** Re-reads all rows from the draft (after binding changes). */
    public void refreshTable() {
        tableModel.fireTableDataChanged();
    }

    private static String drawObjLabel(DrawObj drawObj) {
        String uuid = drawObj.getUniqueId() != null
                ? drawObj.getUniqueId().toString().substring(0, 8) : "????????";
        String name = drawObj.getName() != null ? drawObj.getName() : "(unnamed)";
        return name + "  · " + uuid;
    }

    private UUID uuidForComboIndex(int comboIndex) {
        // index 0 = UNBOUND_ITEM; 1..N = templateDrawObjs.get(0..N-1)
        if (comboIndex <= 0 || comboIndex > templateDrawObjs.size()) {
            return null;
        }
        return templateDrawObjs.get(comboIndex - 1).getUniqueId();
    }

    private int comboIndexForRow(DraftRow row) {
        if (row.boundDrawObjUuid() == null) {
            return 0;
        }
        for (int i = 0; i < templateDrawObjs.size(); i++) {
            if (templateDrawObjs.get(i).getUniqueId().equals(row.boundDrawObjUuid())) {
                return i + 1;
            }
        }
        return 0;
    }

    // ---------- table model ----------

    private final class MatrixTableModel extends AbstractTableModel {

        private static final int COL_POS = 0;
        private static final int COL_SHAPE = 1;
        private static final int COL_BIND = 2;
        private static final int COL_VALUE = 3;
        private static final int COL_AG = 4;
        private static final int COL_PREF1 = 5;
        private static final int COLS = COL_PREF1 + ShapeColorPrefs.NUM_AGS;

        private RelationMatrixDraft draft;
        private List<DraftRow> displayRows = List.of();

        private List<DraftRow> rows() {
            if (draft == null) {
                return List.of();
            }
            displayRows = draft.displayRows();
            return displayRows;
        }

        @Override
        public int getRowCount() {
            return rows().size();
        }

        @Override
        public int getColumnCount() {
            return COLS;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_POS: return "#";
                case COL_SHAPE: return "Shape";
                case COL_BIND: return "Bind To";
                case COL_VALUE: return "Value Target";
                case COL_AG: return "Default AG";
                default: return "AG" + (column - COL_PREF1 + 1) + " Pref";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (columnIndex <= COL_SHAPE) {
                return false;
            }
            if (columnIndex == COL_BIND) {
                return !templateDrawObjs.isEmpty();
            }
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DraftRow row = rows().get(rowIndex);
            switch (columnIndex) {
                case COL_POS: {
                    if (row.isBound()) {
                        int pos = 0;
                        for (int i = 0; i < templateDrawObjs.size(); i++) {
                            if (templateDrawObjs.get(i).getUniqueId()
                                    .equals(row.boundDrawObjUuid())) {
                                pos = i + 1;
                                break;
                            }
                        }
                        return pos > 0 ? pos : "?";
                    }
                    return "–";
                }
                case COL_SHAPE: {
                    DrawObj bound = draft.boundDrawObj(row);
                    if (bound != null && bound.getName() != null) {
                        return bound.getName();
                    }
                    return row.fileShapeName() != null
                            ? "(" + row.fileShapeName() + ")"
                            : "(row " + row.fileShape() + ")";
                }
                case COL_BIND:
                    if (bindComboModel.getSize() == 0) {
                        return UNBOUND_ITEM;
                    }
                    return bindComboModel.getElementAt(comboIndexForRow(row));
                case COL_VALUE:
                    return row.valueTarget();
                case COL_AG:
                    return row.defaultAG();
                default:
                    return row.agPref(columnIndex - COL_PREF1 + 1);
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            DraftRow row = rows().get(rowIndex);
            if (columnIndex == COL_BIND) {
                int idx = value != null
                        ? bindComboModel.getIndexOf(value)
                        : comboIndexForRow(row);
                if (idx < 0) {
                    idx = comboIndexForRow(row); // unrecognized text: keep current binding
                }
                actions.rowBoundTo(row, uuidForComboIndex(idx));
                return;
            }
            String err = actions.cellEdited(row, columnIndex, String.valueOf(value));
            setStatus(err != null ? err : " ");
        }
    }

    /** Installs cell editors after construction (combo sharing per column). */
    private void installEditors() {
        TableColumn bindCol = table.getColumnModel().getColumn(MatrixTableModel.COL_BIND);
        bindCol.setCellEditor(new DefaultCellEditor(bindCombo));
        bindCol.setPreferredWidth(170);
        TableColumn agCol = table.getColumnModel().getColumn(MatrixTableModel.COL_AG);
        agCol.setCellEditor(new DefaultCellEditor(defaultAgCombo));

        table.getColumnModel().getColumn(MatrixTableModel.COL_POS)
                .setPreferredWidth(30);
        table.getColumnModel().getColumn(MatrixTableModel.COL_SHAPE)
                .setPreferredWidth(160);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c = MatrixTableModel.COL_VALUE; c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(right);
            table.getColumnModel().getColumn(c).setPreferredWidth(80);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            installEditors();
        }
        super.setVisible(visible);
    }
} // end class RelationMatrixEditorFrame
