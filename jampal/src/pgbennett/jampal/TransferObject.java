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
import java.io.*;
import java.util.Arrays;
import java.util.regex.Pattern;
import pgbennett.id3.*;

public class TransferObject implements Serializable {
    // 1st subscript = track number 
    // 2nd subscript = column number of field
    String columns[][];
    // subscript = track number
    short alternateNum[];
    boolean hasAlternates[];
    int hashCodes[];
    // Name of the source mainframe
    public String mfSource;
    boolean isMove = false;
    boolean isFileFlavorSupported;
    
    int fileCount;
    int colCount;
    int [] colType;
    String[] colTag;
    int fileNameCol;
    
    public TransferObject() {
        
    }
    
    
    /** Creates a new instance of TransferObject */
    public TransferObject(MainFrame mainFrame) {
        mfSource = mainFrame.name;
        int [] selectedRows = mainFrame.mainTable.getSelectedRows();
        fileCount = selectedRows.length;
        LibraryAttributes attributes = mainFrame.library.attributes;
        colCount = attributes.numLibraryCol;
        columns = new String[fileCount][colCount];
        colType = new int [colCount];
        colTag = new String [colCount];
        alternateNum = new short[fileCount];
        hasAlternates = new boolean[fileCount];
        hashCodes = new int[fileCount];
        System.arraycopy(attributes.colType,0, colType, 0,colCount);
        System.arraycopy(attributes.colId,0, colTag, 0,colCount);
        fileNameCol = attributes.fileNameCol;
        int ix;
        for (ix=0;ix<fileCount;ix++) {
            LibraryTrack track = (LibraryTrack)mainFrame.library.trackVec.get(selectedRows[ix]);
            System.arraycopy(track.columns,0, columns[ix], 0,colCount);
            alternateNum[ix] = track.alternateNum;
            hasAlternates[ix] = track.hasAlternates;
            hashCodes[ix] = track.hashCode();
        }
    }
    
    MainFrame mfDest;
    int [] colSource;
    boolean isColMatch = false;
    int [] colsInOldNotNew;
    int colsInOldNotNewCount = 0;
    
    boolean updateLibrary(MainFrame mainFrame, boolean isFileFlavorSupported) throws Exception {
        this.isFileFlavorSupported = isFileFlavorSupported;
        this.mfDest = mainFrame;
        Library library = mainFrame.library;
        LibraryAttributes attributes = library.attributes;
        colSource = new int[attributes.numLibraryCol];
        int ix;
        int ix2;
        isColMatch = true;
        for (ix=0;ix<attributes.numLibraryCol;ix++) {
            colSource[ix]=-1;
            boolean found = false;
            for (ix2=0;ix2<colCount;ix2++) {
                if (attributes.colType[ix]==colType[ix2]) {
                    if (attributes.colType[ix] != MP3File.COL_ID3V2TAG 
                    || attributes.colId[ix]==null
                    || attributes.colId[ix].length()==0
                    || attributes.colId[ix].equals(colTag[ix2])) {
                        colSource[ix]=ix2;
                        found = true;
                        break;
                    }
                }
            }
            // If any required column is not found - return false
            if (!found) {
                isColMatch = false;
            }
        }
        colsInOldNotNew = new int [colCount];
        for (ix=0; ix<colCount; ix++)  {
            boolean found=false;
            for (ix2=0; ix2<attributes.numLibraryCol; ix2++) {
//                if (colTag[ix].equals(attributes.colId[ix2])) {
                // a null colTag[ix] occurs with loading m3j file.
                // set it as found true because we do not want it in the 
                // colsInOldNoNew list
                if (colTag[ix]== null || colTag[ix].equals(attributes.colId[ix2])) {
                    found=true;
                    break;
                }

            }
            if (!found)
                colsInOldNotNew[colsInOldNotNewCount++] = ix;
        }
        library.startBulkUpdate();
        // if dropping on the same library perform a move
        // otherwise a copy
        isMove = false;
        if (mainFrame.name.equals(mfSource)) 
            isMove = true;
        
        mainFrame.transferObject = this;
        mainFrame.runType = 'P';
        mainFrame.progressBox = new ProgressBox(mainFrame.frame,"Add Tracks to Library");
        library.startBulkUpdate();
        Thread thread = new Thread(mainFrame,"mainframe-addtracks-paste");
        mainFrame.runType = 'P'; // Paste Tracks
        thread.start();
        return true;
    }
    
    static Pattern framesPattern = Pattern.compile(" ");
    static Pattern fixIdPattern = Pattern.compile("[\0\n\t\r ]");

    void updateOneFile(int ix) throws Exception{
        Library library = mfDest.library;
        LibraryAttributes attributes = library.attributes;
        int ix2;
        LibraryTrack track = null;
        File file = null;
        if (isMove) {
            if (track == null) {
                String fileName = columns[ix][fileNameCol];
                fileName = attributes.fixFileNameCase(fileName);
                LibraryTrack trackSearch = library.trackMap.get(fileName);
                while (trackSearch != null) {
                    if (trackSearch.alternateNum == alternateNum[ix]) {
                        if (trackSearch.hashCode() == hashCodes[ix]) {
                            track = trackSearch;
                            break;
                        }
                    }
                    trackSearch = trackSearch.nextEntry;
                }
            }
            if (track != null) {
                library.moveEntry(track);
            }
        }
        if (track == null) {
            // was not found above
            if (isColMatch || !isFileFlavorSupported) {
                String[] trackColumns = new String[attributes.numLibraryCol];
                for (ix2 = 0; ix2 < attributes.numLibraryCol; ix2++) {
                    if (colSource[ix2] != -1) {
                        trackColumns[ix2] = columns[ix][colSource[ix2]];
                        if (attributes.colType[ix2] == MP3File.COL_FRAMES
                                && colsInOldNotNewCount>0) {
                            // find frames that are in the old library but not
                            // in the new library. Check if they are valued in the old
                            // library and if so update the frame list in the new library
                            String frames = trackColumns[ix2];
                            frames = frames.substring(0, frames.length()-1);
                            int ix3;
                            boolean changed = false;
                            for (ix3=0; ix3<colsInOldNotNewCount; ix3++) {
                                if (columns[ix][colsInOldNotNew[ix3]].length()>0) {
                                    String frame = colTag[colsInOldNotNew[ix3]].toString();
                                    frame = fixIdPattern.matcher(frame.toString()).replaceAll("_");
                                    frames = frames + " " + frame;
                                    changed=true;
                                }
                            }
                            if (changed) {
                                // sort the frames
                                String [] keys = framesPattern.split(frames.substring(1));
                                StringBuilder sBuild = new StringBuilder();
                                Arrays.sort(keys);
                                sBuild.append(' ');
                                for (String key : keys) {
                                    sBuild.append(key);
                                    sBuild.append(' ');
                                }
                                // we store in library with a leading and trailing space
                                trackColumns[ix2] = sBuild.toString();
                            }

                        }
                    }
                }
                trackColumns[attributes.fileNameCol] =
                        attributes.translateFileName(trackColumns[attributes.fileNameCol]);
                track = new LibraryTrack(trackColumns, alternateNum[ix], hasAlternates[ix]);
            } else {
                file = new File(columns[ix][fileNameCol]);
            }
            if (library.attributes.libraryType == 'L') {
                library.synchronizeTrack(track, file, '1'); // true
            } else {
                if (hasAlternates[ix] && track == null) {
                    track = new LibraryTrack();
                    track.init(file, library.attributes, alternateNum[ix]);
                }
                library.addEntry(track, file);
            }
        }
    }
    
}
