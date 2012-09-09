package pgbennett.jampal;

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


import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import pgbennett.id3.*;
import pgbennett.utility.TextSimplifier;

class LibraryTableModel extends AbstractTableModel {
    Library library;
    int displaySortCols[];
    boolean displaySortReverse[];

    int colSortSequences[];
    boolean colSortReverse[];
    LibraryTrack [] tracksSort;
    // Attributes for current sort column
    boolean sortAlbumNormalize=false;
    boolean sortNumeric=false;
    
    ImageIcon sortAscIcon[] = {
        new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/sort_asc.png")),
        new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/sort_asc_med.png")),
        new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/sort_asc_sml.png"))
    };
    ImageIcon sortDescIcon[] = {
        new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/sort_desc.png")),
        new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/sort_desc_med.png")),
        new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/sort_desc_sml.png"))
    };

    public LibraryTableModel(Library library) {
        this.library = library;
        displaySortCols = new int[0];
        displaySortReverse = new boolean[0];
        colSortSequences = new int [library.attributes.numDisplayCol];
        colSortReverse = new boolean[library.attributes.numDisplayCol];
        tracksSort = new LibraryTrack[0];
    }
    public int getColumnCount() {
        return library.attributes.numDisplayCol;
    }

    public int getRowCount() {
        return library.trackVec.size();
    }

    @Override
    public String getColumnName(int col) {
        String ret = library.attributes.colHeading[library.attributes.displayCol[col]];
        if (ret.length()==0)
            ret=" ";
        return ret;
    }

    public Object getValueAt(int row, int col) {
        LibraryTrack track = library.trackVec.get(row);
        if (track.status=='D')
            return null;
        String value =  track.columns[library.attributes.displayCol[col]];
        int type=library.attributes.colType[library.attributes.displayCol[col]];
        if (type==MP3File.COL_ID3V2TAG) {
            String frame=library.attributes.colId[library.attributes.displayCol[col]];
            if ("TLAN".equals(frame)) {
                StringBuilder langs = new StringBuilder();
                for (int i = 0; i+3<=value.length(); i+=3) {
                    if (i>0)
                        langs.append("/");
                    langs.append(library.attributes.langCodeProps.getProperty(value.substring(i, i+3)));
                }
                value=langs.toString();
            }
        }
        return value;
    }


    /*
     * Don't need to implement this method unless your table's
     * editable.
     */
    public boolean isCellEditable(int row, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears onscreen.
        if (!library.mainFrame.cellEditCheckBox.isSelected()) {
            if (row!=library.mainFrame.editRow||col!=library.mainFrame.editCol) {
                library.mainFrame.editRow=-1;
                library.mainFrame.editCol=-1;
                return false;
            }
        }
        LibraryTrack track = (LibraryTrack)library.trackVec.get(row);
        if (track.status=='D')
            return false;
        int type=library.attributes.colType[library.attributes.displayCol[col]];
        if (type==MP3File.COL_FILENAME) 
            return false;

        switch (type) {
        case MP3File.COL_ID3V2TAG:
            String frame = library.attributes.colId[library.attributes.displayCol[col]];
            if ("TLAN".equals(frame)) {
                String value =  track.columns[library.attributes.displayCol[col]];
                if (value.length() > 3)
                    return false;
            }
        case MP3File.COL_TITLE:
        case MP3File.COL_ARTIST:
        case MP3File.COL_YEAR:
        case MP3File.COL_COMMENT:
        case MP3File.COL_GENRE:
        case MP3File.COL_DUMMY:
            return true;
        case MP3File.COL_ALBUM:
        case MP3File.COL_TRACK:
            if (track.alternateNum == 0)
                return true;
        }
        return false;
    }



    /*
     * Don't need to implement this method unless your table's
     * data can change.
     */
    public void setValueAt(Object value, int row, int col) {
        LibraryTrack track = (LibraryTrack)library.trackVec.get(row);
        String fileName=track.columns[library.attributes.fileNameCol];
        if (track.status == 'D'||value==null)
            return;
        value=((String)value).trim();

        int libraryCol = library.attributes.displayCol[col];
        int colType = library.attributes.colType[libraryCol];
        String priorValue = track.columns[libraryCol];
        if (value.equals(priorValue))
            return;
        
        int type=library.attributes.colType[library.attributes.displayCol[col]];
        if (type==MP3File.COL_ID3V2TAG) {
            String frame=library.attributes.colId[library.attributes.displayCol[col]];
            if ("TLAN".equals(frame)) {
                value=library.attributes.langNameMap.get(value);
            }
        }
        
        try {
            if (colType==MP3File.COL_DUMMY) {
                track.columns[libraryCol] = value.toString();
                track.status = 'U';
                library.updateLibraryTrack(track);
                track.status = 'A';
                library.mainFrame.editRow=-1;
                library.mainFrame.editCol=-1;
                return;
            }

            String id = library.attributes.colId[libraryCol];

            File file = new File(fileName);
            MP3File mp3 = new MP3File();
            try {
                mp3.init(file,MP3File.BOTH_TAGS);
            }
            catch (ID3Exception ex) {
                ex.printStackTrace();
            }
            mp3.syncV1AndV2();
            mp3.setMp3Field( colType, id, null, null,(String)value) ;
            mp3.writeTags();
            library.updateTrackAllViews(new File(fileName),null);
            if (library.mainFrame.trackEditor!=null) {
                if (fileName.equals(library.mainFrame.trackEditor.fileName))
                    library.mainFrame.trackEditor.refresh();
            }
            // Java 5 requires us to reset the focus cell
            int viewIndex = library.mainFrame.mainTable.convertColumnIndexToView(col);
            library.mainFrame.mainTable.addColumnSelectionInterval(viewIndex,viewIndex); 
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(library.mainFrame.frame,
            ex.toString(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        }
        library.mainFrame.editRow=-1;
        library.mainFrame.editCol=-1;
        
    }


    void setSortCol(int col) {
        Icon sortIcon;
        TableColumnModel columnModel = library.mainFrame.mainTable.getColumnModel();
        int ix;
        resetHeaderIcons();
        //Second click reverses sort
        if (displaySortCols.length>0 && col==displaySortCols[0]) {
            displaySortReverse[0]=!displaySortReverse[0];
        }
        else {
            int [] temp = new int[displaySortCols.length+1];
            boolean [] tempRev = new boolean[displaySortCols.length+1];
            temp[0]=col;
            tempRev[0]=false;
            int i;
            int fileNameCol=-1;
            for (i=0;i<library.attributes.numDisplayCol;i++) {
                int type=library.attributes.colType[library.attributes.displayCol[i]];
                if (type==MP3File.COL_FILENAME) {
                    fileNameCol=i;
                    break;
                }
            }
            int iOut=1;
            int iIn;
            int iPrev;
            // Go through and add prev sort fields to list
            for (iIn=0;iIn<displaySortCols.length;iIn++) {
                // Check that previous field was not filename
                if (temp[iOut-1]==fileNameCol)
                    break;
                // Check that this field was not there already
                boolean dup=false;
                for (iPrev=0;iPrev<iOut;iPrev++) {
                    if (temp[iPrev]==displaySortCols[iIn]) {
                        dup=true;
                        break;
                    }
                }
                if (dup)
                    continue;
                temp[iOut]=displaySortCols[iIn];
                tempRev[iOut]=displaySortReverse[iIn];
                iOut++;
            }
            displaySortCols = new int[iOut];
            displaySortReverse = new boolean[iOut];
            colSortSequences = new int [library.attributes.numDisplayCol];
            colSortReverse = new boolean[library.attributes.numDisplayCol];
            for (i=0;i<iOut;i++) {
                displaySortCols[i]=temp[i];
                displaySortReverse[i]=tempRev[i];
                colSortSequences[temp[i]]=i+1;
                colSortReverse[temp[i]]=tempRev[i];
            }
        }
        for (ix=0; ix<3 && ix<displaySortCols.length; ix++){
            if (displaySortReverse[ix])
                sortIcon=sortDescIcon[ix];
            else
                sortIcon=sortAscIcon[ix];
            int viewIndex = library.mainFrame.mainTable.convertColumnIndexToView(displaySortCols[ix]);
            TableColumn column = columnModel.getColumn(viewIndex);
            MainFrame.MyRenderer renderer= (MainFrame.MyRenderer)column.getHeaderRenderer();
            renderer.setIcon(sortIcon);
        }
            
        JTableHeader header = library.mainFrame.mainTable.getTableHeader();
        header.repaint();
        refresh();
    }

    void resetHeaderIcons() {
        TableColumnModel columnModel = library.mainFrame.mainTable.getColumnModel();
        int ix;
        for (ix=0; ix<3 && ix<displaySortCols.length; ix++){
            int viewIndex = library.mainFrame.mainTable.convertColumnIndexToView(displaySortCols[ix]);
            TableColumn column = columnModel.getColumn(viewIndex);
            MainFrame.MyRenderer renderer= (MainFrame.MyRenderer)column.getHeaderRenderer();
            renderer.setIcon(null);
        }
    }
    
    
    void shuffle() {
        Component header=library.mainFrame.mainTable.getTableHeader();
        // This may mess up header cursors.
        // 20090710 header.setCursor(null);
        library.mainFrame.frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        resetSort(true);
        // This stops it at the end of the song
        library.mainFrame.player.selectedRow=-1;
        displaySortCols = new int[1];
        displaySortCols[0] = 0;
        displaySortReverse = new boolean[1];
        displaySortReverse[0] = false;
        colSortSequences = new int [library.attributes.numDisplayCol];
        colSortReverse = new boolean[library.attributes.numDisplayCol];
        int i;
        Random random = new Random();
        for (i=0;i<library.trackVec.size();i++) {
            LibraryTrack track;
            track=(LibraryTrack)library.trackVec.get(i);
            if (track.status=='D') {
                library.trackVec.remove(i--);
                continue;
            }
            int key = random.nextInt(99999)+100000;
            track.sortKey=String.valueOf(key);
        }
        MyComparator comparator = new MyComparator (displaySortCols, displaySortReverse, 
            library.attributes, false);
        tracksSort = new LibraryTrack[library.trackVec.size()];
        tracksSort = library.trackVec.toArray(tracksSort);
        Arrays.sort(tracksSort,comparator);
        library.trackVec = new Vector();
        for (i=0;i<tracksSort.length;i++) {
                library.trackVec.add(tracksSort[i]);
        }
        displaySortCols = new int[0];
        displaySortReverse = new boolean[0];
        selectAndScrollRowtoVisible(0,0);
        library.mainFrame.frame.setCursor(null);
    }

    void resetSort(boolean mustFire) {
        resetHeaderIcons();
        JTableHeader header = library.mainFrame.mainTable.getTableHeader();
        header.repaint();
        displaySortCols = new int[0];
        displaySortReverse = new boolean[0];
        tracksSort = new LibraryTrack[0];
        if (mustFire)
            fireTableDataChanged();
    }
    
    // Search is the automatic binary search on the sort key that osscurs if you
    // type into the window.
    void search(String search) {
        if (tracksSort.length==0||displaySortCols.length==0)
            return;
        LibraryTrack searchArg = new LibraryTrack();
        search=normalizeSortKey(search);
        searchArg.sortKey = search;
        MyComparator comparator = new MyComparator (displaySortCols, displaySortReverse, 
            library.attributes, false);
        int ix = Arrays.binarySearch(tracksSort,searchArg,comparator);
        if (ix < 0)
            ix = -ix -1;
        if (ix < 0)
            ix = 0;
        if (ix >= tracksSort.length)
            ix = tracksSort.length - 1;
        int sortColViewIndex = library.mainFrame.mainTable.convertColumnIndexToView(displaySortCols[0]);
        selectAndScrollRowtoVisible(ix, sortColViewIndex);
//        TableColumnModel columnModel =  library.mainFrame.mainTable.getColumnModel();
//        ListSelectionModel columnSelectionModel = columnModel.getSelectionModel();
//        library.mainFrame.editorUpdateCount=2;
//        library.mainFrame.mainTable.clearSelection();
//        library.mainFrame.mainTable.addRowSelectionInterval(ix, ix);
//        Rectangle cell = library.mainFrame.mainTable.getCellRect(ix,
//                             displaySortCols[0],true);
//        // include cell above and below
//        cell.y -= cell.height;
//        cell.height *= 3;
//        library.mainFrame.mainTable.scrollRectToVisible(cell);
//        columnSelectionModel.setAnchorSelectionIndex(displaySortCols[0]);
    }

    // This takes view indexes

    void selectAndScrollRowtoVisible(int ix, int col) {
//        TableColumnModel columnModel =  library.mainFrame.mainTable.getColumnModel();
//        ListSelectionModel columnSelectionModel = columnModel.getSelectionModel();
        library.mainFrame.editorUpdateCount=2;
        library.mainFrame.mainTable.clearSelection();
        library.mainFrame.mainTable.addRowSelectionInterval(ix, ix);
        Rectangle cell = library.mainFrame.mainTable.getCellRect(ix,
                             col,true);
        // include cell above and below
        cell.y -= cell.height;
        cell.height *= 3;
        library.mainFrame.mainTable.scrollRectToVisible(cell);
        //columnSelectionModel.setAnchorSelectionIndex(col);
//        int viewIndex = library.mainFrame.mainTable.convertColumnIndexToView(col);
        library.mainFrame.mainTable.addColumnSelectionInterval(col,col);
        library.mainFrame.mainTable.requestFocusInWindow();
//        columnSelectionModel.setSelectionInterval(col,col);

    }
    
    // Find is from the find dialog box
    void find(String find, boolean useSelectedCol, boolean regex,
            boolean findAll, boolean caseInsens){

        Pattern pat=null;
        if (regex) {
            try {
                if (caseInsens) 
                    pat = Pattern.compile(find, Pattern.CASE_INSENSITIVE);
                else
                    pat = Pattern.compile(find);
            }
            catch(Exception ex) {
                ex.printStackTrace();
                pat=null;
            }
        }
        
        if (caseInsens)
            find = TextSimplifier.simplifyString(find,true);
        int selectedRow=-1;
        int firstFoundRow=-1;
        if (!findAll)
            selectedRow=library.mainFrame.mainTable.getSelectedRow();

        if (!library.mainFrame.editorUpdateActionListening) {
            library.mainFrame.timer.addActionListener(library.mainFrame.editorUpdateActionListener);
            library.mainFrame.editorUpdateActionListening=true;
        }

        int selectedCol = -1;
        if (useSelectedCol) {
//            int selectedColCount = library.mainFrame.mainTable.getSelectedColumnCount();
//            if (selectedColCount != 1) {
//                selectedCol = -1;
//                useSelectedCol = false;
//            }
//            else
//                selectedCol = library.mainFrame.mainTable.getSelectedColumn();
            selectedCol = library.mainFrame.focusCol;
            if (selectedCol == -1)
                useSelectedCol = false;
        }
        library.mainFrame.editorUpdateCount=2;
        library.mainFrame.mainTable.clearSelection();
        
        int ix;
//        if (useSelectedCol) {
//            if (caseInsens)
//                find=normalizeSortKey(find);
//            if (tracksSort.length==0||displaySortCols.length==0)
//                return;
//            for (ix=selectedRow+1; ix<tracksSort.length;ix++) {
//                LibraryTrack tr = (LibraryTrack)tracksSort[ix];
//                boolean found=false;
//                int sortColNum = library.attributes.displayCol[displaySortCols[0]];
//                String data = tr.columns[selectedCol];
//                if (data.length()==0)
//                    continue;
//                if (pat!=null) {
//                    Matcher mat=pat.matcher(data);
//                    found=mat.find();
//                }
//                else {
//                    if (caseInsens) {
//                        if (tr.sortKey.indexOf(find)!=-1)
//                            found=true;
//                    }
//                    else {
//                        if (data.indexOf(find)!=-1)
//                            found=true;
//                    }
//                }
//                if (found) {
//                    if (firstFoundRow==-1)
//                        firstFoundRow=ix;
//                    library.mainFrame.mainTable.addRowSelectionInterval(ix, ix);
//                    if (!findAll)
//                        break;
//                }
//            }
//        }
//        else {
            
        int size=library.trackVec.size();
        int startCol = 0;
        int endCol=library.attributes.numDisplayCol;
        if (useSelectedCol) {
            if (selectedCol != -1) {
                int modelSelectedCol = library.mainFrame.mainTable.convertColumnIndexToModel(selectedCol);
                startCol = modelSelectedCol;
                endCol = modelSelectedCol + 1;
            }

        }
        trackloop:
        for (ix=selectedRow+1; ix<size;ix++) {
            LibraryTrack tr = (LibraryTrack)library.trackVec.get(ix);
            int col;
            for (col=startCol;col<endCol;col++){
                boolean found=false;
                String data=tr.columns[library.attributes.displayCol[col]];
                if (data.length()==0)
                    continue;
                if (pat!=null) {
                    Matcher mat=pat.matcher(data);
                    found=mat.find();
                }
                else {
                    if (caseInsens)
                        data=TextSimplifier.simplifyString(data,true);
                    if (data.indexOf(find)!=-1)
                        found=true;
                }
                if (found) {
                    if (firstFoundRow==-1)
                        firstFoundRow=ix;
                    library.mainFrame.mainTable.addRowSelectionInterval(ix, ix);
                    if (!findAll) {
                        selectedCol = col;
                        break trackloop;
                    }
                    break;
                }
            }
        }
//        }
        boolean found = true;
        if (firstFoundRow==-1) {
            firstFoundRow=library.trackVec.size()-1;
            found = false;
        }
        if (selectedCol != -1)
            library.mainFrame.mainTable.addColumnSelectionInterval(selectedCol,selectedCol);
        
        Rectangle cell = library.mainFrame.mainTable.getCellRect(firstFoundRow,
                             0,true);
        // include cell above and below
        cell.y -= cell.height;
        cell.height *= 3;
        library.mainFrame.mainTable.scrollRectToVisible(cell);

        if (!found)
            JOptionPane.showMessageDialog(library.mainFrame.mainTable,
                         "String <" + find +  "> was not found.",
                         "Find",
                         JOptionPane.WARNING_MESSAGE);


    }

    
    
    /**
     * Find a track by tag fields
     * Col types: MP3File.COL_TITLE, MP3File.COL_ARTIST
     */    
    
    
    // New improved find track, allows variable number of search fields
    // First fields take priority
    // precision is minimum matchlevel value required
    // exact means include parenthesized parts
    
    String findTrackByValues(String[] searchValues,  int[] colTypes,
                        boolean exact,  int precision) {
        String[] searchNorm = new String[searchValues.length];
        int[] searchCols = new int[searchValues.length];
        int iy;
        for (iy = 0; iy < searchValues.length; iy++) {
            if (searchValues[iy] == null)
                searchValues[iy] = "";
            // remove diacritics, set to lower case
            searchNorm[iy] = TextSimplifier.simplifyString(searchValues[iy],true);
            if (!exact) {
                // remove "the", "a", parens
                if (colTypes[iy] == MP3File.COL_TITLE || colTypes[iy] == MP3File.COL_ARTIST) {
                    searchNorm[iy] = TextSimplifier.normalize(searchNorm[iy]);
                } else {
                    searchNorm[iy] = TextSimplifier.albumNormalize(searchNorm[iy]);
                }
            }
            searchCols[iy] = library.attributes.findColumnNum(colTypes[iy], null);
            if (searchCols[iy] == -1) {
                return null;
            }
        }

        int size = library.trackVec.size();
        int ix;


        // Find potential matches
        // Match level Result is a hexadecimal number. Number of digits depends on 
        // number of columns
        // Each digit is 0 for no match, 1 for partial match, 2 for case-insensitive match, 
        // 4 for full match
        // 
        String match = null;
        int matchLevel = 0;

        for (ix = 0; ix < size; ix++) {
            LibraryTrack tr = (LibraryTrack) library.trackVec.get(ix);
            int tempMatchLevel = 0;
            for (iy = 0; iy < searchValues.length; iy++) {
                tempMatchLevel *= 16;
                if (searchValues[iy].equals(tr.columns[searchCols[iy]])) {
                    tempMatchLevel += 4;
                } else if (searchValues[iy].equalsIgnoreCase(tr.columns[searchCols[iy]])) {
                    tempMatchLevel += 2;
                } else {
                    String trackValueNorm = TextSimplifier.simplifyString(tr.columns[searchCols[iy]],true);
                    if (!exact) {
                        if (colTypes[iy] == MP3File.COL_TITLE || colTypes[iy] == MP3File.COL_ARTIST) {
                            trackValueNorm = TextSimplifier.normalize(trackValueNorm);
                        } else {
                            trackValueNorm = TextSimplifier.albumNormalize(trackValueNorm);
                        }

                        if (searchNorm[iy].equals(trackValueNorm)) {
                            tempMatchLevel += 1;
                        } else {
                            if (iy == 0) {
                                break;
                            }
                        }
                    }

                }
                if (tempMatchLevel >= matchLevel) {
                    matchLevel = tempMatchLevel;
                    match = tr.columns[library.attributes.fileNameCol];
                }
            }
        }
        int checkMatch = (matchLevel & precision);
        boolean isMatch = true;
        int mask = 7;
        for (iy = 0; iy < searchValues.length; iy++) {
            if ((precision & mask) != 0) {
                if ((checkMatch & mask) == 0)
                    isMatch = false;
            }
            mask = mask << 4;
        }


//        if ((matchLevel & precision) != precision) {
        if (!isMatch)
            return null;
//        }
        return match;
    }
    
    
    
    
    void refresh() {
        // This stops it at the end of the song
        library.mainFrame.player.selectedRow=-1;
        int i;
        // Component header=library.mainFrame.mainTable.getTableHeader();
        // Do not do this - it can clobber the header resize cursor
        // 20090710 header.setCursor(null);
        library.mainFrame.frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        MyComparator comparator = new MyComparator (displaySortCols, displaySortReverse, 
            library.attributes, false);

        int selectedRow=library.mainFrame.mainTable.getSelectedRow();
        LibraryTrack selectedTrack = null;
        if (selectedRow >= 0)
            selectedTrack = (LibraryTrack)library.mainFrame.library.trackVec.get(selectedRow);

        // Actually we do normalize for everything 4/23/2005
        // Update 2006/09/16 - special normalize for album that exvccludes parens
        // Otherwise normalizing album causes multi disk albums to be intermingled
        // if the disk number is in parens

        int sortCol = library.attributes.displayCol[displaySortCols[0]];
        sortAlbumNormalize=false;
        sortNumeric=false;
        switch (library.attributes.colType[sortCol]) {
            case MP3File.COL_ALBUM:
                sortAlbumNormalize=true;
                break;
            case MP3File.COL_BITRATE:
            case MP3File.COL_PLAYINGTIME:
            case MP3File.COL_SAMPLERATE:
//            case MP3File.COL_PLAYINGTIMEMS:
            case MP3File.COL_YEAR:
            case MP3File.COL_TRACK:
                sortNumeric=true;
                break;
        }

        for (i=0;i<library.trackVec.size();i++) {
            LibraryTrack track;
            track=(LibraryTrack)library.trackVec.get(i);
            if (track.status=='D') {
                library.trackVec.remove(i--);
                continue;
            }
            track.sortKey=normalizeSortKey(track.columns[sortCol]);
        }

        tracksSort = new LibraryTrack[library.trackVec.size()];
        tracksSort = library.trackVec.toArray(tracksSort);
        Arrays.sort(tracksSort,comparator);
        java.util.List arrayList = Arrays.asList(tracksSort);
        library.trackVec = new Vector(arrayList);
        fireTableDataChanged();
        // Select the original selected track and scroll into view
        int ix = library.trackVec.indexOf(selectedTrack);
        if (ix >= 0) {
            int sortColViewIndex = library.mainFrame.mainTable.convertColumnIndexToView(displaySortCols[0]);
            selectAndScrollRowtoVisible(ix, sortColViewIndex);
        }
        library.mainFrame.frame.setCursor(null);
    }
    

    String normalizeSortKey(String sortKey) {
        try {
            sortKey=TextSimplifier.simplifyString(sortKey,true);
            if (sortAlbumNormalize)
                sortKey=TextSimplifier.albumNormalize(sortKey);
            else
                sortKey=TextSimplifier.normalize(sortKey);
            if (sortNumeric)
                sortKey="00000".substring(sortKey.length())+sortKey;
        }
        catch (Exception ex) {
            sortKey=sortKey.toLowerCase();
        }
        return sortKey;
    }
    
    
//    static final char translate[] = {
//        ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     
//        ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     
//
//        ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     
//        ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     
//
//        '\u0020','\u0021','\u0022','\u0023','\u0024','\u0025','\u0026','\'',
//        '\u0028','\u0029','\u002a','\u002b','\u002c','\u002d','\u002e','\u002f',
//
//        '\u0030','\u0031','\u0032','\u0033','\u0034','\u0035','\u0036','\u0037',
//        '\u0038','\u0039','\u003a','\u003b','\u003c','\u003d','\u003e','\u003f',
//
//        '\u0040','a',     'b',     'c',     'd',     'e',     'f',     'g',     
//        'h',     'i',     'j',     'k',     'l',     'm',     'n',     'o',     
//
//        'p',     'q',     'r',     's',     't',     'u',     'v',     'w',     
//        'x',     'y',     'z',     '\u005b','\\',    '\u005d','\u005e','\u005f',
//
//        '\u0060','\u0061','\u0062','\u0063','\u0064','\u0065','\u0066','\u0067',
//        '\u0068','\u0069','\u006a','\u006b','\u006c','\u006d','\u006e','\u006f',
//
//        '\u0070','\u0071','\u0072','\u0073','\u0074','\u0075','\u0076','\u0077',
//        '\u0078','\u0079','\u007a','\u007b','\u007c','\u007d','\u007e','\u007f',
//
//        '\u0080','\u0081','\u0082','\u0083','\u0084','\u0085','\u0086','\u0087',
//        '\u0088','\u0089','\u008a','\u008b','\u008c','\u008d','\u008e','\u008f',
//
//        '\u0090','\u0091','\u0092','\u0093','\u0094','\u0095','\u0096','\u0097',
//        '\u0098','\u0099','\u009a','\u009b','\u009c','\u009d','\u009e','\u009f',
//
//        '\u00a0','\u00a1','\u00a2','\u00a3','\u00a4','\u00a5','\u00a6','\u00a7',
//        '\u00a8','\u00a9','\u00aa','\u00ab','\u00ac','\u00ad','\u00ae','\u00af',
//
//        '\u00b0','\u00b1','\u00b2','\u00b3','\u00b4','\u00b5','\u00b6','\u00b7',
//        '\u00b8','\u00b9','\u00ba','\u00bb','\u00bc','\u00bd','\u00be','\u00bf',
//
//        'a',     'a',     'a',     'a',     'a',     'a',     '\u00c6','c',     
//        'e',     'e',     'e',     'e',     'i',     'i',     'i',     'i',     
//
//        '\u00d0','n',     'o',     'o',     'o',     'o',     'o',     '\u00d7',
//        'o',     'u',     'u',     'u',     'u',     'y',     '\u00de','s',     
//
//        'a',     'a',     'a',     'a',     'a',     'a',     '\u00e6','c',     
//        'e',     'e',     'e',     'e',     'i',     'i',     'i',     'i',     
//
//        '\u00f0','n',     'o',     'o',     'o',     'o',     'o',     '\u00f7',
//        'o',     'u',     'u',     'u',     'u',     'y',     '\u00fe','y',
//
//        'a','a','a','a','a','a',
//        'c','c','c','c','c','c','c','c',
//        'd','d','d','d',
//        'e','e','e','e','e','e','e','e','e','e',
//        'g','g','g','g','g','g','g','g',
//        'h','h','h','h',
//        'i','i','i','i','i','i','i','i','i','i','i','i',
//        'j','j',
//        'k','k','k',
//        'l','l','l','l','l','l','l','l','l','l',
//        'n','n','n','n','n','n','n','n','n',
//        'o','o','o','o','o','o','o','o',
//        'r','r','r','r','r','r',
//        's','s','s','s','s','s','s','s',
//        't','t','t','t','t','t',
//        'u','u','u','u','u','u','u','u','u','u','u','u',
//        'w','w',
//        'y','y','y',
//        'z','z','z','z','z','z'
//    };
//    static final int translateLeng = translate.length;
//        
//    
//    // translate string to lower case and also remove diacritics
//    static String simplifyString(String strData) {
//        if (strData == null)
//            return null;
//        char charData[] = strData.toCharArray();
//        int leng = charData.length;
//        
//        int ix;
//        for (ix=0;ix < leng; ix++){
//            char theChar = charData[ix];
//            if (theChar < translateLeng)
//                charData[ix] = translate[theChar];
//        }
//        return new String(charData);
//    }
//    
//    
//    // Remove parenthesized parts () and []
//    static Pattern normalizePattern1 = Pattern.compile(
//        "\\([^\\)]*\\) *|\\[[^\\]]*\\] *");
////        "^[^a-z0-9]*\\([^\\)]*\\)");
//    // Remove the word 'a' and the word 'the' at the beginning, also leading blanks
//    static Pattern normalizePattern2 = Pattern.compile(
//        "^a +|^the +|^ *");
////        "^[^a-z0-9]*a[^a-z0-9]+|^[^a-z0-9]*the[^a-z0-9]+|^[^a-z0-9]+");
//    // Remove special chars
//    static Pattern normalizePattern3 = Pattern.compile(
//        "[^a-z0-9 ]");
//    static String normalize(String s) {
//        if (s==null)
//            return null;
//        Matcher matcher = normalizePattern1.matcher(s);
//        s = matcher.replaceAll("");
//        matcher = normalizePattern2.matcher(s);
//        s = matcher.replaceFirst("");
//        matcher = normalizePattern3.matcher(s);
//        s = matcher.replaceAll("");
//        s = s.trim();
//        return s;
//    }
//    // Normalize without removing parens
//    static String albumNormalize(String s) {
//        if (s==null)
//            return null;
//        Matcher matcher = normalizePattern2.matcher(s);
//        s = matcher.replaceFirst("");
//        matcher = normalizePattern3.matcher(s);
//        s = matcher.replaceAll("");
//        s = s.trim();
//        return s;
//    }


    class MyComparator implements Comparator {
        int [] librarySortCols;
        int [] librarySortSign;
        LibraryAttributes attributes;
        boolean full;

        MyComparator(int [] displaySortCols, boolean[] displaySortReverse,LibraryAttributes attributes, 
            boolean full) {
            this.attributes = attributes;
            this.full=full;
            int i;
            librarySortCols=new int[displaySortCols.length];
            librarySortSign=new int[displaySortCols.length];
            for (i=0;i<displaySortCols.length;i++) {
                librarySortCols[i]=attributes.displayCol[displaySortCols[i]];
                if (displaySortReverse[i])
                    librarySortSign[i]=-1;
                else
                    librarySortSign[i]=1;
            }
        }

        public int compare(Object o1,
                   Object o2) {
            LibraryTrack t1 = (LibraryTrack)o1;
            LibraryTrack t2 = (LibraryTrack)o2;

            int result = (t1.sortKey.compareTo(t2.sortKey)) * librarySortSign[0];
            if (result != 0 || !full)
                return result;

            // If a full compare is required then do keys 1 onwards
            int i;
            String s1;
            String s2;
            for (i=1;i<librarySortCols.length;i++) {
                s1=t1.columns[librarySortCols[i]].toLowerCase().trim();
                s2=t2.columns[librarySortCols[i]].toLowerCase().trim();
                switch (attributes.colType[librarySortCols[i]]) {
                case MP3File.COL_TITLE:
                case MP3File.COL_ARTIST:
                case MP3File.COL_ALBUM:
                    s1=TextSimplifier.normalize(s1);
                    s2=TextSimplifier.normalize(s2);
                    break;
                }
                result=s1.compareTo(s2);
                if (result!=0)
                    return result * librarySortSign[i];
            }
            return 0;
        }
    }

}
