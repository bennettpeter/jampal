/*
 * TagUpdate.java
 *
 * Created on December 29, 2005, 4:48 PM
 *
 */

/*
    Copyright 2006 Peter Bennett

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


package pgbennett.id3;

/**
 * Parameters to main:
 * 
 * -DISPLAY
 * -ID3V2TAG followed by frame-id and value
 * -TITLE    followed by value
 * -ARTIST   followed by value
 * -ALBUM    followed by value
 * -YEAR     followed by value
 * -COMMENT  followed by value
 * -TRACK    followed by value
 * -GENRE    followed by value
 * -SYNCRONIZE
 * If the -DISPLAY option is used no other options are allowed. With the other
 * options any number may be specified. ID3V2TAG gets a frame id and value,
 * the others just get a value.
 * The value can be @filename - in that case the value is read from a file.
 * The @filename is the only way to update a picture
 * Following these parameters can be one or more filenames of mp3 files.
 * Frame-ids are a 4 character frame name, 
 *
 * @author peter
 */

import java.util.*;
import java.io.*;

public class TagUpdate {
    
    
    
    public static void main(String[] args) throws Exception {
        
        int ix;
        TagUpdate tagUpdate = new TagUpdate();
        ix=tagUpdate.processCommandLine(args);
        if (!tagUpdate.error && ix < args.length)
            tagUpdate.updateFiles(args,ix);
        if (tagUpdate.optionFile!=null) {

            FileInputStream fileInputStream = new  FileInputStream(tagUpdate.optionFile);
            InputStreamReader inputStreamReader;
            if (tagUpdate.optionFileEncoding == null)
                inputStreamReader = new InputStreamReader(fileInputStream);
            else
                inputStreamReader = new InputStreamReader(fileInputStream, tagUpdate.optionFileEncoding);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            
           int lineNo = 0;
            for (;;) {
                String inLine = bufferedReader.readLine();
                if (inLine==null)
                    break;
                if (inLine.length() > 0 && inLine.charAt(0) == 0xfeff)
                    inLine=inLine.substring(1);
                lineNo++;
                System.out.println(inLine);
                try {
                    StringTokenizer tok = new StringTokenizer(inLine," \"",true);
                    Vector paramLineVec = new Vector<String>();
                    while (tok.hasMoreTokens()) {
                        String next = tok.nextToken(" \"");
                        if ("\"".equals(next)) {
                            next = tok.nextToken("\"");
                            if ("\"".equals(next))
                                next="";
                            else
                                tok.nextToken("\"");
                        }
                        else if (" ".equals(next)) {
                            continue;
                        }
                        paramLineVec.add(next);
                    }
                    if (paramLineVec.size()>0) {
                        TagUpdate lineUpdate = new TagUpdate();
                        String [] lineArgs = new String[paramLineVec.size()];
                        lineArgs = (String[])paramLineVec.toArray(lineArgs);
                        ix=lineUpdate.processCommandLine(lineArgs);
                        if (!lineUpdate.error && ix < lineArgs.length)
                            lineUpdate.updateFiles(lineArgs,ix);
                    }
                }
                catch(NoSuchElementException ex) {
                    System.err.println("ERROR - Invalid quoting on input line " +lineNo);
                }
            }            
            
        }
    }    

    
    class Parameter {
        String optionStr;
        int fieldType;
        boolean doSynchronize;
        boolean doDisplay;
        boolean doShortDisplay;
        boolean noUpdate;
        String extendedId="";
        String value;
        ID3v2Picture picture;
        byte [] binaryData;
    }
    Vector paramVec;
    boolean error = false;
    String optionFile = null; 
    String optionFileEncoding = null;
    
    int processCommandLine(String[] args)  {
        int ix=0;
        paramVec=new Vector();
        if (args.length < 1) {
            printOptions();
            return ix;
        }
        parseloop:
        for (ix=0; ix<args.length && !error; ix++) {
            if (!args[ix].startsWith("-"))
                break;
            String optionStr = args[ix].substring(1);
            if ("OPTIONFILE".equals(optionStr)) {
                if (optionFile!=null) {
                    error=true;
                    System.err.println("ERROR - Duplicate OPTIONFILE parameter.");
                    break parseloop;
                }
                if (args.length > ++ix)
                    optionFile = args[ix];
                if (optionFile==null) {
                    error=true;
                    System.err.println("ERROR - Missing file name for Option File.");
                    break parseloop;
                }
                continue;
            }
            if ("ENCODING".equals(optionStr)) {
                if (optionFileEncoding!=null) {
                    error=true;
                    System.err.println("ERROR - Duplicate ENCODING parameter.");
                    break parseloop;
                }
                if (args.length > ++ix)
                    optionFileEncoding = args[ix];
                if (optionFileEncoding==null) {
                    error=true;
                    System.err.println("ERROR - Missing file encoding value.");
                    break parseloop;
                }
                continue;
            }
            Parameter parameter = new Parameter();
            paramVec.add(parameter);
            parameter.optionStr=args[ix].substring(1);
            if ("SYNCHRONIZE".equals(parameter.optionStr)) {
                parameter.doSynchronize = true;
                continue;
            }
            if ("DISPLAY".equals(parameter.optionStr)) {
                parameter.doDisplay=true;
                continue;
            }
            if ("SHORTDISPLAY".equals(parameter.optionStr)) {
                parameter.doShortDisplay=true;
                continue;
            }
            if ("NOUPDATE".equals(parameter.optionStr)) {
                parameter.noUpdate=true;
                continue;
            }
            
            String optionVal = FrameDictionary.getSingleton().frameProperties.getProperty(parameter.optionStr);
            try {
                parameter.fieldType = Integer.parseInt(optionVal);
            }
            catch (Exception ex) {
                parameter.fieldType = -1;
            }
            FrameAttributes att = null;
            switch (parameter.fieldType) {
                case MP3File.COL_ID3V2TAG:
                    if (args.length > ++ix)
                        parameter.extendedId = args[ix];
                    if (parameter.extendedId==null) {
                        error=true;
                        System.err.println("ERROR - Missing ID3V2 frame at end of options");
                        break parseloop;
                    }
                    if (parameter.extendedId.length() >= 4)
                        att = (FrameAttributes)
                            FrameDictionary.getSingleton().frameTypes.get
                            (parameter.extendedId.substring(0,4));
                    if (att==null) {
                        error=true;
                        System.err.println("ERROR - Invalid ID3V2 frame: "+parameter.extendedId);
                        break parseloop;
                    }
                    int pos=4;
                    if (att.langReq) {
                        String lang="";
                        if (parameter.extendedId.length() >= pos+3) {
                            lang = parameter.extendedId.substring(pos, pos+3);
                            pos+=3;
                        }
                        if (!lang.matches("[a-z][a-z][a-z]")) {
                            error=true;
                            System.err.println("ERROR - Language code must be 3 lowercase letters: "+parameter.extendedId);
                            break parseloop;
                        }
                    }
                    if (att.type=='I') {
                        String picType="";
                        if (parameter.extendedId.length() >= pos+2) {
                            picType = parameter.extendedId.substring(pos, pos+2);
                            pos+=2;
                        }
                        if (!picType.matches("[0-9a-f][0-9a-f]")) {
                            error=true;
                            System.err.println("ERROR - Picture Type code must be 2 hex digits: "+parameter.extendedId);
                            break parseloop;
                        }
                        if (args.length > ix+1)
                            parameter.value = args[ix+1];
                        if (parameter.value != null 
                                && parameter.value.length()>0 && !parameter.value.startsWith("@")) {
                            error=true;
                            System.err.println("ERROR - To add a picture you must use a file name "+
                                    "preceded by an @ sign: "+parameter.extendedId);
                            break parseloop;
                        }
                        try {
                            parameter.picture = loadPicture(parameter.value);
                        }
                        catch (IOException io) {
                            error=true;
                            System.err.println("ERROR - Cannot read picture file: "+
                                    parameter.extendedId);
                            System.err.println(io.toString());
                            break parseloop;
                        }
                    }
                    if (!att.descReq && att.type!='I') {
                        if (parameter.extendedId.length() > pos) {
                            error=true;
                            System.err.println("ERROR - This ID3V2 frame does not require a description: "+parameter.extendedId);
                            break parseloop;
                        }                        
                    }
                    // fall through to other cases
                case MP3File.COL_TITLE:
                case MP3File.COL_ARTIST:
                case MP3File.COL_ALBUM:
                case MP3File.COL_YEAR:
                case MP3File.COL_COMMENT:
                case MP3File.COL_TRACK:
                case MP3File.COL_GENRE:
                    if (args.length > ++ix)
                        parameter.value = args[ix];
                    if (parameter.value==null) {
                        error=true;
                        System.err.println("ERROR - Missing value for parameter: "+parameter.optionStr);
                        break parseloop;
                    }
                    if (parameter.value.startsWith("@") && parameter.picture==null) {
                        try {
                            String imageFileName = parameter.value.substring(1);
                            File imageFil = new File(imageFileName);
                            FileInputStream in = new FileInputStream(imageFil);
                            long length = imageFil.length();
                            parameter.binaryData = new byte [(int)length];
                            in.read(parameter.binaryData);
                            in.close();
                            if (att!=null && att.type != 'B')
                                parameter.value=new String(parameter.binaryData);
                        }
                        catch (IOException io) {
                            error=true;
                            System.err.println("ERROR - Cannot read file: "
                                    +parameter.optionStr +" "+ parameter.extendedId);
                            System.err.println(io.toString());
                            break parseloop;
                        }
                    }
                    if (att==null || att.type=='T' || att.type=='N') {
                        if (parameter.value.indexOf('\n')!= -1) {
                            error=true;
                            System.err.println("ERROR Multi-line not allowed: "
                                    +parameter.optionStr +" "+ parameter.extendedId);
                            break parseloop;
                        }
                    }
                    if (att!=null && att.type=='L') {
                        if (!parameter.value.matches("[a-z][a-z][a-z]")) {
                            error=true;
                            System.err.println("ERROR - Language must be 3 lowercase letters: "
                                    +parameter.optionStr +" "+ parameter.extendedId);
                            break parseloop;
                        }
                    }                    
                    if (att!=null && att.type=='B' && parameter.value.length()>0) {
                        error=true;
                        System.err.println("ERROR - Updating Binary Fields is not supported: "
                                +parameter.optionStr +" "+ parameter.extendedId);
                        break parseloop;
                    }     
                    
                    if (parameter.extendedId.indexOf('\n')!= -1) {
                        error=true;
                        System.err.println("ERROR - Multi-line Description is not allowed: "
                                +parameter.optionStr +" "+ parameter.extendedId);
                        break parseloop;
                    }
                    break;
                default:
                    error=true;
                    System.err.println("ERROR - Invalid parameter -"+parameter.optionStr);
                    break parseloop;
            }
            
        }
        if (ix == args.length && paramVec.size()>0) {
            error=true;
            System.err.println("ERROR - No file names specified");
        }
//        if (error)
//            printOptions();
        return ix;
    }

    ID3v2Picture loadPicture(String value) throws IOException {
        ID3v2Picture picture = new ID3v2Picture();
        if (value.startsWith("@")) {
            String imageFileName = value.substring(1);
            File imageFil = new File(imageFileName);
            FileInputStream in = new FileInputStream(imageFil);
            long length = imageFil.length();
            picture.pictureData = new byte [(int)length];
            in.read(picture.pictureData);
            in.close();
            String ext = null;
            String s = imageFil.getName();
            int i = s.lastIndexOf('.');
            if (i > 0 &&  i < s.length() - 1) 
                ext = s.substring(i+1).toLowerCase();
            if ("jpg".equalsIgnoreCase(ext)||"jpe".equalsIgnoreCase(ext))
                ext="jpeg";
            picture.mimeType="image/"+ext;
        }
        return picture;
    }
    
    
    void updateFiles(String [] args,int ix) {
        String fileName=null;
        int successCount=0;
        int failureCount=0;
        boolean updatesFound=false;
        boolean noUpdate=false;
        for (; ix<args.length; ix++) {
            try {
                fileName = args[ix];
                File file = new File(fileName);
                MP3File mp3 = new MP3File();
                mp3.init(file,MP3File.BOTH_TAGS);
                int iy;
                for (iy=0; iy<paramVec.size(); iy++) {
                    Parameter param = (Parameter)paramVec.get(iy);
                    if (param.doDisplay) {
                        System.out.println(mp3);
                        continue;
                    }
                    if (param.doShortDisplay) {
                        String title=mp3.getMp3Field(mp3.COL_TITLE,null);
                        String artist=mp3.getMp3Field(mp3.COL_ARTIST,null);
                        String album=mp3.getMp3Field(mp3.COL_ALBUM,null);
                        System.out.println(title+"\t"+artist+"\t"+album);
                        continue;
                    }
                    if (param.doSynchronize ) {
                        mp3.syncV1AndV2();
                        continue;
                    }
                    if (param.noUpdate) {
                        noUpdate=true;
                        continue;
                    }
                    fieldType:
                    switch (param.fieldType) {
                        case MP3File.COL_ID3V2TAG:
                            FrameAttributes att = null;
                            att = (FrameAttributes)
                                FrameDictionary.getSingleton().frameTypes.get
                                (param.extendedId.substring(0,4));
                            switch(att.type) {
                                case 'I':
                                    param.picture.Description=param.extendedId.substring(6);
                                    param.picture.pictureType = (byte)Integer.parseInt(param.extendedId.substring(4,6),16);
                                    if (param.picture.pictureData==null)
                                        param.picture.pictureData = new byte[0];
                                    mp3.setMp3Picture(param.extendedId, param.picture);
                                    updatesFound=true;
                                    break fieldType;
                                case 'N':
                                    mp3.setMp3Integer(param.extendedId, param.value.trim());
                                    updatesFound=true;
                                    break fieldType;
                                //case 'B':
                                //    if (param.binaryData==null)
                                //        param.binaryData=param.value.trim().getBytes("UTF-16");
                                //    mp3.setFrameData(param.extendedId.substring(0,4), param.binaryData);
                                //    updatesFound=true;
                                //    break fieldType;
                            }
                            // Fall Through to default
                        default:
                            mp3.setMp3Field(param.fieldType, param.extendedId, 
                                 null, null, param.value.trim());
                            updatesFound=true;
                                    
                    }
                    
                }
                
                if (updatesFound && !noUpdate) {
                    mp3.writeTags();
                    System.out.println("SUCCESSFULLY UPDATED: "+fileName);
                    successCount++;
                }
            }
            catch (Exception ex) {
                System.err.println("ERROR: Failure updating tags for "+fileName);
                System.err.println(ex.toString());
                failureCount++;
            }
        }
        System.out.println("Number of files successfully updated: "+successCount);
        if (failureCount>0)
            System.out.println("Number of files not updated: "+failureCount);
        
    }
    
    void printOptions() {
        System.err.println("TagUpdate Version 1.17 (c) 2006 - 2008 Peter G Bennett");
        System.err.println("Usage: java -cp [path]/jampal.jar pgbennett.id3.TagUpdate options filename [filename] ...");
        System.err.println("Options are as follows:");
        System.err.println("-OPTIONFILE filename");
        System.err.println("This supplies a file name from which options are read after all options");
        System.err.println("on the command line are exhausted.");
        System.err.println("-ENCODING encoding");
        System.err.println("This supplies encoding for the option file.");
        System.err.println("-DISPLAY");
        System.err.println(" This displays the tags and file information. If it is specified before");
        System.err.println(" update options it displays values before the update. If it is specified after");
        System.err.println(" or between update options it displays the value after the updates");
        System.err.println(" that appear before it.");
        System.err.println("-SHORTDISPLAY");
        System.err.println(" This displays the title, artist and album, tab separated. If it is specified before");
        System.err.println(" update options it displays values before the update. If it is specified after");
        System.err.println(" or between update options it displays the value after the updates");
        System.err.println(" that appear before it.");
        System.err.println("-NOUPDATE");
        System.err.println(" This suppresses writing the updates to the file. It can be specified anywhere");
        System.err.println(" in the options list.");
        System.err.println("update-option");
        System.err.println(" This updates tags.");
        System.err.println(" update-option includes one or more of the following options:");
        System.err.println(" (1) Options that update both ID3V1 and ID3V2 tags.");
        System.err.println("  -TITLE title");
        System.err.println("  -ARTIST artist-name");
        System.err.println("  -ALBUM album-name");
        System.err.println("  -YEAR year");
        System.err.println("  -COMMENT comment");
        System.err.println("  -TRACK track-number");
        System.err.println("  -GENRE genre-name");
        System.err.println(" (2) Options that update the ID3V2 tag only");
        System.err.println("  -ID3V2TAG frame-id value");
        System.err.println("   frame-id is one of the following");
        System.err.println("    XXXX for frames that take no language or description");
        System.err.println("    XXXXlll for frames that take a language");
        System.err.println("    XXXXllldddddd for frames that take a language and a description");
        System.err.println("    XXXXdddddd for frames that take only a description");
        System.err.println("    APICxxddddd for picture frames");
        System.err.println("   language code is always exactly 3 lower-case letters");
        System.err.println("   picture type is two hexadecimal digits");
        System.err.println("   description can be any length");
        System.err.println("   data can be a string or @filename to read from a file");
        System.err.println("   For pictures @filename must be used and the extension of the file must");
        System.err.println("    be a recognized image file type (jpg, jpe, jpeg, bmp, png, gif)");
        System.err.println(" (3) Option that synchronizes tags");
        System.err.println("  -SYNCHRONIZE");
        System.err.println("   This copies values from id3v2 tags to id3v1 if they are present in id3v2");
        System.err.println("    otherwise copies id3v1 values to id3v2.");
        System.err.println("    To synchronize updates performed in the same run this should be at the");
        System.err.println("    end of the options list. It is performed in sequence with the other operations.");
        System.err.println(" If any descriptions or values include spaces or newlines they can be quoted");
        System.err.println(" If any values are specified as empty (i.e. two quotes with nothing between,");
        System.err.println("  the frame is deleted from the tag");
        System.err.println("");
        System.err.println("When updating files, any existing frames not explicitly updated are left unchanged.");
        System.err.println("Many files can be updated at once, to update selected values on each file.");
        System.err.println("");
    }
    
}
