package pgbennett.id3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.util.*;

/*
   Copyright (C) 2001,2002 Jonathan Hilliker
    This file is part of the jd3lib library.

    This copy of jd3lib has been incorporated into Jampal under the 
    GNU general Public License.

    Modifications to the file Copyright 2004 Peter Bennett.

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
 *
 *   Modified By Peter Bennett <pgbennett@users.sourceforge.net>,
 *   April 2004 and incorporated into Jampal.
 */
/**
 * Description: This class reads and writes id3v2 tags from a file.<br />
 *
 * <dl>
 *   <dt> <b>Version History</b> </dt>
 *   <dt> 1.11.1 - <small>2002.1023 by gruni</small> </dt>
 *   <dd> -Made Sourcecode compliant to the Sun CodingConventions</dd>
 *   <dt> 1.11 - <small>2002.0127 by helliker</small> </dt>
 *   <dd> -The getBytes method is public to adhere to the ID3Tag interface.</dd>
 *
 *   <dt> 1.10 - <small>2002.0125 by helliker</small> </dt>
 *   <dd> -Uses RandomAccessFile.readInt method to read ints.</dd>
 *   <dt> 1.9 - <small>2002.0125 by helliker</small> </dt>
 *   <dd> -Writes tag correctly if the 1st mpeg frame is corrupt.</dd>
 *   <dd> -Fixed infinite loop in padding calculation.</dd>
 *   <dt> 1.8 - <small>2002.0124 by helliker</small> </dt>
 *   <dd> -Throws an exception with an invalid frame size.</dd>
 *   <dt> 1.7 - <small>2002.0113 by helliker</small> </dt>
 *   <dd> -Implements new ID3Tag interface.</dd>
 *   <dd> -Optimized new padding calculation for writing.</dd>
 *   <dt> 1.6 - <small>2001.1204 by helliker</small> </dd>
 *   <dd> -Major revisions to size and padding calculations.</dd>
 *   <dd> -Fixes to write and remove methods.</dd>
 *   <dt> 1.5 - <small>2001.1129 by helliker</small> </dt>
 *   <dd> -Fixed file handle leaks</dd>
 *   <dt> 1.4 - <small>2001.1110 by helliker</small> </dt>
 *   <dd> -Removed the getPaddingBytes method because it was not needed.</dd>
 *
 *   <dt> 1.3 - <small>2001.1024 by helliker</small> </dt>
 *   <dd> -The padding size is updated before writing a tag, not during.</dd>
 *
 *   <dd> -Created a method to update the padding size.</dd>
 *   <dt> 1.2 - <small>2001.1019 by helliker</small> </dt>
 *   <dd> -All set for release.</dd>
 * </dl>
 *
 *
 *@author    Jonathan Hilliker
 *@version   1.11.1
 */

public class ID3v2Tag implements ID3Tag {
    
    /**
     * encoding type
     */
    private final String ENC_TYPE = "ISO-8859-1";
    // mp3Ext writes padding (wrongfully) as "MP3ext V...."
    /**
     * MP3 Ext badid
     */
    private final String MP3EXT_BADID = "MP3e";
    // Used to calculate padding change
    /**
     * Padding Change ???
     */
    private final int NEWTAG_LIMIT = 16000;
    
    /**
     * The File ???
     */
    private File mp3 = null;
    /**
     * ???
     */
    private ID3v2Header head = null;
    /**
     * ???
     */
    private ID3v2ExtendedHeader ext_head = null;
    /**
     * ???
     */
    private ID3v2Frames frames = null;
    /**
     * ???
     */
    private ID3v2Footer foot = null;
    /**
     * ???
     */
    private int padding;
    /**
     * ???
     */
    private int writtenTagSize;
    /**
     * ???
     */
    //  private int writtenPadding;
    /**
     * ???
     */
    private boolean exists;
    /**
     * ???
     */
    private long mpegOffset;
    
    /**
     * Create an id3v2 tag bound to the file provided as a parameter. If a tag
     * exists in the file already, then all the information in the tag will be
     * extracted. If a tag doesn't exist, then this is the file that will be
     * written to when the writeTag method is called.
     *
     *@param mp3                        the file to write/read the the tag
     *      information to/from
     *@param mpegOffset                 the byte offset where the mpeg frames
     *      begin
     *@exception FileNotFoundException  if an error occurs
     *@exception IOException            if an error occurs
     *@exception ID3v2FormatException   if an exisiting id3v2 tag isn't correct
     */
    public ID3v2Tag(File mp3, long mpegOffset)
    throws FileNotFoundException, IOException, ID3v2FormatException {
        init(mp3,mpegOffset,false);
    }
    

    public ID3v2Tag(File mp3, long mpegOffset, boolean emptyTag)
    throws FileNotFoundException, IOException, ID3v2FormatException {
        init(mp3,mpegOffset,emptyTag);
    }
    
    
    void init(File mp3, long mpegOffset, boolean emptyTag)
    throws FileNotFoundException, IOException, ID3v2FormatException {
        
        this.mp3 = mp3;
        this.mpegOffset = mpegOffset;
        
        frames = new ID3v2Frames();
        padding = 0;
        exists=false;
        if (emptyTag)
            head = new ID3v2Header(mp3,true);
        else {
            head = new ID3v2Header(mp3);
            exists = head.headerExists();
        }
        if (exists) {
            if (head.getExtendedHeader()) {
                ext_head = new ID3v2ExtendedHeader(mp3, 
                    head.getMajorVersion(),head.getUnsynchronisation());
            }
            if (head.getFooter()) {
                foot = new ID3v2Footer(mp3,
                head.getTagSize() + head.getHeaderSize());
            }
            
            UnsynchRandomAccessFile in = null;
            
            try {
                in = new UnsynchRandomAccessFile(mp3, "r");
                if (head.getMajorVersion()<=3)
                    in.setUnsynch(head.getUnsynchronisation());
                
                if (head.getMajorVersion() <= 4) {
                    parseFrames(in);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
            
            writtenTagSize = head.getTagSize();
            //      writtenPadding = mpegOffset - writtenTagSize;
            
            // Check to validate tag size taken out because MusicMatch
            // has some bugs that causes the check to fail
        }
    }
    
    
    
    static Properties v2Mappings;
    
    /**
     * Read the frames from the file and create ID3v2Frame objects from the data
     * found.
     *
     *@param raf                       the open file to read from
     *@exception IOException           if an error occurs
     *@exception ID3v2FormatException  if an error occurs
     */
    private void parseFrames(UnsynchRandomAccessFile raf)
    throws IOException, ID3v2FormatException {
        
        int offset = head.getHeaderSize();
        // Actually length of frames + padding
        int framesLength = head.getTagSize();
        int bytesRead = 0;
        int curLength = 0;
        ID3v2Frame frame = null;
        String id = null;
        byte[] buf = new byte[0];
        byte[] flags = new byte [2];
        boolean done = false;
        
        if (head.getExtendedHeader()) {
            framesLength -= ext_head.getBytesRead();
            offset += ext_head.getBytesRead();
        }
        
        raf.seek(offset);
        int majorVersion = head.getMajorVersion();
        while ((bytesRead < framesLength||raf.getAlternative()!=null) && !done) {
            switch(majorVersion) {
                case 2:
                    buf = new byte[3];
                    bytesRead += raf.read(buf);
                    id = new String(buf);
                    if (id.startsWith(MP3EXT_BADID) || id.indexOf(0) != -1) {
                        done=true;
                        break;
                    }
                    if (v2Mappings==null) {
                        v2Mappings = new Properties();
                        v2Mappings.load(ClassLoader.getSystemResourceAsStream
                            ("pgbennett/id3/v2mapping.properties"));
                    }
                    if ("CDM".equals(id)) {
                        // Compressed data
                        bytesRead += raf.read(buf);
                        int compLength = BinaryParser.convertToInt(buf);
                        byte[] method = new byte[1];
                        bytesRead += raf.read(method);
                        byte[] buf2 = new byte[4];
                        bytesRead += raf.read(buf2);
                        int uncompLength = BinaryParser.convertToInt(buf2);
                        byte [] compData = new byte[compLength];
                        bytesRead += raf.read(compData);
                        if (method[0]!='z' && method[0]!='Z')
                            continue;
                        // Decompress
                        byte [] uncompData = new byte[uncompLength];
                        try {
                             java.util.zip.Inflater decompresser = new java.util.zip.Inflater();
                             decompresser.setInput(compData);
                             int resultLength = decompresser.inflate(uncompData);
                             decompresser.end();
                        }
                         catch (Exception ex) {
                             ex.printStackTrace();
                             continue;
                             // compressed=false;
                        }
                        ByteArrayInputStream in = new ByteArrayInputStream(uncompData);
                        raf.setAlternative(in);
                        continue;
                    }
                    id = v2Mappings.getProperty(id);
                    bytesRead += raf.read(buf);
                    curLength = BinaryParser.convertToInt(buf);
                    flags[0]=0;
                    flags[1]=0;
                    break;
                case 3:
                    buf = new byte[4];
                    bytesRead += raf.read(buf);
                    id = new String(buf);
                    if (id.startsWith(MP3EXT_BADID) || id.indexOf(0) != -1) {
                        done=true;
                        break;
                    }
//                    bytesRead += 4;
//                    curLength = raf.readInt();
                    
                    bytesRead += raf.read(buf);
                    curLength = BinaryParser.convertToInt(buf);

                    // Added by Reed
                    if (curLength < 0 || curLength > framesLength - bytesRead) {
                        throw new ID3v2FormatException("ID3v2Tag.parseFrames: "
                        + "Invalid frame size");
                    }

                    bytesRead += raf.read(flags);
                    // Empty the flags since v2.3 flags are not supported
                    // in hte ID3v2Frame class
//                    flags[0]=0;
//                    flags[1]=0;
                    break;
                case 4:
                    raf.setUnsynch(false);
                    buf = new byte[4];
                    bytesRead += raf.read(buf);
                    id = new String(buf);
                    if (id.startsWith(MP3EXT_BADID) || id.indexOf(0) != -1) {
                        done=true;
                        break;
                    }
                    bytesRead += raf.read(buf);
                    curLength = BinaryParser.convertToSynchsafeInt(buf);
                    bytesRead += raf.read(flags);
                    boolean unsynchFrame = ((flags[1]&2)!=0);
                    raf.setUnsynch(head.getUnsynchronisation()||unsynchFrame);
                    break;
            }
            if (!done) {
                buf = new byte[curLength];
                bytesRead += raf.read(buf);
                if (majorVersion==2 && "APIC".equals(id)) {
                    // Picture format difference in version 2
                    byte [] buf2 = new byte[curLength+7];
                    // text encoding
                    buf2[0] = buf[0];
                    // new mime type
                    byte[] nameBytes = {'i','m','a','g','e','/'};
                    System.arraycopy(nameBytes,0, buf2, 1, 6);
                    System.arraycopy(buf,1, buf2, 7, 3);
                    buf2[10]=0;
                    System.arraycopy(buf,4, buf2, 11, buf.length - 4);
                    buf=buf2;
                }
                    
                if (id!=null) {
                    frame = new ID3v2Frame(id, flags, buf, majorVersion);
                    frames.put(id, frame);
                }
            } else {
                // We've hit padding so stop reading
                done = true;
                bytesRead -= buf.length;
            }
        }
        
        // Get around the possible precision loss
        Long tmp = new Long(mpegOffset - offset - bytesRead);
        padding = tmp.intValue();
        raf.setUnsynch(false);
    }
    
    
    /**
     * Saves all the information in the tag to the file passed to the constructor.
     * If a tag doesn't exist, a tag is prepended to the file. If the padding has
     * not changed since the creation of this object and the size is less than the
     * original size + the original padding, then the previous tag and part of the
     * previous padding will be overwritten. Otherwise, a new tag will be
     * prepended to the file.
     *
     *@exception FileNotFoundException  if an error occurs
     *@exception IOException            if an error occurs
     */
    public void writeTag() throws FileNotFoundException, IOException {
        RandomAccessFile raf = null;
        // We write version id3v2.3.0
        head.setVersion(3,0);
        // We do not support unsynchronization
        head.setUnsynchronisation(false);
        // Do not write out any footers because then the tag is not allowed
        // to have padding - we would have to write out the whole
        // file again
        head.setFooter(false);
        // Do not support writing extended header
        head.setExtendedHeader(false);
        
        int curSize = getTotalSize();
        head.setTagSize(getSize());
        RandomAccessFile rafTemp = null;
        
        try {
            
            // This means that the file does not need to change size
            if (mpegOffset == curSize) {
                raf = new RandomAccessFile(mp3, "rw");
                byte[] out = getBytes();
                raf.seek(0);
                raf.write(out);
            } else {
                // Mar 2004 Peter Bennett - chnage to use a buffer
                // of 128000 and a new file instead of reading
                // the whole file into storage
                // START of Guillaume Techene's modification
                
                byte[] id3 = getBytes();
                //        long size = raf.length() - mpegOffset;
                //        byte[] previous_file = new byte[(int) size];
                File mp3Temp = new File(mp3.getAbsolutePath()+".XTEMPX");
                if (!mp3.renameTo(mp3Temp))
                    throw new IOException("Unable to rename " + mp3.getAbsolutePath() +
                    " to " + mp3Temp.getAbsolutePath());
                
                raf = new RandomAccessFile(mp3, "rw");
                rafTemp = new RandomAccessFile(mp3Temp, "rw");
                
                byte[] buffer = new byte [128000];
                int leng;
                rafTemp.seek(mpegOffset);
                
                //        if (raf.read(previous_file) != previous_file.length) {
                //          throw new IOException("ID3v2Tag.removeTag: unexpected"
                //                                + " end of file encountered");
                //        }
                
                //        raf.setLength(size + id3.length);
                //        raf.seek(0);
                raf.write(id3);
                //        raf.write(previous_file);
                for (;;) {
                    leng=rafTemp.read(buffer);
                    if (leng==-1)
                        break;
                    raf.write(buffer,0,leng);
                }
                raf.close();
                raf=null;
                rafTemp.close();
                rafTemp=null;
                if (!mp3Temp.delete())
                    throw new IOException("Unable to delete " + mp3Temp.getAbsolutePath());
                
                // END of Guillaume Techene's modification
            }
        }
        finally {
            if (raf != null)
                raf.close();
            if (rafTemp != null)
                rafTemp.close();
        }
        
        writtenTagSize = curSize;
        //    writtenPadding = padding;
        exists = true;
    }
    
    
    /**
     * Remove an existing id3v2 tag from the file passed to the constructor.
     *
     *@exception FileNotFoundException  if an error occurs
     *@exception IOException            if an error occurs
     */
    public void removeTag() throws FileNotFoundException, IOException {
        if (exists) {
            RandomAccessFile raf = null;
            int fullTagSize = writtenTagSize + head.getHeaderSize();
            
            if (head.getFooter()) {
                fullTagSize += foot.getFooterSize();
            }
            
            try {
                Long bufSize = new Long(mp3.length() - fullTagSize);
                byte[] buf = new byte[bufSize.intValue()];
                raf = new RandomAccessFile(mp3, "rw");
                
                raf.seek(fullTagSize);
                
                if (raf.read(buf) != buf.length) {
                    throw new IOException("ID3v2Tag.removeTag: unexpected"
                    + " end of file encountered");
                }
                
                raf.setLength(bufSize.longValue());
                raf.seek(0);
                raf.write(buf);
                raf.close();
            } finally {
                if (raf != null) {
                    raf.close();
                }
            }
            
            exists = false;
        }
    }
    
    
    /**
     * Return a binary representation of this object to be written to a file. This
     * is in the format of the id3v2 specifications. This includes the header,
     * extended header (if it exists), the frames, padding (if it exists), and a
     * footer (if it exists).
     *
     *@return   a binary representation of this id3v2 tag
     */
    public byte[] getBytes() {
        byte[] b = new byte[getTotalSize()];
        int bytesCopied = 0;
        int length = 0;
        //    padding = getUpdatedPadding();
        length = head.getHeaderSize();
        System.arraycopy(head.getBytes(), 0, b, bytesCopied, length);
        bytesCopied += length;
        
        if (head.getExtendedHeader()) {
            length = ext_head.getSize();
            System.arraycopy(ext_head.getBytes(), 0, b, bytesCopied, length);
            bytesCopied += length;
        }
        
        length = frames.getLength();
        System.arraycopy(frames.getBytes(), 0, b, bytesCopied, length);
        bytesCopied += length;
        
        // Bytes should all be zero's by default
        if (padding > 0) {
            System.arraycopy(new byte[padding], 0, b, bytesCopied, padding);
            bytesCopied += padding;
        }
        
        if (head.getFooter()) {
            length = foot.getFooterSize();
            System.arraycopy(foot.getBytes(), 0, b, bytesCopied, length);
            bytesCopied += length;
        }
        
        return b;
    }
    
    
    /**
     * Set the data contained in a text frame. This includes all frames with an id
     * that starts with 'T' but excludes "TXXX". If an improper id is passed, then
     * nothing will happen.
     *
     *@param id    the id of the frame to set the data for
     *@param data  the data for the frame
     */
    public void setTextFrame(String id, String data) {
        if ((id.charAt(0) == 'T')
        && !id.equals(ID3v2Frames.USER_DEFINED_TEXT_INFO)) {

            setGenericFrame(id, null, null, data);
            
//            try {
//                byte[] b = new byte[data.length() + 1];
//                b[0] = 0;
//                System.arraycopy(data.getBytes(ENC_TYPE), 0, b, 1,
//                data.length());
//                
//                updateFrameData(id, b);
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
        }
    }
    
    
    /**
     * Set the data contained in a URL frame. This includes all frames with an id
     * that starts with 'W' but excludes "WXXX". If an improper id is passed, then
     * nothing will happen.
     *
     *@param id    the id of the frame to set the data for
     *@param data  the data for the frame
     */
    public void setURLFrame(String id, String data) {
        if ((id.charAt(0) == 'W')
        && !id.equals(ID3v2Frames.USER_DEFINED_URL)) {

            setGenericFrame(id, null, null, data);
//            updateFrameData(id, data.getBytes());
        }
    }
    
    
    /**
     * Sets the data contained in the user defined text frame (TXXX).
     *
     *@param description  a description of the data
     *@param value        the data for the frame
     */
    public void setUserDefinedTextFrame(String description, String value) {
        setGenericFrame(ID3v2Frames.USER_DEFINED_TEXT_INFO, null, description, value);
//        try {
//                    
//            byte[] b = new byte[description.length() + value.length() + 2];
//            int bytesCopied = 0;
//            b[bytesCopied++] = 0;
//            System.arraycopy(description.getBytes(ENC_TYPE), 0, b,
//            bytesCopied, description.length());
//            bytesCopied += description.length();
//            b[bytesCopied++] = 0;
//            System.arraycopy(value.getBytes(ENC_TYPE), 0, b, bytesCopied,
//            value.length());
//            bytesCopied += value.length();
//            
//            updateFrameData(ID3v2Frames.USER_DEFINED_TEXT_INFO, b);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
    }
    
    
    /**
     * Sets the data contained in the user defined url frame (WXXX).
     *
     *@param description  a description of the url
     *@param value        the url for the frame
     */
    public void setUserDefinedURLFrame(String description, String value) {
        setGenericFrame(ID3v2Frames.USER_DEFINED_URL, null, description, value);
//        try {
//            byte[] b = new byte[description.length() + value.length() + 2];
//            int bytesCopied = 0;
//            b[bytesCopied++] = 0;
//            System.arraycopy(description.getBytes(ENC_TYPE), 0, b,
//            bytesCopied, description.length());
//            bytesCopied += description.length();
//            b[bytesCopied++] = 0;
//            System.arraycopy(value.getBytes(), 0, b, bytesCopied,
//            value.length());
//            bytesCopied += value.length();
//            
//            updateFrameData(ID3v2Frames.USER_DEFINED_URL, b);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
    }
    
    
    /**
     * Set the data contained in the comments frame (COMM).
     *
     *@param description  a description of the comment
     *@param comment      the comment
     */
    public void setCommentFrame(String description, String comment) {
//        try {
            
            setGenericFrame(ID3v2Frames.COMMENTS, "eng", description, comment);
            
//            byte[] b = new byte[description.length() + comment.length() + 5];
//            int bytesCopied = 0;
//            b[bytesCopied++] = 0;
//            b[bytesCopied++] = 'e';
//            b[bytesCopied++] = 'n';
//            b[bytesCopied++] = 'g';
//            System.arraycopy(description.getBytes(ENC_TYPE), 0, b,
//            bytesCopied, description.length());
//            bytesCopied += description.length();
//            b[bytesCopied++] = 0;
//            System.arraycopy(comment.getBytes(ENC_TYPE), 0, b,
//            bytesCopied, comment.length());
//            bytesCopied += comment.length();
//            
//            updateFrameData(ID3v2Frames.COMMENTS, b);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
    }
    
    // Peter Bennett Feb 2004
    /**
     * Set the data contained in any frame.
     * Can be used generically with any frame.
     * The data bytes should have any zero values, language codes,
     * etc. included according to the specification.
     * Recommend 0 for encoding and eng for language.
     *
     *@param id    the id of the frame to set the data for
     *@param data  the data for the frame
     */
    public void setGenericFrame(String id, String data) {
        try {
            byte[] b = data.getBytes(ENC_TYPE);
            updateFrameData(id, b);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    
    public void setGenericFrame(String id, String lang, String desc, String data) {
        ID3v2Frame frame = null;
        if (data == null)
            data = "";
        try {
            frame = new ID3v2Frame(id, lang, desc, data);
        }
        catch(RuntimeException ex) {
            if (data.length() != 0)
                throw ex;
            frame=null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        if (data.length() == 0) {
            String extId;
            if (frame==null) {
                if (lang==null)
                    lang = "";
                if (desc == null)
                    desc = "";
                extId = id + lang + desc;
            }
            else
                extId = frame.getExtendedId();
            removeFrame(extId);
        }
        else
            frames.put(id, frame);
    }
    

    public void setPictureFrame(ID3v2Picture pic) throws UnsupportedEncodingException {
        ID3v2Frame frame = new ID3v2Frame(pic);
        if (pic.pictureData.length == 0) {
            // delete frame
            removeFrame(frame.getExtendedId());
        }
        else
            frames.put(frame.getExtendedId(), frame);
    }
    
    
    
    // Peter Bennett Feb 2004
    /**
     * Set the data contained in any frame.
     * Can be used generically with any frame.
     * The data bytes should have any zero values, language codes,
     * etc. included according to the specification.
     * Recommend 0 for encoding and eng for language.
     *
     *@param id    the id of the frame to set the data for
     *@param data  the data for the frame
     */
    public void setGenericFrame(String id, byte[] data) throws UnsupportedEncodingException {
        
        updateFrameData(id, data);
    }
    
    /**
     * Remove the frame with the specified id from the file. If there is no frame
     * with that id nothing will happen.
     *
     *@param id  the id of the frame to remove. Extended id in the
     * case of frames that support it.
     */
    public void removeFrame(String id) {
        frames.remove(id);
    }
    
    
    /**
     * Updates the data for the frame specified by id. If no frame exists for the
     * id specified, a new frame with that id is created.
     *
     *@param id    the id of the frame to update
     *@param data  the data for the frame
     */
    public void updateFrameData(String id, byte[] data) throws UnsupportedEncodingException {
        // Peter Bennett Feb 2004
        // Simplified so that it will work with extended ids
        ID3v2Frame frame = new ID3v2Frame(id, data);
        frames.put(id, frame);
    }
    
    
    /**
     * Returns the textual information contained in the frame specified by the id.
     * Not every type of frame has textual information. If an id is specified that
     * will not work, the empty string is returned.
     *
     *@param id                        the id of the frame to get text from
     *                                 or extended id.
     *@return                          the text information contained in the frame.
     *                                 if an extended id is supplied the returned data excludes it.
     *@exception ID3v2FormatException  if an error is encountered parsing data
     */
    public String getFrameDataString(String id) throws ID3v2FormatException {
        String str = new String();
        
        if (frames.containsKey(id)) {
            str = ((ID3v2Frame) frames.get(id)).getDataString();
        }
        
        // Peter Bennett Feb 2004
        // If using an extended id do not include it in the returned
        // data
        if (id.length() > 4) {
            int delimPos=str.indexOf(0);
            if (delimPos>-1)
                str=str.substring(delimPos+1);
        }
        if (str.length() > 0 && str.charAt(0) == 0xfeff)
            str=str.substring(1);
        
        return str;
    }
    
    
    /**
     * Returns the data found in the frame specified by the id. If the frame
     * doesn't exist, then a zero length array is returned.
     *
     *@param id  the id of the frame to get the data from
     *                                 or extended id.
     *@return    the data found in the frame.
     *           If an extended id is supplied it is included in the
     *           data.
     */
    public byte[] getFrameData(String id) {
        byte[] b = new byte[0];
        
        if (frames.containsKey(id)) {
            b = ((ID3v2Frame) frames.get(id)).getFrameData();
        }
        
        return b;
    }
    
    
    /**
     * Returns true if an id3v2 tag exists in the file that was passed to the
     * constructor and false otherwise
     *
     *@return   true if an id3v2 tag exists in the file passed to the ctor
     */
    public boolean tagExists() {
        return exists;
    }
    
    
    /**
     * Determines the new amount of padding to use. If the user has not changed
     * the amount of padding then existing padding will be overwritten instead of
     * increasing the size of the file. That is only if there is a sufficient
     * amount of padding for the updated tag.
     *
     *@return   the new amount of padding
     */
    //    private int getUpdatedPadding() {
    //    int pad = padding;
    //    int size = frames.getLength();
    //    int sizeDiff = 0;
    //
    //    if (head.getExtendedHeader()) {
    //      size += ext_head.getSize();
    //    }
    //
    //    size += padding;
    //    sizeDiff = size - writtenTagSize;
    //
    //    if ((padding == writtenPadding) && (sizeDiff != 0) && exists) {
    //      if (sizeDiff < 0) {
    //        pad += Math.abs(sizeDiff);
    //      } else if (sizeDiff <= padding) {
    //        pad -= sizeDiff;
    //      } else {
    //        // As the id3 team recommends, double the size of the tag
    //        // if it needs to get bigger
    //        int newTagSize = 2 * writtenTagSize;
    //        while (newTagSize < size) {
    //          newTagSize += writtenTagSize;
    //        }
    //
    //        if (newTagSize <= NEWTAG_LIMIT) {
    //          pad = newTagSize - size;
    //        } else {
    //          // Gee if it's over the limit this tag is pretty big,
    //          // so screw padding altogether
    //          // Peter Bennett Feb 2004
    //          // The id3 team also recommends using a smaller value
    //          // in this case, not zero
    //          pad = 4096;
    //        }
    //      }
    //    }
    //
    //    return pad;
    //  }
    
    
    /**
     * Returns the size of this id3v2 tag. This includes only the frames, extended
     * header, and padding. For the size of the entire tag including the header
     * and footer, use getTotalSize method.
     *
     *@return   the size (in bytes) of the id3v2 frames, extended header, footer
     */
    public int getSize() {
        int size = frames.getLength();
        int sizeDiff = 0;
        
        if (head.getExtendedHeader()) {
            size += ext_head.getSize();
        }
        
        size += padding;
        //    sizeDiff = size - writtenTagSize;
        
        //    if ((padding == writtenPadding) && (sizeDiff != 0)) {
        //      if ((sizeDiff < 0) || (sizeDiff <= padding)) {
        //        size = head.getTagSize();
        //      }
        //    }
        
        return size;
    }
    
    
    /**
     * Returns the actual size of the tag when written. Includes the header,
     * extended header, frames, padding, and footer.
     *
     *@return   the size (in bytes) of the entire id3v2 tag
     */
    public int getTotalSize() {
        
        // Peter Bennett Mar 2004
        // Make sure padding is correct
        // This changes the total size
        //    padding=getUpdatedPadding();
        padding = 0;
        
        int size = getSize();
        
        size += head.getHeaderSize();
        
        if (head.getFooter()) {
            size += foot.getFooterSize();
        }
        
        // Calculate what padding would be
        padding = (int)mpegOffset - size;
        // Excessive padding must be reduced
        if (padding > 16384)
            padding = -1;

//        if (size <= mpegOffset) {
//            padding = (int)mpegOffset - size;
//        }
//        else {
        // Negative padding must be fixed
        if (padding < 0) {
            padding = size;
            if (padding > 4096)
                padding = 4096;
            int newtagsize = size + padding;
            // round up tag size to multiple of 2048 bytes
            int roundDown = newtagsize & (Integer.MAX_VALUE-2047);
            if (roundDown < newtagsize)
                padding += roundDown+2048-newtagsize;
        }
        size += padding; // This would normally be included with getSize
        
        return size;
    }
    
    
    /**
     * Returns the current number of padding bytes in this id3v2 tag.
     *
     *@return   the current number of padding bytes in this id3v2 tag
     */
    public int getPadding() {
        return padding;
    }
    
    
    /**
     * Set the amount of padding to use when writing this tag. There cannot be any
     * padding if a footer exists. Nothing will happen if this function is called
     * and a footer exists or if the number is negative.
     *
     *@param pad  the amount of padding to use when writing this tag
     */
    //  public void setPadding(int pad) {
    //    if (!head.getFooter() && (pad >= 0)) {
    //      padding = pad;
    //    }
    //  }
    
    
    /**
     * Return a string representation of this object. This includes all data
     * contained in all parts of this tag.
     *
     *@return   a string representation of this object
     */
    public String toString() {
        String str = head.toString();
        
        str += "\nPadding:\t\t\t" + getPadding() + " bytes"
        + "\nTotalSize:\t\t\t" + getTotalSize() + " bytes";
        ;
        
        if (head.getExtendedHeader()) {
            str += "\n" + ext_head.toString();
        }
        
        str += "\n" + frames.toString();
        
        if (head.getFooter()) {
            str += foot.toString();
        }
        
        return str;
    }
    
    // Peter Bennett Feb 2004
    /**
     * Get a collection of all frames in the tag.
     *
     *
     *@return   collection based on a Map
     */
    public ID3v2Frames getFrames() {
        return frames;
    }
    
    public ID3v2Header getHead() {
        return head;
    }
    
    
    
    /**
     * Copies information from the ID3Tag parameter and inserts it into this tag.
     * Previous data will be overwritten. [NOT IMPLEMENTED]
     *
     *@param tag  the tag to copy from
     */
    public void copyFrom(ID3Tag tag) {
        // Not implemented yet
    }
    
}
