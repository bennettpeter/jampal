/*
    Copyright 2004 Peter Bennett

    This file is part of Jampal.

    Jampal is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Jampal is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Jampal.  If not, see <http://www.gnu.org/licenses/>.
*/
package pgbennett.jampal.customize;

import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import pgbennett.id3.FrameAttributes;
import pgbennett.id3.FrameDictionary;
import pgbennett.jampal.TrackEditor;

class TagEditTableModel extends AbstractTableModel {

    JComboBox frameComboBox;
    JComboBox typeComboBox;
    Vector <LibraryEntry> entries;
    // true for library, false for editor
    boolean isLibrary;
    CustomizeDialog dialog;
    // Vector [] values;
    // Note values[0] is not used, the row num + 1 is the value
    // This is valued for libraryTableModel and null for
    // tagEditTableModel
    // Vector displayRows;
    static final int COL_SEQNUM = 0;
    static final int COL_TYPE = 1;
    static final int COL_FRAME = 2;
    static final int COL_LANGUAGE = 3;
    static final int COL_DESCRIPTION = 4;
    static final int COL_TITLE = 5;
    static final int NUM_COLS = 6;

    TagEditTableModel(CustomizeDialog dialog) {
        super();
        this.dialog = dialog;
        init();
    }

    void init() {
        entries = new Vector <LibraryEntry> ();
    }

    public int getColumnCount() {
        return NUM_COLS;
    }

    public int getRowCount() {
        return entries.size();
    }

    public Object getValueAt(int row, int column) {
        LibraryEntry entry =  entries.get(row);
        String value = null;
        switch (column) {
            case COL_SEQNUM:
                value = Integer.toString(row + 1);
                break;
            case COL_TYPE:
                value = entry.type;
                break;
            case COL_FRAME:
                value = entry.frame;
                if (value != null && value.length() > 0) {
                    FrameAttributes frameAtt =  FrameDictionary.getSingleton().frameTypes.get(value);
                    value = frameAtt.colHeading;
                }
                break;
            case COL_LANGUAGE:
                value = entry.language;
                if (value != null && value.length() > 0) {
                    FrameAttributes frameAtt =  FrameDictionary.getSingleton().frameTypes.get(entry.frame);
                    if (frameAtt.langReq) {
                        value = dialog.attributes.langCodeProps.getProperty(value);
                    } else if (frameAtt.type == 'I') {
                        int picIndex = Integer.parseInt(value, 16);
                        value = TrackEditor.pictureTypes[picIndex];
                    }
                }
                break;
            case COL_DESCRIPTION:
                value = entry.description;
                break;
            case COL_TITLE:
                value = entry.title;
                break;
        }
        return value;
    }

    public void setValueAt(Object aValue, int row, int column) {
        String value = (String) aValue;
        LibraryEntry entry =  entries.get(row);
        FrameAttributes frameAtt;
        switch (column) {
            case COL_SEQNUM:
                break;
            case COL_TYPE:
                if (!value.equals(entry.type)) {
                    String frameName = FrameDictionary.getSingleton().frameProperties.getProperty("FRAME-" + value);
                    if (frameName != null) {
                        entry.frame = frameName;
                        entry.language = null;
                        entry.description = null;
                    } else if ("ID3V2TAG".equals(value)) {
                        if (entry.frame == null) {
                            entry.frame = "TXXX";
                            entry.language = null;
                            entry.description = null;
                        }
                    } else {
                        entry.frame = null;
                        entry.language = null;
                        entry.description = null;
                    }
                    entry.type = value;
                    if (value.equals(dialog.typeExtensions[0])) {
                        // JAMPAL
                        entry.type = "ID3V2TAG";
                        entry.frame = "TXXX";
                        entry.language = null;
                        entry.description = "jampal";
                        if (isLibrary) {
                            entry.title = "F";
                        } else {
                            entry.title = "Jampal";
                        }
                    }
                    if (value.equals(dialog.typeExtensions[1])) {
                        // ALTERNATE
                        entry.type = "ID3V2TAG";
                        entry.frame = "COMM";
                        entry.language = "eng";
                        entry.description = "Jampal:Alternate";
                        if (isLibrary) {
                            entry.title = "Alternate";
                        } else {
                            entry.title = "Alternate Album : Track";
                        }
                    }
                    fireTableRowsUpdated(row, row);
                    if (isLibrary) {
                        dialog.mustReload = true;
                    }
                }
                break;
            case COL_FRAME:
                frameAtt = (FrameAttributes) FrameDictionary.getSingleton().frameNameMap.get(value);
                value = frameAtt.id;
                if (!value.equals(entry.frame)) {
                    if (frameAtt.langReq) {
                        entry.language = "eng";
                    } else if (frameAtt.type == 'I') {
                        entry.language = "00";
                    } else {
                        entry.language = null;
                    }
                    if (!frameAtt.descReq && frameAtt.type != 'I') {
                        entry.description = null;
                    }
                    entry.frame = value;
                    fireTableRowsUpdated(row, row);
                    if (isLibrary) {
                        dialog.mustReload = true;
                    }
                }
                break;
            case COL_LANGUAGE:
                frameAtt = FrameDictionary.getSingleton().frameTypes.get(entry.frame);
                if (frameAtt.langReq) {
                    value = (String) dialog.attributes.langNameMap.get(value);
                } else if (frameAtt.type == 'I') {
                    int ix;
                    String search = value;
                    value = "00";
                    for (ix = 0; ix < TrackEditor.pictureTypes.length; ix++) {
                        if (search.equals(TrackEditor.pictureTypes[ix])) {
                            value = Integer.toString(ix, 16);
                            if (value.length() == 1) {
                                value = "0" + value;
                            }
                        }
                    }
                }
                if (!value.equals(entry.language)) {
                    entry.language = value;
                    if (isLibrary) {
                        dialog.mustReload = true;
                    }
                }
                break;
            case COL_DESCRIPTION:
                if (!value.equals(entry.description)) {
                    entry.description = value;
                    if (isLibrary) {
                        dialog.mustReload = true;
                    }
                }
                break;
            case COL_TITLE:
                entry.title = value;
                break;
        }
//        dialog.populateDeleteFrame();

    }

    public boolean isCellEditable(int row, int column) {
        if (column == COL_TYPE || column == COL_TITLE) {
            return true;
        }
        LibraryEntry entry =  entries.get(row);

        if (!"ID3V2TAG".equals(entry.type)) {
            return false;
        }
        if (column == COL_FRAME) {
            return true;
        }
        if (entry.frame == null) {
            return false;
        }
        FrameAttributes frameAttributes = (FrameAttributes) FrameDictionary.getSingleton().frameTypes.get(entry.frame);

        if (column == COL_LANGUAGE) {
            if (frameAttributes.langReq || frameAttributes.type == 'I') {
                return true;
            } else {
                return false;
            }
        }
        if (column == COL_DESCRIPTION) {
            if (frameAttributes.descReq || frameAttributes.type == 'I') {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    public String getColumnName(int column) {
        return dialog.libraryTableTitles[column];
    }

    void AddRow(JTable table) {
        LibraryEntry entry = new LibraryEntry();
        if (table.getEditingRow() != -1) {
            table.getCellEditor().cancelCellEditing();
        }
        int after = table.getSelectedRow();
        if (after == -1) {
            after = table.getRowCount() - 1;
        }
        int ix;
        int before = after + 1;

        entries.add(before, entry);

        entry.type = "ID3V2TAG";
        entry.frame = "TXXX";
        fireTableRowsInserted(before, before);
        if (isLibrary) {
            int dCount = dialog.displayColumnsTableModel.entries.size();
            DisplayEntry dEntry = new DisplayEntry();
            dEntry.libraryEntry = entry;
            entry.displayEntry = dEntry;
            dialog.displayColumnsTableModel.entries.add(dEntry);
            dialog.displayColumnsTableModel.fireTableRowsInserted(dCount, dCount);
//            dialog.populateDeleteFrame();
        }
    }

    void DeleteRow(JTable table) {
        if (table.getEditingRow() != -1) {
            table.getCellEditor().cancelCellEditing();
        }
        int row = table.getSelectedRow();
        if (row == -1) {
            return;
        }
        LibraryEntry entry =  entries.get(row);
        entries.removeElementAt(row);

        fireTableRowsDeleted(row, row);
        table.clearSelection();
        if (row >= getRowCount()) {
            row--;
        }
        if (row >= 0) {
            table.addRowSelectionInterval(row, row);
        }
        if (isLibrary) {
            dialog.displayColumnsTableModel.entries.removeElement(entry.displayEntry);
            dialog.displayColumnsTableModel.fireTableDataChanged();
//            dialog.populateDeleteFrame();
        }
    }

    void MoveUp(JTable table) {
        if (table.getEditingRow() != -1) {
            table.getCellEditor().cancelCellEditing();
        }
        int row = table.getSelectedRow();
        if (row <= 0) {
            return;
        }
        LibraryEntry second = entries.get(row);
        LibraryEntry first = entries.set(row - 1, second);
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
        LibraryEntry second = entries.get(row + 1);
        LibraryEntry first = entries.set(row, second);
        entries.set(row + 1, first);
        fireTableRowsUpdated(row, row + 1);
        table.clearSelection();
        table.addRowSelectionInterval(row + 1, row + 1);
    }
}
