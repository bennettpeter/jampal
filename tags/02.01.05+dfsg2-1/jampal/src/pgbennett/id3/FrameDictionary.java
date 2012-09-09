/*
    Copyright 2005 Peter Bennett

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
import java.util.*;
import java.io.*;

/**
 *
 * @author peter
 */
public class FrameDictionary {

    public Properties frameProperties;
    
    // each entry of this is frameAttributes 
    // (loaded from frame-types in properties file
    // Mapping from frame id to attributes
    public HashMap <String,FrameAttributes> frameTypes;
    // Mapping from frame heading to attributes
    public TreeMap frameNameMap;
    public String[] frameNames;
    
    static FrameDictionary singleton;
    
    public static FrameDictionary getSingleton() {
        if (singleton==null)
            try {
                singleton=new FrameDictionary();
            }
            catch(Exception ex) {
                ex.printStackTrace();
                singleton=null;
            }
        return singleton;
            
    }
    
    /** Creates a new instance of FrameDictionary */
    public FrameDictionary() throws Exception {
        loadFrameList();
    }

    void loadFrameList() throws Exception {
        frameProperties = new Properties();
        InputStream stream;
        stream = ClassLoader.getSystemResourceAsStream(("pgbennett/id3/frame.properties"));
        try {
            frameProperties.load(stream);
        }
        finally {
            stream.close();
        }
        String frameList=frameProperties.getProperty("frame-list");
        int i;
        int ix=-1;
        int commaCount=0;
        for (i=0;;i++) {
            ix=frameList.indexOf(",",ix+1);
            if (ix==-1)
                break;
            commaCount++;
        }
            
        int numFrameList = (commaCount+1) / 5;
        if (numFrameList * 5 != commaCount + 1) 
            throw new ID3Exception("Wrong number of commas in frame-list:"+commaCount);

        StringTokenizer tok = new StringTokenizer(frameList,",",true);
        frameTypes=new HashMap();
        frameNameMap=new TreeMap();
        ix=0;
        String s;
        int maxDisplayCol=0;
        for (ix=0;tok.hasMoreTokens();ix++) {
            FrameAttributes frameAtt = new FrameAttributes();
            // Frame ID
            s = tok.nextToken();
            if (",".equals(s)) {
                throw new ID3Exception("Missing Id in frame-list entry:"+(ix+1));
            }
            else {
                frameAtt.id=s;
                tok.nextToken(); //get the comma at end of type
            }
            // Language Reqd Indicator
            s = tok.nextToken();
            frameAtt.langReq=false;
            if ("Y".equals(s)) 
                frameAtt.langReq=true;
            if (!",".equals(s)) {
                tok.nextToken(); //get the comma at end of field
            }
            // Desc Reqd Indicator
            s = tok.nextToken();
            frameAtt.descReq=false;
            if ("Y".equals(s)) 
                frameAtt.descReq=true;
            if (!",".equals(s)) {
                tok.nextToken(); //get the comma at end of field
            }
            // Type
            s = tok.nextToken();
            if (",".equals(s)) {
                throw new ID3Exception("Missing Type in tag-editor entry:"+(ix+1));
            }
            else {
                frameAtt.type=s.charAt(0);
                tok.nextToken(); //get the comma at end of heading
            }
            // Heading
            s = tok.nextToken();
            if (",".equals(s)) {
                throw new ID3Exception("Missing Heading in tag-editor entry:"+(ix+1));
            }
            else {
                frameAtt.colHeading=s;
                if (tok.hasMoreTokens())
                    tok.nextToken(); //get the comma at end of heading
            }
            frameTypes.put(frameAtt.id,frameAtt);
            frameNameMap.put(frameAtt.colHeading,frameAtt);
        }
        Set entries = frameNameMap.keySet();
        Iterator it = entries.iterator();
        frameNames = new String[frameNameMap.size()];
        ix=0;
        while(it.hasNext()) {
            frameNames[ix++]=(String)it.next();
        }
    }    
    
}
