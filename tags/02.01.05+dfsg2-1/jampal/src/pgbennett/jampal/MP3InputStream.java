package pgbennett.jampal;
import java.io.*;
import pgbennett.id3.*;


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


public class MP3InputStream extends InputStream {

    byte [] buffer = new byte[128000];
    File playFile;
    /** Location of first frame */    
    long startLocation;
    long endLocation;
    /** Location where we start playing */    
    long beginPlayLocation;
    long currentLocation;
    /** Location at the end of the first frame */    
    long lastModified;
    int buffPointer=0;
    int buffUsed=0;
    MP3File mp3;
    MPEGAudioFrameHeader mainHeader;
    String throwException;
    boolean isEOF=false;

    MP3InputStream(String fileName) {
        playFile = new File(fileName);
    }

    // position is a number from 0 to 10000 for a starting place
    // set to -1 for an exception resume
    int setupFile(int position) throws Exception {
        isEOF=false;
        if (position < 0)
            // This for an exception condition
            beginPlayLocation = currentLocation - (buffUsed - buffPointer);
        lastModified = playFile.lastModified();
        mp3 = new MP3File();
        try {
            mp3.init(playFile,MP3File.BOTH_TAGS);
        }
        catch (ID3Exception ex) {
            ex.printStackTrace();
        }
        mainHeader = new MPEGAudioFrameHeader(playFile);
        startLocation = mainHeader.getLocation();
        currentLocation=startLocation;
        if (position == 0)
            beginPlayLocation=startLocation;
        endLocation = playFile.length();
        if (mp3.id3v1Exists())
            endLocation-=128;
        if (position > 0) 
            beginPlayLocation = startLocation + (endLocation-startLocation)*position/10000;
        if (beginPlayLocation < startLocation)
            beginPlayLocation = startLocation;
        // If near the end of the file go right to the end
        if (endLocation - beginPlayLocation  < mainHeader.getFrameLength() )  // was 16000
            beginPlayLocation = endLocation;
        else {
            MPEGAudioFrameHeader header = new MPEGAudioFrameHeader(playFile, beginPlayLocation);
            beginPlayLocation = header.getLocation();
        }
        buffPointer=0;
        buffUsed=0;
        position = (int)((beginPlayLocation - startLocation) *10000 / (endLocation - startLocation));
        return position;
    }

    
    
    RandomAccessFile setupRaf() throws IOException {
        RandomAccessFile raf=null;
        long newLastModified = playFile.lastModified();
        if (newLastModified != lastModified) {
            try {
                MPEGAudioFrameHeader header = new MPEGAudioFrameHeader(playFile);
                long newLocation = header.getLocation();
                currentLocation += (newLocation - startLocation);
                beginPlayLocation += (newLocation - startLocation);
                endLocation += (newLocation - startLocation);
                markPoint += (newLocation - startLocation);
                startLocation = newLocation;
                lastModified = newLastModified;
            }
            catch (Exception ex) {
                throw new IOException(ex.toString());
            }
        }
        raf=new RandomAccessFile(playFile,"r");
        return raf;

    }

    public int getPosition() {
        return (int)(10000 * (currentLocation - startLocation) 
            / (endLocation - startLocation));
    }


    // InputStream
    public int read() throws IOException {
        byte[] in = new byte[1];
        return read(in,0,1);
    }

    public int read(byte[] b) throws IOException {
        return read(b,0,b.length);
    }

    static boolean dumped=false;
    
    public int read(byte[] b, int off, int len)
    throws IOException {
        if (throwException!=null) {
            RuntimeException ioe = new RuntimeException(throwException);
            throwException=null;
            ioe.printStackTrace();
            throw ioe;
        }
            
        if (!dumped) {
            dumped=true;
            Throwable throwable = new Throwable("Trace to show which mp3 decoder is being used.");
            throwable.printStackTrace();
        }
        
        RandomAccessFile raf=null;
        int ret=0;
        int rafRet=0;
        try {
            while (len>0 && rafRet!=-1) {
                int avail = buffUsed - buffPointer;
                if (avail > len)
                    avail = len;
                if (avail > 0) {
                    System.arraycopy(buffer,buffPointer,b,off,avail);
                    off += avail;
                    len -= avail;
                    buffPointer += avail;
                    ret += avail;
                }
                if (buffPointer >= buffUsed) {
                    if (raf==null)
                        raf=setupRaf();
                    long maxLength = buffer.length;
                    if (currentLocation < beginPlayLocation) {
                        maxLength = startLocation + mainHeader.getFrameLength()
                            - currentLocation;
                        if (maxLength <= 0) {
                            maxLength = buffer.length;
                            currentLocation = beginPlayLocation;
                        }
                    }
                    raf.seek(currentLocation);
                    buffPointer=0;
                    buffUsed=0;
                    if (currentLocation+maxLength > endLocation)
                        maxLength = endLocation - currentLocation;
                    if (maxLength <= 0)
                        rafRet=-1;
                    else {
                        rafRet=raf.read(buffer,0,(int)maxLength);
                        currentLocation=raf.getFilePointer();
                    }
                    if (rafRet>0)
                        buffUsed=rafRet;
                }
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
            ret = -1;
        }
        finally {
            if (raf!=null)
                raf.close();
        }
        if (ret==0 && rafRet == -1)
            ret = -1;
        isEOF=(ret==-1);
        
        return ret;
    }
    long markPoint;
    public void mark(int readlimit) {
        markPoint = currentLocation - (buffUsed - buffPointer);
    }
    

    public void reset()
           throws IOException {
               
        buffUsed=0;
        buffPointer=0;
        currentLocation=markPoint;
    }

    public boolean markSupported() {
        return true;
    }
}