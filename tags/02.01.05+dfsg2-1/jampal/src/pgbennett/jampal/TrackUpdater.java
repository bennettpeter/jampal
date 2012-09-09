package pgbennett.jampal;

/*
    Copyright 2004 Peter Bennett

    This file is part of Jampal.

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
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import pgbennett.id3.*;
import java.util.regex.*;


/** Gets an array of the selected file names.
 * Note that there may be some null entries at the end of
 * the array if some files were deleted from the library
 * and blank lines show in the list.
 * Runs in the AWT thread
 */
public class TrackUpdater implements Runnable{
    TrackEditor editor;
    MainFrame mainFrame;
    int [] selectedRows;
    JList updateList;
    int fileCount;
    JDialog updateDialog;
    ProgressBox progressBox;
    int updateCounter;
    Vector errorMessages;
    boolean isSimpleUpdate;



    TrackUpdater(TrackEditor editor) {
        this.editor = editor;
        this.mainFrame = editor.mainFrame;
    }

    TrackUpdater(MainFrame mainFrame) {
        this.editor = null;
        this.mainFrame = mainFrame;
    }


    void updateTags() {
        boolean isOK=true;
        String [] fileNames = getFileNames();
        if (fileNames.length == 1 && fileNames[0]!=null && fileNames[0].equals(editor.fileName))
            isSimpleUpdate=true;
        else
            isSimpleUpdate=false;

        if (fileNames.length ==0 
            || (fileNames.length ==1 && fileNames[0]==null)) {
            JOptionPane.showMessageDialog(editor.dialog,
                "No Track Selected for update",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!isSimpleUpdate) {
            FileListDialog listdialog = new FileListDialog(editor.dialog, 
                "Update Track Tags" ,fileNames,true,true);
            if (!"ok".equals(listdialog.response)) 
                isOK=false;
        }
        if (isOK) {
            Set <Map.Entry<String,MainFrame>> mainframes = mainFrame.mainFrameMap.entrySet();
            for (Map.Entry<String,MainFrame> entry : mainframes) {
                MainFrame mf = entry.getValue();
                mf.library.selectedRows = mf.mainTable.getSelectedRows();
                mf.library.insertBefore = -1;
                mf.library.firstInserted = -1;
                mf.library.numberInserted = 0;
                
            }
            

            
            progressBox = new ProgressBox(editor.dialog,"Update Track Tags");
            Thread thread = new Thread(this,"trackupdater");
            thread.start();
        }
    }

    
    /** Gets an array of file names of songs selected in the table.
     * Note that there may be null values at the end of the array.
     * Runs in the AWT thread.
     * @return Array of file names
     */    
    String [] getFileNames() {
        // Create an array of file names
        selectedRows = mainFrame.mainTable.getSelectedRows();
        fileCount = selectedRows.length;
        String [] fileNames = new String[fileCount];
        int ix = 0;
        int outIx=0;
        for (ix=0;ix<fileCount;ix++) {
            LibraryTrack track = (LibraryTrack)mainFrame.library.trackVec.get(selectedRows[ix]);
            if (track.status != 'D')
                fileNames[outIx++]=track.columns[mainFrame.library.attributes.fileNameCol];
        }
        return fileNames;
    }
    boolean runDeletes;

    // Runs in the AWT thread
    void deleteTracks() throws Exception {
        String [] fileNames = getFileNames();
        if (fileNames.length==0) {
            JOptionPane.showMessageDialog(mainFrame.frame,
            "Please select tracks to delete",
            "Cannot Compress Library",
            JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (mainFrame.library.attributes.libraryType == 'L') {
            String response = "ok";
            FileListDialog listdialog = new FileListDialog(mainFrame.frame, 
                "Delete Tracks from Library" ,fileNames,true,true);
            response = listdialog.response;
            if (!"ok".equals(response)) {
                return;
            }
            
        }
        mainFrame.library.selectedRows = selectedRows;
        mainFrame.library.insertBefore = -1;
        mainFrame.library.firstInserted = -1; 
        mainFrame.library.numberInserted = 0; 

        
        int ix;
        boolean wasUpdated=false;
        Vector trackVec = mainFrame.library.trackVec;
        try {
            mainFrame.library.openLibrary();
            int firstSelected = -1;
            for (ix=0;ix<selectedRows.length;ix++) {
                if (selectedRows[ix] == -1)
                    continue;
                if (firstSelected == -1)
                    firstSelected = selectedRows[ix];
                LibraryTrack track = (LibraryTrack)trackVec.get(selectedRows[ix]);
                if (mainFrame.library.attributes.libraryType == 'L')
                    mainFrame.library.deleteTrack(track);
                else
                    mainFrame.library.deleteEntry(track);
                wasUpdated=true;
            }
            if (wasUpdated) {
                // We need to hide here for an instant
                // If we do not do this, AWT / Swing takes a very long
                // time to recover from firing table data changed
                // if currect position is at end of list and list is now
                // shorter
                // 4/4/2008 - this is causing unacceptable flickering
                // and seems to be ok with java 5 - so removed
                // mainFrame.frame.setVisible(false);
                mainFrame.model.resetSort(true);
                if (firstSelected < mainFrame.mainTable.getRowCount())
                    mainFrame.mainTable.addRowSelectionInterval(firstSelected, firstSelected);
            }
        }
        finally {
            mainFrame.frame.setVisible(true);
            mainFrame.library.closeLibrary();
        }
        
    }


    // This is run in a worker thread
    public void run () {
        int ix = 0;
        String fileName;
        errorMessages=new Vector();
        Random random = new Random();
        
        boolean superTagging = editor.superTagCheckBox.isSelected();
        boolean titleCasing = editor.titleCaseCheckBox.isSelected();
        mainFrame.library.selectedRows = selectedRows;
        mainFrame.library.insertBefore = -1;
        mainFrame.library.firstInserted = -1;
        mainFrame.library.numberInserted = 0;
        for (ix=0;ix<selectedRows.length;ix++) {
            if (selectedRows[ix] == -1)
                continue;
            LibraryTrack track = (LibraryTrack)mainFrame.library.trackVec.get(selectedRows[ix]);
            fileName=track.columns[mainFrame.library.attributes.fileNameCol];
            File file = new File(fileName);
            try {
                if (progressBox.cancel)
                    break;
                if (track.status == 'D')
                    continue;
                String [] superTags=null;
                if (superTagging)
                    superTags = parseFileName(file,editor.delimTextField.getText());
                MP3File mp3 = new MP3File();
                try {
                    mp3.init(file,MP3File.BOTH_TAGS);
                }
                catch(ID3Exception ex) {
                    ex.printStackTrace();
                }
                mp3.syncV1AndV2();
                int numFrames = editor.frames.size();
                int iy;
                for (iy=0;iy<numFrames;iy++) {
                    TrackEditor.Frame frame = (TrackEditor.Frame)editor.frames.get(iy);
                    if (frame.checkBox.isSelected()) {
                        if (track.alternateNum != 0 && !isSimpleUpdate) {
                            if (frame.colType == MP3File.COL_ALBUM || frame.colType == MP3File.COL_TRACK) {
                                errorMessages.add("Cannot update alternate album / track on " +
                                        mp3.getMp3Field(MP3File.COL_TITLE, null) + 
                                        "("+file.getName() + ")" +
                                        "\nOther frames (if any) are updated." +
                                        "\nPlease update Album on this track individually.");
                                continue;
                            }
                        }
                        if (frame.id.startsWith("APIC")) {
                            if (titleCasing) 
                                continue;
                            if (frame.descText==null)
                                frame.picture.Description=frame.id.substring(6);
                            else    
                                frame.picture.Description = frame.descText.getText();
                            if (frame.pictureType==null)
                                frame.picture.pictureType = (byte)Integer.parseInt(frame.id.substring(4,6),16);
                            else
                                frame.picture.pictureType = (byte)frame.pictureType.getSelectedIndex();
                            mp3.setMp3Picture(frame.id, frame.picture);
                        }
                        else if (frame.dataType == 'N') {
                            if (titleCasing) 
                                continue;
                            String text = ((JTextComponent)frame.value).getText().trim();
                            mp3.setMp3Integer(frame.id, text);
                        }
                        else {
                            String langCode=null;
                            String descText=null;
                            if (frame.langCode!=null)
                                langCode=frame.langCode.getText();
                            if (frame.descText!=null)
                                descText=frame.descText.getText();
                            String text;
                            if (frame.dataType=='L') {
                                text="";
                                for (JComboBox comboBox : frame.langValueCombo) {
                                    String langName = (String)comboBox.getSelectedItem();
                                    if (langName == null)
                                        langName="";
                                    String langValueCode = (String)mainFrame.library.attributes.langNameMap.get(langName);
                                    if (langValueCode==null)
                                        langValueCode="";
                                    text = text + langValueCode;
                                }
//                                text=langCode;
//                                langCode=null;
                            }
                            else {
                                if (frame.value==null)
                                    text="";
                                else
                                    text = ((JTextComponent)frame.value).getText().trim();
                            }
                            if (titleCasing) {
                                text = titlecase(mp3.getMp3Field
                                        (frame.colType, frame.id));
                            }
                            String newText;
                            if (superTagging && text.startsWith("#")) {
                                try {
                                    if (text.startsWith("#RAN")) {
                                        int key = random.nextInt(98)+1;
                                        newText = String.valueOf(key);
                                    }
                                    else if (text.startsWith("#DIR")) {
                                        newText = superTags[0];
                                    }
                                    else if (text.startsWith("#TTL")) {
                                        newText = titlecase(mp3.getMp3Field
                                                (frame.colType, frame.id));
                                    }
                                    else {
                                        int tagSeq=Integer.parseInt(text.substring(1));
                                        newText=superTags[tagSeq];
                                    }
                                }
                                catch (Exception ex) {
                                    errorMessages.add("Could not apply " +
                                        text + " to " +
                                        mp3.getMp3Field(MP3File.COL_TITLE, null) + 
                                        "("+file.getName() + ")" +
                                        " " + ex.toString());
                                    continue;
                                }
                                text=newText;
                            }
                            // Do not set null values
                            if (text!=null) { 
                                mp3.setMp3Field(frame.colType, frame.id, 
                                    langCode, descText,text) ;
                            }
                        }
                    }
                }
                // If there is no title put the file name there
                if (mp3.getTitle().length() == 0) {
                    String title = mp3.getMp3Field(MP3File.COL_TITLE, null);
                    mp3.setMp3Field( MP3File.COL_TITLE, null, 
                                    null, null,title);
                }
                mp3.writeTags();
                boolean boo = mainFrame.library.updateTrackAllViews(new File(fileName),null);
                updateCounter++;
                progressBox.progressText = "Number of Tracks Updated: " +updateCounter;
                progressBox.updateProgress();
            }
            catch (Exception ex) {
                errorMessages.add("Error occurred updating tag on file " +file.getName() +
                                                        " " + ex.toString());
                ex.printStackTrace();
            }
        }
        progressBox.stop();
        SwingUtilities.invokeLater(editor);
    }

    

    /**
     * Convert a string to Title Case.
     */
    static String titlecase(String value) {
        StringTokenizer tok = new StringTokenizer(value," \n",true);
        String temp="";
        int ix;
        boolean spacefound=false;
        while(tok.hasMoreTokens()) {
            String word = tok.nextToken();
            if (" ".equals(word)) {
                if (spacefound)
                    continue;
                else
                    spacefound=true;
            }
            else
                spacefound=false;
            int firstLetter = -1;
            for (ix=0;ix<word.length();ix++) {
                if (Character.isLetter(word.charAt(ix))) {
                    firstLetter = ix;
                    break;
                }
            }
            if (firstLetter>=0) {
                word =  word.substring(0,firstLetter) + 
                    word.substring(firstLetter,firstLetter+1).toUpperCase() + 
                    word.substring(firstLetter+1).toLowerCase();
            }
            temp = temp + word;
        }
        return temp;
    }
    
    /** Parse file name for super tagging
     * @param file File Name
     * @param delim Delimiter string
     * @return Array of strings. [0] contains high level directory name.
     * [1] onwards contain strings from the file name.
     */    
    static String [] parseFileName(File file, String delim) {
        // get file name from full path
        String fileName = file.getName();
        int pos;
        // strip extension
        pos = fileName.lastIndexOf('.');
        if (pos != -1) 
            fileName = fileName.substring(0,pos);
        fileName=MP3File.fileNameReplacements(fileName);
        File parentFile = file.getParentFile();
        String parentName="";
        if (parentFile!=null)
            parentName = parentFile.getName();
        parentName=MP3File.fileNameReplacements(parentName);
        String [] resp = new String[21];
        resp[0]=parentName;
        pos = 0;
        int ix;
        for (ix=1;ix<21;ix++) {
            int posNext = fileName.indexOf(delim,pos);
            if (posNext == -1) {
                resp[ix] = fileName.substring(pos);
                break;
            }
            resp[ix]=fileName.substring(pos,posNext);
            pos = posNext+delim.length();
        }
        return resp;
    }

    // This is run in the awt thread after finishing
    void finish() {
        mainFrame.fireChangeKeepSelection();
//        Set <Map.Entry<String,MainFrame>> mainframes = mainFrame.mainFrameMap.entrySet();
//        for (Map.Entry<String,MainFrame> entry : mainframes) {
//            MainFrame mf = entry.getValue();
//
//            int [] selection;
//            selection = mf.library.selectedRows;
//            mf.model.fireTableDataChanged();
//            int iy;
//            for (iy=0;iy<selection.length;iy++){
//                if (selection[iy]!= -1)
//                    mf.mainTable.addRowSelectionInterval(selection[iy],selection[iy]);
//            }
//        }
            
        try {
            if (editor!=null && isSimpleUpdate)
                editor.refresh();
            else if (editor!=null)
                editor.dialog.setVisible(true);
            else
                mainFrame.frame.setVisible(true);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        if (errorMessages!=null && errorMessages.size()>0) {
            String [] errorText = new String [errorMessages.size()];
            errorMessages.toArray(errorText);
            FileListDialog listdialog = new FileListDialog(editor.dialog, 
                "Update Track Tags" ,errorText,false,true);
        }
    }

}