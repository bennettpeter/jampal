package pgbennett.jampal.customize;

import java.util.Vector;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

class VoiceTableModel extends AbstractTableModel {
    
    Vector <VoiceEntry> entries;
    // true for library, false for editor
    boolean isLibrary;
    CustomizeDialog dialog;
    // Vector [] values;
    // Note values[0] is not used, the row num + 1 is the value
    // This is valued for libraryTableModel and null for
    // tagEditTableModel
    // Vector displayRows;
    static final int COL_SEQNUM = 0;
    static final int COL_LANGUAGE = 1;
    static final int COL_ENGINE = 2;
    static final int COL_VOICE = 3;
    static final int COL_VOLUME = 4;
    static final int NUM_COLS = 5;
    
    
    static final String[] tableTitles = new String[]{
        "#", "Language", "Engine", "Voice", "Volume"
    };
    static final int[] colWidths = {35, 200, 100, 300, 50};
    

    VoiceTableModel(CustomizeDialog dialog) {
        super();
        this.dialog = dialog;
        init();
    }
    
    void init() {
        entries = new Vector <VoiceEntry> ();
    }

    public int getColumnCount() {
        return NUM_COLS;
    }

    public int getRowCount() {
        return entries.size();
    }

    public Object getValueAt(int row, int column) {
        VoiceEntry entry =  entries.get(row);
        String value = null;
        switch (column) {
            case COL_SEQNUM:
                value = Integer.toString(row + 1);
                break;
            case COL_LANGUAGE:
                value = entry.language;
                if (value != null && value.length() > 0 && !"other".equals(value)) 
                    value = dialog.attributes.langCodeProps.getProperty(value);
                break;
            case COL_ENGINE:
                value = entry.engine;
                break;
            case COL_VOICE:
                value = entry.voice;
                break;
            case COL_VOLUME:
                value = entry.volume;
                break;
        }
        return value;
    }

    public void setValueAt(Object aValue, int row, int column) {
        VoiceEntry entry =  entries.get(row);
        switch (column) {
            case COL_SEQNUM:
                break;
            case COL_LANGUAGE:
                entry.language = (String) aValue;
                if (entry.language != null && entry.language.length() > 0 && !"other".equals(entry.language)) 
                    entry.language = (String) dialog.attributes.langNameMap.get(aValue);
                break;
            case COL_ENGINE:
                entry.engine = (String) aValue;
                entry.voice = "";
                entry.volume="100";
                fireTableRowsUpdated(row, row);
                break;
            case COL_VOICE:
                entry.voice = (String) aValue;
                break;
            case COL_VOLUME:
                    entry.volume = (String) aValue;
                break;
        }
        dialog.voicePage.isDirty = true;
    }

    public boolean isCellEditable(int row, int column) {
        if (column == COL_SEQNUM ) {
            return false;
        }
        VoiceEntry entry =  entries.get(row);
        
        if ("None".equals(entry.engine))
            if (column == COL_VOICE||column==COL_VOLUME)
                return false;
        
        return true;
    }

    public String getColumnName(int column) {
        return tableTitles[column];
    }

    void AddRow(JTable table) {
        VoiceEntry entry = new VoiceEntry();
        if (table.getEditingRow() != -1) {
            table.getCellEditor().cancelCellEditing();
        }
        // Always Add at end
        // int after = table.getSelectedRow();
        int after = -1;
        if (after == -1) {
            after = table.getRowCount() - 1;
        }
        int ix;
        int before = after + 1;

        entries.add(before, entry);

        fireTableRowsInserted(before, before);
        dialog.voicePage.isDirty = true;
    }

    void DeleteRow(JTable table) {
        if (table.getEditingRow() != -1) {
            table.getCellEditor().cancelCellEditing();
        }
        int row = table.getSelectedRow();
        if (row == -1) {
            return;
        }
        VoiceEntry entry =  entries.get(row);
        entries.removeElementAt(row);

        fireTableRowsDeleted(row, row);
        table.clearSelection();
        if (row >= getRowCount()) {
            row--;
        }
        if (row >= 0) {
            table.addRowSelectionInterval(row, row);
        }
        dialog.voicePage.isDirty = true;
    }

}
