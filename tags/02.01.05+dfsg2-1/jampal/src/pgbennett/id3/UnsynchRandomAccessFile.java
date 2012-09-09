package pgbennett.id3;


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


/*
 * Random AccessFile for use with unsynchronized data
 *
 * Created on April 28, 2004, 6:07 PM
 */

import java.io.*;

/**
 *
 * @author  u602580
 */
public class UnsynchRandomAccessFile extends RandomAccessFile {
    boolean isUnsynch;
    boolean ffound;
    InputStream alternativeInput = null;
    
    public UnsynchRandomAccessFile(File file,String mode)
    throws FileNotFoundException {
        super(file,mode);
    }
    public UnsynchRandomAccessFile(String file,String mode)
    throws FileNotFoundException {
        super(file,mode);
    }
    public void setUnsynch(boolean isUnsynch) {
        this.isUnsynch = isUnsynch;
        ffound=false;
    }
    public void setAlternative(InputStream in){
        alternativeInput = in;
    }

    public InputStream getAlternative(){
        return alternativeInput;
    }

    
    public int read(byte[] b) throws IOException {
        return read(b,0, b.length);
    }
    
    public int read(byte[] b, int off,int len)
         throws IOException {
         if (alternativeInput!=null) {
             int count=alternativeInput.read(b,off,len);
             if (count>0 || len==0)
                return 0;
             alternativeInput.close();
             alternativeInput=null;
         }
         if (!isUnsynch) 
             return super.read(b,off,len);
         int bytesRead=0;
         int remain = len;
         int ixIn=off;
         int ixOut=off;
         int ixEnd=off+len;
         int ret=0;
         while (remain > 0) {
             ixIn=ixOut;
             ret=super.read(b,ixIn,remain);
             if (ret > 0)
                 bytesRead+=ret;
             else
                 break;
             for (;remain>0 && ixIn<ixEnd;ixIn++) {
                 b[ixOut]=b[ixIn];
                 if (!ffound || b[ixIn]!=0) {
                     ixOut++;
                     remain--;
                 }
                 if (b[ixIn] == -1)
                     ffound=true;
                 else
                     ffound=false;
             }
         }
       return bytesRead;    
    }
    
    
}
