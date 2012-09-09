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
import java.text.DecimalFormat;
import java.util.*;
import pgbennett.id3.*;
import java.util.regex.*;

// Structure for storing one track

public class LibraryTrack {
    String columns[];
    // Terminology - track = 1 file on disk, entry = one entry in library file
    // There can be multiple entries per track
    // U = updated
    // 0 = Active (obsolete - becomes A)
    // D = Deleted
    // X = Newly pasted track before library saved
    // 2008/01/11 Alternate Albums
    // A = Active entry without alternates
    // Ann = Entry for Alternate Album nn = 00 to 99
    // A00 = primary 
    // Unn = Updated Alternate Entry
    // Anything else - treated as A
    
    char status = 'X';
    // Alternate seq num of this entry (0 means primary)
    short alternateNum = 0;
    public boolean hasAlternates = false;
    // Chain to next Alternate of track
    // When alternateAlbums is set they will always be in sequence
    LibraryTrack prevEntry = null;
    LibraryTrack nextEntry = null;
    // Array of alternate albums / tracks - only filled in when reading mp3 file
    // Not stored on library
    String alternateAlbums [] = null;
    String alternateTracks [] = null;
    String mainAlbum = null;
    String mainTrack = null;
    // Count of alternate albums - only filled in when reading mp3 file
    short alternateCount = -1;

    String sortKey;
    static Pattern columnPattern = Pattern.compile("[\0\n\t\r]");
    static Pattern newlinePattern = Pattern.compile("\\n");
    static Pattern fixIdPattern = Pattern.compile("[\0\n\t\r ]");

    LibraryTrack() {
    }

    LibraryTrack(String[] columns, short alternateNum, boolean hasAlternates) {
        this.columns = columns;
        this.alternateNum = alternateNum;
        this.hasAlternates = hasAlternates;
    }
    
    void copyFrom(LibraryTrack from) {
        columns = from.columns.clone();
        status = from.status;
        alternateCount = from.alternateCount;
        hasAlternates = from.hasAlternates;
        alternateAlbums  = from.alternateAlbums;
        alternateTracks  = from.alternateTracks;
        mainAlbum = from.mainAlbum;
        mainTrack = from.mainTrack;
    }
    
    
    void init(File file, LibraryAttributes attributes, short alternateNum) throws Exception {
        readMp3File(file, attributes, alternateNum);

    }

    // 2008/01/13 Alternates
    // 0 means base, greater than zero means alternate num
    MP3File readMp3File(File file, LibraryAttributes attributes, short alternateNum) throws Exception {
        columns = new String[attributes.numLibraryCol];
        this.alternateNum = alternateNum;
        MP3File mp3= new MP3File();
        mp3.init(file,MP3File.BOTH_TAGS);
        status = 'A';
        int i;
        // 2008/01/13 Alternate tracks
        String alternates = mp3.getMp3Field(MP3File.COL_ID3V2TAG,"COMMengJampal:Alternate");
        // store all alternates
        mainAlbum = mp3.getMp3Field(MP3File.COL_ALBUM,null);
        mainTrack = mp3.getMp3Field(MP3File.COL_TRACK,null);
        hasAlternates = false;

        int trackPos;
        alternateCount = 0;
        if (alternates != null && alternates.length()>0) {
            String [] alternate = newlinePattern.split(alternates);
            alternateAlbums = new String [alternate.length]; 
            alternateTracks = new String [alternate.length]; 
            for (i=0;i<alternate.length;i++) {
                trackPos = alternate[i].lastIndexOf(':');
                if (trackPos > -1) {
                    alternateAlbums [i] = alternate[i].substring(0,trackPos).trim();
                    alternateTracks [i] = alternate[i].substring(trackPos+1).trim();
                }
                else {
                    alternateAlbums [i] = alternate[i].trim();
                    alternateTracks [i] = "";
                }
                
            }
            alternateCount = (short)alternate.length;
            if (alternateCount > 0)
                hasAlternates = true;
        }
        for (i=0;i<attributes.numLibraryCol;i++) {
            if (attributes.colType[i]!=MP3File.COL_DUMMY) {
                String data = mp3.getMp3Field(attributes.colType[i],attributes.colId[i]);
                // If frame is present but empty, set one space
                // If frame not present leave empty string
                if (data.length()==0 && attributes.colId[i].length() > 0)
                    if (mp3.doesFrameExist(attributes.colId[i]))
                        data = " ";
                // Stop as soon as the first tab or line end char is found
                String [] split = columnPattern.split(data,2);
                columns[i]=split[0];
            }
            if (attributes.colType[i]==MP3File.COL_FRAMES) {
                String data = columns[i];
                data = " " + data + " ";
                // Remove frames that are in the library already.
                for (String frame : attributes.colId ) {
                    if (frame != null && frame.length() > 0) {
                        frame = fixIdPattern.matcher(frame).replaceAll("_");
                        frame = " " + frame + " ";
                        data = data.replace(frame, " ");
                    }
                }
                // we store in library with a leading and trailing space
                columns[i] = data;
            }
            if (columns[i]==null)
                columns[i]="";
            if (attributes.colType[i]==MP3File.COL_FILENAME) {
                columns[i]=attributes.translateFileName(columns[i]);
            }
        }
        if (alternateNum > 0 && alternateNum <= alternateCount) 
            setAlternateNum(attributes, alternateNum);
        return mp3;
    }
    
    // Calling this requires that the array of alternates has been set up
    void setAlternateNum(LibraryAttributes attributes, short alternateNum) {
        int i;
        String altAlbum;
        String altTrack;
        if (this.alternateNum == alternateNum)
            if (!hasAlternates || alternateCount < 1 || alternateAlbums == null)
                return;
        
        this.alternateNum = alternateNum;
        if (alternateNum == 0) {
            altAlbum = mainAlbum;
            altTrack = mainTrack;
        }
        else {
            altAlbum = alternateAlbums [alternateNum-1];
            altTrack = alternateTracks [alternateNum-1];
        }
        for (i=0;i<attributes.numLibraryCol;i++) {
            if (attributes.colType[i]==MP3File.COL_ALBUM) {
                columns[i] = altAlbum;
                continue;
            }
            if (attributes.colType[i]==MP3File.COL_TRACK) {
                try {
                    int x = Integer.parseInt(altTrack);
                    String s=String.valueOf(x);
                    if (s.length()==1)
                        s="0"+s;
                    columns[i] = s;
                } catch (NumberFormatException ex) {
                    columns[i] = "00";
                }
                continue;
            }
        }        
    }

    
    LibraryTrack(String trackStr,LibraryAttributes attributes) {
        int numCol=attributes.numLibraryCol;
        columns = new String[numCol];
        int i=0;
        StringTokenizer tok = new StringTokenizer(trackStr,"\t",true);
        String data=null;
        // first column is status
        if (tok.hasMoreTokens()) {
            data = tok.nextToken();
            status = data.charAt(0);
            // 2008/01/11 - Alternate Albums
            if ((status == 'U' || status == 'A'|| status == 'D') && data.length() > 1) {
                String numstr = data.substring(1);
                alternateNum = Short.parseShort(numstr);
                alternateCount = -1;
                hasAlternates = true;
            }
        }
        if (!"\t".equals(data)) {
            if (tok.hasMoreTokens())
              tok.nextToken();
        }
        for(i=0;tok.hasMoreTokens()&&i<numCol;i++) {
            data = tok.nextToken();
            if ("\t".equals(data))
                columns[i]=new String();
            else {
                columns[i]=data;
                if (tok.hasMoreTokens())
                    tok.nextToken();
            }
        }
        for(;i<numCol;i++)
            columns[i]="";
    }

    String toLibraryRec(LibraryAttributes attributes) {
        StringBuffer buf = new StringBuffer();
        buf.append(status);
        if (hasAlternates) {
            buf.append(Short.toString(alternateNum));
        }
        int i;
        for (i=0;i<attributes.numLibraryCol;i++) {
            buf.append('\t');
            if (columns[i]!=null)
                buf.append(columns[i]);
        }
        return buf.toString();
    }



}

