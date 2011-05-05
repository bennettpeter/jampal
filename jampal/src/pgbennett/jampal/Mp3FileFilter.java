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


import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;


public class Mp3FileFilter extends FileFilter implements java.io.FileFilter {
    
    String reqdExtension = "mp3";
    String description = "mp3 Files";
    
    public void setExtension(String reqdExtension, String description) {
        this.reqdExtension = reqdExtension;
        this.description = description;
    }

    //Accept all directories and all mp3 files.
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = getExtension(f);
        if (reqdExtension.equalsIgnoreCase(extension))
            return true;
        else
            return false;
    }

    //The description of this filter
    public String getDescription() {
        return description;
    }

    /*
     * Get the extension of a file.
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }



}

