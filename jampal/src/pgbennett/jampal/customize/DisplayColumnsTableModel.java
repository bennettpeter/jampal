package pgbennett.jampal.customize;

import java.util.Vector;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

class DisplayColumnsTableModel extends AbstractTableModel {

    CustomizeDialog dialog;
    static final int COL_SELECT = 6;
    static final int NUM_COLS = 7;
    Vector entries = new Vector();

    DisplayColumnsTableModel(CustomizeDialog dialog) {
        super();
        this.dialog = dialog;
    }

    public int getRowCount() {
        return dialog.libraryTableModel.entries.size();
    }

    public Class getColumnClass(int column) {
        if (column == COL_SELECT) {
            return Boolean.class;
        } else {
            return Object.class;
        }
    }

    public Object getValueAt(int row, int column) {
        DisplayEntry entry = (DisplayEntry) entries.get(row);
        if (column == COL_SELECT) {
            return new Boolean(entry.isSelected);
        }
        if (column == 0) {
            return Integer.toString(row + 1);
        }
        int libraryRow = dialog.libraryTableModel.entries.indexOf(entry.libraryEntry);
        if (libraryRow == -1) {
            return null;
        }
        return dialog.libraryTableModel.getValueAt(libraryRow, column);
    }

    public void setValueAt(Object aValue, int row, int column) {
        DisplayEntry entry = (DisplayEntry) entries.get(row);
        if (column == COL_SELECT) {
            entry.isSelected = ((Boolean) aValue).booleanValue();
        }
    }

    public int getColumnCount() {
        return NUM_COLS;
    }

    public String getColumnName(int column) {
        return dialog.libraryTableTitles[column];
    }

    public boolean isCellEditable(int row, int column) {
        if (column == COL_SELECT) {
            return true;
        }

        return false;
    }

    void MoveUp(JTable table) {
        if (table.getEditingRow() != -1) {
            table.getCellEditor().cancelCellEditing();
        }
        int row = table.getSelectedRow();
        if (row <= 0) {
            return;
        }
        Object second = entries.get(row);
        Object first = entries.set(row - 1, second);
        entries.set(row, first);
        fireTableRowsUpdated(row - 1, row);
        table.clearSelection();
        table.addRowSelectionInterval(row - 1, row - 1);
    }

    void MoveDown(JTable table) {
        if (table.getEditingRow() != -1) {
            table.getCellEditor().cancelCellEditing();
        }
        int row = table.getSelectedRow();
        if (row < 0 || row >= getRowCount() - 1) {
            return;
        }
        Object second = entries.get(row + 1);
        Object first = entries.set(row, second);
        entries.set(row + 1, first);
        fireTableRowsUpdated(row, row + 1);
        table.clearSelection();
        table.addRowSelectionInterval(row + 1, row + 1);
    }
}
