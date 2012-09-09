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
import java.util.*;
import java.io.*;

public class TrackTransferable implements Transferable {
    
    Vector fileVec;
    DataFlavor trackFlavor;
    public TransferObject transferObject;
    
    public TrackTransferable(TransferObject transferObject, Vector fileVec) {
        try {
            trackFlavor = new  DataFlavor(Class.forName("pgbennett.jampal.TransferObject"),
                            "Jampal Track");
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        this.transferObject = transferObject;
        this.fileVec = fileVec;
    }
    
    public TrackTransferable(MainFrame mainFrame) {
        try {
            trackFlavor = new  DataFlavor(Class.forName("pgbennett.jampal.TransferObject"),
                            "Jampal Track");
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        // 2 transfer objects, one file names and one our own
        TrackUpdater updater = new TrackUpdater(mainFrame);
        String [] fileNames = updater.getFileNames();
        fileVec = new Vector();
        int ix;
        for (ix=0;ix<fileNames.length;ix++) {
            // do not add fake names used by m3j files
            if (!fileNames[ix].startsWith("/*"))
                fileVec.add(new File(fileNames[ix]));
        }
        transferObject = new TransferObject(mainFrame);
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, java.io.IOException {
        if (flavor.equals(DataFlavor.javaFileListFlavor) && fileVec!= null)
            return fileVec;
        else if (flavor.equals(trackFlavor))
            return transferObject;
        else if (flavor.equals(DataFlavor.stringFlavor) && fileVec!= null) {
            StringBuffer buff = new StringBuffer();
            String lineSep = System.getProperty("line.separator");
            int ix;
            for (ix=0;ix<fileVec.size();ix++) {
                if (buff.length()>0)
                    buff.append(lineSep);
                buff.append(fileVec.get(ix).toString());
            }
            return buff.toString();
        }
        else         
            throw (new UnsupportedFlavorException(flavor));
    }
    
    
    public DataFlavor[] getTransferDataFlavors() {
        Vector flavors = new Vector();
        if (transferObject!=null) {
            flavors.add(trackFlavor);
        }
        if (fileVec!=null && fileVec.size()>0) {
            flavors.add(DataFlavor.javaFileListFlavor);
            flavors.add(DataFlavor.stringFlavor);
        }
        DataFlavor[] ret = new DataFlavor[flavors.size()];
        return (DataFlavor[]) flavors.toArray(ret);
        
    }
    
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return (flavor.equals(DataFlavor.javaFileListFlavor)&&fileVec!=null)
            || (flavor.equals(trackFlavor)&&transferObject!=null);
    }
    
}


