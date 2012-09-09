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


import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.table.*;
import java.io.*;
import java.util.*;

public class TableTransferHandler extends TransferHandler {
    MainFrame mainFrame;
    DataFlavor trackFlavor;
    
    TableTransferHandler(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        try {
            trackFlavor = new  DataFlavor(Class.forName("pgbennett.jampal.TransferObject"),
                            "Jampal Track");
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    protected Transferable createTransferable(JComponent component) {
        TrackTransferable ret = new TrackTransferable(mainFrame);
        return ret;
    }
    
    public int getSourceActions(JComponent component) {
        return COPY; // COPY_OR_MOVE;
    }
    
    public boolean importData(JComponent component, Transferable transferable) {
        boolean isDone=false;
        try {
            
            if (component.equals(mainFrame.mainTable.getTableHeader()))
                mainFrame.mainTable.clearSelection();
            else if (!component.equals(mainFrame.mainTable)) {
                mainFrame.mainTable.clearSelection();
                int lastRow=mainFrame.mainTable.getRowCount()-1;
                if (lastRow >= 0)
                    mainFrame.mainTable.addRowSelectionInterval(lastRow,lastRow);
            }
            if (transferable.isDataFlavorSupported(trackFlavor)) {
                Object data = transferable.getTransferData(trackFlavor);
                if (data instanceof TransferObject) {
                    TransferObject transferObject = 
                            (TransferObject)transferable.getTransferData(trackFlavor);
                    // Note - this always returns true into isDone
                    isDone = transferObject.updateLibrary(mainFrame,
                        transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor));
                }
            }
            Object data = null;
            if (!isDone && transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                data = transferable.getTransferData(DataFlavor.javaFileListFlavor);
            }
            if (!isDone && data == null) {
                DataFlavor [] flavors = transferable.getTransferDataFlavors();
                DataFlavor bestFlavor = DataFlavor.selectBestTextFlavor (flavors);
                Reader reader = bestFlavor.getReaderForText(transferable);
                BufferedReader bufferedReader = new BufferedReader(reader);
                Vector fileVec = new Vector();
                while(true) {
                    String inputLine = bufferedReader.readLine();
                    if (inputLine==null)
                        break;
                    File file;
                        try {
                            java.net.URI uri = new java.net.URI(inputLine);
                            file = new File(uri);
                        }
                        catch (Exception ex) {
                            file = new File(inputLine);
                        }
                    fileVec.add(file);
                }
                data = fileVec;
            }            
            if (!isDone && data != null) {
                // How many files ?
                if (data instanceof java.util.List) {
                    java.util.List al = (java.util.List) data;
                    mainFrame.fileChoose = new File[al.size()];
                    // Read the first File.
                    if (al.size() > 0) {
                        // Add all dropped files to playlist.
                        ListIterator li = al.listIterator();
                        int ix = 0;
                        while (li.hasNext()) {
                            mainFrame.fileChoose [ix++] = (File) li.next();
                        }
                        mainFrame.progressBox = new ProgressBox(mainFrame.frame,"Add Tracks to Library");
                        Thread thread = new Thread(mainFrame,"mainframe-addtracks-files");
                        // start playing
                        if (mainFrame.library.attributes.libraryType == 'P')
                            mainFrame.startPlaying = true;
                        mainFrame.library.startBulkUpdate();
                        mainFrame.runType = 'A'; // Add Tracks
                        thread.start();
                    }
                    isDone=true;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame.frame,
                         ex.toString(),
                         "Error",
                         JOptionPane.ERROR_MESSAGE);
        }
        // maybe Don't want to return true because they may have done ctl-x or alt-move
        // Does not seem to cause sending file to be deleted.
        return isDone;
    }
    
    protected void exportDone(JComponent component, Transferable data, int action) {
        if (action==MOVE) {
                        
        }
    }
    
    public boolean canImport(JComponent component, DataFlavor[] flavors) {
        for (int i = 0; i < flavors.length; i++) {
            if (DataFlavor.javaFileListFlavor.equals(flavors[i])) {
                return true;
            }
            if (trackFlavor.equals(flavors[i])) {
                return true;
            }
                        
        }
        DataFlavor bestFlavor = DataFlavor.selectBestTextFlavor (flavors);
        if (bestFlavor != null)
            return true;
        
        return false;
    }
}

