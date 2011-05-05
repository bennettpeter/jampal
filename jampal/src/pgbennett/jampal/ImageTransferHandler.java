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
import java.net.*;

public class ImageTransferHandler extends TransferHandler {
    TrackEditor editor;
    TrackEditor.Frame frame;
    
    ImageTransferHandler(TrackEditor editor, TrackEditor.Frame frame) {
        this.editor = editor;
        this.frame = frame;
    }
    
    
    public int getSourceActions(JComponent component) {
        return COPY;
    }
    
    public boolean importData(JComponent component, Transferable transferable) {
        try {
            boolean isDone=false;
            Object data = null;
            if (!isDone && transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                data = transferable.getTransferData(DataFlavor.javaFileListFlavor);
            }
            if (!isDone && data == null) {
                DataFlavor [] flavors = transferable.getTransferDataFlavors();
                DataFlavor bestFlavor = DataFlavor.selectBestTextFlavor (flavors);
                // Reader reader = (Reader)transferable.getTransferData(bestFlavor);
                Reader reader = bestFlavor.getReaderForText(transferable);
                BufferedReader bufferedReader = new BufferedReader(reader);
                Vector fileVec = new Vector();
                while(true) {
                    String inputLine = bufferedReader.readLine();
                    if (inputLine==null)
                        break;
                    File file;
                    try {
                        java.net.URL url = new java.net.URL(inputLine);
                        fileVec.add(url);
                    }
                    // work around for failure that occurs on ubuntu linux
                    // where a faulty string consists of html with nulls
                    catch(MalformedURLException murl) {
                        StringBuffer repaired = new StringBuffer();
                        if (inputLine.length()>=2 && inputLine.charAt(1)==0) {
                            for (int ix = 0; ; ix+=2) {
                                if (ix >= inputLine.length())
                                    break;
                                repaired.append(inputLine.charAt(ix));
                            }
                        }
                        else {
                            repaired.append(inputLine);
                        }
                        if (repaired.length() == 0)
                            continue;
                        int ix = repaired.indexOf("src=\"");
                        if (ix >= 0) {
                            repaired.delete(0, ix+5);
                            ix  = repaired.indexOf("\"");
                            repaired.delete(ix, repaired.length());
                        }
                        if (repaired.indexOf("<") >= 0 || repaired.indexOf(">") >= 0)
                            continue;
                        try {
                            java.net.URL url = new java.net.URL(repaired.toString());
                            fileVec.add(url);
                        }
                        catch(MalformedURLException murl2) {
                            file = new File(inputLine);
                            fileVec.add(file);
                        }
                    }
                }
                data = fileVec;
            }            
            if (!isDone && data != null) {
                // How many files ?
                if (data instanceof java.util.List) {
                    java.util.List al = (java.util.List) data;
                    // Read the first File.
                    if (al.size() > 0) {
                        // Use only the first file
                        ListIterator li = al.listIterator();
                        if (li.hasNext()) {
                            Object obj = li.next();
                            if (obj instanceof File)
                                editor.loadImage(frame,(File) obj);
                            if (obj instanceof URL)
                                editor.loadImage(frame,(URL) obj);
                            // turn on the checkbox
                            if (!frame.checkBox.isSelected() && frame.picture.pictureData.length > 0)
                                frame.checkBox.doClick();
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }
    
    protected void exportDone(JComponent component, Transferable data, int action) {
    }
    
    public boolean canImport(JComponent component, DataFlavor[] flavors) {
        for (int i = 0; i < flavors.length; i++) {
            if (DataFlavor.javaFileListFlavor.equals(flavors[i])) {
                return true;
            }
        }
        DataFlavor bestFlavor = DataFlavor.selectBestTextFlavor (flavors);
        if (bestFlavor != null)
            return true;
        
        return false;
    }
}

