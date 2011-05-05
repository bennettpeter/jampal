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

public class LibraryAttributes {

    // Library attributes loaded from properties file
    public int [] colType;
    public String[] colId;
    public String[] colHeading;
    // Indexes into above arrays for sequence of display columns
    public int [] displayCol;
    public int numDisplayCol;
    public int numLibraryCol;
    int numTagEditCol;
    int fileNameCol=-1;

    // tag edit attributes loaded from properties file
    public int [] editColType;
    public String[] editColId;
    public String[] editColHeading;

    // each entry of this is frameAttributes 
    // (loaded from frame-types in properties file

    int dialogMaxHeight = 700;
    int dialogMaxWidth = 700;

    /**
     * langCodeProps has language name keyed by language code.
     * It has all language codes, both bibliographic and terminologic.
     */
    public Properties langCodeProps;
    /**
     * langNameMap has terminologic language code keyed by language name.
     * Bibliographic codes are not here.
     */
    public TreeMap langNameMap;
    /**
     * langNames contains all language names alphabetically
     * without duplicates.
     */
    public String[] langNames;

    /**
     * langFixMap contains translation from bibliographic to terminologic
     * language code
     */
    public HashMap <String, String> langFixMap;

    public HashMap <String, String> shortLangMap;
    
    /** 'L' = Library, 'P' = Playlist */    
    char libraryType;
    
    String playListName;
    boolean compact=false;

    public Properties libraryProperties;
    public String propFileName;
    
    boolean unixPathTranslate=false;
    boolean filenameCaseSensitive=false;
    int deleteFrameIx = -1;


    

    LibraryAttributes(String fileName) throws Exception {
        init(fileName);
    }
    void init(String fileName) throws Exception {
        propFileName=fileName;
        libraryProperties = new Properties();
        InputStream stream;
        stream = ClassLoader.getSystemResourceAsStream(("pgbennett/jampal/jampal.properties"));
        try {
            libraryProperties.load(stream);
        }
        finally {
            stream.close();
        }
        stream = ClassLoader.getSystemResourceAsStream(("pgbennett/jampal/minimal.jampal"));
        try {
            libraryProperties.load(stream);
        }
        finally {
            stream.close();
        }
        stream = new FileInputStream(fileName);
        try {
            libraryProperties.load(stream);
        }
        finally {
            stream.close();
        }
        String version = libraryProperties.getProperty("file-version");
        boolean saveProperties = false;
        if (version == null || version.compareTo("002") < 0) {
            // upgrade to 002
            libraryProperties.setProperty("file-version","002");
            libraryProperties.setProperty("speechtemplate","(TITLE). (ARTIST)");
            int ix;
            for (ix=1;;ix++) {
                String pattern = libraryProperties.getProperty("speech"+ix+"a");
                if (pattern==null) 
                    break;
                else {
                    libraryProperties.remove("speech"+ix+"a");
                    libraryProperties.remove("speech"+ix+"b");
                }
            }
            saveProperties = true;
        }
        String deleteFrame = libraryProperties.getProperty("delete-frame");
        if (deleteFrame == null) {
            deleteFrame = "TXXXjampal";
            libraryProperties.setProperty("delete-frame",deleteFrame);
            saveProperties = true;
        }
        if (saveProperties) {
            OutputStream outStream = new FileOutputStream(propFileName);
            try {
                libraryProperties.store(outStream,null);
            }
            finally {
                outStream.close();
            }
        }
        
        loadLibraryCols();
        loadTagEditorCols();
        GraphicsEnvironment graphics = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gdevice = graphics.getDefaultScreenDevice();
        GraphicsConfiguration gconfig = gdevice.getDefaultConfiguration();
        Rectangle grect = gconfig.getBounds();
        dialogMaxHeight = grect.height - 80;
        dialogMaxWidth = grect.width;

        libraryType = libraryProperties.getProperty("type").charAt(0);
        langCodeProps=new Properties();
        playListName = libraryProperties.getProperty("playlist");
        File playListFile = new File(playListName);
        if (playListFile.getParent() == null) {
            File mainFile = new File(fileName);
            String parentDir = mainFile.getParent();
            if (parentDir != null) {
                playListName = parentDir + File.separator + playListName;
            }
        }

        String deleteFrameId = libraryProperties.getProperty("delete-frame");
        deleteFrameIx = -1;
        for (int ix = 0; ix <  colId.length; ix++) {
            if (deleteFrameId.equals(colId[ix])) {
                deleteFrameIx = ix;
                break;
            }
        }

        langNameMap=new TreeMap();
        langFixMap = new HashMap <String, String>();
        shortLangMap = new HashMap <String, String>();
        try {
//            langCodeProps.load(ClassLoader.getSystemResourceAsStream(("pgbennett/jampal/languages.properties")));
            // this file came from http://www.loc.gov/standards/iso639-2/ISO-639-2_utf-8.txt
            InputStream inStream = ClassLoader.getSystemResourceAsStream("pgbennett/jampal/ISO-639-2_utf-8.txt");
            Reader reader = new InputStreamReader(inStream,"UTF-8");
            BufferedReader bufferedReader = new BufferedReader(reader);
            String inLine = "";
            while (inLine != null) {
                inLine = bufferedReader.readLine();
                if (inLine != null && inLine.length() > 3 && !inLine.startsWith("#")) {
                    // sample input lines
                    // ger|deu|de|German|allemand
                    // eng||en|English|anglais
                    String [] fields = inLine.split("\\|");
                    String theLang;
                    langCodeProps.setProperty(fields[0], fields[3]);
                    if (fields[1].length() == 3) {
                        theLang = fields[1];
                        langCodeProps.setProperty(theLang, fields[3]);
                        langFixMap.put(fields[0],theLang);
                    }
                    else {
                        theLang = fields[0];
                    }
                    langNameMap.put(fields[3],theLang);
                    if (fields[2].length() == 2)
                        shortLangMap.put(theLang,fields[2]);
                }
            }
            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
//        langNameMap=new TreeMap();
//        Set entries = langCodeProps.entrySet();
//        Iterator it = entries.iterator();
//        while(it.hasNext()) {
//            Map.Entry entry = (Map.Entry)it.next();
//            langNameMap.put(entry.getValue(),entry.getKey());
//        }
        Set entries = langNameMap.keySet();
        Iterator it = entries.iterator();
        langNames = new String[langNameMap.size()];
        int ix=0;
        while(it.hasNext()) {
            langNames[ix++]=(String)it.next();
        }
        String windowStyle = 
            libraryProperties.getProperty("window-style");
        if ("compact".equals(windowStyle)) 
            compact=true;
        
        String systemFileSep = System.getProperties().getProperty("file.separator");
        String temp = libraryProperties.getProperty("unix-path-translate");
        if ("Y".equals(temp) && "\\".equals(systemFileSep))
            unixPathTranslate=true;
        
        temp = libraryProperties.getProperty("filename-case-sensitive");
        if ("Y".equals(temp))
            filenameCaseSensitive=true;
        else if (!"N".equals(temp) && "/".equals(systemFileSep))
            filenameCaseSensitive=true;
        
    }
    
    
    int findColumnNum(int seekColType, String seekColTag) {
        int col = -1;
        int ix;
        for (ix=0;ix<colType.length;ix++) {
            if (colType[ix]==seekColType) {
                if (seekColTag==null || seekColTag.equals(colId[ix])) {
                    col=ix;
                    break;
                }
            }
        }
        return col;
    }
    
    
    void loadLibraryCols() throws Exception {
        String libraryCols=libraryProperties.getProperty("library-cols");
        int i;
        int ix=-1;
        int commaCount=0;
        for (i=0;;i++) {
            ix=libraryCols.indexOf(",",ix+1);
            if (ix==-1)
                break;
            commaCount++;
        }
            
        numLibraryCol = (commaCount+1) / 4;
        if (numLibraryCol * 4 != commaCount + 1) 
            throw new JampalException("Wrong number of commas in library-cols:"+commaCount);

        colType = new int[numLibraryCol];
        colId = new String[numLibraryCol];
        colHeading = new String[numLibraryCol];
        // Indexes into above arrays for sequence of display columns
        displayCol = new int[numLibraryCol];

        StringTokenizer tok = new StringTokenizer(libraryCols,",",true);
        ix=0;
        numDisplayCol=0;
        int maxDisplayCol=0;
        for (ix=0;tok.hasMoreTokens();ix++) {
            String type = tok.nextToken();
            if (",".equals(type)) {
                throw new JampalException("Missing type in library-cols entry:"+(ix+1));
            }
            else {
                String typeNumber = FrameDictionary.getSingleton().frameProperties.getProperty(type);
                if (typeNumber==null)
                    throw new JampalException("Invalid column type:"+type);
                int typeInt = Integer.parseInt(typeNumber);
                colType[ix]=typeInt;
                tok.nextToken(); //get the comma at end of type
            }
            String tag = tok.nextToken();
            if (",".equals(tag)) {
                colId[ix]="";
            }
            else {
                colId[ix]=tag;
                tok.nextToken(); //get the comma at end of tag
            }
            String heading = tok.nextToken();
            if (",".equals(heading)) {
                colHeading[ix]="";
            }
            else {
                colHeading[ix]=heading;
                tok.nextToken(); //get the comma at end of heading
            }
            String display = tok.nextToken();
            if (",".equals(display)) {
            }
            else {
                int displayInt = Integer.parseInt(display);
                int displayIx = displayInt-1;
                if (displayIx > -1) {
                    if (displayCol[displayIx] != 0)
                        throw new JampalException("Duplicate display col num in  library-cols entry:"+displayInt);
                    displayCol[displayIx]=ix;
                    numDisplayCol++;
                    if (displayInt > maxDisplayCol)
                        maxDisplayCol=displayInt;
                }
                if (tok.hasMoreTokens())
                    tok.nextToken(); //get the comma at end of display
            }
        }
        if (maxDisplayCol!=numDisplayCol)
            throw new JampalException("Gaps in col num in library-cols entry");

        for (i=0;i<numLibraryCol;i++) {
            if (colType[i]==MP3File.COL_FILENAME) {
                fileNameCol=i;
                break;
            }
        }
        if (fileNameCol==-1)
            throw new JampalException("File Names are not stored on library");

    }
    static Pattern pathPattern = Pattern.compile("\\\\");
    static Pattern drivePattern = Pattern.compile("^[a-zA-Z0-9]:\\\\");
    static char defaultDrive = getDefaultDrive();
    
    static char getDefaultDrive() {
        String defaultDir = System.getProperty("user.dir");
        if (defaultDir.length() < 3 || defaultDir.charAt(1) != ':'
                || defaultDir.charAt(2) != '\\') 
            return 0;
        return Character.toLowerCase(defaultDir.charAt(0));
    }

    String translateFileName(String fileName) throws JampalException {
        if (unixPathTranslate && defaultDrive != 0) {
            // translating paths to unix format
            Matcher driveMatcher = drivePattern.matcher(fileName);
            if (driveMatcher.find()) {
                char drive = Character.toLowerCase(fileName.charAt(0));
                if (drive != defaultDrive)
                    throw new JampalException("Unix style paths: you cannot add files with drive letters other " +
                            "than "+defaultDrive +":");
            }
            fileName=driveMatcher.replaceFirst("/");
            Matcher pathMatcher = pathPattern.matcher(fileName);
            fileName = pathMatcher.replaceAll("/");
            if (!fileName.startsWith("/"))
                throw new JampalException("Problem with translating file name to unix format");
        }
        if (!unixPathTranslate && defaultDrive != 0 && fileName.startsWith("/")) {
            // translate to DOS format
            File file = new File(fileName);
            fileName = file.getAbsolutePath();
        }
        return fileName;
    }

    String fixFileNameCase(String fileName) {
        if (!filenameCaseSensitive)
            fileName=fileName.toLowerCase();
        return fileName;
    }    

    String normalizeFileName(String fileName) throws JampalException{
        fileName=translateFileName(fileName);
        fileName=fixFileNameCase(fileName);
        return fileName;
    }
    

    String normalizeFileName(File file) throws JampalException {
        String fileName = file.getAbsolutePath();
        fileName=translateFileName(fileName);
        fileName=fixFileNameCase(fileName);
        return fileName;
    }
    
    
    
    void loadTagEditorCols() throws Exception {
        String tagEditCols=libraryProperties.getProperty("tag-editor");
        int i;
        int ix=-1;
        int commaCount=0;
        for (i=0;;i++) {
            ix=tagEditCols.indexOf(",",ix+1);
            if (ix==-1)
                break;
            commaCount++;
        }
            
        numTagEditCol = (commaCount+1) / 3;
        if (numTagEditCol!= 0 && numTagEditCol * 3 != commaCount + 1) 
            throw new JampalException("Wrong number of commas in tag-editor:"+commaCount);

        editColType = new int[numTagEditCol];
        editColId = new String[numTagEditCol];
        editColHeading = new String[numTagEditCol];

        StringTokenizer tok = new StringTokenizer(tagEditCols,",",true);
        ix=0;
        int maxDisplayCol=0;
        for (ix=0;tok.hasMoreTokens();ix++) {
            String type = tok.nextToken();
            if (",".equals(type)) {
                throw new JampalException("Missing type in tag-editor entry:"+(ix+1));
            }
            else {
                String typeNumber = FrameDictionary.getSingleton().frameProperties.getProperty(type);
                if (typeNumber==null)
                    throw new JampalException("Invalid column type in tag-editor entry:"+type);
                int typeInt = Integer.parseInt(typeNumber);
                editColType[ix]=typeInt;
                tok.nextToken(); //get the comma at end of type
            }
            String tag = tok.nextToken();
            if (",".equals(tag)) {
                editColId[ix]="";
            }
            else {
                editColId[ix]=tag;
                tok.nextToken(); //get the comma at end of tag
            }
            String heading="";
            if (tok.hasMoreTokens())
                heading = tok.nextToken();
            if (",".equals(heading)) {
                editColHeading[ix]="";
            }
            else {
                editColHeading[ix]=heading;
                if (tok.hasMoreTokens())
                    tok.nextToken(); //get the comma at end of heading
            }
        }

    }

}

