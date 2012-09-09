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
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import pgbennett.id3.*;

// Library will be a tab-delimited text file
// 1st byte is of each record an indicator for deletion
// Update by marking record as deleted and adding a new record
// at end
// Keep file offset of the track entry in the entry object



public class Library {

    MainFrame mainFrame;
    // tracks
    // Key = file name
    // Value = array of strings corresponding to columns
    HashMap <String,LibraryTrack> trackMap;
    // This vector is accessed by the table model
    public Vector <LibraryTrack> trackVec;
    public LibraryAttributes attributes;
    LibraryTableModel model;
    File libraryFile;
    long libraryModifiedTime = 0;
    /** This is true if another user has modified the
     * library since we first opened it
     */
    boolean dirtyLibrary=false;
    static final String newline = System.getProperty("line.separator");
    boolean readOnly;
    // Insertion point
    // Index before which to insert. Value -1 will insert at end or update in place.
    // A number higher than the last entry will add at the end.
    int insertBefore = -1;
    int firstInserted = -1;  // First insertion of current operation
    int numberInserted = 0; // Insertion count current operation.
    // array of selected rows - to be kept up to date while updates are taking place
    // If any selected row is deleted the entry is set to -1
    // TrackUpdater must take account of this.
    int selectedRows[] = null; 
    int selectReplaced;
    int librarySize;
    
    static final String [] templateNames = {
        "default.jampal",
        "minimal.jampal",
        "custom.jampal",
        "playlist.jampal",
        "peter.jampal",
        "tag-fixup.jampal"
    };

    static final String [] templateDesc = {
        "Library with Alternates",
        "Basic library",
        "Library with 3 custom fields",
        "Basic playlist",
        "Peter's Library",
        "Spreadsheet updating (tag-fixup.ods)"
    };
    
    
    Library(MainFrame mf) throws Exception {
        this(mf, false,null,null);
    }
    Library(MainFrame mf, boolean newLibrary, String templateName, String newFileName) throws Exception {
        mainFrame = mf;
        trackMap = new HashMap<String,LibraryTrack>();
        trackVec = new Vector<LibraryTrack>();
        String propFileName = null;
        File propFile = null;
        String notExist = "";
        String [] options = templateDesc;
        boolean isCurrent = mainFrame.library!=null;
        int ix;
        if (!newLibrary) {
            propFileName = mainFrame.name+".jampal";
            propFile = new File(propFileName);
        }
        if (isCurrent) {
            options = new String[templateDesc.length+1];
            options[0]="Copy of Current Library";
            for (ix=0;ix<templateDesc.length;ix++) {
                options[ix+1]=templateDesc[ix];
            }
            
        }
        boolean copyLibrary=false;
        if (newLibrary) {
            if (templateName==null) {
                Object resp= JOptionPane.showInputDialog
                (mainFrame.frame,
                "Please select a template.",
                "Select Type of Library",
                JOptionPane.WARNING_MESSAGE,
                null, //icon
                options,
                null);

                if (resp==null)
                    return;
                templateName=(String)resp;
            }
            
            for (ix=0;ix<options.length &&!options[ix].equals(templateName);ix++)
                ;

            if (isCurrent)
                ix--;
            
            boolean creatingPlaylist = false;
            BufferedReader in;
            if (ix==-1) {
                in = new BufferedReader(new FileReader( mainFrame.name+".jampal"));
                copyLibrary=true;
                creatingPlaylist = (mainFrame.library.attributes.libraryType=='P');
            }
            else {
                in = new BufferedReader (new InputStreamReader
                    (ClassLoader.getSystemResourceAsStream
                    ("pgbennett/jampal/"+templateNames[ix])));
                creatingPlaylist = ("playlist.jampal".equals(templateNames[ix]));
            }

            if (newFileName==null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Create New Library File");
                String currentDirectory=null;
                if (mainFrame.name!=null) {
                    File nameFile = new File(mainFrame.name);
                    currentDirectory = nameFile.getParent();
                    if (currentDirectory==null)
                        currentDirectory = ".";
                }
                if (currentDirectory==null)
                    currentDirectory=Jampal.jampalDirectory;
                if (currentDirectory!=null)
                    fileChooser.setCurrentDirectory(new File(currentDirectory));
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setMultiSelectionEnabled(false);
                Mp3FileFilter filter = new Mp3FileFilter();
                filter.setExtension("jampal", "Jampal library files");
                fileChooser.addChoosableFileFilter(filter);
                fileChooser.setAcceptAllFileFilterUsed(false);
                fileChooser.setFileFilter(filter);
                fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                int returnVal = fileChooser.showSaveDialog(mainFrame.frame);
                if (returnVal == fileChooser.APPROVE_OPTION) {
                    propFile = fileChooser.getSelectedFile();
                    propFileName = propFile.getPath();
                    if (!propFileName.toLowerCase().endsWith(".jampal")) {
                        propFileName = propFileName + ".jampal";
                        propFile = new File(propFileName);
                    }
                }
                else
                    return;
            }
            else {
                propFileName = newFileName;
                propFile = new File(propFileName);
            }
            
            if (propFile.exists()) {
                if (JOptionPane.showConfirmDialog(mainFrame.frame,
                    "File "+propFileName+" already exists. Do you want to overwrite it ?",
                    "Warning", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                    return;
            }    
                
            PrintWriter out = new PrintWriter(new FileOutputStream(propFile));
            // File mainName = new File(mainFrame.name);
            String libName = propFile.getName();
            // drop the ".jampal"
            libName = libName.substring(0, libName.length()-7);
            
            for(;;) {
                String line = in.readLine();
                if (line==null)
                    break;
                if (creatingPlaylist
                    && line.startsWith("playlist=")) {
                    line="playlist="+libName;
                }
                if (line.startsWith("libraryname=")) {
                    line="libraryname="+libName+".jmp";
                }
                
                out.println(line);
            }
            in.close();
            out.close();
            
            if (!creatingPlaylist && !copyLibrary) {
                String playlistName = propFile.getParent() 
                + File.separator + "playlist.jampal";

                File playlistFile = new File(playlistName);

                if (!playlistFile.exists()) {

                    in = new BufferedReader(new InputStreamReader
                    (ClassLoader.getSystemResourceAsStream
                    ("pgbennett/jampal/playlist.jampal")));

                    out = new PrintWriter(new FileOutputStream(playlistFile));

                    for(;;) {
                        String line = in.readLine();
                        if (line==null)
                            break;
                        out.println(line);
                    }
                    in.close();
                    out.close();
                }
            }            
            
        }
        
        if (propFileName!=null)
            attributes = new LibraryAttributes(propFileName);
        insertBefore=-1;
}
    


    // Can run in a worker thread
    boolean updateTrackAllViews(File file, MainFrame mainFrameExclude) throws Exception {
        boolean otherUpdated=false;
        Set <Map.Entry<String,MainFrame>> mainframes = mainFrame.mainFrameMap.entrySet();
        for (Map.Entry<String,MainFrame> entry : mainframes) {
            final MainFrame mf = entry.getValue();
            boolean thisUpdated;
            if (mf.library.attributes.libraryType == 'L') 
                thisUpdated = mf.library.synchronizeTrack(null,file,'R'); // false
            else
                thisUpdated = mf.library.updateTrack(null,file);
                
            if (mf != mainFrameExclude) {
                
                if (thisUpdated) { // && !mainFrame.equals(mf)) {
                    otherUpdated=true;
                    if (SwingUtilities.isEventDispatchThread()) {
                        int [] selection = mf.mainTable.getSelectedRows();
                        mf.model.fireTableDataChanged();
                        int ix;
                        for (ix=0;ix<selection.length;ix++){
                            mf.mainTable.addRowSelectionInterval(selection[ix],selection[ix]);
                        }
                    }
                }
                String translatedFileName = attributes.translateFileName(file.getAbsolutePath());
                if (mf.trackEditor != null && translatedFileName.equals(mf.trackEditor.fileName)) {
                    if (mf.trackEditor.numCboxChecked == 0)
                        SwingUtilities.invokeLater(
                                new Runnable() {

                                    @Override
                                    public void run() {
                                        mf.trackEditor.refresh();
                                    }
                                });
                    
                }
            }
        }
        return otherUpdated;
    }
    

    /** For pasting tracks or files into a library or playlist.
     */    
    void startBulkUpdate() throws Exception {
        insertBefore=-1;
        firstInserted=-1;
        numberInserted=0;
        selectedRows = mainFrame.mainTable.getSelectedRows();
        
        int selectedRowCount = mainFrame.mainTable.getSelectedRowCount();
        selectReplaced=0;
        if (selectedRowCount==0)
            insertBefore = 0;
        else 
            insertBefore = selectedRows[selectedRows.length-1]+1;
        librarySize=trackVec.size();
        openLibrary();
    }

    void endBulkUpdate() throws Exception{
        int selectStart=9999999;
        int selectEnd=-1;
        model.resetSort(false);
        model.fireTableDataChanged();
        mainFrame.mainTable.clearSelection();
        if (numberInserted > 0) {
            int lastInserted = firstInserted + numberInserted - 1;
            mainFrame.mainTable.addRowSelectionInterval(firstInserted,lastInserted);
            if (selectStart > firstInserted)
                selectStart = firstInserted;
            if (selectEnd < lastInserted)
                selectEnd = lastInserted;
        }
        if (selectEnd != -1) {
            Rectangle cell1 = mainFrame.mainTable.getCellRect(selectStart,
                                 0,true);
            Rectangle cell2 = mainFrame.mainTable.getCellRect(selectEnd,
                                 0,true);
            // include cell above and below
            mainFrame.mainTable.scrollRectToVisible(cell2);
            mainFrame.mainTable.scrollRectToVisible(cell1);
        }            
            
        insertBefore=-1;
        closeLibrary();
    }
    
    
    static Pattern pathPattern = Pattern.compile("\\\\");
    static Pattern drivePattern = Pattern.compile("^[a-zA-Z0-9]:\\\\");
    
    // 2008/01/11 Alternate Albums
    // New interface for alternate tracks
    // boolean addTrack(File)
    // boolean addTrack(track)
    // boolean updateTrack(track)
    // boolean updateTrack(file)
    // If alternateAlbums is true, add results in one copy
    // of each alternate (latest values) for the track in the library
    // Update does the same only if there is that track in the library
    // If alternateAlbums is false, add results in the pasted alternate added
    // and any others updated (even if that reults in a duplicate);
    // update results in any alternates being updated


    
    // These methods must not be called for read only libraries
    /**
     * Updates all entries in the library for this track
     * If the librray contains fewer entries, adds new ones
     * If the library contains extra entries, removes them
     * Either track or file is required. If both are supplied
     * only the track is used. If the track is supplied it is
     * assumed to be correct, and to have all entries included
     * in the alternateAlbums and alternteTracks arrays.
     * If track is supplied the object must not already exist in the library
     * If mustAdd is true, also moves all entries to the insertion
     * point
     * Regardless of mustAdd, any added entries are placed at the 
     * insertion point
     * If mustAdd not true and the file not already in the library, it is not added
     * Can run in a worker thread
     * Should only run for a library, for a playlist use addEntry
     * or updateTrack
     * These should be set up before calling these methods: insertBefore,
     * firstInserted, numberInserted, selectedRows[]
     * mustAdd indicates that all entries should be moved to the insert point
     * if mustAdd not set all entries are put after their primary entries
     * and that if the track is not already in the library it must be added
     * Options parameter - R = Refresh all entries if track exists, move new ones to the end
     *                     A = Refresh all entries, add if necessary, move all to insert point,
     *                     1 = Refesh all, add if necessary, move new and matching one to insert point
     * @param track 
     * @param file 
     * @param option
     * @return Track that was updated
     */
    boolean synchronizeTrack(LibraryTrack track, File file, char option) {
        if (readOnly) {
            updateErrorMessage("The Library is read only.", "");
            return false;
        }
        String fileName=null;
        String message = null;
        LibraryTrack trackFound;
        int ix;
        int saveInsertBefore = insertBefore;
        int saveFirstInserted = firstInserted;
        int saveNumberInserted = numberInserted;
        try {
            if (track == null) {
                fileName = attributes.normalizeFileName(file);
            }
            else {
                fileName=track.columns[mainFrame.library.attributes.fileNameCol];
                fileName = attributes.fixFileNameCase(fileName);
                if (track.hasAlternates) {
                    if (track.alternateCount > 0)
                        track.setAlternateNum(attributes, (short)0);
                    else {
                        addTrackToLib(track, true);
                        // Clear sort indicators and prevent search
                        model.resetSort(false);
                        return true;
                    }
                }
            }
            trackFound = trackMap.get(fileName);
            if (option == 'R') {
                ix = trackVec.indexOf(trackFound);
                insertBefore = ix + 1;
                if (insertBefore == 0)
                    insertBefore = -1;
            }
            
            if (trackFound == null && option == 'R')
                return false;
            if (track == null) {
                track = new LibraryTrack();
                // Use entryNum 0 because we actually want all entries
                track.init(file, attributes, (short)0);
            }
            if (trackFound == null) {
                trackMap.put(fileName,track);
                insertEntry(track);
                trackFound = track;
            }
            else {
                // track was found - since this is a library must be main entry 0
                copyTrack(track, trackFound);                
                trackFound.setAlternateNum(attributes, (short)0);
                if (option == 'A' || (option == '1' && track.alternateNum == 0) )
                    moveEntry(trackFound);
                else {
                    track.status = 'U';
                    updateLibraryTrack(track);
                    track.status = 'A';
                }
            }
            // Update other entries
            LibraryTrack prevEntry = trackFound;
            short ixs;
            short alternateCount = track.alternateCount;
            for (ixs = 0; ixs < alternateCount; ixs++) {
                trackFound = trackFound.nextEntry;
                if (trackFound == null) {
                    trackFound = new LibraryTrack();
                    copyTrack(track, trackFound);                
                    trackFound.setAlternateNum(attributes,(short)(ixs+1));
                    trackFound.prevEntry = prevEntry;
                    prevEntry.nextEntry = trackFound;
                    insertEntry(trackFound);
                }
                else {
                    copyTrack(track, trackFound);                
                    trackFound.setAlternateNum(attributes,(short)(ixs+1));
                    if (option == 'A' || (option == '1' && track.alternateNum == ixs+1) )
                        moveEntry(trackFound);
                    else {
                        track.status = 'U';
                        updateLibraryTrack(track);
                        track.status = 'A';
                    }
                }
                prevEntry = trackFound;
            }
            // Remove any extra entries
            if (track.alternateCount > -1) {
                LibraryTrack nextEntry;
                while (trackFound.nextEntry != null) {
                    nextEntry = trackFound.nextEntry;
                    removeEntry(nextEntry);
                    trackFound.nextEntry = nextEntry.nextEntry;
                }
            }
            
            
        } catch (Exception ex) {
            ex.printStackTrace();
            message = ex.toString();
        }
        finally {
            if (option == 'R') {
                insertBefore = saveInsertBefore;
                firstInserted = saveFirstInserted;
                numberInserted= saveNumberInserted;
            }
            
        }
        if (message != null)
            updateErrorMessage(message, fileName);
        
        return true;
    }

    void insertEntry(LibraryTrack track) throws Exception {
        int ix;
        if (insertBefore < 0 || insertBefore > trackVec.size()) {
            if (firstInserted == -1)
                firstInserted = trackVec.size();
            trackVec.add(track);
        }
        else {
            if (firstInserted == -1)
                firstInserted = insertBefore;
            trackVec.add(insertBefore,track);
            // adjust selected rows for new insertion
            if (selectedRows != null) {
                for (ix = 0; ix < selectedRows.length; ix++) {
                    if (selectedRows[ix] >= insertBefore)
                        selectedRows[ix]++;
                }
            }
            insertBefore++;
        }
        numberInserted++;
        // Indicate on library that track was added
        track.status = 'U';
        updateLibraryTrack(track);
        track.status = 'A';
        // Clear sort indicators and prevent search
        model.resetSort(false);
    }


    int removeEntry(LibraryTrack track) throws Exception {
        int ix;
        int row;
        row = trackVec.indexOf(track);
        trackVec.remove(row);
        if (insertBefore != -1 && row < insertBefore)
            insertBefore--;
        if (firstInserted != -1 && row < firstInserted)
            firstInserted--;
        if (firstInserted != -1 && row >= firstInserted && row < firstInserted + numberInserted)
            numberInserted--;
        // adjust selected rows for deletion
        if (selectedRows != null) {
            for (ix = 0; ix < selectedRows.length; ix++) {
                if (selectedRows[ix] == row) {
                    // if a selected row is deleted set it to -1
                    selectedRows[ix] = -1;
                }
                if (selectedRows[ix] > row)
                    selectedRows[ix]--;
            }
        }
        // Indicate on library that track was removed
        track.status = 'D';
        updateLibraryTrack(track);
        // Clear sort indicators and prevent search
        model.resetSort(false);
        return row;
    }

    
    /**
     * Adds an entry to the playlist at the insertion point.
     * Adds the entry regardless of whether it is already in the library
     * Either track or file is required. If both are supplied
     * only the track is used. If the track is supplied it is
     * assumed to be correct.
     * If the track is supplied the alternateNum is used to determine
     * the entry to add, and the track data is assumed correct.
     * The track should already have the correct album and track number 
     * for the specified alternate number. The arrays of alternates is
     * not used.
     * If a file is supplied the main entry is added.
     * Can run in a worker thread
     * These should be set up before calling these methods: insertBefore,
     * firstInserted, numberInserted, selectedRows[]
     * Should only run for a playlist
     * @param track 
     * @param file 
     */
    boolean addEntry(LibraryTrack track, File file) {
        if (readOnly) {
            updateErrorMessage("The Library is read only.", "");
            return false;
        }
        String fileName=null;
        String message = null;
        int ix;
        try {
            if (track == null) {
                fileName = attributes.normalizeFileName(file);
                track = new LibraryTrack();
                // Use entryNum 0 because we actually want main entry
                track.init(file, attributes, (short)0);
            }
            else {
                fileName=track.columns[mainFrame.library.attributes.fileNameCol];
                fileName = attributes.fixFileNameCase(fileName);
            }
            LibraryTrack trackFound = trackMap.get(fileName);
            if (trackFound == null)
                trackMap.put(fileName,track);
            else {
                while(trackFound.nextEntry != null)
                    trackFound = trackFound.nextEntry;
                trackFound.nextEntry = track;
                track.prevEntry = trackFound;
            }
            insertEntry(track);
            
        } catch (Exception ex) {
            ex.printStackTrace();
            message = ex.toString();
        }
        if (message != null)
            updateErrorMessage(message, fileName);
        
        return true;
        
    }
    

    /**
     * Updates track data if it already exists in the playlist.
     * Updates all entries for this file in the playlist.
     * Either track or file is required. If both are supplied
     * the track will be updated with a new file name (verify tracks)
     * If the track is supplied it is
     * assumed to be correct.
     * If the track is supplied it is
     * assumed to have all entries included
     * in the alternateAlbums and alternateTracks arrays.
     * Can run in a worker thread
     * These should be set up before calling these methods: insertBefore,
     * firstInserted, numberInserted.
     * Should only run for a playlist
     * In the case of updating a file name can be run for a library. (i.e.
     * if you are sure the alternate albums are not being updated).
     * @param track 
     * @param file
     * @return false if library was read only. Cannot update in that case.
     */
    boolean updateTrack(LibraryTrack track, File file) {
        if (readOnly) {
            updateErrorMessage("The Library is read only.", "");
            return false;
        }
        String fileName = null;
        String message = null;
        LibraryTrack trackFound;
        try {
            if (track == null) {
                fileName = attributes.normalizeFileName(file);
            } else {
                fileName = track.columns[mainFrame.library.attributes.fileNameCol];
                fileName = attributes.fixFileNameCase(fileName);
                if (track.alternateNum != 0 && track.alternateCount > 0) {
                    track.setAlternateNum(attributes, (short) 0);
                }
            }
            trackFound = trackMap.get(fileName);
            if (trackFound == null) {
                return true;
            }
            if (track == null) {
                track = new LibraryTrack();
                // Use entryNum 0 because we actually want all entries
                track.init(file, attributes, (short) 0);

                // track was found - this is a playlist - preserve entry num
                // Update other entries
                short maxAlternateNum = track.alternateCount;
                if (maxAlternateNum == -1) {
                    maxAlternateNum = 30000;
                }
                LibraryTrack prevEntry = null;
                LibraryTrack firstEntry = trackFound;
                while (trackFound != null) {
                    if (trackFound.alternateNum > maxAlternateNum) {
                        if (attributes.libraryType == 'L') {
                            // Library - Remove invalid entries
                            LibraryTrack nextEntry;
                            nextEntry = trackFound.nextEntry;
                            if (prevEntry != null) {
                                prevEntry.nextEntry = nextEntry;
                            } else {
                                firstEntry = nextEntry;
                            }
                            if (nextEntry != null) {
                                nextEntry.prevEntry = prevEntry;
                            }
                            removeEntry(trackFound);
                        } else {
                            // Playlist - make invalid entries valid
                            copyTrack(track, trackFound);
                            trackFound.setAlternateNum(attributes, (short) 0);
                            trackFound.status = 'U';
                            updateLibraryTrack(trackFound);
                            trackFound.status = 'A';
                        }
                    } else {
                        copyTrack(track, trackFound);
                        trackFound.setAlternateNum(attributes, trackFound.alternateNum);
                        //                    if (!updateFileName) 
                        trackFound.status = 'U';
                        updateLibraryTrack(trackFound);
                        trackFound.status = 'A';
                    }
                    trackFound = trackFound.nextEntry;
                }
                if (firstEntry != null) {
                    trackMap.put(fileName, firstEntry);
                } else {
                    trackMap.remove(fileName);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            message = ex.toString();
        }
        if (message != null) {
            updateErrorMessage(message, fileName);
        }

//        updateCounter++;
        return true;

    }

    
    boolean updateTrackFileName(LibraryTrack track, File file) 
        throws Exception {
        if (readOnly) {
            updateErrorMessage("The Library is read only.", "");
            return false;
        }
        String fileName=null;
        String message = null;
        LibraryTrack trackFound;
        int ix;
        fileName=track.columns[mainFrame.library.attributes.fileNameCol];
        fileName = attributes.fixFileNameCase(fileName);
        trackFound = trackMap.get(fileName);
        String priorFileName = null;
        // update track file name - first delete old track
        priorFileName = fileName;
        fileName = attributes.normalizeFileName(file);
        trackMap.remove(priorFileName);
        trackMap.put(fileName,trackFound);
        String translatedFileName = attributes.translateFileName(file.getAbsolutePath());
        while (trackFound != null) {
            int rowRemoved = removeEntry(trackFound);
            // Update and re-add track
            trackFound.status = 'A';
            trackFound.columns[mainFrame.library.attributes.fileNameCol] = translatedFileName;
            insertBefore = rowRemoved;
            insertEntry(trackFound);
            trackFound = trackFound.nextEntry;
        }
        
        return updateTrack(null, file);
    }    
    
    /**
     * Move entry to insertion point
     * track entry is supplied, and must exist in library.
     * Can run in a worker thread
     * These should be set up before calling these methods: insertBefore,
     * firstInserted, numberInserted.
     * For either playlist or library (drag and drop or copy and paste)
     * @param track 
     * @param file 
     */
    void moveEntry(LibraryTrack track) throws Exception {
        removeEntry(track);
        insertEntry(track);
//        updateCounter++;
    }
    
    /**
     * Deletes all entries in the library for this track
     * Can run in a worker thread
     * Should only run for a library, for a playlist use deleteEntry
     * These should be set up before calling these methods: insertBefore,
     * firstInserted, numberInserted, selectedRows[]
     * @param track 
     */
    boolean deleteTrack(LibraryTrack track) throws Exception {
        if (readOnly) {
            updateErrorMessage("The Library is read only.", "");
            return false;
        }
        String fileName=null;
        LibraryTrack trackFound;
        fileName=track.columns[mainFrame.library.attributes.fileNameCol];
        fileName = attributes.fixFileNameCase(fileName);
        trackFound = trackMap.get(fileName);
        while (trackFound != null) {
            removeEntry(trackFound);
            trackFound = trackFound.nextEntry;
        }
        trackMap.remove(fileName);
        return true;
    }
    
    /**
     * Deletes an entry from the playlist
     * Can run in a worker thread
     * Should only run for a playlist
     * These should be set up before calling these methods: insertBefore,
     * firstInserted, numberInserted, selectedRows[]
     * @param track 
     */
    boolean deleteEntry(LibraryTrack track) throws Exception {
        if (readOnly) {
            updateErrorMessage("The Library is read only.", "");
            return false;
        }
        String fileName;
        LibraryTrack trackFound;
        fileName=track.columns[mainFrame.library.attributes.fileNameCol];
        fileName = attributes.fixFileNameCase(fileName);
        trackFound = trackMap.get(fileName);
        LibraryTrack prevEntry = null;
        LibraryTrack firstEntry = trackFound;
        while (trackFound != null) {
            if (trackFound == track) {
                LibraryTrack nextEntry = trackFound.nextEntry;
                if (prevEntry != null)
                    prevEntry.nextEntry = nextEntry;
                else
                    firstEntry = nextEntry;
                if (nextEntry != null)
                    nextEntry.prevEntry = prevEntry;
                removeEntry(trackFound);
            }
            prevEntry = trackFound;
            trackFound = trackFound.nextEntry;
        }
        if (firstEntry == null)
            trackMap.remove(fileName);
        else
            trackMap.put(fileName,firstEntry);
        return true;
    }

    
    
    void updateErrorMessage(String message, String fileName) {
        if (message!=null) {
            if (mainFrame.errorMessages!=null)
                mainFrame.errorMessages.add(message+" File:"+fileName);
            else {
                JOptionPane.showMessageDialog(mainFrame.frame,
                                     message+" File:"+fileName,
                                     "Error",
                                     JOptionPane.ERROR_MESSAGE);
            }
                
        }
        
    }

    
    // delete a track object from the Vector of tracks, and the library
    // File name must already be in standard format
    void deleteTrackEntry(LibraryTrack track) throws Exception {

        track.status = 'D';
        String fileName=track.columns[mainFrame.library.attributes.fileNameCol];
        int ix = trackVec.indexOf(track);
        if (ix != -1)
            trackVec.remove(ix);
        
        if (!readOnly)
            updateLibraryTrack(track);
    }    


    
    
    void copyTrack(LibraryTrack from, LibraryTrack to) {
        int ix;
        if (to.columns == null)
            to.columns = from.columns.clone();
        else {
            for (ix=0; ix<attributes.numLibraryCol; ix++) {
                if (attributes.colType[ix]!=MP3File.COL_DUMMY) {
                    to.columns[ix]=from.columns[ix];
                }
            }
        }
        to.status = from.status;
        to.alternateCount = from.alternateCount;
        to.hasAlternates = from.hasAlternates;
        to.alternateAlbums  = from.alternateAlbums;
        to.alternateTracks  = from.alternateTracks;
        to.mainAlbum = from.mainAlbum;
        to.mainTrack = from.mainTrack;
    }
        
    
    
    
    void copyDummyCols(LibraryTrack from, LibraryTrack to) {
        int ix;
        for (ix=0; ix<attributes.numLibraryCol; ix++) {
            if (attributes.colType[ix]==MP3File.COL_DUMMY) {
                to.columns[ix]=from.columns[ix];
            }
        }
        
    }
    

    PrintStream libraryWriter;
    void openLibrary() throws Exception {
        updateDirty();
        if (readOnly)
            return;
        String charSet = getLibraryCharset();
        if (charSet == null) {
            saveLibrary();
            charSet = getLibraryCharset();
        }
        try {
//            libraryWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(libraryFile, true)));
            libraryWriter = new PrintStream(new FileOutputStream(libraryFile, true), false, charSet);
        }
        catch (FileNotFoundException ex) {
            readOnly=true;
            throw ex;
        }
    }
 
    void closeLibrary() throws Exception {
        if (readOnly)
            return;
        libraryWriter.close();
        boolean isError = libraryWriter.checkError();
        libraryWriter=null;
        if (!dirtyLibrary)
            libraryModifiedTime = libraryFile.lastModified();
        if (isError) 
            throw new JampalException("Error writing library " + libraryFile.getAbsolutePath());
    }
    
    // Can run in a worker thread
    void updateLibraryTrack(LibraryTrack track) throws Exception {
        boolean doClose=false;
        if  (libraryWriter==null) {
            openLibrary();
            doClose=true;
        }
        libraryWriter.println(track.toLibraryRec(attributes));
        if (doClose)
            closeLibrary();
    }

    // Can run in a worker thread
    void updateDirty() throws Exception {
        if (!dirtyLibrary) {
            long newModifiedTime = libraryFile.lastModified();
            if (newModifiedTime != libraryModifiedTime)
                dirtyLibrary=true;
        }
    }

    void loadLibrary() throws Exception {
        
        String fileName = attributes.libraryProperties.getProperty("libraryname");

        libraryFile = new File(fileName);
        if (libraryFile.getParent() == null) {
            File mainFile = new File(mainFrame.name);
            String parentDir = mainFile.getParent();
            if (parentDir != null) {
                fileName = parentDir + File.separator + fileName;
                libraryFile = new File(fileName);
            }
        }
            
        if (attributes.fileNameCol==-1)
            throw new JampalException("File Names are not stored on library");

        Component header=mainFrame.mainTable.getTableHeader();
        header.setCursor(null);
        mainFrame.frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        trackMap = new HashMap<String, LibraryTrack>();
        trackVec = new Vector<LibraryTrack>();
        dirtyLibrary=false;
        
        readOnly=false;
        libraryModifiedTime = libraryFile.lastModified();
        BufferedReader bufferedReader=null;
        String charSet = attributes.libraryProperties.getProperty("charset");
        try {
            // Create file if not already there
            libraryFile.createNewFile();
            if (!libraryFile.canWrite())
                readOnly=true;
            FileInputStream fileInputStream = new  FileInputStream(libraryFile);
            InputStreamReader inputStreamReader;
            if (charSet == null)
                inputStreamReader = new InputStreamReader(fileInputStream);
            else
                inputStreamReader = new InputStreamReader(fileInputStream, charSet);
            bufferedReader = new BufferedReader(inputStreamReader);
            for (;;) {
                String trackStr = bufferedReader.readLine();
                if (trackStr==null)
                    break;
                LibraryTrack track = new LibraryTrack(trackStr,attributes);
                String trackFileName=track.columns[attributes.fileNameCol];
                trackFileName=attributes.fixFileNameCase(trackFileName);
                // Need to check for library or playlist
                // Library - replace any entry that already exists
                // Playlist - replace entries if you find a U
                LibraryTrack trackFound;
                trackFound = trackMap.get(trackFileName);
                short ixs;
                if (attributes.libraryType == 'L') {
                    // Library (Type L)
                    LibraryTrack prevEntry = null;
                    if (track.status == 'D') {
                        // Deletion of track from library
                        while (trackFound != null) {
                            trackMap.remove(trackFileName);
                            trackVec.removeElement(trackFound);
                            trackFound = trackFound.nextEntry;
                        }
                    }
                    else {
                        addTrackToLib(track, false);
                    }

                }
                else {
                    // Playlist (type P)
                    LibraryTrack prevEntry = null;
                    LibraryTrack trackScan = trackFound;
                    if (track.status == 'D'|| track.status == 'U') {
                        // Deletion of track from playlist
                        while (trackScan != null) {
                            if (trackScan.alternateNum == track.alternateNum) {
                                trackVec.removeElement(trackScan);
                                if (prevEntry == null) {
                                    trackMap.put(trackFileName, trackScan.nextEntry);
                                    trackScan.prevEntry = null;
                                    trackFound = trackScan.nextEntry;
                                }
                                else {
                                    prevEntry.nextEntry = trackScan.nextEntry;
                                    if (trackScan.nextEntry != null)
                                        trackScan.nextEntry.prevEntry = prevEntry;
                                }
                            }
                            prevEntry = trackScan;
                            trackScan = trackScan.nextEntry;
                        }
                        
                    }
                    if (track.status != 'D') {
                        // Addition of track to playlist
                        track.status = 'A';
                        if (trackFound == null) {
                            trackVec.add(track);
                            trackMap.put(trackFileName,track);
                            trackFound = track;
                        }
                        else {
                            while(trackFound.nextEntry != null)
                                trackFound = trackFound.nextEntry;
                            trackFound.nextEntry = track;
                            track.prevEntry = trackFound;
                            trackVec.add(track);
                        }
                    }

                }
            }
        }
        finally {
            model.resetSort(true);
            if (bufferedReader!=null)
                bufferedReader.close();
            mainFrame.frame.setCursor(null);
        }

    }
    
    // this could also be used from loadLibrary
    void addTrackToLib(LibraryTrack track, boolean isPaste) throws Exception {
        short ixs;
        // Addition of track to library
        track.status = 'A';
        String trackFileName=track.columns[attributes.fileNameCol];
        String trackFileNameFixed=attributes.fixFileNameCase(trackFileName);
        LibraryTrack trackFound;
        trackFound = trackMap.get(trackFileNameFixed);
        LibraryTrack prevEntry = null;
        LibraryTrack trackFromFile = null;
        short numberToCheck = (short)(track.alternateNum + 1) ;
        for (ixs = 0; ixs < numberToCheck; ixs ++) {
            if (trackFound == null) {
                trackFound = new LibraryTrack();
                copyTrack(track, trackFound);                
                trackFound.alternateNum = track.alternateNum;
                if (ixs != track.alternateNum) {
                    if (isPaste) {
                        if (trackFromFile == null) {
                            trackFromFile = new LibraryTrack();
                            File file = new File(trackFileName);
                            trackFromFile.init(file, attributes, (short)0);
                            numberToCheck = (short)(trackFromFile.alternateCount + 1);
                        }
                        copyTrack(trackFromFile, trackFound);                
                        trackFound.setAlternateNum(attributes, ixs);
                    }
                    else {
                        if (ixs == 0) {
                            trackFound.mainAlbum = "Album 00";
                            trackFound.mainTrack = "00";
                        }
                        else {
                            trackFound.alternateAlbums = new String [ixs];
                            trackFound.alternateTracks = new String [ixs];
                            trackFound.alternateAlbums [ixs-1] = "Album " + ixs;
                            trackFound.alternateTracks [ixs-1] = "00";
                        }
                    }
                    trackFound.setAlternateNum(attributes,ixs);
                }
                if (isPaste)
                    insertEntry(trackFound);
                else
                    trackVec.add(trackFound);
                    
                if (prevEntry==null)
                    trackMap.put(trackFileNameFixed,trackFound);
                else
                    prevEntry.nextEntry = trackFound;
                trackFound.prevEntry = prevEntry;
                prevEntry = trackFound;
            }
            else {
                if (ixs == track.alternateNum) {
                    copyTrack(track, trackFound);                
                    trackFound.setAlternateNum(attributes,track.alternateNum);
                    if (isPaste) {
                        moveEntry(trackFound);                        
                    }
                    else {
                        trackVec.removeElement(trackFound);
                        trackVec.add(trackFound);
                    }
                }
            }
            prevEntry = trackFound;
            trackFound = trackFound.nextEntry;
        }

    }
    
    public void clearLibrary() throws Exception {
        trackMap = new HashMap<String,LibraryTrack>();
        trackVec = new Vector<LibraryTrack>();
        RandomAccessFile raf = new RandomAccessFile(libraryFile,"rw");
        raf.setLength(0);
        raf.close();
        dirtyLibrary=false;
        libraryModifiedTime = libraryFile.lastModified();
    }

    String saveLibrary() throws Exception {
        Component header=mainFrame.mainTable.getTableHeader();
        header.setCursor(null);
        mainFrame.frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            updateDirty();
            if (dirtyLibrary) {
                loadLibrary();
            }
            if (readOnly)
                return "Library is Read Only";
//            PrintWriter libraryWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(libraryFile, false)));
            String charSet = getLibraryCharset();
            if (charSet == null) 
                charSet = upgradeLibraryCharset();
            libraryWriter = new PrintStream(new BufferedOutputStream(new FileOutputStream(libraryFile, false)), false, charSet);
            int ix;
            for (ix=0;ix<trackVec.size();ix++) {
                LibraryTrack track = trackVec.get(ix);
                if (track.status != 'D')
                    libraryWriter.println(track.toLibraryRec(attributes));
            }
            libraryWriter.close();
            boolean isError = libraryWriter.checkError();
            libraryModifiedTime = libraryFile.lastModified();
            libraryWriter = null;
            System.runFinalization(); // Try make sure library file is closed
            if (isError) 
                return "Error writing library " + libraryFile.getAbsolutePath();
            System.out.println("Saved: " + mainFrame.name);
            return null;
        }
        finally{
            mainFrame.frame.setCursor(null);
        }
    }

    String getLibraryCharset() throws IOException {
        return attributes.libraryProperties.getProperty("charset");
    }
    
    String upgradeLibraryCharset() throws IOException {
        String charSet = attributes.libraryProperties.getProperty("charset");
        if (charSet == null) {
            charSet = "UTF-8";
            attributes.libraryProperties.setProperty("charset", charSet);
            OutputStream outStream = new FileOutputStream(attributes.propFileName);
            try {
                attributes.libraryProperties.store(outStream,null);
            }
            finally {
                outStream.close();
            }
        }
        return charSet;
    }
    
}

